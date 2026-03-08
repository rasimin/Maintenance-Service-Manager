package com.example.servicemaintainreminder.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item

class SearchSuggestionAdapter(
    context: Context,
    private val items: List<Item>
) : ArrayAdapter<Item>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false)
        val item = items[position]

        val tvName = view.findViewById<TextView>(R.id.tvSearchItemName)
        val tvCategory = view.findViewById<TextView>(R.id.tvSearchItemCategory)
        val ivIcon = view.findViewById<ImageView>(R.id.ivSearchItemIcon)

        tvName.text = item.name
        tvCategory.text = item.category

        val iconRes = when (item.category.lowercase()) {
            "vehicle", "kendaraan" -> android.R.drawable.ic_menu_directions
            "electronics", "elektronik" -> android.R.drawable.ic_menu_preferences
            else -> android.R.drawable.ic_menu_slideshow
        }
        ivIcon.setImageResource(iconRes)

        return view
    }
}
