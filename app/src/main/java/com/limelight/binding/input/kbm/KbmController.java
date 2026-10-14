package com.limelight.binding.input.kbm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;

/**
 * Feeds gamepad state (from the on-screen controller or physical controllers)
 * into a {@link GamepadKbmMapper} and sends the resulting keyboard and mouse
 * input to the host instead of gamepad packets.
 */
public class KbmController implements GamepadKbmMapper.Output {
    public static final String PREF_ENABLED = "checkbox_gamepad_kbm";
    public static final String PREF_CAMERA_HOLD_RMB = "checkbox_kbm_camera_hold_rmb";
    public static final String PREF_CAMERA_SPEED = "seekbar_kbm_camera_speed";
    public static final String PREF_CURSOR_SPEED = "seekbar_kbm_cursor_speed";
    public static final String PREF_INVERT_Y = "checkbox_kbm_invert_y";
    public static final String PREF_MOVE_DEADZONE = "seekbar_kbm_move_deadzone";

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_CAMERA_HOLD_RMB = true;
    // Pixels per second at full right stick deflection
    public static final int DEFAULT_CAMERA_SPEED = 1500;
    public static final int DEFAULT_CURSOR_SPEED = 800;
    public static final boolean DEFAULT_INVERT_Y = false;
    public static final int DEFAULT_MOVE_DEADZONE = 30;

    private static final long TICK_INTERVAL_MS = 8;

    private final Context context;
    private final NvConnection conn;
    private final GamepadKbmMapper mapper;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ticking;
    private boolean stopped;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (KbmController.this) {
                if (stopped) {
                    ticking = false;
                    return;
                }
                mapper.tick(SystemClock.uptimeMillis());
                if (mapper.needsTick()) {
                    handler.postDelayed(this, TICK_INTERVAL_MS);
                }
                else {
                    ticking = false;
                }
            }
        }
    };

    public KbmController(Context context, NvConnection conn) {
        this.context = context;
        this.conn = conn;
        this.mapper = new GamepadKbmMapper(this, loadConfig(context), loadBindings(context));
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, DEFAULT_ENABLED);
    }

    public static GamepadKbmMapper.Config loadConfig(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        GamepadKbmMapper.Config config = new GamepadKbmMapper.Config();
        config.cameraHoldsRightMouse = prefs.getBoolean(PREF_CAMERA_HOLD_RMB, DEFAULT_CAMERA_HOLD_RMB);
        config.cameraSpeed = prefs.getInt(PREF_CAMERA_SPEED, DEFAULT_CAMERA_SPEED);
        config.cursorSpeed = prefs.getInt(PREF_CURSOR_SPEED, DEFAULT_CURSOR_SPEED);
        config.invertCameraY = prefs.getBoolean(PREF_INVERT_Y, DEFAULT_INVERT_Y);
        config.moveDeadzone = prefs.getInt(PREF_MOVE_DEADZONE, DEFAULT_MOVE_DEADZONE) / 100f;
        return config;
    }

    public static KbmBindings loadBindings(Context context) {
        return KbmBindings.fromMap(PreferenceManager.getDefaultSharedPreferences(context).getAll());
    }

    public static void saveBindings(Context context, KbmBindings bindings) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        for (java.util.Map.Entry<String, String> entry : bindings.toMap().entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    public synchronized void onGamepadState(int buttonFlags,
                                            short leftStickX, short leftStickY,
                                            short rightStickX, short rightStickY,
                                            byte leftTrigger, byte rightTrigger) {
        if (stopped) {
            return;
        }
        mapper.update(buttonFlags, leftStickX, leftStickY, rightStickX, rightStickY,
                leftTrigger, rightTrigger, SystemClock.uptimeMillis());
        if (!ticking && mapper.needsTick()) {
            ticking = true;
            handler.post(tickRunnable);
        }
    }

    public synchronized void onScreenMouseButton(int button, boolean down) {
        if (stopped) {
            return;
        }
        mapper.setMouseButton(button, down);
    }

    /** Releases everything held on the host and stops processing input. */
    public synchronized void stop() {
        if (stopped) {
            return;
        }
        mapper.releaseAll();
        stopped = true;
        handler.removeCallbacks(tickRunnable);
        ticking = false;
    }

    @Override
    public void keyDown(int keyCode, byte modifiers) {
        conn.sendKeyboardInput(toGameStreamKey(keyCode), KeyboardPacket.KEY_DOWN, modifiers, (byte) 0);
    }

    @Override
    public void keyUp(int keyCode, byte modifiers) {
        conn.sendKeyboardInput(toGameStreamKey(keyCode), KeyboardPacket.KEY_UP, modifiers, (byte) 0);
    }

    @Override
    public void mouseButtonDown(int button) {
        conn.sendMouseButtonDown((byte) button);
    }

    @Override
    public void mouseButtonUp(int button) {
        conn.sendMouseButtonUp((byte) button);
    }

    @Override
    public void mouseMove(int dx, int dy) {
        conn.sendMouseMove((short) dx, (short) dy);
    }

    @Override
    public void mouseScroll(int clicks) {
        conn.sendMouseScroll((byte) clicks);
    }

    @Override
    public void cameraModeChanged(final boolean holdRightMouse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, holdRightMouse ?
                        "Right stick: camera (holds right mouse button)" :
                        "Right stick: mouse cursor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static short toGameStreamKey(int vk) {
        // GameStream prefixes every key code with 0x80 (see KeyboardTranslator)
        return (short) ((0x80 << 8) | (vk & 0xFF));
    }
}
