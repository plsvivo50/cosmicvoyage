package com.Ray1101.cosmicvoyage.command;

import com.Ray1101.cosmicvoyage.test.VT3FlightHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import com.Ray1101.cosmicvoyage.entity.ModEntities;
import com.Ray1101.cosmicvoyage.entity.ShipEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class VT1TestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cvtest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("vt1")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            // 传送到 (1,000,000, 200, 0)
                            player.teleportTo(1_000_000.5, 200.0, 0.5);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("VT-1: 已传送至 x=1,000,000.5, y=200, z=0.5"),
                                    true
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("vt3")
                        .then(Commands.literal("start")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    // 旁观模式，避免碰撞干扰这次基线测试
                                    player.setGameMode(GameType.CREATIVE);
                                    player.getAbilities().mayfly = true;
                                    player.getAbilities().flying = true;
                                    player.onUpdateAbilities();

                                    VT3FlightHandler.start(player);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("VT-3A: 高速飞行启动 | 500格/秒 | 旁观者模式 | 10秒自动停止"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("stop")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    VT3FlightHandler.stop(player);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("VT-3A: 已手动停止"),
                                            true
                                    );
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("ship")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Level level = player.level();
                            double px = player.getX();
                            double py = player.getY();
                            double pz = player.getZ();

                            // 召唤飞船
                            ShipEntity ship = new ShipEntity(ModEntities.SHIP.get(), level);
                            ship.setPos(px, py + 1, pz);
                            level.addFreshEntity(ship);

                            // ===== 生成临时参考平台（玻璃 11x11）=====
                            int platformY = (int) py - 8;
                            for (int dx = -5; dx <= 5; dx++) {
                                for (int dz = -5; dz <= 5; dz++) {
                                    // 棋盘格图案，方便判断运动
                                    var block = (Math.abs(dx) + Math.abs(dz)) % 2 == 0
                                            ? Blocks.GLASS.defaultBlockState()
                                            : Blocks.GLOWSTONE.defaultBlockState();

                                    level.setBlock(
                                            new BlockPos((int) px + dx, platformY, (int) pz + dz),
                                            block,
                                            3
                                    );
                                }
                            }

                            // 平台上放一个高塔作为方向参照
                            for (int dy = 0; dy < 5; dy++) {
                                level.setBlock(
                                        new BlockPos((int) px + 5, platformY + 1 + dy, (int) pz + 5),
                                        Blocks.REDSTONE_BLOCK.defaultBlockState(),
                                        3
                                );
                            }

                            context.getSource().sendSuccess(
                                    () -> Component.literal("§b[CosmicVoyage] Ship spawned! Platform below. Controls: WASD + Space/Ctrl. Shift to dismount."),
                                    true
                            );
                            return 1;
                        })
                )
        );
    }
}