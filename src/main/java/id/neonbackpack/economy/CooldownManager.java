package id.neonbackpack.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import id.neonbackpack.NeonBackPack;

public class CooldownManager {
    
    private final NeonBackPack plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;
    
    public CooldownManager(NeonBackPack plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
    }
    
    public boolean isCooldownActive(Player player, String action) {
        if (!plugin.getConfig().getBoolean("economy.enabled", false)) {
            return false;
        }
        
        int cooldownTime = plugin.getConfig().getInt("economy.cooldowns." + action, 0);
        if (cooldownTime <= 0) {
            return false;
        }
        
        UUID playerUUID = player.getUniqueId();
        if (!cooldowns.containsKey(playerUUID)) {
            return false;
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (!playerCooldowns.containsKey(action)) {
            return false;
        }
        
        long lastUsage = playerCooldowns.get(action);
        long currentTime = System.currentTimeMillis() / 1000;
        
        return (currentTime - lastUsage) < cooldownTime;
    }
    
    public int getRemainingCooldown(Player player, String action) {
        if (!plugin.getConfig().getBoolean("economy.enabled", false)) {
            return 0;
        }
        
        int cooldownTime = plugin.getConfig().getInt("economy.cooldowns." + action, 0);
        if (cooldownTime <= 0) {
            return 0;
        }
        
        UUID playerUUID = player.getUniqueId();
        if (!cooldowns.containsKey(playerUUID)) {
            return 0;
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (!playerCooldowns.containsKey(action)) {
            return 0;
        }
        
        long lastUsage = playerCooldowns.get(action);
        long currentTime = System.currentTimeMillis() / 1000;
        long timeElapsed = currentTime - lastUsage;
        
        int remaining = (int) (cooldownTime - timeElapsed);
        return Math.max(0, remaining);
    }
    
    public void setCooldown(Player player, String action) {
        if (!plugin.getConfig().getBoolean("economy.enabled", false)) {
            return;
        }
        
        int cooldownTime = plugin.getConfig().getInt("economy.cooldowns." + action, 0);
        if (cooldownTime <= 0) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        if (!cooldowns.containsKey(playerUUID)) {
            cooldowns.put(playerUUID, new HashMap<>());
        }
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        playerCooldowns.put(action, System.currentTimeMillis() / 1000);
    }
    
    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
    
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
    
    public boolean handleAction(Player player, String action) {
        if (isCooldownActive(player, action)) {
            int remaining = getRemainingCooldown(player, action);
            player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                              plugin.getConfig().getString("messages.cooldown-active", "§cFitur ini masih dalam cooldown. Tersisa: %time% detik.")
                                  .replace("%time%", String.valueOf(remaining)));
            return false;
        }
        
        // Cek dan ambil biaya jika diperlukan
        if (plugin.getEconomyManager().isEnabled()) {
            double cost = plugin.getConfig().getDouble("economy.use_costs." + action, 0);
            if (cost > 0) {
                if (!plugin.getEconomyManager().hasEnough(player, cost)) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                      plugin.getConfig().getString("messages.not-enough-money", "§cAnda tidak memiliki cukup uang! Dibutuhkan %cost%.")
                                          .replace("%cost%", plugin.getEconomyManager().formatCurrency(cost)));
                    return false;
                }
                
                plugin.getEconomyManager().withdraw(player, cost);
                player.sendMessage(plugin.getConfig().getString("messages.prefix") + 
                                  "§aAnda membayar " + plugin.getEconomyManager().formatCurrency(cost) + " untuk menggunakan fitur ini.");
            }
        }
        
        setCooldown(player, action);
        return true;
    }
}