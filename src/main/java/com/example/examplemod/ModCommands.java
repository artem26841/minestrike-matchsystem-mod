package com.example.examplemod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ms").requires(s -> s.hasPermission(2))
            .then(Commands.literal("round")
                .then(Commands.literal("on").executes(ctx -> { MatchSystem.isModEnabled = true; ctx.getSource().sendSuccess(() -> Component.literal("§a[MineStrike] Мод включен!"), true); return 1; }))
                .then(Commands.literal("off").executes(ctx -> { MatchSystem.isModEnabled = false; MatchSystem.isRoundActive = false; ctx.getSource().sendSuccess(() -> Component.literal("§c[MineStrike] Мод выключен!"), true); return 1; }))
                .then(Commands.literal("start").executes(ctx -> { RoundManager.startNewRound(); return 1; }))
                .then(Commands.literal("stop").executes(ctx -> { MatchSystem.isRoundActive = false; RoundManager.broadcastMessage("§cРаунд принудительно остановлен админом."); return 1; }))
                .then(Commands.literal("startauto").executes(ctx -> { MatchSystem.isAutoMode = true; RoundManager.startNewRound(); return 1; }))
                .then(Commands.literal("paused").executes(ctx -> { MatchSystem.isPaused = !MatchSystem.isPaused; String status = MatchSystem.isPaused ? "§cПАУЗА" : "§aИГРА ПРОДОЛЖЕНА"; RoundManager.broadcastMessage("§eМатч: " + status); return 1; }))
                .then(Commands.literal("reset").executes(ctx -> { MatchSystem.redPoints = 0; MatchSystem.bluePoints = 0; StatsManager.resetStats(); ctx.getSource().sendSuccess(() -> Component.literal("§eСтатистика матча полностью сброшена!"), true); return 1; }))
                .then(Commands.literal("point")
                    .then(Commands.literal("red").then(Commands.argument("num", IntegerArgumentType.integer()).executes(ctx -> { MatchSystem.redPoints = IntegerArgumentType.getInteger(ctx, "num"); StatsManager.saveStatsToFile(); return 1; })))
                    .then(Commands.literal("blue").then(Commands.argument("num", IntegerArgumentType.integer()).executes(ctx -> { MatchSystem.bluePoints = IntegerArgumentType.getInteger(ctx, "num"); StatsManager.saveStatsToFile(); return 1; })))
                )
            )
            .then(Commands.literal("commandaddplayers")
                .then(Commands.literal("red").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); MatchSystem.RED_TEAM.add(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§c" + p.getGameProfile().getName() + " §7добавлен за §cКРАСНЫХ"), true); return 1; })))
                .then(Commands.literal("blue").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); MatchSystem.BLUE_TEAM.add(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§b" + p.getGameProfile().getName() + " §7добавлен за §bСИНИХ"), true); return 1; })))
                .then(Commands.literal("spectator").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); MatchSystem.SPECTATORS.add(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§e" + p.getGameProfile().getName() + " §7переведен в §eНАБЛЮДАТЕЛИ"), true); return 1; })))
                .then(Commands.literal("del").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> { ServerPlayer p = EntityArgument.getPlayer(ctx, "player"); MatchSystem.removeFromAllTeams(p.getUUID()); ctx.getSource().sendSuccess(() -> Component.literal("§7Игрок §f" + p.getGameProfile().getName() + " §7удален из мода"), true); return 1; })))
            )
        );
    }
}
