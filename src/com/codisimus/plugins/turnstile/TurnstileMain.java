package com.codisimus.plugins.turnstile;

import com.codisimus.plugins.turnstile.listeners.BlockEventListener;
import com.codisimus.plugins.turnstile.listeners.CommandListener;
import com.codisimus.plugins.turnstile.listeners.PlayerEventListener;
import com.codisimus.plugins.turnstile.listeners.WorldLoadListener;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class TurnstileMain extends JavaPlugin {
    public static int cost = 0;
    static Permission permission;
    public static PluginManager pm;
    static Server server;
    static int defaultTimeOut;
    static boolean useOpenFreeNode;
    public static boolean useMakeFreeNode;
    static boolean defaultOneWay;
    static boolean defaultNoFraud;
    static String correctMsg;
    static String wrongMsg;
    static String notEnoughMoneyMsg;
    public static String displayCostMsg;
    static String openMsg;
    static String balanceClearedMsg;
    static String privateTurnstileMsg;
    private static Properties p;
    public static LinkedList<Turnstile> turnstiles = new LinkedList<Turnstile>();
    public static LinkedList<TurnstileSign> counterSigns = new LinkedList<TurnstileSign>();
    public static LinkedList<TurnstileSign> moneySigns = new LinkedList<TurnstileSign>();
    public static LinkedList<TurnstileSign> itemSigns = new LinkedList<TurnstileSign>();
    public static LinkedList<TurnstileSign> statusSigns = new LinkedList<TurnstileSign>();
    private static boolean save = true;
    public static boolean citizens;
    static Plugin plugin;

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
        plugin = this;
        
        //Load Config settings
        loadSettings();
        
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
        
        //Load Data
        loadTurnstiles(null);
        for (World world: server.getWorlds())
            loadSigns(world);
        
        //Register Events
        PlayerEventListener playerListener = new PlayerEventListener();
        BlockEventListener blockListener = new BlockEventListener();
        pm.registerEvent(Type.WORLD_LOAD, new WorldLoadListener(), Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.REDSTONE_CHANGE, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
        
        //Watch for interacting with NPCs if linked to Citizens
        if (citizens)
            pm.registerEvent(Type.PLAYER_INTERACT_ENTITY, playerListener, Priority.Normal, this);
        
        //Start the tickListener for each status Sign
        for (TurnstileSign sign: statusSigns)
            sign.tickListener();
        
        getCommand("turnstile").setExecutor(new CommandListener());
        
        System.out.println("Turnstile "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Moves file from Turnstile.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    private void moveFile(String fileName) {
        try {
            //Retrieve file from this plugin's .jar
            JarFile jar = new JarFile("plugins/Turnstile.jar");
            ZipEntry entry = jar.getEntry(fileName);
            
            //Create the destination folder if it does not exist
            String destination = "plugins/Turnstile/";
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            
            //Copy the file
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
    public void loadSettings() {
        p = new Properties();
        try {
            //Copy the file from the jar if it is missing
            if (!new File("plugins/Turnstile/config.properties").exists())
                moveFile("config.properties");
            
            FileInputStream fis = new FileInputStream("plugins/Turnstile/config.properties");
            p.load(fis);
            
            Turnstile.debug = Boolean.parseBoolean(loadValue("Debug"));
            
            cost = Integer.parseInt(loadValue("CostToMakeTurnstile"));
            
            citizens = Boolean.parseBoolean(loadValue("UseCitizens"));
            
            defaultOneWay = Boolean.parseBoolean(loadValue("OneWayByDefault"));
            defaultNoFraud = Boolean.parseBoolean(loadValue("NoFraudByDefault"));
            defaultTimeOut = Integer.parseInt(loadValue("DefaultAutoCloseTimer"));
            
            useOpenFreeNode = Boolean.parseBoolean(loadValue("use'openfree'node"));
            useMakeFreeNode = Boolean.parseBoolean(loadValue("use'makefree'node"));
            
            PlayerEventListener.permissionMsg = format(loadValue("PermissionMessage"));
            PlayerEventListener.lockedMsg = format(loadValue("LockedMessage"));
            PlayerEventListener.freeMsg = format(loadValue("FreeMessage"));
            PlayerEventListener.oneWayMsg = format(loadValue("OneWayMessage"));
            correctMsg = format(loadValue("CorrectItemMessage"));
            wrongMsg = format(loadValue("WrongItemMessage"));
            notEnoughMoneyMsg = format(loadValue("NotEnoughMoneyMessage"));
            displayCostMsg = format(loadValue("DisplayCostMessage"));
            openMsg = format(loadValue("OpenMessage"));
            balanceClearedMsg = format(loadValue("BalanceClearedMessage"));
            privateTurnstileMsg = format(loadValue("PrivateMessage"));
            
            fis.close();
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
    private String loadValue(String key) {
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
    private static String format(String string) {
        return string.replaceAll("&", "§").replaceAll("<ae>", "æ").replaceAll("<AE>", "Æ")
                .replaceAll("<o/>", "ø").replaceAll("<O/>", "Ø")
                .replaceAll("<a>", "å").replaceAll("<A>", "Å");
    }

    /**
     * Reads save files to load Turnstile data
     * Saving is turned off if an error occurs
     */
    public static void loadTurnstiles(World world) {
        BufferedReader bReader;
        try {
            File[] files = new File("plugins/Turnstile").listFiles();

            for (File file: files) {
                String name = file.getName();
                if (name.endsWith(".dat") && !name.endsWith("Signs.dat")) {
                    p.load(new FileInputStream(file));

                    name = name.substring(0, name.length() - 4);
                    String owner = p.getProperty("Owner");
                    String[] location = p.getProperty("Location").split("'");
                    String worldName = location[0];
                    int x = Integer.parseInt(location[1]);
                    int y = Integer.parseInt(location[2]);
                    int z = Integer.parseInt(location[3]);
                    Turnstile turnstile = new Turnstile(name, owner, worldName, x, y, z);

                    turnstile.price = Double.parseDouble(p.getProperty("Price"));
                    turnstile.moneyEarned = Double.parseDouble(p.getProperty("MoneyEarned"));

                    turnstile.item = Integer.parseInt(p.getProperty("ItemID"));
                    turnstile.durability = Short.parseShort(p.getProperty("ItemDurability"));
                    turnstile.amount = Integer.parseInt(p.getProperty("ItemAmount"));
                    turnstile.itemsEarned = Integer.parseInt(p.getProperty("ItemsEarned"));

                    turnstile.oneWay = Boolean.parseBoolean(p.getProperty("OneWay"));
                    turnstile.noFraud = Boolean.parseBoolean(p.getProperty("NoFraud"));
                    turnstile.timeOut = Integer.parseInt(p.getProperty("AutoCloseTimer"));

                    String[] time = p.getProperty("FreeTimeRange").split("-");
                    turnstile.freeStart = Integer.parseInt(time[0]);
                    turnstile.freeEnd = Integer.parseInt(time[1]);

                    time = p.getProperty("LockedTimeRange").split("-");
                    turnstile.lockedStart = Integer.parseInt(time[0]);
                    turnstile.lockedEnd = Integer.parseInt(time[1]);

                    String access = p.getProperty("Access");
                    if (!access.equals("public")) {
                        turnstile.access = new LinkedList<String>();
                        if (!access.equals("private"))
                            turnstile.access.addAll(Arrays.asList(access.split(", ")));
                    }

                    String line = p.getProperty("Buttons");
                    if (!line.isEmpty()) {
                        String[] buttons = line.split(", ");
                        for (String button: buttons) {
                            String[] data = button.split("'");
                            x = Integer.parseInt(data[1]);
                            y = Integer.parseInt(data[2]);
                            z = Integer.parseInt(data[3]);
                            int type = Integer.parseInt(data[4]);
                            
                            turnstile.buttons.add(new TurnstileButton(data[0], x, y, z, type));
                        }
                    }

                    turnstiles.add(turnstile);
                }
            }

            if (!turnstiles.isEmpty())
                return;

            File file = new File("plugins/Turnstile/turnstile.save");
            if (!file.exists())
                return;
        
            System.out.println("[Turnstile] Loading outdated save files");

            bReader = new BufferedReader(new FileReader("plugins/Turnstile/turnstile.save"));
            String line = "";
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                if (split.length == 15) {
                    if (split[11].endsWith("~NETHER"))
                        split[11].replace("~NETHER", "");
                    if (world == null) {
                        for (World loadedWorld: TurnstileMain.server.getWorlds())
                            loadTurnstiles(loadedWorld);
                        return;
                    }

                    if (world.getName().equals(split[11])) {
                        int x = Integer.parseInt(split[12]);
                        int y = Integer.parseInt(split[13]);
                        int z = Integer.parseInt(split[14]);
                        Turnstile turnstile = new Turnstile(split[0], split[9], split[11], x, y, z);

                        turnstile.price = Double.parseDouble(split[1]);
                        if (split[3].contains(".")) {
                            int index = split[3].indexOf('.');
                            turnstile.item = Integer.parseInt(split[3].substring(0, index));
                            turnstile.durability = Short.parseShort(split[3].substring(index+1));
                        }
                        else
                            turnstile.item = Integer.parseInt(split[3]);
                        turnstile.amount = Integer.parseInt(split[4]);

                        if (turnstile.item != 0)
                            turnstile.itemsEarned = Integer.parseInt(split[5]);
                        else
                            turnstile.moneyEarned = Double.parseDouble(split[5]);

                        turnstile.lockedStart = Long.parseLong(split[5]);
                        turnstile.lockedEnd = Long.parseLong(split[6]);
                        turnstile.freeStart = Long.parseLong(split[7]);
                        turnstile.freeEnd = Long.parseLong(split[8]);

                        if (!split[10].equals("public"))
                            if (split[10].equals("private"))
                                turnstile.access = new LinkedList<String>();
                            else
                                turnstile.access = (LinkedList<String>)Arrays.asList(split[10].split(", "));

                        line = bReader.readLine();
                        if (!line.trim().isEmpty()) {
                            String[] buttons = line.split(";");
                            for (int i = 0 ; i < buttons.length; ++i) {
                                String[] coords = buttons[i].split(":");
                                x = Integer.parseInt(coords[0]);
                                y = Integer.parseInt(coords[1]);
                                z = Integer.parseInt(coords[2]);
                                turnstile.buttons.add(new TurnstileButton(split[11], x, y, z, world.getBlockTypeIdAt(x, y, z)));
                            }
                        }

                        turnstiles.add(turnstile);
                    }
                }
                else {
                    if (world == null)
                        world = TurnstileMain.server.getWorld(split[12]);
                    if (world != null) {
                        int x = Integer.parseInt(split[13]);
                        int y = Integer.parseInt(split[14]);
                        int z = Integer.parseInt(split[15]);
                        Turnstile turnstile = new Turnstile(split[0], split[10], split[12], x, y, z);

                        turnstile.price = Double.parseDouble(split[1]);
                        turnstile.item = Integer.parseInt(split[2]);
                        turnstile.durability = Short.parseShort(split[3]);
                        turnstile.amount = Integer.parseInt(split[4]);

                        if (turnstile.item != 0)
                            turnstile.itemsEarned = (int)Double.parseDouble(split[5]);
                        else
                            turnstile.moneyEarned = Double.parseDouble(split[5]);

                        turnstile.lockedStart = Long.parseLong(split[6]);
                        turnstile.lockedEnd = Long.parseLong(split[7]);
                        turnstile.freeStart = Long.parseLong(split[8]);
                        turnstile.freeEnd = Long.parseLong(split[9]);

                        if (!split[11].equalsIgnoreCase("public"))
                            if (split[11].equalsIgnoreCase("private"))
                                turnstile.access = new LinkedList<String>();
                            else
                                turnstile.access = (LinkedList<String>)Arrays.asList(split[11].split(", "));

                        line = bReader.readLine();
                        if (!line.trim().isEmpty()) {
                            String[] buttons = line.split(";");
                            for (int i = 0 ; i < buttons.length; ++i) {
                                String[] coords = buttons[i].split(":");
                                x = Integer.parseInt(coords[0]);
                                y = Integer.parseInt(coords[1]);
                                z = Integer.parseInt(coords[2]);
                                turnstile.buttons.add(new TurnstileButton(split[12], x, y, z, world.getBlockTypeIdAt(x, y, z)));
                            }
                        }

                        turnstiles.add(turnstile);
                    }
                }
            }
            
            saveTurnstiles();
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[Turnstile] Load failed, saving turned off to prevent loss of data");
            loadFailed.printStackTrace();
        }
    }
    
    /**
     * Reads save files to load Sign data
     * Saving is turned off if an error occurs
     */
    public static void loadSigns(World world) {
        try {
            File[] files = new File("plugins/Turnstile").listFiles();

            for (File file: files) {
                String fileName = file.getName();
                String worldName = world.getName();
                
                if (fileName.equals(worldName+"StatusSigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader("plugins/Turnstile/"+fileName));
                    
                    String line = bReader.readLine();
                    while (line != null) {
                        String[] split = line.split("'");

                        Turnstile turnstile = findTurnstile(split[0]);
                        
                        Sign sign = null;
                        
                        try {
                            sign = (Sign)world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3])).getState();
                        }
                        catch (Exception ex) {
                        }
                        
                        int lineNumber = Integer.parseInt(split[4]);

                        if (turnstile != null && sign != null)
                            statusSigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                        
                        line = bReader.readLine();
                    }
                    
                    bReader.close();
                }
                else if (fileName.equals(worldName+"CounterSigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader("plugins/Turnstile/"+fileName));
                    
                    String line = bReader.readLine();
                    while (line != null) {
                        String[] split = line.split("'");

                        Turnstile turnstile = findTurnstile(split[0]);
                        
                        Sign sign = null;
                        
                        try {
                            sign = (Sign)world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3])).getState();
                        }
                        catch (Exception ex) {
                        }
                        
                        int lineNumber = Integer.parseInt(split[4]);

                        if (turnstile != null && sign != null)
                            counterSigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                        
                        line = bReader.readLine();
                    }
                    
                    bReader.close();
                }
                else if (fileName.equals(worldName+"MoneySigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader("plugins/Turnstile/"+fileName));
                    
                    String line = bReader.readLine();
                    while (line != null) {
                        String[] split = line.split("'");

                        Turnstile turnstile = findTurnstile(split[0]);
                        
                        Sign sign = null;
                        
                        try {
                            sign = (Sign)world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3])).getState();
                        }
                        catch (Exception ex) {
                        }
                        
                        int lineNumber = Integer.parseInt(split[4]);

                        if (turnstile != null && sign != null)
                            moneySigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                        
                        line = bReader.readLine();
                    }
                    
                    bReader.close();
                }
                else if (fileName.equals(worldName+"ItemSigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader("plugins/Turnstile/"+fileName));
                    
                    String line = bReader.readLine();
                    while (line != null) {
                        String[] split = line.split("'");

                        Turnstile turnstile = findTurnstile(split[0]);
                        
                        Sign sign = null;
                        
                        try {
                            sign = (Sign)world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3])).getState();
                        }
                        catch (Exception ex) {
                        }
                        
                        int lineNumber = Integer.parseInt(split[4]);

                        if (turnstile != null && sign != null)
                            itemSigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                        
                        line = bReader.readLine();
                    }
                    
                    bReader.close();
                }
            }
        }
        catch (Exception loadFailed) {
            save = false;
            System.out.println("[PvPReward] Loading of Sign data has failed");
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes Turnstile data to save file
     * Old files are overwritten
     */
    public static void saveTurnstiles() {
        //Cancel if saving is turned off
        if (!save) {
            System.out.println("[Turnstile] Warning! Data is not being saved.");
            return;
        }
        
        try {
            Properties p = new Properties();
            
            for (Turnstile turnstile: turnstiles) {
                p.setProperty("Owner", turnstile.owner);
                p.setProperty("Location", turnstile.world+"'"+turnstile.x+"'"+turnstile.y+"'"+turnstile.z);
                p.setProperty("Price", String.valueOf(turnstile.price));
                p.setProperty("MoneyEarned", String.valueOf(turnstile.moneyEarned));
                p.setProperty("ItemID", String.valueOf(turnstile.item));
                p.setProperty("ItemDurability", String.valueOf(turnstile.durability));
                p.setProperty("ItemAmount", String.valueOf(turnstile.amount));
                p.setProperty("ItemsEarned", String.valueOf(turnstile.itemsEarned));
                p.setProperty("OneWay", String.valueOf(turnstile.oneWay));
                p.setProperty("NoFraud", String.valueOf(turnstile.noFraud));
                p.setProperty("AutoCloseTimer", String.valueOf(turnstile.timeOut));
                p.setProperty("FreeTimeRange", turnstile.freeStart+"-"+turnstile.freeEnd);
                p.setProperty("LockedTimeRange", turnstile.lockedStart+"-"+turnstile.lockedEnd);

                if (turnstile.access == null)
                    p.setProperty("Access", "public");
                else if (turnstile.access.isEmpty())
                    p.setProperty("Access", "private");
                else {
                    String access = turnstile.access.toString();
                    p.setProperty("Access", access.substring(1, access.length() - 1));
                }

                if (turnstile.buttons.isEmpty())
                    p.setProperty("Buttons", "");
                else {
                    String buttons = "";
                    for (TurnstileButton button: turnstile.buttons)
                        buttons = buttons.concat(", "+button.toString());
                    p.setProperty("Buttons", buttons.substring(2));
                }

                p.store(new FileOutputStream("plugins/Turnstile/"+turnstile.name+".dat"), null);
            }
        }
        catch (Exception saveFailed) {
            System.err.println("[Turnstile] Save Failed!");
            saveFailed.printStackTrace();
        }
    }
    
    /**
     * Writes Sign data to save file
     * Old files are overwritten
     */
    public static void saveSigns() {
        //Cancel if saving is turned off
        if (!save) {
            System.out.println("[Turnstile] Warning! Data is not being saved.");
            return;
        }
        
        try {
            for (World world: server.getWorlds()) {
                LinkedList<TurnstileSign> tempList = new LinkedList<TurnstileSign>();
                for (TurnstileSign sign: statusSigns)
                    if (sign.sign.getWorld().equals(world))
                        tempList.add(sign);
                
                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            "plugins/Turnstile/"+world.getName()+"StatusSigns.dat"));
                    
                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }
                    
                    bWriter.close();
                }
                
                tempList.clear();
                for (TurnstileSign sign: counterSigns)
                    if (sign.sign.getWorld().equals(world))
                        tempList.add(sign);
                
                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            "plugins/Turnstile/"+world.getName()+"CounterSigns.dat"));
                    
                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }
                    
                    bWriter.close();
                }
                
                tempList.clear();
                for (TurnstileSign sign: moneySigns)
                    if (sign.sign.getWorld().equals(world))
                        tempList.add(sign);
                
                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            "plugins/Turnstile/"+world.getName()+"MoneySigns.dat"));
                    
                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }
                    
                    bWriter.close();
                }
                
                tempList.clear();
                for (TurnstileSign sign: itemSigns)
                    if (sign.sign.getWorld().equals(world))
                        tempList.add(sign);
                
                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            "plugins/Turnstile/"+world.getName()+"ItemSigns.dat"));
                    
                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }
                    
                    bWriter.close();
                }
            }
        }
        catch (Exception saveFailed) {
            System.err.println("[Turnstile] Save Failed!");
            saveFailed.printStackTrace();
        }
    }
    
    /**
     * Returns the Turnstile with the given name
     * 
     * @param name The name of the Turnstile you wish to find
     * @return The Turnstile with the given name or null if not found
     */
    public static Turnstile findTurnstile(String name) {
        //Iterate through the data to find the Turnstile that matches the name
        for (Turnstile turnstile: turnstiles)
            if (turnstile.name.equals(name))
                return turnstile;
        
        //Return null because the Turnstile was not found
        return null;
    }
    
    /**
     * Returns the Turnstile with the given Block
     * 
     * @param block The block of the Turnstile you wish to find
     * @return The Turnstile with the given block or null if not found
     */
    public static Turnstile findTurnstile(Block block) {
        //Iterate through the data to find the Turnstile that matches the given Block
        for (Turnstile turnstile: turnstiles)
            if (turnstile.hasBlock(block))
                return turnstile;
        
        //Return null because the Turnstile was not found
        return null;
    }
}