package com.example.parentapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.ui.MyAppViewModel
import com.example.parentapp.ui.UiState
import com.example.parentapp.ui.components.ChildSelector
import com.example.parentapp.ui.components.CustomCard
import com.example.parentapp.ui.components.StatusIndicator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    vm: MyAppViewModel,
    onOpenHistory: (childId: String) -> Unit,
    onLogout: () -> Unit
) {
    val parent = vm.parent.collectAsState().value
    val childrenState = vm.childrenUiState.collectAsState().value
    val selectedChild = vm.selectedChild.collectAsState().value
    val status = vm.childPresenceStatus.collectAsState().value
    val last5 = vm.last5Records.collectAsState().value
    val displayName = parent?.fullName?.takeIf { it.isNotBlank() }
        ?: parent?.id?.takeIf { it.isNotBlank() }
        ?: "Home"


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName) },
                actions = {
                    IconButton(
                        onClick = {
                            val id = selectedChild?.id ?: return@IconButton
                            onOpenHistory(id)
                        },
                        enabled = selectedChild != null
                    ) { Icon(Icons.Default.History, contentDescription = "History") }

                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (childrenState) {
                is UiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is UiState.Error -> {
                    Text(childrenState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Data -> {
                    if (childrenState.value.isEmpty()) {
                        Text("No children found, contact the school.")
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

            CustomCard {
                StatusIndicator(status = status)
            }

            CustomCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Last updates", style = MaterialTheme.typography.titleSmall)

                    when (last5) {
                        is UiState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is UiState.Error -> {
                            Text(last5.message, color = MaterialTheme.colorScheme.error)
                        }
                        is UiState.Data -> {
                            val list = last5.value
                            if (list.isEmpty()) {
                                Text(
                                    "No attendance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LastRecordsTable(list)
                            }
                        }
                        else -> Text("Select a child to view records")
                    }
                }
            }

        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun LastRecordsTable(records: List<AttendanceRecord>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        userScrollEnabled = false
    ) {
        item {
            // HEADER
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Date",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "Type",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        items(records) { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDateTime(r.time))
                val typeLabel = if (r.isEntry) "Entry" else "Exit"
                val typeColor = if (r.isEntry) com.example.parentapp.ui.theme.Success else com.example.parentapp.ui.theme.ErrorRed
                Text(typeLabel, color = typeColor)
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
