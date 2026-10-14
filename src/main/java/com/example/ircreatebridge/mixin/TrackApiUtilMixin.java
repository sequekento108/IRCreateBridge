package com.example.ircreatebridge.mixin;

import com.example.ircreatebridge.BridgeConfig;
import com.example.ircreatebridge.ir.CreateTrackAdapter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trackapi.lib.ITrack;

@Mixin(value = trackapi.lib.Util.class, remap = false)
public class TrackApiUtilMixin {
    @Inject(method = "findTrackBlocks", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bridgeFindCreateTrack(Level world, Vec3 pos, boolean acceptMinecraftRails, Class<?> type, CallbackInfoReturnable<Object> cir) {
        Object existing = cir.getReturnValue();
        if (existing != null) {
            return;
        }

        if (type == null) {
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

        if (!ITrack.class.isAssignableFrom(type)) {
            return;
        }

        if (world == null || pos == null) {
            return;
        }

        CreateTrackAdapter adapter;
        try {
            adapter = CreateTrackAdapter.find(world, pos);
        } catch (Exception e) {
            return;
        }

        if (adapter == null) {
            return;
        }

        try {
            if (!type.isInstance(adapter)) {
                return;
            }
            cir.setReturnValue(type.cast(adapter));
        } catch (Exception e) {
        }
    }
}
