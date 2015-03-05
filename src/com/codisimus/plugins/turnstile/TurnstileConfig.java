package com.codisimus.plugins.turnstile;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class TurnstileConfig {
    public static int cost;
    public static boolean citizens;
    public static boolean enderpearlProtection;
    public static int defaultTimeOut;
    public static boolean defaultOneWay;
    public static boolean defaultNoFraud;
    public static String permission;
    public static String locked;
    public static String free;
    public static String oneWay;
    public static String correct;
    public static String wrong;
    public static String notEnoughMoney;
    public static String displayCost;
    public static String open;
    public static String balanceCleared;
    public static String privateTurnstile;
    public static String inUse;
    public static String occupied;
    public static String noFraud;
    public static String cooldownPrivate;
    public static String cooldown;

    public static void load() {
        FileConfiguration config = TurnstileMain.plugin.getConfig();

        //Check for an outdated config.yml file
        if (config.get("UseDamageTags", null) == null) {
            TurnstileMain.logger.warning("Your config.yml file is outdated! To get the most out of this plugin please (re)move the old file so a new one can be generated.");
        }


        /* MESSAGES */

        ConfigurationSection section = config.getConfigurationSection("Messages");
        permission = getString(section, "Permission");
        locked = getString(section, "Locked");
        free = getString(section, "Free");
        correct = getString(section, "CorrectItem");
        wrong = getString(section, "WrongItem");
        inUse = getString(section, "ChestInUse");
        occupied = getString(section, "TurnstileOccupied");
        displayCost = getString(section, "DisplayCost");
        open = getString(section, "Open");
        notEnoughMoney = getString(section, "NotEnoughMoney");
        balanceCleared = getString(section, "BalanceCleared");
        privateTurnstile = getString(section, "Private");
        oneWay = getString(section, "OneWay");
        noFraud = getString(section, "NoFraud");
        cooldownPrivate = getString(section, "CooldownWhenPrivate");
        cooldown = getString(section, "Cooldown");


        /* DEFAULTS */

        section = config.getConfigurationSection("Defaults");
        defaultOneWay = section.getBoolean("OneWay");
        defaultNoFraud = section.getBoolean("NoFraud");
        defaultTimeOut = section.getInt("AutoCloseTimer");


        /* OTHER */

        Turnstile.debug = config.getBoolean("Debug");
        enderpearlProtection = config.getBoolean("UseCitizens");
        cost = config.getInt("CostToMakeTurnstile");
        citizens = config.getBoolean("UseCitizens");
    }

    /**
     * Returns the converted string that is loaded from the given configuration
     * & will be converted to § where color codes are used
     *
     * @param config The given ConfigurationSection
     * @param key The key that leads to the requested string
     * @return The String or null if the string was not found or empty
     */
    private static String getString(ConfigurationSection config, String key) {
        String string = ChatColor.translateAlternateColorCodes('&', config.getString(key));
        return string.isEmpty() ? null : string;
    }
}
