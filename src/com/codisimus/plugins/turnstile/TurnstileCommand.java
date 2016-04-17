package com.codisimus.plugins.turnstile;

import com.codisimus.plugins.turnstile.CommandHandler.CodCommand;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Executes Player Commands
 *
 * @author Codisimus
 */
public class TurnstileCommand {
    private static final EnumSet<Material> SEE_THROUGH = EnumSet.of(Material.AIR);

    @CodCommand(
        command = "make",
        weight = 1,
        aliases = {"create", "new"},
        usage = {
            "§2<command> <Name>§b Make target Block into a Turnstile"
        },
        permission = "turnstile.make"
    )
    public boolean make(Player player, String name) {
        //Cancel if the Turnstile already exists
        if (TurnstileMain.findTurnstile(name) != null) {
            player.sendMessage("§4A Turnstile named §6" + name + "§4 already exists.");
            return true;
        }

        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        switch (block.getType()) {
        case FENCE:
        case TRAP_DOOR:
        case FENCE_GATE:
            break;

        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case WOOD_DOOR:
        case WOODEN_DOOR:
            block = TurnstileUtil.getBottomHalf(block);
            break;

        default:
            player.sendMessage("You must target a Door or Fence.");
            return true;
        }

        int price = TurnstileConfig.cost;
        if (price > 0 && (!player.hasPermission("makefree"))) {
            //Cancel if the Player could not afford it
            if (!Econ.charge(player, null, price)) {
                player.sendMessage("You do not have enough money to make the Turnstile");
                return true;
            }

            player.sendMessage(price + " deducted,");
        }

        Turnstile turnstile = new Turnstile(name, player.getName(), block);
        TurnstileMain.addTurnstile(turnstile);
        player.sendMessage("Turnstile " + name + " made!");
        return true;
    }

