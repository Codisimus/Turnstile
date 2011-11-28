package com.codisimus.plugins.turnstile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Holds Turnstile data and is used to load/save data
 *
 * @author Codisimus
 */
public class SaveSystem {
    public static LinkedList<Turnstile> turnstiles = new LinkedList<Turnstile>();
    public static boolean save = true;

    /**
     * Reads save file to load Turnstile data
     * Saving is turned off if an error occurs
     */
    public static void load(World world) {
        BufferedReader bReader = null;
        try {
            File[] files = new File("plugins/Turnstile").listFiles();
            Properties p = new Properties();

            for (File file: files) {
                String name = file.getName();
                if (name.endsWith(".dat")) {
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
                            load(loadedWorld);
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

                        if (!split[11].equals("public"))
                            if (split[11].equals("private"))
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
            
            save();
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[Turnstile] Load failed, saving turned off to prevent loss of data");
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes data to save file
     * Old file is overwritten
     */
    public static void save() {
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
                p.setProperty("ItemID",String.valueOf( turnstile.item));
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
