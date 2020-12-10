package com.ethernom.gattuartserver.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ethernom.gattuartserver.R
import kotlinx.android.synthetic.main.item_discover.view.*

class DeviceAdapter(val cardInfos: ArrayList<BluetoothDevice>, val recyclerviewItemClick: RecyclerviewItemClick) : RecyclerView.Adapter<DeviceAdapter.DiscoverViewHolder>() {

    inner class DiscoverViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bindView(position: Int) {
            val cardInfo = cardInfos[position]
            itemView.txt_card_name.text = "Name: "+cardInfo.name
            itemView.txt_card_sn.text = "Address: "+cardInfo.address

            itemView.setOnClickListener {
                recyclerviewItemClick.onItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverViewHolder {
        return DiscoverViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_discover,parent,false))
    }

    override fun getItemCount(): Int {
        return cardInfos.size
    }

    override fun onBindViewHolder(holder: DiscoverViewHolder, position: Int) {
        holder.bindView(position)
    }
}
interface RecyclerviewItemClick {
    fun onItemClick(position: Int)
}