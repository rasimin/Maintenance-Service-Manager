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
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.databinding.FragmentAddItemBinding
import com.example.servicemaintainreminder.util.DateUtil
import java.util.*

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private var selectedDate: Long = System.currentTimeMillis()

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

        binding.btnSave.setOnClickListener {
            saveItem()
        }
    }

    private fun setupSpinners() {
        val categories = arrayOf("AC", "Kendaraan", "Elektronik", "Mesin", "Rumah Tangga", "Lainnya")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(categoryAdapter)

        val units = arrayOf("Days", "Months")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.spinnerIntervalUnit.setAdapter(unitAdapter)
    }

    private fun setupDatePicker() {
        binding.etLastServiceDate.setText(DateUtil.formatDate(selectedDate))
        binding.etLastServiceDate.setOnClickListener {
            val calendar = Calendar.getInstance()
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

        if (name.isEmpty() || category.isEmpty() || intervalValueStr.isEmpty() || intervalUnit.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val intervalValue = intervalValueStr.toInt()
        val nextServiceDate = DateUtil.getNextServiceDate(selectedDate, intervalValue, intervalUnit)

        val item = Item(
            name = name,
            category = category,
            lastServiceDate = selectedDate,
            serviceIntervalValue = intervalValue,
            serviceIntervalUnit = intervalUnit,
            nextServiceDate = nextServiceDate,
            note = note
        )

        viewModel.insertItem(item)
        Toast.makeText(requireContext(), "Item saved successfully", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
