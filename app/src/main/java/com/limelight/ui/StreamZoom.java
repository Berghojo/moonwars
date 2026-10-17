package com.limelight.ui;

import android.view.View;

/**
 * Magnifies the stream view with a pinch, like GeForce NOW's screen zoom:
 * spreading two fingers zooms in around the point between them, closing them
 * zooms out, and moving both fingers pans the magnified picture.
 *
 * Positions are in the stream view's parent coordinates. The view is scaled
 * around its top left corner, so a point in the view is at
 * {@code view.getX() + local * scale} in the parent.
 */
public class StreamZoom {
    private static final float MAX_SCALE = 4f;
    // Pinching back to nearly 1x snaps to exactly 1x
    private static final float SNAP_TO_NORMAL = 1.08f;

    private final View view;
    private float scale = 1f;
    private float translationX, translationY;
    private float lastDistance, lastMidX, lastMidY;
    private boolean active;

    public StreamZoom(View view) {
        this.view = view;
    }

    public float getScale() {
        return scale;
    }

    public void begin(float x0, float y0, float x1, float y1) {
        active = true;
        lastDistance = Math.max(1f, (float) Math.hypot(x0 - x1, y0 - y1));
        lastMidX = (x0 + x1) / 2;
        lastMidY = (y0 + y1) / 2;
    }

    public void update(float x0, float y0, float x1, float y1) {
        if (!active) {
            return;
        }

        float distance = Math.max(1f, (float) Math.hypot(x0 - x1, y0 - y1));
        float midX = (x0 + x1) / 2;
        float midY = (y0 + y1) / 2;

        float newScale = Math.max(1f, Math.min(MAX_SCALE, scale * distance / lastDistance));

        // Keep the point under the fingers in place while scaling, then follow the fingers
        float left = view.getLeft();
        float top = view.getTop();
        translationX = lastMidX - left - (lastMidX - left - translationX) * newScale / scale + (midX - lastMidX);
        translationY = lastMidY - top - (lastMidY - top - translationY) * newScale / scale + (midY - lastMidY);
        scale = newScale;

        lastDistance = distance;
        lastMidX = midX;
        lastMidY = midY;
        apply();
    }

    public void end() {
        active = false;
        if (scale < SNAP_TO_NORMAL) {
            reset();
        }
    }

    public void reset() {
        scale = 1f;
        translationX = translationY = 0;
        apply();
    }

    private void apply() {
        // Keep the magnified picture covering the area the stream normally fills
        float width = view.getWidth();
        float height = view.getHeight();
        translationX = Math.max(width * (1 - scale), Math.min(0, translationX));
        translationY = Math.max(height * (1 - scale), Math.min(0, translationY));

        view.setPivotX(0);
        view.setPivotY(0);
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setTranslationX(translationX);
        view.setTranslationY(translationY);
    }
}
