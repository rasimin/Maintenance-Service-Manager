package com.example.servicemaintainreminder.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.databinding.ItemServiceHorizontalBinding
import com.example.servicemaintainreminder.util.DateUtil

class ItemAdapter(private val onItemClick: (Item) -> Unit) :
    ListAdapter<Item, ItemAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemServiceHorizontalBinding.inflate(
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

    inner class ItemViewHolder(private val binding: ItemServiceHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.tvItemName.text = item.name
            binding.tvServiceType.text = item.category
            binding.tvNextDate.text = "Servis: ${DateUtil.formatDate(item.nextServiceDate)}"

            // Logika Progress Bar Dinamis & Persentase
            val currentTime = System.currentTimeMillis()
            val totalDuration = item.nextServiceDate - item.lastServiceDate
            
            if (totalDuration > 0) {
                val elapsed = currentTime - item.lastServiceDate
                val progress = ((elapsed.toFloat() / totalDuration.toFloat()) * 100).toInt()
                
                // Batasi progress antara 0 - 100
                val safeProgress = progress.coerceIn(0, 100)
                binding.progressMaintenance.progress = safeProgress
                binding.tvProgressPercent.text = "$safeProgress%"

                // Ganti warna dan label berdasarkan kedekatan jadwal
                val context = binding.root.context
                when {
                    safeProgress >= 90 || currentTime > item.nextServiceDate -> {
                        val color = ContextCompat.getColor(context, R.color.danger)
                        binding.progressMaintenance.setIndicatorColor(color)
                        binding.tvProgressPercent.setTextColor(color)
                        binding.tvStatusLabel.text = "Kritis"
                        binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_error_bg))
                        binding.tvStatusLabel.setTextColor(color)
                    }
                    safeProgress >= 70 -> {
                        val color = ContextCompat.getColor(context, R.color.warning)
                        binding.progressMaintenance.setIndicatorColor(color)
                        binding.tvProgressPercent.setTextColor(color)
                        binding.tvStatusLabel.text = "Segera"
                        binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning_bg))
                        binding.tvStatusLabel.setTextColor(color)
                    }
                    else -> {
                        val color = ContextCompat.getColor(context, R.color.brand_primary)
                        binding.progressMaintenance.setIndicatorColor(color)
                        binding.tvProgressPercent.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        binding.tvStatusLabel.text = "Aman"
                        binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_safe_bg))
                        binding.tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                    }
                }
            } else {
                binding.progressMaintenance.progress = 0
                binding.tvProgressPercent.text = "0%"
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
