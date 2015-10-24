package com.enjin.officialplugin.permlisteners;

import com.enjin.core.Enjin;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.anjocaido.groupmanager.events.GMUserEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.enjin.officialplugin.EnjinMinecraftPlugin;

public class GroupManagerListener implements Listener {

    EnjinMinecraftPlugin plugin;

    public GroupManagerListener(EnjinMinecraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void userGroupChangeListener(GMUserEvent event) {
        Action action = event.getAction();
        if (action == Action.USER_ADDED || action == Action.USER_GROUP_CHANGED || action == Action.USER_SUBGROUP_CHANGED ||
                action == Action.USER_REMOVED) {
            String player = event.getUser().getName();
            String uuid = "";
            if (EnjinMinecraftPlugin.supportsUUID()) {
                uuid = event.getUser().getBukkitPlayer().getUniqueId().toString();
            }
            if (player != null) {
                Enjin.getPlugin().debug(event.getUserName() + " just got a rank change... processing...");
                plugin.listener.updatePlayerRanks(player, uuid);
            }
        }
    }

}
