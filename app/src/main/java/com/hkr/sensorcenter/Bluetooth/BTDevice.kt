package com.hkr.sensorcenter.Bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

class BTDevice(
    val name:String,
    val address : String,
    val master: Boolean
)
@SuppressLint("MissingPermission")
fun BluetoothDevice.toBTDevice(isMaster:Boolean = false): BTDevice{
    return BTDevice(
        name= name,
        address = address,
        master = isMaster
    )
}