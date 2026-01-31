package com.example.parentapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parentapp.model.Parent
import com.example.parentapp.ui.theme.AppDimensions
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "ParentApp",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(AppDimensions.spacingXLarge)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Surface(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "🏫",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            Spacer(Modifier.height(AppDimensions.spacingXXLarge))

            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(AppDimensions.spacingSmall))

            Text(
                text = "Enter the username provided by the school",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppDimensions.spacingXXLarge))

            OutlinedTextField(
                value = parentIdInput,
                onValueChange = onParentIdChange,
                label = { Text("username") },
                placeholder = { Text("") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(),
                enabled = uiState !is UiState.Loading
            )

            Spacer(Modifier.height(AppDimensions.spacingLarge))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is UiState.Loading && parentIdInput.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        strokeWidth = AppDimensions.progressIndicatorStroke,
                        modifier = Modifier.size(AppDimensions.iconSmall),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(AppDimensions.spacingSmall))
                    Text(
                        text = "Login ...",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState is UiState.Error) {
                Spacer(Modifier.height(AppDimensions.spacingLarge))

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(AppDimensions.spacingMedium),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}