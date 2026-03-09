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
        binding.header.cardSettings.setOnClickListener { view ->
            val popupMenu = android.widget.PopupMenu(requireContext(), view)
            popupMenu.menu.add(0, 1, 0, "Account")
            popupMenu.menu.add(0, 2, 1, "Settings")
            
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        showAccountDialog()
                        true
                    }
                    2 -> {
                        android.widget.Toast.makeText(requireContext(), "Settings feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun showAccountDialog() {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val currentName = prefs.getString("user_name", "") ?: ""
        
        val editText = android.widget.EditText(requireContext()).apply {
            hint = "Enter your name"
            setText(currentName)
            setLines(1)
            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Add margins 24dp
        val margin = (24 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, 0, margin, 0)
        editText.layoutParams = params
        container.addView(editText)
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Account Settings")
            .setMessage("Change your display name:")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    prefs.edit().putString("user_name", newName).apply()
                    updateGreeting()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            val oneMonthInMs = 30L * 24 * 60 * 60 * 1000L
            val upcoming = items.filter { it.isActive && it.nextServiceDate in currentTime..(currentTime + oneMonthInMs) }
                .sortedBy { it.nextServiceDate }
                .take(10) // Maksimal muncul 10 card
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
        recyclerViewState = binding.rvItems.layoutManager?.onSaveInstanceState()
        super.onDestroyView()
        _binding = null
    }
}
