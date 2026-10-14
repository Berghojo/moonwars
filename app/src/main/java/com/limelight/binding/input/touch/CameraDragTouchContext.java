package com.limelight.binding.input.touch;

import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Touch mode for mouse-look games like Guild Wars 2:
 * <ul>
 *   <li>Dragging one finger holds the right mouse button and moves the mouse,
 *       so the camera turns with the finger.</li>
 *   <li>A quick tap moves the cursor to the finger and left clicks there.</li>
 *   <li>Dragging two fingers up or down scrolls the mouse wheel (zoom).</li>
 * </ul>
 * Only the first finger drives the camera; the second finger just switches the
 * gesture to zooming.
 */
public class CameraDragTouchContext implements TouchContext {
    // How far a finger must move before a touch turns into a camera drag
    private static final float DRAG_SLOP_DP = 8;
    // Longest touch that still counts as a tap (right click)
    private static final long TAP_TIME_MS = 300;
    // How long the left button stays down for a tap
    private static final long TAP_CLICK_MS = 50;
    // Two-finger drag distance per mouse wheel notch, as a fraction of the view height
    private static final float ZOOM_STEP_FRACTION = 0.04f;

    private final NvConnection conn;
    private final ControllerHandler controllerHandler;
    private final int actionIndex;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final PreferenceConfiguration prefConfig;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final float dragSlopPx;

    private int downX, downY;
    private long downTime;
    private int lastTouchX, lastTouchY;
    private double pendingX, pendingY;
    private double xFactor, yFactor;
    private float zoomAccumulator;
    private int pointerCount;
    private boolean rightButtonHeld;
    // Set once a second finger lands; stays set until every finger is lifted
    private boolean zooming;
    private boolean cancelled;
    // Left button is down for a tap click and waiting for tapReleaseRunnable
    private boolean tapButtonDown;

    private final Runnable tapReleaseRunnable = new Runnable() {
        @Override
        public void run() {
            if (tapButtonDown) {
                tapButtonDown = false;
                controllerHandler.reportOscMouseButton(MouseButtonPacket.BUTTON_LEFT, false);
            }
        }
    };

    public CameraDragTouchContext(NvConnection conn, ControllerHandler controllerHandler,
                                  int actionIndex, int referenceWidth, int referenceHeight,
                                  View view, PreferenceConfiguration prefConfig) {
        this.conn = conn;
        this.controllerHandler = controllerHandler;
        this.actionIndex = actionIndex;
        this.referenceWidth = referenceWidth;
        this.referenceHeight = referenceHeight;
        this.targetView = view;
        this.prefConfig = prefConfig;
        this.dragSlopPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DRAG_SLOP_DP,
                view.getResources().getDisplayMetrics());
    }

    @Override
    public int getActionIndex() {
        return actionIndex;
    }

    @Override
    public void setPointerCount(int pointerCount) {
        this.pointerCount = pointerCount;

        if (actionIndex == 0 && pointerCount >= 2 && !zooming) {
            // A second finger turns the gesture into a zoom
            zooming = true;
            zoomAccumulator = 0;
            setRightButton(false);
        }
        else if (pointerCount == 0) {
            zooming = false;
        }
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger) {
        if (actionIndex != 0) {
            return true;
        }

        lastTouchX = eventX;
        lastTouchY = eventY;
        pendingX = pendingY = 0;

        if (isNewFinger && pointerCount <= 1) {
            // Start of a new gesture
            double sensitivity = prefConfig.touchMouseSensitivity / 100.0;
            xFactor = sensitivity * referenceWidth / (double) targetView.getWidth();
            yFactor = sensitivity * referenceHeight / (double) targetView.getHeight();

            downX = eventX;
            downY = eventY;
            downTime = eventTime;
            zooming = false;
            cancelled = false;
        }
        return true;
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime) {
        if (cancelled || actionIndex != 0) {
            return true;
        }

        if (zooming) {
            // Fingers moving up zoom in (wheel up), down zoom out
            zoomAccumulator += lastTouchY - eventY;
            lastTouchX = eventX;
            lastTouchY = eventY;

            float step = Math.max(1, targetView.getHeight() * ZOOM_STEP_FRACTION);
            int notches = (int) (zoomAccumulator / step);
            if (notches != 0) {
                zoomAccumulator -= notches * step;
                conn.sendMouseScroll((byte) Math.max(-127, Math.min(127, notches)));
            }
            return true;
        }

        if (!rightButtonHeld) {
            if (Math.hypot(eventX - downX, eventY - downY) < dragSlopPx) {
                return true;
            }
            // Start turning the camera from here
            setRightButton(true);
            lastTouchX = eventX;
            lastTouchY = eventY;
            return true;
        }

        // Keep fractional movement so slow drags still turn the camera
        pendingX += (eventX - lastTouchX) * xFactor;
        pendingY += (eventY - lastTouchY) * yFactor;
        lastTouchX = eventX;
        lastTouchY = eventY;

        int dx = (int) pendingX;
        int dy = (int) pendingY;
        pendingX -= dx;
        pendingY -= dy;
        if (dx != 0 || dy != 0) {
            conn.sendMouseMove((short) dx, (short) dy);
        }
        return true;
    }

    @Override
    public void touchUpEvent(int eventX, int eventY, long eventTime) {
        if (actionIndex != 0 || cancelled) {
            return;
        }

        if (rightButtonHeld) {
            setRightButton(false);
        }
        else if (!zooming && eventTime - downTime <= TAP_TIME_MS &&
                Math.hypot(eventX - downX, eventY - downY) < dragSlopPx) {
            // Quick tap: left click where the finger touched
            // Finish any previous click first so double taps are two full clicks
            handler.removeCallbacks(tapReleaseRunnable);
            tapReleaseRunnable.run();
            int x = Math.max(0, Math.min(downX, targetView.getWidth()));
            int y = Math.max(0, Math.min(downY, targetView.getHeight()));
            conn.sendMousePosition((short) x, (short) y,
                    (short) targetView.getWidth(), (short) targetView.getHeight());
            tapButtonDown = true;
            controllerHandler.reportOscMouseButton(MouseButtonPacket.BUTTON_LEFT, true);
            handler.postDelayed(tapReleaseRunnable, TAP_CLICK_MS);
        }
    }

    @Override
    public void cancelTouch() {
        cancelled = true;
        setRightButton(false);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private void setRightButton(boolean down) {
        if (down == rightButtonHeld) {
            return;
        }
        rightButtonHeld = down;
        // Goes through the controller handler so it shares the right button with the
        // camera stick and the on-screen RMB button in keyboard/mouse mode
        controllerHandler.reportOscMouseButton(MouseButtonPacket.BUTTON_RIGHT, down);
    }
}
