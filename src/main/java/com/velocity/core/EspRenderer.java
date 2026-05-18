package com.velocity.core;

import com.velocity.module.combat.AimAssist;
import com.velocity.module.combat.TriggerBot;
import com.velocity.module.combat.TriggerBotV2;
import com.velocity.module.combat.ShieldBreaker;
import com.velocity.module.render.LightSourceEsp;
import com.velocity.module.render.OreEsp;

import com.velocity.module.utility.HealKeybind;
import com.velocity.module.utility.HoverRefill;
import com.velocity.config.ConfigManager;
import com.velocity.config.AimAssistSettings;
import com.velocity.config.UtilitySettings;
import com.velocity.config.OreEspSettings;
import com.velocity.config.EspSettings;
import com.velocity.gui.MenuUI;
import com.velocity.gui.OverlayManager;
import com.velocity.mixin.GameRendererAccessor;
import com.velocity.mixin.HandledScreenAccessor;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;

public class EspRenderer {
    private static ImGuiImplGlfw imGuiGlfw;
    private static ImGuiImplGl3 imGuiGl3;
    private static boolean initialized = false;

    private static boolean insertWasDown = false;
    private static boolean escWasDown = false;
    private static boolean rightControlWasDown = false;

    public static void render(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            BoneDataCache.clear();
            LightDebugManager.reset();
            StructureDebugManager.reset();
            LogoutTracker.clear();
            long overlayWindow = OverlayManager.overlayWindow;
            if (overlayWindow != 0) {
                long prev = GLFW.glfwGetCurrentContext();
                GLFW.glfwMakeContextCurrent(overlayWindow);
                GL11.glClearColor(0, 0, 0, 0);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                GLFW.glfwSwapBuffers(overlayWindow);
                GLFW.glfwMakeContextCurrent(prev);
            }
            return;
        }

        long overlayWindow = OverlayManager.overlayWindow;
        if (overlayWindow == 0)
            return;

        OverlayManager.onFrame();

