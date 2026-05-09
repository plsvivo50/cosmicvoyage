package com.Ray1101.cosmicvoyage;

import com.Ray1101.cosmicvoyage.entity.ModEntities;
import com.Ray1101.cosmicvoyage.network.CosmicVoyagePacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.Ray1101.cosmicvoyage.command.CvSpaceCommand;
import com.Ray1101.cosmicvoyage.command.VT1TestCommand;  // ← 新增导入
import org.slf4j.Logger;
import net.minecraftforge.common.MinecraftForge;

@Mod(CosmicVoyage.MOD_ID)
public class CosmicVoyage {

    public static final String MOD_ID = "cosmicvoyage";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CosmicVoyage(FMLJavaModLoadingContext context) {

        IEventBus modEventBus = context.getModEventBus();

        ModEntities.register(modEventBus);

        CosmicVoyagePacketHandler.register();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[CosmicVoyage] Core initialized");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CvSpaceCommand.register(event.getDispatcher());
        VT1TestCommand.register(event.getDispatcher());  // ← 新增这一行
    }
}