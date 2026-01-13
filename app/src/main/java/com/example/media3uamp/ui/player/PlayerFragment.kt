package com.example.media3uamp.ui.player

import android.os.Bundle
import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.VelocityTracker
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.media3uamp.R
import com.example.media3uamp.databinding.FragmentPlayerBinding
import com.example.media3uamp.playback.PlaybackConnectionManager
import com.example.media3uamp.ui.view.PlayerViewController
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private var player: Player? = null
    private var playerListener: Player.Listener? = null
    private var progressJob: Job? = null
    private var coverAnimator: ObjectAnimator? = null
    private var swipeVelocityTracker: VelocityTracker? = null
    private var swipeDownY = 0f
    private var swipeDownX = 0f
    private var swipeIsDragging = false
    private var swipeCanStart = false
    private var startedSheetEnterAnimation = false

    @Inject
    lateinit var playbackConnectionManager: PlaybackConnectionManager

    companion object {
        private var backgroundSnapshot: Bitmap? = null

        data class RoundedRectMask(
            val left: Float,
            val top: Float,
            val right: Float,
            val bottom: Float,
            val radius: Float,
            val fillColor: Int,
            val radii: FloatArray? = null,
        )

        fun setBackgroundSnapshot(view: View, masks: List<RoundedRectMask> = emptyList()) {
            val width = view.width
            val height = view.height
            if (width <= 0 || height <= 0) return

            val fillColor =
                MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurface)
            val original = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(original)
            canvas.drawColor(fillColor)
            view.draw(canvas)

            masks.forEach { mask ->
                applyRoundedRectMask(
                    bitmap = original,
                    rect = RectF(mask.left, mask.top, mask.right, mask.bottom),
                    radius = mask.radius,
                    fillColor = mask.fillColor,
                    radii = mask.radii,
                )
            }
            val maxSide = 1080
            val scale = min(1f, maxSide.toFloat() / max(original.width, original.height).toFloat())
            if (scale >= 1f) {
                backgroundSnapshot = original
                return
            }
            val w = max(1, (original.width * scale).toInt())
            val h = max(1, (original.height * scale).toInt())
            val scaled = Bitmap.createScaledBitmap(original, w, h, true)
            if (scaled != original) original.recycle()
            backgroundSnapshot = scaled
        }

        private fun applyRoundedRectMask(
            bitmap: Bitmap,
            rect: RectF,
            radius: Float,
            fillColor: Int,
            radii: FloatArray? = null,
        ) {
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = fillColor
            }
            val full = Path().apply { addRect(rect, Path.Direction.CW) }
            val round = Path().apply {
                if (radii != null) {
                    addRoundRect(rect, radii, Path.Direction.CW)
                } else {
                    addRoundRect(rect, radius, radius, Path.Direction.CW)
                }
            }
            full.op(round, Path.Op.DIFFERENCE)
            canvas.drawPath(full, paint)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initCoverAnimator()
        binding.underlaySnapshot.setImageBitmap(backgroundSnapshot)
        backgroundSnapshot = null
        setupSheetEnterAnimation()
        setupSwipeToDismiss()
        val albumId = requireArguments().getString("albumId").orEmpty()
        val hasAlbumId = albumId.isNotBlank()
        var index = requireArguments().getInt("trackIndex", 0)
        requireArguments().getString("trackTitle")?.let { binding.title.text = it }
        requireArguments().getString("trackArtist")?.let { binding.artist.text = it }

        CoroutineScope(Dispatchers.Main).launch {
            val controller = playbackConnectionManager.getController()
            player = controller
            if (!hasAlbumId) {
                bindToController(controller)
                return@launch
            }

            val browser = playbackConnectionManager.getBrowser()
            val parentId = "album:$albumId"
            val children = browser.getChildren(parentId, 0, Int.MAX_VALUE, null).await()
            val tracks = children.value ?: emptyList()
            if (tracks.isEmpty()) {
                Toast.makeText(requireContext(), "该专辑暂无曲目或数据加载失败", Toast.LENGTH_SHORT)
                    .show()
                bindToController(controller)
                return@launch
            }
            if (index !in tracks.indices) index = 0
            val placeholders =
                tracks.map { item -> MediaItem.Builder().setMediaId(item.mediaId).build() }
            controller.setMediaItems(placeholders)
            controller.seekToDefaultPosition(index)
            controller.prepare()
            controller.play()
            bindToController(controller)
        }
    }

    private fun bindToController(controller: Player) {
        updateMetadata(controller)
        _binding?.playerController?.setDurations(controller.currentPosition, controller.duration)
        _binding?.playerController?.setPlaying(controller.playWhenReady)
        updateCoverRotation(controller.playWhenReady)
        playerListener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updateMetadata(player)
                _binding?.playerController?.setDurations(player.currentPosition, player.duration)
                _binding?.playerController?.setPlaying(player.playWhenReady)
                updateCoverRotation(player.playWhenReady)
            }
        }
        controller.addListener(playerListener!!)
        binding.playerController.setControllerListener(object :
            PlayerViewController.PlayerControllerListener {
            override fun onPlayToggle() {
                if (controller.isPlaying) controller.pause() else controller.play()
                updateCoverRotation(controller.playWhenReady)
            }

            override fun onPreviousClick() {
                controller.seekToPrevious()
            }

            override fun onNextClick() {
                controller.seekToNext()
            }

            override fun onSeekTo(progress: Int) {
                controller.seekTo(progress.toLong())
            }
        })
        binding.playerController.setPlaying(controller.playWhenReady)

        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (_binding != null) {
                binding.playerController.setDurations(
                    controller.currentPosition,
                    controller.duration
                )
                delay(1000)
            }
        }
    }

    private fun setupSheetEnterAnimation() {
        val sheet = binding.sheet
        sheet.doOnPreDraw {
            if (startedSheetEnterAnimation) return@doOnPreDraw
            startedSheetEnterAnimation = true
            sheet.translationY = sheet.height.toFloat()
            sheet.alpha = 1f
            val interpolator = AnimationUtils.loadInterpolator(
                requireContext(),
                android.R.interpolator.linear_out_slow_in
            )
            sheet.animate()
                .translationY(0f)
                .setDuration(320L)
                .setInterpolator(interpolator)
                .start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeToDismiss() {
        val sheet = binding.sheet
        val touchSlop = 12f * resources.displayMetrics.density
        val dismissDistanceRatio = 0.25f

        sheet.setOnTouchListener { v, event ->
            val b = _binding ?: return@setOnTouchListener false
            val tracker =
                swipeVelocityTracker ?: VelocityTracker.obtain().also { swipeVelocityTracker = it }
            tracker.addMovement(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeDownY = event.rawY
                    swipeDownX = event.rawX
                    swipeIsDragging = false
                    swipeCanStart = event.y < b.playerController.top
                    swipeCanStart
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!swipeCanStart) return@setOnTouchListener false
                    val dy = event.rawY - swipeDownY
                    val dx = event.rawX - swipeDownX
                    if (!swipeIsDragging) {
                        if (dy > touchSlop && abs(dy) > abs(dx)) {
                            swipeIsDragging = true
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        } else {
                            return@setOnTouchListener true
                        }
                    }
                    if (swipeIsDragging) {
                        val translation = max(0f, dy)
                        v.translationY = translation
                        val progress = (translation / max(1f, v.height.toFloat())).coerceIn(0f, 1f)
                        v.alpha = 1f - progress * 0.15f
                        true
                    } else {
                        true
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                    -> {
                    if (!swipeIsDragging) {
                        swipeVelocityTracker?.recycle()
                        swipeVelocityTracker = null
                        swipeCanStart = false
                        return@setOnTouchListener false
                    }

                    tracker.computeCurrentVelocity(1000)
                    val velocityY = tracker.yVelocity
                    swipeVelocityTracker?.recycle()
                    swipeVelocityTracker = null
                    swipeCanStart = false
                    swipeIsDragging = false

                    val dismissDistance = v.height * dismissDistanceRatio
                    val shouldDismiss = v.translationY >= dismissDistance || velocityY > 1600f

                    if (shouldDismiss) {
                        val interpolator = AnimationUtils.loadInterpolator(
                            requireContext(),
                            android.R.interpolator.fast_out_linear_in
                        )
                        v.animate()
                            .translationY(v.height.toFloat())
                            .alpha(0.85f)
                            .setDuration(180L)
                            .setInterpolator(interpolator)
                            .withEndAction {
                                if (isAdded) findNavController().popBackStack()
                            }
                            .start()
                    } else {
                        val interpolator = AnimationUtils.loadInterpolator(
                            requireContext(),
                            android.R.interpolator.linear_out_slow_in
                        )
                        v.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(180L)
                            .setInterpolator(interpolator)
                            .start()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun initCoverAnimator() {
        if (coverAnimator == null) {
            coverAnimator = ObjectAnimator.ofFloat(binding.cover, "rotation", 0f, 360f)
            coverAnimator?.duration = 20000
            coverAnimator?.interpolator = LinearInterpolator()
            coverAnimator?.repeatCount = ObjectAnimator.INFINITE
            coverAnimator?.repeatMode = ObjectAnimator.RESTART
        }
    }

    private fun updateCoverRotation(isPlaying: Boolean) {
        val animator = coverAnimator ?: return
        if (isPlaying) {
            if (!animator.isStarted) animator.start() else animator.resume()
        } else {
            if (animator.isRunning) animator.pause()
        }
    }

    private fun updateMetadata(player: Player) {
        val b = _binding ?: return
        val md = player.mediaMetadata
        b.title.text = md.title ?: ""
        b.artist.text = md.artist ?: ""
        md.artworkUri?.let {
            Glide.with(b.cover).load(it).circleCrop().placeholder(R.drawable.album_placeholder)
                .into(b.cover)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroyView() {
        coverAnimator?.cancel()
        coverAnimator = null
        playerListener?.let { listener -> player?.removeListener(listener) }
        playerListener = null
        progressJob?.cancel()
        progressJob = null
        binding.sheet.setOnTouchListener(null)
        binding.underlaySnapshot.setImageDrawable(null)
        swipeVelocityTracker?.recycle()
        swipeVelocityTracker = null
        _binding = null
        startedSheetEnterAnimation = false
        super.onDestroyView()
    }
}

