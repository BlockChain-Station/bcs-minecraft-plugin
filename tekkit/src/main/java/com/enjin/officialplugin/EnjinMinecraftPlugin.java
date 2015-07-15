package com.enjin.officialplugin;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import net.milkbowl.vault.permission.Permission;
import net.milkbowl.vault.permission.plugins.Permission_GroupManager;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.enjin.officialplugin.heads.CachedHeadData;
import com.enjin.officialplugin.heads.HeadListener;
import com.enjin.officialplugin.heads.HeadLocations;
import com.enjin.officialplugin.listeners.EnjinStatsListener;
import com.enjin.officialplugin.listeners.NewPlayerChatListener;
import com.enjin.officialplugin.listeners.TekkitPlayerChatListener;
import com.enjin.officialplugin.listeners.VotifierListener;
import com.enjin.officialplugin.permlisteners.GroupManagerListener;
import com.enjin.officialplugin.permlisteners.PermissionsBukkitChangeListener;
import com.enjin.officialplugin.permlisteners.PexChangeListener;
import com.enjin.officialplugin.permlisteners.bPermsChangeListener;
import com.enjin.officialplugin.points.EnjinPointsSyncClass;
import com.enjin.officialplugin.points.PointsAPI;
import com.enjin.officialplugin.points.RetrievePointsSyncClass;
import com.enjin.officialplugin.shop.ShopItems;
import com.enjin.officialplugin.shop.ShopListener;
import com.enjin.officialplugin.stats.StatsPlayer;
import com.enjin.officialplugin.stats.StatsServer;
import com.enjin.officialplugin.stats.WriteStats;
import com.enjin.officialplugin.threaded.CommandExecuter;
import com.enjin.officialplugin.threaded.ConfigSender;
import com.enjin.officialplugin.threaded.DelayedCommandExecuter;
import com.enjin.officialplugin.threaded.NewKeyVerifier;
import com.enjin.officialplugin.threaded.PeriodicEnjinTask;
import com.enjin.officialplugin.threaded.PeriodicVoteTask;
import com.enjin.officialplugin.threaded.ReportMakerThread;
import com.enjin.officialplugin.threaded.UpdateHeadsThread;
import com.enjin.officialplugin.tpsmeter.MonitorTPS;
import com.enjin.officialplugin.EnjinConsole;
import com.enjin.proto.stats.EnjinStats;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

import de.bananaco.bpermissions.imp.Permissions;

import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * @author OverCaste (Enjin LTE PTD).
 *         This software is released under an Open Source license.
 * @copyright Enjin 2013.
 */

public class EnjinMinecraftPlugin extends JavaPlugin {


    public FileConfiguration config;
    public static boolean usingGroupManager = false;
    File hashFile;
    public static String hash = "";
    Server s;
    Logger logger;
    public static Permission permission = null;
    public static boolean debug = false;
    public boolean collectstats = false;
    public PermissionsEx permissionsex;
    public GroupManager groupmanager;
    public Permissions bpermissions;
    public PermissionsPlugin permissionsbukkit;
    public boolean supportsglobalgroups = true;
    public boolean votifierinstalled = false;
    public int xpversion = 0;
    public String mcversion = "";

    int signupdateinterval = 10;

    public HeadLocations headlocation = new HeadLocations();
    public CachedHeadData headdata = new CachedHeadData(this);

    public static String BUY_COMMAND = "buy";

    /**
     * Key is the config value, value is the type, string, boolean, etc.
     */
    public ConcurrentHashMap<String, ConfigValueTypes> configvalues = new ConcurrentHashMap<String, ConfigValueTypes>();

    public int statssendinterval = 5;

    public final static Logger enjinlogger = Logger.getLogger(EnjinMinecraftPlugin.class.getName());

    public CommandExecuter commandqueue = new CommandExecuter();

    public StatsServer serverstats = new StatsServer(this);
    public ConcurrentHashMap<String, StatsPlayer> playerstats = new ConcurrentHashMap<String, StatsPlayer>();
    /**
     * Key is banned player, value is admin that banned the player or blank if the console banned
     */
    public ConcurrentHashMap<String, String> bannedplayers = new ConcurrentHashMap<String, String>();
    /**
     * Key is banned player, value is admin that pardoned the player or blank if the console pardoned
     */
    public ConcurrentHashMap<String, String> pardonedplayers = new ConcurrentHashMap<String, String>();

    public ShopItems cachedItems = new ShopItems();


    static public String apiurl = "://api.enjin.com/api/";
    //static public String apiurl = "://gamers.enjin.ca/api/";
    //static public String apiurl = "://tuxreminder.info/api/";
    //static public String apiurl = "://mxm.enjin.com/api/";
    //static public String apiurl = "://api.enjin.ca/api/";

    public boolean autoupdate = true;
    public String newversion = "";

    public boolean hasupdate = false;
    public boolean updatefailed = false;
    public boolean authkeyinvalid = false;
    public boolean unabletocontactenjin = false;
    public boolean permissionsnotworking = false;
    static public final String updatejar = "http://resources.guild-hosting.net/1/downloads/emp/";
    static public final String bukkitupdatejar = "http://dev.bukkit.org/media/files/";
    static public boolean bukkitversion = false;

    public final EMPListener listener = new EMPListener(this);

    //------------Threaded tasks---------------
    final PeriodicEnjinTask task = new PeriodicEnjinTask(this);
    final PeriodicVoteTask votetask = new PeriodicVoteTask(this);
    public DelayedCommandExecuter commexecuter = new DelayedCommandExecuter(this);
    //Initialize in the onEnable
    public MonitorTPS tpstask;
    //-------------Thread IDS-------------------
    int synctaskid = -1;
    int votetaskid = -1;
    int tpstaskid = -1;
    int commandexecutertask = -1;
    int headsupdateid = -1;

