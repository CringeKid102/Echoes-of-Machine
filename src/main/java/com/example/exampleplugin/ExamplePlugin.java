package com.example.exampleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class ExamplePlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Wire wireBlock;

    public ExamplePlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        
        // Initialize wire block handler
        wireBlock = new Wire(this);
        LOGGER.atInfo().log("Wire block system initialized and listeners registered");

        // Initialize Radio System
        RadioSystem radioSystem = new RadioSystem(this);
        
        // Initialize Microphone System
        MicrophoneSystem microphoneSystem = new MicrophoneSystem(this);
        
        // Initialize Handheld Radio System
        HandheldRadioSystem handheldRadioSystem = new HandheldRadioSystem(this);
    }
}
