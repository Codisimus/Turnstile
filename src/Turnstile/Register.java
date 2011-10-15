
package Turnstile;

import com.codisimus.turnstile.register.payment.Method;
import com.codisimus.turnstile.register.payment.Method.MethodAccount;

/**
 *
 * @author Codisimus
 */
public class Register {
    protected static String economy;
    protected static Method econ;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param owner The Player/Bank that will get the money
     * @param price The amount that will be charged
     * @return false if player doesn't have enough money
     */
    protected static boolean charge(String player, String owner, double price) {
        MethodAccount account = econ.getAccount(player);
        
        if (!account.hasEnough(price))
            return false;
        
        account.subtract(price);
        
        if (owner == null)
            return true;
        
        if (owner.equalsIgnoreCase("server"))
            return true;
        
        if (owner.startsWith("bank:"))
            econ.getBankAccount(owner.substring(5), null).add(price);
        else
            econ.getAccount(owner).add(price);
        return true;
    }

    /**
     * Clears a Players bank account balance
     * 
     * @param player The name of the Player
     */
    protected static void clearBalance(String player) {
        econ.getAccount(player).set(0);
    }

    /**
     * returns Boolean value of whether the Player is an owner of the given Bank
     * 
     * @param bank The name of the Bank
     * @param player The name of the Player
     * @return true if the Player is an owner
     */
    protected static boolean isBankOwner(String bank, String player) {
        return econ.hasBankAccount(bank, player);
    }
}
