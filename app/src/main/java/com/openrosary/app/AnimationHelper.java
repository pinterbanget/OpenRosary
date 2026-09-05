package com.openrosary.app;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Utility for fluid, modern micro-interactions and fast page transitions.
 */
public final class AnimationHelper {
    private static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator(1.6f);
    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(1.4f);

    private AnimationHelper() {}

    /**
     * Attaches a tactile spring scale animation on press to any View.
     */
    public static void attachPressScale(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.97f)
                            .scaleY(0.97f)
                            .setDuration(60)
                            .setInterpolator(DECELERATE)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(130)
                            .setInterpolator(OVERSHOOT)
                            .start();
                    break;
            }
            return false;
        });
    }

    /**
     * Quick pop spring animation for segmented pill selection.
     */
    public static void animatePillSelection(View view) {
        if (view == null) return;
        view.setScaleX(0.93f);
        view.setScaleY(0.93f);
        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(140)
                .setInterpolator(OVERSHOOT)
                .start();
    }

    /**
     * Ultra-fast text glide & fade transition (100ms total) for swift bead navigation.
     */
    public static void slideTransition(View view, boolean isNext, Runnable onHalfway) {
        if (view == null) {
            if (onHalfway != null) onHalfway.run();
            return;
        }

        float travel = 32f;
        float exitX = isNext ? -travel : travel;

        view.animate()
                .translationX(exitX)
                .alpha(0f)
                .setDuration(40)
                .setInterpolator(DECELERATE)
                .withEndAction(() -> {
                    if (onHalfway != null) onHalfway.run();
                    view.setTranslationX(-exitX);
                    view.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(60)
                            .setInterpolator(DECELERATE)
                            .start();
                })
                .start();
    }
}
