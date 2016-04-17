package com.codisimus.plugins.turnstile;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Manages payment of using Turnstiles
 *
 * @author Codisimus
 */
public class Econ {
    public static Economy economy;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     *
     * @param player The name of the Player to be charged
     * @param owner The Player/Bank that will receive the money
     * @param amount The amount that will be charged
     * @return True if the transaction was successful
     */
    public static boolean charge(OfflinePlayer player, String owner, double amount) {
        if (Turnstile.debug) {
            TurnstileMain.logger.warning("Charge Debug: Sending "
                                + format(amount) + " to " + owner
                                + " from " + player);
        }

        //Cancel if the Player cannot afford the transaction
        if (economy.has(player, amount)) {
            economy.withdrawPlayer(player, amount);
        } else {
            if (Turnstile.debug) {
                TurnstileMain.logger.warning("Charge Debug: " + player
                        + " only has " + format(economy.getBalance(player)));
            }
            return false;
        }

        if (Turnstile.debug) {
            TurnstileMain.logger.warning("Charge Debug: " + format(amount)
                                + " has been taken from " + player);
            TurnstileMain.logger.warning("Charge Debug: " + player
                                + "'s new balance is "
                                + format(economy.getBalance(player)));
        }

        //Money does not go to anyone if the source is the server
        if (owner == null || owner.equalsIgnoreCase("server")) {
            if (Turnstile.debug) {
                TurnstileMain.logger.warning("Charge Debug: Turnstile is owned"
                        + "by the server so the charged money is destroyed");
            }
            return true;
        }

        if (owner.startsWith("bank:")) { //Send money to a bank account
            economy.bankDeposit(owner.substring(5), amount);
            if (Turnstile.debug) {
                TurnstileMain.logger.warning("Charge Debug: " + format(amount)
                                    + " has been given to bank "
                                    + owner.substring(5));
            }
        } else {
            //Send money to a Player
            economy.depositPlayer(Bukkit.getOfflinePlayer(owner), amount);
            if (Turnstile.debug) {
                TurnstileMain.logger.warning("Charge Debug: " + format(amount)
                                    + " has been given to " + owner);
                TurnstileMain.logger.warning("Charge Debug: " + owner
                                    + "'s new balance is "
                                    + format(economy.getBalance(owner)));
            }
        }

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

    /**
     * Retrieves the registered Economy plugin
     *
     * @return true if an Economy plugin has been found
     */
    public static boolean setupEconomy() {
        //Return if Vault is not enabled
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = (Economy) rsp.getProvider();
        return economy != null;
    }
}
