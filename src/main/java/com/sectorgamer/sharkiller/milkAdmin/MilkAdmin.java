package com.sectorgamer.sharkiller.milkAdmin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import com.sectorgamer.sharkiller.milkAdmin.WebServer;
import com.sectorgamer.sharkiller.milkAdmin.listeners.*;
import com.sectorgamer.sharkiller.milkAdmin.objects.*;
import com.sectorgamer.sharkiller.milkAdmin.rtk.*;
import com.sectorgamer.sharkiller.milkAdmin.util.*;

import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.files.*;
import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.session.ThreadSafeSession;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import sun.nio.cs.ext.MacCentralEurope;

public class MilkAdmin extends JavaPlugin implements RTKListener {
    /* Ban list variables */

    public Banlist BL;
    public String BLDir;
    public String BLMessage;
    /* White list variables */
    public Whitelist WL;
    public String WLDir;
    public boolean WLCustom = false;
    public boolean WLAlert = false;
    public String WLAlertMessage;
    public String WLKickMessage;
    public ArrayList<String> kickedPlayers = new ArrayList<String>();
    /* PluginUpdates variables */
    public PluginUpdates PU;
    /* RTK variables */
    boolean UsingRTK;
    RTKInterface RTKapi = null;
    String userRTK, passRTK, hostRTK;
    int portRTK;
    /* Server variables */
    String PluginDir = "plugins" + File.separator + "milkAdmin";
    PropertiesFile ServerProperties = new PropertiesFile("server.properties");
    YamlConfiguration Settings = null;
    private WebServer server = null;
    public static String initTime = "";
    public boolean OnlineMode = true;
    public static Permission Permissions = null;
    private boolean permissionsEnabled = false;
    private WerrisRemoteToolkitConfig werrisRemoteToolkitConfig = null;
    private milkAdminUpdateThread m = null;
    private WhitelistCommandExecuter w;
    PluginManager pm;
    public boolean MCWhitelist;
    private static Plugin milkAdmin = null;
    private Config logAccounts = null;
    private String logAccountsDir;
    private String logAccountsFileS;
    private File logAccountsFile;
    private Map<String, String> logAccountsMap;
    private boolean logAccountsB;

