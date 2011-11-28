package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.SaveSystem;
import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileMain;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;


/**
 * Listens for griefing events
 *
 * @author Codisimus
 */
public class blockListener extends BlockListener {
    
    /**
     * Does not allow redstone to effect Doors that are linked to Turnstiles
     * 
     * @param event The BlockRedstoneEvent that occurred
     */
    @Override
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        //Return if the Block is not a door
        Block block = event.getBlock();
        int id = block.getTypeId();
        if (!TurnstileMain.isDoor(id))
            return;
        
        //Iterate through all Turnstiles and cancel the event if the Block is the Turnstile gate
        for (Turnstile turnstile: SaveSystem.turnstiles)
            if (turnstile.isBlock(block) || turnstile.isNeighbor(block))
                event.setNewCurrent(event.getOldCurrent());
    }

    /**
     * Does not allow Blocks that are linked to Turnstiles to be broken
     * 
     * @param event The BlockBreakEvent that occurred
     */
    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        Block block = event.getBlock();
        int id = block.getTypeId();
        Player player = event.getPlayer();
        
        //Check if the Turnstile is a Switch or a Chest
        if (TurnstileMain.isSwitch(id) || id == 85) {
            //Return if the Block is not linked to a Turnstile
            Turnstile turnstile = SaveSystem.findTurnstile(block);
            if (turnstile == null)
                return;
            
            //Cancel the Event if the Player is not the Owner
            if (!turnstile.isOwner(player))
                event.setCancelled(true);
        }
        
        //Return if the Block is a Door
        if (!TurnstileMain.isDoor(id))
            return;
        
        //Check the Event if the Block is linked to a Turnstile
        for (Turnstile turnstile: SaveSystem.turnstiles)
            if (turnstile.isBlock(block) || turnstile.isNeighbor(block)) {
                //Cancel the Event if the Player is not the Owner
                if (!turnstile.isOwner(player))
                    event.setCancelled(true);
                
                return;
            }
    }
}
