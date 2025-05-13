package id.neonbackpack.gui;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.neonbackpack.NeonBackPack;

public class ShopGUI {
    private final NeonBackPack plugin;
    private final Inventory inventory;
    private final Map<Integer, String> backpackTypes;
    private final Player player;

    public ShopGUI(NeonBackPack plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.backpackTypes = new HashMap<>();
        
        // Buat inventory untuk shop dengan interface yang lebih menarik
        this.inventory = Bukkit.createInventory(player, 45, "§8§l« §b§lNusa§f§lBackpack Shop §8§l»");
        
        // Setup item-item toko
        setupShopItems();
    }
    
    private void setupShopItems() {
        // Header dekoratif
        ItemStack header = createDecorItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§l✧ §f§lNusaBackpack Shop §b§l✧", 
            Arrays.asList("§7Pilih backpack yang ingin Anda beli"));
        
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, header);
        }
        
        // Tambahkan backpack reguler dengan desain yang lebih menarik
        addShopItem(19, "BASIC", Material.PAPER, 1009,
            Arrays.asList("§7• §fUkuran dasar dengan 20 slot", 
                          "§7• §fPenyimpanan umum", 
                          "§7• §fDapat menyimpan semua jenis item",
                          "",
                          "§b✨ §fCocok untuk pemain baru"));
        
        addShopItem(20, "ADVANCED", Material.PAPER, 1010,
            Arrays.asList("§7• §fKapasitas 18 slot", 
                          "§7• §fKonstruksi yang lebih kokoh",
                          "§7• §fDapat menyimpan semua jenis item",
                          "",
                          "§b✨ §fUpgrade pertama setelah Basic"));
        
        addShopItem(21, "PREMIUM", Material.PAPER, 1011,
            Arrays.asList("§7• §fKapasitas 27 slot", 
                          "§7• §fDilengkapi teknologi auto-collect",
                          "§7• §fDesign premium yang elegan",
                          "",
                          "§b✨ §fNikmati fitur auto-collect!"));
        
        addShopItem(22, "ULTIMATE", Material.PAPER, 1012,
            Arrays.asList("§7• §fKapasitas jumbo 36 slot", 
                          "§7• §fTeknologi auto-collect terintegrasi",
                          "§7• §fFitur auto-compress canggih",
                          "",
                          "§b✨ §fBackpack terbaik untuk segala situasi"));
        
        // Pembatas dekoratif tengah
        ItemStack divider = createDecorItem(Material.BLACK_STAINED_GLASS_PANE, "§8§l❈", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                if (i >= 27 && i <= 35) {
                    inventory.setItem(i, divider);
                }
            }
        }
        
        // Tambahkan special backpack dengan highlight khusus
        addShopItem(24, "MINING", Material.PAPER, 1013,
            Arrays.asList("§7• §fKapasitas 27 slot khusus pertambangan", 
                          "§7• §fAuto-collect material tambang",
                          "§7• §f§lAuto-smelt §flangsung jadi ingot!",
                          "",
                          "§b✨ §fSempurna untuk para penambang"));
        
        addShopItem(25, "FARMING", Material.PAPER, 1013,
            Arrays.asList("§7• §fKapasitas 27 slot khusus pertanian", 
                          "§7• §fAuto-collect hasil panen",
                          "§7• §fPenyimpanan hasil tanaman otomatis",
                          "",
                          "§b✨ §fIdeal untuk petani berpengalaman"));
        
        addShopItem(26, "COMBAT", Material.PAPER, 1013,
            Arrays.asList("§7• §fKapasitas 27 slot khusus pertempuran", 
                          "§7• §fAuto-collect drop monster",
                          "§7• §fKhusus untuk perlengkapan tempur",
                          "",
                          "§b✨ §fSenjata dan peralatan tempur premium"));
        
        // Informasi tambahan di bagian bawah
        ItemStack info = createDecorItem(Material.BOOK, "§e§lInformasi Pembelian", 
            Arrays.asList("§7• §fKlik pada backpack untuk membeli",
                          "§7• §fHarga tercantum pada setiap backpack",
                          "§7• §fBackpack akan tersimpan di inventory Anda"));
        inventory.setItem(40, info);
        
        // Tombol tutup
        ItemStack closeButton = createDecorItem(Material.BARRIER, "§c§lTutup", 
            Arrays.asList("§7Klik untuk menutup toko"));
        inventory.setItem(44, closeButton);
        
        // Tambahkan item dekorasi untuk slot kosong
        ItemStack glassPane = createDecorItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glassPane);
            }
        }
    }
    
    private void addShopItem(int slot, String type, Material icon, int modelData, List<String> extraLore) {
        String displayName = plugin.getConfig().getString("backpacks.types." + type + ".display-name", type);
        int slots = plugin.getConfig().getInt("backpacks.types." + type + ".slots", 9);
        double cost = plugin.getEconomyManager().getBackpackCost(type);
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        // Set custom model data
        meta.setCustomModelData(modelData);
        
        List<String> lore = new ArrayList<>();
        
        // Tambahkan lore khusus yang diberikan
        if (extraLore != null) {
            lore.addAll(extraLore);
        }
        
        // Tambahkan informasi harga dan permission
        if (plugin.getEconomyManager().isEnabled()) {
            lore.add("");
            lore.add("§f§lDetail:");
            lore.add("§7• Slots: §f" + slots);
            lore.add("§7• Harga: §f" + plugin.getEconomyManager().formatCurrency(cost));
            lore.add("§7• Permission: §fneonbackpack." + type.toLowerCase());
            lore.add("");
            lore.add("§e» Klik untuk membeli!");
        } else {
            lore.add("");
            lore.add("§f§lDetail:");
            lore.add("§7• Slots: §f" + slots);
            lore.add("§7• Permission: §fneonbackpack." + type.toLowerCase());
            lore.add("");
            lore.add("§e» Klik untuk mendapatkan!");
        }
        
        meta.setLore(lore);
        
        // Sembunyikan atribut item normal agar tampilan lebih bersih
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        // Berikan efek enchant untuk highlight (tanpa teks enchant)
        if (type.equals("PREMIUM") || type.equals("ULTIMATE") || 
            type.equals("MINING") || type.equals("FARMING") || type.equals("COMBAT")) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
        backpackTypes.put(slot, type);
    }
    
    /**
     * Membuat item dekoratif untuk GUI
     */
    private ItemStack createDecorItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (name != null) {
            meta.setDisplayName(name);
        }
        
        if (lore != null) {
            meta.setLore(lore);
        }
        
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // Tutup toko jika klik tombol tutup
        if (slot == 44) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        if (backpackTypes.containsKey(slot)) {
            String type = backpackTypes.get(slot);
            
            // Cek permission
            if (!player.hasPermission("neonbackpack." + type.toLowerCase())) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                   plugin.getConfig().getString("messages.no-permission"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                return;
            }
            
            // Proses pembelian jika Vault diaktifkan
            if (plugin.getEconomyManager().isEnabled()) {
                if (!plugin.getEconomyManager().buyBackpack(player, type)) {
                    // Pesan error sudah ditangani di dalam buyBackpack()
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return;
                }
                // Efek sukses beli
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
            
            // Update tipe backpack di database
            String currentType = plugin.getDatabaseManager().getPlayerBackpackType(player.getUniqueId());
            ItemStack[] currentContents = plugin.getDatabaseManager().getPlayerBackpackContents(player.getUniqueId());
            int newSize = plugin.getDatabaseManager().getBackpackSize(type);
            
            // Update tipe backpack
            plugin.getDatabaseManager().savePlayerBackpack(
                player.getUniqueId(),
                type,
                currentContents,
                newSize
            );
            
            // Beri backpack ke pemain
            ItemStack backpack = plugin.getBackpackManager().createBackpackItem(type);
            if (player.getInventory().firstEmpty() == -1) {
                // Inventory penuh
                player.getWorld().dropItemNaturally(player.getLocation(), backpack);
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                   plugin.getConfig().getString("messages.backpack-received") + 
                                   " §c(ɪɴᴠᴇɴᴛᴏʀʏ ᴘᴇɴᴜʜ, ɪᴛᴇᴍ ᴅɪᴊᴀᴛᴜʜᴋᴀɴ)");
            } else {
                player.getInventory().addItem(backpack);
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                   plugin.getConfig().getString("messages.backpack-received"));
            }
            
            // Tutup inventory
            player.closeInventory();
        }
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}