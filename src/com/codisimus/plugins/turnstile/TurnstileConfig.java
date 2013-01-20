package com.codisimus.plugins.turnstile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class TurnstileConfig {
    private static Properties p;

    public static void load() {
        //Load Config settings
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(TurnstileMain.dataFolder + "/config.properties");
            if (!file.exists()) {
                TurnstileMain.plugin.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(new InputStreamReader(fis, "UTF8"));

            Turnstile.debug = loadBool("Debug", false);

            TurnstileMain.enderpearlProtection = loadBool("UseCitizens", false);

            TurnstileMain.cost = loadInt("CostToMakeTurnstile", 0);

            TurnstileMain.citizens = loadBool("UseCitizens", false);

            TurnstileMain.defaultOneWay = loadBool("OneWayByDefault", true);
            TurnstileMain.defaultNoFraud = loadBool("NoFraudByDefault", false);
            TurnstileMain.defaultTimeOut = loadInt("DefaultAutoCloseTimer", 10);

            TurnstileMain.useOpenFreeNode = loadBool("use'openfree'node", false);
            TurnstileMain.useMakeFreeNode = loadBool("use'makefree'node", false);

            /* Messages */
            String string = "PLUGIN CONFIG MUST BE REGENERATED!";
            TurnstileMessages.permission = loadString("PermissionMessage", string);
            TurnstileMessages.locked = loadString("LockedMessage", string);
            TurnstileMessages.free = loadString("FreeMessage", string);
            TurnstileMessages.oneWay = loadString("OneWayMessage", string);
            TurnstileMessages.correct = loadString("CorrectItemMessage", string);
            TurnstileMessages.wrong = loadString("WrongItemMessage", string);
            TurnstileMessages.notEnoughMoney = loadString("NotEnoughMoneyMessage", string);
            TurnstileMessages.displayCost = loadString("DisplayCostMessage", string);
            TurnstileMessages.open = loadString("OpenMessage", string);
            TurnstileMessages.balanceCleared = loadString("BalanceClearedMessage", string);
            TurnstileMessages.privateTurnstile = loadString("PrivateMessage", string);
            TurnstileMessages.inUse = loadString("ChestInUseMessage", string);
            TurnstileMessages.occupied = loadString("TurnstileOccupiedMessage", string);
            TurnstileMessages.noFraud = loadString("NoFraudMessage", string);
            TurnstileMessages.cooldownPrivate = loadString("CooldownWhenPrivateMessage", string);
            TurnstileMessages.cooldown = loadString("CooldownMessage", string);
            TurnstileMessages.formatAll();
        } catch (Exception missingProp) {
            TurnstileMain.logger.severe("Failed to load Turnstile Config");
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private static String loadString(String key, String defaultString) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            TurnstileMain.logger.severe("Missing value for " + key);
            TurnstileMain.logger.severe("Please regenerate the config.properties file (delete the old file to allow a new one to be created)");
            TurnstileMain.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultString;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not an Integer
     *
     * @param key The key to be loaded
     * @return The Integer value of the loaded key
     */
    private static int loadInt(String key, int defaultValue) {
        String string = loadString(key, null);
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            TurnstileMain.logger.severe("The setting for " + key + " must be a valid integer");
            TurnstileMain.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not a boolean
     *
     * @param key The key to be loaded
     * @return The boolean value of the loaded key
     */
    private static boolean loadBool(String key, boolean defaultValue) {
        String string = loadString(key, null);
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception e) {
            TurnstileMain.logger.severe("The setting for " + key + " must be 'true' or 'false' ");
            TurnstileMain.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }
}
