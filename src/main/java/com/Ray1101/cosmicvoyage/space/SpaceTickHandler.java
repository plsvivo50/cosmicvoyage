package com.Ray1101.cosmicvoyage.space;

import com.Ray1101.cosmicvoyage.CosmicVoyage;
import com.Ray1101.cosmicvoyage.dimension.ModDimensions;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CosmicVoyage.MOD_ID)
public class SpaceTickHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();

        // 太空维度和月球维度
        if (level.dimension().equals(ModDimensions.SPACE)
                || level.dimension().equals(ModDimensions.MOON)) {

            // 无重力
            if (!player.isNoGravity()) {
                player.setNoGravity(true);
            }

            // 重置摔落距离（防摔落伤害）
            player.fallDistance = 0;

        } else {
            // 主世界等其他维度：恢复重力
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }
        }
    }

    // === Issue #9：虚空伤害拦截 ===
    // LivingAttackEvent 在 hurt() 最开头触发——客户端尚未收到受伤信号。
    // 取消后：无特效、无伤害、无网络包。LivingHurtEvent 会留下特效。
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        if (entity.level().isClientSide()) return;

        Level level = entity.level();
        boolean isSpaceDimension = level.dimension().equals(ModDimensions.SPACE)
                || level.dimension().equals(ModDimensions.MOON);
        if (!isSpaceDimension) return;

        // 只拦截虚空掉落伤害
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setCanceled(true);
        }
    }
}