package com.limelight.binding.input.touch;

/**
 * Tracks the two fingers of a zoom gesture and turns it into mouse wheel notches.
 * Both a pinch (fingers spreading apart / closing together) and a two-finger drag
 * (both fingers moving up / down) zoom: spreading or moving up zooms in.
 *
 * Shared by the touch contexts of the first and second finger, since each context
 * only sees its own finger.
 */
public class TwoFingerZoom {
    private final float[] x = new float[2];
    private final float[] y = new float[2];
    private final boolean[] valid = new boolean[2];
    private boolean active;
    private boolean haveBaseline;
    private float lastDistance, lastMidY;
    private float accumulator;

    /** Starts a zoom gesture; the first finger is at the given position. */
    public void begin(float firstX, float firstY) {
        active = true;
        haveBaseline = false;
        accumulator = 0;
        valid[0] = valid[1] = false;
        setPoint(0, firstX, firstY);
    }

    public void end() {
        active = false;
        valid[0] = valid[1] = false;
        haveBaseline = false;
    }

    public boolean isActive() {
        return active;
    }

    /** Forgets a finger that was lifted, so the remaining one doesn't zoom by itself. */
    public void removePoint(int index) {
        if (index < 2) {
            valid[index] = false;
            haveBaseline = false;
        }
    }

    /**
     * Records a finger's new position and returns how many wheel notches to send
     * (positive zooms in).
     *
     * @param stepPx finger movement per notch
     */
    public int update(int index, float px, float py, float stepPx) {
        if (!active || index >= 2) {
            return 0;
        }
        setPoint(index, px, py);
        if (!valid[0] || !valid[1]) {
            return 0;
        }

        float distance = (float) Math.hypot(x[0] - x[1], y[0] - y[1]);
        float midY = (y[0] + y[1]) / 2;
        if (!haveBaseline) {
            haveBaseline = true;
        }
        else {
            // Spreading the fingers or moving them up both zoom in
            accumulator += (distance - lastDistance) + (lastMidY - midY);
        }
        lastDistance = distance;
        lastMidY = midY;

        int notches = (int) (accumulator / stepPx);
        accumulator -= notches * stepPx;
        return Math.max(-127, Math.min(127, notches));
    }

    private void setPoint(int index, float px, float py) {
        x[index] = px;
        y[index] = py;
        valid[index] = true;
    }
}
