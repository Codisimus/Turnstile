package com.codisimus.plugins.turnstile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Door;


/**
 * Listens for interactions with Turnstiles
 *
 * @author Codisimus
 */
public class TurnstileListener implements Listener {
    private static enum Type { NAME, PRICE, COST, COUNTER, MONEY, ITEMS, ACCESS, STATUS }
    public static LinkedList<Turnstile> openTurnstiles = new LinkedList<Turnstile>();
    private static HashMap<Player, Block> openChests = new HashMap<Player, Block>();
    static HashMap<Player, Block> occupiedTrendulas = new HashMap<Player, Block>();

    /**
     * Listens for Players attempting to open Turnstiles
     * 
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler
    public void onPlayerInteract (PlayerInteractEvent event) {
        if (event.isCancelled())
            return;
        
        //Return if the Action was arm flailing
        Block block = event.getClickedBlock();
        if (block == null)
            return;
        
        switch (block.getType()) {
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Get the bottom half of the door
                block = ((Door)block.getState().getData()).isTopHalf() ?
                        block.getRelative(BlockFace.DOWN) : block;
                
            case TRAP_DOOR: //Fall through
            case FENCE_GATE: //Cancel event if a Turnstile Gate was clicked
                for (Turnstile turnstile: TurnstileMain.getTurnstiles())
                    if (turnstile.isBlock(block)) {
                        event.setCancelled(true);
                        return;
                    }
                
                break;
                
            case CHEST:
                //Return if the Chest is not linked to a Turnstile
                if (TurnstileMain.findTurnstile(block) == null)
                    return;
                
                //Remove the Player as having the Chest open if they are offline
                for (Player player: openChests.keySet())
                    if (!player.isOnline())
                        openChests.remove(player);
                
                Player chestOpener = event.getPlayer();
                
                Block chestBlock = openChests.containsValue(block) ? block : TurnstileMain.getOtherHalf(block);
                
                //Check if the Chest is already in use
                if (openChests.containsValue(chestBlock)) {
                    Block using = openChests.get(chestOpener);
                    if (using == null || !using.equals(chestBlock)) {
                        event.setCancelled(true);
                        chestOpener.sendMessage(TurnstileMessages.inUse);
                    }
                }
                else
                    openChests.put(chestOpener, block);
                
                break;
                
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
                    player.sendMessage(TurnstileMessages.permission);
                    return;
                }

                //Return if the Player does not have access rights to the Turnstile
                if (!turnstile.hasAccess(player))
                    return;
                
                long time = player.getWorld().getTime();

                //Return if the Turnstile is locked
                if (turnstile.isLocked(time)) {
                    player.sendMessage(TurnstileMessages.locked);
                    return;
                }

                //Open Turnstile and Return without charging if the Turnstile is free
                if (turnstile.isFree(time)) {
                    player.sendMessage(TurnstileMessages.free);
                    turnstile.open(block);
                    return;
                }

                if (turnstile.noFraud) {
                    turnstile.open(block);

                    if (turnstile.price == 0)
                        player.sendMessage(TurnstileMessages.free);
                    else
                        player.sendMessage(TurnstileMessages.displayCost.replaceAll("<price>",
                                ""+Econ.economy.format(turnstile.price)));
                }
                else if (turnstile.checkBalance(player))
                    turnstile.open(block);
                
            default:
        }
    }

    /**
     * Listens for Players entering open Turnstiles or leaving Chests
     * 
     * @param event The PlayerMoveEvent that occurred
     */
    @EventHandler
    public void onPlayerMove (PlayerMoveEvent event) {
        if (event.isCancelled())
            return;
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        if (openChests.containsKey(player)) {
            Block block = openChests.get(player);
            if ((int)to.getPitch() != (int)from.getPitch() || to.distance(block.getLocation()) > 8) {
                openChests.remove(player);

                Turnstile turnstile = TurnstileMain.findTurnstile(block);

                if (turnstile != null)
                    turnstile.checkContents(((Chest)block.getState()).getInventory(), player);
            }
        }
        
        //Return if no Turnstiles are open
        if (openTurnstiles.isEmpty())
            return;
        
        Block toBlock = to.getBlock();
        
        Block trendula = occupiedTrendulas.get(player);
        if (trendula != null)
            if (trendula.equals(toBlock))
                return;
            else
                TurnstileMain.closeTurnstile(trendula);
        
        //Check each open Turnstile
        for (Turnstile turnstile: openTurnstiles) {
            //Check if the Player stepped into this Turnstile's gate
            if (turnstile.isBlock(toBlock)) {
                Location pushBackTo;
                switch (toBlock.getType()) {
                    case TRAP_DOOR: //Fall through
                    case WOOD_DOOR: //Fall through
                    case WOODEN_DOOR: //Fall through
                    case IRON_DOOR: //Fall through
                    case IRON_DOOR_BLOCK: //Set pushback farther back so Players cannot glitch through
                        pushBackTo = from.getBlock().getLocation();
                        pushBackTo.add(0.5, 0, 0.5);
                        pushBackTo.setPitch(from.getPitch());
                        pushBackTo.setYaw(from.getYaw());
                        break;
                        
                    default: pushBackTo = from; break;
                }

                //Send the Player back to the previous Block if they entered the Turnstile backwards
                if (turnstile.oneWay && !turnstile.checkOneWay(from.getBlock())) {
                    event.setTo(pushBackTo);
                    player.sendMessage(TurnstileMessages.oneWay);
                    return;
                }
                
                //Send the Player back to the previous Block if the trendula is occupied
                if (occupiedTrendulas.containsValue(toBlock) && (!occupiedTrendulas.containsKey(player)
                        || !occupiedTrendulas.get(player).equals(toBlock))) {
                    event.setTo(pushBackTo);
                    player.sendMessage(TurnstileMessages.occupied);
                    return;
                }
                
                //Charge the Player if necessary
                if (turnstile.noFraud && !turnstile.isFree(player.getWorld().getTime()))
                    if (!turnstile.checkBalance(player)) {
                        event.setTo(pushBackTo);
                        return;
                    }
                
                //Mark the trendula as occupied
                occupiedTrendulas.put(player, toBlock);
                
                //Increment the counter on linked Signs
                for (TurnstileSign sign: TurnstileMain.counterSigns)
                    if (sign.turnstile.equals(turnstile))
                        sign.incrementCounter();
                
                return;
            }
        }
    }
    
