package id.neonbackpack.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.neonbackpack.NeonBackPack;
import id.neonbackpack.gui.BackpackGUI;
import id.neonbackpack.gui.BackpackGUI.ButtonAction;
import id.neonbackpack.gui.ShopGUI;
import id.neonbackpack.gui.AdminBackpackGUI;

public class GUIListener implements Listener {
    
    private final NeonBackPack plugin;
    
    public GUIListener(NeonBackPack plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getWhoClicked();
            
            // Tambahkan pengecekan untuk slot yang dinonaktifkan
            if (event.getClickedInventory() != null && 
                event.getClickedInventory().equals(event.getView().getTopInventory()) && 
                isDisabledSlot(event.getCurrentItem())) {
                
                event.setCancelled(true);
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                "§cᴀɴᴅᴀ ᴛɪᴅᴀᴋ ᴅᴀᴘᴀᴛ ᴍᴇɴɢɢᴜɴᴀᴋᴀɴ sʟᴏᴛ ʏᴀɴɢ ᴛɪᴅᴀᴋ ᴛᴇʀsᴇᴅɪᴀ!");
                // Tambahkan efek suara untuk feedback
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }
            
            // Cek untuk Admin GUI
            if (plugin.getGUIManager().isAdminGUI(event.getInventory())) {
                event.setCancelled(true); // Selalu batalkan perubahan di GUI Admin (view only)
                
                // Get clicked slot
                int slot = event.getRawSlot();
                
                // Check if clicked on a button
                AdminBackpackGUI gui = plugin.getGUIManager().getAdminGUI(event.getInventory());
                if (gui != null && gui.isButton(slot)) {
                    AdminBackpackGUI.GUIButton button = gui.getButton(slot);
                    
                    // Handle button action
                    switch (button.getAction()) {
                        case PREVIOUS_PAGE:
                            gui.previousPage();
                            // Tambahkan efek suara untuk navigasi halaman
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                            break;
                        case NEXT_PAGE:
                            gui.nextPage();
                            // Tambahkan efek suara untuk navigasi halaman
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                            break;
                        default:
                            break;
                    }
                }
                return;
            }
            
