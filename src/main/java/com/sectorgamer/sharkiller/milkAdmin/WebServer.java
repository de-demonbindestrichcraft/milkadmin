package com.sectorgamer.sharkiller.milkAdmin;

import com.evilmidget38.UUIDFetcher;
import com.sectorgamer.sharkiller.milkAdmin.MilkAdmin;
import java.net.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.regex.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;

import com.sectorgamer.sharkiller.milkAdmin.rtk.*;
import com.sectorgamer.sharkiller.milkAdmin.util.*;
import de.demonbindestrichcraft.lib.bukkit.wbukkitlib.player.WPlayerInterface;
import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.files.GenerallyFileManager;
import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.session.Session;
import de.demonbindestrichcraft.werri.lib.bukkit.wbukkitlib.common.session.ThreadSafeSession;
import java.util.concurrent.CopyOnWriteArrayList;
import sun.awt.image.ImageWatched.WeakLink;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

/**
 * Simple <code>WebServer</code> All-In-One.
 * @author Sharkiller
 */
public class WebServer extends Thread implements RTKListener {

    private int WebServerMode;
    private MilkAdmin milkAdminInstance;
    private Socket WebServerSocket;
    private ServerSocket rootSocket;
    private static Logger log = Logger.getLogger("Minecraft");
    private String Lang;
    private boolean Debug = true;
    private static InetAddress Ip = null;
    private int Port;
    private int consoleLines;
    private String BannedString;
    private String KickedString;
    private String levelname;
    private static final String PluginDir = "plugins/milkAdmin/";
    private String BackupPath;
    private String ExternalUrl;
    private String BanListDir;
    private Configuration Settings;
    private Configuration Worlds;
    private PropertiesFile BukkitProperties;
    private String bannedplayers;
    private ArrayList<String> bannedPlayers;
    private String bannedips;
    private ArrayList<String> bannedIps;
    private NoSavePropertiesFile adminList;
    private PropertiesFile saveAdminList;
    private PropertiesFile LoggedIn;
    private static File loggedinFile = new File(PluginDir + "loggedin.ini");
    ;
    private static File loggedinUserPasswordFile = new File(PluginDir + "loggedin_user_password.ini");
    ;
    private static File loggedinUserTimestampFile = new File(PluginDir + "loggedin_user_timestamp.ini");
    private static final String splitchar = "=";
    private static final boolean updateLoggedinFile = true;
    private static final boolean updateUserPasswordFile = true;
    private static final boolean updateUserTimestampFile = true;
    private static List<String> bannedUpdatedRequests = new LinkedList<String>();
    private milkAdminUpdateThread milkAdminUpdateThreadC = null;
    private boolean useCustomWhitelist = false;

    /**
     * Create the socket and listens for a connection.
     * 
     * @param i milkAdmin instance.
     */
    public WebServer() {
        /*File file = new File("white-list.txt");
        if (!file.exists()) {
        try {
        file.createNewFile();
        } catch (IOException ex) {
        Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        }*/
        rootSocket = null;
        BackupPath = "Backups [milkAdmin]";
        ExternalUrl = "";
        Settings = new Configuration(new File(PluginDir + "settings.yml"));
        Worlds = new Configuration(new File(PluginDir + "worlds.yml"));
        BukkitProperties = new PropertiesFile("server.properties");
        bannedplayers = "banned-players.txt";
        bannedPlayers = new ArrayList<String>();
        bannedips = "banned-ips.txt";
        bannedIps = new ArrayList<String>();
        adminList = new NoSavePropertiesFile(PluginDir + "admins.ini");
        saveAdminList = new PropertiesFile(PluginDir + "admins.ini");
        LoggedIn = new PropertiesFile(PluginDir + "loggedin.ini");
        milkAdminUpdateThreadC = null;
    }

    public WebServer(MilkAdmin i) {
        this();
        WebServerMode = 0;
        milkAdminInstance = i;
        milkAdminUpdateThreadC = null;
        start();
    }

    /**
     * Process the GET request.
     * 
     * @param i milkAdmin instance.
     * @param s Socket with the request.
     */
    public WebServer(MilkAdmin i, Socket s) {
        this();
        WebServerMode = 1;
        milkAdminInstance = i;
        milkAdminUpdateThreadC = null;
        WebServerSocket = s;
        start();
    }

    public void debug(String text) {
        if (Debug) {
            MilkAdminLog.debug(text);
        }
    }

    public String readFileAsString(String filePath)
            throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(65536);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            char[] buf = new char[65536];
            int length;

