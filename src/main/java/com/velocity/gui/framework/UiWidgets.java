package com.velocity.gui.framework;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom widgets ported from ImguiMenu C++ framework/widgets/
 */
public final class UiWidgets {
    private UiWidgets() {}

    private static final Map<Integer, CheckboxAnim> CHECKBOX_ANIMS = new HashMap<>();
    private static final Map<Integer, SliderAnim> SLIDER_ANIMS = new HashMap<>();

    private static class CheckboxAnim {
        float alpha;
        float offset;
        float[] textColor = copy(UiColors.TEXT_INACTIVE);
        float[] circleColor = copy(UiColors.WIDGETS_INACTIVE);
    }

    private static class SliderAnim {
        float offset;
        float alpha;
    }

    private static float[] copy(float[] c) {
        return new float[] { c[0], c[1], c[2], c[3] };
    }

    /** ImGui ID suffix after ## — not shown in UI. */
    public static String displayLabel(String label) {
        int hash = label.indexOf("##");
        return hash >= 0 ? label.substring(0, hash) : label;
    }

    private static boolean onSameLine() {
        return ImGui.getCursorPosX() > ImGui.getStyle().getWindowPaddingX() + 2f;
    }

    public static boolean checkbox(String label, ImBoolean value) {
        return checkbox(label, value, false);
    }

    public static boolean checkbox(String label, ImBoolean value, boolean disabled) {
        float rowH = UiScale.s(32f);
        float rectW = UiScale.s(30f);
        float indent = UiScale.s(6f);
        float circleR = UiScale.s(7f);
        float circlePad = UiScale.s(3f);
        float rounding = UiScale.s(100f);

        String shown = displayLabel(label);
        boolean sameLine = onSameLine();
        ImGui.pushID(label);
        float availW = ImGui.getContentRegionAvailX();
        float itemW = availW;
        if (sameLine) {
            ImVec2 textSize = new ImVec2();
            ImGui.calcTextSize(textSize, shown);
            itemW = Math.min(availW, textSize.x + rectW + UiScale.s(20f));
        }

        ImVec2 cursor = ImGui.getCursorScreenPos();
        float rowMinX = cursor.x;
        float rowMinY = cursor.y;
        float rowMaxX = rowMinX + itemW;
        float rowMaxY = rowMinY + rowH;

        ImGui.invisibleButton("##cb", itemW, rowH);
        boolean hovered = ImGui.isItemHovered() && !disabled;
        boolean clicked = hovered && ImGui.isMouseClicked(0) && !disabled;
        if (clicked) {
            value.set(!value.get());
        }

        int id = ImGui.getID("##cb_anim");
        CheckboxAnim anim = CHECKBOX_ANIMS.computeIfAbsent(id, k -> new CheckboxAnim());

        boolean on = value.get();
        anim.alpha = UiEasing.ease(anim.alpha, on ? 1f : 0f, 8f, UiEasing.STATIC);
        float maxOffset = rectW - circlePad * 2 - circleR * 2;
        anim.offset = UiEasing.ease(anim.offset, on ? maxOffset : 0f, 16f, UiEasing.DYNAMIC);
        anim.textColor = UiEasing.easeColor(anim.textColor, on ? UiColors.TEXT : UiColors.TEXT_INACTIVE, 16f);
        anim.circleColor = UiEasing.easeColor(anim.circleColor, on ? UiColors.accent() : UiColors.WIDGETS_INACTIVE, 16f);

        float rectMaxX = rowMaxX;
        float rectMinX = rectMaxX - rectW;
        float rectMinY = rowMinY + indent;
        float rectMaxY = rowMaxY - indent;

        ImDrawList dl = ImGui.getWindowDrawList();
        UiDraw.rectFilled(dl, rectMinX, rectMinY, rectMaxX, rectMaxY,
                UiColors.getClr(UiColors.WIDGETS_BACKGROUND, 1f), rounding);
        UiDraw.rectFilled(dl, rectMinX, rectMinY, rectMaxX, rectMaxY,
                UiColors.getClr(UiColors.accent()[0], UiColors.accent()[1], UiColors.accent()[2], anim.alpha * 0.1f), rounding);

        float cy = (rectMinY + rectMaxY) * 0.5f;
        float cx = rectMinX + circlePad + circleR + anim.offset;
        UiDraw.circleFilled(dl, cx, cy, circleR, UiColors.getClr(anim.circleColor, 1f));

        dl.addText(rowMinX, rowMinY + (rowH - ImGui.getFontSize()) * 0.5f,
                UiColors.getClr(anim.textColor, disabled ? 0.5f : 1f), shown);

        if (!sameLine) {
            ImGui.spacing();
        }
        ImGui.popID();
        return clicked;
    }

    public static boolean sliderFloat(String label, float[] value, float min, float max, String format) {
        return slider(label, value[0], min, max, format, v -> value[0] = v);
    }

