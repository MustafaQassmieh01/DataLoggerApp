package com.hkr.sensorcenter.Bluetooth.Pair

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.hkr.sensorcenter.Bluetooth.BTDevice
import kotlinx.coroutines.flow.Flow


@SuppressLint("MissingPermission")
class BTController (private val context: Context, private val bluetoothAdapter: BluetoothAdapter?)
    : BTMenu {
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* Not needed */ }
    override fun turnOnBluetooth() {
        if(!checkPermissions()){
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            val enableBT = Intent(ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableIntent)
        }else {
            bluetoothAdapter?.enable()
        }
    }

    override fun startDiscover() {
        TODO("Not yet implemented")
    }

    override fun stopDiscover() {
        TODO("Not yet implemented")
    }

    override fun connectToDevice(device: BTDevice): Flow<ConnectionResult> {
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

}