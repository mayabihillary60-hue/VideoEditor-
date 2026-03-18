package com.yourname.videoeditor.library.core.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*

class PerformanceManager(private val context: Context) : LifecycleObserver {

    companion object {
        private const val TAG = "PerformanceManager"
        private const val MEMORY_WARNING_THRESHOLD = 0.85 // 85% of heap
        private const val MAX_PREVIEW_FPS = 60
        private const val MIN_PREVIEW_FPS = 24
    }

    // Performance metrics
    data class PerformanceMetrics(
        var fps: Float = 0f,
        var frameTimeMs: Float = 0f,
        var memoryUsageMb: Long = 0,
        var heapSizeMb: Long = 0,
        var cpuUsage: Float = 0f,
        var gpuUsage: Float = 0f,
        var batteryTemperature: Float = 0f,
        var thermalThrottling: Boolean = false
    )

    private val metrics = PerformanceMetrics()
    private val performanceListeners = mutableListOf<PerformanceListener>()
    private val optimizationStrategies = mutableListOf<OptimizationStrategy>()

    // Coroutine scope for monitoring
    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    interface PerformanceListener {
        fun onMetricsUpdated(metrics: PerformanceMetrics)
        fun onPerformanceWarning(warning: String)
        fun onOptimizationApplied(strategy: String)
    }

