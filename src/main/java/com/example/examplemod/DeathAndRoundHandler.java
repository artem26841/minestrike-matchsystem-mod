package com.example.matchsystem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "matchsystem")
public class DeathAndRoundHandler {
    public static final Set<UUID> DEAD_PLAYERS = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isRoundActive) return;
        if (event.getEntity() instanceof ServerPlayer victim) {
            UUID victimUUID = victim.getUUID();
            if (!MatchSystem.RED_TEAM.contains(victimUUID) && !MatchSystem.BLUE_TEAM.contains(victimUUID)) return;

            DEAD_PLAYERS.add(victimUUID);
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                if (killer.getUUID() != victimUUID) {
                    StatsManager.addKill(killer.getUUID());
                }
            }
            RoundManager.broadcastMessage("§7[MineStrike] Игрок " + victim.getGameProfile().getName() + " погиб!");
            checkWinConditions();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!MatchSystem.isModEnabled) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (DEAD_PLAYERS.contains(player.getUUID())) {
            player.setGameMode(GameType.SPECTATOR);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255, false, false));
        }
    }

    public static void checkWinConditions() {
        int aliveRed = 0, aliveBlue = 0;
        for (UUID uuid : MatchSystem.RED_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveRed++;
        for (UUID uuid : MatchSystem.BLUE_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveBlue++;

        if (aliveRed == 0 && !MatchSystem.RED_TEAM.isEmpty()) {
            RoundManager.endRound("blue", "§bВсе Красные уничтожены! Победили Синие.");
        } else if (aliveBlue == 0 && !MatchSystem.BLUE_TEAM.isEmpty()) {
            RoundManager.endRound("red", "§cВсе Синие уничтожены! Победили Красные.");
        }
    }
}
