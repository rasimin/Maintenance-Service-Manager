package com.example.servicemaintainreminder.ui

import android.content.res.ColorStateList
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
import java.text.NumberFormat
import java.util.Locale

class ItemAdapterVertical(
    private val onItemClick: (Item) -> Unit,
    private val onMoreOptionsClick: (Item, android.view.View) -> Unit
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

            // Estimated cost
            val fmt = NumberFormat.getInstance(Locale("in", "ID"))
            binding.tvEstCost.text = if (item.estimatedCost > 0)
                "Rp ${fmt.format(item.estimatedCost.toLong())}"
            else
                "-"

            // Set custom or category icon
            val customIconResId = item.icon?.let { iconName ->
                binding.root.context.resources.getIdentifier(iconName, "drawable", binding.root.context.packageName)
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

            val currentTime = System.currentTimeMillis()
            val timeDiff = item.nextServiceDate - currentTime
            val daysDiff = (timeDiff / (24 * 60 * 60 * 1000)).toInt()

            if (item.isActive) {
                // ACTIVE STATE STYLING
                binding.root.alpha = 1.0f
                binding.ivItemIcon.alpha = 1.0f
                binding.tvItemName.setTextColor(Color.parseColor("#1A1A2E"))
                binding.tvCategory.setTextColor(Color.parseColor("#8A8A9A"))
                binding.tvNextServiceDate.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.brand_primary)
                )

                // Update accent bar color and status badge based on urgency
                val prefs = binding.root.context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val upcomingLimit = prefs.getInt("upcoming_days_limit", 30)

                when {
                    daysDiff < 0 -> {
                        // Overdue - red accent
                        binding.viewAccentBar.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E74C3C"))
                        binding.tvStatus.text = "Overdue"
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(binding.root.context, R.color.status_error)
                        )
                        binding.tvStatus.backgroundTintList =
                            ContextCompat.getColorStateList(binding.root.context, R.color.stats_red_bg)
                    }
                    daysDiff <= upcomingLimit -> {
                        // Urgent - orange accent (based on dynamic config)
                        binding.viewAccentBar.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F5A623"))
                        binding.tvStatus.text = if (daysDiff == 0) "Today!" else "$daysDiff days left"
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(binding.root.context, R.color.status_warning)
                        )
                        binding.tvStatus.backgroundTintList =
                            ContextCompat.getColorStateList(binding.root.context, R.color.stats_orange_bg)
                    }
                    else -> {
                        // Safe - green accent
                        binding.viewAccentBar.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2ECC71"))
                        binding.tvStatus.text = "$daysDiff days left"
                        binding.tvStatus.setTextColor(
                            ContextCompat.getColor(binding.root.context, R.color.status_safe)
                        )
                        binding.tvStatus.backgroundTintList =
                            ContextCompat.getColorStateList(binding.root.context, R.color.status_safe_bg)
                    }
                }
                binding.btnMoreOptions.isEnabled = true
                binding.btnMoreOptions.alpha = 1.0f
            } else {
                // INACTIVE STATE STYLING (Grayed out)
                binding.root.alpha = 0.75f
                binding.ivItemIcon.alpha = 0.5f

                val grayText = Color.parseColor("#AAAABC")
                binding.tvItemName.setTextColor(grayText)
                binding.tvCategory.setTextColor(grayText)
                binding.tvNextServiceDate.setTextColor(grayText)
                binding.viewAccentBar.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))

                binding.tvStatus.text = "Inactive"
                binding.tvStatus.setTextColor(grayText)
                binding.tvStatus.backgroundTintList =
                    ContextCompat.getColorStateList(binding.root.context, R.color.divider)
                binding.btnMoreOptions.isEnabled = false
                binding.btnMoreOptions.alpha = 0.4f
                binding.tvEstCost.setTextColor(Color.parseColor("#AAAABC"))
            }

            binding.btnMoreOptions.setOnClickListener { view ->
                onMoreOptionsClick(item, view)
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
