/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.session;

import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.files.Config;
import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.files.GenerallyFileManager;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ABC
 */
public class Session implements Serializable {

    private Map<String, String> sessionsUserPassword;
    private Map<String, String> sessionsUserTimestamp;
    private Map<String, String> sessionsHostaddressUsername;
    private List<String> loggedinUser;
    private List<String> loggedinIps;
    private String splitchar;
    private long timestampDiff;
    private File file;
    private static Session singleton = null;

    public Session(File file, long timestampDiff) {
        sessionsUserPassword = new HashMap<String, String>();
        sessionsUserTimestamp = new HashMap<String, String>();
        sessionsHostaddressUsername = new HashMap<String, String>();
        loggedinUser = new LinkedList<String>();
        loggedinIps = new LinkedList<String>();
        this.file = file;
        this.timestampDiff = timestampDiff;
    }

    public String getUsernameByIp(String ip) {
        return loggedinUser.get(loggedinIps.indexOf(ip));
    }

    public String getIpByUsername(String username) {
        return loggedinIps.get(loggedinUser.indexOf(username));
    }

    public void addUser(String user, String password, String hostAddress) {

        sessionsUserPassword.put(user, password);
        sessionsUserTimestamp.put(user, Long.toString(System.currentTimeMillis()));
        sessionsHostaddressUsername.put(hostAddress, user);
        loggedinUser.add(user);
        loggedinIps.add(hostAddress);
    }

    public void removeUser(String user, String ip) {
        sessionsUserPassword.remove(user);
        sessionsUserTimestamp.remove(user);
        sessionsHostaddressUsername.remove(ip);
        loggedinUser.remove(user);
        loggedinIps.remove(ip);
    }

    public void removeUserByUsername(String user) {
        String ip = getIpByUsername(user);
        removeUser(user, ip);
    }

    public void removeUserByIp(String ip) {
        String username = getUsernameByIp(ip);
        removeUser(username, ip);
    }

    public void update(Socket webServerSocket, File file1, File file2, File file3, String splitchar) {
        if (update(webServerSocket)) {
            writeSessionHostaddressUsernameToFile(file1, splitchar);
            writeSessionUsernamePasswordToFile(file2, splitchar);
            writeSessionUsernameTimestampToFile(file3, splitchar);
        }
    }

    public boolean update(Socket WebServerSocket) {
        if (timestampDiff <= 0L) {
            return false;
        }
        update(WebServerSocket, timestampDiff);
        return true;
    }

    private void update(Socket WebServerSocket, long timestampDiff) {
        String HostAddress = WebServerSocket.getInetAddress().getHostAddress();
        if (!loggedinIps.contains(HostAddress)) {
            return;
        }


        String username = getUsernameByIp(HostAddress);
        long get = Long.parseLong(sessionsUserTimestamp.get(username));
        long diff = System.currentTimeMillis() - get;
        if (diff >= timestampDiff) {
            removeUser(username, HostAddress);
        } else {
            String timestamp = Long.toString(System.currentTimeMillis());
            sessionsUserTimestamp.put(username, timestamp);
        }
    }

    public void save() throws IOException {
        if (file == null) {
            return;
        }
        save(file);
    }

    public void save(File file) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(this);
        oos.flush();
        oos.close();
    }

    public void load() throws IOException, ClassNotFoundException {
        if (!file.exists()) {
            return;
        }
        load(file);
    }

    public void load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        Session session = (Session) ois.readObject();
        loggedinIps = session.loggedinIps;
        loggedinUser = session.loggedinUser;
        sessionsHostaddressUsername = session.sessionsHostaddressUsername;
        sessionsUserPassword = session.sessionsUserPassword;
        sessionsUserTimestamp = session.sessionsUserTimestamp;
        ois.close();
    }

    public boolean hasSessionByUsername(String username) {
        return sessionsHostaddressUsername.containsValue(username);
    }

    public boolean hasSessionByIp(String ip) {
        return sessionsHostaddressUsername.containsKey(ip);
    }

    public boolean hasSession(String value) {
        return hasSessionByUsername(value) || hasSessionByIp(value);
    }

    public void writeSessionHostaddressUsernameToFile(File file, String splitchar) {
        GenerallyFileManager.FileWrite(sessionsHostaddressUsername, file, splitchar);
    }

    public void writeSessionUsernamePasswordToFile(File file, String splitchar) {
        GenerallyFileManager.FileWrite(sessionsHostaddressUsername, file, splitchar);
    }

    public void writeSessionUsernameTimestampToFile(File file, String splitchar) {
        GenerallyFileManager.FileWrite(sessionsHostaddressUsername, file, splitchar);
    }

    public void output(OutputStream out) {
        PrintStream printStream = new PrintStream(new BufferedOutputStream(out));
        printStream.println("loggedinIps:");
        printStream.println(loggedinIps);
        printStream.println("loggedinUser:");
        printStream.println(loggedinUser);
        printStream.println("sessionsHostaddressUsername:");
        printStream.println(sessionsHostaddressUsername);
        printStream.println("sessionsUserPassword:");
        printStream.println(sessionsUserPassword);
        printStream.println("sessionsUserTimestamp:");
        printStream.println(sessionsUserTimestamp);
        printStream.println("splitchar:");
        printStream.println(splitchar);
        printStream.println("timestampDiff:");
        printStream.println(timestampDiff);
        printStream.flush();
    }

    public static void createSingleton(File file, long timestampDiff) {
        if (singleton == null) {
            singleton = new Session(file, timestampDiff);
        }
    }

    public static Session getSingleton() {
        return singleton;
    }
}
