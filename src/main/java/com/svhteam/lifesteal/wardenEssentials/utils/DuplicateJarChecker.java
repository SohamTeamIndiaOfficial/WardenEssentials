package com.svhteam.lifesteal.wardenEssentials.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DuplicateJarChecker {

    private final Plugin plugin;

    public DuplicateJarChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void runCheck() {
        try {
            if (!plugin.getConfig().getBoolean("settings.check-duplicate-jars", true)) {
                return; // disabled via config
            }

            File pluginsFolder = plugin.getDataFolder().getParentFile();
            String pluginName = plugin.getPluginMeta().getName().toLowerCase();

            File[] files = pluginsFolder.listFiles((dir, name) ->
                    name.toLowerCase().startsWith(pluginName) && name.toLowerCase().endsWith(".jar")
            );

            if (files == null || files.length <= 1) {
                return; // Only 1 file > OK
            }

            // Find Duplicates
            List<String> duplicates = new ArrayList<>();
            for (File f : files) {
                duplicates.add(f.getName());
            }

            plugin.getLogger().warning("⚠ Detected possible duplicate " + pluginName + " JARs:");
            for (String fileName : duplicates) {
                plugin.getLogger().warning("  - " + fileName);
            }

            // Send warning to console + OPS
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Component msg = Component.text("⚠ WARNING: Multiple " + pluginName + " plugin JARs detected!", NamedTextColor.RED);
                Bukkit.getConsoleSender().sendMessage(msg);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.isOp()) p.sendMessage(msg);
                }
            }, 60L);

            } catch (Exception e) {
            plugin.getLogger().warning("Error while checking for duplicate JARs: " + e.getMessage());
        }
    }
}
