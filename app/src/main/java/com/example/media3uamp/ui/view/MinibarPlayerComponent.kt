package com.example.media3uamp.ui.view

import android.graphics.RectF
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.media3uamp.R
import com.example.media3uamp.databinding.LayoutMinibarPlayerControllerBinding
import com.example.media3uamp.playback.PlaybackClient
import com.example.media3uamp.ui.player.PlayerFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import jp.wasabeef.glide.transformations.BlurTransformation

class MinibarPlayerComponent(
    private val activity: AppCompatActivity,
    private val minibarBinding: LayoutMinibarPlayerControllerBinding,
    private val navController: NavController,
    private val navHostFragmentId: Int,
    private val navHostView: View,
    private val playerDestinationId: Int,
) {
    private var controller: MediaController? = null
    private var controllerListener: Player.Listener? = null
    private var destinationChangedListener: NavController.OnDestinationChangedListener? = null

    fun bind() {
        minibarBinding.ivPrevious.setOnClickListener { controller?.seekToPrevious() }
        minibarBinding.ivNext.setOnClickListener { controller?.seekToNext() }
        minibarBinding.ivPlay.setOnClickListener {
            val c = controller ?: return@setOnClickListener
            if (c.isPlaying) c.pause() else c.play()
            updateMinibarState(c)
        }
        val openPlayerClickListener = View.OnClickListener {
            if (minibarBinding.root.visibility != View.VISIBLE) return@OnClickListener
            val snapshotView = resolveSnapshotView()
            val masks = createRoundedCoverMasks(snapshotView)
            PlayerFragment.Companion.setBackgroundSnapshot(snapshotView, masks)
            navController.navigate(playerDestinationId, Bundle.EMPTY)
        }
        minibarBinding.layoutMusicCover.setOnClickListener(openPlayerClickListener)
        minibarBinding.layoutMusicInfo.setOnClickListener(openPlayerClickListener)

        destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val c = controller
            val shouldShow = destination.id != playerDestinationId && c?.currentMediaItem != null
            minibarBinding.root.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }.also { navController.addOnDestinationChangedListener(it) }

        CoroutineScope(Dispatchers.Main).launch {
            val controller = PlaybackClient.getController(activity)
            this@MinibarPlayerComponent.controller = controller
            controllerListener?.let { controller.removeListener(it) }
            controllerListener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateMinibarState(player)
                    val destId = navController.currentDestination?.id
                    val shouldShow = destId != playerDestinationId && player.currentMediaItem != null
                    minibarBinding.root.visibility = if (shouldShow) View.VISIBLE else View.GONE
                }
            }
            controller.addListener(controllerListener!!)
            updateMinibarState(controller)
            val destId = navController.currentDestination?.id
            val shouldShow = destId != playerDestinationId && controller.currentMediaItem != null
            minibarBinding.root.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
    }

    fun unbind() {
        destinationChangedListener?.let { navController.removeOnDestinationChangedListener(it) }
        destinationChangedListener = null

        CoroutineScope(Dispatchers.Main).launch {
            controllerListener?.let { listener -> controller?.removeListener(listener) }
            controllerListener = null
            controller = null
        }
    }

    private fun resolveSnapshotView(): View {
        val navHost = activity.supportFragmentManager.findFragmentById(navHostFragmentId) as? NavHostFragment
        val currentFragmentView = navHost
            ?.childFragmentManager
            ?.primaryNavigationFragment
            ?.view
        return currentFragmentView ?: navHostView
    }

    private fun updateMinibarState(player: Player) {
        val md = player.mediaMetadata
        minibarBinding.tvMusicName.text = md.title ?: activity.getString(R.string.unknown)
        minibarBinding.tvSingerName.text = md.artist ?: activity.getString(R.string.unknown)
        minibarBinding.ivPlay.setImageResource(if (player.playWhenReady) R.drawable.icon_minibar_play else R.drawable.icon_minibar_pause)

        val artworkUri = md.artworkUri
        if (artworkUri == null) {
            minibarBinding.ivMusicCover.setImageResource(R.drawable.ic_player_default_cover)
            minibarBinding.ivCoverBg.setImageDrawable(null)
        } else {
            Glide.with(minibarBinding.ivMusicCover)
                .load(artworkUri)
                .placeholder(R.drawable.ic_player_default_cover)
                .into(minibarBinding.ivMusicCover)
            Glide.with(minibarBinding.ivCoverBg)
                .load(artworkUri)
                .transform(BlurTransformation(23, 4))
                .into(minibarBinding.ivCoverBg)
        }
    }

    private fun createRoundedCoverMasks(root: View): List<PlayerFragment.Companion.RoundedRectMask> {
        val fillColor = MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface)

        val recycler = root.findViewById<RecyclerView?>(R.id.recycler)
        if (recycler != null && recycler.childCount > 0) {
            val masks = ArrayList<PlayerFragment.Companion.RoundedRectMask>(recycler.childCount)
            val rootLoc = IntArray(2)
            root.getLocationInWindow(rootLoc)
            for (i in 0 until recycler.childCount) {
                val item = recycler.getChildAt(i) ?: continue
                val cover = item.findViewById<View?>(R.id.cover) ?: continue
                val coverCard = item.findViewById<MaterialCardView?>(R.id.coverCard) ?: continue
                val mask = createRoundedCoverMask(root, rootLoc, cover, coverCard, fillColor) ?: continue
                masks.add(mask)
            }
            if (masks.isNotEmpty()) return masks
        }

        val cover = root.findViewById<View?>(R.id.cover) ?: return emptyList()
        val coverCard = root.findViewById<MaterialCardView?>(R.id.coverCard) ?: return emptyList()
        val rootLoc = IntArray(2)
        root.getLocationInWindow(rootLoc)
        val mask = createRoundedCoverMask(root, rootLoc, cover, coverCard, fillColor) ?: return emptyList()
        return listOf(mask)
    }

    private fun createRoundedCoverMask(
        root: View,
        rootLoc: IntArray,
        cover: View,
        coverCard: MaterialCardView,
        fillColor: Int,
    ): PlayerFragment.Companion.RoundedRectMask? {
        if (cover.width <= 0 || cover.height <= 0 || root.width <= 0 || root.height <= 0) return null

        val bounds = RectF(0f, 0f, coverCard.width.toFloat(), coverCard.height.toFloat())
        val model = coverCard.shapeAppearanceModel
        val tl = model.topLeftCornerSize.getCornerSize(bounds)
        val tr = model.topRightCornerSize.getCornerSize(bounds)
        val br = model.bottomRightCornerSize.getCornerSize(bounds)
        val bl = model.bottomLeftCornerSize.getCornerSize(bounds)
        val maxRadius = maxOf(tl, tr, br, bl)
        if (maxRadius <= 0f) return null

        val coverLoc = IntArray(2)
        cover.getLocationInWindow(coverLoc)

        val left = (coverLoc[0] - rootLoc[0]).toFloat()
        val top = (coverLoc[1] - rootLoc[1]).toFloat()
        val right = left + cover.width.toFloat()
        val bottom = top + cover.height.toFloat()

        val radii = floatArrayOf(
            tl, tl,
            tr, tr,
            br, br,
            bl, bl,
        )

        return PlayerFragment.Companion.RoundedRectMask(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radius = maxRadius,
            fillColor = fillColor,
            radii = radii,
        )
    }
}