    public boolean setup() {
        try {
            File dir;


            /* Check and Create plugin folder */
            dir = new File(PluginDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            /* Check and Copy web files folder */
            dir = new File(PluginDir + File.separator + "html");
            if (!dir.exists()) {
                MilkAdminLog.info("Copying default HTML ZIP...");
                dir = new File(PluginDir + File.separator + "milkAdmin.zip");
                FileMgmt.copy(getResource("milkAdmin.zip"), dir);
                MilkAdminLog.info("Done! Unzipping...");
                if (FileMgmt.unziptodir(dir, new File(PluginDir))) {
                    MilkAdminLog.info("Done! Deleting zip.");
                    dir.deleteOnExit();
                } else {
                    MilkAdminLog.info("Unzip failed!");
                }

            }

            /* Check and Copy default config files */
            dir = new File(PluginDir + File.separator + "settings.yml");
            if (!dir.exists()) {
                MilkAdminLog.info("Copying default settings.yml file.");
                FileMgmt.copy(getResource("settings.yml"), dir);
            }

            /* Init configs */
            Settings = YamlConfiguration.loadConfiguration(dir);
            ServerProperties.load();

            dir = new File(PluginDir + File.separator + "admins.ini");
            if (!dir.exists()) {
                MilkAdminLog.info("Copying default admins.ini file.");
                FileMgmt.copy(getResource("admins.ini"), dir);
            }

            /* Init loggedin system */
            eraseLoggedIn();

            /* Get configs */
            BLDir = Settings.getString("Settings.BanListDir", "plugins" + File.separator + "milkAdmin");
            WLDir = Settings.getString("Whitelist.WhitelistDir", "plugins" + File.separator + "milkAdmin");
            BLMessage = Settings.getString("Strings.Banned", "Banned from the server!");
            UsingRTK = Settings.getBoolean("RTK.UsingRTK");
            WLCustom = Settings.getBoolean("Whitelist.Custom", false);
            boolean MCWhitelist = ServerProperties.getBoolean("white-list", false);
            if (WLCustom || MCWhitelist) {
                this.MCWhitelist = true;
            } else {
                this.MCWhitelist = false;
            }
            OnlineMode = ServerProperties.getBoolean("online-mode", true);

            /* Setup permissions */
            boolean perm = Settings.getBoolean("Settings.UsingPermissions", true);
            if (perm) {
                enablePermissions();
            } else {
                MilkAdminLog.warning("No permission system enabled!");
            }

            /* Init whitelist */
            if (MCWhitelist && WLCustom) {
                MilkAdminLog.warning("Minecraft Whitelist is actitivated. Shutting down custom Whitelist.");
                WLCustom = false;
            } else if (WLCustom) {
                WL = new Whitelist(this);
                WLAlert = Settings.getBoolean("Whitelist.Alert", true);
                WLAlertMessage = Settings.getString("Whitelist.AlertMessage", "&6%s trying to join but is not in whitelist.");
                WLKickMessage = Settings.getString("Whitelist.KickMessage", "You are not in whitelist. Register on the forum!");
                MilkAdminLog.info("Using Custom Whitelist (" + WL.count() + " users)");
            }

            /* Init banlist */
            BL = new Banlist(this);
            MilkAdminLog.info("Banlist Files Loaded");
            MilkAdminLog.info("Banned Nicknames: (" + BL.count(true) + " nicks)");
            MilkAdminLog.info("Banned IPs: (" + BL.count(false) + " IPs)");



            /* Init RTK Listener */
            if (UsingRTK) {
                if (werrisRemoteToolkitConfig == null) {
                    werrisRemoteToolkitConfig = new WerrisRemoteToolkitConfig();
                }
                userRTK = Settings.getString("RTK.Username");
                passRTK = Settings.getString("RTK.Password");
                hostRTK = werrisRemoteToolkitConfig.getString("remote-bind-address");
                portRTK = werrisRemoteToolkitConfig.getInt("remote-control-port");
                RTKapi = RTKInterface.createRTKInterface(portRTK, hostRTK, userRTK, passRTK);
                RTKapi.registerRTKListener(this);
            } else {
                MilkAdminLog.warning("Not using RTK. Required to Start/Stop/Restart/Backup.");
            }
        } catch (IOException e) {
            MilkAdminLog.severe("Could not create milkAdmin files.", e);
            return false;
        } catch (RTKInterfaceException e) {
            MilkAdminLog.severe("Could not create RTK Interface.");
            return false;
        } catch (Exception e) {
            MilkAdminLog.severe("Something went wrong!", e);
            return false;
        }
        if (this != null) {
            m = new milkAdminUpdateThread(this);
        }
        return true;
    }

    public synchronized void enableLogAccounts() {
        logAccountsDir = PluginDir;
        logAccountsFileS = logAccountsDir + File.separator + "logaccounts.txt";
        logAccountsFile = new File(logAccountsFileS);
        logAccountsMap = new ConcurrentHashMap<String, String>();
        if (!logAccountsFile.exists()) {
            logAccountsMap.put("_WERRI_logAccounts", "false");
            logAccounts = new Config(logAccountsFile);
            logAccounts.update(logAccountsMap);
            logAccounts.save("=");
        } else {
            logAccounts = new Config(logAccountsFile);
            logAccounts.load("=");
            Map<String, String> copyOfProperties = logAccounts.getCopyOfProperties();
            logAccountsMap.putAll(copyOfProperties);
        }

        if (logAccountsMap.containsKey("_WERRI_logAccounts")) {
            try {
                logAccountsB = logAccounts.getBoolean("_WERRI_logAccounts");
            } catch (Throwable ex) {
                logAccountsB = false;
                logAccountsMap.put("_WERRI_logAccounts", "false");
                logAccounts.update(logAccountsMap);
                logAccounts.save("=");
            }
        } else {
            logAccountsB = false;
            logAccountsMap.put("_WERRI_logAccounts", "false");
            logAccounts.update(logAccountsMap);
            logAccounts.save("=");
        }
    }

    public void logAccount(String log) {
        MilkAdminLog.info(log);
    }

    public void logAccount(String account, String url) {
        if (!isLocAccount(account)) {
            return;
        }
        String ipByUsername = ThreadSafeSession.getSingleton().getIpByUsername(account);
        String log = "[milkAdmin][LogAccount] User: " + account + " Ip: " + ipByUsername + " Url: " + url;
        logAccount(log);
    }

    public synchronized boolean addLocAccount(String account) {
        if (!logAccountsB) {
            return false;
        }
        if (!isLocAccount(account)) {
            logAccountsMap.put(account, "true");
            logAccounts.update(logAccountsMap);
            logAccounts.save("=");
            return true;
        }
        return false;
    }

    public synchronized boolean removeLocAccount(String account) {
        if (!logAccountsB) {
            return false;
        }
        if (isLocAccount(account)) {
            logAccountsMap.put(account, "false");
            logAccounts.update(logAccountsMap);
            logAccounts.save("=");
            return true;
        }
        return false;
    }

    public boolean isLocAccount(String account) {
        if (!logAccountsB) {
            return false;
        }
        if (!logAccountsMap.containsKey(account)) {
            return false;
        }
        try
        {
            String get = logAccountsMap.get(account);
            boolean parseBoolean = Boolean.parseBoolean(get);
            return parseBoolean;
        } catch (Throwable ex) {
            return false;
        }
    }

    public void enablePermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            Permissions = permissionProvider.getProvider();
            permissionsEnabled = true;
            MilkAdminLog.info("Permission support enabled!");
        } else {
            MilkAdminLog.warning("Permission system not found!");
        }
    }

    public void onRTKStringReceived(String s) {
        if (s.equals("RTK_TIMEOUT")) {
            MilkAdminLog.warning("RTK not response to the user '" + userRTK + "' in the port '" + portRTK + "' bad configuration?");
            MilkAdminLog.warning("Check remote-bind-address out toolkit\\remote.properties is equals with host in settings.yml and the remote port is equals with port in remote.properties!");
        } else {
            MilkAdminLog.info("From RTK: " + s);
        }
    }

    public void eraseLoggedIn() {
        try {
            File loggedin = new File(PluginDir + File.separator + "loggedin.ini");
            if (loggedin.exists()) {
                loggedin.delete();
            }
            loggedin.createNewFile();
        } catch (IOException ex) {
            MilkAdminLog.severe("Failed to create loggedin.ini file.", ex);
        }
    }

    public static File getLoggedinFile() {
        return new File("plugins/milkAdmin" + File.separator + "loggedin.ini");
    }

    public static File getLoggedinBackupFile() {
        return new File("plugins/milkAdmin" + File.separator + "loggedin_backup.ini");
    }

    public static void backupLoggedIn() {
        try {
            File loggedin = new File("plugins/milkAdmin" + File.separator + "loggedin.ini");
            Map<String, String> logins = GenerallyFileManager.AllgemeinFileReader(loggedin, "=");
            if (logins == null) {
                return;
            }
            GenerallyFileManager.FileWrite(logins, getLoggedinBackupFile(), "=");
        } catch (IOException ex) {
            Logger.getLogger(MilkAdmin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void regenerateBackupLoggedIn() {
        try {
            File loggedin_backup = new File("plugins/milkAdmin" + File.separator + "loggedin_backup.ini");
            Map<String, String> logins = GenerallyFileManager.AllgemeinFileReader(loggedin_backup, "=");
            if (logins == null) {
                return;
            }
            GenerallyFileManager.FileWrite(logins, getLoggedinFile(), "=");
            loggedin_backup.delete();
        } catch (IOException ex) {
            Logger.getLogger(MilkAdmin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onEnable() {
        pm = Bukkit.getPluginManager();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        initTime = sdf.format(cal.getTime());
        /* Init configs */
        boolean init = setup();
        if (init) {
            /* Start listeners */
            PluginManager pm = getServer().getPluginManager();
            pm.registerEvents(new BanlistListener(this), this);
            if (WLCustom) {
                pm.registerEvents(new WhitelistListener(this), this);
                w = new WhitelistCommandExecuter(this);
                getCommand("whitelist").setExecutor(w);
            }
            //regenerateBackupLoggedIn();
            /* Welcome messages */
            PluginDescriptionFile pdfFile = this.getDescription();
            MilkAdminLog.info("v" + pdfFile.getVersion() + " is enabled!");
            MilkAdminLog.info("Developed by: " + pdfFile.getAuthors());
            /* Init PluginUpdates */
            PU = new PluginUpdates(pm);
            /* Init web server class */
            server = new WebServer(this);
        } else {
            MilkAdminLog.severe("Failed to initialized!");
        }
        milkAdmin = this;
        enableLogAccounts();
    }

    @Override
    public void onDisable() {
        MilkAdmin.backupLoggedIn();
        try {
            /* Stop web server */
            if (server != null) {
                server.stopServer();
            }

            MilkAdminLog.info("milkAdmin disabled successfully!");
        } catch (IOException e) {
            MilkAdminLog.severe("Error closing WebServer", e);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd == null) {
            return true;
        }
        if ((sender instanceof Player)) {
            return true;
        }
        String commandName = cmd.getName();

        if (commandName.equalsIgnoreCase("milkadmin")) {
            try {
                if (args == null || args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                    sender.sendMessage("/milkadmin whitelist add <player>");
                    sender.sendMessage("/milkadmin whitelist remove <player>");
                    sender.sendMessage("/milkadmin ban <player>");
                    sender.sendMessage("/milkadmin updatebanlist");
                    sender.sendMessage("/milkadmin updatewhitelist");
                    sender.sendMessage("/milkadmin remoeveallwhitelist");
                    sender.sendMessage("/milkadmin addallwhitelist");
                    sender.sendMessage("/milkadmin removeallbanlist");
                    sender.sendMessage("/milkadmin addlocaccount <account> (add log account)");
                    sender.sendMessage("/milkadmin removelocaccount <account> (remove log account)");
                    sender.sendMessage("/milkadmin islocaccount <account> (get true or false)");
                    sender.sendMessage("/milkadmin logplayers <set> (set is either true or false)");
                    sender.sendMessage("/milkadmin logplayers (get the set value true or false)");
                } else if (args[0].equalsIgnoreCase("whitelist") || args[0].equalsIgnoreCase("wl")) {
                    whitelistProccess(sender, args);
                } else if (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("b")) {
                    sender.sendMessage("=D");
                } else if (args[0].equalsIgnoreCase("updatebanlist")) {
                    this.BL.update();
                    sender.sendMessage("banlist is updated!");
                } else if (args[0].equalsIgnoreCase("updatewhitelist")) {
                    if (m != null) {
                        this.m.update();
                    } else {
                        this.WL.update();
                    }
                    sender.sendMessage("whitelist is updated!");
                } else if (args[0].equalsIgnoreCase("removeallwhitelist")) {
                    if (m != null) {
                        m.removeAllWhiteListedPlayers();
                    } else {
                        this.WL.removeAllWhiteListedPlayers();
                    }
                    sender.sendMessage("All players removed from the whitelist!");
                } else if (args[0].equalsIgnoreCase("addallwhitelist")) {
                    if (m != null) {
                        m.addAllWhiteListedPlayers();
                    } else {
                        this.WL.addAllWhiteListedPlayers();
                        this.WL.update();
                    }
                    sender.sendMessage("All players added to the whitelist!");
                } else if (args[0].equalsIgnoreCase("removeallbanlist")) {
                    this.BL.removeAllPlayersFromBanList();
                    sender.sendMessage("All players removed from the banlist!");
                } else if (args[0].equalsIgnoreCase("addlocaccount")) {
                    if (args.length != 2) {
                        sender.sendMessage("/milkadmin addlocaccount <account>");
                        return true;
                    }
                    boolean addLocAccount = addLocAccount(args[1]);
                    if (addLocAccount) {
                        sender.sendMessage("Account is added to the log accounts list!");
                    } else {
                        sender.sendMessage("Account can not removed from the log accounts list!");
                    }
                } else if (args[0].equalsIgnoreCase("removelocaccount")) {
                    if (args.length != 2) {
                        sender.sendMessage("/milkadmin removelocaccount <account>");
                        return true;
                    }
                    boolean removeLocAccount = removeLocAccount(args[1]);
                    if (removeLocAccount) {
                        sender.sendMessage("Account is removed gtom the log accounts list!");
                    } else {
                        sender.sendMessage("Account can not removed from the log accounts list!");
                    }
                } else if (args[0].equalsIgnoreCase("islocaccount")) {
                    if (args.length != 2) {
                        sender.sendMessage("/milkadmin islocaccount <account>");
                        return true;
                    }
                    boolean isLocAccount = isLocAccount(args[1]);
                    if (isLocAccount) {
                        sender.sendMessage("Account: " + args[1] + " is in the account list!!");
                    } else {
                        sender.sendMessage("Account: " + args[1] + " is not in the account list!!");
                    }
                } else if (args[0].equalsIgnoreCase("logplayers")) {
                    if (args.length == 1) {
                        sender.sendMessage("" + logAccountsB);
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("true")) {
                        logAccountsB = true;
                        logAccountsMap.put("_WERRI_logAccounts", "true");
                        logAccounts.update(logAccountsMap);
                        logAccounts.save("=");
                        sender.sendMessage("_WERRI_logAccounts is setted to true!");
                    } else {
                        logAccountsB = false;
                        logAccountsMap.put("_WERRI_logAccounts", "false");
                        logAccounts.update(logAccountsMap);
                        logAccounts.save("=");
                        sender.sendMessage("_WERRI_logAccounts is setted to false!");
                    }
                }
                return true;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return false;
            }
        }
        return false;
    }

    public void whitelistProccess(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender || !(permissionsEnabled) || Permissions.has((Player) sender, "milkadmin.whitelist")) {
            if (WLCustom) {
                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("importdefault") || args[1].equalsIgnoreCase("impdef")) {
                        WL.importDefault();
                        sender.sendMessage(ChatColor.GOLD + "[milkAdmin] " + ChatColor.GREEN + "Default Whitelist imported.");
                    } else if (args[1].equalsIgnoreCase("reload")) {
                        WL.myReload();
                        sender.sendMessage(ChatColor.GOLD + "[milkAdmin] " + ChatColor.GREEN + "Whitelist reloaded.");
                    }
                } else if (args.length == 3) {
                    if (args[1].equalsIgnoreCase("add")) {
                        String res = null;
                        if (m != null) {
                            m.addWhiteListedPlayersAsList(args[2]);
                            res = "ok";
                        } else {
                            res = WL.myAddDefaultPlayer(args[2]);
                        }
                        if (res == "ok") {
                            sender.sendMessage(ChatColor.GOLD + "[milkAdmin] " + ChatColor.GREEN + args[2] + " fue agregado a la whitelist.");
                        } else {
                            sender.sendMessage(ChatColor.GOLD + "[milkAdmin] " + ChatColor.GREEN + res);
                        }
                    } else if (args[1].equalsIgnoreCase("remove")) {
                        String res = null;
                        if (m != null) {
                            m.removeWhiteListedPlayersAsList(args[2]);
                            res = "ok";
                        } else {
                            res = WL.myRemovePlayer(args[2]);
                        }
                        if (res == "ok") {
                            sender.sendMessage(ChatColor.GOLD + "[milkAdmin] " + ChatColor.GREEN + args[2] + " fue sacado de la whitelist.");
                        } else {
                            sender.sendMessage(ChatColor.GOLD + "[milkAdmin] " + ChatColor.GREEN + res);
                        }
                    }
                }
            } else {
                sender.sendMessage(ChatColor.RED + "The custom whitelist is not activated.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have access to this command.");
        }
    }

    public static Plugin getMilkAdminInstance() {
        if (milkAdmin == null) {
            Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();
            if (plugins == null || plugins.length == 0) {
                return null;
            }
            for (Plugin plugin : plugins) {
                if (plugin == null) {
                    continue;
                }
                if (plugin.getName().equalsIgnoreCase("milkAdmin")) {
                    milkAdmin = plugin;
                    return plugin;
                }
            }
            return null;
        }
        return milkAdmin;
    }
}
