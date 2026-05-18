package com.velocity.core;

import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

/**
 * Custom 3D Structure ESP Renderer.
 *
 * Scans loaded chunks on the server-side integrated server (singleplayer)
 * and renders detailed wireframe bounding boxes for every structure piece.
 * Completely bypasses vanilla packet limitations!
 */
public class StructureDebugManager {
    public static boolean enabled = false;

    // Cache found structure pieces to eliminate frame-rate scans
    private static class CachedPiece {
        public final int minX, minY, minZ;
        public final int maxX, maxY, maxZ;
        public final String name;
        public final int color;

        public CachedPiece(BlockBox box, String name, int color) {
            this.minX = box.getMinX();
            this.minY = box.getMinY();
            this.minZ = box.getMinZ();
            this.maxX = box.getMaxX();
            this.maxY = box.getMaxY();
            this.maxZ = box.getMaxZ();
            this.name = name;
            this.color = color;
        }
    }

    private static final List<CachedPiece> cachedPieces = new ArrayList<>();
    private static int scanTickCounter = 0;
    private static BlockPos lastPlayerPos = BlockPos.ORIGIN;

    // Pre-allocated corner buffers (zero GC)
    private static final float[] SX = new float[8];
    private static final float[] SY = new float[8];
    private static final org.joml.Vector3f SCREEN = new org.joml.Vector3f();

    public static void tick() {
        // Ticking logic integrated into render method for frame/delta synchronization
    }

    /**
     * Renders 3D wireframe boxes for all cached structure pieces directly on the ImGui overlay.
     */
    public static void render(MinecraftClient client, ImDrawList drawList, float tickDelta) {
        if (!enabled) {
            cachedPieces.clear();
            return;
        }

        if (client.world == null || client.player == null) {
            cachedPieces.clear();
            return;
        }

        // Only accessible on Singleplayer / Integrated server where client holds local server authority
        net.minecraft.server.integrated.IntegratedServer server = client.getServer();
        if (server == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        scanTickCounter++;

        // Periodically rescan chunks (once per 40 frames, or if player travels more than 16 blocks)
        if (scanTickCounter >= 40 || playerPos.getSquaredDistance(lastPlayerPos) > 256.0) {
            scanTickCounter = 0;
            lastPlayerPos = playerPos;
            scanStructures(client, server);
        }

        if (cachedPieces.isEmpty()) return;

        double camX = client.gameRenderer.getCamera().getCameraPos().x;
        double camY = client.gameRenderer.getCamera().getCameraPos().y;
        double camZ = client.gameRenderer.getCamera().getCameraPos().z;

        for (CachedPiece piece : cachedPieces) {
            // Project 3D boundaries onto 2D screenspace
            boolean anyBehind = false;
            
            // 8 corners of the 3D block bounds (using +1 on Max to fully encompass block space)
            double x0 = piece.minX;
            double y0 = piece.minY;
            double z0 = piece.minZ;
            double x1 = piece.maxX + 1.0;
            double y1 = piece.maxY + 1.0;
            double z1 = piece.maxZ + 1.0;

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

            // Draw 12 bounding box edges (wireframe)
            drawList.addLine(SX[0], SY[0], SX[1], SY[1], piece.color, 1.5f);
            drawList.addLine(SX[1], SY[1], SX[2], SY[2], piece.color, 1.5f);
            drawList.addLine(SX[2], SY[2], SX[3], SY[3], piece.color, 1.5f);
            drawList.addLine(SX[3], SY[3], SX[0], SY[0], piece.color, 1.5f);

            drawList.addLine(SX[4], SY[4], SX[5], SY[5], piece.color, 1.5f);
            drawList.addLine(SX[5], SY[5], SX[6], SY[6], piece.color, 1.5f);
            drawList.addLine(SX[6], SY[6], SX[7], SY[7], piece.color, 1.5f);
            drawList.addLine(SX[7], SY[7], SX[4], SY[4], piece.color, 1.5f);

            drawList.addLine(SX[0], SY[0], SX[4], SY[4], piece.color, 1.5f);
            drawList.addLine(SX[1], SY[1], SX[5], SY[5], piece.color, 1.5f);
            drawList.addLine(SX[2], SY[2], SX[6], SY[6], piece.color, 1.5f);
            drawList.addLine(SX[3], SY[3], SX[7], SY[7], piece.color, 1.5f);
        }
    }

    /**
     * Intercepts server world chunks and parses structure starts directly.
     */
    private static void scanStructures(MinecraftClient client, net.minecraft.server.integrated.IntegratedServer server) {
        cachedPieces.clear();
        ServerWorld serverWorld = server.getWorld(client.world.getRegistryKey());
        if (serverWorld == null) return;

        BlockPos playerPos = client.player.getBlockPos();
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;
        int radius = 8; // Scan loaded chunks within 8-chunk radius

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = chunkX + dx;
                int cz = chunkZ + dz;
                if (!serverWorld.getChunkManager().isChunkLoaded(cx, cz)) continue;
                Chunk chunk = serverWorld.getChunk(cx, cz);
                if (chunk == null) continue;

                Map<net.minecraft.world.gen.structure.Structure, StructureStart> starts = chunk.getStructureStarts();
                for (Map.Entry<net.minecraft.world.gen.structure.Structure, StructureStart> entry : starts.entrySet()) {
                    StructureStart start = entry.getValue();
                    if (start == null || !start.hasChildren()) continue;

                    String structName = entry.getKey().toString().toLowerCase();
                    int color = getStructureColor(structName);

                    for (StructurePiece piece : start.getChildren()) {
                        BlockBox box = piece.getBoundingBox();
                        if (box != null) {
                            cachedPieces.add(new CachedPiece(box, structName, color));
                        }
                    }
                }
            }
        }
    }

    private static int getStructureColor(String name) {
        if (name.contains("stronghold")) {
            return ImGui.colorConvertFloat4ToU32(0.0f, 1.0f, 0.5f, 1.0f); // Bright Neon Green
        }
        if (name.contains("monument")) {
            return ImGui.colorConvertFloat4ToU32(0.0f, 0.7f, 1.0f, 1.0f); // Cyan
        }
        if (name.contains("fortress")) {
            return ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f); // Ruby Red
        }
        if (name.contains("mansion")) {
            return ImGui.colorConvertFloat4ToU32(0.7f, 0.4f, 0.1f, 1.0f); // Orange Brown
        }
        if (name.contains("mineshaft")) {
            return ImGui.colorConvertFloat4ToU32(0.8f, 0.8f, 0.0f, 0.5f); // Transparent Yellow
        }
        if (name.contains("outpost")) {
            return ImGui.colorConvertFloat4ToU32(0.6f, 0.6f, 0.6f, 1.0f); // Charcoal Gray
        }
        return ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 1.0f, 0.8f); // Default White
    }

    public static void reset() {
        cachedPieces.clear();
        scanTickCounter = 0;
    }
}
