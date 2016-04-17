package com.codisimus.plugins.turnstile;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Prevents Players from bypassing Turnstiles using Ender Pearls
 *
 * @author Codisimus
 */
public class EnderPearlProtection implements Listener {
    private static final HashMap<Projectile, BukkitTask> tasks = new HashMap<>();

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

        BukkitTask task = new BukkitRunnable() {
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
    	}.runTaskTimer(TurnstileMain.plugin, 1, 1);

        tasks.put(projectile, task);
    }

    private static void cancelTask(Projectile projectile) {
        BukkitTask task = tasks.remove(projectile);
        if (task != null) {
            task.cancel();
        }
        projectile.remove();
    }
}
