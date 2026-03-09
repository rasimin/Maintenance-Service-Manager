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
import android.view.MotionEvent
import android.content.res.ColorStateList
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
import com.example.servicemaintainreminder.databinding.FragmentDetailBinding
import com.example.servicemaintainreminder.util.DateUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.text.NumberFormat
import java.util.*

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter
    private var mInterstitialAd: InterstitialAd? = null
    private var currentItem: Item? = null
    private var selectedHistoryDate: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToDelete()
        loadInterstitialAd()
        observeItem()
        setupPullToEdit()

        // Setup header back button & title
        binding.header.ivBackButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.header.tvHeaderTitle.text = "Detail"

        binding.btnAddHistory.setOnClickListener {
            showAddHistoryDialog()
        }
        binding.fabAddHistoryMini.setOnClickListener {
            showAddHistoryDialog()
        }

        binding.nestedScrollViewDetail.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Munculkan Mini FAB jika di-scroll ke bawah lebih dari 400px
            if (scrollY > 400) {
                binding.fabAddHistoryMini.show()
            } else {
                binding.fabAddHistoryMini.hide()
            }
        }

        binding.swActiveDetail.setOnCheckedChangeListener { _, isChecked ->
            currentItem?.let { item ->
                if (item.isActive != isChecked) {
                    val updatedItem = item.copy(isActive = isChecked)
                    viewModel.updateItem(updatedItem)
                    Toast.makeText(requireContext(), "Device status updated", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnEditDetail.setOnClickListener {
            currentItem?.let { item ->
                val action = DetailFragmentDirections.actionDetailFragmentToAddItemFragment(item.id)
                findNavController().navigate(action)
            }
        }
    }

    private fun setupPullToEdit() {
        var startY = 0f
        var isSwiping = false
        val threshold = 250f // px

        binding.nestedScrollViewDetail.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (binding.nestedScrollViewDetail.scrollY == 0) {
                        startY = event.y
                        isSwiping = true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isSwiping && event.y > startY && binding.nestedScrollViewDetail.scrollY == 0) {
                        val deltaY = event.y - startY
                        val dragY = deltaY / 2.5f

                        if (dragY > 0) {
                            binding.nestedScrollViewDetail.translationY = dragY
                            val alpha = (dragY / threshold).coerceIn(0f, 1f)
                            binding.llSwipeEditBg.alpha = alpha

                            if (dragY > threshold) {
                                binding.tvSwipeEditText.text = "Release to Edit Device"
                            } else {
                                binding.tvSwipeEditText.text = "Pull down to Edit Device"
                            }
                            return@setOnTouchListener true
                        }
                    } else if (isSwiping && event.y < startY) {
                         isSwiping = false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isSwiping) {
                        val deltaY = event.y - startY
                        val dragY = deltaY / 2.5f

                        if (dragY > threshold) {
                            currentItem?.let { item ->
                                val action = DetailFragmentDirections.actionDetailFragmentToAddItemFragment(item.id)
                                findNavController().navigate(action)
                            }
                        }

                        binding.nestedScrollViewDetail.animate().translationY(0f).setDuration(250).start()
                        binding.llSwipeEditBg.animate().alpha(0f).setDuration(250).start()
                        isSwiping = false
                    }
                }
            }
            false
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
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

    private fun observeItem() {
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            currentItem = items.find { it.id == args.itemId }
            currentItem?.let { item ->
                bindItemDetails(item)
                observeHistory(item.id)
            }
        }
    }

    private fun bindItemDetails(item: Item) {
        binding.tvDetailName.text = item.name
        binding.tvDetailCategory.text = item.category
        binding.tvDetailNextService.text = DateUtil.formatDate(item.nextServiceDate)
        binding.tvDetailLastService.text = DateUtil.formatDate(item.lastServiceDate)
        binding.tvDetailInterval.text = "${item.serviceIntervalValue} ${item.serviceIntervalUnit}"
        
        val format = NumberFormat.getInstance(Locale("in", "ID"))
        binding.tvDetailEstimatedCost.text = if (item.estimatedCost > 0) "Rp ${format.format(item.estimatedCost.toLong())}" else "Rp 0"
        
        binding.tvDetailNote.text = item.note.ifEmpty { "No notes added" }

        // Update switch without triggering listener
        binding.swActiveDetail.setOnCheckedChangeListener(null)
        binding.swActiveDetail.isChecked = item.isActive
        updateSwitchColor(item.isActive)
        
        binding.swActiveDetail.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchColor(isChecked)
            if (item.isActive != isChecked) {
                viewModel.updateItem(item.copy(isActive = isChecked))
                Toast.makeText(requireContext(), "Device status updated", Toast.LENGTH_SHORT).show()
            }
        }

        updateStatusIndicator(item.nextServiceDate)
    }

    private fun updateSwitchColor(isActive: Boolean) {
        val color = if (isActive) ContextCompat.getColor(requireContext(), R.color.brand_primary) else Color.parseColor("#D0D0D0")
        binding.swActiveDetail.trackTintList = ColorStateList.valueOf(color)
    }

    private fun updateStatusIndicator(nextDate: Long) {
        val currentTime = System.currentTimeMillis()
        val daysDiff = (nextDate - currentTime) / (24 * 60 * 60 * 1000)

        when {
            daysDiff < 0 -> {
                binding.tvStatusIndicator.text = "❗ Overdue"
                binding.tvStatusIndicator.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error))
                binding.flDetailStatusHeader.setBackgroundColor(Color.parseColor("#1AE74C3C"))
            }
            daysDiff <= 7 -> {
                binding.tvStatusIndicator.text = "⚠ Maintenance Soon"
                binding.tvStatusIndicator.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
                binding.flDetailStatusHeader.setBackgroundColor(Color.parseColor("#1AF5A623"))
            }
            else -> {
                binding.tvStatusIndicator.text = "✅ Scheduled"
                binding.tvStatusIndicator.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_safe))
                binding.flDetailStatusHeader.setBackgroundColor(Color.parseColor("#1A2ECC71"))
            }
        }
    }

    private fun observeHistory(itemId: Long) {
        viewModel.getHistory(itemId).observe(viewLifecycleOwner) { history ->
            historyAdapter.submitList(history)

            binding.llEmptyHistory.isVisible = history.isEmpty()
            binding.rvHistory.isVisible = history.isNotEmpty()

            val totalCost = history.sumOf { it.cost }
            val format = NumberFormat.getInstance(Locale("in", "ID"))
            binding.tvDetailTotalCost.text = "Rp ${format.format(totalCost.toLong())}"
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.adapter = historyAdapter
    }

    // ── Swipe to Delete ──────────────────────────────────────────────────────
    private fun setupSwipeToDelete() {
        val deleteBackground = ColorDrawable(Color.parseColor("#E74C3C"))
        val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
        val iconMarginDp = (24 * resources.displayMetrics.density).toInt()

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val history = historyAdapter.currentList[position]
                showDeleteConfirmDialog(history, position)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isActive: Boolean
            ) {
                val itemView = vh.itemView
                val itemHeight = itemView.bottom - itemView.top

                if (dX > 0) {
                    // Red background
                    deleteBackground.setBounds(
                        itemView.left, itemView.top,
                        itemView.left + dX.toInt(), itemView.bottom
                    )
                    deleteBackground.draw(c)

                    // Trash icon
                    deleteIcon?.let { icon ->
                        val iconHeight = icon.intrinsicHeight
                        val iconWidth = icon.intrinsicWidth
                        val iconTop = itemView.top + (itemHeight - iconHeight) / 2
                        val iconBottom = iconTop + iconHeight
                        val iconLeft = itemView.left + iconMarginDp
                        val iconRight = iconLeft + iconWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.setTint(Color.WHITE)
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvHistory)
    }

    private fun showDeleteConfirmDialog(history: ServiceHistory, position: Int) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_history_confirm, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvDesc = dialogView.findViewById<TextView>(R.id.tvDeleteHistoryDesc)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDeleteHistoryCancel)
        val btnDelete = dialogView.findViewById<View>(R.id.btnDeleteHistoryConfirm)

        tvDesc.text = history.description

        btnDelete.setOnClickListener {
            viewModel.deleteHistory(history)
            
            // Recalculate schedule if this was the latest history
            currentItem?.let { item ->
                // Find remaining histories excluding the one being deleted
                val remainingHistory = historyAdapter.currentList.filter { it.id != history.id }
                if (remainingHistory.isNotEmpty()) {
                    // Find the most recent date from the remaining history
                    val latestHistoryDate = remainingHistory.maxOf { it.serviceDate }
                    
                    // If the current item's service date logic was driven by this maxDate, update it
                    val nextDate = DateUtil.getNextServiceDate(latestHistoryDate, item.serviceIntervalValue, item.serviceIntervalUnit)
                    viewModel.updateItem(item.copy(
                        lastServiceDate = latestHistoryDate, 
                        nextServiceDate = nextDate
                    ))
                } else {
                    // Revert to originalLastServiceDate since history is empty
                    val originalNextDate = DateUtil.getNextServiceDate(item.originalLastServiceDate, item.serviceIntervalValue, item.serviceIntervalUnit)
                    viewModel.updateItem(item.copy(
                        lastServiceDate = item.originalLastServiceDate,
                        nextServiceDate = originalNextDate
                    ))
                }
            }

            Toast.makeText(requireContext(), "Riwayat dihapus", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            historyAdapter.notifyItemChanged(position)
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            historyAdapter.notifyItemChanged(position)
        }

        dialog.show()
    }

    // ── Add History Dialog ───────────────────────────────────────────────────
    private fun showAddHistoryDialog() {
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
                val selected = Calendar.getInstance()
                selected.set(year, month, dayOfMonth)
                selectedHistoryDate = selected.timeInMillis
                etDate.setText(DateUtil.formatDate(selectedHistoryDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Catatan Servis")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val desc = etDesc.text.toString()
                val costStr = etCost.text.toString()
                if (desc.isNotEmpty() && costStr.isNotEmpty()) {
                    saveHistory(desc, costStr.toDouble(), selectedHistoryDate)
                } else {
                    Toast.makeText(requireContext(), "Isi semua field terlebih dahulu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveHistory(description: String, cost: Double, date: Long) {
        val history = ServiceHistory(
            itemId = args.itemId,
            serviceDate = date,
            description = description,
            cost = cost
        )
        viewModel.addHistory(history)

        currentItem?.let { item ->
            if (date >= item.lastServiceDate) {
                val nextDate = DateUtil.getNextServiceDate(date, item.serviceIntervalValue, item.serviceIntervalUnit)
                viewModel.updateItem(item.copy(lastServiceDate = date, nextServiceDate = nextDate))
            }
        }

        Toast.makeText(requireContext(), "Catatan servis disimpan ✓", Toast.LENGTH_SHORT).show()

        mInterstitialAd?.show(requireActivity())
        loadInterstitialAd()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
