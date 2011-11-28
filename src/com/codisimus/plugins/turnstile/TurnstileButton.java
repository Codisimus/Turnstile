package com.codisimus.plugins.turnstile;

import org.bukkit.block.Block;

/**
 * A TurnstileButton is a Block location and the Type ID of the Block
 * 
 * @author Codisimus
 */
public class TurnstileButton {
    public String world;
    public int x;
    public int y;
    public int z;
    public int type;

    /**
     * Constructs a new Button with the given Block
     * 
     * @param block The given Block
     */
    public TurnstileButton (Block block) {
        world = block.getWorld().getName();
        x = block.getX();
        y = block.getY();
        z = block.getZ();
        type = block.getTypeId();
    }
    
    /**
     * Constructs a new Button with the given Block data
     * 
     * @param world The name of the World
     * @param x The x-coordinate of the Block
     * @param y The y-coordinate of the Block
     * @param z The z-coordinate of the Block
     * @param type The Type ID of the Block
     */
    public TurnstileButton (String world, int x, int y, int z, int type) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
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
        return world+"'"+x+"'"+y+"'"+z+"'"+type;
    }
}