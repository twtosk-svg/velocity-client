package com.velocity.core;

import com.velocity.config.ConfigManager;

import com.velocity.config.FriendManager;

import net.fabricmc.api.ClientModInitializer;

public class EspMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Load saved configuration (before anything else)
        ConfigManager.load();
        FriendManager.init();
    }
}
