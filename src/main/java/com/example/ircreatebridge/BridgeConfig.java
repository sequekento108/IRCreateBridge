package com.example.ircreatebridge;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BridgeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_IR_ON_CREATE;
    public static final ModConfigSpec.BooleanValue ENABLE_CREATE_ON_IR;
    public static final ModConfigSpec.DoubleValue CREATE_RAIL_HEAD_OFFSET;
    public static final ModConfigSpec.DoubleValue IR_RAIL_HEAD_OFFSET;
    public static final ModConfigSpec.DoubleValue NARROW_GAUGE_VALUE;
    public static final ModConfigSpec.DoubleValue WIDE_GAUGE_VALUE;
    public static final ModConfigSpec.DoubleValue MONO_GAUGE_VALUE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("bridge");
        ENABLE_IR_ON_CREATE = builder.define("enableIrOnCreate", true);
        ENABLE_CREATE_ON_IR = builder.define("enableCreateOnIr", true);
        CREATE_RAIL_HEAD_OFFSET = builder.defineInRange("createRailHeadOffset", 0.125, -1.0, 1.0);
        IR_RAIL_HEAD_OFFSET = builder.defineInRange("irRailHeadOffset", 0.3, -1.0, 2.0);
        NARROW_GAUGE_VALUE = builder.defineInRange("narrowGauge", 1.0, 0.3, 3.0);
        WIDE_GAUGE_VALUE = builder.defineInRange("wideGauge", 1.524, 0.3, 3.0);
        MONO_GAUGE_VALUE = builder.defineInRange("monoGauge", 0.632, 0.3, 3.0);
        builder.pop();
        SPEC = builder.build();
    }
}
