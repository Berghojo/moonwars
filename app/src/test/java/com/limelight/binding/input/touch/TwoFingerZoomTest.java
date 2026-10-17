package com.limelight.binding.input.touch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TwoFingerZoomTest {
    private static final float STEP = 40;

    private int pinch(TwoFingerZoom zoom, float startGap, float endGap) {
        // Fingers on a horizontal line, moving apart or together symmetrically
        zoom.begin(500 - startGap / 2, 300);
        zoom.update(1, 500 + startGap / 2, 300, STEP);
        int total = 0;
        for (int i = 1; i <= 10; i++) {
            float gap = startGap + (endGap - startGap) * i / 10;
            total += zoom.update(0, 500 - gap / 2, 300, STEP);
            total += zoom.update(1, 500 + gap / 2, 300, STEP);
        }
        return total;
    }

    @Test
    public void spreadingFingersZoomsIn() {
        assertEquals(5, pinch(new TwoFingerZoom(), 100, 300));
    }

    @Test
    public void closingFingersZoomsOut() {
        assertEquals(-5, pinch(new TwoFingerZoom(), 300, 100));
    }

    @Test
    public void draggingBothFingersUpZoomsIn() {
        TwoFingerZoom zoom = new TwoFingerZoom();
        zoom.begin(400, 500);
        zoom.update(1, 600, 500, STEP);
        int total = 0;
        for (int y = 490; y >= 300; y -= 10) {
            total += zoom.update(0, 400, y, STEP);
            total += zoom.update(1, 600, y, STEP);
        }
        assertEquals(5, total);
    }

    @Test
    public void oneFingerAloneDoesNotZoom() {
        TwoFingerZoom zoom = new TwoFingerZoom();
        zoom.begin(400, 500);
        assertEquals(0, zoom.update(0, 400, 100, STEP));
        zoom.update(1, 600, 500, STEP);
        zoom.removePoint(1);
        assertEquals(0, zoom.update(0, 400, 900, STEP));
        assertTrue(zoom.isActive());
    }
}
