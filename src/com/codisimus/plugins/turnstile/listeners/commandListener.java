package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.Econ;
import com.codisimus.plugins.turnstile.SaveSystem;
import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileButton;
import com.codisimus.plugins.turnstile.TurnstileMain;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class CommandListener implements CommandExecutor {
    public static enum Action {
        HELP, MAKE, LINK, PRICE, OWNER, ACCESS, BANK, UNLINK,
        DELETE, FREE, LOCKED, NOFRAUD, COLLECT, LIST, INFO, RENAME
    }
    public static final HashSet MAKE_TRANSPARENT = Sets.newHashSet((byte)0, (byte)6,
            (byte)8, (byte)9, (byte)10, (byte)11, (byte)26, (byte)27, (byte)28,
            (byte)30, (byte)31, (byte)32, (byte)37, (byte)38, (byte)39, (byte)40,
            (byte)44, (byte)50, (byte)51, (byte)53, (byte)55, (byte)59, (byte)65,
            (byte)66, (byte)67, (byte)69, (byte)70, (byte)72, (byte)75, (byte)76,
            (byte)77, (byte)78, (byte)90, (byte)92, (byte)101, (byte)102, (byte)104,
            (byte)105, (byte)106, (byte)108, (byte)109, (byte)111, (byte)114,
            (byte)115, (byte)117);
    public static final HashSet LINK_TRANSPARENT = Sets.newHashSet((byte)0, (byte)6,
            (byte)8, (byte)9, (byte)10, (byte)11, (byte)26, (byte)27, (byte)28,
            (byte)30, (byte)31, (byte)32, (byte)37, (byte)38, (byte)39, (byte)40,
            (byte)44, (byte)50, (byte)51, (byte)53, (byte)55, (byte)59, (byte)64,
            (byte)65, (byte)66, (byte)67, (byte)71, (byte)75, (byte)76, (byte)78,
            (byte)85, (byte)90, (byte)92, (byte)96, (byte)101, (byte)102, (byte)104,
            (byte)105, (byte)106, (byte)107, (byte)108, (byte)109, (byte)111,
            (byte)113, (byte)114, (byte)115, (byte)117);
    public static final HashSet TRANSPARENT = Sets.newHashSet((byte)0, (byte)6,
            (byte)8, (byte)9, (byte)10, (byte)11, (byte)26, (byte)27, (byte)28,
            (byte)30, (byte)31, (byte)32, (byte)37, (byte)38, (byte)39, (byte)40,
            (byte)44, (byte)50, (byte)51, (byte)53, (byte)55, (byte)59, (byte)65,
            (byte)66, (byte)67, (byte)75, (byte)76, (byte)78, (byte)90, (byte)92,
            (byte)101, (byte)102, (byte)104, (byte)105, (byte)106, (byte)108,
            (byte)109, (byte)111, (byte)114, (byte)115, (byte)117);
    public static String permissionMsg;
    
    /**
     * Listens for Turnstile commands to execute them
     * 
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player))
            return true;
        
        Player player = (Player)sender;
        
        //Display the help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        Action action;
        
        try {
            action = Action.valueOf(args[0].toUpperCase());
        }
        catch (Exception notEnum) {
            sendHelp(player);
            return true;
        }
        
        //Execute the correct command
        switch (action) {
            case MAKE:
                if (args.length == 2)
                    make(player, args[1]);
                else
                    sendHelp(player);
                return true;
                
            case LINK:
                if (args.length == 2)
                    link(player, args[1]);
                else
                    sendHelp(player);
                return true;
                
            case PRICE:
                switch (args.length) {
                    case 2: price(player, null, Double.parseDouble(args[1])); return true;
                        
                    case 3:
                        if (args[1].equals("all"))
                            price(player, args[1], -411);
                        else
                            price(player, args[1], Double.parseDouble(args[2]));
                        return true;
                        
                    case 4:
                        try {
                            price(player, null, Integer.parseInt(args[1]), args[2], Short.parseShort(args[3]));
                        }
                        catch (Exception notInt) {
                            price(player, args[1], Integer.parseInt(args[2]), args[3], (short)-1);
                        }
                        
                        return true;
                        
                    case 5: price(player, args[1], Integer.parseInt(args[2]),
                            args[3], Short.parseShort(args[4])); return true;
                        
                    default: sendHelp(player); return true;
                }
                
            case NOFRAUD:
                try {
                    switch (args.length) {
                        case 2: noFraud(player, null, Boolean.parseBoolean(args[1])); return true;
                        case 3: noFraud(player, args[1], Boolean.parseBoolean(args[2])); return true;
                        default: break;
                    }
                }
                catch (Exception notInt) {
                }
                
                sendHelp(player);
                return true;
                
            case OWNER:
                switch (args.length) {
                    case 2: owner(player, null, args[1]); return true;
                    case 3: owner(player, args[1], args[2]); return true;
                    default: sendHelp(player); return true;
                }
                
            case ACCESS:
                switch (args.length) {
                    case 2: access(player, null, args[1]); return true;
                    case 3: access(player, args[1], args[2]); return true;
                    default: sendHelp(player); return true;
                }
                
            case BANK:
                switch (args.length) {
                    case 2: bank(player, null, args[1]); return true;
                    case 3: bank(player, args[1], args[2]); return true;
                    default: sendHelp(player); return true;
                }
                
            case UNLINK:
                if (args.length == 1)
                    unlink(player);
                else
                    sendHelp(player);
                return true;
                
            case DELETE:
                switch (args.length) {
                    case 1: delete(player, null); return true;
                    case 2: delete(player, args[1]); return true;
                    default: sendHelp(player); return true;
                }
                
            case FREE:
                switch (args.length) {
                    case 2: free(player, null, args[1]); return true;
                    case 3: free(player, args[1], args[2]); return true;
                    default: sendHelp(player); return true;
                }
                
            case LOCKED:
                switch (args.length) {
                    case 2: locked(player, null, args[1]); return true;
                    case 3: locked(player, args[1], args[2]); return true;
                    default: sendHelp(player); return true;
                }
                
            case COLLECT:
                if (args.length == 1)
                    collect(player);
                else
                    sendHelp(player);
                return true;
                
            case LIST:
                if (args.length == 1)
                    list(player);
                else
                    sendHelp(player);
                return true;
                
            case INFO:
                switch (args.length) {
                    case 1: info(player, null); return true;
                    case 2: info(player, args[1]); return true;
                    default: sendHelp(player); return true;
                }
                
            case RENAME:
                switch (args.length) {
                    case 2: rename(player, null, args[1]); return true;
                    case 3: rename(player, args[1], args[2]); return true;
                    default: sendHelp(player); return true;
                }
                
            default: sendHelp(player); return true;
        }
    }
    
    /**
     * Creates a new Turnstile of the given name
     * 
     * @param player The Player creating the Turnstile
     * @param name The name of the Turnstile being created (must not already exist)
     */
    public static void make(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        //Cancel if the Turnstile already exists
        if (SaveSystem.findTurnstile(name) != null) {
            player.sendMessage("A Turnstile named "+name+" already exists.");
            return;
        }
        
        Block block = player.getTargetBlock(MAKE_TRANSPARENT, 10);
        switch (block.getType()) {
            case FENCE: break;
                
            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Fall through
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK: //Make sure the bottom half of the Door is used
                if (((Door)block.getState().getData()).isTopHalf())
                    block = block.getRelative(BlockFace.DOWN);
                
                break;
                
            default: //Cancel because an invalid Block type is targeted
                player.sendMessage("You must target a Door or Fence.");
                return;
        }
        
        //Do not charge cost to make a Turnstile is 0 or Player has the 'turnstile.makefree' node
        int price = TurnstileMain.cost;
        if (price > 0 && (!TurnstileMain.useMakeFreeNode || !TurnstileMain.hasPermission(player, "makefree"))) {
            //Cancel if the Player could not afford it
            if (!Econ.charge(player.getName(), null, price)) {
                player.sendMessage("You do not have enough money to make the Turnstile");
                return;
            }
            
            player.sendMessage(price+" deducted,");
        }
        
        Turnstile turnstile = new Turnstile(name, player.getName(), block);
        player.sendMessage("Turnstile "+name+" made!");
        SaveSystem.turnstiles.add(turnstile);
        SaveSystem.save();
    }
    
    /**
     * Links the target Block to the specified Turnstile
     * 
     * @param player The Player linking the Block they are targeting
     * @param name The name of the Turnstile the Block will be linked to
     */
    public static void link(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Block block = player.getTargetBlock(LINK_TRANSPARENT, 10);
        
        //Cancel if a correct block type is not targeted
        switch (block.getType()) {
            case CHEST: //Fall through
            case STONE_PLATE: //Fall through
            case WOOD_PLATE: //Fall through
            case STONE_BUTTON: break;
            
            default:
                player.sendMessage("You must link the Turnstile to a Button, Chest, or Pressure plate.");
                return;
        }
        
        Turnstile turnstile = SaveSystem.findTurnstile(name);
        
        //Cancel if the Turnstile does not exist
        if (turnstile == null) {
            player.sendMessage("Turnstile "+name+" does not exist.");
            return;
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        turnstile.buttons.add(new TurnstileButton(block));
        player.sendMessage("Succesfully linked to Turnstile "+name+"!");
        SaveSystem.save();
    }
    
    /**
     * Sets the price of the specified Turnstile to an item
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player setting the price
     * @param name The name of the Turnstile being modified
     * @param amount The amount/stack size of the item
     * @param item The id or String of the item Material
     * @param durability The Durability of the item
     */
    public static void price(Player player, String name, int amount, String item, short durability) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.price")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        Material type = Material.getMaterial(item);
        
        if (type == null)
            turnstile.item = Integer.parseInt(item);
        else
            turnstile.item = type.getId();
        
        turnstile.amount = amount;
        turnstile.durability = durability;
        turnstile.itemsEarned = 0;
        
        player.sendMessage("Price of Turnstile "+turnstile.name+" has been set to "
                +amount+" of "+Material.getMaterial(turnstile.item).name()+"!");
        SaveSystem.save();
    }
    
    /**
     * Sets the price of the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player setting the price
     * @param name The name of the Turnstile being modified
     * @param price The new price
     */
    public static void price(Player player, String name, double price) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.price")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        //Set price to money amount or -411 to take all the Players money
        turnstile.price = price;
        player.sendMessage("Price of Turnstile "+turnstile.name+" has been set to "+price+"!");

        SaveSystem.save();
    }
    
    /**
     * Sets the time range that the specified Turnstile is locked
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player modifying the Turnstile
     * @param name The name of the Turnstile being modified
     * @param bool True if the Turnstile will be set to noFraud mode
     */
    public static void noFraud(Player player, String name, boolean bool) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.nofraud")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        //Toggle whether the Safe is lockable
        if (turnstile.noFraud)
            if (bool)
                player.sendMessage("Turnstile "+turnstile.name+" is already in NoFraud mode.");
            else
                player.sendMessage("Turnstile "+turnstile.name+" is no longer set to NoFraud mode.");
        else
            if (bool)
                player.sendMessage("Turnstile "+turnstile.name+" set to NoFraud mode.");
            else
                player.sendMessage("Turnstile "+turnstile.name+" is not set to NoFraud mode.");
        
        turnstile.noFraud = bool;
        SaveSystem.save();
    }
    
    /**
     * Changes the Owner of the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player changing the Owner
     * @param name The name of the Turnstile being modified
     * @param owner The new Owner
     */
    public static void owner(Player player, String name, String owner) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.owner")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        turnstile.owner = owner;
        player.sendMessage("Money from Turnstile "+turnstile.name+" will go to "+owner+"!");
        SaveSystem.save();
    }
    
    /**
     * Modifies the access value of the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player modifying the Turnstile
     * @param name The name of the Turnstile being modified
     * @param access The new access value
     */
    public static void access(Player player, String name, String access) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.access")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        turnstile.access = new LinkedList<String>();
        if (access.contains(","))
            turnstile.access.addAll(Arrays.asList(access.split(",")));
        else
            turnstile.access.add(access);
        
        player.sendMessage("Access to Turnstile "+turnstile.name+" has been set to "+access+"!");
        SaveSystem.save();
    }
    
    /**
     * Modifies the bank value of the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player modifying the Turnstile
     * @param name The name of the Turnstile being modified
     * @param bank The new bank value
     */
    public static void bank(Player player, String name, String bank) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.bank")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        turnstile.owner = "bank:"+bank;
        player.sendMessage("Money from Turnstile "+turnstile.name+" will go to "+bank+"!");
        SaveSystem.save();
    }
    
    /**
     * Unlinks the target Block from the specified Turnstile
     * 
     * @param player The Player unlinking the Block they are targeting
     */
    public static void unlink(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Block block = player.getTargetBlock(LINK_TRANSPARENT, 10);
        
        //Cancel if a correct block type is not targeted
        switch (block.getType()) {
            case CHEST: //Fall through
            case STONE_PLATE: //Fall through
            case WOOD_PLATE: //Fall through
            case STONE_BUTTON: break;
            
            default:
                player.sendMessage("You must link the Turnstile to a Button, Chest, or Pressure plate.");
                return;
        }
        
        //Cancel if the Turnstile does not exist
        Turnstile turnstile = SaveSystem.findTurnstile(block);
        if (turnstile == null) {
            player.sendMessage("Target Block is not linked to a Turnstile.");
            return;
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        Iterator itr = turnstile.buttons.iterator();
        while (itr.hasNext()) {
            TurnstileButton button = (TurnstileButton)itr.next();
            if (block.getX() == button.x && block.getY() == button.y &&
            block.getZ() == button.z && block.getWorld().getName().equals(button.world))
                itr.remove();
        }
        
        player.sendMessage("Sucessfully unlinked!");
        SaveSystem.save();
    }
    
    /**
     * Deletes the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is deleted
     * 
     * @param player The Player deleting the Turnstile
     * @param name The name of the Turnstile to be deleted
     */
    public static void delete(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        SaveSystem.turnstiles.remove(turnstile);
        File trash = new File("plugins/Turnstile/"+turnstile.name+".dat");
        trash.delete();
        player.sendMessage("Turnstile "+turnstile.name+" was deleted!");
        SaveSystem.save();
    }
    
    /**
     * Sets the time range that the specified Turnstile is free
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player modifying the Turnstile
     * @param name The name of the Turnstile being modified
     * @param range The given time range
     */
    public static void free(Player player, String name, String range) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.free")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        //Split range into start time and end time
        String[] time = range.split("-");
        turnstile.freeStart = Long.parseLong(time[0]);
        turnstile.freeEnd = Long.parseLong(time[1]);
        player.sendMessage("Turnstile "+turnstile.name+" is free to use from "+time[0]+" to "+time[1]+"!");
        SaveSystem.save();
    }
    
    /**
     * Sets the time range that the specified Turnstile is locked
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player modifying the Turnstile
     * @param name The name of the Turnstile being modified
     * @param range The given time range
     */
    public static void locked(Player player, String name, String range) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.locked")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        //Split range into start time and end time
        String[] time = range.split("-");
        turnstile.lockedStart = Long.parseLong(time[0]);
        turnstile.lockedEnd = Long.parseLong(time[1]);
        player.sendMessage("Turnstile "+turnstile.name+" is locked from "+time[0]+" to "+time[1]+"!");
        SaveSystem.save();
    }
    
    /**
     * Allows the Owner to collect items from the target Chest
     * 
     * @param player The Player collecting the items
     */
    public static void collect(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "collect")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Block block = player.getTargetBlock(LINK_TRANSPARENT, 10);
        
        //Cancel if a Chest is not targeted
        if (block.getTypeId() != 54) {
            player.sendMessage("You must target the Turnstile Chest you wish to collect from.");
            return;
        }
        
        Turnstile turnstile = SaveSystem.findTurnstile(block);
        
        //Cancel if the Chest is not linked to a Turnstile
        if (turnstile == null) {
            player.sendMessage("You must target the Turnstile Chest you wish to collect from.");
            return;
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        turnstile.collect(player);
        SaveSystem.save();
    }
    
    /**
     * Displays a list of current Turnstiles
     * 
     * @param player The Player requesting the list
     */
    public static void list(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "list")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        String list = "Current Turnstiles:  ";
        
        for (Turnstile turnstile: SaveSystem.turnstiles)
            list = list.concat(turnstile.name+", ");
        
        player.sendMessage(list.substring(0, list.length()-2));
    }
    
    /**
     * Displays the info of the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is used
     * 
     * @param player The Player requesting the info
     * @param name The name of the Turnstile
     */
    public static void info(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "collect")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = null;
        
        if (name == null) {
            //Find the Turnstile that will be modified using the target Block
            turnstile = SaveSystem.findTurnstile(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the Turnstile does not exist
            if (turnstile == null ) {
                player.sendMessage("Target Block is not linked to a Turnstile");
                return;
            }
        }
        else {
            //Find the Turnstile that will be modified using the given name
            turnstile = SaveSystem.findTurnstile(name);
            
            //Cancel if the Warp does not exist
            if (turnstile == null ) {
                player.sendMessage("Turnstile "+name+" does not exsist.");
                return;
            }
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return;
        }
        
        player.sendMessage("Turnstile Info");
        player.sendMessage("Name: "+turnstile.name);
        player.sendMessage("Owner: "+turnstile.owner);
        player.sendMessage("Location: "+turnstile.world+"'"+turnstile.x+"'"+turnstile.y+"'"+turnstile.z);
        player.sendMessage("Item: "+turnstile.amount+" of "+turnstile.item+" with durability of "+turnstile.durability);
        
        //Only display if an Economy plugin is present
        if (Econ.economy != null) {
            player.sendMessage("Price: "+Econ.format(turnstile.price)+", NoFraud: "+turnstile.noFraud);
            player.sendMessage("MoneyEarned: "+turnstile.moneyEarned+", ItemsEarned: "+turnstile.itemsEarned);
            player.sendMessage("Free: "+turnstile.freeStart+"-"+turnstile.freeEnd);
        }
        
        player.sendMessage("Locked: "+turnstile.lockedStart+"-"+turnstile.lockedEnd);
        player.sendMessage("NoFraud: "+turnstile.noFraud);
        player.sendMessage("Access: "+turnstile.access.toString());
        
        String buttons = "Buttons:  ";
        for (TurnstileButton button: turnstile.buttons)
            buttons.concat(button.toString()+", ");
        
        player.sendMessage(buttons.substring(0, buttons.length() - 2));
    }
    
    /**
     * Changes the name of the specified Turnstile
     * If a name is not provided, the Turnstile of the target Block is modified
     * 
     * @param player The Player renaming the Turnstile
     * @param name The name of the Turnstile being renamed
     * @param newName The new name
     */
    public static void rename(Player player, String name, String newName) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = SaveSystem.findTurnstile(newName);
        
        //Cancel if the Turnstile already exists
        if (turnstile == null) {
            player.sendMessage("You must target the turnstile chest you wish to collect from.");
            return;
        }
        
        turnstile = SaveSystem.findTurnstile(name);
        
        //Cancel if the Turnstile does not exist
        if (turnstile == null) {
            player.sendMessage("You must target the turnstile chest you wish to collect from.");
            return;
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.name = newName;
        player.sendMessage("Turnstile "+name+" renamed to "+newName+".");
    }
    
    /**
     * Displays the Turnstile Help Page to the given Player
     *
     * @param player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§e     Turnstile Help Page:");
        player.sendMessage("§2/ts make [Name]§b Make target Block into a Turnstile");
        player.sendMessage("§2/ts link [Name]§b Link target Block with Turnstile");
        player.sendMessage("§2/ts rename (Name) [NewName]§b Rename a Turnstile");
        player.sendMessage("§2/ts unlink §b Unlink target Block with Turnstile");
        player.sendMessage("§2/ts delete (Name)§b Delete Turnstile");
        player.sendMessage("§2/ts price (Name) [Price]§b Set cost of Turnstile");
        player.sendMessage("§2/ts price (Name) [Amount] [Item] (Durability)§b Set cost to item");
        player.sendMessage("§2/ts nofraud (Name) ['true' or 'false']§b Set noFraud mode");
        player.sendMessage("§2/ts access (Name) public§b Allow anyone to open the Turnstile");
        player.sendMessage("§2/ts access (Name) private§b Allow only the Owner to open");
        player.sendMessage("§2/ts access (Name) [Group1,Group2,...]");
        player.sendMessage("§bAllow only specific Groups to open the Turnstile");
        player.sendMessage("§2/ts free (Name) [TimeStart]-[TimeEnd]§b Free during timespan");
        player.sendMessage("§2/ts locked (Name) [TimeStart]-[TimeEnd]§b Locked for timespan");
        player.sendMessage("§2/ts collect§b Retrieve items from the target Turnstile chest");
        player.sendMessage("§2/ts owner (Name) [Player]§b Send money for Turnstile to Player");
        player.sendMessage("§2/ts bank (Name) [Bank]§b Send money for Turnstile to Bank");
        player.sendMessage("§2/ts list§b List all Turnstiles");
        player.sendMessage("§2/ts info (Name)§b Display info of Turnstile");
    }
}
