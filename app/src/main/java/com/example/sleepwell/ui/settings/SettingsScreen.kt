package com.example.sleepwell.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sleepwell.ui.components.DurationPickerDialog
import com.example.sleepwell.ui.components.TimePickerDialog
import com.example.sleepwell.utils.BypassManager
import com.example.sleepwell.utils.BypassMethod
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAppSelection: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Alarm Time Setting
            item {
                SettingCard(
                    title = "Alarm Time",
                    subtitle = formatTime(uiState.alarmSettings.alarmHour, uiState.alarmSettings.alarmMinute),
                    icon = Icons.Default.Schedule,
                    onClick = { viewModel.showTimePicker() }
                )
            }

            // Lockout Duration Setting
            item {
                SettingCard(
                    title = "Lockout Duration",
                    subtitle = "${uiState.alarmSettings.lockoutDurationMinutes} minutes",
                    icon = Icons.Default.Timer,
                    onClick = { viewModel.showDurationPicker() }
                )
            }

            // App Selection Setting
            item {
                SettingCard(
                    title = "Select Apps to Lock",
                    subtitle = "${uiState.alarmSettings.selectedApps.size} apps selected",
                    icon = Icons.Default.Apps,
                    onClick = onNavigateToAppSelection
                )
            }

            // Bypass Method Setting
            item {
                SettingCard(
                    title = "Bypass Method",
                    subtitle = BypassManager.getBypassMethodDisplayName(uiState.alarmSettings.bypassMethod),
                    icon = Icons.Default.Lock,
                    onClick = { viewModel.showBypassMethodPicker() }
                )
            }
        }
    }

    // Time Picker Dialog (using new wheel picker)
    if (uiState.showTimePicker) {
        TimePickerDialog(
            initialHour = uiState.alarmSettings.alarmHour,
            initialMinute = uiState.alarmSettings.alarmMinute,
            onTimeSelected = { hour, minute ->
                viewModel.updateAlarmTime(hour, minute)
                viewModel.hideTimePicker()
            },
            onDismiss = { viewModel.hideTimePicker() }
        )
    }

    // Duration Picker Dialog (using new wheel picker)
    if (uiState.showDurationPicker) {
        DurationPickerDialog(
            initialDurationMinutes = uiState.alarmSettings.lockoutDurationMinutes,
            onDurationSelected = { duration ->
                viewModel.updateLockoutDuration(duration)
                viewModel.hideDurationPicker()
            },
            onDismiss = { viewModel.hideDurationPicker() }
        )
    }

    // Bypass Method Picker Dialog
    if (uiState.showBypassMethodPicker) {
        BypassMethodPickerDialog(
            currentMethod = uiState.alarmSettings.bypassMethod,
            onMethodSelected = { method ->
                viewModel.updateBypassMethod(method)
                viewModel.hideBypassMethodPicker()
            },
            onDismiss = { viewModel.hideBypassMethodPicker() }
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BypassMethodPickerDialog(
    currentMethod: BypassMethod,
    onMethodSelected: (BypassMethod) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        BypassMethod.NONE,
        BypassMethod.MATH,
        BypassMethod.STRING_MATCH
    )
    var selectedMethod by remember { mutableStateOf(currentMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Bypass Method",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(methods.size) { index ->
                    val method = methods[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMethod = method }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = BypassManager.getBypassMethodDisplayName(method),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = BypassManager.getBypassMethodDescription(method),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (index < methods.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onMethodSelected(selectedMethod) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
    return format.format(calendar.time)
}