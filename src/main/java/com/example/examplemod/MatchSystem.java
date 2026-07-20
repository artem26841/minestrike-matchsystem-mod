package com.example.matchsystem;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

@Mod("matchsystem")
public class MatchSystem {
    public static final String MODID = "matchsystem";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Set<UUID> RED_TEAM = new HashSet<>();
    public static final Set<UUID> BLUE_TEAM = new HashSet<>();
    public static final Set<UUID> SPECTATORS = new HashSet<>();

    public static boolean isModEnabled = false;
    public static boolean isRoundActive = false;
    public static boolean isPaused = false;
    public static boolean isAutoMode = false;

    public static int redPoints = 0;
    public static int bluePoints = 0;

    public MatchSystem() {
        MinecraftForge.EVENT_BUS.register(this);
        createConfigFolder();
    }

    private void createConfigFolder() {
        File configDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "matchsystem");
        if (!configDir.exists()) configDir.mkdirs();
    }

    public static void removeFromAllTeams(UUID uuid) {
        RED_TEAM.remove(uuid);
        BLUE_TEAM.remove(uuid);
        SPECTATORS.remove(uuid);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        ModCommands.register(dispatcher);
    }
}