    /**
     * Does not allow redstone to effect Doors that are linked to Turnstiles
     * 
     * @param event The BlockRedstoneEvent that occurred
     */
    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        //Return if the Block is not a door
        Block block = event.getBlock();
        switch (block.getType()) {
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Fall through
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK: //Get the bottom half of the door
                if (((Door)block.getState().getData()).isTopHalf())
                    block = block.getRelative(BlockFace.DOWN);
                
            case TRAP_DOOR: break;
                
            default: return;
        }
        
        //Iterate through all Turnstiles and cancel the event if the Block is the Turnstile gate
        for (Turnstile turnstile: TurnstileMain.getTurnstiles())
            if (turnstile.isBlock(block))
                event.setNewCurrent(event.getOldCurrent());
    }

    /**
     * Does not allow Blocks that are linked to Turnstiles to be broken
     * 
     * @param event The BlockBreakEvent that occurred
     */
    @EventHandler
    public void onBlockBreak (BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        
        Block block = event.getBlock();
        Player player = event.getPlayer();
        
        switch (block.getType()) {
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Fall through
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK: //Get the bottom half of the Door
                block = ((Door)block.getState().getData()).isTopHalf() ?
                        block.getRelative(BlockFace.DOWN) : block;
                
            case TRAP_DOOR: //Fall through
            case FENCE_GATE: //Fall through
            case FENCE: //Cancel the Event if the Block is linked to a Turnstile
                for (Turnstile turnstile: TurnstileMain.getTurnstiles())
                    if (turnstile.isBlock(block)) {
                        //Cancel the Event if the Player is not the Owner
                        if (player == null || !turnstile.isOwner(player))
                            event.setCancelled(true);

                        return;
                    }
                
                break;
                
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
                
                break;
                
            case SIGN: //Fall through
            case SIGN_POST: //Fall through
            case WALL_SIGN: //Unlink Signs if they are broken
                Sign sign = (Sign)event.getBlock().getState();
                for (int i = 0; i <= 2; i = i + 2) {
                    String line = sign.getLine(i);
                    
                    if (line.equals("Currently:")) {
                        for (TurnstileSign tsSign: TurnstileMain.statusSigns.keySet())
                            if (tsSign.sign.equals(sign)) {
                                int id = TurnstileMain.statusSigns.get(tsSign);
                                TurnstileMain.server.getScheduler().cancelTask(id);
                                TurnstileMain.statusSigns.remove(tsSign);
                            }
                    }
                    else if (line.equals("Player Count:")) {
                        Iterator<TurnstileSign> itr = TurnstileMain.counterSigns.iterator();
                        while (itr.hasNext())
                            if (itr.next().sign.equals(sign))
                                itr.remove();
                    }
                    else if (line.equals("Money Earned:")) {
                        Iterator<TurnstileSign> itr = TurnstileMain.moneySigns.iterator();
                        while (itr.hasNext())
                            if (itr.next().sign.equals(sign))
                                itr.remove();
                    }
                    else if (line.equals("Items Earned:")) {
                        Iterator<TurnstileSign> itr = TurnstileMain.itemSigns.iterator();
                        while (itr.hasNext())
                            if (itr.next().sign.equals(sign))
                                itr.remove();
                    }
                }
                
                TurnstileMain.saveSigns();
                
            default:
        }
    }
    
    /**
     * Listens for Players placing Turnstile Signs
     * 
     * @param event The SignChangeEvent that occurred
     */
    @EventHandler
    public void onSignChange (SignChangeEvent event) {
        if (event.isCancelled())
            return;
        
        //Check if this is a Turnstile sign to be linked
        String line = event.getLine(0).toLowerCase();
        if (!line.equals(TurnstileCommand.command+" link"))
            return;

        //Cancel if the Player does not have permission to create Turnstile Signs
        Player player = event.getPlayer();
        if (!TurnstileMain.hasPermission(player, "sign")) {
            player.sendMessage(TurnstileMessages.permission);
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
        if (line.isEmpty()) {
            event.setLine(0, line);
            event.setLine(1, line);
        }
        else {
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
                    event.setLine(1, Econ.format(turnstile.price));
                    break;

                case COST:
                    event.setLine(0, "Cost:");
                    event.setLine(1, turnstile.amount+" of "+Material.getMaterial(turnstile.item).name());
                    break;

                case COUNTER:
                    event.setLine(0, "Player Count:");
                    event.setLine(1, "0");

                    TurnstileMain.counterSigns.add(new TurnstileSign(sign, turnstile, 1));
                    TurnstileMain.saveSigns();
                    break;

                case MONEY:
                    event.setLine(0, "Money Earned:");
                    event.setLine(1, Econ.format(turnstile.moneyEarned));

                    TurnstileMain.moneySigns.add(new TurnstileSign(sign, turnstile, 1));
                    TurnstileMain.saveSigns();
                    break;

                case ITEMS:
                    event.setLine(0, "Items Earned:");
                    event.setLine(1, String.valueOf(turnstile.itemsEarned));

                    TurnstileMain.itemSigns.add(new TurnstileSign(sign, turnstile, 1));
                    TurnstileMain.saveSigns();
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
                    TurnstileMain.statusSigns.put(tsSign, tsSign.tickListener());

                    break;

                default: return;
            }
        }

        //Return if the fourth line is not a valid type
        line = event.getLine(3).toUpperCase();
        if (line.isEmpty()) {
            event.setLine(2, line);
            event.setLine(3, line);
        }
        else {
            try {
                type = Type.valueOf(line);
            }
            catch (Exception notEnum) {
                player.sendMessage(line+" is not a valid type. Valid types: NAME,"
                        + "PRICE, COST, COUNTER, MONEY, ITEMS, ACCESS, STATUS");
                
                TurnstileMain.saveSigns();
                return;
            }

            switch (type) {
                case NAME:
                    event.setLine(2, "Turnstile:");
                    event.setLine(3, turnstile.name);
                    break;

                case PRICE:
                    event.setLine(2, "Price:");
                    event.setLine(3, Econ.format(turnstile.price));
                    break;

                case COST:
                    event.setLine(2, "Cost:");
                    event.setLine(3, turnstile.amount+" of "+Material.getMaterial(turnstile.item).name());
                    break;

                case COUNTER:
                    event.setLine(2, "Player Count:");
                    event.setLine(3, "0");

                    TurnstileMain.counterSigns.add(new TurnstileSign(sign, turnstile, 3));
                    break;

                case MONEY:
                    event.setLine(2, "Money Earned:");
                    event.setLine(3, Econ.format(turnstile.moneyEarned));

                    TurnstileMain.moneySigns.add(new TurnstileSign(sign, turnstile, 3));
                    TurnstileMain.saveSigns();
                    break;

                case ITEMS:
                    event.setLine(2, "Items Earned:");
                    event.setLine(3, String.valueOf(turnstile.itemsEarned));

                    TurnstileMain.itemSigns.add(new TurnstileSign(sign, turnstile, 3));
                    TurnstileMain.saveSigns();
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
                    TurnstileMain.statusSigns.put(tsSign, tsSign.tickListener());

                    TurnstileMain.saveSigns();
                    break;

                default:
            }
        }
        
        TurnstileMain.saveSigns();
    }
}