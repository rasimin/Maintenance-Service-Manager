package com.example.servicemaintainreminder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.databinding.FragmentDevicesBinding

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ItemAdapterVertical

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
        adapter = ItemAdapterVertical { item ->
            val action = DevicesFragmentDirections.actionDevicesFragmentToDetailFragment(item.id)
            findNavController().navigate(action)
        }
        binding.rvAllDevices.adapter = adapter
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
