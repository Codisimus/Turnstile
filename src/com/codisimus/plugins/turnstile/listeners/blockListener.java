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

    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        Block block = event.getBlock();
        int id = block.getTypeId();
        
        if (TurnstileMain.isSwitch(id) || id == 85) {
            Turnstile turnstile = SaveSystem.findTurnstile(block);
            
            if (turnstile == null)
                return;
            
            if (!turnstile.isOwner(event.getPlayer()))
                event.setCancelled(true);
        }
        
        if (!TurnstileMain.isDoor(id))
            return;
            
        for (Turnstile turnstile: SaveSystem.turnstiles)
            if (turnstile.isBlock(block) || turnstile.isNeighbor(block)) {
                if (!turnstile.isOwner(event.getPlayer()))
                    event.setCancelled(true);
                
                return;
            }
    }
}
