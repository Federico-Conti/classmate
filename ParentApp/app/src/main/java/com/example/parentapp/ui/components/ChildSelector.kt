package com.example.parentapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.parentapp.model.Child

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSelector(
    children: List<Child>,
    selected: Child?,
    onSelect: (Child) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.fullName ?: "",
            onValueChange = {},
            label = { Text("Select child") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            children.forEach { child ->
                DropdownMenuItem(
                    text = { Text(child.fullName) },
                    onClick = {
                        expanded = false
                        onSelect(child)
                    }
                )
            }
        }
    }
}
