package com.svhteam.lifesteal.wardenEssentials;

import com.svteam.wardenlib.EconomyHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WardenEssentials extends JavaPlugin implements Listener {

    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Set<Player> hiddenNameTags = new HashSet<>();
    private final Set<Player> flyingPlayers = new HashSet<>();
    private boolean hideJoinLeave = false;
    private File vaultFile;
    private YamlConfiguration vaultConfig;
    private final Map<UUID, Inventory> openVaults = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WardenEssentials has been enabled!");

        checkForUpdates();
        loadVaults();
    }

    @Override
    public void onDisable() {
        getLogger().info("WardenEssentials has been disabled!");
        saveVaults();
    }

    // Helper: color text
    private Component msg(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    // Helper: broadcast message
    private void broadcast(String text, NamedTextColor color) {
        Bukkit.broadcast(Component.text(text, color));
    }

    // ===== Update Checker =====
    private void checkForUpdates() {
        String updateUrl = "https://raw.githubusercontent.com/SohamTeamIndiaOfficial/WardenEssentials/refs/heads/master/version.txt";
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.net.URL url = new java.net.URL(updateUrl);
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream()));
                String latestVersion = in.readLine().trim();
                in.close();

                String currentVersion = getDescription().getVersion();

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    getLogger().warning("A new version is available: " + latestVersion + " (current: " + currentVersion + ")");
                } else {
                    getLogger().info("WardenEssentials is up to date (" + currentVersion + ")");
                }
            } catch (Exception e) {
                getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    // ===== Player Join/Quit =====
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = getConfig();

        player.sendMessage(msg("Welcome back to the server!", NamedTextColor.GREEN));
        player.sendMessage(msg("Use /wessentials help to see commands.", NamedTextColor.YELLOW));

        if (config.getBoolean("messages.vanish-hide-joinquit", true) &&
                vanishedPlayers.contains(player.getUniqueId())) {
            event.joinMessage(null);
        }

        if (hideJoinLeave) event.joinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (hideJoinLeave) event.quitMessage(null);
        if (vanishedPlayers.contains(player.getUniqueId())) event.quitMessage(null);
    }

    // ===== Vault Storage =====
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
        if (openVaults.containsKey(uuid)) return openVaults.get(uuid);

        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Vault", NamedTextColor.DARK_AQUA));

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
        if (event.getView().title().equals(Component.text("Vault", NamedTextColor.DARK_AQUA))) {
            Player player = (Player) event.getPlayer();
            saveVault(player.getUniqueId());
        }
    }

    // ===== Command Handler =====
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        FileConfiguration config = getConfig();

        // === Example simple command replacements (Adventure style) ===

        if (cmd.getName().equalsIgnoreCase("wefeed")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg("Only players can use this command!", NamedTextColor.RED));
                return true;
            }
            if (!p.hasPermission("wardenessentials.feed")) {
                p.sendMessage(msg("You do not have permission!", NamedTextColor.RED));
                return true;
            }
            p.setFoodLevel(20);
            p.setSaturation(20);
            p.sendMessage(msg("You have been fed!", NamedTextColor.GREEN));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("weheal")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg("Only players can use this command!", NamedTextColor.RED));
                return true;
            }
            if (!p.hasPermission("wardenessentials.heal")) {
                p.sendMessage(msg("You do not have permission!", NamedTextColor.RED));
                return true;
            }
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setSaturation(20);
            p.setFireTicks(0);
            p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));
            p.sendMessage(msg("You have been fully healed!", NamedTextColor.GREEN));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wevanish")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(msg("Only players can use this command!", NamedTextColor.RED));
                return true;
            }
            if (!player.hasPermission("wardenessentials.vanish")) {
                player.sendMessage(msg("You do not have permission!", NamedTextColor.RED));
                return true;
            }

            if (vanishedPlayers.contains(player.getUniqueId())) {
                vanishedPlayers.remove(player.getUniqueId());
                Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(this, player));
                player.sendMessage(msg("You are now visible!", NamedTextColor.RED));
            } else {
                vanishedPlayers.add(player.getUniqueId());
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.hasPermission("wardenessentials.vanish.see")) {
                        p.hidePlayer(this, player);
                    }
                });
                player.sendMessage(msg("You have vanished!", NamedTextColor.GREEN));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wealert")) {
            if (!sender.hasPermission("wardenessentials.alert")) {
                sender.sendMessage(msg("You do not have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(msg("Usage: /wealert <message>", NamedTextColor.RED));
                return true;
            }
            String message = String.join(" ", args);
            Bukkit.broadcast(
                    Component.text("[ALERT] ", NamedTextColor.RED, TextDecoration.BOLD)
                            .append(Component.text(message, NamedTextColor.YELLOW))
            );
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("webalance")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg("Only players can use this command!", NamedTextColor.RED));
                return true;
            }

            if (!p.hasPermission("wardenessentials.balance")) {
                p.sendMessage(msg("You don't have permission!", NamedTextColor.RED));
                return true;
            }

            if (!EconomyHandler.hasEconomy()) {
                p.sendMessage(msg("No economy plugin detected (Vault missing).", NamedTextColor.RED));
                return true;
            }

            double balance = EconomyHandler.getBalance(p);
            p.sendMessage(Component.text("Your balance: ", NamedTextColor.AQUA)
                    .append(Component.text(String.format("%.2f", balance), NamedTextColor.GOLD)));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wepay")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg("Only players can use this command!", NamedTextColor.RED));
                return true;
            }
            if (!p.hasPermission("wardenessentials.pay")) {
                p.sendMessage(msg("You don't have permission!", NamedTextColor.RED));
                return true;
            }

            if (args.length != 2) {
                p.sendMessage(msg("Usage: /wepay <player> <amount>", NamedTextColor.RED));
                return true;
            }

            if (!EconomyHandler.hasEconomy()) {
                p.sendMessage(msg("Vault not found, cannot process payments!", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                p.sendMessage(msg("Player not found!", NamedTextColor.RED));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(msg("Invalid number!", NamedTextColor.RED));
                return true;
            }

            if (amount <= 0) {
                p.sendMessage(msg("Amount must be positive!", NamedTextColor.RED));
                return true;
            }

            if (!EconomyHandler.withdraw(p, amount)) {
                p.sendMessage(msg("You don't have enough funds!", NamedTextColor.RED));
                return true;
            }

            EconomyHandler.deposit(target, amount);
            p.sendMessage(Component.text("You sent ", NamedTextColor.GREEN)
                    .append(Component.text(amount + " coins to " + target.getName(), NamedTextColor.GOLD)));
            target.sendMessage(Component.text(p.getName() + " sent you " + amount + " coins!", NamedTextColor.GOLD));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("wetpall")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
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

        return false;
    }



    // ===== Custom Items =====
    private ItemStack createWardenBlade() {
        ItemStack blade = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = blade.getItemMeta();

        meta.displayName(Component.text("Warden Blade", NamedTextColor.DARK_AQUA));
        meta.lore(List.of(
                Component.text("Forged in the depths of the Deep Dark", NamedTextColor.GRAY),
                Component.text("Infused with Warden's power", NamedTextColor.GRAY)
        ));

        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        blade.setItemMeta(meta);
        return blade;
    }
}
