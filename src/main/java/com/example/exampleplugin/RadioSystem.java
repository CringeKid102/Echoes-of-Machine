package com.example.exampleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Radio system - handles radio block interactions
 * Manages frequency tuning, song playback, and radio network communication
 */
public class RadioSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private final JavaPlugin plugin;
    
    // Track radio block states (position -> station index)
    private final Map<Vector3i, Integer> radioStations = new HashMap<>();
    
    // Available frequencies and station names
    private static final int[] FREQUENCIES = {88, 95, 100, 105, 110};
    private static final String[] STATIONS = {"Classic Rock", "Jazz FM", "Pop Hits", "Electronic", "Ambient"};

    public RadioSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }
    
    private void registerEvents() {
        // Register event handler for radio block interactions
        plugin.getEventRegistry().register(UseBlockEvent.class, this::onRadioInteract);
        
        LOGGER.atInfo().log("Radio system initialized and event handlers registered");
    }
    
    /**
     * Called when a player interacts with a radio block.
     * Handles frequency tuning, song selection, and playback control.
     */
    private void onRadioInteract(UseBlockEvent event) {
        // Check if the interacted block is a radio
        if (event.getBlockType() != null && event.getBlockType().getId().equals("exampleplugin:radio")) {
            Vector3i pos = event.getTargetBlock();
            
            // Get interaction context
            var context = event.getContext();
            if (context == null) return;
            
            // Cycle to next station
            int currentIndex = radioStations.getOrDefault(pos, 0);
            int newIndex = (currentIndex + 1) % FREQUENCIES.length;
            radioStations.put(pos, newIndex);
            
            int frequency = FREQUENCIES[newIndex];
            String stationName = STATIONS[newIndex];
            
            // Log the station change
            LOGGER.atInfo().log("Radio at %s tuned to %d MHz - %s", pos, frequency, stationName);
            
            // PLAYER FEEDBACK:
            // To send messages to the player who interacted:
            // 1. Get entity reference from context.getEntity()
            // 2. Access the entity through Store: entityRef.getStore().get(entityRef.getIndex())
            // 3. Cast to Player and call player.sendMessage(Message.raw("text"))
            // 4. Message format: "§6Radio tuned to §e" + frequency + " MHz §6- §e" + stationName
            // Note: Complex entity access requires proper Store handling which varies by context
            
            // BLOCK STATE PERSISTENCE:
            // Block properties (frequency, wavelength, song) are defined in Radio.json
            // To persist state across server restarts, would need to:
            // 1. Access World.getBlockState(pos) to get BlockState
            // 2. Use BlockState.getData() to get NBT-like data storage
            // 3. Store frequency/station index in block data
            // 4. Read from block data on interaction to restore state
            
            // AUDIO PLAYBACK:
            // Would use Hytale's sound system when API is available:
            // 1. Load audio files for each station from resources
            // 2. Use World.playSound() or similar to stream audio
            // 3. Calculate nearby players within hearing range
            // 4. Stream audio to all players in range
            
            // RADIO NETWORKING:
            // Sync frequency across multiple radio blocks:
            // 1. Maintain a network map of radios by frequency
            // 2. When one radio changes frequency, update all on same frequency
            // 3. Broadcast audio from microphones to all radios on matching frequency
            
            // VISUAL EFFECTS:
            // Enhance experience with particles and animations:
            // 1. Spawn particles around radio when playing (musical notes, waves)
            // 2. Change block texture/model based on on/off state
            // 3. Add glowing effect when radio is active
        }
    }
    
    /**
     * Gets the current station index for a radio at the given position.
     * @param pos The position of the radio block
     * @return The station index (0-4)
     */
    public int getStationIndex(Vector3i pos) {
        return radioStations.getOrDefault(pos, 0);
    }
    
    /**
     * Gets the current frequency for a radio at the given position.
     * @param pos The position of the radio block
     * @return The frequency in MHz
     */
    public int getFrequency(Vector3i pos) {
        int index = radioStations.getOrDefault(pos, 0);
        return FREQUENCIES[index];
    }
    
    /**
     * Gets the current station name for a radio at the given position.
     * @param pos The position of the radio block
     * @return The station name
     */
    public String getStationName(Vector3i pos) {
        int index = radioStations.getOrDefault(pos, 0);
        return STATIONS[index];
    }
    
    /**
     * Clears the state of a radio (called when block is broken).
     * @param pos The position of the radio block
     */
    public void clearRadioState(Vector3i pos) {
        radioStations.remove(pos);
        LOGGER.atInfo().log("Radio state cleared at %s", pos);
    }
}
