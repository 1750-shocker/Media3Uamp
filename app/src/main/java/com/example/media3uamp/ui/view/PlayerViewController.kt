package com.example.media3uamp.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import com.example.media3uamp.R
import kotlin.math.min

class PlayerViewController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var llController: ConstraintLayout
    private lateinit var tvCurDuration: TextView
    private lateinit var tvDuration: TextView
    private lateinit var seekBar: SmoothSeekbar
    private lateinit var ivPlay: ImageView
    private lateinit var ivPrevious: ImageView
    private lateinit var ivNext: ImageView

    interface PlayerControllerListener {
        fun onPlayToggle()
        fun onPreviousClick()
        fun onNextClick()
        fun onSeekTo(progress: Int)
    }

    private var controllerListener: PlayerControllerListener? = null

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        val view =
            LayoutInflater.from(context).inflate(R.layout.layout_player_controller, this, false)
        llController = view.findViewById(R.id.ll_controller)
        tvCurDuration = view.findViewById(R.id.tv_current_duration)
        tvDuration = view.findViewById(R.id.tv_duration)
        seekBar = view.findViewById(R.id.seekbar)
        ivPlay = view.findViewById(R.id.iv_play)
        ivPrevious = view.findViewById(R.id.iv_previous)
        ivNext = view.findViewById(R.id.iv_next)

        seekBar.progressDrawable =
            ContextCompat.getDrawable(this.context, R.drawable.seekbar_style_drawables)

        ivPlay.setOnClickListener { controllerListener?.onPlayToggle() }
        ivPrevious.setOnClickListener { controllerListener?.onPreviousClick() }
        ivNext.setOnClickListener { controllerListener?.onNextClick() }

        applyScale(ivPlay)
        applyScale(ivPrevious)
        applyScale(ivNext)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) =
                Unit

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                controllerListener?.onSeekTo(seekBar.progress)
            }
        })

        addView(view)
    }

    fun setControllerListener(listener: PlayerControllerListener?) {
        controllerListener = listener
    }

    fun setDurations(currentMs: Long, totalMs: Long) {
        val safeTotal = if (totalMs == C.TIME_UNSET || totalMs <= 0) 0 else totalMs
        val safeCurrent =
            if (currentMs == C.TIME_UNSET || currentMs < 0) 0 else min(currentMs, safeTotal)
        tvCurDuration.text = formatTime(safeCurrent)
        tvDuration.text = if (safeTotal == 0L) "--:--" else formatTime(safeTotal)
        val max = min(Int.MAX_VALUE.toLong(), safeTotal).toInt()
        val progress = min(max, safeCurrent.toInt())
        seekBar.max = max
        seekBar.progress = progress
    }

    fun setPlaying(playing: Boolean) {
        ivPlay.setImageResource(if (playing) R.drawable.ic_player_play else R.drawable.ic_player_pause)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    companion object {
        @JvmStatic
        fun applyScale(view: View) {
            applyScale(view, 0.9f, 100)
        }

        @SuppressLint("ClickableViewAccessibility")
        @JvmStatic
        fun applyScale(view: View, scale: Float, duration: Int) {
            view.isClickable = true
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(scale).scaleY(scale)
                        .setDuration(duration.toLong()).start()

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                        -> v.animate().scaleX(1f).scaleY(1f).setDuration(duration.toLong()).start()
                }
                false
            }
        }
    }
}
