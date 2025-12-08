package com.example.sleepwell.overlay

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.sleepwell.data.models.AlarmSettings
import com.example.sleepwell.data.models.AppLockState
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.ui.theme.SleepWellTheme
import com.example.sleepwell.utils.BypassManager
import com.example.sleepwell.utils.BypassMethod
import com.example.sleepwell.utils.MathChallenge
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppBlockOverlayActivity : ComponentActivity() {

    private lateinit var repository: SleepWellRepository
    private val handler = Handler(Looper.getMainLooper())
    private var checkRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("SleepWell", "Overlay onCreate called")

        // Basic window setup to ensure overlay appears on top
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        repository = SleepWellRepository(this)

        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        android.util.Log.d("SleepWell", "Overlay blocking package: $blockedPackage")

        // Check lock state immediately on creation
        lifecycleScope.launch {
            try {
                val lockState = repository.appLockState.first()
                android.util.Log.d("SleepWell", "Overlay onCreate - Lock state: active=${lockState.isActive}, currentTime=${System.currentTimeMillis()}, endTime=${lockState.endTime}, lockedApps=${lockState.lockedApps}")
            } catch (e: Exception) {
                android.util.Log.e("SleepWell", "Error reading lock state in onCreate", e)
            }
        }

        setContent {
            SleepWellTheme {
                AppBlockOverlay(
                    blockedPackage = blockedPackage,
                    repository = repository,
                    onTimeExpired = {
                        android.util.Log.d("SleepWell", "Overlay onTimeExpired callback triggered - finishing")
                        finish()
                    },
                    onBypassSuccess = {
                        android.util.Log.d("SleepWell", "Bypass successful - unlocking apps")
                        // Clear the lock state so monitoring service doesn't re-launch overlay
                        lifecycleScope.launch {
                            repository.updateAppLockState(
                                AppLockState(
                                    isActive = false,
                                    startTime = 0L,
                                    endTime = 0L,
                                    lockedApps = emptySet()
                                )
                            )
                            finish()
                        }
                    }
                )
            }
        }

        // Start periodic checks after a delay to let everything settle
        handler.postDelayed({
            android.util.Log.d("SleepWell", "Starting periodic checks after delay")
            startPeriodicCheck()
        }, 3000) // Wait 3 seconds before starting periodic checks
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("SleepWell", "Overlay onResume called")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("SleepWell", "Overlay onPause called")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.d("SleepWell", "Overlay onStop called")
    }

    override fun onDestroy() {
        android.util.Log.d("SleepWell", "Overlay onDestroy called")
        super.onDestroy()
        stopPeriodicCheck()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button from closing the overlay
        // Do nothing - this is intentional to prevent bypassing
        android.util.Log.d("SleepWell", "Overlay onBackPressed - ignoring")
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        android.util.Log.d("SleepWell", "Overlay onUserLeaveHint called")
        // Allow user to leave to home screen - this is expected behavior
    }

    private fun startPeriodicCheck() {
        checkRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    try {
                        val lockState = repository.appLockState.first()
                        android.util.Log.d("SleepWell", "Overlay checking lock state: active=${lockState.isActive}, currentTime=${System.currentTimeMillis()}, endTime=${lockState.endTime}")

                        if (!lockState.isActive || System.currentTimeMillis() >= lockState.endTime) {
                            android.util.Log.d("SleepWell", "Overlay finishing due to expired lock")
                            finish()
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SleepWell", "Error in overlay periodic check", e)
                        e.printStackTrace()
                    }
                }

                handler.postDelayed(this, 5000) // Check every 5 seconds (less aggressive)
            }
        }
        handler.post(checkRunnable!!)
    }

    private fun stopPeriodicCheck() {
        checkRunnable?.let { handler.removeCallbacks(it) }
        checkRunnable = null
    }
}

