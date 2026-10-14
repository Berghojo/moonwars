/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

import com.limelight.R;
import com.limelight.binding.input.kbm.KbmBindings;
import com.limelight.nvstream.input.ControllerPacket;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class VirtualControllerConfigurationLoader {
    public static final String OSC_PREFERENCE = "OSC";

    private static int getPercent(
            int percent,
            int total) {
        return (int) (((float) total / (float) 100) * (float) percent);
    }

    // The default controls are specified using a grid of 128*72 cells at 16:9
    private static int screenScale(int units, int height) {
        return (int) (((float) height / (float) 72) * (float) units);
    }

    private static DigitalPad createDigitalPad(
            final VirtualController controller,
            final Context context) {

        DigitalPad digitalPad = new DigitalPad(controller, context);
        digitalPad.addDigitalPadListener(new DigitalPad.DigitalPadListener() {
            @Override
            public void onDirectionChange(int direction) {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();

                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_LEFT) != 0) {
                    inputContext.inputMap |= ControllerPacket.LEFT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.LEFT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_RIGHT) != 0) {
                    inputContext.inputMap |= ControllerPacket.RIGHT_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.RIGHT_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_UP) != 0) {
                    inputContext.inputMap |= ControllerPacket.UP_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.UP_FLAG;
                }
                if ((direction & DigitalPad.DIGITAL_PAD_DIRECTION_DOWN) != 0) {
                    inputContext.inputMap |= ControllerPacket.DOWN_FLAG;
                }
                else {
                    inputContext.inputMap &= ~ControllerPacket.DOWN_FLAG;
                }

                controller.sendControllerInputContext();
            }
        });

        return digitalPad;
    }

    private static DigitalButton createDigitalButton(
            final int elementId,
            final int keyShort,
            final int keyLong,
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        DigitalButton button = new DigitalButton(controller, elementId, layer, context);
        button.setText(text);
        button.setIcon(icon);

        KbmBindings.Input input = KbmBindings.Input.forButtonFlag(keyShort);
        if (input != null) {
            controller.registerBoundButton(button, input, text);
        }

        button.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyShort;

                controller.sendControllerInputContext();
            }

            @Override
            public void onLongClick() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap |= keyLong;

                controller.sendControllerInputContext();
            }

            @Override
            public void onRelease() {
                VirtualController.ControllerInputContext inputContext =
                        controller.getControllerInputContext();
                inputContext.inputMap &= ~keyShort;
                inputContext.inputMap &= ~keyLong;

                controller.sendControllerInputContext();
            }
        });

        return button;
    }

    private static DigitalButton createMouseButton(
            final int elementId,
            final byte mouseButton,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        DigitalButton button = new DigitalButton(controller, elementId, 1, context);
        button.setText(text);
        button.setIcon(icon);

        button.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
                controller.sendMouseButton(mouseButton, true);
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onRelease() {
                controller.sendMouseButton(mouseButton, false);
            }
        });

        return button;
    }

    // A button that runs an action when released
    private static DigitalButton createActionButton(
            final int elementId,
            final String text,
            final int icon,
            final Runnable action,
            final VirtualController controller,
            final Context context) {
        DigitalButton button = new DigitalButton(controller, elementId, 1, context);
        button.setText(text);
        button.setIcon(icon);

        button.addDigitalButtonListener(new DigitalButton.DigitalButtonListener() {
            @Override
            public void onClick() {
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onRelease() {
                // Act on release so a keyboard or mode change doesn't steal the touch that triggered it
                action.run();
            }
        });

        return button;
    }

    private static DigitalButton createLeftTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        LeftTrigger button = new LeftTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        controller.registerBoundButton(button, KbmBindings.Input.LT, text);
        return button;
    }

    private static DigitalButton createRightTrigger(
            final int layer,
            final String text,
            final int icon,
            final VirtualController controller,
            final Context context) {
        RightTrigger button = new RightTrigger(controller, layer, context);
        button.setText(text);
        button.setIcon(icon);
        controller.registerBoundButton(button, KbmBindings.Input.RT, text);
        return button;
    }

    private static AnalogStick createLeftStick(
            final VirtualController controller,
            final Context context) {
        return new LeftAnalogStick(controller, context);
    }

    private static AnalogStick createRightStick(
            final VirtualController controller,
            final Context context) {
        return new RightAnalogStick(controller, context);
    }


    private static final int TRIGGER_L_BASE_X = 1;
    private static final int TRIGGER_R_BASE_X = 92;
    private static final int TRIGGER_DISTANCE = 23;
    private static final int TRIGGER_BASE_Y = 31;
    private static final int TRIGGER_WIDTH = 12;
    private static final int TRIGGER_HEIGHT = 9;

    // Face buttons are defined based on the Y button (button number 9)
    private static final int BUTTON_BASE_X = 106;
    private static final int BUTTON_BASE_Y = 1;
    private static final int BUTTON_SIZE = 10;

    private static final int DPAD_BASE_X = 4;
    private static final int DPAD_BASE_Y = 41;
    private static final int DPAD_SIZE = 30;

    private static final int ANALOG_L_BASE_X = 6;
    private static final int ANALOG_L_BASE_Y = 4;
    private static final int ANALOG_R_BASE_X = 98;
    private static final int ANALOG_R_BASE_Y = 42;
    private static final int ANALOG_SIZE = 26;

    private static final int L3_R3_BASE_Y = 60;

    private static final int START_X = 83;
    private static final int BACK_X = 34;
    private static final int START_BACK_Y = 64;
    private static final int START_BACK_WIDTH = 12;
    private static final int START_BACK_HEIGHT = 7;

    // Make the Guide Menu be in the center of START and BACK menu
    private static final int GUIDE_X = START_X-BACK_X;
    private static final int GUIDE_Y = START_BACK_Y;

    // Mouse click buttons sit left of the right stick, the keyboard button
    // at the top right of the left stick
    private static final int MOUSE_BUTTON_X = 84;
    private static final int MOUSE_LEFT_Y = 42;
    private static final int MOUSE_RIGHT_Y = 52;
    private static final int MOUSE_BUTTON_WIDTH = 12;
    private static final int MOUSE_BUTTON_HEIGHT = 9;
    // Top row of icon buttons, right of the left stick
    private static final int KEYBOARD_X = 36;
    private static final int KEYBOARD_Y = 1;
    private static final int KEYBOARD_WIDTH = 10;
    private static final int KEYBOARD_HEIGHT = 8;
    private static final int TOUCH_MODE_X = 47;
    private static final int SENSITIVITY_X = 58;
    private static final int VISIBILITY_X = 69;

    // Keyboard/mouse mode only: skill bar swap and an extra skill button (Dragon
    // Trigger for Bladesworn), left of the face buttons
    private static final int KBM_EXTRA_X = 84;
    private static final int BAR_Y = 2;
    private static final int EXTRA_SKILL_Y = 12;

    public static void createDefaultLayout(final VirtualController controller, final Context context) {

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        // Displace controls on the right by this amount of pixels to account for different aspect ratios
        int rightDisplacement = screen.widthPixels - screen.heightPixels * 16 / 9;

        int height = screen.heightPixels;

        // In keyboard/mouse mode, touching the stream turns the camera (drag), clicks (tap)
        // and zooms (two fingers), so the right stick, mouse buttons, touch mode button,
        // guide (camera/cursor toggle) and LB (zoom) are left out of the default layout
        final boolean kbm = controller.getKbmController() != null;

        // NOTE: Some of these getPercent() expressions seem like they can be combined
        // into a single call. Due to floating point rounding, this isn't actually possible.

        if (!config.onlyL3R3)
        {
            controller.addElement(createDigitalPad(controller, context),
                    screenScale(DPAD_BASE_X, height),
                    screenScale(DPAD_BASE_Y, height),
                    screenScale(DPAD_SIZE, height),
                    screenScale(DPAD_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_A,
                    !config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                    !config.flipFaceButtons ? "A" : "B", -1, controller, context),
                    screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + 2 * BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_B,
                    config.flipFaceButtons ? ControllerPacket.A_FLAG : ControllerPacket.B_FLAG, 0, 1,
                    config.flipFaceButtons ? "A" : "B", -1, controller, context),
                    screenScale(BUTTON_BASE_X + BUTTON_SIZE, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_X,
                    !config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                    !config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                    screenScale(BUTTON_BASE_X - BUTTON_SIZE, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y + BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_Y,
                    config.flipFaceButtons ? ControllerPacket.X_FLAG : ControllerPacket.Y_FLAG, 0, 1,
                    config.flipFaceButtons ? "X" : "Y", -1, controller, context),
                    screenScale(BUTTON_BASE_X, height) + rightDisplacement,
                    screenScale(BUTTON_BASE_Y, height),
                    screenScale(BUTTON_SIZE, height),
                    screenScale(BUTTON_SIZE, height)
            );

            controller.addElement(createLeftTrigger(
                    1, "LT", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X, height),
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createRightTrigger(
                    1, "RT", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X + TRIGGER_DISTANCE, height) + rightDisplacement,
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            if (!kbm) {
                controller.addElement(createDigitalButton(
                        VirtualControllerElement.EID_LB,
                        ControllerPacket.LB_FLAG, 0, 1, "LB", -1, controller, context),
                        screenScale(TRIGGER_L_BASE_X + TRIGGER_DISTANCE, height),
                        screenScale(TRIGGER_BASE_Y, height),
                        screenScale(TRIGGER_WIDTH, height),
                        screenScale(TRIGGER_HEIGHT, height)
                );
            }

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_RB,
                    ControllerPacket.RB_FLAG, 0, 1, "RB", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X, height) + rightDisplacement,
                    screenScale(TRIGGER_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createLeftStick(controller, context),
                    screenScale(ANALOG_L_BASE_X, height),
                    screenScale(ANALOG_L_BASE_Y, height),
                    screenScale(ANALOG_SIZE, height),
                    screenScale(ANALOG_SIZE, height)
            );

            if (!kbm) {
                controller.addElement(createRightStick(controller, context),
                        screenScale(ANALOG_R_BASE_X, height) + rightDisplacement,
                        screenScale(ANALOG_R_BASE_Y, height),
                        screenScale(ANALOG_SIZE, height),
                        screenScale(ANALOG_SIZE, height)
                );
            }

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_BACK,
                    ControllerPacket.BACK_FLAG, 0, 2, "BACK", -1, controller, context),
                    screenScale(BACK_X, height),
                    screenScale(START_BACK_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_START,
                    ControllerPacket.PLAY_FLAG, 0, 3, "START", -1, controller, context),
                    screenScale(START_X, height) + rightDisplacement,
                    screenScale(START_BACK_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );
        }
        else {
            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_LSB,
                    ControllerPacket.LS_CLK_FLAG, 0, 1, "L3", -1, controller, context),
                    screenScale(TRIGGER_L_BASE_X, height),
                    screenScale(L3_R3_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(
                    VirtualControllerElement.EID_RSB,
                    ControllerPacket.RS_CLK_FLAG, 0, 1, "R3", -1, controller, context),
                    screenScale(TRIGGER_R_BASE_X + TRIGGER_DISTANCE, height) + rightDisplacement,
                    screenScale(L3_R3_BASE_Y, height),
                    screenScale(TRIGGER_WIDTH, height),
                    screenScale(TRIGGER_HEIGHT, height)
            );
        }

        if (config.showGuideButton && !kbm) {
            controller.addElement(createDigitalButton(VirtualControllerElement.EID_GDB,
                            ControllerPacket.SPECIAL_BUTTON_FLAG, 0, 1, "GUIDE", -1, controller, context),
                    screenScale(GUIDE_X, height)+ rightDisplacement,
                    screenScale(GUIDE_Y, height),
                    screenScale(START_BACK_WIDTH, height),
                    screenScale(START_BACK_HEIGHT, height)
            );
        }

        if (!kbm) {
            controller.addElement(createMouseButton(VirtualControllerElement.EID_MOUSE_LEFT,
                            MouseButtonPacket.BUTTON_LEFT, "LMB", R.drawable.ic_osc_mouse_left, controller, context),
                    screenScale(MOUSE_BUTTON_X, height) + rightDisplacement,
                    screenScale(MOUSE_LEFT_Y, height),
                    screenScale(MOUSE_BUTTON_WIDTH, height),
                    screenScale(MOUSE_BUTTON_HEIGHT, height)
            );

            controller.addElement(createMouseButton(VirtualControllerElement.EID_MOUSE_RIGHT,
                            MouseButtonPacket.BUTTON_RIGHT, "RMB", R.drawable.ic_osc_mouse_right, controller, context),
                    screenScale(MOUSE_BUTTON_X, height) + rightDisplacement,
                    screenScale(MOUSE_RIGHT_Y, height),
                    screenScale(MOUSE_BUTTON_WIDTH, height),
                    screenScale(MOUSE_BUTTON_HEIGHT, height)
            );
        }

        controller.addElement(createActionButton(VirtualControllerElement.EID_KEYBOARD, "KEYBOARD", R.drawable.ic_osc_keyboard,
                        new Runnable() {
                            @Override
                            public void run() {
                                controller.toggleKeyboard();
                            }
                        }, controller, context),
                screenScale(KEYBOARD_X, height),
                screenScale(KEYBOARD_Y, height),
                screenScale(KEYBOARD_WIDTH, height),
                screenScale(KEYBOARD_HEIGHT, height)
        );

        if (controller.getKbmController() != null) {
            controller.addElement(createDigitalButton(VirtualControllerElement.EID_KBM_BAR,
                            KbmBindings.Input.OSC_BAR.buttonFlag, 0, 1, "BAR", -1, controller, context),
                    screenScale(KBM_EXTRA_X, height) + rightDisplacement,
                    screenScale(BAR_Y, height),
                    screenScale(MOUSE_BUTTON_WIDTH, height),
                    screenScale(MOUSE_BUTTON_HEIGHT, height)
            );

            controller.addElement(createDigitalButton(VirtualControllerElement.EID_KBM_EXTRA,
                            KbmBindings.Input.OSC_EXTRA.buttonFlag, 0, 1, "EXTRA", -1, controller, context),
                    screenScale(KBM_EXTRA_X, height) + rightDisplacement,
                    screenScale(EXTRA_SKILL_Y, height),
                    screenScale(MOUSE_BUTTON_WIDTH, height),
                    screenScale(MOUSE_BUTTON_HEIGHT, height)
            );
        }

        if (!kbm) {
            controller.addElement(createActionButton(VirtualControllerElement.EID_TOUCH_MODE, "TOUCH", R.drawable.ic_osc_touch,
                            new Runnable() {
                                @Override
                                public void run() {
                                    controller.toggleTouchMode();
                                }
                            }, controller, context),
                    screenScale(TOUCH_MODE_X, height),
                    screenScale(KEYBOARD_Y, height),
                    screenScale(KEYBOARD_WIDTH, height),
                    screenScale(KEYBOARD_HEIGHT, height)
            );
        }

        controller.addElement(createActionButton(VirtualControllerElement.EID_SENSITIVITY, "SENS", R.drawable.ic_osc_tune,
                        new Runnable() {
                            @Override
                            public void run() {
                                controller.showSensitivitySettings();
                            }
                        }, controller, context),
                screenScale(kbm ? TOUCH_MODE_X : SENSITIVITY_X, height),
                screenScale(KEYBOARD_Y, height),
                screenScale(KEYBOARD_WIDTH, height),
                screenScale(KEYBOARD_HEIGHT, height)
        );

        DigitalButton visibilityButton = createActionButton(VirtualControllerElement.EID_VISIBILITY, "HIDE",
                R.drawable.ic_osc_visibility,
                new Runnable() {
                    @Override
                    public void run() {
                        controller.toggleOverlayVisibility();
                    }
                }, controller, context);
        controller.addElement(visibilityButton,
                screenScale(kbm ? SENSITIVITY_X : VISIBILITY_X, height),
                screenScale(KEYBOARD_Y, height),
                screenScale(KEYBOARD_WIDTH, height),
                screenScale(KEYBOARD_HEIGHT, height)
        );
        controller.setVisibilityToggle(visibilityButton);

        controller.setOpacity(config.oscOpacity);
    }

    public static void saveProfile(final VirtualController controller,
                                   final Context context) {
        SharedPreferences.Editor prefEditor = context.getSharedPreferences(OSC_PREFERENCE, Activity.MODE_PRIVATE).edit();

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;
            try {
                prefEditor.putString(prefKey, element.getConfiguration().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        prefEditor.apply();
    }

    public static void loadFromPreferences(final VirtualController controller, final Context context) {
        SharedPreferences pref = context.getSharedPreferences(OSC_PREFERENCE, Activity.MODE_PRIVATE);

        for (VirtualControllerElement element : controller.getElements()) {
            String prefKey = ""+element.elementId;

            String jsonConfig = pref.getString(prefKey, null);
            if (jsonConfig != null) {
                try {
                    element.loadConfiguration(new JSONObject(jsonConfig));
                } catch (JSONException e) {
                    e.printStackTrace();

                    // Remove the corrupt element from the preferences
                    pref.edit().remove(prefKey).apply();
                }
            }
        }
    }
}
