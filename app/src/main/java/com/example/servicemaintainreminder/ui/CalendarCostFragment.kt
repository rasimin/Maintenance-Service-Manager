package com.example.servicemaintainreminder.ui

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.servicemaintainreminder.R
import com.example.servicemaintainreminder.data.Item
import com.example.servicemaintainreminder.databinding.FragmentCalendarCostBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class DayCostInfo(
    val date: Calendar,
    val totalCost: Double,
    val items: List<Item>
)

class CalendarCostFragment : Fragment() {

    private var _binding: FragmentCalendarCostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    private var currentCalendar = Calendar.getInstance()
    private var allItemsList: List<Item> = emptyList()
    private val currencyFormat = NumberFormat.getInstance(Locale("in", "ID"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    private var selectedDay: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarCostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupMonthNavigation()
        setupDayLabels()

        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            allItemsList = items.filter { it.isActive && it.estimatedCost > 0 }
            renderCalendar()
        }
    }

    private fun setupHeader() {
        binding.headerBack.ivBackButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.headerBack.tvHeaderTitle.text = "Cost Calendar"
    }

    private fun setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            selectedDay = -1
            renderCalendar()
            binding.cardSelectedDate.visibility = View.GONE
        }
        binding.btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            selectedDay = -1
            renderCalendar()
            binding.cardSelectedDate.visibility = View.GONE
        }

        // Tap on month/year text to open Month-Year picker
        binding.tvCurrentMonth.setOnClickListener {
            showMonthYearPicker()
        }
    }

    private fun showMonthYearPicker() {
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        val dpd = DatePickerDialog(requireContext(), { _, selYear, selMonth, _ ->
            currentCalendar.set(Calendar.YEAR, selYear)
            currentCalendar.set(Calendar.MONTH, selMonth)
            selectedDay = -1
            binding.cardSelectedDate.visibility = View.GONE
            renderCalendar()
        }, year, month, 1)
        dpd.show()
    }

    private fun setupDayLabels() {
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for (day in days) {
            val tv = TextView(requireContext()).apply {
                text = day
                textSize = 11f
                setTextColor(Color.parseColor("#8A8A9A"))
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            binding.llDayLabels.addView(tv)
        }
    }

    private fun renderCalendar() {
        binding.tvCurrentMonth.text = monthYearFormat.format(currentCalendar.time)
        binding.gridCalendar.removeAllViews()

        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Build a map: dayOfMonth -> DayCostInfo
        val dayCostMap = buildDayCostMap(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))

        val totalRows = ((firstDayOfWeek + daysInMonth + 6) / 7)
        binding.gridCalendar.rowCount = totalRows

        var monthTotal = 0.0
        var deviceCount = 0

        // Empty slots before first day
        for (i in 0 until firstDayOfWeek) {
            addEmptyCell()
        }

        // Day cells
        for (day in 1..daysInMonth) {
            val info = dayCostMap[day]
            val cost = info?.totalCost ?: 0.0
            monthTotal += cost
            if (info != null) deviceCount += info.items.size
            addDayCell(day, cost, info, dayCostMap)
        }

        // Fill remaining cells in the last row
        val remainder = (firstDayOfWeek + daysInMonth) % 7
        if (remainder != 0) {
            for (i in 0 until (7 - remainder)) {
                addEmptyCell()
            }
        }

        // Update monthly summary
        binding.tvMonthlyTotal.text = "Rp ${currencyFormat.format(monthTotal.toLong())}"
        binding.tvDeviceCount.text = "$deviceCount service schedules"
    }

    private fun buildDayCostMap(year: Int, month: Int): Map<Int, DayCostInfo> {
        val map = mutableMapOf<Int, MutableList<Item>>()

        for (item in allItemsList) {
            // Check if the nextServiceDate falls in this month/year
            val nextCal = Calendar.getInstance().apply { timeInMillis = item.nextServiceDate }
            if (nextCal.get(Calendar.YEAR) == year && nextCal.get(Calendar.MONTH) == month) {
                val day = nextCal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(item)
            }

            // Also project future service dates within this month
            val interval = item.serviceIntervalValue
            val unit = item.serviceIntervalUnit
            if (interval > 0) {
                val projCal = Calendar.getInstance().apply { timeInMillis = item.lastServiceDate }
                for (i in 1..24) {
                    if (unit == "Months") {
                        projCal.add(Calendar.MONTH, interval)
                    } else {
                        projCal.add(Calendar.DAY_OF_YEAR, interval)
                    }
                    if (projCal.get(Calendar.YEAR) == year && projCal.get(Calendar.MONTH) == month) {
                        val day = projCal.get(Calendar.DAY_OF_MONTH)
                        val existingItems = map.getOrPut(day) { mutableListOf() }
                        if (!existingItems.contains(item)) {
                            existingItems.add(item)
                        }
                    }
                    if (projCal.get(Calendar.YEAR) > year ||
                        (projCal.get(Calendar.YEAR) == year && projCal.get(Calendar.MONTH) > month)) {
                        break
                    }
                }
            }
        }

        return map.mapValues { (_, items) ->
            val cal = Calendar.getInstance()
            DayCostInfo(cal, items.sumOf { it.estimatedCost }, items)
        }
    }

    private fun addEmptyCell() {
        val cell = View(requireContext()).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.calendar_cell_height)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
        binding.gridCalendar.addView(cell)
    }

    private fun addDayCell(day: Int, cost: Double, info: DayCostInfo?, dayCostMap: Map<Int, DayCostInfo>) {
        val cell = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(2, 6, 2, 6)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.calendar_cell_height)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            val today = Calendar.getInstance()
            val isToday = today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == day
            val isSelected = selectedDay == day

            // Background styling
            val bgShape = GradientDrawable().apply {
                cornerRadius = 12f * resources.displayMetrics.density
                when {
                    isSelected -> {
                        setColor(Color.parseColor("#6366F1"))
                    }
                    isToday -> {
                        setColor(Color.parseColor("#EEF2FF"))
                    }
                    else -> {
                        setColor(Color.TRANSPARENT)
                    }
                }
            }
            background = bgShape

            if (cost > 0 || isToday) {
                setOnClickListener {
                    selectedDay = day
                    renderCalendar()
                    if (info != null) {
                        showDateDetail(day, info)
                    } else {
                        binding.cardSelectedDate.visibility = View.GONE
                    }
                }
            }
        }

        val isSelected = selectedDay == day

        // Day number
        val tvDay = TextView(requireContext()).apply {
            text = day.toString()
            textSize = 13f
            setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#1A1A2E"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        cell.addView(tvDay)

        // Cost label (small badge)
        if (cost > 0) {
            val costLabel = TextView(requireContext()).apply {
                val shortCost = when {
                    cost >= 1_000_000 -> "${(cost / 1_000_000).toInt()}M"
                    cost >= 1_000 -> "${(cost / 1_000).toInt()}K"
                    else -> cost.toInt().toString()
                }
                text = shortCost
                textSize = 8f
                gravity = Gravity.CENTER

                if (isSelected) {
                    setTextColor(Color.parseColor("#6366F1"))
                    setBackgroundResource(R.drawable.bg_status_badge)
                    backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                } else {
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_status_badge)
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
                }
                setPadding(6, 2, 6, 2)
            }
            cell.addView(costLabel)
        }

        binding.gridCalendar.addView(cell)
    }

    private fun showDateDetail(day: Int, info: DayCostInfo) {
        binding.cardSelectedDate.visibility = View.VISIBLE

        val dateCal = currentCalendar.clone() as Calendar
        dateCal.set(Calendar.DAY_OF_MONTH, day)
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH)
        binding.tvSelectedDateTitle.text = dateFormat.format(dateCal.time)
        binding.tvSelectedDateTotal.text = "Rp ${currencyFormat.format(info.totalCost.toLong())}"

        // Build list of items
        binding.llCostItems.removeAllViews()
        for (item in info.items) {
            val itemView = createCostItemView(item)
            binding.llCostItems.addView(itemView)
        }
    }

    private fun createCostItemView(item: Item): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 16)
        }

        // Icon
        val iconRes = when (item.category.lowercase()) {
            "vehicle", "kendaraan" -> android.R.drawable.ic_menu_directions
            "electronics", "elektronik" -> android.R.drawable.ic_menu_preferences
            else -> android.R.drawable.ic_menu_slideshow
        }

        val icon = android.widget.ImageView(requireContext()).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(36.dp(), 36.dp()).apply {
                marginEnd = 12.dp()
            }
            setColorFilter(Color.parseColor("#6C63FF"))
            setBackgroundResource(R.drawable.bg_next_service_rounded)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF2FF"))
            setPadding(8, 8, 8, 8)
        }
        container.addView(icon)

        // Text column
        val textCol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvName = TextView(requireContext()).apply {
            text = item.name
            textSize = 14f
            setTextColor(Color.parseColor("#1A1A2E"))
            typeface = Typeface.DEFAULT_BOLD
        }
        textCol.addView(tvName)

        val tvCategory = TextView(requireContext()).apply {
            text = item.category
            textSize = 12f
            setTextColor(Color.parseColor("#8A8A9A"))
        }
        textCol.addView(tvCategory)

        container.addView(textCol)

        // Cost
        val tvCost = TextView(requireContext()).apply {
            text = "Rp ${currencyFormat.format(item.estimatedCost.toLong())}"
            textSize = 14f
            setTextColor(Color.parseColor("#6C63FF"))
            typeface = Typeface.DEFAULT_BOLD
        }
        container.addView(tvCost)

        return container
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
