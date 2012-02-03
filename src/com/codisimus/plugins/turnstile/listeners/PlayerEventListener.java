package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.Econ;
import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileMain;
import com.codisimus.plugins.turnstile.TurnstileSign;
import java.util.LinkedList;
import net.citizensnpcs.api.CitizensManager;
import net.citizensnpcs.resources.npclib.HumanNPC;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Door;

/**
 * Listens for Players using Turnstiles
 *
 * @author Codisimus
 */
public class PlayerEventListener extends PlayerListener{
    public static String permissionMsg;
    public static String lockedMsg;
    public static String freeMsg;
    public static String oneWayMsg;
    public static LinkedList<Turnstile> openTurnstiles = new LinkedList<Turnstile>();

    /**
     * Listens for Players attempting to open Turnstiles
     * 
     * @param event The PlayerInteractEvent that occurred
     */
    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        //Return if the Action was arm flailing
        Block block = event.getClickedBlock();
        if (block == null)
            return;
        
        switch (block.getType()) {
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Cancel event if the Wood Door of a Turnstile was clicked
                block = ((Door)block.getState().getData()).isTopHalf() ?
                        block.getRelative(BlockFace.DOWN) : block;
                
                for (Turnstile turnstile: TurnstileMain.getTurnstiles())
                    if (turnstile.isBlock(block)) {
                        event.setCancelled(true);
                        return;
                    }
                
                return;
                
            case CHEST: //Fall through
            case STONE_PLATE: //Fall through
            case WOOD_PLATE: //Fall through
            case STONE_BUTTON: //Try to open a Turnstile if a linked switch was activated
                //Return if the switch is not linked to a Turnstile
                Turnstile turnstile = TurnstileMain.findTurnstile(block);
                if (turnstile == null)
                    return;

                //Return if the Turnstile is already open
                if (turnstile.open)
                    return;

                //Return if the Player does not have permission to open Turnstiles
                Player player = event.getPlayer();
                if (!TurnstileMain.hasPermission(player, "open")) {
                    player.sendMessage(permissionMsg);
                    return;
                }

                //Return if the Player does not have access rights to the Turnstile
                if (!turnstile.hasAccess(player))
                    return;
                
                long time = player.getWorld().getTime();

                //Return if the Turnstile is locked
                if (turnstile.isLocked(time)) {
                    player.sendMessage(lockedMsg);
                    return;
                }

                //Open Turnstile and Return without charging if the Turnstile is free
                if (turnstile.isFree(time)) {
                    player.sendMessage(freeMsg);
                    turnstile.open(block);
                    return;
                }

                //Charge with items if the switch is a Chest
                if (block.getTypeId() == 54) {
                    turnstile.checkContents(((Chest)block.getState()).getInventory(), player);
                    return;
                }

                if (turnstile.noFraud) {
                    turnstile.open(block);

                    if (turnstile.price == 0)
                        player.sendMessage(freeMsg);
                    else
                        player.sendMessage(TurnstileMain.displayCostMsg.replaceAll("<price>",
                                ""+Econ.economy.format(turnstile.price)));
                }
                else if (turnstile.checkBalance(player))
                    turnstile.open(block);
                
                return;
                
            default: return;
        }
    }
    
    /**
     * Listens for Players attempting to open Turnstiles
     * 
     * @param event The PlayerInteractEvent that occurred
     */
    @Override
    public void onPlayerInteractEntity (PlayerInteractEntityEvent event) {
        //Return if the Entity clicked is not an NPC
        Entity entity = event.getRightClicked();
        if(!CitizensManager.isNPC(entity))
            return;
        
        HumanNPC npc = CitizensManager.get(entity);
        
        //Return if the switch is not linked to a Turnstile
        Turnstile turnstile = TurnstileMain.findTurnstile(npc.getBaseLocation().getBlock());
        if (turnstile == null)
            return;

        //Return if the Turnstile is already open
        if (turnstile.open)
            return;

        //Return if the Player does not have permission to open Turnstiles
        Player player = event.getPlayer();
        if (!TurnstileMain.hasPermission(player, "open")) {
            player.sendMessage(permissionMsg);
            return;
        }

        //Return if the Player does not have access rights to the Turnstile
        if (!turnstile.hasAccess(player))
            return;

        long time = player.getWorld().getTime();
        
        //Return if the Turnstile is locked
        if (turnstile.isLocked(time)) {
            player.sendMessage(lockedMsg);
            return;
        }

        //Open Turnstile and Return without charging if the Turnstile is free
        if (turnstile.isFree(time)) {
            player.sendMessage(freeMsg);
            turnstile.open(null);
            return;
        }
        
        turnstile.checkContents(npc.getInventory(), player);
    }

    /**
     * Listens for Players entering open Turnstiles
     * 
     * @param event The PlayerMoveEvent that occurred
     */
    @Override
    public void onPlayerMove (PlayerMoveEvent event) {
        //Return if no Turnstiles are open
        if (openTurnstiles.isEmpty())
            return;
        
        Block to = event.getTo().getBlock();
        
        //Check each open Turnstile
        for (Turnstile turnstile: openTurnstiles) {
            //Check if the Player stepped into this Turnstile's gate
            if (turnstile.isBlock(to)) {
                Location from = event.getFrom();
                Player player = event.getPlayer();

                //Send the Player back to the previous Block if they entered the Turnstile backwards
                if (turnstile.oneWay && !turnstile.checkOneWay(from.getBlock())) {
                    event.setTo(from);
                    player.sendMessage(oneWayMsg);
                    return;
                }

                turnstile.close();
                
                //Send the Player to the middle of the Block
                Location middle = to.getLocation().add(0.5, 0, 0.5);
                Location playerLocation = player.getLocation();
                middle.setPitch(playerLocation.getPitch());
                middle.setYaw(playerLocation.getYaw());
                event.setTo(middle);
                
                //Increment the counter on linked Signs
                for (TurnstileSign sign: TurnstileMain.counterSigns) {
                    if (sign.turnstile.equals(turnstile))
                        sign.incrementCounter();
                }

                //If NoFraud mode is off, the Player already payed so Return without charging
                if (!turnstile.noFraud)
                    return;

                //Return without charging if the Turnstile is currently free
                if (turnstile.isFree(player.getWorld().getTime()))
                    return;

                //Send the Player back to the previous Block if they could not pay
                if (!turnstile.checkBalance(player)) {
                    event.setTo(from);
                    return;
                }
                
                return;
            }
        }
    }
}