package id.neonbackpack.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import id.neonbackpack.NeonBackPack;
import id.neonbackpack.BackpackManager;

public class BackpackListener implements Listener {
    
    private final NeonBackPack plugin;
    private final BackpackManager backpackManager;
    
    public BackpackListener(NeonBackPack plugin) {
        this.plugin = plugin;
        this.backpackManager = plugin.getBackpackManager();
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        // Cek apakah inventory yang ditutup adalah backpack
        if (title.contains("NusaBackpack:") || title.contains("NeonBackpack:")) {
            // Simpan backpack
            backpackManager.saveBackpack(player);
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.backpack-closed"));
            // Efek suara saat menutup
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            
            // Cek klik kanan di udara
            boolean isRightClickAir = (event.getAction() == Action.RIGHT_CLICK_AIR);
            
            // Kasus 1: Item di tangan adalah backpack
            if (item != null && backpackManager.isBackpack(item)) {
                // Hanya cek untuk klik kanan
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    openBackpackFromItem(player, item);
                    // Efek suara saat membuka
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                }
                return;
            }
            
            // Kasus 2: Player klik kanan di udara dan memiliki backpack di slot helm
            if (isRightClickAir) {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null && backpackManager.isBackpack(helmet)) {
                    event.setCancelled(true);
                    openBackpackFromItem(player, helmet);
                    // Efek suara saat membuka
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onPlayerInteract: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Metode helper untuk membuka backpack
     */
    private void openBackpackFromItem(Player player, ItemStack backpackItem) {
        try {
            // Pastikan player memiliki izin untuk membuka backpack
            if (!player.hasPermission("neonbackpack.use")) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.no-permission"));
                // Efek suara kesalahan
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            // Deteksi tipe backpack dari item
            String backpackType = backpackManager.getBackpackType(backpackItem);
            
            // Cek permission untuk tipe backpack spesifik
            if (!player.hasPermission("neonbackpack." + backpackType.toLowerCase())) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.no-permission"));
                // Efek suara kesalahan
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            // Update tipe backpack di database jika berbeda dari yang tersimpan
            String currentType = plugin.getDatabaseManager().getPlayerBackpackType(player.getUniqueId());
            if (!backpackType.equals(currentType)) {
                // Simpan isi backpack saat ini
                ItemStack[] currentContents = plugin.getDatabaseManager().getPlayerBackpackContents(player.getUniqueId());
                int newSize = plugin.getDatabaseManager().getBackpackSize(backpackType);
                
                // Update tipe backpack
                plugin.getDatabaseManager().savePlayerBackpack(
                    player.getUniqueId(),
                    backpackType,
                    currentContents,
                    newSize
                );
                
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                        "§aᴛɪᴘᴇ ʙᴀᴄᴋᴘᴀᴄᴋ ᴅɪᴘᴇʀʙᴀʀᴜɪ ᴍᴇɴᴊᴀᴅɪ " + backpackType + "!");
                // Efek suara pengaturan berubah
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            }
            
            // Buka backpack untuk pemain
            backpackManager.openBackpack(player);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening backpack from item: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Cek apakah inventory yang dibuka adalah backpack
        String title = event.getView().getTitle();
        if (title.contains("NusaBackpack:") || title.contains("NeonBackpack:")) {
            // Cek apakah mencoba memasukkan backpack ke dalam backpack (mencegah nested backpack)
            if (event.getCursor() != null && backpackManager.isBackpack(event.getCursor())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                        "§cᴀɴᴅᴀ ᴛɪᴅᴀᴋ ᴅᴀᴘᴀᴛ ᴍᴇᴍᴀsᴜᴋᴋᴀɴ ʙᴀᴄᴋᴘᴀᴄᴋ ᴋᴇ ᴅᴀʟᴀᴍ ʙᴀᴄᴋᴘᴀᴄᴋ ʟᴀɪɴ!");
                // Efek suara kesalahan
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            // Cek untuk item yang ingin dipindahkan (shift-click)
            if (event.getCurrentItem() != null && backpackManager.isBackpack(event.getCurrentItem()) && 
                    event.isShiftClick()) {
                event.setCancelled(true);
                // Efek suara kesalahan
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                return;
            }
        } else {
            // Jika bukan di backpack inventory, cek untuk shift-click pada backpack
            if (event.getCurrentItem() != null && backpackManager.isBackpack(event.getCurrentItem()) && 
                    event.isShiftClick()) {
                // Mencoba shift-click backpack ke inventory lain, tolak jika itu adalah backpack inventory
                Inventory clickedInv = event.getClickedInventory();
                InventoryHolder holder = clickedInv != null ? clickedInv.getHolder() : null;
                
                if (holder instanceof Player && (event.getView().getTitle().contains("NusaBackpack:") || 
                                              event.getView().getTitle().contains("NeonBackpack:"))) {
                    event.setCancelled(true);
                    // Efek suara kesalahan
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Simpan backpack jika pemain memiliki backpack yang terbuka
        backpackManager.saveBackpack(player);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Cek apakah ini adalah pemain baru dan opsi pemberian backpack otomatis diaktifkan
        if (!player.hasPlayedBefore() && plugin.getConfig().getBoolean("settings.give-new-player-backpack", false)) {
            // Berikan basic backpack ke pemain baru
            ItemStack backpackItem = backpackManager.createBackpackItem("BASIC", player);
            player.getInventory().addItem(backpackItem);
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.backpack-received"));
            
            // Efek suara hadiah
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            player.sendMessage("§b§l✦ §aSelamat datang di server! §b§l✦");
            player.sendMessage("§7Anda telah menerima §fBasic Backpack §7sebagai hadiah selamat datang!");
        }
    }
}