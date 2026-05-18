package com.velocity.gui.framework;

import imgui.ImGui;

/**
 * Port of c_gui::easing from ImguiMenu C++ framework/headers/functions.h
 */
public final class UiEasing {
    private UiEasing() {}

    public static final int STATIC = 0;
    public static final int DYNAMIC = 1;

    public static float fixedSpeed(float speed) {
        float fps = ImGui.getIO().getFramerate();
        if (fps < 1f) fps = 60f;
        return speed / fps;
    }

    public static float ease(float value, float target, float speed, int type) {
        if (type == STATIC) {
            float step = fixedSpeed(speed);
            if (value < target) {
                value += step;
                if (value > target) value = target;
            } else if (value > target) {
                value -= step;
                if (value < target) value = target;
            }
            return value;
        } else {
            return lerp(value, target, fixedSpeed(speed));
        }
    }

    public static float[] easeColor(float[] value, float[] target, float speed) {
        float[] out = new float[4];
        for (int i = 0; i < 4; i++) {
            out[i] = lerp(value[i], target[i], fixedSpeed(speed));
        }
        return out;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
