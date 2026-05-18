package com.velocity.module.render;

import com.velocity.config.OreEspSettings;
import com.velocity.gui.OverlayManager;
import com.velocity.core.EspRenderer;
import com.velocity.core.ProjectionMath;
import com.velocity.mixin.GameRendererAccessor;

import imgui.ImGui;
import imgui.ImDrawList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Ore ESP — High Performance Block ESP.
 *
 * Optimizations vs naive approach:
 *  1. Chunk-section palette skip — checks if a 16³ section even CONTAINS
 *     an ore block before iterating its 4096 blocks (instant skip for 99% of sections)
 *  2. Zero-alloc render loop — all Vec3d corners and float arrays are pre-allocated
 *     static fields, reused every frame (zero GC pressure)
 *  3. Frustum culling — blocks behind the camera are rejected early via a cheap
 *     center-point dot-product check before any projection math
 *  4. Distance LOD — blocks beyond 48 blocks render as a single colored dot
 *     instead of a 12-line wireframe (12× fewer draw calls for distant ores)
 *  5. Max render cap — limits to 4096 drawn blocks per frame to prevent
 *     FPS death in extremely ore-dense areas
 *  6. Cached rescan with configurable interval — only scans every N ticks
 */
public class OreEsp {

    // ── Cached ore storage (flat int arrays — no object allocation) ───────────
    // Each ore is stored as 4 ints: [x, y, z, color]
    private static int[] oreData = new int[0]; // packed array
    private static int oreCount = 0;
    private static int tickCounter = 0;
    private static int lastScanX = Integer.MIN_VALUE;
    private static int lastScanY = Integer.MIN_VALUE;
    private static int lastScanZ = Integer.MIN_VALUE;

    // ── Pre-allocated render buffers (zero GC) ───────────────────────────────
    private static final float[] SX = new float[8];
    private static final float[] SY = new float[8];
    private static final Vector3f SCREEN = new Vector3f();
    // Pre-allocated GLFW window size arrays (avoid per-frame alloc)
    private static final int[] GWIN_W = new int[1];
    private static final int[] GWIN_H = new int[1];



    // ── Render limits ────────────────────────────────────────────────────────
    private static final int MAX_RENDER_BLOCKS = 4096;
    private static final double LOD_DIST_SQ = 48.0 * 48.0; // beyond this, draw dots

    // ── Color constants ──────────────────────────────────────────────────────
    private static int COL_DIAMOND;
    private static int COL_EMERALD;
    private static int COL_LAPIS;
    private static int COL_REDSTONE;
    private static int COL_GOLD;
    private static int COL_IRON;
    private static int COL_COAL;
    private static int COL_COPPER;
    private static int COL_ANCIENT_DEBRIS;
    private static int COL_QUARTZ;
    private static boolean colorsInitialized = false;

    private static void initColors() {
        if (colorsInitialized) return;
        COL_DIAMOND       = ImGui.colorConvertFloat4ToU32(0.0f,  1.0f,  1.0f,  1.0f);
        COL_EMERALD       = ImGui.colorConvertFloat4ToU32(0.0f,  1.0f,  0.3f,  1.0f);
        COL_LAPIS         = ImGui.colorConvertFloat4ToU32(0.1f,  0.1f,  0.8f,  1.0f);
        COL_REDSTONE      = ImGui.colorConvertFloat4ToU32(1.0f,  0.0f,  0.0f,  1.0f);
        COL_GOLD          = ImGui.colorConvertFloat4ToU32(1.0f,  0.85f, 0.0f,  1.0f);
        COL_IRON          = ImGui.colorConvertFloat4ToU32(0.85f, 0.55f, 0.35f, 1.0f);
        COL_COAL          = ImGui.colorConvertFloat4ToU32(0.3f,  0.3f,  0.3f,  1.0f);
        COL_COPPER        = ImGui.colorConvertFloat4ToU32(0.9f,  0.5f,  0.2f,  1.0f);
        COL_ANCIENT_DEBRIS= ImGui.colorConvertFloat4ToU32(0.55f, 0.3f,  0.2f,  1.0f);
        COL_QUARTZ        = ImGui.colorConvertFloat4ToU32(1.0f,  1.0f,  1.0f,  1.0f);
        colorsInitialized = true;
    }

