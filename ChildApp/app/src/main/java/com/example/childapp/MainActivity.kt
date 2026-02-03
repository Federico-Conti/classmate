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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.childapp.service.ForegroundWifiScanService
import com.example.childapp.ui.theme.*
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

                // Permission launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val allGranted = result.values.all { it }
                    vm.onPermissionsResult(allGranted)
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Permissions.requiredPermissions())
                }

                // WiFi-off event from ForegroundWifiScanService
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val wifiEnabled = intent.getBooleanExtra("wifiEnabled", false)
                            vm.setWifiEnabled(wifiEnabled)
                        }
                    }
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
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "ChildApp",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(AppDimensions.spacingXLarge),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large WiFi icon at top
                        WifiStatusBadge(wifiEnabled = state.wifiEnabled)

                        // WiFi status card
                        WifiStatusCard(wifiEnabled = state.wifiEnabled)

                        Spacer(modifier = Modifier.height(AppDimensions.spacingSmall))

                        // Service status card
                        ServiceStatusCard(
                            serviceRunning = state.serviceRunning,
                            message = state.message
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Control buttons
                        ControlButtons(
                            permissionsGranted = state.permissionsGranted,
                            serviceRunning = state.serviceRunning,
                            onStartClick = {
                                val i = Intent(context, ForegroundWifiScanService::class.java).apply {
                                    putExtra(ForegroundWifiScanService.EXTRA_CHILD_ID, getString(R.string.config_child_id))
                                    putExtra(ForegroundWifiScanService.EXTRA_CHILD_NAME, getString(R.string.config_child_name))
                                    putExtra(ForegroundWifiScanService.EXTRA_CHILD_SURNAME, getString(R.string.config_child_surname))
                                    putExtra(ForegroundWifiScanService.EXTRA_TARGET_SSID, getString(R.string.config_target_ssid))
                                    putExtra(ForegroundWifiScanService.EXTRA_TARGET_BSSID, getString(R.string.config_target_bssid))

                                }
                                ContextCompat.startForegroundService(context, i) // onCreate() (if the service does not yet exist) && onStartCommand(intent, flags, startId)
                                vm.setServiceRunning(true)
                            },
                            onStopClick = {
                                stopService(Intent(context, ForegroundWifiScanService::class.java))
                                vm.setServiceRunning(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WifiStatusBadge(wifiEnabled: Boolean) {
    val borderColor = if (wifiEnabled) WifiActiveContent else WifiDisabledContent
    val iconColor = if (wifiEnabled) WifiActiveContent else WifiDisabledContent

    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(AppDimensions.statusBadgeBorderWidth, borderColor),
        modifier = Modifier.size(AppDimensions.statusBadgeSize)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (wifiEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = if (wifiEnabled) "WiFi On" else "WiFi Off",
                tint = iconColor,
                modifier = Modifier.size(AppDimensions.wifiIconSize)
            )
        }
    }
}

@Composable
fun WifiStatusCard(wifiEnabled: Boolean) {
    val backgroundColor = if (wifiEnabled) WifiActiveBg else WifiDisabledBg
    val contentColor = if (wifiEnabled) WifiActiveContent else WifiDisabledContent
    val statusText = if (wifiEnabled) "Wi-Fi On" else "Wi-Fi Off"
    val statusDescription = if (wifiEnabled)
        ""
    else
        "Turn on Wi-Fi to start scanning"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingSmall)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ServiceStatusCard(serviceRunning: Boolean, message: String) {
    val backgroundColor = ServiceStoppedBg
    val contentColor = ServiceStoppedContent

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AppDimensions.cardElevation
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingMedium)
        ) {
            Text(
                text = "Monitoring Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = backgroundColor,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppDimensions.spacingMedium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ControlButtons(
    permissionsGranted: Boolean,
    serviceRunning: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingMedium)
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.buttonHeight),
            enabled = permissionsGranted && !serviceRunning,
            onClick = onStartClick,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
            Text(
                text = "Start Monitoring",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimensions.buttonHeight),
            enabled = serviceRunning,
            onClick = onStopClick,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp,
                if (serviceRunning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppDimensions.spacingSmall))
            Text(
                text = "Stop Monitoring",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}