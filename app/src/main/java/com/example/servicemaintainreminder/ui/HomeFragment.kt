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
            val upcoming = items.filter { it.isActive && it.nextServiceDate in currentTime..(currentTime + oneMonthInMs) }
                .sortedBy { it.nextServiceDate }
                .take(10) // Maksimal muncul 10 card
            adapter.submitList(upcoming)
        }
    }

    private fun setupClickListeners() {
        binding.cardMyDevices.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToDevicesFragment()
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

        // Pindah ke halaman My Devices
        binding.tvViewAll.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToDevicesFragment()
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

    private fun loadAds() {
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
