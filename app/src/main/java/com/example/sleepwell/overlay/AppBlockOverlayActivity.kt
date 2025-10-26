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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.sleepwell.data.repository.SleepWellRepository
import com.example.sleepwell.ui.theme.SleepWellTheme
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
        repository = SleepWellRepository(this)

        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""

        setContent {
            SleepWellTheme {
                AppBlockOverlay(
                    blockedPackage = blockedPackage,
                    repository = repository,
                    onTimeExpired = { finish() }
                )
            }
        }

        // Start checking if lockout period has ended
        startPeriodicCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicCheck()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button from closing the overlay
        // Do nothing - this is intentional to prevent bypassing
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // When user tries to leave (home button, recent apps), bring overlay back to front
        moveTaskToFront()
    }

    private fun moveTaskToFront() {
        val blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        val newIntent = Intent(this, AppBlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                   Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                   Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("blocked_package", blockedPackage)
        }
        startActivity(newIntent)
    }

    private fun startPeriodicCheck() {
        checkRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    try {
                        val lockState = repository.appLockState.first()
                        if (!lockState.isActive || System.currentTimeMillis() >= lockState.endTime) {
                            finish()
                            return@launch
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                handler.postDelayed(this, 1000) // Check every second
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
    onTimeExpired: () -> Unit
) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    var unlockTime by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf("") }

    // Load app info
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
        } catch (e: Exception) {
            appName = "Unknown App"
            e.printStackTrace()
        }
    }

    // Update time remaining
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val lockState = repository.appLockState.first()
                if (!lockState.isActive || System.currentTimeMillis() >= lockState.endTime) {
                    onTimeExpired()
                    break
                }

                val remainingMs = lockState.endTime - System.currentTimeMillis()
                val remainingMinutes = (remainingMs / (1000 * 60)).toInt()
                val hours = remainingMinutes / 60
                val minutes = remainingMinutes % 60

                timeRemaining = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }

                kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                e.printStackTrace()
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
            }
        }
    }
}