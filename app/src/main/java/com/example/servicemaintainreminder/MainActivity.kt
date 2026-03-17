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
        val savedPin = prefs.getString("app_custom_pin", "")
        val biometricEnabled = prefs.getBoolean("is_biometric_enabled", false)

        val overlay = binding.root.findViewById<android.widget.FrameLayout>(R.id.viewLockOverlay)
        if (!isLocked || savedPin.isNullOrEmpty()) {
            overlay?.visibility = android.view.View.GONE
            return
        }

        overlay?.visibility = android.view.View.VISIBLE
        overlay?.removeAllViews()

        // Inflate modern PIN numpad view
        val pinView = layoutInflater.inflate(R.layout.view_pin_lock, overlay, false)
        overlay?.addView(pinView)

        val dots = listOf(
            pinView.findViewById<android.view.View>(R.id.pinDot1),
            pinView.findViewById<android.view.View>(R.id.pinDot2),
            pinView.findViewById<android.view.View>(R.id.pinDot3),
            pinView.findViewById<android.view.View>(R.id.pinDot4),
        )
        val tvError = pinView.findViewById<android.widget.TextView>(R.id.tvPinError)
        val tvSubtitle = pinView.findViewById<android.widget.TextView>(R.id.tvPinSubtitle)
        val btnBiometric = pinView.findViewById<android.view.View>(R.id.btnBiometric)
        val spaceBottomLeft = pinView.findViewById<android.view.View>(R.id.spaceBottomLeft)

        var enteredPin = ""
        val brandColor = ContextCompat.getColor(this, R.color.brand_primary)
        val grayColor = android.graphics.Color.parseColor("#DDDDDD")

        fun dismissOverlay() {
            overlay?.animate()
                ?.alpha(0f)
                ?.scaleX(1.08f)
                ?.scaleY(1.08f)
                ?.setDuration(380)
                ?.setInterpolator(android.view.animation.DecelerateInterpolator())
                ?.withEndAction {
                    overlay?.visibility = android.view.View.GONE
                    overlay?.alpha = 1f
                    overlay?.scaleX = 1f
                    overlay?.scaleY = 1f
                }?.start()
        }

        fun updateDots() {
            dots.forEachIndexed { i, dot ->
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (i < enteredPin.length) brandColor else grayColor
                )
            }
        }

        fun shakeAndReset() {
            tvError.visibility = android.view.View.VISIBLE
            enteredPin = ""
            updateDots()
            // Shake animation using TranslateAnimation
            val shake = android.view.animation.TranslateAnimation(0f, 18f, 0f, 0f).apply {
                duration = 80
                repeatCount = 5
                repeatMode = android.view.animation.Animation.REVERSE
            }
            dots.forEach { it.startAnimation(shake) }
        }

        fun onDigit(d: String) {
            if (enteredPin.length < 4) {
                enteredPin += d
                updateDots()
                tvError.visibility = android.view.View.INVISIBLE
                if (enteredPin.length == 4) {
                    if (enteredPin == savedPin) {
                        isAuthenticatedSession = true
                        dismissOverlay()
                    } else {
                        shakeAndReset()
                    }
                }
            }
        }

        fun onBack() {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
                updateDots()
            }
        }

        val numBtnIds = mapOf(
            R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6",
            R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9",
            R.id.btn0 to "0"
        )
        numBtnIds.forEach { (id, digit) ->
            pinView.findViewById<android.view.View>(id).setOnClickListener { onDigit(digit) }
        }
        pinView.findViewById<android.view.View>(R.id.btnBackspace).setOnClickListener { onBack() }

        fun showBiometricPrompt() {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticatedSession = true
                        dismissOverlay()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Fall back to PIN silently
                    }
                })
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock App")
                .setSubtitle("Use biometric or PIN to access")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
            biometricPrompt.authenticate(promptInfo)
        }

        if (biometricEnabled) {
            // Show biometric button and trigger auto
            btnBiometric.visibility = android.view.View.VISIBLE
            spaceBottomLeft.visibility = android.view.View.GONE
            tvSubtitle.text = "Enter PIN or use biometric"
            btnBiometric.setOnClickListener { showBiometricPrompt() }
            showBiometricPrompt() // auto-trigger biometric prompt
        } else {
            btnBiometric.visibility = android.view.View.GONE
            spaceBottomLeft.visibility = android.view.View.VISIBLE
            tvSubtitle.text = "Enter your PIN to continue"
        }
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
