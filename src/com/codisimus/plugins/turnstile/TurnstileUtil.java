package com.codisimus.plugins.turnstile;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Gate;
import org.bukkit.material.Openable;
import org.bukkit.material.TrapDoor;

/**
 *
 * @author Codisimus
 */
public class TurnstileUtil {
    public static void openDoor(Block block) {
        switch (block.getType()) {
        case FENCE:
            block.setType(Material.AIR);
            break;
        case TRAP_DOOR:
            BlockState state = block.getState();
            TrapDoor trapDoor = (TrapDoor) state.getData();
            trapDoor.setOpen(true);
            state.update();
            break;
        case FENCE_GATE:
            BlockState gateState = block.getState();
            Gate fenceGate = (Gate) gateState.getData();
            fenceGate.setOpen(true);
            gateState.update();
            break;
        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case WOODEN_DOOR:
        case WOOD_DOOR:
            block = getBottomHalf(block);
            byte data = block.getData();
            if (isDoorClosed(block)) {
                data = (byte) (data | 0x4);
                block.setData(data, true);
                block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0);
            }
            break;
        default:
            //Not supported
            break;
        }
    }
   
    public static void closeDoor(Block block) {
        switch (block.getType()) {
        case AIR:
            block.setType(Material.FENCE);
            break;
        case TRAP_DOOR:
        case FENCE_GATE:
            BlockState state = block.getState();
            Openable door = (Openable) state.getData();
            door.setOpen(false);
            state.update();
            break;
        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case WOODEN_DOOR:
        case WOOD_DOOR:
            block = getBottomHalf(block);
            byte data = block.getData();
            if (!isDoorClosed(block)) {
                data = (byte) (data & 0xb);
                block.setData(data, true);
                block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0);
            }
            break;
        default:
            //Not supported
            break;
        }
    }

    public static boolean isDoorClosed(Block block) {
        byte data = block.getData();
        if ((data & 0x8) == 0x8) {
            block = block.getRelative(BlockFace.DOWN);
            data = block.getData();
        }
        return ((data & 0x4) == 0);
    }

    public static Block getBottomHalf(Block block) {
        switch (block.getType()) {
        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case WOODEN_DOOR:
        case WOOD_DOOR:
            byte data = block.getData();
            if ((data & 0x8) == 0x8) {
                block = block.getRelative(BlockFace.DOWN);
            }
            break;
        default:
            break;
        }
        return block;
    }
}
