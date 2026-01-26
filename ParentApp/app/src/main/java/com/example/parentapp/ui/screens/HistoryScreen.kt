package com.example.parentapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.ui.MyAppViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    childId: String,
    vm: MyAppViewModel,
    onBack: () -> Unit
) {
    val state = vm.historyUiState.collectAsState().value
    val listState = rememberLazyListState()

    // Reload every time open the screen (or change childId)
    LaunchedEffect(childId) {
        vm.refreshHistory(childId)
    }

    // Infinite scroll
    LaunchedEffect(listState.firstVisibleItemIndex, state.items.size, state.endReached, state.isLoadingMore) {
        if (!state.endReached && !state.isLoadingMore) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            if (total > 0 && lastVisible >= total - 5) {
                vm.loadMoreHistory(childId)
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshHistory(childId) }, enabled = !state.isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (!state.isRefreshing && state.items.isEmpty()) {
                Text("No records available")
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.items) { _, item ->
                    HistoryRow(item)
                }

                item {
                    if (state.isLoadingMore) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    } else if (state.endReached) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "End of list",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HistoryRow(record: AttendanceRecord) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDateTime(record.time))
            val typeLabel = if (record.isEntry) "Entry" else "Exit"
            val typeColor = if (record.isEntry) com.example.parentapp.ui.theme.Success else com.example.parentapp.ui.theme.ErrorRed
            Text(typeLabel, color = typeColor)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
