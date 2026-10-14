package com.example.ircreatebridge.ir;

import java.util.List;
import com.example.ircreatebridge.BridgeConfig;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import trackapi.lib.Gauges;
import trackapi.lib.ITrackV2;
import trackapi.lib.PathingData;

public class CreateTrackAdapter implements ITrackV2 {
    private final Level level;
    private final BlockPos pos;

    public CreateTrackAdapter(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public static boolean isCreateTrack(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        if (pos.getY() < level.getMinBuildHeight() || pos.getY() > level.getMaxBuildHeight()) {
            return false;
        }

        BlockState state;
        try {
            state = level.getBlockState(pos);
        } catch (Exception e) {
            return false;
        }

        if (!(state.getBlock() instanceof ITrackBlock)) {
            return false;
        }

        try {
            if (state.hasProperty(TrackBlock.SHAPE)) {
                if (state.getValue(TrackBlock.SHAPE).isPortal()) {
                    return false;
                }
                if (state.getValue(TrackBlock.SHAPE) == com.simibubi.create.content.trains.track.TrackShape.NONE) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static double gaugeFor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        String name = state.getBlock().getClass().getName().toLowerCase(java.util.Locale.ROOT);
        String id = "";
        try {
            id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(java.util.Locale.ROOT);
        } catch (Exception e) {
            id = "";
        }
        String combined = name + " " + id;
        if (combined.contains("mono")) {
            try {
                return BridgeConfig.MONO_GAUGE_VALUE.get();
            } catch (Exception e) {
                return Gauges.MINECRAFT;
            }
        }
        if (combined.contains("narrow")) {
            try {
                return BridgeConfig.NARROW_GAUGE_VALUE.get();
            } catch (Exception e) {
                return Gauges.METER;
            }
        }
        if (combined.contains("wide")) {
            try {
                return BridgeConfig.WIDE_GAUGE_VALUE.get();
            } catch (Exception e) {
                return Gauges.RU_STANDARD;
            }
        }
        return Gauges.STANDARD;
    }

    public static CreateTrackAdapter find(Level level, Vec3 pos) {
        BlockPos bp = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        if (isCreateTrack(level, bp)) {
            return new CreateTrackAdapter(level, bp);
        }
        BlockPos up = bp.above();
        if (isCreateTrack(level, up)) {
            return new CreateTrackAdapter(level, up);
        }
        BlockPos down = bp.below();
        if (isCreateTrack(level, down)) {
            return new CreateTrackAdapter(level, down);
        }
        Vec3 upFuzz = pos.add(0.0, 0.4, 0.0);
        BlockPos bpu = new BlockPos((int) Math.floor(upFuzz.x), (int) Math.floor(upFuzz.y), (int) Math.floor(upFuzz.z));
        if (!bpu.equals(bp) && !bpu.equals(up) && isCreateTrack(level, bpu)) {
            return new CreateTrackAdapter(level, bpu);
        }
        Vec3 downFuzz = pos.add(0.0, -0.4, 0.0);
        BlockPos bpd = new BlockPos((int) Math.floor(downFuzz.x), (int) Math.floor(downFuzz.y), (int) Math.floor(downFuzz.z));
        if (!bpd.equals(bp) && !bpd.equals(down) && isCreateTrack(level, bpd)) {
            return new CreateTrackAdapter(level, bpd);
        }
        return null;
    }

    @Override
    public double[] getTrackGauges() {
        double g;
        try {
            g = gaugeFor(level, pos);
        } catch (Exception e) {
            g = Gauges.STANDARD;
        }
        return new double[] { g };
    }

    @Override
    public <D extends PathingData> void getNextPosition(D inputData, Vec3 motion, double gauge) {
        if (inputData == null || motion == null) {
            return;
        }
        double motionLen = motion.length();
        if (motionLen < 1.0E-9) {
            return;
        }
        Vec3 current = inputData.getPos();
        BlockState state;
        try {
            state = level.getBlockState(pos);
        } catch (Exception e) {
            return;
        }
        if (!(state.getBlock() instanceof ITrackBlock track)) {
            return;
        }
        if (tryBezier(inputData, current, motion, motionLen)) {
            return;
        }
        List<Vec3> axes;
        try {
            axes = track.getTrackAxes(level, pos, state);
        } catch (Exception e) {
            return;
        }
        if (axes == null || axes.isEmpty()) {
            return;
        }
        double head;
        try {
            head = BridgeConfig.CREATE_RAIL_HEAD_OFFSET.get();
        } catch (Exception e) {
            head = 0.125;
        }
        double elev;
        try {
            elev = track.getElevationAtCenter(level, pos, state);
        } catch (Exception e) {
            elev = 0.0;
        }
        Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0, elev + head, 0.0);
        Vec3 bestAxis = null;
        double bestScore = -1.0;
        for (Vec3 axis : axes) {
            if (axis == null) {
                continue;
            }
            if (axis.lengthSqr() < 1.0E-9) {
                continue;
            }
            Vec3 n = axis.normalize();
            double score = Math.abs(n.dot(motion.normalize()));
            if (score > bestScore) {
                bestScore = score;
                bestAxis = n;
            }
        }
        if (bestAxis == null) {
            return;
        }
        Vec3 toCur = current.subtract(center);
        double along = toCur.dot(bestAxis);
        Vec3 snapped = center.add(bestAxis.scale(along));
        boolean inverted = motion.dot(bestAxis) < 0.0;
        Vec3 step = bestAxis.scale(inverted ? -motionLen : motionLen);
        Vec3 next = snapped.add(step);
        inputData.setPos(next);
        try {
            inputData.setRoll(0.0);
        } catch (Exception e) {
        }
    }

    private boolean tryBezier(PathingData inputData, Vec3 current, Vec3 motion, double motionLen) {
        BlockEntity be;
        try {
            be = level.getBlockEntity(pos);
        } catch (Exception e) {
            return false;
        }
        if (!(be instanceof TrackBlockEntity tbe)) {
            return false;
        }
        java.util.Map<BlockPos, BezierConnection> cons;
        try {
            cons = tbe.getConnections();
        } catch (Exception e) {
            return false;
        }
        if (cons == null || cons.isEmpty()) {
            return false;
        }
        BezierConnection best = null;
        double bestT = 0.0;
        double bestDist = Double.MAX_VALUE;
        for (BezierConnection bc : cons.values()) {
            if (bc == null) {
                continue;
            }
            double len;
            try {
                len = bc.getLength();
            } catch (Exception e) {
                continue;
            }
            if (len < 1.0E-6 || len > 64.0) {
                continue;
            }
            int samples = 16;
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / (double) samples;
                Vec3 p;
                try {
                    p = bc.getPosition(t);
                } catch (Exception e) {
                    continue;
                }
                double d = p.distanceToSqr(current);
                if (d < bestDist) {
                    bestDist = d;
                    best = bc;
                    bestT = t;
                }
            }
        }
        if (best == null) {
            return false;
        }
        if (bestDist > 4.0) {
            return false;
        }
        double tEps = 0.02;
        Vec3 ahead;
        Vec3 behind;
        try {
            ahead = best.getPosition(Math.min(1.0, bestT + tEps));
            behind = best.getPosition(Math.max(0.0, bestT - tEps));
        } catch (Exception e) {
            return false;
        }
        Vec3 tangent = ahead.subtract(behind);
        if (tangent.lengthSqr() < 1.0E-9) {
            return false;
        }
        tangent = tangent.normalize();
        double sign = motion.dot(tangent) >= 0.0 ? 1.0 : -1.0;
        double len;
        try {
            len = best.getLength();
        } catch (Exception e) {
            return false;
        }
        if (len < 1.0E-6) {
            return false;
        }
        double newT;
        try {
            newT = best.incrementT(bestT, sign * motionLen / len);
        } catch (Exception e) {
            return false;
        }
        if (newT < 0.0) {
            newT = 0.0;
        }
        if (newT > 1.0) {
            newT = 1.0;
        }
        Vec3 next;
        try {
            next = best.getPosition(newT);
        } catch (Exception e) {
            return false;
        }
        double head;
        try {
            head = BridgeConfig.CREATE_RAIL_HEAD_OFFSET.get();
        } catch (Exception e) {
            head = 0.125;
        }
        next = next.add(0.0, head, 0.0);
        inputData.setPos(next);
        try {
            inputData.setRoll(0.0);
        } catch (Exception e) {
        }
        return true;
    }
}