            // Cek apakah inventory adalah Shop GUI
            if (plugin.getGUIManager().isShopGUI(event.getView().getTopInventory())) {
                ShopGUI shopGUI = plugin.getGUIManager().getShopGUI(event.getView().getTopInventory());
                shopGUI.handleClick(event);
                // Tambahkan efek suara saat berinteraksi dengan shop
                if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                }
                return;
            }
            
            Inventory clickedInv = event.getClickedInventory();
            
            // Cek apakah inventory adalah GUI backpack
            if (clickedInv != null && plugin.getGUIManager().isBackpackGUI(event.getView().getTopInventory())) {
                BackpackGUI gui = plugin.getGUIManager().getBackpackGUI(event.getView().getTopInventory());
                
                // Cek apakah klik pada baris tombol (baris terakhir)
                int lastRowStart = Math.max(9, clickedInv.getSize() - 9);
                if (event.getSlot() >= lastRowStart && event.getSlot() < clickedInv.getSize() && clickedInv.equals(event.getView().getTopInventory())) {
                    event.setCancelled(true); // Batalkan klik pada baris tombol
                    
                    // Cek apakah slot adalah tombol
                    if (gui.isButton(event.getSlot())) {
                        BackpackGUI.GUIButton button = gui.getButton(event.getSlot());
                        
                        // Eksekusi aksi tombol
                        switch (button.getAction()) {
                            case SORT:
                                gui.sortItems();
                                // Efek suara saat mengurutkan item
                                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.5f);
                                break;
                            case PREVIOUS_PAGE:
                                gui.previousPage();
                                // Efek suara saat halaman berubah
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                                break;
                            case NEXT_PAGE:
                                gui.nextPage();
                                // Efek suara saat halaman berubah
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                                break;
                            case FILTER:
                                gui.cycleFilter();
                                // Efek suara saat memfilter
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
                                break;
                            case COMPRESS:
                                gui.compressItems();
                                // Efek suara saat mengompresi item
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.5f);
                                break;
                            case TOGGLE_AUTO_COLLECT:
                                gui.toggleAutoCollect();
                                // Efek suara saat toggle auto-collect
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.8f);
                                break;
                            default:
                                // Default sound for other buttons
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                                break;
                        }
                    }
                }
                
                // Cek apakah mencoba memasukkan backpack ke dalam backpack
                if (event.getCursor() != null && plugin.getBackpackManager().isBackpack(event.getCursor())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                       "§cᴀɴᴅᴀ ᴛɪᴅᴀᴋ ᴅᴀᴘᴀᴛ ᴍᴇᴍᴀsᴜᴋᴋᴀɴ ʙᴀᴄᴋᴘᴀᴄᴋ ᴋᴇ ᴅᴀʟᴀᴍ ʙᴀᴄᴋᴘᴀᴄᴋ ʟᴀɪɴ!");
                    // Efek suara kesalahan
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return;
                }
                
                // Validasi item untuk special backpack
                if (event.getCurrentItem() == null || event.isShiftClick()) {
                    return; // Tidak perlu validasi untuk item yang diambil atau shift-click
                }
                
                String backpackType = gui.getBackpackType();
                
                // Jika mencoba menaruh item di GUI special backpack
                if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType) && 
                    event.getCursor() != null && 
                    event.getCursor().getType() != org.bukkit.Material.AIR) {
                    
                    // Validasi item
                    if (!plugin.getBackpackManager().canAddItemToBackpack(backpackType, event.getCursor())) {
                        event.setCancelled(true);
                        String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
                        player.sendMessage(
                            plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.backpack-item-not-allowed", "ɪᴛᴇᴍ ɪɴɪ ᴛɪᴅᴀᴋ ʙɪsᴀ ᴅɪsɪᴍᴘᴀɴ ᴅᴀʟᴀᴍ %backpack%!")
                                .replace("%backpack%", displayName)
                        );
                        // Efek suara kesalahan
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    } else {
                        // Efek suara saat memasukkan item yang valid
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                    }
                }
            }
            
            // Cek shift-click dari player inventory ke backpack GUI
            if (event.isShiftClick() && 
                event.getCurrentItem() != null && 
                clickedInv != event.getView().getTopInventory() && 
                plugin.getGUIManager().isBackpackGUI(event.getView().getTopInventory())) {
                
                BackpackGUI gui = plugin.getGUIManager().getBackpackGUI(event.getView().getTopInventory());
                String backpackType = gui.getBackpackType();
                
                // Validasi item untuk special backpack
                if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType) && 
                    !plugin.getBackpackManager().canAddItemToBackpack(backpackType, event.getCurrentItem())) {
                    
                    event.setCancelled(true);
                    String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
                    player.sendMessage(
                        plugin.getConfig().getString("messages.prefix") + 
                        plugin.getConfig().getString("messages.backpack-item-not-allowed", "ɪᴛᴇᴍ ɪɴɪ ᴛɪᴅᴀᴋ ʙɪsᴀ ᴅɪsɪᴍᴘᴀɴ ᴅᴀʟᴀᴍ %backpack%!")
                            .replace("%backpack%", displayName)
                    );
                    // Efek suara kesalahan
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                } else {
                    // Efek suara saat shift-click item yang valid
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onInventoryClick: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getWhoClicked();
            
            // Cek apakah inventory adalah GUI backpack
            if (plugin.getGUIManager().isBackpackGUI(event.getView().getTopInventory())) {
                BackpackGUI gui = plugin.getGUIManager().getBackpackGUI(event.getView().getTopInventory());
                
                // Batalkan drag pada baris tombol (baris terakhir)
                int lastRowStart = Math.max(9, event.getView().getTopInventory().getSize() - 9);
                
                for (int slot : event.getRawSlots()) {
                    // Batalkan jika mencoba drag ke baris tombol
                    if (slot >= lastRowStart && slot < event.getView().getTopInventory().getSize()) {
                        event.setCancelled(true);
                        // Efek suara kesalahan
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                        return;
                    }
                    
                    // Batalkan jika mencoba drag ke slot yang tidak tersedia (yang ditandai dengan RED_STAINED_GLASS_PANE)
                    if (slot < event.getView().getTopInventory().getSize()) { // Pastikan slot ada di inventory atas
                        ItemStack item = event.getView().getTopInventory().getItem(slot);
                        if (isDisabledSlot(item)) {
                            event.setCancelled(true);
                            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                    "§cᴀɴᴅᴀ ᴛɪᴅᴀᴋ ᴅᴀᴘᴀᴛ ᴍᴇʟᴇᴛᴀᴋᴋᴀɴ ɪᴛᴇᴍ ᴅɪ sʟᴏᴛ ʏᴀɴɢ ᴛɪᴅᴀᴋ ᴛᴇʀsᴇᴅɪᴀ!");
                            // Efek suara kesalahan
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                            return;
                        }
                    }
                }
                
                // Validasi item untuk special backpack
                if (plugin.getSpecialBackpackManager().isSpecialBackpack(gui.getBackpackType())) {
                    String backpackType = gui.getBackpackType();
                    
                    // Cek semua item yang didrag
                    for (int slot : event.getRawSlots()) {
                        if (slot < event.getView().getTopInventory().getSize()) {
                            if (!plugin.getBackpackManager().canAddItemToBackpack(backpackType, event.getOldCursor())) {
                                event.setCancelled(true);
                                String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
                                player.sendMessage(
                                    plugin.getConfig().getString("messages.prefix") + 
                                    plugin.getConfig().getString("messages.backpack-item-not-allowed", "ɪᴛᴇᴍ ɪɴɪ ᴛɪᴅᴀᴋ ʙɪsᴀ ᴅɪsɪᴍᴘᴀɴ ᴅᴀʟᴀᴍ %backpack%!")
                                        .replace("%backpack%", displayName)
                                );
                                // Efek suara kesalahan
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                                return;
                            }
                        }
                    }
                }
            }
            
            // Cek untuk Admin GUI
            if (plugin.getGUIManager().isAdminGUI(event.getView().getTopInventory())) {
                // Batalkan semua drag pada Admin GUI (view only)
                for (int slot : event.getRawSlots()) {
                    if (slot < event.getView().getTopInventory().getSize()) {
                        event.setCancelled(true);
                        // Efek suara kesalahan
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                        return;
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onInventoryDrag: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        try {
            Inventory destination = event.getDestination();
            
            // Cek apakah tujuan adalah backpack GUI
            if (plugin.getGUIManager().isBackpackGUI(destination)) {
                // Cek apakah item akan diletakkan di slot yang dinonaktifkan
                int slot = destination.firstEmpty();
                if (slot != -1) {
                    ItemStack existingItem = destination.getItem(slot);
                    if (isDisabledSlot(existingItem)) {
                        event.setCancelled(true);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onInventoryMove: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Metode untuk cek apakah item adalah slot yang dinonaktifkan (RED_STAINED_GLASS_PANE dengan nama khusus)
     */
    private boolean isDisabledSlot(ItemStack item) {
        if (item != null && item.getType() == Material.RED_STAINED_GLASS_PANE && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            return meta.hasDisplayName() && meta.getDisplayName().equals("§c§lSlot Tidak Tersedia");
        }
        return false;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        try {
            if (!(event.getPlayer() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getPlayer();
            
            // Cek apakah inventory yang ditutup adalah GUI backpack
            if (plugin.getGUIManager().isBackpackGUI(event.getInventory())) {
                // Simpan dan tutup GUI
                plugin.getGUIManager().closeBackpackGUI(player.getUniqueId());
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                   plugin.getConfig().getString("messages.backpack-closed"));
                // Efek suara saat menutup backpack
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onInventoryClose: " + e.getMessage());
            e.printStackTrace();
        }
    }
}