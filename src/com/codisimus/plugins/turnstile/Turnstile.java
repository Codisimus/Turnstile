package com.codisimus.plugins.turnstile;

import com.codisimus.plugins.turnstile.listeners.playerListener;
import java.util.LinkedList;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Button;
import org.bukkit.material.Door;

/**
 * A Turnstile is a fence or door used as a gate.
 * 
 * @author Codisimus
 */
public class Turnstile {
    public static boolean oneWay;
    public String name;
    public String access = "public";
    public double earned = 0;
    public double price = 0;
    public int item = 0;
    public int amount = 0;
    public short durability = -1;
    public boolean open = false;
    public int openedFrom;
    public String owner;
    public Block gate;
    public LinkedList<Block> buttons = new LinkedList<Block>();
    public int instance = 0;
    public long freeStart = 0;
    public long freeEnd = 0;
    public long lockedStart = 0;
    public long lockedEnd = 0;


    /**
     * Creates a Turnstile from the save file.
     * 
     * @param name The name of the Turnstile
     * @param gate The Block of the Turnstile
     * @param owner The Player or Bank that owns the Turnstile
     */
    public Turnstile (String name, Block gate, String owner) {
        this.name = name;
        this.gate = gate;
        this.owner = owner;
    }

    /**
     * Constructs a new Turnstile
     * 
     * @param name The name of the Turnstile which cannot already exist
     * @param creator The Player who is creating the Turnstile and also the default owner
     */
    public Turnstile (String name, Player creator) {
        this.name = name;
        this.owner = creator.getName();
        gate = creator.getTargetBlock(null, 100);
    }
    
    /**
     * Checks if the Player is attempting to enter the Turnstile backwards
     * 
     * @param from The Block the Player is entering the Turnstile from
     * @return false if Player is attempting to enter Turnstile from the wrong direction
     */
    public boolean checkOneWay(Block from) {
        switch (openedFrom) {
            case 0: return from.getX() < gate.getX();
            case 1: return from.getX() > gate.getX();
            case 2: return from.getZ() < gate.getZ();
            case 3: return from.getZ() > gate.getZ();
            default: return true;
        }
    }

    /**
     * Checks the contents of the Chest and compares it to the item, amount, and durability 
     * 
     * @param chest The Chest being activated
     * @param player The Player who activated the Chest
     */
    public void checkContents(Chest chest, Player player) {
        Inventory inventory = chest.getInventory();
        LinkedList<Integer> flagForDelete = new LinkedList<Integer>();
        int total = 0;
        
        //Iterate through the contents of the Chest to look for matching items
        ItemStack[] stacks = inventory.getContents();
        for (int i=0; i < stacks.length; i++) {
            if (stacks[i] != null)
                if (stacks[i].getTypeId() == item)
                    if (durability == -1 || stacks[i].getDurability() == durability) {
                        total = total + stacks[i].getAmount();
                        flagForDelete.add(i);
                    }
            
            //Check if enough items were found
            if (total >= amount) {
                //Delete each stack in the Chest that is flagged for delete
                for (int stack: flagForDelete)
                    inventory.clear(stack);
                
                //Increment earned and open the Turnstile
                player.sendMessage(TurnstileMain.correct);
                open(chest.getBlock());
                earned++;
                return;
            }
        }
            
        player.sendMessage(TurnstileMain.wrong);
    }

    /**
     * Checks the account balance of the Player and opens the Turnstile if there is enough
     * 
     * @param player The Player who is activating the Button
     * @return true if the Player could afford to open the Turnstile
     */
    public boolean checkBalance(Player player) {
        //Return true if the Player can open Turnstiles for free
        if (TurnstileMain.useOpenFreeNode && TurnstileMain.hasPermission(player, "openfree"))
            return true;
        
        //Clear the Player's account if the price is set to -411 (All)
        if (price == -411) {
            Register.clearBalance(player.getName());
            player.sendMessage(TurnstileMain.balanceCleared);
            return true;
        }
        
        //Return true if the price is not positive
        if (price <= 0)
            return true;
        
        //Return false if the Player could not afford the transaction
        if (!Register.charge(player.getName(), owner, price)) {
            player.sendMessage(TurnstileMain.notEnoughMoney);
            return false;
        }
        
        //Return true after incrementing earned by the price
        player.sendMessage(TurnstileMain.open.replaceAll("<price>", ""+Register.format(price)));
        earned = earned + price;
        return true;
    }
    
