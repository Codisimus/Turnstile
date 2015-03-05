package com.codisimus.plugins.turnstile;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Codisimus
 */
public class Item implements ConfigurationSerializable {
    private int id = 0;
    private String name;
    private short durability = -1;
    private int amount = 1;
    private LinkedList<Enchantment> enchantments = new LinkedList<Enchantment>();

    /**
     *
     * @param id The Material id of the item
     * @param durability The durability of the item
     * @param amount The stack size of the item
     */
    public Item(int id, short durability, int amount) {
        this.id = id;
        this.durability = durability;
        this.amount = amount;
    }

    /**
     *
     * @param id The Material id of the item
     * @param enchantments The enchantments on the item
     * @param amount The stack size of the item
     */
    public Item(int id, LinkedList<Enchantment> enchantments, int amount) {
        this.id = id;
        this.enchantments = enchantments;
        this.amount = amount;
    }

    /**
     *
     * @param item The ItemStack that will be looted
     */
    public Item(ItemStack item) {
        id = item.getTypeId();
        amount = item.getAmount();
        enchantments.addAll(item.getEnchantments().keySet());

        if (enchantments.isEmpty()) {
            durability = item.getDurability();
        }
    }

    /**
     * Constructs a new Item from a Configuration Serialized phase
     *
     * @param map The map of data values
     */
    public Item(Map<String, Object> map) {
        id = (Integer) map.get("MaterialID");
        name = (String) map.get("DisplayName");
        durability = (Short) map.get("Durability");
        amount = (Integer) map.get("Amount");
        for (String string : (ArrayList<String>) map.get("AutoEnchant")) {
            enchantments.add(Enchantment.getByName(string));
        }
    }

    public LinkedList<Integer> findItem(Inventory inventory) {
        LinkedList<Integer> flagForDelete = new LinkedList<Integer>();
        int total = 0;

        //Iterate through the contents of the Chest to look for matching items
        ItemStack[] stacks = inventory.getContents();
        for (int i=0; i < stacks.length; i++) {
            if (stacks[i] != null) {
                if (stacks[i].getTypeId() == id) {
                    if (!enchantments.isEmpty()) {
                        //Make sure all enchantments are present
                        boolean missingEnchantment = false;

                        for (Enchantment enchantment: enchantments) {
                            if (!stacks[i].containsEnchantment(enchantment)) {
                                missingEnchantment = true;
                                break;
                            }
                        }

                        if (!missingEnchantment) {
                            total = total + stacks[i].getAmount();
                            flagForDelete.add(i);
                        }
                    } else if (durability == -1 || stacks[i].getDurability() == durability) {
                        total = total + stacks[i].getAmount();
                        flagForDelete.add(i);
                    }
                }
            }

            //Check if enough items were found
            if (total >= amount) {
                return flagForDelete;
            }
        }

        //Not enough items were found
        return null;
    }

    public ItemStack getItem() {
        ItemStack item = new ItemStack(id, amount);

        if (!enchantments.isEmpty()) {
            for (Enchantment enchantment: enchantments) {
                item.addEnchantment(enchantment, enchantment.getStartLevel());
            }
        } else if (durability == -1) {
            item.setDurability(durability);
        }

        return item;
    }

    /**
     * Returns the Enchantments of this Item as a String
     * This String is user friendly
     *
     * @return The String representation of this Item's Enchantments
     */
    public String enchantmentsToString() {
        String string = "";
        for (Enchantment enchantment : enchantments) {
            string = string.concat("&"+enchantment.getName());
        }
        return string.isEmpty() ? string : string.substring(1);
    }

    /**
     * Returns the Enchantments of this Item as a List of Strings
     *
     * @return The String representation of this Item's Enchantments
     */
    public ArrayList<String> enchantmentsToStrings() {
        ArrayList<String> stringList = new ArrayList<String>();
        for (Enchantment enchantment : enchantments) {
            stringList.add(enchantment.getName());
        }
        return stringList;
    }

    /**
     * Returns the info of this Item as a String
     *
     * @return The String representation of this Loot
     */
    public String toInfoString() {
        return amount + " of " + Material.getMaterial(id).name()
                + (!enchantments.isEmpty()
                   ? " with enchantments " + enchantmentsToString()
                   : (durability > 0 ? " with data " + durability : ""));
    }

    /**
     * Returns the String representation of this Item
     * The format for a Loot with Enchantments is MaterialID'Enchantment1(level)&Enchantment2(level)...'Amount
     * The format for a Loot without Enchantments is MaterialID'Durability'Amount
     *
     * @return The String representation of this Item
     */
    @Override
    public String toString() {
        return id + "'"
               + (name != null
                  ? name
                  : (enchantments.isEmpty()
                     ? String.valueOf(durability)
                     : enchantmentsToString()))
               + "'" + amount;
    }

    @Override
    public Map<String, Object> serialize() {
        Map map = new TreeMap();
        map.put("MaterialID", id);
        map.put("DisplayName", name);
        map.put("Durability", durability);
        map.put("Amount", amount);
        map.put("Enchantments", enchantmentsToStrings());
        return map;
    }
}
