/*
 *  Copyright (C) 2011 Codisimus
 *
 */

package Turnstile;

import java.util.LinkedList;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;


/**
 *
 * @author Codisimus
 */
public class TurnstileBlockListener extends BlockListener {
    
    @Override
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (TurnstileMain.isDoor(block.getType())) {
            LinkedList<Turnstile> turnstiles = SaveSystem.getTurnstiles();
            for (Turnstile turnstile : turnstiles) {
                Block gate = turnstile.gate;
                if (TurnstileMain.isDoor(gate.getType()))
                    if (gate.getLocation().equals(block.getLocation()) || TurnstileMain.areNeighbors(gate, block))
                        event.setNewCurrent(event.getOldCurrent());
            }
        }
    }

    @Override
    public void onBlockBreak (BlockBreakEvent event) {
        Block block = event.getBlock();
        Material mat = block.getType();
        Player player = event.getPlayer();
        LinkedList<Turnstile> Turnstiles = SaveSystem.getTurnstiles();
        if (TurnstileMain.isSwitch(mat))
            for (Turnstile turnstile : Turnstiles) {
                LinkedList<Block> buttons = turnstile.buttons;
                for (Block button : buttons ) {
                    if (button.getLocation().equals(block.getLocation()))
                        if (!turnstile.isOwner(player))
                            event.setCancelled(true);
                }
            }
        else if (TurnstileMain.isDoor(mat))
            for (Turnstile turnstile : Turnstiles) {
                Block gate = turnstile.gate;
                if (block.getLocation().equals(gate.getLocation()) || TurnstileMain.areNeighbors(block, gate))
                    if (!turnstile.isOwner(player))
                        event.setCancelled(true);
            }
        else if (mat.equals(Material.FENCE))
            for (Turnstile turnstile : Turnstiles) {
                if (block.getLocation().equals(turnstile.gate.getLocation()))
                    if (!turnstile.isOwner(player))
                        event.setCancelled(true);
            }
    }
}
