package com.ethernom.gattuartserver

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import java.util.*

object UARTProfile {
    val ETH_SERVICE_DISCOVER_UUID =  UUID.fromString("19490016-5537-4f5e-99ca-290f4fbff142")
    //Service UUID to expose our UART characteristics
    val UART_SERVICE = UUID.fromString("19500001-5537-4f5e-99ca-290f4fbff142")

    //RX, Write characteristic
     val RX_WRITE_CHAR = UUID.fromString("19500002-5537-4f5e-99ca-290f4fbff142")

    //TX READ Notify
     val TX_READ_CHAR = UUID.fromString("19500003-5537-4f5e-99ca-290f4fbff142")
     val TX_READ_CHAR_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
     val DESCRIPTOR_PERMISSION = BluetoothGattDescriptor.PERMISSION_WRITE

    fun getStateDescription(state: Int): String {
        return when (state) {
            BluetoothProfile.STATE_CONNECTED -> "Connected"
            BluetoothProfile.STATE_CONNECTING -> "Connecting"
            BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            else -> "Unknown State $state"
        }
    }

    fun getStatusDescription(status: Int): String {
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "SUCCESS"
            else -> "Unknown Status $status"
        }
    }
}