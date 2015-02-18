/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sectorgamer.sharkiller.milkAdmin;

import com.sectorgamer.sharkiller.milkAdmin.util.MilkAdminLog;
import de.demonbindestrichcraft.lib.bukkit.wbukkitlib.player.WPlayerInterface;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author ABC
 */
public class MilkAdminWebServer {

    public static void kickPlayer(final WebServer webServer, final String user, final String cause, final String KickedString) {
        String json = "";
        if (user.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                String kickString = KickedString;
                if (cause.length() > 0) {
                    kickString = cause;
                }
                p.kickPlayer(kickString);
                json = "ok:kickplayer:_NAME_," + user;
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void givePlayerItem(WebServer webServer, String user, String amount, String item) {
        String json = "";
        if (user.length() > 0 && amount.length() > 0 && item.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                p.getInventory().addItem(new ItemStack(Material.getMaterial(Integer.valueOf(item)), Integer.valueOf(amount)));
                json = "ok:itemsgiven:_NAME_," + user + ",_AMOUNT_," + amount + ",_ITEM_," + Material.getMaterial(Integer.valueOf(item));
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }

        webServer.print(json, "text/plain");
    }

    public static void removePlayerItem(WebServer webServer, String user, String amount, String item) {
        String json = "";
        if (user.length() > 0 && amount.length() > 0 && item.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                p.getInventory().removeItem(new ItemStack(Material.getMaterial(Integer.valueOf(item)), Integer.valueOf(amount)));
                json = "ok:itemsremoved:_NAME_," + user + ",_AMOUNT_," + amount + ",_ITEM_," + Material.getMaterial(Integer.valueOf(item));
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");

    }

    public static void getPlayerHealth(WebServer webServer, String user) {
        String json = "";
        if (user.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                json = "ok:" + String.valueOf(p.getHealth());
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void setPlayerHealth(WebServer webServer, String user, String amount) {
        String json = "";
        if (user.length() > 0 && amount.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                try {
                    int health = Integer.parseInt(amount, 10);
                    if (health >= 0 && health <= 20) {
                        p.setHealth(health);
                        if (health == 0) {
                            json = "ok:playerkilled:_NAME_," + user;
                        } else {
                            json = "ok:healthchanged:_NAME_," + user + ",_AMOUNT_," + amount;
                        }
                    } else {
                        json = "error:badparameters";
                    }
                } catch (NumberFormatException err) {
                    json = "error:badparameters";
                }
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void banPlayer(WebServer webServer, MilkAdmin milkAdminInstance, String user, String cause, String BannedString) {
        String json = "";
        if (user.length() > 0) {
            String banstring = BannedString;
            if (cause.length() > 0) {
                banstring = cause;
            }
            user = milkAdminInstance.BL.getPlayerName(user);
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                milkAdminInstance.BL.banListName.setString(p.getName(), banstring);
                p.kickPlayer(banstring);
                MilkAdminLog.info(p.getName() + " banned for: " + banstring);
            } else {
                milkAdminInstance.BL.banListName.setString(user, banstring);
            }
            json = "ok:playerbanned:_NAME_," + user;
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void banPlayerIp(WebServer webServer, MilkAdmin milkAdminInstance, String ip, String cause, String BannedString) {
        String json = "";
        if (ip.length() > 0) {
            String banstring = BannedString;
            if (cause.length() > 0) {
                banstring = cause;
            }
            ip = milkAdminInstance.BL.getPlayerName(ip);
            Player p = WPlayerInterface.getOnlinePlayerOld(ip);
            if (p != null && p.isOnline()) {
                milkAdminInstance.BL.banListIp.setString(String.valueOf(p.getAddress()).split("/")[1].split(":")[0], banstring);
                p.kickPlayer(banstring);
                MilkAdminLog.info(p.getName() + " banned for: " + banstring);
            } else {
                milkAdminInstance.BL.banListIp.setString(ip, banstring);
            }
            json = "ok:ipbanned:_IP_," + ip;
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void unbanPlayer(WebServer webServer, MilkAdmin milkAdminInstance, String user) {
        String json = "";
        user = milkAdminInstance.BL.getPlayerName(user);
        if (user.length() > 0) {
            if (milkAdminInstance.BL.banListName.keyExists(user)) {
                milkAdminInstance.BL.banListName.removeKey(user);
                json = "ok:playerunbanned:_NAME_," + user;
            } else {
                json = "error:playernotbanned";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void unbanIp(WebServer webServer, MilkAdmin milkAdminInstance, String ip) {
        String json = "";
        ip = milkAdminInstance.BL.getPlayerName(ip);
        if (ip.length() > 0) {
            if (milkAdminInstance.BL.banListIp.keyExists(ip)) {
                milkAdminInstance.BL.banListIp.removeKey(ip);
                json = "ok:ipunbanned:_IP_," + ip;
            } else {
                json = "error:ipnotbanned";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void listBans(WebServer webServer) {
        webServer.listBans();
    }

    public static void shootArrow(WebServer webServer, String user, int amount) {
        String json = "";
        if (user.length() > 0 && amount > 0 && amount < 1000) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                for (int i = 0; i < amount; i++) {
                    p.launchProjectile(org.bukkit.entity.Arrow.class);
                }
                json = "ok:arrowshooted";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void shootFireball(WebServer webServer, String user, int amount) {
        String json = "";
        if (user.length() > 0 && amount > 0 && amount < 1000) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                for (int i = 0; i < amount; i++) {
                    p.launchProjectile(org.bukkit.entity.Fireball.class);
                }

                json = "ok:fireballshooted";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void throwSnowball(WebServer webServer, String user, int amount) {
        String json = "";
        if (user.length() > 0 && amount > 0 && amount < 1000) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                for (int i = 0; i < amount; i++) {
                    p.launchProjectile(org.bukkit.entity.Snowball.class);
                }

                json = "ok:throwsnowball";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void throwEgg(WebServer webServer, String user, int amount) {
        String json = "";
        if (user.length() > 0 && amount > 0 && amount < 1000) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                for (int i = 0; i < amount; i++) {
                    p.launchProjectile(org.bukkit.entity.Egg.class);
                }

                json = "ok:throwegg";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void throwBomb(WebServer webServer, String user, int amount) {
        String json = "";
        if (user.length() > 0 && amount > 0 && amount < 1000) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                for (int i = 0; i < amount; i++) {
                    p.launchProjectile(org.bukkit.entity.SmallFireball.class);
                }

                json = "ok:throwegg";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void changeDisplayName(WebServer webServer, String user, String name) {
        String json = "";
        if (user.length() > 0 && name.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                p.setDisplayName(name);
                json = "ok:changename:_OLD_," + user + ",_NEW_," + name;
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void teleportToPlayer(WebServer webServer, MilkAdmin milkAdminInstance, String user, String touser) {
        String json = "";
        if (user.length() > 0 && touser.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            Player p2 = milkAdminInstance.getServer().getPlayer(touser);
            if (p != null && p2 != null && p.isOnline() && p2.isOnline()) {
                p.teleport(p2);
                json = "ok:playerteleported";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void teleportToLocation(WebServer webServer, String user, String x, String y, String z) {
        String json = "";
        if (user.length() > 0 && x.length() > 0 && y.length() > 1 && y.length() < 128 && z.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                p.teleport(new Location(p.getWorld(), Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z)));
                json = "ok:playerteleported";
            } else {
                json = "error:playernotconnected";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void playerIsOnline(WebServer webServer, String user) {
        String json = "";
        if (user.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                json = "ok:" + String.valueOf(p.isOnline());
            } else {
                json = "ok:false";
            }
        } else {
            json = "error:badparameters";
        }
        webServer.print(json, "text/plain");
    }

    public static void getIpPort(WebServer webServer, String user) {
        String json = "";
        if (user.length() > 0) {
            Player p = WPlayerInterface.getOnlinePlayerOld(user);
            if (p != null && p.isOnline()) {
                String ip_port = String.valueOf(p.getAddress()).split("/")[1];
                json = "{\"status\":\"ok\",\"ip\":\"" + ip_port.split(":")[0] + "\",\"port\":\"" + ip_port.split(":")[1] + "\"}";
            } else {
                json = "{\"status\":\"error\", \"error\":\"playernotconnected\"}";
            }
        } else {
            json = "{\"status\":\"error\", \"error\":\"badparameters\"}";
        }
        webServer.print(json, "application/json");
    }
}
