package com.codisimus.plugins.turnstile.listeners;

import com.codisimus.plugins.turnstile.Register;
import com.codisimus.plugins.turnstile.SaveSystem;
import com.codisimus.plugins.turnstile.Turnstile;
import com.codisimus.plugins.turnstile.TurnstileMain;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class commandListener implements CommandExecutor {
    //public static final HashSet TRANSPARENT = Sets.newHashSet(27, 28, 37, 38, 39, 40, 50, 65, 66, 69, 70, 72, 75, 76, 78);
    public static int cornerID;
    public static String permissionMsg;
    public static String claimedMsg;
    public static String limitMsg;
    public static String unclaimedMsg;
    public static String buyFreeMsg;
    
    /**
     * Listens for ChunkOwn commands to execute them
     *
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player))
            return true;
        
        Player player = (Player)sender;
        
        //Display help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        //Set the ID of the command
        int commandID = 0;
        if (args[0].equals("make"))
            commandID = 1;
        else if (args[0].equals("link"))
            commandID = 2;
        else if (args[0].equals("price"))
            commandID = 3;
        else if (args[0].equals("owner"))
            commandID = 4;
        else if (args[0].equals("access"))
            commandID = 5;
        else if (args[0].equals("bank"))
            commandID = 6;
        else if (args[0].equals("unlink"))
            commandID = 7;
        else if (args[0].equals("delete"))
            commandID = 8;
        else if (args[0].equals("free"))
            commandID = 9;
        else if (args[0].equals("locked"))
            commandID = 10;
        else if (args[0].equals("earned"))
            commandID = 11;
        else if (args[0].equals("collect"))
            commandID = 12;
        else if (args[0].equals("list"))
            commandID = 13;
        else if (args[0].equals("info"))
            commandID = 14;
        else if (args[0].equals("rename"))
            commandID = 15;
        
        //Execute the command
        switch (commandID) {
            case 1:
                if (args.length == 2)
                    make(player, args[1]);
                else
                    sendHelp(player);
                return true;
            case 2:
                if (args.length == 2)
                    link(player, args[1]);
                else
                    sendHelp(player);
                return true;
            case 3:
                switch (args.length) {
                    case 3:
                        if (args[1].equals("all"))
                            price(player, args[1], -411);
                        else
                            price(player, args[1], Double.parseDouble(args[2]));
                        break;
                    case 4:
                        price(player, args[1], Integer.parseInt(args[2]), args[3], (short)-1); break;
                    case 5:
                        price(player, args[1], Integer.parseInt(args[2]), args[3], Short.parseShort(args[4])); break;
                    default: sendHelp(player); break;
                }
                return true;
            case 4:
                if (args.length == 3)
                    owner(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            case 5:
                if (args.length == 3)
                    access(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            case 6:
                if (args.length == 3)
                    bank(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            case 7:
                if (args.length == 2)
                    unlink(player, args[1]);
                else
                    sendHelp(player);
                return true;
            case 8:
                if (args.length == 2)
                    delete(player, args[1]);
                else
                    sendHelp(player);
                return true;
            case 9:
                if (args.length == 3)
                    free(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            case 10:
                if (args.length == 3)
                    locked(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            case 11:
                if (args.length == 3)
                    earned(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            case 12: collect(player); return true;
            case 13:
                if (args.length == 2)
                    list(player, args[1]);
                else
                    sendHelp(player);
                return true;
            case 14:
                if (args.length == 2)
                    info(player, args[1]);
                else
                    sendHelp(player);
                return true;
            case 15:
                if (args.length == 3)
                    rename(player, args[1], args[2]);
                else
                    sendHelp(player);
                return true;
            default: sendHelp(player); return true;
        }
    }
    
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
        
        int id = player.getTargetBlock(null, 10).getTypeId();
        
        //Cancel if a correct block type is not targeted
        if (id != 85 && !TurnstileMain.isDoor(id)) {
            player.sendMessage("You must target a door or fence.");
            return;
        }
        
        int price = TurnstileMain.cost;
        
        //Do not charge cost to make a Turnstile is 0 or Player has the 'turnstile.makefree' node
        if (price > 0 && (!TurnstileMain.useMakeFreeNode || !TurnstileMain.hasPermission(player, "makefree"))) {
            //Cancel if the Player could not afford it
            if (!Register.charge(player.getName(), null, price)) {
                player.sendMessage("You do not have enough money to make the turnstile");
                return;
            }
            
            player.sendMessage(price+" deducted,");
        }
        
        Turnstile turnstile = new Turnstile(name, player);
        player.sendMessage("Turnstile "+name+" made!");
        SaveSystem.turnstiles.add(turnstile);
    }
    
    public static void link(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Block block = player.getTargetBlock(null, 10);
        
        //Cancel if a correct block type is not targeted
        if (!TurnstileMain.isSwitch(block.getTypeId())) {
            player.sendMessage("You must link the turnstile to a button, chest, or pressure plate.");
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.buttons.add(block);
        player.sendMessage("Succesfully linked to turnstile "+name+"!");
        SaveSystem.save();
    }
    
    public static void price(Player player, String name, int amount, String item, short durability) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.price")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        Material type = Material.getMaterial(item);
        if (type == null)
            turnstile.item = Integer.parseInt(item);
        else
            turnstile.item = type.getId();
        turnstile.amount = amount;
        turnstile.durability = durability;
        turnstile.earned = 0;
        player.sendMessage("Price of turnstile "+name+" has been set to "+amount+" of "+Material.getMaterial(turnstile.item).name()+"!");

        SaveSystem.save();
    }
    
    public static void price(Player player, String name, double price) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.price")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        //Set price to money amount or -411 to take all the Players money
        turnstile.price = price;
        turnstile.earned = 0;
        player.sendMessage("Price of turnstile "+name+" has been set to "+price+"!");

        SaveSystem.save();
    }
    
    public static void owner(Player player, String name, String owner) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.owner")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.owner = owner;
        player.sendMessage("Money from turnstile "+name+" will go to "+owner+"!");
        SaveSystem.save();
    }
    
    public static void access(Player player, String name, String access) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.access")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.access = access;
        player.sendMessage("Access to turnstile "+name+" has been set to "+access+"!");
        SaveSystem.save();
    }
    
    /**
     * Removes all the Chunks that are owned by the given Player
     * A Chunk is owned buy a Player if the owner field is the Player's name
     * 
     * @param player The name of the Player
     */
    public static void bank(Player player, String name, String bank) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.bank")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.owner = "bank:"+bank;
        player.sendMessage("Money from turnstile "+name+" will go to "+bank+"!");
        SaveSystem.save();
    }
    
    public static void unlink(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Block block = player.getTargetBlock(null, 10);
        
        //Cancel if a correct block type is not targeted
        if (!TurnstileMain.isSwitch(block.getTypeId())) {
            player.sendMessage("You must link the turnstile to a button, chest, or pressure plate.");
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.buttons.remove(block);
        player.sendMessage("Sucessfully unlinked!");
        SaveSystem.save();
    }
    
    public static void delete(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "make")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        SaveSystem.turnstiles.remove(turnstile);
        player.sendMessage("Turnstile "+name+" was deleted!");
        SaveSystem.save();
    }
    
    public static void free(Player player, String name, String range) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.free")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        //Split range into start time and end time
        String[] time = range.split("-");
        turnstile.freeStart = Long.parseLong(time[0]);
        turnstile.freeEnd = Long.parseLong(time[1]);
        player.sendMessage("Turnstile "+name+" is free to use from "+time[0]+" to "+time[1]+"!");
        SaveSystem.save();
    }
    
    public static void locked(Player player, String name, String range) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.locked")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        //Split range into start time and end time
        String[] time = range.split("-");
        turnstile.lockedStart = Long.parseLong(time[0]);
        turnstile.lockedEnd = Long.parseLong(time[1]);
        player.sendMessage("Turnstile "+name+" is locked from "+time[0]+" to "+time[1]+"!");
        SaveSystem.save();
    }
    
    public static void earned(Player player, String name, String range) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "set.locked")) {
            player.sendMessage(permissionMsg);
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
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        player.sendMessage("Turnstile "+name+" has earned "+turnstile.earned+"!");
    }
    
    public static void collect(Player player) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "collect")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Block block = player.getTargetBlock(null, 10);
        
        //Cancel if a Chest is not targeted
        if (block.getTypeId() != 54) {
            player.sendMessage("You must target the turnstile chest you wish to collect from.");
            return;
        }
        
        Turnstile turnstile = SaveSystem.findTurnstile(block);
        
        //Cancel if the Chest is not linked to a Turnstile
        if (turnstile == null) {
            player.sendMessage("You must target the turnstile chest you wish to collect from.");
            return;
        }
        
        //Cancel if the Player does not own the Turnstile
        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile owner can do that.");
            return;
        }
        
        turnstile.collect(player);
    }
    
    public static void list(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "list")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        String list = "Current turnstiles:  ";
        
        for (Turnstile turnstile : SaveSystem.turnstiles)
            list.concat(turnstile.name+", ");
        
        player.sendMessage(list.substring(0, list.length()-2));
    }
    
    public static void info(Player player, String name) {
        //Cancel if the Player does not have permission to use the command
        if (!TurnstileMain.hasPermission(player, "collect")) {
            player.sendMessage(permissionMsg);
            return;
        }
        
        Turnstile turnstile = SaveSystem.findTurnstile(name);
        
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
        
        player.sendMessage("Turnstile Info");
        player.sendMessage("Name: "+turnstile.name);
        player.sendMessage("Location: "+turnstile.gate.getLocation().toString());
        player.sendMessage("Owner: "+turnstile.owner);
        if (turnstile.item == 0)
            player.sendMessage("Price: "+Register.format(turnstile.price));
        else
            player.sendMessage("Item: "+turnstile.price+" of "+turnstile.item+" with durability of "+turnstile.durability);
        player.sendMessage("Earned: "+turnstile.earned);
        player.sendMessage("Access: "+turnstile.access);
        player.sendMessage("Free: "+turnstile.freeStart+"-"+turnstile.freeEnd);
        player.sendMessage("Locked: "+turnstile.lockedStart+"-"+turnstile.lockedEnd);
    }
    
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
        player.sendMessage("§2/ts make [Name]§b Make target fence block into a Turnstile");
        player.sendMessage("§2/ts link [Name]§b Link target block with Turnstile");
        player.sendMessage("§2/ts rename [Name] [NewName]§b rename a Turnstile");
        player.sendMessage("§2/ts unlink [Name]§b Unlink target block with Turnstile");
        player.sendMessage("§2/ts delete [Name]§b Delete Turnstile and unlink blocks");
        player.sendMessage("§2/ts price [Name] [Price]§b Set cost of Turnstile");
        player.sendMessage("§2/ts price [Name] [Amount] [Item] (Durability)§b Set cost to item");
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
