package com.velocity.core;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Win32Setup extends StdCallLibrary {
    Win32Setup INSTANCE = Native.load("user32", Win32Setup.class, W32APIOptions.DEFAULT_OPTIONS);

    int WDA_NONE = 0x00;
    int WDA_EXCLUDEFROMCAPTURE = 0x11;

    int GWL_EXSTYLE = -20;
    int WS_EX_TRANSPARENT = 0x00000020;
    int WS_EX_NOACTIVATE = 0x08000000;
    int WS_EX_LAYERED = 0x00080000;
    int WS_EX_TOOLWINDOW = 0x00000080;

    HWND HWND_TOPMOST = new HWND(Pointer.createConstant(-1));
    int SWP_NOMOVE = 0x0002;
    int SWP_NOSIZE = 0x0001;
    int SWP_NOACTIVATE = 0x0010;
    int SWP_FRAMECHANGED = 0x0020;

    // Virtual-key codes
    int VK_INSERT = 0x2D;
    int VK_ESCAPE = 0x1B;
    int VK_LBUTTON = 0x01; // left mouse button
    int VK_RBUTTON = 0x02; // right mouse button
    int VK_XBUTTON1 = 0x05; // mouse4 / back button
    int VK_RCONTROL = 0xA3; // right control button

    boolean SetWindowDisplayAffinity(HWND hWnd, int dwAffinity);

    long SetWindowLongPtr(HWND hWnd, int nIndex, long dwNewLong);

    long GetWindowLongPtr(HWND hWnd, int nIndex);

    boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);

    // Returns HWND of the foreground (active) window
    HWND GetForegroundWindow();

    // Brings the given window to the foreground and gives it focus
    boolean SetForegroundWindow(HWND hWnd);

    // Returns async key/button state — high bit set = currently held down
    short GetAsyncKeyState(int vKey);

    // Returns cursor position in screen coordinates
    boolean GetCursorPos(POINT lpPoint);
}
