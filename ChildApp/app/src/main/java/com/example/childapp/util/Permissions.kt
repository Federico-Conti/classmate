package com.example.childapp.util

import android.Manifest
import android.os.Build

object Permissions {
    fun requiredPermissions(): Array<String> {
        val list = mutableListOf<String>()

        // Android 13+ notifications
        if (Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.POST_NOTIFICATIONS
            list += Manifest.permission.NEARBY_WIFI_DEVICES
            list += Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            list += Manifest.permission.ACCESS_FINE_LOCATION
            list += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        return list.toTypedArray()
    }
}
