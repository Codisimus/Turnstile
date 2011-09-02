
package Turnstile;

import org.bukkit.event.server.ServerListener;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.register.payment.Methods;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Checks for plugins whenever one is enabled
 *
 */
public class PluginListener extends ServerListener {
    public PluginListener() { }
    private Methods methods = new Methods();
    protected static Boolean useOP;

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        if (!TurnstileMain.op) {
            if (TurnstileMain.permissions == null && !useOP) {
                Plugin permissions = TurnstileMain.pm.getPlugin("Permissions");
                if (permissions != null) {
                    TurnstileMain.permissions = ((Permissions)permissions).getHandler();
                    System.out.println("[Turnstile] Successfully linked with Permissions!");
                }
            }
        }
        if (Register.economy == null)
            System.err.println("[Turnstile] Config file outdated, Please regenerate");
        else if (!Register.economy.equalsIgnoreCase("none") && !methods.hasMethod()) {
            try {
                methods.setMethod(TurnstileMain.pm.getPlugin(Register.economy));
                if (methods.hasMethod()) {
                    Register.econ = methods.getMethod();
                    System.out.println("[Turnstile] Successfully linked with "+
                            Register.econ.getName()+" "+Register.econ.getVersion()+"!");
                }
            }
            catch (Exception e) {
            }
        }
//        if (TurnstileMain.TextPlayer == null) {
//            Plugin TextPlayer = TurnstileMain.pm.getPlugin("TextPlayer");
//            if (TextPlayer != null) {
//                TurnstileMain.TextPlayer = (TextPlayer)TextPlayer;
//                System.out.println("Turnstile Successfully linked with TextPlayer!");
//            }
//        }
    }
}