            while ((length = reader.read(buf)) > -1) {
                fileData.append(String.valueOf(buf, 0, length).replaceAll("_ExternalUrl_", ExternalUrl));
            }
            reader.close();
        } catch (Exception e) {
            debug("ERROR in readFileAsString(): " + e.getMessage());
        }
        return fileData.toString();
    }

    public void readFileAsBinary(String path, String type)
            throws java.io.IOException {
        readFileAsBinary(path, type, false);
    }

    public void readFileAsBinary(String path, String type, boolean replace)
            throws java.io.IOException {
        try {
            File archivo = new File(path);
            String StringData = new String("");
            long lengthData;
            if (archivo.exists()) {
                FileInputStream file = new FileInputStream(archivo);
                byte[] fileData = new byte[65536];
                int length;

                if (replace) {
                    while ((length = file.read(fileData)) > 0) {
                        String aux = new String(fileData, 0, length);
                        StringData = StringData + aux.replaceAll("_ExternalUrl_", ExternalUrl);
                    }
                    lengthData = StringData.length();
                } else {
                    lengthData = archivo.length();
                }

                DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                if (type != null) {
                    out.writeBytes("Content-Type: " + type + "; charset=utf-8\r\n");
                }
                out.writeBytes("Content-Length: " + lengthData + "\r\n");
                out.writeBytes("Cache-Control: no-cache, must-revalidate\r\n");
                out.writeBytes("Server: milkAdmin Webserver\r\n");
                out.writeBytes("Connection: Close\r\n\r\n");

                if (replace) {
                    out.writeBytes(StringData);
                } else {
                    while ((length = file.read(fileData)) > 0) {
                        out.write(fileData, 0, length);
                    }
                }
                out.flush();

                file.close();
                out.close();
            } else {
                httperror("404 Not Found");
            }
        } catch (Exception e) {
            debug("ERROR in readFileAsBinary(): " + e.getMessage());
        }
    }

    public void onRTKStringReceived(String s) {
        debug("From wrapper: " + s);
    }

    public void consoleCommand(String cmd) {
        milkAdminInstance.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    public String readConsole() {
        String mc_172 = "logs" + File.separator + "latest.log";
        String mc_164 = "server.log";
        String console = readConsole(mc_172);
        console = convertUnixToNothing1(convertUnixToNothing(console));
        return console;
    }

    public String readConsole(String file) {
        String console = "";
        String line = "";
        try {
            File f = new File(file);
            RandomAccessFile randomFile = new RandomAccessFile(f, "r");
            long numberOfLines = Long.valueOf(consoleLines).longValue();
            long fileLength = randomFile.length();
            long startPosition = fileLength - (numberOfLines * 100);
            if (startPosition < 0) {
                startPosition = 0;
            }
            randomFile.seek(startPosition);
            while ((line = randomFile.readLine()) != null) {
                console = console + (line + "\n");
            }
            randomFile.close();
        } catch (Exception e) {
            debug("ERROR in readConsole(): " + e.getMessage());
        }
        return console;
    }

    public static String convertUnixToNothing(String data) {
        data = data.replaceAll("(\\[[0-1];[3-4][0-9];1m)", "").replaceAll("\\[m", "");
        return data;
    }

    public static String convertUnixToNothing1(String data) {
        data = data.replaceAll("\\[m", "");
        return data;
    }

    public static String convertUnixToHtmlColors(String data) {
        String BLACK_UNIX = "0;30";
        String DARK_GRAY_UNIX = "1;30";
        String BLUE_UNIX = "0;34";
        String LIGHT_BLUE_UNIX = "1;34";
        String GREEN_UNIX = "0;32";
        String LIGHT_GREEN_UNIX = "1;32";
        String CYAN_UNIX = "0;36";
        String LIGHT_CYAN_UNIX = "1;36";
        String RED_UNIX = "0;31";
        String LIGHT_RED_UNIX = "1;31";
        String PURPLE_UNIX = "0;35";
        String LIGHT_PURPLE_UNIX = "1;35";
        String BROWN_UNIX = "0;33";
        String YELLOW_UNIX = "1;33";
        String LIGHT_GRAY_UNIX = "0;37";
        String WHITE_UNIX = "1;37";
        String BLACK_HEX = "000000";
        String DARK_GRAY_HEX = "030303";
        String BLUE_HEX = "0000CD";
        String LIGHT_BLUE_HEX = "ADD8E6";
        String GREEN_HEX = "GH";
        String LIGHT_GREEN_HEX = "LGH";
        String CYAN_HEX = "CH";
        String LIGHT_CYAN_HEX = "LCH";
        String RED_HEX = "RH";
        String LIGHT_RED_HEX = "LRH";
        String PURPLE_HEX = "PH";
        String LIGHT_PURPLE_HEX = "LPH";
        String BROWN_HEX = "BH";
        String YELLOW_HEX = "YH";
        String LIGHT_GRAY_HEX = "bebebe";
        String WHITE_HEX = "ffffff";
        String[] unixColors = {BLACK_UNIX, DARK_GRAY_UNIX, BLUE_UNIX, LIGHT_BLUE_UNIX,
            GREEN_UNIX, LIGHT_GREEN_UNIX, CYAN_UNIX, LIGHT_CYAN_UNIX,
            RED_UNIX, LIGHT_RED_UNIX, PURPLE_UNIX, LIGHT_PURPLE_UNIX,
            BROWN_UNIX, YELLOW_UNIX, LIGHT_GRAY_UNIX, WHITE_UNIX};
        String[] hexColors = {BLACK_HEX, DARK_GRAY_HEX, BLUE_HEX, LIGHT_BLUE_HEX,
            GREEN_HEX, LIGHT_GREEN_HEX, CYAN_HEX, LIGHT_CYAN_HEX,
            RED_HEX, LIGHT_RED_HEX, PURPLE_HEX, LIGHT_PURPLE_HEX,
            BROWN_HEX, YELLOW_HEX, LIGHT_GRAY_HEX, WHITE_HEX};
        int length = unixColors.length;
        String searcher = "(\\[[0-1];[3-4][0-9];1m)";
        Matcher matcher = Pattern.compile(searcher).matcher(data);
        StringBuffer my = new StringBuffer();
        int gc = 0;
        while (matcher.find()) {
            String group = matcher.group();
            String add = "";
            switch (gc) {
                case 0: {
                    gc++;
                }
                break;

                default: {
                    add += "</font>";
                }
            }
            for (int i = 0; i < length; i++) {
                if (group.contains(unixColors[i])) {
                    group = "<font color=\"" + hexColors[i] + "\">";
                }
            }
            matcher.appendReplacement(my, add + group);
        }
        matcher.appendTail(my);
        my.append("</font>");
        data = my.toString();
        return data;
    }

    public String lastConsoleLine() {
        String mc_172 = "logs" + File.separator + "latest.log";
        String mc_164 = "server.log";
        String lastConsoleLine = lastConsoleLine(mc_172);
        lastConsoleLine = convertUnixToNothing1(convertUnixToNothing(lastConsoleLine));
        return lastConsoleLine;
    }

    public String lastConsoleLine(String file) {
        String console = "";
        String lastline = "";
        try {
            File f = new File(file);
            RandomAccessFile randomFile = new RandomAccessFile(f, "r");
            long fileLength = randomFile.length();
            long startPosition = fileLength - 200;
            if (startPosition < 0) {
                startPosition = 0;
            }
            randomFile.seek(startPosition);
            while ((lastline = randomFile.readLine()) != null) {
                console = lastline;
            }
            randomFile.close();
        } catch (Exception e) {
            debug("ERROR in lastConsoleLine(): " + e.getMessage());
        }
        return console;
    }

    public String infoProperties() throws IOException {
        BukkitProperties.load();
        String ip = BukkitProperties.getString("server-ip", "");
        String port = BukkitProperties.getString("server-port", "25565");
        String maxplayers = BukkitProperties.getString("max-players", "10");
        String viewdistance = BukkitProperties.getString("view-distance", "10");
        String holdmessage = BukkitProperties.getString("hold-message", "");
        boolean allownether = BukkitProperties.getBoolean("allow-nether", true);
        boolean spawnmonsters = BukkitProperties.getBoolean("spawn-monsters", false);
        boolean spawnanimals = BukkitProperties.getBoolean("spawn-animals", false);
        boolean onlinemode = BukkitProperties.getBoolean("online-mode", false);
        boolean pvp = BukkitProperties.getBoolean("pvp", false);
        boolean flight = BukkitProperties.getBoolean("allow-flight", false);
        boolean whitelist = BukkitProperties.getBoolean("white-list", false);
        if (!whitelist) {
            if (this.milkAdminInstance != null) {
                if (this.milkAdminInstance.WLCustom) {
                    this.useCustomWhitelist = true;
                    whitelist = true;
                } else {
                    this.useCustomWhitelist = false;
                    whitelist = false;
                }
            }
        } else {
            this.useCustomWhitelist = false;
        }


        String json = "{\"ip\":\"" + ip + "\","
                + "\"port\":\"" + port + "\","
                + "\"maxplayers\":\"" + maxplayers + "\","
                + "\"viewdistance\":\"" + viewdistance + "\","
                + "\"holdmessage\":\"" + holdmessage + "\","
                + "\"levelname\":\"" + levelname + "\","
                + "\"allownether\":\"" + allownether + "\","
                + "\"spawnmonsters\":\"" + spawnmonsters + "\","
                + "\"spawnanimals\":\"" + spawnanimals + "\","
                + "\"onlinemode\":\"" + onlinemode + "\","
                + "\"pvp\":\"" + pvp + "\","
                + "\"flight\":\"" + flight + "\","
                + "\"whitelist\":\"" + whitelist + "\"}";

        return json;
    }

    public String infoData() {
        String data = "";
        String build = "???";
        String freespace = "1";
        String totalspace = "1";
        String usedspace = "0";
        try {
            String version = "";
            if (milkAdminInstance != null) {
                version = milkAdminInstance.getServer().getVersion();
            }
            Matcher result = Pattern.compile("b([0-9]+)jnks").matcher(version);
            result.find();
            try {
                build = result.group(1);
            } catch (IllegalStateException e) {
            }
            String totmem = String.valueOf(Runtime.getRuntime().totalMemory() / 1024 / 1024);
            String maxmem = String.valueOf(Runtime.getRuntime().maxMemory() / 1024 / 1024);
            String freemem = String.valueOf(Runtime.getRuntime().freeMemory() / 1024 / 1024);
            String usedmem = String.valueOf((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
            File Disk = new File(BackupPath);
            try {
                if (!Disk.exists()) {
                    Disk.mkdir();
                }
                double fs = (double) (Disk.getFreeSpace() / 1024 / 1024) / 1024;
                double ts = (double) (Disk.getTotalSpace() / 1024 / 1024) / 1024;
                double us = ts - fs;
                freespace = String.format("%.2f%n", fs).trim();
                totalspace = String.format("%.2f%n", ts).trim();
                usedspace = String.format("%.2f%n", us).trim();
            } catch (SecurityException e) {
                debug("Security Exception in Space Data");
            }

            String users = "[]";
            Player[] onlinePlayersOld = WPlayerInterface.getOnlinePlayersOld();
            if (onlinePlayersOld == null) {
                onlinePlayersOld = new Player[]{};
            }
            String amountusers = String.valueOf(onlinePlayersOld.length);
            if (onlinePlayersOld.length > 0) {
                users = "[";
                Player[] p = onlinePlayersOld;
                for (int i = 0; i < p.length; i++) {
                    if (p[i] == null) {
                        continue;
                    }
                    users = users + "\"" + p[i].getName() + "\"";
                    if (i < p.length - 1) {
                        users = users + ", ";
                    }
                }
                users = users + "]";
            }
            boolean usingrtk = false;
            String initTime = "";
            if (MilkAdmin.initTime != null) {
                initTime = MilkAdmin.initTime;
            }
            if (milkAdminInstance != null) {
                usingrtk = milkAdminInstance.UsingRTK;
            } else {
                usingrtk = false;
            }
            String infoProperties = "";
            try {
                infoProperties = infoProperties();
            } catch (Exception ex) {
                infoProperties = "";
            }
            data = "{\"lastrestart\":\"" + initTime + "\","
                    + "\"version\":\"" + version + "\","
                    + "\"build\":\"" + build + "\","
                    + "\"totmem\":\"" + totmem + "\","
                    + "\"maxmem\":\"" + maxmem + "\","
                    + "\"freemem\":\"" + freemem + "\","
                    + "\"usedmem\":\"" + usedmem + "\","
                    + "\"freespace\":\"" + freespace + "\","
                    + "\"totalspace\":\"" + totalspace + "\","
                    + "\"usedspace\":\"" + usedspace + "\","
                    + "\"amountusers\":\"" + amountusers + "\","
                    + "\"users\":" + users + ","
                    + "\"usingrtk\":" + usingrtk + ","
                    + "\"properties\":" + infoProperties + "}";
        } catch (Exception e) {
            debug("ERROR in infoData(): " + e.getMessage());
        }
        return data;
    }

    public void readLine(String path, ArrayList<String> save) {
        try {
            save.clear();
            File banlist = new File(path);
            if (banlist.exists()) {
                BufferedReader in = new BufferedReader(new FileReader(banlist));
                String data = null;
                while ((data = in.readLine()) != null) {
                    //Checking for blank lines
                    if (data.length() > 0) {
                        save.add(data);
                    }
                }
                in.close();
            }
        } catch (IOException e) {
            debug("ERROR in readLine(): " + e.getMessage());
        }
    }

    public List<String> loadWhitelist() {
        /*String line;
        BufferedReader fin;
        List<String> players = new ArrayList<String>();
        
        try {
        fin = new BufferedReader(new FileReader("white-list.txt"));
        } catch (FileNotFoundException e) {
        debug("ERROR in loadWhitelist(): " + e.getMessage());
        return new ArrayList<String>();
        }
        try {
        while ((line = fin.readLine()) != null) {
        if (!line.equals("")) {
        players.add(line.trim());
        }
        }
        
        } catch (Exception e) {
        debug("ERROR in loadWhitelist(): " + e.getMessage());
        return new ArrayList<String>();
        } finally {
        try {
        fin.close();
        } catch (IOException e) {
        debug("ERROR in loadWhitelist(): " + e.getMessage());
        }
        }
        return players;*/
        if (milkAdminInstance != null) {
            if (milkAdminUpdateThreadC == null) {
                milkAdminUpdateThreadC = new milkAdminUpdateThread(milkAdminInstance);
            }
            milkAdminUpdateThreadC.update();
            return milkAdminUpdateThreadC.getWhiteListedPlayersAsList();
        } else {
            return new CopyOnWriteArrayList<String>();
        }
    }

    public boolean saveWhitelist(List<String> players) {
        /*final String newLine = System.getProperty("line.separator");
        try {
        BufferedWriter writer = new BufferedWriter(new FileWriter("white-list.txt"));
        for (String player : players) {
        writer.write(player + newLine);
        }
        writer.flush();
        writer.close();
        return true;
        } catch (Exception e) {
        debug("ERROR in saveWhitelist(): " + e.getMessage());
        return false;
        }*/

        if (milkAdminInstance != null) {
            if (milkAdminUpdateThreadC == null) {
                milkAdminUpdateThreadC = new milkAdminUpdateThread(milkAdminInstance);
            }
            milkAdminUpdateThreadC.updateLists(players);
        }
        return true;
    }

    public void addToWhitelist(String user) {
        //user=milkAdminInstance.BL.getPlayerName(user);
        if (milkAdminInstance != null) {
            if (milkAdminUpdateThreadC == null) {
                milkAdminUpdateThreadC = new milkAdminUpdateThread(milkAdminInstance);
            }
            milkAdminUpdateThreadC.myAddDefaultPlayer(user);
        }
        /*File file = new File("white-list.txt");
        try {
        FileWriter writer = new FileWriter(file, true);
        writer.write(user + System.getProperty("line.separator"));
        writer.flush();
        writer.close();
        } catch (IOException e) {
        debug("ERROR in saveWhitelist(): " + e.getMessage());
        }*/
    }

    public void listBans() {
        //[{"players":[{"name":"pepito"},{"name":"sharkale31"}]},{"ips":[{"ip":"127.0.0.1"},{"ip":"127.0.0.2"}]}]
        String listban = "";
        Iterator<Map.Entry<String, String>> i;
        Map.Entry<String, String> e;
        try {
            debug("Writing listbans.");
            // names
            Map<String, String> banNames = milkAdminInstance.BL.banListName.returnMap();
            i = banNames.entrySet().iterator();
            listban = "[{\"players\":[";
            while (i.hasNext()) {
                e = i.next();
                listban = listban + "{\"name\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
                if (i.hasNext()) {
                    listban = listban + ",";
                }
            }
            listban = listban + "]},";
            // ips
            Map<String, String> banIps = milkAdminInstance.BL.banListIp.returnMap();
            i = banIps.entrySet().iterator();
            listban = listban + "{\"ips\":[";
            while (i.hasNext()) {
                e = i.next();
                listban = listban + "{\"ip\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
                if (i.hasNext()) {
                    listban = listban + ",";
                }
            }
            listban = listban + "]},";
            Map<String, String> banUuid = milkAdminInstance.BL.banListUuid.returnMap();
            i = banUuid.entrySet().iterator();
            listban = listban + "{\"uuids\":[";
            while (i.hasNext()) {
                e = i.next();
                listban = listban + "{\"uuid\":\"" + e.getKey() + "\",\"cause\":\"" + e.getValue() + "\"}";
                if (i.hasNext()) {
                    listban = listban + ",";
                }
            }
            listban = listban + "]}]";
        } catch (Exception err) {
            debug("ERROR in listBans(): " + err.getMessage());
        }
        debug("Banlist - Sending JSON lenght: " + listban.length());
        print(listban, "application/json");
    }

    public static void copyFolder(File src, File dest)
            throws IOException {

        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }

            if (!src.exists()) {
                MilkAdminLog.info("Directory does not exist.");
                return;
            }
            String files[] = src.list();

            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                copyFolder(srcFile, destFile);
            }
        } else {

            if (!dest.exists()) {
                dest.createNewFile();
            }

            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(src).getChannel();
                destination = new FileOutputStream(dest).getChannel();
                destination.transferFrom(source, 0, source.size());
            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }
        }
    }

    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public String sha512me(String message) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(message.getBytes());
            byte[] mb = md.digest();
            String out = "";
            for (int i = 0; i < mb.length; i++) {
                byte temp = mb[i];
                String s = Integer.toHexString(new Byte(temp));
                while (s.length() < 2) {
                    s = "0" + s;
                }
                s = s.substring(s.length() - 2);
                out += s;
            }
            message = out;

        } catch (NoSuchAlgorithmException e) {
            debug("ERROR in sha512me(): " + e.getMessage());
        }
        return message;
    }

    public void print(String data, String MimeType) {
        try {
            DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
            out.writeBytes("HTTP/1.1 200 OK\r\n");
            out.writeBytes("Content-Type: " + MimeType + "; charset=utf-8\r\n");
            out.writeBytes("Cache-Control: no-cache, must-revalidate\r\n");
            out.writeBytes("Content-Length: " + data.length() + "\r\n");
            out.writeBytes("Server: milkAdmin Server\r\n");
            out.writeBytes("Connection: Close\r\n\r\n");
            out.writeBytes(data);
            out.flush();
            out.close();
        } catch (Exception e) {
            debug("ERROR in print(): " + e.getMessage());
        }
    }

    public void httperror(String error) {

        try {
            DataOutputStream out = new DataOutputStream(WebServerSocket.getOutputStream());
            out.writeBytes("HTTP/1.1 " + error + "\r\n");
            out.writeBytes("Server: milkAdmin Server\r\n");
            out.writeBytes("Connection: Close\r\n\r\n");
            out.flush();
            out.close();
        } catch (Exception e) {
            debug("ERROR in httperror(): " + e.getMessage());
        }
    }

    public void load_settings() throws IOException {
        Settings.load();
        //LoggedIn.load();
        loadSession();
        BackupPath = Settings.getString("Backup.Path", "Backups [milkAdmin]");
        BanListDir = Settings.getString("Settings.BanListDir", "plugins/milkAdmin");
        ExternalUrl = Settings.getString("Settings.ExternalUrl", "http://www.sharkale.com.ar/milkAdmin");
        Debug = Settings.getBoolean("Settings.Debug", false);
        String ipaux = Settings.getString("Settings.Ip", null);
        if (ipaux != null && !ipaux.equals("")) {
            try {
                Ip = InetAddress.getByName(ipaux);
            } catch (UnknownHostException e) {
                debug("ERROR UnknownHostException - Ip: " + ipaux + " - Message: " + e.getMessage());
            }
        }
        Port = Settings.getInt("Settings.Port", 64712);
        consoleLines = Settings.getInt("Settings.ConsoleLines", 13);
        BannedString = Settings.getString("Strings.Banned", "Banned from this server");
        KickedString = Settings.getString("Strings.Kicked", "Kicked!");
        NoSavePropertiesFile serverProperties = new NoSavePropertiesFile("server.properties");
        levelname = serverProperties.getString("level-name");
    }

    public synchronized void loadSession() {
        if (ThreadSafeSession.getSingleton() == null) {

            String SessionTime = GenerallyFileManager.FileReadLine("    SessionTimeBeforeLoggedOut", new File(PluginDir + "/settings.yml"), ": '").replaceAll("'", "");
            long sessionTime = 0;
            try {
                if (SessionTime.startsWith("-")) {
                    sessionTime = -1L;
                } else {
                    sessionTime = Long.parseLong(SessionTime);
                }
            } catch (Exception ex) {
                sessionTime = -1L;
            }
            ThreadSafeSession.createSingleton(new File(PluginDir + "milkadmin_login.session"), sessionTime);
            try {
                ThreadSafeSession.getSingleton().load();
            } catch (IOException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            initBannedUpdatedGetRequests();
            System.out.println("create new singleton!");
        }
    }

    public String getParam(String param, String URL) {
        Pattern regex = Pattern.compile("[\\?&]" + param + "=([^&#]*)");
        Matcher result = regex.matcher(URL);
        if (result.find()) {
            try {
                String resdec = URLDecoder.decode(result.group(1), "UTF-8");
                if (param != "password") {
                    debug(" - getParam: " + param + " - Value: " + resdec);
                }
                return resdec;
            } catch (UnsupportedEncodingException e) {
                debug("ERROR in getParam(): " + e.getMessage());
                return "";
            }
        } else {
            return "";
        }
    }

    public void run() {
        try {
            load_settings();
            if (WebServerMode == 0) {
                if (Ip == null) {
                    rootSocket = new ServerSocket(Port);
                    MilkAdminLog.info("WebServer listening on port " + Port);
                } else {
                    rootSocket = new ServerSocket(Port, 50, Ip);
                    MilkAdminLog.info("WebServer listening on " + Ip + ":" + Port);
                }
                while (!rootSocket.isClosed()) {
                    Socket requestSocket = rootSocket.accept();
                    new WebServer(milkAdminInstance, requestSocket);
                }
            } else {
                long timeDebug = System.currentTimeMillis();
                String urlDebug = "";
                BufferedReader in = new BufferedReader(new InputStreamReader(WebServerSocket.getInputStream()));
                try {
                    String l, g, url = "", param = "", json, htmlDir = "./plugins/milkAdmin/html";
                    boolean flag = true;
                    while ((l = in.readLine()) != null && flag) {
                        if (l.startsWith("GET")) {
                            flag = false;
                            g = (l.split(" "))[1];
                            Pattern regex = Pattern.compile("([^\\?]*)([^#]*)");
                            Matcher result = regex.matcher(g);
                            if (result.find()) {
                                url = result.group(1);
                                param = result.group(2);
                            }
                            String HostAddress = WebServerSocket.getInetAddress().getHostAddress();
                            debug(HostAddress + " - " + url);
                            urlDebug = url;
                            //System.out.println(url);
                            if (url.startsWith("/server") || url.startsWith("/")) //debug(" - ContainsKey: "+String.valueOf(LoggedIn.containsKey(HostAddress)) + " - keyExists: "+String.valueOf(LoggedIn.keyExists(HostAddress)));
                            {
                                updateSessionAndFiles(url, updateLoggedinFile, updateUserPasswordFile, updateUserTimestampFile);
                                if (url.contains("./")) {
                                    httperror("403 Access Denied");
                                } else if (url.startsWith("/ping")) {
                                    //if (!LoggedIn.containsKey(HostAddress)) {
                                    if (!ThreadSafeSession.getSingleton().hasSessionByIp(HostAddress)) {
                                        json = "login";
                                        //System.out.println("login");
                                    } else {
                                        json = "pong";
                                        //System.out.println("pong");
                                    }
                                    print(json, "text/plain");
                                } else if (url.startsWith("/server/login")) {
                                    String username = getParam("username", param);
                                    String password = getParam("password", param);
                                    if (username.length() > 0 && password.length() > 0) {
                                        if (adminList.containsKey(username)) {
                                            String login = adminList.getString(username, password);
                                            if (login.contentEquals(sha512me(password))) {
                                                debug(" - " + username + " logged in from " + HostAddress);
                                                //LoggedIn.setString(HostAddress, username);
                                                ThreadSafeSession.getSingleton().addUser(username, sha512me(password), HostAddress);
                                                ThreadSafeSession.getSingleton().save();
                                                ThreadSafeSession.getSingleton().writeSessionHostaddressUsernameToFile(loggedinFile, splitchar);
                                                json = "ok";
                                            } else {
                                                json = "error";
                                            }
                                        } else {
                                            json = "error";
                                        }
                                    } else {
                                        json = "error";
                                    }
                                    print(json, "text/plain");
                                    // } else if (!LoggedIn.containsKey(HostAddress)) {
                                } else if (!ThreadSafeSession.getSingleton().hasSessionByIp(HostAddress)) {
                                    debug(" - No logged.");
                                    if (url.equals("/") || url.equals("/login.html")) {
                                        readFileAsBinary(htmlDir + "/login.html", "text/html", true);
                                    } else if (url.startsWith("/js/lang/")) {
                                        readFileAsBinary(htmlDir + url, "text/javascript");
                                    } else if (url.startsWith("/js/")) {
                                        readFileAsBinary(htmlDir + url, "text/javascript", true);
                                    } else if (url.startsWith("/css/")) {
                                        readFileAsBinary(htmlDir + url, "text/css", true);
                                    } else if (url.startsWith("/images/")) {
                                        readFileAsBinary(htmlDir + url, null);
                                    } //OTHERWISE LOAD PAGES
                                    else {
                                        json = "<head><meta http-equiv=refresh content=0;URL=/></head>";
                                        print(json, "text/html");
                                        //httperror("403 Access Denied");
                                    }
                                } else {
                                    if (adminList.containsKey("admin")) {
                                        if (url.equals("/register.html")) {
                                            readFileAsBinary(htmlDir + "/register.html", "text/html", true);
                                        } else if (url.startsWith("/js/lang/")) {
                                            readFileAsBinary(htmlDir + url, "text/javascript");
                                        } else if (url.startsWith("/js/")) {
                                            readFileAsBinary(htmlDir + url, "text/javascript", true);
                                        } else if (url.startsWith("/css/")) {
                                            readFileAsBinary(htmlDir + url, "text/css", true);
                                        } else if (url.startsWith("/images/")) {
                                            readFileAsBinary(htmlDir + url, null);
                                        } else if (url.startsWith("/server/account_create")) {
                                            String username = getParam("username", param);
                                            String password = getParam("password", param);
                                            if (username.length() > 0 && password.length() > 0) {
                                                saveAdminList.setString(username, sha512me(password));
                                                saveAdminList.removeKey("admin");
                                                json = "ok:accountcreated";
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/html");
                                        } else {
                                            readFileAsBinary(htmlDir + "/register.html", "text/html", true);
                                        }
                                    } //FINISHED LOGIN
                                    //SERVER
                                    //AREA
                                    else if (url.startsWith("/server/account_create")) {
                                        String username = getParam("username", param);
                                        String password = getParam("password", param);
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            if (username.length() > 0 && password.length() > 0) {
                                                saveAdminList.setString(username, sha512me(password));
                                                json = "ok:accountcreated";
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }
                                    } else if (url.equals("/server/logout")) {
                                        //LoggedIn.removeKey(HostAddress);
                                        ThreadSafeSession.getSingleton().removeUserByIp(HostAddress);
                                        json = "ok";
                                        print(json, "text/plain");
                                        ThreadSafeSession.getSingleton().writeSessionHostaddressUsernameToFile(loggedinFile, splitchar);
                                    } else if (url.equals("/save")) {
                                        consoleCommand("save-all");
                                        json = "ok:worldsaved";
                                        print(json, "text/plain");

                                    } else if (url.startsWith("/server/say")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String text = getParam("message", param);
                                            if (text.length() > 0) {
                                                if (text.startsWith("/")) {
                                                    String command = text.replace("/", "");
                                                    consoleCommand(command);
                                                } else {
                                                    consoleCommand("say " + text);
                                                }
                                                json = "ok:messagesent";
                                            } else {
                                                json = "error:messageempty";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.startsWith("/server/broadcast_message")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String text = getParam("message", param);
                                            if (text.length() > 0) {
                                                milkAdminInstance.getServer().broadcastMessage(text);
                                                json = "ok:broadcastedmessage";
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.equals("/stop")) {
                                        json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
                                        json += "<head><script type=\"text/javascript\">tourl = './';</script>" + readFileAsString(htmlDir + "/wait.html");
                                        print(json, "text/html");

                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            debug("ERROR in Stop: " + e.getMessage());
                                        }
                                        milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.HOLD_SERVER, null);

                                    } else if (url.equals("/reload_server")) {
                                        milkAdminInstance.getServer().reload();
                                        json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
                                        json += "<head><script type=\"text/javascript\">tourl = './';</script>" + readFileAsString(htmlDir + "/wait.html");
                                        print(json, "text/html");

                                    } else if (url.equals("/restart_server")) {
                                        try {
                                            milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.RESTART, null);
                                        } catch (IOException e) {
                                            debug("ERROR in restart_server: " + e.getMessage());
                                        }
                                        json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
                                        json += "<head><script type=\"text/javascript\">tourl = './';</script>" + readFileAsString(htmlDir + "/wait.html");
                                        print(json, "text/html");

                                    } else if (url.equals("/force_stop")) {
                                        json = "ok:forcestop";
                                        print(json, "text/plain");
                                        System.exit(1);

                                    } else if (url.equals("/server/get_plugins.json")) {
                                        Plugin[] p = milkAdminInstance.getServer().getPluginManager().getPlugins();
                                        json = "[";
                                        int cant = p.length;
                                        for (int i = 0; i < cant; i++) {
                                            PluginDescriptionFile pdf = p[i].getDescription();
                                            json = json + "{\"name\":\"" + pdf.getName() + "\", \"version\":\"" + pdf.getVersion() + "\",\"enabled\":" + p[i].isEnabled() + "}";
                                            if (i < (cant) - 1) {
                                                json = json + ",";
                                            }
                                        }
                                        json = json + "]";
                                        print(json, "application/json");

                                    } else if (url.startsWith("/server/plugin_latest.json")) {
                                        String plugin = getParam("plugin", param);
                                        if (plugin.length() > 0) {
                                            json = milkAdminInstance.PU.getLatest(plugin);
                                        } else {
                                            json = "[{\"error\":\"badparameters\"}]";
                                        }
                                        // TODO: Change to "application/json"
                                        print(json, "text/plain");

                                    } else if (url.startsWith("/server/disable_plugin")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String plugin = getParam("plugin", param);
                                            if (plugin.length() > 0) {
                                                if (milkAdminInstance.getServer().getPluginManager().isPluginEnabled(plugin)) {
                                                    milkAdminInstance.getServer().getPluginManager().disablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
                                                    json = "ok:plugindisabled:_NAME_," + plugin;
                                                } else {
                                                    json = "ok:pluginnotenabled";
                                                }
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.startsWith("/server/enable_plugin")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String plugin = getParam("plugin", param);
                                            if (plugin.length() > 0) {
                                                milkAdminInstance.getServer().getPluginManager().enablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
                                                json = "ok:pluginenabled:_NAME_," + plugin;
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.startsWith("/server/reload_plugin")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String plugin = getParam("plugin", param);
                                            if (plugin.length() > 0) {
                                                if (milkAdminInstance.getServer().getPluginManager().isPluginEnabled(plugin)) {
                                                    milkAdminInstance.getServer().getPluginManager().disablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
                                                    milkAdminInstance.getServer().getPluginManager().enablePlugin(milkAdminInstance.getServer().getPluginManager().getPlugin(plugin));
                                                    json = "ok:pluginreloaded:_NAME_," + plugin;
                                                } else {
                                                    json = "ok:pluginnotenabled";
                                                }
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.startsWith("/server/load_plugin")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String plugin = getParam("plugin", param);
                                            if (plugin.length() > 0) {
                                                File pluginFile = new File(new File("plugins"), plugin + ".jar");
                                                if (pluginFile.isFile()) {
                                                    try {
                                                        Plugin newPlugin = milkAdminInstance.getServer().getPluginManager().loadPlugin(pluginFile);
                                                        if (newPlugin != null) {
                                                            String pluginName = newPlugin.getDescription().getName();
                                                            milkAdminInstance.getServer().getPluginManager().enablePlugin(newPlugin);
                                                            if (newPlugin.isEnabled()) {
                                                                MilkAdminLog.info("Plugin loaded and enabled [" + pluginName + "]");
                                                                json = "ok:pluginloaded:_NAME_," + pluginName;
                                                            } else {
                                                                json = "error:pluginloadfailed";
                                                            }
                                                        } else {
                                                            json = "error:pluginloadfailed";
                                                        }
                                                    } catch (UnknownDependencyException ex) {
                                                        json = "error:pluginnotplugin";
                                                    } catch (InvalidPluginException ex) {
                                                        json = "error:pluginnotplugin";
                                                    } catch (InvalidDescriptionException ex) {
                                                        json = "error:plugininvalid";
                                                    }
                                                } else {
                                                    json = "error:pluginnotexist";
                                                }
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.equals("/server/console")) {
                                        print(readConsole(), "text/plain");

                                    } else if (url.startsWith("/server/properties_edit")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String property = getParam("property", param);
                                            String value = getParam("value", param);
                                            if (property.length() > 0 && value.length() > 0) {
                                                if (property.equalsIgnoreCase("white-list")) {
                                                    if (value.equalsIgnoreCase("true")) {
                                                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "whitelist on");
                                                        if (milkAdminInstance != null) {
                                                            if (useCustomWhitelist) {
                                                                milkAdminInstance.WLCustom = true;
                                                            }
                                                            milkAdminInstance.MCWhitelist = true;
                                                        }
                                                    } else if (value.equalsIgnoreCase("false")) {
                                                        if (milkAdminInstance != null) {
                                                            if (useCustomWhitelist) {
                                                                milkAdminInstance.WLCustom = false;
                                                            }
                                                            milkAdminInstance.MCWhitelist = false;
                                                        }
                                                        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "whitelist off");
                                                    }
                                                } else {
                                                    BukkitProperties.setString(property, value);
                                                }
                                                json = "ok:editedproperty";
                                            } else {
                                                json = "error:badparameters";
                                            }

                                            print(json, "text/plain");
                                        }

                                    } else if (url.startsWith("/page/change_lang")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String lang = getParam("lang", param);
                                            if (lang.length() > 0) {
                                                if (new File(htmlDir + "/js/lang/" + lang, "default.js").exists()) {
                                                    File src = new File(htmlDir + "/js/lang/" + lang, "default.js");
                                                    File dest = new File(htmlDir + "/js/lang", "default.js");
                                                    copyFolder(src, dest);
                                                    json = "ok:langchanged";
                                                } else {
                                                    json = "error:langnotfound";
                                                }
                                            } else {
                                                json = "error:badparameters";
                                            }

                                            print(json, "text/plain");
                                        }

                                    } else if (url.equals("/backup")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            Worlds.load();
                                            //Worlds.setHeader("# milkAdmin - INTERNAL USE DO NOT MODIFY");
                                            List<World> worlds = milkAdminInstance.getServer().getWorlds();
                                            List<String> wstr = new ArrayList<String>();
                                            if (worlds.size() > 0) {
                                                for (World world : worlds) {
                                                    wstr.add(world.getName());
                                                }
                                            }
                                            Worlds.setProperty("Worlds", wstr);
                                            Worlds.save();
                                            json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
                                            json += "<head><script type=\"text/javascript\">tourl = '/backup';</script>" + readFileAsString(htmlDir + "/wait.html");
                                            print(json, "text/html");


                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                debug("ERROR in backup: " + e.getMessage());
                                            }
                                            milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.HOLD_SERVER, null);
                                        }
                                    } else if (url.startsWith("/restore")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String id = getParam("id", param);
                                            String clear = getParam("clear", param);
                                            if (id.length() > 0) {
                                                json = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">";
                                                json += "<head><script type=\"text/javascript\">tourl = '/restore?id=" + id + "&clear=" + clear + "';</script>" + readFileAsString(htmlDir + "/wait.html");
                                                print(json, "text/html");

                                            } else {
                                                readFileAsBinary(htmlDir + "/index.html", "text/html");

                                            }
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                debug("ERROR in backup: " + e.getMessage());
                                            }
                                            milkAdminInstance.RTKapi.executeCommand(RTKInterface.CommandType.HOLD_SERVER, null);
                                        }
                                    } else if (url.startsWith("/delete")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String id = getParam("id", param);
                                            if (id.length() > 0) {
                                                deleteDirectory(new File(BackupPath + "/" + id));
                                                json = "ok:deletebackup";
                                            } else {
                                                json = "error:badparameters";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.equals("/info/list_backups.json")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            File dir = new File(BackupPath);
                                            String[] children = dir.list();
                                            String listbu = "[";
                                            if (children == null) {
                                                listbu = "[]";
                                            } else {
                                                listbu = "[";
                                                int i = 0;
                                                while (i < (children.length)) {
                                                    String filename = children[i];
                                                    String filenamed = filename;
                                                    String filenamechanged = filenamed.replace(".", "/").replace("_", " ").replace("-", ":");
                                                    listbu = listbu + ("{\"optionValue\":\"" + filename + "\", \"optionDisplay\":\"" + filenamechanged + "\"}");
                                                    if (i < (children.length) - 1) {
                                                        listbu = listbu + (",");
                                                    }
                                                    i++;
                                                }
                                                listbu = listbu + ("]");
                                            }
                                            print(listbu, "application/json");
                                        }
                                    } /////////////
                                    //INFO AREA
                                    /////////////
                                    else if (url.equals("/info/data.json")) {
                                        print(infoData(), "application/json");

                                    } /////////////////////////
                                    //CUSTOM WHITELIST AREA
                                    /////////////////////////
                                    /*else if ( url.equals("/customwl/get.json") ){
                                    List<String> players = loadWhitelist();
                                    String wl = "[";
                                    for(String p: players){
                                    if(wl.length() > 1)
                                    wl+= ",";
                                    wl+= "\""+p+"\"";
                                    }
                                    wl+= "]";
                                    print(wl, "application/json");
                                    }*/ else if (url.equals("/customwl/add.php")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String user = getParam("user", param);
                                            if (user.length() > 0) {
                                                json = milkAdminInstance.WL.myAddDefaultPlayer(user);
                                            } else {
                                                json = "Invalid User";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.equals("/customwl/remove.php")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String user = getParam("user", param);
                                            if (user.length() > 0) {
                                                json = milkAdminInstance.WL.myRemovePlayer(user);
                                            } else {
                                                json = "Invalid User";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } //////////////////
                                    //WHITELIST AREA
                                    //////////////////
                                    else if (url.equals("/whitelist/get.json")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            List<String> players = loadWhitelist();
                                            String wl = "[";
                                            for (String p : players) {
                                                if (wl.length() > 1) {
                                                    wl += ",";
                                                }
                                                wl += "\"" + p + "\"";
                                            }
                                            wl += "]";
                                            print(wl, "application/json");
                                        }
                                    } else if (url.equals("/whitelist/add")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String user = getParam("myuser", param);

                                            if (user.length() > 0) {
                                                addToWhitelist(user);
                                                json = "ok";
                                            } else {
                                                json = "error";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } else if (url.equals("/whitelist/save")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            String usersP = getParam("users", param);
                                            List<String> usersL = new ArrayList<String>();
                                            if (usersP.length() > 0) {
                                                String[] users = usersP.split(",");
                                                for (String user : users) {
                                                    usersL.add(user);
                                                }
                                            }
                                            if (saveWhitelist(usersL)) {
                                                json = "ok";
                                            } else {
                                                json = "error";
                                            }
                                            print(json, "text/plain");
                                        }

                                    } ////////////////
                                    //PLAYER AREA
                                    ////////////////
                                    else if (url.startsWith("/player/kick")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String cause = getParam("cause", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.kickPlayer(webServer, user, cause, KickedString);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/give_item")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String item = getParam("item", param);
                                            final String amount = getParam("amount", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.givePlayerItem(webServer, user, amount, item);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/remove_item")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String item = getParam("item", param);
                                            final String amount = getParam("amount", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.removePlayerItem(webServer, user, amount, item);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/get_health")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.getPlayerHealth(webServer, user);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/set_health")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String amount = getParam("amount", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.setPlayerHealth(webServer, user, amount);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/ban_player")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String cause = getParam("cause", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.banPlayer(webServer, myMilkAdminInstance, user, cause, BannedString);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/ban_uuid")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String uuid = getParam("uuid", param);
                                            final String cause = getParam("cause", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.banPlayerUUID(webServer, myMilkAdminInstance, uuid, cause, BannedString);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/ban_ip")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String ip = getParam("ip", param);
                                            final String cause = getParam("cause", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.banPlayerIp(webServer, myMilkAdminInstance, ip, cause, BannedString);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/unban_player")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.unbanPlayer(webServer, myMilkAdminInstance, user);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/unban_uuid")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String uuid = getParam("uuid", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.unbanPlayerUUID(webServer, myMilkAdminInstance, uuid);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/unban_ip")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String ip = getParam("user", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.unbanIp(webServer, myMilkAdminInstance, ip);
                                                }
                                            }).start();
                                        }
                                    } else if (url.equals("/player/banlist.json")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.listBans(webServer);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/shoot_arrow")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final int amount = Integer.parseInt(getParam("amount", param));
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.shootArrow(webServer, user, amount);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/shoot_fireball")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final int amount = Integer.parseInt(getParam("amount", param));
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.shootFireball(webServer, user, amount);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/throw_snowball")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final int amount = Integer.parseInt(getParam("amount", param));
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.throwSnowball(webServer, user, amount);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/throw_egg")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final int amount = Integer.parseInt(getParam("amount", param));
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.throwEgg(webServer, user, amount);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/throw_bomb")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final int amount = Integer.parseInt(getParam("amount", param));
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.throwBomb(webServer, user, amount);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/change_display_name")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String name = getParam("name", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.changeDisplayName(webServer, user, name);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/teleport_to_player")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String touser = getParam("to_user", param);
                                            final WebServer webServer = this;
                                            final MilkAdmin myMilkAdminInstance = milkAdminInstance;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.teleportToPlayer(webServer, myMilkAdminInstance, user, touser);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/teleport_to_location")) {
                                        String my = ThreadSafeSession.getSingleton().getUsernameByIp(HostAddress);
                                        if (!my.endsWith("NL")) {
                                            final String user = getParam("user", param);
                                            final String x = getParam("x", param);
                                            final String y = getParam("y", param);
                                            final String z = getParam("z", param);
                                            final WebServer webServer = this;
                                            new MilkAdminThread(new Runnable() {

                                                @Override
                                                public void run() {
                                                    MilkAdminWebServer.teleportToLocation(webServer, user, x, y, z);
                                                }
                                            }).start();
                                        }
                                    } else if (url.startsWith("/player/is_online")) {
                                        final String user = getParam("user", param);
                                        final WebServer webServer = this;
                                        new MilkAdminThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                MilkAdminWebServer.playerIsOnline(webServer, user);
                                            }
                                        }).start();
                                    } else if (url.startsWith("/player/get_ip_port.json")) {
                                        final String user = getParam("user", param);
                                        final WebServer webServer = this;
                                        new MilkAdminThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                MilkAdminWebServer.getIpPort(webServer, user);
                                            }
                                        }).start();
                                    } else if (url.equals("/") || url.equals("/index.html")) {
                                        readFileAsBinary(htmlDir + "/index.html", "text/html", true);

                                    } else if (url.startsWith("/js/lang/")) {
                                        readFileAsBinary(htmlDir + url, "text/javascript");

                                    } else if (url.startsWith("/js/")) {
                                        readFileAsBinary(htmlDir + url, "text/javascript", true);

                                    } else if (url.startsWith("/css/")) {
                                        readFileAsBinary(htmlDir + url, "text/css", true);
                                    } else if (url.startsWith("/images/")) {
                                        readFileAsBinary(htmlDir + url, null);
                                    } else {
                                        readFileAsBinary(htmlDir + "/index.html", "text/html", true);

                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                } catch (Exception e) {
                    debug("ERROR in ServerParser: " + e);
                }
                timeDebug = System.currentTimeMillis() - timeDebug;
                debug(" - Took " + timeDebug + "ms to process: " + urlDebug);
            }
        } catch (IOException e) {
            debug("ERROR in ServerInitialize: " + e.getMessage());
        }
    }

    public static void initBannedUpdatedGetRequests() {
        if (!bannedUpdatedRequests.isEmpty()) {
            return;
        }

        //must filter to use the session!
        bannedUpdatedRequests.add("/ping");
        bannedUpdatedRequests.add("/info/data.json");
        bannedUpdatedRequests.add("/server/console");
        //must not filter to use the session!
        //filter to use faster the session!
        bannedUpdatedRequests.add("/index.html");
        bannedUpdatedRequests.add("/js");
        bannedUpdatedRequests.add("/css");
        bannedUpdatedRequests.add("/images");
        bannedUpdatedRequests.add("/server/login");
        bannedUpdatedRequests.add("/login.html");
        bannedUpdatedRequests.add("/server/logout");
        bannedUpdatedRequests.add("/register.html");
        bannedUpdatedRequests.add("/server/account_create");

    }

    public void updateSessionAndFiles(String url, boolean set) {
        updateSessionAndFiles(url, set, set, set);
    }

    public void updateSessionAndFiles(String url, boolean updateLoggedinFile, boolean updateUserPasswordFile, boolean updateUserTimestampFile) {
        if (url.equals("/")) {
            return;
        }
        boolean canUpdate = true;
        for (String getRequest : bannedUpdatedRequests) {
            if (url.startsWith(getRequest)) {
                canUpdate = false;
                break;
            }
        }
        if (!canUpdate) {
            return;
        }
        if (updateLoggedinFile && updateUserPasswordFile && updateUserTimestampFile) {
            ThreadSafeSession.getSingleton().update(WebServerSocket, loggedinFile, loggedinUserPasswordFile, loggedinUserTimestampFile, splitchar);
        } else if (ThreadSafeSession.getSingleton().update(WebServerSocket)) {
            if (updateLoggedinFile) {
                ThreadSafeSession.getSingleton().writeSessionHostaddressUsernameToFile(loggedinFile, splitchar);
            }
            if (updateUserPasswordFile) {
                ThreadSafeSession.getSingleton().writeSessionUsernamePasswordToFile(loggedinUserPasswordFile, splitchar);
            }
            if (updateUserTimestampFile) {
                ThreadSafeSession.getSingleton().writeSessionUsernameTimestampToFile(loggedinUserTimestampFile, splitchar);
            }
        }
        try {
            ThreadSafeSession.getSingleton().save();
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stopServer() throws IOException {
        ThreadSafeSession.getSingleton().save();
        if (rootSocket != null) {
            rootSocket.close();
        }
    }
}
