package id.neonbackpack;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;

import id.neonbackpack.specialbackpack.SpecialBackpack;
import id.neonbackpack.specialbackpack.SpecialBackpackManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.NamespacedKey;

public class BackpackManager {
    private final NeonBackPack plugin;
    
    public BackpackManager(NeonBackPack plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Buka backpack untuk pemain
     */
    public void openBackpack(Player player) {
        // Cek cooldown dan biaya
        if (!plugin.getCooldownManager().handleAction(player, "open")) {
            return;
        }
        
        // Gunakan GUI Manager untuk membuka backpack
        plugin.getGUIManager().openBackpackGUI(player);
    }
    
    /**
     * Simpan backpack pemain saat ditutup
     */
    public void saveBackpack(Player player) {
        // Ini dihandle oleh GUIManager
        plugin.getGUIManager().closeBackpackGUI(player.getUniqueId());
    }
    
    /**
     * Membuat item backpack dengan Custom Model Data
     */
    public ItemStack createBackpackItem(String backpackType) {
        // Gunakan PAPER daripada PLAYER_HEAD
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        // Set nama backpack sesuai tipe
        String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
        meta.setDisplayName(displayName);
        
        // Set custom model data berdasarkan tipe backpack
        int modelData;
        switch (backpackType) {
            case "BASIC":
                modelData = 1009;
                break;
            case "ADVANCED":
                modelData = 1010;
                break;
            case "PREMIUM":
                modelData = 1011;
                break;
            case "ULTIMATE":
                modelData = 1012;
                break;
            case "MINING":
            case "FARMING":
            case "COMBAT":
                modelData = 1013;
                break;
            default:
                modelData = 1009; // Default ke basic
                break;
        }
        meta.setCustomModelData(modelData);
        
        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add("§7§oKlik kanan untuk membuka");
        lore.add("§7§o" + backpackType + " Backpack");
        meta.setLore(lore);
        
        // Set tipe backpack sebagai Persistent Data Container
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "backpack-type"),
            PersistentDataType.STRING,
            backpackType
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Membuat item backpack untuk pemain tertentu
     * (Overload method untuk menetapkan pemilik)
     */
    public ItemStack createBackpackItem(String backpackType, Player owner) {
        ItemStack item = createBackpackItem(backpackType);
        ItemMeta meta = item.getItemMeta();
        
        // Set owner untuk anti-theft protection
        if (owner != null) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "owner"),
                PersistentDataType.STRING,
                owner.getUniqueId().toString()
            );
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Deteksi tipe backpack dari item menggunakan Persistant Data Container
     */
    public String getBackpackType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "BASIC";
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "backpack-type"),
                PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "backpack-type"),
                PersistentDataType.STRING
            );
        }
        
        // Fallback ke metode deteksi berdasarkan nama jika tidak ada PDC
        return detectBackpackTypeFromName(item);
    }
    
    /**
     * Deteksi tipe backpack dari nama item
     */
    private String detectBackpackTypeFromName(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return "BASIC";
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        // Cek dari konfigurasi
        for (String type : plugin.getConfig().getConfigurationSection("backpacks.types").getKeys(false)) {
            String configDisplayName = plugin.getConfig().getString("backpacks.types." + type + ".display-name", "");
            if (configDisplayName.equals(displayName) || 
                ChatColor.stripColor(configDisplayName).equals(ChatColor.stripColor(displayName))) {
                return type;
            }
        }
        
        // Cek dari kata kunci
        if (displayName.contains("Advanced")) return "ADVANCED";
        if (displayName.contains("Premium")) return "PREMIUM";
        if (displayName.contains("Ultimate")) return "ULTIMATE";
        if (displayName.contains("Mining")) return "MINING";
        if (displayName.contains("Farming")) return "FARMING";
        if (displayName.contains("Combat")) return "COMBAT";
        
        return "BASIC";
    }
    
    /**
     * Mengecek apakah item adalah backpack
     */
    public boolean isBackpack(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // Ganti pengecekan material ke PAPER
        if (item.getType() != Material.PAPER) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Cek apakah item memiliki custom model data yang sesuai
        if (!meta.hasCustomModelData()) {
            return false;
        }
        
        int modelData = meta.getCustomModelData();
        if (modelData < 1009 || modelData > 1013) {
            return false;
        }
        
        // Cek juga PDC tag untuk memastikan ini adalah backpack
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(plugin, "backpack-type"),
            PersistentDataType.STRING
        );
    }
    
    /**
     * Validasi item yang masuk ke backpack khusus
     */
    public boolean canAddItemToBackpack(String backpackType, ItemStack item) {
        SpecialBackpackManager specialManager = plugin.getSpecialBackpackManager();
        if (specialManager.isSpecialBackpack(backpackType)) {
            return specialManager.isItemAllowedInBackpack(backpackType, item);
        }
        return true; // reguler backpack menerima semua item
    }
}