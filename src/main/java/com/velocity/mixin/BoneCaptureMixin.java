package com.velocity.mixin;

import com.velocity.core.BoneDataCache;
import com.velocity.core.BoneDataCache.BoneSnapshot;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(LivingEntityRenderer.class)
public abstract class BoneCaptureMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Shadow protected M model;

    public record EntityContext(int id, float height, boolean swimming, boolean gliding) {}
    private static final WeakHashMap<LivingEntityRenderState, EntityContext> stateContextMap = new WeakHashMap<>();

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        stateContextMap.put(state, new EntityContext(entity.getId(), entity.getHeight(), entity.isSwimming(), false));
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Ljava/lang/Object;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRender(LivingEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState, CallbackInfo ci) {
        if (!(this.model instanceof BipedEntityModel<?> biped)) return;

        EntityContext ctx = stateContextMap.get(state);
        if (ctx == null) return;

        BoneDataCache.BoneSnapshot snap = new BoneDataCache.BoneSnapshot(
            copyBone(biped.head),
            copyBone(biped.body),
            copyBone(biped.rightArm),
            copyBone(biped.leftArm),
            copyBone(biped.rightLeg),
            copyBone(biped.leftLeg),
            state.x, state.y, state.z,
            state.bodyYaw,
            ctx.height(),
            state.sneaking,
            ctx.swimming(),
            ctx.gliding()
        );

        BoneDataCache.cache.put(ctx.id(), snap);
    }

    private static BoneDataCache.Bone copyBone(net.minecraft.client.model.ModelPart part) {
        try {
            float px = getField(part, "originX", "field_3657", "pivotX", "x", "translateX", "offsetX");
            float py = getField(part, "originY", "field_3656", "pivotY", "y", "translateY", "offsetY");
            float pz = getField(part, "originZ", "field_3655", "pivotZ", "z", "translateZ", "offsetZ");
            
            float rx = getField(part, "pitch", "field_3654", "rx", "rotateAngleX");
            float ry = getField(part, "yaw", "field_3675", "ry", "rotateAngleY");
            float rz = getField(part, "roll", "field_3674", "rz", "rotateAngleZ");
            
            return new BoneDataCache.Bone(new Vector3f(px, py, pz), new Vector3f(rx, ry, rz));
        } catch (Exception e) {
            return new BoneDataCache.Bone(new Vector3f(), new Vector3f());
        }
    }

    private static float getField(Object obj, String... names) throws Exception {
        for (String name : names) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(name);
                f.setAccessible(true);
                return (float) f.get(obj);
            } catch (Exception ignored) {}
        }
        for (java.lang.reflect.Field f : obj.getClass().getFields()) {
            if (f.getType() == float.class) {
                // If we reach here, we'll just return 0, but this is for debugging
            }
        }
        return 0f;
    }
}
