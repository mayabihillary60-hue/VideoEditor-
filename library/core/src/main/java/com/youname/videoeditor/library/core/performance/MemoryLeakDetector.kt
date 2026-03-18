package com.yourname.videoeditor.library.core.performance

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MemoryLeakDetector(private val context: Context) : 
    Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "MemoryLeakDetector"
        private const val LEAK_WARNING_THRESHOLD = 5
        private const val GC_WAIT_TIME = 1000L
    }

    private val activityReferences = ConcurrentHashMap<String, WeakReference<Activity>>()
    private val fragmentReferences = ConcurrentHashMap<String, WeakReference<Fragment>>()
    private val viewReferences = ConcurrentHashMap<String, WeakReference<android.view.View>>()
    
    private val handler = Handler(Looper.getMainLooper())
    private val leakCheckRunnable = object : Runnable {
        override fun run() {
            checkForLeaks()
            handler.postDelayed(this, 30000) // Check every 30 seconds
        }
    }

    interface LeakListener {
        fun onLeakDetected(className: String, references: Int)
        fun onPotentialLeak(className: String, stackTrace: String)
        fun onMemoryWarning(freeMemoryMb: Long)
    }

    private val listeners = mutableListOf<LeakListener>()

    fun startMonitoring() {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        handler.post(leakCheckRunnable)
        
        // Enable heap dumps for debugging
        if (BuildConfig.DEBUG) {
            Debug.startAllocCounting()
        }
    }

    fun stopMonitoring() {
        (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(this)
        handler.removeCallbacks(leakCheckRunnable)
    }

    fun monitorFragment(fragment: Fragment, fragmentManager: FragmentManager) {
        val key = "${fragment.javaClass.simpleName}_${System.identityHashCode(fragment)}"
        fragmentReferences[key] = WeakReference(fragment)

        fragmentManager.registerFragmentLifecycleCallbacks(object : 
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                super.onFragmentDestroyed(fm, f)
                // Schedule leak check for destroyed fragment
                handler.postDelayed({
                    checkFragmentForLeak(f)
                }, 5000)
            }
        }, false)
    }

    private fun checkForLeaks() {
        // Force garbage collection
        System.gc()
        System.runFinalization()
        
        // Check activities
        activityReferences.forEach { (key, ref) ->
            val activity = ref.get()
            if (activity == null) {
                // Activity was garbage collected - good
                activityReferences.remove(key)
            } else if (!isActivityValid(activity)) {
                // Activity is still alive but should be dead
                notifyLeak(key, countReferences(activity))
            }
        }

        // Check fragments
        fragmentReferences.forEach { (key, ref) ->
            val fragment = ref.get()
            if (fragment == null) {
                fragmentReferences.remove(key)
            } else if (fragment.isRemoving || fragment.isDetached) {
                notifyLeak(key, countReferences(fragment))
            }
        }

        // Memory threshold warning
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        if (freeMemory < 10) { // Less than 10MB free
            notifyMemoryWarning(freeMemory)
        }
    }

    private fun isActivityValid(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            !activity.isDestroyed && !activity.isFinishing
        } else {
            !activity.isFinishing
        }
    }

    private fun countReferences(obj: Any): Int {
        return try {
            // This is a simplified reference count
            // In production, use LeakCanary or similar library
            val field = obj.javaClass.getDeclaredField("mReferenced")
            field.isAccessible = true
            val referenced = field.get(obj) as? Array<*>?
            referenced?.size ?: 0
        } catch (e: Exception) {
            -1
        }
    }

    private fun checkFragmentForLeak(fragment: Fragment) {
        handler.postDelayed({
            System.gc()
            if (fragmentReferences.values.any { it.get() == fragment }) {
                val stackTrace = getStackTrace(fragment)
                notifyPotentialLeak(fragment.javaClass.simpleName, stackTrace)
            }
        }, 10000)
    }

    private fun getStackTrace(obj: Any): String {
        return try {
            val throwable = Throwable()
            val writer = java.io.StringWriter()
            val printer = java.io.PrintWriter(writer)
            throwable.printStackTrace(printer)
            writer.toString()
        } catch (e: Exception) {
            "Unable to get stack trace"
        }
    }

    fun trackView(view: android.view.View) {
        val key = "${view.javaClass.simpleName}_${System.identityHashCode(view)}"
        viewReferences[key] = WeakReference(view)

        view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View?) {}
            override fun onViewDetachedFromWindow(v: android.view.View?) {
                handler.postDelayed({
                    checkViewForLeak(view)
                }, 3000)
            }
        })
    }

    private fun checkViewForLeak(view: android.view.View) {
        if (viewReferences.values.any { it.get() == view }) {
            Log.w(TAG, "Potential view leak: ${view.javaClass.simpleName}")
        }
    }

    private fun notifyLeak(className: String, references: Int) {
        listeners.forEach { it.onLeakDetected(className, references) }
        Log.e(TAG, "Memory leak detected: $className ($references references)")
    }

    private fun notifyPotentialLeak(className: String, stackTrace: String) {
        listeners.forEach { it.onPotentialLeak(className, stackTrace) }
        Log.w(TAG, "Potential memory leak: $className")
    }

    private fun notifyMemoryWarning(freeMemoryMb: Long) {
        listeners.forEach { it.onMemoryWarning(freeMemoryMb) }
        Log.w(TAG, "Low memory warning: $freeMemoryMb MB free")
    }

    fun addListener(listener: LeakListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: LeakListener) {
        listeners.remove(listener)
    }

    // ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val key = "${activity.javaClass.simpleName}_${System.identityHashCode(activity)}"
        activityReferences[key] = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        val key = "${activity.javaClass.simpleName}_${System.identityHashCode(activity)}"
        handler.postDelayed({
            checkForLeaks()
        }, 5000)
    }
}
