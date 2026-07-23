package com.example.examplemod;

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
        if (!MatchSystem.isModEnabled || !MatchSystem.isMatchStarted || !MatchSystem.isRoundActive) return;
        if (event.getEntity() instanceof ServerPlayer victim) {
            UUID victimUUID = victim.getUUID();
            if (!MatchSystem.T_TEAM.contains(victimUUID) && !MatchSystem.CT_TEAM.contains(victimUUID)) return;

            DEAD_PLAYERS.add(victimUUID);
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
                if (killer.getUUID() != victimUUID) {
                    StatsManager.addKill(killer.getUUID());
                }
            }
            RoundManager.broadcastMessage("§7[MineStrike] " + victim.getGameProfile().getName() + " was killed!");
            checkWinConditions();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isMatchStarted) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        UUID uuid = player.getUUID();
        
        if (DEAD_PLAYERS.contains(uuid)) {
            player.getServer().execute(() -> {
                if (player.isAlive()) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255, false, false));
                }
            });
        }
    }

    public static void checkWinConditions() {
        int aliveT = 0, aliveCT = 0;
        for (UUID uuid : MatchSystem.T_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveT++;
        for (UUID uuid : MatchSystem.CT_TEAM) if (!DEAD_PLAYERS.contains(uuid)) aliveCT++;

        if (aliveT == 0 && !MatchSystem.T_TEAM.isEmpty()) {
            RoundManager.endRound("ct", "§bAll Terrorists eliminated! CT Win.");
        } else if (aliveCT == 0 && !MatchSystem.CT_TEAM.isEmpty()) {
            RoundManager.endRound("t", "§cAll Counter-Terrorists eliminated! T Win.");
        }
    }
}
