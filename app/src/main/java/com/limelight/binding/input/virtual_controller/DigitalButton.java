/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a digital button on screen element. It is used to get click and double click user input.
 */
public class DigitalButton extends VirtualControllerElement {

    /**
     * Listener interface to update registered observers.
     */
    public interface DigitalButtonListener {

        /**
         * onClick event will be fired on button click.
         */
        void onClick();

        /**
         * onLongClick event will be fired on button long click.
         */
        void onLongClick();

        /**
         * onRelease event will be fired on button unpress.
         */
        void onRelease();
    }

    private List<DigitalButtonListener> listeners = new ArrayList<>();
    private String text = "";
    private int icon = -1;
    private long timerLongClickTimeout = 3000;
    private final Runnable longClickRunnable = new Runnable() {
        @Override
        public void run() {
            onLongClickCallback();
        }
    };

    private final Paint paint = new Paint();
    private final RectF rect = new RectF();

    private int layer;
    private DigitalButton movingButton = null;

    boolean inRange(float x, float y) {
        return (this.getX() < x && this.getX() + this.getWidth() > x) &&
                (this.getY() < y && this.getY() + this.getHeight() > y);
    }

    public boolean checkMovement(float x, float y, DigitalButton movingButton) {
        // check if the movement happened in the same layer
        if (movingButton.layer != this.layer) {
            return false;
        }

        // save current pressed state
        boolean wasPressed = isPressed();

        // check if the movement directly happened on the button
        if ((this.movingButton == null || movingButton == this.movingButton)
                && this.inRange(x, y)) {
            // set button pressed state depending on moving button pressed state
            if (this.isPressed() != movingButton.isPressed()) {
                this.setPressed(movingButton.isPressed());
            }
        }
        // check if the movement is outside of the range and the movement button
        // is the saved moving button
        else if (movingButton == this.movingButton) {
            this.setPressed(false);
        }

        // check if a change occurred
        if (wasPressed != isPressed()) {
            if (isPressed()) {
                // is pressed set moving button and emit click event
                this.movingButton = movingButton;
                onClickCallback();
            } else {
                // no longer pressed reset moving button and emit release event
                this.movingButton = null;
                onReleaseCallback();
            }

            invalidate();

            return true;
        }

        return false;
    }

    private void checkMovementForAllButtons(float x, float y) {
        for (VirtualControllerElement element : virtualController.getElements()) {
            if (element != this && element instanceof DigitalButton) {
                ((DigitalButton) element).checkMovement(x, y, this);
            }
        }
    }

    public DigitalButton(VirtualController controller, int elementId, int layer, Context context) {
        super(controller, context, elementId);
        this.layer = layer;
    }

    public void addDigitalButtonListener(DigitalButtonListener listener) {
        listeners.add(listener);
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    public void setIcon(int id) {
        this.icon = id;
        invalidate();
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        // set transparent background
        canvas.drawColor(Color.TRANSPARENT);

        // Size text to fit both the width and height of the button
        float textSize = Math.min(getPercent(getWidth(), 25), getPercent(getHeight(), 42));
        if (text.length() > 3) {
            textSize = Math.min(textSize, getWidth() * 1.1f / text.length());
        }
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStrokeWidth(getDefaultStrokeWidth());

        rect.left = rect.top = paint.getStrokeWidth();
        rect.right = getWidth() - rect.left;
        rect.bottom = getHeight() - rect.top;

        // Dark backdrop (tinted when pressed) for contrast against the video
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isPressed() ? (pressedColor & 0x00FFFFFF) | 0x80000000 : backdropColor);
        canvas.drawOval(rect, paint);

        paint.setColor(isPressed() ? pressedColor : getDefaultColor());
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawOval(rect, paint);

        if (icon != -1) {
            Drawable d = getResources().getDrawable(icon).mutate();
            d.setColorFilter(paint.getColor(), android.graphics.PorterDuff.Mode.SRC_IN);
            // Keep the icon square and centered inside the oval
            int size = (int) (Math.min(getWidth(), getHeight()) * 0.6f);
            int left = (getWidth() - size) / 2;
            int top = (getHeight() - size) / 2;
            d.setBounds(left, top, left + size, top + size);
            d.draw(canvas);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setFakeBoldText(true);
            paint.setShadowLayer(getDefaultStrokeWidth(), 0, 0, 0xFF000000);
            canvas.drawText(text, getPercent(getWidth(), 50),
                    getHeight() / 2f - (paint.descent() + paint.ascent()) / 2, paint);
            paint.clearShadowLayer();
            paint.setFakeBoldText(false);
        }
    }

    private void onClickCallback() {
        _DBG("clicked");
        // notify listeners
        for (DigitalButtonListener listener : listeners) {
            listener.onClick();
        }

        virtualController.getHandler().removeCallbacks(longClickRunnable);
        virtualController.getHandler().postDelayed(longClickRunnable, timerLongClickTimeout);
    }

    private void onLongClickCallback() {
        _DBG("long click");
        // notify listeners
        for (DigitalButtonListener listener : listeners) {
            listener.onLongClick();
        }
    }

    private void onReleaseCallback() {
        _DBG("released");
        // notify listeners
        for (DigitalButtonListener listener : listeners) {
            listener.onRelease();
        }

        // We may be called for a release without a prior click
        virtualController.getHandler().removeCallbacks(longClickRunnable);
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        // get masked (not specific to a pointer) action
        float x = getX() + event.getX();
        float y = getY() + event.getY();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                movingButton = null;
                setPressed(true);
                onClickCallback();

                invalidate();

                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                checkMovementForAllButtons(x, y);

                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                setPressed(false);
                onReleaseCallback();

                checkMovementForAllButtons(x, y);

                invalidate();

                return true;
            }
            default: {
            }
        }
        return true;
    }
}
