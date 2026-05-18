package com.velocity.mixin;

import com.velocity.core.EspRenderer;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes GameRenderer.getFov(Camera, float, boolean) which is private in
 * 1.21.1.
 * Without this, client.gameRenderer.getFov(...) won't compile.
 *
 * Usage in EspRenderer.drawEsp():
 * float fov = (float) ((GameRendererAccessor)(Object) client.gameRenderer)
 * .invokeGetFov(camera, tickDelta, true);
 *
 * The (Object) intermediate cast is required because GameRenderer and
 * GameRendererAccessor don't share a class hierarchy — only a Mixin bridge.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Invoker("getFov")
    float invokeGetFov(Camera camera, float tickDelta, boolean changingFov);
}
