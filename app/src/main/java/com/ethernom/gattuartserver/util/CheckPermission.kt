package com.ethernom.gattuartserver.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

object CheckPermission {
    val isBluetoothEnable: Boolean
        get() = try {
            val bAdapter = BluetoothAdapter.getDefaultAdapter()
            bAdapter?.isEnabled ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
}