package com.yourname.videoeditor.library.rendering.filter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilterParameter(
    val name: String,
    val minValue: Float,
    val maxValue: Float,
    val defaultValue: Float,
    val unit: String = "",
    val step: Float = 0.01f
) : Parcelable {
    
    fun normalizeValue(value: Float): Float {
        return (value - minValue) / (maxValue - minValue)
    }
    
    fun denormalizeValue(normalized: Float): Float {
        return minValue + (normalized * (maxValue - minValue))
    }
    
    fun clampValue(value: Float): Float {
        return value.coerceIn(minValue, maxValue)
    }
    
    fun formatValue(value: Float): String {
        return when {
            unit == "%" -> "${(value * 100).toInt()}%"
            unit.isNotEmpty() -> "$value$unit"
            else -> String.format("%.2f", value)
        }
    }
}