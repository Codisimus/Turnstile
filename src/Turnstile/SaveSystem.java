
package Turnstile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;

/**
 *
 * @author Codisimus
 */
public class SaveSystem {
    private static LinkedList<Turnstile> turnstiles = new LinkedList<Turnstile>();
    private static boolean save = true;

    /**
     * Reads save file to load Turnstile data
     * Saving is turned off if an error occurs
     */
    protected static void loadFromFile() {
        BufferedReader bReader = null;
        try {
            new File("plugins/Turnstile").mkdir();
            new File("plugins/Turnstile/turnstile.save").createNewFile();
            bReader = new BufferedReader(new FileReader("plugins/Turnstile/turnstile.save"));
            String line = "";
            while ((line = bReader.readLine()) != null) {
                String[] split = line.split(";");
                String name = split[0];
                double price = Double.parseDouble(split[1]);
                double earned = Double.parseDouble(split[2]);
                String item = split[3];
                int amount = Integer.parseInt(split[4]);
                long lockedStart = Long.parseLong(split[5]);
                long lockedEnd = Long.parseLong(split[6]);
                long freeStart = Long.parseLong(split[7]);
                long freeEnd = Long.parseLong(split[8]);
                String owner = split[9];
                String access = split[10];
                int x = Integer.parseInt(split[12]);
                int y = Integer.parseInt(split[13]);
                int z = Integer.parseInt(split[14]);
                World world;
                Block block;
                try {
                    world = TurnstileMain.server.getWorld(split[11]);
                    block = world.getBlockAt(x, y, z);
                }
                catch (NullPointerException newWorld) {
                    Environment environment = Environment.NORMAL;
                    if (split[3].endsWith("~NETHER")) {
                        environment = Environment.NETHER;
                        split[3] = split[3].replace("~NETHER", "");
                    }
                    world = TurnstileMain.server.createWorld(split[11], environment);
                    block = world.getBlockAt(x, y, z);
                }
                LinkedList<Block> buttonList = new LinkedList<Block>();
                line = bReader.readLine();
                if (!line.trim().isEmpty()) {
                    String[] buttons = line.split(";");
                    for (int i = 0 ; i < buttons.length; ++i) {
                        String[] coords = buttons[i].split(":");
                        x = Integer.parseInt(coords[0]);
                        y = Integer.parseInt(coords[1]);
                        z = Integer.parseInt(coords[2]);
                        buttonList.add(world.getBlockAt(x, y, z));
                    }
                }
                Turnstile Turnstile = new Turnstile(name, block, buttonList, price, earned, item,
                        amount, owner, access, lockedStart, lockedEnd, freeStart, freeEnd);
                turnstiles.add(Turnstile);
            }
        }
        catch (Exception e) {
            save = false;
            System.out.println("[Turnstile] Load failed, saving turned off to prevent loss of data");
            e.printStackTrace();
        }
    }

    /**
     * Writes data to save file
     * Old file is overwritten
     */
    protected static void save() {
        //cancels if saving is turned off
        if (!save)
            return;
        BufferedWriter bWriter = null;
        try {
            bWriter = new BufferedWriter(new FileWriter("plugins/Turnstile/turnstile.save"));
            for(Turnstile turnstile : turnstiles) {
                bWriter.write(turnstile.name.concat(";"));
                bWriter.write(turnstile.price+";");
                bWriter.write(turnstile.earned+";");
                bWriter.write(turnstile.item.concat(";"));
                bWriter.write(turnstile.amount+";");
                bWriter.write(turnstile.lockedStart+";");
                bWriter.write(turnstile.lockedEnd+";");
                bWriter.write(turnstile.freeStart+";");
                bWriter.write(turnstile.freeEnd+";");
                bWriter.write(turnstile.owner.concat(";"));
                bWriter.write(turnstile.access.concat(";"));
                Block block = turnstile.gate;
                World world = block.getWorld();
                String name = world.getName();
                if (world.getEnvironment().equals(Environment.NETHER))
                    name = name.concat("~NETHER");
                bWriter.write(name+";");
                bWriter.write(block.getX()+";");
                bWriter.write(block.getY()+";");
                bWriter.write(block.getZ()+";");
                bWriter.newLine();
                for (Block button : turnstile.buttons) {
                    bWriter.write(button.getX()+":");
                    bWriter.write(button.getY()+":");
                    bWriter.write(button.getZ()+";");
                }
                bWriter.newLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                bWriter.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the LinkedList of saved Turnstiles
     * 
     * @return the LinkedList of saved Turnstiles
     */
    public static LinkedList<Turnstile> getTurnstiles() {
        return turnstiles;
    }
    
    /**
     * Returns the Turnstile with the given name
     * 
     * @param name The name of the Turnstile you wish to find
     * @return The Turnstile with the given name or null if not found
     */
    public static Turnstile findTurnstile(String name) {
        for (Turnstile Turnstile : turnstiles) {
            if (Turnstile.name.equals(name))
                return Turnstile;
        }
        return null;
    }

    /**
     * Adds the Turnstile to the LinkedList of saved Turnstiles
     * 
     * @param turnstile The Turnstile to be added
     */
    protected static void addTurnstile(Turnstile turnstile) {
        try {
            turnstiles.add(turnstile);
            save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes the Turnstile from the LinkedList of saved Turnstiles
     * 
     * @param turnstile The Turnstile to be removed
     */
    protected static void removeTurnstile(Turnstile turnstile){
        try {
            turnstile.buttons.clear();
            turnstiles.remove(turnstile);
            save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
