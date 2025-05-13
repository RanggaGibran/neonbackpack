package id.neonbackpack.specialbackpack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import id.neonbackpack.NeonBackPack;

public class SpecialBackpackManager {
    
    private final NeonBackPack plugin;
    private final Map<String, SpecialBackpack> specialBackpacks;
    
    public SpecialBackpackManager(NeonBackPack plugin) {
        this.plugin = plugin;
        this.specialBackpacks = new HashMap<>();
        loadSpecialBackpacks();
    }
    
    /**
     * Muat semua jenis backpack khusus dari konfigurasi
     */
    private void loadSpecialBackpacks() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("backpacks.types");
        if (section == null) {
            return;
        }
        
        Set<String> keys = section.getKeys(false);
        
        for (String key : keys) {
            String specialType = section.getString(key + ".special-type");
            if (specialType != null && !specialType.isEmpty()) {
                SpecialBackpack specialBackpack = new SpecialBackpack(plugin, key);
                specialBackpacks.put(key, specialBackpack);
                plugin.getLogger().info("Loaded special backpack: " + key);
            }
        }
    }
    
    /**
     * Muat ulang semua jenis backpack khusus dari konfigurasi
     */
    public void reloadSpecialBackpacks() {
        specialBackpacks.clear();
        loadSpecialBackpacks();
        plugin.getLogger().info("Special backpack configurations reloaded.");
    }
    
    /**
     * Periksa apakah tipe backpack adalah backpack khusus
     */
    public boolean isSpecialBackpack(String backpackType) {
        return specialBackpacks.containsKey(backpackType);
    }
    
    /**
     * Dapatkan instance SpecialBackpack berdasarkan tipe
     */
    public SpecialBackpack getSpecialBackpack(String backpackType) {
        return specialBackpacks.get(backpackType);
    }
    
    /**
     * Coba masukkan item ke backpack khusus pemain jika memungkinkan
     */
    public boolean tryAddItemToPlayerSpecialBackpack(Player player, ItemStack item) {
        // Ambil tipe backpack pemain dari database
        String backpackType = plugin.getDatabaseManager().getPlayerBackpackType(player.getUniqueId());
        
        // Cek apakah tipe backpack adalah backpack khusus
        if (!isSpecialBackpack(backpackType)) {
            return false;
        }
        
        // Dapatkan special backpack pemain
        SpecialBackpack specialBackpack = getSpecialBackpack(backpackType);
        
        // Periksa apakah auto-collect diaktifkan
        if (!specialBackpack.isAutoCollect()) {
            return false;
        }
        
        // Coba tambahkan item ke backpack khusus
        return specialBackpack.tryAddItemToBackpack(player, item);
    }
    
    /**
     * Validasi apakah item diizinkan masuk ke backpack tertentu
     */
    public boolean isItemAllowedInBackpack(String backpackType, ItemStack item) {
        if (!isSpecialBackpack(backpackType)) {
            return true; // Reguler backpack allow all items
        }
        
        SpecialBackpack specialBackpack = getSpecialBackpack(backpackType);
        return specialBackpack.isItemAllowed(item);
    }
}