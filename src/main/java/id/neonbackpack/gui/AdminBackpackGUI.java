package id.neonbackpack.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import id.neonbackpack.NeonBackPack;
import id.neonbackpack.specialbackpack.SpecialBackpack;

/**
 * GUI khusus Admin untuk melihat isi backpack pemain lain
 */
public class AdminBackpackGUI {
    
    private final NeonBackPack plugin;
    private final Player admin;
    private final UUID targetUUID;
    private final String targetName;
    private final Inventory inventory;
    private final String backpackType;
    private final int contentSize;
    private final int guiSize;
    private final Map<Integer, GUIButton> buttons;
    private ItemStack[] contents;
    private int currentPage = 0;
    private int maxPage = 0;
    private String currentFilter = "ALL";
    
    // Daripada menggunakan slot tetap, kita gunakan fungsi dinamis berdasarkan ukuran
    private int getPrevPageSlot() {
        return Math.max(9, guiSize - 9) + 1; // Slot kedua di baris terakhir
    }

    private int getInfoSlot() {
        return Math.max(9, guiSize - 9) + 4; // Slot kelima di baris terakhir
    }

    private int getNextPageSlot() {
        return Math.max(9, guiSize - 9) + 7; // Slot kedelapan di baris terakhir
    }
    
    /**
     * Buat GUI Admin untuk backpack pemain lain
     */
    public AdminBackpackGUI(NeonBackPack plugin, Player admin, UUID targetUUID, String targetName) {
        this.plugin = plugin;
        this.admin = admin;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.backpackType = plugin.getDatabaseManager().getPlayerBackpackType(targetUUID);
        this.contentSize = plugin.getDatabaseManager().getBackpackSize(backpackType);
        
        // Pastikan ukuran GUI selalu kelipatan 9 dan cukup untuk tombol
        int displayRows = (int) Math.ceil((double) contentSize / 9) + 1; // Tambah 1 baris untuk tombol
        this.guiSize = Math.min(54, displayRows * 9); // Batas maksimum Minecraft 54 slot (6 baris)
        
        this.buttons = new HashMap<>();
        
        // Buat inventory dengan ukuran yang sesuai
        String title = "§8§l« §c§lAdmin: §f" + targetName + " §8§l»";
        
        // Tambahkan info tipe backpack
        String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
        title += " [" + ChatColor.stripColor(displayName) + "]";
        
        this.inventory = Bukkit.createInventory(admin, guiSize, title);
        
        // Ambil isi backpack dari database
        ItemStack[] dbContents = plugin.getDatabaseManager().getPlayerBackpackContents(targetUUID);
        
        // Inisialisasi array contents dengan ukuran yang tepat
        this.contents = new ItemStack[contentSize];
        
        // Salin isi dari database ke array contents dengan panjang yang benar
        if (dbContents.length > 0) {
            for (int i = 0; i < Math.min(dbContents.length, contentSize); i++) {
                contents[i] = dbContents[i];
            }
        }
        
        // Hitung jumlah halaman
        int contentSlots = guiSize - 9; // Kurangi 1 baris untuk tombol
        this.maxPage = (int) Math.ceil((double) contentSize / contentSlots) - 1;
        if (this.maxPage < 0) this.maxPage = 0;
        
        // Setup GUI
        setupGUI();
        refreshGUI();
    }
    
