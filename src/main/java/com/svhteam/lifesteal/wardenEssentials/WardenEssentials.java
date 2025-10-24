package com.svhteam.lifesteal.wardenEssentials;

// Imports
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import com.svteam.wardenlib.EconomyHandler;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import org.bukkit.permissions.Permissible;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.*;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.jetbrains.annotations.NotNull;


// Main Start
public class WardenEssentials extends JavaPlugin implements Listener {


    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WardenEssentials has been enabled! Author is NoFailsXD!");

        checkForUpdates();
        loadVaults();

        FileConfiguration config = getConfig();
        if (config.getBoolean("bossbar.enabled", false)) {
            String title = ChatColor.translateAlternateColorCodes('&', config.getString("bossbar.title", "&bWelcome!"));
            String color = config.getString("bossbar.color", "PURPLE");
            float progress = (float) config.getDouble("bossbar.progress", 1.0);
            com.svteam.wardenlib.BossBarHandler.init(title, color, progress);
        }

    }

    // Check For Updates
    private void checkForUpdates() {
        // The URL should point to a raw text file containing the latest version string
        String updateUrl = "https://raw.githubusercontent.com/SohamTeamIndiaOfficial/WardenEssentials/refs/heads/master/version.txt";

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.net.URL url = new java.net.URL(updateUrl);
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream()));
                String latestVersion = in.readLine().trim();
                in.close();

                String currentVersion = getDescription().getVersion();

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    getLogger().warning("A new version of WardenEssentials is available! (" + latestVersion + ")");
                    getLogger().warning("You are running version: " + currentVersion);
                } else {
                    getLogger().info("WardenEssentials is up to date! (" + currentVersion + ")");
                }
            } catch (Exception e) {
                getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    // Privates
    private final java.util.Set<Player> flyingPlayers = new java.util.HashSet<>();
    private boolean hideJoinLeave = false;
    private final Set<Player> hiddenNameTags = new HashSet<>();
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private File vaultFile;
    private YamlConfiguration vaultConfig;
    private final Map<UUID, Inventory> openVaults = new HashMap<>();


    @Override
    public void onDisable() {
        getLogger().info("WardenEssentials has been disabled!"); saveVaults();
        com.svteam.wardenlib.BossBarHandler.remove();
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        FileConfiguration config = getConfig();
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.join-welcome", "&aWelcome back to server!")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.join-help", "&eUse '/wessentials help' to see cmds.")));
        if (config.getBoolean("messages.vanish-hide-joinquit", true)
                && vanishedPlayers.contains(player.getUniqueId())) {
            event.setJoinMessage(null);
        }
        if (hideJoinLeave) {
            event.setJoinMessage(null);
        }
    }

    public void onPlayerQuit (PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();
        if (hideJoinLeave) {
            event.setQuitMessage(null);
        }
        if (config.getBoolean("messages.vanish-hide-joinquit", true)
                && vanishedPlayers.contains(player.getUniqueId()))  {
            event.setQuitMessage(null);
        }
    }

    @EventHandler
    public void onRightClickWithBlade(PlayerInteractEvent event) {
        if (!(event.getPlayer().getInventory().getItemInMainHand().hasItemMeta())) return;
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;

        if (item.getItemMeta().getDisplayName().equals(ChatColor.DARK_AQUA + "Warden Blade")) {
            event.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "You feel the power of the Warden...");
        }
    }

    // Vaults Load And Save
    private void loadVaults() {
        vaultFile = new File(getDataFolder(), "vaults.yml");
        if (!vaultFile.exists()) {
            try {
                vaultFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        vaultConfig = YamlConfiguration.loadConfiguration(vaultFile);
    }

    private void saveVaults() {
        try {
            vaultConfig.save(vaultFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Inventory getVault(UUID uuid) {
        if (openVaults.containsKey(uuid)) {
            return openVaults.get(uuid);
        }

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Vault");

        if (vaultConfig.contains("vaults." + uuid)) {
            List<?> list = vaultConfig.getList("vaults." + uuid);
            if (list != null) {
                ItemStack[] items = list.toArray(new ItemStack[0]);
                inv.setContents(items);
            }
        }

        openVaults.put(uuid, inv);
        return inv;
    }

    private void saveVault(UUID uuid) {
        Inventory inv = openVaults.get(uuid);
        if (inv != null) {
            vaultConfig.set("vaults." + uuid, Arrays.asList(inv.getContents()));
            saveVaults();
        }
    }

    @EventHandler
    public void onVaultClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_AQUA + "Vault")) {
            Player player = (Player) event.getPlayer();
            saveVault(player.getUniqueId());
        }
    }


    // Main CMDS START
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        FileConfiguration config = getConfig();

        if (cmd.getName().equalsIgnoreCase("wessentials")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(ChatColor.GOLD + "=== WardenEssentials Help ===");
                    sender.sendMessage(ChatColor.AQUA + "/wessentials help" + ChatColor.WHITE + " - Show this help menu");
                    sender.sendMessage(ChatColor.AQUA + "/weban <player> <reason>" + ChatColor.WHITE + " - Ban a player");
                    sender.sendMessage(ChatColor.AQUA + "/weunban <player>" + ChatColor.WHITE + " - Unban a player");
                    sender.sendMessage(ChatColor.AQUA + "/givewardenblade" + ChatColor.WHITE + " - Get the Warden Blade");
                    sender.sendMessage(ChatColor.AQUA + "/wessentials reload" + ChatColor.WHITE + " - Reload the config");
                    sender.sendMessage(ChatColor.AQUA + "/wegive" + ChatColor.WHITE + " - Give an item to a player");
                    sender.sendMessage(ChatColor.AQUA + "/wekill" + ChatColor.WHITE + " - Kill a player");
                    sender.sendMessage(ChatColor.AQUA + "/weup" + ChatColor.WHITE + " - Places a block beneath you");
                    sender.sendMessage(ChatColor.AQUA + "/weeffect" + ChatColor.WHITE + " - Gives player a potion effect");
                    sender.sendMessage(ChatColor.AQUA + "/westop" + ChatColor.WHITE + " - Stops the server " + ChatColor.RED + "(This might be dangerous.)");
                    sender.sendMessage(ChatColor.AQUA + "/weinvsee" + ChatColor.WHITE + " - View another player's inventory ");
                    sender.sendMessage(ChatColor.AQUA + "/wesmite" + ChatColor.WHITE + " - Strike a player with lightning");
                    sender.sendMessage(ChatColor.AQUA + "/weheal" + ChatColor.WHITE + " - Fully heal yourself or another player");
                    sender.sendMessage(ChatColor.AQUA + "/wetp" + ChatColor.WHITE + " - Teleport to a player");
                    sender.sendMessage(ChatColor.AQUA + "/wetphere" + ChatColor.WHITE + " - Teleport a player to you");
                    sender.sendMessage(ChatColor.AQUA + "/wetime" + ChatColor.WHITE + " - Changes the time of day");
                    sender.sendMessage(ChatColor.AQUA + "/weweather" + ChatColor.WHITE + " - Changes the weather");
                    sender.sendMessage(ChatColor.AQUA + "/wealert" + ChatColor.WHITE + " - Sends a server-wide alert");
                    sender.sendMessage(ChatColor.AQUA + "/wefind" + ChatColor.WHITE + " - Find a player's coordinates");
                    sender.sendMessage(ChatColor.AQUA + "/hidemynametag" + ChatColor.WHITE + " - Toggle your nametag visibility");
                    sender.sendMessage(ChatColor.AQUA + "/hideleaveandjoin" + ChatColor.WHITE + " - Toggle join/leave messages globally");
                    sender.sendMessage(ChatColor.AQUA + "/wefeed" + ChatColor.WHITE + " - Feed Yourself");
                    sender.sendMessage(ChatColor.AQUA + "/wehome" + ChatColor.WHITE + " - Teleport To Your Home");
                    sender.sendMessage(ChatColor.AQUA + "/wesethome" + ChatColor.WHITE + " - Set your home");
                    sender.sendMessage(ChatColor.AQUA + "/wegm" + ChatColor.WHITE + " - Change gamemode");
                    sender.sendMessage(ChatColor.AQUA + "/wesudo" + ChatColor.WHITE + " - Force a player to run a command or chat message");
                    sender.sendMessage(ChatColor.AQUA + "/wevanish" + ChatColor.WHITE + " - Toggles vanish mode");
                    sender.sendMessage(ChatColor.AQUA + "/wevault" + ChatColor.WHITE + " - Check Your Vault");
                    sender.sendMessage(ChatColor.AQUA + "/wepay" + ChatColor.WHITE + " - Pay money to a player");
                    sender.sendMessage(ChatColor.AQUA + "/webalance" + ChatColor.WHITE + " - Check your balance");
                    sender.sendMessage(ChatColor.AQUA + "/wetpall" + ChatColor.WHITE + " - Tp all players to the player you selected");
                    sender.sendMessage(ChatColor.AQUA + "/wetphereall" + ChatColor.WHITE + " - Tp all players to you");
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    com.svteam.wardenlib.BossBarHandler.remove();

                    if (getConfig().getBoolean("bossbar.enabled", false)) {
                        String title = ChatColor.translateAlternateColorCodes('&', getConfig().getString("bossbar.title", "&bWelcome!"));
                        String color = getConfig().getString("bossbar.color", "PURPLE");
                        float progress = (float) getConfig().getDouble("bossbar.progress", 1.0);
                        com.svteam.wardenlib.BossBarHandler.init(title, color, progress);
                    }

                    sender.sendMessage(Component.text("WardenEssentials reloaded.", NamedTextColor.GREEN));
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "Usage: /wessentials help");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("weban")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /weban <player> <reason>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            String reason = String.join(" ", args).substring(args[0].length()).trim();
            String banMsg = ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.ban-message", "&cYou are banned from this server! Reason: %reason%")
                            .replace("%reason%", reason));

            Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, null, sender.getName());
            target.kickPlayer(banMsg);

            sender.sendMessage(ChatColor.GREEN + "Banned " + target.getName() + " for: " + reason);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("weunban")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /weunban <player>");
                return true;
            }

            String targetName = args[0];
            Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
            sender.sendMessage(ChatColor.GREEN + "Unbanned " + targetName);
            return true;
        }

        Player player = null;
        if (cmd.getName().equalsIgnoreCase("givewardenblade")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            player = (Player) sender;
            player.getInventory().addItem(createWardenBlade());
            player.sendMessage(ChatColor.GREEN + "You have been given the " + ChatColor.DARK_AQUA + "Warden Blade!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wekill")) {
            if (!sender.hasPermission("wardenessentials.kill")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wekill <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            target.setHealth(0.0);
            sender.sendMessage(ChatColor.GREEN + "You have killed " + target.getName() + ".");
            target.sendMessage(ChatColor.DARK_RED + "You were killed by" + sender.getName() + ".");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wegive")) {
            if (!sender.hasPermission("wardenessentials.give")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /wegive <player> <item> [amount]");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount.");
                    return true;
                }
            }

            ItemStack item;

            // Check for your custom "warden_blade"
            if (args[1].equalsIgnoreCase("warden_blade")) {
                item = new ItemStack(Material.DIAMOND_SWORD, amount);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Warden Blade");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Forged in the depths"));
                meta.setCustomModelData(1); // Your texture's model data
                item.setItemMeta(meta);
            } else {
                // Vanilla material
                Material material = Material.matchMaterial(args[1]);
                if (material == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid item: " + args[1]);
                    return true;
                }
                item = new ItemStack(material, amount);
            }

            target.getInventory().addItem(item);

            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + item.getType().name() + " to " + target.getName() + ".");
            target.sendMessage(ChatColor.GOLD + "You received " + amount + "x " + item.getType().name() + " from " + sender.getName() + ".");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("weup")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }

            Player p =  (Player) sender;

            if (!p.hasPermission("wardenessentials.up")) {
                p.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            int height = 1;
            if (args.length >= 1) {
                try {
                    height = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Invalid height. Must be a number.");
                    return true;
                }
            }

            Location loc = p.getLocation();
            Location newLoc = loc.clone().add(0, height, 0);

            newLoc.getBlock().setType(Material.GLASS);

            p.teleport(newLoc.add(0, 1, 0));

            p.sendMessage(ChatColor.GREEN + "You have been lifted up " + height + " blocks!");
            return true;

        }

        if (cmd.getName().equalsIgnoreCase("weeffect")) {
            if (!sender.hasPermission("wardenessentials.effect")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /weeffect <player> <effect> <duration_seconds> <amplifier>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            try {
                org.bukkit.potion.PotionEffectType effectType = org.bukkit.potion.PotionEffectType.getByName(args[1].toUpperCase());
                if  (effectType == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid effect: " + args[1]);
                    return true;
                }

                int duration =  Integer.parseInt(args[2]) * 20;
                int amplifier = Integer.parseInt(args[3]) - 1;

                target.addPotionEffect(new org.bukkit.potion.PotionEffect(effectType, duration, amplifier));

                sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " effect " + effectType.getName()+ " for " + args[2] + " seconds (level " + args[3] + ")!");
                target.sendMessage(ChatColor.GOLD + "You were given " + effectType.getName() + " for " + args[2] + " seconds (level " + args[3] + ")!");
            }  catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Duration and amplifier must be numbers.");
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("westop")) {
            if (!sender.isOp() && !sender.hasPermission("wardenessentials.stop")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            Bukkit.broadcastMessage(ChatColor.RED + "⚠ The server is shutting down...");

            Bukkit.getWorlds().forEach(world -> {
                world.save();
                getLogger().info("Saved world: " + world.getName());
            });

            Bukkit.getOnlinePlayers().forEach(p -> {
                p.saveData();
                getLogger().info("Saved player data for: " + p.getName());
            });

            Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 20L);

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("weinvsee")) {
            if (!sender.hasPermission("wardenessentials.invsee")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /weinvsee <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            ((Player) sender).openInventory(target.getInventory());
            sender.sendMessage(ChatColor.GREEN + "You are now viewing " + target.getName() + "'s inventory.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wesmite")) {
            if (!sender.hasPermission("wardenessentials.smite")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wesmite <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            target.getWorld().strikeLightning(target.getLocation());
            Bukkit.broadcastMessage(ChatColor.YELLOW + target.getName() + " has been smitten by the gods!");
            return true;
        }

        if  (cmd.getName().equalsIgnoreCase("weheal")) {
            if (!sender.hasPermission("wardenessentials.heal")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player: /weheal <player>");
                    return true;
                }
                Player p =  (Player) sender;
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setSaturation(20);
                p.setFireTicks(0);
                p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
                p.sendMessage(ChatColor.GREEN + "You have been fully healed!");
                return true;
            } else {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                target.setSaturation(20);
                target.setFireTicks(0);
                target.getActivePotionEffects().forEach(effect -> target.removePotionEffect(effect.getType()));
                target.sendMessage(ChatColor.GREEN + "You have been fully healed!");
                sender.sendMessage(ChatColor.GREEN + "You healed " + target.getName() + ".");
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("wefly")) {
            if (!sender.hasPermission("wardenessentials.fly")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player: /wefly <player>");
                    return true;
                }
                Player p =  (Player) sender;
                boolean enable = !p.getAllowFlight();
                p.setAllowFlight(enable);
                if (!enable) p.setFlying(false);
                p.sendMessage(enable ? ChatColor.GREEN + "Flight enabled." : ChatColor.RED + "Flight disabled.");
                return true;
            } else {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                boolean enable = !target.getAllowFlight();
                target.setAllowFlight(enable);
                if (!enable) target.setFlying(false);
                target.sendMessage(enable ?  ChatColor.GREEN + "Flight enabled." : ChatColor.RED + "Flight disabled.");
                sender.sendMessage(enable
                        ? ChatColor.GREEN + "Enabled flight for " + target.getName()
                        : ChatColor.RED + "Disabled flight for " + target.getName());
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("wetp")) {
            if (!sender.hasPermission("wardenessentials.tp")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wetp <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            Player pl = (Player) sender;
            target.teleport(pl.getLocation());
            pl.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
            return true;

        }

        if (cmd.getName().equalsIgnoreCase("wetphere")) {
            if  (!sender.hasPermission("wardenessentials.tphere")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wetphere <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            Player pl1 = (Player) sender;
            target.teleport(pl1.getLocation());
            pl1.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to you.");
            target.sendMessage(ChatColor.GREEN + "You have been teleported to " + pl1.getName() + ".");
            return true;
        }

        // /wetime <day|night|noon|midnight>
        if (cmd.getName().equalsIgnoreCase("wetime")) {
            if (!sender.hasPermission("wardenessentials.time")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wetime <day|night|noon|midnight>");
                return true;
            }

            long time;
            switch (args[0].toLowerCase()) {
                case "day": time = 1000L; break;
                case "noon": time = 6000L; break;
                case "night": time = 13000L; break;
                case "midnight": time = 18000L; break;
                default:
                    sender.sendMessage(ChatColor.RED + "Invalid time! Use: day, noon, night, midnight");
                    return true;
            }

            Bukkit.getWorlds().forEach(world -> world.setTime(time));
            sender.sendMessage(ChatColor.GREEN + "Time set to " + args[0] + " in all worlds.");
            return true;
        }

// /weweather <clear|rain|storm>
        if (cmd.getName().equalsIgnoreCase("weweather")) {
            if (!sender.hasPermission("wardenessentials.weather")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /weweather <clear|rain|storm>");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "clear":
                    Bukkit.getWorlds().forEach(world -> {
                        world.setStorm(false);
                        world.setThundering(false);
                    });
                    sender.sendMessage(ChatColor.GREEN + "Weather set to clear in all worlds.");
                    break;
                case "rain":
                    Bukkit.getWorlds().forEach(world -> {
                        world.setStorm(true);
                        world.setThundering(false);
                    });
                    sender.sendMessage(ChatColor.GREEN + "Weather set to rain in all worlds.");
                    break;
                case "storm":
                    Bukkit.getWorlds().forEach(world -> {
                        world.setStorm(true);
                        world.setThundering(true);
                    });
                    sender.sendMessage(ChatColor.GREEN + "Weather set to storm in all worlds.");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Invalid weather! Use: clear, rain, storm");
                    return true;
            }
            return true;
        }

        // /wealert <message>
        if (cmd.getName().equalsIgnoreCase("wealert")) {
            if (!sender.hasPermission("wardenessentials.alert")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /wealert <message>");
                return true;
            }

            String message = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
            Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "[ALERT] " + ChatColor.RESET + ChatColor.YELLOW + message);
            return true;
        }

// /wefind <player>
        if (cmd.getName().equalsIgnoreCase("wefind")) {
            if (!sender.hasPermission("wardenessentials.find")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wefind <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            Location loc = target.getLocation();
            sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " is at X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ() + " in world " + loc.getWorld().getName());
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("hidemynametag")) {
            if (!sender.hasPermission("wardenessentials.hidemynametag")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            Player pl = (Player) sender;
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = board.getTeam("hiddenNameTags");

            if (team == null) {
                team = board.registerNewTeam("hiddenNameTags");
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            }

            if (hiddenNameTags.contains(player)) {
                hiddenNameTags.remove(player);
                team.removeEntry(player.getName());
                player.sendMessage(ChatColor.GREEN + "Your nametag is now visible.");
            } else {
                hiddenNameTags.add(player);
                team.addEntry(player.getName());
                player.sendMessage(ChatColor.RED + "Your nametag is now hidden.");
            }
            return true;
        }

        // /hideleaveandjoin
        if (cmd.getName().equalsIgnoreCase("hideleaveandjoin")) {
            if (!sender.hasPermission("wardenessentials.hideleave")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
            hideJoinLeave = !hideJoinLeave;
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Join/Leave messages are now " +
                    (hideJoinLeave ? ChatColor.RED + "HIDDEN" : ChatColor.GREEN + "VISIBLE"));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wefeed")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("wardenessentials.feed")) {
                p.sendMessage(ChatColor.RED + "You do not have permission!");
                return true;
            }
            p.setFoodLevel(20);
            p.setSaturation(20);
            p.sendMessage(ChatColor.GREEN + "You have been fed!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wesethome")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("wardenessentials.sethome")) {
                p.sendMessage(ChatColor.RED + "You do not have permission!");
                return true;
            }

            getConfig().set("homes." + p.getUniqueId(), p.getLocation());
            saveConfig();

            p.sendMessage(ChatColor.GREEN + "Home set!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wehome")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("wardenessentials.home")) {
                p.sendMessage(ChatColor.RED + "You do not have permission!");
                return true;
            }

            if (getConfig().contains("homes." + p.getUniqueId())) {
                Location home = (Location) getConfig().get("homes." + p.getUniqueId());
                p.teleport(home);
                p.sendMessage(ChatColor.GREEN + "Teleported to home!");
            } else {
                p.sendMessage(ChatColor.RED + "You don’t have a home set. Use /wesethome first.");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wegm")) {
            if (!sender.hasPermission("wardenessentials.gamemode")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission!");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /wegm <survival|creative|adventure|spectator> [player]");
                return true;
            }

            GameMode mode;
            switch (args[0].toLowerCase()) {
                case "s":
                case "survival": mode = GameMode.SURVIVAL; break;
                case "c":
                case "creative": mode = GameMode.CREATIVE; break;
                case "a":
                case "adventure": mode = GameMode.ADVENTURE; break;
                case "sp":
                case "spectator": mode = GameMode.SPECTATOR; break;
                default:
                    sender.sendMessage(ChatColor.RED + "Invalid gamemode!");
                    return true;
            }

            Player target;
            if (args.length == 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Console must specify a player!");
                    return true;
                }
                target = (Player) sender;
            }

            target.setGameMode(mode);
            target.sendMessage(ChatColor.GREEN + "Your gamemode has been set to " + mode.name());
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + " gamemode to " + mode.name());
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wesudo")) {
            if (!sender.hasPermission("wardenessentials.sudo")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /wesudo <player> <command/message>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            if (message.startsWith("/")) {
                // Force them to run a command
                String cmdToRun = message.substring(1);
                target.performCommand(cmdToRun);
                sender.sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to run command: " + message);
            } else {
                // Force them to send chat
                target.chat(message);
                sender.sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to say: " + message);
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wevanish")) {
            if (!(sender instanceof Player player11)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (!sender.hasPermission("wardenessentials.vanish")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (vanishedPlayers.contains(player11.getUniqueId())) {
                // Unvanish
                vanishedPlayers.remove(player11.getUniqueId());
                Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(this, player11));

                String msg = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.vanish-off", "&cYou are now visible!"));
                player11.sendMessage(msg);

                String broadcast = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.vanish-broadcast-off", "&e%player% is visible again."));
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (p.hasPermission("wardenessentials.vanish.see")) {
                        p.sendMessage(broadcast.replace("%player%", player11.getName()));
                    }
                });

            } else {
                // Vanish
                vanishedPlayers.add(player11.getUniqueId());
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.hasPermission("wardenessentials.vanish.see")) {
                        p.hidePlayer(this, player11);
                    }
                });

                String msg = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.vanish-on", "&aYou have vanished!"));
                player11.sendMessage(msg);

                String broadcast = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.vanish-broadcast-on", "&e%player% has vanished."));
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (p.hasPermission("wardenessentials.vanish.see")) {
                        p.sendMessage(broadcast.replace("%player%", player11.getName()));
                    }
                });
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wescoreboard")) {
            if (!(sender instanceof Player player1)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (!player1.hasPermission("wardenessentials.scoreboard")) {
                player1.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }

            if (com.svteam.wardenlib.ScoreboardAPI.hasScoreboard(player1)) {
                com.svteam.wardenlib.ScoreboardAPI.disableScoreboard(player1);
                player1.sendMessage(ChatColor.YELLOW + "Your scoreboard has been disabled.");
            } else {
                com.svteam.wardenlib.ScoreboardAPI.enableScoreboard(player1);
                player1.sendMessage(ChatColor.GREEN + "Your scoreboard has been enabled.");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wevault")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }

            Player p = (Player) sender;

            if (!p.hasPermission("wardenessentials.vault")) {
                p.sendMessage(ChatColor.RED + "You do not have permission!");
                return true;
            }

            if (args.length == 0) {
                // open own vault
                p.openInventory(getVault(p.getUniqueId()));
                p.sendMessage(ChatColor.GREEN + "Opened your vault!");
            } else if (args.length == 1 && p.hasPermission("wardenessentials.vault.others")) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    p.sendMessage(ChatColor.RED + "Player not found!");
                    return true;
                }
                p.openInventory(getVault(target.getUniqueId()));
                p.sendMessage(ChatColor.YELLOW + "Opened " + target.getName() + "'s vault!");
            } else {
                p.sendMessage(ChatColor.RED + "Usage: /wevault [player]");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("webalance")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }

            Player p = (Player) sender; // use 'p' consistently

            if (!p.hasPermission("wardenessentials.balance")) {
                p.sendMessage(ChatColor.RED + "You don't have permission!");
                return true;
            }

            if (!EconomyHandler.hasEconomy()) {
                p.sendMessage(ChatColor.RED + "No economy plugin detected (Vault missing).");
                return true;
            }

            double balance = EconomyHandler.getBalance(p); // use 'p'
            p.sendMessage(ChatColor.AQUA + "Your balance: " + ChatColor.GOLD + balance);
            return true;
        }


        if (cmd.getName().equalsIgnoreCase("wepay")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }
            if (!p.hasPermission("wardenessentials.pay")) {
                p.sendMessage(ChatColor.RED + "You don't have permission!");
                return true;
            }

            if (args.length != 2) {
                p.sendMessage(ChatColor.RED + "Usage: /wepay <player> <amount>");
                return true;
            }

            if (!EconomyHandler.hasEconomy()) {
                p.sendMessage(ChatColor.RED + "Vault not found, cannot process payments!");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "Player " + args[0] + " is not online or found!");
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Invalid number!");
                return true;
            }

            if (amount <= 0) {
                p.sendMessage(ChatColor.RED + "Amount must be positive!");
                return true;
            }

            if (!EconomyHandler.withdraw(p, amount)) {
                p.sendMessage(ChatColor.RED + "You don't have enough funds!");
                return true;
            }

            EconomyHandler.deposit(target, amount);
            p.sendMessage(ChatColor.GREEN + "You sent " + amount + " coins to " + target.getName() + ".");
            target.sendMessage(ChatColor.GOLD + p.getName() + " has sent you " + amount + " coins!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wetpall")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return true;
            }

            Player pl2 = (Player) sender;

            if (!pl2.hasPermission("wardenessentials.tpall")) {
                pl2.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }

            if (args.length != 1) {
                pl2.sendMessage(Component.text("Usage: /wetpall <player>", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                pl2.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(target)) {
                    p.teleport(target.getLocation());
                    p.sendMessage(Component.text("You have been teleported to " + target.getName() + ".", NamedTextColor.GREEN));
                }
            }

            Bukkit.broadcast(Component.text("⚡ All players were teleported to " + target.getName() + "!", NamedTextColor.GOLD));
            pl2.sendMessage(Component.text("You teleported all players to " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }

        // --- /wetpall <player> ---
        if (cmd.getName().equalsIgnoreCase("wetpall")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return true;
            }

            Player pl2 = (Player) sender;

            if (!pl2.hasPermission("wardenessentials.tpall")) {
                pl2.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }

            if (args.length != 1) {
                pl2.sendMessage(Component.text("Usage: /wetpall <player>", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                pl2.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(target)) {
                    p.teleport(target.getLocation());
                    p.sendMessage(Component.text("You have been teleported to " + target.getName() + ".", NamedTextColor.GREEN));
                }
            }

            Bukkit.broadcast(Component.text("⚡ All players were teleported to " + target.getName() + "!", NamedTextColor.GOLD));
            pl2.sendMessage(Component.text("You teleported all players to " + target.getName() + ".", NamedTextColor.YELLOW));
            return true;
        }


// --- /wetphereall ---
        if (cmd.getName().equalsIgnoreCase("wetphereall")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return true;
            }

            Player pl2 = (Player) sender;

            if (!pl2.hasPermission("wardenessentials.tphereall")) {
                pl2.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(pl2)) {
                    p.teleport(pl2.getLocation());
                    p.sendMessage(Component.text("You have been teleported to " + pl2.getName() + ".", NamedTextColor.GREEN));
                }
            }

            Bukkit.broadcast(Component.text("⚡ All players were teleported to " + pl2.getName() + "!", NamedTextColor.GOLD));
            pl2.sendMessage(Component.text("You teleported all players to yourself.", NamedTextColor.YELLOW));
            return true;
        }



        // --- Rest of your commands (ban, heal, fly, etc.) stay the same ---

        return false;
    }




    private ItemStack createWardenBlade() {
        ItemStack blade = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = blade.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_AQUA + "Warden Blade");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Forged in the depths of the Deep Dark");
        lore.add(ChatColor.GRAY + "Infused with Warden's power");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        blade.setItemMeta(meta);
        return blade;
    }
}

