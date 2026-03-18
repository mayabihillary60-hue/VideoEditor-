package com.yourname.videoeditor.feature.editor.ui.animations

import android.animation.*
import android.view.View
import android.view.animation.*
import androidx.core.animation.addListener
import kotlin.math.*

enum class AnimationType {
    FADE_IN,
    FADE_OUT,
    SLIDE_IN_LEFT,
    SLIDE_IN_RIGHT,
    SLIDE_IN_TOP,
    SLIDE_IN_BOTTOM,
    SCALE_IN,
    SCALE_OUT,
    ROTATE_IN,
    BOUNCE,
    PULSE,
    SHAKE,
    FLIP_HORIZONTAL,
    FLIP_VERTICAL,
    CUSTOM
}

enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BOUNCE_IN,
    BOUNCE_OUT,
    ELASTIC_IN,
    ELASTIC_OUT,
    OVERSHOOT,
    BACK_IN,
    BACK_OUT
}

data class AnimationConfig(
    val type: AnimationType = AnimationType.FADE_IN,
    val duration: Long = 300,
    val delay: Long = 0,
    val easing: EasingType = EasingType.EASE_IN_OUT,
    val repeatCount: Int = 0,
    val repeatMode: Int = ValueAnimator.RESTART,
    val interpolator: TimeInterpolator? = null,
    val startOffset: Float = 0f,
    val endOffset: Float = 1f
)

class AnimationSystem {

    private val activeAnimations = mutableListOf<Animator>()

    interface AnimationListener {
        fun onAnimationStart(animation: Animator)
        fun onAnimationEnd(animation: Animator)
        fun onAnimationCancel(animation: Animator)
        fun onAnimationRepeat(animation: Animator)
    }

    fun animate(view: View, config: AnimationConfig, listener: AnimationListener? = null): Animator {
        val animator = createAnimator(view, config)
        
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                listener?.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation: Animator) {
                listener?.onAnimationEnd(animation)
                activeAnimations.remove(animation)
            }

            override fun onAnimationCancel(animation: Animator) {
                listener?.onAnimationCancel(animation)
                activeAnimations.remove(animation)
            }

