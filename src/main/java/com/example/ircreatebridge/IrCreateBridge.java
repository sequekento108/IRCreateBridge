package com.example.ircreatebridge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import com.example.ircreatebridge.create.IrTrackGraphBridge;

@Mod(IrCreateBridge.MOD_ID)
public class IrCreateBridge {
    public static final String MOD_ID = "ircreatebridge";

    public IrCreateBridge(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, BridgeConfig.SPEC);
        NeoForge.EVENT_BUS.register(IrTrackGraphBridge.class);
    }
}
