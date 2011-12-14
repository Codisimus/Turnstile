package com.codisimus.plugins.turnstile;

import com.codisimus.plugins.turnstile.listeners.WorldLoadListener;
import com.codisimus.plugins.turnstile.listeners.BlockEventListener;
import com.codisimus.plugins.turnstile.listeners.CommandListener;
import com.codisimus.plugins.turnstile.listeners.PlayerEventListener;
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
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Permissions
 *
 * @author Codisimus
 */
public class TurnstileMain extends JavaPlugin {
    public static int cost = 0;
    public static Permission permission;
    public static PluginManager pm;
    public static Server server;
    public static int defaultTimeOut;
    public static boolean useOpenFreeNode;
    public static boolean useMakeFreeNode;
    public static boolean defaultOneWay;
    public static boolean defaultNoFraud;
    public static String correct;
    public static String wrong;
    public static String notEnoughMoney;
    public static String displayCost;
    public static String open;
    public static String balanceCleared;
    public static String privateTurnstile;
    public static Properties p;

    /**
     * Closes all open Turnstiles when this Plugin is disabled
     *
     */
    @Override
    public void onDisable () {
        //Close all open Turnstiles
        System.out.println("[Turnstile] Closing all open Turnstiles...");
        for (Turnstile turnstile: PlayerEventListener.openTurnstiles)
            turnstile.close();
    }

    /**
     * Calls methods to load this Plugin when it is enabled
     *
     */
    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        
        //Load Config settings
        loadConfig();
        
        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null)
            permission = permissionProvider.getProvider();
        
        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            Econ.economy = economyProvider.getProvider();
        
        //Load Turnstiles Data
        SaveSystem.load(null);
        
        //Register Events
        PlayerEventListener playerListener = new PlayerEventListener();
        BlockEventListener blockListener = new BlockEventListener();
        pm.registerEvent(Type.WORLD_LOAD, new WorldLoadListener(), Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.REDSTONE_CHANGE, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        getCommand("turnstile").setExecutor(new CommandListener());
        
        System.out.println("Turnstile "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Moves file from Turnstile.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    public void moveFile(String fileName) {
        try {
            //Retrieve file from this plugin's .jar
            JarFile jar = new JarFile("plugins/Turnstile.jar");
            ZipEntry entry = jar.getEntry(fileName);
            
            //Create the destination folder if it does not exist
            String destination = "plugins/Turnstile/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            
            //Copy the file
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
        catch (Exception moveFailed) {
            System.err.println("[Turnstile] File Move Failed!");
            moveFailed.printStackTrace();
        }
    }

    /**
     * Loads settings from the config.properties file
     * 
     */
    public void loadConfig() {
        p = new Properties();
        try {
            //Copy the file from the jar if it is missing
            if (!new File("plugins/Turnstile/config.properties").exists())
                moveFile("config.properties");
            
            p.load(new FileInputStream("plugins/Turnstile/config.properties"));
            
            cost = Integer.parseInt(loadValue("CostToMakeTurnstile"));
            
            defaultOneWay = Boolean.parseBoolean(loadValue("OneWayByDefault"));
            defaultNoFraud = Boolean.parseBoolean(loadValue("NoFraudByDefault"));
            defaultTimeOut = Integer.parseInt(loadValue("DefaultAutoCloseTimer"));
            
            useOpenFreeNode = Boolean.parseBoolean(loadValue("use'openfree'node"));
            useMakeFreeNode = Boolean.parseBoolean(loadValue("use'makefree'node"));
            
            PlayerEventListener.permission = format(loadValue("PermissionMessage"));
            PlayerEventListener.locked = format(loadValue("LockedMessage"));
            PlayerEventListener.free = format(loadValue("FreeMessage"));
            PlayerEventListener.oneWay = format(loadValue("OneWayMessage"));
            correct = format(loadValue("CorrectItemMessage"));
            wrong = format(loadValue("WrongItemMessage"));
            notEnoughMoney = format(loadValue("NotEnoughMoneyMessage"));
            displayCost = format(loadValue("DisplayCostMessage"));
            open = format(loadValue("OpenMessage"));
            balanceCleared = format(loadValue("BalanceClearedMessage"));
            privateTurnstile = format(loadValue("PrivateMessage"));
        }
        catch (Exception missingProp) {
            System.err.println("Failed to load ButtonWarp "+this.getDescription().getVersion());
            missingProp.printStackTrace();
        }
    }
    
    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    public String loadValue(String key) {
        //Print an error if the key is not found
        if (!p.containsKey(key)) {
            System.err.println("[Turnstile] Missing value for "+key+" in config file");
            System.err.println("[Turnstile] Please regenerate config file");
        }
        
        return p.getProperty(key);
    }
    
    /**
     * Returns boolean value of whether the given player has the specific permission
     * 
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        return permission.has(player, "turnstile."+type);
    }
    
    /**
     * Adds various Unicode characters and colors to a string
     * 
     * @param string The string being formated
     * @return The formatted String
     */
    public static String format(String string) {
        return string.replaceAll("&", "§").replaceAll("<ae>", "æ").replaceAll("<AE>", "Æ")
                .replaceAll("<o/>", "ø").replaceAll("<O/>", "Ø")
                .replaceAll("<a>", "å").replaceAll("<A>", "Å");
    }
}
