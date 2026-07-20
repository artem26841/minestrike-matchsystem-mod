package com.example.matchsystem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "matchsystem")
public class RoundTimerHandler {
    private static int tickCounter = 0;
    public static int roundTimeLeft = 115;
    public static int freezeTimeLeft = 15;
    public static boolean isFreezePeriod = true;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!MatchSystem.isModEnabled || MatchSystem.isPaused || !MatchSystem.isRoundActive) return;
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                secondsTick();
            }
        }
    }

    private static void secondsTick() {
        if (isFreezePeriod) {
            if (freezeTimeLeft > 0) {
                freezeTimeLeft--;
                broadcastActionBar("§eВремя до старта: §c" + freezeTimeLeft + " сек");
            } else {
                isFreezePeriod = false;
                RoundManager.broadcastMessage("§c§lРАУНД НАЧАЛСЯ!");
            }
            return;
        }

        if (roundTimeLeft > 0) {
            roundTimeLeft--;
            String timerColor = roundTimeLeft <= 10 ? "§c" : "§a";
            broadcastActionBar("§cRED " + MatchSystem.redPoints + " §7| " + timerColor + formatTime(roundTimeLeft) + " §7| §b" + MatchSystem.bluePoints + " BLUE");
        } else {
            RoundManager.endRound("blue", "§bВремя вышло! Победили Синие.");
        }
    }

    @SubscribeEvent
    public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!MatchSystem.isModEnabled || !MatchSystem.isRoundActive) return;
        UUID uuid = event.getEntity().getUUID();
        if (MatchSystem.RED_TEAM.contains(uuid) || MatchSystem.BLUE_TEAM.contains(uuid)) {
            MatchSystem.isPaused = true;
            RoundManager.broadcastMessage("§c[MineStrike] Игрок " + event.getEntity().getGameProfile().getName() + " вышел. Пауза!");
        }
    }

    private static void broadcastActionBar(String text) {
        RoundManager.getMatchPlayers().forEach(p -> p.sendSystemMessage(Component.literal(text), true));
    }

    private static String formatTime(int secs) {
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }
}
