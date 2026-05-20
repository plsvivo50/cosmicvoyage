package com.Ray1101.cosmicvoyage.space;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 太空运行时状态管理 — 内存级玩家太空状态记录
 *
 * <p>职责边界：
 *   - 只负责内存中记录哪些玩家当前在太空维度
 *   - 不处理持久化、不处理维度切换逻辑
 *   - 持久化锚点由 SpaceData（SavedData）负责
 *
 * <p>内存泄漏防护：
 *   - 订阅 PlayerLoggedOutEvent，玩家下线时自动清理
 *   - 避免崩溃/断线导致 UUID 永久残留
 */
@Mod.EventBusSubscriber(modid = "cosmicvoyage", value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpaceState {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 记录"在太空中的玩家"（内存级，不持久化）
    private static final Set<UUID> IN_SPACE = new HashSet<>();

    public static void enterSpace(UUID uuid) {
        IN_SPACE.add(uuid);
    }

    public static void exitSpace(UUID uuid) {
        IN_SPACE.remove(uuid);
    }

    public static boolean isInSpace(UUID uuid) {
        return IN_SPACE.contains(uuid);
    }

    /**
     * 玩家下线时自动清理其在太空的状态记录，防止内存泄漏。
     *
     * <p>覆盖场景：崩溃、断线、强制关游戏 — 这些情况不会调用 exitSpace()。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        if (IN_SPACE.remove(uuid)) {
            LOGGER.debug("[SpaceState] Player {} logged out, cleaned up IN_SPACE entry", uuid);
        }
    }
}