package com.example.childapp.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log

class WifiScanReceiver(
    private val wifiManager: WifiManager,
    private val onTargetFound: (ScanResult) -> Unit, //function passed when i create a constructor in FWifiScanService
    private val targetSsid: String,
    private val targetBssid: String? = null
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val results = wifiManager.scanResults ?: return

        val target = results.firstOrNull { r ->
            val ssidOk = r.SSID == targetSsid
            val bssidOk = targetBssid?.let { it.equals(r.BSSID, ignoreCase = true) } ?: true
            ssidOk && bssidOk
        } ?: return

        if (target != null) {
            Log.d("WifiScanReceiver", "WIFI Target found: $targetSsid, level=${target.level}")
            onTargetFound(target)
        } else {
            Log.w("WifiScanReceiver", "SSID Target NOT found")
        }
    }
}
