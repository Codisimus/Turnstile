package com.codisimus.plugins.turnstile;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Listens for Turnstile NPC interactions
 *
 * @author Codisimus
 */
public class NPCListener implements Listener {

    /**
     * Listens for Players interacting with linked NPCs
     *
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler
    public void onPlayerInteractEntity (PlayerInteractEntityEvent event) {
        //Return if the Entity clicked is not an NPC
        Entity entity = event.getRightClicked();
        if(!CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);

        //Return if the NPC is not linked to a Turnstile
        Turnstile turnstile = TurnstileMain.findTurnstile(npc.getStoredLocation().getBlock());
        if (turnstile == null) {
            return;
        }

        //Return if the Turnstile is already open
        if (turnstile.open) {
            return;
        }

        //Return if the Player does not have permission to open Turnstiles
        Player player = event.getPlayer();
        if (!player.hasPermission("turnstile.open")) {
            player.sendMessage(TurnstileConfig.permission);
            return;
        }

        //Return if the Player does not have access rights to the Turnstile
        if (!turnstile.hasAccess(player)) {
            return;
        }

        long time = player.getWorld().getTime();

        //Return if the Turnstile is locked
        if (turnstile.isLocked(time)) {
            player.sendMessage(TurnstileConfig.locked);
            return;
        }

        //Open Turnstile and Return without charging if the Turnstile is free
        if (turnstile.isFree(time)) {
            player.sendMessage(TurnstileConfig.free);
            turnstile.open(null);
            return;
        }

        turnstile.checkContents(player.getInventory(), player);
    }
}
