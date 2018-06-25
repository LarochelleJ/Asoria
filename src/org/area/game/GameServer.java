package org.area.game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.area.check.IpCheck;
import org.area.client.Player;
import org.area.common.*;
import org.area.fight.object.Monster;
import org.area.kernel.Config;
import org.area.kernel.Logs;
import org.area.kernel.Main;
import org.area.kernel.Reboot;

import com.mysql.jdbc.PreparedStatement;
import org.area.object.Maps;
import org.joda.time.DateTime;

public class GameServer implements Runnable {

    /**
     * SheduledExecutor
     **/
    /*public static ScheduledExecutorService executorTimer = Executors.newSingleThreadScheduledExecutor(),
            fightExecutor = Executors.newSingleThreadScheduledExecutor();*/
    private final static int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public static ScheduledExecutorService executorTimer = Executors.newSingleThreadScheduledExecutor(),
            fightExecutor = Executors.newScheduledThreadPool(NUM_THREADS);
    /**
     * Socket and Thread
     **/
    private ServerSocket serverSocket;
    private Thread thread;
    public static String ip;
    public static int lang;
    /**
     * Exchange
     **/
    public static int id, state = 0;
    public static String key;
    public static String encryptPacketKey = "0";
    /**
     * Waiting accounts & players in the game
     **/
    private ArrayList<GameThread> clients = new ArrayList<GameThread>();
    private int maxClients;
    static long startTime;
    private static long timeShutdown;
    private int rebootTimeEllap = 0;
    private int rebootStep = 0;
    private int hoursForReboot = 48; // 2 jour
    private boolean hourL = false;

