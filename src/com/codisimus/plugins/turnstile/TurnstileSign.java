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
     */
    public int tickListener() {
        final World world = TurnstileMain.server.getWorld(turnstile.world);

        //Repeat every second
    	return TurnstileMain.server.getScheduler().scheduleSyncRepeatingTask(TurnstileMain.plugin, new Runnable() {
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
    	}, 0L, 20L);
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
}
