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
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(private val binding: ItemServiceHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val context = binding.root.context

            binding.tvItemName.text = item.name
            binding.tvServiceType.text = item.category
            binding.tvNextDate.text = "Servis: ${DateUtil.formatDate(item.nextServiceDate)}"

            // Set Category or Custom Icon
            val customIconResId = item.icon?.let { iconName ->
                context.resources.getIdentifier(iconName, "drawable", context.packageName)
            } ?: 0

            if (customIconResId != 0) {
                binding.ivItemIcon.setImageResource(customIconResId)
            } else {
                val iconRes = when (item.category.lowercase()) {
                    "ac", "air conditioner" -> R.drawable.ic_ac
                    "vehicle", "kendaraan", "mobil", "motor" -> R.drawable.ic_car
                    "electronics", "elektronik" -> R.drawable.ic_electronic
                    "machine", "mesin" -> R.drawable.ic_machine
                    "home appliance" -> R.drawable.ic_electronic
                    else -> R.drawable.ic_devices
                }
                binding.ivItemIcon.setImageResource(iconRes)
            }


            // Hitung sisa hari
            val currentTime = System.currentTimeMillis()
            val msLeft = item.nextServiceDate - currentTime
            val daysLeft = (msLeft / (24 * 60 * 60 * 1000L)).toInt()

            // Tentukan teks, warna, dan accent berdasarkan urgensi
            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val upcomingLimit = prefs.getInt("upcoming_days_limit", 7)

            when {
                daysLeft < 0 -> {
                    // Sudah lewat jadwal
                    val color = ContextCompat.getColor(context, R.color.danger)
                    val bgColor = ContextCompat.getColor(context, R.color.status_error_bg)
                    binding.viewTopAccent.setBackgroundColor(color)
                    binding.tvStatusLabel.text = "Overdue"
                    binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(bgColor)
                    binding.tvStatusLabel.setTextColor(color)
                }
                daysLeft == 0 -> {
                    // Hari ini!
                    val color = ContextCompat.getColor(context, R.color.danger)
                    val bgColor = ContextCompat.getColor(context, R.color.status_error_bg)
                    binding.viewTopAccent.setBackgroundColor(color)
                    binding.tvStatusLabel.text = "Today!"
                    binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(bgColor)
                    binding.tvStatusLabel.setTextColor(color)
                }
                daysLeft <= upcomingLimit -> {
                    // Segera (sesuai config)
                    val color = ContextCompat.getColor(context, R.color.warning)
                    val bgColor = ContextCompat.getColor(context, R.color.status_warning_bg)
                    binding.viewTopAccent.setBackgroundColor(color)
                    binding.tvStatusLabel.text = "$daysLeft days left"
                    binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(bgColor)
                    binding.tvStatusLabel.setTextColor(color)
                }
                else -> {
                    // Masih aman
                    val color = ContextCompat.getColor(context, R.color.status_safe)
                    val bgColor = ContextCompat.getColor(context, R.color.status_safe_bg)
                    binding.viewTopAccent.setBackgroundColor(color)
                    binding.tvStatusLabel.text = "$daysLeft days left"
                    binding.tvStatusLabel.backgroundTintList = ColorStateList.valueOf(bgColor)
                    binding.tvStatusLabel.setTextColor(color)
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
