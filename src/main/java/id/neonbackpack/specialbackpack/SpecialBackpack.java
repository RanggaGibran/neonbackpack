package id.neonbackpack.specialbackpack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import id.neonbackpack.NeonBackPack;

public class SpecialBackpack {
    
    private final NeonBackPack plugin;
    private final String backpackType;
    private final List<Material> allowedMaterials;
    private final boolean isRegularWithSpecial;
    private final boolean autoCollect;
    private final boolean autoCompress;
    private final boolean autoSmelt; // Tambahkan property autoSmelt
    private Map<Material, Material> smeltingRecipes; // Map untuk menyimpan resep smelt
    
    public SpecialBackpack(NeonBackPack plugin, String backpackType) {
        this.plugin = plugin;
        this.backpackType = backpackType;
        this.allowedMaterials = new ArrayList<>();
        
        // Ambil tipe khusus dari konfigurasi
        FileConfiguration config = plugin.getConfig();
        String specialType = config.getString("backpacks.types." + backpackType + ".special-type", "");
        
        // Cek apakah ini backpack reguler dengan fitur khusus
        this.isRegularWithSpecial = specialType.equals("BASIC") || 
                                  specialType.equals("ADVANCED") || 
                                  specialType.equals("PREMIUM") || 
                                  specialType.equals("ULTIMATE");
        
        this.autoCollect = config.getBoolean("backpacks.types." + backpackType + ".auto-collect", false);
        this.autoCompress = config.getBoolean("backpacks.types." + backpackType + ".auto-compress", false);
        this.autoSmelt = config.getBoolean("backpacks.types." + backpackType + ".auto-smelt", false); // Baca konfigurasi auto-smelt
        
        // Jika bukan backpack reguler, load daftar material yang diizinkan
        if (!isRegularWithSpecial) {
            List<String> configMaterials = config.getStringList("backpacks.types." + backpackType + ".allowed-materials");
            for (String material : configMaterials) {
                try {
                    Material mat = Material.valueOf(material);
                    allowedMaterials.add(mat);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Material tidak valid dalam konfigurasi: " + material);
                }
            }
        }
        
        // Inisialisasi resep smelting jika auto-smelt diaktifkan
        if (autoSmelt) {
            initializeSmeltingRecipes();
        }
    }
    
    /**
     * Inisialisasi map resep smelting
     */
    private void initializeSmeltingRecipes() {
        smeltingRecipes = new HashMap<>();
        
        // Tambahkan resep smelting ore
        smeltingRecipes.put(Material.IRON_ORE, Material.IRON_INGOT);
        smeltingRecipes.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        smeltingRecipes.put(Material.RAW_IRON, Material.IRON_INGOT);
        
        smeltingRecipes.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        smeltingRecipes.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        smeltingRecipes.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        smeltingRecipes.put(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET); // Menghasilkan nugget
        
        smeltingRecipes.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        smeltingRecipes.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        smeltingRecipes.put(Material.RAW_COPPER, Material.COPPER_INGOT);
        
        smeltingRecipes.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        
        smeltingRecipes.put(Material.COBBLESTONE, Material.STONE);
        smeltingRecipes.put(Material.SAND, Material.GLASS);
        smeltingRecipes.put(Material.CLAY_BALL, Material.BRICK);
        smeltingRecipes.put(Material.CLAY, Material.TERRACOTTA);
    }
    
    /**
     * Auto-smelt pada item jika diperlukan
     * @param item Item yang akan di-smelt
     * @return Item yang sudah di-smelt atau item asli jika tidak perlu/bisa di-smelt
     */
    private ItemStack autoSmeltItem(ItemStack item) {
        if (!autoSmelt || smeltingRecipes == null) {
            return item;
        }
        
        Material result = smeltingRecipes.get(item.getType());
        if (result != null) {
            ItemStack smeltedItem = new ItemStack(result, item.getAmount());
            
            // Kasus khusus untuk Nether Gold Ore (menghasilkan lebih banyak nugget)
            if (item.getType() == Material.NETHER_GOLD_ORE) {
                smeltedItem.setAmount(item.getAmount() * 4); // Nether Gold Ore menghasilkan ~4 nugget
            }
            
            return smeltedItem;
        }
        
        return item;
    }
    
    /**
     * Cek apakah item diizinkan dalam backpack ini
     */
    public boolean isItemAllowed(ItemStack item) {
        if (isRegularWithSpecial) {
            return true; // Backpack reguler dengan fitur khusus dapat menyimpan semua item
        }
        
        return allowedMaterials.contains(item.getType());
    }
    
    /**
     * Apakah backpack ini memiliki fitur auto-collect
     */
    public boolean isAutoCollect() {
        return autoCollect;
    }
    
