package com.example.servicemaintainreminder.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CostChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var dataPoints: List<Float> = listOf(0.1f, 0.4f, 0.9f)
        set(value) {
            field = value.take(3)
            invalidate()
        }

    // Default colors for Estimated mode
    private var barGradStart = Color.parseColor("#E1D6FF")
    private var barGradEnd = Color.parseColor("#A18DFF")
    private var lineColor = Color.parseColor("#A18DFF")
    
    private val density = context.resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val pointStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }

    fun setChartMode(isRealized: Boolean) {
        if (isRealized) {
            // Blue/Teal colors for Realized
            barGradStart = Color.parseColor("#B3E5FC")
            barGradEnd = Color.parseColor("#29B6F6")
            lineColor = Color.parseColor("#4FC3F7")
        } else {
            // Purple colors for Estimated
            barGradStart = Color.parseColor("#E1D6FF")
            barGradEnd = Color.parseColor("#A18DFF")
            lineColor = Color.parseColor("#A18DFF")
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        
        val paddT = 24f * density
        val paddB = 8f * density
        val drawH = h - paddT - paddB
        val segmentW = w / 3f

        val maxVal = dataPoints.maxOrNull() ?: 1f
        val peak = if (maxVal > 0) maxVal else 1f

        val cx = FloatArray(3)
        val cy = FloatArray(3)

        val barWidth = 22f * density
        val barRadius = 6f * density

        for (i in 0 until 3) {
            var v = if (i < dataPoints.size) dataPoints[i] else 0f
            // minimum height visible
            if (v == 0f) v = peak * 0.05f 
            
            val pct = v / peak
            val barH = drawH * pct

            val centerX = (i * segmentW) + (segmentW / 2f)
            cx[i] = centerX
            cy[i] = h - paddB - barH

            val left = centerX - (barWidth / 2f)
            val right = centerX + (barWidth / 2f)
            val top = cy[i] + (barRadius)
            val bot = h - paddB

            // Each bar has gradient
            barPaint.shader = LinearGradient(left, bot, left, top, 
               barGradStart, barGradEnd, Shader.TileMode.CLAMP)

            // Draw Bar
            val rectF = RectF(left, top, right, bot)
            canvas.drawRoundRect(rectF, barRadius, barRadius, barPaint)
        }

        // Draw Beziere Line connecting points smoothly
        val path = Path()
        val controlFactor = 0.5f

        path.moveTo(ax(cx[0], -segmentW), ay(cy[0], cy[0], 0f))
        
        path.cubicTo(
            cx[0] - (segmentW * controlFactor), cy[0],
            cx[0] - (segmentW * 0.2f), cy[0],
            cx[0], cy[0]
        )

        for (i in 0 until 2) {
            val dist = cx[i+1] - cx[i]
            path.cubicTo(
                cx[i] + dist * controlFactor, cy[i],
                cx[i+1] - dist * controlFactor, cy[i+1],
                cx[i+1], cy[i+1]
            )
        }
        
        path.cubicTo(
            cx[2] + (segmentW * 0.2f), cy[2],
            cx[2] + (segmentW * controlFactor), cy[2],
            ax(cx[2], segmentW), ay(cy[2], cy[2], 0f)
        )

        linePaint.color = lineColor
        canvas.drawPath(path, linePaint)

        // Draw dots at the peak
        pointStroke.color = lineColor
        for (i in 0 until 3) {
            canvas.drawCircle(cx[i], cy[i], 4f * density, pointPaint)
            canvas.drawCircle(cx[i], cy[i], 4f * density, pointStroke)
        }
    }
    
    private fun ax(x: Float, offset: Float) = x + offset
    private fun ay(y1: Float, y2: Float, offset: Float) = y1 + offset
}
