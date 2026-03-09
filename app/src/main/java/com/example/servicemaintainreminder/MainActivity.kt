package com.example.servicemaintainreminder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.servicemaintainreminder.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds
import androidx.work.*
import com.example.servicemaintainreminder.worker.ReminderWorker
import java.util.concurrent.TimeUnit
import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAuthenticatedSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Ads
        MobileAds.initialize(this) {}

        // Schedule Notification Worker
        setupReminderWorker()
    }

    override fun onResume() {
        super.onResume()
        checkAppLock()
    }

    override fun onStop() {
        super.onStop()
        // Reset authentication when app goes to background
        isAuthenticatedSession = false
    }

    private fun checkAppLock() {
        if (isAuthenticatedSession) return

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isLocked = prefs.getBoolean("is_app_lock_enabled", false)

        val overlay = binding.root.findViewById<android.view.View>(R.id.viewLockOverlay)
        if (!isLocked) {
            overlay?.visibility = android.view.View.GONE
            return
        }

        // Show lock overlay
        overlay?.visibility = android.view.View.VISIBLE

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticatedSession = true
                    overlay?.visibility = android.view.View.GONE
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        finishAffinity()
                    } else {
                        Toast.makeText(this@MainActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle("Please authenticate to unlock the app")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ServiceReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