    /**
     * Checks access for private or a specific group
     * Returns true if the Player has access rights
     * 
     * @param player The Player who is activating the Button
     * @return true if the Player has access rights
     */
    public boolean hasAccess(Player player) {
        //Return true if the Turnstile is public
        if (access.equals("public"))
            return true;
        
        //Return false if the Turnstile is private and the Player is an owner
        if (access.equals("private")) {
            if (!isOwner(player)) {
                player.sendMessage(TurnstileMain.privateTurnstile);
                return false;
            }
        }
        
        //Return false if the Player is not in the group that has access
        if (TurnstileMain.permissions == null || !TurnstileMain.permissions.getUser(player).inGroup(access)) {
            player.sendMessage(TurnstileMain.privateTurnstile);
            return false;
        }
        
        //Return true because the Player has access rights
        return true;
    }

    /**
     * Opens the Turnstile
     *
     * @param block The Block that was used to open the Turnstile
     */
    public void open(Block block) {
        open = true;
        setOpenedFrom(block);
        playerListener.openTurnstiles.add(this);
        
        //Start a new thread
        Thread thread = new Thread() {
            @Override
            public void run() {
                //Determine the type of the gate to know how to open it
                Door door;
                switch (gate.getTypeId()) {
                    case 85: gate.setTypeId(0); break; //Change FENCE to AIR
                    //case 96: TrapDoor trapDoor = (TrapDoor)gate.getState().getData(); break; //Open TrapDoor
                    //case 107: break; //Open FenceGate
                    case 64: //Open Door
                        door = (Door)gate.getState().getData();
                        if (door.isOpen())
                            door.setOpen(true);
                        break;
                    case 71: //Open Door
                        door = (Door)gate.getState().getData();
                        if (door.isOpen())
                            door.setOpen(true);
                        break;
                    case 324: //Open Door
                        door = (Door)gate.getState().getData();
                        if (door.isOpen())
                            door.setOpen(true);
                        break;
                    case 330: //Open Door
                        door = (Door)gate.getState().getData();
                        if (door.isOpen())
                            door.setOpen(true);
                        break;
                    default: break;
                }
                
                //Return if there is no timeOut set
                if (TurnstileMain.timeOut == 0)
                    return;
                
                //Increment the instance and set what instance this open is
                int temp = instance++;
                
                //Leave gate open for specific amount of time
                try {
                    Thread.currentThread().sleep(TurnstileMain.timeOut * 1000);
                }
                catch (InterruptedException ex) {
                }
                
                //Close if the Turnstile is open and a new instance was not started
                if (open && (temp == instance))
                    close();
            }
        };
        thread.start();
    }

    /**
     * Closes the Turnstile
     *
     */
    public void close() {
        //Determine the type of the gate to know how to close it
        Door door;
        switch (gate.getTypeId()) {
            case 0: gate.setTypeId(85); break; //Change AIR to FENCE
            //case 96: TrapDoor trapDoor = (TrapDoor)gate.getState().getData(); break; //Close TrapDoor
            //case 107: break; //Close FenceGate
            case 64: //Close Door
                door = (Door)gate.getState().getData();
                if (door.isOpen())
                    door.setOpen(false);
                break;
            case 71: //Close Door
                door = (Door)gate.getState().getData();
                if (door.isOpen())
                    door.setOpen(false);
                break;
            case 324: //Close Door
                door = (Door)gate.getState().getData();
                if (door.isOpen())
                    door.setOpen(false);
                break;
            case 330: //Close Door
                door = (Door)gate.getState().getData();
                if (door.isOpen())
                    door.setOpen(false);
                break;
            default: break;
        }
        
        open = false;
        playerListener.openTurnstiles.remove(this);
    }
    
