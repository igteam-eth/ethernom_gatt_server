package com.ethernom.gattuartserver.util

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ethernom.gattuartserver.R

class BLEDialogActivity : AppCompatActivity() {
    lateinit var bleAlertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleMessageDialog()
    }

    private fun bleMessageDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.warning))
        builder.setMessage(resources.getString(R.string.turn_on_bluetooth_message))
        builder.setCancelable(true)
        builder.setPositiveButton(resources.getString(R.string.enable)) { dialog, _ ->
            dialog.cancel()
                turnOnBluetooth()
                finish()
        }
        bleAlertDialog = builder.create()
        bleAlertDialog.show()
    }

    private fun turnOnBluetooth(): Boolean {
        val bluetoothAdapter = BluetoothAdapter
            .getDefaultAdapter()
        return bluetoothAdapter?.enable() ?: false
    }
}