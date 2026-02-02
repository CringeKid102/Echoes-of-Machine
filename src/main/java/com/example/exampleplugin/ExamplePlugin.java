package com.example.exampleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class ExamplePlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private Wire wireBlock;
    private Radio radioBlock;

    public ExamplePlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        
        // Initialize wire block handler
        wireBlock = new Wire();
        this.getEventManager().registerListener(wireBlock);
        LOGGER.atInfo().log("Wire block system initialized and listeners registered");

        // Initialize radio block handler
        radioBlock = new Radio();
        this.getEventManager().registerListener(radioBlock);
        LOGGER.atInfo().log("Radio block system initialized and listeners registered");
    }
}
