package com.enjin.officialplugin.sync.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.UUID;

import com.enjin.core.Enjin;
import com.enjin.officialplugin.util.PacketUtilities;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.enjin.officialplugin.EnjinMinecraftPlugin;
import com.enjin.officialplugin.events.RemoveWhitelistPlayersEvent;

/**
 * @author OverCaste (Enjin LTE PTD).
 *         This software is released under an Open Source license.
 * @copyright Enjin 2012.
 */

public class Packet18RemovePlayersFromWhitelist {
    public static void handle(BufferedInputStream in, EnjinMinecraftPlugin plugin) {
        try {
            String players = PacketUtilities.readString(in);
            Enjin.getPlugin().debug("Removing these players from the whitelist: " + players);
            String[] msg = players.split(",");
            OfflinePlayer[] oplayers = new OfflinePlayer[msg.length];
            for (int i = 0; i < msg.length; i++) {
                String playername = msg[i];
                if (playername.length() == 32) {
                    // expand UUIDs which do not have dashes in them
                    playername = playername.substring(0, 8) + "-" + playername.substring(8, 12) + "-" + playername.substring(12, 16) +
                            "-" + playername.substring(16, 20) + "-" + playername.substring(20, 32);
                }
                if (playername.length() == 36) {
                    try {
                        oplayers[i] = Bukkit.getOfflinePlayer(UUID.fromString(playername));
                    } catch (Exception e) {
                        oplayers[i] = Bukkit.getOfflinePlayer(playername);
                    }
                } else {
                    oplayers[i] = Bukkit.getOfflinePlayer(playername);
                }
            }
            plugin.getServer().getPluginManager().callEvent(new RemoveWhitelistPlayersEvent(oplayers));
            if ((msg.length > 0)) {
                for (int i = 0; i < msg.length; i++) {
                    oplayers[i].setWhitelisted(false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handle(String player) {
        Enjin.getPlugin().getInstructionHandler().removeFromWhitelist(player);
    }
}
