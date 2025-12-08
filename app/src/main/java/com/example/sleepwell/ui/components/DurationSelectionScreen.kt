package com.example.sleepwell.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog for selecting duration using iOS-style wheel pickers
 * Displays two columns: Hours (0-23) and Minutes (0-59)
 * Default: 10 minutes
 * Range: 0h 1m to 23h 59m
 *
 * @param initialDurationMinutes Initial duration in total minutes
 * @param onDurationSelected Callback when duration is confirmed (total minutes)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun DurationPickerDialog(
    initialDurationMinutes: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert initial duration to hours and minutes
    val initialHours = initialDurationMinutes / 60
    val initialMinutes = initialDurationMinutes % 60

    var selectedHours by remember { mutableStateOf(initialHours) }
    var selectedMinutes by remember { mutableStateOf(initialMinutes) }

    // Generate lists for pickers
    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Lockout Duration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display current selection
                Text(
                    text = formatDuration(selectedHours, selectedMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Picker
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WheelPicker(
                            items = hours,
                            initialIndex = initialHours,
                            onItemSelected = { index ->
                                selectedHours = hours[index]
                            },
                            modifier = Modifier.fillMaxWidth(),
                            itemHeight = 50,
                            visibleItemsCount = 5,
                            textStyle = { item, isSelected ->
                                Text(
                                    text = item.toString().padStart(2, '0'),
                                    fontSize = if (isSelected) 28.sp else 20.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                        Text(
                            text = "hours",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Minutes Picker
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WheelPicker(
                            items = minutes,
                            initialIndex = initialMinutes,
                            onItemSelected = { index ->
                                selectedMinutes = minutes[index]
                            },
                            modifier = Modifier.fillMaxWidth(),
                            itemHeight = 50,
                            visibleItemsCount = 5,
                            textStyle = { item, isSelected ->
                                Text(
                                    text = item.toString().padStart(2, '0'),
                                    fontSize = if (isSelected) 28.sp else 20.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                        Text(
                            text = "minutes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Validation message
                if (selectedHours == 0 && selectedMinutes == 0) {
                    Text(
                        text = "Duration must be at least 1 minute",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            val totalMinutes = (selectedHours * 60) + selectedMinutes
            val isValid = totalMinutes > 0

            TextButton(
                onClick = {
                    if (isValid) {
                        onDurationSelected(totalMinutes)
                    }
                },
                enabled = isValid
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

/**
 * Format duration for display
 */
private fun formatDuration(hours: Int, minutes: Int): String {
    return when {
        hours == 0 && minutes == 0 -> "No duration"
        hours == 0 -> "$minutes minute${if (minutes != 1) "s" else ""}"
        minutes == 0 -> "$hours hour${if (hours != 1) "s" else ""}"
        else -> "$hours hour${if (hours != 1) "s" else ""} $minutes minute${if (minutes != 1) "s" else ""}"
    }
}
