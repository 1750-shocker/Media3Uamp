package com.example.media3uamp.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class CustomImageViewButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val baseAlpha: Float

    init {
        scaleX = 1.0f
        scaleY = 1.0f
        baseAlpha = alpha
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scaleX = 0.95f
                scaleY = 0.95f
                alpha = baseAlpha * 0.8f
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
                -> {
                scaleX = 1.0f
                scaleY = 1.0f
                alpha = baseAlpha
            }
        }
        return super.onTouchEvent(event)
    }
}
