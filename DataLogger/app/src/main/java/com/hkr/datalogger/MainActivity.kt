package com.hkr.datalogger

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private val enable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) prepare() else { ready = false; status = "Bluetooth permission denied; tap Enable / refresh to retry" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var command by remember { mutableStateOf("") }
            DataLoggerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Data Logger", style = MaterialTheme.typography.headlineMedium)
                        Text(status)
                        Button(onClick = { prepare() }) { Text("Enable / refresh Bluetooth") }
                        Text("Pair the phones in Android settings first. Use Listen on the slave, then connect from the master.")
                        Button(onClick = { session?.listen() }, enabled = ready && !connected) { Text("Listen as slave") }
                        devices.forEach { (name, address) ->
                            OutlinedButton(onClick = { session?.connect(address) }, enabled = ready && !connected) {
                                Text("Connect to $name\n$address")
                            }
                        }
                        OutlinedButton(onClick = { session?.disconnect() }, enabled = session != null) { Text("Disconnect / stop listening") }
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
        prepare()
    }

    private fun allowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun prepare() {
        if (!allowed()) { permission.launch(Manifest.permission.BLUETOOTH_CONNECT); return }
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
            status = if (devices.isEmpty()) "No paired devices. Pair in Android settings." else "Ready"
        }
    }

    override fun onDestroy() {
        session?.close()
        super.onDestroy()
    }
}
