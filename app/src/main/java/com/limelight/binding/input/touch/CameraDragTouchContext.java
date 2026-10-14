package com.limelight.binding.input.touch;

import android.view.View;

import com.limelight.binding.input.ControllerHandler;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Touch mode for mouse-look games like Guild Wars 2: touching the stream holds
 * the right mouse button and dragging moves the mouse, so the camera turns with
 * the finger. A tap is a right click. Only the first finger is used.
 */
public class CameraDragTouchContext implements TouchContext {
    private final NvConnection conn;
    private final ControllerHandler controllerHandler;
    private final int actionIndex;
    private final int referenceWidth;
    private final int referenceHeight;
    private final View targetView;
    private final PreferenceConfiguration prefConfig;

    private int lastTouchX, lastTouchY;
    private double pendingX, pendingY;
    private double xFactor, yFactor;
    private boolean rightButtonHeld;
    private boolean cancelled;

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
    }

    @Override
    public int getActionIndex() {
        return actionIndex;
    }

    @Override
    public void setPointerCount(int pointerCount) {
    }

    @Override
    public boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger) {
        if (actionIndex != 0) {
            return true;
        }

        double sensitivity = prefConfig.touchMouseSensitivity / 100.0;
        xFactor = sensitivity * referenceWidth / (double) targetView.getWidth();
        yFactor = sensitivity * referenceHeight / (double) targetView.getHeight();

        lastTouchX = eventX;
        lastTouchY = eventY;
        pendingX = pendingY = 0;
        cancelled = false;

        setRightButton(true);
        return true;
    }

    @Override
    public boolean touchMoveEvent(int eventX, int eventY, long eventTime) {
        if (cancelled || actionIndex != 0 || !rightButtonHeld) {
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
        setRightButton(false);
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
