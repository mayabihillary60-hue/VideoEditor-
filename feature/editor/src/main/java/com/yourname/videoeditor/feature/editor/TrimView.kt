package com.yourname.videoeditor.feature.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TrimView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var totalDurationMs: Long = 0
    private var startMs: Long = 0
    private var endMs: Long = 0
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    
    private var isDraggingLeft = false
    private var isDraggingRight = false
    private var isDraggingHandle = false
    
    private val handleWidth = dpToPx(24f)
    private val handleColor = Color.parseColor("#FF4081")
    private val selectedColor = Color.parseColor("#4DFF4081")
    
    var onTrimRangeChanged: ((startMs: Long, endMs: Long) -> Unit)? = null
    
    fun setDuration(durationMs: Long) {
        this.totalDurationMs = durationMs
        this.endMs = durationMs
        invalidate()
    }
    
    fun setTrimRange(startMs: Long, endMs: Long) {
        this.startMs = startMs.coerceIn(0, totalDurationMs)
        this.endMs = endMs.coerceIn(0, totalDurationMs)
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (totalDurationMs == 0L) return
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw background (timeline)
        paint.color = Color.LTGRAY
        canvas.drawRect(0f, 0f, width, height, paint)
        
        // Draw selected region
        val startX = (startMs.toFloat() / totalDurationMs) * width
        val endX = (endMs.toFloat() / totalDurationMs) * width
        
        rectF.set(startX, 0f, endX, height)
        paint.color = selectedColor
        canvas.drawRect(rectF, paint)
        
        // Draw left handle
        paint.color = handleColor
        rectF.set(startX, 0f, startX + handleWidth, height)
        canvas.drawRect(rectF, paint)
        
        // Draw right handle
        rectF.set(endX - handleWidth, 0f, endX, height)
        canvas.drawRect(rectF, paint)
        
        // Draw center line
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        canvas.drawLine(startX + handleWidth/2, 0f, startX + handleWidth/2, height, paint)
        canvas.drawLine(endX - handleWidth/2, 0f, endX - handleWidth/2, height, paint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val width = width.toFloat()
        val startX = (startMs.toFloat() / totalDurationMs) * width
        val endX = (endMs.toFloat() / totalDurationMs) * width
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDraggingLeft = x in startX..(startX + handleWidth)
                isDraggingRight = x in (endX - handleWidth)..endX
                isDraggingHandle = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingLeft) {
                    val newStartMs = (x.coerceIn(0f, endX - handleWidth) / width) * totalDurationMs
                    startMs = newStartMs.toLong().coerceIn(0, endMs - 1000)
                    onTrimRangeChanged?.invoke(startMs, endMs)
                    invalidate()
                } else if (isDraggingRight) {
                    val newEndMs = (x.coerceIn(startX + handleWidth, width) / width) * totalDurationMs
                    endMs = newEndMs.toLong().coerceIn(startMs + 1000, totalDurationMs)
                    onTrimRangeChanged?.invoke(startMs, endMs)
                    invalidate()
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingLeft = false
                isDraggingRight = false
                isDraggingHandle = false
            }
        }
        
        return true
    }
    
    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}