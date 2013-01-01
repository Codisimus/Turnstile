package com.codisimus.plugins.turnstile;

import java.util.Iterator;
import java.util.LinkedList;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Button;
import org.bukkit.material.Door;

/**
 * A Turnstile is a fence or door used as a gate.
 *
 * @author Codisimus
 */
public class Turnstile {
    static boolean debug;

    public String name;
    public String owner;

    public String world;
    public int x;
    public int y;
    public int z;

    public double price = 0;
    public double moneyEarned = 0;

    public LinkedList<Item> items = new LinkedList<Item>();
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

    /**
     * Creates a Turnstile from the save file.
     *
     * @param name The name of the Turnstile
     * @param owner The Player or Bank that owns the Turnstile
     * @param world The name of World that the Block is in
     * @param x The x-coord of the Block
     * @param y The y-coord of the Block
     * @param z The z-coord of the Block
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
     * @param owner The Player who is creating the Turnstile and also the default owner
     * @param block The Block of the Trendulla
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

        for (Item item: items) {
            LinkedList<Integer> delete = item.findItem(inventory);

            if (delete == null) {
                player.sendMessage(TurnstileMessages.wrong);
                return;
            }

            flagForDelete.addAll(delete);
        }

        //Delete each stack in the Chest that is flagged for delete
        for (int stack: flagForDelete) {
            inventory.clear(stack);
        }

        //Increment earned and open the Turnstile
        player.sendMessage(TurnstileMessages.correct);
        open(null);
        itemsEarned++;

        //Increment the counter on linked Signs
        for (TurnstileSign sign: TurnstileMain.itemSigns) {
            if (sign.turnstile.equals(this)) {
                sign.incrementCounter();
            }
        }

        save();
    }

    /**
     * Checks the account balance of the Player and opens the Turnstile if there is enough
     *
     * @param player The Player who is activating the Button
     * @return true if the Player could afford to open the Turnstile
     */
    public boolean checkBalance(Player player) {
        String playerName = player.getName();

        if (debug) {
            TurnstileMain.logger.warning(name + " Debug: Charging " + playerName);
        }

        //Return true if the Player can open Turnstiles for free
        if (TurnstileMain.useOpenFreeNode && TurnstileMain.hasPermission(player, "openfree")) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: " + playerName
                                    + " is not charged to open Turnstiles");
                player.sendMessage("You are not charged to open Turnstiles");
            }
            return true;
        }

        if (playerName.equals(owner)) {
            if (Turnstile.debug) {
                TurnstileMain.logger.warning(name + " Debug: "+ playerName
                                + " is not charged because they are the Owner");
                player.sendMessage("You are not charged to open your own Turnstile");
            }
            return true;
        }

        //Clear the Player's account if the price is set to -411 (All)
        if (price == -411) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: " + playerName
                                    + "'s account will be set to 0");
            }

            Econ.economy.withdrawPlayer(playerName, Econ.economy.getBalance(playerName));
            player.sendMessage(TurnstileMessages.balanceCleared);
            return true;
        }

        //Return true if the price is not positive
        if (price <= 0) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Price is not positive, no charge");
            }
            return true;
        }

        //Return false if the Player could not afford the transaction
        if (!Econ.charge(playerName, owner, price)) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: " + playerName
                                    + " cannot afford " + Econ.format(price));
            }

            player.sendMessage(TurnstileMessages.notEnoughMoney);
            return false;
        }

        //Increment earned by the price
        player.sendMessage(TurnstileMessages.open.replace("<price>", Econ.format(price)));
        moneyEarned = moneyEarned + price;

        //Increment the amount of money earned on linked Signs
        for (TurnstileSign sign: TurnstileMain.moneySigns) {
            if (sign.turnstile.equals(this)) {
                sign.incrementEarned();
            }
        }

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
        if (debug) {
            TurnstileMain.logger.warning(name + " Debug: Checking access rights for " + player.getName());
        }

        //Return true if the Turnstile is public
        if (access == null) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Turnstile is public");
            }
            return true;
        }

        //Return isOwner() if the Turnstile is private
        if (access.isEmpty()) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Turnstile is private");
                TurnstileMain.logger.warning(name + " Debug: " + player.getName()
                        + (isOwner(player) ? " is an owner" : " is not an owner"));
            }

            if (isOwner(player)) {
                return true;
            }
        } else {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Turnstile has group access");
            }

            //Return true if the Player is in a group that has access
            World world = null;
            for (String group: access) {
                if (debug) {
                    TurnstileMain.logger.warning(name + " Debug: " + player.getName()
                            + (TurnstileMain.permission.playerInGroup(world, player.getName(), group)
                                ? " is in group "
                                : " is not in group ")
                            + group);
                }

                if (TurnstileMain.permission.playerInGroup(world, player.getName(), group)) {
                    return true;
                }
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

        block = TurnstileMain.server.getWorld(world).getBlockAt(x, y, z);

        //Determine the type of the gate to know how to open it
        switch (block.getType()) {
        case FENCE: //Change FENCE to AIR
            block.setTypeId(0);
            break;

        case TRAP_DOOR: //Fall through
        case FENCE_GATE: //Open FenceGate/TrapDoor
            //Open the gate if it is closed
            block.setData((byte) (block.getState().getRawData() | 4));
            break;

        case WOOD_DOOR: //Fall through
        case WOODEN_DOOR: //Fall through
        case IRON_DOOR: //Fall through
        case IRON_DOOR_BLOCK: //Open Door
            //Convert the Block to a Door
            BlockState state = block.getState();
            Door door = (Door) state.getData();

            //Open the Door
            door.setOpen(true);
            state.update();

            //Get the other half of the Door
            state = block.getRelative(BlockFace.UP).getState();
            door = (Door) state.getData();

            //Open the Door
            door.setOpen(true);
            state.update();

            break;

        default: break;
        }

        //Return if there is no timeOut set
        if (timeOut == 0) {
            return;
        }

        //Increment the instance and set what instance this open is
        instance++;
        final int temp = instance;

        //Close the gate after the specified amount of time
        TurnstileMain.server.getScheduler().scheduleSyncDelayedTask(TurnstileMain.plugin, new Runnable() {
            @Override
    	    public void run() {
                //Close if the Turnstile is open and a new instance was not started
                if (open && (temp == instance)) {
                    close();
                }
    	    }
    	}, 20L * timeOut);
    }

    /**
     * Closes the Turnstile
     */
    public void close() {
        Block block = TurnstileMain.server.getWorld(world).getBlockAt(x, y, z);

        //Determine the type of the gate to know how to close it
        switch (block.getType()) {
        case TRAP_DOOR: //Fall through
        case FENCE_GATE: //Close FenceGate/TrapDoor
            //Close the gate if it is open
            block.setData((byte) (block.getState().getRawData() & 11));
            break;

        case WOOD_DOOR: //Fall through
        case WOODEN_DOOR: //Fall through
        case IRON_DOOR: //Fall through
        case IRON_DOOR_BLOCK: //Open Door
            //Convert the Block to a Door
            BlockState state = block.getState();
            Door door = (Door) state.getData();

            //Open the Door
            door.setOpen(false);
            state.update();

            //Get the other half of the Door
            state = block.getRelative(BlockFace.UP).getState();
            door = (Door) state.getData();

            //Open the Door
            door.setOpen(false);
            state.update();

            break;

        default: //Change AIR to FENCE
            block.setTypeId(85);
            break;
        }

        open = false;
        TurnstileListener.openTurnstiles.remove(this);

        Iterator<Player> itr = TurnstileListener.occupiedTrendulas.keySet().iterator();
        Player player;

        while (itr.hasNext()) {
            player = itr.next();
            if (isBlock(TurnstileListener.occupiedTrendulas.get(player))) {
                TurnstileListener.occupiedTrendulas.remove(player);
            }
        }
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

            //Add the stack to the Player's inventory and decrement earned
            for (Item item: items) {
                int firstEmpty = sack.firstEmpty();
                if (firstEmpty >= 0) {
                    sack.setItem(firstEmpty, item.getItem());
                }
            }

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
        if (lockedStart == lockedEnd) {
            return false;
        } else if (lockedStart < lockedEnd) {
            return lockedStart < time && time < lockedEnd;
        } else {
            return lockedStart < time || time < lockedEnd;
        }
    }

    /**
     * Returns true if the Turnstile is currently free
     *
     * @param time The current Minecraft Server time
     * @return true if the Turnstile is currently free
     */
    public boolean isFree(long time) {
        if (freeStart == freeEnd) {
            return false;
        } else if (freeStart < freeEnd) {
            return freeStart < time && time < freeEnd;
        } else {
            return freeStart < time || time < freeEnd;
        }
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
        if (playerName.equalsIgnoreCase(owner)) {
            return true;
        }

        //Return true if Player is the owner of the Turnstile's bank
        if (owner.startsWith("bank:") && Econ.economy.isBankOwner(
                owner.substring(5), playerName).transactionSuccess()) {
            return true;
        }

        //Return true if Player has the Permission to ignore owner rights
        if (TurnstileMain.hasPermission(player, "admin.ignoreowner")) {
            return true;
        }

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
        if (!oneWay) {
            return;
        }

        //OneWay does not effect paying with items
        if (block == null) {
            openedFrom = BlockFace.SELF;
            return;
        }

        //Determine the type of the Block to find out the direction opened from
        switch (block.getType()) {
        case WOOD_BUTTON: //Fall through
        case STONE_BUTTON:
            openedFrom = ((Button) block.getState().getData()).getFacing();
            break;

        case WOOD_PLATE: //Fall through
        case STONE_PLATE:
            if (block.getZ() < z) {
                openedFrom = BlockFace.NORTH;
            } else if (block.getZ() > z) {
                openedFrom = BlockFace.SOUTH;
            } else if (block.getX() < x) {
                openedFrom = BlockFace.WEST;
            } else if (block.getX() > x) {
                openedFrom = BlockFace.EAST;
            } else {
                openedFrom = BlockFace.SELF;
            }

        default: break;
        }
    }

    /**
     * Returns true if the given Block has the same Location data as this Turnstile
     *
     * @param block The given Block
     * @return True if the Location data is the same
     */
    public boolean isBlock(Block block) {
        if (block.getX() != x) {
            return false;
        }

        if (block.getY() != y) {
            return false;
        }

        if (block.getZ() != z) {
            return false;
        }

        return block.getWorld().getName().equals(world);
    }

    /**
     * Returns true if the given Block has the same Location data as a Linked Button
     *
     * @param block The given Block
     * @return True if the Location data is the same
     */
    public boolean hasBlock(Block block) {
        switch (block.getType()) {
        case WOOD_DOOR: //Fall through
        case WOODEN_DOOR: //Fall through
        case IRON_DOOR: //Fall through
        case IRON_DOOR_BLOCK: //Get bottom half
            if (((Door) block.getState().getData()).isTopHalf())
                block = block.getRelative(BlockFace.DOWN);
            break;
        default: break;
        }

        //Return True if the Trendulla Block matches the given Block
        if (block.getX() == x && block.getY() == y &&
                block.getZ() == z && block.getWorld().getName().equals(world)) {
            return true;
        }

        //Iterate through the data to find a TurnstileButton that matches the given Block
        for (TurnstileButton button: buttons) {
            if (block.getX() == button.x && block.getY() == button.y
                    && block.getZ() == button.z
                    && block.getWorld().getName().equals(button.world)) {
                return true;
            }
        }

        //Return false because no Button was found
        return false;
    }

    /**
     * Returns the info of the Items as a String
     * This String is user friendly
     *
     * @return The String representation of the Items
     */
    public String itemsToInfoString() {
        if (items.isEmpty()) {
            return "";
        }

        String string = "";
        for (Item item: items) {
            string = string.concat(", " + item.toInfoString());
        }
        return string.substring(2);
    }

    /**
     * Returns the info of the Items as a String
     * This String is used for write data to file
     *
     * @return The String representation of the Items
     */
    public String itemsToString() {
        if (items.isEmpty()) {
            return "";
        }

        String string = "";
        for (Item item: items) {
            string = string.concat(", " + item.toString());
        }
        return string.substring(2);
    }

    /**
     * Adds Items from the given String of Item datas
     *
     * @data The String representation of the Items
     */
    public void setItems(String data) {
        if (data.isEmpty()) {
            return;
        }

        for (String item: data.split(", ")) {
            try {
                String[] itemData = item.split("'");
                LinkedList<Enchantment> enchantments = TurnstileCommand.getEnchantments(itemData[1]);

                if (enchantments == null) {
                    items.add(new Item(Integer.parseInt(itemData[0]),
                            Short.parseShort(itemData[1]), Integer.parseInt(itemData[2])));
                } else {
                    items.add(new Item(Integer.parseInt(itemData[0]),
                            enchantments, Integer.parseInt(itemData[2])));
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Writes the Turnstile data to file
     *
     */
    public void save() {
        TurnstileMain.saveTurnstile(this);
    }
}
