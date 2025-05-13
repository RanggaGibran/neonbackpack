package id.neonbackpack;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import id.neonbackpack.commands.BackpackCommand;
import id.neonbackpack.economy.CooldownManager;
import id.neonbackpack.economy.EconomyManager;
import id.neonbackpack.gui.GUIManager;
import id.neonbackpack.listeners.AutoCollectListener;
import id.neonbackpack.listeners.BackpackListener;
import id.neonbackpack.listeners.GUIListener;
import id.neonbackpack.listeners.BackpackProtectionListener;
import id.neonbackpack.specialbackpack.SpecialBackpackManager;

/*
 * NusaBackpack java plugin
 */
public class NeonBackPack extends JavaPlugin
{
  private static final Logger LOGGER = Logger.getLogger("nusabackpack");
  private DatabaseManager databaseManager;
  private BackpackManager backpackManager;
  private SpecialBackpackManager specialBackpackManager;
  private GUIManager guiManager;
  private EconomyManager economyManager;
  private CooldownManager cooldownManager;

  @Override
  public void onEnable()
  {
    // Buat folder config jika belum ada
    if (!getDataFolder().exists()) {
      getDataFolder().mkdir();
    }
    
    // Simpan config default
    saveDefaultConfig();
    
    // Inisialisasi database manager
    this.databaseManager = new DatabaseManager(this);
    this.databaseManager.initialize();
    
    // Inisialisasi economy manager
    this.economyManager = new EconomyManager(this);
    
    // Inisialisasi cooldown manager
    this.cooldownManager = new CooldownManager(this);
    
    // Inisialisasi special backpack manager
    this.specialBackpackManager = new SpecialBackpackManager(this);
    
    // Inisialisasi backpack manager
    this.backpackManager = new BackpackManager(this);
    
    // Inisialisasi GUI manager
    this.guiManager = new GUIManager(this);
    
    // Register commands
    BackpackCommand backpackCommand = new BackpackCommand(this, backpackManager);
    getCommand("backpack").setExecutor(backpackCommand);
    getCommand("backpack").setTabCompleter(backpackCommand);
    getCommand("bp").setExecutor(backpackCommand);
    getCommand("bp").setTabCompleter(backpackCommand);
    
    // Register event listeners
    getServer().getPluginManager().registerEvents(new BackpackListener(this), this);
    getServer().getPluginManager().registerEvents(new AutoCollectListener(this), this);
    getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    
    // Tambahkan listener untuk anti-theft protection
    getServer().getPluginManager().registerEvents(new BackpackProtectionListener(this), this);
    
    LOGGER.info("ɴᴜsᴀʙᴀᴄᴋᴘᴀᴄᴋ ᴇɴᴀʙʟᴇᴅ");
  }

  @Override
  public void onDisable()
  {
    // Tutup semua GUI backpack
    if (this.guiManager != null) {
      this.guiManager.closeAllGUIs();
    }
    
    // Tutup koneksi database
    if (this.databaseManager != null) {
      this.databaseManager.closeConnection();
    }
    
    LOGGER.info("ɴᴜsᴀʙᴀᴄᴋᴘᴀᴄᴋ ᴅɪsᴀʙʟᴇᴅ");
  }
  
  @Override
  public void reloadConfig() {
    // Simpan data penting sebelum reload
    Map<UUID, String> playerBackpackTypes = new HashMap<>();
    if (databaseManager != null) {
      // Kode backup data bisa ditambahkan di sini jika diperlukan
    }
    
    // Reload config dari disk
    super.reloadConfig();
    
    // Reload komponen yang menggunakan nilai dari config
    if (this.specialBackpackManager != null) {
      this.specialBackpackManager.reloadSpecialBackpacks();
    }
    
    if (this.economyManager != null) {
      // Tidak perlu membuat ulang, karena EconomyManager mengambil nilai langsung dari config
      // Tapi kita bisa menambahkan metode refresh jika diperlukan
      // this.economyManager.refreshConfig();
    }
    
    if (this.cooldownManager != null) {
      // CooldownManager mengambil nilai langsung dari config saat diperlukan
      this.cooldownManager.clearAllCooldowns(); // Reset semua cooldown saat config di-reload
    }
    
    if (this.backpackManager != null) {
      // BackpackManager sebagian besar mengambil nilai langsung dari config
      // Tidak perlu membuat ulang
    }
    
    if (this.databaseManager != null) {
      // Memperbarui tipe backpack di database jika ada perubahan
      try {
        this.databaseManager.updateBackpackTypesFromConfig();
      } catch (Exception e) {
        getLogger().warning("ɢᴀɢᴀʟ ᴍᴇᴍᴘᴇʀʙᴀʀᴜɪ ᴛɪᴘᴇ ʙᴀᴄᴋᴘᴀᴄᴋ ᴅᴀʀɪ ᴄᴏɴғɪɢ: " + e.getMessage());
      }
    }
    
    // Log pesan sukses
    getLogger().info("ᴄᴏɴғɪɢ ʙᴇʀʜᴀsɪʟ ᴅɪ-ʀᴇʟᴏᴀᴅ!");
  }
  
  public DatabaseManager getDatabaseManager() {
    return this.databaseManager;
  }
  
  public BackpackManager getBackpackManager() {
    return this.backpackManager;
  }
  
  public SpecialBackpackManager getSpecialBackpackManager() {
    return this.specialBackpackManager;
  }
  
  public GUIManager getGUIManager() {
    return this.guiManager;
  }
  
  public EconomyManager getEconomyManager() {
    return this.economyManager;
  }
  
  public CooldownManager getCooldownManager() {
    return this.cooldownManager;
  }
}
