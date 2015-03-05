package com.codisimus.plugins.turnstile;

import java.util.Map;
import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * A TurnstileSign is a Sign that is linked to a Turnstile
 * There is a value on a specific line which must stay updated
 *
 * @author Codisimus
 */
@SerializableAs("TurnstileSign")
public class TurnstileSign implements ConfigurationSerializable {
    static {
        ConfigurationSerialization.registerClass(TurnstileSign.class, "TurnstileSign");
    }
    private static TurnstileSign last;
    public Sign sign;
    public Turnstile turnstile;
    public int line;

    /**
     * Constructs a new TurnstileSign with the given data
     *
     * @param sign The Sign object
     * @param turnstile The Turnstile that the Sign is linked to
     * @param line The line of the Sign that holds the data
     */
    public TurnstileSign (Sign sign, Turnstile turnstile, int line) {
        this.sign = sign;
        this.turnstile = turnstile;
        this.line = line;
    }

    /**
     * Constructs a new TurnstileSign from a Configuration Serialized phase
     *
     * @param map The map of data values
     */
    public TurnstileSign(Map<String, Object> map) {
        String currentLine = null; //The value that is about to be loaded (used for debugging)
        try {
            turnstile = TurnstileMain.findTurnstile((String) map.get(currentLine = "Turnstile"));

            World world = Bukkit.getWorld(turnstile.world);
            int x = (Integer) map.get(currentLine = "x");
            int y = (Integer) map.get(currentLine = "y");
            int z = (Integer) map.get(currentLine = "z");

            Block block = world.getBlockAt(x, y, z);
            switch (block.getType()) {
                case SIGN:
                case WALL_SIGN:
                    break;
                default:
                    TurnstileMain.logger.warning("The block '" + x + ", " + y + ", " + z + "' in '"  + world + "' is not a SIGN and has been unlinked.");
                    TurnstileMain.logger.warning("THIS SIGN UNLINKING IS PERMANANT IF YOU MODIFY THIS TURNSTILE IN ANYWAY!");
            }

            line = (Integer) map.get(currentLine = "Line");
        } catch (Exception ex) {
            //Print debug messages
            TurnstileMain.logger.severe("Failed to load line: " + currentLine);
            TurnstileMain.logger.severe("of TurnstileSign");
            if (last != null) {
                TurnstileMain.logger.severe("Last successfull load was...");
                TurnstileMain.logger.severe("TurnstileSign: " + (last != null ? "unknown" : last));
            }
        }
        last = this;
    }

    /**
     * Updates the Turnstile Sign when it becomes either locked, free, or open
     * 
     * @return The BukkitTask to be run
     */
    public BukkitTask tickListenerTask() {
        final World world = Bukkit.getWorld(turnstile.world);

        //Repeat every second
    	return new BukkitRunnable() {
            @Override
    	    public void run() {
                sign = (Sign) sign.getBlock().getState();
                long time = world.getTime();

                if (turnstile.isLocked(time)) {
                    sign.setLine(line, "locked");
                } else if (turnstile.isFree(time)) {
                    sign.setLine(line, "free");
                } else {
                    sign.setLine(line, "open");
                }

                sign.update();
    	    }
    	}.runTaskTimer(TurnstileMain.plugin, 0L, 20L);
    }

    /**
     * Increments the Player count displayed on the Sign by one
     * If the Sign displays the money earned, increment by the price
     */
    public void incrementCounter() {
        sign = (Sign) sign.getBlock().getState();

        int amount = Integer.parseInt(sign.getLine(line));
        amount++;

        sign.setLine(line, String.valueOf(amount));
        sign.update();
    }

    /**
     * Increments the amount of money earned that is displayed on the Sign
     */
    public void incrementEarned() {
        sign = (Sign) sign.getBlock().getState();

        String earned = sign.getLine(line).split(" ")[0];
        double newEarned = Double.parseDouble(earned) + turnstile.price;

        sign.setLine(line, Econ.format(newEarned));
        sign.update();
    }

    /**
     * Clears each line of the Sign
     * Should only be used before unlinking the Sign
     */
    public void clear() {
        for (int i = 0; i < 4; i++) {
            sign.setLine(i, "");
        }
        sign.update();
    }

    /**
     * Returns the String representation of this TurnstileSign
     * The format of the returned String is as follows
     * TurnstileName'x'y'z'line
     *
     * @return The String representation of this TurnstileSign
     */
    @Override
    public String toString() {
        return turnstile.name+"'" + sign.getX() + "'" + sign.getY()
                + "'" + sign.getZ() + "'" + line;
    }

    @Override
    public Map<String, Object> serialize() {
        Map map = new TreeMap();
        map.put("Turnstile", turnstile.name);
        map.put("x", sign.getX());
        map.put("y", sign.getY());
        map.put("z", sign.getZ());
        map.put("Line", line);
        return map;
    }
}
