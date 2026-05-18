package com.velocity.gui.framework;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;

/**
 * Minimal draw helpers ported from ImguiMenu C++ framework/helpers/draw.cpp
 */
public final class UiDraw {
    private UiDraw() {}

    public static void rectFilled(ImDrawList dl, float x0, float y0, float x1, float y1, int col, float rounding) {
        dl.addRectFilled(x0, y0, x1, y1, col, rounding);
    }

    public static void rect(ImDrawList dl, float x0, float y0, float x1, float y1, int col, float rounding, float thickness) {
        dl.addRect(x0, y0, x1, y1, col, rounding, 0, thickness);
    }

    public static void line(ImDrawList dl, float x0, float y0, float x1, float y1, int col, float thickness) {
        dl.addLine(x0, y0, x1, y1, col, thickness);
    }

    public static void circleFilled(ImDrawList dl, float cx, float cy, float radius, int col) {
        dl.addCircleFilled(cx, cy, radius, col, 24);
    }

    public static void separator() {
        ImDrawList dl = ImGui.getWindowDrawList();
        float y = ImGui.getCursorScreenPosY() + UiScale.s(4f);
        float x0 = ImGui.getWindowPosX() + UiScale.s(8f);
        float x1 = ImGui.getWindowPosX() + ImGui.getWindowWidth() - UiScale.s(8f);
        line(dl, x0, y, x1, y, UiColors.getClr(UiColors.WINDOW_LINE, 0.8f), UiScale.s(1f));
        ImGui.dummy(0, UiScale.s(8f));
    }

    public static void textSection(String label) {
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, UiColors.TEXT_INACTIVE[0], UiColors.TEXT_INACTIVE[1],
                UiColors.TEXT_INACTIVE[2], 1f);
        ImGui.text(label);
        ImGui.popStyleColor();
        ImGui.spacing();
    }
}