    static final ExecutorService exec = Executors.newCachedThreadPool();
    public static String minecraftport;
    public static boolean usingSSL = true;
    NewKeyVerifier verifier = null;
    public ConcurrentHashMap<String, String> playerperms = new ConcurrentHashMap<String, String>();
    //Player, lists voted on.
    public ConcurrentHashMap<String, String> playervotes = new ConcurrentHashMap<String, String>();

    public EnjinErrorReport lasterror = null;

    public EnjinStatsListener esl = null;

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
    public ShopListener shoplistener;

    public static void debug(String s) {
        if (debug) {
            System.out.println("Enjin Debug: " + s);
        }
        enjinlogger.fine(s);
    }

    @Override
    public void onEnable() {
        try {
            debug("Initializing internal logger");
            enjinlogger.setLevel(Level.FINEST);
            File logsfolder = new File(getDataFolder().getAbsolutePath() + File.separator + "logs");
            if (!logsfolder.exists()) {
                logsfolder.mkdirs();
            }
            FileHandler fileTxt = new FileHandler(getDataFolder().getAbsolutePath() + File.separator + "logs" + File.separator + "enjin.log", true);
            EnjinLogFormatter formatterTxt = new EnjinLogFormatter();
            fileTxt.setFormatter(formatterTxt);
            enjinlogger.addHandler(fileTxt);
            enjinlogger.setUseParentHandlers(false);
            debug("Logger initialized.");
            debug("Begin init");
            initVariables();
            debug("Init vars done.");
            debug("Init Files");
            initFiles();
            headlocation.loadHeads();
            debug("Init files done.");
            initPlugins();
            debug("Init plugins done.");
            setupPermissions();
            debug("Setup permissions integration");
            setupVotifierListener();
            debug("Setup Votifier integration");

            //------We should do TPS even if we have an invalid auth key
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, tpstask = new MonitorTPS(this), 40, 40);

            Thread configthread = new Thread(new ConfigSender(this));
            configthread.start();

            //Let's get the minecraft version.
            String[] cbversionstring = getServer().getVersion().split(":");
            String[] versionstring = cbversionstring[1].split("\\.");
            try {
                int majorversion = Integer.parseInt(versionstring[0].trim());
                int minorversion = Integer.parseInt(versionstring[1].trim().substring(0, 1));
                int buildnumber = 0;
                if (versionstring.length > 2) {
                    try {
                        buildnumber = Integer.parseInt(versionstring[2].substring(0, 1));
                    } catch (NumberFormatException e) {

                    }
                }
                mcversion = majorversion + "." + minorversion + "." + buildnumber;
                if (majorversion == 1) {
                    if (minorversion > 2) {
                        xpversion = 1;
                        logger.info("[Enjin Minecraft Plugin] MC 1.3 or above found, enabling version 2 XP handling.");
                    } else {
                        logger.info("[Enjin Minecraft Plugin] MC 1.2 or below found, enabling version 1 XP handling.");
                    }
                } else if (majorversion > 1) {
                    xpversion = 1;
                    logger.info("[Enjin Minecraft Plugin] MC 1.3 or above found, enabling version 2 XP handling.");
                }
            } catch (Exception e) {
                logger.severe("[Enjin Minecraft Plugin] Unable to get server version! Inaccurate XP handling may occurr!");
                logger.severe("[Enjin Minecraft Plugin] Server Version String: " + getServer().getVersion());
            }

            if (collectstats) {
                startStatsCollecting();
                File stats = new File("stats.stats");
                if (stats.exists()) {
                    FileInputStream input = new FileInputStream(stats);
                    EnjinStats.Server serverstats = EnjinStats.Server.parseFrom(input);
                    debug("Parsing stats input.");
                    this.serverstats = new StatsServer(this, serverstats);
                    for (EnjinStats.Server.Player player : serverstats.getPlayersList()) {
                        debug("Adding player " + player.getName() + ".");
                        playerstats.put(player.getName().toLowerCase(), new StatsPlayer(player));
                    }
                }
                //XP handling and chat event handling changed at 1.3, so we can use the same variable. :D
                if (xpversion < 1) {
                    Bukkit.getPluginManager().registerEvents(new TekkitPlayerChatListener(this), this);
                } else {
                    Bukkit.getPluginManager().registerEvents(new NewPlayerChatListener(this), this);
                }
            }
            usingGroupManager = (permission instanceof Permission_GroupManager);
            //debug("Checking key valid.");
            //Bypass key checking, but only if the key looks valid
            registerEvents();
            if (hash.length() == 50) {
                debug("Starting periodic tasks.");
                startTask();
            } else {
                authkeyinvalid = true;
                debug("Auth key is invalid. Wrong length.");
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[Enjin Minecraft Plugin] Couldn't enable EnjinMinecraftPlugin! Reason: " + t.getMessage());
            enjinlogger.warning("Couldn't enable EnjinMinecraftPlugin! Reason: " + t.getMessage());
            t.printStackTrace();
            this.setEnabled(false);
        }
    }

    public void stopStatsCollecting() {
        HandlerList.unregisterAll(esl);
    }

    public void startStatsCollecting() {
        if (esl == null) {
            esl = new EnjinStatsListener(this);
        }
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(esl, this);
        if (!config.getBoolean("statscollected.player.travel", true)) {
            PlayerMoveEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.player.blocksbroken", true)) {
            BlockBreakEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.player.blocksplaced", true)) {
            BlockPlaceEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.player.kills", true)) {
            EntityDeathEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.player.deaths", true)) {
            PlayerDeathEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.player.xp", true)) {
            PlayerExpChangeEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.server.creeperexplosions", true)) {
            EntityExplodeEvent.getHandlerList().unregister(esl);
        }
        if (!config.getBoolean("statscollected.server.playerkicks", true)) {
            PlayerKickEvent.getHandlerList().unregister(esl);
        }
    }