    /**
     * Called every frame from EspRenderer.render().
     */
    public static void render(MinecraftClient client, float tickDelta) {
        if (!OreEspSettings.enabled) return;
        if (client == null || client.world == null || client.player == null) return;

        initColors();

        // ── Periodic rescan ──────────────────────────────────────────────────
        tickCounter++;
        BlockPos playerPos = client.player.getBlockPos();
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        boolean posChanged = px != lastScanX || py != lastScanY || pz != lastScanZ;

        if (tickCounter >= OreEspSettings.rescanTicks || posChanged) {
            tickCounter = 0;
            lastScanX = px;
            lastScanY = py;
            lastScanZ = pz;
            rescan(client, px, py, pz);
        }

        // ── Render ───────────────────────────────────────────────────────────
        if (oreCount == 0) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float fov = (float) ((GameRendererAccessor) (Object) client.gameRenderer)
                .invokeGetFov(camera, tickDelta, true);

        GLFW.glfwGetWindowSize(OverlayManager.overlayWindow, GWIN_W, GWIN_H);
        int width = GWIN_W[0];
        int height = GWIN_H[0];

        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        // Prepare per-frame camera context (caches all trig — zero trig per projection)
        ProjectionMath.prepareFrame(pitch, yaw, fov, width, height, camX, camY, camZ);

        // Pre-compute camera forward vector for frustum culling
        // Use raw trig (not cached negated values) since forward vec uses positive yaw
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float cosP = (float) Math.cos(pitchRad);
        float fwdX = (float) (-Math.sin(yawRad) * cosP);
        float fwdY = (float) (-Math.sin(pitchRad));
        float fwdZ = (float) (Math.cos(yawRad) * cosP);

        ImDrawList drawList = ImGui.getBackgroundDrawList();
        int rendered = 0;

        for (int i = 0; i < oreCount && rendered < MAX_RENDER_BLOCKS; i++) {
            int base = i * 4;
            int bx = oreData[base];
            int by = oreData[base + 1];
            int bz = oreData[base + 2];
            int color = oreData[base + 3];

            // Center of the block
            double cx = bx + 0.5 - camX;
            double cy = by + 0.5 - camY;
            double cz = bz + 0.5 - camZ;

            // Frustum cull: dot product with camera forward — if negative, block is behind us
            double dot = cx * fwdX + cy * fwdY + cz * fwdZ;
            if (dot < -1.0) continue; // behind camera (with 1 block tolerance)

            double distSq = cx * cx + cy * cy + cz * cz;

            // Distance LOD: far blocks → draw a single dot
            if (distSq > LOD_DIST_SQ) {
                // Project center point only (primitive overload — no Vec3d alloc)
                if (ProjectionMath.projectCached(
                        bx + 0.5, by + 0.5, bz + 0.5, SCREEN)) {
                    drawList.addCircleFilled(SCREEN.x, SCREEN.y, 2.5f, color);
                    rendered++;
                }
                continue;
            }

            // Full wireframe for close blocks (primitive overload — no Vec3d alloc)
            boolean anyBehind = false;
            // 8 corners: {x, x+1} × {y, y+1} × {z, z+1}
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
     * Chunk-section-aware rescan. Skips entire 16×16×16 sections whose block
     * palette doesn't contain any ore blocks (eliminates ~99% of block checks).
     */
    private static void rescan(MinecraftClient client, int px, int py, int pz) {
        oreCount = 0;
        int r = OreEspSettings.scanRadius;

        // Ensure capacity (worst case: entire volume is ores)
        int maxBlocks = (2 * r + 1) * (2 * r + 1) * (2 * r + 1);
        if (oreData.length < maxBlocks * 4) {
            // Cap allocation to prevent OOM
            int cap = Math.min(maxBlocks, 65536);
            oreData = new int[cap * 4];
        }
        int maxOres = oreData.length / 4;

        int minCX = (px - r) >> 4;
        int maxCX = (px + r) >> 4;
        int minCZ = (pz - r) >> 4;
        int maxCZ = (pz + r) >> 4;

        int worldBottom = client.world.getBottomY();
        int minY = Math.max(py - r, worldBottom);
        int maxY = Math.min(py + r, client.world.getTopYInclusive());

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int chunkX = minCX; chunkX <= maxCX; chunkX++) {
            for (int chunkZ = minCZ; chunkZ <= maxCZ; chunkZ++) {
                if (!client.world.isChunkLoaded(chunkX, chunkZ)) continue;
                WorldChunk chunk = client.world.getChunk(chunkX, chunkZ);

                // Iterate chunk sections (each is 16×16×16)
                int sectionMinY = minY >> 4;
                int sectionMaxY = maxY >> 4;

                for (int sectionIdx = sectionMinY; sectionIdx <= sectionMaxY; sectionIdx++) {
                    int sectionArrayIdx = sectionIdx - (worldBottom >> 4);
                    ChunkSection[] sections = chunk.getSectionArray();
                    if (sectionArrayIdx < 0 || sectionArrayIdx >= sections.length) continue;

                    ChunkSection section = sections[sectionArrayIdx];
                    if (section == null || section.isEmpty()) continue;

                    // Palette check: does this section contain ANY ore?
                    if (!sectionContainsOre(section)) continue;

                    // This section has ores — scan its blocks
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
                                // Read directly from section (avoids world.getBlockState overhead)
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                int color = getOreColor(state.getBlock());
                                if (color != 0 && oreCount < maxOres) {
                                    int base = oreCount * 4;
                                    oreData[base]     = x;
                                    oreData[base + 1] = y;
                                    oreData[base + 2] = z;
                                    oreData[base + 3] = color;
                                    oreCount++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a chunk section's palette contains any ore block.
     * This is the key optimization: if a 16³ section has no ores in its palette,
     * we skip all 4096 blocks instantly.
     */
    private static boolean sectionContainsOre(ChunkSection section) {
        // We use container.hasAny() which checks the palette without iterating all blocks.
        // Since we need to check multiple blocks, we iterate via the predicate.
        return section.getBlockStateContainer().hasAny(state -> getOreColor(state.getBlock()) != 0);
    }

    /**
     * Returns the ore color for the given block, or 0 if not tracked.
     */
    private static int getOreColor(Block block) {
        if (OreEspSettings.diamond && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE))
            return COL_DIAMOND;
        if (OreEspSettings.emerald && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE))
            return COL_EMERALD;
        if (OreEspSettings.lapis && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE))
            return COL_LAPIS;
        if (OreEspSettings.redstone && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE))
            return COL_REDSTONE;
        if (OreEspSettings.gold && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE))
            return COL_GOLD;
        if (OreEspSettings.iron && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE))
            return COL_IRON;
        if (OreEspSettings.coal && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE))
            return COL_COAL;
        if (OreEspSettings.copper && (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE))
            return COL_COPPER;
        if (OreEspSettings.ancientDebris && block == Blocks.ANCIENT_DEBRIS)
            return COL_ANCIENT_DEBRIS;
        if (OreEspSettings.quartz && block == Blocks.NETHER_QUARTZ_ORE)
            return COL_QUARTZ;
        return 0;
    }

    /**
     * Forces a rescan next tick.
     */
    public static void invalidateCache() {
        tickCounter = Integer.MAX_VALUE;
        lastScanX = Integer.MIN_VALUE;
    }
}