    public GameServer() {
        try {

            /** @Automatic reboot **/
            int n = 60; // 1 heure
            Calendar c = Calendar.getInstance();
            int heure = c.get(Calendar.HOUR_OF_DAY);
            if (heure > 2) { // La journée est déjà commencée
                hoursForReboot -= heure; // on préfère reboot plus tôt que trop tard
            } else {
                n = 2 - heure;
            }
            rebootStep = 0;
            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    if (++rebootTimeEllap >= hoursForReboot - 1 && !hourL) { // Plus qu'une heure
                        hourL = true;
                        SocketManager.GAME_SEND_MESSAGE_TO_ALL("Plus qu'une heure avant le redémarage du serveur !", Config.CONFIG_MOTD_COLOR);
                        executorTimer.scheduleWithFixedDelay(new Runnable() {
                            public void run() {
                                rebootStep++;
                                if (rebootStep == 1) { // 30
                                    SocketManager.GAME_SEND_MESSAGE_TO_ALL("Plus que 30 minutes avant le redémarage du serveur !", Config.CONFIG_MOTD_COLOR);
                                } else if (rebootStep == 3) { // 40
                                    SocketManager.GAME_SEND_MESSAGE_TO_ALL("Plus que 20 minutes avant le redémarage du serveur ! Dans 5 minutes les combats seront bloqués !", Config.CONFIG_MOTD_COLOR);
                                } else if (rebootStep == 4) { // 45, on leur laisse 15 minutes de jeu avant de reboot
                                    Constant.COMBAT_BLOQUE = true;
                                } else if (rebootStep == 5) { // 50
                                    SocketManager.GAME_SEND_MESSAGE_TO_ALL("Redémarage du serveur dans 10 minutes !", Config.CONFIG_MOTD_COLOR);
                                } else if (rebootStep == 7) { // 60
                                    SocketManager.GAME_SEND_MESSAGE_TO_ALL("Redémarage du serveur !", Config.CONFIG_MOTD_COLOR);
                                    Reboot.reboot();
                                }
                            }
                        }, 30, 5, TimeUnit.MINUTES);
                    } else {
                        SocketManager.GAME_SEND_MESSAGE_TO_ALL((hoursForReboot - rebootTimeEllap) + " heures avant le redémarage du serveur !", Config.CONFIG_MOTD_COLOR);
                    }
                }
            }, n, 60, TimeUnit.MINUTES);

            /** @Automatic save **/
            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1164");
                    World.saveAll(null);
                    SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1165");
                }
            }, 30, 30, TimeUnit.MINUTES);

            /** @Automatic MobGroup spawn **/
            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    for (Monster.MobGroup group : World.variableMobGroup) {
                        Maps m = group.getMap();
                        boolean couldSpawn = (group.child == null || !m.getMobGroups().containsValue(group.child));
                        if (couldSpawn) { // on incremente seulement si le groupe n'existe pas
                            group.ellaps++;
                        }
                        if (group.ellaps >= group.spawnTimeFix && couldSpawn) { // va spawn et n'est pas déjà spawn
                            group.ellaps = 0;
                            if (group.haveVariableSpawnTime()) {
                                group.spawnTimeFix = group.generateRandomSpawnTime();
                            }
                            group.child = m.spawnGroupOnCommand(group.getCellID(), group.groupD);
                        }
                        SQLManager.SAVE_FIXGROUP(m.get_id(), group.getCellID(), group.groupD, group.ellaps);
                    }
                }
            }, 1, 1, TimeUnit.MINUTES);

            /** @Automatic pub **/
            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    int rand = Formulas.getRandomValue(1, 5);
                    switch (rand) {
                        case 1:
                            SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB1, Config.CONFIG_MOTD_COLOR);
                            break;
                        case 2:
                            SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB2, Config.CONFIG_MOTD_COLOR);
                            break;
                        case 3:
                            SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB3, Config.CONFIG_MOTD_COLOR);
                            break;
                        case 4:
                            SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB4, Config.CONFIG_MOTD_COLOR);
                            break;
                        case 5:
                            SocketManager.GAME_SEND_MESSAGE_TO_ALL(Config.PUB5, Config.CONFIG_MOTD_COLOR);
                            break;
                    }
                }
            }, 10, 10, TimeUnit.MINUTES);

            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    World.MoveMobsOnMaps();
                }
            }, 1, 1, TimeUnit.MINUTES);

            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    World.PnjParoleOnMaps();
                }
            }, 5, 5, TimeUnit.MINUTES);

            /** @Refresh world mobs System **/
            /*executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    World.RefreshAllMob();
                    GameServer.addToLog(">La recharge des mobs est finie\n");
                }
            }, 5, 5, TimeUnit.HOURS); */

            /** Clear Garbage collector @Flow
             * L'utilisation du System.gc n'est pas conseill? ! **/
			/*
			executorTimer.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					try
					{
					Runtime r = Runtime.getRuntime();
					try
					{
						r.runFinalization();
						r.gc();
						System.gc();
					}
					catch(Exception e){}
					}catch(Exception e){}
					GameServer.addToLog(">La recharge des mobs est finie\n");
				}
			}, 3, 3, TimeUnit.HOURS);*/

            /** @Uptime infos **/
            executorTimer.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    String baseQuery = "UPDATE `servers` SET `uptime` = ?, `logged` = ? WHERE `id` = ?;";

                    try {
                        PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection(true));
                        p.setString(1, GameServer.uptimeSQL());
                        p.setInt(2, clients.size());
                        p.setInt(3, GameServer.id);
                        p.execute();
                        SQLManager.closePreparedStatement(p);
                    } catch (SQLException e) {
                    }
                }
            }, 30, 30, TimeUnit.SECONDS);

            /**
             * @Start the GameServer
             **/
            try {
                Thread.sleep(30000); // 1/2 minute
            } catch (Exception e) {}
            serverSocket = new ServerSocket(Config.CONFIG_GAME_PORT);
            if (Config.CONFIG_USE_IP)
                Config.GAMESERVER_IP = CryptManager.CryptIP(ip) + CryptManager.CryptPort(Config.CONFIG_GAME_PORT);
            startTime = System.currentTimeMillis();
            thread = new Thread(this, "GameServer");
            thread.start();
        } catch (IOException e) {
            addToLog("IOException: " + e.getMessage());
            Logs.addToDebug("IOException Game-Manager: " + e.getMessage());
            Reboot.reboot();
        }
    }

    public void run() {
        while (Main.isRunning) {
            try {
                Socket socket = serverSocket.accept();
                if (!IpCheck.canGameConnect(socket.getInetAddress().getHostAddress()))
                    socket.close();
                else {
                    //socket.setSoTimeout(1200000); // 1200000 = 20 minutes *** Inactivité
                    GameThread gamethread = new GameThread(socket);

                    getClients().add(gamethread);
                    if (getClients().size() > maxClients)
                        maxClients = getClients().size();
                }
            } catch (IOException e) {
                Logs.addToDebug(e.getMessage());
                addToLog(e.getMessage());
                if (serverSocket.isClosed())
                    Reboot.start();
            }
        }
    }

    public void kickAll() throws Exception {
        try {
            serverSocket.close();
        } catch (IOException e) {
        }

        ArrayList<GameThread> c = new ArrayList<GameThread>();
        c.addAll(clients);
        for (GameThread GT : c) {
            try {
                GT.closeSocket();
            } catch (Exception e) {
            }
            ;
        }
    }

    public void delClient(GameThread gameThread) {
        clients.remove(gameThread);
    }

    public static String getServerTime() {
        Date actDate = new Date();
        return "BT" + (actDate.getTime() + 3600000);
    }

    public void restartGameServer() {
        if (!thread.isAlive()) {
            Logs.addToDebug("GameServer plant?, tentative de le red?marer.");
            thread.start();
        }
    }

    public static class SaveThread implements Runnable {
        public void run() {
            if (!Config.IS_SAVING) {
                World.saveAll(null);
            }
        }
    }

    public synchronized static void addToLog(String str) {
        if (Config.LOGS) {
            try {
                String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
                Config.Log_Game.write(date + ": " + str);
                Config.Log_Game.newLine();
                Config.Log_Game.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void addToHdvLog(String str) {
        if (Config.LOGS) {
            try {
                String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
                Config.Log_Hdv.write(date + ": " + str);
                Config.Log_Hdv.newLine();
                Config.Log_Hdv.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void addToSockLog(String str) {
        if (Config.DEBUG) System.out.println(str);
        if (Config.LOGS) {
            try {
                String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
                Config.Log_GameSock.write(date + ": " + str);
                Config.Log_GameSock.newLine();
                Config.Log_GameSock.flush();
            } catch (IOException e) {
            }
        }
    }

    public static String getServerDate() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd");

        String day = Integer.parseInt(dateFormat.format(date)) + "";

        while (day.length() < 2) day = "0" + day;

        dateFormat = new SimpleDateFormat("MM");
        String mounth = (Integer.parseInt(dateFormat.format(date)) - 1) + "";

        while (mounth.length() < 2) mounth = "0" + mounth;

        dateFormat = new SimpleDateFormat("yyyy");
        String year = (Integer.parseInt(dateFormat.format(date)) - 1370) + "";
        return "BD" + year + "|" + mounth + "|" + day;
    }

    public static String uptime() {
        long uptime = System.currentTimeMillis() - Main.gameServer.getStartTime();
        int jour = (int) (uptime / (1000 * 3600 * 24));
        uptime %= (1000 * 3600 * 24);
        int hour = (int) (uptime / (1000 * 3600));
        uptime %= (1000 * 3600);
        int min = (int) (uptime / (1000 * 60));
        uptime %= (1000 * 60);
        int sec = (int) (uptime / (1000));
        return "Uptime : <b>" + jour + "j " + hour + "h " + min + "m " + sec + "s</b>\n";
    }

    public static String uptimeSQL() {
        long uptime = System.currentTimeMillis() - Main.gameServer.getStartTime();
        int jour = (int) (uptime / (1000 * 3600 * 24));
        uptime %= (1000 * 3600 * 24);
        int hour = (int) (uptime / (1000 * 3600));
        uptime %= (1000 * 3600);
        int min = (int) (uptime / (1000 * 60));
        return jour + "j " + hour + "h " + min + "m";
    }

    public synchronized void stop() {
        this.stop(); // Causer un stackOverFlow pour arrêter le serveur, mouais...
    }

    public static void addShutDown(int time) {
        /** @Automatic reboot **/
        timeShutdown = time;
        executorTimer.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                boolean box = false;
                if (timeShutdown > 0) {
                    box = true;
                    timeShutdown = (System.currentTimeMillis() + 60000) * -1;
                }
                long time = (timeShutdown * -1 - System.currentTimeMillis()) / 1000;
                for (Player player : World.getOnlinePlayers()) {
                    player.sendText("<b>Red?marrage automatique:</b> " + time + " secondes restantes");
                    if (box)
                        player.sendBox("Serveur " + GameServer.key, "Reboot Automatique\nRed?marrage du serveur dans 1 minute");
                }
                if (time <= 0)
                    Reboot.reboot();
            }
        }, timeShutdown, 10, TimeUnit.SECONDS);
    }

    public ArrayList<GameThread> getClients() {
        return clients;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getMaxPlayer() {
        return maxClients;
    }

    public int getPlayerNumber() {
        return clients.size();
    }

    public Thread getThread() {
        return thread;
    }

    public static String getIp() {
        return ip;
    }

    public static void setIp(String ip) {
        GameServer.ip = ip;
    }

    public static void addToSockLog(String str, String name) { // Pour les packets re?us @Flow
        if (Config.DEBUG) System.out.println(str);
        if (Config.LOGS) {
            try {
                String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
                Config.Log_GameSock.write(date + ": (" + name + ") " + str);
                Config.Log_GameSock.newLine();
                Config.Log_GameSock.flush();
            } catch (IOException e) {
            }
        }
    }
}