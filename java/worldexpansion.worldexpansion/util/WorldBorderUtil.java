package worldexpansion.worldexpansion.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import worldexpansion.worldexpansion.WorldExpansion;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;


public class WorldBorderUtil implements Listener {
    private final WorldExpansion plugin;
    private final Map<String, WorldBorder> worldBorders = new HashMap<>();
    private final Map<String, Double> worldBorderSizes = new HashMap<>();
    private final Map<String, Double> worldBorderMinSizes = new HashMap<>();

    private int spawnX = 0;
    private int spawnZ = 0;
    private List<String> worlds;

    private int passiveMobExpandChance;
    private int passiveMobExpandDistanceLowerBound;
    private int passiveMobExpandDistanceUpperBound;

    private int hostileMobExpandChance;
    private int hostileMobExpandDistanceLowerBound;
    private int hostileMobExpandDistanceUpperBound;

    private int playerExpandChance;
    private int playerExpandDistanceLowerBound;
    private int playerExpandDistanceUpperBound;

    private int bossMobExpandChance;
    private int bossMobExpandDistanceLowerBound;
    private int bossMobExpandDistanceUpperBound;

    private boolean evenWorldBorder;
    private double bonusExpansionFactor;
    private double worldBorderMultiplier;

    private double worldBorderSpeedFactor;

    private double warningDistanceFactor;
    private int maxWarningDistance;
    private double damageAmount;
    private double damageBuffer;

    public WorldBorderUtil(WorldExpansion plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        loadConfig();
        initializeWorldBorder();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        // Add default values for mob-related settings
        config.addDefault("passiveMobExpandChance", 60);
        config.addDefault("passiveMobExpandDistanceLowerBound", 2);
        config.addDefault("passiveMobExpandDistanceUpperBound", 3);
        config.addDefault("hostileMobExpandChance", 75);
        config.addDefault("hostileMobExpandDistanceLowerBound", 4);
        config.addDefault("hostileMobExpandDistanceUpperBound", 5);
        config.addDefault("playerExpandChance", 65);
        config.addDefault("playerExpandDistanceLowerBound", 1);
        config.addDefault("playerExpandDistanceUpperBound", 3);
        config.addDefault("bossMobExpandChance", 100);
        config.addDefault("bossMobExpandDistanceLowerBound", 30);
        config.addDefault("bossMobExpandDistanceUpperBound", 38);

        // Add default values for general settings
        config.addDefault("evenWorldBorder", false);
        config.addDefault("bonusExpansionFactor", 0.2);
        config.addDefault("worldBorderMultiplier", 1.0);
        config.addDefault("worldBorderSpeedFactor", 7.0);
        config.addDefault("warningDistanceFactor", 7.0);
        config.addDefault("maxWarningDistance", 5);
        config.addDefault("damageAmount", 0.4);
        config.addDefault("damageBuffer", 1.0);
        config.addDefault("spawnX", 0);
        config.addDefault("spawnZ", 0);

        config.addDefault("worldStartSize", 10.0);
        config.addDefault("worldMinSize", 8.0);

        // Get all available worlds from Bukkit
        List<String> availableWorlds = Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList());

        // Set worlds list in config
        config.set("worlds", availableWorlds);

        // Ensure worldBorders section exists
        if (!config.contains("worldBorders")) {
            config.createSection("worldBorders");
        }

        double worldStartSize = config.getDouble("worldStartSize");
        double worldMinSize = config.getDouble("worldMinSize");

        // Initialize border settings for each world
        ConfigurationSection worldBordersSection = config.getConfigurationSection("worldBorders");
        for (String worldName : availableWorlds) {
            if (!worldBordersSection.contains(worldName)) {
                worldBordersSection.createSection(worldName);
                worldBordersSection.set(worldName + ".size", worldStartSize);
                worldBordersSection.set(worldName + ".minSize", worldMinSize);
            }
        }

        // Copy defaults and save
        config.options().copyDefaults(true);
        plugin.saveConfig();

        // Load expansion settings
        passiveMobExpandChance = config.getInt("passiveMobExpandChance");
        passiveMobExpandDistanceLowerBound = config.getInt("passiveMobExpandDistanceLowerBound");
        passiveMobExpandDistanceUpperBound = config.getInt("passiveMobExpandDistanceUpperBound");
        hostileMobExpandChance = config.getInt("hostileMobExpandChance");
        hostileMobExpandDistanceLowerBound = config.getInt("hostileMobExpandDistanceLowerBound");
        hostileMobExpandDistanceUpperBound = config.getInt("hostileMobExpandDistanceUpperBound");
        playerExpandChance = config.getInt("playerExpandChance");
        playerExpandDistanceLowerBound = config.getInt("playerExpandDistanceLowerBound");
        playerExpandDistanceUpperBound = config.getInt("playerExpandDistanceUpperBound");
        bossMobExpandChance = config.getInt("bossMobExpandChance");
        bossMobExpandDistanceLowerBound = config.getInt("bossMobExpandDistanceLowerBound");
        bossMobExpandDistanceUpperBound = config.getInt("bossMobExpandDistanceUpperBound");

