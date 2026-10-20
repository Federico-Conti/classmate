package com.example.parentapp.ui.screens

import android.os.Build
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.parentapp.model.AttendanceRecord
import com.example.parentapp.ui.ParentViewModel
import com.example.parentapp.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    childId: String,
    vm: ParentViewModel,
    onBack: () -> Unit
) {
    val state = vm.historyUiState.collectAsState().value
    val selectedChildId = vm.selectedChildId.collectAsState().value
    val listState = rememberLazyListState() //monitors the scroll position of the LazyColumn
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(childId, lifecycleOwner, context) {
        vm.openHistory(childId)
        var wasStopped = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> wasStopped = true
                Lifecycle.Event.ON_START -> if (wasStopped) {
                    wasStopped = false
                    vm.refreshOpenHistory()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val hadNetwork = AtomicBoolean(connectivity.activeNetwork != null)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                hadNetwork.set(false)
            }

            override fun onAvailable(network: Network) {
                if (!hadNetwork.getAndSet(true) &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    vm.refreshOpenHistory()
                }
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose {
            connectivity.unregisterNetworkCallback(callback)
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.closeHistory(childId)
        }
    }

    LaunchedEffect(childId, selectedChildId) {
        if (selectedChildId != null && selectedChildId != childId) onBack()
    }

    LaunchedEffect(listState.firstVisibleItemIndex, state.items.size, state.endReached, state.isLoadingMore, state.error) {
        if (!state.endReached && !state.isLoadingMore && !state.isRefreshing && state.error == null) {
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
                title = { Text("History") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (state.error != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimensions.spacingLarge),
                        color = ErrorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = state.error,
                            color = OnErrorContainer,
                            modifier = Modifier.padding(AppDimensions.spacingMedium),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }


                if (!state.isRefreshing && state.items.isEmpty() && state.error == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingSmall)
                        ) {
                            Text(
                                text = "📋",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = "No attendance recorded",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    return@Column
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppDimensions.spacingLarge,
                        end = AppDimensions.spacingLarge,
                        top = AppDimensions.spacingMedium,
                        bottom = AppDimensions.spacingLarge
                    )
                ) {
                    itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                        TimelineEventItem(
                            record = item,
                            isFirst = index == 0,
                            isLast = index == state.items.lastIndex && state.endReached
                        )
                    }

                    item {
                        if (state.isLoadingMore) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppDimensions.spacingLarge),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(AppDimensions.progressIndicatorSize),
                                    strokeWidth = AppDimensions.progressIndicatorStroke
                                )
                            }
                        } else if (state.endReached && state.items.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppDimensions.spacingLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "End of list",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimelineEventItem(
    record: AttendanceRecord,
    isFirst: Boolean,
    isLast: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        TimelineIndicator(
            isEntry = record.isEntry,
            isFirst = isFirst,
            isLast = isLast
        )

        Spacer(modifier = Modifier.width(AppDimensions.spacingLarge))


        Box(modifier = Modifier.padding(bottom = AppDimensions.timelineItemSpacing)) {
            EventCard(record = record, isFirst = isFirst)
        }
    }
}
@Composable
private fun TimelineIndicator(
    isEntry: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight()
            .width(40.dp)
    ) {

        Box(
            modifier = Modifier
                .width(AppDimensions.timelineLineWidth)
                .weight(if (isFirst) 0.0001f else 0.2f)
                .background(if (isFirst) Color.Transparent else OutlineVariant)
        )


        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isFirst) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isEntry) EntryColor else ExitColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEntry) Icons.Default.Login else Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(AppDimensions.timelineCircleSize)
                        .clip(CircleShape)
                        .background(OutlineVariant)
                )
            }
        }


        Box(
            modifier = Modifier
                .width(AppDimensions.timelineLineWidth)
                .weight(1f)
                .background(if (isLast) Color.Transparent else OutlineVariant)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EventCard(
    record: AttendanceRecord,
    isFirst: Boolean
) {
    val backgroundColor = if (record.isEntry) EntryBackground else ExitBackground
    val textColor = if (record.isEntry) EntryColor else ExitColor
    val eventType = if (record.isEntry) "ENTRY" else "EXIT"
    val icon = if (record.isEntry) Icons.AutoMirrored.Filled.Login else Icons.Default.Logout

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFirst) AppDimensions.cardElevation else AppDimensions.historyCardElevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.historyCardPadding)
        ) {
            // Header row with date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Event type badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Leading icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.spacingMedium))

                    // Event type text
                    Text(
                        text = eventType,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                // Date timestamp
                Text(
                    text = formatDate(record.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.spacingSmall))

            // Time and location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 48.dp)
            ) {
                Text(
                    text = formatTime(record.time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface
                )

            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
