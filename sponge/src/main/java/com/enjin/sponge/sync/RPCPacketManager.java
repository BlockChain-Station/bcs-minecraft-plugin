package com.enjin.sponge.sync;

import com.enjin.core.Enjin;
import com.enjin.core.EnjinServices;
import com.enjin.core.InstructionHandler;
import com.enjin.rpc.mappings.mappings.plugin.*;
import com.enjin.rpc.mappings.mappings.plugin.data.ExecuteData;
import com.enjin.rpc.mappings.mappings.plugin.data.NotificationData;
import com.enjin.rpc.mappings.mappings.plugin.data.PlayerGroupUpdateData;
import com.enjin.sponge.EnjinMinecraftPlugin;
import com.enjin.rpc.mappings.mappings.general.RPCData;
import com.enjin.rpc.mappings.services.PluginService;
import com.enjin.sponge.config.EMPConfig;
import com.enjin.sponge.config.RankUpdatesConfig;
import com.enjin.sponge.listeners.ConnectionListener;
import com.enjin.sponge.managers.VotifierManager;
import com.enjin.sponge.stats.WriteStats;
import com.enjin.sponge.sync.data.*;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RPCPacketManager implements Runnable {
    private EnjinMinecraftPlugin plugin;
	private long nextStatUpdate = System.currentTimeMillis();

    public RPCPacketManager(EnjinMinecraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
		String stats = null;
		if (Enjin.getConfiguration(EMPConfig.class).isCollectPlayerStats() && System.currentTimeMillis() > nextStatUpdate) {
			stats = getStats();
			nextStatUpdate = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
		}

        Status status = new Status(System.getProperty("java.version"),
                Sponge.getPlatform().getMinecraftVersion().getName(),
                getPlugins(),
                ConnectionListener.permissionsEnabled(),
                plugin.getContainer().getVersion().get(),
                getWorlds(),
                getGroups(),
                getMaxPlayers(),
                getOnlineCount(),
                getOnlinePlayers(),
                getPlayerGroups(),
                null,
                EnjinMinecraftPlugin.getExecutedCommandsConfiguration().getExecutedCommands(),
				getVotes(),
                stats);

        PluginService service = EnjinServices.getService(PluginService.class);
        RPCData<SyncResponse> data = service.sync(status);

        if (data == null) {
            return;
        }

        if (data.getError() != null) {
            Enjin.getLogger().warning(data.getError().getMessage());
        } else {
            SyncResponse result = data.getResult();
            if (result != null && result.getStatus().equalsIgnoreCase("ok")) {
				for (Instruction instruction : result.getInstructions()) {
					switch (instruction.getCode()) {
						case ADD_PLAYER_GROUP:
							AddPlayerGroupInstruction.handle((PlayerGroupUpdateData) instruction.getData());
							break;
						case REMOVE_PLAYER_GROUP:
							RemovePlayerGroupInstruction.handle((PlayerGroupUpdateData) instruction.getData());
							break;
						case EXECUTE:
							ExecuteCommandInstruction.handle((ExecuteData) instruction.getData());
							break;
						case EXECUTE_AS:
							break;
						case CONFIRMED_COMMANDS:
							CommandsReceivedInstruction.handle((ArrayList<Long>) instruction.getData());
							break;
						case CONFIG:
							RemoteConfigUpdateInstruction.handle((Map<String, Object>) instruction.getData());
							break;
						case ADD_PLAYER_WHITELIST:
							AddWhitelistPlayerInstruction.handle((String) instruction.getData());
							break;
						case REMOVE_PLAYER_WHITELIST:
							RemoveWhitelistPlayerInstruction.handle((String) instruction.getData());
							break;
						case RESPONSE_STATUS:
							Enjin.getPlugin().getInstructionHandler().statusReceived((String) instruction.getData());
							break;
						case BAN_PLAYER:
							BanPlayersInstruction.handle((String) instruction.getData());
							break;
						case UNBAN_PLAYER:
							PardonPlayersInstruction.handle((String) instruction.getData());
							break;
						case CLEAR_INGAME_CACHE:
							break;
						case NOTIFICATIONS:
							NotificationsInstruction.handle((NotificationData) instruction.getData());
							break;
						case PLUGIN_VERSION:
							NewerVersionInstruction.handle((String) instruction.getData());
							break;
						default:
					}
				}
            }
        }
    }

    private List<String> getPlugins() {
        return Sponge.getPluginManager().getPlugins().stream().map(PluginContainer::getName).collect(Collectors.toList());
    }

    private List<String> getWorlds() {
        return plugin.getGame().getServer().getWorlds().stream().map(World::getName).collect(Collectors.toList());
    }

    private List<String> getGroups() {
        return ConnectionListener.getGroups();
    }

    private int getMaxPlayers() {
        return plugin.getGame().getServer().getMaxPlayers();
    }

    private int getOnlineCount() {
        return plugin.getGame().getServer().getOnlinePlayers().size();
    }

    private List<PlayerInfo> getOnlinePlayers() {
        return plugin.getGame().getServer().getOnlinePlayers().stream().map(player -> new PlayerInfo(player.getName(),
				Enjin.getApi().getVanishState(player.getUniqueId()),
				player.getUniqueId())).collect(Collectors.toList());
    }

	private Map<String, PlayerGroupInfo> getPlayerGroups() {
		RankUpdatesConfig config = EnjinMinecraftPlugin.getRankUpdatesConfiguration();

		if (config == null) {
			Enjin.getLogger().warning("Rank updates configuration did not load properly.");
			return null;
		}

		Map<String, PlayerGroupInfo> groups = config.getPlayerPerms();
		Map<String, PlayerGroupInfo> update = new HashMap<>();

		int index = 0;
		for (String player : new HashSet<>(groups.keySet())) {
			if (index >= 500) {
				break;
			}

			update.put(player, groups.get(player));
		}

		for (Map.Entry<String, PlayerGroupInfo> entry : update.entrySet()) {
			groups.remove(entry.getKey());
		}

		EnjinMinecraftPlugin.saveRankUpdatesConfiguration();
		return update;
	}

	private Map<String, List<Object[]>> getVotes() {
		Map<String, List<Object[]>> votes = null;
		if (VotifierManager.isEnabled() && !VotifierManager.getPlayerVotes().isEmpty()) {
			votes = new HashMap<>(VotifierManager.getPlayerVotes());
			VotifierManager.getPlayerVotes().clear();
		}
		return votes;
	}

	private String getStats() {
		return new WriteStats().getStatsJSON();
	}
}
