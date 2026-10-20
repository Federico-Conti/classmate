package com.example.parentapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parentapp.ui.components.ChildSelector
import com.example.parentapp.ui.components.StatusIndicator
import com.example.parentapp.ui.theme.*
import com.example.parentapp.ui.ParentViewModel
import com.example.parentapp.ui.UiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: ParentViewModel,
    onOpenHistory: (childId: String) -> Unit,
    onLogout: () -> Unit,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val parent = vm.parent.collectAsState().value
    val childrenState = vm.childrenUiState.collectAsState().value
    val selectedChild = vm.selectedChild.collectAsState().value
    val status = vm.childPresenceStatus.collectAsState().value
    val confirmed = vm.confirmedAttendance.collectAsState().value

    var showLogoutDialog by remember { mutableStateOf(false) }

    val displayName = parent?.fullName?.takeIf { it.isNotBlank() }
        ?: parent?.id?.takeIf { it.isNotBlank() }
        ?: "Home"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") },
                    selected = currentRoute.startsWith("history"),
                    onClick = {
                        val childId = selectedChild?.id
                        if (childId != null) {
                            onOpenHistory(childId)
                        }
                    },
                    enabled = selectedChild != null
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimensions.spacingLarge)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingLarge)
        ) {
            when (childrenState) {
                is UiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is UiState.Error -> {
                    Text(
                        text = childrenState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is UiState.Data -> {
                    if (childrenState.value.isEmpty()) {
                        Text(
                            text = "No child found, contact the school.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        ChildSelector(
                            children = childrenState.value,
                            selected = selectedChild,
                            onSelect = { vm.selectChild(it.id) }
                        )
                    }
                }
                else -> Unit
            }

            StatusIndicator(status = status)
            vm.realtimeConfigurationError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (confirmed != null) {
                Text(
                    text = "Last confirmed update: ${formatDateTime(confirmed.time)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            userName = displayName
        )
    }
}

@Composable
private fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    userName: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Do you want to log out of your account?",
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Disconnect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
