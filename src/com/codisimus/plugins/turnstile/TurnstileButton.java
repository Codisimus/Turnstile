package com.codisimus.plugins.turnstile;

import java.util.Map;
import java.util.TreeMap;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

/**
 * A TurnstileButton is a Block location and the Type ID of the Block
 *
 * @author Codisimus
 */
@SerializableAs("TurnstileButton")
public class TurnstileButton implements ConfigurationSerializable {
    private static TurnstileButton last;
    public String world;
    public int x, y, z;

    /**
     * Constructs a new Button with the given Block
     *
     * @param block The given Block
     */
    public TurnstileButton(Block block) {
        world = block.getWorld().getName();
        x = block.getX();
        y = block.getY();
        z = block.getZ();
    }

    /**
     * Constructs a new TurnstileButton from a Configuration Serialized phase
     *
     * @param map The map of data values
     */
    public TurnstileButton(Map<String, Object> map) {
        String currentLine = null; //The value that is about to be loaded (used for debugging)
        try {
            world = (String) map.get(currentLine = "World");
            x = (Integer) map.get(currentLine = "x");
            y = (Integer) map.get(currentLine = "y");
            z = (Integer) map.get(currentLine = "z");
        } catch (Exception ex) {
            //Print debug messages
            TurnstileMain.logger.severe("Failed to load line: " + currentLine + " In TurnstileButton ");
            TurnstileMain.logger.severe("of Turnstile: " + (Turnstile.current == null ? "unknown" : Turnstile.current));
            if (last != null) {
                TurnstileMain.logger.severe("Last successfull load was...");
                TurnstileMain.logger.severe(last.toString());
            }
        }
        last = this;
    }

    /**
     * Returns the Block that this TurnstileButton Represents
     *
     * @return The Block that this TurnstileButton Represents
     */
    public Block getBlock() {
        return Bukkit.getWorld(world).getBlockAt(x, y, z);
    }

    /**
     * Returns true if the given Block is this TurnstileButton
     *
     * @param block The given Block
     * @return true if the given Block is the same Button
     */
    public boolean matchesBlock(Block block) {
        return block.getX() == x && block.getY() == y && block.getZ() == z
                && block.getWorld().getName().equals(world);
    }

    /**
     * Returns the String representation of this TurnstileButton
     * The format of the returned String is as follows
     * world'x'y'z'type
     *
     * @return The String representation of this TurnstileButton
     */
    @Override
    public String toString() {
        return world + "'" + x + "'" + y + "'" + z;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TurnstileButton) {
            TurnstileButton button = (TurnstileButton) object;
            return button.x == x
                   && button.y == y
                   && button.z == z
                   && button.world.equals(world);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (world != null ? world.hashCode() : 0);
        hash = 47 * hash + x;
        hash = 47 * hash + y;
        hash = 47 * hash + z;
        return hash;
    }

    @Override
    public Map<String, Object> serialize() {
        Map map = new TreeMap();
        map.put("World", world);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        return map;
    }
}
