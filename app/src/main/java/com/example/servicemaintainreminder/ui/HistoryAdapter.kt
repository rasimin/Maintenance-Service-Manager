package com.example.servicemaintainreminder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.data.ServiceHistory
import com.example.servicemaintainreminder.databinding.ItemHistoryBinding
import com.example.servicemaintainreminder.util.DateUtil
import java.text.NumberFormat
import java.util.*

class HistoryAdapter : ListAdapter<ServiceHistory, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

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
            binding.tvHistoryDate.text = DateUtil.formatDate(history.serviceDate)
            binding.tvHistoryDescription.text = history.description
            
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            binding.tvHistoryCost.text = format.format(history.cost)
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
