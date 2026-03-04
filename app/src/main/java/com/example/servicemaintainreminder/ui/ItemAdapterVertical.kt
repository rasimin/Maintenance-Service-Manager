package com.example.servicemaintainreminder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.R
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

            // Set Category Icon
            val iconRes = when (item.category.lowercase()) {
                "vehicle", "kendaraan" -> android.R.drawable.ic_menu_directions
                "electronics", "elektronik" -> android.R.drawable.ic_menu_preferences
                else -> android.R.drawable.ic_menu_slideshow
            }
            binding.ivItemIcon.setImageResource(iconRes)

            val currentTime = System.currentTimeMillis()
            val timeDiff = item.nextServiceDate - currentTime
            val daysDiff = (timeDiff / (24 * 60 * 60 * 1000)).toInt()

            // Update Status Badge and Indicator
            when {
                daysDiff < 0 -> {
                    binding.tvStatus.text = "Overdue"
                    binding.cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_overdue))
                    binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(binding.root.context, R.color.status_overdue)
                }
                daysDiff <= 3 -> {
                    binding.tvStatus.text = if (daysDiff == 0) "Today" else "$daysDiff days left"
                    binding.cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_soon))
                    binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(binding.root.context, R.color.status_soon)
                }
                else -> {
                    binding.tvStatus.text = "$daysDiff days left"
                    binding.cvStatusBadge.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_safe))
                    binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(binding.root.context, R.color.status_safe)
                }
            }

            binding.root.setOnClickListener { 
                it.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).withEndAction {
                    it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                        onItemClick(item)
                    }.start()
                }.start()
            }
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
