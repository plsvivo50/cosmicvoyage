package com.Ray1101.cosmicvoyage.command;

import com.Ray1101.cosmicvoyage.data.SpaceData;
import com.Ray1101.cosmicvoyage.space.SpaceState;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import net.minecraft.server.level.ServerLevel;

public class CvSpaceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("cvspace")
                        .requires(source -> source.hasPermission(2))

                        // =====================
                        // 根命令：状态输出
                        // =====================
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                    () -> Component.literal("§b[CosmicVoyage] Space system ready."),
                                    false
                            );
                            return 1;
                        })

                        // =====================
                        // 子命令：tp测试
                        // =====================
                        .then(Commands.literal("tp")
                                .executes(ctx -> {

                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    if (player == null) return 0;

                                    // 🌌 1. 标记进入太空
                                    SpaceState.enterSpace(player.getUUID());

                                    // 🚀 2. 传送到“临时宇宙点”
                                    player.teleportTo(
                                            player.serverLevel(),
                                            0,
                                            200,   // 高空代表“宇宙层”
                                            0,
                                            player.getYRot(),
                                            player.getXRot()
                                    );

                                    // 💬 3. 反馈
                                    player.sendSystemMessage(
                                            Component.literal("§b[CosmicVoyage] You have entered space space-state.")
                                    );

                                    return 1;
                                })
                        )
                        .then(Commands.literal("enter")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ServerLevel spaceLevel = player.server.getLevel(ModDimensions.SPACE);

                                    if (spaceLevel == null) {
                                        context.getSource().sendFailure(Component.literal("§cSpace dimension not loaded! Is the JSON valid?"));
                                        return 0;
                                    }

                                    if (player.level().dimension().equals(ModDimensions.SPACE)) {
                                        context.getSource().sendFailure(Component.literal("§cYou are already in space."));
                                        return 0;
                                    }

                                    // 保存返回锚点
                                    SpaceData.get(player.serverLevel()).setAnchor(player.getX(), player.getY(), player.getZ());

                                    // 传送到太空维度 (0, 200, 0)
                                    player.teleportTo(spaceLevel, 0, 200, 0, player.getYRot(), player.getXRot());
                                    SpaceState.enterSpace(player.getUUID());

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§b[CosmicVoyage] Entered space. Look up."),
                                            true
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("return")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    if (!player.level().dimension().equals(ModDimensions.SPACE)) {
                                        context.getSource().sendFailure(Component.literal("§cYou are not in space."));
                                        return 0;
                                    }

                                    var data = SpaceData.get(player.server.overworld());
                                    if (!data.hasAnchor()) {
                                        context.getSource().sendFailure(Component.literal("§cNo return anchor found."));
                                        return 0;
                                    }

                                    player.teleportTo(
                                            player.server.overworld(),
                                            data.getX(), data.getY(), data.getZ(),
                                            player.getYRot(), player.getXRot()
                                    );
                                    SpaceState.exitSpace(player.getUUID());

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§b[CosmicVoyage] Returned to overworld."),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(
                                Commands.literal("setanchor")
                                        .executes(context -> {

                                            var player = context.getSource().getPlayerOrException();
                                            var level = player.serverLevel();

                                            var data = SpaceData.get(level);

                                            data.setAnchor(player.getX(), player.getY(), player.getZ());

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("§aAnchor set!"),
                                                    false
                                            );

                                            return 1;
                                        })
                        )

                        .then(
                                Commands.literal("warp")
                                        .executes(context -> {

                                            var player = context.getSource().getPlayerOrException();
                                            var level = player.serverLevel();
                                            var data = SpaceData.get(level);

                                            if (!data.hasAnchor()) {
                                                context.getSource().sendFailure(
                                                        Component.literal("§cNo anchor set!")
                                                );
                                                return 0;
                                            }

                                            player.connection.teleport(
                                                    data.getX(),
                                                    data.getY(),
                                                    data.getZ(),
                                                    player.getYRot(),
                                                    player.getXRot()
                                            );

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("§bWarped to anchor."),
                                                    false
                                            );

                                            return 1;
                                        })
                        )
        );
    }
}