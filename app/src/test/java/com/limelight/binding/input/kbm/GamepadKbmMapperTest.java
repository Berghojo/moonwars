package com.limelight.binding.input.kbm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamepadKbmMapperTest {
    private static final short FULL = 32767;
    private static final int A = 0x1000;
    private static final int LB = 0x0100;
    private static final int RB = 0x0200;

    private static class RecordingOutput implements GamepadKbmMapper.Output {
        final List<String> events = new ArrayList<>();
        int mouseX, mouseY;

        @Override public void keyDown(int keyCode, byte modifiers) { events.add("down " + KbmAction.keyLabel(keyCode)); }
        @Override public void keyUp(int keyCode, byte modifiers) { events.add("up " + KbmAction.keyLabel(keyCode)); }
        @Override public void mouseButtonDown(int button) { events.add("mdown " + button); }
        @Override public void mouseButtonUp(int button) { events.add("mup " + button); }
        @Override public void mouseMove(int dx, int dy) { mouseX += dx; mouseY += dy; }
        @Override public void mouseScroll(int clicks) { events.add("scroll " + clicks); }
    }

    private RecordingOutput out;
    private GamepadKbmMapper.Config config;
    private GamepadKbmMapper mapper;

    @Before
    public void setUp() {
        out = new RecordingOutput();
        config = new GamepadKbmMapper.Config();
        mapper = new GamepadKbmMapper(out, config, KbmBindings.guildWars2Defaults());
    }

    private void state(int buttons, int lx, int ly, int rx, int ry, long now) {
        mapper.update(buttons, (short) lx, (short) ly, (short) rx, (short) ry, (byte) 0, (byte) 0, now);
    }

    @Test
    public void leftStickMapsToWasd() {
        state(0, 0, FULL, 0, 0, 0);
        assertEquals(List.of("down W"), out.events);

        // Up-right diagonal adds D
        state(0, 23170, 23170, 0, 0, 1);
        assertEquals(List.of("down W", "down D"), out.events);

        // Hard left releases W and D, presses A
        state(0, -FULL, 0, 0, 0, 2);
        assertEquals(List.of("down W", "down D", "up W", "up D", "down A"), out.events);

        // Back to center
        state(0, 0, 0, 0, 0, 3);
        assertEquals("up A", out.events.get(out.events.size() - 1));
    }

    @Test
    public void smallStickMovementInsideDeadzoneDoesNothing() {
        state(0, 0, (int) (FULL * 0.2), 0, 0, 0);
        assertTrue(out.events.isEmpty());
    }

    @Test
    public void layerShiftSelectsShiftedBinding() {
        state(A, 0, 0, 0, 0, 0);
        state(0, 0, 0, 0, 0, 1);
        assertEquals(List.of("down 1", "up 1"), out.events);
        out.events.clear();

        // LB and A in the same report: A must use the shifted layer
        state(LB | A, 0, 0, 0, 0, 2);
        assertEquals(List.of("down 6"), out.events);

        // Releasing LB first must still release 6, not 1
        state(A, 0, 0, 0, 0, 3);
        state(0, 0, 0, 0, 0, 4);
        assertEquals(List.of("down 6", "up 6"), out.events);
    }

    @Test
    public void shiftedNoneFallsBackToBase() {
        // L3 has no shifted binding in the GW2 defaults
        state(LB | 0x0040, 0, 0, 0, 0, 0);
        assertEquals(List.of("down Tab"), out.events);
    }

    @Test
    public void triggersUseThreshold() {
        mapper.update(0, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 40, (byte) 0, 0);
        assertTrue(out.events.isEmpty());
        mapper.update(0, (short) 0, (short) 0, (short) 0, (short) 0, (byte) 200, (byte) 255, 1);
        assertEquals(List.of("down V", "down Space"), out.events);
    }

    @Test
    public void rightStickHoldsRightMouseAndMovesCamera() {
        state(0, 0, 0, FULL, 0, 0);
        assertTrue(mapper.needsTick());
        mapper.tick(0);
        assertEquals(List.of("mdown " + KbmAction.MOUSE_RIGHT), out.events);

        for (long t = 10; t <= 1000; t += 10) {
            mapper.tick(t);
        }
        // Full deflection for one second ~= cameraSpeed pixels to the right
        assertEquals(config.cameraSpeed, out.mouseX, config.cameraSpeed * 0.02);
        assertEquals(0, out.mouseY);

        // Recenter: RMB is held for the release delay, then released
        state(0, 0, 0, 0, 0, 1000);
        mapper.tick(1010);
        assertEquals(1, out.events.size());
        mapper.tick(1000 + config.rightMouseReleaseDelayMs + 10);
        assertEquals("mup " + KbmAction.MOUSE_RIGHT, out.events.get(1));
        assertFalse(mapper.needsTick());
    }

    @Test
    public void stickUpMovesMouseUp() {
        state(0, 0, 0, 0, FULL, 0);
        mapper.tick(0);
        mapper.tick(50);
        assertTrue(out.mouseY < 0);

        config.invertCameraY = true;
        out.mouseY = 0;
        mapper.tick(100);
        assertTrue(out.mouseY > 0);
    }

    @Test
    public void toggleCameraModeStopsHoldingRightMouse() {
        int guide = 0x0400;
        state(guide, 0, 0, 0, 0, 0);
        state(0, 0, 0, FULL, 0, 1);
        mapper.tick(1);
        mapper.tick(20);
        assertFalse(out.events.contains("mdown " + KbmAction.MOUSE_RIGHT));
        assertTrue(out.mouseX > 0);
    }

    @Test
    public void cursorModeUsesCursorSpeed() {
        config.cameraSpeed = 2000;
        config.cursorSpeed = 500;
        state(0x0400, 0, 0, 0, 0, 0); // Guide: switch to cursor mode
        state(0, 0, 0, FULL, 0, 1);
        for (long t = 1; t <= 1001; t += 10) {
            mapper.tick(t);
        }
        assertEquals(config.cursorSpeed, out.mouseX, config.cursorSpeed * 0.03);
    }

    @Test
    public void onScreenRightClickDoesNotCancelCameraHold() {
        state(0, 0, 0, FULL, 0, 0);
        mapper.tick(0);
        mapper.setMouseButton(KbmAction.MOUSE_RIGHT, true);
        mapper.setMouseButton(KbmAction.MOUSE_RIGHT, false);
        // Camera still holds the button, so nothing was released
        assertEquals(List.of("mdown " + KbmAction.MOUSE_RIGHT), out.events);

        mapper.setMouseButton(KbmAction.MOUSE_LEFT, true);
        mapper.setMouseButton(KbmAction.MOUSE_LEFT, false);
        assertEquals(List.of("mdown " + KbmAction.MOUSE_RIGHT,
                "mdown " + KbmAction.MOUSE_LEFT, "mup " + KbmAction.MOUSE_LEFT), out.events);
    }

    @Test
    public void barButtonTogglesSkillBar() {
        final List<Boolean> layerEvents = new ArrayList<>();
        out = new RecordingOutput() {
            @Override public void layerChanged(boolean shifted) { layerEvents.add(shifted); }
        };
        mapper = new GamepadKbmMapper(out, config, KbmBindings.bladeswornDefaults());
        int bar = KbmBindings.Input.OSC_BAR.buttonFlag;

        // Bar 1: A..RB send 1..5
        state(A, 0, 0, 0, 0, 0);
        state(RB, 0, 0, 0, 0, 1);
        state(0, 0, 0, 0, 0, 2);
        assertEquals(List.of("down 1", "up 1", "down 5", "up 5"), out.events);
        out.events.clear();

        // Tap BAR: stays on bar 2 after release, A..RB send 6..0
        state(bar, 0, 0, 0, 0, 3);
        state(0, 0, 0, 0, 0, 4);
        assertTrue(mapper.isShiftedLayerActive());
        state(A, 0, 0, 0, 0, 5);
        state(RB, 0, 0, 0, 0, 6);
        state(0, 0, 0, 0, 0, 7);
        assertEquals(List.of("down 6", "up 6", "down 0", "up 0"), out.events);
        out.events.clear();

        // Holding a skill across a swap releases the key it pressed
        state(A, 0, 0, 0, 0, 8);
        state(A | bar, 0, 0, 0, 0, 9);
        state(0, 0, 0, 0, 0, 10);
        assertEquals(List.of("down 6", "up 6"), out.events);
        assertFalse(mapper.isShiftedLayerActive());
        assertEquals(List.of(true, false), layerEvents);
    }

    @Test
    public void extraButtonSendsDragonTrigger() {
        mapper = new GamepadKbmMapper(out, config, KbmBindings.bladeswornDefaults());
        int extra = KbmBindings.Input.OSC_EXTRA.buttonFlag;
        state(extra, 0, 0, 0, 0, 0);
        state(0, 0, 0, 0, 0, 1);
        assertEquals(List.of("down F1", "up F1"), out.events);
    }

    @Test
    public void zoomButtonsScrollAndRepeatWhileHeld() {
        mapper = new GamepadKbmMapper(out, config, KbmBindings.bladeswornDefaults());
        int zoomIn = KbmBindings.Input.OSC_ZOOM_IN.buttonFlag;
        int zoomOut = KbmBindings.Input.OSC_ZOOM_OUT.buttonFlag;

        state(zoomIn, 0, 0, 0, 0, 0);
        assertTrue(mapper.needsTick());
        mapper.tick(config.wheelRepeatDelayMs);
        state(0, 0, 0, 0, 0, config.wheelRepeatDelayMs + 1);
        assertEquals(List.of("scroll 1", "scroll 1"), out.events);

        out.events.clear();
        state(zoomOut, 0, 0, 0, 0, 1000);
        state(0, 0, 0, 0, 0, 1001);
        assertEquals(List.of("scroll -1"), out.events);
        assertFalse(mapper.needsTick());
    }

    @Test
    public void sharedKeyIsReleasedOnlyByLastHolder() {
        KbmBindings bindings = KbmBindings.guildWars2Defaults();
        bindings.setBase(KbmBindings.Input.RB, KbmAction.key('W'));
        mapper = new GamepadKbmMapper(out, config, bindings);

        state(RB, 0, FULL, 0, 0, 0);
        state(RB, 0, 0, 0, 0, 1);
        assertEquals(List.of("down W"), out.events);
        state(0, 0, 0, 0, 0, 2);
        assertEquals(List.of("down W", "up W"), out.events);
    }

    @Test
    public void wheelRepeatsWhileHeld() {
        int dpadLeft = 0x0004; // shifted: wheel up
        state(LB | dpadLeft, 0, 0, 0, 0, 0);
        assertEquals(List.of("scroll 1"), out.events);
        mapper.tick(config.wheelRepeatDelayMs);
        mapper.tick(config.wheelRepeatDelayMs + config.wheelRepeatIntervalMs);
        assertEquals(3, out.events.size());
        state(LB, 0, 0, 0, 0, 1000);
        assertFalse(mapper.needsTick());
    }

    @Test
    public void releaseAllLeavesNothingHeld() {
        state(A | RB, 0, FULL, FULL, 0, 0);
        mapper.tick(0);
        mapper.releaseAll();

        Map<String, Integer> balance = new HashMap<>();
        for (String event : out.events) {
            String[] parts = event.split(" ", 2);
            int delta = parts[0].endsWith("down") ? 1 : -1;
            balance.merge(parts[1], delta, Integer::sum);
        }
        for (int value : balance.values()) {
            assertEquals(0, value);
        }
        assertFalse(mapper.needsTick());
    }

    @Test
    public void bindingsRoundTripThroughPreferences() {
        KbmBindings bindings = KbmBindings.guildWars2Defaults();
        bindings.setBase(KbmBindings.Input.A, KbmAction.mouse(KbmAction.MOUSE_LEFT));
        bindings.setShifted(KbmBindings.Input.Y, KbmAction.WHEEL_DOWN);

        KbmBindings loaded = KbmBindings.fromMap(bindings.toMap());
        for (KbmBindings.Input input : KbmBindings.Input.values()) {
            assertEquals(bindings.getBase(input), loaded.getBase(input));
            assertEquals(bindings.getShifted(input), loaded.getShifted(input));
        }
    }

    @Test
    public void malformedPreferencesFallBackToDefaults() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put(KbmBindings.PREF_PREFIX_BASE + "A", "key:notanumber");
        prefs.put(KbmBindings.PREF_PREFIX_BASE + "B", 42);
        KbmBindings loaded = KbmBindings.fromMap(prefs);
        assertEquals(KbmAction.key('1'), loaded.getBase(KbmBindings.Input.A));
        assertEquals(KbmAction.key('2'), loaded.getBase(KbmBindings.Input.B));
    }

    @Test
    public void everySelectableActionRoundTrips() {
        for (KbmAction action : KbmAction.all()) {
            assertEquals(action, KbmAction.decode(action.encode()));
        }
    }
}
