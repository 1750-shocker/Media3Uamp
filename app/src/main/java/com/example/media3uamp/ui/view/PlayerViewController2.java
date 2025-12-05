package com.example.media3uamp.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.media3uamp.R;

public class PlayerViewController2 extends FrameLayout {
    private static final String TAG = "PlayerViewController2";
    private Context mContext;
    private ConstraintLayout llController;
    private TextView tvCurDuration;
    private TextView tvDuration;
    private SmoothSeekbar seekBar;
    private ImageView ivPlay, ivPause, ivPrevious, ivNext, ivPlayMode, ivFavorite, ivSoundEffect, ivPlayList;


    public PlayerViewController2(@NonNull Context context) {
        super(context);
        initView(context, null);
    }

    public PlayerViewController2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public PlayerViewController2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        this.mContext = context;

        View view = LayoutInflater.from(context).inflate(R.layout.layout_player_controller2, null);
        llController = view.findViewById(R.id.ll_controller);

        //播放进度条
        tvCurDuration = view.findViewById(R.id.tv_current_duration);
        tvDuration = view.findViewById(R.id.tv_duration);
        seekBar = view.findViewById(R.id.seekbar);
        //播放器按键
        ivPlay = view.findViewById(R.id.iv_play);
        ivPause = view.findViewById(R.id.iv_pause);
        ivPrevious = view.findViewById(R.id.iv_previous);
        ivNext = view.findViewById(R.id.iv_next);
        ivPlayMode = view.findViewById(R.id.iv_play_mode);
        ivFavorite = view.findViewById(R.id.iv_favorite);
        ivSoundEffect = view.findViewById(R.id.iv_sound_effect);
        ivPlayList = view.findViewById(R.id.iv_play_list);

        ivPlay.setOnClickListener(onClickListener);
        ivPause.setOnClickListener(onClickListener);
        ivPlayMode.setOnClickListener(onClickListener);
        ivFavorite.setOnClickListener(onClickListener);
        ivSoundEffect.setOnClickListener(onClickListener);
        ivPlayList.setOnClickListener(onClickListener);

        ivPrevious.setOnTouchListener(previousGestureListener);
        ivNext.setOnTouchListener(nextGestureListener);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlayerView);
            boolean isShowPlayMode = typedArray.getBoolean(R.styleable.PlayerView_isShowPlayMode, true);
            boolean isShowFavorite = typedArray.getBoolean(R.styleable.PlayerView_isShowFavorite, true);
            boolean isShowPrevious = typedArray.getBoolean(R.styleable.PlayerView_isShowPrevious, true);
            boolean isShowNext = typedArray.getBoolean(R.styleable.PlayerView_isShowNext, true);
            boolean isShowSoundEffect = typedArray.getBoolean(R.styleable.PlayerView_isShowSoundEffect, true);
            boolean isShowPlayList = typedArray.getBoolean(R.styleable.PlayerView_isShowPlayList, true);
            float playButtonWidth = typedArray.getDimension(R.styleable.PlayerView_playButtonWidth, 340);
            float buttonWidth = typedArray.getDimension(R.styleable.PlayerView_buttonWidth, 170);

            if (isShowPlayMode) {
                ivPlayMode.setVisibility(VISIBLE);
                ivPlayMode.setEnabled(true);
            } else {
                ivPlayMode.setVisibility(GONE);
                ivPlayMode.setEnabled(false);
            }

            setFavoriteState(isShowFavorite);

            if (isShowPrevious) {
                ivPrevious.setVisibility(VISIBLE);
                ivPrevious.setEnabled(true);
            } else {
                ivPrevious.setVisibility(INVISIBLE);
                ivPrevious.setEnabled(false);
            }

            if (isShowNext) {
                ivNext.setVisibility(VISIBLE);
                ivNext.setEnabled(true);
            } else {
                ivNext.setVisibility(INVISIBLE);
                ivNext.setEnabled(false);
            }

            if (isShowSoundEffect) {
                ivSoundEffect.setVisibility(VISIBLE);
                ivSoundEffect.setEnabled(true);
            } else {
                ivSoundEffect.setVisibility(GONE);
                ivSoundEffect.setEnabled(false);
            }

            if (isShowPlayList) {
                ivPlayList.setVisibility(VISIBLE);
                ivPlayList.setEnabled(true);
            } else {
                ivPlayList.setVisibility(GONE);
                ivPlayList.setEnabled(false);
            }

