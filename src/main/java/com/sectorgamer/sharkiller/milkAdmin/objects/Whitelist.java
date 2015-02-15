package com.sectorgamer.sharkiller.milkAdmin.objects;

import com.evilmidget38.UUIDFetcher;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;
import com.sectorgamer.sharkiller.milkAdmin.util.Configuration;
import com.sectorgamer.sharkiller.milkAdmin.util.MilkAdminLog;
import de.demonbindestrichcraft.lib.bukkit.wbukkitlib.player.WPlayerInterface;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonParser;
import org.bukkit.entity.Player;

/**
 * Handle milkAdmin White list system.
 * @author Sharkiller
 */
public class Whitelist {

    /**
     *
     * sharkale31:
     *   enabled: true
     *   vip:
     *     active: true
     *     donations:
     *       - '31/12/2000:23'
     *       - '06/08/2011:50'
     *   dates:
     *     register: 999999999
     *     lastlogin: 888888888
     *   referer:
     *     referedby: 'iL_nono'
     *     referers:
     *       - 'Geroo0'
     *       - 'Alexander'
     * warnings: 0
     * comments: 'groso'
     * likes: 50
     *
     **/
    private MilkAdmin plugin;
    private File file;
    private JsonParser json;
    //private JsonArray whitelist;
    private Configuration whitelist;
    private int size;
    private Map<String, String> whitelistMapName_Uuid = new ConcurrentHashMap<String, String>();
    private Map<String, String> whitelistMapUuid_Name = new ConcurrentHashMap<String, String>();
    private List<String> whitelistPlayers = new CopyOnWriteArrayList<String>();
    private boolean removeoperation = false;
    public Whitelist(MilkAdmin i) throws FileNotFoundException {
        this.plugin = i;
        //file = new File(plugin.WLDir + File.separator + "whitelist.json");
        file = new File(plugin.WLDir + File.separator + "whitelist.txt");
        whitelist = new Configuration(file);
        /* JsonElement parse = json.parse(new JsonReader(new FileReader(file)));
        whitelist = parse.getAsJsonArray();
        size = whitelist.size();
        asJsonArray.get(0).getAsJsonObject().get("uuid").getAsString();
        JsonElement get = asJsonObject.get("uuid");
        JsonElement get1 = asJsonObject.get("name");
        String asString = get.getAsString();
        String as2String = get1.getAsString();*/
        if (file.exists()) {
            whitelist.load();
        } else {
            whitelist.save();
            List<String> players = new CopyOnWriteArrayList<String>(getPlayers());
            this.update(players, players);
        }

        this.update();
    }

    public String removePlayer(String player) {
        String result;
        if (keyExists(player)) {
            whitelist.setProperty(player + ".enabled", false);
            whitelist.save();
            result = "Jugador eliminado de la whitelist";
        } else {
            result = "El jugador no existe";
        }
        return result;
    }

    public String myRemovePlayer(String player) {
        this.removeoperation=true;
        String name = getPlayerName(player);
        this.removeUserWC(name);
        this.removeoperation=false;
        return removePlayer(name);
    }

    public String addDefaultPlayer(String player) {
        String result;
        if (!keyExists(player)) {
            Date today = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yy");
            String register = formatter.format(today);
            result = setPlayer(player, true, null, null, register, null, null, null, null, null, null);
        } else {
            result = "Player already exist and his account is " + (whitelist.getBoolean(player + ".enabled", false) ? "enabled" : "disabled");
        }
        return result;
    }

    public String myAddDefaultPlayer(String player) {
        String name = getPlayerName(player);
        this.addUserWC(name);
        return addDefaultPlayer(name);
    }

    public String setPlayer(String player, boolean enabled, String vip, List<String> donations, String register, String lastlogin, String refererby, String referers, String warnings, String comments, String likes) {
        String result;

        whitelist.setProperty(player + ".enabled", enabled);
        if (vip != null) {
            whitelist.setProperty(player + ".vip.active", vip);
        }
        if (donations != null) {
            whitelist.setProperty(player + ".vip.donations", vip);
        }

        if (register != null) {
            whitelist.setProperty(player + ".dates.register", register);
        }
        if (lastlogin != null) {
            whitelist.setProperty(player + ".dates.lastlogin", lastlogin);
        }

        if (refererby != null) {
            whitelist.setProperty(player + ".referer.refererby", refererby);
        }
        if (referers != null) {
            whitelist.setProperty(player + ".referer.referers", referers);
        }

        if (warnings != null) {
            whitelist.setProperty(player + ".warnings", warnings);
        }
        if (comments != null) {
            whitelist.setProperty(player + ".comments", comments);
        }
        if (likes != null) {
            whitelist.setProperty(player + ".likes", likes);
        }

        whitelist.save();

        result = "ok";
        return result;
    }

