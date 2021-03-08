package com.javmarina.turtlebot;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.content.ContextCompat;


public class CustomSeekBar extends AppCompatSeekBar {

    private static final int THUMB_SIZE = 56;
    private static final int THRESHOLD = 16;
    private static final int ANIM_DURATION = 100;

    public CustomSeekBar(final Context context) {
        super(context);
    }

    public CustomSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSeekBar(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(final CustomSeekBar.Callback callback) {
        setMax(255);
        setProgress(128);

        final ShapeDrawable th = new ShapeDrawable(new OvalShape());
        th.setIntrinsicWidth(THUMB_SIZE);
        th.setIntrinsicHeight(THUMB_SIZE);
        th.setColorFilter(ContextCompat.getColor(getContext(), R.color.accent), PorterDuff.Mode.SRC_OVER);
        setThumb(th);

        setOnSeekBarChangeListener(new CustomSeekBar.CustomOnSeekBarChangeListener(callback));
        callback.onChange(128);
    }

    public double getNormalizedProgress() {
        return (getProgress() - 128.0) / 128.0;
    }

    public interface Callback {
        void onChange(final int progress);
    }

    private static final class CustomOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        private final CustomSeekBar.Callback callback;

        private CustomOnSeekBarChangeListener(final CustomSeekBar.Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            if (Math.abs(progress - 128) < THRESHOLD) {
                seekBar.setProgress(128);
                callback.onChange(128);
            } else {
                callback.onChange(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            final ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), 128);
            anim.setDuration(ANIM_DURATION);
            anim.setInterpolator(new DecelerateInterpolator(1.5f));
            anim.addUpdateListener(animation -> {
                final int val = (int) animation.getAnimatedValue();
                seekBar.setProgress(val);
                callback.onChange(val);
            });
            anim.start();
        }
    }
}
