
package Turnstile;

import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author Codisimus
 */
public class TurnstilePlayerListener extends PlayerListener{
    protected static String permission;
    protected static String locked;
    protected static String free;
    protected static String oneWay;
    protected static LinkedList<Turnstile> openTurnstiles = new LinkedList<Turnstile>();

    @Override
    public void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String[] split = event.getMessage().split(" ");
        if (split[0].equals("/turnstile") || split[0].equals("/ts")) {
            event.setCancelled(true);
            try {
                Block block = player.getTargetBlock(null, 100);
                Material mat = block.getType();
                if (split[1].equals("make")) {
                    if (!TurnstileMain.hasPermission(player, "make")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (SaveSystem.findTurnstile(split[2]) != null) {
                        player.sendMessage("A Turnstile named "+split[2]+" already exists.");
                        return;
                    }
                    if (!mat.equals(Material.FENCE) && !TurnstileMain.isDoor(mat)) {
                        player.sendMessage("You must target a door or fence block.");
                        return;
                    }
                    int price = TurnstileMain.cost;
                    if (price > 0 && (!TurnstileMain.useMakeFreeNode || !TurnstileMain.hasPermission(player, "makefree"))) {
                        if (!Register.charge(player.getName(), null, price)) {
                            player.sendMessage("You do not have enough money to make the turnstile");
                            return;
                        }
                        else
                            player.sendMessage(price+" deducted,");
                    }
                    Turnstile turnstile = new Turnstile(split[2], player);
                    player.sendMessage("Turnstile "+split[2]+" made!");
                    SaveSystem.addTurnstile(turnstile);
                }
                else if (split[1].equals("link")) {
                    if (!TurnstileMain.hasPermission(player, "make")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (!TurnstileMain.isSwitch(mat)) {
                        player.sendMessage("You must link the turnstile to a button, chest, or pressure plate.");
                        return;
                    }
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        turnstile.buttons.add(block);
                        player.sendMessage("Succesfully linked to turnstile "+split[2]+"!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("price")) {
                    if (!TurnstileMain.hasPermission(player, "set.price")) {
                        player.sendMessage(permission);
                        return;
                    }
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        if (split.length == 5) {
                            turnstile.item = split[3];
                            turnstile.amount = Integer.parseInt(split[4]);
                            turnstile.earned = 0;
                            player.sendMessage("Price of turnstile "+split[2]+" has been set to "+split[4]+" of ItemID "+split[3]+"!");
                        }
                        else {
                            player.sendMessage("Price of turnstile "+split[2]+" has been set to "+split[3]+"!");
                            if (split[3].equalsIgnoreCase("all"))
                                split[3] = "-411";
                            turnstile.price = Double.parseDouble(split[3]);
                        }
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("owner")) {
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (!TurnstileMain.hasPermission(player, "set.owner")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        turnstile.owner = split[3];
                        player.sendMessage("Money from turnstile "+split[2]+" will go to "+split[3]+"!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("access")) {
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (!TurnstileMain.hasPermission(player, "set.access")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        turnstile.access = split[3];
                        player.sendMessage("Access to turnstile "+split[2]+" has been set to "+split[3]+"!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("bank")) {
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (!TurnstileMain.hasPermission(player, "set.bank")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        turnstile.owner = "bank:"+split[3];
                        player.sendMessage("Money from turnstile "+split[2]+" will go to "+split[3]+"!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("unlink")) {
                    if (!TurnstileMain.hasPermission(player, "make")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (!TurnstileMain.isSwitch(mat)) {
                        player.sendMessage("You must target the button, chest, or pressure plate you wish to unlink");
                        return;
                    }
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        turnstile.buttons.remove(block);
                        player.sendMessage("Sucessfully unlinked!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("delete")) {
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null ) {
                        player.sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (!TurnstileMain.hasPermission(player, "make")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (!turnstile.isOwner(player)) {
                        player.sendMessage("Only the Turnstile owner can do that.");
                        return;
                    }
                    SaveSystem.removeTurnstile(turnstile);
                    player.sendMessage("Turnstile "+split[2]+" was deleted!");
                    SaveSystem.save();
                }
                else if (split[1].equals("free")) {
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (!TurnstileMain.hasPermission(player, "set.free")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        String[] time = split[3].split("-");
                        turnstile.freeStart = Long.parseLong(time[0]);
                        turnstile.freeEnd = Long.parseLong(time[1]);
                        player.sendMessage("Turnstile "+split[2]+" is free to use from "+time[0]+" to "+time[1]+"!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("locked")) {
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    if (!TurnstileMain.hasPermission(player, "set.locked")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (turnstile.isOwner(player)) {
                        String[] time = split[3].split("-");
                        turnstile.lockedStart = Long.parseLong(time[0]);
                        turnstile.lockedEnd = Long.parseLong(time[1]);
                        player.sendMessage("Turnstile "+split[2]+" is locked from "+time[0]+" to "+time[1]+"!");
                        SaveSystem.save();
                    }
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("earned")) {
                    if (!TurnstileMain.hasPermission(player, "earned")) {
                        player.sendMessage(permission);
                        return;
                    }
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null) {
                        event.getPlayer().sendMessage("Turnstile "+split[2]+" does not exsist.");
                        return;
                    }
                    if (turnstile.isOwner(player))
                        player.sendMessage("Turnstile "+split[2]+" has earned "+turnstile.earned+"!");
                    else
                        player.sendMessage("Only the Turnstile owner can do that.");
                }
                else if (split[1].equals("collect")) {
                    if (!TurnstileMain.hasPermission(player, "collect")) {
                        player.sendMessage(permission);
                        return;
                    }
                    if (mat.equals(Material.CHEST)) {
                        LinkedList<Turnstile> turnstiles = SaveSystem.getTurnstiles();
                        for (Turnstile turnstile : turnstiles) {
                            LinkedList<Block> buttons = turnstile.buttons;
                            for (Block button : buttons )
                                if (button.getLocation().equals(block.getLocation())) {
                                    if (turnstile.isOwner(player))
                                        turnstile.collect(player);
                                    else
                                        player.sendMessage("Only the Turnstile owner can do that.");
                                    return;
                                }
                        }
                    }
                    player.sendMessage("You must target the turnstile chest you wish to collect from.");
                }
                else if (split[1].equals("list")) {
                    if (!TurnstileMain.hasPermission(player, "list")) {
                        player.sendMessage(permission);
                        return;
                    }
                    LinkedList<Turnstile> tempList = SaveSystem.getTurnstiles();
                    StringBuilder sBuilder = new StringBuilder();
                    if (tempList.isEmpty())
                        sBuilder.append("No Turnstiles");
                    else {
                        sBuilder.append("Current turnstiles:");
                        for (Turnstile turnstile : tempList) {
                            sBuilder.append(turnstile.name);
                            sBuilder.append(" ");
                        }
                    }
                    player.sendMessage(sBuilder.toString());
                }
                else if (split[1].equals("info")) {
                    if (!TurnstileMain.hasPermission(player, "info")) {
                        player.sendMessage(permission);
                        return;
                    }
                    Turnstile turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null ) {
                        player.sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    player.sendMessage("Turnstile Info");
                    player.sendMessage("Name: "+turnstile.name);
                    player.sendMessage("Price: "+turnstile.price);
                    player.sendMessage("Earned: "+turnstile.earned);
                    player.sendMessage("Owner: "+turnstile.owner);
                    player.sendMessage("Location: "+turnstile.gate.getLocation().toString());
                }
                else if (split[1].equals("rename")) {
                    if (!TurnstileMain.hasPermission(player, "make")) {
                        player.sendMessage(permission);
                        return;
                    }
                    Turnstile turnstile = SaveSystem.findTurnstile(split[3]);
                    if (turnstile != null ) {
                        player.sendMessage("Turnstile "+split[3]+" already exists.");
                        return;
                    }
                    turnstile = SaveSystem.findTurnstile(split[2]);
                    if (turnstile == null ) {
                        player.sendMessage("Turnstile "+split[2]+" does not exist.");
                        return;
                    }
                    turnstile.name = split[3];
                    player.sendMessage("Turnstile "+split[2]+" renamed to "+split[3]+".");
                }
                else if (split[1].equals("help"))
                    throw new Exception();
            }
            catch (Exception e) {
                player.sendMessage("§e     Turnstile Help Page:");
                player.sendMessage("§2/ts make [Name]§b Make target fence block into a Turnstile");
                player.sendMessage("§2/ts link [Name]§b Link target block with Turnstile");
                player.sendMessage("§2/ts rename [Name] [NewName]§b rename a Turnstile");
                player.sendMessage("§2/ts unlink [Name]§b Unlink target block with Turnstile");
                player.sendMessage("§2/ts delete [Name]§b Delete Turnstile and unlink blocks");
                player.sendMessage("§2/ts price [Name] [Price]§b Set cost of Turnstile");
                player.sendMessage("§2/ts price [Name] [ItemID(.Damage)] [Amount]§b Set cost to item");
                player.sendMessage("§2/ts access [Name] public§b Allow anyone to open the Turnstile");
                player.sendMessage("§2/ts access [Name] private§b Allow only the owner to open");
                player.sendMessage("§2/ts access [Name] [Group]§b Allow only specific Group to open");
                player.sendMessage("§2/ts free [Name] [TimeStart-TimeEnd]§b Free during timespan");
                player.sendMessage("§2/ts locked [Name] [TimeStart-TimeEnd]§b Locked during timespan");
                player.sendMessage("§2/ts earned [Name]§b Display money the Turnstile earned");
                player.sendMessage("§2/ts collect§b Retrieve items from the target Turnstile chest");
                player.sendMessage("§2/ts owner [Name] [Player]§b Send money for Turnstile to Player");
                player.sendMessage("§2/ts bank [Name] [Bank]§b Send money for Turnstile to Bank");
                player.sendMessage("§2/ts list§b List all Turnstiles");
                player.sendMessage("§2/ts info§b Gives info of Turnstile");
            }
        }
        else
            return;
    }

    @Override
    public void onPlayerInteract (PlayerInteractEvent event) {
        //Return if Action was arm flailing
        if (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;
        
        Block block = event.getClickedBlock();
        Material mat = block.getType();
        Player player = event.getPlayer();
        if (TurnstileMain.isSwitch(mat)) {
            Turnstile turnstile = SaveSystem.findTurnstile(block);
            
            //Return if switch is not linked to a Turnstile
            if (turnstile == null)
                return;
            
            //Return if Turnstile Is already open
            if (turnstile.open)
                return;
            
            //Return if Player does not have permission to open Turnstiles
            if (!TurnstileMain.hasPermission(player, "open")) {
                player.sendMessage(permission);
                return;
            }
            
            //Return if Player does not have access rights to Turnstile
            if (!turnstile.hasAccess(player))
                return;
            
            //Return if Turnstile is locked
            if (turnstile.isLocked(player.getWorld().getTime())) {
                player.sendMessage(locked);
                return;
            }
            
            //Return without charging if Turnstile is free
            if (turnstile.isFree(player.getWorld().getTime())) {
                player.sendMessage(free);
                turnstile.open(block);
                return;
            }
            
            if (block.getType().equals(Material.CHEST)) {
                turnstile.checkContents((Chest)block.getState(), player);
                return;
            }
            
            if (TurnstileMain.noFraud) {
                turnstile.open(block);
                if (turnstile.price == 0)
                    player.sendMessage(free);
                else
                    player.sendMessage(TurnstileMain.displayCost.replaceAll("<price>", ""+turnstile.price));
            }
            else if (turnstile.checkBalance(player))
                turnstile.open(block);
        }
        else if (TurnstileMain.isDoor(block.getType()))
            for (Turnstile turnstile : SaveSystem.getTurnstiles()) {
                Block gate = turnstile.gate;
                if (gate.getLocation().equals(block.getLocation()) || TurnstileMain.areNeighbors(gate, block))
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
        for (Turnstile turnstile : openTurnstiles) {
            //Return if the Player did not step into this Turnstile's gate
            if (to != turnstile.gate)
                return;
            
            Location from = event.getFrom();
            Player player = event.getPlayer();
            
            //Teleport Player back where they came from if they enter Turnstile backwards
            if (Turnstile.oneWay && !turnstile.checkOneWay(from.getBlock())) {
                from.setX(from.getBlockX()+.5);
                from.setY(from.getBlockY());
                from.setZ(from.getBlockZ()+.5);
                player.teleport(from);
                player.sendMessage(oneWay);
                return;
            }
            
            turnstile.close();
            
            //If NoFraud mode is off, Player already payed (so Return without recharging)
            if (!TurnstileMain.noFraud)
                return;
            
            //Return without charging if Turnstile is currently free
            if (turnstile.isFree(player.getWorld().getTime()))
                return;
            
            //Teleport Player back where they came from if they could not pay
            if (!turnstile.checkBalance(player)) {
                from.setX(from.getBlockX()+.5);
                from.setY(from.getBlockY());
                from.setZ(from.getBlockZ()+.5);
                player.teleport(from);
            }
        }
    }
}

