package worldexpansion.worldexpansion;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import worldexpansion.worldexpansion.util.WorldBorderUtil;
import java.io.File;

public final class WorldExpansion extends JavaPlugin {

    private WorldBorderUtil worldBorderUtil;

    @Override
    public void onEnable() {
        try {
            Bukkit.getLogger().info("[WorldExpansion] Activating plugin...");

            // Initialize configuration and utilities
            createDefaultConfig();
            initializeWorldBorderUtil();

            Bukkit.getLogger().info("[WorldExpansion] Plugin activated successfully!");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[WorldExpansion] Failed to enable plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("[WorldExpansion] Deactivating plugin...");

        if (worldBorderUtil != null) {
            Bukkit.getLogger().info("[WorldExpansion] WorldBorderUtil cleaned up.");
        }

        Bukkit.getLogger().info("[WorldExpansion] Plugin deactivated!");
    }

    private void initializeWorldBorderUtil() {
        worldBorderUtil = new WorldBorderUtil(this);

        Bukkit.getLogger().info("[WorldExpansion] WorldBorderUtil initialized successfully!");
    }

    private void createDefaultConfig() {
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
    }
}
