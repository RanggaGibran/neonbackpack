package id.neonbackpack.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import id.neonbackpack.NeonBackPack;

public class EconomyManager {
    private final NeonBackPack plugin;
    private Economy economy;
    private boolean enabled;

    public EconomyManager(NeonBackPack plugin) {
        this.plugin = plugin;
        this.enabled = setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault tidak ditemukan, fitur ekonomi dinonaktifkan.");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Plugin ekonomi yang kompatibel dengan Vault tidak ditemukan, fitur ekonomi dinonaktifkan.");
            return false;
        }
        
        economy = rsp.getProvider();
        plugin.getLogger().info("Vault terintegrasi dengan sukses: " + economy.getName());
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEnough(OfflinePlayer player, double amount) {
        return enabled && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String formatCurrency(double amount) {
        if (!enabled) return String.valueOf(amount);
        
        return economy.format(amount);
    }
    
    // Metode untuk mendapatkan biaya backpack berdasarkan tipe
    public double getBackpackCost(String type) {
        return plugin.getConfig().getDouble("economy.prices." + type.toLowerCase(), 0);
    }
    
    // Metode untuk mendapatkan biaya upgrade backpack
    public double getUpgradeCost(String fromType, String toType) {
        String path = "economy.upgrade_costs." + fromType.toLowerCase() + "_to_" + toType.toLowerCase();
        return plugin.getConfig().getDouble(path, 0);
    }
    
    // Metode untuk membeli backpack
    public boolean buyBackpack(Player player, String type) {
        if (!enabled) return true;
        
        double cost = getBackpackCost(type);
        if (cost <= 0) return true; // Gratis jika tidak dikonfigurasi
        
        if (!hasEnough(player, cost)) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.not-enough-money", "§cAnda tidak memiliki cukup uang! Dibutuhkan %cost%.")
                                   .replace("%cost%", formatCurrency(cost)));
            return false;
        }
        
        if (withdraw(player, cost)) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.backpack-purchased", "§aAnda membeli %type% Backpack seharga %cost%!")
                                   .replace("%type%", type)
                                   .replace("%cost%", formatCurrency(cost)));
            return true;
        }
        
        return false;
    }
    
    // Metode untuk upgrade backpack
    public boolean upgradeBackpack(Player player, String fromType, String toType) {
        if (!enabled) return true;
        
        double cost = getUpgradeCost(fromType, toType);
        if (cost <= 0) return true; // Gratis jika tidak dikonfigurasi
        
        if (!hasEnough(player, cost)) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.not-enough-money-upgrade", "§cAnda tidak memiliki cukup uang untuk upgrade! Dibutuhkan %cost%.")
                                   .replace("%cost%", formatCurrency(cost)));
            return false;
        }
        
        if (withdraw(player, cost)) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                               plugin.getConfig().getString("messages.backpack-upgraded-cost", "§aBackpack Anda diupgrade dari %from% ke %to% dengan biaya %cost%!")
                                   .replace("%from%", fromType)
                                   .replace("%to%", toType)
                                   .replace("%cost%", formatCurrency(cost)));
            return true;
        }
        
        return false;
    }
}