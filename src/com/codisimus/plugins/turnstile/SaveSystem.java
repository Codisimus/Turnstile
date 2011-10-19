package com.codisimus.plugins.turnstile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
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
    public static void loadFromFile() {
        BufferedReader bReader = null;
        try {
            new File("plugins/Turnstile").mkdir();
            new File("plugins/Turnstile/turnstile.save").createNewFile();
            bReader = new BufferedReader(new FileReader("plugins/Turnstile/turnstile.save"));
            String line = "";
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                if (split.length == 15) {
                    if (split[11].endsWith("~NETHER"))
                        split[11].replace("~NETHER", "");
                    World world = TurnstileMain.server.getWorld(split[11]);
                    if (world != null) {
                        int x = Integer.parseInt(split[12]);
                        int y = Integer.parseInt(split[13]);
                        int z = Integer.parseInt(split[14]);
                        Turnstile turnstile = new Turnstile(split[0], world.getBlockAt(x, y, z), split[9]);
                        turnstiles.add(turnstile);
                        
                        turnstile.price = Double.parseDouble(split[1]);
                        if (split[3].contains(".")) {
                            int index = split[3].indexOf('.');
                            turnstile.item = Integer.parseInt(split[3].substring(0, index));
                            turnstile.durability = Short.parseShort(split[3].substring(index+1));
                        }
                        else
                            turnstile.item = Integer.parseInt(split[3]);
                        turnstile.amount = Integer.parseInt(split[4]);
                        turnstile.earned = Double.parseDouble(split[2]);
                        turnstile.lockedStart = Long.parseLong(split[5]);
                        turnstile.lockedEnd = Long.parseLong(split[6]);
                        turnstile.freeStart = Long.parseLong(split[7]);
                        turnstile.freeEnd = Long.parseLong(split[8]);
                        turnstile.access = split[10];
                        
                        line = bReader.readLine();
                        if (!line.trim().isEmpty()) {
                            String[] buttons = line.split(";");
                            for (int i = 0 ; i < buttons.length; ++i) {
                                String[] coords = buttons[i].split(":");
                                x = Integer.parseInt(coords[0]);
                                y = Integer.parseInt(coords[1]);
                                z = Integer.parseInt(coords[2]);
                                turnstile.buttons.add(world.getBlockAt(x, y, z));
                            }
                        }
                    }
                }
                else {
                    World world = TurnstileMain.server.getWorld(split[12]);
                    if (world != null) {
                        int x = Integer.parseInt(split[13]);
                        int y = Integer.parseInt(split[14]);
                        int z = Integer.parseInt(split[15]);
                        Turnstile turnstile = new Turnstile(split[0], world.getBlockAt(x, y, z), split[10]);
                        turnstiles.add(turnstile);
                        
                        turnstile.price = Double.parseDouble(split[1]);
                        turnstile.item = Integer.parseInt(split[2]);
                        turnstile.durability = Short.parseShort(split[3]);
                        turnstile.amount = Integer.parseInt(split[4]);
                        turnstile.earned = Double.parseDouble(split[5]);
                        turnstile.lockedStart = Long.parseLong(split[6]);
                        turnstile.lockedEnd = Long.parseLong(split[7]);
                        turnstile.freeStart = Long.parseLong(split[8]);
                        turnstile.freeEnd = Long.parseLong(split[9]);
                        turnstile.access = split[11];
                        
                        line = bReader.readLine();
                        if (!line.trim().isEmpty()) {
                            String[] buttons = line.split(";");
                            for (int i = 0 ; i < buttons.length; ++i) {
                                String[] coords = buttons[i].split(":");
                                x = Integer.parseInt(coords[0]);
                                y = Integer.parseInt(coords[1]);
                                z = Integer.parseInt(coords[2]);
                                turnstile.buttons.add(world.getBlockAt(x, y, z));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception loadFailed) {
            save = false;
            System.err.println("[Turnstile] Load failed, saving turned off to prevent loss of data");
            loadFailed.printStackTrace();
        }
    }
    
    /**
     * Reads save file to load Turnstile data for given World
     * Saving is turned off if an error occurs
     */
    public static void loadData(World world) {
        BufferedReader bReader = null;
        try {
            new File("plugins/Turnstile").mkdir();
            new File("plugins/Turnstile/turnstile.save").createNewFile();
            bReader = new BufferedReader(new FileReader("plugins/Turnstile/turnstile.save"));
            String line = "";
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                if (split.length == 15) {
                    if (split[11].equals(world.getName())) {
                        int x = Integer.parseInt(split[12]);
                        int y = Integer.parseInt(split[13]);
                        int z = Integer.parseInt(split[14]);
                        Turnstile turnstile = new Turnstile(split[0], world.getBlockAt(x, y, z), split[9]);
                        turnstiles.add(turnstile);
                        
                        turnstile.price = Double.parseDouble(split[1]);
                        if (split[3].contains(".")) {
                            int index = split[3].indexOf('.');
                            turnstile.item = Integer.parseInt(split[3].substring(0, index));
                            turnstile.durability = Short.parseShort(split[3].substring(index+1));
                        }
                        else
                            turnstile.item = Integer.parseInt(split[3]);
                        turnstile.amount = Integer.parseInt(split[4]);
                        turnstile.earned = Double.parseDouble(split[2]);
                        turnstile.lockedStart = Long.parseLong(split[5]);
                        turnstile.lockedEnd = Long.parseLong(split[6]);
                        turnstile.freeStart = Long.parseLong(split[7]);
                        turnstile.freeEnd = Long.parseLong(split[8]);
                        turnstile.access = split[10];
                        
                        line = bReader.readLine();
                        if (!line.trim().isEmpty()) {
                            String[] buttons = line.split(";");
                            for (int i = 0 ; i < buttons.length; ++i) {
                                String[] coords = buttons[i].split(":");
                                x = Integer.parseInt(coords[0]);
                                y = Integer.parseInt(coords[1]);
                                z = Integer.parseInt(coords[2]);
                                turnstile.buttons.add(world.getBlockAt(x, y, z));
                            }
                        }
                    }
                }
                else {
                    if (split[12].equals(world.getName())) {
                        int x = Integer.parseInt(split[13]);
                        int y = Integer.parseInt(split[14]);
                        int z = Integer.parseInt(split[15]);
                        Turnstile turnstile = new Turnstile(split[0], world.getBlockAt(x, y, z), split[10]);
                        turnstiles.add(turnstile);
                        
                        turnstile.price = Double.parseDouble(split[1]);
                        turnstile.item = Integer.parseInt(split[2]);
                        turnstile.durability = Short.parseShort(split[3]);
                        turnstile.amount = Integer.parseInt(split[4]);
                        turnstile.earned = Double.parseDouble(split[5]);
                        turnstile.lockedStart = Long.parseLong(split[6]);
                        turnstile.lockedEnd = Long.parseLong(split[7]);
                        turnstile.freeStart = Long.parseLong(split[8]);
                        turnstile.freeEnd = Long.parseLong(split[9]);
                        turnstile.access = split[11];
                        
                        line = bReader.readLine();
                        if (!line.trim().isEmpty()) {
                            String[] buttons = line.split(";");
                            for (int i = 0 ; i < buttons.length; ++i) {
                                String[] coords = buttons[i].split(":");
                                x = Integer.parseInt(coords[0]);
                                y = Integer.parseInt(coords[1]);
                                z = Integer.parseInt(coords[2]);
                                turnstile.buttons.add(world.getBlockAt(x, y, z));
                            }
                        }
                    }
                }
            }
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
        if (!save)
            return;
        
        try {
            //Open save file for writing data
            BufferedWriter bWriter = new BufferedWriter(new FileWriter("plugins/Turnstile/turnstile.save"));
            
            //Iterate through all Turnstiles to write each to the file
            for(Turnstile turnstile: turnstiles) {
                //Write data in format "name;price;item;durability;amount;earned;lockedStart;lockedEnd;freeStart;freeEnd;owner;access;world;x;y;z"
                bWriter.write(turnstile.name.concat(";"));
                bWriter.write(turnstile.price+";");
                bWriter.write(turnstile.item+";");
                bWriter.write(turnstile.durability+";");
                bWriter.write(turnstile.amount+";");
                bWriter.write(turnstile.earned+";");
                bWriter.write(turnstile.lockedStart+";");
                bWriter.write(turnstile.lockedEnd+";");
                bWriter.write(turnstile.freeStart+";");
                bWriter.write(turnstile.freeEnd+";");
                bWriter.write(turnstile.owner.concat(";"));
                bWriter.write(turnstile.access.concat(";"));
                Block block = turnstile.gate;
                bWriter.write(block.getWorld().getName()+";");
                bWriter.write(block.getX()+";");
                bWriter.write(block.getY()+";");
                bWriter.write(block.getZ()+";");
                
                //Start a new line to save linked Blocks
                bWriter.newLine();
                
                //Iterate through all linked Blocks to write data in format "x1:y1:z1;x2:y2:z2;x3:y3:z3;..."
                for (Block button : turnstile.buttons) {
                    bWriter.write(button.getX()+":");
                    bWriter.write(button.getY()+":");
                    bWriter.write(button.getZ()+";");
                }
                
                //Start a new line to write data for the next Turnstile
                bWriter.newLine();
            }
            
            bWriter.close();
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
        for (Turnstile turnstile: turnstiles) {
            LinkedList<Block> buttons = turnstile.buttons;
            for (Block button: buttons)
                if (button.equals(block))
                    return turnstile;
        }
        
        //Return null because the Turnstile was not found
        return null;
    }
}
