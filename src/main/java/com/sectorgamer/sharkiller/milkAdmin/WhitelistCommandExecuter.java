/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sectorgamer.sharkiller.milkAdmin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author ABC
 */
public class WhitelistCommandExecuter implements CommandExecutor {

    private MilkAdmin i;

    public WhitelistCommandExecuter(MilkAdmin i) {
        this.i = i;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        if (cmnd != null) {
            if (cmnd.getName().equalsIgnoreCase("whitelist") && cs.isOp()) {
                String command = "milkadmin whitelist ";
                if (strings != null) {
                    int length = strings.length;
                    switch (length) {
                        case 2: {
                            if (strings[0].equalsIgnoreCase("add")) {
                                this.i.WL.myAddDefaultPlayer(strings[1]);
                                cs.sendMessage("You have added " + strings[1] + " to the whitelist!");
                            } else if (strings[0].equalsIgnoreCase("remove")) {
                                this.i.WL.myRemovePlayer(strings[1]);
                                cs.sendMessage("You have removed " + strings[1] + " to the whitelist!");
                            }
                            break;
                        }
                        case 1: {
                            if (strings[0].equalsIgnoreCase("on")) {
                                this.i.Settings.set("Whitelist.Custom", true);
                                this.i.WLCustom=true;
                                this.i.MCWhitelist=true;
                                try {
                                    this.i.Settings.save(new File(this.i.PluginDir + File.separator + "settings.yml"));
                                } catch (IOException ex) {
                                    Logger.getLogger(WhitelistCommandExecuter.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                cs.sendMessage("Whitelist is on!");
                                String name = this.i.getName();
                                //Bukkit.getServer().getPluginManager().disablePlugin(i);
                                //Bukkit.getServer().getPluginManager().enablePlugin(Bukkit.getServer().getPluginManager().getPlugin(name));
                            } else if (strings[0].equalsIgnoreCase("off")) {
                                this.i.Settings.set("Whitelist.Custom", false);
                                this.i.WLCustom=false;
                                this.i.MCWhitelist=false;
                                try {
                                    this.i.Settings.save(new File(this.i.PluginDir + File.separator + "settings.yml"));
                                } catch (IOException ex) {
                                    Logger.getLogger(WhitelistCommandExecuter.class.getName()).log(Level.SEVERE, null, ex);
                                }
                               // String name = this.i.getName();
                                //Bukkit.getServer().getPluginManager().disablePlugin(i);
                               // Bukkit.getServer().getPluginManager().enablePlugin(Bukkit.getServer().getPluginManager().getPlugin(name));
                                cs.sendMessage("Whitelist is off!");
                            }
                        }
                        break;
                    }
                }
            }
        }
        return true;
    }
}
