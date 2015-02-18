package com.sectorgamer.sharkiller.milkAdmin.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handle events for Ban list System.
 * @author Sharkiller
 */
public class BanlistListener implements Listener {

    private MilkAdmin plugin;
    private Map<String, String> ips;

    public BanlistListener(MilkAdmin i) {
        ips = new ConcurrentHashMap<String, String>();
        this.plugin = i;
    }

    /**
     * Handle events for Banlist System.
     * @author Sharkiller
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void banCheckLogin(PlayerLoginEvent event) {
        if (!event.getResult().equals(Result.ALLOWED)) {
            return;
        }
        Player player = event.getPlayer();
        String pName = player.getName();
        String pIp = "";
        String uuid = "";
        try {
            uuid = player.getUniqueId().toString();
        } catch (Throwable ex) {
            uuid = "false";
        }

        pIp = event.getKickMessage();
        if(ips.containsKey(pName.toLowerCase()))
        {
            pIp=ips.get(pName.toLowerCase());
        }

        String Cause = plugin.BL.isBanned(pName, pIp, uuid);

        if (Cause != null) {
            event.disallow(Result.KICK_OTHER, Cause);
        }

        if (plugin.BL.isBanned(event.getPlayer())) {
            event.disallow(Result.KICK_OTHER, Cause);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event == null) {
            return;
        }
        Player player = event.getPlayer();
        String ip = "";
        try {
            ip = player.getAddress().getAddress().getHostAddress();
            ips.put(player.getName().toLowerCase(), ip);
            if(plugin.BL.banListIp.containsKey(ip))
            {
                player.kickPlayer("Your ip " + ip + " is banned on this server!");
            }
        } catch (Throwable ex) {
            ip = "";
            ips.put(player.getName().toLowerCase(), ip);
        }
    }

    public static String getIp(Player player) {
        if (player != null && player.isOnline()) {
            String ip_port = String.valueOf(player.getAddress()).split("/")[1];
            String ip = ip_port.split(":")[0];
            System.out.println(ip);
            return ip;
        }
        System.out.println("dddd");
        return "";
    }
}
