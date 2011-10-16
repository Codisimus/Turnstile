
package com.codisimus.plugins.turnstile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionManager;

/**
 *
 * @author Codisimus
 */
public class TurnstileMain extends JavaPlugin {
    protected static int cost = 0;
    protected static boolean op;
    protected static PermissionManager permissions;
    protected static PluginManager pm;
    protected static Server server;
    protected static boolean noFraud;
    protected static int timeOut;
    protected static boolean useOpenFreeNode;
    protected static boolean useMakeFreeNode;
    protected static String correct;
    protected static String wrong;
    protected static String notEnoughMoney;
    protected static String displayCost;
    protected static String open;
    protected static String balanceCleared;
    protected static String privateTurnstile;
    private static Properties p;

    @Override
    public void onDisable () {
    }

    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        checkFiles();
        loadConfig();
        SaveSystem.loadFromFile();
        registerEvents();
        System.out.println("Turnstile "+this.getDescription().getVersion()+" is enabled!");
    }

    /**
     * Makes sure all needed files exist
     *
     */
    private void checkFiles() {
        File file = new File("plugins/Turnstile/config.properties");
        if (!file.exists())
            moveFile("config.properties");
    }
    
    /**
     * Moves file from ButtonWarp.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    private void moveFile(String fileName) {
        try {
            JarFile jar = new JarFile("plugins/Turnstile.jar");
            ZipEntry entry = jar.getEntry(fileName);
            String destination = "plugins/Turnstile/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            while (true) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0)
                    break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads settings from the config.properties file
     * 
     */
    private void loadConfig() {
        p = new Properties();
        try {
            p.load(new FileInputStream("plugins/Turnstile/config.properties"));
        }
        catch (Exception e) {
        }
        cost = Integer.parseInt(loadValue("CostToMakeTurnstile"));
        Register.economy = loadValue("Economy");
        PluginListener.useOP = Boolean.parseBoolean(loadValue("UseOP"));
        op = Boolean.parseBoolean(loadValue("UseOP"));
        Turnstile.oneWay = Boolean.parseBoolean(loadValue("OneWayTurnstiles"));
        noFraud = Boolean.parseBoolean(loadValue("NoFraud"));
        timeOut = Integer.parseInt(loadValue("AutoCloseTimer"));
        useOpenFreeNode = Boolean.parseBoolean(loadValue("use'openfree'node"));
        useMakeFreeNode = Boolean.parseBoolean(loadValue("use'makefree'node"));
        TurnstilePlayerListener.permission = loadValue("PermissionMessage").replaceAll("&", "§");
        TurnstilePlayerListener.locked = loadValue("LockedMessage").replaceAll("&", "§");
        TurnstilePlayerListener.free = loadValue("FreeMessage").replaceAll("&", "§");
        TurnstilePlayerListener.oneWay = loadValue("OneWayMessage").replaceAll("&", "§");
        correct = loadValue("CorrectItemMessage").replaceAll("&", "§");
        wrong = loadValue("WrongItemMessage").replaceAll("&", "§");
        notEnoughMoney = loadValue("NotEnoughMoneyMessage").replaceAll("&", "§");
        displayCost = loadValue("DisplayCostMessage").replaceAll("&", "§");
        open = loadValue("OpenMessage").replaceAll("&", "§");
        balanceCleared = loadValue("BalanceClearedMessage").replaceAll("&", "§");
        privateTurnstile = loadValue("PrivateMessage").replaceAll("&", "§");
    }
    
    /**
     * Loads the given key and prints error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private String loadValue(String key) {
        if (!p.containsKey(key)) {
            System.err.println("[Turnstile] Missing value for "+key+" in config file");
            System.err.println("[Turnstile] Please regenerate config file");
        }
        return p.getProperty(key);
    }
    
    /**
     * Registers events for the Turnstile Plugin
     *
     */
    private void registerEvents() {
        TurnstilePlayerListener playerListener = new TurnstilePlayerListener();
        TurnstileBlockListener blockListener = new TurnstileBlockListener();
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, new PluginListener(), Priority.Monitor, this);
        pm.registerEvent(Type.WORLD_LOAD, new TurnstileWorldListener(), Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.REDSTONE_CHANGE, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
    }
    
    /**
     * Returns boolean value of whether the given player has the specific permission
     * 
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        if (permissions != null)
            return permissions.has(player, "turnstile."+type);
        else if (type.equals("open"))
            return true;
        return player.isOp();
    }
    
    /**
     * Checks if the given Material is a Button, Chest, or Pressure Plate
     * 
     * @param target The Material to be checked
     * @return true if the Material is a Button, Chest, or Pressure Plate
     */
    protected static boolean isSwitch(Material block) {
        if (block.equals(Material.STONE_BUTTON))
            return true;
        else if (block.equals(Material.CHEST))
            return true;
        else if (block.equals(Material.STONE_PLATE))
            return true;
        else if (block.equals(Material.WOOD_PLATE))
            return true;
        return false;
    }
    
    /**
     * Checks if the given Material is a Door
     * 
     * @param target The Material to be checked
     * @return true if the Material is a Door
     */
    protected static boolean isDoor(Material door) {
        if (door.equals(Material.WOOD_DOOR))
            return true;
        else if (door.equals(Material.WOODEN_DOOR))
            return true;
        else if (door.equals(Material.IRON_DOOR))
            return true;
        else if (door.equals(Material.IRON_DOOR_BLOCK))
            return true;
        return false;
    }
    
    /**
     * Returns whether the given Block is above or below the other given Block
     * 
     * @param blockOne The first Block to be compared
     * @param blockTwo The second Block to be compared
     * @return true if the given Block is above or below the other given Block
     */
    protected static boolean areNeighbors(Block blockOne, Block blockTwo) {
        int a = blockOne.getX();
        int b = blockOne.getY();
        int c = blockOne.getZ();
        int x = blockTwo.getX();
        int y = blockTwo.getY();
        int z = blockTwo.getZ();
        if (blockOne.getWorld() == blockTwo.getWorld())
            if (a == x && c == z)
                if (b == y+1 || b == y-1)
                    return true;
        return false;
    }
}
