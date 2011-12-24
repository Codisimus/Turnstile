package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.TurnstileMain;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Loads Turnstile data for each World that is loaded
 *
 * @author Codisimus
 */
public class WorldLoadListener extends WorldListener{

    /**
     * Loads Turnstile data for the loaded World
     * 
     * @param event The WorldLoadEvent that occurred
     */
    @Override
    public void onWorldLoad (WorldLoadEvent event) {
        TurnstileMain.loadTurnstiles(event.getWorld());
        TurnstileMain.loadSigns(event.getWorld());
    }
}