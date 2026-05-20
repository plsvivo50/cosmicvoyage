package com.Ray1101.cosmicvoyage.network;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络包注册中心 — 只负责 SimpleChannel 管理和包注册。
 *
 * <p>职责边界：
 *   - 只负责 {@link SimpleChannel} 的创建和消息注册
 *   - 不处理任何 Packet 的编解码或业务逻辑
 *   - 每个 Packet 的 encode/decode/handle 自包含在各自文件中（P0-5）
 *
 * <p>Phase 2：新增 MarsLandingPacket 和 MarsEscapePacket 注册。
 */
public class CosmicVoyagePacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CosmicVoyage.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
                packetId++,
                ShipSyncPacket.class,
                ShipSyncPacket::encode,
                ShipSyncPacket::decode,
                ShipSyncPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                MoonTransitionPacket.class,
                MoonTransitionPacket::encode,
                MoonTransitionPacket::decode,
                MoonTransitionPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                LaunchToSpacePacket.class,
                LaunchToSpacePacket::encode,
                LaunchToSpacePacket::decode,
                LaunchToSpacePacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                LandOnEarthPacket.class,
                LandOnEarthPacket::encode,
                LandOnEarthPacket::decode,
                LandOnEarthPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                ReturnToSpacePacket.class,
                ReturnToSpacePacket::encode,
                ReturnToSpacePacket::decode,
                ReturnToSpacePacket::handle
        );

        // Phase 2：火星着陆 + 返回
        INSTANCE.registerMessage(
                packetId++,
                MarsLandingPacket.class,
                MarsLandingPacket::encode,
                MarsLandingPacket::decode,
                MarsLandingPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                MarsEscapePacket.class,
                MarsEscapePacket::encode,
                MarsEscapePacket::decode,
                MarsEscapePacket::handle
        );
    }
}