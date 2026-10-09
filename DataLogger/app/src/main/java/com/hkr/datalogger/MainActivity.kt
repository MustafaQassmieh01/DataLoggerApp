package com.hkr.datalogger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hkr.datalogger.ui.theme.DataLoggerTheme

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Grant Bluetooth access to begin")
    private var connected by mutableStateOf(false)
    private var ready by mutableStateOf(false)
    private var devices by mutableStateOf<List<Pair<String, String>>>(emptyList())
    private val lines = mutableStateListOf<String>()
    private var session: BluetoothSession? = null
    private var role by mutableStateOf("MASTER")
    private var discovering by mutableStateOf(false)
    private var pendingAddress by mutableStateOf<String?>(null)
    private var scanAfterPermission = false
    private val main = Handler(Looper.getMainLooper())
    private val pairingTimeout = Runnable {
        pendingAddress = null
        status = "Pairing timed out. Retry after completing or dismissing the system prompt."
    }
    private lateinit var sensorCapture: SensorCapture
    private var capturing by mutableStateOf(false)
    private var sensorStatus by mutableStateOf("Sensor preview stopped")
    private var sensorAvailability by mutableStateOf<List<String>>(emptyList())
    private val sensorSamples = mutableStateMapOf<String, SensorSample>()
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (!allowed() && intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = bluetoothDevice(intent) ?: return
                    devices = (devices.filterNot { it.second == device.address } +
                        ((device.name ?: "Unnamed device") to device.address)).sortedBy { it.first }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> discovering = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> discovering = false
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) != BluetoothAdapter.STATE_ON) {
                        cancelPending()
                        ready = false
                        connected = false
                        discovering = false
                        session?.disconnect()
                        status = "Bluetooth is disabled"
                    } else refresh()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = bluetoothDevice(intent) ?: return
                    if (device.address != pendingAddress) return
                    when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)) {
                        BluetoothDevice.BOND_BONDED -> {
                            cancelPending()
                            if (role == "MASTER" && ready) connectBonded(device)
                        }
                        BluetoothDevice.BOND_NONE -> {
                            cancelPending()
                            status = "Pairing refused or cancelled. Tap the device to retry."
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun bluetoothDevice(intent: Intent): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private val enable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (allowed()) {
            prepare()
            if (scanAfterPermission && scanAllowed() && ready) discover()
        } else { ready = false; status = "Bluetooth permission denied; tap Enable / refresh to retry" }
        if (scanAfterPermission && !scanAllowed()) status = "Discovery permission denied; paired devices remain available"
        scanAfterPermission = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        role = savedInstanceState?.getString("role") ?: "MASTER"
        sensorCapture = SensorCapture(this) { sample -> sensorSamples[sample.channel] = sample }
        sensorAvailability = sensorCapture.availability()
        enableEdgeToEdge()
        setContent {
            var command by rememberSaveable { mutableStateOf("") }

            DataLoggerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Data Logger", style = MaterialTheme.typography.headlineMedium)
                        Text(status)
                        Button(onClick = { prepare() }) { Text("Enable / refresh Bluetooth") }
                        Text("Mode: $role", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MASTER", "SLAVE").forEach { choice ->
                                OutlinedButton(onClick = {
                                    if (role != choice) {
                                        stopSensors()
                                        cancelPending()
                                        stopDiscovery()
                                        session?.disconnect()
                                        connected = false
                                        lines.clear()
                                        command = ""
                                        role = choice
                                    }
                                }, enabled = role != choice) { Text(choice) }
                            }
                        }
                        Text(if (role == "MASTER") "Master connects to a paired slave and sends commands."
                             else "Slave listens for the master and can preview local sensors.")
                        OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) {
                            Text("Open Bluetooth pairing settings")
                        }
                        Text("Keep both phones nearby. Android will ask you to confirm pairing.")
                        if (role == "SLAVE") {
                            OutlinedButton(onClick = { makeDiscoverable() }, enabled = ready) { Text("Make discoverable (120 seconds)") }
                            Button(onClick = { session?.listen() }, enabled = ready && !connected) { Text("Listen for master") }
                        } else {
                            Button(onClick = { discover() }, enabled = ready && !connected && pendingAddress == null && !discovering) { Text("Find nearby devices") }
                            if (discovering) OutlinedButton(onClick = { stopDiscovery() }) { Text("Stop discovery") }
                            devices.forEach { (name, address) ->
                                OutlinedButton(onClick = { pairAndConnect(address) }, enabled = ready && !connected && pendingAddress == null) {
                                    Text("Pair / connect to $name\n$address")
                                }
                            }
                        }
                        if (role == "SLAVE") {
                            Text("Local sensor preview", style = MaterialTheme.typography.titleMedium)
                            sensorAvailability.forEach { Text(it) }
                            Text(sensorStatus)
                            Button(onClick = {
                                sensorSamples.clear()
                                val registered = sensorCapture.start()
                                capturing = sensorCapture.running
                                sensorStatus = if (capturing) "Previewing: ${registered.joinToString()} (up to 10 Hz per sensor)"
                                    else "No sensor listeners could be registered"
                            }, enabled = !capturing) { Text("Start sensor preview") }
                            OutlinedButton(onClick = { stopSensors() }, enabled = capturing) { Text("Stop sensor preview") }
                            sensorSamples.values.sortedBy { it.channel }.forEach { sample ->
                                Text("${sample.channel}: ${sample.values.joinToString()} ${sample.units} • #${sample.sequence}")
                            }
                            Text("Preview stays on this phone. Bluetooth sensor transmission is the next stage.")
                        }
                        OutlinedButton(onClick = { cancelPending(); stopDiscovery(); session?.disconnect() }, enabled = session != null) { Text("Disconnect / stop listening") }
                        OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text("ASCII command") }, singleLine = true)
                        Button(onClick = {
                            try { session?.send(command); command = "" }
                            catch (e: IllegalArgumentException) { status = e.message ?: "Invalid command" }
                        }, enabled = connected && command.isNotBlank()) { Text("Send") }
                        Text("Received data (latest 100 lines)", style = MaterialTheme.typography.titleMedium)
                        Text("Incoming newline-delimited ASCII is saved to the app's private received.log file.")
                        lines.forEach { Text(it) }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
        prepare()
    }

    private fun allowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun prepare() {
        if (!allowed()) { permission.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT)); return }
        val bluetooth = adapter
        if (bluetooth == null) { ready = false; status = "This device does not support Bluetooth"; return }
        if (!bluetooth.isEnabled) { ready = false; enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return }
        refresh()
    }

    @SuppressLint("MissingPermission")
    private fun refresh() {
        if (!allowed()) { ready = false; return }
        val bluetooth = adapter ?: return
        ready = bluetooth.isEnabled
        if (!ready) { session?.disconnect(); status = "Bluetooth is disabled"; return }
        devices = bluetooth.bondedDevices.map { (it.name ?: "Unnamed device") to it.address }.sortedBy { it.first }
        if (session == null) {
            session = BluetoothSession(this, bluetooth, { message, isConnected ->
                status = message; connected = isConnected
            }, { line ->
                lines.add(line)
                if (lines.size > 100) lines.removeAt(0)
            })
            status = if (devices.isEmpty()) "No paired devices. Find nearby devices to pair." else "Ready"
        }
    }

    private fun scanAllowed() = checkSelfPermission(if (Build.VERSION.SDK_INT >= 31)
        Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun discover() {
        if (!allowed() || !scanAllowed()) {
            scanAfterPermission = true
            permission.launch(if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        if (!ready || connected || role != "MASTER") return
        stopDiscovery()
        devices = adapter?.bondedDevices.orEmpty().map { (it.name ?: "Unnamed device") to it.address }
        discovering = adapter?.startDiscovery() == true
        status = if (discovering) "Searching for nearby devices…" else "Discovery could not start. On Android 9–11, enable Location in system settings."
    }

    @SuppressLint("MissingPermission")
    private fun stopDiscovery() {
        if (Build.VERSION.SDK_INT < 31 || scanAllowed()) adapter?.cancelDiscovery()
        discovering = false
    }

    private fun cancelPending() {
        main.removeCallbacks(pairingTimeout)
        pendingAddress = null
    }

    @SuppressLint("MissingPermission")
    private fun pairAndConnect(address: String) {
        if (!allowed() || !ready || role != "MASTER" || connected || pendingAddress != null) return
        stopDiscovery()
        val device = adapter?.getRemoteDevice(address) ?: return
        if (device.bondState == BluetoothDevice.BOND_BONDED) { connectBonded(device); return }
        pendingAddress = address
        status = "Confirm pairing with ${device.name ?: address} in Android's system prompt"
        main.postDelayed(pairingTimeout, 60_000)
        if (device.bondState != BluetoothDevice.BOND_BONDING && !device.createBond()) {
            cancelPending()
            status = "Pairing could not start. Retry or use Bluetooth settings."
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBonded(device: BluetoothDevice) {
        if (!allowed() || device.bondState != BluetoothDevice.BOND_BONDED) return
        stopDiscovery()
        session?.connect(device.address)
    }

    @SuppressLint("MissingPermission")
    private fun makeDiscoverable() {
        if (!allowed() || !ready || role != "SLAVE") return
        enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("role", role)
        super.onSaveInstanceState(outState)
    }

    private fun stopSensors() {
        sensorCapture.stop()
        capturing = false
        sensorStatus = "Sensor preview stopped"
    }

    override fun onStop() {
        stopSensors()
        stopDiscovery()
        super.onStop()
    }

    override fun onDestroy() {
        stopSensors()
        cancelPending()
        stopDiscovery()
        if (receiverRegistered) unregisterReceiver(receiver)
        session?.close()
        super.onDestroy()
    }
}
