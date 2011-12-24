package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileMain;
import com.codisimus.plugins.turnstile.TurnstileSign;
import java.util.Iterator;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Door;


/**
 * Listens for griefing events
 *
 * @author Codisimus
 */
public class BlockEventListener extends BlockListener {
    private static enum Type { NAME, PRICE, COST, COUNTER, MONEY, ITEMS, ACCESS, STATUS }
    
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
                
            case SIGN: //Fall through
            case SIGN_POST: //Fall through
            case WALL_SIGN: //Unlink Signs if they are broken
                Sign sign = (Sign)event.getBlock().getState();
                for (int i = 0; i <= 2; i = i + 2) {
                    String line = sign.getLine(i);
                    
                    if (line.equals("Currently:")) {
                        Iterator itr = TurnstileMain.statusSigns.iterator();
                        while (itr.hasNext())
                            if (((TurnstileSign)itr.next()).sign.equals(sign))
                                itr.remove();
                    }
                    else if (line.equals("Player Count:")) {
                        Iterator itr = TurnstileMain.counterSigns.iterator();
                        while (itr.hasNext())
                            if (((TurnstileSign)itr.next()).sign.equals(sign))
                                itr.remove();
                    }
                }
                
                return;
                
            default: return;
        }
    }
    
    /**
     * Listens for Players placing Turnstile Signs
     * 
     * @param event The SignChangeEvent that occurred
     */
    @Override
    public void onSignChange (SignChangeEvent event) {
        //Check if this is a Turnstile sign to be linked
        String line = event.getLine(0).toLowerCase();
        if (!line.equals("ts link"))
            return;

        //Cancel if the Player does not have permission to create Inns
        Player player = event.getPlayer();
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(PlayerEventListener.permissionMsg);
            return;
        }
        
        //Find the Turnstile that the Sign will be linked to
        line = event.getLine(1);
        Turnstile turnstile = TurnstileMain.findTurnstile(line);
        if (turnstile == null) {
            player.sendMessage("Turnstile "+line+" does not exsist.");
            return;
        }
        
        Type type;
        Sign sign = (Sign)event.getBlock().getState();
        
        //Return if the third line is not a valid type
        line = event.getLine(2).toUpperCase();
        try {
            type = Type.valueOf(line);
        }
        catch (Exception notEnum) {
            player.sendMessage(line+" is not a valid type. Valid types: NAME,"
                    + "PRICE, COST, COUNTER, MONEY, ITEMS, ACCESS, FREE, LOCKED");
            return;
        }

        switch (type) {
            case NAME:
                event.setLine(0, "Turnstile:");
                event.setLine(1, turnstile.name);
                break;
                
            case PRICE:
                event.setLine(0, "Price:");
                event.setLine(1, String.valueOf(turnstile.price));
                break;
                
            case COST:
                event.setLine(0, "Cost:");
                event.setLine(1, turnstile.amount+" of "+Material.getMaterial(turnstile.item).name());
                break;
                
            case COUNTER:
                event.setLine(0, "Player Count:");
                event.setLine(1, "0");
                
                TurnstileMain.counterSigns.add(new TurnstileSign(sign, turnstile, 3));
                break;
                
            case MONEY:
                event.setLine(0, "Money Earned:");
                event.setLine(1, String.valueOf(turnstile.moneyEarned));
                break;
                
            case ITEMS:
                event.setLine(0, "Items Earned:");
                event.setLine(1, String.valueOf(turnstile.itemsEarned));
                break;
                
            case ACCESS:
                event.setLine(0, "Access:");
                
                if (turnstile.access == null)
                    event.setLine(1, "public");
                else if (turnstile.access.isEmpty())
                    event.setLine(1, "private");
                else {
                    String access = turnstile.access.toString();
                    event.setLine(1, access.substring(1, access.length() - 1));
                }
                
                break;
                
            case STATUS:
                event.setLine(0, "Currently:");
                event.setLine(1, "open");
                
                //Start the tickListener for the Sign
                TurnstileSign tsSign = new TurnstileSign(sign, turnstile, 1);
                TurnstileMain.statusSigns.add(tsSign);
                tsSign.tickListener();
                
                break;
                
            default: return;
        }

        //Return if the fourth line is not a valid type
        line = event.getLine(3).toUpperCase();
        try {
            type = Type.valueOf(line);
        }
        catch (Exception notEnum) {
            player.sendMessage(line+" is not a valid type. Valid types: NAME,"
                    + "PRICE, COST, COUNTER, MONEY, ITEMS, ACCESS, FREE, LOCKED");
            return;
        }

        switch (type) {
            case NAME:
                event.setLine(2, "Turnstile:");
                event.setLine(3, turnstile.name);
                break;
                
            case PRICE:
                event.setLine(2, "Price:");
                event.setLine(3, String.valueOf(turnstile.price));
                break;
                
            case COST:
                event.setLine(2, "Cost:");
                event.setLine(3, turnstile.amount+" of "+Material.getMaterial(turnstile.item).name());
                break;
                
            case COUNTER:
                event.setLine(2, "Player Count:");
                event.setLine(3, "0");
                
                TurnstileMain.counterSigns.add(new TurnstileSign(sign, turnstile, 3));
                TurnstileMain.saveSigns();
                break;
                
            case MONEY:
                event.setLine(2, "Money Earned:");
                event.setLine(3, String.valueOf(turnstile.moneyEarned));
                break;
                
            case ITEMS:
                event.setLine(2, "Items Earned:");
                event.setLine(3, String.valueOf(turnstile.itemsEarned));
                break;
                
            case ACCESS:
                event.setLine(2, "Access:");
                
                if (turnstile.access == null)
                    event.setLine(3, "public");
                else if (turnstile.access.isEmpty())
                    event.setLine(3, "private");
                else {
                    String access = turnstile.access.toString();
                    event.setLine(3, access.substring(1, access.length() - 1));
                }
                
                break;
                
            case STATUS:
                event.setLine(2, "Currently:");
                event.setLine(3, "open");
                
                //Start the tickListener for the Sign
                TurnstileSign tsSign = new TurnstileSign(sign, turnstile, 3);
                TurnstileMain.statusSigns.add(tsSign);
                tsSign.tickListener();
                
                TurnstileMain.saveSigns();
                break;
                
            default: return;
        }
    }
}
