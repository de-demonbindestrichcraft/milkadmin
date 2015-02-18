/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sectorgamer.sharkiller.milkAdmin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author ABC
 */
public class MilkAdminThread {

    private Plugin milkAdmin;
    private Runnable runnable;
    private boolean sync;
    private boolean old;

    public MilkAdminThread(Runnable runnable) {
        this.milkAdmin = MilkAdmin.getMilkAdminInstance();
        this.runnable = runnable;
        this.sync = true;
        this.old = false;
    }
    
    public MilkAdminThread(MilkAdmin milkAdmin, Runnable runnable, boolean sync) {
        this.milkAdmin = milkAdmin;
        this.runnable = runnable;
        this.sync = sync;
        this.old = false;
    }

    public MilkAdminThread(MilkAdmin milkAdmin, Runnable runnable, boolean sync, boolean old) {
        this(milkAdmin, runnable, sync);
        this.old = old;
    }

    public void start() {
        if (sync) {
            MilkAdminThread.sheduleSyncTask(milkAdmin, runnable);
        } else if (!sync && !old) {
            MilkAdminThread.sheduleAsyncTask(milkAdmin, runnable);
        } else if (!sync && old) {
            new Thread(runnable).start();
        }
    }
    
    public void setSync(boolean sync)
    {
        this.sync=sync;
    }
    
    public boolean getSync()
    {
        return sync;
    }

    public static void sheduleSyncTask(Plugin i, Runnable run) {
        Bukkit.getScheduler().runTask(i, run);
    }

    public static void sheduleAsyncTask(Plugin i, Runnable run) {
        Bukkit.getScheduler().runTaskAsynchronously(i, run);
    }
    
    
}