        // Load general settings
        evenWorldBorder = config.getBoolean("evenWorldBorder");
        bonusExpansionFactor = config.getDouble("bonusExpansionFactor");
        worldBorderMultiplier = config.getDouble("worldBorderMultiplier");
        worldBorderSpeedFactor = config.getDouble("worldBorderSpeedFactor");
        warningDistanceFactor = config.getDouble("warningDistanceFactor");
        maxWarningDistance = config.getInt("maxWarningDistance");
        damageAmount = config.getDouble("damageAmount");
        damageBuffer = config.getDouble("damageBuffer");
        spawnX = config.getInt("spawnX");
        spawnZ = config.getInt("spawnZ");
        worlds = availableWorlds;

        // Load world-specific border sizes
        for (String worldName : worlds) {
            double size = config.getDouble("worldBorders." + worldName + ".size", 10.0);
            double minSize = config.getDouble("worldBorders." + worldName + ".minSize", 8.0);
            worldBorderSizes.put(worldName, size);
            worldBorderMinSizes.put(worldName, minSize);
        }
    }

    private void initializeWorldBorder() {
        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                Bukkit.getLogger().log(Level.SEVERE, "World '{0}' not found. Please check the configuration.", worldName);
                continue;
            }

            WorldBorder wb = world.getWorldBorder();
            if (wb == null) {
                Bukkit.getLogger().log(Level.SEVERE, "World border not found for world '{0}'.", worldName);
                continue;
            }

            wb.setCenter(spawnX, spawnZ);
            wb.setSize(worldBorderSizes.get(worldName));
            wb.setDamageAmount(damageAmount);
            wb.setDamageBuffer(damageBuffer);
            updateWarningDistance(wb);

            worldBorders.put(worldName, wb);
        }
    }

    private void updateWarningDistance(WorldBorder wb) {
        int warningDistance = Math.min((int) (wb.getSize() / warningDistanceFactor), maxWarningDistance);
        wb.setWarningDistance(warningDistance);
    }

    private void expandWorldBorder(World world, double blocks) {
        if (world == null) return;

        String worldName = world.getName();
        WorldBorder wb = worldBorders.get(worldName);
        if (wb == null) return;

        double minSize = worldBorderMinSizes.get(worldName);
        double newSize = Math.max(wb.getSize() + blocks * worldBorderMultiplier, minSize);
        if (evenWorldBorder) {
            newSize = Math.round(newSize / 2) * 2;
        }
        wb.setSize(newSize, (long) (blocks / worldBorderSpeedFactor));
        updateWarningDistance(wb);

        // Update the config with the new size
        plugin.getConfig().set("worldBorders." + worldName + ".size", newSize);
        worldBorderSizes.put(worldName, newSize);
        plugin.saveConfig();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location spawnLocation = event.getRespawnLocation();
        World world = event.getRespawnLocation().getWorld();

        if (spawnLocation == null && world != null) {
            int y = world.getHighestBlockYAt(spawnX, spawnZ);
            spawnLocation = new Location(world, spawnX, y, spawnZ);
            event.setRespawnLocation(spawnLocation);
        }
    }

    private static int randomInt(int lowerBound, int higherBound) {
        Random random = new Random();
        return random.nextInt(higherBound - lowerBound + 1) + lowerBound;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        World world = entity.getWorld();
        int r = randomInt(0, 100);
        double health = Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
        double bonusExpansion = health * bonusExpansionFactor;

        if (entity instanceof Player) {
            if (r < playerExpandChance) {
                expandWorldBorder(world, randomInt(playerExpandDistanceLowerBound, playerExpandDistanceUpperBound) + bonusExpansion);
            }
        } else if (entity instanceof Monster) {
            if (entity instanceof Wither || entity instanceof Warden || entity instanceof EnderDragon) {
                if (r < bossMobExpandChance) {
                    expandWorldBorder(world, randomInt(bossMobExpandDistanceLowerBound, bossMobExpandDistanceUpperBound) + bonusExpansion);
                }
            } else {
                if (r < hostileMobExpandChance) {
                    expandWorldBorder(world, randomInt(hostileMobExpandDistanceLowerBound, hostileMobExpandDistanceUpperBound) + bonusExpansion);
                }
            }
        } else {
            if (r < passiveMobExpandChance) {
                expandWorldBorder(world, randomInt(passiveMobExpandDistanceLowerBound, passiveMobExpandDistanceUpperBound) + bonusExpansion);
            }
        }
    }
}