    /**
     * Apakah backpack ini memiliki fitur auto-smelt
     */
    public boolean isAutoSmelt() {
        return autoSmelt;
    }
    
    /**
     * Coba masukkan item ke backpack khusus pemain
     * @return true jika berhasil, false jika gagal
     */
    public boolean tryAddItemToBackpack(Player player, ItemStack item) {
        try {
            // Untuk backpack khusus, validasi item terlebih dahulu
            if (!isRegularWithSpecial && !isItemAllowed(item)) {
                return false;
            }
            
            UUID playerUuid = player.getUniqueId();
            
            // Cek apakah pemain memiliki backpack tipe ini
            String playerBackpackType = plugin.getDatabaseManager().getPlayerBackpackType(playerUuid);
            if (!playerBackpackType.equals(backpackType)) {
                return false;
            }
            
            // Ambil isi backpack saat ini
            ItemStack[] dbContents = plugin.getDatabaseManager().getPlayerBackpackContents(playerUuid);
            int fullSize = plugin.getDatabaseManager().getBackpackSize(backpackType);
            
            // Untuk ukuran simulasi, batasi maksimum 54 slot (batasan Minecraft)
            // Pastikan ukuran simulasi valid (kelipatan 9 antara 9-54)
            int simulationSize = Math.min(54, fullSize);
            simulationSize = ((simulationSize + 8) / 9) * 9;
            simulationSize = Math.min(54, Math.max(9, simulationSize));
            
            // Buat inventory sementara untuk simulasi penambahan item
            Inventory tempInv = plugin.getServer().createInventory(null, simulationSize);
            
            // Salin dengan cara yang aman
            if (dbContents.length > 0) {
                for (int i = 0; i < Math.min(dbContents.length, simulationSize); i++) {
                    if (dbContents[i] != null) {
                        tempInv.setItem(i, dbContents[i]);
                    }
                }
            }
            
            // Coba tambahkan item ke inventory sementara
            HashMap<Integer, ItemStack> leftover = new HashMap<>();
            
            // Simpan item asli
            ItemStack originalItem = item.clone();
            
            // Jika auto-smelt aktif, coba lakukan smelting pada item
            boolean smelted = false;
            if (autoSmelt && backpackType.equals("MINING")) {
                ItemStack beforeSmelt = originalItem.clone();
                originalItem = autoSmeltItem(originalItem);
                
                // Cek apakah item berubah (berhasil di-smelt)
                smelted = !beforeSmelt.getType().equals(originalItem.getType());
            }
            
            // Coba tambahkan ke inventory
            leftover = tempInv.addItem(originalItem);
            
            // Jika berhasil ditambahkan semua (tidak ada sisa)
            if (leftover.isEmpty()) {
                // Konversi kembali ke array untuk penyimpanan
                ItemStack[] newContents;
                
                if (fullSize > simulationSize) {
                    // Jika ukuran sebenarnya lebih besar dari simulasi,
                    // gunakan array asli dan perbarui hanya yang diubah
                    newContents = dbContents.length >= fullSize ? dbContents : new ItemStack[fullSize];
                    
                    // Salin perubahan dari inventory simulasi
                    ItemStack[] tempContents = tempInv.getContents();
                    for (int i = 0; i < tempContents.length; i++) {
                        newContents[i] = tempContents[i];
                    }
                } else {
                    // Jika ukuran simulasi sama dengan ukuran sebenarnya
                    newContents = new ItemStack[fullSize];
                    ItemStack[] tempContents = tempInv.getContents();
                    
                    for (int i = 0; i < fullSize; i++) {
                        newContents[i] = i < tempContents.length ? tempContents[i] : null;
                    }
                }
                
                // Jika auto-compress aktif, lakukan kompresi pada inventory
                if (autoCompress) {
                    // Pastikan ukuran simulasi valid untuk kompres (kelipatan 9 antara 9-54)
                    int compressSize = ((Math.min(fullSize, 54) + 8) / 9) * 9;
                    compressSize = Math.min(54, Math.max(9, compressSize));
                    
                    // Buat inventory simulasi baru dengan ukuran valid untuk kompresi
                    Inventory compressInv = plugin.getServer().createInventory(null, compressSize);
                    
                    // Salin item yang ada ke inventory kompresi
                    for (int i = 0; i < Math.min(newContents.length, compressSize); i++) {
                        if (newContents[i] != null) {
                            compressInv.setItem(i, newContents[i]);
                        }
                    }
                    
                    // Lakukan kompresi
                    compressInv = compressItems(compressInv);
                    
                    // Salin hasil kompresi kembali ke array newContents
                    ItemStack[] compressedContents = compressInv.getContents();
                    for (int i = 0; i < Math.min(compressedContents.length, newContents.length); i++) {
                        newContents[i] = compressedContents[i];
                    }
                }
                
                // Simpan perubahan ke database
                plugin.getDatabaseManager().savePlayerBackpack(
                    playerUuid, 
                    backpackType, 
                    newContents, 
                    fullSize
                );
                
                // Jika item berhasil di-smelt, berikan notifikasi ke pemain
                if (smelted) {
                    String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
                    player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                      plugin.getConfig().getString("messages.backpack-auto-smelted", "§aItem otomatis di-smelt dalam %backpack%!")
                                          .replace("%backpack%", displayName));
                }
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Error in tryAddItemToBackpack: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kompres item dalam inventory berdasarkan resep kompresi
     */
    public Inventory compressItems(Inventory inventory) {
        if (!plugin.getConfig().getBoolean("compression.enabled", true)) {
            return inventory;
        }
        
        // Pastikan ukuran inventory valid
        if (inventory.getSize() % 9 != 0 || inventory.getSize() < 9 || inventory.getSize() > 54) {
            plugin.getLogger().warning("Mencoba kompresi dengan ukuran inventory tidak valid: " + inventory.getSize());
            // Buat inventory baru dengan ukuran valid jika yang diberikan tidak valid
            int validSize = ((inventory.getSize() + 8) / 9) * 9;
            validSize = Math.min(54, Math.max(9, validSize));
            
            Inventory validInventory = plugin.getServer().createInventory(null, validSize);
            for (int i = 0; i < Math.min(inventory.getSize(), validSize); i++) {
                validInventory.setItem(i, inventory.getItem(i));
            }
            inventory = validInventory;
        }
        
        List<List<Object>> recipes = getCompressionRecipes();
        for (List<Object> recipe : recipes) {
            try {
                Material sourceMaterial = Material.valueOf((String) recipe.get(0));
                int amount = (int) recipe.get(1);
                Material resultMaterial = Material.valueOf((String) recipe.get(2));
                
                // Temukan semua slot dengan material sumber
                List<Integer> slots = new ArrayList<>();
                int totalItems = 0;
                
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.getType() == sourceMaterial) {
                        slots.add(i);
                        totalItems += item.getAmount();
                    }
                }
                
                // Lanjutkan jika ada cukup item untuk dikompresi
                while (totalItems >= amount) {
                    // Kurangi item dari inventory
                    int remaining = amount;
                    for (int i = 0; i < slots.size() && remaining > 0; i++) {
                        int slot = slots.get(i);
                        ItemStack item = inventory.getItem(slot);
                        if (item != null && item.getType() == sourceMaterial) {
                            int itemAmount = item.getAmount();
                            if (itemAmount <= remaining) {
                                // Hapus seluruh stack
                                inventory.setItem(slot, null);
                                remaining -= itemAmount;
                                totalItems -= itemAmount;
                            } else {
                                // Kurangi sebagian stack
                                item.setAmount(itemAmount - remaining);
                                totalItems -= remaining;
                                remaining = 0;
                            }
                        }
                    }
                    
                    // Tambahkan item hasil
                    ItemStack resultItem = new ItemStack(resultMaterial);
                    HashMap<Integer, ItemStack> leftover = inventory.addItem(resultItem);
                    
                    // Jika tidak ada ruang, kembalikan item sumber
                    if (!leftover.isEmpty()) {
                        ItemStack sourceItem = new ItemStack(sourceMaterial, amount);
                        inventory.addItem(sourceItem);
                        break;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error compressing items with recipe: " + recipe);
                e.printStackTrace();
            }
        }
        
        return inventory;
    }
    
    /**
     * Ambil daftar resep kompresi dari config
     */
    @SuppressWarnings("unchecked")
    private List<List<Object>> getCompressionRecipes() {
        List<List<Object>> recipes = new ArrayList<>();
        
        // Perbaikan: Ubah tipe List<List<?>> menjadi List<?> yang merupakan tipe return dari getList()
        List<?> configRecipes = plugin.getConfig().getList("compression.recipes");
        if (configRecipes != null) {
            // Perbaikan: Periksa setiap elemen dan pastikan itu List sebelum casting
            for (Object obj : configRecipes) {
                if (obj instanceof List) {
                    recipes.add((List<Object>) obj);
                } else {
                    plugin.getLogger().warning("Format resep kompresi tidak valid: " + obj);
                }
            }
        }
        
        return recipes;
    }
    
    public String getBackpackType() {
        return backpackType;
    }
    
    public boolean isAutoCompress() {
        return autoCompress;
    }
    
    public boolean isRegularWithSpecial() {
        return isRegularWithSpecial;
    }
    
    public List<Material> getAllowedMaterials() {
        return allowedMaterials;
    }
}