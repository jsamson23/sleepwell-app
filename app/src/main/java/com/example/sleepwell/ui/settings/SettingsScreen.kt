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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
        }
    }

    // Time Picker Dialog
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

    // Duration Picker Dialog
    if (uiState.showDurationPicker) {
        DurationPickerDialog(
            initialDuration = uiState.alarmSettings.lockoutDurationMinutes,
            onDurationSelected = { duration ->
                viewModel.updateLockoutDuration(duration)
                viewModel.hideDurationPicker()
            },
            onDismiss = { viewModel.hideDurationPicker() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Alarm Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                }
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

@Composable
private fun DurationPickerDialog(
    initialDuration: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val durations = listOf(15, 30, 45, 60, 90, 120, 180, 240)
    var selectedDuration by remember { mutableStateOf(initialDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Lockout Duration") },
        text = {
            LazyColumn {
                items(durations.size) { index ->
                    val duration = durations[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDuration = duration }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (duration < 60) {
                                "$duration minutes"
                            } else {
                                "${duration / 60} hour${if (duration / 60 > 1) "s" else ""}"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDurationSelected(selectedDuration) }
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