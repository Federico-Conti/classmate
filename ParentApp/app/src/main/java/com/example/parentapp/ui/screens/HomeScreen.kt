package com.example.parentapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.ui.components.ChildSelector
import com.example.parentapp.ui.components.CustomCard
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
    val last5 = vm.last5Records.collectAsState().value

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

            CustomCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingMedium)
                ) {
                    Text(
                        text = "Latest status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    when (last5) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(AppDimensions.iconLarge)
                                )
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = last5.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is UiState.Data -> {
                            val list = last5.value
                            if (list.isEmpty()) {
                                Text(
                                    text = "No attendances",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LastRecordsTable(list)
                            }
                        }
                        else -> Text(
                            text = "Select a child to view attendance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
@Composable
private fun LastRecordsTable(records: List<AttendanceRecord>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingSmall)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.spacingXSmall),
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingSmall),
            userScrollEnabled = true
        ) {
            items(records) { record ->
                AttendanceRow(record)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AttendanceRow(record: AttendanceRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.spacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateTime(record.time),
            style = MaterialTheme.typography.bodyMedium
        )

        Surface(
            color = if (record.isEntry) EntryBackground else ExitBackground,
            shape = MaterialTheme.shapes.small,
            contentColor = if (record.isEntry) EntryColor else ExitColor
        ) {
            Text(
                text = if (record.isEntry) "Entry" else "Exit",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(
                    horizontal = AppDimensions.spacingMedium,
                    vertical = AppDimensions.spacingXSmall
                )
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}