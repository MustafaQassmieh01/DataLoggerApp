package com.hkr.sensorcenter.Bluetooth

import android.bluetooth.BluetoothSocket
import com.hkr.sensorcenter.Bluetooth.Commands.Cmd

interface Comunication {

    fun connect (device: BTDevice):BluetoothSocket

    fun sendCmd (socket: BluetoothSocket,cmd: Cmd)

    fun status()

    // keep going as you go

}