package com.example.ircreatebridge.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.example.ircreatebridge.BridgeConfig;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackPropagator;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class IrTrackGraphBridge {
    public static boolean isIrRail(LevelAccessor accessor, BlockPos pos) {
        if (accessor == null || pos == null) {
            return false;
        }
        if (!(accessor instanceof Level level)) {
            return false;
        }
        BlockState state;
        try {
            state = level.getBlockState(pos);
        } catch (Exception e) {
            return false;
        }
        if (state.getBlock() instanceof ITrackBlock) {
            return false;
        }
        if (state.isAir()) {
            return false;
        }
        BlockEntity be;
        try {
            be = level.getBlockEntity(pos);
        } catch (Exception e) {
            return false;
        }
        return be instanceof trackapi.lib.ITrackV2;
    }

    public static Vec3 irCenter(LevelAccessor accessor, BlockPos pos) {
        double head = 0.3;
        try {
            head = BridgeConfig.IR_RAIL_HEAD_OFFSET.get();
        } catch (Exception e) {
            head = 0.3;
        }
        return Vec3.atBottomCenterOf(pos).add(0.0, head, 0.0);
    }

    public static Vec3 estimateDirection(LevelAccessor accessor, BlockPos pos) {
        boolean x = isIrRail(accessor, pos.east()) || isIrRail(accessor, pos.west());
        boolean z = isIrRail(accessor, pos.south()) || isIrRail(accessor, pos.north());
        boolean pd = isIrRail(accessor, pos.east().south()) || isIrRail(accessor, pos.west().north());
        boolean nd = isIrRail(accessor, pos.west().south()) || isIrRail(accessor, pos.east().north());
        if (x && !z && !pd && !nd) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        if (z && !x && !pd && !nd) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        if (pd && !nd) {
            return new Vec3(1.0, 0.0, 1.0).normalize();
        }
        if (nd && !pd) {
            return new Vec3(-1.0, 0.0, 1.0).normalize();
        }
        if (x) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        if (z) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        if (pd) {
            return new Vec3(1.0, 0.0, 1.0).normalize();
        }
        if (nd) {
            return new Vec3(-1.0, 0.0, 1.0).normalize();
        }
        return new Vec3(1.0, 0.0, 0.0);
    }

    public static Collection<TrackNodeLocation.DiscoveredLocation> findIrConnections(LevelAccessor accessor, TrackNodeLocation location) {
        List<TrackNodeLocation.DiscoveredLocation> out = new ArrayList<>();
        if (accessor == null || location == null) {
            return out;
        }
        boolean enabled = true;
        try {
            enabled = BridgeConfig.ENABLE_CREATE_ON_IR.get();
        } catch (Exception e) {
            enabled = true;
        }
        if (!enabled) {
            return out;
        }
        ResourceKey<Level> dim;
        if (accessor instanceof Level level) {
            dim = level.dimension();
        } else {
            return out;
        }
        for (BlockPos bp : location.allAdjacent()) {
            if (!isIrRail(accessor, bp)) {
                continue;
            }
            Vec3 center = irCenter(accessor, bp);
            Vec3 dir = estimateDirection(accessor, bp);
            TrackNodeLocation.DiscoveredLocation dl = new TrackNodeLocation.DiscoveredLocation(dim, center);
            dl.withNormal(new Vec3(0.0, 1.0, 0.0));
            dl.withDirection(dir);
            dl.materials(TrackMaterial.ANDESITE, TrackMaterial.ANDESITE);
            dl.forceNode();
            boolean dup = false;
            for (TrackNodeLocation.DiscoveredLocation e : out) {
                if (e.equals(dl)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                out.add(dl);
            }
            if (out.size() >= 8) {
                break;
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        boolean enabled = true;
        try {
            enabled = BridgeConfig.ENABLE_CREATE_ON_IR.get();
        } catch (Exception e) {
            enabled = true;
        }
        if (!enabled) {
            return;
        }
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        boolean relevant = isIrRail(level, pos);
        if (!relevant) {
            for (BlockPos n : event.getNotifiedSides().stream().map(d -> pos.relative(d)).toList()) {
                if (isIrRail(level, n)) {
                    relevant = true;
                    break;
                }
            }
        }
        if (!relevant) {
            return;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos bp = pos.offset(dx, dy, dz);
                    BlockState st;
                    try {
                        st = level.getBlockState(bp);
                    } catch (Exception e) {
                        continue;
                    }
                    if (st.getBlock() instanceof ITrackBlock) {
                        try {
                            TrackPropagator.onRailAdded(level, bp, st);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }
}
