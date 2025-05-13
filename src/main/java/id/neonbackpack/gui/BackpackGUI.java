package id.neonbackpack.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.neonbackpack.NeonBackPack;
import id.neonbackpack.specialbackpack.SpecialBackpack;

public class BackpackGUI {
    private final NeonBackPack plugin;
    private final Player owner;
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
    private int getSortButtonSlot() {
        return Math.max(9, guiSize - 9) + 0; // Slot pertama di baris terakhir
    }

    private int getPrevPageSlot() {
        return Math.max(9, guiSize - 9) + 3; // Slot keempat di baris terakhir
    }

    private int getInfoSlot() {
        return Math.max(9, guiSize - 9) + 4; // Slot tengah di baris terakhir
    }

    private int getNextPageSlot() {
        return Math.max(9, guiSize - 9) + 5; // Slot keenam di baris terakhir  
    }

    private int getFilterButtonSlot() {
        return Math.max(9, guiSize - 9) + 8; // Slot terakhir di baris terakhir
    }

    // Dapatkan slot untuk tombol kompresi
    private int getCompressButtonSlot() {
        return Math.max(9, guiSize - 9) + 6; // Slot ketujuh di baris terakhir
    }

    // Tambahkan slot baru untuk tombol toggle auto-collect
    private int getAutoCollectToggleSlot() {
        return Math.max(9, guiSize - 9) + 7; // Slot kedelapan di baris terakhir (sebelum filter)
    }
    
