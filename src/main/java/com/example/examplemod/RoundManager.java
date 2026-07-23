package com.example.examplemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RoundManager {
    public static List<ServerPlayer> getMatchPlayers() {
        List<ServerPlayer> list = new ArrayList<>();
        if (ServerLifecycleHooks.getCurrentServer() == null) return list;
        for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (MatchSystem.T_TEAM.contains(p.getUUID()) || MatchSystem.CT_TEAM.contains(p.getUUID())) list.add(p);
        }
        return list;
    }

    public static void togglePause() {
        MatchSystem.isPaused = !MatchSystem.isPaused;
        if (MatchSystem.isPaused) {
            broadcastMessage("§c[MineStrike] MATCH PAUSED! Blindness applied.");
            for (ServerPlayer player : getMatchPlayers()) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 999999, 255, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255, false, false));
            }
        } else {
            broadcastMessage("§a[MineStrike] MATCH RESUMED! Fight!");
            for (ServerPlayer player : getMatchPlayers()) {
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
    }

    public static void endRound(String winningTeam, String endReason) {
        MatchSystem.isRoundActive = false;
        String titleText = "";
        int fireworkColor = 0; // 0 = Красный (Т), 1 = Синий (СТ)

        if (winningTeam.equalsIgnoreCase("t")) {
            MatchSystem.tPoints++;
            titleText = "§c§lTERRORISTS WIN";
            fireworkColor = 0;
        } else if (winningTeam.equalsIgnoreCase("ct")) {
            MatchSystem.ctPoints++;
            titleText = "§b§lCOUNTER-TERRORISTS WIN";
            fireworkColor = 1;
        }

        StatsManager.saveStatsToFile();
        broadcastMessage("§6=================================\n§e§lROUND OVER!\n" + endReason + "\n§7Score: §cT " + MatchSystem.tPoints + " §7| §b" + MatchSystem.ctPoints + " CT\n§6=================================");

        // ВНЕДРЕНО: Вывод огромных надписей TITLE на весь экран
        sendBigTitle(titleText, "§fScore: §c" + MatchSystem.tPoints + " §7- §b" + MatchSystem.ctPoints);

        // ВНЕДРЕНО: Спавн праздничных салютов над победителями
        spawnVictoryFireworks(winningTeam, fireworkColor);

        // Проверка лимита победных очков матча
        if (MatchSystem.tPoints >= MatchSystem.configMaxPoints) {
            sendBigTitle("§c§l🏆 TERRORISTS 🏆", "§fWON THE ENTIRE MATCH!");
            forceStopMatch();
            return;
        } else if (MatchSystem.ctPoints >= MatchSystem.configMaxPoints) {
            sendBigTitle("§b§l🏆 COUNTER-TERRORISTS 🏆", "§fWON THE ENTIRE MATCH!");
            forceStopMatch();
            return;
        }

        if (MatchSystem.isAutoMode) {
            RoundTimerHandler.freezeTimeLeft = MatchSystem.configFreezeTime;
            RoundTimerHandler.isFreezePeriod = true;
            startNewRound();
        }
    }

    // Метод генерации салютов над игроками победной команды
    private static void spawnVictoryFireworks(String team, int colorType) {
        Set<UUID> targetTeam = team.equalsIgnoreCase("t") ? MatchSystem.T_TEAM : MatchSystem.CT_TEAM;
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        
        for (UUID uuid : targetTeam) {
            ServerPlayer p = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
            if (p != null && p.isAlive()) {
                p.getServer().execute(() -> {
                    ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
                    CompoundTag tag = new CompoundTag();
                    CompoundTag fireworks = new CompoundTag();
                    ListTag explosions = new ListTag();
                    CompoundTag explosion = new CompoundTag();
                    
                    explosion.putByte("Type", (byte) 1); // Большой взрыв
                    explosion.putIntArray("Colors", new int[]{colorType == 0 ? 0xFF0000 : 0x0000FF}); // Красный или Синий
                    explosion.putByte("Flicker", (byte) 1); // Мерцание
                    
                    explosions.add(explosion);
                    fireworks.put("Explosions", explosions);
                    fireworks.putByte("Flight", (byte) 1); // Высота полета
                    tag.put("Fireworks", fireworks);
                    fireworkStack.setTag(tag);

                    // Спавним сущность салюта прямо на позиции игрока
                    FireworkRocketEntity rocket = new FireworkRocketEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), fireworkStack);
                    p.level().addFreshEntity(rocket);
                });
            }
        }
    }

    // Метод отправки сетевых пакетов больших титров на экраны
    private static void sendBigTitle(String mainTitle, String subTitle) {
        for (ServerPlayer player : getMatchPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10)); // Время анимации
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(mainTitle)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subTitle)));
        }
    }

    public static void startNewRound() {
        DeathAndRoundHandler.DEAD_PLAYERS.clear();
        RoundTimerHandler.roundTimeLeft = MatchSystem.configRoundTime;
        RoundTimerHandler.freezeTimeLeft = MatchSystem.configFreezeTime;
        RoundTimerHandler.isFreezePeriod = true;
        MatchSystem.isRoundActive = true;
        MatchSystem.isPaused = false;

        resetPlayersToSpawn();
        broadcastMessage("§a[MineStrike] New round started!");
    }

    public static void resetPlayersToSpawn() {
        for (ServerPlayer player : getMatchPlayers()) {
            player.getServer().execute(() -> {
                player.setGameMode(GameType.SURVIVAL);
                player.removeAllEffects();
                if (player.getRespawnPosition() != null) {
                    player.teleportTo(player.serverLevel(), player.getRespawnPosition().getX() + 0.5, player.getRespawnPosition().getY(), player.getRespawnPosition().getZ() + 0.5, player.getYRot(), player.getXRot());
                }
            });
        }
    }

    public static void forceStopMatch() {
        MatchSystem.isMatchStarted = false;
        MatchSystem.isRoundActive = false;
        MatchSystem.isPaused = false;
        for (ServerPlayer p : getMatchPlayers()) {
            p.removeAllEffects();
        }
    }

    public static void broadcastMessage(String text) {
        getMatchPlayers().forEach(p -> p.sendSystemMessage(Component.literal(text), false));
    }
}

