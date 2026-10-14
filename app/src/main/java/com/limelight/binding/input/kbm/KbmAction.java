package com.limelight.binding.input.kbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Something a gamepad input can be bound to when gamepad-to-keyboard/mouse
 * mapping is enabled: a keyboard key, a mouse button, a wheel tick, or one
 * of the special mapper actions.
 *
 * This class has no Android dependencies so it can be unit tested on the JVM.
 */
public final class KbmAction {
    public enum Type {
        NONE,
        KEY,
        MOUSE_BUTTON,
        WHEEL_UP,
        WHEEL_DOWN,
        // While held, buttons use their "shifted" binding instead of their base binding
        LAYER_SHIFT,
        // Toggles whether the camera stick holds the right mouse button while it moves
        TOGGLE_CAMERA_MODE,
    }

    // Windows virtual key codes (what GameStream/Sunshine expect)
    public static final int VK_BACK = 0x08;
    public static final int VK_TAB = 0x09;
    public static final int VK_RETURN = 0x0D;
    public static final int VK_ESCAPE = 0x1B;
    public static final int VK_SPACE = 0x20;
    public static final int VK_PRIOR = 0x21;
    public static final int VK_NEXT = 0x22;
    public static final int VK_END = 0x23;
    public static final int VK_HOME = 0x24;
    public static final int VK_LEFT = 0x25;
    public static final int VK_UP = 0x26;
    public static final int VK_RIGHT = 0x27;
    public static final int VK_DOWN = 0x28;
    public static final int VK_INSERT = 0x2D;
    public static final int VK_DELETE = 0x2E;
    public static final int VK_F1 = 0x70;
    public static final int VK_LSHIFT = 0xA0;
    public static final int VK_LCONTROL = 0xA2;
    public static final int VK_LMENU = 0xA4;
    public static final int VK_OEM_1 = 0xBA;      // ;
    public static final int VK_OEM_PLUS = 0xBB;   // =
    public static final int VK_OEM_COMMA = 0xBC;  // ,
    public static final int VK_OEM_MINUS = 0xBD;  // -
    public static final int VK_OEM_PERIOD = 0xBE; // .
    public static final int VK_OEM_2 = 0xBF;      // /
    public static final int VK_OEM_3 = 0xC0;      // `
    public static final int VK_OEM_4 = 0xDB;      // [
    public static final int VK_OEM_5 = 0xDC;      // \
    public static final int VK_OEM_6 = 0xDD;      // ]
    public static final int VK_OEM_7 = 0xDE;      // '

    // Mouse button IDs (match MouseButtonPacket)
    public static final int MOUSE_LEFT = 1;
    public static final int MOUSE_MIDDLE = 2;
    public static final int MOUSE_RIGHT = 3;
    public static final int MOUSE_X1 = 4;
    public static final int MOUSE_X2 = 5;

    public static final KbmAction NONE = new KbmAction(Type.NONE, 0, "None");
    public static final KbmAction LAYER_SHIFT = new KbmAction(Type.LAYER_SHIFT, 0, "Layer shift (hold)");
    public static final KbmAction TOGGLE_CAMERA_MODE = new KbmAction(Type.TOGGLE_CAMERA_MODE, 0, "Toggle camera / cursor mode");
    public static final KbmAction WHEEL_UP = new KbmAction(Type.WHEEL_UP, 0, "Mouse wheel up");
    public static final KbmAction WHEEL_DOWN = new KbmAction(Type.WHEEL_DOWN, 0, "Mouse wheel down");

    private static final List<KbmAction> ALL;

    static {
        List<KbmAction> list = new ArrayList<>();
        list.add(NONE);
        list.add(LAYER_SHIFT);
        list.add(TOGGLE_CAMERA_MODE);
        list.add(mouse(MOUSE_LEFT));
        list.add(mouse(MOUSE_RIGHT));
        list.add(mouse(MOUSE_MIDDLE));
        list.add(mouse(MOUSE_X1));
        list.add(mouse(MOUSE_X2));
        list.add(WHEEL_UP);
        list.add(WHEEL_DOWN);
        for (int vk : new int[] {
                VK_SPACE, VK_TAB, VK_ESCAPE, VK_RETURN, VK_BACK,
                VK_LSHIFT, VK_LCONTROL, VK_LMENU,
                VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT,
        }) {
            list.add(key(vk));
        }
        for (int vk = '0'; vk <= '9'; vk++) {
            list.add(key(vk));
        }
        for (int vk = 'A'; vk <= 'Z'; vk++) {
            list.add(key(vk));
        }
        for (int i = 0; i < 12; i++) {
            list.add(key(VK_F1 + i));
        }
        for (int vk : new int[] {
                VK_OEM_3, VK_OEM_MINUS, VK_OEM_PLUS, VK_OEM_4, VK_OEM_6, VK_OEM_5,
                VK_OEM_1, VK_OEM_7, VK_OEM_COMMA, VK_OEM_PERIOD, VK_OEM_2,
                VK_INSERT, VK_DELETE, VK_HOME, VK_END, VK_PRIOR, VK_NEXT,
        }) {
            list.add(key(vk));
        }
        ALL = Collections.unmodifiableList(list);
    }

