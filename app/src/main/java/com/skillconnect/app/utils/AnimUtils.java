package com.skillconnect.app.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** Small animations that make the UI feel alive. */
public final class AnimUtils {

    private AnimUtils() {
    }

    /** Quick "pop" used when a provider is saved (♡ → ❤). */
    public static void pop(View view) {
        view.animate().cancel();
        view.setScaleX(0.7f);
        view.setScaleY(0.7f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    /** Gentle pulsing used on skeleton placeholders while content loads. Returns the animator so it can be stopped. */
    public static ObjectAnimator pulse(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.45f);
        animator.setDuration(750);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
        return animator;
    }

    /** Fades and slides a view in (used when content replaces a skeleton). */
    public static void fadeIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(16f);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1f).translationY(0f).setDuration(260).start();
    }
}