@Composable
fun AppBlockOverlay(
    blockedPackage: String,
    repository: SleepWellRepository,
    onTimeExpired: () -> Unit,
    onBypassSuccess: () -> Unit
) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    var unlockTime by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf("") }

    // Bypass state
    var showBypassDialog by remember { mutableStateOf(false) }
    var bypassMethod by remember { mutableStateOf(BypassMethod.NONE) }

    // Load app info and bypass settings
    LaunchedEffect(blockedPackage) {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(blockedPackage, 0)
            appName = packageManager.getApplicationLabel(appInfo).toString()
            appIcon = packageManager.getApplicationIcon(blockedPackage)

            val lockState = repository.appLockState.first()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = lockState.endTime
            }
            val format = SimpleDateFormat("h:mm a 'on' MMM dd", Locale.getDefault())
            unlockTime = format.format(calendar.time)

            // Load bypass method
            val alarmSettings = repository.alarmSettings.first()
            bypassMethod = alarmSettings.bypassMethod
        } catch (e: Exception) {
            appName = "Unknown App"
            e.printStackTrace()
        }
    }

    // Track lock state and update time remaining
    val lockState by repository.appLockState.collectAsState(initial = null)

    // Update time remaining every second - only when lock state is loaded
    LaunchedEffect(lockState) {
        val currentLockState = lockState
        if (currentLockState == null) return@LaunchedEffect // Wait for real lock state to load

        android.util.Log.d("SleepWell", "AppBlockOverlay LaunchedEffect - lockState loaded: active=${currentLockState.isActive}, endTime=${currentLockState.endTime}")

        if (!currentLockState.isActive) {
            android.util.Log.d("SleepWell", "AppBlockOverlay - lock not active, calling onTimeExpired")
            onTimeExpired()
            return@LaunchedEffect
        }

        while (currentLockState.isActive) {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime >= currentLockState.endTime) {
                    android.util.Log.d("SleepWell", "AppBlockOverlay - time expired, calling onTimeExpired")
                    onTimeExpired()
                    break
                }

                val remainingMs = currentLockState.endTime - currentTime
                val remainingSeconds = (remainingMs / 1000).toInt()
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val seconds = remainingSeconds % 60

                timeRemaining = when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m ${seconds}s"
                    else -> "${seconds}s"
                }

                kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                android.util.Log.e("SleepWell", "Error in AppBlockOverlay time update", e)
                break
            }
        }
    }

    // Full screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App icon and name
                if (appIcon != null) {
                    val bitmap = remember(appIcon) {
                        try {
                            appIcon!!.toBitmap(64, 64)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        // Fallback if icon fails to load
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This app is locked by SleepWell",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Time information
                Text(
                    text = "Unlocks at:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = unlockTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (timeRemaining.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "($timeRemaining remaining)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Use this time for productive activities like reading, exercising, or planning your day.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Bypass button (only show if bypass method is not NONE)
                if (bypassMethod != BypassMethod.NONE) {
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { showBypassDialog = true },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock Early")
                    }

                    Text(
                        text = when (bypassMethod) {
                            BypassMethod.MATH -> "Solve a math problem to unlock"
                            BypassMethod.STRING_MATCH -> "Type the code to unlock"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    // Bypass dialog
    if (showBypassDialog) {
        when (bypassMethod) {
            BypassMethod.MATH -> {
                MathChallengeDialog(
                    onSuccess = {
                        showBypassDialog = false
                        onBypassSuccess()
                    },
                    onDismiss = { showBypassDialog = false }
                )
            }
            BypassMethod.STRING_MATCH -> {
                StringMatchDialog(
                    onSuccess = {
                        showBypassDialog = false
                        onBypassSuccess()
                    },
                    onDismiss = { showBypassDialog = false }
                )
            }
            else -> {
                // Should not happen
                showBypassDialog = false
            }
        }
    }
}

@Composable
private fun MathChallengeDialog(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentChallenge by remember { mutableStateOf(BypassManager.generateMathChallenge()) }
    var userAnswer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Math Challenge",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Solve this problem to unlock:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Math problem
                Text(
                    text = currentChallenge.question,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Answer input
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = {
                        userAnswer = it
                        showError = false
                    },
                    label = { Text("Your Answer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Incorrect! Try the new problem.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (BypassManager.validateMathAnswer(currentChallenge, userAnswer)) {
                        onSuccess()
                    } else {
                        showError = true
                        userAnswer = ""
                        // Generate new challenge on failure
                        currentChallenge = BypassManager.generateMathChallenge()
                    }
                }
            ) {
                Text("Submit")
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
private fun StringMatchDialog(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val targetString = remember { BypassManager.generateRandomString(8) }
    var userInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "String Match Challenge",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Type this code exactly to unlock:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Target string display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = targetString,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        letterSpacing = 4.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Case-sensitive • No spaces",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User input
                OutlinedTextField(
                    value = userInput,
                    onValueChange = {
                        userInput = it
                        showError = false
                    },
                    label = { Text("Type the code") },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Incorrect! Please try again.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (BypassManager.validateStringMatch(targetString, userInput)) {
                        onSuccess()
                    } else {
                        showError = true
                        userInput = ""
                    }
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}