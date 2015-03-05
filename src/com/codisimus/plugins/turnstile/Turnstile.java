package com.codisimus.plugins.turnstile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.time.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Button;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A Turnstile is a fence or door used as a gate.
 *
 * @author Codisimus
 */
@SerializableAs("Turnstile")
public class Turnstile implements ConfigurationSerializable {
    static {
        ConfigurationSerialization.registerClass(Turnstile.class, "Turnstile");
    }
    static String current;
    static String last;
    static boolean debug;

    public String name;
    public String owner;

    public String world;
    public int x;
    public int y;
    public int z;

    public double price = 0;
    public double moneyEarned = 0;

    public ArrayList<ItemStack> items = new ArrayList<>();
    public int itemsEarned = 0;

    public boolean oneWay = TurnstileConfig.defaultOneWay;
    public boolean noFraud = TurnstileConfig.defaultNoFraud;
    public int timeOut = TurnstileConfig.defaultTimeOut;

    public int freeStart = 0;
    public int freeEnd = 0;
    public int lockedStart = 0;
    public int lockedEnd = 0;

    public String access = "public";

    /* Cooldown time (will never cooldown if any are negative) */
    public int days;
    public int hours;
    public int minutes;
    public int seconds;
    public boolean roundDown = false;

    public final Properties onCooldown = new Properties();
    public String addedToCooldown = ""; //Should be removed if the Player fails to pass through the Turnstile
    public boolean privateWhileOnCooldown = false;
    public int amountPerCooldown = 1; //This value only applies when privateWhileOnCooldown == true

    public ArrayList<TurnstileButton> buttons = new ArrayList<>(); //List of Blocks that activate the Turnstile

    public boolean open = false;
    private int instance = 0;
    private BlockFace openedFrom;

    /**
     * Constructs a new Turnstile
     *
     * @param name The name of the Turnstile which cannot already exist
     * @param owner The Player who is creating the Turnstile and also the default owner
     * @param block The Block of the Trendula
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
        case NORTH: return from.getZ() < z;
        case SOUTH: return from.getZ() > z;
        case WEST: return from.getX() < x;
        case EAST: return from.getX() > x;
        default: return true;
        }
    }

    /**
     * Checks the contents of the Inventory and compares it to the item, amount, and durability
     *
     * @param inventory The Inventory being checked
     * @param player The Player who activated the Chest
     */
    public void checkContents(Inventory inventory, Player player) {
        HashSet<Integer> flagForDelete = new HashSet<>();

        for (ItemStack item : items) {
            HashSet<Integer> delete = findItem(inventory, item);

            if (delete.isEmpty()) {
                player.sendMessage(TurnstileMessages.wrong);
                return;
            }

            flagForDelete.addAll(delete);
        }

        //Delete each stack in the Chest that is flagged for delete
        for (int stack : flagForDelete) {
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

    private HashSet<Integer> findItem(Inventory inventory, ItemStack item) {
        HashSet<Integer> flagForDelete = new HashSet<>();
        int total = 0;
        String displayName = null;
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                displayName = meta.getDisplayName();
            }
        }

        //Iterate through the contents of the Inventory to look for matching items
        ItemStack[] stacks = inventory.getContents();
        for (ItemStack stack : stacks) {
            if (stack != null) {
                boolean match = false;
                if (displayName != null) {
                    if (stack.hasItemMeta()) {
                        ItemMeta meta = stack.getItemMeta();
                        if (meta.hasDisplayName() && meta.getDisplayName().equals(displayName)) {
                            match = true;
                        }
                    }
                } else if (item.isSimilar(stack)) {
                    match = true;
                }
                if (match) {
                    total += stack.getAmount();
                }
                //Check if enough items were found
                if (total >= item.getAmount()) {
                    return flagForDelete;
                }
            }
        }

        //Not enough items were found
        return new HashSet<>();
    }

