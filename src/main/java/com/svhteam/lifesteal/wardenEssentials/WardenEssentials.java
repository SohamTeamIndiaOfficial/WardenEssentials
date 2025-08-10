package com.svhteam.lifesteal.wardenEssentials;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WardenEssentials extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WardenEssentials has been enabled! Author is NoFailsXD!");
    }

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
