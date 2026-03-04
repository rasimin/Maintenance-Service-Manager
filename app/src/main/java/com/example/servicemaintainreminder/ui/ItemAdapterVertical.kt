package com.example.servicemaintainreminder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.databinding.ItemServiceListBinding
import com.example.servicemaintainreminder.util.DateUtil

class ItemAdapterVertical(private val onItemClick: (Item) -> Unit) :
    ListAdapter<Item, ItemAdapterVertical.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemServiceListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ItemViewHolder(private val binding: ItemServiceListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.tvItemName.text = item.name
            binding.tvCategory.text = item.category
            binding.tvNextServiceDate.text = DateUtil.formatDate(item.nextServiceDate)

            val currentTime = System.currentTimeMillis()
            val timeDiff = item.nextServiceDate - currentTime
            val daysDiff = timeDiff / (24 * 60 * 60 * 1000)

            when {
                daysDiff < 0 -> {
                    binding.tvStatus.text = "Overdue"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark))
                }
                daysDiff <= 7 -> {
                    binding.tvStatus.text = "$daysDiff days remaining"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark))
                }
                else -> {
                    binding.tvStatus.text = "$daysDiff days remaining"
                    binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.darker_gray))
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
