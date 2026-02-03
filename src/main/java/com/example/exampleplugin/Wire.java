package com.example.exampleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.*;

/**
 * Wire block event handler - functions like Minecraft redstone
 * Handles signal propagation, connections, and power distribution
 */
public class Wire {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // Maximum signal strength (like redstone: 15)
    private static final int MAX_SIGNAL_STRENGTH = 15;
    
    // Signal decay rate per block
    private static final int SIGNAL_DECAY = 1;
    
    private final JavaPlugin plugin;
    
    // Track wire power levels (position -> power level)
    private final Map<Vector3i, Integer> wirePowerLevels = new HashMap<>();
    
    public Wire(JavaPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }
    
    private void registerEvents() {
        // Register event handlers for wire block placement and breaking
        plugin.getEventRegistry().register(PlaceBlockEvent.class, this::onBlockPlaced);
        plugin.getEventRegistry().register(BreakBlockEvent.class, this::onBlockBroken);
        
        LOGGER.atInfo().log("Wire event handlers registered");
    }
    
    /**
     * Called when a block is placed.
     */
    private void onBlockPlaced(PlaceBlockEvent event) {
        // Check if the placed item is a wire block
        if (event.getItemInHand() != null && event.getItemInHand().getItem().getId().equals("exampleplugin:wire")) {
            Vector3i pos = event.getTargetBlock();
            
            // Initialize power level to 0 (unpowered)
            wirePowerLevels.put(pos, 0);
            
            LOGGER.atInfo().log("Wire placed at %s with initial power level 0", pos);
            
            // Check neighboring blocks for power sources
            int maxNeighborPower = getMaxNeighborPower(pos);
            if (maxNeighborPower > 0) {
                // Set power level based on strongest neighbor (with decay)
                int newPower = Math.max(0, maxNeighborPower - SIGNAL_DECAY);
                wirePowerLevels.put(pos, newPower);
                LOGGER.atInfo().log("Wire at %s powered to level %d from neighbors", pos, newPower);
            }
            
            // Update neighboring wires
            updateNeighbors(pos);
        }
    }
    
    /**
     * Called when a block is broken.
     */
    private void onBlockBroken(BreakBlockEvent event) {
        // Check if the broken block is a wire
        if (event.getBlockType() != null && event.getBlockType().getId().equals("exampleplugin:wire")) {
            Vector3i pos = event.getTargetBlock();
            
            // Remove power level tracking
            Integer powerLevel = wirePowerLevels.remove(pos);
            
            LOGGER.atInfo().log("Wire broken at %s (was at power level %d)", pos, powerLevel != null ? powerLevel : 0);
            
            // Notify neighbors that this wire is gone
            updateNeighbors(pos);
        }
    }
    
    /**
     * Gets the maximum power level from neighboring wire blocks.
     * @param pos The position to check neighbors around
     * @return The maximum power level found in neighbors
     */
    private int getMaxNeighborPower(Vector3i pos) {
        int maxPower = 0;
        
        // Check all six adjacent positions (North, South, East, West, Up, Down)
        Vector3i[] neighbors = {
            new Vector3i(pos.getX() + 1, pos.getY(), pos.getZ()),
            new Vector3i(pos.getX() - 1, pos.getY(), pos.getZ()),
            new Vector3i(pos.getX(), pos.getY() + 1, pos.getZ()),
            new Vector3i(pos.getX(), pos.getY() - 1, pos.getZ()),
            new Vector3i(pos.getX(), pos.getY(), pos.getZ() + 1),
            new Vector3i(pos.getX(), pos.getY(), pos.getZ() - 1)
        };
        
        for (Vector3i neighbor : neighbors) {
            Integer neighborPower = wirePowerLevels.get(neighbor);
            if (neighborPower != null && neighborPower > maxPower) {
                maxPower = neighborPower;
            }
        }
        
        return maxPower;
    }
    
    /**
     * Updates power levels of neighboring wire blocks.
     * @param pos The position that changed
     */
    private void updateNeighbors(Vector3i pos) {
        // Check all six adjacent positions
        Vector3i[] neighbors = {
            new Vector3i(pos.getX() + 1, pos.getY(), pos.getZ()),
            new Vector3i(pos.getX() - 1, pos.getY(), pos.getZ()),
            new Vector3i(pos.getX(), pos.getY() + 1, pos.getZ()),
            new Vector3i(pos.getX(), pos.getY() - 1, pos.getZ()),
            new Vector3i(pos.getX(), pos.getY(), pos.getZ() + 1),
            new Vector3i(pos.getX(), pos.getY(), pos.getZ() - 1)
        };
        
        for (Vector3i neighbor : neighbors) {
            if (wirePowerLevels.containsKey(neighbor)) {
                // Recalculate power for this neighbor
                int maxPower = getMaxNeighborPower(neighbor);
                int newPower = Math.max(0, maxPower - SIGNAL_DECAY);
                wirePowerLevels.put(neighbor, newPower);
                
                LOGGER.atInfo().log("Updated wire at %s to power level %d", neighbor, newPower);
            }
        }
    }
    
    /**
     * Gets the current power level of a wire at the given position.
     * @param pos The position of the wire
     * @return The power level (0-15), or 0 if not found
     */
    public int getPowerLevel(Vector3i pos) {
        return wirePowerLevels.getOrDefault(pos, 0);
    }
    
    /**
     * Sets the power level of a wire at the given position.
     * @param pos The position of the wire
     * @param power The power level to set (0-15)
     */
    public void setPowerLevel(Vector3i pos, int power) {
        int clampedPower = Math.max(0, Math.min(MAX_SIGNAL_STRENGTH, power));
        wirePowerLevels.put(pos, clampedPower);
        LOGGER.atInfo().log("Wire at %s set to power level %d", pos, clampedPower);
        updateNeighbors(pos);
        
        // VISUAL EFFECTS:
        // Display power level with visual indicators:
        // 1. Change wire texture/color based on power level (darker = lower power)
        // 2. Spawn particles along wire path showing signal flow direction
        // 3. Add glowing effect that intensifies with higher power levels
        // 4. Display power level number above wire when player looks at it
        // 5. Animate signal propagation with traveling particle effects
        
        // POWER SOURCES:
        // Integrate with power source blocks:
        // 1. Detect adjacent power source blocks (buttons, levers, pressure plates)
        // 2. Read power output from source blocks
        // 3. Set wire power level to source power (without decay)
        // 4. Support multiple power sources (use maximum power)
        // 5. Handle power source removal (recalculate from neighbors)
        
        // POWERED DEVICES:
        // Power other blocks with wire signal:
        // 1. Detect adjacent powered device blocks (doors, lights, radios)
        // 2. Send power signal to devices when wire is powered
        // 3. Devices activate when receiving power above threshold
        // 4. Support variable power levels (dimming lights, volume control)
        // 5. Handle device-specific power requirements
        
        // RADIO SYSTEM INTEGRATION:
        // Use wires to connect radios and microphones:
        // 1. Detect adjacent radio/microphone blocks
        // 2. Use wire power level to represent signal strength
        // 3. Connect radios on same wire network for synchronized playback
        // 4. Transmit audio data through wire connections
        // 5. Support long-distance connections with repeaters
        
        // ADVANCED FEATURES:
        // Additional wire functionality:
        // 1. Wire crossing without interference (insulated wires)
        // 2. Repeater blocks to boost signal strength
        // 3. Comparator blocks for signal comparison
        // 4. Diode blocks for one-way signal flow
        // 5. Logic gates (AND, OR, NOT, XOR) for complex circuits
    }
}
