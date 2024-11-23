package com.hkr.sensorcenter.Bluetooth.Pair

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.ConnectionResult
import com.hkr.sensorcenter.Bluetooth.BTDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import com.hkr.sensorcenter.Bluetooth.Pair.FoundDeviceReciever
import com.hkr.sensorcenter.Bluetooth.toBTDevice
import kotlin.math.log

@SuppressLint("MissingPermission")
class BTController (private val context: Context, private val bluetoothAdapter: BluetoothAdapter?)
    : BTMenu {

    // StateFlow to manage connection and devices
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BTDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BTDevice>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BTDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BTDevice>>
        get() = _pairedDevices.asStateFlow()

    // SharedFlow to emit error messages
    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReciever { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBTDevice()
            if (newDevice in devices) devices else devices + newDevice
        }
    }


    override fun turnOnBluetooth() {
        if(!checkPermissions()){
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){

            val enableBluetoothLauncher =
                (context as? ComponentActivity)?.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    // Handle the result of the Bluetooth enable activity if needed
                    if (result.resultCode == Activity.RESULT_OK) {
                        println("Bluetooth enabled successfully.")
                    } else {
                        println("Failed to enable Bluetooth.")
                    }
                }
            val enableBT = Intent(ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher?.launch(enableBT)
        }else {
            bluetoothAdapter?.enable()
        }
    }

    override fun startDiscover() {
        if (!checkPermissions()){
            return
        }
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.S){
            context.registerReceiver(
                foundDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            updatePairedDevices()
            bluetoothAdapter?.startDiscovery()
        }
    }

    override fun stopDiscover() {
        TODO("Not yet implemented")
    }

    override fun connectToDevice(device: BTDevice): Flow<ConnectionResult> {
        TODO("Not yet implemented")
    }

    override fun getPairedDevices() {
        TODO("Not yet implemented")
    }

    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun sendData() {
        TODO("Not yet implemented")
    }

    private fun checkPermissions ():Boolean{
        return true
    }

    fun updatePairedDevices() {
        val paired: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        paired?.forEach{
        device ->
            val deviceName = device.name
            val macAddress = device.address
        }
        val pairedBTDevices = paired?.map{ it.toBTDevice() }
        if (pairedBTDevices != null) {
            _pairedDevices.value = pairedBTDevices
        }
        else {


        }

    }


    companion object{
        val UUID = "c61467fb-8db7-4793-b5bb-42cfacb0184e"
    }


}