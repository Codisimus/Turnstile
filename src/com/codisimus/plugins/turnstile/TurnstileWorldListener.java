
package com.codisimus.plugins.turnstile;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 *
 * @author Codisimus
 */
public class TurnstileWorldListener extends WorldListener{

    @Override
    public void onWorldLoad (WorldLoadEvent event) {
        SaveSystem.loadData(event.getWorld());
    }
}

