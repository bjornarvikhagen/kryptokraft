package com.cryptoplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class CryptoSignPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Your plugin initialization logic
        getServer().getPluginManager().registerEvents(new SignClickListener(), this);
    }
}
