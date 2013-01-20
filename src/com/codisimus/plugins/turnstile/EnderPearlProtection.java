package com.codisimus.plugins.turnstile;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Prevents Players from bypassing Turnstiles using Ender Pearls
 *
 * @author Codisimus
 */
public class EnderPearlProtection implements Listener {
    private static HashMap<Projectile, Integer> pearls = new HashMap<Projectile, Integer>();

    @EventHandler (ignoreCancelled = true)
    public void onEnderPearlThrow(ProjectileLaunchEvent event) {
        final Projectile projectile = event.getEntity();
        if (!(projectile instanceof EnderPearl)) {
            return;
        }

        Location location = projectile.getLocation();
        for (Turnstile turnstile: TurnstileMain.getTurnstiles()) {
            if (location.getWorld().getName().equals(turnstile.world)
                    && Math.abs(location.getBlockX() - turnstile.x) <= 1
                    && Math.abs(location.getBlockZ() - turnstile.z) <= 1
                    && Math.abs(location.getBlockY() - turnstile.y) <= 2) {
                projectile.remove();
                return;
            }
        }

        int id = TurnstileMain.server.getScheduler().scheduleSyncRepeatingTask(TurnstileMain.plugin, new Runnable() {
            @Override
    	    public void run() {
                Location location = projectile.getLocation();
                if (location == null) {
                    cancelTask(projectile);
                    return;
                }

                for (Turnstile turnstile: TurnstileMain.getTurnstiles()) {
                    if (location.getWorld().getName().equals(turnstile.world)
                            && Math.abs(location.getBlockX() - turnstile.x) <= 1
                            && Math.abs(location.getBlockZ() - turnstile.z) <= 1
                            && Math.abs(location.getBlockY() - turnstile.y) <= 2) {
                        cancelTask(projectile);
                        break;
                    }
                }
    	    }
    	}, 1, 1);

        pearls.put(projectile, id);
    }

    private static void cancelTask(Projectile projectile) {
        TurnstileMain.server.getScheduler().cancelTask(pearls.get(projectile));
        projectile.remove();
        pearls.remove(projectile);
    }
}
