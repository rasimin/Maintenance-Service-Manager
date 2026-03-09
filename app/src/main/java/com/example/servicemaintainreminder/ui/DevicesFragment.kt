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
import com.example.servicemaintainreminder.util.CurrencyTextWatcher
import com.example.servicemaintainreminder.util.ModernMenuItem
import com.example.servicemaintainreminder.util.ModernMenuUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import android.graphics.Paint
import android.graphics.Typeface
import java.text.NumberFormat
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
    private var savedScrollY: Int = 0

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
        setupSwipeActions()

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

        adapter.submitList(finalSortedList) {
            if (savedScrollY > 0) {
                binding.nestedScrollViewDevices.post {
                    binding.nestedScrollViewDevices.scrollTo(0, savedScrollY)
                    savedScrollY = 0
                }
            }
        }
        binding.llEmptyState.isVisible = finalSortedList.isEmpty()
        
        binding.tvSwipeHint.isVisible = (checkedChipId == R.id.chipUpcoming || checkedChipId == R.id.chipOverdue) && finalSortedList.isNotEmpty()
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
            onMoreOptionsClick = { item, view ->
                val menuItems = listOf(
                    ModernMenuItem(1, "Edit", R.drawable.ic_input_edit),
                    ModernMenuItem(2, "Quick Done", android.R.drawable.ic_menu_myplaces),
                    ModernMenuItem(3, "Add Service", android.R.drawable.ic_menu_add),
                    ModernMenuItem(4, "Delete", android.R.drawable.ic_menu_delete, Color.parseColor("#E74C3C"))
                )

                ModernMenuUtil.showMenu(requireContext(), view, menuItems) { selectedId ->
                    when (selectedId) {
                        1 -> {
                            val action = DevicesFragmentDirections.actionDevicesFragmentToAddItemFragment(item.id)
                            findNavController().navigate(action)
                        }
                        2 -> {
                            // Check if Upcoming or Overdue
                            if (item.nextServiceDate <= System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)) {
                                val position = adapter.currentList.indexOf(item)
                                showAddHistoryConfirmDialog(item, position)
                            } else {
                                Toast.makeText(requireContext(), "Quick Done is only for Upcoming/Overdue devices", Toast.LENGTH_SHORT).show()
                            }
                        }
                        3 -> {
                            showAddHistoryDialog(item)
                        }
                        4 -> {
                            val position = adapter.currentList.indexOf(item)
                            showDeleteConfirmationDialog(item, position)
                        }
                    }
                }
            }
        )
        binding.rvAllDevices.adapter = adapter
    }

    private fun setupSwipeActions() {
        val textPaint = Paint().apply {
            color = Color.parseColor("#27AE60")
            textSize = 14f * resources.displayMetrics.scaledDensity
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            private val bgDelete = ColorDrawable(Color.parseColor("#FFE5E5"))
            private val bgDone = ColorDrawable(Color.parseColor("#E8F8F0"))
            private val iconDelete = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)

            override fun getSwipeDirs(r: RecyclerView, v: RecyclerView.ViewHolder): Int {
                val position = v.adapterPosition
                val item = adapter.currentList[position]
                val diff = item.nextServiceDate - System.currentTimeMillis()
                val isUpcomingOrOverdue = item.isActive && diff <= (7L * 24L * 60L * 60L * 1000L)
                return if (isUpcomingOrOverdue) {
                    ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
                } else {
                    ItemTouchHelper.RIGHT
                }
            }

            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                if (direction == ItemTouchHelper.RIGHT) {
                    showDeleteConfirmationDialog(item, position)
                } else if (direction == ItemTouchHelper.LEFT) {
                    showAddHistoryConfirmDialog(item, position)
                }
            }

            override fun onChildDraw(c: Canvas, r: RecyclerView, v: RecyclerView.ViewHolder, dX: Float, dY: Float, s: Int, a: Boolean) {
                val itemView = v.itemView
                val itemHeight = itemView.bottom - itemView.top

                if (dX > 0) { // Swipe Right for Delete
                    bgDelete.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    bgDelete.draw(c)
                    
                    iconDelete?.let {
                        val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(Color.parseColor("#E74C3C"))
                        it.draw(c)
                    }
                } else if (dX < 0) { // Swipe Left for Quick Done
                    bgDone.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    bgDone.draw(c)

                    val text = "Quick Done ✅"
                    val textMargin = (24 * resources.displayMetrics.density).toInt()
                    val textX = itemView.right - textMargin.toFloat()
                    val textY = itemView.top + (itemHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                    
                    if (Math.abs(dX) > textPaint.measureText(text) + textMargin) {
                        c.drawText(text, textX, textY, textPaint)
                    }
                }
                super.onChildDraw(c, r, v, dX, dY, s, a)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvAllDevices)
    }

    private fun showAddHistoryConfirmDialog(item: Item, position: Int) {
        val format = NumberFormat.getInstance(Locale("in", "ID"))
        val costText = if (item.estimatedCost > 0) "Rp ${format.format(item.estimatedCost.toLong())}" else "Rp 0"

        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_quick_done, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvName = dialogView.findViewById<TextView>(R.id.tvQuickDeviceName)
        val tvDate = dialogView.findViewById<TextView>(R.id.tvQuickDate)
        val tvCost = dialogView.findViewById<TextView>(R.id.tvQuickCost)
        val btnCancel = dialogView.findViewById<View>(R.id.btnQuickCancel)
        val btnSave = dialogView.findViewById<View>(R.id.btnQuickSave)

        tvName.text = item.name
        tvDate.text = DateUtil.formatDate(System.currentTimeMillis())
        tvCost.text = costText

        btnSave.setOnClickListener {
            val date = System.currentTimeMillis()
            val history = ServiceHistory(
                itemId = item.id,
                serviceDate = date,
                description = "Auto done by swipe",
                cost = item.estimatedCost
            )
            viewModel.addHistory(history)
            val nextDate = DateUtil.getNextServiceDate(date, item.serviceIntervalValue, item.serviceIntervalUnit)
            viewModel.updateItem(item.copy(lastServiceDate = date, nextServiceDate = nextDate))
            Toast.makeText(requireContext(), "Service record added automatically", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            adapter.notifyItemChanged(position)
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            adapter.notifyItemChanged(position)
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(item: Item, position: Int) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirm, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvName = dialogView.findViewById<TextView>(R.id.tvDeleteDeviceName)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDeleteCancel)
        val btnDelete = dialogView.findViewById<View>(R.id.btnDeleteConfirm)

        tvName.text = item.name

        btnDelete.setOnClickListener {
            viewModel.deleteItem(item)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            adapter.notifyItemChanged(position)
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            adapter.notifyItemChanged(position)
        }

        dialog.show()
    }

    private fun showAddHistoryDialog(item: Item) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_history, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etDesc = dialogView.findViewById<EditText>(R.id.etHistoryDesc)
        val etCost = dialogView.findViewById<EditText>(R.id.etHistoryCost)
        val etDate = dialogView.findViewById<EditText>(R.id.etHistoryDate)
        val btnSave = dialogView.findViewById<View>(R.id.btnSaveHistory)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancelHistory)

        // Format otomatis titik ribuan
        CurrencyTextWatcher.attach(etCost)
        
        selectedHistoryDate = System.currentTimeMillis()
        etDate.setText(DateUtil.formatDate(selectedHistoryDate))
        
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedHistoryDate
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val selCal = Calendar.getInstance()
                selCal.set(year, month, day)
                selectedHistoryDate = selCal.timeInMillis
                etDate.setText(DateUtil.formatDate(selectedHistoryDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val desc = etDesc.text.toString()
            if (desc.isNotEmpty()) {
                val cost = CurrencyTextWatcher.getRawValue(etCost)
                saveHistory(item, desc, cost, selectedHistoryDate)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
        savedScrollY = binding.nestedScrollViewDevices.scrollY
        super.onDestroyView()
        _binding = null
    }
}