    public static boolean sliderInt(String label, int[] value, int min, int max) {
        return sliderInt(label, value, min, max, "%d");
    }

    public static boolean sliderInt(String label, int[] value, int min, int max, String format) {
        float[] f = { value[0] };
        boolean changed = slider(label, f[0], min, max, format, v -> value[0] = Math.round(v));
        return changed;
    }

    private interface FloatSetter {
        void set(float v);
    }

    private static boolean slider(String label, float current, float min, float max, String format, FloatSetter setter) {
        float rowH = UiScale.s(36f);
        float trackW = UiScale.s(160f);
        float trackH = UiScale.s(6f);
        float indent = UiScale.s(17f);
        float grabR = UiScale.s(6f);
        float rounding = UiScale.s(100f);

        String shown = displayLabel(label);
        ImGui.pushID(label);
        float availW = ImGui.getContentRegionAvailX();
        ImVec2 cursor = ImGui.getCursorScreenPos();
        float rowMinX = cursor.x;
        float rowMinY = cursor.y;

        ImGui.invisibleButton("##sl", availW, rowH);
        boolean hovered = ImGui.isItemHovered();
        boolean held = hovered && ImGui.isMouseDown(0);

        float trackMaxX = rowMinX + availW;
        float trackMinX = trackMaxX - trackW;
        float trackMinY = rowMinY + indent;
        float trackMaxY = trackMinY + trackH;

        float padding = 0f;
        float grabWidth = grabR * 2;
        float widthWithGrab = trackW - grabWidth - padding * 2;
        float t = (current - min) / (max - min);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        float grabOffset = padding + grabR + t * widthWithGrab;

        boolean changed = false;
        if (held) {
            float mx = ImGui.getIO().getMousePosX();
            float localT = (mx - (trackMinX + padding + grabR)) / widthWithGrab;
            if (localT < 0f) localT = 0f;
            if (localT > 1f) localT = 1f;
            float newVal = min + localT * (max - min);
            if (newVal != current) {
                setter.set(newVal);
                current = newVal;
                changed = true;
                t = localT;
                grabOffset = padding + grabR + t * widthWithGrab;
            }
        }

        int id = ImGui.getID("##sl_anim");
        SliderAnim anim = SLIDER_ANIMS.computeIfAbsent(id, k -> new SliderAnim());
        anim.offset = UiEasing.ease(anim.offset, grabOffset, 20f, UiEasing.DYNAMIC);
        anim.alpha = UiEasing.ease(anim.alpha, current > min ? 1f : 0f, 8f, UiEasing.STATIC);

        ImDrawList dl = ImGui.getWindowDrawList();
        UiDraw.rectFilled(dl, trackMinX, trackMinY, trackMaxX, trackMaxY,
                UiColors.getClr(UiColors.WIDGETS_BACKGROUND, 1f), rounding);
        float fillX = trackMinX + grabR + anim.offset;
        UiDraw.rectFilled(dl, trackMinX, trackMinY, fillX, trackMaxY,
                UiColors.getClr(UiColors.accent(), 1f), rounding);
        UiDraw.circleFilled(dl, trackMinX + anim.offset, (trackMinY + trackMaxY) * 0.5f, grabR,
                UiColors.getClr(UiColors.TEXT, 1f));

        dl.addText(rowMinX, rowMinY + (rowH - ImGui.getFontSize()) * 0.5f,
                UiColors.getClr(UiColors.TEXT_INACTIVE, 1f), shown);

        String valueText;
        if (format != null && format.contains("%.0f")) {
            valueText = String.format("%.0f", current);
        } else if (format != null && format.contains("%.1f")) {
            valueText = String.format("%.1f", current);
        } else if ("%d%%".equals(format)) {
            valueText = String.format("%d%%", (int) current);
        } else if (format != null && format.contains("%d")) {
            valueText = String.format("%d", (int) current);
        } else {
            valueText = String.format("%.2f", current);
        }

        ImVec2 valSize = new ImVec2();
        ImGui.calcTextSize(valSize, valueText);
        float valX = trackMinX - valSize.x - UiScale.s(10f);
        float valY = rowMinY + (rowH - valSize.y) * 0.5f;
        dl.addText(valX, valY, UiColors.getClr(UiColors.TEXT, 1f), valueText);

        ImGui.spacing();
        ImGui.popID();
        return changed;
    }

    public static boolean combo(String label, ImInt index, String[] items) {
        ImGui.pushID(label);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, UiColors.WIDGETS_BACKGROUND[0], UiColors.WIDGETS_BACKGROUND[1],
                UiColors.WIDGETS_BACKGROUND[2], 1f);
        boolean changed = ImGui.combo(label, index, items);
        ImGui.popStyleColor();
        ImGui.popID();
        return changed;
    }

    public static void pushItemWidth(float w) {
        ImGui.pushItemWidth(w * UiScale.dpi());
    }

    public static void popItemWidth() {
        ImGui.popItemWidth();
    }
}