    @CodCommand(
        command = "rename",
        weight = 2,
        aliases = {"setname"},
        usage = {
            "§2<command> [Name] <NewName>§b Rename a Turnstile"
        },
        permission = "turnstile.make"
    )
    public boolean rename(Player player, String name) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return rename(player, turnstile, name);
        }
    }
    @CodCommand(command = "rename", weight = 2.1)
    public boolean rename(CommandSender sender, Turnstile turnstile, String name) {
        Turnstile t = TurnstileMain.findTurnstile(name);

        if (t != null) {
            sender.sendMessage("Turnstile " + name + " already exists.");
            return true;
        }

        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile owner can do that.");
            return true;
        }

        sender.sendMessage("Turnstile " + turnstile.name + " renamed to " + name + ".");
        TurnstileMain.removeTurnstile(turnstile);
        turnstile.name = name;
        TurnstileMain.addTurnstile(turnstile);
        return true;
    }

    @CodCommand(
        command = "owner",
        weight = 3,
        aliases = {"setowner"},
        usage = {
            "§2<command> [Name] <Player>§b Set the Owner of the Turnstile"
        },
        permission = "turnstile.set.owner"
    )
    public boolean owner(Player player, OfflinePlayer newOwner) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return owner(player, turnstile, newOwner);
        }
    }
    @CodCommand(command = "owner", weight = 3.1)
    public boolean owner(CommandSender sender, Turnstile turnstile, OfflinePlayer newOwner) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.owner = newOwner.getName();
        sender.sendMessage("Money from Turnstile " + turnstile.name
                            + " will go to " + newOwner.getName() + "!");
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "bank",
        weight = 4,
        aliases = {"setbank"},
        usage = {
            "§2<command> [Name] <Player>§b Set the Owner of the Turnstile to a Bank"
        },
        permission = "turnstile.set.bank"
    )
    public boolean bank(Player player, String bankName) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return bank(player, turnstile, bankName);
        }
    }
    @CodCommand(command = "bank", weight = 4.1)
    public boolean bank(CommandSender sender, Turnstile turnstile, String bankName) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.owner = "bank:" + bankName;
        sender.sendMessage("Money from Turnstile " + turnstile.name
                            + " will go to " + bankName + "!");
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "link",
        weight = 10,
        aliases = {"+", "button", "chest"},
        usage = {
            "§2<command> <Name>§b Link target Block with Turnstile"
        },
        permission = "turnstile.make"
    )
    public boolean link(Player player, Turnstile turnstile) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        switch (block.getType()) {
        case CHEST:
        case STONE_PLATE:
        case WOOD_PLATE:
        case STONE_BUTTON:
        case WOOD_BUTTON:
            turnstile.buttons.add(new TurnstileButton(block));
            break;

        default:
            player.sendMessage("You must link the Turnstile to a Button,"
                                + "Chest, or Pressure plate");
            return true;
        }

        player.sendMessage("Succesfully linked to Turnstile " + turnstile.name + "!");
        turnstile.save();
        return true;
    }

    @CommandHandler.CodCommand(
        command = "link",
        subcommand = "npc",
        weight = 11,
        usage = {
            "§2<command> <Name>§b Link selected NPC with specified Turnstile"
        },
        permission = "turnstile.link.npc"
    )
    public void linkNPC(CommandSender sender, Turnstile turnstile) {
        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        if (npc == null) {
            sender.sendMessage("You do not have an NPC selected");
            return;
        }

        turnstile.buttons.add(new TurnstileButton(npc.getStoredLocation().getBlock()));

        sender.sendMessage(npc.getName() + " succesfully linked to Turnstile " + turnstile.name + "!");
        turnstile.save();
    }

    @CodCommand(
        command = "unlink",
        weight = 12,
        aliases = {"-"},
        usage = {
            "§2<command>§b Unlink target Block with Turnstile"
        },
        permission = "turnstile.make"
    )
    public boolean unlink(Player player) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);

        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            player.sendMessage("Target Block is not linked to a Turnstile.");
            return true;
        }

        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        Iterator itr = turnstile.buttons.iterator();
        while (itr.hasNext()) {
            TurnstileButton button = (TurnstileButton) itr.next();
            if (button.matchesBlock(block)) {
                itr.remove();
            }
        }

        player.sendMessage("Sucessfully unlinked!");
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "delete",
        weight = 13,
        aliases = {"destroy", "remove"},
        usage = {
            "§2<command> [Name]§b Delete Turnstile"
        },
        permission = "turnstile.make"
    )
    public boolean delete(CommandSender sender, Turnstile turnstile) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        TurnstileMain.removeTurnstile(turnstile);
        sender.sendMessage("Turnstile " + turnstile.name + " was deleted!");
        return true;
    }

    @CodCommand(
        command = "list",
        weight = 20,
        aliases = {"all"},
        usage = {
            "§2<command>§b List all Turnstiles"
        },
        permission = "turnstile.list"
    )
    public boolean list(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Turnstile turnstile : TurnstileMain.getTurnstiles()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(turnstile.name);
        }
        sb.insert(0, "Current Turnstiles: ");
        sender.sendMessage(sb.toString());
        return true;
    }

    @CodCommand(
        command = "info",
        weight = 21,
        aliases = {"name"},
        usage = {
            "§2<command> [Name]§b Display info of Turnstile"
        },
        permission = "turnstile.info"
    )
    public boolean info(Player player, String name) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return info(player, turnstile, name);
        }
    }
    @CodCommand(command = "info", weight = 21.1)
    public boolean info(CommandSender sender, Turnstile turnstile, String name) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        sender.sendMessage("Turnstile Info");
        sender.sendMessage("Name: " + turnstile.name);
        sender.sendMessage("Owner: " + turnstile.owner);
        sender.sendMessage("Location: " + turnstile.world + "'" + turnstile.x
                            + "'" + turnstile.y + "'" + turnstile.z);
        sender.sendMessage("Items: " + turnstile.itemsToInfoString());

        //Only display if an Economy plugin is present
        if (Econ.economy != null) {
            sender.sendMessage("Price: " + Econ.format(turnstile.price)
                                + ", NoFraud: " + turnstile.noFraud);
            sender.sendMessage("MoneyEarned: " + turnstile.moneyEarned
                                + ", ItemsEarned: " + turnstile.itemsEarned);
            sender.sendMessage("Free: " + turnstile.freeStart
                                + "-" + turnstile.freeEnd);
        }

        sender.sendMessage("Locked: " + turnstile.lockedStart
                            + "-" + turnstile.lockedEnd);
        sender.sendMessage("NoFraud: " + turnstile.noFraud);

        if (turnstile.access == null) {
            sender.sendMessage("Access: Public");
        } else if (turnstile.access.isEmpty()) {
            sender.sendMessage("Access: Private");
        } else {
            sender.sendMessage("Access: " + turnstile.access);
        }

        String buttons = "Buttons:  ";
        for (TurnstileButton button: turnstile.buttons) {
            buttons += button.toString() + ", ";
        }

        sender.sendMessage(buttons.substring(0, buttons.length() - 2));
        return true;
    }

    @CodCommand(
        command = "reload",
        weight = 22,
        aliases = {"rl"},
        usage = {
            "§2<command>§b Reload Turnstile Plugin"
        },
        permission = "turnstile.rl"
    )
    public boolean reload(CommandSender sender) {
        TurnstileMain.rl(sender);
        return true;
    }

    @CodCommand(
        command = "collect",
        weight = 23,
        usage = {
            "§2<command> [Name]§b Retrieve items from the target Turnstile chest"
        },
        permission = "turnstile.collect"
    )
    public boolean collect(Player player) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);

        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            player.sendMessage("You must target the Turnstile Chest you wish to collect from.");
            return true;
        }

        if (!turnstile.isOwner(player)) {
            player.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.collect(player);
        return true;
    }

    @CodCommand(
        command = "sign",
        weight = 24,
        usage = {
            "§e     Turnstile Sign Help Page:",
            "§2Turnstile Signs can automatically update information",
            "§2Each Sign can display two pieces of information such as:",
            "§2Name:§b The name of the Turnstile",
            "§2Price:§b The amount of money to use the Turnstile",
            "§2Cost:§b The item cost to use the Turnstile",
            "§2Counter:§b The amount of Players who used the Turnstile",
            "§2Money:§b The amount of money the Turnstile has earned",
            "§2Items:§b The amount of items the Turnstile has earned",
            "§2Access:§b Whether the Turnstile is public or private",
            "§2Status:§b Whether the Turnstile is open, free, or locked",
            "§2Turnstile Signs are created using the following format:",
            "§b    ts link",
            "§b  <Turnstile Name>",
            "§b<Information type 1>",
            "§b<Information type 2>"
        },
        permission = "turnstile.sign"
    )
    public boolean sign(CommandSender sender) {
        return false;
    }


    @CodCommand(
        command = "access",
        weight = 30,
        usage = {
            "§2<command> [Name] public§b Allow anyone to open",
            "§2<command> [Name] private§b Allow no one to open",
            "§2<command> [Name] <Player>§b Allow a specific player to open",
            "§2<command> [Name] <Node>§b Require permission to open",
        },
        permission = "turnstile.set.access"
    )
    public boolean access(Player player, String node) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return access(player, turnstile, node);
        }
    }
    @CodCommand(command = "access", weight = 30.1)
    public boolean access(CommandSender sender, Turnstile turnstile, String node) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.access = node;
        sender.sendMessage("Access to Turnstile " + turnstile.name
                            + " has been set to " + node + "!");
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "free",
        weight = 40,
        usage = {
            "§2<command> [Name] <StartTick> <EndTick>§b Free during timespan"
        },
        permission = "turnstile.set.free"
    )
    public boolean free(Player player, int start, int end) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return free(player, turnstile, start, end);
        }
    }
    @CodCommand(command = "free", weight = 40.1)
    public boolean free(CommandSender sender, Turnstile turnstile, int start, int end) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.lockedStart = start;
        turnstile.lockedEnd = end;

        sender.sendMessage("Turnstile " + turnstile.name
                            + " is free to use from " + start
                            + " to " + end + "!");
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "locked",
        weight = 41,
        usage = {
            "§2<command> [Name] <StartTick> <EndTick>§b Locked during timespan"
        },
        permission = "turnstile.set.locked"
    )
    public boolean locked(Player player, int start, int end) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return locked(player, turnstile, start, end);
        }
    }
    @CodCommand(command = "locked", weight = 41.1)
    public boolean locked(CommandSender sender, Turnstile turnstile, int start, int end) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.lockedStart = start;
        turnstile.lockedEnd = end;

        sender.sendMessage("Turnstile " + turnstile.name + " is locked from "
                            + start + " to " + end + "!");
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "cooldown",
        weight = 42,
        usage = {
            "§2<command> [Name]§b Set the cooldown options"
        },
        permission = "turnstile.set.cooldown"
    )
    public boolean cooldown(Player player) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return cooldown(player, turnstile);
        }
    }
    @CodCommand(command = "cooldown", weight = 42.1)
    public boolean cooldown(CommandSender sender, Turnstile turnstile) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        new CooldownConvo(sender, turnstile);
        return true;
    }

    @CodCommand(
        command = "nofraud",
        weight = 43,
        usage = {
            "§2<command> <true|false>§b Set noFraud mode"
        },
        permission = "turnstile.set.nofraud"
    )
    public boolean noFraud(Player player, boolean bool) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return noFraud(player, turnstile, bool);
        }
    }
    @CodCommand(command = "cooldown", weight = 42.1)
    public boolean noFraud(CommandSender sender, Turnstile turnstile, boolean bool) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        if (turnstile.noFraud) {
            if (bool) {
                sender.sendMessage("Turnstile " + turnstile.name + " is already in NoFraud mode.");
            } else {
                sender.sendMessage("Turnstile " + turnstile.name + " is no longer set to NoFraud mode.");
            }
        } else {
            if (bool) {
                sender.sendMessage("Turnstile " + turnstile.name + " set to NoFraud mode.");
            } else {
                sender.sendMessage("Turnstile " + turnstile.name + " is not set to NoFraud mode.");
            }
        }

        turnstile.noFraud = bool;
        turnstile.save();
        return true;
    }

    @CodCommand(
        command = "price",
        weight = 50,
        aliases = {"cost"},
        usage = {
            "§2<command> [Name] <Amount>§b Set the cost of the Turnstile"
        },
        permission = "turnstile.set.price"
    )
    public boolean price(Player player) {
        Block block = player.getTargetBlock(SEE_THROUGH, 10);
        Turnstile turnstile = TurnstileMain.findTurnstile(block);
        if (turnstile == null) {
            return false;
        } else {
            return cooldown(player, turnstile);
        }
    }
    @CodCommand(command = "price", weight = 50.1)
    public boolean price(CommandSender sender, Turnstile turnstile, int price) {
        if (!turnstile.isOwner(sender)) {
            sender.sendMessage("Only the Turnstile Owner can do that.");
            return true;
        }

        turnstile.price = price;
        sender.sendMessage("Price of Turnstile " + turnstile.name
                            + " has been set to " + price + "!");
        return true;
    }

    @CodCommand(
        command = "add",
        subcommand = "hand",
        weight = 51,
        aliases = {"+"},
        usage = {
            "§e     Turnstile Manage Item Price Help Page:",
            "§5A Parameter starts with the 1 character §2id",
            "§2t§f: §5The Name of the Turnstile ex. §6pMainGate",
            "§bIf Turnstile is not specified then the Turnstile linked to the target Block will be affected",
            "§2#§f: §5The amount of the item to be payed ex. §6#10",
            "§2d§f: §5The data/durability value of the item ex. §6d5",
            "§2e§f: §5The item enchantment ex. §6earrow_fire",
            "§bEnchantment levels can be added. ex. §6arrow_fire(2)",
            "§2<command> <Item|ID|hand> [Parameter1] [Parameter2]...",
            "§bex. §6<command> hand #16 tMainGate",
            "§bex. §6<command> wool d5",
            "§bex. §6<command> diamond_sword efire_aspect(2) edamage_all",
        },
        permission = "turnstile.set.price"
    )
    public boolean addHand(Player player, String[] args) {
        setItem(player, false, player.getEquipment().getItemInMainHand(), args);
        return true;
    }

    @CodCommand(
        command = "add",
        weight = 52,
        aliases = {"+"},
        usage = {
            "§e     Turnstile Manage Item Price Help Page:",
            "§5A Parameter starts with the 1 character §2id",
            "§2t§f: §5The Name of the Turnstile ex. §6pMainGate",
            "§bIf Turnstile is not specified then the Turnstile linked to the target Block will be affected",
            "§2#§f: §5The amount of the item to be payed ex. §6#10",
            "§2d§f: §5The data/durability value of the item ex. §6d5",
            "§2e§f: §5The item enchantment ex. §6earrow_fire",
            "§bEnchantment levels can be added. ex. §6arrow_fire(2)",
            "§2<command> <Item|ID|hand> [Parameter1] [Parameter2]...",
            "§bex. §6<command> hand #16 tMainGate",
            "§bex. §6<command> wool d5",
            "§bex. §6<command> diamond_sword efire_aspect(2) edamage_all",
        },
        permission = "turnstile.set.price"
    )
    public boolean add(CommandSender sender, Material mat, String[] args) {
        setItem(sender, true, new ItemStack(mat), args);
        return true;
    }

    @CodCommand(
        command = "remove",
        subcommand = "hand",
        weight = 53,
        aliases = {"+"},
        usage = {
            "§e     Turnstile Manage Item Price Help Page:",
            "§5A Parameter starts with the 1 character §2id",
            "§2t§f: §5The Name of the Turnstile ex. §6pMainGate",
            "§bIf Turnstile is not specified then the Turnstile linked to the target Block will be affected",
            "§2#§f: §5The amount of the item to be payed ex. §6#10",
            "§2d§f: §5The data/durability value of the item ex. §6d5",
            "§2e§f: §5The item enchantment ex. §6earrow_fire",
            "§bEnchantment levels can be added. ex. §6arrow_fire(2)",
            "§2<command> <Item|ID|hand> [Parameter1] [Parameter2]...",
            "§bex. §6<command> hand #16 tMainGate",
            "§bex. §6<command> wool d5",
            "§bex. §6<command> diamond_sword efire_aspect(2) edamage_all",
        },
        permission = "turnstile.set.price"
    )
    public boolean removeHand(Player player, String[] args) {
        setItem(player, false, player.getEquipment().getItemInMainHand(), args);
        return true;
    }

    @CodCommand(
        command = "remove",
        weight = 54,
        aliases = {"-"},
        usage = {
            "§e     Turnstile Manage Item Price Help Page:",
            "§5A Parameter starts with the 1 character §2id",
            "§2t§f: §5The Name of the Turnstile ex. §6pMainGate",
            "§bIf Turnstile is not specified then the Turnstile linked to the target Block will be affected",
            "§2#§f: §5The amount of the item to be payed ex. §6#10",
            "§2d§f: §5The data/durability value of the item ex. §6d5",
            "§2e§f: §5The item enchantment ex. §6earrow_fire",
            "§bEnchantment levels can be added. ex. §6arrow_fire(2)",
            "§2<command> <Item|ID|hand> [Parameter1] [Parameter2]...",
            "§bex. §6<command> hand #16 tMainGate",
            "§bex. §6<command> wool d5",
            "§bex. §6<command> diamond_sword efire_aspect(2) edamage_all",
        },
        permission = "turnstile.set.price"
    )
    public boolean remove(CommandSender sender, Material mat, String[] args) {
        setItem(sender, false, new ItemStack(mat), args);
        return true;
    }

    private static void setItem(CommandSender sender, boolean add, ItemStack item, String[] args) {
        String name = null; //The name of the Turnstile

        //Check each parameter
        int i = 0;
        while (i < args.length) {
            char c = args[i].charAt(0);
            String s = args[i].substring(1);
            switch (c) {
            case 't': //Turnstile Name
                name = s;
                break;

            case '#': //Amount
                try {
                    item.setAmount(Integer.parseInt(s));
                } catch (Exception ex) {
                    sender.sendMessage("§6" + s + "§4 is not a valid amount");
                    return;
                }
                break;

            case 'e': //Enchantment
                Map<Enchantment, Integer> enchantments = getEnchantments(s);
                if (enchantments == null) {
                    sender.sendMessage("§6" + s + "§4 is not a valid enchantment");
                    return;
                }
                item.addUnsafeEnchantments(enchantments);
                break;

            case 'd': //Durability
                try {
                    item.setDurability(Short.parseShort(s));
                } catch (Exception ex) {
                    sender.sendMessage("§6" + s + "§4 is not a valid data/durability value");
                    return;
                }
                break;

            default: //Invalid Parameter
                sender.sendMessage("§6" + c + "§4 is not a valid parameter ID");
                return;
            }

            i++;
        }

        Turnstile turnstile;
        if (name != null) {
            turnstile = TurnstileMain.findTurnstile(name);
            if (turnstile == null ) {
                sender.sendMessage("§4Turnstile §6" + name + "§4 does not exist.");
                return;
            }
        } else {
            if (sender instanceof Player) {
                turnstile = TurnstileMain.findTurnstile(((Player) sender).getTargetBlock(SEE_THROUGH, 10));
            } else {
                sender.sendMessage("§4You cannot do this from the console!");
                return;
            }
        }

        String itemName = Turnstile.getItemName(item);
        if (add) {
            sender.sendMessage("§6" + itemName + "§4 was added to Turnstile §6" + name);
        } else {
            if (turnstile.items.remove(item)) {
                sender.sendMessage("§6" + itemName + "§4 was removed from Turnstile §6" + name);
            } else {
                sender.sendMessage("§6" + itemName + "§4 was not found for Turnstile §6" + name);
            }
        }
    }

    /**
     * Retrieves Enchantments from the given string
     *
     * @param string The String that contains the item
     * @return The Enchantments of the item
     */
    public static Map<Enchantment, Integer> getEnchantments(String string) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        try {
            for (String split: string.split("&")) {
                Enchantment enchantment = null;
                int level = -1;

                if (split.contains("(")) {
                    int index = split.indexOf('(');
                    level = Integer.parseInt(split.substring(index + 1, split.length() - 1));
                    split = split.substring(0, index);
                }

                for (Enchantment enchant: Enchantment.values()) {
                    if (enchant.getName().equalsIgnoreCase(split)) {
                        enchantment = enchant;
                    }
                }

                if (level < enchantment.getStartLevel()) {
                    level = enchantment.getStartLevel();
                }

                enchantments.put(enchantment, level);
            }
        } catch (Exception notEnchantment) {
            return null;
        }
        return enchantments;
    }
}
