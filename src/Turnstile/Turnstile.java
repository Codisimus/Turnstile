
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
import org.bukkit.material.Door;

/**
 * A Turnstile is a fence used as a gate.
 * @author Codisimus
 */
public class Turnstile {
    protected String name;
    protected String access = "public";
    protected double earned = 0;
    protected double price = 0;
    protected String item = "0";
    protected int amount = 0;
    protected boolean open = false;
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
        //Check for door material
        if (TurnstileMain.isDoor(gate.getType())) {
            Door door = (Door)gate.getState().getData();
            //Swing door shut
            if (door.isOpen()) {
                Block neighbor;
                if (door.isTopHalf())
                    neighbor = gate.getRelative(BlockFace.DOWN);
                else
                    neighbor = gate.getRelative(BlockFace.UP);
                gate.setData((byte)(gate.getState().getData().getData()^4));
                neighbor.setData((byte)(neighbor.getState().getData().getData()^4));
            }
        }
        else
            gate.setType(Material.FENCE);
    }

    /**
     * Constructs a new Turnstile
     * 
     * @param name The name of the Turnstile which cannot already exist
     * @param creator The player who is creating the Turnstile and also the default owner
     */
    public Turnstile (String name, Player creator) {
        this.name = name;
        this.owner = creator.getName();
        gate = creator.getTargetBlock(null, 100);
    }

    /**
     * Checks the contents of the Chest and compares it to the ItemID in price
     * 
     * @param chest The Chest being activated
     * @param player The Player who activated the Chest
     */
    public void checkContents(Chest chest, Player player) {
        //Cancels the event if the Turnstile is already open
        if (open)
            return;
        if (!hasAccess(player))
            return;
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
                    for (int stack : flagForDelete) {
                        inventory.clear(stack);
                    }
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
            open();
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
        if (!hasAccess(player))
            return false;
        if (!TurnstileMain.useMakeFreeNode || !TurnstileMain.hasPermission(player, "openfree")) {
            if (price > 0)
                if (!Register.charge(player.getName(), owner, price)) {
                    player.sendMessage(TurnstileMain.notEnoughMoney);
                    return false;
                }
                else {
                    String msg = TurnstileMain.open.replaceAll("<price>", ""+price);
                    player.sendMessage(msg);
                    earned = earned + price;
                    TextPlayer TextPlayer = TurnstileMain.textPlayer;
                    if (TextPlayer != null)
                        TextPlayer.sendMsg(null, TextPlayer.getUser(owner), earned+"");
                }
            else if (price == -411) {
                Register.clearBalance(player.getName());
                player.sendMessage(TurnstileMain.balanceCleared);
            }
        }
        return true;
    }
    
    /**
     * Checks access for private or a specific group
     * Returns whether the player has access
     * 
     * @param player The Player who is activating the Button
     * @return true if the play has access rights
     */
    private boolean hasAccess(Player player) {
        if (access.equals("private")) {
            if (!isOwner(player)) {
                player.sendMessage(TurnstileMain.privateTurnstile);
                return false;
            }
        }
        if (!access.equals("public")) {
            String world = player.getWorld().getName();
            if (!TurnstileMain.permissions.inGroup(world, player.getName(), access)) {
                player.sendMessage(TurnstileMain.privateTurnstile);
                return false;
            }
        }
        return true;
    }

    /**
     * Opens the Turnstile
     *
     */
    public void open() {
        open = true;
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
            else {
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
        if (lockedStart < lockedEnd)
            if (lockedStart < time && time < lockedEnd)
                return true;
        else if (lockedStart > lockedEnd)
            if (lockedStart < time || time < lockedEnd)
                return true;
        return false;
    }

    /**
     * Returns true if the Turnstile is currently free
     * 
     * @param time The current Minecraft Server time
     * @return true if the Turnstile is currently free
     */
    public boolean isFree(long time) {
        if (freeStart < freeEnd)
            if (freeStart < time && time < freeEnd)
                return true;
        else if (freeStart > freeEnd)
            if (freeStart < time || time < freeEnd)
                return true;
        return false;
    }

    /**
     * Compares player to owner to see if they match
     * Checks Bank owners as well
     * 
     * @param player The player who is using the Turnstile command
     * @return true if the player is an owner
     */
    protected boolean isOwner(Player player) {
        if (player.getName().equalsIgnoreCase(owner))
            return true;
        if (owner.substring(0, 5).equalsIgnoreCase("bank:"))
            if (Register.isBankOwner(owner.substring(5), player.getName()))
                return true;
        if (TurnstileMain.hasPermission(player, "admin.ignoreowner"))
            return true;
        return false;
    }
}
