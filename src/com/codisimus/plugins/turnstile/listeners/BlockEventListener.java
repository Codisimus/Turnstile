package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileMain;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.Door;


/**
 * Listens for griefing events
 *
 * @author Codisimus
 */
public class BlockEventListener extends BlockListener {
    
    /**
     * Does not allow redstone to effect Doors that are linked to Turnstiles
     * 
     * @param event The BlockRedstoneEvent that occurred
     */
    @Override
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        //Return if the Block is not a door
        Block block = event.getBlock();
        switch (block.getType()) {
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Fall through
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK: break;
                
            default: return;
        }
        
        if (((Door)block.getState().getData()).isTopHalf())
            block = block.getRelative(BlockFace.DOWN);
        
        //Iterate through all Turnstiles and cancel the event if the Block is the Turnstile gate
        for (Turnstile turnstile: TurnstileMain.turnstiles)
            if (turnstile.isBlock(block))
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
        Player player = event.getPlayer();
        
        switch (block.getType()) {
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Fall through
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK: //Cancel the Event if the Block is linked to a Turnstile
                block = ((Door)block.getState().getData()).isTopHalf() ?
                        block.getRelative(BlockFace.DOWN) : block;
                
                for (Turnstile turnstile: TurnstileMain.turnstiles)
                    if (turnstile.isBlock(block)) {
                        //Cancel the Event if the Player is not the Owner
                        if (player == null || !turnstile.isOwner(player))
                            event.setCancelled(true);

                        return;
                    }
                
                return;
                
            case CHEST: //Fall through
            case STONE_PLATE: //Fall through
            case WOOD_PLATE: //Fall through
            case STONE_BUTTON: //Do not allow Turnstile Blocks to be broken
                //Return if the Block is not linked to a Turnstile
                Turnstile turnstile = TurnstileMain.findTurnstile(block);
                if (turnstile == null)
                    return;

                //Cancel the Event if the Player is not the Owner
                if (player == null || !turnstile.isOwner(player))
                    event.setCancelled(true);
                
                return;
                
            default: return;
        }
    }
}