    interface OptimizationStrategy {
        fun getName(): String
        fun getPriority(): Int // Higher = more important
        fun shouldApply(metrics: PerformanceMetrics): Boolean
        fun apply(): Boolean
        fun revert()
        fun getEstimatedGain(): Float // 0-100% improvement
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun startMonitoring() {
        monitoringScope.launch {
            while (isActive) {
                updateMetrics()
                delay(1000) // Update every second
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopMonitoring() {
        monitoringScope.coroutineContext.cancelChildren()
    }

    private suspend fun updateMetrics() {
        withContext(Dispatchers.Default) {
            // Memory metrics
            val runtime = Runtime.getRuntime()
            metrics.heapSizeMb = runtime.totalMemory() / (1024 * 1024)
            metrics.memoryUsageMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

            // Check for memory pressure
            if (metrics.memoryUsageMb.toFloat() / metrics.heapSizeMb > MEMORY_WARNING_THRESHOLD) {
                notifyWarning("High memory usage: ${metrics.memoryUsageMb}MB / ${metrics.heapSizeMb}MB")
                applyMemoryOptimizations()
            }

            // CPU metrics (simplified)
            metrics.cpuUsage = getCpuUsage()

            // GPU metrics (requires device-specific implementation)
            metrics.gpuUsage = getGpuUsage()

            // Thermal metrics
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                metrics.batteryTemperature = getBatteryTemperature()
                metrics.thermalThrottling = isThermalThrottling()
            }

            // Notify listeners
            withContext(Dispatchers.Main) {
                performanceListeners.forEach { it.onMetricsUpdated(metrics) }
            }
        }
    }

    private fun getCpuUsage(): Float {
        // Read from /proc/stat
        return try {
            val statFile = java.io.File("/proc/stat")
            if (statFile.exists()) {
                val reader = java.io.BufferedReader(java.io.FileReader(statFile))
                val line = reader.readLine()
                reader.close()
                
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    
                    val total = user + nice + system + idle
                    val used = user + nice + system
                    
                    (used.toFloat() / total) * 100
                } else {
                    0f
                }
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CPU usage", e)
            0f
        }
    }

    private fun getGpuUsage(): Float {
        // GPU usage depends on device and OpenGL implementation
        // For now, return a placeholder
        return 0f
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getBatteryTemperature(): Float {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10f
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isThermalThrottling(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.currentThermalStatus != android.os.PowerManager.THERMAL_STATUS_NONE
    }

    private fun applyMemoryOptimizations() {
        // Clear caches
        System.gc()
        
        // Reduce texture quality if needed
        applyStrategy(TextureQualityOptimization())
        
        // Reduce preview FPS
        applyStrategy(FPSCapOptimization())
        
        // Clear unused bitmaps
        applyStrategy(BitmapCacheOptimization())
    }

    private fun applyStrategy(strategy: OptimizationStrategy) {
        if (strategy.shouldApply(metrics)) {
            if (strategy.apply()) {
                notifyOptimization("Applied: ${strategy.getName()} (+${strategy.getEstimatedGain()}%)")
                optimizationStrategies.add(strategy)
            }
        }
    }

    fun revertOptimizations() {
        optimizationStrategies.forEach { it.revert() }
        optimizationStrategies.clear()
    }

    fun addPerformanceListener(listener: PerformanceListener) {
        performanceListeners.add(listener)
    }

    fun removePerformanceListener(listener: PerformanceListener) {
        performanceListeners.remove(listener)
    }

    private fun notifyWarning(warning: String) {
        performanceListeners.forEach { it.onPerformanceWarning(warning) }
    }

    private fun notifyOptimization(strategy: String) {
        performanceListeners.forEach { it.onOptimizationApplied(strategy) }
    }

    fun getDeviceCapability(): DeviceCapability {
        val memoryClass = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val hasOpenGLES31 = isOpenGLES31Supported()
        
        return when {
            memoryClass >= 512 && cpuCores >= 8 && hasOpenGLES31 -> DeviceCapability.HIGH_END
            memoryClass >= 256 && cpuCores >= 4 -> DeviceCapability.MID_RANGE
            else -> DeviceCapability.LOW_END
        }
    }

    private fun isOpenGLES31Supported(): Boolean {
        val config = android.opengl.EGL14.eglGetCurrentContext()
        return config != android.opengl.EGL14.EGL_NO_CONTEXT
    }

    fun getRecommendedQuality(): QualityLevel {
        return when (getDeviceCapability()) {
            DeviceCapability.HIGH_END -> QualityLevel.ULTRA
            DeviceCapability.MID_RANGE -> QualityLevel.HIGH
            DeviceCapability.LOW_END -> QualityLevel.MEDIUM
        }
    }
}

enum class DeviceCapability {
    LOW_END,
    MID_RANGE,
    HIGH_END
}

enum class QualityLevel {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}

// Specific optimization strategies
class TextureQualityOptimization : PerformanceManager.OptimizationStrategy {
    private var originalQuality = 1f
    private var reducedQuality = 0.75f
    private var applied = false

    override fun getName(): String = "Texture Quality Reduction"
    override fun getPriority(): Int = 1
    override fun getEstimatedGain(): Float = 25f

    override fun shouldApply(metrics: PerformanceManager.PerformanceMetrics): Boolean {
        return metrics.memoryUsageMb > metrics.heapSizeMb * 0.7
    }

    override fun apply(): Boolean {
        if (!applied) {
            // Reduce texture sizes in rendering pipeline
            applied = true
            return true
        }
        return false
    }

    override fun revert() {
        if (applied) {
            // Restore original texture quality
            applied = false
        }
    }
}

class FPSCapOptimization : PerformanceManager.OptimizationStrategy {
    private var originalFPS = 60
    private var cappedFPS = 30
    private var applied = false

    override fun getName(): String = "FPS Cap Reduction"
    override fun getPriority(): Int = 2
    override fun getEstimatedGain(): Float = 40f

    override fun shouldApply(metrics: PerformanceManager.PerformanceMetrics): Boolean {
        return metrics.batteryTemperature > 40f || metrics.thermalThrottling
    }

    override fun apply(): Boolean {
        if (!applied) {
            // Reduce rendering FPS
            applied = true
            return true
        }
        return false
    }

    override fun revert() {
        if (applied) {
            // Restore original FPS
            applied = false
        }
    }
}

class BitmapCacheOptimization : PerformanceManager.OptimizationStrategy {
    override fun getName(): String = "Bitmap Cache Clear"
    override fun getPriority(): Int = 3
    override fun getEstimatedGain(): Float = 15f

    override fun shouldApply(metrics: PerformanceManager.PerformanceMetrics): Boolean {
        return metrics.memoryUsageMb > metrics.heapSizeMb * 0.8
    }

    override fun apply(): Boolean {
        // Clear bitmap caches
        System.gc()
        return true
    }

    override fun revert() {
        // Can't revert cache clearing
    }
}

class RenderResolutionOptimization : PerformanceManager.OptimizationStrategy {
    override fun getName(): String = "Render Resolution Scale"
    override fun getPriority(): Int = 1
    override fun getEstimatedGain(): Float = 50f

    override fun shouldApply(metrics: PerformanceManager.PerformanceMetrics): Boolean {
        return metrics.fps < 30 && metrics.gpuUsage > 80
    }

    override fun apply(): Boolean {
        // Reduce render resolution scale (e.g., 0.8x)
        return true
    }

    override fun revert() {
        // Restore full resolution
    }
}
