package com.ethernom.gattuartserver

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import com.ethernom.gattuartserver.util.*
import kotlin.collections.ArrayList


class UARTService(ctx:Context) {
    var TAG = "UARTService"
    var ETH_BLE_HEADER_SIZE = 8

    private val context = ctx
    private var bluetoothManager  = ctx.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothAdapter = bluetoothManager.adapter
    private var bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
    private  var bluetoothGattServer: BluetoothGattServer? = null
    private var connectedDevices:ArrayList<BluetoothDevice> = ArrayList()
    private var state:State = State.INIT_0000
    private var buffer: ArrayList<Byte> = ArrayList()
    private var bufferSize = 0
    private var MTU = 512
    private var indicationConfirm:IndicationConfirm? = null

    /**
     * Create the GATT server instance, attaching all services and
     * characteristics that should be exposed
     */
    fun initServer() {
        bluetoothGattServer =  bluetoothManager.openGattServer(context, gattServerCallBack)

        val UART_SERVICE = BluetoothGattService(UARTProfile.UART_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val TX_READ_CHAR = BluetoothGattCharacteristic(
            UARTProfile.TX_READ_CHAR,  //Read-only characteristic, supports notifications
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // Descriptor for read notifications
        val TX_READ_CHAR_DESC = BluetoothGattDescriptor(UARTProfile.TX_READ_CHAR_DESC, UARTProfile.DESCRIPTOR_PERMISSION)
        TX_READ_CHAR.addDescriptor(TX_READ_CHAR_DESC)

        val RX_WRITE_CHAR = BluetoothGattCharacteristic(
            UARTProfile.RX_WRITE_CHAR,  //write permissions
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        UART_SERVICE.addCharacteristic(TX_READ_CHAR)
        UART_SERVICE.addCharacteristic(RX_WRITE_CHAR)
        bluetoothGattServer?.addService(UART_SERVICE)
    }

    /**
     * Initialize the advertiser
     */
    fun startAdvertising() {
        if (bluetoothLeAdvertiser == null) return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(UARTProfile.ETH_SERVICE_DISCOVER_UUID))
            .build()
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
    }

    fun send(device: BluetoothDevice, data:ByteArray) {
        val readCharacteristic: BluetoothGattCharacteristic = bluetoothGattServer?.getService(UARTProfile.UART_SERVICE)?.getCharacteristic(UARTProfile.TX_READ_CHAR)!!
        readCharacteristic.value = data
        Log.d(TAG, "Sending Notifications$data")
        val isNotified: Boolean = bluetoothGattServer?.notifyCharacteristicChanged(device, readCharacteristic, true)!!
        Log.d(TAG, "Notifications =$isNotified")
        val intent = Intent(DATA_SEND)
            .putExtra(INTENT.DEVICE, device)
            .putExtra(INTENT.DATA, data.hexa())
        context.sendBroadcast(intent)
    }

    private val gattServerCallBack:BluetoothGattServerCallback =  object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.i(TAG, ("onConnectionStateChange " + UARTProfile.getStatusDescription(status)) + " " + UARTProfile.getStateDescription(newState))
            val intent = Intent(DEVICE_CHANGE)
                .putExtra(INTENT.DEVICE, device)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevices.add(device!!)
                intent.putExtra(INTENT.STATUS, true)
                state = State.CARD_1000
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevices.remove(device!!)
                intent.putExtra(INTENT.STATUS, false)
                state = State.INIT_0000
            }
            context.sendBroadcast(intent)
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d("Start", "Our gatt server service was added.")
            super.onServiceAdded(status, service)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d(TAG, "READ called onCharacteristicReadRequest " + characteristic.uuid.toString())
            if (UARTProfile.TX_READ_CHAR == characteristic.uuid) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, storage)
            }
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            Log.i(TAG, "onCharacteristicWriteRequest " + characteristic.uuid.toString())
            if (UARTProfile.RX_WRITE_CHAR == characteristic.uuid) {
                //IMP: Copy the received value to storage
                storage = value
                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
                    Log.d(TAG, "Received  data on " + characteristic.uuid.toString())
//                    Log.d(TAG, "Received data" +value.hexa())
                }
                val intent = Intent(DATA_RECEIVE)
                    .putExtra(INTENT.DEVICE, device)
                    .putExtra(INTENT.DATA, value.hexa())
                context.sendBroadcast(intent)

                //IMP: Respond
                sendOurResponse(device!!)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Log.d("GattServer", "onNotificationSent $status")
            super.onNotificationSent(device, status)
            if(indicationConfirm != null) {
                val tmpIndicationConfirm = indicationConfirm
                indicationConfirm = null
                tmpIndicationConfirm?.onIndicationConfirmation(status)
            }

        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            Log.d("HELLO", "Our gatt server descriptor was read.")
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Log.d("DONE", "Our gatt server descriptor was read.")
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.d("HELLO", "Our gatt server descriptor was written.")
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            Log.d("DONE", "Our gatt server descriptor was written.")

            // NOTE: Its important to send response. It expects response else it will disconnect
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Log.d("GattServer", "onMtuChanged $mtu")
            MTU = mtu
        }

        //end of gatt server
    }

    /**
     *  Callback handles events from the framework describing
     * if we were successful in starting the advertisement requests.
     */
    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "Peripheral Advertise Started.")
            val intent = Intent(SERVER_STATUS)
                .putExtra(INTENT.STATUS, "Peripheral Advertise Started.")
            context.sendBroadcast(intent)

        }
        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "Peripheral Advertise Failed: $errorCode")
            val intent = Intent(SERVER_STATUS)
                .putExtra(INTENT.STATUS, "Peripheral Advertise Failed: $errorCode")
            context.sendBroadcast(intent)
        }
    }

    //Send notification to the devices once you write
    private fun sendOurResponse(device: BluetoothDevice) {
        var notifyMsg = storage
        Log.d("UART_SERVER", " sendOurResponse ${notifyMsg.hexa()}")
        if (state == State.CARD_1000) {
            if(notifyMsg[0] == 0x02.toByte()) {
                notifyMsg = byteArrayOf(0x42, 0x00, 0x00, 0x00)
                state = State.CARD_1000
            }
            else if (notifyMsg[0] == 0x03.toByte()) {
                notifyMsg = byteArrayOf(0x43, 0x00, 0x00, 0x01, 0x00)
                state = State.APP_4000
            }
        }
        else if (state == State.APP_4000) {
           val res = checkHeaderAndReassemblePackets(notifyMsg)
            if(!res) return
            val finaldata = buffer.toByteArray()
            clearBuffer()

            if (finaldata[6] == 0x01.toByte()) {
                Log.d("UART_SERVER", " connect")
                val sp = finaldata[0]
                val dp = finaldata[1]
                finaldata[0] = dp
                finaldata[1] = sp
                finaldata[6] = 0x02
                finaldata[7] = getCheckSum(finaldata)
                Log.d("UART_SERVER", " connect resp ${finaldata.hexa()}")
            }
            else if(finaldata[6] == 0x03.toByte()) {
                Log.d("UART_SERVER", " Data")
                val sp = finaldata[0]
                val dp = finaldata[1]
                finaldata[0] = dp
                finaldata[1] = sp
                val payload = finaldata.copyOfRange(8, finaldata.size)
                val lsb = payload.size shr 8
                val msb = payload.size and 0xff

                if(finaldata[8] == 0x20.toByte()) { // ONE SHOT
                    finaldata[8] = 0x50
                }
                finaldata[4] = lsb.toByte()
                finaldata[5] = msb.toByte()
                finaldata[7] = getCheckSum(finaldata)
            }
            notifyMsg = finaldata
        }
        val intent = Intent(DATA_SEND)
            .putExtra( INTENT.DEVICE, device)
            .putExtra(INTENT.DATA, notifyMsg.hexa())
        context.sendBroadcast(intent)
        val packets = fragmentPackets(notifyMsg, MTU - 5)
        recursiveIndication(device,packets)
    }

    private fun recursiveIndication(device: BluetoothDevice, packets: ArrayList<ByteArray>) {

        indicateData(device, packets[0],
            object : IndicationConfirm {
                override fun onIndicationConfirmation(status: Int) {
                    Log.d(TAG, "onIndicationConfirmation ")
                    if(packets.size > 1) {
                        packets.removeAt(0)
                        recursiveIndication(device, packets)
                    }
                }
            })
    }

    private fun indicateData(device: BluetoothDevice, packet: ByteArray, indicationConfirm: IndicationConfirm) {
        this.indicationConfirm = indicationConfirm
        val readCharacteristic: BluetoothGattCharacteristic = bluetoothGattServer?.getService(UARTProfile.UART_SERVICE)?.getCharacteristic(UARTProfile.TX_READ_CHAR)!!
        readCharacteristic.value = packet
        Log.d(TAG, "Sending Indication ${packet.hexa()}")
        val isNotified = bluetoothGattServer?.notifyCharacteristicChanged(device, readCharacteristic, true)!!
        Log.d(TAG, "Indications =$isNotified")
    }

    private var storage = "1111".toByteArray(Charsets.UTF_8)

    private fun getCheckSum(packet: ByteArray) :Byte{
        var xorValue = packet[0].toInt()
        // xor the packet header for checksum
        for (j in 1..6) {
            val c = packet[j].toInt()
            xorValue = xorValue xor c
        }
        return xorValue.toByte()
    }

    private fun fragmentPackets(data: ByteArray, maxSize: Int): ArrayList<ByteArray> {
        val result = arrayListOf<ByteArray>()
        for (x in data.indices step maxSize) {
            result.add(data.copyOfRange(x, Math.min(data.size, x + maxSize)))
        }
        return result
    }

    private fun checkHeaderAndReassemblePackets(packet: ByteArray): Boolean {
        if (bufferSize == 0 && packet.size >= ETH_BLE_HEADER_SIZE) { // check first packet
            // calculate checksum of header
            val checksum = getCheckSum(packet)
            // Check checksum
            if (packet[7] != checksum) {
                return false
            }
            bufferSize = getPayloadLength(packet[4], packet[5]) // index 4 & 5 are length of payload
            println("buffer size $bufferSize ")
        }
        println("+++++++reassemble+++++++")
        println("packet <- ${packet.hexa()} ")
        buffer.addAll(packet.toList())
        println("packet F <- ${buffer.size} ")

        if (bufferSize == buffer.size - ETH_BLE_HEADER_SIZE) {
            return true
        }
        return false
    }

    private fun clearBuffer() {
        buffer.clear() // clear buffer
        bufferSize = 0
    }

    private fun getPayloadLength(l0: Byte, l1: Byte) : Int{ // Get length of payload
        val l = byteArrayOf(l0, l1)
        return l.hexa().toInt(radix = 16)
    }

}

interface  IndicationConfirm {
    fun onIndicationConfirmation(status:Int)
}