    public void updateLastLogin(String player) {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yy");
        String lastlogin = Calendar.getInstance().getTimeInMillis() + "," + formatter.format(today);
        whitelist.setProperty(player + ".dates.lastlogin", lastlogin);
        whitelist.save();
    }

    private boolean keyExists(String key) {
        if (whitelist.getKeys(key) != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean inWhitelist(String player) {
        reload();
        if (keyExists(player)) {
            return whitelist.getBoolean(player + ".enabled", false);
        } else {
            return false;
        }
    }

    public boolean myInWhitelist(String player) {
        player = getPlayerName(player);
        return inWhitelist(player);
    }

    public void reload() {
        whitelist = new Configuration(file);
        whitelist.load();
    }

    public void myReload() {
        file.delete();
        whitelist = new Configuration(file);
        whitelist.save();
        update();
    }

    public String exportDefault() {
        String result;

        result = "ok";
        return result;
    }

    public String importDefault() {
        String result;
        List<String> def = loadDefaultWhitelist();
        for (String player : def) {
            addDefaultPlayer(player);
            MilkAdminLog.info("Imported: " + player);
        }
        result = "ok";
        return result;
    }

    public String count() {
        return String.valueOf(whitelist.getKeys(null).size());
    }

    private List<String> loadDefaultWhitelist() {
        String line;
        BufferedReader fin;
        List<String> players = new ArrayList<String>();

        try {
            fin = new BufferedReader(new FileReader("white-list.txt"));
        } catch (FileNotFoundException e) {
            MilkAdminLog.warning("ERROR in loadDefaultWhitelist()", e);
            return new ArrayList<String>();
        }
        try {
            while ((line = fin.readLine()) != null) {
                if (!line.equals("")) {
                    players.add(line.trim());
                }
            }

        } catch (Exception e) {
            MilkAdminLog.warning("ERROR in loadDefaultWhitelist()", e);
            return new ArrayList<String>();
        } finally {
            try {
                fin.close();
            } catch (IOException e) {
                MilkAdminLog.warning("ERROR in loadDefaultWhitelist()", e);
            }
        }
        return players;
    }

    private void whiteListedPlayers() {
        String name = "", uuid = "";
        whitelistMapName_Uuid.clear();
        whitelistMapUuid_Name.clear();
        Set<OfflinePlayer> whitelistedPlayers = new HashSet<OfflinePlayer>(Bukkit.getServer().getWhitelistedPlayers());
        for (OfflinePlayer offlinePlayer : whitelistedPlayers) {
            if (offlinePlayer.isWhitelisted()) {
                name = offlinePlayer.getName();
                try {
                    uuid = UUIDFetcher.getUUIDOf(name).toString();
                } catch (Exception ex) {
                    uuid = "false";
                }
                whitelistMapName_Uuid.put(name, uuid);
                whitelistMapUuid_Name.put(uuid, name);
                this.addDefaultPlayer(name);
            }
        }
    }

    private void whiteListedPlayers(List<String> whitelistedPlayers) {
        String uuid = "";
        whitelistMapName_Uuid.clear();
        whitelistMapUuid_Name.clear();
        String myname = "";
        for (String name : whitelistedPlayers) {
            myname = getPlayerName(name);
            try {
                uuid = UUIDFetcher.getUUIDOf(myname).toString();
            } catch (Exception ex) {
                uuid = "false";
            }
            whitelistMapName_Uuid.put(myname, uuid);
            whitelistMapUuid_Name.put(uuid, myname);
            this.addDefaultPlayer(myname);
        }
    }

    private void whitelistPlayersList(List<String> whitelistedPlayers, boolean allPlayers) {
        whiteListedPlayers();
        whitelistedPlayersList();
        List<String> currentPlayers = getWhitelistedPlayersList();
        whiteListedPlayers(whitelistedPlayers);
        whitelistedPlayersList();
        List<String> whitelistedPlayersList = getWhitelistedPlayersList();
        ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
        Server server = Bukkit.getServer();
        for (String name : currentPlayers) {
            if (!whitelistedPlayersList.contains(name)) {
                server.dispatchCommand(consoleSender, "whitelist remove " + name);
                server.dispatchCommand(consoleSender, "kick " + name);
                this.removePlayer(name);
            }

        }
        for (String name : whitelistedPlayersList) {
            server.dispatchCommand(consoleSender, "whitelist add " + name);
            this.addDefaultPlayer(name);
        }


        if (allPlayers) {
            List<String> players = getPlayers();
            String myname = "";
            for (String name : players) {
                myname = getPlayerName(name);
                if (!whitelistedPlayersList.contains(myname)) {
                    server.dispatchCommand(consoleSender, "whitelist remove " + myname);
                    server.dispatchCommand(consoleSender, "kick " + myname);
                    this.removePlayer(myname);
                }

            }
            for (String name : players) {
                myname = getPlayerName(name);
                server.dispatchCommand(consoleSender, "whitelist add " + myname);
                this.addDefaultPlayer(myname);
            }
        }
    }

    public void removeAllWhiteListedPlayers() {
        this.removeoperation=true;
        List<String> players = getPlayers();
        for(String player:players)
        {
            myRemovePlayer(player);
        }
        this.whitelist.save();
        this.removeoperation=false;
    }

    private String getPlayerName(String name) {
        String uuid = null;
        String myname = null;
        UUID myuuid = null;
        try {
            myuuid = UUIDFetcher.getUUIDOf(name);
            uuid = myuuid.toString();
            myname = name;
        } catch (Throwable ex) {
            uuid = null;
            myuuid = null;
            myname = null;
        }
        if (myuuid == null) {
            try {
                myuuid = UUID.fromString(name);
                OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(myuuid);
                if (!(offlinePlayer instanceof OfflinePlayer)) {
                    Player player = Bukkit.getServer().getPlayer(myuuid);
                    myname = player.getName();

                } else {
                    myname = offlinePlayer.getName();
                }
            } catch (Throwable ex) {
                myname = name;
                return myname;
            }
            return myname;
        } else {
            myname = name;
            return myname;
        }

    }

    private void whitelistedPlayersList() {
        HashSet<String> hashSet = new HashSet<String>(whitelistMapName_Uuid.keySet());
        whitelistPlayers.clear();
        for (String player : hashSet) {
            whitelistPlayers.add(player);
        }
    }

    public void update() {
        if(!removeoperation)
        {
        this.whiteListedPlayers();
        this.whitelistedPlayersList();
        List<String> whitelistedPlayersList = this.getWhitelistedPlayersList();
        List<String> players = getPlayers();
        this.clear();
        this.update(whitelistedPlayersList, players);
        this.whiteListedPlayers();
        this.whitelistedPlayersList();
        }
    }

    public List<String> getWhitelistedPlayersList() {
        List<String> list = new CopyOnWriteArrayList<String>();
        list.addAll(whitelistPlayers);
        return list;
    }

    public void updateLists(List<String> players, boolean allPlayers) {
        if(!removeoperation)
        whitelistPlayersList(players, allPlayers);
    }

    public void addUserMap(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(playerName, uuid);
        this.whitelistMapUuid_Name.put(uuid, playerName);
    }

    public void removeUserMap(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(playerName);
        this.whitelistMapUuid_Name.remove(uuid);
    }

    public void addUserList(String name) {
        name = this.getPlayerName(name);
        this.whitelistPlayers.add(name);
    }

    public void removeUserList(String name) {
        name = this.getPlayerName(name);
        this.whitelistPlayers.remove(name);
    }

    public void addUser(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(playerName, uuid);
        this.whitelistMapUuid_Name.put(uuid, playerName);
        this.whitelistPlayers.add(playerName);
    }

    public void removeUser(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(playerName);
        this.whitelistMapUuid_Name.remove(uuid);
        this.whitelistPlayers.remove(playerName);
    }

    public void addUserMapW(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(name, uuid);
        this.whitelistMapUuid_Name.put(uuid, name);
    }

    public void removeUserMapW(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(name);
        this.whitelistMapUuid_Name.remove(uuid);
    }

    public void addUserListW(String name) {
        this.whitelistPlayers.add(name);
    }

    public void removeUserListW(String name) {
        this.whitelistPlayers.remove(name);
    }

    public void addUserW(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(name, uuid);
        this.whitelistMapUuid_Name.put(uuid, name);
        this.whitelistPlayers.add(name);
    }

    public void removeUserW(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(name);
        this.whitelistMapUuid_Name.remove(uuid);
        this.whitelistPlayers.remove(name);
    }

    public void addUserMapC(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(playerName, uuid);
        this.whitelistMapUuid_Name.put(uuid, playerName);
        this.addUserConsole(playerName);
    }

    public void removeUserMapC(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(playerName);
        this.whitelistMapUuid_Name.remove(uuid);
        this.removeUserConsole(playerName);
    }

    public void addUserListC(String name) {
        name = this.getPlayerName(name);
        this.whitelistPlayers.add(name);
        this.addUserConsole(name);
    }

    public void removeUserListC(String name) {
        name = this.getPlayerName(name);
        this.whitelistPlayers.remove(name);
        this.removeUserConsole(name);
    }

    public void addUserC(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(playerName, uuid);
        this.whitelistMapUuid_Name.put(uuid, playerName);
        this.whitelistPlayers.add(playerName);
        this.addUserConsole(playerName);
    }

    public void removeUserC(String name) {
        String playerName = this.getPlayerName(name);
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(playerName).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(playerName);
        this.whitelistMapUuid_Name.remove(uuid);
        this.whitelistPlayers.remove(playerName);
        this.removeUserConsole(playerName);
    }

    public void addUserMapWC(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(name, uuid);
        this.whitelistMapUuid_Name.put(uuid, name);
        this.addUserConsole(name);
    }

    public void removeUserMapWC(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(name);
        this.whitelistMapUuid_Name.remove(uuid);
        this.removeUserConsole(name);
    }

    public void addUserListWC(String name) {
        this.whitelistPlayers.add(name);
        this.addUserConsole(name);
    }

    public void removeUserListWC(String name) {
        this.whitelistPlayers.remove(name);
        this.removeUserConsole(name);
    }

    public void addUserWC(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.put(name, uuid);
        this.whitelistMapUuid_Name.put(uuid, name);
        this.whitelistPlayers.add(name);
        this.addUserConsole(name);
    }

    public void removeUserWC(String name) {
        String uuid = "false";
        try {
            uuid = UUIDFetcher.getUUIDOf(name).toString();
        } catch (Exception ex) {
            uuid = "false";
        }
        this.whitelistMapName_Uuid.remove(name);
        this.whitelistMapUuid_Name.remove(uuid);
        this.whitelistPlayers.remove(name);
        this.removeUserConsole(name);
    }

    public void addUserConsole(String name) {
        ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
        Bukkit.getServer().dispatchCommand(consoleSender, "whitelist add " + name);
        Bukkit.getServer().dispatchCommand(consoleSender, "kick " + name);
    }

    public void removeUserConsole(String name) {
        ConsoleCommandSender consoleSender = Bukkit.getServer().getConsoleSender();
        Bukkit.getServer().dispatchCommand(consoleSender, "whitelist remove " + name);
    }

    public void update(List<String> whitelistedPlayers, List<String> players) {
        List<String> myPlayers = new CopyOnWriteArrayList<String>();
        myPlayers.addAll(players);
        List<String> myWhitelistedPlayers = new CopyOnWriteArrayList<String>();
        myWhitelistedPlayers.addAll(whitelistedPlayers);
        for (String name : myPlayers) {
            if (!myWhitelistedPlayers.contains(name)) {
                this.myRemovePlayer(name);
            } else {
                this.myAddDefaultPlayer(name);
            }
        }
    }

    public List<String> getPlayers() {
        List<String> list = new CopyOnWriteArrayList<String>();
        OfflinePlayer[] offlinePlayers = Bukkit.getServer().getOfflinePlayers();
        for (OfflinePlayer offlinePlayer : offlinePlayers) {
            if (offlinePlayer != null && !offlinePlayer.isOnline()) {
                list.add(offlinePlayer.getName());
            }
        }

        Player[] players = WPlayerInterface.getOnlinePlayersOld();
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                list.add(player.getName());
            }
        }
        return list;
    }

    public void clear() {
        this.whitelistMapName_Uuid.clear();
        this.whitelistMapUuid_Name.clear();
        this.whitelistPlayers.clear();
    }

    private List<String> getWhitelist() {
        List<String> players = this.getPlayers();
        List<String> whitelistedPlayers = new CopyOnWriteArrayList<String>();
        for (String name : players) {
            if (this.inWhitelist(name)) {
                whitelistedPlayers.add(name);
            }
        }
        return whitelistedPlayers;
    }
}