//            ViewGroup.LayoutParams playButtonLayoutParams = flPlayButton.getLayoutParams();
//            playButtonLayoutParams.width = (int) playButtonWidth;
//            flPlayButton.setLayoutParams(playButtonLayoutParams);

            typedArray.recycle();
        }

        this.addView(view);
    }


    public PlayerViewController2 setShowFavorite(boolean isShowFavorite) {
        setFavoriteState(isShowFavorite);
        return this;
    }

    private void setFavoriteState(boolean isShowFavorite) {
        if (isShowFavorite) {
            ivFavorite.setVisibility(VISIBLE);
            ivFavorite.setEnabled(true);
        } else {
            ivFavorite.setVisibility(INVISIBLE);
            ivFavorite.setEnabled(false);
        }

        int mediaCenterSize = MediaWidgetManager.getInstance().getMediaCenterSize();
        if (mediaCenterSize == 1) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) ivSoundEffect.getLayoutParams();
            // 先清掉旧的约束，避免多重约束冲突
            params.rightToRight = ConstraintLayout.LayoutParams.UNSET;
            if (isShowFavorite) {
                params.rightToRight = ivNext.getId();
            } else {
                params.rightToRight = ivPlayList.getId();
            }
            ivSoundEffect.setLayoutParams(params);
        }
    }

    public PlayerViewController2 setShowSoundEffect(boolean isShowSoundEffect) {
        if (isShowSoundEffect) {
            ivSoundEffect.setVisibility(VISIBLE);
            ivSoundEffect.setEnabled(true);
        } else {
            ivSoundEffect.setVisibility(INVISIBLE);
            ivSoundEffect.setEnabled(false);
        }
        return this;
    }

    public PlayerViewController2 setShowPlayList(boolean isShowPlayList) {
        if (isShowPlayList) {
            ivPlayList.setVisibility(VISIBLE);
            ivPlayList.setEnabled(true);
        } else {
            ivPlayList.setVisibility(INVISIBLE);
            ivPlayList.setEnabled(false);
        }
        return this;
    }

    private final OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.iv_play) {
                if (controllerListener != null) {
                    controllerListener.onPlayClick();
                }
            } else if (id == R.id.iv_pause) {
                if (controllerListener != null) {
                    controllerListener.onPauseClick();
                }
            } else if (id == R.id.iv_play_mode) {
                if (controllerListener != null) {
                    controllerListener.onPlayModeClick();
                }
            } else if (id == R.id.iv_favorite) {
                if (controllerListener != null) {
                    controllerListener.onFavoriteClick();
                }
            } else if (id == R.id.iv_sound_effect) {
                if (controllerListener != null) {
                    controllerListener.onSoundEffect();
                }
            } else if (id == R.id.iv_play_list) {
                if (controllerListener != null) {
                    controllerListener.onPlayListClick();
                }
            }
        }
    };

    /**
     * Next 按钮 Listener
     */
    private final PlayerButtonGestureListener nextGestureListener = new PlayerButtonGestureListener() {
        @Override
        public void onClick() {
            super.onClick();
            LogUtil.i("onClick Next");
            if (controllerListener != null) {
                controllerListener.onNextClick();
            }
        }

        @Override
        public void onLongPress() {
            super.onLongPress();
            LogUtil.i("onLongPress Next");
            if (controllerListener != null) {
                controllerListener.onNextLongPress();
            }
        }

        @Override
        public void onLongPressEnd() {
            super.onLongPressEnd();
            LogUtil.i("onLongPressEnd Next");
            if (controllerListener != null) {
                controllerListener.onNextLongPressEnd();
            }
        }
    };

    /**
     * Previous 按钮 Listener
     */
    private final PlayerButtonGestureListener previousGestureListener = new PlayerButtonGestureListener() {
        @Override
        public void onClick() {
            super.onClick();
            LogUtil.i(TAG, "onClick Previous");
            if (controllerListener != null) {
                controllerListener.onPreviousClick();
            }
        }

        @Override
        public void onLongPress() {
            super.onLongPress();
            LogUtil.i(TAG, "onLongPress Previous");
            if (controllerListener != null) {
                controllerListener.onPreviousLongPress();
            }
        }

        @Override
        public void onLongPressEnd() {
            super.onLongPressEnd();
            LogUtil.i(TAG, "onLongPressEnd Previous");
            if (controllerListener != null) {
                controllerListener.onPreviousLongPressEnd();
            }
        }
    };


    private PlayerControllerListener controllerListener;

    public void setVideoControllerListener(PlayerControllerListener listener) {
        this.controllerListener = listener;
    }


    public ConstraintLayout getLlController() {
        return llController;
    }

    public TextView getTvCurDuration() {
        return tvCurDuration;
    }

    public TextView getTvDuration() {
        return tvDuration;
    }

    public SmoothSeekbar getSeekBar() {
        return seekBar;
    }

    public ImageView getIvPlay() {
        return ivPlay;
    }

    public ImageView getIvPause() {
        return ivPause;
    }

    public ImageView getIvPrevious() {
        return ivPrevious;
    }

    public ImageView getIvNext() {
        return ivNext;
    }

    public ImageView getIvPlayMode() {
        return ivPlayMode;
    }

    public ImageView getIvFavorite() {
        return ivFavorite;
    }

    public ImageView getIvSoundEffect() {
        return ivSoundEffect;
    }

    public ImageView getIvPlayList() {
        return ivPlayList;
    }


    public void refreshUI() {
        if (seekBar != null) {
            seekBar.setProgressDrawable(ContextCompat.getDrawable(mContext, R.drawable.seekbar_style_drawables));
        }
        if (tvDuration != null) {
            tvDuration.setTextColor(ContextCompat.getColor(mContext, R.color.color_duration));
        }
        if (tvCurDuration != null) {
            tvCurDuration.setTextColor(ContextCompat.getColor(mContext, R.color.color_current_position));
        }

        if (ivPrevious != null) {
            if (ivPrevious.isEnabled()) {
                ivPrevious.setImageResource(R.drawable.icon_previous);
            } else {
                ivPrevious.setEnabled(false);
            }
        }

        if (ivPlay != null) {
            ivPlay.setImageResource(R.drawable.ic_player_play);
        }

        if (ivPause != null) {
            ivPause.setImageResource(R.drawable.ic_player_pause);
        }

        if (ivNext != null) {
            ivNext.setImageResource(R.drawable.icon_next);
        }

        if (ivSoundEffect != null) {
            ivSoundEffect.setImageResource(R.drawable.icon_sound_effect);
        }

        if (ivPlayList != null) {
            ivPlayList.setImageResource(R.drawable.icon_play_list);
        }
    }
}
