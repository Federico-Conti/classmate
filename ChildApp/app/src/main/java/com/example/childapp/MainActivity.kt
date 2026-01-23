package com.example.childapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.childapp.service.ForegroundWifiScanService
import com.example.childapp.util.Permissions

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val state by vm.uiState.collectAsState()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val allGranted = result.values.all { it }
                    vm.onPermissionsResult(allGranted)
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Permissions.requiredPermissions())
                }

                //for WIFI-off event send by ForegroundWifiScanService.notifyWifiState
                val context = LocalContext.current
                DisposableEffect(Unit) {
                    val receiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                            val wifiEnabled = intent.getBooleanExtra("wifiEnabled", false)
                            vm.setWifiEnabled(wifiEnabled) // if received event changes uistate
                        }
                    }
                    // event log to listen to
                    val filter = android.content.IntentFilter("com.example.childapp.WIFI_STATE_CHANGED")
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose { context.unregisterReceiver(receiver) }
                }

                //check uistate
                if (!state.wifiEnabled) {
                    Text(
                        text = "Wifi OFF",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else{
                    Text(
                        text = "Wifi ON",
                        color = Color.Green,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message)

                    Button(
                        enabled = state.permissionsGranted && !state.serviceRunning,
                        onClick = {
                            val i = Intent(this@MainActivity, ForegroundWifiScanService::class.java).apply {
                                putExtra(ForegroundWifiScanService.EXTRA_CHILD_ID, "no7Cldaej69rPhDl50Gz")
                                putExtra(ForegroundWifiScanService.EXTRA_CHILD_NAME, "Gino")
                                putExtra(ForegroundWifiScanService.EXTRA_CHILD_SURNAME, "Paoli")
                                putExtra(ForegroundWifiScanService.EXTRA_TARGET_SSID, "Home&Life SuperWiFi-B06F")
                                putExtra(ForegroundWifiScanService.EXTRA_TARGET_BSSID, "bc:cf:4f:11:b0:6f")
                            }
                            ContextCompat.startForegroundService(this@MainActivity, i)
                            vm.setServiceRunning(true)
                        }
                    ) { Text("Start scan") }

                    Button(
                        enabled = state.serviceRunning,
                        onClick = {
                            stopService(Intent(this@MainActivity, ForegroundWifiScanService::class.java))
                            vm.setServiceRunning(false)
                        }
                    ) { Text("Stop scan") }
                }



            }
        }
    }
}
