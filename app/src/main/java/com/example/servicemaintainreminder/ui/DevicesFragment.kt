package com.example.servicemaintainreminder.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
    private val args: DevicesFragmentArgs by navArgs()
    private lateinit var adapter: ItemAdapterVertical
    
    private var allItemsList: List<Item> = emptyList()
    private var selectedHistoryDate: Long = System.currentTimeMillis()
    private var currentSortMode = SortMode.NAME_ASC

    enum class SortMode {
        NAME_ASC, DATE_ASC, DATE_DESC
    }

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
        setupFilters()
        setupSwipeToDelete()

        // Setup header back button & title
        binding.ivBackButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.tvHeaderTitle.text = "My Devices"

        binding.fabAddDevice.setOnClickListener {
            findNavController().navigate(R.id.action_devicesFragment_to_addItemFragment)
        }
    }

    private fun setupFilters() {
        when (args.filterType) {
            "Upcoming" -> binding.chipGroupFilters.check(R.id.chipUpcoming)
            "Overdue" -> binding.chipGroupFilters.check(R.id.chipOverdue)
            else -> binding.chipGroupFilters.check(R.id.chipAll)
        }

        binding.chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
            applyFilters(checkedId, binding.searchViewDevices.query?.toString())
        }

        binding.btnSort.setOnClickListener {
            val options = arrayOf("Name (A-Z)", "Closest Service Date", "Furthest Service Date")
            val checkedItem = when (currentSortMode) {
                SortMode.NAME_ASC -> 0
                SortMode.DATE_ASC -> 1
                SortMode.DATE_DESC -> 2
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    currentSortMode = when (which) {
                        0 -> SortMode.NAME_ASC
                        1 -> SortMode.DATE_ASC
                        2 -> SortMode.DATE_DESC
                        else -> SortMode.NAME_ASC
                    }
                    applyFilters(binding.chipGroupFilters.checkedChipId, binding.searchViewDevices.query?.toString())
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun applyFilters(checkedChipId: Int, searchQuery: String?) {
        var filteredList = allItemsList

        // 1. Filter by Status Chip
        filteredList = when (checkedChipId) {
            R.id.chipUpcoming -> filteredList.filter {
                val diff = it.nextServiceDate - System.currentTimeMillis()
                it.isActive && diff in 0..(7 * 24 * 60 * 60 * 1000L) // Next 7 days
            }
            R.id.chipOverdue -> filteredList.filter {
                it.isActive && it.nextServiceDate < System.currentTimeMillis()
            }
            else -> filteredList // All
        }

        // 2. Filter by Search Query
        if (!searchQuery.isNullOrEmpty()) {
            filteredList = filteredList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }

        // 3. Apply Current Sort
        val finalSortedList = when (currentSortMode) {
            SortMode.NAME_ASC -> filteredList.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortMode.DATE_ASC -> filteredList.sortedBy { it.nextServiceDate }
            SortMode.DATE_DESC -> filteredList.sortedByDescending { it.nextServiceDate }
        }

        adapter.submitList(finalSortedList)
        binding.llEmptyState.isVisible = finalSortedList.isEmpty()
    }

    private fun setupObservers() {
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            allItemsList = items
            applyFilters(binding.chipGroupFilters.checkedChipId, binding.searchViewDevices.query?.toString())
        }
    }

    private fun setupSearchView() {
        binding.searchViewDevices.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters(binding.chipGroupFilters.checkedChipId, newText)
                return true
            }
        })
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
            private val background = ColorDrawable(Color.parseColor("#FFE5E5"))
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
            private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0

            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                showDeleteConfirmationDialog(item, position)
            }

            override fun onChildDraw(c: Canvas, r: RecyclerView, v: RecyclerView.ViewHolder, dX: Float, dY: Float, s: Int, a: Boolean) {
                val itemView = v.itemView
                val itemHeight = itemView.bottom - itemView.top
                background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                background.draw(c)
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
                super.onChildDraw(c, r, v, dX, dY, s, a)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvAllDevices)
    }

    private fun showDeleteConfirmationDialog(item: Item, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Perangkat")
            .setMessage("Apakah Anda yakin ingin menghapus '${item.name}'?")
            .setPositiveButton("Hapus") { _, _ -> viewModel.deleteItem(item) }
            .setNegativeButton("Batal") { dialog, _ ->
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }.show()
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
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val selCal = Calendar.getInstance()
                selCal.set(year, month, day)
                selectedHistoryDate = selCal.timeInMillis
                etDate.setText(DateUtil.formatDate(selectedHistoryDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Catatan Servis")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val desc = etDesc.text.toString()
                val costStr = etCost.text.toString()
                if (desc.isNotEmpty() && costStr.isNotEmpty()) saveHistory(item, desc, costStr.toDouble(), selectedHistoryDate)
            }
            .setNegativeButton("Batal", null).show()
    }

    private fun saveHistory(item: Item, description: String, cost: Double, date: Long) {
        val history = ServiceHistory(itemId = item.id, serviceDate = date, description = description, cost = cost)
        viewModel.addHistory(history)
        if (date >= item.lastServiceDate) {
            val nextDate = DateUtil.getNextServiceDate(date, item.serviceIntervalValue, item.serviceIntervalUnit)
            viewModel.updateItem(item.copy(lastServiceDate = date, nextServiceDate = nextDate))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
