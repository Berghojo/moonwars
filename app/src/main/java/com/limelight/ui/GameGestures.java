package com.limelight.ui;

public interface GameGestures {
    void toggleKeyboard();

    // Switches touch input between trackpad mode and direct click mode
    void toggleTouchMode();

    // Opens the in-stream sensitivity settings
    void showSensitivitySettings();

    // Switches whether tapping the stream left or right clicks; returns true for right click
    boolean toggleTapRightClick();
}
