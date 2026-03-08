package com.example.servicemaintainreminder.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.util.DateUtil

class SearchOverlayAdapter(
    private var items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<SearchOverlayAdapter.ViewHolder>() {

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSearchItemName)
        val tvCategory: TextView = view.findViewById(R.id.tvSearchItemCategory)
        val tvStatus: TextView = view.findViewById(R.id.tvSearchStatusBadge)
        val ivIcon: ImageView = view.findViewById(R.id.ivSearchItemIcon)
        val tvNextDate: TextView = view.findViewById(R.id.tvSearchNextDate)
        val flIconBackground: View = view.findViewById(R.id.flIconBackground)

        fun bind(item: Item) {
            val context = itemView.context
            tvName.text = item.name
            tvCategory.text = item.category
            tvNextDate.text = "Servis: ${DateUtil.formatDate(item.nextServiceDate)}"

            val isVehicle = item.category.equals("vehicle", ignoreCase = true) || item.category.equals("kendaraan", ignoreCase = true)
            val iconRes = if (isVehicle) android.R.drawable.ic_menu_directions else android.R.drawable.ic_menu_preferences
            ivIcon.setImageResource(iconRes)

            if (isVehicle) {
                flIconBackground.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2BA57A"))
            } else {
                flIconBackground.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#8E75FF"))
            }

            val msLeft = item.nextServiceDate - System.currentTimeMillis()
            val daysLeft = (msLeft / (24 * 60 * 60 * 1000L)).toInt()

            when {
                daysLeft < 0 -> {
                    tvStatus.text = "Overdue"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.danger))
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_error_bg))
                }
                daysLeft == 0 -> {
                    tvStatus.text = "Today!"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.danger))
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_error_bg))
                }
                daysLeft <= 7 -> {
                    tvStatus.text = "$daysLeft days left"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning_bg))
                }
                else -> {
                    tvStatus.text = "$daysLeft days left"
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_safe))
                    tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_safe_bg))
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
