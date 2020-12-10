package com.ethernom.gattuartserver

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.recyclerview.widget.LinearLayoutManager
import com.ethernom.gattuartserver.adapter.DeviceAdapter
import com.ethernom.gattuartserver.adapter.RecyclerviewItemClick
import com.ethernom.gattuartserver.util.*
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), RecyclerviewItemClick {
    var uartService :UARTService? = null
    private lateinit var adapter: DeviceAdapter
    private var connectedDevices: ArrayList<BluetoothDevice> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        rcvDiscover.layoutManager = LinearLayoutManager(this)
        adapter = DeviceAdapter(connectedDevices, this)
        rcvDiscover.adapter = adapter

        if(CheckPermission.isBluetoothEnable) {
            // If everything is okay then start
            start()
        } else {
            dialogBLEPopup(this)
        }
    }

    fun bleOnEvent() {
        start()
    }

    private var uartServiceCallback = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                SERVER_STATUS -> {
                    val status = intent.getStringExtra(INTENT.STATUS)
                    server_status.text = "Status: $status"
                }
                DEVICE_CHANGE ->  {
                    val device = intent.getParcelableExtra<BluetoothDevice>(INTENT.DEVICE)!!
                    val status = intent.getBooleanExtra(INTENT.STATUS, false)
                    postDeviceChange(device, status)
                }
                DATA_SEND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(INTENT.DEVICE)!!
                    val data = intent.getStringExtra(INTENT.DATA)
                    msg_list.append("Notified to ${device.address} : $data \n")
                }
                DATA_RECEIVE -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(INTENT.DEVICE)!!
                    val data = intent.getStringExtra(INTENT.DATA)
                    msg_list.append("Receive from ${device.address} : $data \n")


                }
            }

        }
    }
    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(SERVER_STATUS)
        intentFilter.addAction(DEVICE_CHANGE)
        intentFilter.addAction(DATA_SEND)
        intentFilter.addAction(DATA_RECEIVE)
        this.registerReceiver(uartServiceCallback, intentFilter)

    }

    private fun postDeviceChange(device: BluetoothDevice, toAdd: Boolean) {
        if (toAdd) {
            connectedDevices.add(device)
        }else {
            connectedDevices.remove(device)
        }
        rcvDiscover.adapter?.notifyDataSetChanged()
    }

    private fun start() {
        uartService = UARTService(this)
        registerBroadcastReceiver()
        uartService?.initServer()
        uartService?.startAdvertising()
    }

    private fun dialogBLEPopup(ctx: Context) {
        // Track bluetooth state change
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ctx.registerReceiver(bluetoothStateChange, filter)
        val intent = Intent(ctx, BLEDialogActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    private val bluetoothStateChange =
        object : BroadcastReceiver() { // Broadcast receiver callback Bluetooth state change
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent!!.action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    when (intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )) {
                        BluetoothAdapter.STATE_ON -> {
                            bleOnEvent()
                            unregisterBLEBroadcastReceiver()
                        }
                    }
                }
            }
        }

    private fun unregisterBLEBroadcastReceiver() {
        unregisterReceiver(bluetoothStateChange)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(uartServiceCallback)
    }

    override fun onItemClick(position: Int) {
        sendDialog(connectedDevices[position])
    }


    fun sendDialog(device: BluetoothDevice) {
        val alertDialog =  AlertDialog.Builder(this);
        alertDialog.setTitle("Data");
        alertDialog.setMessage("Enter Data");

         val input =  EditText(this);
        val lp =  LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setIcon(R.drawable.ic_launcher_background)

        alertDialog.setPositiveButton("SEND", DialogInterface.OnClickListener { _, _ ->
            uartService?.send(device,input.text.toString().toByteArray(Charsets.UTF_8))
        })
        alertDialog.show();
    }
}