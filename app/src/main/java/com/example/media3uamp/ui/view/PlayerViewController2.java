package com.example.media3uamp.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.media3uamp.R;

public class PlayerViewController2 extends FrameLayout {
    private ConstraintLayout llController;
    private TextView tvCurDuration;
    private TextView tvDuration;
    private SmoothSeekbar seekBar;
    private ImageView ivPlay, ivPrevious, ivNext, ivShuffle, ivRepeat, ivFavorite;

    public interface PlayerControllerListener {
        void onPlayToggle();
        void onPreviousClick();
        void onNextClick();
        void onShuffleToggle();
        void onRepeatToggle();
        void onFavoriteClick();
        void onSeekTo(int progress);
    }

    private PlayerControllerListener controllerListener;

    public PlayerViewController2(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public PlayerViewController2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PlayerViewController2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_player_controller2, this, false);
        llController = view.findViewById(R.id.ll_controller);
        tvCurDuration = view.findViewById(R.id.tv_current_duration);
        tvDuration = view.findViewById(R.id.tv_duration);
        seekBar = view.findViewById(R.id.seekbar);
        ivPlay = view.findViewById(R.id.iv_play);
        ivPrevious = view.findViewById(R.id.iv_previous);
        ivNext = view.findViewById(R.id.iv_next);
        ivShuffle = view.findViewById(R.id.iv_shuffle);
        ivRepeat = view.findViewById(R.id.iv_repeat);
        ivFavorite = view.findViewById(R.id.iv_favorite);

        ivPlay.setOnClickListener(v -> { if (controllerListener != null) controllerListener.onPlayToggle(); });
        ivPrevious.setOnClickListener(v -> { if (controllerListener != null) controllerListener.onPreviousClick(); });
        ivNext.setOnClickListener(v -> { if (controllerListener != null) controllerListener.onNextClick(); });
        ivShuffle.setOnClickListener(v -> { if (controllerListener != null) controllerListener.onShuffleToggle(); });
        ivRepeat.setOnClickListener(v -> { if (controllerListener != null) controllerListener.onRepeatToggle(); });
        ivFavorite.setOnClickListener(v -> { if (controllerListener != null) controllerListener.onFavoriteClick(); });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (controllerListener != null) controllerListener.onSeekTo(seekBar.getProgress());
            }
        });

        this.addView(view);
    }

    public void setControllerListener(PlayerControllerListener listener) { this.controllerListener = listener; }

    public void setDurations(long currentMs, long totalMs) {
        tvCurDuration.setText(formatTime(currentMs));
        tvDuration.setText(formatTime(totalMs));
        seekBar.setMax((int) totalMs);
        seekBar.setProgress((int) currentMs);
    }

    public void setPlaying(boolean playing) {
        ivPlay.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    public void setShuffle(boolean enabled) {
        ivShuffle.setAlpha(enabled ? 1f : 0.5f);
    }

    public void setRepeatMode(int mode) {
        ivRepeat.setAlpha(mode == 0 ? 0.5f : 1f);
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
