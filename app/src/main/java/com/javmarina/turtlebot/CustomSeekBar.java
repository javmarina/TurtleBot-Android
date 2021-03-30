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

    private static final int THUMB_SIZE = 64;
    private static final int THRESHOLD = 16;
    private static final int ANIM_DURATION = 100;
    private static final int RESOLUTION = 100;

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
        setMax(2*RESOLUTION);
        setProgress(RESOLUTION);

        final ShapeDrawable th = new ShapeDrawable(new OvalShape());
        th.setIntrinsicWidth(THUMB_SIZE);
        th.setIntrinsicHeight(THUMB_SIZE);
        th.setColorFilter(ContextCompat.getColor(getContext(), R.color.accent), PorterDuff.Mode.SRC_OVER);
        setThumb(th);

        setOnSeekBarChangeListener(new CustomSeekBar.CustomOnSeekBarChangeListener(callback));
        callback.onChange(0.0);
    }

    public double getNormalizedProgress() {
        return (getProgress() - RESOLUTION) / (double) RESOLUTION;
    }

    public interface Callback {
        void onChange(final double progress);
    }

    private static final class CustomOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        private static final float DECELERATE_FACTOR = 1.5f;

        private final CustomSeekBar.Callback callback;

        private CustomOnSeekBarChangeListener(final CustomSeekBar.Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            if (Math.abs(progress-RESOLUTION) < THRESHOLD) {
                seekBar.setProgress(RESOLUTION);
                callback.onChange(0.0);
            } else {
                callback.onChange((progress - RESOLUTION) / (double) RESOLUTION);
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
            final ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), RESOLUTION);
            anim.setDuration(ANIM_DURATION);
            anim.setInterpolator(new DecelerateInterpolator(DECELERATE_FACTOR));
            anim.addUpdateListener(animation -> {
                final int val = (int) animation.getAnimatedValue();
                seekBar.setProgress(val);
                callback.onChange((val - RESOLUTION) / (double) RESOLUTION);
            });
            anim.start();
        }
    }
}
