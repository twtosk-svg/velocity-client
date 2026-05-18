package com.velocity.gui;

import com.velocity.config.EspSettings;
import com.velocity.core.EspRenderer;
import com.velocity.core.Win32Setup;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

public class OverlayManager {
    public static long overlayWindow = 0;
    static HWND nativeHwnd; // package-private so EspRenderer can pass it to SetForegroundWindow
    private static boolean menuOpen = false;

    private static int framesSinceTopmost = 0;

    public static void init() {
        MinecraftClient client = MinecraftClient.getInstance();
        long mcWindow = client.getWindow().getHandle();

        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);

        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();

        // Pass mcWindow for share so we can access Minecraft textures for item icons!
        overlayWindow = GLFW.glfwCreateWindow(width, height, "Velocity", 0L, mcWindow);
        if (overlayWindow == 0) {
            System.err.println("Failed to create overlay window");
            return;
        }

        // Setup Win32 specific properties
        long hwndLong = GLFWNativeWin32.glfwGetWin32Window(overlayWindow);
        nativeHwnd = new HWND(new Pointer(hwndLong));

        // Make streamproof!
        Win32Setup.INSTANCE.SetWindowDisplayAffinity(nativeHwnd, Win32Setup.WDA_EXCLUDEFROMCAPTURE);

        // Make clickthrough initially
        updateWindowStyle();

        // Assert topmost immediately
        assertTopmost();

        int[] x = new int[1], y = new int[1];
        GLFW.glfwGetWindowPos(mcWindow, x, y);
        GLFW.glfwSetWindowPos(overlayWindow, x[0], y[0]);
    }

    public static void toggleMenu() {
        menuOpen = !menuOpen;
        MinecraftClient client = MinecraftClient.getInstance();
        long mcWindow = client.getWindow().getHandle();

        // Update overlay transparency / passthrough
        GLFW.glfwSetWindowAttrib(overlayWindow, GLFW.GLFW_MOUSE_PASSTHROUGH,
                menuOpen ? GLFW.GLFW_FALSE : GLFW.GLFW_TRUE);
        updateWindowStyle();

        if (menuOpen) {
            GLFW.glfwSetInputMode(mcWindow, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);

            // Give the overlay Win32 focus so it can receive mouse events.
            // Without this Windows routes clicks to MC even though the overlay
            // is on top and WS_EX_NOACTIVATE has been removed.
            Win32Setup.INSTANCE.SetForegroundWindow(nativeHwnd);

        } else {
            // Restore Minecraft cursor dynamically based on whether a UI menu is open
            if (client.currentScreen == null) {
                GLFW.glfwSetInputMode(mcWindow, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            } else {
                GLFW.glfwSetInputMode(mcWindow, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            }

            // Give focus back to Minecraft
            long mcHwndLong = GLFWNativeWin32.glfwGetWin32Window(mcWindow);
            HWND mcHwnd = new HWND(new Pointer(mcHwndLong));
            Win32Setup.INSTANCE.SetForegroundWindow(mcHwnd);
        }
    }

    private static void updateWindowStyle() {
        if (nativeHwnd == null)
            return;

        long exStyle = Win32Setup.INSTANCE.GetWindowLongPtr(nativeHwnd, Win32Setup.GWL_EXSTYLE);
        long newExStyle = exStyle;

        if (!menuOpen) {
            newExStyle |= Win32Setup.WS_EX_TRANSPARENT | Win32Setup.WS_EX_NOACTIVATE;
        } else {
            newExStyle &= ~(Win32Setup.WS_EX_TRANSPARENT | Win32Setup.WS_EX_NOACTIVATE);
        }

        // Always apply ToolWindow so it hides from the taskbar!
        newExStyle |= Win32Setup.WS_EX_TOOLWINDOW;

        Win32Setup.INSTANCE.SetWindowLongPtr(nativeHwnd, Win32Setup.GWL_EXSTYLE, newExStyle);
    }

    public static void onFrame() {
        if (overlayWindow == 0)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long mcWindow = client.getWindow().getHandle();
        
        // Hide overlay completely if Minecraft is minimized or we alt-tabbed to another app.
        // We allow the overlay to stay visible if the overlay itself is focused (e.g. Menu is open and we clicked it).
        boolean mcFocused = GLFW.glfwGetWindowAttrib(mcWindow, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        boolean ovFocused = GLFW.glfwGetWindowAttrib(overlayWindow, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        boolean mcIconified = GLFW.glfwGetWindowAttrib(mcWindow, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

        if (mcIconified || (!mcFocused && !ovFocused)) {
            GLFW.glfwHideWindow(overlayWindow);
            return;
        } else {
            GLFW.glfwShowWindow(overlayWindow);
        }

        // Apply dynamically streamproof toggle
        if (EspSettings.streamproofEnabled) {
            Win32Setup.INSTANCE.SetWindowDisplayAffinity(nativeHwnd, Win32Setup.WDA_EXCLUDEFROMCAPTURE);
        } else {
            Win32Setup.INSTANCE.SetWindowDisplayAffinity(nativeHwnd, Win32Setup.WDA_NONE);
        }

        int[] mcW = new int[1], mcH = new int[1];
        GLFW.glfwGetWindowSize(mcWindow, mcW, mcH);

        int[] ovW = new int[1], ovH = new int[1];
        GLFW.glfwGetWindowSize(overlayWindow, ovW, ovH);

        if (mcW[0] != ovW[0] || mcH[0] != ovH[0]) {
            GLFW.glfwSetWindowSize(overlayWindow, mcW[0], mcH[0]);
        }

        int[] mcX = new int[1], mcY = new int[1];
        GLFW.glfwGetWindowPos(mcWindow, mcX, mcY);

        int[] ovX = new int[1], ovY = new int[1];
        GLFW.glfwGetWindowPos(overlayWindow, ovX, ovY);

        if (mcX[0] != ovX[0] || mcY[0] != ovY[0]) {
            GLFW.glfwSetWindowPos(overlayWindow, mcX[0], mcY[0]);
        }

        framesSinceTopmost++;
        if (framesSinceTopmost >= 60) {
            assertTopmost();
            framesSinceTopmost = 0;
        }
    }

    private static void assertTopmost() {
        if (nativeHwnd != null) {
            Win32Setup.INSTANCE.SetWindowPos(nativeHwnd, Win32Setup.HWND_TOPMOST, 0, 0, 0, 0,
                    Win32Setup.SWP_NOMOVE | Win32Setup.SWP_NOSIZE | Win32Setup.SWP_NOACTIVATE);
        }
    }

    public static void fixOverlay() {
        assertTopmost();
        if (menuOpen && nativeHwnd != null) {
            Win32Setup.INSTANCE.SetForegroundWindow(nativeHwnd);
        }
    }

    public static boolean isMenuOpen() {
        return menuOpen;
    }
}