    /**
     * Checks the account balance of the Player and opens the Turnstile if there is enough
     *
     * @param player The Player who is activating the Button
     * @return true if the Player could afford to open the Turnstile
     */
    public boolean checkBalance(Player player) {
        String playerName = player.getName();
        if (!addedToCooldown.isEmpty() && !addedToCooldown.equals(playerName)) {
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: " + playerName
                                    + " is trying to enter a Turnstile that they did no pay for");
            }
            player.sendMessage(TurnstileMessages.noFraud);
            return false;
        }

        if (debug) {
            TurnstileMain.logger.warning(name + " Debug: Charging " + playerName);
        }

        //Return true if the Player can open Turnstiles for free
        if (player.hasPermission("turnstile.openfree")) {
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
            if (!addedToCooldown.isEmpty()) {
                onCooldown.remove(addedToCooldown);
                addedToCooldown = "";
            }
            return false;
        }

        //Increment earned by the price
        player.sendMessage(TurnstileMessages.open.replace("<price>", Econ.format(price)));
        moneyEarned += price;

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
        String playerName = player.getName();
        if (debug) {
            TurnstileMain.logger.warning(name + " Debug: Checking access rights for " + playerName);
        }

        switch (access) {
        case "public":
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Turnstile is public");
            }
        case "private":
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Turnstile is private");
                TurnstileMain.logger.warning(name + " Debug: " + playerName
                        + (isOwner(player) ? " is an owner" : " is not an owner"));
            }

            if (!isOwner(player)) {
                player.sendMessage(TurnstileMessages.privateTurnstile);
                return false;
            }
        default: //Turnstile has limited access
            if (debug) {
                TurnstileMain.logger.warning(name + " Debug: Turnstile has limited access");
            }

            boolean hasAccess = false;
            for (String node : access.split(" ")) {
                if (node.equalsIgnoreCase(playerName)) {
                    if (debug) {
                        TurnstileMain.logger.warning(name + " Debug: " + playerName + " is on the access list");
                    }
                    break;
                } else if (player.hasPermission(node)) {
                    hasAccess = true;
                    break;
                }
                    
            }
            if (!hasAccess) {
                if (debug) {
                    TurnstileMain.logger.warning(name + " Debug: " + playerName + " is not on the access list");
                }
                player.sendMessage(TurnstileMessages.privateTurnstile);
            }
        }

        if (days == 0 && hours == 0 && minutes == 0 && seconds == 0) { //Does not use cooldown
            return true;
        }

        String time = String.valueOf(getCurrentMillis());
        if (!onCooldown.isEmpty()) { //Turnstile currently on cooldown
            if (!onCooldown.containsKey(playerName)) { //Player currently on cooldown
                if (privateWhileOnCooldown) {
                    String lastUse = (String) onCooldown.values().toArray()[0];
                    String timeRemaining = getTimeRemaining(Long.parseLong(lastUse));
                    if (timeRemaining == null || !timeRemaining.equals("0")) { //Still cooling down
                        if (onCooldown.size() >= amountPerCooldown) { //Full amount is already on cooldown
                            player.sendMessage(TurnstileMessages.cooldownPrivate.replace("<time>", timeRemaining));
                            return false;
                        } else {
                            time = lastUse;
                        }
                    } else {
                        onCooldown.clear();
                    }
                }
            } else {
                String timeRemaining = getTimeRemaining(Long.parseLong(onCooldown.getProperty(playerName)));
                if (timeRemaining == null || !timeRemaining.equals("0")) { //Still cooling down
                    player.sendMessage(TurnstileMessages.cooldown.replace("<time>", timeRemaining));
                    return true;
                }
                onCooldown.remove(playerName);
            }
        }

        addToCooldown(playerName, time);
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
        TurnstileListener.openTurnstiles.add(this);

        TurnstileUtil.openDoor(getBlock());

        //Return if there is no timeOut set
        if (timeOut == 0) {
            return;
        }

        //Increment the instance and set what instance this open is
        instance++;
        final int temp = instance;

        //Close the gate after the specified amount of time
        new BukkitRunnable() {
            @Override
            public void run() {
                //Close if the Turnstile is open and a new instance was not started
                if (open && (temp == instance)) {
                    if (!addedToCooldown.isEmpty()) {
                        onCooldown.remove(addedToCooldown);
                        addedToCooldown = "";
                    }
                    close();
                }
            }
        }.runTaskLater(TurnstileMain.plugin, 20L * timeOut);
    }

    /**
     * Closes the Turnstile
     */
    public void close() {
        TurnstileUtil.closeDoor(getBlock());

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
        while (sack.firstEmpty() >= 0) { //TODO make sure there is room for each item
            //Return when all earned items are collected
            if (itemsEarned <= 0) {
                player.sendMessage("There are no more items to collect");
                return;
            }

            //Add the stack to the Player's inventory and decrement earned
            for (ItemStack item : items) {
                int firstEmpty = sack.firstEmpty();
                if (firstEmpty >= 0) {
                    sack.setItem(firstEmpty, item.clone());
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
     * Returns the remaining time until the Button resets
     * Returns null if the Button never resets
     *
     * @param time The given time
     * @return the remaining time until the Button resets
     */
    private String getTimeRemaining(long time) {
        //Return null if the reset time is set to never
        if (days < 0 || hours < 0 || minutes < 0 || seconds < 0) {
            return null;
        }

        //Calculate the time that the Warp will reset
        time += days * DateUtils.MILLIS_PER_DAY
                + hours * DateUtils.MILLIS_PER_HOUR
                + minutes * DateUtils.MILLIS_PER_MINUTE
                + seconds * DateUtils.MILLIS_PER_SECOND;

        return timeToString(time - getCurrentMillis());
    }

    /**
     * Returns a human friendly String of the remaining time until the PhatLootChest resets
     *
     * @param time The given time
     * @return the remaining time until the PhatLootChest resets
     */
    private String timeToString(long time) {
        //Find the appropriate unit of time and return that amount
        if (time > DateUtils.MILLIS_PER_DAY) {
            return time / DateUtils.MILLIS_PER_DAY + " day(s)";
        } else if (time > DateUtils.MILLIS_PER_HOUR) {
            return time / DateUtils.MILLIS_PER_HOUR + " hour(s)";
        } else if (time > DateUtils.MILLIS_PER_MINUTE) {
            return time / DateUtils.MILLIS_PER_MINUTE + " minute(s)";
        } else if (time > DateUtils.MILLIS_PER_SECOND) {
            return time / DateUtils.MILLIS_PER_SECOND + " second(s)";
        } else {
            return "0";
        }
    }

    /**
     * Compares sender to owner to see if they match
     * Checks Bank owners as well
     *
     * @param sender The CommandSender who is using the Turnstile command
     * @return true if the player is an owner
     */
    public boolean isOwner(CommandSender sender) {
        String playerName = sender.getName();

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
        if (sender.hasPermission("admin.ignoreowner")) {
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
        block = TurnstileUtil.getBottomHalf(block);
        return block.getX() == x && block.getY() == y && block.getZ() == z
                && block.getWorld().getName().equals(world);
    }

    /**
     * Returns The Block of the Turnstile Door/Gate
     *
     * @return The Block of the Trendula
     */
    public Block getBlock() {
        return Bukkit.getWorld(world).getBlockAt(x, y, z);
    }

    /**
     * Returns true if the given Block has the same Location data as a Linked Button
     *
     * @param block The given Block
     * @return True if the Location data is the same
     */
    public boolean hasBlock(Block block) {
        //Return True if the Trendula Block matches the given Block
        if (isBlock(block)) {
            return true;
        }

        //Iterate through the data to find a TurnstileButton that matches the given Block
        for (TurnstileButton button : buttons) {
            if (button.matchesBlock(block)) {
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

        StringBuilder sb = new StringBuilder();
        Iterator<ItemStack> itr = items.iterator();
        while (itr.hasNext()) {
            sb.append(getItemName(itr.next()));
            if (itr.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns a user friendly String of the given ItemStack's name
     *
     * @param item The given ItemStack
     * @return The name of the item
     */
    public static String getItemName(ItemStack item) {
        StringBuilder sb = new StringBuilder();
        if (item.getAmount() > 1) {
            sb.append(item.getAmount());
            sb.append(" of ");
        }
        //Return the Display name of the item if there is one
        if (item.hasItemMeta()) {
            String name = item.getItemMeta().getDisplayName();
            if (name != null && !name.isEmpty()) {
                sb.append(name);
            }
        }
        //A display name was not found so use a cleaned up version of the Material name
        sb.append(WordUtils.capitalizeFully(item.getType().toString().replace("_", " ")));
        return sb.toString();
    }

    public void addToCooldown(String player, String time) {
        onCooldown.setProperty(player, time);
        addedToCooldown = player;
    }

    public long getCurrentMillis() {
        Calendar cal = Calendar.getInstance();

        if (roundDown) {
            if (seconds == 0) {
                cal.set(Calendar.SECOND, 0);
                if (minutes != 0) {
                    cal.set(Calendar.MINUTE, 0);
                    if (hours != 0) {
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                    }
                }
            }
        }

        return Calendar.getInstance().getTimeInMillis();
    }

    public void clean() {
        //TODO
    }

    /**
     * Writes the Turnstile data to file
     */
    public void saveAll() {
        save();
        saveCooldownTimes();
    }

    /**
     * Writes the Cooldown times of the Turnstile to file
     * if there is an old file it is over written
     */
    public void saveCooldownTimes() {
        //Don't save an empty file
        if (onCooldown.isEmpty()) {
            return;
        }

        File file = new File(TurnstileMain.dataFolder, "CooldownTimes" + File.separator + name + ".properties");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            onCooldown.store(fos, null);
        } catch (IOException ex) {
            TurnstileMain.logger.log(Level.SEVERE, "Save Failed!", ex);
        }
    }

    /**
     * Reads Cooldown times of the Turnstile from file
     */
    public void loadCooldownTimes() {
        try {
            File file = new File(TurnstileMain.dataFolder, "CooldownTimes" + File.separator + name + ".properties");
            if (!file.exists()) {
                return;
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                onCooldown.load(fis);
            }
        } catch (IOException ex) {
            TurnstileMain.logger.log(Level.SEVERE, "Load Failed!", ex);
        }
    }

    /**
     * Writes the Turnstile to file.
     * If there is an old file it is over written
     */
    public void save() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set(name, this);
            config.save(new File(TurnstileMain.dataFolder, "Turnstiles" + File.separator + name + ".yml"));
        } catch (IOException ex) {
            TurnstileMain.logger.log(Level.SEVERE, "Could not save Turnstile " + name, ex);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map map = new TreeMap();
        map.put("Name", name);
        map.put("Owner", owner);

        Map nestedMap = new HashMap();
        nestedMap.put("World", world);
        nestedMap.put("x", x);
        nestedMap.put("y", y);
        nestedMap.put("z", z);
        map.put("Trendula", nestedMap);

        if (price != 0) {
            map.put("Price", price);
        }
        if (moneyEarned != 0) {
            map.put("MoneyEarned", moneyEarned);
        }

        if (!items.isEmpty()) {
            map.put("Items", items);
        }
        if (itemsEarned != 0) {
            map.put("ItemsEarned", itemsEarned);
        }

        map.put("OneWay", oneWay);
        map.put("NoFraud", noFraud);
        map.put("TimeOut", timeOut);
        map.put("PrivateWhileOnCooldown", privateWhileOnCooldown);
        map.put("AmountPerCooldown", amountPerCooldown);

        if (freeStart != 0 || freeEnd != 0) {
            nestedMap = new HashMap();
            nestedMap.put("Start", freeStart);
            nestedMap.put("End", freeEnd);
            map.put("Free", nestedMap);
        }

        if (lockedStart != 0 || lockedEnd != 0) {
            nestedMap = new HashMap();
            nestedMap.put("Start", lockedStart);
            nestedMap.put("End", lockedEnd);
            map.put("Locked", nestedMap);
        }

        map.put("Access", access);

        nestedMap = new HashMap();
        nestedMap.put("Days", days);
        nestedMap.put("Hours", hours);
        nestedMap.put("Minutes", minutes);
        nestedMap.put("Seconds", seconds);
        map.put("Cooldown", nestedMap);
        map.put("RoundDownTime", roundDown);

        map.put("Buttons", buttons);

        return map;
    }

    /**
     * Constructs a new Turnstile from a Configuration Serialized phase
     *
     * @param map The map of data values
     */
    public Turnstile(Map<String, Object> map) {
        String currentLine = null; //The value that is about to be loaded (used for debugging)
        try {
            current = name = (String) map.get(currentLine = "Name");
            owner = (String) map.get(currentLine = "Owner");

            Map nestedMap = (Map) map.get(currentLine = "Trendula");
            world = (String) nestedMap.get(currentLine = "World");
            x = (Integer) nestedMap.get(currentLine = "x");
            y = (Integer) nestedMap.get(currentLine = "y");
            z = (Integer) nestedMap.get(currentLine = "z");

            nestedMap = (Map) map.get(currentLine = "Reset");
            days = (Integer) nestedMap.get(currentLine = "Days");
            hours = (Integer) nestedMap.get(currentLine = "Hours");
            minutes = (Integer) nestedMap.get(currentLine = "Minutes");
            seconds = (Integer) nestedMap.get(currentLine = "Seconds");

            if (map.containsKey(currentLine = "Price")) {
                price = (Double) map.get(currentLine);
            }
            if (map.containsKey(currentLine = "MoneyEarned")) {
                moneyEarned = (Double) map.get(currentLine);
            }

            if (map.containsKey(currentLine = "Items")) {
                items = (ArrayList) map.get(currentLine);
            }
            if (map.containsKey(currentLine = "ItemsEarned")) {
                itemsEarned = (Integer) map.get(currentLine);
            }

            timeOut = (Integer) map.get(currentLine = "TimeOut");
            oneWay = (Boolean) map.get(currentLine = "OneWay");
            noFraud = (Boolean) map.get(currentLine = "NoFraud");
            privateWhileOnCooldown = (Boolean) map.get(currentLine = "PrivateWhileOnCooldown");
            amountPerCooldown = (Integer) map.get(currentLine = "AmountPerCooldown");

            if (map.containsKey(currentLine = "Free")) {
                nestedMap = (Map) map.get(currentLine);
                freeStart = (Integer) nestedMap.get(currentLine = "Start");
                freeEnd = (Integer) nestedMap.get(currentLine = "End");
            }

            if (map.containsKey(currentLine = "Locked")) {
                nestedMap = (Map) map.get(currentLine = "Locked");
                lockedStart = (Integer) nestedMap.get(currentLine = "Start");
                lockedEnd = (Integer) nestedMap.get(currentLine = "End");
            }

            access = (String) map.get(currentLine = "Access");
            buttons = (ArrayList) map.get(currentLine = "Buttons");
        } catch (Exception ex) {
            //Print debug messages
            TurnstileMain.logger.severe("Failed to load line: " + currentLine);
            TurnstileMain.logger.severe("of Turnstile: " + (current == null ? "unknown" : current));
            if (current == null) {
                TurnstileMain.logger.severe("Last successful load was...");
                TurnstileMain.logger.severe("Turnstile: " + (last == null ? "unknown" : last));
            }
        }
        last = current;
        current = null;
    }
}
