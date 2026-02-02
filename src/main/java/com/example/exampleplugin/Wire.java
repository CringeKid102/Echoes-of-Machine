package com.example.exampleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.block.BlockState;
import com.hypixel.hytale.server.core.event.EventHandler;
import com.hypixel.hytale.server.core.event.Listener;
import com.hypixel.hytale.server.core.event.block.BlockBreakEvent;
import com.hypixel.hytale.server.core.event.block.BlockPlaceEvent;
import com.hypixel.hytale.server.core.event.block.BlockUpdateEvent;
import com.hypixel.hytale.server.core.world.World;
import com.hypixel.hytale.server.core.world.position.BlockPosition;

import java.util.*;

/**
 * Wire block implementation - functions like Minecraft redstone
 * Handles signal propagation, connections, and power distribution
 */
public class Wire implements Listener {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // Maximum signal strength (like redstone: 15)
    private static final int MAX_SIGNAL_STRENGTH = 15;
    
    // Signal decay rate per block
    private static final int SIGNAL_DECAY = 1;
    
    /**
     * Gets the current power level of a wire at the given position
     */
    public int getPowerLevel(World world, BlockPosition pos) {
        BlockState state = world.getBlockState(pos);
        if (state == null) return 0;
        return state.getProperty("power").map(p -> Integer.parseInt(p.toString())).orElse(0);
    }
    
    /**
     * Sets the power level of a wire at the given position
     */
    public void setPowerLevel(World world, BlockPosition pos, int power) {
        if (power < 0) power = 0;
        if (power > MAX_SIGNAL_STRENGTH) power = MAX_SIGNAL_STRENGTH;
        
        BlockState state = world.getBlockState(pos);
        if (state != null && isWireBlock(state)) {
            world.setBlockState(pos, state.withProperty("power", String.valueOf(power)));
        }
        LOGGER.atInfo().log("Setting wire power at %s to %d", pos, power);
    }
    
    /**
     * Called when a wire block is placed or a neighbor updates
     * Recalculates power levels and propagates signals
     */
    public void onNeighborUpdate(World world, BlockPosition pos) {
        int maxPower = calculateMaxInputPower(world, pos);
        int currentPower = getPowerLevel(world, pos);
        
        // Only update if power changed
        if (maxPower != currentPower) {
            setPowerLevel(world, pos, maxPower);
            notifyNeighbors(world, pos);
        }
    }
    
    /**
     * Calculates the maximum input power from adjacent blocks
     */
    private int calculateMaxInputPower(World world, BlockPosition pos) {
        int maxPower = 0;
        
        // Check all 6 adjacent blocks (4 horizontal + 2 vertical)
        for (BlockPosition neighbor : getAdjacentPositions(pos)) {
            int neighborPower = getReceivedPower(world, neighbor);
            
            // Power decreases by 1 for each block traveled
            if (neighborPower > 0) {
                maxPower = Math.max(maxPower, neighborPower - SIGNAL_DECAY);
            }
        }
        
        // Check for direct power sources (buttons, levers, etc.)
        maxPower = Math.max(maxPower, getDirectPower(world, pos));
        
        return Math.min(maxPower, MAX_SIGNAL_STRENGTH);
    }
    
    /**
     * Gets power received from a neighboring block
     */
    private int getReceivedPower(World world, BlockPosition pos) {
        BlockState state = world.getBlockState(pos);
        if (state == null) return 0;
        
        // Check if it's another wire block
        if (isWireBlock(state)) {
            return getPowerLevel(world, pos);
        }
        
        // Check if it's a power source
        if (state.getBlock().getName().equals("ExampleGroup:PowerSource") || state.getBlock().getTags().contains("power_source")) {
            return MAX_SIGNAL_STRENGTH;
        }
        
        return 0;
    }
    
    /**
     * Gets direct power from power sources (levers, buttons, etc.)
     */
    private int getDirectPower(World world, BlockPosition pos) {
        int maxDirectPower = 0;
        for (BlockPosition neighbor : getAdjacentPositions(pos)) {
            BlockState state = world.getBlockState(neighbor);
            if (state != null && state.getBlock().getTags().contains("direct_power")) {
                maxDirectPower = MAX_SIGNAL_STRENGTH;
            }
        }
        return maxDirectPower;
    }
    
    /**
     * Notifies all adjacent blocks that this wire's power changed
     */
    private void notifyNeighbors(World world, BlockPosition pos) {
        for (BlockPosition neighbor : getAdjacentPositions(pos)) {
            BlockState state = world.getBlockState(neighbor);
            
            // If neighbor is also a wire, update it recursively
            if (isWireBlock(state)) {
                onNeighborUpdate(world, neighbor);
            } else {
                // Notify other blocks they're being powered
                // TODO: Trigger block update events
            }
        }
    }
    
    /**
     * Gets all 6 adjacent block positions (N, S, E, W, Up, Down)
     */
    private List<BlockPosition> getAdjacentPositions(BlockPosition pos) {
        List<BlockPosition> positions = new ArrayList<>();
        
        // Horizontal neighbors
        positions.add(pos.offset(1, 0, 0));   // East
        positions.add(pos.offset(-1, 0, 0));  // West
        positions.add(pos.offset(0, 0, 1));   // South
        positions.add(pos.offset(0, 0, -1));  // North
        
        // Vertical neighbors
        positions.add(pos.offset(0, 1, 0));   // Up
        positions.add(pos.offset(0, -1, 0));  // Down
        
        return positions;
    }
    
    /**
     * Checks if a block state is a wire block
     */
    private boolean isWireBlock(BlockState state) {
        return state != null && state.getBlock().getName().equals("ExampleGroup:Wire");
    }
    
    /**
     * Called when wire block is placed
     */
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        BlockPosition pos = event.getPosition();
        World world = event.getWorld();
        if (isWireBlock(world.getBlockState(pos))) {
            LOGGER.atInfo().log("Wire placed at %s", pos);
            setPowerLevel(world, pos, 0);
            onNeighborUpdate(world, pos);
        }
    }
    
    /**
     * Called when wire block is broken
     */
    @EventHandler
    public void onBlockBroken(BlockBreakEvent event) {
        BlockPosition pos = event.getPosition();
        World world = event.getWorld();
        // Since the block is already broken, we can't check if it's a wire via state at pos
        // but we can notify neighbors
        LOGGER.atInfo().log("Wire broken at %s", pos);
        // Notify neighbors that power source is gone
        for (BlockPosition neighbor : getAdjacentPositions(pos)) {
            BlockState state = world.getBlockState(neighbor);
            if (isWireBlock(state)) {
                onNeighborUpdate(world, neighbor);
            }
        }
    }

    /**
     * Called when a block is updated (e.g., neighbor changed)
     */
    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        BlockPosition pos = event.getPosition();
        World world = event.getWorld();
        if (isWireBlock(world.getBlockState(pos))) {
            onNeighborUpdate(world, pos);
        }
    }
    
    /**
     * Checks if this wire can connect to a block in the given direction
     */
    public boolean canConnectTo(World world, BlockPosition pos, BlockPosition neighbor) {
        BlockState neighborState = world.getBlockState(neighbor);
        
        // Connect to other wires
        if (isWireBlock(neighborState)) {
            return true;
        }
        
        // Connect to solid blocks that can be powered
        // TODO: Check if block accepts redstone power
        
        return false;
    }
}
