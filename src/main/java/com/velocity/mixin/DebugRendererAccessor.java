package com.velocity.mixin;

import net.minecraft.client.render.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Accessor mixin for DebugRenderer — exposes the internal renderers list
 * and version counter so we can inject our light debug renderers.
 */
@Mixin(DebugRenderer.class)
public interface DebugRendererAccessor {

    @Accessor("renderers")
    List<DebugRenderer.Renderer> getRenderers();

    @Accessor("currentVersion")
    long getCurrentVersion();

    @Accessor("currentVersion")
    void setCurrentVersion(long version);
}
