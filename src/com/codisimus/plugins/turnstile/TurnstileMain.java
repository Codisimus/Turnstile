package com.codisimus.plugins.turnstile;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class TurnstileMain extends JavaPlugin {
    public static final String YAML_EXTENSION = ".yml";
    public static final FilenameFilter YAML_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(YAML_EXTENSION);
        }
    };
    public static ArrayList<TurnstileSign> counterSigns = new ArrayList<>();
    public static ArrayList<TurnstileSign> moneySigns = new ArrayList<>();
    public static ArrayList<TurnstileSign> itemSigns = new ArrayList<>();
    public static final HashMap<TurnstileSign, BukkitTask> statusSigns = new HashMap<>();
    public static Logger logger;
    public static Plugin plugin;
    public static String dataFolder;
    public static CommandHandler handler;
    private static final HashMap<String, Turnstile> turnstiles = new HashMap<>();

    /**
     * Closes all open Turnstiles when this Plugin is disabled
     */
    @Override
    public void onDisable () {
        //Close all open Turnstiles
        logger.info("Closing all open Turnstiles...");
        for (Turnstile turnstile: TurnstileListener.openTurnstiles) {
            if (!turnstile.addedToCooldown.isEmpty()) {
                turnstile.onCooldown.remove(turnstile.addedToCooldown);
                turnstile.addedToCooldown = "";
            }
            turnstile.close();
        }
    }

    /**
     * Calls methods to load this Plugin when it is enabled
     */
    @Override
    public void onEnable () {
        logger = getLogger();
        plugin = this;

        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dataFolder = dir.getPath();

        dir = new File(dataFolder, "Turnstiles");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        ConfigurationSerialization.registerClass(Turnstile.class, "Turnstile");
        ConfigurationSerialization.registerClass(TurnstileButton.class, "TurnstileButton");
        ConfigurationSerialization.registerClass(TurnstileSign.class, "TurnstileSign");

        loadData();

        /* Register Events */
        Bukkit.getPluginManager().registerEvents(new TurnstileListener(), this);
        if (TurnstileConfig.enderpearlProtection) {
            Bukkit.getPluginManager().registerEvents(new EnderPearlProtection(), this);
        }
        if (TurnstileConfig.citizens && Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            //Watch for interacting with NPCs if linked to Citizens
            Bukkit.getPluginManager().registerEvents(new NPCListener(), this);
        }

        /* Register the command found in the plugin.yml */
        String command = (String) getDescription().getCommands().keySet().toArray()[0];
        handler = new CommandHandler(this, command);
    }

    /**
     * Reloads the config from the config.yml file.
     * Loads values from the newly loaded config.
     * This method is automatically called when the plugin is enabled
     */
    @Override
    public void reloadConfig() {
        //Save the config file if it does not already exist
        saveDefaultConfig();

        //Reload the config as this method would normally do if not overriden
        super.reloadConfig();

        //Load values from the config now that it has been reloaded
        TurnstileConfig.load();

        Econ.setupEconomy();
    }

    public static void loadData() {
        loadTurnstiles();
        loadSigns();
    }

    /**
     * Loads each Turnstile
     */
    public static void loadTurnstiles() {
        turnstiles.clear();

        //Load each YAML file in the Turnstiles folder
        File[] files = new File(dataFolder, "Turnstiles").listFiles(YAML_FILTER);
        for (File file : files) {
            try {
                String name = file.getName();
                name = name.substring(0, name.length() - 4);
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                //Ensure the Turnstile name matches the file name
                Turnstile turnstile = (Turnstile) config.get(config.contains(name)
                                                             ? name
                                                             : config.getKeys(false).iterator().next());
                if (!turnstile.name.equals(name)) {
                    turnstile.name = name;
                    turnstile.save();
                }

                turnstile.loadCooldownTimes();
                turnstiles.put(name, turnstile);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to load " + file.getName(), ex);
            }
        }
    }

    /**
     * Reads save file to load Sign data
     */
    public static void loadSigns() {
        try {
            File file = new File(dataFolder, "signs.yml");
            if (!file.exists()) {
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            counterSigns = (ArrayList<TurnstileSign>) config.get("CounterSigns");
            moneySigns = (ArrayList<TurnstileSign>) config.get("MoneySigns");
            itemSigns = (ArrayList<TurnstileSign>) config.get("ItemSigns");
            for (TurnstileSign sign : (ArrayList<TurnstileSign>) config.get("StatusSigns")) {
                statusSigns.put(sign, sign.tickListenerTask());
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load signs", ex);
        }
    }

    /**
     * Invokes save() method for each Turnstile
     * Also invokes the saveSigns() method
     */
    public static void saveAll() {
        for (Turnstile turnstile: turnstiles.values()) {
            turnstile.saveAll();
        }
        saveSigns();
    }

    /**
     * Writes Sign data to save file
     * Old files are overwritten
     */
    public static void saveSigns() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("CounterSigns", counterSigns);
            config.set("MoneySigns", moneySigns);
            config.set("ItemSigns", itemSigns);
            config.set("StatusSigns", statusSigns.keySet());
            config.save(new File(TurnstileMain.dataFolder, "signs.yml"));
        } catch (Exception saveFailed) {
            logger.log(Level.SEVERE, "Failed to save signs", saveFailed);
        }
    }

    /**
     * Returns the Collection of all Turnstiles
     *
     * @return The Collection of all Turnstiles
     */
    public static Collection<Turnstile> getTurnstiles() {
        return turnstiles.values();
    }

    /**
     * Adds the given Turnstile to the collection of Turnstiles
     *
     * @param turnstile The given Turnstile
     */
    public static void addTurnstile(Turnstile turnstile) {
        turnstiles.put(turnstile.name, turnstile);
        turnstile.saveAll();
    }

    /**
     * Removes the given Turnstile from the collection of Turnstiles
     *
     * @param turnstile The given Turnstile
     */
    public static void removeTurnstile(Turnstile turnstile) {
        //Ensure that the Turnstile is closed
        if (turnstile.open) {
            turnstile.close();
        }

        Iterator<TurnstileSign> itr = counterSigns.iterator();
        while (itr.hasNext()) {
            TurnstileSign sign = itr.next();
            if (sign.turnstile.equals(turnstile)) {
                sign.clear();
                itr.remove();
            }
        }

        itr = moneySigns.iterator();
        while (itr.hasNext()) {
            TurnstileSign sign = itr.next();
            if (sign.turnstile.equals(turnstile)) {
                sign.clear();
                itr.remove();
            }
        }

        itr = itemSigns.iterator();
        while (itr.hasNext()) {
            TurnstileSign sign = itr.next();
            if (sign.turnstile.equals(turnstile)) {
                sign.clear();
                itr.remove();
            }
        }

        itr = statusSigns.keySet().iterator();
        while (itr.hasNext()) {
            TurnstileSign sign = itr.next();
            if (sign.turnstile.equals(turnstile)) {
                sign.clear();
                BukkitTask task = statusSigns.get(sign);
                if (task != null) {
                    task.cancel();
                }
                statusSigns.remove(sign);
            }
        }

        turnstiles.remove(turnstile.name);

        File trash = new File(dataFolder + File.separator + "Turnstiles"
                + File.separator + turnstile.name + ".properties");
        trash.delete();
    }

    /**
     * Reloads Turnstile data
     */
    public static void rl() {
        rl(null);
    }

    /**
     * Reloads Turnstile data
     *
     * @param sender The CommandSender reloading the data
     */
    public static void rl(CommandSender sender) {
        plugin.reloadConfig();
        turnstiles.clear();
        loadTurnstiles();

        logger.info("reloaded");
        if (sender != null) {
            sender.sendMessage("Turnstile reloaded");
        }
    }

    /**
     * Return the other half of the Double Chest
     * If this is a not a Chest then null is returned
     * If this is a single Chest then the normal Chest Block is returned
     *
     * @param block The Chest Block
     * @return The other half of the Double Chest
     */
    public static Block getOtherSide(Block block) {
        switch (block.getType()) {
        case TRAPPED_CHEST:
        case CHEST:
            Chest chest = (Chest) block.getState();
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory) {
                Chest leftChest = (Chest) ((DoubleChestInventory) inventory).getLeftSide().getHolder();
                Chest rightChest = (Chest) ((DoubleChestInventory) inventory).getRightSide().getHolder();
                
                block = (chest == leftChest ? rightChest : leftChest).getBlock();
            }
            break;
        default:
            break;
        }
        return block;
    }

    /**
     * Returns the Turnstile with the given name
     *
     * @param name The name of the Turnstile you wish to find
     * @return The Turnstile with the given name or null if not found
     */
    public static Turnstile findTurnstile(String name) {
        return turnstiles.get(name);
    }

    /**
     * Returns the Turnstile with the given Block
     *
     * @param block The block of the Turnstile you wish to find
     * @return The Turnstile with the given block or null if not found
     */
    public static Turnstile findTurnstile(Block block) {
        //Iterate through the data to find the Turnstile that matches the given Block
        for (Turnstile turnstile: turnstiles.values()) {
            if (turnstile.hasBlock(block)) {
                return turnstile;
            }
        }

        //Return null because the Turnstile was not found
        return null;
    }

    /**
     * Closes the Turnstile that the given Block is linked to
     *
     * @param block The given Block
     */
    public static void closeTurnstile(Block block) {
        //Iterate through all open Turnstiles to find the one this Block is linked to
        for (Turnstile turnstile: TurnstileListener.openTurnstiles) {
            if (turnstile.hasBlock(block)) {
                turnstile.close();
                return;
            }
        }
    }
}
