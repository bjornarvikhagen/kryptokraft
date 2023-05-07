package com.cryptoplugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SignClickListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();

        if (isTotalSign(sign)) {
            double totalValue = getTotalCombinedValue(sign);
            sign.setLine(1, String.format("%.2f", totalValue) + " USD");
            sign.update();
        } else {
            String symbol = getSymbolFromLine(sign.getLine(0));
            double holdings = getHoldingsFromLine(sign.getLine(2));

            if (symbol != null && holdings > 0) {
                double value = fetchCryptoValue(symbol, holdings);
                sign.setLine(1, String.format("%.2f", value) + " USD");
                sign.update();
            }
        }
    }

    private String getSymbolFromLine(String line) {
        if (line.matches("^\\[(\\w+)\\]$")) {
            return line.replaceAll("[\\[\\]]", "");
        }

        return null;
    }

    private double getHoldingsFromLine(String line) {
        try {
            return Double.parseDouble(line.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isTotalSign(Sign sign) {
        return sign.getLine(0).equalsIgnoreCase("[TOTAL]");
    }

    private double getTotalCombinedValue(Sign totalSign) {
        double totalValue = 0;
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Sign) {
                        Sign sign = (Sign) state;
                        if (!isTotalSign(sign)) {
                            String symbol = getSymbolFromLine(sign.getLine(0));
                            double holdings = getHoldingsFromLine(sign.getLine(2));

                            if (symbol != null && holdings > 0) {
                                totalValue += fetchCryptoValue(symbol, holdings);
                            }
                        }
                    }
                }
            }
        }
        return totalValue;
    }

    private double fetchCryptoValue(String symbol, double holdings) {
        String apiKey = "YOUR_API";
        String apiUrl = "https://min-api.cryptocompare.com/data/price?fsym=" + symbol + "&tsyms=USD&api_key=" + apiKey;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();

            JSONObject json = new JSONObject(content.toString());
            double cryptoToUsd = json.getDouble("USD");

            return holdings * cryptoToUsd;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}

