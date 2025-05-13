package id.neonbackpack.listeners;

import id.neonbackpack.NeonBackPack;
import id.neonbackpack.BackpackManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listener untuk mencegah pencurian backpack
 */
public class BackpackProtectionListener implements Listener {

    private final NeonBackPack plugin;
    private final BackpackManager backpackManager;

    public BackpackProtectionListener(NeonBackPack plugin) {
        this.plugin = plugin;
        this.backpackManager = plugin.getBackpackManager();
    }

    /**
     * Mencegah pemain mengambil backpack dari inventory pemain lain
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!plugin.getConfig().getBoolean("settings.enable-anti-theft", true)) {
                return;
            }

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            
            // Pengecualian untuk admin
            if (player.hasPermission("neonbackpack.admin.bypass")) {
                return;
            }

            // Cek apakah ini inventory dari pemain lain
            if (event.getView().getTopInventory().getHolder() instanceof Player) {
                Player inventoryOwner = (Player) event.getView().getTopInventory().getHolder();

                // Jika bukan inventori pemain itu sendiri
                if (!inventoryOwner.getUniqueId().equals(player.getUniqueId())) {
                    ItemStack clickedItem = event.getCurrentItem();
                    
                    // Jika mencoba mengambil backpack dari inventory pemain lain
                    if (clickedItem != null && backpackManager.isBackpack(clickedItem)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.cannot-take-backpack", "§cAnda tidak dapat mengambil backpack pemain lain!"));
                    }
                }
            }
            
            // Cek untuk pengambilan armor (helm) yang berupa backpack
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
                Inventory clickedInventory = event.getClickedInventory();
                
                if (clickedInventory != null && clickedInventory.getHolder() instanceof Player) {
                    Player armorOwner = (Player) clickedInventory.getHolder();
                    
                    // Jika bukan armor diri sendiri
                    if (!armorOwner.getUniqueId().equals(player.getUniqueId())) {
                        ItemStack clickedItem = event.getCurrentItem();
                        
                        // Jika mencoba mengambil backpack dari slot helm
                        if (clickedItem != null && backpackManager.isBackpack(clickedItem)) {
                            event.setCancelled(true);
                            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                    plugin.getConfig().getString("messages.cannot-take-backpack", "§cAnda tidak dapat mengambil backpack pemain lain!"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error di onInventoryClick (BackpackProtectionListener): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Mencegah pemain mengambil backpack dari armor stand
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        try {
            if (!plugin.getConfig().getBoolean("settings.enable-anti-theft", true)) {
                return;
            }
            
            Player player = event.getPlayer();
            
            // Pengecualian untuk admin
            if (player.hasPermission("neonbackpack.admin.bypass")) {
                return;
            }
            
            ItemStack armorItem = event.getArmorStandItem();
            
            // Jika mencoba mengambil backpack dari armor stand
            if (armorItem != null && backpackManager.isBackpack(armorItem)) {
                // Cek apakah armor stand ini milik pemain
                String ownerTag = event.getRightClicked().getScoreboardTags().stream()
                        .filter(tag -> tag.startsWith("backpack_owner:"))
                        .findFirst()
                        .orElse(null);
                
                // Jika tidak ada tag pemilik atau bukan milik pemain ini
                if (ownerTag == null || !ownerTag.equals("backpack_owner:" + player.getUniqueId().toString())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.cannot-take-backpack", "§cAnda tidak dapat mengambil backpack pemain lain!"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error di onArmorStandManipulate: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Mencegah backpack jatuh ketika pemain mati
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            if (!plugin.getConfig().getBoolean("settings.protect-backpack-on-death", true)) {
                return;
            }
            
            Player player = event.getEntity();
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
            List<ItemStack> backpacks = new ArrayList<>();
            
            // Identifikasi semua backpack di antara barang yang akan jatuh
            for (ItemStack item : drops) {
                if (item != null && backpackManager.isBackpack(item)) {
                    backpacks.add(item);
                }
            }
            
            // Hapus backpack dari daftar yang akan jatuh
            for (ItemStack backpack : backpacks) {
                event.getDrops().remove(backpack);
                
                // Simpan backpack ke database jika diperlukan
                // (Ini opsional, tergantung apakah Anda ingin memastikan konten backpack tetap aman)
                String backpackType = backpackManager.getBackpackType(backpack);
                saveBackpackContent(player, backpackType);
                
                // Log aktivitas
                plugin.getLogger().info("Melindungi backpack " + backpack.getItemMeta().getDisplayName() + " milik " + player.getName() + " dari jatuh saat kematian");
            }
            
            // Beri tahu pemain jika backpack mereka dilindungi
            if (!backpacks.isEmpty()) {
                // Jadwalkan pesan untuk ditampilkan setelah respawn
                UUID playerUuid = player.getUniqueId();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player respawnedPlayer = Bukkit.getPlayer(playerUuid);
                    if (respawnedPlayer != null && respawnedPlayer.isOnline()) {
                        respawnedPlayer.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.backpack-protected-death", 
                                        "§aBackpack Anda telah dilindungi dan akan dikembalikan saat respawn!"));
                        
                        // Kembalikan backpack ke inventory pemain
                        for (ItemStack backpack : backpacks) {
                            if (respawnedPlayer.getInventory().firstEmpty() != -1) {
                                respawnedPlayer.getInventory().addItem(backpack);
                            } else {
                                // Jika inventory penuh, letakkan di lokasi respawn
                                respawnedPlayer.getWorld().dropItemNaturally(respawnedPlayer.getLocation(), backpack);
                                respawnedPlayer.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                        "§eBackpack Anda dijatuhkan di dekat Anda karena inventory penuh!");
                            }
                        }
                    }
                }, 20L); // Delay 1 detik setelah respawn
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error di onPlayerDeath: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Mencegah pemain mengambil backpack yang bukan miliknya dari tanah
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        try {
            if (!plugin.getConfig().getBoolean("settings.enable-anti-theft", true) || 
                !plugin.getConfig().getBoolean("settings.prevent-pickup-dropped-backpack", false)) {
                return;
            }
            
            if (!(event.getEntity() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getEntity();
            
            // Pengecualian untuk admin
            if (player.hasPermission("neonbackpack.admin.bypass")) {
                return;
            }
            
            ItemStack item = event.getItem().getItemStack();
            
            // Jika item adalah backpack
            if (backpackManager.isBackpack(item)) {
                // Cek apakah item memiliki tag "owner"
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "owner"),
                        org.bukkit.persistence.PersistentDataType.STRING)) {
                    
                    String ownerUuid = item.getItemMeta().getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "owner"),
                            org.bukkit.persistence.PersistentDataType.STRING);
                    
                    // Jika bukan miliknya
                    if (!player.getUniqueId().toString().equals(ownerUuid)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.cannot-pickup-backpack", 
                                        "§cAnda tidak dapat mengambil backpack yang bukan milik Anda!"));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error di onItemPickup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method untuk menyimpan konten backpack ke database
     */
    private void saveBackpackContent(Player player, String backpackType) {
        try {
            // Ambil data backpack saat ini
            ItemStack[] contents = plugin.getDatabaseManager().getPlayerBackpackContents(player.getUniqueId());
            int size = plugin.getDatabaseManager().getBackpackSize(backpackType);
            
            // Simpan ke database
            plugin.getDatabaseManager().savePlayerBackpack(
                player.getUniqueId(), 
                backpackType, 
                contents, 
                size
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Error menyimpan backpack untuk " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}