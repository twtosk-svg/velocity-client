package com.velocity.gui.framework;

/**
 * DPI scale matching C++ SCALE() macro (stored_dpi / 100).
 */
public final class UiScale {
    private UiScale() {}

    public static int storedDpi = 100;

    public static float dpi() {
        return storedDpi / 100f;
    }

    public static float s(float value) {
        return value * dpi();
    }

    public static int si(int value) {
        return Math.round(value * dpi());
    }
}