        long mcHwnd = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getHandle());
        HWND fgHwnd = Win32Setup.INSTANCE.GetForegroundWindow();
        long ovHwnd = GLFWNativeWin32.glfwGetWin32Window(overlayWindow);
        long fgVal = fgHwnd != null ? Pointer.nativeValue(fgHwnd.getPointer()) : 0;
        boolean validForeground = (fgVal == mcHwnd || fgVal == ovHwnd);

        if (!validForeground) {
            long prev = GLFW.glfwGetCurrentContext();
            GLFW.glfwMakeContextCurrent(overlayWindow);
            GL11.glClearColor(0, 0, 0, 0);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GLFW.glfwSwapBuffers(overlayWindow);
            GLFW.glfwMakeContextCurrent(prev);
            return;
        }

        boolean insertDown = (Win32Setup.INSTANCE.GetAsyncKeyState(Win32Setup.VK_INSERT) & 0x8000) != 0;
        boolean escDown = (Win32Setup.INSTANCE.GetAsyncKeyState(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) & 0x8000) != 0;
        boolean rightControlDown = (Win32Setup.INSTANCE.GetAsyncKeyState(Win32Setup.VK_RCONTROL) & 0x8000) != 0;

        if (insertDown && !insertWasDown) {
            boolean wasOpen = OverlayManager.isMenuOpen();
            OverlayManager.toggleMenu();
            if (wasOpen)
                ConfigManager.save(); // save when menu closes
        }
        if (escDown && !escWasDown && OverlayManager.isMenuOpen()) {
            OverlayManager.toggleMenu();
            ConfigManager.save(); // save when menu closes
        }

        if (rightControlDown && !rightControlWasDown) {
            OverlayManager.fixOverlay();
        }

        insertWasDown = insertDown;
        escWasDown = escDown;
        rightControlWasDown = rightControlDown;

        long previousContext = GLFW.glfwGetCurrentContext();
        GLFW.glfwMakeContextCurrent(overlayWindow);

        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        if (!initialized) {
            ImGui.createContext();
            ImGuiIO io = ImGui.getIO();
            java.io.File configDir = new java.io.File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(), "velocity");
            if (!configDir.exists()) configDir.mkdirs();
            io.setIniFilename(new java.io.File(configDir, "velocity_ui.ini").getAbsolutePath());
            
            // Set Theme colors permanently
            imgui.ImGuiStyle style = ImGui.getStyle();
            style.setColor(imgui.flag.ImGuiCol.TitleBgActive,   0.15f, 0.45f, 0.90f, 1.00f);
            style.setColor(imgui.flag.ImGuiCol.Header,          0.10f, 0.35f, 0.75f, 0.55f);
            style.setColor(imgui.flag.ImGuiCol.HeaderHovered,   0.15f, 0.45f, 0.90f, 0.75f);
            style.setColor(imgui.flag.ImGuiCol.HeaderActive,    0.20f, 0.55f, 1.00f, 1.00f);
            style.setColor(imgui.flag.ImGuiCol.CheckMark,       0.20f, 0.75f, 1.00f, 1.00f);
            style.setColor(imgui.flag.ImGuiCol.SliderGrab,      0.20f, 0.65f, 1.00f, 1.00f);
            style.setColor(imgui.flag.ImGuiCol.SliderGrabActive,0.25f, 0.80f, 1.00f, 1.00f);
            style.setColor(imgui.flag.ImGuiCol.Button,          0.10f, 0.35f, 0.75f, 0.70f);
            style.setColor(imgui.flag.ImGuiCol.ButtonHovered,   0.15f, 0.45f, 0.90f, 0.90f);
            style.setColor(imgui.flag.ImGuiCol.ButtonActive,    0.20f, 0.55f, 1.00f, 1.00f);
            style.setColor(imgui.flag.ImGuiCol.FrameBg,         0.08f, 0.08f, 0.12f, 0.80f);
            style.setColor(imgui.flag.ImGuiCol.FrameBgHovered,  0.12f, 0.12f, 0.20f, 0.90f);

            try {
                java.io.File fontFile = new java.io.File("C:\\Windows\\Fonts\\segoeui.ttf");
                if (fontFile.exists()) {
                    io.getFonts().addFontFromFileTTF(fontFile.getAbsolutePath(), 18.0f);
                } else {
                    io.getFonts().addFontDefault();
                }
            } catch (Exception e) {
                e.printStackTrace();
                io.getFonts().addFontDefault();
            }
            GL.createCapabilities();

            imGuiGlfw = new ImGuiImplGlfw();
            imGuiGlfw.init(overlayWindow, true);

            imGuiGl3 = new ImGuiImplGl3();
            imGuiGl3.init("#version 130");

            initialized = true;
        }

        if (OverlayManager.isMenuOpen()) {
            POINT cursorScreen = new POINT();
            Win32Setup.INSTANCE.GetCursorPos(cursorScreen);

            int[] ovX = new int[1], ovY = new int[1];
            GLFW.glfwGetWindowPos(overlayWindow, ovX, ovY);
            ImGui.getIO().setMousePos(cursorScreen.x - ovX[0], cursorScreen.y - ovY[0]);
            ImGui.getIO().setMouseDown(0, (Win32Setup.INSTANCE.GetAsyncKeyState(Win32Setup.VK_LBUTTON) & 0x8000) != 0);
            ImGui.getIO().setMouseDown(1, (Win32Setup.INSTANCE.GetAsyncKeyState(Win32Setup.VK_RBUTTON) & 0x8000) != 0);
        } else {
            ImGui.getIO().setMousePos(-1, -1);
        }

        imGuiGlfw.newFrame();
        ImGui.newFrame();

        boolean inGame = client.currentScreen == null;

        // 0 ── Aim assist tick ────────────────────────────────────────────────
        if (inGame) {
            AimAssist.tick(client, tickDelta);
            InputManager.checkMiddleClickFriend();
        }

        // 0b ── TriggerBot tick ───────────────────────────────────────────────
        if (inGame) {
            if (AimAssistSettings.triggerBotVersion == 0) {
                TriggerBot.tick(client);
            } else {
                TriggerBotV2.tick(client);
            }
            ShieldBreaker.tick(client);
        }

        // 0c ── Heal Keybind tick ─────────────────────────────────────────────
        HealKeybind.tick(client);

        // 0e ── Parkour tick ──────────────────────────────────────────────────

        // 0f ── AutoBucket tick ───────────────────────────────────────────────

        // 0g ── Auto W-Tap tick ───────────────────────────────────────────────
        if (inGame) {
        }

        // 0d ── Hover Refill tick ─────────────────────────────────────────────
        if (UtilitySettings.hoverRefillEnabled && client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) {
            int hoveredSlot = -1;
            try {
                net.minecraft.screen.slot.Slot focused =
                        ((com.velocity.mixin.HandledScreenAccessor) client.currentScreen).getFocusedSlot();
                if (focused != null) {
                    hoveredSlot = focused.id;
                }
            } catch (Exception ignored) {}
            HoverRefill.tick(client, hoveredSlot);
        }

        // 1 ── ESP boxes ───────────────────────────────────────────────────────
        if (EspSettings.espEnabled) {
            drawEsp(client, tickDelta);
        } else if (StructureDebugManager.enabled) {
            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getCameraPos();
            float pitch = camera.getPitch();
            float yaw = camera.getYaw();
            float fov = (float) ((GameRendererAccessor) (Object) client.gameRenderer)
                    .invokeGetFov(camera, tickDelta, true);

            GLFW.glfwGetWindowSize(OverlayManager.overlayWindow, GWIN_W, GWIN_H);
            int width = GWIN_W[0];
            int height = GWIN_H[0];

            ProjectionMath.prepareFrame(pitch, yaw, fov, width, height,
                    cameraPos.x, cameraPos.y, cameraPos.z);
            StructureDebugManager.render(client, ImGui.getBackgroundDrawList(), tickDelta);
        }

        // 1b ── Ore ESP (with toggle keybind) ──────────────────────────────────
        tickOreToggleKey();
        OreEsp.render(client, tickDelta);
        // 1c ── Light Source ESP (through walls) ────────────────────────────────
        LightSourceEsp.render(client, tickDelta);

        // 1d ── Light Debug Renderers ──────────────────────────────────────────
        LightDebugManager.tick();
        StructureDebugManager.tick();
        LogoutTracker.tick(client);

        // 2 ── Menu ────────────────────────────────────────────────────────────
        if (OverlayManager.isMenuOpen()) {
            MenuUI.draw(1.0f, 1.0f);
        }
        
        // 3 ── Admin Radar ─────────────────────────────────────────────────────
        com.velocity.gui.AdminRadar.draw(client);
        
        // 4 ── Player Radar ─────────────────────────────────────────────────────────
        com.velocity.gui.PlayerRadar.draw(client);

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        GLFW.glfwSwapBuffers(overlayWindow);
        GLFW.glfwMakeContextCurrent(previousContext);
    }

    // ── Ore ESP toggle keybind state ──────────────────────────────────────────
    private static boolean oreKeyWasDown = false;

    private static void tickOreToggleKey() {
        int key = OreEspSettings.oreToggleKey;
        if (key < 0) return;
        boolean keyDown = false;
        try {
            int vk = HealKeybind.glfwKeyToVk(key);
            if (vk > 0) {
                keyDown = (Win32Setup.INSTANCE.GetAsyncKeyState(vk) & 0x8000) != 0;
            }
        } catch (Exception ignored) {}
        boolean pressed = keyDown && !oreKeyWasDown;
        oreKeyWasDown = keyDown;
        if (pressed) {
            OreEspSettings.enabled = !OreEspSettings.enabled;
            if (OreEspSettings.enabled) OreEsp.invalidateCache();
        }
    }

    // ── Pre-allocated render buffers (zero GC pressure) ──────────────────────
    private static final Vector3f espScreen = new Vector3f();
    private static final StringBuilder distSb = new StringBuilder(16);
    // Cached constant colors (computed once)
    private static int cachedBlack = 0;
    private static int cachedGreen = 0;
    private static int cachedBlue = 0;
    private static boolean espColorsInit = false;
    // Pre-allocated GLFW window size arrays (avoid per-frame alloc)
    private static final int[] GWIN_W = new int[1];
    private static final int[] GWIN_H = new int[1];
    // Pre-allocated corner buffer for 8 corners × 3 coords = 24 doubles
    private static final double[] CORNER_BUF = new double[24];

    private static void initEspColors() {
        if (espColorsInit) return;
        cachedBlack = ImGui.colorConvertFloat4ToU32(0f, 0f, 0f, 1f);
        cachedGreen = ImGui.colorConvertFloat4ToU32(0f, 1f, 0f, 1f);
        cachedBlue  = ImGui.colorConvertFloat4ToU32(0f, 0f, 1f, 1f);
        espColorsInit = true;
    }

    private static void drawEsp(MinecraftClient client, float tickDelta) {
        initEspColors();

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        // Use the private getFov via our Mixin Invoker accessor.
        float fov = (float) ((GameRendererAccessor) (Object) client.gameRenderer)
                .invokeGetFov(camera, tickDelta, true);

        GLFW.glfwGetWindowSize(OverlayManager.overlayWindow, GWIN_W, GWIN_H);
        int width = GWIN_W[0];
        int height = GWIN_H[0];

        // Prepare per-frame camera context (caches all trig — zero trig per projection)
        ProjectionMath.prepareFrame(pitch, yaw, fov, width, height,
                cameraPos.x, cameraPos.y, cameraPos.z);

        // Cache the draw list reference (avoid repeated method calls)
        imgui.ImDrawList drawList = ImGui.getBackgroundDrawList();

        // ── FOV circle ───────────────────────────────────────────────────────
        if (AimAssistSettings.enabled && AimAssistSettings.fovCircleEnabled) {
            float gameTanHalf = ProjectionMath.getCachedTanHalfFov();
            float aimTan = (float) Math.tan(Math.toRadians(AimAssistSettings.fovDegrees));
            float radius = (aimTan / gameTanHalf) * (height * 0.5f);
            float[] fc = AimAssistSettings.fovCircleColor;
            int fovCircleCol = ImGui.colorConvertFloat4ToU32(fc[0], fc[1], fc[2], fc[3]);
            drawList.addCircle(
                    width * 0.5f, height * 0.5f, radius, fovCircleCol, 64, 1.5f);
            
            // Render Focus text above FOV circle
            if (AimAssist.isFocusMode()) {
                String text = "Focus: Waiting...";
                if (AimAssist.getTargetCandidate() != null) {
                    text = "Focus: " + AimAssist.getTargetCandidate().getName().getString();
                }
                float tw = ImGui.calcTextSize(text).x;
                float tx = width * 0.5f - tw * 0.5f;
                float ty = height * 0.5f - radius - 20f;
                drawList.addText(tx, ty, cachedBlack, text);
                drawList.addText(tx + 1f, ty, cachedBlack, text);
                drawList.addText(tx + 0.5f, ty - 0.5f, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), text);
                drawList.addText(tx + 1.5f, ty - 0.5f, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), text);
            }
        }

        Entity aimTarget = AimAssist.getTargetCandidate();

        float maxDistSq = EspSettings.maxDistance * EspSettings.maxDistance;

        // Pre-compute per-frame colors (avoid per-entity colorConvert calls)
        int tracerCol = 0;
        if (EspSettings.tracersEnabled) {
            float[] tc = EspSettings.tracerColor;
            tracerCol = ImGui.colorConvertFloat4ToU32(tc[0], tc[1], tc[2], tc[3]);
        }
        int eyeTraceCol = 0;
        if (EspSettings.eyeTraceEnabled) {
            float[] ec = EspSettings.eyeTraceColor;
            eyeTraceCol = ImGui.colorConvertFloat4ToU32(ec[0], ec[1], ec[2], ec[3]);
        }
        int shieldCol = 0;
        if (EspSettings.shieldEspEnabled) {
            float[] sc = EspSettings.shieldColor;
            shieldCol = ImGui.colorConvertFloat4ToU32(sc[0], sc[1], sc[2], sc[3]);
        }



        for (Entity entity : client.world.getEntities()) {
            boolean isPlayer = entity instanceof AbstractClientPlayerEntity && entity != client.player;
            boolean isVillager = entity instanceof VillagerEntity;
            boolean isZombie = entity instanceof net.minecraft.entity.mob.ZombieEntity;

            if (!isPlayer && !isVillager && !isZombie)
                continue;

            // Early distance cull — skip entities beyond configured max distance
            if (client.player.squaredDistanceTo(entity) > maxDistSq)
                continue;

            Box box = entity.getBoundingBox();
            Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
            Vec3d currentPos = entity.getEntityPos();

            double dx = lerpedPos.x - currentPos.x;
            double dy = lerpedPos.y - currentPos.y;
            double dz = lerpedPos.z - currentPos.z;
            Box lerpedBox = box.offset(dx, dy, dz);

            // Project all 8 corners using primitive overload (zero Vec3d alloc)
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean anyBehind = false;

            // Populate pre-allocated corner buffer (zero alloc)
            CORNER_BUF[0]  = lerpedBox.minX; CORNER_BUF[1]  = lerpedBox.minY; CORNER_BUF[2]  = lerpedBox.minZ;
            CORNER_BUF[3]  = lerpedBox.minX; CORNER_BUF[4]  = lerpedBox.maxY; CORNER_BUF[5]  = lerpedBox.minZ;
            CORNER_BUF[6]  = lerpedBox.maxX; CORNER_BUF[7]  = lerpedBox.minY; CORNER_BUF[8]  = lerpedBox.minZ;
            CORNER_BUF[9]  = lerpedBox.maxX; CORNER_BUF[10] = lerpedBox.maxY; CORNER_BUF[11] = lerpedBox.minZ;
            CORNER_BUF[12] = lerpedBox.minX; CORNER_BUF[13] = lerpedBox.minY; CORNER_BUF[14] = lerpedBox.maxZ;
            CORNER_BUF[15] = lerpedBox.minX; CORNER_BUF[16] = lerpedBox.maxY; CORNER_BUF[17] = lerpedBox.maxZ;
            CORNER_BUF[18] = lerpedBox.maxX; CORNER_BUF[19] = lerpedBox.minY; CORNER_BUF[20] = lerpedBox.maxZ;
            CORNER_BUF[21] = lerpedBox.maxX; CORNER_BUF[22] = lerpedBox.maxY; CORNER_BUF[23] = lerpedBox.maxZ;

            for (int i = 0; i < 8; i++) {
                int ci = i * 3;
                if (!ProjectionMath.projectCached(
                        CORNER_BUF[ci], CORNER_BUF[ci + 1], CORNER_BUF[ci + 2],
                        espScreen)) {
                    anyBehind = true;
                    break;
                }
                if (espScreen.x < minX) minX = espScreen.x;
                if (espScreen.y < minY) minY = espScreen.y;
                if (espScreen.x > maxX) maxX = espScreen.x;
                if (espScreen.y > maxY) maxY = espScreen.y;
            }

            if (!anyBehind) {
                // Prevent NaN and Infinity propagation which causes ImGui native crashes (EXCEPTION_ACCESS_VIOLATION)
                if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX) || !Float.isFinite(maxY)) continue;
                if (minX >= maxX || minY >= maxY) continue;
                
                // Prevent astronomically large bounding boxes from overflowing ImGui's vertex generation
                if (maxX - minX > 15000f || maxY - minY > 15000f) continue;
                if (minX < -15000f || maxX > 15000f || minY < -15000f || maxY > 15000f) continue;

                if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
                    SkeletonEsp.renderSkeleton(drawList, player, tickDelta);
                }

                if (EspSettings.espBoxesEnabled) {
                    int color = isVillager ? cachedGreen : isZombie ? cachedBlue : ImGui.colorConvertFloat4ToU32(EspSettings.boxColor[0], EspSettings.boxColor[1], EspSettings.boxColor[2], EspSettings.boxColor[3]);
                    if (EspSettings.friendsSystemEnabled && EspSettings.friendEspOverride && com.velocity.config.FriendManager.isFriend(entity.getName().getString())) {
                        color = ImGui.colorConvertFloat4ToU32(EspSettings.friendColor[0], EspSettings.friendColor[1], EspSettings.friendColor[2], EspSettings.friendColor[3]);
                    }
                    drawList.addRect(minX, minY, maxX, maxY, color, 0, 0, 2f);
                }

                // --- Tracers ---
                if (EspSettings.tracersEnabled) {
                    float entityCenterX = (minX + maxX) * 0.5f;
                    float entityBottomY = maxY;
                    drawList.addLine(
                            width * 0.5f, height * 0.5f,
                            entityCenterX, entityBottomY,
                            tracerCol, 1.5f);
                }

                // --- Eye Tracers ---
                if (EspSettings.eyeTraceEnabled && entity instanceof net.minecraft.entity.LivingEntity living) {
                    net.minecraft.util.math.Vec3d eyePos = living.getEyePos();
                    net.minecraft.util.math.Vec3d lookVec = living.getRotationVec(1.0f);
                    net.minecraft.util.math.Vec3d endPos = eyePos.add(lookVec.multiply(EspSettings.eyeTraceLength));
                    
                    org.joml.Vector3f startScreen = new org.joml.Vector3f();
                    org.joml.Vector3f endScreen = new org.joml.Vector3f();
                    
                    if (ProjectionMath.projectCached((float)eyePos.x, (float)eyePos.y, (float)eyePos.z, startScreen) &&
                        ProjectionMath.projectCached((float)endPos.x, (float)endPos.y, (float)endPos.z, endScreen)) {
                        drawList.addLine(startScreen.x, startScreen.y, endScreen.x, endScreen.y, eyeTraceCol, 2f);
                    }
                }

                // --- Shield Indicator ---
                if (EspSettings.shieldEspEnabled && entity instanceof net.minecraft.entity.LivingEntity living) {
                    if (living.isUsingItem() && living.getActiveItem().getItem().getTranslationKey().toLowerCase().contains("shield")) {
                        if (EspSettings.shieldEspMode == 0 || EspSettings.shieldEspMode == 2) {
                            drawList.addRect(minX, minY, maxX, maxY, shieldCol, 0f, 0, EspSettings.shieldEspThickness);
                        }
                        if (EspSettings.shieldEspMode == 1 || EspSettings.shieldEspMode == 2) {
                            // Calculate a fill color with 30% of the original alpha
                            int fillAlpha = (int) (((shieldCol >> 24) & 0xFF) * 0.3f);
                            int fillCol = (shieldCol & 0x00FFFFFF) | (fillAlpha << 24);
                            drawList.addRectFilled(minX, minY, maxX, maxY, fillCol);
                        }
                    }
                }

                // --- Nametags ---
                if (EspSettings.nametagsEnabled) {
                    String name = EspSettings.advancedNametagsEnabled ? entity.getDisplayName().getString() : entity.getName().getString();
                    float textWidth = ImGui.calcTextSize(name).x;
                    float nameX = minX + (maxX - minX) / 2f - textWidth / 2f;
                    float nameY = minY - 15f;

                    drawList.addText(nameX + 1.5f, nameY + 1.5f, cachedBlack, name);
                    drawList.addText(nameX, nameY, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), name);
                    drawList.addText(nameX + 1f, nameY, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), name);
                }

                // --- Distance (allocation-free formatting) ---
                if (EspSettings.distanceEnabled) {
                    float dist = client.player.distanceTo(entity);
                    // StringBuilder instead of String.format (avoids Formatter + char[] alloc)
                    distSb.setLength(0);
                    int whole = (int) dist;
                    int frac = (int) ((dist - whole) * 10 + 0.5f);
                    if (frac >= 10) { whole++; frac = 0; }
                    distSb.append(whole).append('.').append(frac).append('m');
                    String distText = distSb.toString();

                    float textWidth = ImGui.calcTextSize(distText).x;
                    float distX = minX + (maxX - minX) / 2f - textWidth / 2f;
                    float distY = maxY + 4f;

                    if (EspSettings.healthbarsEnabled && EspSettings.healthPosition == 3) {
                        distY += EspSettings.healthbarThickness + 4f;
                    }

                    drawList.addText(distX + 1f, distY + 1f, cachedBlack, distText);
                    drawList.addText(distX, distY, ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f), distText);

                    if (EspSettings.equipmentEspEnabled && entity instanceof net.minecraft.entity.LivingEntity) {
                        net.minecraft.entity.LivingEntity living = (net.minecraft.entity.LivingEntity) entity;
                        net.minecraft.item.ItemStack stack = living.getMainHandStack();
                        if (stack != null && !stack.isEmpty()) {
                            String itemName = stack.getName().getString();
                            float itemWidth = ImGui.calcTextSize(itemName).x;
                            float itemX = minX + (maxX - minX) / 2f - itemWidth / 2f;
                            float itemY = distY + 12f;
                            drawList.addText(itemX + 1f, itemY + 1f, cachedBlack, itemName);
                            drawList.addText(itemX, itemY, ImGui.colorConvertFloat4ToU32(1f, 1f, 0f, 1f), itemName); // Yellow text for item
                        }
                    }
                }

                // --- Healthbar ---
                if (EspSettings.healthbarsEnabled && entity instanceof net.minecraft.entity.LivingEntity) {
                    net.minecraft.entity.LivingEntity living = (net.minecraft.entity.LivingEntity) entity;
                    float health = living.getHealth();
                    float maxHealth = Math.max(1.0f, living.getMaxHealth());
                    float healthPercent = Math.max(0f, Math.min(1f, health / maxHealth));

                    float boxWidth = maxX - minX;
                    float boxHeight = maxY - minY;

                    float barWidth = EspSettings.healthbarThickness;
                    float barX = minX, barY = minY;
                    float w = barWidth, h = boxHeight;
                    boolean horizontal = false;

                    switch (EspSettings.healthPosition) {
                        case 0: barX = minX - barWidth - 2f; barY = minY; w = barWidth; h = boxHeight; break;
                        case 1: barX = maxX + 2f; barY = minY; w = barWidth; h = boxHeight; break;
                        case 2: barX = minX; barY = minY - barWidth - 2f; w = boxWidth; h = barWidth; horizontal = true; break;
                        case 3: barX = minX; barY = maxY + 2f; w = boxWidth; h = barWidth; horizontal = true; break;
                    }

                    int healthColor;

                    if (EspSettings.healthV2Enabled) {
                        healthColor = ImGui.colorConvertFloat4ToU32(1f, 0.3f, 1f, 1f);
                        int glowLayers = 10;
                        float baseStrength = EspSettings.healthV2GlowStrength / 10f;
                        float maxGlowSize = 2f + (EspSettings.healthV2GlowStrength * 1.5f);

                        float fillW = horizontal ? w * healthPercent : w;
                        float fillH = horizontal ? h : h * healthPercent;
                        float fillX = barX;
                        float fillY = horizontal ? barY : barY + h - fillH;

                        for (int i = 1; i <= glowLayers; i++) {
                            float layerAlpha = baseStrength * (float) Math.pow(1.0 - ((float) i / glowLayers), 2.0);
                            int layerColor = ImGui.colorConvertFloat4ToU32(1f, 0.3f, 1f, layerAlpha);
                            float offset = (maxGlowSize / glowLayers) * i;
                            float rectRounding = offset * 0.8f;
                            drawList.addRectFilled(
                                    fillX - offset, fillY - offset,
                                    fillX + fillW + offset, fillY + fillH + offset,
                                    layerColor, rectRounding);
                        }
                    } else {
                        healthColor = ImGui.colorConvertFloat4ToU32((1f - healthPercent), healthPercent, 0f, 1f);
                    }

                    drawList.addRectFilled(barX, barY, barX + w, barY + h, cachedBlack);

                    if (horizontal) {
                        float fillW = w * healthPercent;
                        drawList.addRectFilled(barX, barY, barX + fillW, barY + h, healthColor);
                    } else {
                        float fillH = h * healthPercent;
                        drawList.addRectFilled(barX, barY + h - fillH, barX + w, barY + h, healthColor);
                    }
                    
                    // --- Absorption (Golden Health) ---
                    if (EspSettings.absorptionBarsEnabled && living.getAbsorptionAmount() > 0) {
                        float abs = living.getAbsorptionAmount();
                        float absPercent = Math.min(1f, abs / maxHealth);
                        int absColor = ImGui.colorConvertFloat4ToU32(1f, 0.8f, 0f, 1f); // Gold

                        float aBarX = barX;
                        float aBarY = barY;
                        if (EspSettings.healthPosition == 0) aBarX = barX - barWidth - 1f;
                        if (EspSettings.healthPosition == 1) aBarX = barX + barWidth + 1f;
                        if (EspSettings.healthPosition == 2) aBarY = barY - barWidth - 1f;
                        if (EspSettings.healthPosition == 3) aBarY = barY + barWidth + 1f;

                        if (horizontal) {
                            float fillW = w * absPercent;
                            drawList.addRectFilled(aBarX, aBarY, aBarX + w, aBarY + h, cachedBlack);
                            drawList.addRectFilled(aBarX, aBarY, aBarX + fillW, aBarY + h, absColor);
                        } else {
                            float fillH = h * absPercent;
                            drawList.addRectFilled(aBarX, aBarY, aBarX + w, aBarY + h, cachedBlack);
                            drawList.addRectFilled(aBarX, aBarY + h - fillH, aBarX + w, aBarY + h, absColor);
                        }
                    }

                    // --- Armor Bar ---
                    if (EspSettings.armorBarsEnabled && living.getArmor() > 0) {
                        int armor = living.getArmor();
                        float armorPercent = Math.min(1f, armor / 20f);
                        int armorColor = ImGui.colorConvertFloat4ToU32(0.3f, 0.7f, 1f, 1f); // Light blue

                        float armBarX = barX;
                        float armBarY = barY;
                        
                        if (EspSettings.healthPosition == 0) {
                            armBarX = barX - barWidth - 1f;
                            if (EspSettings.absorptionBarsEnabled && living.getAbsorptionAmount() > 0) armBarX -= barWidth + 1f;
                        } else if (EspSettings.healthPosition == 1) {
                            armBarX = barX + barWidth + 1f;
                            if (EspSettings.absorptionBarsEnabled && living.getAbsorptionAmount() > 0) armBarX += barWidth + 1f;
                        } else if (EspSettings.healthPosition == 2) {
                            armBarY = barY - barWidth - 1f;
                            if (EspSettings.absorptionBarsEnabled && living.getAbsorptionAmount() > 0) armBarY -= barWidth + 1f;
                        } else if (EspSettings.healthPosition == 3) {
                            armBarY = barY + barWidth + 1f;
                            if (EspSettings.absorptionBarsEnabled && living.getAbsorptionAmount() > 0) armBarY += barWidth + 1f;
                        }

                        if (horizontal) {
                            drawList.addRectFilled(armBarX, armBarY, armBarX + w, armBarY + h, cachedBlack);
                            drawList.addRectFilled(armBarX, armBarY, armBarX + (w * armorPercent), armBarY + h, armorColor);
                            // Draw separators
                            for (int i = 1; i < 10; i++) {
                                float segX = armBarX + (w * (i / 10f));
                                drawList.addLine(segX, armBarY, segX, armBarY + h, cachedBlack, 1f);
                            }
                            String armText = String.format(java.util.Locale.US, "%.1f", armor / 2.0f).replace(".0", "");
                            drawList.addText(armBarX + w + 2f, armBarY - 2f, cachedBlack, armText);
                            drawList.addText(armBarX + w + 1f, armBarY - 3f, armorColor, armText);
                        } else {
                            drawList.addRectFilled(armBarX, armBarY, armBarX + w, armBarY + h, cachedBlack);
                            drawList.addRectFilled(armBarX, armBarY + h - (h * armorPercent), armBarX + w, armBarY + h, armorColor);
                            // Draw separators
                            for (int i = 1; i < 10; i++) {
                                float segY = armBarY + (h * (i / 10f));
                                drawList.addLine(armBarX, segY, armBarX + w, segY, cachedBlack, 1f);
                            }
                            String armText = String.format(java.util.Locale.US, "%.1f", armor / 2.0f).replace(".0", "");
                            float tw = ImGui.calcTextSize(armText).x;
                            drawList.addText(armBarX + w / 2f - tw / 2f + 1f, armBarY + h + 1f, cachedBlack, armText);
                            drawList.addText(armBarX + w / 2f - tw / 2f, armBarY + h, armorColor, armText);
                        }
                    }
                }
            }
        }
        
        // Draw custom Structure ESP
        StructureDebugManager.render(client, drawList, tickDelta);
    }
}
