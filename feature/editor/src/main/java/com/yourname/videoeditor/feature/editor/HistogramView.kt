package com.yourname.videoeditor.feature.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class HistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    
    private var redHistogram = FloatArray(256)
    private var greenHistogram = FloatArray(256)
    private var blueHistogram = FloatArray(256)
    private var luminanceHistogram = FloatArray(256)
    
    private var maxValue = 1.0f
    
    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.CENTER
    }
    
    fun setHistogramData(red: FloatArray, green: FloatArray, blue: FloatArray, luminance: FloatArray) {
        this.redHistogram = red
        this.greenHistogram = green
        this.blueHistogram = blue
        this.luminanceHistogram = luminance
        
        // Find max value for scaling
        maxValue = maxOf(
            red.maxOrNull() ?: 0f,
            green.maxOrNull() ?: 0f,
            blue.maxOrNull() ?: 0f,
            luminance.maxOrNull() ?: 0f
        )
        
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 20f
        
        // Draw background grid
        paint.color = Color.parseColor("#333333")
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        
        // Horizontal lines
        for (i in 0..4) {
            val y = padding + (i * (height - 2 * padding) / 4)
            canvas.drawLine(padding, y, width - padding, y, paint)
        }
        
        // Draw histogram lines
        drawHistogramLine(canvas, luminanceHistogram, Color.WHITE, padding, width, height)
        drawHistogramLine(canvas, redHistogram, Color.RED, padding, width, height)
        drawHistogramLine(canvas, greenHistogram, Color.GREEN, padding, width, height)
        drawHistogramLine(canvas, blueHistogram, Color.BLUE, padding, width, height)
        
        // Draw labels
        textPaint.textSize = 24f
        canvas.drawText("0", padding, height - 10f, textPaint)
        canvas.drawText("255", width - padding - 30f, height - 10f, textPaint)
    }
    
    private fun drawHistogramLine(canvas: Canvas, data: FloatArray, color: Int, padding: Float, width: Float, height: Float) {
        if (maxValue == 0f) return
        
        path.reset()
        paint.color = color
        paint.style = Paint.Style.STROKE
        
        val step = (width - 2 * padding) / (data.size - 1)
        
        for (i in data.indices) {
            val x = padding + i * step
            val normalizedValue = (data[i] / maxValue).coerceIn(0f, 1f)
            val y = height - padding - (normalizedValue * (height - 2 * padding))
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        canvas.drawPath(path, paint)
    }
}