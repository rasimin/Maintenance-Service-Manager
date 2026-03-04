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
import com.example.servicemaintainreminder.databinding.FragmentHomeBinding
import com.google.android.gms.ads.AdRequest

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ItemAdapter

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
            binding.progressBar.isVisible = false
            binding.rvItems.isVisible = true
            updateDashboard(items)
            
            if (binding.tvUpcomingHeader.text == "Upcoming Maintenance") {
                loadUpcomingItems()
            }
        }
    }
    
    private fun loadUpcomingItems() {
        viewModel.allItems.value?.let { items ->
            val currentTime = System.currentTimeMillis()
            val oneMonthInMs = 30L * 24 * 60 * 60 * 1000L
            // Filter only active items for upcoming maintenance list
            val upcoming = items.filter { it.isActive && it.nextServiceDate in currentTime..(currentTime + oneMonthInMs) }
                .sortedBy { it.nextServiceDate }
            adapter.submitList(upcoming)
        }
    }

    private fun setupClickListeners() {
        binding.cardMyDevices.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                    val action = HomeFragmentDirections.actionHomeFragmentToDevicesFragment()
                    findNavController().navigate(action)
                }.start()
            }.start()
        }

        binding.cardUpcoming.setOnClickListener {
            viewModel.allItems.value?.let { items ->
                val currentTime = System.currentTimeMillis()
                val oneWeekInMs = 7 * 24 * 60 * 60 * 1000L
                // Filter only active items
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
                // Filter only active items
                val overdue = items.filter { it.isActive && it.nextServiceDate < currentTime }
                binding.tvUpcomingHeader.text = "Overdue Items"
                adapter.submitList(overdue.sortedBy { it.nextServiceDate })
            }
        }

        binding.tvViewAll.setOnClickListener {
            binding.tvUpcomingHeader.text = "Upcoming Maintenance"
            loadUpcomingItems()
        }
    }

    private fun updateDashboard(items: List<com.example.servicemaintainreminder.data.Item>) {
        // Only count active items for the dashboard counters
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

    private fun loadAds() {
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