    /**
     * Gives earned items to the Player if they are an owner
     * 
     * @param player The Player who is collecting the items
     */
    public void collect(Player player) {
        PlayerInventory sack = player.getInventory();
        
        //Loop unless the Player's inventory is full
        while (sack.firstEmpty() >= 0) {
            //Return when all earned items are collected
            if (earned <= 0) {
                player.sendMessage("There are no more items to collect");
                return;
            }
            
            ItemStack stack = new ItemStack(item, amount);
            
            //Set the durability if it is not equal to -1
            if (durability != -1)
                stack.setDurability(durability);
            
            //Add the stack to the Player's inventory and decrement earned
            sack.setItem(sack.firstEmpty(), stack);
            earned--;
        }
        
        player.sendMessage("Inventory full");
    }

    /**
     * Returns true if the Turnstile is currently locked
     * 
     * @param time The current Minecraft Server time
     * @return true if the Turnstile is currently locked
     */
    public boolean isLocked(long time) {
        if (lockedStart == lockedEnd)
            return false;
        else if (lockedStart < lockedEnd)
            return lockedStart < time && time < lockedEnd;
        else
            return lockedStart < time || time < lockedEnd;
    }

    /**
     * Returns true if the Turnstile is currently free
     * 
     * @param time The current Minecraft Server time
     * @return true if the Turnstile is currently free
     */
    public boolean isFree(long time) {
        if (freeStart == freeEnd)
            return false;
        else if (freeStart < freeEnd)
            return freeStart < time && time < freeEnd;
        else
            return freeStart < time || time < freeEnd;
    }

    /**
     * Compares player to owner to see if they match
     * Checks Bank owners as well
     * 
     * @param player The player who is using the Turnstile command
     * @return true if the player is an owner
     */
    public boolean isOwner(Player player) {
        //Return true if Player is the owner
        if (player.getName().equalsIgnoreCase(owner))
            return true;
        
        //Return true if Player is the owner of the Turnstile's bank
        if (owner.substring(0, 5).equalsIgnoreCase("bank:") && Register.isBankOwner(owner.substring(5), player.getName()))
            return true;
        
        //Return true if Player has the Permission to ignore owner rights
        if (TurnstileMain.hasPermission(player, "admin.ignoreowner"))
            return true;
        
        return false;
    }
    
    /**
     * Discovers the type of a switch
     * Sets the direction that the Turnstile is opened from
     * 
     * @param block The given switch
     */
    public void setOpenedFrom(Block block) {
        //Cancel if Turnstiles are not one way
        if (!oneWay)
            return;
        
        //Determine the type of the Block to find out the direction opened from
        int id = block.getTypeId();
        switch (id) {
            case 54: //Material == Chest
                openedFrom = -1; //OneWay does not effect paying with items
                break;
            case 77: //Material == Stone Button
                Button button = (Button)block.getState().getData();
                BlockFace face = button.getFacing();
                if (face.equals(BlockFace.NORTH))
                    openedFrom = 0;
                else if (face.equals(BlockFace.SOUTH))
                    openedFrom = 1;
                else if (face.equals(BlockFace.EAST))
                    openedFrom = 2;
                else if (face.equals(BlockFace.WEST))
                    openedFrom = 3;
                else
                    openedFrom = -1;
                break;
            default: //Material == Stone Plate || Wood Plate
                if (block.getX() < gate.getX())
                    openedFrom = 0;
                else if (block.getX() > gate.getX())
                    openedFrom = 1;
                else if (block.getZ() < gate.getZ())
                    openedFrom = 2;
                else if (block.getZ() > gate.getZ())
                    openedFrom = 3;
                else
                    openedFrom = -1;
        }
    }
}
