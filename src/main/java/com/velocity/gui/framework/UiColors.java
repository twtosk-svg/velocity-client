package com.velocity.gui.framework;

import imgui.ImGui;

/**
 * Palette ported from ImguiMenu C++ framework/settings/colors.h
 */
public final class UiColors {
    private UiColors() {}

    public static float accentR = 103f / 255f;
    public static float accentG = 100f / 255f;
    public static float accentB = 255f / 255f;

    public static final float[] ACCENT = { accentR, accentG, accentB, 1f };

    public static final float[] WINDOW_BG = { 25f / 255f, 25f / 255f, 32f / 255f, 1f };
    public static final float[] WINDOW_BAR = { 17f / 255f, 17f / 255f, 22f / 255f, 1f };
    public static final float[] WINDOW_CHILD = { 20f / 255f, 20f / 255f, 27f / 255f, 1f };
    public static final float[] WINDOW_RECT = { 25f / 255f, 25f / 255f, 33f / 255f, 1f };
    public static final float[] WINDOW_LINE = { 54f / 255f, 55f / 255f, 66f / 255f, 1f };

    public static final float[] TEXT = { 1f, 1f, 1f, 1f };
    public static final float[] TEXT_INACTIVE = { 86f / 255f, 85f / 255f, 106f / 255f, 1f };
    public static final float[] SECTION_ON = { 77f / 255f, 79f / 255f, 95f / 255f, 1f };
    public static final float[] WIDGETS_ACTIVE = { 35f / 255f, 35f / 255f, 46f / 255f, 1f };
    public static final float[] WIDGETS_INACTIVE = { 47f / 255f, 47f / 255f, 63f / 255f, 1f };
    public static final float[] WIDGETS_BACKGROUND = { 28f / 255f, 28f / 255f, 39f / 255f, 1f };

    public static float[] accent() {
        return new float[] { accentR, accentG, accentB, 1f };
    }

    public static int getClr(float[] rgba, float alpha) {
        float a = rgba[3] * alpha * ImGui.getStyle().getAlpha();
        return ImGui.colorConvertFloat4ToU32(rgba[0], rgba[1], rgba[2], a);
    }

    public static int getClr(float r, float g, float b, float alpha) {
        float a = alpha * ImGui.getStyle().getAlpha();
        return ImGui.colorConvertFloat4ToU32(r, g, b, a);
    }

    public static void applyImGuiStyle() {
        float[] accent = accent();
        imgui.ImGuiStyle style = ImGui.getStyle();

        style.setWindowRounding(UiScale.s(16f));
        style.setFrameRounding(UiScale.s(8f));
        style.setGrabRounding(UiScale.s(100f));
        style.setTabRounding(UiScale.s(6f));
        style.setWindowBorderSize(0f);
        style.setFramePadding(UiScale.s(10f), UiScale.s(8f));
        style.setItemSpacing(UiScale.s(8f), UiScale.s(4f));
        style.setWindowPadding(UiScale.s(14f), UiScale.s(12f));

        style.setColor(imgui.flag.ImGuiCol.WindowBg, WINDOW_BG[0], WINDOW_BG[1], WINDOW_BG[2], 0f);
        style.setColor(imgui.flag.ImGuiCol.ChildBg, WINDOW_CHILD[0], WINDOW_CHILD[1], WINDOW_CHILD[2], 0.95f);
        style.setColor(imgui.flag.ImGuiCol.PopupBg, WINDOW_BG[0], WINDOW_BG[1], WINDOW_BG[2], 0.98f);
        style.setColor(imgui.flag.ImGuiCol.Border, WINDOW_LINE[0], WINDOW_LINE[1], WINDOW_LINE[2], 0.5f);
        style.setColor(imgui.flag.ImGuiCol.Text, TEXT[0], TEXT[1], TEXT[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.TextDisabled, TEXT_INACTIVE[0], TEXT_INACTIVE[1], TEXT_INACTIVE[2], 1f);

        style.setColor(imgui.flag.ImGuiCol.Tab, WIDGETS_BACKGROUND[0], WIDGETS_BACKGROUND[1], WIDGETS_BACKGROUND[2], 0.85f);
        style.setColor(imgui.flag.ImGuiCol.TabHovered, SECTION_ON[0], SECTION_ON[1], SECTION_ON[2], 0.9f);
        style.setColor(imgui.flag.ImGuiCol.TabActive, WIDGETS_ACTIVE[0], WIDGETS_ACTIVE[1], WIDGETS_ACTIVE[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.TabUnfocused, WIDGETS_BACKGROUND[0], WIDGETS_BACKGROUND[1], WIDGETS_BACKGROUND[2], 0.7f);
        style.setColor(imgui.flag.ImGuiCol.TabUnfocusedActive, WIDGETS_ACTIVE[0], WIDGETS_ACTIVE[1], WIDGETS_ACTIVE[2], 0.85f);

        style.setColor(imgui.flag.ImGuiCol.FrameBg, WIDGETS_BACKGROUND[0], WIDGETS_BACKGROUND[1], WIDGETS_BACKGROUND[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.FrameBgHovered, WIDGETS_ACTIVE[0], WIDGETS_ACTIVE[1], WIDGETS_ACTIVE[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.FrameBgActive, WIDGETS_ACTIVE[0], WIDGETS_ACTIVE[1], WIDGETS_ACTIVE[2], 1f);

        style.setColor(imgui.flag.ImGuiCol.CheckMark, accent[0], accent[1], accent[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.SliderGrab, accent[0], accent[1], accent[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.SliderGrabActive, accent[0], accent[1], accent[2], 1f);

        style.setColor(imgui.flag.ImGuiCol.Button, WIDGETS_BACKGROUND[0], WIDGETS_BACKGROUND[1], WIDGETS_BACKGROUND[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.ButtonHovered, SECTION_ON[0], SECTION_ON[1], SECTION_ON[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.ButtonActive, accent[0], accent[1], accent[2], 0.9f);

        style.setColor(imgui.flag.ImGuiCol.Header, WIDGETS_ACTIVE[0], WIDGETS_ACTIVE[1], WIDGETS_ACTIVE[2], 0.8f);
        style.setColor(imgui.flag.ImGuiCol.HeaderHovered, SECTION_ON[0], SECTION_ON[1], SECTION_ON[2], 0.9f);
        style.setColor(imgui.flag.ImGuiCol.HeaderActive, accent[0], accent[1], accent[2], 0.85f);

        style.setColor(imgui.flag.ImGuiCol.TitleBg, WINDOW_BAR[0], WINDOW_BAR[1], WINDOW_BAR[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.TitleBgActive, WINDOW_BG[0], WINDOW_BG[1], WINDOW_BG[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.TitleBgCollapsed, WINDOW_BAR[0], WINDOW_BAR[1], WINDOW_BAR[2], 0.8f);

        style.setColor(imgui.flag.ImGuiCol.ScrollbarBg, WINDOW_BAR[0], WINDOW_BAR[1], WINDOW_BAR[2], 0.5f);
        style.setColor(imgui.flag.ImGuiCol.ScrollbarGrab, WIDGETS_INACTIVE[0], WIDGETS_INACTIVE[1], WIDGETS_INACTIVE[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.ScrollbarGrabHovered, SECTION_ON[0], SECTION_ON[1], SECTION_ON[2], 1f);
        style.setColor(imgui.flag.ImGuiCol.ScrollbarGrabActive, accent[0], accent[1], accent[2], 1f);

        style.setColor(imgui.flag.ImGuiCol.Separator, WINDOW_LINE[0], WINDOW_LINE[1], WINDOW_LINE[2], 0.6f);
    }
}