class StatsManager {
    private static final Map<UUID, Integer> playerKills = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void addKill(UUID uuid) { playerKills.put(uuid, playerKills.getOrDefault(uuid, 0) + 1); }
    public static int getKills(UUID uuid) { return playerKills.getOrDefault(uuid, 0); }
    public static void resetStats() { playerKills.clear(); saveStatsToFile(); }

    public static void saveStatsToFile() {
        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "matchsystem/session.json");
        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("t_score", MatchSystem.tPoints);
        rootJson.addProperty("ct_score", MatchSystem.ctPoints);
        rootJson.addProperty("config_round_time", MatchSystem.configRoundTime);
        rootJson.addProperty("config_freeze_time", MatchSystem.configFreezeTime);
        rootJson.addProperty("config_max_points", MatchSystem.configMaxPoints);
        JsonObject playersJson = new JsonObject();

        MatchSystem.T_TEAM.forEach(uuid -> addPlayerStat(playersJson, uuid, "T"));
        MatchSystem.CT_TEAM.forEach(uuid -> addPlayerStat(playersJson, uuid, "CT"));

        rootJson.add("players_statistics", playersJson);
        try (FileWriter writer = new FileWriter(configFile)) { GSON.toJson(rootJson, writer); } catch (IOException ignored) {}
    }

    private static void addPlayerStat(JsonObject json, UUID uuid, String team) {
        JsonObject pStat = new JsonObject();
        pStat.addProperty("team", team);
        pStat.addProperty("kills", playerKills.getOrDefault(uuid, 0));
        json.add(uuid.toString(), pStat);
    }
}
