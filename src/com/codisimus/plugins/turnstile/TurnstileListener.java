package com.codisimus.plugins.turnstile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Listens for interactions with Turnstiles
 *
 * @author Codisimus
 */
public class TurnstileListener implements Listener {
    private static final int SIGN_MAX_LENGTH = 20;
    private static final int FIRST_TYPE_LOCATION = 2;
    private static final int SECOND_TYPE_LOCATION = 3;
    public static LinkedList<Turnstile> openTurnstiles = new LinkedList<>();
    static HashMap<Player, Block> occupiedTrendulas = new HashMap<>();
    private static HashMap<Player, Block> openChests = new HashMap<>();

    /**
     * Listens for Players attempting to open Turnstiles
     *
     * @param event The PlayerInteractEvent that occurred
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        //Return if the Action was arm flailing
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        switch (block.getType()) {
        case WOOD_DOOR:
        case WOODEN_DOOR:
        case TRAP_DOOR:
        case FENCE_GATE: //Cancel event if a Turnstile Gate was clicked
            for (Turnstile turnstile : TurnstileMain.getTurnstiles()) {
                if (turnstile.isBlock(block)) {
                    event.setCancelled(true);
                    return;
                }
            }
            break;

        case CHEST:
        case TRAPPED_CHEST:
            //Return if the Chest is not linked to a Turnstile
            if (TurnstileMain.findTurnstile(block) == null) {
                return;
            }

            //Remove the Player as having the Chest open if they are offline
            for (Player player : openChests.keySet()) {
                if (!player.isOnline()) {
                    openChests.remove(player);
                }
            }

            Player chestOpener = event.getPlayer();

            Block chestBlock = openChests.containsValue(block) ? block : TurnstileMain.getOtherSide(block);

            //Check if the Chest is already in use
            if (openChests.containsValue(chestBlock)) {
                Block using = openChests.get(chestOpener);
                if (using == null || !using.equals(chestBlock)) {
                    event.setCancelled(true);
                    chestOpener.sendMessage(TurnstileMessages.inUse);
                }
            } else {
                openChests.put(chestOpener, block);
            }
            break;

        case STONE_PLATE:
        case WOOD_PLATE:
        case STONE_BUTTON:
        case WOOD_BUTTON: //Try to open a Turnstile if a linked switch was activated
            //Return if the switch is not linked to a Turnstile
            Turnstile turnstile = TurnstileMain.findTurnstile(block);
            if (turnstile == null) {
                return;
            }

            if (turnstile.open) {
                return;
            }

            Player player = event.getPlayer();
            if (!player.hasPermission("turnstile.open")) {
                player.sendMessage(TurnstileMessages.permission);
                return;
            }

            if (!turnstile.hasAccess(player)) {
                return;
            }

            long time = player.getWorld().getTime();

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

                if (turnstile.price == 0) {
                    player.sendMessage(TurnstileMessages.free);
                } else {
                    player.sendMessage(TurnstileMessages.displayCost.replace("<price>",
                            "" + Econ.economy.format(turnstile.price)));
                }
            } else if (turnstile.checkBalance(player)) {
                turnstile.open(block);
            }
            break;

        default:
            break;
        }
    }

    /**
     * Listens for Players leaving Chests
     *
     * @param event The PlayerMoveEvent that occurred
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        //Return if the Chest opener was not a Player
        HumanEntity chestCloser = (Player)event.getPlayer();
        if (!(chestCloser instanceof Player)) {
            return;
        }
        Player player = (Player) chestCloser;

        if (openChests.containsKey(player)) {
            Block block = openChests.get(player);
            openChests.remove(player);

            Turnstile turnstile = TurnstileMain.findTurnstile(block);
            if (turnstile != null) {
                turnstile.checkContents(((Chest) block.getState()).getInventory(), player);
            }
        }
    }

    /**
     * Listens for Players entering open Turnstiles
     *
     * @param event The PlayerMoveEvent that occurred
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        //Return if no Turnstiles are open
        if (openTurnstiles.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        Block toBlock = to.getBlock();

        Block trendula = occupiedTrendulas.get(player);
        if (trendula != null) {
            if (trendula.equals(toBlock)) {
                return;
            } else {
                TurnstileMain.closeTurnstile(trendula);
            }
        }

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

                default:
                    pushBackTo = from;
                    break;
                }

                //Send the Player back to the previous Block if they entered the Turnstile backwards
                if (turnstile.oneWay && !turnstile.checkOneWay(from.getBlock())) {
                    event.setTo(pushBackTo);
                    player.sendMessage(TurnstileMessages.oneWay);
                    return;
                }

                //Send the Player back to the previous Block if the trendula is occupied
                if (occupiedTrendulas.containsValue(toBlock)
                        && (!occupiedTrendulas.containsKey(player)
                        || !occupiedTrendulas.get(player).equals(toBlock))) {
                    event.setTo(pushBackTo);
                    player.sendMessage(TurnstileMessages.occupied);
                    return;
                }

                //Charge the Player if necessary
                if (turnstile.noFraud && !turnstile.isFree(player.getWorld().getTime())) {
                    if (!turnstile.checkBalance(player)) {
                        event.setTo(pushBackTo);
                        return;
                    }
                }

                //Mark the trendula as occupied
                occupiedTrendulas.put(player, toBlock);

                //Increment the counter on linked Signs
                for (TurnstileSign sign: TurnstileMain.counterSigns) {
                    if (sign.turnstile.equals(turnstile)) {
                        sign.incrementCounter();
                    }
                }

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
        case WOOD_DOOR:
        case WOODEN_DOOR:
        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case TRAP_DOOR:
        case FENCE_GATE:
            break;
        default:
            return;
        }

        //Iterate through all Turnstiles and cancel the event if the Block is the Turnstile gate
        for (Turnstile turnstile : TurnstileMain.getTurnstiles()) {
            if (turnstile.isBlock(block)) {
                event.setNewCurrent(event.getOldCurrent());
            }
        }
    }

    /**
     * Does not allow Blocks that are linked to Turnstiles to be broken
     *
     * @param event The BlockBreakEvent that occurred
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();

        switch (block.getType()) {
        case WOOD_DOOR:
        case WOODEN_DOOR:
        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case TRAP_DOOR:
        case FENCE_GATE:
        case FENCE: //Cancel the Event if the Block is linked to a Turnstile
            for (Turnstile turnstile: TurnstileMain.getTurnstiles()) {
                if (turnstile.isBlock(block)) {
                    //Cancel the Event if the Player is not the Owner
                    if (player == null || !turnstile.isOwner(player)) {
                        event.setCancelled(true);
                    }
                    return;
                }
            }
            break;

        case CHEST:
        case STONE_PLATE:
        case WOOD_PLATE:
        case STONE_BUTTON: //Do not allow Turnstile Blocks to be broken
            //Return if the Block is not linked to a Turnstile
            Turnstile turnstile = TurnstileMain.findTurnstile(block);
            if (turnstile == null) {
                return;
            }

            //Cancel the Event if the Player is not the Owner
            if (player == null || !turnstile.isOwner(player)) {
                event.setCancelled(true);
            }
            break;

        case SIGN:
        case SIGN_POST:
        case WALL_SIGN: //Unlink Signs if they are broken
            Sign sign = (Sign) event.getBlock().getState();
            for (int i = 0; i <= 2; i = i + 2) {
                String line = sign.getLine(i);

                switch (line) {
                case "Currently:":
                    for (TurnstileSign tsSign: TurnstileMain.statusSigns.keySet()) {
                        if (tsSign.sign.equals(sign)) {
                            BukkitTask task = TurnstileMain.statusSigns.get(tsSign);
                            if (task != null) {
                                task.cancel();
                            }
                            TurnstileMain.statusSigns.remove(tsSign);
                        }
                    }
                    break;
                case "Player Count:":
                    Iterator<TurnstileSign> counterSignsItr = TurnstileMain.counterSigns.iterator();
                    while (counterSignsItr.hasNext()) {
                        if (counterSignsItr.next().sign.equals(sign)) {
                            counterSignsItr.remove();
                        }
                    }
                    break;
                case "Money Earned:":
                    Iterator<TurnstileSign> moneySignsItr = TurnstileMain.moneySigns.iterator();
                    while (moneySignsItr.hasNext()) {
                        if (moneySignsItr.next().sign.equals(sign)) {
                            moneySignsItr.remove();
                        }
                    }   break;
                case "Items Earned:":
                    Iterator<TurnstileSign> itemSignsItr = TurnstileMain.itemSigns.iterator();
                    while (itemSignsItr.hasNext()) {
                        if (itemSignsItr.next().sign.equals(sign)) {
                            itemSignsItr.remove();
                        }
                    }
                    break;
                default:
                    return;
                }
            }

            TurnstileMain.saveSigns();

        default:
            break;
        }
    }

    /**
     * Listens for Players placing Turnstile Signs
     *
     * @param event The SignChangeEvent that occurred
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        //Check if this is a Turnstile sign to be linked
        String line = event.getLine(0).toLowerCase();
        if (!line.equals("ts link")) {
            return;
        }

        //Cancel if the Player does not have permission to create Turnstile Signs
        Player player = event.getPlayer();
        if (!player.hasPermission("sign")) {
            player.sendMessage(TurnstileMessages.permission);
            return;
        }

        //Find the Turnstile that the Sign will be linked to
        line = event.getLine(1);
        Turnstile turnstile = TurnstileMain.findTurnstile(line);
        if (turnstile == null) {
            player.sendMessage("Turnstile " + line + " does not exsist.");
            return;
        }

        handleType(event, turnstile, FIRST_TYPE_LOCATION);
        handleType(event, turnstile, SECOND_TYPE_LOCATION);

        TurnstileMain.saveSigns();
    }

    private static void handleType(SignChangeEvent event, Turnstile turnstile, int lineNumber) {
        Sign sign = (Sign) event.getBlock().getState();
        String line = event.getLine(lineNumber).toUpperCase();
        lineNumber = lineNumber == FIRST_TYPE_LOCATION ? 0 : 2; //2 -> {0, 1} | 3 -> {2, 3}

        switch (line) {
        case "NAME":
            event.setLine(lineNumber, "Turnstile:");
            event.setLine(lineNumber + 1, trim(turnstile.name));
            break;

        case "PRICE":
            event.setLine(lineNumber, "Price:");
            event.setLine(lineNumber + 1, trim(Econ.format(turnstile.price)));
            break;

        case "COST":
            event.setLine(lineNumber, "Cost:");
            if (turnstile.items.size() > 0) {
                String name = Turnstile.getItemName(turnstile.items.get(0));
                event.setLine(lineNumber + 1, trim(name));
            }
            break;

        case "COUNTER":
            event.setLine(lineNumber, "Player Count:");
            event.setLine(lineNumber + 1, "0");

            TurnstileMain.counterSigns.add(new TurnstileSign(sign, turnstile, 3));
            break;

        case "MONEY":
            event.setLine(lineNumber, "Money Earned:");
            event.setLine(lineNumber + 1, trim(Econ.format(turnstile.moneyEarned)));

            TurnstileMain.moneySigns.add(new TurnstileSign(sign, turnstile, 3));
            TurnstileMain.saveSigns();
            break;

        case "ITEMS":
            event.setLine(lineNumber, "Items Earned:");
            event.setLine(lineNumber + 1, String.valueOf(turnstile.itemsEarned));

            TurnstileMain.itemSigns.add(new TurnstileSign(sign, turnstile, 3));
            TurnstileMain.saveSigns();
            break;

        case "ACCESS":
            event.setLine(lineNumber, "Access:");

            if (turnstile.access == null) {
                event.setLine(lineNumber + 1, "public");
            } else if (turnstile.access.isEmpty()) {
                event.setLine(lineNumber + 1, "private");
            } else {
                String access = turnstile.access.split(" ")[0];
                event.setLine(lineNumber + 1, trim(access));
            }

            break;

        case "STATUS":
            event.setLine(lineNumber, "Currently:");
            event.setLine(lineNumber + 1, "open");

            //Start the tickListener for the Sign
            TurnstileSign tsSign = new TurnstileSign(sign, turnstile, 3);
            TurnstileMain.statusSigns.put(tsSign, tsSign.tickListenerTask());
            break;

        case "":
            event.setLine(lineNumber, "");
            event.setLine(lineNumber + 1, "");
            break;

        default:
            event.getPlayer().sendMessage(line + " is not a valid type. Valid types: "
                    + "NAME, PRICE, COST, COUNTER, MONEY, ITEMS,"
                    + " ACCESS, STATUS");
            break;
        }
    }

    private static String trim(String line) {
        return line.length() > SIGN_MAX_LENGTH ? line.substring(0, SIGN_MAX_LENGTH - 1) : line;
    }
}
