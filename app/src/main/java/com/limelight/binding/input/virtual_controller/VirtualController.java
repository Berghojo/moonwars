/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.kbm.KbmAction;
import com.limelight.binding.input.kbm.KbmBindings;
import com.limelight.binding.input.kbm.KbmController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualController {
    public static class ControllerInputContext {
        public int inputMap = 0;
        public byte leftTrigger = 0x00;
        public byte rightTrigger = 0x00;
        public short rightStickX = 0x0000;
        public short rightStickY = 0x0000;
        public short leftStickX = 0x0000;
        public short leftStickY = 0x0000;
    }

    public enum ControllerMode {
        Active,
        MoveButtons,
        ResizeButtons
    }

    private static final boolean _PRINT_DEBUG_INFORMATION = false;

    private final ControllerHandler controllerHandler;
    private final Context context;
    private final Handler handler;

    private final Runnable delayedRetransmitRunnable = new Runnable() {
        @Override
        public void run() {
            sendControllerInputContextInternal();
        }
    };

    private FrameLayout frame_layout = null;

    ControllerMode currentMode = ControllerMode.Active;
    ControllerInputContext inputContext = new ControllerInputContext();

    private Button buttonConfigure = null;

    private List<VirtualControllerElement> elements = new ArrayList<>();

    // Buttons that show the key they send in keyboard/mouse mode, with their gamepad label
    private final Map<DigitalButton, KbmBindings.Input> boundButtons = new HashMap<>();
    private final Map<DigitalButton, String> defaultLabels = new HashMap<>();

    // The eye button stays visible while every other control is hidden, so
    // controls can be cleared out of the way of something that needs clicking
    private VirtualControllerElement visibilityToggle;
    private boolean overlayHidden;

    // Utility buttons that fold away to the screen edge behind a small arrow tab
    private static final String PREF_TRAY_COLLAPSED = "kbm_tray_collapsed";
    private final List<VirtualControllerElement> trayElements = new ArrayList<>();
    private DigitalButton trayToggle;
    private boolean trayCollapsed;

    public VirtualController(final ControllerHandler controllerHandler, FrameLayout layout, final Context context) {
        this.controllerHandler = controllerHandler;
        this.frame_layout = layout;
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.trayCollapsed = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_TRAY_COLLAPSED, false);

        buttonConfigure = new Button(context);
        buttonConfigure.setAlpha(0.25f);
        buttonConfigure.setFocusable(false);
        buttonConfigure.setBackgroundResource(R.drawable.ic_settings);
        buttonConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;

                if (currentMode == ControllerMode.Active){
                    currentMode = ControllerMode.MoveButtons;
                    message = "Entering configuration mode (Move buttons)";
                } else if (currentMode == ControllerMode.MoveButtons) {
                    currentMode = ControllerMode.ResizeButtons;
                    message = "Entering configuration mode (Resize buttons)";
                } else {
                    currentMode = ControllerMode.Active;
                    VirtualControllerConfigurationLoader.saveProfile(VirtualController.this, context);
                    message = "Exiting configuration mode";
                }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                buttonConfigure.invalidate();

                for (VirtualControllerElement element : elements) {
                    element.invalidate();
                }
            }
        });

    }

    Handler getHandler() {
        return handler;
    }

    public void hide() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.INVISIBLE);
        }

        buttonConfigure.setVisibility(View.INVISIBLE);
    }

    public void show() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.VISIBLE);
        }

        buttonConfigure.setVisibility(View.VISIBLE);
        applyOverlayVisibility();
    }

    void setVisibilityToggle(VirtualControllerElement element) {
        visibilityToggle = element;
    }

    void toggleOverlayVisibility() {
        overlayHidden = !overlayHidden;
        applyOverlayVisibility();
    }

    void addTrayElement(VirtualControllerElement element) {
        trayElements.add(element);
    }

    void setTrayToggle(DigitalButton toggle) {
        trayToggle = toggle;
    }

    void toggleTray() {
        trayCollapsed = !trayCollapsed;
        android.preference.PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(PREF_TRAY_COLLAPSED, trayCollapsed)
                .apply();
        applyOverlayVisibility();
    }

    private void applyOverlayVisibility() {
        if (visibilityToggle == null && trayToggle == null) {
            return;
        }

        for (VirtualControllerElement element : elements) {
            boolean visible;
            if (element == trayToggle) {
                // The tray tab is always reachable at the screen edge
                visible = true;
            }
            else if (trayCollapsed && trayElements.contains(element)) {
                visible = false;
            }
            else {
                // The eye button stays visible so hidden controls can be brought back
                visible = element == visibilityToggle || !overlayHidden;
            }
            element.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
        buttonConfigure.setVisibility(overlayHidden ? View.INVISIBLE : View.VISIBLE);

        if (visibilityToggle != null) {
            // Dim the eye button while everything else is hidden
            visibilityToggle.setAlpha(overlayHidden ? 0.5f : 1f);
        }
        if (trayToggle != null) {
            trayToggle.setText(trayCollapsed ? "\u203A" : "\u2039");
        }
    }

    public void removeElements() {
        for (VirtualControllerElement element : elements) {
            frame_layout.removeView(element);
        }
        elements.clear();
        boundButtons.clear();
        defaultLabels.clear();
        visibilityToggle = null;
        trayElements.clear();
        trayToggle = null;

        frame_layout.removeView(buttonConfigure);
    }

    public void setOpacity(int opacity) {
        for (VirtualControllerElement element : elements) {
            element.setOpacity(opacity);
        }
    }


    public void addElement(VirtualControllerElement element, int x, int y, int width, int height) {
        elements.add(element);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.setMargins(x, y, 0, 0);

        frame_layout.addView(element, layoutParams);
    }

    public List<VirtualControllerElement> getElements() {
        return elements;
    }

    private static final void _DBG(String text) {
        if (_PRINT_DEBUG_INFORMATION) {
            LimeLog.info("VirtualController: " + text);
        }
    }

    public void refreshLayout() {
        removeElements();

        DisplayMetrics screen = context.getResources().getDisplayMetrics();

        int buttonSize = (int)(screen.heightPixels*0.06f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        params.leftMargin = 15;
        params.topMargin = 15;
        frame_layout.addView(buttonConfigure, params);

        // Start with the default layout
        VirtualControllerConfigurationLoader.createDefaultLayout(this, context);

        // Apply user preferences onto the default layout
        VirtualControllerConfigurationLoader.loadFromPreferences(this, context);
        applyOverlayVisibility();

        KbmController kbmController = getKbmController();
        if (kbmController != null) {
            kbmController.setLayerListener(new KbmController.LayerListener() {
                @Override
                public void onLayerChanged(boolean shiftedLayerActive) {
                    updateKbmLabels(shiftedLayerActive);
                }
            });
            updateKbmLabels(kbmController.isShiftedLayerActive());
        }
    }

    KbmController getKbmController() {
        return controllerHandler != null ? controllerHandler.getKbmController() : null;
    }

    void registerBoundButton(DigitalButton button, KbmBindings.Input input, String defaultLabel) {
        boundButtons.put(button, input);
        defaultLabels.put(button, defaultLabel);
    }

    // Label each button with the key it currently sends, so the active skill bar is visible
    private void updateKbmLabels(boolean shiftedLayerActive) {
        KbmController kbmController = getKbmController();
        if (kbmController == null) {
            return;
        }

        KbmBindings bindings = kbmController.getBindings();
        for (Map.Entry<DigitalButton, KbmBindings.Input> entry : boundButtons.entrySet()) {
            KbmAction action = bindings.resolve(entry.getValue(), shiftedLayerActive);
            String label = action.shortLabel();
            if (action.type == KbmAction.Type.LAYER_TOGGLE) {
                label = shiftedLayerActive ? "6-0" : "1-5";
            }
            entry.getKey().setText(label != null ? label : defaultLabels.get(entry.getKey()));
        }
    }

    public ControllerMode getControllerMode() {
        return currentMode;
    }

    public ControllerInputContext getControllerInputContext() {
        return inputContext;
    }

    private void sendControllerInputContextInternal() {
        _DBG("INPUT_MAP + " + inputContext.inputMap);
        _DBG("LEFT_TRIGGER " + inputContext.leftTrigger);
        _DBG("RIGHT_TRIGGER " + inputContext.rightTrigger);
        _DBG("LEFT STICK X: " + inputContext.leftStickX + " Y: " + inputContext.leftStickY);
        _DBG("RIGHT STICK X: " + inputContext.rightStickX + " Y: " + inputContext.rightStickY);

        if (controllerHandler != null) {
            controllerHandler.reportOscState(
                    inputContext.inputMap,
                    inputContext.leftStickX,
                    inputContext.leftStickY,
                    inputContext.rightStickX,
                    inputContext.rightStickY,
                    inputContext.leftTrigger,
                    inputContext.rightTrigger
            );
        }
    }

    void sendMouseButton(byte button, boolean down) {
        if (controllerHandler != null) {
            controllerHandler.reportOscMouseButton(button, down);
        }
    }

    void toggleKeyboard() {
        if (controllerHandler != null) {
            controllerHandler.toggleKeyboard();
        }
    }

    void toggleTouchMode() {
        if (controllerHandler != null) {
            controllerHandler.toggleTouchMode();
        }
    }

    void showSensitivitySettings() {
        if (controllerHandler != null) {
            controllerHandler.showSensitivitySettings();
        }
    }

    void sendControllerInputContext() {
        // Cancel retransmissions of prior gamepad inputs
        handler.removeCallbacks(delayedRetransmitRunnable);

        sendControllerInputContextInternal();

        // HACK: GFE sometimes discards gamepad packets when they are received
        // very shortly after another. This can be critical if an axis zeroing packet
        // is lost and causes an analog stick to get stuck. To avoid this, we retransmit
        // the gamepad state a few times unless another input event happens before then.
        handler.postDelayed(delayedRetransmitRunnable, 25);
        handler.postDelayed(delayedRetransmitRunnable, 50);
        handler.postDelayed(delayedRetransmitRunnable, 75);
    }
}
