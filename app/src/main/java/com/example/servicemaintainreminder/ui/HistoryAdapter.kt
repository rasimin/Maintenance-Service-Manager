package com.example.servicemaintainreminder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.data.ServiceHistory
import com.example.servicemaintainreminder.databinding.ItemHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onEditClick: ((ServiceHistory) -> Unit)? = null
) : ListAdapter<ServiceHistory, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(
            ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(history: ServiceHistory) {
            // Format day and month separately for the badge
            val dateObj = Date(history.serviceDate)
            val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            binding.tvHistoryDay.text = dayFormat.format(dateObj)
            binding.tvHistoryMonth.text = monthFormat.format(dateObj).uppercase()

            // Full date stored in hidden view (used by swipe helper if needed)
            binding.tvHistoryDate.text = android.text.format.DateFormat.format("dd MMM yyyy", dateObj)

            binding.tvHistoryDescription.text = history.description

            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            binding.tvHistoryCost.text = format.format(history.cost)
            
            binding.root.setOnClickListener {
                onEditClick?.invoke(history)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ServiceHistory>() {
        override fun areItemsTheSame(oldItem: ServiceHistory, newItem: ServiceHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ServiceHistory, newItem: ServiceHistory): Boolean {
            return oldItem == newItem
        }
    }
}
