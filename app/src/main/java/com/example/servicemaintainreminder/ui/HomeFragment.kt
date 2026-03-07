package com.example.servicemaintainreminder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.data.ServiceHistory
import com.example.servicemaintainreminder.databinding.FragmentHomeBinding
import com.example.servicemaintainreminder.util.DateUtil
import com.google.android.gms.ads.AdRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.LinearLayout
import android.widget.TextView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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
        loadAds()
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter { item ->
            val action = HomeFragmentDirections.actionHomeFragmentToDetailFragment(item.id)
            findNavController().navigate(action)
        }
        binding.rvItems.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { query ->
                    if (query.isEmpty()) {
                        binding.tvUpcomingHeader.text = "Upcoming Maintenance"
                        loadUpcomingItems()
                    } else {
                        viewModel.searchItems(query).observe(viewLifecycleOwner) { items ->
                            binding.tvUpcomingHeader.text = "Search Results"
                            adapter.submitList(items)
                        }
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
            val oneMonthInMs = 30L * 24 * 60 * 60 * 1000L
            val upcoming = items.filter { it.isActive && it.nextServiceDate in currentTime..(currentTime + oneMonthInMs) }
                .sortedBy { it.nextServiceDate }
                .take(10) // Maksimal muncul 10 card
            adapter.submitList(upcoming)
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
                val oneWeekInMs = 7 * 24 * 60 * 60 * 1000L
                val urgent = items.filter { 
                    it.isActive && it.nextServiceDate - currentTime in 0..oneWeekInMs 
                }
                binding.tvUpcomingHeader.text = "Urgent Maintenance"
                adapter.submitList(urgent.sortedBy { it.nextServiceDate })
            }
        }

        binding.cardOverdue.setOnClickListener {
            viewModel.allItems.value?.let { items ->
                val currentTime = System.currentTimeMillis()
                val overdue = items.filter { it.isActive && it.nextServiceDate < currentTime }
                binding.tvUpcomingHeader.text = "Overdue Items"
                adapter.submitList(overdue.sortedBy { it.nextServiceDate })
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
    }

    private fun updateDashboard(items: List<com.example.servicemaintainreminder.data.Item>) {
        val activeItems = items.filter { it.isActive }
        val total = activeItems.size
        val currentTime = System.currentTimeMillis()
        val oneWeekInMs = 7 * 24 * 60 * 60 * 1000L

        val upcoming = activeItems.count { it.nextServiceDate - currentTime in 0..oneWeekInMs }
        val overdue = activeItems.count { it.nextServiceDate < currentTime }

        binding.tvTotalItems.text = total.toString()
        binding.tvUpcomingService.text = upcoming.toString()
        binding.tvOverdueService.text = overdue.toString()
    }

    private fun updateCostEstimations() {
        if (allItemsList.isEmpty()) {
            binding.tvMonth1Cost.text = "-"
            binding.tvMonth2Cost.text = "-"
            binding.tvMonth3Cost.text = "-"
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
        val monthFormats = SimpleDateFormat("MMMM", Locale.getDefault())
        
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

        binding.tvMonth1Name.text = monthNames[0]
        binding.tvMonth2Name.text = monthNames[1]
        binding.tvMonth3Name.text = monthNames[2]

        val format = NumberFormat.getInstance(Locale("in", "ID"))
        val formatCost = { cost: Double ->
            if (cost == 0.0) "-" else "Rp ${format.format(cost.toLong())}"
        }

        binding.tvMonth1Cost.text = formatCost(months[0])
        binding.tvMonth2Cost.text = formatCost(months[1])
        binding.tvMonth3Cost.text = formatCost(months[2])

        // Add Click Listeners for Pop up
        binding.cvMonth1.setOnClickListener { showCostDetailDialog("Bulan ${monthNames[0]}", monthDetails[0], months[0]) }
        binding.cvMonth2.setOnClickListener { showCostDetailDialog("Bulan ${monthNames[1]}", monthDetails[1], months[1]) }
        binding.cvMonth3.setOnClickListener { showCostDetailDialog("Bulan ${monthNames[2]}", monthDetails[2], months[2]) }
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

        tvTitle.text = "Estimasi Cost: $title"
        
        val format = NumberFormat.getInstance(Locale("in", "ID"))
        tvTotal.text = "Rp ${format.format(totalCost.toLong())}"

        if (details.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "Tidak ada jadwal servis yang butuh biaya estimasi di bulan ini."
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
        super.onDestroyView()
        _binding = null
    }
}
