package com.codisimus.plugins.turnstile;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
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
    public static int cost;
    public static PluginManager pm;
    public static boolean useMakeFreeNode;
    public static LinkedList<TurnstileSign> counterSigns = new LinkedList<TurnstileSign>();
    public static LinkedList<TurnstileSign> moneySigns = new LinkedList<TurnstileSign>();
    public static LinkedList<TurnstileSign> itemSigns = new LinkedList<TurnstileSign>();
    public static HashMap<TurnstileSign, Integer> statusSigns = new HashMap<TurnstileSign, Integer>();
    public static boolean citizens;
    static Permission permission;
    static Server server;
    static Logger logger;
    static int defaultTimeOut;
    static boolean useOpenFreeNode;
    static boolean defaultOneWay;
    static boolean defaultNoFraud;
    static Plugin plugin;
    private static HashMap<String, Turnstile> turnstiles = new HashMap<String, Turnstile>();
    private static String dataFolder;
    private Properties p;

    /**
     * Closes all open Turnstiles when this Plugin is disabled
     */
    @Override
    public void onDisable () {
        //Close all open Turnstiles
        logger.info("Closing all open Turnstiles...");
        for (Turnstile turnstile: TurnstileListener.openTurnstiles) {
            turnstile.close();
        }
    }

    /**
     * Calls methods to load this Plugin when it is enabled
     */
    @Override
    public void onEnable () {
        server = getServer();
        logger = getLogger();
        pm = server.getPluginManager();
        plugin = this;

        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dataFolder = dir.getPath();

        dir = new File(dataFolder+"/Turnstiles");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        //Load Config settings
        loadSettings();

        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            Econ.economy = economyProvider.getProvider();
        }

        //Load Data
        loadTurnstiles();
        loadSigns();

        //Register Events
        pm.registerEvents(new TurnstileListener(), this);
        pm.registerEvents(new EnderPearlProtection(), this);
        //Watch for interacting with NPCs if linked to Citizens
        if (citizens) {
            pm.registerEvents(new NPCListener(), this);
        }

        //Register the command found in the plugin.yml
        TurnstileCommand.command = (String)this.getDescription().getCommands().keySet().toArray()[0];
        getCommand(TurnstileCommand.command).setExecutor(new TurnstileCommand());

        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
        }
        logger.info("Turnstile " + this.getDescription().getVersion() + " (Build " + version.getProperty("Build") + ") is enabled!");
    }

    /**
     * Loads settings from the config.properties file
     */
    public void loadSettings() {
        try {
            //Copy the file from the jar if it is missing
            File file = new File(dataFolder+"/config.properties");
            if (!file.exists()) {
                this.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            FileInputStream fis = new FileInputStream(file);
            p.load(fis);

            Turnstile.debug = Boolean.parseBoolean(loadValue("Debug"));

            cost = Integer.parseInt(loadValue("CostToMakeTurnstile"));

            citizens = Boolean.parseBoolean(loadValue("UseCitizens"));

            defaultOneWay = Boolean.parseBoolean(loadValue("OneWayByDefault"));
            defaultNoFraud = Boolean.parseBoolean(loadValue("NoFraudByDefault"));
            defaultTimeOut = Integer.parseInt(loadValue("DefaultAutoCloseTimer"));

            useOpenFreeNode = Boolean.parseBoolean(loadValue("use'openfree'node"));
            useMakeFreeNode = Boolean.parseBoolean(loadValue("use'makefree'node"));

            //Load custom messages
            TurnstileMessages.permission = loadValue("PermissionMessage");
            TurnstileMessages.locked = loadValue("LockedMessage");
            TurnstileMessages.free = loadValue("FreeMessage");
            TurnstileMessages.oneWay = loadValue("OneWayMessage");
            TurnstileMessages.correct = loadValue("CorrectItemMessage");
            TurnstileMessages.wrong = loadValue("WrongItemMessage");
            TurnstileMessages.notEnoughMoney = loadValue("NotEnoughMoneyMessage");
            TurnstileMessages.displayCost = loadValue("DisplayCostMessage");
            TurnstileMessages.open = loadValue("OpenMessage");
            TurnstileMessages.balanceCleared = loadValue("BalanceClearedMessage");
            TurnstileMessages.privateTurnstile = loadValue("PrivateMessage");
            TurnstileMessages.inUse = loadValue("ChestInUseMessage");
            TurnstileMessages.occupied = loadValue("TurnstileOccupiedMessage");
            TurnstileMessages.formatAll();

            fis.close();
        } catch (Exception missingProp) {
            logger.severe("Failed to load Turnstile "+this.getDescription().getVersion());
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
            logger.severe("Missing value for " + key + " in config file");
            logger.severe("Please regenerate config file");
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
     * Reads save files to load Turnstile data
     */
    public static void loadTurnstiles() {
        File[] files = plugin.getDataFolder().listFiles();

        //Organize files
        if (files != null) {
            for (File file: files) {
                String name = file.getName();

                if (name.endsWith("Signs.dat")) {
                    File dest = new File(dataFolder+"/Signs");
                    dest.mkdir();
                    dest = new File(dataFolder+"/Signs/"+name);
                    file.renameTo(dest);
                } else if (name.endsWith(".dat")) {
                    File dest = new File(dataFolder+"/Turnstiles");
                    dest.mkdir();
                    dest = new File(dataFolder+"/Turnstiles/"+name.substring(0, name.length() - 4)+".properties");
                    file.renameTo(dest);
                }
            }
        }

        File dir = new File(dataFolder+"/Turnstiles");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }
        files = new File(dataFolder+"/Turnstiles/").listFiles();
        FileInputStream fis = null;

        for (File file: files) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    fis = new FileInputStream(file);
                    p.load(fis);

                    //Construct a new Turnstile using the file name, Owner name, and Location data
                    String owner = p.getProperty("Owner");
                    String[] location = p.getProperty("Location").split("'");
                    String worldName = location[0];
                    int x = Integer.parseInt(location[1]);
                    int y = Integer.parseInt(location[2]);
                    int z = Integer.parseInt(location[3]);
                    Turnstile turnstile = new Turnstile(name.substring(0, name.length() - 11), owner, worldName, x, y, z);

                    turnstile.price = Double.parseDouble(p.getProperty("Price"));
                    turnstile.moneyEarned = Double.parseDouble(p.getProperty("MoneyEarned"));

                    if (p.containsKey("Items")) {
                        turnstile.setItems(p.getProperty("Items"));
                    } else {
                        turnstile.items.add(new Item(Integer.parseInt(p.getProperty("ItemID")),
                                Short.parseShort(p.getProperty("ItemDurability")), Integer.parseInt(p.getProperty("ItemAmount"))));
                    }

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

                    if (p.containsKey("CooldownTime")) {
                        String[] resetTime = p.getProperty("CooldownTime").split("'");
                        turnstile.days = Integer.parseInt(resetTime[0]);
                        turnstile.hours = Integer.parseInt(resetTime[1]);
                        turnstile.minutes = Integer.parseInt(resetTime[2]);
                        turnstile.seconds = Integer.parseInt(resetTime[3]);

                        turnstile.privateWhileOnCooldown = Boolean.parseBoolean(p.getProperty("RoundDownTime"));
                        turnstile.privateWhileOnCooldown = Boolean.parseBoolean(p.getProperty("PrivateWhileOnCooldown"));
                        turnstile.amountPerCooldown = Integer.parseInt(p.getProperty("PlayersAllowedInPrivateCooldown"));
                    }

                    String access = p.getProperty("Access");
                    if (!access.equals("public")) {
                        turnstile.access = new LinkedList<String>();
                        if (!access.equals("private")) {
                            turnstile.access.addAll(Arrays.asList(access.split(", ")));
                        }
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

                    turnstiles.put(turnstile.name, turnstile);

                    fis.close();

                    file = new File(dataFolder + "/Turnstiles/"
                                    + turnstile.name + ".cooldowntimes");
                    if (file.exists()) {
                        fis = new FileInputStream(file);
                        turnstile.onCooldown.load(fis);
                    } else {
                        turnstile.save();
                    }
                } catch (Exception loadFailed) {
                    logger.severe("Failed to load " + name);
                    loadFailed.printStackTrace();
                } finally {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        if (!turnstiles.isEmpty()) {
            return;
        }

        loadOld();
    }

    /**
     * Reads outdated save files to load Turnstile data
     */
    public static void loadOld() {
        try {
            File file = new File(dataFolder+"/turnstile.save");
            if (!file.exists()) {
                return;
            }

            logger.info("Loading outdated save files");

            BufferedReader bReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                if (split.length == 15) {
                    if (split[11].endsWith("~NETHER")) {
                        split[11].replace("~NETHER", "");
                    }

                    World world = server.getWorld(split[11]);
                    int x = Integer.parseInt(split[12]);
                    int y = Integer.parseInt(split[13]);
                    int z = Integer.parseInt(split[14]);
                    Turnstile turnstile = new Turnstile(split[0], split[9], split[11], x, y, z);

                    turnstile.price = Double.parseDouble(split[1]);

                    int id;
                    short durability = -1;
                    if (split[3].contains(".")) {
                        int index = split[3].indexOf('.');
                        id = Integer.parseInt(split[3].substring(0, index));
                        durability = Short.parseShort(split[3].substring(index + 1));
                    } else
                        id = Integer.parseInt(split[3]);
                    turnstile.items.add(new Item(id, durability, Integer.parseInt(split[4])));

                    if (id != 0) {
                        turnstile.itemsEarned = Integer.parseInt(split[5]);
                    } else {
                        turnstile.moneyEarned = Double.parseDouble(split[5]);
                    }

                    turnstile.lockedStart = Long.parseLong(split[5]);
                    turnstile.lockedEnd = Long.parseLong(split[6]);
                    turnstile.freeStart = Long.parseLong(split[7]);
                    turnstile.freeEnd = Long.parseLong(split[8]);

                    if (!split[10].equals("public")) {
                        if (split[10].equals("private")) {
                            turnstile.access = new LinkedList<String>();
                        } else {
                            turnstile.access = (LinkedList<String>) Arrays.asList(split[10].split(", "));
                        }
                    }

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

                    turnstiles.put(turnstile.name, turnstile);
                } else {
                    World world = server.getWorld(split[12]);
                    int x = Integer.parseInt(split[13]);
                    int y = Integer.parseInt(split[14]);
                    int z = Integer.parseInt(split[15]);
                    Turnstile turnstile = new Turnstile(split[0], split[10], split[12], x, y, z);

                    turnstile.price = Double.parseDouble(split[1]);
                    int id = Integer.parseInt(split[2]);
                    turnstile.items.add(new Item(id, Short.parseShort(split[3]), Integer.parseInt(split[4])));

                    if (id != 0) {
                        turnstile.itemsEarned = (int)Double.parseDouble(split[5]);
                    } else {
                        turnstile.moneyEarned = Double.parseDouble(split[5]);
                    }

                    turnstile.lockedStart = Long.parseLong(split[6]);
                    turnstile.lockedEnd = Long.parseLong(split[7]);
                    turnstile.freeStart = Long.parseLong(split[8]);
                    turnstile.freeEnd = Long.parseLong(split[9]);

                    if (!split[11].equalsIgnoreCase("public")) {
                        if (split[11].equalsIgnoreCase("private")) {
                            turnstile.access = new LinkedList<String>();
                        } else {
                            turnstile.access = (LinkedList<String>) Arrays.asList(split[11].split(", "));
                        }
                    }

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

                    turnstiles.put(turnstile.name, turnstile);
                    turnstile.save();
                }
            }

            bReader.close();
        } catch (Exception loadFailed) {
            logger.severe("Loading data from old save file has failed");
            loadFailed.printStackTrace();
        }
    }

    /**
     * Reads save files to load Sign data
     */
    public static void loadSigns() {
        File dir = new File(dataFolder+"/Signs");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }

        for (File file: dir.listFiles()) {
            try {
                String fileName = file.getName();

                if (fileName.endsWith("StatusSigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader(file));

                    String line = bReader.readLine();
                    while (line != null) {
                        try {
                            World world = server.getWorld(fileName.substring(0, fileName.length() - 15));

                            String[] split = line.split("'");

                            Turnstile turnstile = findTurnstile(split[0]);

                            Block block = world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3]));

                            switch (block.getType()) {
                            case SIGN: break;
                            case SIGN_POST: break;
                            case WALL_SIGN: break;
                            default: continue;
                            }

                            Sign sign = (Sign)block.getState();

                            int lineNumber = Integer.parseInt(split[4]);

                            if (turnstile != null) {
                                TurnstileSign tsSign = new TurnstileSign(sign, turnstile, lineNumber);
                                statusSigns.put(tsSign, tsSign.tickListener());
                            }
                        } catch (Exception loadFailed) {
                            logger.severe("Failed to load line:");
                            logger.severe(line);
                            logger.severe("in file:");
                            logger.severe(file.getName());
                            loadFailed.printStackTrace();
                        } finally {
                            line = bReader.readLine();
                        }
                    }

                    bReader.close();
                } else if (fileName.endsWith("CounterSigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader(file));

                    String line = bReader.readLine();
                    while (line != null) {
                        try {
                            World world = server.getWorld(fileName.substring(0, fileName.length() - 16));

                            String[] split = line.split("'");

                            Turnstile turnstile = findTurnstile(split[0]);

                            Block block = world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3]));

                            switch (block.getType()) {
                            case SIGN: break;
                            case SIGN_POST: break;
                            case WALL_SIGN: break;
                            default: continue;
                            }

                            Sign sign = (Sign)block.getState();

                            int lineNumber = Integer.parseInt(split[4]);

                            if (turnstile != null) {
                                counterSigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                            }
                        } catch (Exception loadFailed) {
                            logger.severe("Failed to load line:");
                            logger.severe(line);
                            logger.severe("in file:");
                            logger.severe(file.getName());
                            loadFailed.printStackTrace();
                        } finally {
                            line = bReader.readLine();
                        }
                    }

                    bReader.close();
                } else if (fileName.endsWith("MoneySigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader(file));

                    String line = bReader.readLine();
                    while (line != null) {
                        try {
                            World world = server.getWorld(fileName.substring(0, fileName.length() - 14));

                            String[] split = line.split("'");

                            Turnstile turnstile = findTurnstile(split[0]);

                            Block block = world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3]));

                            switch (block.getType()) {
                            case SIGN: break;
                            case SIGN_POST: break;
                            case WALL_SIGN: break;
                            default: continue;
                            }

                            Sign sign = (Sign)block.getState();

                            int lineNumber = Integer.parseInt(split[4]);

                            if (turnstile != null) {
                                moneySigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                            }
                        } catch (Exception loadFailed) {
                            logger.severe("Failed to load line:");
                            logger.severe(line);
                            logger.severe("in file:");
                            logger.severe(file.getName());
                            loadFailed.printStackTrace();
                        } finally {
                            line = bReader.readLine();
                        }
                    }

                    bReader.close();
                } else if (fileName.endsWith("ItemSigns.dat")) {
                    BufferedReader bReader = new BufferedReader(new FileReader(file));

                    String line = bReader.readLine();
                    while (line != null) {
                        try {
                            World world = server.getWorld(fileName.substring(0, fileName.length() - 13));

                            String[] split = line.split("'");

                            Turnstile turnstile = findTurnstile(split[0]);

                            Block block = world.getBlockAt(Integer.parseInt(split[1]),
                                    Integer.parseInt(split[2]), Integer.parseInt(split[3]));

                            switch (block.getType()) {
                            case SIGN: break;
                            case SIGN_POST: break;
                            case WALL_SIGN: break;
                            default: continue;
                            }

                            Sign sign = (Sign)block.getState();

                            int lineNumber = Integer.parseInt(split[4]);

                            if (turnstile != null) {
                                itemSigns.add(new TurnstileSign(sign, turnstile, lineNumber));
                            }
                        } catch (Exception loadFailed) {
                            logger.severe("Failed to load line:");
                            logger.severe(line);
                            logger.severe("in file:");
                            logger.severe(file.getName());
                            loadFailed.printStackTrace();
                        } finally {
                            line = bReader.readLine();
                        }
                    }

                    bReader.close();
                }
            } catch (Exception ex) {
                logger.severe("Unexpected error occured!");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Invokes save() method for each Turnstile
     * Also invokes the saveSigns() method
     */
    public static void saveAll() {
        for (Turnstile turnstile: turnstiles.values()) {
            saveTurnstile(turnstile);
        }
        saveSigns();
    }

    /**
     * Writes the given Turnstile to its save file
     * If the file already exists, it is overwritten
     *
     * @param turnstile The given Turnstile
     */
    static void saveTurnstile(Turnstile turnstile) {
        try {
            Properties p = new Properties();

            p.setProperty("Owner", turnstile.owner);
            p.setProperty("Location", turnstile.world+"'"+turnstile.x+"'"+turnstile.y+"'"+turnstile.z);
            p.setProperty("Price", String.valueOf(turnstile.price));
            p.setProperty("MoneyEarned", String.valueOf(turnstile.moneyEarned));
            p.setProperty("Items", turnstile.itemsToString());
            p.setProperty("ItemsEarned", String.valueOf(turnstile.itemsEarned));
            p.setProperty("OneWay", String.valueOf(turnstile.oneWay));
            p.setProperty("NoFraud", String.valueOf(turnstile.noFraud));
            p.setProperty("AutoCloseTimer", String.valueOf(turnstile.timeOut));
            p.setProperty("FreeTimeRange", turnstile.freeStart+"-"+turnstile.freeEnd);
            p.setProperty("LockedTimeRange", turnstile.lockedStart+"-"+turnstile.lockedEnd);
            p.setProperty("CooldownTime", turnstile.days+"'" + turnstile.hours + "'"
                            + turnstile.minutes + "'" + turnstile.seconds);
            p.setProperty("RoundDownTime", String.valueOf(turnstile.roundDown));
            p.setProperty("PrivateWhileOnCooldown", String.valueOf(turnstile.privateWhileOnCooldown));
            p.setProperty("PlayersAllowedInPrivateCooldown", String.valueOf(turnstile.amountPerCooldown));

            if (turnstile.access == null) {
                p.setProperty("Access", "public");
            } else if (turnstile.access.isEmpty()) {
                p.setProperty("Access", "private");
            } else {
                String access = turnstile.access.toString();
                p.setProperty("Access", access.substring(1, access.length() - 1));
            }

            if (turnstile.buttons.isEmpty()) {
                p.setProperty("Buttons", "");
            } else {
                String buttons = "";
                for (TurnstileButton button: turnstile.buttons) {
                    buttons = buttons.concat(", "+button.toString());
                }
                p.setProperty("Buttons", buttons.substring(2));
            }

            //Write the Turnstile Properties to file
            FileOutputStream fos = new FileOutputStream(dataFolder+"/Turnstiles/"+turnstile.name+".properties");
            p.store(fos, null);
            fos.close();

            fos = new FileOutputStream(dataFolder+"/Turnstiles/"+turnstile.name+".cooldowntimes");
            turnstile.onCooldown.store(fos, null);
            fos.close();
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
        }
    }

    /**
     * Writes Sign data to save file
     * Old files are overwritten
     */
    public static void saveSigns() {
        try {
            for (World world: server.getWorlds()) {
                LinkedList<TurnstileSign> tempList = new LinkedList<TurnstileSign>();
                for (TurnstileSign sign: statusSigns.keySet()) {
                    if (sign.sign.getWorld().equals(world)) {
                        tempList.add(sign);
                    }
                }

                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            dataFolder + "/Signs/" + world.getName()
                            + "StatusSigns.dat"));

                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }

                    bWriter.close();
                }

                tempList.clear();
                for (TurnstileSign sign: counterSigns) {
                    if (sign.sign.getWorld().equals(world)) {
                        tempList.add(sign);
                    }
                }

                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            dataFolder + "/Signs/" + world.getName()
                            + "CounterSigns.dat"));

                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }

                    bWriter.close();
                }

                tempList.clear();
                for (TurnstileSign sign: moneySigns) {
                    if (sign.sign.getWorld().equals(world)) {
                        tempList.add(sign);
                    }
                }

                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            dataFolder + "/Signs/" + world.getName()
                            + "MoneySigns.dat"));

                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }

                    bWriter.close();
                }

                tempList.clear();
                for (TurnstileSign sign: itemSigns) {
                    if (sign.sign.getWorld().equals(world)) {
                        tempList.add(sign);
                    }
                }

                if (!tempList.isEmpty()) {
                    BufferedWriter bWriter = new BufferedWriter(new FileWriter(
                            dataFolder + "/Signs/" + world.getName()
                            + "ItemSigns.dat"));

                    for (TurnstileSign sign: tempList) {
                        bWriter.write(sign.toString());
                        bWriter.newLine();
                    }

                    bWriter.close();
                }
            }
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
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
        turnstile.save();
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
                int id = statusSigns.get(sign);
                server.getScheduler().cancelTask(id);
                statusSigns.remove(sign);
            }
        }

        turnstiles.remove(turnstile.name);

        File trash = new File(dataFolder+"/Turnstiles/"+turnstile.name+".properties");
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
     * @param player The Player reloading the data
     */
    public static void rl(Player player) {
        turnstiles.clear();
        loadTurnstiles();

        logger.info("reloaded");
        if (player != null) {
            player.sendMessage("Turnstile reloaded");
        }
    }

    /**
     * Return the other half of the Double Chest
     * If this is a not a Chest then null is returned
     * If this is a single Chest then the normal Chest Block is returned
     *
     * @return The other half of the Double Chest
     */
    public static Block getOtherHalf(Block block) {
        if (block.getTypeId() != 54) {
            return null;
        }

        Block neighbor = block.getRelative(0, 0, 1);
        if (neighbor.getTypeId() == 54) {
            return neighbor;
        }

        neighbor = block.getRelative(1, 0, 0);
        if (neighbor.getTypeId() == 54) {
            return neighbor;
        }

        neighbor = block.getRelative(0, 0, -1);
        if (neighbor.getTypeId() == 54) {
            return neighbor;
        }

        neighbor = block.getRelative(-1, 0, 0);
        if (neighbor.getTypeId() == 54) {
            return neighbor;
        }

        //Return the single Block
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
