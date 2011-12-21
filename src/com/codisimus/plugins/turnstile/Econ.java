package com.codisimus.plugins.turnstile;

import net.milkbowl.vault.economy.Economy;

/**
 * Manages payment/rewards of using Warps
 * 
 * @author Codisimus
 */
public class Econ {
    public static Economy economy;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param source The Player/Bank that will receive the money
     * @param amount The amount that will be charged
     * @return True if the transaction was successful
     */
    public static boolean charge(String player, String owner, double amount) {
        //Cancel if the Player cannot afford the transaction
        if (!economy.has(player, amount))
            return false;
        
        //Money does not go to anyone if the source is the server
        if (owner == null || owner.equalsIgnoreCase("server"))
            return true;
        
        if (owner.startsWith("bank:"))
            //Send money to a bank account
            economy.bankDeposit(owner.substring(5), amount);
        else
            //Send money to a Player
            economy.depositPlayer(owner, amount);
        
        return true;
    }
    
    /**
     * Formats the money amount by adding the unit
     * 
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return economy.format(amount).replace(".00", "");
    }
}