    /**
     * Buat GUI backpack baru
     */
    public BackpackGUI(NeonBackPack plugin, Player owner) {
        this.plugin = plugin;
        this.owner = owner;
        this.backpackType = plugin.getDatabaseManager().getPlayerBackpackType(owner.getUniqueId());
        this.contentSize = plugin.getDatabaseManager().getBackpackSize(backpackType);
        
        // Pastikan ukuran GUI selalu kelipatan 9 dan cukup untuk tombol
        int displayRows = (int) Math.ceil((double) contentSize / 9) + 1; // Tambah 1 baris untuk tombol
        this.guiSize = Math.min(54, displayRows * 9); // Batas maksimum Minecraft 54 slot (6 baris)
        
        this.buttons = new HashMap<>();
        this.currentPage = 0; // Pastikan currentPage selalu dimulai dari 0
        
        // Buat inventory dengan ukuran yang sesuai
        String title = "§8§l« §b§lNusa§f§lBackpack §8§l»";
        
        // Cek apakah ini special backpack
        if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
            String displayName = plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType);
            title = "§8§l« §b§lNusa§f§lBackpack: " + displayName + " §8§l»";
        }
        
        this.inventory = Bukkit.createInventory(owner, guiSize, title);
        
        // Ambil isi backpack dari database
        ItemStack[] dbContents = plugin.getDatabaseManager().getPlayerBackpackContents(owner.getUniqueId());
        
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
            // Tombol sortir
            ItemStack sortButton = createButton(Material.HOPPER, "§6§lSortir Item", 
                    Arrays.asList("§7Klik untuk mengurutkan item", "§7berdasarkan jenis"));
            buttons.put(getSortButtonSlot(), new GUIButton(sortButton, ButtonAction.SORT));
            
            // Tombol halaman sebelumnya
            ItemStack prevButton = createButton(Material.ARROW, "§e§lHalaman Sebelumnya", 
                    Arrays.asList("§7Klik untuk melihat", "§7halaman sebelumnya"));
            buttons.put(getPrevPageSlot(), new GUIButton(prevButton, ButtonAction.PREVIOUS_PAGE));
            
            // Tombol info
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Tipe: §f" + backpackType);
            infoLore.add("§7Ukuran: §f" + contentSize + " slot");
            infoLore.add("§7Halaman: §f" + (currentPage + 1) + "/" + (maxPage + 1));
            
            // Tambahkan info khusus jika ini special backpack atau reguler dengan kemampuan khusus
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                boolean autoCollectEnabled = plugin.getDatabaseManager().isAutoCollectEnabled(owner.getUniqueId());
                infoLore.add("§7Auto-Collect: §f" + (specialBackpack.isAutoCollect() ? 
                             (autoCollectEnabled ? "Aktif" : "Nonaktif (Toggle)") : "Tidak Tersedia"));
                if (specialBackpack.isAutoCompress()) {
                    infoLore.add("§7Auto-Compress: §fAktif");
                }
                if (specialBackpack.isAutoSmelt()) {
                    infoLore.add("§7Auto-Smelt: §fAktif");
                }
                infoLore.add("§7Filter: §f" + currentFilter);
            }
            
            ItemStack infoButton = createButton(Material.BOOK, "§b§lInfo Backpack", infoLore);
            buttons.put(getInfoSlot(), new GUIButton(infoButton, ButtonAction.INFO));
            
            // Tombol halaman berikutnya
            ItemStack nextButton = createButton(Material.ARROW, "§e§lHalaman Berikutnya", 
                    Arrays.asList("§7Klik untuk melihat", "§7halaman berikutnya"));
            buttons.put(getNextPageSlot(), new GUIButton(nextButton, ButtonAction.NEXT_PAGE));
            
            // Tombol filter dan auto-collect (hanya untuk special backpack atau reguler dengan kemampuan khusus)
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                
                // Tombol filter (hanya jika tidak reguler dengan khusus)
                if (!specialBackpack.isRegularWithSpecial() && plugin.getConfig().getBoolean("gui.show-filter-button", true)) {
                    ItemStack filterButton = createButton(Material.COMPASS, "§a§lFilter Item", 
                            Arrays.asList("§7Filter Saat Ini: §f" + currentFilter,
                                    "§7Klik untuk mengubah filter"));
                    buttons.put(getFilterButtonSlot(), new GUIButton(filterButton, ButtonAction.FILTER));
                }
                
                // Tombol kompresi (hanya jika autoCompress atau gui option aktif)
                if ((specialBackpack.isAutoCompress() || plugin.getConfig().getBoolean("gui.show-compress-button", true))) {
                    ItemStack compressButton = createButton(Material.PISTON, "§d§lKompres Item", 
                            Arrays.asList("§7Ubah 9 item menjadi 1 blok",
                                    "§7Contoh: 9 Iron Ingot → 1 Iron Block"));
                    buttons.put(getCompressButtonSlot(), new GUIButton(compressButton, ButtonAction.COMPRESS));
                }
                
                // Tombol toggle auto collect (hanya jika backpack support auto-collect)
                if (specialBackpack.isAutoCollect()) {
                    boolean autoCollectEnabled = plugin.getDatabaseManager().isAutoCollectEnabled(owner.getUniqueId());
                    Material toggleMaterial = autoCollectEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
                    String toggleName = autoCollectEnabled ? "§a§lAuto-Collect: Aktif" : "§c§lAuto-Collect: Nonaktif";
                    List<String> toggleLore = Arrays.asList(
                            "§7Status: §f" + (autoCollectEnabled ? "Aktif" : "Nonaktif"),
                            "§7Klik untuk " + (autoCollectEnabled ? "nonaktifkan" : "aktifkan") + " auto-collect");
                    
                    ItemStack toggleButton = createButton(toggleMaterial, toggleName, toggleLore);
                    if (autoCollectEnabled) {
                        toggleButton = addGlow(toggleButton); // Tambahkan efek glow jika aktif
                    }
                    buttons.put(getAutoCollectToggleSlot(), new GUIButton(toggleButton, ButtonAction.TOGGLE_AUTO_COLLECT));
                }
            } else {
                // Jika backpack biasa, tambahkan tombol kosong
                ItemStack dummyButton = createButton(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ", null);
                buttons.put(getFilterButtonSlot(), new GUIButton(dummyButton, ButtonAction.NONE));
            }
            
            // Tambahkan dekorasi untuk slot kosong di baris tombol
            int lastRowStart = Math.max(9, guiSize - 9);
            for (int i = lastRowStart; i < guiSize; i++) {
                if (!buttons.containsKey(i)) {
                    ItemStack decorButton = createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null);
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
            // Hanya pada halaman terakhir jika contentSize % contentSlots != 0
            if (currentPage == maxPage && contentSize % contentSlots != 0) {
                int lastPageUsableSlots = contentSize % contentSlots;
                if (lastPageUsableSlots == 0) lastPageUsableSlots = contentSlots; // Jika pas habis dibagi
                
                // Isi slot yang tidak digunakan dengan glass pane merah
                for (int i = lastPageUsableSlots; i < contentSlots; i++) {
                    inventory.setItem(i, createDisabledSlot());
                }
            }
            
            // Tambahkan tombol navigasi dan fungsi lainnya
            // Tombol sort
            if (plugin.getConfig().getBoolean("gui.show-sort-button", true)) {
                ItemStack sortButton = createButton(Material.HOPPER, "§6§lSortir Item", 
                        Arrays.asList("§7Klik untuk mengurutkan item", "§7berdasarkan jenis"));
                buttons.put(getSortButtonSlot(), new GUIButton(sortButton, ButtonAction.SORT));
                inventory.setItem(getSortButtonSlot(), sortButton);
            }
            
            // Tombol halaman sebelumnya
            if (plugin.getConfig().getBoolean("gui.show-pagination", true)) {
                if (currentPage > 0) {
                    ItemStack prevButton = createButton(Material.ARROW, "§e§lHalaman Sebelumnya", 
                            Arrays.asList("§7Klik untuk melihat", "§7halaman sebelumnya"));
                    buttons.put(getPrevPageSlot(), new GUIButton(prevButton, ButtonAction.PREVIOUS_PAGE));
                    inventory.setItem(getPrevPageSlot(), prevButton);
                } else {
                    int prevSlot = getPrevPageSlot();
                    if (prevSlot < inventory.getSize()) {
                        inventory.setItem(prevSlot, createButton(Material.GRAY_STAINED_GLASS_PANE, "§8§lHalaman Pertama", null));
                    }
                }
                
                if (currentPage >= maxPage) {
                    int nextSlot = getNextPageSlot();
                    if (nextSlot < inventory.getSize()) {
                        inventory.setItem(nextSlot, createButton(Material.GRAY_STAINED_GLASS_PANE, "§8§lHalaman Terakhir", null));
                    }
                } else {
                    ItemStack nextButton = createButton(Material.ARROW, "§e§lHalaman Berikutnya", 
                            Arrays.asList("§7Klik untuk melihat", "§7halaman berikutnya"));
                    buttons.put(getNextPageSlot(), new GUIButton(nextButton, ButtonAction.NEXT_PAGE));
                    inventory.setItem(getNextPageSlot(), nextButton);
                }
            }

            // Tombol info
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Tipe: §f" + backpackType);
            infoLore.add("§7Ukuran: §f" + contentSize + " slot");
            infoLore.add("§7Halaman: §f" + (currentPage + 1) + "/" + (maxPage + 1));
            
            // Tambahkan info khusus jika ini special backpack atau reguler dengan kemampuan khusus
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                boolean autoCollectEnabled = plugin.getDatabaseManager().isAutoCollectEnabled(owner.getUniqueId());
                infoLore.add("§7Auto-Collect: §f" + (specialBackpack.isAutoCollect() ? 
                             (autoCollectEnabled ? "Aktif" : "Nonaktif (Toggle)") : "Tidak Tersedia"));
                if (specialBackpack.isAutoCompress()) {
                    infoLore.add("§7Auto-Compress: §fAktif");
                }
                if (specialBackpack.isAutoSmelt()) {
                    infoLore.add("§7Auto-Smelt: §fAktif");
                }
                infoLore.add("§7Filter: §f" + currentFilter);
            }
            
            ItemStack infoButton = createButton(Material.BOOK, "§b§lInfo Backpack", infoLore);
            buttons.put(getInfoSlot(), new GUIButton(infoButton, ButtonAction.INFO));
            inventory.setItem(getInfoSlot(), infoButton);
            
            // Tambahkan tombol filter jika ini backpack khusus
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
                
                // Filter (untuk backpack khusus)
                if (!specialBackpack.isRegularWithSpecial() && plugin.getConfig().getBoolean("gui.show-filter-button", true)) {
                    ItemStack filterButton = createButton(Material.COMPASS, "§a§lFilter Item", 
                            Arrays.asList("§7Filter Saat Ini: §f" + currentFilter,
                                    "§7Klik untuk mengubah filter"));
                    buttons.put(getFilterButtonSlot(), new GUIButton(filterButton, ButtonAction.FILTER));
                    inventory.setItem(getFilterButtonSlot(), filterButton);
                }
                
                // Tombol compress
                if ((specialBackpack.isAutoCompress() || plugin.getConfig().getBoolean("gui.show-compress-button", true))) {
                    ItemStack compressButton = createButton(Material.PISTON, "§d§lKompres Item", 
                            Arrays.asList("§7Ubah 9 item menjadi 1 blok",
                                    "§7Contoh: 9 Iron Ingot → 1 Iron Block"));
                    buttons.put(getCompressButtonSlot(), new GUIButton(compressButton, ButtonAction.COMPRESS));
                    inventory.setItem(getCompressButtonSlot(), compressButton);
                }
                
                // Toggle tombol auto-collect (hanya jika backpack support auto-collect)
                if (specialBackpack.isAutoCollect()) {
                    boolean autoCollectEnabled = plugin.getDatabaseManager().isAutoCollectEnabled(owner.getUniqueId());
                    Material toggleMaterial = autoCollectEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
                    String toggleName = autoCollectEnabled ? "§a§lAuto-Collect: Aktif" : "§c§lAuto-Collect: Nonaktif";
                    List<String> toggleLore = Arrays.asList(
                            "§7Status: §f" + (autoCollectEnabled ? "Aktif" : "Nonaktif"),
                            "§7Klik untuk " + (autoCollectEnabled ? "nonaktifkan" : "aktifkan") + " auto-collect");
                    
                    ItemStack toggleButton = createButton(toggleMaterial, toggleName, toggleLore);
                    if (autoCollectEnabled) {
                        toggleButton = addGlow(toggleButton); // Tambahkan efek glow jika aktif
                    }
                    buttons.put(getAutoCollectToggleSlot(), new GUIButton(toggleButton, ButtonAction.TOGGLE_AUTO_COLLECT));
                    inventory.setItem(getAutoCollectToggleSlot(), toggleButton);
                }
            }
            
            // Terapkan dekorasi untuk slot kosong di baris tombol
            int lastRowStart = Math.max(9, guiSize - 9);
            for (int i = lastRowStart; i < guiSize; i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, createButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null));
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error in refreshGUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simpan isi backpack ke database
     */
    public void saveContents() {
        try {
            // Ambil isi inventory dan update array contents
            int contentSlots = Math.max(9, guiSize - 9);
            int startSlot = currentPage * contentSlots;
            
            for (int i = 0; i < contentSlots; i++) {
                if (i + startSlot < contents.length) {
                    contents[i + startSlot] = inventory.getItem(i);
                }
            }
            
            // Simpan ke database
            plugin.getDatabaseManager().savePlayerBackpack(
                owner.getUniqueId(),
                backpackType,
                contents,
                contentSize
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving backpack contents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Pindah ke halaman berikutnya
     */
    public void nextPage() {
        if (currentPage < maxPage) {
            saveContents(); // Simpan isi halaman saat ini
            currentPage++;
            refreshGUI();
        }
    }
    
    /**
     * Pindah ke halaman sebelumnya
     */
    public void previousPage() {
        if (currentPage > 0) {
            saveContents(); // Simpan isi halaman saat ini
            currentPage--;
            refreshGUI();
        }
    }
    
    /**
     * Urutkan item di backpack
     */
    public void sortItems() {
        try {
            // Cek cooldown dan biaya
            if (!plugin.getCooldownManager().handleAction(owner, "sort")) {
                return; // Jika cooldown aktif atau biaya tidak mencukupi
            }
            
            saveContents(); // Simpan isi saat ini
            
            List<ItemStack> itemList = new ArrayList<>();
            
            // Ambil semua item non-null
            for (ItemStack item : contents) {
                if (item != null && item.getType() != Material.AIR) {
                    itemList.add(item.clone());
                }
            }
            
            // Urutkan item berdasarkan material dan nama
            itemList.sort(Comparator
                .comparing((ItemStack item) -> item.getType().toString())
                .thenComparing(item -> {
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        return item.getItemMeta().getDisplayName();
                    }
                    return "";
                })
            );
            
            // Reset array contents (dengan safety check)
            for (int i = 0; i < contents.length; i++) {
                contents[i] = null;
            }
            
            // Isi kembali dengan item yang sudah diurutkan
            for (int i = 0; i < Math.min(itemList.size(), contents.length); i++) {
                contents[i] = itemList.get(i);
            }
            
            // Reset currentPage ke 0 setelah sorting
            currentPage = 0;
            
            // Refresh tampilan
            refreshGUI();
            
            // Beri tahu pemain
            owner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                              plugin.getConfig().getString("messages.backpack-sorted", "§aItem di backpack telah diurutkan!"));
        } catch (Exception e) {
            plugin.getLogger().severe("Error sorting backpack: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ganti filter item
     */
    public void cycleFilter() {
        try {
            // Hanya untuk special backpack
            if (!plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                return;
            }
            
            // Urutan filter: ALL -> ALLOWED -> ALL
            if (currentFilter.equals("ALL")) {
                currentFilter = "ALLOWED";
            } else {
                currentFilter = "ALL";
            }
            
            // Update tombol filter
            ItemStack filterButton = createButton(Material.COMPASS, "§a§lFilter Item", 
                    Arrays.asList("§7Filter Saat Ini: §f" + currentFilter,
                            "§7Klik untuk mengubah filter"));
            buttons.put(getFilterButtonSlot(), new GUIButton(filterButton, ButtonAction.FILTER));
            
            // Refresh tampilan
            refreshGUI();
            
            // Tampilkan pesan
            owner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                              plugin.getConfig().getString("messages.backpack-filter-changed", "§aFilter backpack telah diubah menjadi: %filter%")
                                .replace("%filter%", currentFilter));
            
            // Efek suara saat mengubah filter
            owner.playSound(owner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
        } catch (Exception e) {
            plugin.getLogger().severe("Error cycling filter: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Kompres item dalam backpack
     */
    public void compressItems() {
        try {
            // Cek cooldown dan biaya
            if (!plugin.getCooldownManager().handleAction(owner, "compress")) {
                return;
            }
            
            saveContents(); // Simpan isi saat ini
            
            if (!plugin.getConfig().getBoolean("compression.enabled", true)) {
                return;
            }
            
            // Get special backpack instance
            SpecialBackpack specialBackpack = null;
            if (plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
            }
            
            // Buat inventory sementara untuk simulasi kompresi
            Inventory tempInv = Bukkit.createInventory(null, 54); // Ukuran maksimum
            
            // Salin isi ke inventory sementara
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    tempInv.setItem(i, contents[i]);
                }
            }
            
            // Lakukan kompresi
            if (specialBackpack != null) {
                tempInv = specialBackpack.compressItems(tempInv);
            } else {
                // Gunakan implementasi default untuk backpack non-spesial
                List<List<Object>> recipes = getCompressionRecipes();
                tempInv = compressItems(tempInv, recipes);
            }
            
            // Salin kembali ke array contents
            ItemStack[] tempContents = tempInv.getContents();
            for (int i = 0; i < contents.length; i++) {
                if (i < tempContents.length) {
                    contents[i] = tempContents[i];
                } else {
                    contents[i] = null;
                }
            }
            
            // Reset halaman
            currentPage = 0;
            
            // Refresh GUI
            refreshGUI();
            
            // Beri tahu pemain
            owner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.backpack-items-compressed", "§aItem di backpack telah dikompresi!"));
            
            // Efek suara
            owner.playSound(owner.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.2f);
        } catch (Exception e) {
            plugin.getLogger().severe("Error compressing items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Toggle status auto-collect
     */
    public void toggleAutoCollect() {
        try {
            if (!plugin.getSpecialBackpackManager().isSpecialBackpack(backpackType)) {
                return;
            }
            
            SpecialBackpack specialBackpack = plugin.getSpecialBackpackManager().getSpecialBackpack(backpackType);
            if (!specialBackpack.isAutoCollect()) {
                return;
            }
            
            // Toggle status
            boolean currentStatus = plugin.getDatabaseManager().isAutoCollectEnabled(owner.getUniqueId());
            plugin.getDatabaseManager().setAutoCollectEnabled(owner.getUniqueId(), !currentStatus);
            
            // Refresh GUI
            refreshGUI();
            
            // Notifikasi
            if (!currentStatus) {
                owner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.auto-collect-enabled"));
                owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
            } else {
                owner.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                plugin.getConfig().getString("messages.auto-collect-disabled"));
                owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error toggling auto-collect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Implementasi kompresi item standar
     */
    private Inventory compressItems(Inventory inventory, List<List<Object>> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return inventory;
        }
        
        for (List<Object> recipe : recipes) {
            try {
                // Validasi format resep
                if (recipe.size() < 3) continue;
                
                String sourceMatStr = recipe.get(0).toString();
                int amount = Integer.parseInt(recipe.get(1).toString());
                String resultMatStr = recipe.get(2).toString();
                
                Material sourceMaterial = Material.valueOf(sourceMatStr);
                Material resultMaterial = Material.valueOf(resultMatStr);
                
                // Hitung total item yang dapat dikompres
                int total = 0;
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && item.getType() == sourceMaterial) {
                        total += item.getAmount();
                    }
                }
                
                // Lakukan kompresi jika memungkinkan
                while (total >= amount) {
                    // Hapus bahan dari inventory
                    int remaining = amount;
                    for (int i = 0; i < inventory.getSize(); i++) {
                        ItemStack item = inventory.getItem(i);
                        if (item != null && item.getType() == sourceMaterial) {
                            if (item.getAmount() <= remaining) {
                                // Hapus seluruh stack
                                remaining -= item.getAmount();
                                inventory.setItem(i, null);
                            } else {
                                // Kurangi sebagian
                                item.setAmount(item.getAmount() - remaining);
                                remaining = 0;
                            }
                            
                            if (remaining <= 0) break;
                        }
                    }
                    
                    // Tambahkan hasil kompresi
                    ItemStack result = new ItemStack(resultMaterial);
                    HashMap<Integer, ItemStack> leftover = inventory.addItem(result);
                    
                    // Jika hasil tidak muat, batalkan kompresi dan kembalikan bahan
                    if (!leftover.isEmpty()) {
                        // Tambahkan kembali item yang dihapus
                        inventory.addItem(new ItemStack(sourceMaterial, amount));
                        break;
                    }
                    
                    total -= amount;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error saat melakukan kompresi: " + e.getMessage());
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
        
        List<?> configRecipes = plugin.getConfig().getList("compression.recipes");
        if (configRecipes != null) {
            for (Object obj : configRecipes) {
                if (obj instanceof List) {
                    recipes.add((List<Object>) obj);
                }
            }
        }
        
        return recipes;
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
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        
        if (name != null) {
            meta.setDisplayName(name);
        }
        
        if (lore != null) {
            meta.setLore(lore);
        }
        
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        
        button.setItemMeta(meta);
        return button;
    }
    
    /**
     * Menambahkan efek glow pada item
     */
    private ItemStack addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
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
     * Dapatkan tipe backpack
     */
    public String getBackpackType() {
        return backpackType;
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
        SORT,
        PREVIOUS_PAGE,
        NEXT_PAGE,
        INFO,
        FILTER,
        COMPRESS,
        TOGGLE_AUTO_COLLECT
    }
}