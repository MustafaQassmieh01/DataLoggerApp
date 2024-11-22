package com.hkr.sensorcenter.Bluetooth.Pair

import com.google.android.gms.common.ConnectionResult
import com.hkr.sensorcenter.Bluetooth.BTDevice
import kotlinx.coroutines.flow.Flow


interface BTMenu {



    fun turnOnBluetooth()

    fun startDiscover()

    fun stopDiscover()

    // Connects to a specific Bluetooth device.
    fun connectToDevice(device: BTDevice): Flow<ConnectionResult>

    fun connect()

    fun sendData()


}