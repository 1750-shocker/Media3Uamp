package com.example.media3uamp.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * 可以按下缩小的ImageView
 */
public class CustomImageViewButton extends AppCompatImageView {

    public CustomImageViewButton(Context context) {
        super(context);
        init();
    }

    public CustomImageViewButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomImageViewButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private float alpha = 1f;

    private void init() {
        // 设置按钮的初始状态
        setScaleX(1.0f);
        setScaleY(1.0f);
        alpha = getAlpha();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 点击按下时缩小并变暗
                setScaleX(0.95f);
                setScaleY(0.95f);
                setAlpha(alpha * 0.8f);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 点击释放时恢复初始状态
                setScaleX(1.0f);
                setScaleY(1.0f);
                setAlpha(alpha);
                break;
        }
        return super.onTouchEvent(event);
    }
}