    @Override
    public void onDisable() {
        stopTask();
        //unregisterEvents();
        if (collectstats) {
            new WriteStats(this).write("stats.stats");
            debug("Stats saved to stats.stats.");
        }
    }

    private void initVariables() throws Throwable {
        hashFile = new File(this.getDataFolder(), "HASH.txt");
        s = Bukkit.getServer();
        logger = Bukkit.getLogger();
        try {
            Properties serverProperties = new Properties();
            FileInputStream in = new FileInputStream(new File("server.properties"));
            serverProperties.load(in);
            in.close();
            minecraftport = serverProperties.getProperty("server-port");
        } catch (Throwable t) {
            t.printStackTrace();
            enjinlogger.severe("Couldn't find a localhost ip! Please report this problem!");
            throw new Exception("[Enjin Minecraft Plugin] Couldn't find a localhost ip! Please report this problem!");
        }
    }

    public void initFiles() {
        //let's read in the old hash file if there is one and convert it to the new format.
        if (hashFile.exists()) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(hashFile));
                hash = r.readLine();
                r.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Remove it, we won't ever need it again.
            hashFile.delete();
        }
        config = getConfig();
        File configfile = new File(getDataFolder().toString() + "/config.yml");
        if (!configfile.exists()) {
            createConfig();
        }
        configvalues.put("debug", ConfigValueTypes.BOOLEAN);
        debug = config.getBoolean("debug", false);
        configvalues.put("authkey", ConfigValueTypes.FORBIDDEN);
        hash = config.getString("authkey", "");
        debug("Key value retrieved: " + hash);
        configvalues.put("https", ConfigValueTypes.BOOLEAN);
        usingSSL = config.getBoolean("https", true);
        configvalues.put("autoupdate", ConfigValueTypes.BOOLEAN);
        autoupdate = config.getBoolean("autoupdate", true);
        apiurl = config.getString("apiurl", apiurl);
        //Test to see if we need to update the config file.
        String teststats = config.getString("collectstats", "");
        if (teststats.equals("")) {
            createConfig();
        }
        configvalues.put("collectstats", ConfigValueTypes.BOOLEAN);
        collectstats = config.getBoolean("collectstats", collectstats);
        configvalues.put("sendstatsinterval", ConfigValueTypes.INT);
        statssendinterval = config.getInt("sendstatsinterval", 5);
        configvalues.put("statscollected.player.travel", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.blocksbroken", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.blocksplaced", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.kills", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.deaths", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.xp", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.creeperexplosions", ConfigValueTypes.BOOLEAN);
        configvalues.put("statscollected.player.playerkicks", ConfigValueTypes.BOOLEAN);
        configvalues.put("buycommand", ConfigValueTypes.STRING);
        teststats = config.getString("statscollected.player.travel", "");
        if (teststats.equals("")) {
            createConfig();
        }
        teststats = config.getString("buycommand", null);
        if (teststats == null) {
            createConfig();
        }
        BUY_COMMAND = config.getString("buycommand", null);
    }

    private void createConfig() {
        config.set("debug", debug);
        config.set("authkey", hash);
        config.set("https", usingSSL);
        config.set("autoupdate", autoupdate);
        config.set("collectstats", collectstats);
        config.set("sendstatsinterval", statssendinterval);
        String teststats = config.getString("statscollected.player.travel", "");
        if (teststats.equals("")) {
            config.set("statscollected.player.travel", true);
            config.set("statscollected.player.blocksbroken", true);
            config.set("statscollected.player.blocksplaced", true);
            config.set("statscollected.player.kills", true);
            config.set("statscollected.player.deaths", true);
            config.set("statscollected.player.xp", true);
            config.set("statscollected.server.creeperexplosions", true);
            config.set("statscollected.server.playerkicks", true);
        }
        teststats = config.getString("buycommand", null);
        if (teststats == null) {
            config.set("buycommand", BUY_COMMAND);
        }
        saveConfig();
    }

    public void startTask() {
        debug("Starting tasks.");
        BukkitScheduler scheduler = Bukkit.getScheduler();
        synctaskid = scheduler.scheduleAsyncRepeatingTask(this, task, 1200L, 1200L);
        //execute the command executer task every 10 ticks, which should vary between .5 and 1 second on servers.
        commandexecutertask = scheduler.scheduleSyncRepeatingTask(this, commexecuter, 20L, 10L);
        commexecuter.loadCommands(Bukkit.getConsoleSender());
        //We want to wait an entire minute before running this to make sure all the worlds have had the time
        //to load before we go and start updating heads.
        headsupdateid = scheduler.scheduleAsyncRepeatingTask(this, new UpdateHeadsThread(this), 120L, 20 * 60 * signupdateinterval);
        //Only start the vote task if votifier is installed.
        if (votifierinstalled) {
            debug("Starting votifier task.");
            votetaskid = scheduler.scheduleAsyncRepeatingTask(this, votetask, 80L, 80L);
        }
    }

    public void registerEvents() {
        debug("Registering events.");
        Bukkit.getPluginManager().registerEvents(listener, this);
        //Listen for all the heads events.
        Bukkit.getPluginManager().registerEvents(new HeadListener(this), this);
        if (BUY_COMMAND != null) {
            shoplistener = new ShopListener();
            Bukkit.getPluginManager().registerEvents(shoplistener, this);
        }
    }

    public void stopTask() {
        debug("Stopping tasks.");
        if (synctaskid != -1) {
            Bukkit.getScheduler().cancelTask(synctaskid);
        }
        if (commandexecutertask != -1) {
            Bukkit.getScheduler().cancelTask(commandexecutertask);
            commexecuter.saveCommands();
        }
        if (votetaskid != -1) {
            Bukkit.getScheduler().cancelTask(votetaskid);
        }
        if (headsupdateid != -1) {
            Bukkit.getScheduler().cancelTask(headsupdateid);
        }
        //Bukkit.getScheduler().cancelTasks(this);
    }

    public void unregisterEvents() {
        debug("Unregistering events.");
        HandlerList.unregisterAll(listener);
    }

    private void setupVotifierListener() {
        if (Bukkit.getPluginManager().isPluginEnabled("Votifier")) {
            System.out.println("[Enjin Minecraft Plugin] Votifier plugin found, enabling Votifier support.");
            enjinlogger.info("Votifier plugin found, enabling Votifier support.");
            Bukkit.getPluginManager().registerEvents(new VotifierListener(this), this);
            votifierinstalled = true;
        }
    }

    private void initPlugins() throws Throwable {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            enjinlogger.warning("Couldn't find the vault plugin! Please get it from dev.bukkit.org/server-mods/vault/!");
            getLogger().warning("[Enjin Minecraft Plugin] Couldn't find the vault plugin! Please get it from dev.bukkit.org/server-mods/vault/!");
            return;
        }
        debug("Initializing permissions.");
        initPermissions();
    }

    private void initPermissions() throws Throwable {
        RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (provider == null) {
            enjinlogger.warning("Couldn't find a vault compatible permission plugin! Please install one before using the Enjin Minecraft Plugin.");
            Bukkit.getLogger().warning("[Enjin Minecraft Plugin] Couldn't find a vault compatible permission plugin! Please install one before using the Enjin Minecraft Plugin.");
            return;
        }
        permission = provider.getProvider();
        if (permission == null) {
            enjinlogger.warning("Couldn't find a vault compatible permission plugin! Please install one before using the Enjin Minecraft Plugin.");
            Bukkit.getLogger().warning("[Enjin Minecraft Plugin] Couldn't find a vault compatible permission plugin! Please install one before using the Enjin Minecraft Plugin.");
            return;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //This command is now depreciated in favor of /enjin key
        if (command.getName().equals("enjinkey") || command.getName().equalsIgnoreCase("ek")) {
            if (!sender.hasPermission("enjin.setkey")) {
                sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.setkey\" permission or OP to run that command!");
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            enjinlogger.info("Checking if key is valid");
            Bukkit.getLogger().info("Checking if key is valid");
            //Make sure we don't have several verifier threads going at the same time.
            if (verifier == null || verifier.completed) {
                verifier = new NewKeyVerifier(this, args[0], sender, false);
                Thread verifierthread = new Thread(verifier);
                verifierthread.start();
            } else {
                sender.sendMessage(ChatColor.RED + "Please wait until we verify the key before you try again!");
            }
            return true;
            //We have the main enjin command, and the alias e command.
        }
        if (command.getName().equalsIgnoreCase("enjin") || command.getName().equalsIgnoreCase("e")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("key")) {
                    if (!sender.hasPermission("enjin.setkey")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.setkey\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length != 2) {
                        return false;
                    }
                    enjinlogger.info("Checking if key is valid");
                    Bukkit.getLogger().info("Checking if key is valid");
                    //Make sure we don't have several verifier threads going at the same time.
                    if (verifier == null || verifier.completed) {
                        verifier = new NewKeyVerifier(this, args[1], sender, false);
                        Thread verifierthread = new Thread(verifier);
                        verifierthread.start();
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please wait until we verify the key before you try again!");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("report")) {
                    if (!sender.hasPermission("enjin.report")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.report\" permission or OP to run that command!");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Please wait as we generate the report");
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
                    Date date = new Date();
                    StringBuilder report = new StringBuilder();
                    report.append("Enjin Debug Report generated on " + dateFormat.format(date) + "\n");
                    report.append("Enjin plugin version: " + getDescription().getVersion() + "\n");
                    String permsmanager = "Generic";
                    String permsversion = "Unknown";
                    if (permissionsex != null) {
                        permsmanager = "PermissionsEx";
                        permsversion = permissionsex.getDescription().getVersion();
                    } else if (bpermissions != null) {
                        permsmanager = "bPermissions";
                        permsversion = bpermissions.getDescription().getVersion();
                    } else if (groupmanager != null) {
                        permsmanager = "GroupManager";
                        permsversion = groupmanager.getDescription().getVersion();
                    } else if (permissionsbukkit != null) {
                        permsmanager = "PermissionsBukkit";
                        permsversion = permissionsbukkit.getDescription().getVersion();
                    }
                    report.append("Permissions plugin used: " + permsmanager + " version " + permsversion + "\n");
                    if (permission != null) {
                        report.append("Vault permissions system reported: " + permission.getName() + "\n");
                    }
                    if (votifierinstalled) {
                        String votiferversion = Bukkit.getPluginManager().getPlugin("Votifier").getDescription().getVersion();
                        report.append("Votifier version: " + votiferversion + "\n");
                    }
                    report.append("Bukkit version: " + getServer().getVersion() + "\n");
                    report.append("Java version: " + System.getProperty("java.version") + " " + System.getProperty("java.vendor") + "\n");
                    report.append("Operating system: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + "\n");

                    if (authkeyinvalid) {
                        report.append("ERROR: Authkey reported by plugin as invalid!\n");
                    }
                    if (unabletocontactenjin) {
                        report.append("WARNING: Plugin has been unable to contact Enjin for the past 5 minutes\n");
                    }
                    if (permissionsnotworking) {
                        report.append("WARNING: Permissions plugin is not configured properly and is disabled. Check the server.log for more details.\n");
                    }

                    report.append("\nPlugins: \n");
                    for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                        report.append(p.getName() + " version " + p.getDescription().getVersion() + "\n");
                    }
                    report.append("\nWorlds: \n");
                    for (World world : getServer().getWorlds()) {
                        report.append(world.getName() + "\n");
                    }
                    ReportMakerThread rmthread = new ReportMakerThread(this, report, sender);
                    Thread dispatchThread = new Thread(rmthread);
                    dispatchThread.start();
                    return true;
                } else if (args[0].equalsIgnoreCase("debug")) {
                    if (!sender.hasPermission("enjin.debug")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.debug\" permission or OP to run that command!");
                        return true;
                    }
                    if (debug) {
                        debug = false;
                    } else {
                        debug = true;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Debugging has been set to " + debug);
                    return true;
                } else if (args[0].equalsIgnoreCase("updateheads") || args[0].equalsIgnoreCase("syncheads")) {
                    if (!sender.hasPermission("enjin.updateheads")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.updateheads\" permission or OP to run that command!");
                        return true;
                    }
                    UpdateHeadsThread uhthread = new UpdateHeadsThread(this, sender);
                    Thread dispatchThread = new Thread(uhthread);
                    dispatchThread.start();
                    sender.sendMessage(ChatColor.GREEN + "Head update queued, please wait...");
                    return true;
                } else if (args[0].equalsIgnoreCase("push")) {
                    if (!sender.hasPermission("enjin.push")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.push\" permission or OP to run that command!");
                        return true;
                    }
                    OfflinePlayer[] allplayers = getServer().getOfflinePlayers();
                    if (playerperms.size() > 3000 || playerperms.size() >= allplayers.length) {
                        int minutes = playerperms.size() / 3000;
                        //Make sure to tack on an extra minute for the leftover players.
                        if (playerperms.size() % 3000 > 0) {
                            minutes++;
                        }
                        //Add an extra 10% if it's going to take more than one synch.
                        //Just in case a synch fails.
                        if (playerperms.size() > 3000) {
                            minutes += minutes * 0.1;
                        }
                        sender.sendMessage(ChatColor.RED + "A rank sync is still in progress, please wait until the current sync completes.");
                        sender.sendMessage(ChatColor.RED + "Progress:" + Integer.toString(playerperms.size()) + " more player ranks to transmit, ETA: " + minutes + " minute" + (minutes > 1 ? "s" : "") + ".");
                        return true;
                    }
                    for (OfflinePlayer offlineplayer : allplayers) {
                        playerperms.put(offlineplayer.getName(), "");
                    }

                    //Calculate how many minutes approximately it's going to take.
                    int minutes = playerperms.size() / 3000;
                    //Make sure to tack on an extra minute for the leftover players.
                    if (playerperms.size() % 3000 > 0) {
                        minutes++;
                    }
                    //Add an extra 10% if it's going to take more than one synch.
                    //Just in case a synch fails.
                    if (playerperms.size() > 3000) {
                        minutes += minutes * 0.1;
                    }
                    if (minutes == 1) {
                        sender.sendMessage(ChatColor.GREEN + Integer.toString(playerperms.size()) + " players have been queued for synching. This should take approximately " + Integer.toString(minutes) + " minute.");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + Integer.toString(playerperms.size()) + " players have been queued for synching. This should take approximately " + Integer.toString(minutes) + " minutes.");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("savestats")) {
                    if (!sender.hasPermission("enjin.savestats")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.savestats\" permission or OP to run that command!");
                        return true;
                    }
                    new WriteStats(this).write("stats.stats");
                    sender.sendMessage(ChatColor.GREEN + "Stats saved to stats.stats.");
                    return true;
                } else if (args[0].equalsIgnoreCase("playerstats")) {
                    if (!sender.hasPermission("enjin.playerstats")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.playerstats\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length > 1) {
                        if (playerstats.containsKey(args[1].toLowerCase())) {
                            StatsPlayer player = playerstats.get(args[1].toLowerCase());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Player stats for player: " + ChatColor.GOLD + player.getName());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Deaths: " + ChatColor.GOLD + player.getDeaths());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Kills: " + ChatColor.GOLD + player.getKilled());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Blocks broken: " + ChatColor.GOLD + player.getBrokenblocks());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Blocks placed: " + ChatColor.GOLD + player.getPlacedblocks());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Block types broken: " + ChatColor.GOLD + player.getBrokenblocktypes().toString());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Block types placed: " + ChatColor.GOLD + player.getPlacedblocktypes().toString());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Foot distance traveled: " + ChatColor.GOLD + player.getFootdistance());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Boat distance traveled: " + ChatColor.GOLD + player.getBoatdistance());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Minecart distance traveled: " + ChatColor.GOLD + player.getMinecartdistance());
                            sender.sendMessage(ChatColor.DARK_GREEN + "Pig distance traveled: " + ChatColor.GOLD + player.getPigdistance());
                        }
                    } else {
                        return false;
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("serverstats")) {
                    if (!sender.hasPermission("enjin.serverstats")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.serverstats\" permission or OP to run that command!");
                        return true;
                    }
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
                    Date date = new Date(serverstats.getLastserverstarttime());
                    sender.sendMessage(ChatColor.DARK_GREEN + "Server Stats");
                    sender.sendMessage(ChatColor.DARK_GREEN + "Server Start time: " + ChatColor.GOLD + dateFormat.format(date));
                    sender.sendMessage(ChatColor.DARK_GREEN + "Total number of creeper explosions: " + ChatColor.GOLD + serverstats.getCreeperexplosions());
                    sender.sendMessage(ChatColor.DARK_GREEN + "Total number of kicks: " + ChatColor.GOLD + serverstats.getTotalkicks());
                    sender.sendMessage(ChatColor.DARK_GREEN + "Kicks per player: " + ChatColor.GOLD + serverstats.getPlayerkicks().toString());
                    return true;
                } else if (apiurl.equals("://gamers.enjin.ca/api/") && args[0].equalsIgnoreCase("vote") && args.length > 2) {
                    String username = args[1];
                    String lists = "";
                    String listname = args[2];
                    if (playervotes.containsKey(username)) {
                        lists = playervotes.get(username);
                        lists = lists + "," + listname.replaceAll("[^0-9A-Za-z.\\-]", "");
                    } else {
                        lists = listname.replaceAll("[^0-9A-Za-z.\\-]", "");
                    }
                    playervotes.put(username, lists);
                    sender.sendMessage(ChatColor.GREEN + "You just added a vote for player " + username + " on list " + listname);
                } else if (args[0].equalsIgnoreCase("inform")) {
                    if (!sender.hasPermission("enjin.inform")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.inform\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "To send a message do: /enjin inform playername message");
                        return true;
                    }
                    Player player = getServer().getPlayerExact(args[1]);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "That player isn't on the server at the moment.");
                        return true;
                    }
                    StringBuilder thestring = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) {
                            thestring.append(" ");
                        }
                        thestring.append(args[i]);
                    }
                    player.sendMessage(EnjinConsole.translateColorCodes(thestring.toString()));
                    return true;
                } else if (args[0].equalsIgnoreCase("broadcast")) {
                    if (!sender.hasPermission("enjin.broadcast")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.broadcast\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "To broadcast a message do: /enjin broadcast message");
                    }
                    StringBuilder thestring = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        if (i > 1) {
                            thestring.append(" ");
                        }
                        thestring.append(args[i]);
                    }
                    getServer().broadcastMessage(EnjinConsole.translateColorCodes(thestring.toString()));
                    return true;
                } else if (args[0].equalsIgnoreCase("lag")) {
                    if (!sender.hasPermission("enjin.lag")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.lag\" permission or OP to run that command!");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GOLD + "Average TPS: " + ChatColor.GREEN + tpstask.getTPSAverage());
                    sender.sendMessage(ChatColor.GOLD + "Last TPS measurement: " + ChatColor.GREEN + tpstask.getLastTPSMeasurement());
                    Runtime runtime = Runtime.getRuntime();
                    long memused = (runtime.maxMemory() - runtime.freeMemory()) / (1024 * 1024);
                    long maxmemory = runtime.maxMemory() / (1024 * 1024);
                    sender.sendMessage(ChatColor.GOLD + "Memory Used: " + ChatColor.GREEN + memused + "MB/" + maxmemory + "MB");
                    return true;
                } else if (args[0].equalsIgnoreCase("addpoints")) {
                    if (!sender.hasPermission("enjin.points.add")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.points.add\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length > 2) {
                        String playername = args[1].trim();
                        int pointsamount = 1;
                        try {
                            pointsamount = Integer.parseInt(args[2].trim());
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "Usage: /enjin addpoints [player] [amount]");
                            return true;
                        }
                        if (pointsamount < 1) {
                            sender.sendMessage(ChatColor.DARK_RED + "You cannot add less than 1 point to a user. You might want to try /enjin removepoints!");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GOLD + "Please wait as we add the " + pointsamount + " points to " + playername + "...");
                        EnjinPointsSyncClass mthread = new EnjinPointsSyncClass(sender, playername, pointsamount, PointsAPI.Type.AddPoints);
                        Thread dispatchThread = new Thread(mthread);
                        dispatchThread.start();
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "Usage: /enjin addpoints [player] [amount]");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("removepoints")) {
                    if (!sender.hasPermission("enjin.points.remove")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.points.remove\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length > 2) {
                        String playername = args[1].trim();
                        int pointsamount = 1;
                        try {
                            pointsamount = Integer.parseInt(args[2].trim());
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "Usage: /enjin removepoints [player] [amount]");
                            return true;
                        }
                        if (pointsamount < 1) {
                            sender.sendMessage(ChatColor.DARK_RED + "You cannot remove less than 1 point to a user.");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GOLD + "Please wait as we remove the " + pointsamount + " points from " + playername + "...");
                        EnjinPointsSyncClass mthread = new EnjinPointsSyncClass(sender, playername, pointsamount, PointsAPI.Type.RemovePoints);
                        Thread dispatchThread = new Thread(mthread);
                        dispatchThread.start();
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "Usage: /enjin removepoints [player] [amount]");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("setpoints")) {
                    if (!sender.hasPermission("enjin.points.set")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.points.set\" permission or OP to run that command!");
                        return true;
                    }
                    if (args.length > 2) {
                        String playername = args[1].trim();
                        int pointsamount = 1;
                        try {
                            pointsamount = Integer.parseInt(args[2].trim());
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.DARK_RED + "Usage: /enjin setpoints [player] [amount]");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GOLD + "Please wait as we set the points to " + pointsamount + " points for " + playername + "...");
                        EnjinPointsSyncClass mthread = new EnjinPointsSyncClass(sender, playername, pointsamount, PointsAPI.Type.SetPoints);
                        Thread dispatchThread = new Thread(mthread);
                        dispatchThread.start();
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "Usage: /enjin setpoints [player] [amount]");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("points")) {
                    if (args.length > 1 && sender.hasPermission("enjin.points.getothers")) {
                        String playername = args[1].trim();
                        sender.sendMessage(ChatColor.GOLD + "Please wait as we retrieve the points balance for " + playername + "...");
                        RetrievePointsSyncClass mthread = new RetrievePointsSyncClass(sender, playername, false);
                        Thread dispatchThread = new Thread(mthread);
                        dispatchThread.start();
                    } else if (sender.hasPermission("enjin.points.getself")) {
                        sender.sendMessage(ChatColor.GOLD + "Please wait as we retrieve your points balance...");
                        RetrievePointsSyncClass mthread = new RetrievePointsSyncClass(sender, sender.getName(), true);
                        Thread dispatchThread = new Thread(mthread);
                        dispatchThread.start();
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + "I'm sorry, you don't have permission to check points!");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("head") || args[0].equalsIgnoreCase("heads")) {
                    if (!sender.hasPermission("enjin.sign.set")) {
                        sender.sendMessage(ChatColor.RED + "You need to have the \"enjin.sign.set\" permission or OP to run that command!");
                        return true;
                    }
                    /*
                     * Display detailed Enjin help for heads in console
	                 */
                    sender.sendMessage(EnjinConsole.header());

                    sender.sendMessage(ChatColor.AQUA + "To set a sign with a head, just place the head, then place the sign either above or below it.");
                    sender.sendMessage(ChatColor.AQUA + "To create a sign of a specific type just put the code on the first line. # denotes the number.");
                    sender.sendMessage(ChatColor.AQUA + " Example: [donation2] would show the second most recent donation.");
                    sender.sendMessage(ChatColor.AQUA + "If there are sub-types, those go on the second line of the sign.");
                    sender.sendMessage(ChatColor.GOLD + "[donation#] " + ChatColor.RESET + " - Most recent donation.");
                    sender.sendMessage(ChatColor.GRAY + " Subtypes: " + ChatColor.RESET + " Place the item id on the second line to only get donations for that package.");
                    sender.sendMessage(ChatColor.GOLD + "[topvoter#] " + ChatColor.RESET + " - Top voter of the month.");
                    sender.sendMessage(ChatColor.GRAY + " Subtypes: " + ChatColor.RESET + " day, week, month. Changes it to the top voter of the day/week/month.");
                    sender.sendMessage(ChatColor.GOLD + "[voter#] " + ChatColor.RESET + " - Most recent voter.");
                    sender.sendMessage(ChatColor.GOLD + "[topplayer#] " + ChatColor.RESET + " - Top player (gets data from module on website).");
                    sender.sendMessage(ChatColor.GOLD + "[topposter#] " + ChatColor.RESET + " - Top poster on the forum.");
                    sender.sendMessage(ChatColor.GOLD + "[toplikes#] " + ChatColor.RESET + " - Top forum likes.");
                    sender.sendMessage(ChatColor.GOLD + "[newmember#] " + ChatColor.RESET + " - Latest player to sign up on the website.");
                    sender.sendMessage(ChatColor.GOLD + "[toppoints#] " + ChatColor.RESET + " - Which player has the most unspent points.");
                    sender.sendMessage(ChatColor.GOLD + "[pointsspent#] " + ChatColor.RESET + " - Player which has spent the most points overall.");
                    sender.sendMessage(ChatColor.GRAY + " Subtypes: " + ChatColor.RESET + " day, week, month. Changes the range to day/week/month.");
                    sender.sendMessage(ChatColor.GOLD + "[moneyspent#] " + ChatColor.RESET + " - Player which has spent the most money on the server overall.");
                    sender.sendMessage(ChatColor.GRAY + " Subtypes: " + ChatColor.RESET + " day, week, month. Changes the range to day/week/month.");
                    return true;
                }
            } else {
                /*
                 * Display detailed Enjin help in console
                 */
                sender.sendMessage(EnjinConsole.header());

                if (sender.hasPermission("enjin.setkey"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin key <KEY>: "
                            + ChatColor.RESET + "Enter the secret key from your " + ChatColor.GRAY + "Admin - Games - Minecraft - Enjin Plugin " + ChatColor.RESET + "page.");
                if (sender.hasPermission("enjin.broadcast"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin broadcast <MESSAGE>: "
                            + ChatColor.RESET + "Broadcast a message to all players.");
                if (sender.hasPermission("enjin.push"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin push: "
                            + ChatColor.RESET + "Sync your website tags with the current ranks.");
                /*if(sender.hasPermission("enjin.playerstats"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin playerstats <NAME>: "
                            + ChatColor.RESET + "Display player statistics.");
                if(sender.hasPermission("enjin.serverstats"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin serverstats: "
                            + ChatColor.RESET + "Display server statistics.");*/
                if (sender.hasPermission("enjin.lag"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin lag: "
                            + ChatColor.RESET + "Display TPS average and memory usage.");
                if (sender.hasPermission("enjin.debug"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin debug: "
                            + ChatColor.RESET + "Enable debug mode and display extra information in console.");
                if (sender.hasPermission("enjin.report"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin report: "
                            + ChatColor.RESET + "Generate a report file that you can send to Enjin Support for troubleshooting.");
                if (sender.hasPermission("enjin.sign.set"))
                    sender.sendMessage(ChatColor.GOLD + "/enjin heads: "
                            + ChatColor.RESET + "Shows in game help for the heads and sign stats part of the plugin.");

                // Shop buy commands
                sender.sendMessage(ChatColor.GOLD + "/buy: "
                        + ChatColor.RESET + "Display items available for purchase.");
                sender.sendMessage(ChatColor.GOLD + "/buy page <#>: "
                        + ChatColor.RESET + "View the next page of results.");
                sender.sendMessage(ChatColor.GOLD + "/buy <ID>: "
                        + ChatColor.RESET + "Purchase the specified item ID in the server shop.");
                return true;
            }
        }
        return false;
    }

    public void forceHeadUpdate() {
        UpdateHeadsThread uhthread = new UpdateHeadsThread(this, null);
        Thread dispatchThread = new Thread(uhthread);
        dispatchThread.start();
    }

    /**
     * @param urls
     * @param queryValues
     * @return 0 = Invalid key, 1 = OK, 2 = Exception encountered.
     * @throws MalformedURLException
     */
    public static int sendAPIQuery(String urls, String... queryValues) throws MalformedURLException {
        URL url = new URL((usingSSL ? "https" : "http") + apiurl + urls);
        StringBuilder query = new StringBuilder();
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setReadTimeout(3000);
            con.setConnectTimeout(3000);
            con.setDoOutput(true);
            con.setDoInput(true);
            for (String val : queryValues) {
                query.append('&');
                query.append(val);
            }
            if (queryValues.length > 0) {
                query.deleteCharAt(0); //remove first &
            }
            con.setRequestProperty("Content-length", String.valueOf(query.length()));
            con.getOutputStream().write(query.toString().getBytes());
            if (con.getInputStream().read() == '1') {
                return 1;
            }
            return 0;
        } catch (SSLHandshakeException e) {
            enjinlogger.warning("SSLHandshakeException, The plugin will use http without SSL. This may be less secure.");
            Bukkit.getLogger().warning("[Enjin Minecraft Plugin] SSLHandshakeException, The plugin will use http without SSL. This may be less secure.");
            usingSSL = false;
            return sendAPIQuery(urls, queryValues);
        } catch (SocketTimeoutException e) {
            enjinlogger.warning("Timeout, the enjin server didn't respond within the required time. Please be patient and report this bug to enjin.");
            Bukkit.getLogger().warning("[Enjin Minecraft Plugin] Timeout, the enjin server didn't respond within the required time. Please be patient and report this bug to enjin.");
            return 2;
        } catch (Throwable t) {
            t.printStackTrace();
            enjinlogger.warning("Failed to send query to enjin server! " + t.getClass().getName() + ". Data: " + url + "?" + query.toString());
            Bukkit.getLogger().warning("[Enjin Minecraft Plugin] Failed to send query to enjin server! " + t.getClass().getName() + ". Data: " + url + "?" + query.toString());
            return 2;
        }
    }

    public static synchronized void setHash(String hash) {
        EnjinMinecraftPlugin.hash = hash;
    }

    public static synchronized String getHash() {
        return EnjinMinecraftPlugin.hash;
    }

    private void setupPermissions() {
        Plugin pex = this.getServer().getPluginManager().getPlugin("PermissionsEx");
        if (pex != null) {
            permissionsex = (PermissionsEx) pex;
            debug("PermissionsEx found, hooking custom events.");
            Bukkit.getPluginManager().registerEvents(new PexChangeListener(this), this);
            return;
        }
        Plugin bperm = this.getServer().getPluginManager().getPlugin("bPermissions");
        if (bperm != null) {
            bpermissions = (Permissions) bperm;
            debug("bPermissions found, hooking custom events.");
            supportsglobalgroups = false;
            Bukkit.getPluginManager().registerEvents(new bPermsChangeListener(this), this);
            return;
        }
        Plugin groupmanager = this.getServer().getPluginManager().getPlugin("GroupManager");
        if (groupmanager != null) {
            this.groupmanager = (GroupManager) groupmanager;
            debug("GroupManager found, hooking custom events.");
            supportsglobalgroups = false;
            Bukkit.getPluginManager().registerEvents(new GroupManagerListener(this), this);
            return;
        }
        Plugin bukkitperms = this.getServer().getPluginManager().getPlugin("PermissionsBukkit");
        if (bukkitperms != null) {
            this.permissionsbukkit = (PermissionsPlugin) bukkitperms;
            debug("PermissionsBukkit found, hooking custom events.");
            Bukkit.getPluginManager().registerEvents(new PermissionsBukkitChangeListener(this), this);
            return;
        }
        debug("No suitable permissions plugin found, falling back to synching on player disconnect.");
        debug("You might want to switch to PermissionsEx, bPermissions, or Essentials GroupManager.");

    }

    public int getTotalXP(int level, float xp) {
        int atlevel = 0;
        int totalxp = 0;
        int xpneededforlevel = 0;
        if (xpversion == 1) {
            xpneededforlevel = 17;
            while (atlevel < level) {
                atlevel++;
                totalxp += xpneededforlevel;
                if (atlevel >= 16) {
                    xpneededforlevel += 3;
                }
            }
            //We only have 2 versions at the moment
        } else {
            xpneededforlevel = 7;
            boolean odd = true;
            while (atlevel < level) {
                atlevel++;
                totalxp += xpneededforlevel;
                if (odd) {
                    xpneededforlevel += 3;
                    odd = false;
                } else {
                    xpneededforlevel += 4;
                    odd = true;
                }
            }
        }
        totalxp = (int) (totalxp + (xp * xpneededforlevel));
        return totalxp;
    }

    public StatsPlayer GetPlayerStats(String name) {
        StatsPlayer stats = playerstats.get(name.toLowerCase());
        if (stats == null) {
            stats = new StatsPlayer(name);
            playerstats.put(name.toLowerCase(), stats);
        }
        return stats;
    }

    public void noEnjinConnectionEvent() {
        if (!unabletocontactenjin) {
            unabletocontactenjin = true;
            Player[] players = getServer().getOnlinePlayers();
            for (Player player : players) {
                if (player.hasPermission("enjin.notify.connectionstatus")) {
                    player.sendMessage(ChatColor.DARK_RED + "[Enjin Minecraft Plugin] Unable to connect to enjin, please check your settings.");
                    player.sendMessage(ChatColor.DARK_RED + "If this problem persists please send enjin the results of the /enjin log");
                }
            }
        }
    }

    public boolean testHTTPSconnection() {
        try {
            URL url = new URL("https://api.enjin.com/ok.html");
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = in.readLine();
            in.close();
            if (inputLine != null && inputLine.startsWith("OK")) {
                return true;
            }
            return false;
        } catch (SSLHandshakeException e) {
            if (debug) {
                e.printStackTrace();
            }
            return false;
        } catch (SocketTimeoutException e) {
            if (debug) {
                e.printStackTrace();
            }
            return false;
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
            }
            return false;
        }
    }

    public boolean testWebConnection() {
        try {
            URL url = new URL("http://google.com");
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = in.readLine();
            in.close();
            if (inputLine != null) {
                return true;
            }
            return false;
        } catch (SocketTimeoutException e) {
            if (debug) {
                e.printStackTrace();
            }
            return false;
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
            }
            return false;
        }
    }

    public boolean testHTTPconnection() {
        try {
            URL url = new URL("http://api.enjin.com/ok.html");
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = in.readLine();
            in.close();
            if (inputLine != null && inputLine.startsWith("OK")) {
                return true;
            }
            return false;
        } catch (SocketTimeoutException e) {
            if (debug) {
                e.printStackTrace();
            }
            return false;
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
            }
            return false;
        }
    }

    public static boolean isMineshafterPresent() {
        try {
            Class.forName("mineshafter.MineServer");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public PeriodicEnjinTask getTask() {
        return task;
    }
}
