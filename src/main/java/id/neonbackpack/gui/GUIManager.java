package id.neonbackpack.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import id.neonbackpack.NeonBackPack;

public class GUIManager {
    private final NeonBackPack plugin;
    private final Map<UUID, BackpackGUI> openGUIs;
    private final Map<UUID, ShopGUI> openShopGUIs = new HashMap<>();
    private final Map<UUID, AdminBackpackGUI> openAdminGUIs = new HashMap<>();
    
    public GUIManager(NeonBackPack plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }
    
    /**
     * Buka GUI backpack untuk pemain
     */
    public void openBackpackGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Cek apakah pemain sudah memiliki GUI yang terbuka
        if (openGUIs.containsKey(playerUUID)) {
            BackpackGUI gui = openGUIs.get(playerUUID);
            gui.refreshGUI();
            player.openInventory(gui.getInventory());
            return;
        }
        
        // Buat GUI baru untuk pemain
        BackpackGUI gui = new BackpackGUI(plugin, player);
        openGUIs.put(playerUUID, gui);
        player.openInventory(gui.getInventory());
    }
    
    /**
     * Buka GUI shop untuk pemain
     */
    public void openBackpackShopGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Buat GUI baru untuk shop
        ShopGUI gui = new ShopGUI(plugin, player);
        openShopGUIs.put(playerUUID, gui);
        player.openInventory(gui.getInventory());
    }
    
    /**
     * Buka GUI admin untuk melihat backpack pemain lain
     */
    public void openAdminViewGUI(Player admin, UUID targetUUID, String targetName) {
        UUID adminUUID = admin.getUniqueId();
        
        // Close GUI lain yang mungkin terbuka
        if (openGUIs.containsKey(adminUUID)) {
            closeBackpackGUI(adminUUID);
        }
        if (openShopGUIs.containsKey(adminUUID)) {
            closeShopGUI(adminUUID);
        }
        if (openAdminGUIs.containsKey(adminUUID)) {
            closeAdminGUI(adminUUID);
        }
        
        // Buat GUI baru untuk melihat backpack orang lain
        AdminBackpackGUI gui = new AdminBackpackGUI(plugin, admin, targetUUID, targetName);
        openAdminGUIs.put(adminUUID, gui);
        admin.openInventory(gui.getInventory());
    }
    
    /**
     * Dapatkan GUI backpack pemain
     */
    public BackpackGUI getBackpackGUI(UUID playerUUID) {
        return openGUIs.get(playerUUID);
    }
    
    /**
     * Tutup dan simpan GUI backpack pemain
     */
    public void closeBackpackGUI(UUID playerUUID) {
        if (openGUIs.containsKey(playerUUID)) {
            BackpackGUI gui = openGUIs.get(playerUUID);
            gui.saveContents();
            openGUIs.remove(playerUUID);
        }
    }
    
    /**
     * Tutup GUI shop pemain
     */
    public void closeShopGUI(UUID playerUUID) {
        openShopGUIs.remove(playerUUID);
    }
    
    /**
     * Tutup GUI admin
     */
    public void closeAdminGUI(UUID adminUUID) {
        openAdminGUIs.remove(adminUUID);
    }
    
    /**
     * Cek apakah inventory adalah GUI backpack
     */
    public boolean isBackpackGUI(Inventory inventory) {
        for (BackpackGUI gui : openGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cek apakah inventory adalah GUI shop
     */
    public boolean isShopGUI(Inventory inventory) {
        for (ShopGUI gui : openShopGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cek apakah inventory adalah GUI admin
     */
    public boolean isAdminGUI(Inventory inventory) {
        for (AdminBackpackGUI gui : openAdminGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Dapatkan GUI backpack dari inventory
     */
    public BackpackGUI getBackpackGUI(Inventory inventory) {
        for (BackpackGUI gui : openGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return gui;
            }
        }
        return null;
    }
    
    /**
     * Dapatkan GUI shop dari inventory
     */
    public ShopGUI getShopGUI(Inventory inventory) {
        for (ShopGUI gui : openShopGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return gui;
            }
        }
        return null;
    }
    
    /**
     * Dapatkan GUI admin dari inventory
     */
    public AdminBackpackGUI getAdminGUI(Inventory inventory) {
        for (AdminBackpackGUI gui : openAdminGUIs.values()) {
            if (gui.getInventory().equals(inventory)) {
                return gui;
            }
        }
        return null;
    }
    
    /**
     * Tutup semua GUI backpack
     */
    public void closeAllGUIs() {
        for (UUID playerUUID : openGUIs.keySet()) {
            BackpackGUI gui = openGUIs.get(playerUUID);
            gui.saveContents();
            
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
    }
}