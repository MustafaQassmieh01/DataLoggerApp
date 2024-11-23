package com.hkr.sensorcenter.Bluetooth.Pair

import com.google.android.gms.common.ConnectionResult
import com.hkr.sensorcenter.Bluetooth.BTDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


interface BTMenu {



    fun turnOnBluetooth()

    fun startDiscover()

    fun stopDiscover()

    // Connects to a specific Bluetooth device.
    fun connectToDevice(device: BTDevice): Flow<ConnectionResult>

    fun getPairedDevices()

    fun connect()

    fun sendData()


    val isConnected: StateFlow<Boolean>
    val scannedDevices: StateFlow<List<Any>>
    val pairedDevices: StateFlow<List<Any>>
    val errors: SharedFlow<String>
}