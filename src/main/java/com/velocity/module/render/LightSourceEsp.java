package com.velocity.module.render;

import com.velocity.config.LightSourceEspSettings;
import com.velocity.gui.OverlayManager;
import com.velocity.core.ProjectionMath;
import com.velocity.mixin.GameRendererAccessor;

import imgui.ImGui;
import imgui.ImDrawList;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Light Source ESP — renders all light-emitting blocks through walls
 * on the ImGui overlay. Uses the same high-performance pattern as OreEsp:
 *   1. Chunk-section palette skip (luminance check on palette entries)
 *   2. Zero-alloc render loop via ProjectionMath.projectCached()
 *   3. Frustum culling + Distance LOD (dots for far blocks)
 *   4. Max render cap to prevent FPS death in torch-heavy areas
 *
 * Color is based on luminance intensity: warm yellow/orange gradient.
 */
public class LightSourceEsp {

    // ── Cached light source storage (flat int arrays — no object allocation) ──
    // Each entry: [x, y, z, color]
    private static int[] lightData = new int[0];
    private static int lightCount = 0;
    private static int tickCounter = 0;
    private static int lastScanX = Integer.MIN_VALUE;
    private static int lastScanY = Integer.MIN_VALUE;
    private static int lastScanZ = Integer.MIN_VALUE;

    // ── Pre-allocated render buffers ──────────────────────────────────────────
    private static final float[] SX = new float[8];
    private static final float[] SY = new float[8];
    private static final Vector3f SCREEN = new Vector3f();
    private static final int[] GWIN_W = new int[1];
    private static final int[] GWIN_H = new int[1];

    // ── Render limits ────────────────────────────────────────────────────────
    private static final int MAX_RENDER_BLOCKS = 4096;
    private static final double LOD_DIST_SQ = 48.0 * 48.0;

    // ── Pre-computed luminance colors (index 0-15) ───────────────────────────
    private static final int[] LUM_COLORS = new int[16];
    private static boolean colorsInit = false;

    private static void initColors() {
        if (colorsInit) return;
        // Gradient from dim red-orange (low luminance) to bright yellow-white (high)
        for (int i = 0; i < 16; i++) {
            float t = i / 15.0f;
            // R: 0.6 → 1.0, G: 0.2 → 0.95, B: 0.0 → 0.4, A: 0.6 → 1.0
            float r = 0.6f + 0.4f * t;
            float g = 0.2f + 0.75f * t;
            float b = 0.0f + 0.4f * t * t; // quadratic for warm feel
            float a = 0.6f + 0.4f * t;
            LUM_COLORS[i] = ImGui.colorConvertFloat4ToU32(r, g, b, a);
        }
        colorsInit = true;
    }

    public static void render(MinecraftClient client, float tickDelta) {
        if (!LightSourceEspSettings.enabled) return;
        if (client == null || client.world == null || client.player == null) return;

        initColors();

        // ── Periodic rescan ──────────────────────────────────────────────────
        tickCounter++;
        BlockPos playerPos = client.player.getBlockPos();
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        boolean posChanged = px != lastScanX || py != lastScanY || pz != lastScanZ;

        if (tickCounter >= LightSourceEspSettings.rescanTicks || posChanged) {
            tickCounter = 0;
            lastScanX = px;
            lastScanY = py;
            lastScanZ = pz;
            rescan(client, px, py, pz);
        }

        // ── Render ───────────────────────────────────────────────────────────
        if (lightCount == 0) return;

        Camera camera = client.gameRenderer.getCamera();
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float fov = (float) ((GameRendererAccessor) (Object) client.gameRenderer)
                .invokeGetFov(camera, tickDelta, true);

        GLFW.glfwGetWindowSize(OverlayManager.overlayWindow, GWIN_W, GWIN_H);
        int width = GWIN_W[0];
        int height = GWIN_H[0];

        double camX = camera.getCameraPos().x;
        double camY = camera.getCameraPos().y;
        double camZ = camera.getCameraPos().z;

        ProjectionMath.prepareFrame(pitch, yaw, fov, width, height, camX, camY, camZ);

        // Camera forward vector for frustum culling
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float cosP = (float) Math.cos(pitchRad);
        float fwdX = (float) (-Math.sin(yawRad) * cosP);
        float fwdY = (float) (-Math.sin(pitchRad));
        float fwdZ = (float) (Math.cos(yawRad) * cosP);

        ImDrawList drawList = ImGui.getForegroundDrawList();
        int rendered = 0;

        for (int i = 0; i < lightCount && rendered < MAX_RENDER_BLOCKS; i++) {
            int base = i * 4;
            int bx = lightData[base];
            int by = lightData[base + 1];
            int bz = lightData[base + 2];
            int color = lightData[base + 3];

            double cx = bx + 0.5 - camX;
            double cy = by + 0.5 - camY;
            double cz = bz + 0.5 - camZ;

            // Frustum cull
            double dot = cx * fwdX + cy * fwdY + cz * fwdZ;
            if (dot < -1.0) continue;

            double distSq = cx * cx + cy * cy + cz * cz;

            // LOD: far blocks → dot only
            if (distSq > LOD_DIST_SQ) {
                if (ProjectionMath.projectCached(bx + 0.5, by + 0.5, bz + 0.5, SCREEN)) {
                    drawList.addCircleFilled(SCREEN.x, SCREEN.y, 3.0f, color);
                    rendered++;
                }
                continue;
            }

            // Close blocks → wireframe
            boolean anyBehind = false;
            double x0 = bx, x1 = bx + 1, y0 = by, y1 = by + 1, z0 = bz, z1 = bz + 1;
            if (!ProjectionMath.projectCached(x0, y0, z0, SCREEN)) { anyBehind = true; }
            else { SX[0] = SCREEN.x; SY[0] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x1, y0, z0, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[1] = SCREEN.x; SY[1] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x1, y0, z1, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[2] = SCREEN.x; SY[2] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x0, y0, z1, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[3] = SCREEN.x; SY[3] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x0, y1, z0, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[4] = SCREEN.x; SY[4] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x1, y1, z0, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[5] = SCREEN.x; SY[5] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x1, y1, z1, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[6] = SCREEN.x; SY[6] = SCREEN.y; }
            if (!anyBehind && !ProjectionMath.projectCached(x0, y1, z1, SCREEN)) { anyBehind = true; }
            else if (!anyBehind) { SX[7] = SCREEN.x; SY[7] = SCREEN.y; }
            if (anyBehind) continue;

            // Draw 12 edges
            drawList.addLine(SX[0], SY[0], SX[1], SY[1], color, 1.5f);
            drawList.addLine(SX[1], SY[1], SX[2], SY[2], color, 1.5f);
            drawList.addLine(SX[2], SY[2], SX[3], SY[3], color, 1.5f);
            drawList.addLine(SX[3], SY[3], SX[0], SY[0], color, 1.5f);
            drawList.addLine(SX[4], SY[4], SX[5], SY[5], color, 1.5f);
            drawList.addLine(SX[5], SY[5], SX[6], SY[6], color, 1.5f);
            drawList.addLine(SX[6], SY[6], SX[7], SY[7], color, 1.5f);
            drawList.addLine(SX[7], SY[7], SX[4], SY[4], color, 1.5f);
            drawList.addLine(SX[0], SY[0], SX[4], SY[4], color, 1.5f);
            drawList.addLine(SX[1], SY[1], SX[5], SY[5], color, 1.5f);
            drawList.addLine(SX[2], SY[2], SX[6], SY[6], color, 1.5f);
            drawList.addLine(SX[3], SY[3], SX[7], SY[7], color, 1.5f);
            rendered++;
        }
    }

