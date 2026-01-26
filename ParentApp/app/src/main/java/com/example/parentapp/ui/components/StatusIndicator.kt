package com.example.parentapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parentapp.model.PresenceStatus
import com.example.parentapp.ui.theme.*

@Composable
fun StatusIndicator(
    status: PresenceStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, statusText, icon) = when (status) {
        PresenceStatus.AtSchool -> StatusColors(
            StatusAtSchoolBg,
            StatusAtSchoolContent,
            "At school",
            "✓"
        )
        PresenceStatus.NotAtSchool -> StatusColors(
            StatusNotAtSchoolBg,
            StatusNotAtSchoolContent,
            "Not at school",
            "✕"
        )
        PresenceStatus.NoRecords -> StatusColors(
            StatusNoRecordsBg,
            StatusNoRecordsContent,
            "No attendance",
            "—"
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(AppDimensions.spacingLarge)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingLarge)
        ) {
            StatusBadge(
                icon = icon,
                backgroundColor = contentColor,
                contentColor = Color.White
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spacingXSmall)
            ) {
                Text(
                    text = "Current status",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    icon: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(3.dp, backgroundColor),
        modifier = Modifier.size(AppDimensions.statusBadgeSize)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium,
                color = backgroundColor
            )
        }
    }
}

private data class StatusColors(
    val background: Color,
    val content: Color,
    val text: String,
    val icon: String
)