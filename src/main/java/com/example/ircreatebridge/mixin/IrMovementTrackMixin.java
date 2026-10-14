package com.example.ircreatebridge.mixin;

import com.example.ircreatebridge.BridgeConfig;
import com.example.ircreatebridge.ir.CreateTrackAdapter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import cam72cam.immersiverailroading.util.VecUtil;

@Mixin(value = cam72cam.immersiverailroading.physics.MovementTrack.class, remap = false)
public class IrMovementTrackMixin {
    @Inject(method = "findTrack", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bridgeFindCreateIgnoringGauge(World world, Vec3d currentPosition, float trainYaw, double gauge, CallbackInfoReturnable<cam72cam.immersiverailroading.thirdparty.trackapi.ITrack> cir) {
        try {
            if (cir.getReturnValue() != null) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (world == null || currentPosition == null) {
            return;
        }
        boolean enabled = true;
        try {
            enabled = BridgeConfig.ENABLE_IR_ON_CREATE.get();
        } catch (Exception e) {
            enabled = true;
        }
        if (!enabled) {
            return;
        }
        Level vanilla;
        try {
            vanilla = world.internal;
        } catch (Exception e) {
            return;
        }
        if (vanilla == null) {
            return;
        }
        Vec3d[] candidates;
        try {
            candidates = new Vec3d[] {
                currentPosition,
                currentPosition.add(VecUtil.fromWrongYaw(1.0, trainYaw)),
                currentPosition.add(VecUtil.fromWrongYaw(-1.0, trainYaw))
            };
        } catch (Exception e) {
            candidates = new Vec3d[] { currentPosition };
        }
        for (Vec3d cand : candidates) {
            if (cand == null) {
                continue;
            }
            Vec3 vanillaPos;
            try {
                vanillaPos = cand.internal();
            } catch (Exception e) {
                continue;
            }
            CreateTrackAdapter adapter;
            try {
                adapter = CreateTrackAdapter.find(vanilla, vanillaPos);
            } catch (Exception e) {
                continue;
            }
            if (adapter == null) {
                continue;
            }
            try {
                cam72cam.immersiverailroading.thirdparty.trackapi.ITrack wrapped = cam72cam.immersiverailroading.thirdparty.trackapi.ITrack.from(adapter);
                if (wrapped != null) {
                    cir.setReturnValue(wrapped);
                    return;
                }
            } catch (Exception e) {
            }
        }
    }
}
