package id.neonbackpack;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DatabaseManager {
    private final NeonBackPack plugin;
    private Connection connection;
    private final String dbPath;

    public DatabaseManager(NeonBackPack plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + "neonbackpack.db";
    }

    /**
     * Inisialisasi koneksi database dan buat tabel jika belum ada
     */
    public void initialize() {
        try {
            // Pastikan driver SQLite tersedia
            Class.forName("org.sqlite.JDBC");
            
            // Buat koneksi
            makeConnection();
            
            // Buat tabel untuk backpack
            try (Statement statement = connection.createStatement()) {
                // Tabel untuk data backpack pemain
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_backpacks (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "backpack_type VARCHAR(20) NOT NULL DEFAULT 'BASIC', " +
                    "backpack_contents TEXT, " +
                    "backpack_size INTEGER DEFAULT 9, " +
                    "auto_collect_enabled BOOLEAN DEFAULT 1, " +  // Default ke aktif
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );
                
                // Cek apakah perlu menambahkan kolom baru untuk auto_collect_enabled
                try {
                    statement.executeQuery("SELECT auto_collect_enabled FROM player_backpacks LIMIT 1");
                } catch (SQLException e) {
                    // Kolom belum ada, tambahkan
                    statement.execute("ALTER TABLE player_backpacks ADD COLUMN auto_collect_enabled BOOLEAN DEFAULT 1");
                    plugin.getLogger().info("Kolom auto_collect_enabled ditambahkan ke tabel player_backpacks");
                }
                
                // Tabel untuk upgrade dan konfigurasi backpack
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS backpack_types (" +
                    "type_name VARCHAR(20) PRIMARY KEY, " +
                    "slots INTEGER NOT NULL, " +
                    "display_name TEXT NOT NULL, " +
                    "permission TEXT, " +
                    "texture_value TEXT, " + 
                    "special_type TEXT, " +
                    "auto_collect BOOLEAN DEFAULT 0, " +
                    "auto_compress BOOLEAN DEFAULT 0" +
                    ")"
                );
                
                // Isi data default untuk backpack types jika belum ada
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM backpack_types");
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDefaultBackpackTypes();
                }
            }
            
            plugin.getLogger().info("Database berhasil diinisialisasi!");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Driver SQLite tidak ditemukan!", e);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menginisialisasi database!", e);
        }
    }

    /**
     * Tambahkan tipe backpack default ke dalam database
     */
    private void insertDefaultBackpackTypes() throws SQLException {
        // Tipe backpack reguler
        String[] types = {"BASIC", "ADVANCED", "PREMIUM", "ULTIMATE", "MINING", "FARMING", "COMBAT"};
        int[] slots = {9, 18, 27, 36, 27, 27, 27}; // Pastikan ukuran sesuai dengan konfigurasi
        String[] names = {
            "§7§lBasic Backpack", 
            "§a§lAdvanced Backpack", 
            "§e§lPremium Backpack", 
            "§d§lUltimate Backpack", 
            "§8§lMining Backpack",
            "§2§lFarming Backpack",
            "§4§lCombat Backpack"
        };
        String[] permissions = {
            "neonbackpack.basic", 
            "neonbackpack.advanced", 
            "neonbackpack.premium", 
            "neonbackpack.ultimate",
            "neonbackpack.mining",
            "neonbackpack.farming", 
            "neonbackpack.combat"
        };
        
        // Texture value untuk player heads dari config
        String[] textures = new String[7];
        try {
            textures[0] = plugin.getConfig().getString("backpacks.types.BASIC.texture", "");
            textures[1] = plugin.getConfig().getString("backpacks.types.ADVANCED.texture", "");
            textures[2] = plugin.getConfig().getString("backpacks.types.PREMIUM.texture", "");
            textures[3] = plugin.getConfig().getString("backpacks.types.ULTIMATE.texture", "");
            textures[4] = plugin.getConfig().getString("backpacks.types.MINING.texture", "");
            textures[5] = plugin.getConfig().getString("backpacks.types.FARMING.texture", "");
            textures[6] = plugin.getConfig().getString("backpacks.types.COMBAT.texture", "");
        } catch (Exception e) {
            plugin.getLogger().warning("Gagal mendapatkan texture value dari config: " + e.getMessage());
            // Gunakan default kosong jika gagal
            textures = new String[]{"", "", "", "", "", "", ""};
        }
        
        String sql = "INSERT OR IGNORE INTO backpack_types (type_name, slots, display_name, permission, texture_value) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < types.length; i++) {
                pstmt.setString(1, types[i]);
                pstmt.setInt(2, slots[i]);
                pstmt.setString(3, names[i]);
                pstmt.setString(4, permissions[i]);
                pstmt.setString(5, textures[i]);
                pstmt.executeUpdate();
            }
        }
        
        plugin.getLogger().info("Data tipe backpack default berhasil dimasukkan!");
    }

    /**
     * Buat koneksi ke database
     */
    private void makeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        
        // Set beberapa properti database
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }

    /**
     * Tutup koneksi database
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Gagal menutup koneksi database", e);
        }
    }
    
    /**
     * Menyimpan isi backpack pemain ke database
     */
    public void savePlayerBackpack(UUID playerUuid, String backpackType, ItemStack[] contents, int size) {
        try {
            makeConnection();
            
            String serializedContents = serializeItems(contents);
            
            // Cek apakah pemain sudah memiliki data di database
            boolean exists = playerExists(playerUuid);
            boolean autoCollectEnabled = true; // Default value
            
            // Jika pemain sudah ada, ambil nilai auto_collect_enabled yang sudah ada
            if (exists) {
                autoCollectEnabled = isAutoCollectEnabled(playerUuid);
            }
            
            String sql = "INSERT OR REPLACE INTO player_backpacks (player_uuid, backpack_type, backpack_contents, backpack_size, auto_collect_enabled, last_updated) " +
                         "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                pstmt.setString(2, backpackType);
                pstmt.setString(3, serializedContents);
                pstmt.setInt(4, size);
                pstmt.setBoolean(5, autoCollectEnabled);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menyimpan backpack untuk pemain: " + playerUuid, e);
        }
    }
    
    /**
     * Cek apakah pemain sudah memiliki data di database
     */
    private boolean playerExists(UUID playerUuid) {
        try {
            makeConnection();
            
            String sql = "SELECT 1 FROM player_backpacks WHERE player_uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal memeriksa keberadaan pemain: " + playerUuid, e);
        }
        
        return false;
    }
    
    /**
     * Memeriksa apakah auto-collect diaktifkan untuk pemain
     */
    public boolean isAutoCollectEnabled(UUID playerUuid) {
        try {
            makeConnection();
            
            String sql = "SELECT auto_collect_enabled FROM player_backpacks WHERE player_uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBoolean("auto_collect_enabled");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal memeriksa status auto-collect untuk pemain: " + playerUuid, e);
        }
        
        // Default ke aktif jika belum ada setting
        return true;
    }
    
    /**
     * Mengatur status auto-collect untuk pemain
     */
    public void setAutoCollectEnabled(UUID playerUuid, boolean enabled) {
        try {
            makeConnection();
            
            String sql = "UPDATE player_backpacks SET auto_collect_enabled = ? WHERE player_uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setBoolean(1, enabled);
                pstmt.setString(2, playerUuid.toString());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal mengatur status auto-collect untuk pemain: " + playerUuid, e);
        }
    }
    
    /**
     * Mengambil isi backpack pemain dari database
     */
    public ItemStack[] getPlayerBackpackContents(UUID playerUuid) {
        try {
            makeConnection();
            
            String sql = "SELECT backpack_contents FROM player_backpacks WHERE player_uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String serializedContents = rs.getString("backpack_contents");
                        if (serializedContents != null && !serializedContents.isEmpty()) {
                            return deserializeItems(serializedContents);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal mengambil backpack untuk pemain: " + playerUuid, e);
        }
        
        return new ItemStack[0];
    }
    
    /**
     * Mengambil tipe backpack pemain dari database
     */
    public String getPlayerBackpackType(UUID playerUuid) {
        try {
            makeConnection();
            
            String sql = "SELECT backpack_type FROM player_backpacks WHERE player_uuid = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("backpack_type");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal mengambil tipe backpack untuk pemain: " + playerUuid, e);
        }
        
        return "BASIC"; // Default ke basic jika tidak ditemukan
    }
    
    /**
     * Mengambil ukuran backpack berdasarkan tipenya
     */
    public int getBackpackSize(String backpackType) {
        try {
            makeConnection();
            
            // Pertama coba ambil dari database
            String sql = "SELECT slots FROM backpack_types WHERE type_name = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, backpackType);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("slots");
                    }
                }
            }
            
            // Jika tidak ada di database, ambil dari konfigurasi
            int configSize = plugin.getConfig().getInt("backpacks.types." + backpackType + ".slots", 9);
            
            // Pastikan ukuran valid untuk penyimpanan (tidak perlu dibatasi 54 untuk database)
            if (configSize > 0) {
                // Gunakan nilai asli dari config, tidak perlu dibulatkan ke kelipatan 9
                int adjustedSize = configSize;
                
                // Simpan ke database untuk penggunaan di masa depan
                try (PreparedStatement pstmt = connection.prepareStatement(
                        "INSERT OR REPLACE INTO backpack_types (type_name, slots, display_name, permission, texture_value) VALUES (?, ?, ?, ?, ?)"
                    )) {
                    pstmt.setString(1, backpackType);
                    pstmt.setInt(2, adjustedSize);
                    pstmt.setString(3, plugin.getConfig().getString("backpacks.types." + backpackType + ".display-name", backpackType));
                    pstmt.setString(4, plugin.getConfig().getString("backpacks.types." + backpackType + ".permission", "neonbackpack." + backpackType.toLowerCase()));
                    pstmt.setString(5, plugin.getConfig().getString("backpacks.types." + backpackType + ".texture", ""));
                    pstmt.executeUpdate();
                    
                    plugin.getLogger().info("Tipe backpack " + backpackType + " dengan ukuran " + adjustedSize + " ditambahkan ke database.");
                    return adjustedSize;
                } catch (SQLException e) {
                    plugin.getLogger().warning("Gagal menyimpan backpack type ke database: " + e.getMessage());
                }
                
                return adjustedSize;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal mengambil ukuran backpack untuk tipe: " + backpackType, e);
        }
        
        return 9; // Default ke 9 slot jika tidak ditemukan
    }
    
    /**
     * Mendapatkan texture value untuk player head berdasarkan tipe backpack
     */
    public String getBackpackTexture(String backpackType) {
        try {
            makeConnection();
            
            String sql = "SELECT texture_value FROM backpack_types WHERE type_name = ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, backpackType);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("texture_value");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal mengambil texture value untuk tipe backpack: " + backpackType, e);
        }
        
        return ""; // Return empty string jika tidak ditemukan
    }
    
    /**
     * Serialize ItemStack array ke dalam format Base64
     */
    private String serializeItems(ItemStack[] contents) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Tulis jumlah items
            dataOutput.writeInt(contents.length);
            
            // Tulis setiap item
            for (ItemStack item : contents) {
                dataOutput.writeObject(item);
            }
            
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal serialize item backpack", e);
            return "";
        }
    }
    
    /**
     * Deserialize string Base64 kembali ke ItemStack array
     */
    private ItemStack[] deserializeItems(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            // Baca jumlah items
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            
            // Baca setiap item
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            
            dataInput.close();
            return items;
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal deserialize item backpack", e);
            return new ItemStack[0];
        }
    }
    
    /**
     * Memperbarui tipe backpack dari config ke database
     */
    public void updateBackpackTypesFromConfig() {
        try {
            makeConnection();
            
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("backpacks.types");
            if (section == null) {
                plugin.getLogger().warning("Bagian backpacks.types tidak ditemukan di config");
                return;
            }
            
            for (String type : section.getKeys(false)) {
                int slots = section.getInt(type + ".slots", 9);
                String displayName = section.getString(type + ".display-name", type);
                String permission = section.getString(type + ".permission", "neonbackpack." + type.toLowerCase());
                String texture = section.getString(type + ".texture", "");
                
                String sql = "INSERT OR REPLACE INTO backpack_types (type_name, slots, display_name, permission, texture_value) "
                        + "VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, type);
                    pstmt.setInt(2, slots);
                    pstmt.setString(3, displayName);
                    pstmt.setString(4, permission);
                    pstmt.setString(5, texture);
                    pstmt.executeUpdate();
                }
            }
            
            plugin.getLogger().info("Tipe backpack berhasil diperbarui dari config");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal memperbarui tipe backpack: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mencari UUID pemain berdasarkan nama (untuk pemain offline)
     * 
     * @param playerName Nama pemain
     * @return UUID pemain atau null jika tidak ditemukan
     */
    public UUID getPlayerUUIDByName(String playerName) {
        // Pertama coba dengan pemain online
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        // Coba cari di database Bukkit
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }
        
        // Tidak ditemukan
        return null;
    }
}