    public final Type type;
    // VK code for KEY, button ID for MOUSE_BUTTON, otherwise 0
    public final int code;
    public final String label;

    private KbmAction(Type type, int code, String label) {
        this.type = type;
        this.code = code;
        this.label = label;
    }

    public static KbmAction key(int vk) {
        return new KbmAction(Type.KEY, vk, keyLabel(vk));
    }

    public static KbmAction mouse(int button) {
        String label;
        switch (button) {
            case MOUSE_LEFT: label = "Left mouse button"; break;
            case MOUSE_RIGHT: label = "Right mouse button"; break;
            case MOUSE_MIDDLE: label = "Middle mouse button"; break;
            case MOUSE_X1: label = "Mouse button 4 (back)"; break;
            case MOUSE_X2: label = "Mouse button 5 (forward)"; break;
            default: label = "Mouse button " + button; break;
        }
        return new KbmAction(Type.MOUSE_BUTTON, button, label);
    }

    /** Every action selectable in the bindings editor, in display order. */
    public static List<KbmAction> all() {
        return ALL;
    }

    public boolean isModifierKey() {
        return type == Type.KEY && (code == VK_LSHIFT || code == VK_LCONTROL || code == VK_LMENU);
    }

    public String encode() {
        switch (type) {
            case KEY: return "key:" + code;
            case MOUSE_BUTTON: return "mouse:" + code;
            case WHEEL_UP: return "wheel_up";
            case WHEEL_DOWN: return "wheel_down";
            case LAYER_SHIFT: return "shift";
            case TOGGLE_CAMERA_MODE: return "toggle_camera";
            default: return "none";
        }
    }

    /** Parses a value produced by {@link #encode()}, or returns null if it is malformed. */
    public static KbmAction decode(String s) {
        if (s == null) {
            return null;
        }
        switch (s) {
            case "none": return NONE;
            case "wheel_up": return WHEEL_UP;
            case "wheel_down": return WHEEL_DOWN;
            case "shift": return LAYER_SHIFT;
            case "toggle_camera": return TOGGLE_CAMERA_MODE;
        }
        try {
            if (s.startsWith("key:")) {
                int vk = Integer.parseInt(s.substring(4));
                if (vk > 0 && vk < 0xFF) {
                    return key(vk);
                }
            }
            else if (s.startsWith("mouse:")) {
                int button = Integer.parseInt(s.substring(6));
                if (button >= MOUSE_LEFT && button <= MOUSE_X2) {
                    return mouse(button);
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    static String keyLabel(int vk) {
        if ((vk >= '0' && vk <= '9') || (vk >= 'A' && vk <= 'Z')) {
            return String.valueOf((char) vk);
        }
        if (vk >= VK_F1 && vk < VK_F1 + 24) {
            return "F" + (vk - VK_F1 + 1);
        }
        switch (vk) {
            case VK_BACK: return "Backspace";
            case VK_TAB: return "Tab";
            case VK_RETURN: return "Enter";
            case VK_ESCAPE: return "Esc";
            case VK_SPACE: return "Space";
            case VK_PRIOR: return "Page Up";
            case VK_NEXT: return "Page Down";
            case VK_END: return "End";
            case VK_HOME: return "Home";
            case VK_LEFT: return "Arrow Left";
            case VK_UP: return "Arrow Up";
            case VK_RIGHT: return "Arrow Right";
            case VK_DOWN: return "Arrow Down";
            case VK_INSERT: return "Insert";
            case VK_DELETE: return "Delete";
            case VK_LSHIFT: return "Shift";
            case VK_LCONTROL: return "Ctrl";
            case VK_LMENU: return "Alt";
            case VK_OEM_1: return ";";
            case VK_OEM_PLUS: return "=";
            case VK_OEM_COMMA: return ",";
            case VK_OEM_MINUS: return "-";
            case VK_OEM_PERIOD: return ".";
            case VK_OEM_2: return "/";
            case VK_OEM_3: return "` (backtick)";
            case VK_OEM_4: return "[";
            case VK_OEM_5: return "\\";
            case VK_OEM_6: return "]";
            case VK_OEM_7: return "'";
            default: return String.format("Key 0x%02X", vk);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KbmAction)) {
            return false;
        }
        KbmAction other = (KbmAction) o;
        return type == other.type && code == other.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, code);
    }

    @Override
    public String toString() {
        return label;
    }
}
