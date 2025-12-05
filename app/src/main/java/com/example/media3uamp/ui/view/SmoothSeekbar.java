package com.example.media3uamp.ui.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.widget.AppCompatSeekBar;

public class SmoothSeekbar extends AppCompatSeekBar {
    public final int MIN_HEIGHT = 8;
    public final int MAX_HEIGHT = 20;
    public final int DURATION = 150;
    float fraction = 0f;
    private Paint mDotPaint;
    private int dotRadius = 5;
    private int dotColor = Color.WHITE;
    private int markerPosition;
    ValueAnimator valueAnimator = null;
    ValueAnimator.AnimatorUpdateListener animatorUpdateListenerUp = valueAnimator -> {
        try {
            fraction = Math.max(0, fraction - (float) valueAnimator.getAnimatedValue());
            postInvalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };
    ValueAnimator.AnimatorUpdateListener animatorUpdateListenerDown = valueAnimator -> {
        try {
            fraction = Math.max(0, fraction - (float) valueAnimator.getAnimatedValue());
            postInvalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };
    Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            try {
                valueAnimator = ValueAnimator.ofFloat(0, 1.0f);
                valueAnimator.setDuration(DURATION);
                valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                valueAnimator.addUpdateListener(animatorUpdateListenerDown);
                valueAnimator.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };
    ValueAnimator.AnimatorUpdateListener animatorUpdateListener = valueAnimator -> {
        try {
            fraction = Math.min(1, fraction + (float) valueAnimator.getAnimatedValue());
            postInvalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    public SmoothSeekbar(Context context) {
        super(context, null);
        init();
    }

    public SmoothSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        init();
    }

    public SmoothSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDotPaint = new Paint();
        mDotPaint.setColor(dotColor);
        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setAntiAlias(true);
    }

    public void setFraction(float fraction) {
        this.fraction = fraction;
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            LayerDrawable drawable = (LayerDrawable) getProgressDrawable();
            GradientDrawable background = (GradientDrawable) drawable.findDrawableByLayerId(android.R.id.background);
            int backgroundHeight = (int) (MIN_HEIGHT + (MAX_HEIGHT - MIN_HEIGHT) * fraction);
            int viewHeight = getHeight();
            background.setBounds(0, viewHeight / 2 - backgroundHeight / 2, getWidth(), viewHeight / 2 + backgroundHeight / 2);
            ClipDrawable progress = (ClipDrawable) drawable.findDrawableByLayerId(android.R.id.progress);
            progress.setBounds(0, viewHeight / 2 - backgroundHeight / 2, getWidth(), viewHeight / 2 + backgroundHeight / 2);
            float dotX = (float) (markerPosition * getWidth()) / getMax();
            float dotY = getHeight() / 2f;
            if (markerPosition != 0)canvas.drawCircle(dotX, dotY, dotRadius, mDotPaint);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.draw(canvas);
    }

    void startDownAnimation() {
        try {
            if (valueAnimator != null) {
                valueAnimator.cancel();
                valueAnimator = null;
            }
            valueAnimator = ValueAnimator.ofFloat(0, 1.0f);
            valueAnimator.setDuration(DURATION);
            valueAnimator.setInterpolator(new AccelerateInterpolator());
            valueAnimator.addUpdateListener(animatorUpdateListener);
            valueAnimator.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startUpAnimation() {
        try {
            if (valueAnimator != null && valueAnimator.isRunning()) {
                valueAnimator.addListener(animatorListener);
            } else {
                valueAnimator = ValueAnimator.ofFloat(0, 1.0f);
                valueAnimator.setDuration(DURATION);
                valueAnimator.setInterpolator(new AccelerateInterpolator());
                valueAnimator.addUpdateListener(animatorUpdateListenerUp);
                valueAnimator.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setFixedDotProgress(int progress) {
        this.markerPosition = progress;
        invalidate();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            if (valueAnimator != null) {
                if (valueAnimator.isRunning()) {
                    valueAnimator.cancel();
                }
                valueAnimator = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            if (!isEnabled()) {
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startDownAnimation();
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    startUpAnimation();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onTouchEvent(event);
    }
}