package com.example.childapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.childapp.service.ForegroundWifiScanService
import com.example.childapp.ui.theme.ChildappTheme
import com.example.childapp.util.Permissions

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChildappTheme {
                val state by vm.uiState.collectAsState()
                val context = LocalContext.current

                // Launcher per la richiesta dei permessi
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val allGranted = result.values.all { it }
                    vm.onPermissionsResult(allGranted)
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Permissions.requiredPermissions())
                }

                // for WIFI-off event send by ForegroundWifiScanService.notifyWifiState
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val wifiEnabled = intent.getBooleanExtra("wifiEnabled", false)
                            vm.setWifiEnabled(wifiEnabled) // if received event changes uistate
                        }
                    }
                    // event log to listen to
                    val filter = IntentFilter("com.example.childapp.WIFI_STATE_CHANGED")
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    onDispose { context.unregisterReceiver(receiver) }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("ChildApp Monitor") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Card di stato Connessione/Wifi
                        WifiStatusCard(wifiEnabled = state.wifiEnabled)

                        // Card principale dei controlli
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Stato Servizio",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bottone Start
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = state.permissionsGranted && !state.serviceRunning,
                                    onClick = {
                                        val i = Intent(context, ForegroundWifiScanService::class.java).apply {
                                            putExtra(ForegroundWifiScanService.EXTRA_CHILD_ID, "no7Cldaej69rPhDl50Gz")
                                            putExtra(ForegroundWifiScanService.EXTRA_CHILD_NAME, "Gino")
                                            putExtra(ForegroundWifiScanService.EXTRA_CHILD_SURNAME, "Paoli")
                                            putExtra(ForegroundWifiScanService.EXTRA_TARGET_SSID, "Home&Life SuperWiFi-B06F")
                                            putExtra(ForegroundWifiScanService.EXTRA_TARGET_BSSID, "bc:cf:4f:11:b0:6f")
                                        }
                                        ContextCompat.startForegroundService(context, i)
                                        vm.setServiceRunning(true)
                                    }
                                ) {
                                    Text("ATTIVA SCANSIONE")
                                }

                                // Bottone Stop
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = state.serviceRunning,
                                    onClick = {
                                        stopService(Intent(context, ForegroundWifiScanService::class.java))
                                        vm.setServiceRunning(false)
                                    }
                                ) {
                                    Text("INTERROMPI")
                                }
                            }
                        }

                        if (!state.permissionsGranted) {
                            Text(
                                text = "Permessi necessari non concessi",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WifiStatusCard(wifiEnabled: Boolean) {
    val containerColor = if (wifiEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (wifiEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
    val icon = if (wifiEnabled) Icons.Default.Info else Icons.Default.Warning
    val statusText = if (wifiEnabled) "WI-FI ATTIVO" else "WI-FI DISATTIVATO"

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}