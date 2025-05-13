package id.neonbackpack.commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import id.neonbackpack.NeonBackPack;
import id.neonbackpack.BackpackManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class BackpackCommand implements CommandExecutor, TabCompleter {
    
    private final NeonBackPack plugin;
    private final BackpackManager backpackManager;
    
    public BackpackCommand(NeonBackPack plugin, BackpackManager backpackManager) {
        this.plugin = plugin;
        this.backpackManager = backpackManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Periksa apakah pengirim adalah pemain
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(getPrefix() + "ᴘᴇʀɪɴᴛᴀʜ ɪɴɪ ʜᴀɴʏᴀ ᴅᴀᴘᴀᴛ ᴅɪɢᴜɴᴀᴋᴀɴ ᴏʟᴇʜ ᴘᴇᴍᴀɪɴ.");
            return true;
        }

        // Jika tidak ada argumen, buka backpack pemain dengan GUI baru
        if (args.length == 0) {
            Player player = (Player) sender;
            plugin.getGUIManager().openBackpackGUI(player);
            sendMessage(player, getMessage("backpack-opened"));
            return true;
        }

        // Handle subcommands
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                return handleGiveCommand(sender, args);
            case "upgrade":
                return handleUpgradeCommand(sender, args);
            case "help":
                return handleHelpCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "special":
                return handleSpecialCommand(sender, args);
            case "shop":
                return handleShopCommand(sender);
            case "view":
            case "inspect":
                return handleViewCommand(sender, args);
            default:
                sendMessage(sender, "§cSᴜʙᴄᴏᴍᴍᴀɴᴅ ᴛɪᴅᴀᴋ ᴅɪᴋᴇɴᴀʟ. Cᴏʙᴀ '/ʙᴘ ʜᴇʟᴘ' ᴜɴᴛᴜᴋ ᴅᴀғᴛᴀʀ ᴘᴇʀɪɴᴛᴀʜ.");
                return true;
        }
    }

    private boolean handleSpecialCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "§cᴘᴇʀɪɴᴛᴀʜ ɪɴɪ ʜᴀɴʏᴀ ᴅᴀᴘᴀᴛ ᴅɪɢᴜɴᴀᴋᴀɴ ᴏʟᴇʜ ᴘᴇᴍᴀɪɴ.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Format: /bp special <type>
        if (args.length < 2) {
            sendMessage(player, "§cᴘᴇɴɢɢᴜɴᴀᴀɴ: /ʙᴘ sᴘᴇᴄɪᴀʟ <ᴛʏᴘᴇ>");
            sendMessage(player, "§cᴛɪᴘᴇ ʏᴀɴɢ ᴛᴇʀsᴇᴅɪᴀ: MINING, FARMING, COMBAT");
            return true;
        }
        
        String type = args[1].toUpperCase();
        List<String> validTypes = Arrays.asList("MINING", "FARMING", "COMBAT");
        
        if (!validTypes.contains(type)) {
            sendMessage(player, getMessage("backpack-not-found"));
            return true;
        }
        
        // Cek permission
        if (!player.hasPermission("neonbackpack." + type.toLowerCase())) {
            sendMessage(player, getMessage("no-permission"));
            return true;
        }
        
        // Buat item special backpack
        ItemStack backpackItem = backpackManager.createBackpackItem(type);
        
        // Berikan ke pemain
        if (player.getInventory().firstEmpty() == -1) {
            // Inventory penuh
            player.getWorld().dropItemNaturally(player.getLocation(), backpackItem);
            sendMessage(player, getMessage("backpack-received") + " §c(ɪɴᴠᴇɴᴛᴏʀʏ ᴘᴇɴᴜʜ, ɪᴛᴇᴍ ᴅɪᴊᴀᴛᴜʜᴋᴀɴ)");
        } else {
            player.getInventory().addItem(backpackItem);
            sendMessage(player, getMessage("backpack-received"));
        }
        
        return true;
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("neonbackpack.admin")) {
            sendMessage(sender, getMessage("no-permission"));
            return true;
        }
        
        if (args.length < 3) {
            sendMessage(sender, "§cᴘᴇɴɢɢᴜɴᴀᴀɴ: /ʙᴘ ɢɪᴠᴇ <ᴘᴇᴍᴀɪɴ> <ᴛɪᴘᴇ>");
            return true;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            sendMessage(sender, getMessage("player-not-found"));
            return true;
        }
        
        String backpackType = args[2].toUpperCase();
        
        // Validasi tipe backpack
        if (!plugin.getConfig().isConfigurationSection("backpacks.types." + backpackType)) {
            sendMessage(sender, getMessage("backpack-not-found"));
            return true;
        }
        
        // Gunakan metode overload yang tepat dengan menyertakan pemain target
        ItemStack backpackItem = plugin.getBackpackManager().createBackpackItem(backpackType, targetPlayer);
        
        if (targetPlayer.getInventory().firstEmpty() == -1) {
            // Inventory penuh
            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), backpackItem);
            sendMessage(targetPlayer, getMessage("backpack-received") + " §c(ɪɴᴠᴇɴᴛᴏʀʏ ᴘᴇɴᴜʜ, ɪᴛᴇᴍ ᴅɪᴊᴀᴛᴜʜᴋᴀɴ)");
        } else {
            targetPlayer.getInventory().addItem(backpackItem);
            sendMessage(targetPlayer, getMessage("backpack-received"));
        }
        
        sendMessage(sender, getMessage("backpack-given").replace("%player%", targetPlayer.getName()));
        
        return true;
    }

    private boolean handleUpgradeCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "§cᴘᴇʀɪɴᴛᴀʜ ɪɴɪ ʜᴀɴʏᴀ ᴅᴀᴘᴀᴛ ᴅɪɢᴜɴᴀᴋᴀɴ ᴏʟᴇʜ ᴘᴇᴍᴀɪɴ.");
            return true;
        }

        Player player = (Player) sender;
        
        // Implementasi upgrade logic
        // 1. Cek tipe backpack pemain saat ini
        String currentType = plugin.getDatabaseManager().getPlayerBackpackType(player.getUniqueId());
        
        // 2. Tentukan tipe berikutnya
        String nextType;
        boolean isMaxLevel = false;
        
        switch (currentType) {
            case "BASIC":
                nextType = "ADVANCED";
                break;
            case "ADVANCED":
                nextType = "PREMIUM";
                break;
            case "PREMIUM":
                nextType = "ULTIMATE";
                break;
            default:
                nextType = currentType;
                isMaxLevel = true;
                break;
        }
        
        // 3. Cek apakah sudah max level
        if (isMaxLevel) {
            sendMessage(player, getMessage("backpack-max-level"));
            return true;
        }
        
        // 4. Cek permission untuk level berikutnya
        String permission = "neonbackpack." + nextType.toLowerCase();
        if (!player.hasPermission(permission)) {
            sendMessage(player, getMessage("no-permission"));
            return true;
        }
        
        // 5. Cek ekonomi jika Vault diaktifkan
        if (plugin.getEconomyManager().isEnabled()) {
            if (!plugin.getEconomyManager().upgradeBackpack(player, currentType, nextType)) {
                // Pesan error sudah ditangani di dalam upgradeBackpack()
                return true;
            }
        }
        
        // 6. Upgrade backpack di database
        // Simpan isi backpack saat ini
        ItemStack[] currentContents = plugin.getDatabaseManager().getPlayerBackpackContents(player.getUniqueId());
        
        // Update tipe backpack
        plugin.getDatabaseManager().savePlayerBackpack(
            player.getUniqueId(),
            nextType,
            currentContents,
            plugin.getDatabaseManager().getBackpackSize(nextType)
        );
        
        if (!plugin.getEconomyManager().isEnabled()) {
            // Tampilkan pesan upgrade biasa jika tidak menggunakan ekonomi
            sendMessage(player, getMessage("backpack-upgraded"));
        }
        return true;
    }

    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage("§b§l===== NusaBackpack Help =====");
        sender.sendMessage("§a/bp §7- ʙᴜᴋᴀ ʙᴀᴄᴋᴘᴀᴄᴋ ᴀɴᴅᴀ");
        sender.sendMessage("§a/bp upgrade §7- ᴜᴘɢʀᴀᴅᴇ ʙᴀᴄᴋᴘᴀᴄᴋ ᴀɴᴅᴀ ᴋᴇ ʟᴇᴠᴇʟ ʙᴇʀɪᴋᴜᴛɴʏᴀ");
        sender.sendMessage("§a/bp special <type> §7- ᴅᴀᴘᴀᴛᴋᴀɴ ʙᴀᴄᴋᴘᴀᴄᴋ ᴋʜᴜsᴜs (ᴍɪɴɪɴɢ, ғᴀʀᴍɪɴɢ, ᴄᴏᴍʙᴀᴛ)");
        sender.sendMessage("§a/bp shop §7- ʙᴜᴋᴀ ᴛᴏᴋᴏ ʙᴀᴄᴋᴘᴀᴄᴋ");
        
        if (sender.hasPermission("neonbackpack.admin")) {
            sender.sendMessage("§a/bp give <pemain> <tipe> §7- ʙᴇʀɪᴋᴀɴ ʙᴀᴄᴋᴘᴀᴄᴋ ᴋᴇ ᴘᴇᴍᴀɪɴ");
            sender.sendMessage("§a/bp view <pemain> §7- ᴍᴇʟɪʜᴀᴛ ɪsɪ ʙᴀᴄᴋᴘᴀᴄᴋ ᴘᴇᴍᴀɪɴ ʟᴀɪɴ");
            sender.sendMessage("§a/bp reload §7- ᴍᴜᴀᴛ ᴜʟᴀɴɢ ᴋᴏɴғɪɢᴜʀᴀsɪ ᴘʟᴜɢɪɴ");
        }
        
        sender.sendMessage("§b§l==========================");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("neonbackpack.admin")) {
            sendMessage(sender, getMessage("no-permission"));
            return true;
        }
        
        // Reload config
        plugin.reloadConfig();
        
        sendMessage(sender, "§aᴋᴏɴғɪɢᴜʀᴀsɪ ʙᴇʀʜᴀsɪʟ ᴅɪᴍᴜᴀᴛ ᴜʟᴀɴɢ! sᴇᴍᴜᴀ ᴘᴇɴɢᴀᴛᴜʀᴀɴ ᴛᴇʟᴀʜ ᴅɪᴘᴇʀʙᴀʀᴜɪ.");
        return true;
    }

    private boolean handleShopCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "§cᴘᴇʀɪɴᴛᴀʜ ɪɴɪ ʜᴀɴʏᴀ ᴅᴀᴘᴀᴛ ᴅɪɢᴜɴᴀᴋᴀɴ ᴏʟᴇʜ ᴘᴇᴍᴀɪɴ.");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getGUIManager().openBackpackShopGUI(player);
        return true;
    }

    private boolean handleViewCommand(CommandSender sender, String[] args) {
        // Hanya admin yang bisa menggunakan perintah ini
        if (!sender.hasPermission("neonbackpack.admin")) {
            sendMessage(sender, getMessage("no-permission"));
            return true;
        }
        
        // Harus berupa player
        if (!(sender instanceof Player)) {
            sendMessage(sender, "§cᴘᴇʀɪɴᴛᴀʜ ɪɴɪ ʜᴀɴʏᴀ ᴅᴀᴘᴀᴛ ᴅɪɢᴜɴᴀᴋᴀɴ ᴏʟᴇʜ ᴘᴇᴍᴀɪɴ.");
            return true;
        }
        
        // Format: /bp view <pemain>
        if (args.length < 2) {
            sendMessage(sender, "§cᴘᴇɴɢɢᴜɴᴀᴀɴ: /ʙᴘ ᴠɪᴇᴡ <ᴘᴇᴍᴀɪɴ>");
            return true;
        }
        
        Player admin = (Player) sender;
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        // Cek apakah pemain ada (online)
        if (targetPlayer == null) {
            // Jika offline, coba cari di database
            try {
                UUID targetUUID = plugin.getDatabaseManager().getPlayerUUIDByName(targetName);
                if (targetUUID != null) {
                    plugin.getGUIManager().openAdminViewGUI(admin, targetUUID, targetName);
                    sendMessage(admin, "§aᴍᴇᴍʙᴜᴋᴀ ʙᴀᴄᴋᴘᴀᴄᴋ §e" + targetName + " §a(ᴏғғʟɪɴᴇ).");
                    return true;
                } else {
                    sendMessage(sender, getMessage("player-not-found"));
                    return true;
                }
            } catch (Exception e) {
                sendMessage(sender, getMessage("player-not-found"));
                plugin.getLogger().warning("ᴇʀʀᴏʀ sᴀᴀᴛ ᴍᴇɴᴄᴀʀɪ ᴘᴇᴍᴀɪɴ ᴅɪ ᴅᴀᴛᴀʙᴀsᴇ: " + e.getMessage());
                return true;
            }
        }
        
        // Pemain ditemukan dan online
        plugin.getGUIManager().openAdminViewGUI(admin, targetPlayer.getUniqueId(), targetPlayer.getName());
        sendMessage(admin, "§aᴍᴇᴍʙᴜᴋᴀ ʙᴀᴄᴋᴘᴀᴄᴋ §e" + targetPlayer.getName() + "§a.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommand suggestions
            List<String> subCommands = new ArrayList<>();
            subCommands.add("help");
            
            if (sender instanceof Player) {
                subCommands.add("upgrade");
                subCommands.add("special");
                subCommands.add("shop");
            }
            
            if (sender.hasPermission("neonbackpack.admin")) {
                subCommands.add("give");
                subCommands.add("reload");
                subCommands.add("view");
            }
            
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            // Argumen kedua
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("neonbackpack.admin")) {
                // Nama pemain
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> StringUtil.startsWithIgnoreCase(name, args[1]))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("view") && sender.hasPermission("neonbackpack.admin")) {
                // Nama pemain
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> StringUtil.startsWithIgnoreCase(name, args[1]))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("special") && sender instanceof Player) {
                // Tipe special backpack
                List<String> types = Arrays.asList("MINING", "FARMING", "COMBAT");
                StringUtil.copyPartialMatches(args[1], types, completions);
            }
        } else if (args.length == 3) {
            // Argumen ketiga
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("neonbackpack.admin")) {
                // Tipe backpack, termasuk special types
                List<String> types = new ArrayList<>(
                    Arrays.asList("BASIC", "ADVANCED", "PREMIUM", "ULTIMATE", "MINING", "FARMING", "COMBAT")
                );
                StringUtil.copyPartialMatches(args[2], types, completions);
            }
        }
        
        Collections.sort(completions);
        return completions;
    }
    
    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "§b[NusaBackpack] §f");
    }
    
    private String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "§cᴘᴇʀɪɴᴛᴀʜ ᴛɪᴅᴀᴋ ᴅᴀᴘᴀᴛ ᴅɪᴘʀᴏsᴇs.");
    }
    
    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(getPrefix() + message);
    }
}