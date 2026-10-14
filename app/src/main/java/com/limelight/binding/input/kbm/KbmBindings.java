package com.limelight.binding.input.kbm;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Two layers of gamepad -> keyboard/mouse bindings. The shifted layer is used
 * while an input bound to {@link KbmAction#LAYER_SHIFT} is held. A shifted
 * binding of {@link KbmAction#NONE} falls back to the base binding.
 *
 * Left stick directions always use the base layer so movement never changes
 * while the layer shift is held.
 *
 * This class has no Android dependencies so it can be unit tested on the JVM.
 */
public final class KbmBindings {
    public enum Input {
        // Button flags match com.limelight.nvstream.input.ControllerPacket
        A("A", 0x1000),
        B("B", 0x2000),
        X("X", 0x4000),
        Y("Y", 0x8000),
        LB("LB", 0x0100),
        RB("RB", 0x0200),
        LT("LT", 0),
        RT("RT", 0),
        DPAD_UP("D-pad up", 0x0001),
        DPAD_DOWN("D-pad down", 0x0002),
        DPAD_LEFT("D-pad left", 0x0004),
        DPAD_RIGHT("D-pad right", 0x0008),
        START("Start", 0x0010),
        BACK("Back / Select", 0x0020),
        L3("L3 (left stick click)", 0x0040),
        R3("R3 (right stick click)", 0x0080),
        GUIDE("Guide", 0x0400),
        LS_UP("Left stick up", 0),
        LS_DOWN("Left stick down", 0),
        LS_LEFT("Left stick left", 0),
        LS_RIGHT("Left stick right", 0);

        public final String label;
        public final int buttonFlag;

        Input(String label, int buttonFlag) {
            this.label = label;
            this.buttonFlag = buttonFlag;
        }

        public boolean isStickDirection() {
            return this == LS_UP || this == LS_DOWN || this == LS_LEFT || this == LS_RIGHT;
        }
    }

    public static final String PREF_PREFIX_BASE = "kbm_bind_base_";
    public static final String PREF_PREFIX_SHIFT = "kbm_bind_shift_";

    private final EnumMap<Input, KbmAction> base = new EnumMap<>(Input.class);
    private final EnumMap<Input, KbmAction> shifted = new EnumMap<>(Input.class);

    public KbmBindings() {
        for (Input input : Input.values()) {
            base.put(input, KbmAction.NONE);
            shifted.put(input, KbmAction.NONE);
        }
    }

    /** Defaults tuned for Guild Wars 2's default keybinds. */
    public static KbmBindings guildWars2Defaults() {
        KbmBindings b = new KbmBindings();

        b.setBase(Input.LS_UP, KbmAction.key('W'));
        b.setBase(Input.LS_LEFT, KbmAction.key('A'));
        b.setBase(Input.LS_DOWN, KbmAction.key('S'));
        b.setBase(Input.LS_RIGHT, KbmAction.key('D'));

        // Weapon skills on the face buttons + RB, utility skills on LB + the same buttons
        b.setBase(Input.A, KbmAction.key('1'));
        b.setBase(Input.B, KbmAction.key('2'));
        b.setBase(Input.X, KbmAction.key('3'));
        b.setBase(Input.Y, KbmAction.key('4'));
        b.setBase(Input.RB, KbmAction.key('5'));
        b.setShifted(Input.A, KbmAction.key('6'));
        b.setShifted(Input.B, KbmAction.key('7'));
        b.setShifted(Input.X, KbmAction.key('8'));
        b.setShifted(Input.Y, KbmAction.key('9'));
        b.setShifted(Input.RB, KbmAction.key('0'));
        b.setBase(Input.LB, KbmAction.LAYER_SHIFT);

        // Dodge and jump
        b.setBase(Input.LT, KbmAction.key('V'));
        b.setBase(Input.RT, KbmAction.key(KbmAction.VK_SPACE));

        // Profession skills on the D-pad
        b.setBase(Input.DPAD_UP, KbmAction.key(KbmAction.VK_F1));
        b.setBase(Input.DPAD_RIGHT, KbmAction.key(KbmAction.VK_F1 + 1));
        b.setBase(Input.DPAD_DOWN, KbmAction.key(KbmAction.VK_F1 + 2));
        b.setBase(Input.DPAD_LEFT, KbmAction.key(KbmAction.VK_F1 + 3));
        b.setShifted(Input.DPAD_UP, KbmAction.key(KbmAction.VK_F1 + 4));
        b.setShifted(Input.DPAD_RIGHT, KbmAction.key(KbmAction.VK_OEM_3)); // weapon swap
        b.setShifted(Input.DPAD_DOWN, KbmAction.WHEEL_DOWN); // zoom out
        b.setShifted(Input.DPAD_LEFT, KbmAction.WHEEL_UP); // zoom in

        // Targeting and interaction
        b.setBase(Input.L3, KbmAction.key(KbmAction.VK_TAB));
        b.setBase(Input.R3, KbmAction.key('F'));

        // Menus
        b.setBase(Input.START, KbmAction.key(KbmAction.VK_ESCAPE));
        b.setBase(Input.BACK, KbmAction.key('M'));
        b.setShifted(Input.BACK, KbmAction.key('I'));
        b.setBase(Input.GUIDE, KbmAction.TOGGLE_CAMERA_MODE);

        return b;
    }

    public KbmAction getBase(Input input) {
        return base.get(input);
    }

    public KbmAction getShifted(Input input) {
        return shifted.get(input);
    }

    public void setBase(Input input, KbmAction action) {
        base.put(input, action != null ? action : KbmAction.NONE);
    }

    public void setShifted(Input input, KbmAction action) {
        shifted.put(input, action != null ? action : KbmAction.NONE);
    }

    /** The action an input triggers when pressed with the given layer state. */
    public KbmAction resolve(Input input, boolean shiftActive) {
        if (shiftActive && !input.isStickDirection()) {
            KbmAction action = shifted.get(input);
            if (action.type != KbmAction.Type.NONE) {
                return action;
            }
        }
        return base.get(input);
    }

    /** Serializes to preference key/value pairs. */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        for (Input input : Input.values()) {
            map.put(PREF_PREFIX_BASE + input.name(), base.get(input).encode());
            if (!input.isStickDirection()) {
                map.put(PREF_PREFIX_SHIFT + input.name(), shifted.get(input).encode());
            }
        }
        return map;
    }

    /**
     * Loads bindings from preference key/value pairs, using the GW2 defaults
     * for anything missing or malformed.
     */
    public static KbmBindings fromMap(Map<String, ?> map) {
        KbmBindings b = guildWars2Defaults();
        for (Input input : Input.values()) {
            KbmAction action = decodeValue(map.get(PREF_PREFIX_BASE + input.name()));
            if (action != null) {
                b.setBase(input, action);
            }
            if (!input.isStickDirection()) {
                action = decodeValue(map.get(PREF_PREFIX_SHIFT + input.name()));
                if (action != null) {
                    b.setShifted(input, action);
                }
            }
        }
        return b;
    }

    private static KbmAction decodeValue(Object value) {
        return value instanceof String ? KbmAction.decode((String) value) : null;
    }
}
