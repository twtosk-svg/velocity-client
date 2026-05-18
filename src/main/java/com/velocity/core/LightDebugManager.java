package com.velocity.core;

import com.velocity.config.ConfigManager;
import com.velocity.mixin.DebugRendererAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.debug.LightDebugRenderer;
import net.minecraft.client.render.debug.SkyLightDebugRenderer;
import net.minecraft.world.LightType;

/**
 * Manages toggling MC's built-in light debug renderers from our mod's menu.
 *
 * Works by injecting/removing the renderers from MC's DebugRenderer.renderers
 * list, and bumping the version counter to trigger a re-init on next frame.
 */
public class LightDebugManager {

    // ── Settings (auto-persisted by ConfigManager) ───────────────────────────
    public static boolean skyLightEnabled = false;
    public static boolean blockLightEnabled = false;
    public static boolean skyLightSectionsEnabled = false;

    // ── Track previous state for edge detection ──────────────────────────────
    private static boolean prevSkyLight = false;
    private static boolean prevBlockLight = false;
    private static boolean prevSkyLightSections = false;

    // ── Cached renderer instances ────────────────────────────────────────────
    private static SkyLightDebugRenderer skyLightRenderer = null;
    private static LightDebugRenderer lightSectionsRenderer = null;

    /**
     * Called every frame from EspRenderer.render().
     * Checks if toggles changed and injects/removes renderers accordingly.
     */
    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        // Sky Light / Block Light levels (combined renderer)
        boolean skyOrBlockChanged = (skyLightEnabled != prevSkyLight) || (blockLightEnabled != prevBlockLight);
        if (skyOrBlockChanged) {
            DebugRenderer debugRenderer = client.worldRenderer.debugRenderer;
            DebugRendererAccessor acc = (DebugRendererAccessor) debugRenderer;

            // Remove old instance
            if (skyLightRenderer != null) {
                acc.getRenderers().remove(skyLightRenderer);
                skyLightRenderer = null;
            }

            // Add new if either is on
            if (skyLightEnabled || blockLightEnabled) {
                skyLightRenderer = new SkyLightDebugRenderer(client, blockLightEnabled, skyLightEnabled);
                acc.getRenderers().add(skyLightRenderer);
            }

            prevSkyLight = skyLightEnabled;
            prevBlockLight = blockLightEnabled;
        }

        // Sky Light Sections (separate renderer)
        if (skyLightSectionsEnabled != prevSkyLightSections) {
            DebugRenderer debugRenderer = client.worldRenderer.debugRenderer;
            DebugRendererAccessor acc = (DebugRendererAccessor) debugRenderer;

            if (lightSectionsRenderer != null) {
                acc.getRenderers().remove(lightSectionsRenderer);
                lightSectionsRenderer = null;
            }

            if (skyLightSectionsEnabled) {
                lightSectionsRenderer = new LightDebugRenderer(client, LightType.SKY);
                acc.getRenderers().add(lightSectionsRenderer);
            }

            prevSkyLightSections = skyLightSectionsEnabled;
        }
    }

    /** Called on world change / disconnect to clean up. */
    public static void reset() {
        skyLightRenderer = null;
        lightSectionsRenderer = null;
        prevSkyLight = false;
        prevBlockLight = false;
        prevSkyLightSections = false;
    }
}
