package com.example.servicemaintainreminder.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.databinding.FragmentAddItemBinding
import com.example.servicemaintainreminder.util.DateUtil
import com.example.servicemaintainreminder.util.CurrencyTextWatcher
import com.example.servicemaintainreminder.R
import java.util.*
import android.graphics.Color
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val args: AddItemFragmentArgs by navArgs()
    private var selectedDate: Long = System.currentTimeMillis()
    private var isEditMode = false
    private var itemToEdit: Item? = null
    private var isFixedScheduleSelected = false  // false = Flexible (default)
    private var selectedIcon: String? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var deviceCount = 0

    // Icons shown inline in scroll row (first 8)
    private val previewIcons = listOf(
        "ic_ac", "ic_car", "ic_electronic", "ic_machine",
        "ic_service", "ic_devices", "ic_clock", "ic_upcoming"
    )

    // All icons available in the picker
    private val allIcons = listOf(
        "ic_ac", "ic_car", "ic_electronic", "ic_machine",
        "ic_service", "ic_devices", "ic_clock", "ic_upcoming",
        "ic_history", "ic_input_calendar", "ic_input_cost", "ic_input_note",
        "ic_input_category", "ic_input_edit", "ic_input_info",
        "ic_dashboard", "ic_lock", "ic_notif_bell", "ic_person",
        "ic_pin", "ic_search_clean", "ic_fingerprint", "ic_more_vert",
        "ic_arrow_right", "ic_chevron_left", "ic_chevron_right",
        "ic_home", "ic_restaurant", "ic_local_cafe", "ic_directions_car",
        "ic_local_shipping", "ic_flight", "ic_build", "ic_favorite",
        "ic_star", "ic_thumb_up", "ic_shopping_cart", "ic_account_balance_wallet",
        "ic_attach_money", "ic_local_mall", "ic_pets", "ic_music_note",
        "ic_games", "ic_fitness_center", "ic_camera_alt", "ic_brush"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupDatePicker()
        setupIconSelection()

        isEditMode = args.itemId != -1L

        // Setup header back button & title
        binding.header.ivBackButton.setOnClickListener {
            binding.header.ivBackButton.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(80)
                .withEndAction {
                    binding.header.ivBackButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(80)
                        .withEndAction { findNavController().navigateUp() }
                        .start()
                }.start()
        }
        binding.header.tvHeaderTitle.text = if (isEditMode) "Edit Item" else "Add Item"

        if (isEditMode) {
            setupEditMode()
        }

        binding.btnSave.setOnClickListener {
            saveItem()
        }

        binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchColor(isChecked)
        }
        updateSwitchColor(binding.switchActive.isChecked)

        setupScheduleTypeCards()

        // Format otomatis titik ribuan pada field estimated cost
        CurrencyTextWatcher.attach(binding.etEstimatedCost)

        // Fade scroll hint: hilang saat sudah scroll ke bawah
        binding.nestedScrollViewAddItem.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val scrollView = v as androidx.core.widget.NestedScrollView
            val isAtBottom = scrollY >= (scrollView.getChildAt(0).measuredHeight - scrollView.measuredHeight - 16)
            binding.viewScrollFade.animate()
                .alpha(if (isAtBottom) 0f else 1f)
                .setDuration(200)
                .start()
        }

        // Amati jumlah perangkat untuk logika iklan
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            deviceCount = items.size
        }

        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        // Test ID: ca-app-pub-3940256099942544/1033173712
        InterstitialAd.load(requireContext(), "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    private fun setupIconSelection() {
        binding.llIconContainer.removeAllViews()

        // Show preview icons inline
        previewIcons.forEach { iconName ->
            val iconView = layoutInflater.inflate(R.layout.item_icon_choice, binding.llIconContainer, false)
            val ivIcon = iconView.findViewById<android.widget.ImageView>(R.id.ivIconChoice)
            val cardIcon = iconView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconChoice)

            val resId = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
            ivIcon.setImageResource(if (resId != 0) resId else R.drawable.ic_devices)

            updateIconSelectionUI(cardIcon, iconName == selectedIcon)

            cardIcon.setOnClickListener {
                selectedIcon = iconName
                setupIconSelection()
            }

            binding.llIconContainer.addView(iconView)
        }

        // If an extra icon is selected, show it before the "more" button
        val isExtraSelected = selectedIcon != null && !previewIcons.contains(selectedIcon)
        if (isExtraSelected) {
            val extraView = layoutInflater.inflate(R.layout.item_icon_choice, binding.llIconContainer, false)
            val ivExtra = extraView.findViewById<android.widget.ImageView>(R.id.ivIconChoice)
            val cardExtra = extraView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconChoice)
            val resId = resources.getIdentifier(selectedIcon, "drawable", requireContext().packageName)
            ivExtra.setImageResource(if (resId != 0) resId else R.drawable.ic_devices)
            updateIconSelectionUI(cardExtra, true)
            cardExtra.setOnClickListener {
                showIconPickerBottomSheet()
            }
            binding.llIconContainer.addView(extraView)
        }

        // Add "More" button with custom text layout
        val moreBtnView = layoutInflater.inflate(R.layout.item_more_button, binding.llIconContainer, false)
        val cardMore = moreBtnView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardMoreBtn)
        cardMore.setOnClickListener {
            showIconPickerBottomSheet()
        }
        binding.llIconContainer.addView(moreBtnView)
    }

    private fun showIconPickerBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_icon_picker, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val rv = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvIconPicker)
        rv.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 5)

        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            inner class IconVH(v: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
                val card = v.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardIconChoice)
                val icon = v.findViewById<android.widget.ImageView>(R.id.ivIconChoice)
            }
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val v = layoutInflater.inflate(R.layout.item_icon_choice, parent, false)
                return IconVH(v)
            }
            override fun getItemCount() = allIcons.size
            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val vh = holder as IconVH
                val iconName = allIcons[position]
                val resId = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
                vh.icon.setImageResource(if (resId != 0) resId else R.drawable.ic_devices)
                updateIconSelectionUI(vh.card, iconName == selectedIcon)
                vh.card.setOnClickListener {
                    selectedIcon = iconName
                    notifyDataSetChanged()
                    setupIconSelection()
                    dialog.dismiss()
                }
            }
        }

        rv.adapter = adapter
        dialog.show()
    }

    private fun updateIconSelectionUI(card: com.google.android.material.card.MaterialCardView, isSelected: Boolean) {
        val brandColor = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        if (isSelected) {
            card.strokeColor = brandColor
            card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            card.setCardBackgroundColor(Color.parseColor("#EDF1FF"))
        } else {
            card.strokeColor = Color.parseColor("#EEEEEE")
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.setCardBackgroundColor(Color.WHITE)
        }
    }

    private fun updateSwitchColor(isActive: Boolean) {
        val color = if (isActive) ContextCompat.getColor(requireContext(), R.color.brand_primary) else Color.parseColor("#D0D0D0")
        binding.switchActive.trackTintList = ColorStateList.valueOf(color)
    }

    private fun setupScheduleTypeCards() {
        updateScheduleCardUI(isFixedScheduleSelected)

        binding.cardFlexible.setOnClickListener {
            isFixedScheduleSelected = false
            updateScheduleCardUI(false)
        }
        binding.cardFixed.setOnClickListener {
            isFixedScheduleSelected = true
            updateScheduleCardUI(true)
        }
    }

    private fun updateScheduleCardUI(isFixed: Boolean) {
        val brandColor = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val grayColor = Color.parseColor("#AAAAAA")
        val activeBg = Color.parseColor("#EDF1FF")
        val inactiveBg = Color.parseColor("#F5F5F7")
        val activeStroke = brandColor
        val inactiveStroke = Color.parseColor("#DEDEDE")

        android.transition.TransitionManager.beginDelayedTransition(binding.root as android.view.ViewGroup)

        if (isFixed) {
            // ── Fixed active ────────────────────────────────────
            binding.cardFixed.setCardBackgroundColor(activeBg)
            binding.cardFixed.strokeColor = activeStroke
            binding.tvFixedLabel.setTextColor(brandColor)
            binding.tvFixedSubLabel.setTextColor(brandColor)
            binding.tvFixedSubLabel.alpha = 0.6f
            binding.ivFixedIcon.imageTintList = android.content.res.ColorStateList.valueOf(brandColor)

            // Rolling inactive
            binding.cardFlexible.setCardBackgroundColor(inactiveBg)
            binding.cardFlexible.strokeColor = inactiveStroke
            binding.ivFlexibleIcon.imageTintList = android.content.res.ColorStateList.valueOf(grayColor)

            // Description box → Fixed style (orange/warm)
            binding.llScheduleDescription.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
            binding.tvScheduleDescIcon.text = "📌"
            binding.tvScheduleDescTitle.text = "Fixed"
            binding.tvScheduleDescTitle.setTextColor(Color.parseColor("#E67E22"))
            binding.tvScheduleDescBody.text =
                "Next schedule = Previous Schedule + Interval. Schedule does not shift even if service is late/early."
            binding.tvScheduleDescBody.setTextColor(Color.parseColor("#C0622D"))

        } else {
            // ── Rolling active ───────────────────────────────────
            binding.cardFlexible.setCardBackgroundColor(activeBg)
            binding.cardFlexible.strokeColor = activeStroke
            binding.ivFlexibleIcon.imageTintList = android.content.res.ColorStateList.valueOf(brandColor)

            // Fixed inactive
            binding.cardFixed.setCardBackgroundColor(inactiveBg)
            binding.cardFixed.strokeColor = inactiveStroke
            binding.tvFixedLabel.setTextColor(grayColor)
            binding.tvFixedSubLabel.setTextColor(Color.parseColor("#BBBBBB"))
            binding.tvFixedSubLabel.alpha = 1f
            binding.ivFixedIcon.imageTintList = android.content.res.ColorStateList.valueOf(grayColor)

            // Description box → Rolling style (blue/brand)
            binding.llScheduleDescription.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#EEF2FF"))
            binding.tvScheduleDescIcon.text = "🔄"
            binding.tvScheduleDescTitle.text = "Rolling"
            binding.tvScheduleDescTitle.setTextColor(brandColor)
            binding.tvScheduleDescBody.text =
                "Next schedule = Last Service Date + Interval. Schedule follows when you perform service."
            binding.tvScheduleDescBody.setTextColor(brandColor)
        }
    }

    private fun setupEditMode() {
        binding.btnSave.text = "Update Item"
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            itemToEdit = items.find { it.id == args.itemId }
            itemToEdit?.let { item ->
                binding.etName.setText(item.name)
                binding.spinnerCategory.setText(item.category, false)
                binding.etIntervalValue.setText(item.serviceIntervalValue.toString())
                
                if (item.serviceIntervalUnit.equals("Months", ignoreCase = true)) {
                    binding.toggleUnit.check(R.id.btnUnitMonths)
                } else {
                    binding.toggleUnit.check(R.id.btnUnitDays)
                }
                binding.etNote.setText(item.note)
                binding.switchActive.isChecked = item.isActive
                isFixedScheduleSelected = item.isFixedSchedule
                selectedIcon = item.icon
                setupIconSelection()
                binding.root.post { updateScheduleCardUI(item.isFixedSchedule) }
                val costFormatted = if (item.estimatedCost > 0) {
                    java.text.NumberFormat.getInstance(java.util.Locale("in", "ID"))
                        .format(item.estimatedCost.toLong())
                } else ""
                binding.etEstimatedCost.setText(costFormatted)

                selectedDate = item.lastServiceDate
                binding.etLastServiceDate.setText(DateUtil.formatDate(selectedDate))
            }
        }
    }

    private fun setupSpinners() {
        val categories = arrayOf("AC", "Vehicle", "Electronics", "Machine", "Home Appliance", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(categoryAdapter)
    }

    private fun setupDatePicker() {
        binding.etLastServiceDate.setText(DateUtil.formatDate(selectedDate))
        binding.etLastServiceDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.timeInMillis
                    binding.etLastServiceDate.setText(DateUtil.formatDate(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun saveItem() {
        val name = binding.etName.text.toString().trim()
        val category = binding.spinnerCategory.text.toString()
        val intervalValueStr = binding.etIntervalValue.text.toString()
        val intervalUnit = if (binding.toggleUnit.checkedButtonId == R.id.btnUnitMonths) "Months" else "Days"
        val note = binding.etNote.text.toString()

        if (name.isEmpty() || category.isEmpty() || intervalValueStr.isEmpty() || intervalUnit.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val intervalValue = intervalValueStr.toInt()
        val nextServiceDate = DateUtil.getNextServiceDate(selectedDate, intervalValue, intervalUnit)
        val estimatedCost = CurrencyTextWatcher.getRawValue(binding.etEstimatedCost)

        if (isEditMode && itemToEdit != null) {
            val updatedItem = itemToEdit!!.copy(
                name = name,
                category = category,
                lastServiceDate = selectedDate,
                serviceIntervalValue = intervalValue,
                serviceIntervalUnit = intervalUnit,
                nextServiceDate = nextServiceDate,
                note = note,
                estimatedCost = estimatedCost,
                isActive = binding.switchActive.isChecked,
                isFixedSchedule = isFixedScheduleSelected,
                icon = selectedIcon
            )
            viewModel.updateItem(updatedItem)
            Toast.makeText(requireContext(), "Item updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            val newItem = Item(
                name = name,
                category = category,
                lastServiceDate = selectedDate,
                originalLastServiceDate = selectedDate,
                serviceIntervalValue = intervalValue,
                serviceIntervalUnit = intervalUnit,
                nextServiceDate = nextServiceDate,
                note = note,
                estimatedCost = estimatedCost,
                isActive = binding.switchActive.isChecked,
                isFixedSchedule = isFixedScheduleSelected,
                icon = selectedIcon
            )
            viewModel.insertItem(newItem)
            Toast.makeText(requireContext(), "Item saved successfully", Toast.LENGTH_SHORT).show()
        }

        // Munculkan iklan jika jumlah perangkat sudah 4 atau lebih (berlaku untuk simpan baru maupun update)
        if (deviceCount >= 4) {
            mInterstitialAd?.show(requireActivity())
        }

        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
