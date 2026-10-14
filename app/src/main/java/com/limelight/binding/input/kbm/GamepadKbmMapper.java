package com.limelight.binding.input.kbm;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates gamepad state into keyboard and mouse input:
 * <ul>
 *   <li>Left stick -> 8-way movement keys (WASD by default)</li>
 *   <li>Right stick -> relative mouse movement for the camera, optionally
 *       holding the right mouse button while the stick is deflected (needed for
 *       mouse-look in games like Guild Wars 2)</li>
 *   <li>Buttons and triggers -> configurable keys / mouse buttons, with a
 *       second layer available while a "layer shift" button is held</li>
 * </ul>
 *
 * The mapper is driven by {@link #update} whenever gamepad state changes and
 * by {@link #tick} periodically while {@link #needsTick()} is true (camera
 * movement and mouse wheel auto-repeat are time based).
 *
 * This class has no Android dependencies so it can be unit tested on the JVM.
 * All public methods are synchronized because gamepad state can arrive from
 * both the UI thread and the USB driver threads.
 */
public class GamepadKbmMapper {
    public interface Output {
        // keyCode is a Windows virtual key code
        void keyDown(int keyCode, byte modifiers);
        void keyUp(int keyCode, byte modifiers);
        void mouseButtonDown(int button);
        void mouseButtonUp(int button);
        void mouseMove(int dx, int dy);
        void mouseScroll(int clicks);
        default void cameraModeChanged(boolean holdRightMouse) {}
        // Called when the active layer (skill bar) changes
        default void layerChanged(boolean shiftedLayerActive) {}
    }

    public static class Config {
        // Hold the right mouse button while the camera stick is deflected
        public boolean cameraHoldsRightMouse = true;
        // Mouse speed in pixels per second at full right stick deflection, in
        // camera mode (right mouse button held) and cursor mode respectively
        public float cameraSpeed = 1500f;
        public float cursorSpeed = 800f;
        // Response curve exponent; higher gives finer control near the center
        public float cameraCurve = 1.8f;
        public boolean invertCameraY = false;
        // Fractions of full deflection
        public float moveDeadzone = 0.30f;
        public float cameraDeadzone = 0.12f;
        public float triggerThreshold = 0.35f;
        // Keep the right mouse button held briefly after the stick recenters
        // so quick flicks don't produce a stream of right clicks
        public long rightMouseReleaseDelayMs = 150;
        public long wheelRepeatDelayMs = 350;
        public long wheelRepeatIntervalMs = 90;
    }

    // Keyboard modifier flags (match KeyboardPacket)
    static final byte MODIFIER_SHIFT = 0x01;
    static final byte MODIFIER_CTRL = 0x02;
    static final byte MODIFIER_ALT = 0x04;

    private static final long MAX_TICK_DELTA_MS = 50;

    private final Output output;
    private final Config config;
    private final KbmBindings bindings;

    // The action each input triggered when it was pressed, so releasing it
    // undoes exactly that even if the layer has changed in the meantime
    private final EnumMap<KbmBindings.Input, KbmAction> activeActions = new EnumMap<>(KbmBindings.Input.class);
    private final EnumMap<KbmBindings.Input, Long> wheelRepeatDeadlines = new EnumMap<>(KbmBindings.Input.class);

    // Reference counts so a key or mouse button held by several inputs is only
    // released when the last of them lets go
    private final Map<Integer, Integer> keyHolds = new HashMap<>();
    private final Map<Integer, Integer> mouseHolds = new HashMap<>();

    private int layerShiftHolds;
    private boolean layerToggled;
    private boolean cameraHoldsRightMouse;
    private boolean rightMouseHeldByCamera;

    private float cameraX, cameraY;
    private float pendingMouseX, pendingMouseY;
    private long lastTickMs = -1;
    private long lastCameraActiveMs;

    public GamepadKbmMapper(Output output, Config config, KbmBindings bindings) {
        this.output = output;
        this.config = config;
        this.bindings = bindings;
        this.cameraHoldsRightMouse = config.cameraHoldsRightMouse;
    }

    /**
     * Reports the latest gamepad state, using the same conventions as
     * GameStream controller packets (stick Y is positive when pushed up,
     * triggers are 0-255 stored in a byte).
     */
    public synchronized void update(int buttonFlags,
                                    short leftStickX, short leftStickY,
                                    short rightStickX, short rightStickY,
                                    byte leftTrigger, byte rightTrigger,
                                    long nowMs) {
        EnumMap<KbmBindings.Input, Boolean> pressed = new EnumMap<>(KbmBindings.Input.class);
        for (KbmBindings.Input input : KbmBindings.Input.values()) {
            if (input.buttonFlag != 0) {
                pressed.put(input, (buttonFlags & input.buttonFlag) != 0);
            }
        }
        pressed.put(KbmBindings.Input.LT, (leftTrigger & 0xFF) / 255f > config.triggerThreshold);
        pressed.put(KbmBindings.Input.RT, (rightTrigger & 0xFF) / 255f > config.triggerThreshold);
        computeStickDirections(normalizeAxis(leftStickX), normalizeAxis(leftStickY), pressed);

        // Releases first, then layer shifts, then everything else, so a button
        // pressed in the same report as the layer shift lands on the shifted layer
        for (KbmBindings.Input input : KbmBindings.Input.values()) {
            if (!pressed.get(input) && activeActions.containsKey(input)) {
                release(input, activeActions.remove(input));
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (KbmBindings.Input input : KbmBindings.Input.values()) {
                if (!pressed.get(input) || activeActions.containsKey(input)) {
                    continue;
                }
                boolean isLayerAction = bindings.getBase(input).isLayerAction();
                if ((pass == 0) != isLayerAction) {
                    continue;
                }
                KbmAction action = isLayerAction ? bindings.getBase(input) : bindings.resolve(input, isShiftedLayerActive());
                activeActions.put(input, action);
                press(input, action, nowMs);
            }
        }

        boolean wasCameraActive = isCameraDeflected();
        cameraX = normalizeAxis(rightStickX);
        cameraY = normalizeAxis(rightStickY);
        if (isCameraDeflected() && !wasCameraActive && !rightMouseHeldByCamera) {
            // Restart the movement clock so the first tick doesn't jump
            lastTickMs = nowMs;
        }
    }

    /** True while time-based work (camera movement, wheel repeat, delayed RMB release) is pending. */
    public synchronized boolean needsTick() {
        return isCameraDeflected() || rightMouseHeldByCamera || !wheelRepeatDeadlines.isEmpty();
    }

    /** Advances time-based work. Call roughly every 8-16 ms while {@link #needsTick()} is true. */
    public synchronized void tick(long nowMs) {
        long deltaMs = lastTickMs < 0 ? 0 : Math.min(Math.max(nowMs - lastTickMs, 0), MAX_TICK_DELTA_MS);
        lastTickMs = nowMs;

        if (isCameraDeflected()) {
            if (cameraHoldsRightMouse && !rightMouseHeldByCamera) {
                holdMouse(KbmAction.MOUSE_RIGHT);
                rightMouseHeldByCamera = true;
            }
            lastCameraActiveMs = nowMs;

            float magnitude = (float) Math.hypot(cameraX, cameraY);
            float scaled = Math.min(1f, (magnitude - config.cameraDeadzone) / (1f - config.cameraDeadzone));
            float maxSpeed = cameraHoldsRightMouse ? config.cameraSpeed : config.cursorSpeed;
            float speed = maxSpeed * (float) Math.pow(scaled, config.cameraCurve);
            float seconds = deltaMs / 1000f;
            // Stick up is positive Y, but mouse up is negative Y
            float ySign = config.invertCameraY ? 1f : -1f;
            pendingMouseX += cameraX / magnitude * speed * seconds;
            pendingMouseY += ySign * cameraY / magnitude * speed * seconds;

            int dx = (int) pendingMouseX;
            int dy = (int) pendingMouseY;
            pendingMouseX -= dx;
            pendingMouseY -= dy;
            if (dx != 0 || dy != 0) {
                output.mouseMove(clampShort(dx), clampShort(dy));
            }
        }
        else {
            pendingMouseX = pendingMouseY = 0;
            if (rightMouseHeldByCamera && nowMs - lastCameraActiveMs >= config.rightMouseReleaseDelayMs) {
                rightMouseHeldByCamera = false;
                unholdMouse(KbmAction.MOUSE_RIGHT);
            }
        }

        for (Map.Entry<KbmBindings.Input, Long> entry : wheelRepeatDeadlines.entrySet()) {
            if (nowMs >= entry.getValue()) {
                KbmAction action = activeActions.get(entry.getKey());
                if (action != null) {
                    scrollFor(action);
                }
                entry.setValue(nowMs + config.wheelRepeatIntervalMs);
            }
        }
    }

    /** Releases every key and mouse button this mapper is holding. */
    public synchronized void releaseAll() {
        for (Map.Entry<KbmBindings.Input, KbmAction> entry : activeActions.entrySet()) {
            release(entry.getKey(), entry.getValue());
        }
        activeActions.clear();
        wheelRepeatDeadlines.clear();
        if (rightMouseHeldByCamera) {
            rightMouseHeldByCamera = false;
            unholdMouse(KbmAction.MOUSE_RIGHT);
        }
        // Anything still held here would be a bookkeeping bug, but never leave
        // keys stuck down on the host
        for (int keyCode : keyHolds.keySet().toArray(new Integer[0])) {
            output.keyUp(keyCode, (byte) 0);
        }
        keyHolds.clear();
        for (int button : mouseHolds.keySet().toArray(new Integer[0])) {
            output.mouseButtonUp(button);
        }
        mouseHolds.clear();
        boolean wasShifted = isShiftedLayerActive();
        layerShiftHolds = 0;
        layerToggled = false;
        if (wasShifted) {
            output.layerChanged(false);
        }
        cameraX = cameraY = 0;
        pendingMouseX = pendingMouseY = 0;
    }

    /**
     * Presses or releases a mouse button directly (e.g. an on-screen click button).
     * Shares reference counts with the camera, so releasing an on-screen right
     * click doesn't cancel the camera's held right mouse button.
     */
    public synchronized void setMouseButton(int button, boolean down) {
        if (down) {
            holdMouse(button);
        }
        else {
            unholdMouse(button);
        }
    }

    /** True while the shifted layer (skill bar 6-0) is in use. */
    public synchronized boolean isShiftedLayerActive() {
        return layerShiftHolds > 0 || layerToggled;
    }

    public KbmBindings getBindings() {
        return bindings;
    }

    private void notifyLayerChange(boolean wasShifted) {
        boolean shifted = isShiftedLayerActive();
        if (shifted != wasShifted) {
            output.layerChanged(shifted);
        }
    }

    public synchronized boolean isCameraHoldingRightMouse() {
        return cameraHoldsRightMouse;
    }

    private boolean isCameraDeflected() {
        return Math.hypot(cameraX, cameraY) > config.cameraDeadzone;
    }

    private void computeStickDirections(float x, float y, EnumMap<KbmBindings.Input, Boolean> pressed) {
        boolean up = false, down = false, left = false, right = false;
        if (Math.hypot(x, y) > config.moveDeadzone) {
            // 8 sectors of 45 degrees each, centered on the cardinal and diagonal directions
            double angle = Math.toDegrees(Math.atan2(y, x));
            up = angle > 22.5 && angle < 157.5;
            down = angle < -22.5 && angle > -157.5;
            right = Math.abs(angle) < 67.5;
            left = Math.abs(angle) > 112.5;
        }
        pressed.put(KbmBindings.Input.LS_UP, up);
        pressed.put(KbmBindings.Input.LS_DOWN, down);
        pressed.put(KbmBindings.Input.LS_LEFT, left);
        pressed.put(KbmBindings.Input.LS_RIGHT, right);
    }

    private void press(KbmBindings.Input input, KbmAction action, long nowMs) {
        switch (action.type) {
            case KEY:
                holdKey(action);
                break;
            case MOUSE_BUTTON:
                holdMouse(action.code);
                break;
            case WHEEL_UP:
            case WHEEL_DOWN:
                scrollFor(action);
                wheelRepeatDeadlines.put(input, nowMs + config.wheelRepeatDelayMs);
                break;
            case LAYER_SHIFT: {
                boolean wasShifted = isShiftedLayerActive();
                layerShiftHolds++;
                notifyLayerChange(wasShifted);
                break;
            }
            case LAYER_TOGGLE: {
                boolean wasShifted = isShiftedLayerActive();
                layerToggled = !layerToggled;
                notifyLayerChange(wasShifted);
                break;
            }
            case TOGGLE_CAMERA_MODE:
                cameraHoldsRightMouse = !cameraHoldsRightMouse;
                if (!cameraHoldsRightMouse && rightMouseHeldByCamera) {
                    rightMouseHeldByCamera = false;
                    unholdMouse(KbmAction.MOUSE_RIGHT);
                }
                output.cameraModeChanged(cameraHoldsRightMouse);
                break;
            default:
                break;
        }
    }

    private void release(KbmBindings.Input input, KbmAction action) {
        switch (action.type) {
            case KEY:
                unholdKey(action);
                break;
            case MOUSE_BUTTON:
                unholdMouse(action.code);
                break;
            case WHEEL_UP:
            case WHEEL_DOWN:
                wheelRepeatDeadlines.remove(input);
                break;
            case LAYER_SHIFT: {
                boolean wasShifted = isShiftedLayerActive();
                layerShiftHolds = Math.max(0, layerShiftHolds - 1);
                notifyLayerChange(wasShifted);
                break;
            }
            default:
                break;
        }
    }

    private void scrollFor(KbmAction action) {
        output.mouseScroll(action.type == KbmAction.Type.WHEEL_UP ? 1 : -1);
    }

    private void holdKey(KbmAction action) {
        int count = keyHolds.getOrDefault(action.code, 0);
        keyHolds.put(action.code, count + 1);
        if (count == 0) {
            output.keyDown(action.code, currentModifiers());
        }
    }

    private void unholdKey(KbmAction action) {
        Integer count = keyHolds.get(action.code);
        if (count == null) {
            return;
        }
        if (count > 1) {
            keyHolds.put(action.code, count - 1);
        }
        else {
            keyHolds.remove(action.code);
            output.keyUp(action.code, currentModifiers());
        }
    }

    private void holdMouse(int button) {
        int count = mouseHolds.getOrDefault(button, 0);
        mouseHolds.put(button, count + 1);
        if (count == 0) {
            output.mouseButtonDown(button);
        }
    }

    private void unholdMouse(int button) {
        Integer count = mouseHolds.get(button);
        if (count == null) {
            return;
        }
        if (count > 1) {
            mouseHolds.put(button, count - 1);
        }
        else {
            mouseHolds.remove(button);
            output.mouseButtonUp(button);
        }
    }

    private byte currentModifiers() {
        byte modifiers = 0;
        if (keyHolds.containsKey(KbmAction.VK_LSHIFT)) {
            modifiers |= MODIFIER_SHIFT;
        }
        if (keyHolds.containsKey(KbmAction.VK_LCONTROL)) {
            modifiers |= MODIFIER_CTRL;
        }
        if (keyHolds.containsKey(KbmAction.VK_LMENU)) {
            modifiers |= MODIFIER_ALT;
        }
        return modifiers;
    }

    private static float normalizeAxis(short value) {
        return Math.max(-1f, value / 32767f);
    }

    private static int clampShort(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }
}