    /**
     * Setup tombol-tombol di GUI
     */
    private void setupGUI() {
        try {
            // Tombol halaman sebelumnya
            ItemStack prevButton = createButton(Material.ARROW, "§e§lHalaman Sebelumnya", 
                    Arrays.asList("§7Klik untuk melihat", "§7halaman sebelumnya"));
            buttons.put(getPrevPageSlot(), new GUIButton(prevButton, ButtonAction.PREVIOUS_PAGE));
            
            // Tombol info
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Pemain: §f" + targetName);
            infoLore.add("§7Tipe: §f" + backpackType);
            infoLore.add("§7Ukuran: §f" + contentSize + " slot");
            infoLore.add("§7Halaman: §f" + (currentPage + 1) + "/" + (maxPage + 1));
            
            // Tambahkan info khusus jika ini special backpack
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                boolean autoCollectEnabled = plugin.getDatabaseManager().isAutoCollectEnabled(targetUUID);
                infoLore.add("§7Auto-Collect: §f" + (specialBackpack.isAutoCollect() ? 
                             (autoCollectEnabled ? "Aktif" : "Nonaktif") : "Tidak Tersedia"));
                if (specialBackpack.isAutoCompress()) {
                    infoLore.add("§7Auto-Compress: §fAktif");
                }
                if (specialBackpack.isAutoSmelt()) {
                    infoLore.add("§7Auto-Smelt: §fAktif");
                }
                infoLore.add("§7Mode Admin: §c§lView Only");
            }
            
            ItemStack infoButton = createButton(Material.BOOK, "§b§lInfo Backpack", infoLore);
            buttons.put(getInfoSlot(), new GUIButton(infoButton, ButtonAction.INFO));
            
            // Tombol halaman berikutnya
            ItemStack nextButton = createButton(Material.ARROW, "§e§lHalaman Berikutnya", 
                    Arrays.asList("§7Klik untuk melihat", "§7halaman berikutnya"));
            buttons.put(getNextPageSlot(), new GUIButton(nextButton, ButtonAction.NEXT_PAGE));
            
            // Tambahkan dekorasi untuk slot kosong di baris tombol
            int lastRowStart = Math.max(9, guiSize - 9);
            for (int i = lastRowStart; i < guiSize; i++) {
                if (!buttons.containsKey(i)) {
                    ItemStack decorButton = createButton(Material.RED_STAINED_GLASS_PANE, "§c§lMode Admin", 
                            Arrays.asList("§7Anda melihat backpack pemain lain", "§c§oPerubahan tidak akan disimpan"));
                    buttons.put(i, new GUIButton(decorButton, ButtonAction.NONE));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in setupGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Refresh tampilan GUI
     */
    public void refreshGUI() {
        try {
            // Clear inventory
            inventory.clear();
            
            // Pastikan currentPage tidak melebihi maxPage
            if (currentPage > maxPage) {
                currentPage = maxPage;
            }
            
            // Pastikan currentPage tidak negatif
            if (currentPage < 0) {
                currentPage = 0;
            }
            
            // Hitung slot awal dan akhir untuk halaman saat ini
            int contentSlots = Math.max(9, guiSize - 9); // Kurangi 1 baris untuk tombol
            int startSlot = currentPage * contentSlots;
            int endSlot = Math.min(startSlot + contentSlots, contents.length);
            
            // Tambahkan isi backpack
            for (int i = startSlot; i < endSlot; i++) {
                if (i < contents.length && contents[i] != null) {
                    // Jika ada filter aktif, cek apakah item sesuai filter
                    if (!currentFilter.equals("ALL") && plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                        SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                        if (!specialBackpack.isItemAllowed(contents[i])) {
                            continue; // Skip item yang tidak sesuai filter
                        }
                    }
                    
                    int displaySlot = i - startSlot;
                    if (displaySlot >= 0 && displaySlot < contentSlots) {
                        inventory.setItem(displaySlot, contents[i]);
                    }
                }
            }
            
            // Tambahkan glass pane untuk slot yang melebihi contentSize
            if (currentPage == maxPage && contentSize % contentSlots != 0) {
                int lastPageUsableSlots = contentSize % contentSlots;
                if (lastPageUsableSlots == 0) lastPageUsableSlots = contentSlots; // Jika pas habis dibagi
                
                // Isi slot yang tidak digunakan dengan glass pane merah
                for (int i = lastPageUsableSlots; i < contentSlots; i++) {
                    inventory.setItem(i, createDisabledSlot());
                }
            }
            
            // Update info button dengan halaman terkini
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Pemain: §f" + targetName);
            infoLore.add("§7Tipe: §f" + backpackType);
            infoLore.add("§7Ukuran: §f" + contentSize + " slot");
            infoLore.add("§7Halaman: §f" + (currentPage + 1) + "/" + (maxPage + 1));
            
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                boolean autoCollectEnabled = plugin.getDatabaseManager().isAutoCollectEnabled(targetUUID);
                infoLore.add("§7Auto-Collect: §f" + (specialBackpack.isAutoCollect() ? 
                             (autoCollectEnabled ? "Aktif" : "Nonaktif") : "Tidak Tersedia"));
                if (specialBackpack.isAutoCompress()) {
                    infoLore.add("§7Auto-Compress: §fAktif");
                }
                if (specialBackpack.isAutoSmelt()) {
                    infoLore.add("§7Auto-Smelt: §fAktif");
                }
                infoLore.add("§7Mode Admin: §c§lView Only");
            }
            
            ItemStack infoButton = createButton(Material.BOOK, "§b§lInfo Backpack", infoLore);
            buttons.put(getInfoSlot(), new GUIButton(infoButton, ButtonAction.INFO));
            
            // Tombol halaman
            if (currentPage > 0) {
                ItemStack prevButton = createButton(Material.ARROW, "§e§lHalaman Sebelumnya", 
                        Arrays.asList("§7Klik untuk melihat", "§7halaman sebelumnya"));
                buttons.put(getPrevPageSlot(), new GUIButton(prevButton, ButtonAction.PREVIOUS_PAGE));
                inventory.setItem(getPrevPageSlot(), prevButton);
            } else {
                ItemStack disabledPrev = createButton(Material.GRAY_STAINED_GLASS_PANE, "§8§lHalaman Pertama", null);
                inventory.setItem(getPrevPageSlot(), disabledPrev);
            }
            
            if (currentPage < maxPage) {
                ItemStack nextButton = createButton(Material.ARROW, "§e§lHalaman Berikutnya", 
                        Arrays.asList("§7Klik untuk melihat", "§7halaman berikutnya"));
                buttons.put(getNextPageSlot(), new GUIButton(nextButton, ButtonAction.NEXT_PAGE));
                inventory.setItem(getNextPageSlot(), nextButton);
            } else {
                ItemStack disabledNext = createButton(Material.GRAY_STAINED_GLASS_PANE, "§8§lHalaman Terakhir", null);
                inventory.setItem(getNextPageSlot(), disabledNext);
            }
            
            // Terapkan semua tombol
            inventory.setItem(getInfoSlot(), infoButton);
            
            // Tambahkan dekorasi untuk slot kosong di baris tombol
            int lastRowStart = Math.max(9, guiSize - 9);
            for (int i = lastRowStart; i < guiSize; i++) {
                if (inventory.getItem(i) == null) {
                    ItemStack decorButton = createButton(Material.RED_STAINED_GLASS_PANE, "§c§lMode Admin", 
                            Arrays.asList("§7Anda melihat backpack pemain lain", "§c§oPerubahan tidak akan disimpan"));
                    inventory.setItem(i, decorButton);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in refreshGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Pindah ke halaman berikutnya
     */
    public void nextPage() {
        if (currentPage < maxPage) {
            currentPage++;
            refreshGUI();
        }
    }
    
    /**
     * Pindah ke halaman sebelumnya
     */
    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            refreshGUI();
        }
    }
    
    /**
     * Cek apakah slot tertentu adalah tombol
     */
    public boolean isButton(int slot) {
        return buttons.containsKey(slot);
    }
    
    /**
     * Dapatkan tombol di slot tertentu
     */
    public GUIButton getButton(int slot) {
        return buttons.get(slot);
    }
    
    /**
     * Buat item button dengan nama dan lore tertentu
     */
    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        
        // Tambahkan flag untuk menyembunyikan atribut
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Buat item slot yang dinonaktifkan
     */
    private ItemStack createDisabledSlot() {
        ItemStack disabledSlot = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = disabledSlot.getItemMeta();
        
        meta.setDisplayName("§c§lSlot Tidak Tersedia");
        meta.setLore(Arrays.asList("§7Slot ini melebihi kapasitas backpack"));
        
        // Tambahkan semua flag item untuk menyembunyikan properti
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        disabledSlot.setItemMeta(meta);
        return disabledSlot;
    }
    
    /**
     * Dapatkan inventory GUI
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Class untuk merepresentasikan tombol di GUI
     */
    public static class GUIButton {
        private final ItemStack itemStack;
        private final ButtonAction action;
        
        public GUIButton(ItemStack itemStack, ButtonAction action) {
            this.itemStack = itemStack;
            this.action = action;
        }
        
        public ItemStack getItemStack() {
            return itemStack;
        }
        
        public ButtonAction getAction() {
            return action;
        }
    }
    
    /**
     * Enum untuk jenis aksi tombol
     */
    public enum ButtonAction {
        NONE,
        PREVIOUS_PAGE,
        NEXT_PAGE,
        INFO
    }
}