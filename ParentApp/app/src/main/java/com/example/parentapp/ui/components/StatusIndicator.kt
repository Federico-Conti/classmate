package com.example.parentapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentapp.ui.PresenceStatus

@Composable
fun StatusIndicator(
    status: PresenceStatus,
    modifier: Modifier = Modifier
) {
    val (symbol, text) = when (status) {
        PresenceStatus.AtSchool -> "✓" to "At school"
        PresenceStatus.NotAtSchool -> "✕" to "Not at school"
        PresenceStatus.NoRecords -> "—" to "No attendance records available"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusBadge(status = status)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusBadge(status: PresenceStatus) {
    val bg = when (status) {
        PresenceStatus.AtSchool -> com.example.parentapp.ui.theme.Success
        PresenceStatus.NotAtSchool -> com.example.parentapp.ui.theme.ErrorRed
        PresenceStatus.NoRecords -> com.example.parentapp.ui.theme.Warning
    }

    androidx.compose.material3.Surface(
        color = bg,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            val symbol = when (status) {
                PresenceStatus.AtSchool -> "✓"
                PresenceStatus.NotAtSchool -> "✕"
                PresenceStatus.NoRecords -> "—"
            }
            Text(text = symbol, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
