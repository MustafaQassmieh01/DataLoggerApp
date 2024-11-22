package com.hkr.sensorcenter.Bluetooth.Commands

data class Cmd(
    val prefix:Char= '[',
    val command: String,
    val argv:Array<String>
)
