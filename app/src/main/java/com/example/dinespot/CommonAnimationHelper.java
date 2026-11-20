package com.example.dinespot;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

public class CommonAnimationHelper {

    public static void animateListEntry(View view, int position) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(50f);
        
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(Math.min(position * 30L, 300))
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void applyClickAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    public static void animateStatChange(TextView textView, int newValue) {
        if (textView == null) return;
        String currentText = textView.getText().toString();
        int startValue = 0;
        try {
            startValue = Integer.parseInt(currentText);
        } catch (NumberFormatException ignored) {}

        if (startValue == newValue) return;

        ValueAnimator animator = ValueAnimator.ofInt(startValue, newValue);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            if (textView != null) {
                textView.setText(animation.getAnimatedValue().toString());
            }
        });
        animator.start();
    }
}
