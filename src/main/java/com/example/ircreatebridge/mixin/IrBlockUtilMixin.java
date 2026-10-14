package com.example.ircreatebridge.mixin;

import com.simibubi.create.content.trains.track.ITrackBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

@Mixin(value = cam72cam.immersiverailroading.util.BlockUtil.class, remap = false)
public class IrBlockUtilMixin {
    @Inject(method = "isIRRail", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bridgeTreatCreateAsRail(World world, Vec3i pos, CallbackInfoReturnable<Boolean> cir) {
        try {
            Boolean existing = cir.getReturnValue();
            if (existing != null && existing.booleanValue()) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (world == null || pos == null) {
            return;
        }
        try {
            if (world.internal == null) {
                return;
            }
            BlockState state = world.internal.getBlockState(pos.internal());
            if (state.getBlock() instanceof ITrackBlock) {
                cir.setReturnValue(Boolean.TRUE);
            }
        } catch (Exception e) {
        }
    }
}
