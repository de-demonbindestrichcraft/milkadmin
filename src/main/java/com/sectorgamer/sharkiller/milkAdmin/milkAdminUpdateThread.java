/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sectorgamer.sharkiller.milkAdmin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author ABC
 */
public class milkAdminUpdateThread {

    private MilkAdmin milkAdminInstance;
    private List<String> players;

    public milkAdminUpdateThread(MilkAdmin i) {
        milkAdminInstance = i;
        players = new CopyOnWriteArrayList<String>();
        if (milkAdminInstance.WL != null) {
            List<String> whitelist = milkAdminInstance.WL.getWhitelist();
            if (!whitelist.isEmpty()) {
                setWhiteListedPlayersAsList(whitelist);
            }
        }
    }

    public void updateLists(final List<String> players) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (milkAdminInstance.WL != null) {
                    milkAdminInstance.WL.updateLists(players);
                    setWhiteListedPlayersAsList(players);
                }
            }
        }).start();
    }

    public void update() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (milkAdminInstance.WL != null) {
                    milkAdminInstance.WL.update();
                    List<String> whitelist = milkAdminInstance.WL.getWhitelist();
                    if (!whitelist.isEmpty()) {
                        setWhiteListedPlayersAsList(whitelist);
                    }
                }
            }
        }).start();
    }

    public void myAddDefaultPlayer(final String player) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (milkAdminInstance.WL != null) {
                    milkAdminInstance.WL.myAddDefaultPlayer(player);
                    addWhiteListedPlayersAsList(player);
                }
            }
        }).start();
    }

    public void myRemovePlayer(final String player) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (milkAdminInstance.WL != null) {
                    milkAdminInstance.WL.myRemovePlayer(player);
                    removeWhiteListedPlayersAsList(player);
                }
            }
        }).start();
    }

    public synchronized void setWhiteListedPlayersAsList(List<String> players) {
        this.players.clear();
        this.players.addAll(players);
    }

    public synchronized void addWhiteListedPlayersAsList(String player) {
        this.players.add(player);
    }

    public synchronized void removeWhiteListedPlayersAsList(String player) {
        this.players.remove(player);
    }

    public synchronized List<String> getWhiteListedPlayersAsList() {
        return players;
    }

    public void addAllWhiteListedPlayers() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (milkAdminInstance.WL != null) {
                    List<String> players1 = milkAdminInstance.WL.getPlayers();
                    milkAdminInstance.WL.updateLists(players1);
                    setWhiteListedPlayersAsList(players1);
                }
            }
        }).start();
    }

    public void removeAllWhiteListedPlayers() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (milkAdminInstance.WL != null) {
                    List<String> players1 = new CopyOnWriteArrayList<String>();
                    milkAdminInstance.WL.updateLists(players1);
                    setWhiteListedPlayersAsList(players1);
                }
            }
        }).start();
    }
}
