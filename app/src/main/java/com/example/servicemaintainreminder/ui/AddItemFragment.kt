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

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private val args: AddItemFragmentArgs by navArgs()
    private var selectedDate: Long = System.currentTimeMillis()
    private var isEditMode = false
    private var itemToEdit: Item? = null
    private var isFixedScheduleSelected = false  // false = Flexible (default)

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

        isEditMode = args.itemId != -1L

        // Setup header back button & title
        binding.header.ivBackButton.setOnClickListener {
            findNavController().navigateUp()
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

        // Buat animasi kemunculan/menghilang menjadi smooth
        android.transition.TransitionManager.beginDelayedTransition(binding.root as android.view.ViewGroup)
        
        // Tampilkan/sembunyikan pesan peringatan di bawah field Last Service Date
        binding.llFixedDateHint.visibility = if (isFixed) View.VISIBLE else View.GONE

        if (isFixed) {
            // Fixed → aktif
            binding.cardFixed.setCardBackgroundColor(activeBg)
            binding.cardFixed.strokeColor = activeStroke
            binding.tvFixedLabel.setTextColor(brandColor)
            binding.tvFixedSubLabel.setTextColor(brandColor)
            binding.tvFixedSubLabel.alpha = 0.6f
            binding.ivFixedIcon.imageTintList = android.content.res.ColorStateList.valueOf(brandColor)
            // Flexible → tidak aktif
            binding.cardFlexible.setCardBackgroundColor(inactiveBg)
            binding.cardFlexible.strokeColor = inactiveStroke
            binding.ivFlexibleIcon.imageTintList = android.content.res.ColorStateList.valueOf(grayColor)
        } else {
            // Flexible → aktif
            binding.cardFlexible.setCardBackgroundColor(activeBg)
            binding.cardFlexible.strokeColor = activeStroke
            binding.ivFlexibleIcon.imageTintList = android.content.res.ColorStateList.valueOf(brandColor)
            // Fixed → tidak aktif
            binding.cardFixed.setCardBackgroundColor(inactiveBg)
            binding.cardFixed.strokeColor = inactiveStroke
            binding.tvFixedLabel.setTextColor(grayColor)
            binding.tvFixedSubLabel.setTextColor(Color.parseColor("#BBBBBB"))
            binding.tvFixedSubLabel.alpha = 1f
            binding.ivFixedIcon.imageTintList = android.content.res.ColorStateList.valueOf(grayColor)
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
                binding.spinnerIntervalUnit.setText(item.serviceIntervalUnit, false)
                binding.etNote.setText(item.note)
                binding.switchActive.isChecked = item.isActive
                isFixedScheduleSelected = item.isFixedSchedule
                // panggil setelah view siap
                binding.root.post { updateScheduleCardUI(item.isFixedSchedule) }
                // Prefill cost dengan format titik ribuan
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

        val units = arrayOf("Days", "Months")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.spinnerIntervalUnit.setAdapter(unitAdapter)
        
        // Default ke "Days". Jika sedang edit mode, ini akan ditimpa di setupEditMode()
        binding.spinnerIntervalUnit.setText(units[0], false)
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
        val intervalUnit = binding.spinnerIntervalUnit.text.toString()
        val note = binding.etNote.text.toString()
        val costStr = binding.etEstimatedCost.text.toString().trim()

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
                isFixedSchedule = isFixedScheduleSelected
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
                isFixedSchedule = isFixedScheduleSelected
            )
            viewModel.insertItem(newItem)
            Toast.makeText(requireContext(), "Item saved successfully", Toast.LENGTH_SHORT).show()
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
