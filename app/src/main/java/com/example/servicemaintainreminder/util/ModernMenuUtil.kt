package com.example.servicemaintainreminder.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.servicemaintainreminder.R

data class ModernMenuItem(
    val id: Int, 
    val title: String, 
    val iconResId: Int, 
    val textColor: Int = Color.parseColor("#1A1A2E")
)

object ModernMenuUtil {
    fun showMenu(
        context: Context,
        anchor: View,
        items: List<ModernMenuItem>,
        onItemClick: (Int) -> Unit
    ) {
        var popupWindow: PopupWindow? = null
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_modern_menu, null)
        
        val llContainer = view.findViewById<LinearLayout>(R.id.llMenuContainer)
        
        items.forEachIndexed { index, item ->
            val itemView = inflater.inflate(R.layout.item_modern_menu, llContainer, false)
            val llClickable = itemView.findViewById<View>(R.id.llMenuItem)
            val ivIcon = itemView.findViewById<ImageView>(R.id.ivMenuIcon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvMenuTitle)
            val divider = itemView.findViewById<View>(R.id.vMenuDivider)
            
            ivIcon.setImageResource(item.iconResId)
            tvTitle.text = item.title
            tvTitle.setTextColor(item.textColor)
            ivIcon.setColorFilter(item.textColor)
            
            if (index == items.size - 1) {
                divider.visibility = View.GONE
            }
            
            llClickable.setOnClickListener {
                // Must be deferred slightly to allow ripple to run
                view.postDelayed({ popupWindow?.dismiss() }, 100)
                onItemClick(item.id)
            }
            llContainer.addView(itemView)
        }

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        
        popupWindow = PopupWindow(
            view,
            view.measuredWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Transparent background required to see CardView rounded corners and shadow
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 0f // The CardView handles shadow itself
        
        // Show below anchor, gravity END aligns the right edge of popup to right edge of anchor
        popupWindow.showAsDropDown(anchor, 0, -20, android.view.Gravity.END)
    }
}
