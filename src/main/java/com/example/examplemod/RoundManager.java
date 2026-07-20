package com.example.matchsystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
            if (MatchSystem.RED_TEAM.contains(p.getUUID()) || MatchSystem.BLUE_TEAM.contains(p.getUUID())) list.add(p);
        }
        return list;
    }

    public static void endRound(String winningTeam, String endReason) {
        MatchSystem.isRoundActive = false;
        if (winningTeam.equalsIgnoreCase("red")) MatchSystem.redPoints++;
        else if (winningTeam.equalsIgnoreCase("blue")) MatchSystem.bluePoints++;

        StatsManager.saveStatsToFile();
        broadcastMessage("§6=================================\n§e§lРАУНД ЗАВЕРШЕН!\n" + endReason + "\n§7Счет: §cRED " + MatchSystem.redPoints + " §7| §b" + MatchSystem.bluePoints + " BLUE\n§6=================================");
        giveAdminStatsBook();

        if (MatchSystem.isAutoMode) {
            RoundTimerHandler.freezeTimeLeft = 15;
            RoundTimerHandler.isFreezePeriod = true;
            startNewRound();
        }
    }

    public static void startNewRound() {
        DeathAndRoundHandler.DEAD_PLAYERS.clear();
        RoundTimerHandler.roundTimeLeft = 115;
        RoundTimerHandler.freezeTimeLeft = 15;
        RoundTimerHandler.isFreezePeriod = true;
        MatchSystem.isRoundActive = true;
        MatchSystem.isPaused = false;

        for (ServerPlayer player : getMatchPlayers()) {
            player.setGameMode(GameType.SURVIVAL);
            player.removeAllEffects();
            if (player.getRespawnPosition() != null) {
                player.teleportTo(player.serverLevel(), player.getRespawnPosition().getX() + 0.5, player.getRespawnPosition().getY(), player.getRespawnPosition().getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        }
        broadcastMessage("§a[MineStrike] Новый раунд начался!");
    }

    public static void giveAdminStatsBook() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putString("title", "§4§lMineStrike Статистика");
        tag.putString("author", "Server");
        ListTag pages = new ListTag();
        StringBuilder p1 = new StringBuilder("§0§lМАТЧ СТАТУС\n\n§cКрасные: " + MatchSystem.redPoints + "\n§bСиние: " + MatchSystem.bluePoints + "\n\n§0§lКИЛЛЫ:\n");
        
        for (ServerPlayer player : getMatchPlayers()) {
            p1.append(MatchSystem.RED_TEAM.contains(player.getUUID()) ? "§c" : "§b").append(player.getGameProfile().getName()).append("§0: ").append(StatsManager.getKills(player.getUUID())).append("\n");
        }
        pages.add(StringTag.valueOf(p1.toString()));
        tag.put("pages", pages);
        book.setTag(tag);

        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.hasPermission(2)) player.getInventory().add(book.copy());
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
        rootJson.addProperty("red_score", MatchSystem.redPoints);
        rootJson.addProperty("blue_score", MatchSystem.bluePoints);
        JsonObject playersJson = new JsonObject();

        MatchSystem.RED_TEAM.forEach(uuid -> addPlayerStat(playersJson, uuid, "RED"));
        MatchSystem.BLUE_TEAM.forEach(uuid -> addPlayerStat(playersJson, uuid, "BLUE"));

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
