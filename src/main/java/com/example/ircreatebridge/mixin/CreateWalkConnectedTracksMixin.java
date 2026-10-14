package com.example.ircreatebridge.mixin;

import java.util.ArrayList;
import java.util.Collection;
import com.example.ircreatebridge.create.IrTrackGraphBridge;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ITrackBlock.class, remap = false)
public interface CreateWalkConnectedTracksMixin {
    @Inject(method = "walkConnectedTracks", at = @At("RETURN"), cancellable = true, remap = false)
    private static void bridgeAppendIrTracks(BlockGetter worldIn, TrackNodeLocation location, boolean linear, CallbackInfoReturnable<Collection<TrackNodeLocation.DiscoveredLocation>> cir) {
        Collection<TrackNodeLocation.DiscoveredLocation> base = cir.getReturnValue();
        if (base == null) {
            return;
        }

        if (!(worldIn instanceof net.minecraft.world.level.LevelAccessor accessor)) {
            return;
        }

        Collection<TrackNodeLocation.DiscoveredLocation> extra;
        try {
            extra = IrTrackGraphBridge.findIrConnections(accessor, location);
        } catch (Exception e) {
            return;
        }

        if (extra == null || extra.isEmpty()) {
            return;
        }

        ArrayList<TrackNodeLocation.DiscoveredLocation> merged = new ArrayList<>(base.size() + extra.size());
        merged.addAll(base);

        for (TrackNodeLocation.DiscoveredLocation dl : extra) {
            boolean dup = false;
            for (TrackNodeLocation.DiscoveredLocation e : merged) {
                if (e.equals(dl)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                merged.add(dl);
            }
        }
        cir.setReturnValue(merged);
    }
}
