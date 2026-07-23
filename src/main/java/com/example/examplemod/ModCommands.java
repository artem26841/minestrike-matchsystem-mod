package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ms").requires(s -> s.hasPermission(2))
            .then(Commands.literal("round")
                .then(Commands.literal("on").executes(ctx -> { MatchSystem.isModEnabled = true; ctx.getSource().sendSuccess(() -> Component.literal("§a[MineStrike] Мод включен!"), true); return 1; }))
                .then(Commands.literal("off").executes(ctx -> {
                    MatchSystem.isModEnabled = false;
                    if (MatchSystem.isMatchStarted) RoundManager.forceStopMatch();
                    ctx.getSource().sendSuccess(() -> Component.literal("§c[MineStrike] Мод выключен! Матч завершен."), true);
                    return 1;
                }))
                .then(Commands.literal("startmatch").executes(ctx -> {
                    if (!MatchSystem.isModEnabled) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Мод выключен!")); return 0; }
                    MatchSystem.isMatchStarted = true;
                    MatchSystem.tPoints = 0;
                    MatchSystem.ctPoints = 0;
                    StatsManager.resetStats();
                    RoundManager.broadcastMessage("§6[MineStrike] МАТЧ ПОДГОТОВЛЕН К РАБОТЕ! Табло активировано.");
                    return 1;
                }))
                .then(Commands.literal("stopmatch").executes(ctx -> {
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Матч не запущен!")); return 0; }
                    RoundManager.forceStopMatch();
                    RoundManager.broadcastMessage("§c[MineStrike] Матч официально ЗАВЕРШЕН админом.");
                    return 1;
                }))
                .then(Commands.literal("start").executes(ctx -> {
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Матч не запущен!")); return 0; }
                    if (MatchSystem.isRoundActive) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Раунд уже идет!")); return 0; }
                    RoundManager.startNewRound();
                    return 1;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Матч не запущен!")); return 0; }
                    MatchSystem.isRoundActive = false;
                    RoundManager.resetPlayersToSpawn();
                    RoundManager.broadcastMessage("§c[MineStrike] Раунд остановлен. Игроки вернулись на спавн.");
                    return 1;
                }))
                .then(Commands.literal("startauto").executes(ctx -> {
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Матч не запущен!")); return 0; }
                    MatchSystem.isAutoMode = !MatchSystem.isAutoMode;
                    String status = MatchSystem.isAutoMode ? "§aВКЛЮЧЕН" : "§cВЫКЛЮЧЕН";
                    ctx.getSource().sendSuccess(() -> Component.literal("§e[MineStrike] Автоматический режим раундов: " + status), true);
                    return 1;
                }))
                .then(Commands.literal("paused").executes(ctx -> {
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Матч не запущен!")); return 0; }
                    RoundManager.togglePause();
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    if (!MatchSystem.isMatchStarted) { ctx.getSource().sendFailure(Component.literal("§cОшибка: Матч не запущен!")); return 0; }
                    MatchSystem.tPoints = 0;
                    MatchSystem.ctPoints = 0;
                    MatchSystem.isRoundActive = false;
                    StatsManager.resetStats();
                    RoundManager.broadcastMessage("§e[MineStrike] Матч сброшен и ожидает старта.");
                    return 1;
                }))
                .then(Commands.literal("settings")
                    .then(Commands.argument("roundTime", IntegerArgumentType.integer(1))
                    .then(Commands.argument("freezeTime", IntegerArgumentType.integer(0))
                    .then(Commands.argument("maxPoints", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        MatchSystem.configRoundTime = IntegerArgumentType.getInteger(ctx, "roundTime");
                        MatchSystem.configFreezeTime = IntegerArgumentType.getInteger(ctx, "freezeTime");
                        MatchSystem.configMaxPoints = IntegerArgumentType.getInteger(ctx, "maxPoints");
                        StatsManager.saveStatsToFile();
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[MineStrike] Настройки успешно изменены!"), true);
                        return 1;
                    })))))
                .then(Commands.literal("settingsinfo").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§e=== НАСТРОЙКИ МАТЧА ===\n" +
                            "§7Время раунда: §f" + MatchSystem.configRoundTime + " сек\n" +
                            "§7Время закупки: §f" + MatchSystem.configFreezeTime + " сек\n" +
                            "§7Очков до победы: §f" + MatchSystem.configMaxPoints), false);
                    return 1;
                }))
            )
            .then(Commands.literal("commandaddplayers")
                .then(Commands.literal("red").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); MatchSystem.T_TEAM.add(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§c" + p.getGameProfile().getName() + " §7добавлен за §cT"), true); return 1; })))
                .then(Commands.literal("blue").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); MatchSystem.CT_TEAM.add(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§b" + p.getGameProfile().getName() + " §7добавлен за §bCT"), true); return 1; })))
                .then(Commands.literal("spectator").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); MatchSystem.SPECTATORS.add(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§e" + p.getGameProfile().getName() + " §7переведен в §eНАБЛЮДАТЕЛИ"), true); return 1; })))
                .then(Commands.literal("del").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§7Игрок §f" + p.getGameProfile().getName() + " §7удален из мода"), true); return 1; })))
                // ИСПРАВЛЕНО: Теперь выводит реальные имена игроков вместо технических UUID
                .then(Commands.literal("info").executes(ctx -> {
                    StringBuilder sb = new StringBuilder("§e=== СОСТАВ КОМАНД ===\n§c§lTERRORISTS (T):\n");
                    var server = ServerLifecycleHooks.getCurrentServer();
                    MatchSystem.T_TEAM.forEach(uuid -> {
                        var p = server.getPlayerList().getPlayer(uuid);
                        sb.append("§7- §f").append(p != null ? p.getGameProfile().getName() : "Offline (" + uuid.toString().substring(0,5) + ")").append("\n");
                    });
                    sb.append("§b§lCOUNTER-TERRORISTS (CT):\n");
                    MatchSystem.CT_TEAM.forEach(uuid -> {
                        var p = server.getPlayerList().getPlayer(uuid);
                        sb.append("§7- §f").append(p != null ? p.getGameProfile().getName() : "Offline (" + uuid.toString().substring(0,5) + ")").append("\n");
                    });
                    ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    return 1;
                }))
            )
        );
    }
}
