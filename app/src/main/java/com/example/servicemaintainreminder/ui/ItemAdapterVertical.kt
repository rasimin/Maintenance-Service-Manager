package com.example.servicemaintainreminder.ui

import android.graphics.Color
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

class ItemAdapterVertical(
    private val onItemClick: (Item) -> Unit,
    private val onEditClick: (Item) -> Unit,
    private val onAddRecordClick: (Item) -> Unit
) : ListAdapter<Item, ItemAdapterVertical.ItemViewHolder>(DiffCallback) {

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

            // Update Active/Inactive Status Label
            binding.tvActiveStatus.text = if (item.isActive) "Active" else "Inactive"

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

            if (item.isActive) {
                // ACTIVE STATE STYLING
                binding.root.alpha = 1.0f
                binding.ivItemIcon.alpha = 1.0f
                binding.tvItemName.setTextColor(Color.parseColor("#222222"))
                binding.tvCategory.setTextColor(Color.parseColor("#777777"))
                binding.tvActiveStatus.setTextColor(Color.parseColor("#2ECC71"))
                
                // Update Status Badge and Text Color based on date
                when {
                    daysDiff < 0 -> {
                        binding.tvStatus.text = "Overdue"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_error))
                        binding.flStatusHeader.setBackgroundColor(Color.parseColor("#1AE74C3C")) // Soft red
                    }
                    daysDiff <= 3 -> {
                        binding.tvStatus.text = if (daysDiff == 0) "Today" else "$daysDiff days left"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_warning))
                        binding.flStatusHeader.setBackgroundColor(Color.parseColor("#1AF5A623")) // Soft orange
                    }
                    else -> {
                        binding.tvStatus.text = "$daysDiff days left"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_safe))
                        binding.flStatusHeader.setBackgroundColor(Color.parseColor("#1A2ECC71")) // Soft green
                    }
                }
                
                binding.btnAddRecord.isEnabled = true
                binding.btnAddRecord.alpha = 1.0f
            } else {
                // INACTIVE STATE STYLING (Grayed out)
                binding.root.alpha = 0.8f
                binding.ivItemIcon.alpha = 0.5f
                
                // Gray colors
                val grayText = Color.parseColor("#9E9E9E")
                val lightGrayBg = Color.parseColor("#F5F5F5")
                
                binding.tvItemName.setTextColor(grayText)
                binding.tvCategory.setTextColor(grayText)
                binding.tvActiveStatus.setTextColor(grayText)
                binding.tvStatus.setTextColor(grayText)
                binding.tvNextServiceDate.setTextColor(grayText)
                
                binding.tvStatus.text = "Disabled"
                binding.flStatusHeader.setBackgroundColor(lightGrayBg)
                
                // Disable record button for inactive items
                binding.btnAddRecord.isEnabled = false
                binding.btnAddRecord.alpha = 0.5f
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(item)
            }

            binding.btnAddRecord.setOnClickListener {
                onAddRecordClick(item)
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
