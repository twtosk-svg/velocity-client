package com.velocity.gui.framework;

import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImGuiIO;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads Inter / icon fonts from resources or Windows fallbacks.
 */
public final class UiFonts {
    private UiFonts() {}

    /** Must persist for the lifetime of the font atlas. */
    private static final short[] ICON_GLYPH_RANGES = { 0x0020, (short) 0x00FF, 0 };

    public static ImFont mainFont;
    public static ImFont iconFont;

    public static void load(ImGuiIO io) {
        try {
            float size = UiScale.s(16f);
            byte[] inter = loadBytes("fonts/inter_medium.ttf");
            if (inter != null && inter.length > 0) {
                mainFont = io.getFonts().addFontFromMemoryTTF(inter, size);
            } else {
                Path winInter = Path.of("C:\\Windows\\Fonts", "Inter-Medium.ttf");
                Path segoe = Path.of("C:\\Windows\\Fonts", "segoeui.ttf");
                if (Files.exists(winInter)) {
                    mainFont = io.getFonts().addFontFromFileTTF(winInter.toString(), size);
                } else if (Files.exists(segoe)) {
                    mainFont = io.getFonts().addFontFromFileTTF(segoe.toString(), size);
                } else {
                    io.getFonts().addFontDefault();
                }
            }

            byte[] icons = loadBytes("fonts/icons.ttf");
            if (icons != null && icons.length > 0 && mainFont != null) {
                ImFontConfig iconCfg = new ImFontConfig();
                iconCfg.setMergeMode(true);
                iconFont = io.getFonts().addFontFromMemoryTTF(icons, UiScale.s(18f), iconCfg, ICON_GLYPH_RANGES);
            }
        } catch (Exception e) {
            System.err.println("[Velocity] UiFonts.load failed, using default font: " + e.getMessage());
            e.printStackTrace();
            try {
                io.getFonts().clear();
                io.getFonts().addFontDefault();
            } catch (Exception ignored) {
            }
        }
    }

    private static byte[] loadBytes(String resourcePath) {
        try (InputStream in = UiFonts.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }
}