            override fun onAnimationRepeat(animation: Animator) {
                listener?.onAnimationRepeat(animation)
            }
        })

        animator.start()
        activeAnimations.add(animator)
        return animator
    }

    private fun createAnimator(view: View, config: AnimationConfig): Animator {
        val interpolator = config.interpolator ?: getInterpolator(config.easing)
        
        return when (config.type) {
            AnimationType.FADE_IN -> createFadeInAnimator(view, config, interpolator)
            AnimationType.FADE_OUT -> createFadeOutAnimator(view, config, interpolator)
            AnimationType.SLIDE_IN_LEFT -> createSlideInLeftAnimator(view, config, interpolator)
            AnimationType.SLIDE_IN_RIGHT -> createSlideInRightAnimator(view, config, interpolator)
            AnimationType.SLIDE_IN_TOP -> createSlideInTopAnimator(view, config, interpolator)
            AnimationType.SLIDE_IN_BOTTOM -> createSlideInBottomAnimator(view, config, interpolator)
            AnimationType.SCALE_IN -> createScaleInAnimator(view, config, interpolator)
            AnimationType.SCALE_OUT -> createScaleOutAnimator(view, config, interpolator)
            AnimationType.ROTATE_IN -> createRotateInAnimator(view, config, interpolator)
            AnimationType.BOUNCE -> createBounceAnimator(view, config, interpolator)
            AnimationType.PULSE -> createPulseAnimator(view, config, interpolator)
            AnimationType.SHAKE -> createShakeAnimator(view, config, interpolator)
            AnimationType.FLIP_HORIZONTAL -> createFlipHorizontalAnimator(view, config, interpolator)
            AnimationType.FLIP_VERTICAL -> createFlipVerticalAnimator(view, config, interpolator)
            AnimationType.CUSTOM -> createCustomAnimator(view, config)
        }
    }

    private fun getInterpolator(easing: EasingType): TimeInterpolator {
        return when (easing) {
            EasingType.LINEAR -> LinearInterpolator()
            EasingType.EASE_IN -> AccelerateInterpolator()
            EasingType.EASE_OUT -> DecelerateInterpolator()
            EasingType.EASE_IN_OUT -> AccelerateDecelerateInterpolator()
            EasingType.BOUNCE_IN -> BounceInterpolator().apply { 
                // Custom bounce in
            }
            EasingType.BOUNCE_OUT -> BounceInterpolator()
            EasingType.ELASTIC_IN -> ElasticInterpolator(ElasticInterpolator.ELASTIC_IN)
            EasingType.ELASTIC_OUT -> ElasticInterpolator(ElasticInterpolator.ELASTIC_OUT)
            EasingType.OVERSHOOT -> OvershootInterpolator()
            EasingType.BACK_IN -> AnticipateInterpolator()
            EasingType.BACK_OUT -> OvershootInterpolator()
        }
    }

    private fun createFadeInAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        animator.duration = config.duration
        animator.startDelay = config.delay
        animator.interpolator = interpolator
        animator.repeatCount = config.repeatCount
        animator.repeatMode = config.repeatMode
        return animator
    }

    private fun createFadeOutAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        animator.duration = config.duration
        animator.startDelay = config.delay
        animator.interpolator = interpolator
        animator.repeatCount = config.repeatCount
        animator.repeatMode = config.repeatMode
        return animator
    }

    private fun createSlideInLeftAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val translationX = ObjectAnimator.ofFloat(view, "translationX", -view.width.toFloat(), 0f)
        translationX.duration = config.duration
        translationX.startDelay = config.delay
        translationX.interpolator = interpolator

        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = config.duration
        alpha.startDelay = config.delay

        val set = AnimatorSet()
        set.playTogether(translationX, alpha)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createSlideInRightAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val translationX = ObjectAnimator.ofFloat(view, "translationX", view.width.toFloat(), 0f)
        translationX.duration = config.duration
        translationX.startDelay = config.delay
        translationX.interpolator = interpolator

        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = config.duration
        alpha.startDelay = config.delay

        val set = AnimatorSet()
        set.playTogether(translationX, alpha)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createSlideInTopAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val translationY = ObjectAnimator.ofFloat(view, "translationY", -view.height.toFloat(), 0f)
        translationY.duration = config.duration
        translationY.startDelay = config.delay
        translationY.interpolator = interpolator

        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = config.duration
        alpha.startDelay = config.delay

        val set = AnimatorSet()
        set.playTogether(translationY, alpha)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createSlideInBottomAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val translationY = ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
        translationY.duration = config.duration
        translationY.startDelay = config.delay
        translationY.interpolator = interpolator

        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = config.duration
        alpha.startDelay = config.delay

        val set = AnimatorSet()
        set.playTogether(translationY, alpha)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createScaleInAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        
        scaleX.duration = config.duration
        scaleY.duration = config.duration
        scaleX.startDelay = config.delay
        scaleY.startDelay = config.delay
        scaleX.interpolator = interpolator
        scaleY.interpolator = interpolator

        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createScaleOutAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        
        scaleX.duration = config.duration
        scaleY.duration = config.duration
        scaleX.startDelay = config.delay
        scaleY.startDelay = config.delay
        scaleX.interpolator = interpolator
        scaleY.interpolator = interpolator

        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createRotateInAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val rotation = ObjectAnimator.ofFloat(view, "rotation", -180f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        rotation.duration = config.duration
        alpha.duration = config.duration
        rotation.startDelay = config.delay
        alpha.startDelay = config.delay
        rotation.interpolator = interpolator
        alpha.interpolator = interpolator

        val set = AnimatorSet()
        set.playTogether(rotation, alpha)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createBounceAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f, -50f, 0f, -25f, 0f)
        animator.duration = config.duration
        animator.startDelay = config.delay
        animator.interpolator = interpolator
        animator.repeatCount = config.repeatCount
        animator.repeatMode = config.repeatMode
        return animator
    }

    private fun createPulseAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        
        scaleX.duration = config.duration
        scaleY.duration = config.duration
        scaleX.startDelay = config.delay
        scaleY.startDelay = config.delay
        scaleX.interpolator = interpolator
        scaleY.interpolator = interpolator

        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.repeatCount = config.repeatCount
        set.repeatMode = config.repeatMode
        return set
    }

    private fun createShakeAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
        animator.duration = config.duration
        animator.startDelay = config.delay
        animator.interpolator = interpolator
        animator.repeatCount = config.repeatCount
        animator.repeatMode = config.repeatMode
        return animator
    }

    private fun createFlipHorizontalAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val rotationY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f)
        rotationY.duration = config.duration
        rotationY.startDelay = config.delay
        rotationY.interpolator = interpolator
        rotationY.repeatCount = config.repeatCount
        rotationY.repeatMode = config.repeatMode
        return rotationY
    }

    private fun createFlipVerticalAnimator(view: View, config: AnimationConfig, interpolator: TimeInterpolator): Animator {
        val rotationX = ObjectAnimator.ofFloat(view, "rotationX", 0f, 360f)
        rotationX.duration = config.duration
        rotationX.startDelay = config.delay
        rotationX.interpolator = interpolator
        rotationX.repeatCount = config.repeatCount
        rotationX.repeatMode = config.repeatMode
        return rotationX
    }

    private fun createCustomAnimator(view: View, config: AnimationConfig): Animator {
        // Custom animation can be defined by subclasses
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = config.duration
            startDelay = config.delay
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode
            addUpdateListener {
                val value = it.animatedValue as Float
                // Apply custom transformation
                view.alpha = value
                view.scaleX = 1f + value * 0.5f
                view.scaleY = 1f + value * 0.5f
            }
        }
    }

    fun cancelAll() {
        activeAnimations.forEach { it.cancel() }
        activeAnimations.clear()
    }

    fun pauseAll() {
        activeAnimations.forEach { it.pause() }
    }

    fun resumeAll() {
        activeAnimations.forEach { it.resume() }
    }
}

// Custom Elastic Interpolator
class ElasticInterpolator(private val type: Int) : TimeInterpolator {
    companion object {
        const val ELASTIC_IN = 0
        const val ELASTIC_OUT = 1
    }

    override fun getInterpolation(input: Float): Float {
        return when (type) {
            ELASTIC_IN -> elasticIn(input)
            ELASTIC_OUT -> elasticOut(input)
            else -> input
        }
    }

    private fun elasticIn(t: Float): Float {
        return if (t == 0f) 0f else {
            val p = 0.3f
            val s = p / 4f
            val a = 1f
            val postFix = a * 2.0f.pow(-10f * t)
            -postFix * sin((t - s) * (2 * PI).toFloat() / p) + 1f
        }
    }

    private fun elasticOut(t: Float): Float {
        return if (t == 1f) 1f else {
            val p = 0.3f
            val s = p / 4f
            val a = 1f
            2.0f.pow(-10f * t) * sin((t - s) * (2 * PI).toFloat() / p) + 1f
        }
    }
}
