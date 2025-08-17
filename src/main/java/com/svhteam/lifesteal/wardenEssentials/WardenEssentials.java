package com.svhteam.lifesteal.wardenEssentials;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.*;

public class WardenEssentials extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WardenEssentials has been enabled! Author is NoFailsXD!");

        checkForUpdates();

    }
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

    private final java.util.Set<Player> flyingPlayers = new java.util.HashSet<>();
    private boolean hideJoinLeave = false;
    private final Set<Player> hiddenNameTags = new HashSet<>();

    @Override
    public void onDisable() {
        getLogger().info("WardenEssentials has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        FileConfiguration config = getConfig();
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.join-welcome", "&aWelcome back to server!")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.join-help", "&eUse '/wessentials help' to see cmds.")));
        if (hideJoinLeave) {
            event.setJoinMessage(null);
        }
    }

    public void onPlayerQuit (PlayerQuitEvent event) {
        if (hideJoinLeave) {
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
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "WardenEssentials config reloaded!");
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

