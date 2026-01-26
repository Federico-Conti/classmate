package com.example.parentapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parentapp.model.Parent
import com.example.parentapp.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: UiState<Parent>,
    parentIdInput: String,
    onParentIdChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onLoginSuccessNavigate: () -> Unit
) {
    LaunchedEffect(uiState) {
        if (uiState is UiState.Data) {
            onLoginSuccessNavigate()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Parent App") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("enter the username provided by the school", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = parentIdInput,
                onValueChange = onParentIdChange,
                label = { Text("Parent ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is UiState.Loading
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Signing in...")
                } else {
                    Text("Login")
                }
            }
            if (uiState is UiState.Error) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
