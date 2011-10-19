package com.codisimus.plugins.turnstile;

import com.codisimus.plugins.turnstile.register.payment.Method;
import com.codisimus.plugins.turnstile.register.payment.Method.MethodAccount;

/**
 * Manages payment for buying and selling Chunks
 * Using Nijikokun's Register API
 * 
 * @author Codisimus
 */
public class Register {
    public static String economy;
    public static Method econ;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param owner The Player/Bank that will get the money
     * @param price The amount that will be charged
     * @return false if player doesn't have enough money
     */
    public static boolean charge(String player, String owner, double price) {
        MethodAccount account = econ.getAccount(player);
        
        //Cancel if the Player can not afford the transaction
        if (!account.hasEnough(price))
            return false;
        
        account.subtract(price);
        
        //Money does not go to anyone if the owner is null or the server
        if (owner == null || owner.equalsIgnoreCase("server"))
            return true;
        
        if (owner.startsWith("bank:"))
            //Send money to a bank account
            econ.getBankAccount(owner.substring(5), null).add(price);
        else
            //Send money to a Player
            econ.getAccount(owner).add(price);
        
        return true;
    }

    /**
     * Clears a Players bank account balance
     * 
     * @param player The name of the Player
     */
    public static void clearBalance(String player) {
        econ.getAccount(player).set(0);
    }

    /**
     * returns Boolean value of whether the Player is an owner of the given Bank
     * 
     * @param bank The name of the Bank
     * @param player The name of the Player
     * @return true if the Player is an owner
     */
    public static boolean isBankOwner(String bank, String player) {
        return econ.hasBankAccount(bank, player);
    }
    
    /**
     * Formats the money amount by adding the unit
     *
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return econ.format(amount);
    }
}
