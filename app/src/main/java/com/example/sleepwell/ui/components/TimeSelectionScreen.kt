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
import java.util.*

/**
 * Dialog for selecting time using iOS-style wheel pickers
 * Displays three columns: Hour (1-12), Minute (00-59), and AM/PM
 *
 * @param initialHour Initial hour in 24-hour format (0-23)
 * @param initialMinute Initial minute (0-59)
 * @param onTimeSelected Callback when time is confirmed (hour in 24-hour format, minute)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert 24-hour to 12-hour format
    val is24HourAfternoon = initialHour >= 12
    val initial12Hour = when {
        initialHour == 0 -> 12
        initialHour > 12 -> initialHour - 12
        else -> initialHour
    }

    var selectedHour by remember { mutableStateOf(initial12Hour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var selectedAmPm by remember { mutableStateOf(if (is24HourAfternoon) 1 else 0) } // 0 = AM, 1 = PM

    // Generate lists for pickers
    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val amPmOptions = listOf("AM", "PM")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Alarm Time",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Picker
                InfiniteWheelPicker(
                    items = hours,
                    initialIndex = hours.indexOf(initial12Hour),
                    onItemSelected = { index ->
                        selectedHour = hours[index]
                    },
                    modifier = Modifier.weight(1f),
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
                    text = ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Minute Picker
                InfiniteWheelPicker(
                    items = minutes,
                    initialIndex = initialMinute,
                    onItemSelected = { index ->
                        selectedMinute = minutes[index]
                    },
                    modifier = Modifier.weight(1f),
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

                Spacer(modifier = Modifier.width(8.dp))

                // AM/PM Picker
                WheelPicker(
                    items = amPmOptions,
                    initialIndex = selectedAmPm,
                    onItemSelected = { index ->
                        selectedAmPm = index
                    },
                    modifier = Modifier.weight(0.8f),
                    itemHeight = 50,
                    visibleItemsCount = 5,
                    textStyle = { item, isSelected ->
                        Text(
                            text = item,
                            fontSize = if (isSelected) 24.sp else 18.sp,
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Convert back to 24-hour format
                    val hour24 = when {
                        selectedAmPm == 0 && selectedHour == 12 -> 0 // 12 AM = 0
                        selectedAmPm == 1 && selectedHour != 12 -> selectedHour + 12 // PM (not 12)
                        else -> selectedHour
                    }
                    onTimeSelected(hour24, selectedMinute)
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
