package com.example.servicemaintainreminder.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.data.ServiceHistory
import com.example.servicemaintainreminder.databinding.FragmentDetailBinding
import com.example.servicemaintainreminder.util.DateUtil
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
        loadInterstitialAd()
        observeItem()

        binding.fabAddHistory.setOnClickListener {
            showAddHistoryDialog()
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
        binding.tvDetailCategory.text = "Category: ${item.category}"
        binding.tvDetailNextService.text = DateUtil.formatDate(item.nextServiceDate)
        binding.tvDetailLastService.text = "Last Service: ${DateUtil.formatDate(item.lastServiceDate)}"
        binding.tvDetailInterval.text = "Interval: ${item.serviceIntervalValue} ${item.serviceIntervalUnit}"
        binding.tvDetailNote.text = if (item.note.isNotEmpty()) "Note: ${item.note}" else ""
    }

    private fun observeHistory(itemId: Long) {
        viewModel.getHistory(itemId).observe(viewLifecycleOwner) { history ->
            historyAdapter.submitList(history)
            
            // Calculate and display total cost
            val totalCost = history.sumOf { it.cost }
            binding.tvDetailTotalCost.text = "Total Maintenance Cost: Rp ${String.format("%,.2f", totalCost)}"
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.adapter = historyAdapter
    }

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
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                selectedHistoryDate = selectedCalendar.timeInMillis
                etDate.setText(DateUtil.formatDate(selectedHistoryDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Service History")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val desc = etDesc.text.toString()
                val costStr = etCost.text.toString()
                if (desc.isNotEmpty() && costStr.isNotEmpty()) {
                    saveHistory(desc, costStr.toDouble(), selectedHistoryDate)
                }
            }
            .setNegativeButton("Cancel", null)
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
            // If the added history is newer than current last service date, update item
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
        }
        
        Toast.makeText(requireContext(), "History added", Toast.LENGTH_SHORT).show()
        
        mInterstitialAd?.show(requireActivity())
        loadInterstitialAd() // Reload for next time
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
