package com.yourname.videoeditor.feature.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class VolumeControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var volumeLevel = 1.0f // 0.0 to 2.0
    private var isMuted = false
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartVolume = 1.0f
    
    var onVolumeChanged: ((volume: Float, isMuted: Boolean) -> Unit)? = null
    
    init {
        textPaint.color = Color.WHITE
        textPaint.textSize = spToPx(14f)
        textPaint.textAlign = Paint.Align.CENTER
    }
    
    fun setVolume(volume: Float) {
        this.volumeLevel = volume.coerceIn(0f, 2f)
        invalidate()
    }
    
    fun setMuted(muted: Boolean) {
        this.isMuted = muted
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 16f
        
        // Draw background
        paint.color = Color.parseColor("#333333")
        canvas.drawRoundRect(0f, 0f, width, height, 8f, 8f, paint)
        
        // Draw volume bar
        val barWidth = width - (padding * 2)
        val barHeight = 8f
        val barY = height / 2 - barHeight / 2
        
        // Background bar
        paint.color = Color.parseColor("#666666")
        canvas.drawRoundRect(padding, barY, width - padding, barY + barHeight, 4f, 4f, paint)
        
        // Volume level bar
        val levelWidth = barWidth * (if (isMuted) 0f else (volumeLevel / 2f))
        paint.color = if (isMuted) Color.RED else Color.parseColor("#FF4081")
        canvas.drawRoundRect(padding, barY, padding + levelWidth, barY + barHeight, 4f, 4f, paint)
        
        // Draw handle
        val handleX = padding + levelWidth
        paint.color = Color.WHITE
        canvas.drawCircle(handleX, height / 2, 16f, paint)
        
        // Draw volume text
        val volumeText = if (isMuted) {
            "Muted"
        } else {
            "${(volumeLevel * 50).toInt()}%"
        }
        canvas.drawText(volumeText, width / 2, height / 2 - 24f, textPaint)
        
        // Draw icons
        drawVolumeIcon(canvas, padding / 2, height / 2)
    }
    
    private fun drawVolumeIcon(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        
        if (isMuted || volumeLevel == 0f) {
            // Muted icon (speaker with X)
            canvas.drawCircle(x, y, 12f, paint)
            paint.color = Color.RED
            paint.strokeWidth = 4f
            canvas.drawLine(x - 8f, y - 8f, x + 8f, y + 8f, paint)
        } else if (volumeLevel < 0.5f) {
            // Low volume (speaker with one wave)
            canvas.drawCircle(x, y, 12f, paint)
        } else if (volumeLevel < 1.5f) {
            // Medium volume (speaker with two waves)
            canvas.drawCircle(x, y, 12f, paint)
        } else {
            // High volume (speaker with three waves)
            canvas.drawCircle(x, y, 12f, paint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                
                // Check if touching the handle
                val handleX = padding + ((if (isMuted) 0f else volumeLevel / 2f) * (width - padding * 2))
                val distance = Math.hypot((x - handleX).toDouble(), (y - height/2).toDouble())
                
                if (distance < 32) {
                    isDragging = true
                    dragStartX = x
                    dragStartVolume = volumeLevel
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = event.x - dragStartX
                    val maxDelta = width - padding * 2
                    val volumeDelta = (deltaX / maxDelta) * 2f // 2f is max volume range
                    
                    var newVolume = (dragStartVolume + volumeDelta).coerceIn(0f, 2f)
                    
                    // Check for mute toggle
                    if (newVolume == 0f && volumeLevel > 0f) {
                        isMuted = true
                    } else if (newVolume > 0f && isMuted) {
                        isMuted = false
                    }
                    
                    volumeLevel = newVolume
                    onVolumeChanged?.invoke(volumeLevel, isMuted)
                    invalidate()
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun spToPx(sp: Float): Float {
        return sp * resources.displayMetrics.scaledDensity
    }
}