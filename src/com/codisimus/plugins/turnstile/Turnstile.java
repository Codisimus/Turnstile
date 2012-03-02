package com.codisimus.plugins.turnstile;

import java.util.LinkedList;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
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
    public String name;
    public String owner;

    public String world;
    public int x;
    public int y;
    public int z;

    public double price = 0;
    public double moneyEarned = 0;

    public int item = 0;
    public short durability = -1;
    public int amount = 0;
    public int itemsEarned = 0;

    public boolean oneWay = TurnstileMain.defaultOneWay;
    public boolean noFraud = TurnstileMain.defaultNoFraud;
    public int timeOut = TurnstileMain.defaultTimeOut;

    public long freeStart = 0;
    public long freeEnd = 0;
    public long lockedStart = 0;
    public long lockedEnd = 0;

    public LinkedList<String> access = null; //List of Groups that have access (private if empty, public if null)
    public LinkedList<TurnstileButton> buttons = new LinkedList<TurnstileButton>(); //List of Blocks that activate the Warp

    public boolean open = false;
    private int instance = 0;
    private BlockFace openedFrom;
    
    static boolean debug;

    /**
     * Creates a Turnstile from the save file.
     * 
     * @param name The name of the Turnstile
     * @param gate The Block of the Turnstile
     * @param owner The Player or Bank that owns the Turnstile
     */
    public Turnstile (String name, String owner, String world, int x, int y, int z) {
        this.name = name;
        this.owner = owner;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Constructs a new Turnstile
     * 
     * @param name The name of the Turnstile which cannot already exist
     * @param creator The Player who is creating the Turnstile and also the default owner
     */
    public Turnstile (String name, String owner, Block block) {
        this.name = name;
        this.owner = owner;
        world = block.getWorld().getName();
        x = block.getX();
        y = block.getY();
        z = block.getZ();
    }
    
    /**
     * Checks if the Player is attempting to enter the Turnstile backwards
     * 
     * @param from The Block the Player is entering the Turnstile from
     * @return false if Player is attempting to enter Turnstile from the wrong direction
     */
    public boolean checkOneWay(Block from) {
        switch (openedFrom) {
            case NORTH: return from.getX() < x;
            case SOUTH: return from.getX() > x;
            case EAST: return from.getZ() < z;
            case WEST: return from.getZ() > z;
            default: return true;
        }
    }

    /**
     * Checks the contents of the Chest and compares it to the item, amount, and durability 
     * 
     * @param chest The Chest being activated
     * @param player The Player who activated the Chest
     */
    public void checkContents(Inventory inventory, Player player) {
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
                player.sendMessage(TurnstileMessages.correct);
                open(null);
                itemsEarned++;
                
                //Increment the counter on linked Signs
                for (TurnstileSign sign: TurnstileMain.itemSigns) {
                    if (sign.turnstile.equals(this))
                        sign.incrementCounter();
                }
                
                save();
                return;
            }
        }
            
        player.sendMessage(TurnstileMessages.wrong);
    }

    /**
     * Checks the account balance of the Player and opens the Turnstile if there is enough
     * 
     * @param player The Player who is activating the Button
     * @return true if the Player could afford to open the Turnstile
     */
    public boolean checkBalance(Player player) {
        if (debug)
            System.out.println("Turnstile "+name+" Debug: Charging "+player.getName());
        
        String playerName = player.getName();
        
        //Return true if the Player can open Turnstiles for free
        if (TurnstileMain.useOpenFreeNode && TurnstileMain.hasPermission(player, "openfree")) {
            if (debug)
                System.out.println("Turnstile "+name+" Debug: "+playerName+" is not charged to open Turnstiles");
            
            player.sendMessage("You are not charged to open Turnstiles");
            return true;
        }
        
        if (playerName.equals(owner)) {
            if (Turnstile.debug)
                System.out.println("Turnstile "+name+" Debug: "+playerName+" is not charged because they are the Owner");
            
            player.sendMessage("You are not charged to open your own Turnstile");
            return true;
        }
        
        //Clear the Player's account if the price is set to -411 (All)
        if (price == -411) {
            if (debug)
                System.out.println("Turnstile "+name+" Debug: "+player.getName()+"'s account will be set to 0");
            
            Econ.economy.withdrawPlayer(playerName, Econ.economy.getBalance(playerName));
            player.sendMessage(TurnstileMessages.balanceCleared);
            return true;
        }
        
        //Return true if the price is not positive
        if (price <= 0) {
            if (debug)
                System.out.println("Turnstile "+name+" Debug: Price is not positive, no charge");
            return true;
        }
        
        //Return false if the Player could not afford the transaction
        if (!Econ.charge(playerName, owner, price)) {
            if (debug)
                System.out.println("Turnstile "+name+" Debug: "+player.getName()+" cannot afford "+Econ.format(price));
            
            player.sendMessage(TurnstileMessages.notEnoughMoney);
            return false;
        }
        
        //Increment earned by the price
        player.sendMessage(TurnstileMessages.open.replaceAll("<price>", Econ.format(price)));
        moneyEarned = moneyEarned + price;
        
        //Increment the amount of money earned on linked Signs
        for (TurnstileSign sign: TurnstileMain.moneySigns)
            if (sign.turnstile.equals(this))
                sign.incrementEarned();
        
        save();
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
        if (debug)
            System.out.println("Turnstile "+name+" Debug: Checking access rights for "+player.getName());
        
        //Return true if the Turnstile is public
        if (access == null) {
            if (debug)
                System.out.println("Turnstile "+name+" Debug: Turnstile is public");
            return true;
        }

        //Return isOwner() if the Turnstile is private
        if (access.isEmpty()) {
            if (debug) {
                System.out.println("Turnstile "+name+" Debug: Turnstile is private");
                System.out.println("Turnstile "+name+" Debug: "+player.getName()+(isOwner(player) ? " is an owner" : " is not an owner"));
            }
            
            if (isOwner(player))
                return true;
        }
        else {
            if (debug)
                System.out.println("Turnstile "+name+" Debug: Turnstile has group access");
            
            //Return true if the Player is in a group that has access
            for (String group: access) {
                System.out.println("Turnstile "+name+" Debug: "+player.getName()+
                        (TurnstileMain.permission.playerInGroup(player, group) ? " is in group " : " is not in group ")+group);
                
                if (TurnstileMain.permission.playerInGroup(player, group))
                    return true;
            }
        }
        
        //Return false because the Player does not have access rights
        player.sendMessage(TurnstileMessages.privateTurnstile);
        return false;
    }

    /**
     * Opens the Turnstile
     *
     * @param block The Block that was used to open the Turnstile
     */
    public void open(Block block) {
        open = true;
        setOpenedFrom(block);
        TurnstileListener.openTurnstiles.add(this);
        
        //Start a new thread
        Thread thread = new Thread() {
            @Override
            public void run() {
                Block block = TurnstileMain.server.getWorld(world).getBlockAt(x, y, z);
                
                //Determine the type of the gate to know how to open it
                switch (block.getType()) {
                    case FENCE: block.setTypeId(0); break; //Change FENCE to AIR
                    
                    case TRAP_DOOR: //Fall through
                    case FENCE_GATE: //Open FenceGate/TrapDoor
                        //Open the gate if it is closed
                        block.setData((byte)(block.getState().getData().getData() | 4));
                        break;

                    case WOOD_DOOR: //Fall through
                    case WOODEN_DOOR: //Fall through
                    case IRON_DOOR: //Fall through
                    case IRON_DOOR_BLOCK: //Open Door
                        //Convert the Block to a Door
                        BlockState state = block.getState();
                        Door door = (Door)state.getData();

                        //Open the Door
                        door.setOpen(true);
                        state.update();

                        //Get the other half of the Door
                        state = block.getRelative(BlockFace.UP).getState();
                        door = (Door)state.getData();

                        //Open the Door
                        door.setOpen(true);
                        state.update();
                        
                        break;

                    default: break;
                }
                
                //Return if there is no timeOut set
                if (timeOut == 0)
                    return;
                
                //Increment the instance and set what instance this open is
                instance++;
                int temp = instance;
                
                //Leave gate open for specific amount of time
                try {
                    Thread.currentThread().sleep(timeOut * 1000);
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
        Block block = TurnstileMain.server.getWorld(world).getBlockAt(x, y, z);

        //Determine the type of the gate to know how to close it
        switch (block.getType()) {
            case TRAP_DOOR: //Fall through
            case FENCE_GATE: //Close FenceGate/TrapDoor
                //Close the gate if it is open
                block.setData((byte)(block.getState().getData().getData() & 11));
                break;

            case WOOD_DOOR: //Fall through
            case WOODEN_DOOR: //Fall through
            case IRON_DOOR: //Fall through
            case IRON_DOOR_BLOCK: //Open Door
                //Convert the Block to a Door
                BlockState state = block.getState();
                Door door = (Door)state.getData();
                
                //Open the Door
                door.setOpen(false);
                state.update();

                //Get the other half of the Door
                state = block.getRelative(BlockFace.UP).getState();
                door = (Door)state.getData();
                
                //Open the Door
                door.setOpen(false);
                state.update();

                break;

            default: block.setTypeId(85); break; //Change AIR to FENCE
        }
        
        open = false;
        TurnstileListener.openTurnstiles.remove(this);
        for (Player player: TurnstileListener.occupiedTrendulas.keySet())
            if (isBlock(TurnstileListener.occupiedTrendulas.get(player)))
                TurnstileListener.occupiedTrendulas.remove(player);
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
            if (itemsEarned <= 0) {
                player.sendMessage("There are no more items to collect");
                return;
            }
            
            ItemStack stack = new ItemStack(item, amount);
            
            //Set the durability if it is not equal to -1
            if (durability != -1)
                stack.setDurability(durability);
            
            //Add the stack to the Player's inventory and decrement earned
            sack.setItem(sack.firstEmpty(), stack);
            itemsEarned--;
        }
        
        player.sendMessage("Inventory full");
        save();
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
        String playerName = player.getName();
        
        //Return true if Player is the owner
        if (playerName.equalsIgnoreCase(owner))
            return true;
        
        //Return true if Player is the owner of the Turnstile's bank
        if (owner.startsWith("bank:") &&
                Econ.economy.isBankOwner(owner.substring(5), playerName).transactionSuccess())
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
    private void setOpenedFrom(Block block) {
        //Cancel if Turnstiles are not one way
        if (!oneWay)
            return;
        
        //OneWay does not effect paying with items
        if (block == null) {
            openedFrom = BlockFace.SELF;
            return;
        }
        
        //Determine the type of the Block to find out the direction opened from
        switch (block.getType()) {
            case STONE_BUTTON: openedFrom = ((Button)block.getState().getData()).getFacing(); break;

            case WOOD_PLATE: //Fall through
            case STONE_PLATE:
                if (block.getX() < x)
                    openedFrom = BlockFace.NORTH;
                else if (block.getX() > x)
                    openedFrom = BlockFace.SOUTH;
                else if (block.getZ() < z)
                    openedFrom = BlockFace.EAST;
                else if (block.getZ() > z)
                    openedFrom = BlockFace.WEST;
                else
                    openedFrom = BlockFace.SELF;
                
            default:
        }
    }

    /**
     * Returns true if the given Block has the same Location data as this Turnstile
     *
     * @param block The given Block
     * @return True if the Location data is the same
     */
    public boolean isBlock(Block block) {
        if (block.getX() != x)
            return false;

        if (block.getY() != y)
            return false;

        if (block.getZ() != z)
            return false;

        return block.getWorld().getName().equals(world);
    }

    /**
     * Returns true if the given Block has the same Location data as a Linked Button
     *
     * @param block The given Block
     * @return True if the Location data is the same
     */
    public boolean hasBlock(Block block) {
        //Return True if the Trendulla Block matches the given Block
        if (block.getX() == x && block.getY() == y &&
                block.getZ() == z && block.getWorld().getName().equals(world))
            return true;
        
        //Iterate through the data to find a TurnstileButton that matches the given Block
        for (TurnstileButton button: buttons)
            if (block.getX() == button.x && block.getY() == button.y &&
                    block.getZ() == button.z && block.getWorld().getName().equals(button.world))
                return true;

        //Return false because no Button was found
        return false;
    }
    
    /**
     * Writes the Turnstile data to file
     * 
     */
    public void save() {
        TurnstileMain.saveTurnstile(this);
    }
}