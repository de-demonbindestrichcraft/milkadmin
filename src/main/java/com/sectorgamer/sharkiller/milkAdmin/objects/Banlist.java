package com.sectorgamer.sharkiller.milkAdmin.objects;

import com.evilmidget38.UUIDFetcher;
import java.io.File;
import java.io.IOException;

import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;
import com.sectorgamer.sharkiller.milkAdmin.util.*;
import de.demonbindestrichcraft.lib.bukkit.wbukkitlib.player.WPlayerInterface;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Handle milkAdmin Ban list system.
 * @author Sharkiller
 */
public class Banlist {

    public PropertiesFile banListName;
    public PropertiesFile banListIp;
    private MilkAdmin plugin;

    public Banlist(MilkAdmin i) {
        this.plugin = i;
        banListName = new PropertiesFile(plugin.BLDir + File.separator + "banlistname.ini");
        banListIp = new PropertiesFile(plugin.BLDir + File.separator + "banlistip.ini");

        try {
            banListName.load();
            banListIp.load();
        } catch (IOException e) {
            MilkAdminLog.severe("Could not load banlist files!", e);
        }
        this.update();
    }

    public String count(boolean nick) throws Exception {
        if (nick) {
            return String.valueOf(banListName.returnMap().size());
        } else {
            return String.valueOf(banListIp.returnMap().size());
        }
    }

    /**
     * Check if player name or ip is in a banlist.
     * 
     * @param name Player name to check 
     * @param ip Ip address to check
     * @return Ban cause or null if not banned
     */
    public String isBanned(String name, String ip) {
        String ret = null;
        if (banListName.keyExists(name)) {
            ret = banListName.getString(name, plugin.BLMessage);
        } else if (banListIp.keyExists(ip)) {
            ret = banListIp.getString(ip, plugin.BLMessage);
        }

        return ret;
    }

    public void update() {
        String banstring = "murder";
        List<Player> players = getPlayers();
        for (Player p : players) {
            String user = p.getName();
            if (p != null && p.isOnline()) {
                if(p.isBanned())
                {
                    banListName.setString(user, banstring);
                    p.kickPlayer(banstring);
                    MilkAdminLog.info(user + " banned for: " + banstring);
                } else {
                    banListName.removeKey(user);
                }
            } else {
                if(p.isBanned())
                {
                    banListName.setString(user, banstring);
                } else {
                    banListName.removeKey(user);
                }
            }
        }
    }
    
    public void removeAllPlayersFromBanList()
    {
        this.banListIp.clear();
        this.banListName.clear();
    }

    public List<Player> getPlayers() {
        List<Player> list = new CopyOnWriteArrayList<Player>();

        Player[] players = WPlayerInterface.getOnlinePlayersOld();
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                list.add(player);
            }
        }
        return list;
    }

    public String getPlayerName(String name) {
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
}
