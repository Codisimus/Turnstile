package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.Register;
import com.codisimus.plugins.turnstile.SaveSystem;
import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileMain;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author Codisimus
 */
public class playerListener extends PlayerListener{
    public static String permission;
    public static String locked;
    public static String free;
    public static String oneWay;
    public static LinkedList<Turnstile> openTurnstiles = new LinkedList<Turnstile>();

    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        //Return if Action was arm flailing
        if (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;
        
        Block block = event.getClickedBlock();
        int id = block.getTypeId();
        //Try to open a Turnstile if a linked switch was activated
        if (TurnstileMain.isSwitch(id)) {
            Turnstile turnstile = SaveSystem.findTurnstile(block);
            
            //Return if the switch is not linked to a Turnstile
            if (turnstile == null)
                return;
            
            //Return if the Turnstile is already open
            if (turnstile.open)
                return;
            
            Player player = event.getPlayer();
            
            //Return if the Player does not have permission to open Turnstiles
            if (!TurnstileMain.hasPermission(player, "open")) {
                player.sendMessage(permission);
                return;
            }
            
            //Return if the Player does not have access rights to the Turnstile
            if (!turnstile.hasAccess(player))
                return;
            
            //Return if the Turnstile is locked
            if (turnstile.isLocked(player.getWorld().getTime())) {
                player.sendMessage(locked);
                return;
            }
            
            //Return without charging if the Turnstile is free
            if (turnstile.isFree(player.getWorld().getTime())) {
                player.sendMessage(free);
                turnstile.open(block);
                return;
            }
            
            //Charge with items if the switch is a Chest
            if (block.getTypeId() == 54) {
                turnstile.checkContents((Chest)block.getState(), player);
                return;
            }
            
            if (TurnstileMain.noFraud) {
                turnstile.open(block);
                
                if (turnstile.price == 0)
                    player.sendMessage(free);
                else
                    player.sendMessage(TurnstileMain.displayCost.replaceAll("<price>", ""+Register.format(turnstile.price)));
            }
            else if (turnstile.checkBalance(player))
                turnstile.open(block);
        }
        //Cancel event if the Wood Door of a Turnstile was clicked
        else if (id == 64 || id == 324)
            for (Turnstile turnstile: SaveSystem.turnstiles) {
                Block gate = turnstile.gate;
                if (gate.equals(block) || TurnstileMain.areNeighbors(gate, block))
                    event.setCancelled(true);
            }
    }

    @Override
    public void onPlayerMove (PlayerMoveEvent event) {
        //Return if no Turnstiles are open
        if (openTurnstiles.isEmpty())
            return;
        
        Block to = event.getTo().getBlock();
        
        //Check each open Turnstile
        for (Turnstile turnstile: openTurnstiles) {
            //Check if the Player stepped into this Turnstile's gate
            if (turnstile.gate.equals(to)) {
                Location from = event.getFrom();
                Player player = event.getPlayer();

                //Teleport Player back where they came from if they entered the Turnstile backwards
                if (Turnstile.oneWay && !turnstile.checkOneWay(from.getBlock())) {
                    from.setX(from.getBlockX()+.5);
                    from.setY(from.getBlockY());
                    from.setZ(from.getBlockZ()+.5);
                    player.teleport(from);
                    player.sendMessage(oneWay);
                    return;
                }

                turnstile.close();

                //If NoFraud mode is off, the Player already payed so Return without charging
                if (!TurnstileMain.noFraud)
                    return;

                //Return without charging if the Turnstile is currently free
                if (turnstile.isFree(player.getWorld().getTime()))
                    return;

                //Teleport Player back where they came from if they could not pay
                if (!turnstile.checkBalance(player)) {
                    from.setX(from.getBlockX()+.5);
                    from.setY(from.getBlockY());
                    from.setZ(from.getBlockZ()+.5);
                    player.teleport(from);
                }
                return;
            }
        }
    }
}