    /**
     * Scans for light-emitting blocks using chunk section palette skip.
     */
    private static void rescan(MinecraftClient client, int px, int py, int pz) {
        lightCount = 0;
        int r = LightSourceEspSettings.scanRadius;
        int minLum = LightSourceEspSettings.minLuminance;

        int maxBlocks = (2 * r + 1) * (2 * r + 1) * (2 * r + 1);
        if (lightData.length < maxBlocks * 4) {
            int cap = Math.min(maxBlocks, 65536);
            lightData = new int[cap * 4];
        }
        int maxEntries = lightData.length / 4;

        int minCX = (px - r) >> 4;
        int maxCX = (px + r) >> 4;
        int minCZ = (pz - r) >> 4;
        int maxCZ = (pz + r) >> 4;

        int worldBottom = client.world.getBottomY();
        int minY = Math.max(py - r, worldBottom);
        int maxY = Math.min(py + r, client.world.getTopYInclusive());

        for (int chunkX = minCX; chunkX <= maxCX; chunkX++) {
            for (int chunkZ = minCZ; chunkZ <= maxCZ; chunkZ++) {
                if (!client.world.isChunkLoaded(chunkX, chunkZ)) continue;
                WorldChunk chunk = client.world.getChunk(chunkX, chunkZ);

                int sectionMinY = minY >> 4;
                int sectionMaxY = maxY >> 4;

                for (int sectionIdx = sectionMinY; sectionIdx <= sectionMaxY; sectionIdx++) {
                    int sectionArrayIdx = sectionIdx - (worldBottom >> 4);
                    ChunkSection[] sections = chunk.getSectionArray();
                    if (sectionArrayIdx < 0 || sectionArrayIdx >= sections.length) continue;

                    ChunkSection section = sections[sectionArrayIdx];
                    if (section == null || section.isEmpty()) continue;

                    // Palette check: does this section contain any light-emitting block?
                    if (!sectionContainsLight(section, minLum)) continue;

                    int secBaseX = chunkX << 4;
                    int secBaseY = sectionIdx << 4;
                    int secBaseZ = chunkZ << 4;

                    int bMinX = Math.max(secBaseX, px - r);
                    int bMaxX = Math.min(secBaseX + 15, px + r);
                    int bMinY = Math.max(secBaseY, minY);
                    int bMaxY = Math.min(secBaseY + 15, maxY);
                    int bMinZ = Math.max(secBaseZ, pz - r);
                    int bMaxZ = Math.min(secBaseZ + 15, pz + r);

                    for (int x = bMinX; x <= bMaxX; x++) {
                        for (int z = bMinZ; z <= bMaxZ; z++) {
                            for (int y = bMinY; y <= bMaxY; y++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                int lum = state.getLuminance();
                                if (lum >= minLum && lightCount < maxEntries) {
                                    int base = lightCount * 4;
                                    lightData[base]     = x;
                                    lightData[base + 1] = y;
                                    lightData[base + 2] = z;
                                    lightData[base + 3] = LUM_COLORS[lum];
                                    lightCount++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean sectionContainsLight(ChunkSection section, int minLum) {
        return section.getBlockStateContainer().hasAny(state -> state.getLuminance() >= minLum);
    }

    public static void invalidateCache() {
        tickCounter = Integer.MAX_VALUE;
        lastScanX = Integer.MIN_VALUE;
    }
}
