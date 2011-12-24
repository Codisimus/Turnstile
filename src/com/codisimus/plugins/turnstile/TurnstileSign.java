package com.codisimus.plugins.turnstile;

import org.bukkit.World;
import org.bukkit.block.Sign;

/**
 * A TurnstileSign is a Sign that is linked to a Turnstile
 * There is a value on a specific line which must stay updated
 * 
 * @author Codisimus
 */
public class TurnstileSign {
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
     * Updates the Turnstile Sign when it becomes either locked, free, or open
     * 
     * @param sign The Sign that is to be updated
     * @param line The line of the sign to be updated
     * @param turnstile The Turnstile to be watching
     */
    public void tickListener() {
        final World world = TurnstileMain.server.getWorld(turnstile.world);
        
        //Delay Teleporting
    	TurnstileMain.server.getScheduler().scheduleAsyncRepeatingTask(TurnstileMain.plugin, new Runnable() {
            @Override
    	    public void run() {
                long time = world.getFullTime();
                
                if (time > turnstile.lockedStart && time < turnstile.lockedEnd)
                    sign.setLine(line, "locked");
                else if (time > turnstile.freeStart && time < turnstile.freeEnd)
                    sign.setLine(line, "free");
                else
                    sign.setLine(line, "open");
                    
                sign.update();
    	    }
    	}, 0L, 0);
    }

    /**
     * Increments the Player count displayed on the Sign by one
     * 
     */
    public void incrementCounter() {
        int counter = Integer.parseInt(sign.getLine(line));
        counter++;
        
        sign.setLine(line, String.valueOf(counter));
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
        return turnstile.name+"'"+sign.getX()+"'"+sign.getY()+"'"+sign.getZ()+"'"+line;
    }
}