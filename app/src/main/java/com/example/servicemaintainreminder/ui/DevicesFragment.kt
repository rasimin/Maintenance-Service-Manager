package com.example.servicemaintainreminder.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.data.ServiceHistory
import com.example.servicemaintainreminder.databinding.FragmentDevicesBinding
import com.example.servicemaintainreminder.util.DateUtil
import java.util.*

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ItemAdapterVertical
    private var selectedHistoryDate: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupSwipeToDelete()
        
        binding.fabAddDevice.setOnClickListener {
            // Add scale animation before navigating
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                    findNavController().navigate(R.id.action_devicesFragment_to_addItemFragment)
                }.start()
            }.start()
        }
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapterVertical(
            onItemClick = { item ->
                val action = DevicesFragmentDirections.actionDevicesFragmentToDetailFragment(item.id)
                findNavController().navigate(action)
            },
            onEditClick = { item ->
                val action = DevicesFragmentDirections.actionDevicesFragmentToAddItemFragment(item.id)
                findNavController().navigate(action)
            },
            onAddRecordClick = { item ->
                showAddHistoryDialog(item)
            }
        )
        binding.rvAllDevices.adapter = adapter
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private val background = ColorDrawable(Color.parseColor("#FFE5E5")) // Soft red background
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
            private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                
                showDeleteConfirmationDialog(item, position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                // Draw background
                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                background.draw(c)

                // Calculate icon position
                if (dX > 0) {
                    val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                    val iconMargin = (itemHeight - intrinsicHeight) / 2
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + intrinsicWidth
                    val iconBottom = iconTop + intrinsicHeight

                    deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon?.setTint(Color.parseColor("#E74C3C"))
                    deleteIcon?.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvAllDevices)
    }

    private fun showDeleteConfirmationDialog(item: Item, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Device")
            .setMessage("Are you sure you want to delete '${item.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteItem(item)
                Toast.makeText(requireContext(), "${item.name} deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showAddHistoryDialog(item: Item) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_history, null)
        val etDesc = dialogView.findViewById<EditText>(R.id.etHistoryDesc)
        val etCost = dialogView.findViewById<EditText>(R.id.etHistoryCost)
        val etDate = dialogView.findViewById<EditText>(R.id.etHistoryDate)
        
        selectedHistoryDate = System.currentTimeMillis()
        etDate.setText(DateUtil.formatDate(selectedHistoryDate))
        
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedHistoryDate
            
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                selectedHistoryDate = selectedCalendar.timeInMillis
                etDate.setText(DateUtil.formatDate(selectedHistoryDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Maintenance Record")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val desc = etDesc.text.toString()
                val costStr = etCost.text.toString()
                if (desc.isNotEmpty() && costStr.isNotEmpty()) {
                    saveHistory(item, desc, costStr.toDouble(), selectedHistoryDate)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveHistory(item: Item, description: String, cost: Double, date: Long) {
        val history = ServiceHistory(
            itemId = item.id,
            serviceDate = date,
            description = description,
            cost = cost
        )
        viewModel.addHistory(history)
        
        if (date >= item.lastServiceDate) {
            val nextDate = DateUtil.getNextServiceDate(
                date,
                item.serviceIntervalValue,
                item.serviceIntervalUnit
            )
            val updatedItem = item.copy(
                lastServiceDate = date,
                nextServiceDate = nextDate
            )
            viewModel.updateItem(updatedItem)
        }
        
        Toast.makeText(requireContext(), "Record added", Toast.LENGTH_SHORT).show()
    }

    private fun setupSearchView() {
        // Smooth focus animation for search box
        binding.searchViewDevices.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
        }

        binding.searchViewDevices.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadAllDevices()
                } else {
                    viewModel.searchItems(newText).observe(viewLifecycleOwner) { items ->
                        adapter.submitList(items.sortedBy { it.name })
                    }
                }
                return true
            }
        })
    }

    private fun setupObservers() {
        loadAllDevices()
    }

    private fun loadAllDevices() {
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            // Only update if search is empty to avoid overwriting search results
            if (binding.searchViewDevices.query.isNullOrEmpty()) {
                adapter.submitList(items.sortedBy { it.name })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
