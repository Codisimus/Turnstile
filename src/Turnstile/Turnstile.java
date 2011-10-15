
package Turnstile;

import TextPlayer.TextPlayer;
import java.util.LinkedList;
import org.bukkit.Material;
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
 * A Turnstile is a fence used as a gate.
 * @author Codisimus
 */
public class Turnstile {
    protected static boolean oneWay;
    protected String name;
    protected String access = "public";
    protected double earned = 0;
    protected double price = 0;
    protected String item = "0";
    protected int amount = 0;
    protected boolean open = false;
    private int openedFrom;
    protected String owner;
    protected Block gate;
    protected LinkedList<Block> buttons = new LinkedList<Block>();
    private int instance = 0;
    protected long freeStart = 0;
    protected long freeEnd = 0;
    protected long lockedStart = 0;
    protected long lockedEnd = 0;


    /**
     * Creates a Turnstile from the save file.
     * 
     * @param name The name of the Turnstile
     * @param gate The block of the Turnstile
     * @param buttons All buttons linked with the Turnstile
     * @param price The cost of activating the Turnstile
     * @param earned The amount the Turnstile earned
     * @param item The ID of the item needed to activate the Turnstile
     * @param amount The amount of the item needed to activate the Turnstile
     * @param owner The Player/Bank that receives money from the Turnstile
     * @param access The type of access ex. private, public, admin
     * @param lockedStart The start time of the Turnstile being locked
     * @param lockedEnd The end time of the Turnstile being locked
     * @param freeStart The start time of the Turnstile being free
     * @param freeEnd The end time of the Turnstile being free
     */
    public Turnstile (String name, Block gate, LinkedList<Block> buttons,
            double price, double earned, String item, int amount, String owner, String access,
            long lockedStart, long lockedEnd, long freeStart, long freeEnd) {
        this.name = name;
        this.gate = gate;
        this.buttons = buttons;
        this.price = price;
        this.earned = earned;
        this.item = item;
        this.amount = amount;
        this.owner = owner;
        this.access = access;
        this.lockedStart = lockedStart;
        this.lockedEnd = lockedEnd;
        this.freeStart = freeStart;
        this.freeEnd = freeEnd;
        
        //Make sure gate is present
        if (gate.getTypeId() == 0)
            gate.setType(Material.FENCE);
        
        if (gate.getTypeId() == 85)
            return;
        
        Door door = (Door)gate.getState().getData();
        
        //Swing door shut
        if (!door.isOpen())
            return;
        
        Block neighbor;
        if (door.isTopHalf())
            neighbor = gate.getRelative(BlockFace.DOWN);
        else
            neighbor = gate.getRelative(BlockFace.UP);
        gate.setData((byte)(gate.getState().getData().getData()^4));
        neighbor.setData((byte)(neighbor.getState().getData().getData()^4));
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
     * Checks if Player is attempting to enter Turnstile backwards
     * 
     * @param chest The Chest being activated
     * @return false if Player is attempting to enter Turnstile backwards
     */
    public boolean checkOneWay(Block from) {
        switch (openedFrom) {
            case 0:
                return from.getX() < gate.getX();
            case 1:
                return from.getX() > gate.getX();
            case 2:
                return from.getZ() < gate.getZ();
            case 3:
                return from.getZ() > gate.getZ();
            default:
                return true;
        }
    }

    /**
     * Checks the contents of the Chest and compares it to the ItemID in price
     * 
     * @param chest The Chest being activated
     * @param player The Player who activated the Chest
     */
    public void checkContents(Chest chest, Player player) {
        Inventory inventory = chest.getInventory();
        boolean flagForOpen = false;
        if (item.contains(".")) {
            int index = item.indexOf('.');
            int itemType = Integer.parseInt(item.substring(0, index));
            short itemDamage = Short.parseShort(item.substring(index+1));
            ItemStack[] stacks = inventory.getContents();
            int total = 0;
            LinkedList<Integer> flagForDelete = new LinkedList<Integer>();
            for (int i=0; i < stacks.length; i++) {
                if (stacks[i] != null)
                    if (stacks[i].getTypeId() == itemType)
                        if (stacks[i].getDurability() == itemDamage) {
                            total = total + stacks[i].getAmount();
                            flagForDelete.add(i);
                        }
                if (total >= amount) {
                    for (int stack : flagForDelete)
                        inventory.clear(stack);
                    flagForOpen = true;
                    break;
                }
            }
        }
        else if (inventory.contains(Integer.parseInt(item), amount)) {
            inventory.remove(Integer.parseInt(item));
            flagForOpen = true;
        }
        if (flagForOpen) {
            player.sendMessage(TurnstileMain.correct);
            open(chest.getBlock());
            earned++;
        }
        else
            player.sendMessage(TurnstileMain.wrong);
    }

    /**
     * Checks the account balance of the player and opens turnstile if there is enough
     * Also checks access for private or a specific group
     * 
     * @param player The Player who is activating the Button
     */
    public boolean checkBalance(Player player) {
        if (TurnstileMain.useOpenFreeNode && TurnstileMain.hasPermission(player, "openfree"))
            return true;
        if (price == -411) {
            Register.clearBalance(player.getName());
            player.sendMessage(TurnstileMain.balanceCleared);
            return true;
        }
        if (price <= 0)
            return true;
        if (!Register.charge(player.getName(), owner, price)) {
            player.sendMessage(TurnstileMain.notEnoughMoney);
            return false;
        }
        String msg = TurnstileMain.open.replaceAll("<price>", ""+price);
        player.sendMessage(msg);
        earned = earned + price;
        TextPlayer TextPlayer = TurnstileMain.textPlayer;
        if (TextPlayer != null)
            TextPlayer.sendMsg(null, TextPlayer.getUser(owner), earned+"");
        return true;
    }
    
    /**
     * Checks access for private or a specific group
     * Returns whether the player has access
     * 
     * @param player The Player who is activating the Button
     * @return true if the play has access rights
     */
    protected boolean hasAccess(Player player) {
        //Return if Turnstile is public
        if (access.equals("public"))
            return true;
        
        if (access.equals("private"))
            if (!isOwner(player)) {
                player.sendMessage(TurnstileMain.privateTurnstile);
                return false;
            }
        else if (!TurnstileMain.permissions.getUser(player).inGroup(access)) {
            player.sendMessage(TurnstileMain.privateTurnstile);
            return false;
        }
        
        return true;
    }

    /**
     * Opens the Turnstile
     *
     */
    public void open(Block block) {
        open = true;
        setOpenedFrom(block);
        TurnstilePlayerListener.openTurnstiles.add(this);
        
        //Starts a new thread
        Thread thread = new Thread() {
            @Override
            public void run() {
                //Check for door material
                if (TurnstileMain.isDoor(gate.getType())) {
                    Door door = (Door)gate.getState().getData();
                    Block neighbor;
                    
                    if (door.isTopHalf())
                        neighbor = gate.getRelative(BlockFace.DOWN);
                    else
                        neighbor = gate.getRelative(BlockFace.UP);
                    
                    //Swing door open
                    if (!door.isOpen()) {
                        gate.setData((byte)(gate.getState().getData().getData()^4));
                        neighbor.setData((byte)(neighbor.getState().getData().getData()^4));
                    }
                }
                else
                    gate.setType(Material.AIR);
                
                if (TurnstileMain.timeOut == 0)
                    return;
                instance++;
                int temp = instance;
                
                //Leaves gate open for specific amount of time
                try {
                    Thread.currentThread().sleep(TurnstileMain.timeOut * 1000);
                }
                catch (InterruptedException ex) {
                }
                
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
        //Check for door material
        if (TurnstileMain.isDoor(gate.getType())) {
            Door door = (Door)gate.getState().getData();
            Block neighbor;
            
            if (door.isTopHalf())
                neighbor = gate.getRelative(BlockFace.DOWN);
            else
                neighbor = gate.getRelative(BlockFace.UP);
            
            //Swing door shut
            if (door.isOpen()) {
                gate.setData((byte)(gate.getState().getData().getData()^4));
                neighbor.setData((byte)(neighbor.getState().getData().getData()^4));
            }
        }
        else
            gate.setType(Material.FENCE);
        
        open = false;
        TurnstilePlayerListener.openTurnstiles.remove(this);
    }
    
    /**
     * Gives earned items to Player if they are an owner
     * 
     * @param player The Player who is collecting the items
     */
    public void collect(Player player) {
        PlayerInventory sack = player.getInventory();
        
        //Loops unless inventory is full
        while (sack.firstEmpty() >= 0) {
            //Check for additional items that were earned
            if (earned <= 0) {
                player.sendMessage("There are no more items to collect");
                return;
            }
            int itemType = Integer.parseInt(item);
            short itemDamage = 0;
            if (item.contains(".")) {
                int index = item.indexOf('.');
                itemType = Integer.parseInt(item.substring(0, index));
                itemDamage = Short.parseShort(item.substring(index+1));
            }
            ItemStack itemStack = new ItemStack(itemType, amount);
            if (itemDamage != 0)
                itemStack.setDurability(itemDamage);
            sack.setItem(sack.firstEmpty(), itemStack);
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
    protected boolean isOwner(Player player) {
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
    protected void setOpenedFrom(Block block) {
        //Return is Turnstiles are not one way
        if (!oneWay)
            return;
        
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
