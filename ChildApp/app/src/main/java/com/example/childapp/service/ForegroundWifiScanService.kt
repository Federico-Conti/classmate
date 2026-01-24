package com.example.childapp.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.example.childapp.data.firestore.AttendanceRepository
import com.example.childapp.util.Notifications
import com.example.childapp.wifi.WifiScanProcessor
import com.example.childapp.wifi.WifiScanReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ForegroundWifiScanService : Service() {

    companion object {
        // In produzione: passali via Intent extras o DataStore
        const val EXTRA_CHILD_ID = "extra_child_id"
        const val EXTRA_CHILD_NAME = "extra_child_name"
        const val EXTRA_CHILD_SURNAME = "extra_child_surname"
        const val EXTRA_TARGET_SSID = "extra_target_ssid"
        const val EXTRA_TARGET_BSSID = "extra_target_bssid"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var wifiManager: WifiManager
    private lateinit var receiver: WifiScanReceiver

    private val processor = WifiScanProcessor(
        windowSize = 5,
        thresholdDbm = -60,
        stableDecisionsNeeded = 2
    )

    private val repo = AttendanceRepository()
    private var lastType: String? = null

    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        Log.i("ForegroundWifiScanService", "Service onCreate")
        super.onCreate()

        Notifications.ensureChannel(this)
        startForeground(
            Notifications.NOTIF_ID,
            Notifications.foregroundNotification(this, "Start scanning WIFI ... ")
        )

        wifiManager = getSystemService<WifiManager>()!!

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val childId = intent?.getStringExtra(EXTRA_CHILD_ID) ?: ""
        val name = intent?.getStringExtra(EXTRA_CHILD_NAME) ?: ""
        val surname = intent?.getStringExtra(EXTRA_CHILD_SURNAME) ?: ""

        val targetSsid = intent?.getStringExtra(EXTRA_TARGET_SSID) ?: ""
        val targetBssid = intent?.getStringExtra(EXTRA_TARGET_BSSID) ?: ""

        Log.i("ForegroundWifiScanService", """
        Service onStartCommand data:
        Child ID: $childId
        Name: $name $surname
        Target SSID: $targetSsid
        Target BSSID: $targetBssid
        """.trimIndent())

        receiver = WifiScanReceiver( wifiManager = wifiManager,  onTargetFound = { scanResult ->
                                                                                    val maybeEvent = processor.onNewRssiSample(scanResult.level)
                                                                                    if (maybeEvent != null) {
                                                                                        onStableDecision(childId, name, surname, maybeEvent) // call if (target & stableCount==3)
                                                                                    }
                                                                                },
            targetSsid = targetSsid,
            targetBssid = targetBssid
        )

        registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        serviceScope.launch {
            lastType = repo.getLastType(childId)   // read last record (to avoid duplication)
            scanLoop()
        }

        return START_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun onStableDecision(childId: String, name: String, surname: String, newType: String) {
        if (newType == lastType) {
            Log.d("ForegroundService", "$newType record skipped") //already present in the DB
            return
        }

        serviceScope.launch {
            try {
                Log.d("ForegroundService", "$newType record added to the database")
                repo.addAttendance(childId, newType)
                lastType = newType

                Notifications.ensureChannel(this@ForegroundWifiScanService)
                startForeground(
                    Notifications.NOTIF_ID,
                    Notifications.foregroundNotification(this@ForegroundWifiScanService, newType)
                )
            } catch (_: Exception) {
                startForeground(
                    Notifications.NOTIF_ID,
                    Notifications.foregroundNotification(this@ForegroundWifiScanService, "DB Error")
                )
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private suspend fun scanLoop() {
        while (true) {
            val wifiEnabled = wifiManager.isWifiEnabled
            if (!wifiEnabled) {
                notifyWifiState(wifiEnabled) //Notify MainActivity
                Log.w("ForegroundService", "Wi-Fi Off!")
                startForeground(
                    Notifications.NOTIF_ID,
                    Notifications.foregroundNotification(this, "Wi-Fi disabled!")
                )
                delay(10_000)
                continue
            }
            notifyWifiState(wifiEnabled) //Notify MainActivity
            try {
                Log.i("ForegroundWifiScanService", "Wi-Fi Scan Request")
                @Suppress("DEPRECATION")
                wifiManager.startScan()
            } catch (_: SecurityException) {
                startForeground(
                    Notifications.NOTIF_ID,
                    Notifications.foregroundNotification(this, "Wi-Fi/Location permissions missing")
                )
            }
            delay(5000)
        }
    }

    override fun onDestroy() {
        Log.i("ForegroundWifiScanService", "Service onDestroy")
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
        serviceScope.cancel()
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("ForegroundWifiScanService", "onTaskRemoved: stopping service")
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }




    private fun notifyWifiState(enabled: Boolean) {
        val intent = Intent("com.example.childapp.WIFI_STATE_CHANGED") // event send
        intent.putExtra("wifiEnabled", enabled)
        sendBroadcast(intent)
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
