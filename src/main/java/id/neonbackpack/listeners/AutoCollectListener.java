package id.neonbackpack.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import id.neonbackpack.NeonBackPack;

public class AutoCollectListener implements Listener {
    
    private final NeonBackPack plugin;
    
    public AutoCollectListener(NeonBackPack plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        try {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getEntity();
            ItemStack item = event.getItem().getItemStack();
            
            if (item == null || item.getType() == Material.AIR) {
                return; // Skip empty items
            }
            
            // Cek dulu apakah auto-collect diaktifkan untuk pemain ini
            if (!plugin.getDatabaseManager().isAutoCollectEnabled(player.getUniqueId())) {
                return; // Skip jika auto-collect dinonaktifkan oleh pemain
            }
            
            // Coba masukkan item ke backpack khusus jika memungkinkan
            boolean added = plugin.getSpecialBackpackManager().tryAddItemToPlayerSpecialBackpack(player, item);
            
            if (added) {
                // Cancel event karena item sudah dimasukkan ke backpack
                event.setCancelled(true);
                event.getItem().remove(); // Hapus item dari dunia
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onPlayerPickupItem: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            
            // Cek apakah inventory yang dibuka adalah backpack
            String title = event.getView().getTitle();
            if (title.contains("NeonBackpack:")) {
                Player player = (Player) event.getWhoClicked();
                
                // Jika sedang mencoba memasukkan item ke backpack
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    // Ambil tipe backpack pemain dari database
                    String backpackType = plugin.getDatabaseManager().getPlayerBackpackType(player.getUniqueId());
                    
                    // Validasi item yang akan dimasukkan
                    if (!plugin.getBackpackManager().canAddItemToBackpack(backpackType, event.getCursor())) {
                        event.setCancelled(true);
                        String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
                        player.sendMessage(
                            plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.backpack-item-not-allowed", "Item ini tidak bisa disimpan dalam %backpack%!")
                                .replace("%backpack%", displayName)
                        );
                        return;
                    }
                }
                
                // Jika mencoba shift-click item dari inventory pemain ke backpack
                if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    String backpackType = plugin.getDatabaseManager().getPlayerBackpackType(player.getUniqueId());
                    
                    // Validasi item yang akan dimasukkan dengan shift-click
                    if (!plugin.getBackpackManager().canAddItemToBackpack(backpackType, event.getCurrentItem())) {
                        event.setCancelled(true);
                        String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
                        player.sendMessage(
                            plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.backpack-item-not-allowed", "Item ini tidak bisa disimpan dalam %backpack%!")
                                .replace("%backpack%", displayName)
                        );
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in onInventoryClick: " + e.getMessage());
            e.printStackTrace();
        }
    }
}