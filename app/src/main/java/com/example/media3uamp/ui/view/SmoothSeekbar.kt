package com.example.media3uamp.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.widget.AppCompatSeekBar
import kotlin.math.max
import kotlin.math.min

class SmoothSeekbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private companion object {
        private const val MIN_HEIGHT = 8
        private const val MAX_HEIGHT = 20
        private const val DURATION = 150L
    }

    private var fraction: Float = 0f
    private val dotRadius: Int = 5
    private val dotColor: Int = Color.WHITE
    private var markerPosition: Int = 0

    private val dotPaint: Paint = Paint().apply {
        color = dotColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var valueAnimator: ValueAnimator? = null

    private val animatorUpdateListenerUp = ValueAnimator.AnimatorUpdateListener { animator ->
        try {
            fraction = max(0f, fraction - (animator.animatedValue as Float))
            postInvalidate()
        } catch (_: Exception) {
        }
    }

    private val animatorUpdateListenerDown = ValueAnimator.AnimatorUpdateListener { animator ->
        try {
            fraction = max(0f, fraction - (animator.animatedValue as Float))
            postInvalidate()
        } catch (_: Exception) {
        }
    }

    private val animatorListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) = Unit

        override fun onAnimationEnd(animator: Animator) {
            try {
                valueAnimator = ValueAnimator.ofFloat(0f, 1.0f).apply {
                    duration = DURATION
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener(animatorUpdateListenerDown)
                    start()
                }
            } catch (_: Exception) {
            }
        }

        override fun onAnimationCancel(animator: Animator) = Unit

        override fun onAnimationRepeat(animator: Animator) = Unit
    }

    private val animatorUpdateListener = ValueAnimator.AnimatorUpdateListener { animator ->
        try {
            fraction = min(1f, fraction + (animator.animatedValue as Float))
            postInvalidate()
        } catch (_: Exception) {
        }
    }

    fun setFraction(fraction: Float) {
        this.fraction = fraction
    }

    override fun draw(canvas: Canvas) {
        try {
            val drawable = progressDrawable as? LayerDrawable
            val background =
                drawable?.findDrawableByLayerId(android.R.id.background) as? GradientDrawable
            val progress = drawable?.findDrawableByLayerId(android.R.id.progress) as? ClipDrawable

            if (background != null && progress != null) {
                val backgroundHeight = (MIN_HEIGHT + (MAX_HEIGHT - MIN_HEIGHT) * fraction).toInt()
                val viewHeight = height
                val left = 0
                val top = viewHeight / 2 - backgroundHeight / 2
                val right = width
                val bottom = viewHeight / 2 + backgroundHeight / 2
                background.setBounds(left, top, right, bottom)
                progress.setBounds(left, top, right, bottom)
            }

            val max = max
            if (markerPosition != 0 && max > 0) {
                val dotX = markerPosition.toFloat() * width / max
                val dotY = height / 2f
                canvas.drawCircle(dotX, dotY, dotRadius.toFloat(), dotPaint)
            }
        } catch (_: Exception) {
        }

        super.draw(canvas)
    }

    private fun startDownAnimation() {
        try {
            valueAnimator?.cancel()
            valueAnimator = ValueAnimator.ofFloat(0f, 1.0f).apply {
                duration = DURATION
                interpolator = AccelerateInterpolator()
                addUpdateListener(animatorUpdateListener)
                start()
            }
        } catch (_: Exception) {
        }
    }

    private fun startUpAnimation() {
        try {
            val running = valueAnimator?.isRunning == true
            if (running) {
                valueAnimator?.addListener(animatorListener)
                return
            }

            valueAnimator = ValueAnimator.ofFloat(0f, 1.0f).apply {
                duration = DURATION
                interpolator = AccelerateInterpolator()
                addUpdateListener(animatorUpdateListenerUp)
                start()
            }
        } catch (_: Exception) {
        }
    }

    fun setFixedDotProgress(progress: Int) {
        markerPosition = progress
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            valueAnimator?.let { animator ->
                if (animator.isRunning) animator.cancel()
            }
            valueAnimator = null
        } catch (_: Exception) {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            if (!isEnabled) {
                return false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> startDownAnimation()
                MotionEvent.ACTION_MOVE -> Unit
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                    -> startUpAnimation()
            }
        } catch (_: Exception) {
        }

        return super.onTouchEvent(event)
    }
}
