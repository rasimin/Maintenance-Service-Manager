package com.example.servicemaintainreminder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.data.ServiceHistory
import com.example.servicemaintainreminder.databinding.FragmentHomeBinding
import com.example.servicemaintainreminder.util.DateUtil
import com.example.servicemaintainreminder.util.ModernMenuItem
import com.example.servicemaintainreminder.util.ModernMenuUtil
import com.google.android.gms.ads.AdRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat

data class CostDetail(
    val itemName: String,
    val itemCategory: String,
    val cost: Double,
    val serviceDate: Long
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ItemAdapter
    private var allItemsList: List<Item> = emptyList()
    private var allHistoryList: List<ServiceHistory> = emptyList()
    private var recyclerViewState: android.os.Parcelable? = null

    private lateinit var searchOverlayAdapter: SearchOverlayAdapter
    private var searchResults: List<Item> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupClickListeners()
        setupHeader()
        loadAds()
    }

    private fun updateGreeting() {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val name = prefs.getString("user_name", "User") ?: "User"
        binding.header.tvHeaderWelcome.text = "Welcome back, $name 👋"
    }

    private fun setupHeader() {
        updateGreeting()
        binding.header.cardSettings.setOnClickListener {
            binding.header.cardSettings.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(80)
                .withEndAction {
                    binding.header.cardSettings.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(80)
                        .withEndAction { showSettingsDialog() }
                        .start()
                }.start()
        }
    }

    private fun showAccountDialog(onDismissed: (() -> Unit)? = null) {
        // ... (keep as is)
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val currentName = prefs.getString("user_name", "") ?: ""

        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_account, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val etName = dialogView.findViewById<android.widget.EditText>(R.id.etAccountName)
        val btnSave = dialogView.findViewById<View>(R.id.btnSaveAccount)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancelAccount)

        etName.setText(currentName)

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isNotEmpty()) {
                prefs.edit().putString("user_name", newName).apply()
                updateGreeting()
                dialog.dismiss()
                onDismissed?.invoke()
            } else {
                android.widget.Toast.makeText(requireContext(), "Name cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onDismissed?.invoke()
        }

        dialog.setOnCancelListener { onDismissed?.invoke() }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // — Akun —
        val tvCurrentAccountName = dialogView.findViewById<android.widget.TextView>(R.id.tvCurrentAccountName)
        val btnChangeAccount = dialogView.findViewById<android.view.View>(R.id.btnChangeAccount)

        // — Keamanan —
        val switchAppLock = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchAppLock)
        val llSecurityStatus = dialogView.findViewById<android.view.View>(R.id.llSecurityStatus)
        val tvPinStatus = dialogView.findViewById<android.widget.TextView>(R.id.tvPinStatus)
        val tvBiometricStatus = dialogView.findViewById<android.widget.TextView>(R.id.tvBiometricStatus)
        val switchBiometric = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchBiometric)
        val btnChangePinInline = dialogView.findViewById<android.view.View>(R.id.btnChangePinInline)

        // — Notifikasi —
        val switchEnableNotif = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchEnableNotif)
        val rowNotifTime = dialogView.findViewById<android.view.View>(R.id.rowNotifTime)
        val rowExactAlarm = dialogView.findViewById<android.view.View>(R.id.rowExactAlarm)
        val d1 = dialogView.findViewById<android.view.View>(R.id.dividerNotif1)
        val d2 = dialogView.findViewById<android.view.View>(R.id.dividerNotif2)
        val tvNotifTimeDesc = dialogView.findViewById<android.widget.TextView>(R.id.tvNotifTimeDesc)
        val btnChangeNotifTime = dialogView.findViewById<android.view.View>(R.id.btnChangeNotifTime)
        val btnTestNotif = dialogView.findViewById<android.view.View>(R.id.btnTestNotif)
        val switchExactAlarm = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchExactAlarm)

        // — Dashboard —
        val tvThresholdDesc = dialogView.findViewById<android.widget.TextView>(R.id.tvThresholdDesc)
        val btnChangeThreshold = dialogView.findViewById<android.view.View>(R.id.btnChangeThreshold)

        // — Versioning —
        val tvAppVersion = dialogView.findViewById<android.widget.TextView>(R.id.tvAppVersion)
        val tvBuildDate = dialogView.findViewById<android.widget.TextView>(R.id.tvBuildDate)
        
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btnCloseSettings)

        fun refreshStatus() {
            val isLocked = prefs.getBoolean("is_app_lock_enabled", false)
            val pinSet = !prefs.getString("app_custom_pin", "").isNullOrEmpty()
            val biometricEnabled = prefs.getBoolean("is_biometric_enabled", false)
            val notifHour = prefs.getInt("notif_hour", 8)
            val notifMinute = prefs.getInt("notif_minute", 0)
            val daysLimit = prefs.getInt("upcoming_days_limit", 7)
            val accountName = prefs.getString("user_name", "") ?: ""

            // Version info
            tvAppVersion.text = "Version %s (%s)".format(com.example.servicemaintainreminder.BuildConfig.VERSION_NAME, com.example.servicemaintainreminder.BuildConfig.GIT_COMMIT)
            tvBuildDate.text = "Built on: %s".format(com.example.servicemaintainreminder.BuildConfig.BUILD_TIME)

            // Akun
            tvCurrentAccountName.text = if (accountName.isNotEmpty()) accountName else "Belum diset"

            // Dashboard
            tvThresholdDesc.text = "Tampilkan item dalam $daysLimit hari ke depan"

            // Notifikasi
            val isNotifEnabled = prefs.getBoolean("is_notif_enabled", true)
            switchEnableNotif.isChecked = isNotifEnabled
            
            val isExactAlarmEnabled = prefs.getBoolean("is_exact_alarm_enabled", false)
            switchExactAlarm.tag = true
            switchExactAlarm.isChecked = isExactAlarmEnabled
            switchExactAlarm.tag = null

            val visibility = if (isNotifEnabled) android.view.View.VISIBLE else android.view.View.GONE
            rowNotifTime.visibility = visibility
            rowExactAlarm.visibility = visibility
            d1?.visibility = visibility
            d2?.visibility = visibility

            tvNotifTimeDesc.text = "Dikirim setiap hari pukul %02d:%02d".format(notifHour, notifMinute)

            // Keamanan
            var isProgrammaticLockChange = switchAppLock.tag as? Boolean ?: false
            if (!isProgrammaticLockChange) {
                switchAppLock.isChecked = isLocked
            }
            llSecurityStatus.isVisible = isLocked

            if (pinSet) {
                tvPinStatus.text = "Sudah diset"
                tvPinStatus.setTextColor(android.graphics.Color.parseColor("#27AE60"))
            } else {
                tvPinStatus.text = "Belum diset"
                tvPinStatus.setTextColor(android.graphics.Color.parseColor("#E74C3C"))
            }

            switchBiometric.isEnabled = pinSet
            var isProgrammaticBioChange = switchBiometric.tag as? Boolean ?: false
            if (!isProgrammaticBioChange) {
                switchBiometric.isChecked = biometricEnabled
            }

            if (biometricEnabled) {
                tvBiometricStatus.text = "Aktif"
                tvBiometricStatus.setTextColor(android.graphics.Color.parseColor("#27AE60"))
            } else {
                tvBiometricStatus.text = "Nonaktif"
                tvBiometricStatus.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            }
        }

        var isProgrammaticChange = false

        fun setSwitchChecked(checked: Boolean) {
            isProgrammaticChange = true
            switchAppLock.tag = true
            switchAppLock.isChecked = checked
            switchAppLock.tag = false
            isProgrammaticChange = false
        }

        refreshStatus()

        // ── Akun ──
        btnChangeAccount.setOnClickListener {
            dialog.dismiss()
            showAccountDialog { showSettingsDialog() }
        }

        // ── Keamanan ──
        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticChange) return@setOnCheckedChangeListener

            if (isChecked) {
                setSwitchChecked(false)
                showSetPinDialog(isEnableLockFlow = true) {
                    prefs.edit().putBoolean("is_app_lock_enabled", true).apply()
                    setSwitchChecked(true)
                    refreshStatus()
                    showBiometricOfferDialog(prefs) { refreshStatus() }
                }
            } else {
                prefs.edit()
                    .putBoolean("is_app_lock_enabled", false)
                    .putBoolean("is_biometric_enabled", false)
                    .remove("app_custom_pin")
                    .apply()
                refreshStatus()
            }
        }

        var isProgrammaticBiometricChange = false

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticBiometricChange) return@setOnCheckedChangeListener

            if (isChecked) {
                isProgrammaticBiometricChange = true
                switchBiometric.isChecked = false
                isProgrammaticBiometricChange = false

                val executor = ContextCompat.getMainExecutor(requireContext())
                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            prefs.edit().putBoolean("is_biometric_enabled", true).apply()
                            isProgrammaticBiometricChange = true
                            switchBiometric.isChecked = true
                            isProgrammaticBiometricChange = false
                            refreshStatus()
                            android.widget.Toast.makeText(requireContext(), "✅ Biometrik diaktifkan", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            android.widget.Toast.makeText(requireContext(), "Biometrik tidak dikonfirmasi", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        override fun onAuthenticationFailed() { super.onAuthenticationFailed() }
                    })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Konfirmasi Biometrik")
                    .setSubtitle("Autentikasi untuk mengaktifkan login biometrik")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Batal")
                    .build()
                biometricPrompt.authenticate(promptInfo)
            } else {
                prefs.edit().putBoolean("is_biometric_enabled", false).apply()
                refreshStatus()
            }
        }

        btnChangePinInline.setOnClickListener {
            showSetPinDialog(isEnableLockFlow = false) { refreshStatus() }
        }

        // ── Notifikasi ──
        switchEnableNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_notif_enabled", isChecked).apply()
            
            val visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            rowNotifTime.visibility = visibility
            rowExactAlarm.visibility = visibility
            d1?.visibility = visibility
            d2?.visibility = visibility
            
            if (isChecked) {
                val h = prefs.getInt("notif_hour", 8)
                val m = prefs.getInt("notif_minute", 0)
                rescheduleNotifWorker(h, m)
            } else {
                // Cancel everything
                androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("ServiceReminderWork")
                val am = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                val intent = android.content.Intent(requireContext(), com.example.servicemaintainreminder.worker.AlarmReceiver::class.java)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    requireContext(), 1001, intent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                am.cancel(pendingIntent)
            }
        }

        btnChangeNotifTime.setOnClickListener {
            dialog.dismiss()
            showNotifTimePicker(prefs) { showSettingsDialog() }
        }

        switchExactAlarm.setOnCheckedChangeListener { _, isChecked ->
            if (switchExactAlarm.tag == true) return@setOnCheckedChangeListener
            if (isChecked) {
                // Check for exact alarm permission on Android 12+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        // Redirect to system settings
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Izin Diperlukan")
                            .setMessage("Untuk pengingat real-time, Anda perlu mengaktifkan izin 'Alarm & Reminders' di pengaturan sistem. Tanpa izin ini, Android akan menunda notifikasi untuk menghemat baterai.")
                            .setPositiveButton("Buka Pengaturan") { _, _ ->
                                val intent = android.content.Intent().apply {
                                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                }
                                startActivity(intent)
                            }
                            .setNegativeButton("Batal") { _, _ -> 
                                switchExactAlarm.isChecked = false
                            }
                            .show()
                        return@setOnCheckedChangeListener
                    }
                }
                prefs.edit().putBoolean("is_exact_alarm_enabled", true).apply()
                android.widget.Toast.makeText(requireContext(), "🚀 Pengingat Real-time diaktifkan!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putBoolean("is_exact_alarm_enabled", false).apply()
                android.widget.Toast.makeText(requireContext(), "Pengingat kembali ke mode hemat baterai.", android.widget.Toast.LENGTH_SHORT).show()
            }

            // Reschedule regardless of exact or periodic
            val h = prefs.getInt("notif_hour", 8)
            val m = prefs.getInt("notif_minute", 0)
            rescheduleNotifWorker(h, m)
        }

        btnTestNotif.setOnClickListener {
            val testRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.servicemaintainreminder.worker.ReminderWorker>()
                .build()
            androidx.work.WorkManager.getInstance(requireContext()).enqueue(testRequest)
            android.widget.Toast.makeText(requireContext(), "🔔 Mengirim notifikasi test...", android.widget.Toast.LENGTH_SHORT).show()
        }

        // ── Dashboard ──
        btnChangeThreshold.setOnClickListener {
            dialog.dismiss()
            showThresholdSettingsDialog { showSettingsDialog() }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showNotifTimePicker(
        prefs: android.content.SharedPreferences,
        onTimeSet: () -> Unit
    ) {
        val currentHour = prefs.getInt("notif_hour", 8)
        val currentMinute = prefs.getInt("notif_minute", 0)

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_time_picker, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val npHour = view.findViewById<android.widget.NumberPicker>(R.id.npHour)
        val npMinute = view.findViewById<android.widget.NumberPicker>(R.id.npMinute)
        val btnSave = view.findViewById<android.view.View>(R.id.btnSaveTime)
        val btnCancel = view.findViewById<android.view.View>(R.id.btnCancelTime)

        // Setup Hours
        npHour.minValue = 0
        npHour.maxValue = 23
        npHour.setFormatter { "%02d".format(it) }
        npHour.value = currentHour

        // Setup Minutes
        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.setFormatter { "%02d".format(it) }
        npMinute.value = currentMinute

        btnSave.setOnClickListener {
            val selectedHour = npHour.value
            val selectedMinute = npMinute.value

            prefs.edit()
                .putInt("notif_hour", selectedHour)
                .putInt("notif_minute", selectedMinute)
                .apply()

            rescheduleNotifWorker(selectedHour, selectedMinute)

            android.widget.Toast.makeText(
                requireContext(),
                "Notifikasi setiap hari pukul %02d:%02d".format(selectedHour, selectedMinute),
                android.widget.Toast.LENGTH_SHORT
            ).show()

            onTimeSet()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onTimeSet()
        }

        dialog.setOnCancelListener { onTimeSet() }
        dialog.show()
    }

    private fun rescheduleNotifWorker(hour: Int, minute: Int) {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isExact = prefs.getBoolean("is_exact_alarm_enabled", false)
        
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis

        // 1. Always cancel existing WorkManager periodic work
        androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("ServiceReminderWork")

        // 2. Clear existing Exact Alarm regardless of current setting
        val am = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(requireContext(), com.example.servicemaintainreminder.worker.AlarmReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            requireContext(), 1001, intent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pendingIntent)

        if (isExact) {
            // MODE REAL-TIME (Exact Alarm)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            } else {
                am.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            // MODE HEMAT BATERAI (WorkManager)
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.servicemaintainreminder.worker.ReminderWorker>(
                1, java.util.concurrent.TimeUnit.DAYS
            )
                .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            androidx.work.WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "ServiceReminderWork",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    private fun showBiometricOfferDialog(prefs: android.content.SharedPreferences, onDone: () -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val view = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(80, 60, 80, 60)
            setBackgroundResource(R.drawable.bg_search_overlay)
        }
        val icon = android.widget.ImageView(requireContext()).apply {
            setImageResource(android.R.drawable.ic_secure)
            imageTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_primary))
            layoutParams = android.widget.LinearLayout.LayoutParams(120, 120).apply { gravity = android.view.Gravity.CENTER }
        }
        val title = android.widget.TextView(requireContext()).apply {
            text = "Enable Biometric Login?"
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#1A1A2E"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        val subtitle = android.widget.TextView(requireContext()).apply {
            text = "Use fingerprint or face recognition for faster access.\nYou can still use PIN as backup."
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#8A8A9A"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        val btnEnable = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "Enable Biometric"
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_primary))
            setTextColor(android.graphics.Color.WHITE)
            cornerRadius = 24
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 140)
        }
        val btnSkip = com.google.android.material.button.MaterialButton(requireContext(), null,
            com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "Skip for now"
            setTextColor(android.graphics.Color.parseColor("#8A8A9A"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity = android.view.Gravity.CENTER }
        }
        view.addView(icon)
        view.addView(title)
        view.addView(subtitle)
        view.addView(btnEnable)
        view.addView(btnSkip)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnEnable.setOnClickListener {
            prefs.edit().putBoolean("is_biometric_enabled", true).apply()
            android.widget.Toast.makeText(requireContext(), "✅ Security setup complete! PIN + Biometric enabled.", android.widget.Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onDone()
        }
        btnSkip.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "✅ PIN lock enabled. You can add biometric later in Security settings.", android.widget.Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onDone()
        }
        dialog.show()
    }

    private fun authenticateForSecurity(onSuccess: () -> Unit): Unit? {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.widget.Toast.makeText(requireContext(), "Auth Error: $errString", android.widget.Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Security Confirmation")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
        return Unit
    }

    private fun showSetPinDialog(isEnableLockFlow: Boolean = false, onPinSaved: (() -> Unit)? = null) {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_pin, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        if (isEnableLockFlow) dialog.setCancelable(false)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvSetPinTitle)
        val tvSubtitle = dialogView.findViewById<android.widget.TextView>(R.id.tvSetPinSubtitle)
        val tvError = dialogView.findViewById<android.widget.TextView>(R.id.tvSetPinError)
        val btnCancel = dialogView.findViewById<android.view.View>(R.id.btnCancelSetPin)

        val dots = listOf(
            dialogView.findViewById<android.view.View>(R.id.setupPinDot1),
            dialogView.findViewById<android.view.View>(R.id.setupPinDot2),
            dialogView.findViewById<android.view.View>(R.id.setupPinDot3),
            dialogView.findViewById<android.view.View>(R.id.setupPinDot4),
        )

        val oldPin = prefs.getString("app_custom_pin", "") ?: ""
        // 0 = ENTER_OLD, 1 = ENTER_NEW, 2 = CONFIRM_NEW
        var currentState = if (isEnableLockFlow || oldPin.isEmpty()) 1 else 0

        if (currentState == 0) {
            tvTitle.text = "Current PIN"
            tvSubtitle.text = "Please enter your current PIN first"
        } else {
            tvTitle.text = "Create PIN"
            tvSubtitle.text = "Set a 4-digit PIN to lock your app"
        }

        var enteredPin = ""
        var firstEnteredNewPin = ""
        val brandColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_primary)

        fun updateDots() {
            dots.forEachIndexed { i, dot ->
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (i < enteredPin.length) brandColor else android.graphics.Color.parseColor("#DDDDDD")
                )
            }
        }

        fun shakeDots() {
            val shake = android.view.animation.TranslateAnimation(0f, 18f, 0f, 0f).apply {
                duration = 80
                repeatCount = 5
                repeatMode = android.view.animation.Animation.REVERSE
            }
            dots.forEach { it.startAnimation(shake) }
        }

        fun onDigit(d: String, view: android.view.View? = null) {
            if (enteredPin.length < 4) {
                // Button pop animation
                view?.animate()?.scaleX(0.9f)?.scaleY(0.9f)?.setDuration(80)?.withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }?.start()

                enteredPin += d
                updateDots()
                
                // Dot pop animation
                val currentDot = dots[enteredPin.length - 1]
                currentDot.animate().scaleX(1.4f).scaleY(1.4f).setDuration(100).withEndAction {
                    currentDot.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                
                if (enteredPin.length == 4) {
                    view?.postDelayed({
                        if (currentState == 0) {
                            if (enteredPin == oldPin) {
                                currentState = 1
                                enteredPin = ""
                                updateDots()
                                tvTitle.text = "Create New PIN"
                                tvSubtitle.text = "Enter a new 4-digit PIN"
                                tvError.visibility = android.view.View.INVISIBLE
                            } else {
                                shakeDots()
                                enteredPin = ""
                                updateDots()
                                tvError.text = "Incorrect current PIN"
                                tvError.visibility = android.view.View.VISIBLE
                            }
                        } else if (currentState == 1) {
                            firstEnteredNewPin = enteredPin
                            currentState = 2
                            enteredPin = ""
                            updateDots()
                            tvTitle.text = "Confirm PIN"
                            tvSubtitle.text = "Re-enter your new PIN"
                            tvError.visibility = android.view.View.INVISIBLE
                        } else if (currentState == 2) {
                            if (enteredPin == firstEnteredNewPin) {
                                prefs.edit().putString("app_custom_pin", enteredPin).apply()
                                prefs.edit().putBoolean("is_app_lock_enabled", true).apply()
                                android.widget.Toast.makeText(requireContext(), "✅ PIN saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                onPinSaved?.invoke()
                            } else {
                                shakeDots()
                                currentState = 1
                                enteredPin = ""
                                firstEnteredNewPin = ""
                                updateDots()
                                tvTitle.text = "Create PIN"
                                tvSubtitle.text = "PIN mismatch. Try again."
                                tvError.text = "PINs do not match"
                                tvError.visibility = android.view.View.VISIBLE
                            }
                        }
                    }, 150)
                } else {
                    tvError.visibility = android.view.View.INVISIBLE
                }
            }
        }

        fun onBack(view: android.view.View? = null) {
            view?.animate()?.scaleX(0.9f)?.scaleY(0.9f)?.setDuration(80)?.withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }?.start()

            if (enteredPin.isNotEmpty()) {
                val currentDot = dots[enteredPin.length - 1]
                enteredPin = enteredPin.dropLast(1)
                updateDots()
                
                // Dot shrink animation on delete
                currentDot.animate().scaleX(0.7f).scaleY(0.7f).setDuration(100).withEndAction {
                    currentDot.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
            }
        }

        val btnIds = mapOf(
            R.id.setupBtn1 to "1", R.id.setupBtn2 to "2", R.id.setupBtn3 to "3",
            R.id.setupBtn4 to "4", R.id.setupBtn5 to "5", R.id.setupBtn6 to "6",
            R.id.setupBtn7 to "7", R.id.setupBtn8 to "8", R.id.setupBtn9 to "9",
            R.id.setupBtn0 to "0"
        )
        btnIds.forEach { (id, digit) ->
            dialogView.findViewById<android.view.View>(id).setOnClickListener { onDigit(digit, it) }
        }
        dialogView.findViewById<android.view.View>(R.id.setupBtnBackspace).setOnClickListener { onBack(it) }
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnCancel.isVisible = !isEnableLockFlow

        dialog.show()
    }


    private fun showThresholdSettingsDialog(onDismissed: (() -> Unit)? = null) {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_threshold_settings, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val etUpcomingDays = dialogView.findViewById<android.widget.EditText>(R.id.etUpcomingDays)
        val btnSaveUpcoming = dialogView.findViewById<android.view.View>(R.id.btnSaveUpcoming)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btnCloseUpcomingDialog)
        
        val daysLimit = prefs.getInt("upcoming_days_limit", 7)
        etUpcomingDays.setText(daysLimit.toString())

        btnSaveUpcoming.setOnClickListener {
            val daysStr = etUpcomingDays.text.toString()
            val newDaysLimit = if (daysStr.isNotEmpty()) daysStr.toIntOrNull() ?: 30 else 30
            prefs.edit().putInt("upcoming_days_limit", newDaysLimit).apply()

            if (binding.tvUpcomingHeader.text == "Upcoming Maintenance") {
                loadUpcomingItems()
            }
            updateDashboard(viewModel.allItems.value ?: emptyList())
            
            android.widget.Toast.makeText(requireContext(), "Parameter saved", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onDismissed?.invoke()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            onDismissed?.invoke()
        }

        dialog.setOnCancelListener { onDismissed?.invoke() }
        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter { item ->
            val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(item.id)
            findNavController().navigate(action)
        }
        binding.rvItems.adapter = adapter
    }

    private fun setupSearchView() {
        searchOverlayAdapter = SearchOverlayAdapter(emptyList()) { item ->
            binding.rvSearchOverlay.isVisible = false
            binding.searchDivider.isVisible = false
            binding.searchView.clearFocus()
            binding.searchView.setQuery("", false)
            val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(item.id)
            findNavController().navigate(action)
        }
        binding.rvSearchOverlay.adapter = searchOverlayAdapter
        binding.rvSearchOverlay.layoutManager = LinearLayoutManager(requireContext())

        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.searchCardHome.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start()
            } else {
                binding.searchCardHome.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                if (binding.searchView.query.isNullOrEmpty()) {
                    binding.rvSearchOverlay.isVisible = false
                    binding.searchDivider.isVisible = false
                }
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    binding.rvSearchOverlay.isVisible = false
                    binding.searchDivider.isVisible = false
                } else {
                    searchResults = allItemsList.filter {
                        it.name.contains(newText, ignoreCase = true) || it.category.contains(newText, ignoreCase = true)
                    }.take(5)
                    if (searchResults.isNotEmpty()) {
                        searchOverlayAdapter.submitList(searchResults)
                        binding.rvSearchOverlay.isVisible = true
                        binding.searchDivider.isVisible = true
                    } else {
                        binding.rvSearchOverlay.isVisible = false
                        binding.searchDivider.isVisible = false
                    }
                }
                return true
            }
        })
    }

    private fun setupObservers() {
        binding.progressBar.isVisible = true
        
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            allItemsList = items
            binding.progressBar.isVisible = false
            binding.rvItems.isVisible = true
            updateDashboard(items)
            updateCostEstimations()
            
            if (binding.tvUpcomingHeader.text == "Upcoming Maintenance") {
                loadUpcomingItems()
            }
        }

        viewModel.allHistory.observe(viewLifecycleOwner) { history ->
            allHistoryList = history
            updateCostEstimations()
        }
    }
    
    private fun loadUpcomingItems() {
        viewModel.allItems.value?.let { items ->
            val currentTime = System.currentTimeMillis()
            val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val daysLimit = prefs.getInt("upcoming_days_limit", 7)
            
            val upcoming = items.filter { 
                if (!it.isActive) return@filter false
                val ds = DateUtil.getDaysDifference(it.nextServiceDate)
                ds in 0..daysLimit 
            }
                .sortedBy { it.nextServiceDate }
                .take(10)
            
            binding.rvItems.isVisible = upcoming.isNotEmpty()
            binding.llEmptyState.isVisible = upcoming.isEmpty()
            
            adapter.submitList(upcoming) {
                recyclerViewState?.let {
                    binding.rvItems.layoutManager?.onRestoreInstanceState(it)
                    recyclerViewState = null
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cardMyDevices.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToDevicesFragment("All")
            findNavController().navigate(action)
        }

        binding.cardUpcoming.setOnClickListener {
            viewModel.allItems.value?.let { items ->
                val currentTime = System.currentTimeMillis()
                val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val daysLimit = prefs.getInt("upcoming_days_limit", 7)
                
                val urgent = items.filter { 
                    if (!it.isActive) return@filter false
                    val ds = DateUtil.getDaysDifference(it.nextServiceDate)
                    ds in 0..daysLimit
                }
                binding.tvUpcomingHeader.text = "Upcoming Maintenance"
                val sorted = urgent.sortedBy { it.nextServiceDate }
                binding.rvItems.isVisible = sorted.isNotEmpty()
                binding.llEmptyState.isVisible = sorted.isEmpty()
                adapter.submitList(sorted)
            }
        }

            binding.cardOverdue.setOnClickListener {
            viewModel.allItems.value?.let { items ->
                val overdue = items.filter { it.isActive && DateUtil.getDaysDifference(it.nextServiceDate) < 0 }
                binding.tvUpcomingHeader.text = "Overdue Items"
                val sorted = overdue.sortedBy { it.nextServiceDate }
                binding.rvItems.isVisible = sorted.isNotEmpty()
                binding.llEmptyState.isVisible = sorted.isEmpty()
                adapter.submitList(sorted)
            }
        }

        // Pindah ke halaman My Devices otomatis sesuai filter
        binding.tvViewAll.setOnClickListener {
            val filterType = when (binding.tvUpcomingHeader.text.toString()) {
                "Overdue Items" -> "Overdue"
                "Urgent Maintenance", "Upcoming Maintenance" -> "Upcoming"
                else -> "All"
            }
            val action = HomeFragmentDirections.actionHomeFragmentToDevicesFragment(filterType)
            findNavController().navigate(action)
        }

        // Highlight View All ketika mentok scroll ke kanan
        binding.rvItems.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Cek jika tidak bisa scroll ke kanan lagi (mentok) dan scroll berhenti
                if (!recyclerView.canScrollHorizontally(1) && newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                    binding.tvViewAll.animate()
                        .scaleX(1.3f).scaleY(1.3f)
                        .setDuration(150)
                        .withEndAction {
                            binding.tvViewAll.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                        }.start()
                }
            }
        })

        binding.fabAddHome.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAddItemFragment()
            findNavController().navigate(action)
        }

        binding.layoutCostDashboard.btnCalendarView.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToCalendarCostFragment()
            findNavController().navigate(action)
        }
    }

    private fun updateDashboard(items: List<com.example.servicemaintainreminder.data.Item>) {
        val activeItems = items.filter { it.isActive }
        val total = activeItems.size
        val currentTime = System.currentTimeMillis()
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val daysLimit = prefs.getInt("upcoming_days_limit", 7)

        val upcoming = activeItems.count { 
            val ds = DateUtil.getDaysDifference(it.nextServiceDate)
            ds in 0..daysLimit 
        }
        val overdue = activeItems.count { DateUtil.getDaysDifference(it.nextServiceDate) < 0 }

        binding.tvTotalItems.text = total.toString()
        binding.tvUpcomingService.text = upcoming.toString()
        binding.tvOverdueService.text = overdue.toString()
    }

    private fun updateCostEstimations() {
        val dash = binding.layoutCostDashboard
        if (allItemsList.isEmpty()) {
            dash.tvEstMonth1Cost.text = "-"
            dash.tvEstMonth2Cost.text = "-"
            dash.tvEstMonth3Cost.text = "-"
            dash.tvRealMonth1Cost.text = "-"
            dash.tvRealMonth2Cost.text = "-"
            dash.tvRealMonth3Cost.text = "-"
            return
        }

        // 1. Calculate average cost per item using historical data, fallback to estimatedCost
        val itemAverageCost = mutableMapOf<Long, Double>()
        for (item in allItemsList) {
            val itemHistory = allHistoryList.filter { it.itemId == item.id }
            val avgCost = if (itemHistory.isNotEmpty()) {
                itemHistory.sumOf { it.cost } / itemHistory.size 
            } else {
                item.estimatedCost
            }
            itemAverageCost[item.id] = avgCost
        }

        // 2. Setup Months
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val months = DoubleArray(3) { 0.0 }
        val monthNames = Array(3) { "" }
        val monthFormats = SimpleDateFormat("MMM", Locale.getDefault())
        
        // Hold detail data for popup
        val monthDetails = Array(3) { mutableListOf<CostDetail>() }

        for (i in 0..2) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, i)
            monthNames[i] = monthFormats.format(cal.time)
        }

        // 3. Project future services
        val activeItems = allItemsList.filter { it.isActive }

        for (item in activeItems) {
            val avgCost = itemAverageCost[item.id] ?: 0.0
            if (avgCost == 0.0) continue // Cannot estimate if no history

            var simulatedNextDate = item.nextServiceDate
            
            // Loop sequentially into the future
            var maxLoops = 20
            while(maxLoops > 0) {
                maxLoops--
                val svcCal = Calendar.getInstance()
                svcCal.timeInMillis = simulatedNextDate

                val svcYear = svcCal.get(Calendar.YEAR)
                val svcMonth = svcCal.get(Calendar.MONTH)

                // Calculate month difference relative to logic's current month
                val monthDiff = (svcYear - currentYear) * 12 + (svcMonth - currentMonth)

                if (monthDiff > 2) {
                    break // Beyond our 3 month window
                }

                if (monthDiff < 0) {
                    months[0] += avgCost // Overdue goes to current month
                    monthDetails[0].add(CostDetail(item.name, item.category, avgCost, simulatedNextDate))
                } else if (monthDiff <= 2) {
                    months[monthDiff] += avgCost
                    monthDetails[monthDiff].add(CostDetail(item.name, item.category, avgCost, simulatedNextDate))
                }

                simulatedNextDate = DateUtil.getNextServiceDate(simulatedNextDate, item.serviceIntervalValue, item.serviceIntervalUnit)
            }
        }
        
        // 4. Calculate Realized Cost
        val realMonthNames = Array(3) { "" }
        for (i in 0..2) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, i - 2)
            realMonthNames[i] = monthFormats.format(cal.time)
        }
        
        val realMonths = DoubleArray(3) { 0.0 }
        val realDetails = Array(3) { mutableListOf<CostDetail>() }
        
        for (history in allHistoryList) {
            val item = allItemsList.find { it.id == history.itemId } ?: continue
            val cal = Calendar.getInstance()
            cal.timeInMillis = history.serviceDate

            val hYear = cal.get(Calendar.YEAR)
            val hMonth = cal.get(Calendar.MONTH)

            val monthDiff = (hYear - currentYear) * 12 + (hMonth - currentMonth)
            val targetIndex = monthDiff + 2
            
            if (targetIndex in 0..2) {
                realMonths[targetIndex] += history.cost
                realDetails[targetIndex].add(CostDetail(item.name, item.category, history.cost, history.serviceDate))
            }
        }

        // 5. Update UI
        val formatCost = { cost: Double ->
            if (cost == 0.0) "-" 
            else {
                if (cost >= 1_000_000) {
                    val m = cost / 1_000_000.0
                    if (m % 1.0 == 0.0) "Rp ${m.toInt()}M" else String.format(Locale.US, "Rp %.1fM", m)
                } else if (cost >= 1_000) {
                    val k = cost / 1_000.0
                    if (k % 1.0 == 0.0) "Rp ${k.toInt()}K" else String.format(Locale.US, "Rp %.1fK", k)
                } else {
                    "Rp ${cost.toInt()}"
                }
            }
        }

        dash.tvEstMonth1Name.text = monthNames[0]
        dash.tvEstMonth2Name.text = monthNames[1]
        dash.tvEstMonth3Name.text = monthNames[2]
        
        dash.tvEstMonth1Cost.text = formatCost(months[0])
        dash.tvEstMonth2Cost.text = formatCost(months[1])
        dash.tvEstMonth3Cost.text = formatCost(months[2])

        dash.tvRealMonth1Name.text = realMonthNames[0]
        dash.tvRealMonth2Name.text = realMonthNames[1]
        dash.tvRealMonth3Name.text = realMonthNames[2]
        
        dash.tvRealMonth1Cost.text = formatCost(realMonths[0])
        dash.tvRealMonth2Cost.text = formatCost(realMonths[1])
        dash.tvRealMonth3Cost.text = formatCost(realMonths[2])
        
        // Chart text labels
        dash.tvChartEstMonth1.text = monthNames[0]
        dash.tvChartEstMonth2.text = monthNames[1]
        dash.tvChartEstMonth3.text = monthNames[2]
        
        dash.tvChartRealMonth1.text = realMonthNames[0]
        dash.tvChartRealMonth2.text = realMonthNames[1]
        dash.tvChartRealMonth3.text = realMonthNames[2]

        // Setup Charts
        dash.chartEstimated.setChartMode(false)
        dash.chartEstimated.dataPoints = listOf(months[0].toFloat(), months[1].toFloat(), months[2].toFloat())

        dash.chartRealized.setChartMode(true)
        dash.chartRealized.dataPoints = listOf(realMonths[0].toFloat(), realMonths[1].toFloat(), realMonths[2].toFloat())

        // Add Click Listeners for Pop up
        val clickEst1 = View.OnClickListener { showCostDetailDialog("Estimasi ${monthNames[0]}", monthDetails[0], months[0]) }
        dash.llEstMonth1.setOnClickListener(clickEst1); dash.vEstBar1.setOnClickListener(clickEst1)
        val clickEst2 = View.OnClickListener { showCostDetailDialog("Estimasi ${monthNames[1]}", monthDetails[1], months[1]) }
        dash.llEstMonth2.setOnClickListener(clickEst2); dash.vEstBar2.setOnClickListener(clickEst2)
        val clickEst3 = View.OnClickListener { showCostDetailDialog("Estimasi ${monthNames[2]}", monthDetails[2], months[2]) }
        dash.llEstMonth3.setOnClickListener(clickEst3); dash.vEstBar3.setOnClickListener(clickEst3)
        
        val clickReal1 = View.OnClickListener { showCostDetailDialog("Realisasi ${realMonthNames[0]}", realDetails[0], realMonths[0]) }
        dash.llRealMonth1.setOnClickListener(clickReal1); dash.vRealBar1.setOnClickListener(clickReal1)
        val clickReal2 = View.OnClickListener { showCostDetailDialog("Realisasi ${realMonthNames[1]}", realDetails[1], realMonths[1]) }
        dash.llRealMonth2.setOnClickListener(clickReal2); dash.vRealBar2.setOnClickListener(clickReal2)
        val clickReal3 = View.OnClickListener { showCostDetailDialog("Realisasi ${realMonthNames[2]}", realDetails[2], realMonths[2]) }
        dash.llRealMonth3.setOnClickListener(clickReal3); dash.vRealBar3.setOnClickListener(clickReal3)
    }

    private fun showCostDetailDialog(
        title: String,
        details: List<CostDetail>,
        totalCost: Double
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_estimated_cost_detail, null)
        dialog.setContentView(dialogView)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogMonthTitle)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDialogTotalCost)
        val container = dialogView.findViewById<LinearLayout>(R.id.llCostDetailContainer)
        val btnClose = dialogView.findViewById<View>(R.id.btnDialogClose)

        tvTitle.text = title
        
        val format = NumberFormat.getInstance(Locale("in", "ID"))
        tvTotal.text = "Rp ${format.format(totalCost.toLong())}"

        if (details.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "No service schedules require an estimated cost this month."
                textSize = 13f
                setPadding(0, 16, 0, 16)
            }
            container.addView(emptyTv)
        } else {
            for (item in details) {
                val rowView = layoutInflater.inflate(R.layout.item_cost_detail_row, container, false)
                
                val tvName = rowView.findViewById<TextView>(R.id.tvDetailItemName)
                val tvCategory = rowView.findViewById<TextView>(R.id.tvDetailItemCategory)
                val tvCost = rowView.findViewById<TextView>(R.id.tvDetailItemCost)

                tvName.text = item.itemName
                
                val formattedDate = DateUtil.formatDate(item.serviceDate)
                tvCategory.text = "${item.itemCategory} • $formattedDate"
                tvCost.text = "Rp ${format.format(item.cost.toLong())}"

                container.addView(rowView)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadAds() {
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    override fun onDestroyView() {
        recyclerViewState = binding.rvItems.layoutManager?.onSaveInstanceState()
        super.onDestroyView()
        _binding = null
    }
}
