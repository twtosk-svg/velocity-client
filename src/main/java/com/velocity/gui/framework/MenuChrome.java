package com.velocity.gui.framework;

import com.velocity.config.UiSettings;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;

/**
 * Custom menu window chrome and animated tab indicator (top tabs preserved).
 */
public final class MenuChrome {
    private MenuChrome() {}

    private static final float DEFAULT_W = 650f;
    private static final float DEFAULT_H = 680f;
    private static final float MIN_W = 480f;
    private static final float MIN_H = 420f;

    private static float tabLineX = 0f;
    private static float tabLineW = 0f;
    private static float tabLineTargetX = 0f;
    private static float tabLineTargetW = 0f;
    private static boolean tabBarActive = false;

    public static void beginMenuWindow(float menuAlpha, float menuScale) {
        ImGui.setNextWindowSizeConstraints(MIN_W, MIN_H, 2000f, 2000f);
        ImGui.setNextWindowSize(DEFAULT_W * menuScale, DEFAULT_H * menuScale, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowBgAlpha(0f);

        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.Alpha, menuAlpha);
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowRounding, UiScale.s(16f));
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowPadding, UiScale.s(14f), UiScale.s(12f));

        ImGui.begin("##velocity_menu", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.AlwaysVerticalScrollbar);
    }

    public static void drawWindowBackground(float menuAlpha, int blurTextureReady) {
        ImVec2 pos = ImGui.getWindowPos();
        float w = ImGui.getWindowWidth();
        float h = ImGui.getWindowHeight();
        ImDrawList dl = ImGui.getWindowDrawList();

        float bgStrength = UiSettings.menuBackgroundOpacity * menuAlpha;

        if (blurTextureReady != 0) {
            com.velocity.gui.GlBackgroundBlur.drawBehindMenu(dl, pos.x, pos.y, w, h, menuAlpha);
            float tint = bgStrength * 0.45f;
            UiDraw.rectFilled(dl, pos.x, pos.y, pos.x + w, pos.y + h,
                    UiColors.getClr(UiColors.WINDOW_BG[0], UiColors.WINDOW_BG[1], UiColors.WINDOW_BG[2], tint),
                    UiScale.s(16f));
        } else {
            UiDraw.rectFilled(dl, pos.x, pos.y, pos.x + w, pos.y + h,
                    UiColors.getClr(UiColors.WINDOW_BG[0], UiColors.WINDOW_BG[1], UiColors.WINDOW_BG[2], bgStrength),
                    UiScale.s(16f));
        }

        UiDraw.rect(dl, pos.x, pos.y, pos.x + w, pos.y + h,
                UiColors.getClr(UiColors.WINDOW_LINE, menuAlpha * 0.6f), UiScale.s(16f), UiScale.s(1f));

        float headerH = UiScale.s(36f);
        float headerY = pos.y + UiScale.s(10f);
        ImVec2 titleSize = new ImVec2();
        ImGui.calcTextSize(titleSize, "velocity");
        dl.addText(pos.x + UiScale.s(16f), headerY,
                UiColors.getClr(UiColors.accent(), menuAlpha), "velocity");
        dl.addText(pos.x + UiScale.s(16f) + titleSize.x + UiScale.s(8f), headerY,
                UiColors.getClr(UiColors.TEXT_INACTIVE, menuAlpha), "1.4");

        float lineY = pos.y + headerH;
        UiDraw.line(dl, pos.x + UiScale.s(12f), lineY, pos.x + w - UiScale.s(12f), lineY,
                UiColors.getClr(UiColors.accent(), menuAlpha * 0.85f), UiScale.s(1.5f));

        ImGui.dummy(0, headerH + UiScale.s(6f));
    }

    public static void beforeTabBar() {
        tabBarActive = true;
    }

    public static void afterTabBar() {
        if (!tabBarActive) return;
        tabLineX = UiEasing.ease(tabLineX, tabLineTargetX, 14f, UiEasing.DYNAMIC);
        tabLineW = UiEasing.ease(tabLineW, tabLineTargetW, 14f, UiEasing.DYNAMIC);

        ImDrawList dl = ImGui.getWindowDrawList();
        float y = ImGui.getItemRectMaxY() - UiScale.s(2f);
        UiDraw.line(dl, tabLineX, y, tabLineX + tabLineW, y,
                UiColors.getClr(UiColors.accent(), 1f), UiScale.s(2f));
        tabBarActive = false;
    }

    public static void onTabSelected() {
        if (!tabBarActive) return;
        ImVec2 min = ImGui.getItemRectMin();
        ImVec2 max = ImGui.getItemRectMax();
        tabLineTargetX = min.x;
        tabLineTargetW = max.x - min.x;
        if (tabLineW < 1f) {
            tabLineX = tabLineTargetX;
            tabLineW = tabLineTargetW;
        }
    }

    public static void endMenuWindow() {
        ImGui.end();
        ImGui.popStyleVar(3);
    }
}
