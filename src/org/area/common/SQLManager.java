package org.area.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang.ArrayUtils;
import org.area.client.Account;
import org.area.client.Player;
import org.area.client.tools.RapidStuff;
import org.area.command.CommandManager;
import org.area.command.GmCommand;
import org.area.command.player.Tickets;
import org.area.common.World.Area;
import org.area.common.World.Couple;
import org.area.common.World.Drop;
import org.area.common.World.IOTemplate;
import org.area.common.World.ItemSet;
import org.area.common.World.SubArea;
import org.area.event.Event;
import org.area.event.type.EventQuizz;
import org.area.fight.object.Collector;
import org.area.fight.object.Monster;
import org.area.fight.object.Prism;
import org.area.game.GameServer;
import org.area.game.tools.ParseTool;
import org.area.game.tools.Util;
import org.area.kernel.Config;
import org.area.kernel.Console;
import org.area.kernel.Console.Color;
import org.area.kernel.Logs;
import org.area.kernel.Reboot;
import org.area.object.*;
import org.area.object.AuctionHouse.HdvEntry;
import org.area.object.Guild.GuildMember;
import org.area.object.Item.ObjTemplate;
import org.area.object.Maps.MountPark;
import org.area.object.NpcTemplate.NPC_Exchange;
import org.area.object.NpcTemplate.NPC_question;
import org.area.object.NpcTemplate.NPC_reponse;
import org.area.object.job.Job;
import org.area.quests.Quest;
import org.area.quests.QuestPlayer;
import org.area.quests.Quest_Objective;
import org.area.quests.Quest_Step;
import org.area.spell.Spell;
import org.area.spell.Spell.SortStats;

import com.mysql.jdbc.PreparedStatement;

public class SQLManager {

    private static Connection gameConnection, realmConnection;
    private static Lock myConnectionLocker = new ReentrantLock();
    private static Timer timerCommit;
    private static boolean needCommit;


    public static void closeResultSet(ResultSet RS) {
        try {
            RS.getStatement().close();
            RS.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closePreparedStatement(PreparedStatement p) {
        try {
            p.clearParameters();
            p.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static final boolean setUpConnexion(boolean realm) {
        try {
            myConnectionLocker.lock();
            gameConnection = DriverManager.getConnection("jdbc:mysql://"
                            + Config.DB_HOST + "/" + Config.DB_NAME, Config.DB_USER,
                    Config.DB_PASS);
            gameConnection.setAutoCommit(false);

            realmConnection = DriverManager.getConnection("jdbc:mysql://"
                            + Config.RDB_HOST + "/" + Config.RDB_NAME, Config.RDB_USER,
                    Config.RDB_PASS);
            realmConnection.setAutoCommit(false);


            if (!gameConnection.isValid(1000) || !realmConnection.isValid(1000)) {
                Console.println("conection to the database invalid", Color.RED);
                return false;
            }

            needCommit = false;
            TIMER(true);

            return true;
        } catch (SQLException e) {
            String error = e.getMessage();

            if (error.contains("Communications link failure"))
                Console.println("connection to the database error : communications link failure", Color.RED);
            else Console.println("connection to the database error : " + error, Color.RED);
            e.printStackTrace();
        } finally {
            myConnectionLocker.unlock();
        }
        return false;
    }

    public static Connection Connection(boolean realm) {
        try {
            myConnectionLocker.lock();
            boolean valid = true;
            try {
                if (realmConnection == null || gameConnection == null) {
                    valid = false;
                } else {
                    valid = realm ? !realmConnection.isClosed() : !gameConnection.isClosed();
                }
            } catch (Exception e) {
                valid = false;
            }
            if (!realm && !valid) { // On doit cérer une connexion
                closeCons(realm);
                setUpConnexion(realm);
            } else if (realm && !valid) {
                closeCons(realm);
                setUpConnexion(realm);
            }

            return realm ? realmConnection : gameConnection;
        } finally {
            myConnectionLocker.unlock();
        }
    }


    public static ResultSet executeQuery(String query,
                                         boolean realm) throws SQLException {
        Connection DB = Connection(realm);
        java.sql.PreparedStatement stat = DB.prepareStatement(query);
        ResultSet RS = stat.executeQuery();
        return RS;
    }

    public static PreparedStatement newTransact(String baseQuery,
                                                Connection dbCon) throws SQLException {
        PreparedStatement toReturn = (PreparedStatement) dbCon
                .prepareStatement(baseQuery);

        needCommit = true;
        return toReturn;
    }

    public static void commitTransacts() {
        try {
            myConnectionLocker.lock();
            Connection(false).commit();
            Connection(true).commit();
        } catch (SQLException e) {
            Console.println("SQL ERROR:" + e.getMessage(), Color.RED);
            e.printStackTrace();
        } finally {
            myConnectionLocker.unlock();
        }
    }

    public static void rollBack(Connection con) {
        try {
            synchronized (con) {
                con.rollback();
            }
        } catch (SQLException e) {
            Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    public static void closeCons(boolean realm) {
        if ((realm && realmConnection != null) || (!realm && gameConnection != null)) { // On peut pas fermer une connexion qui n'a jamais eu lieu
            try {
                commitTransacts();
                try {
                    myConnectionLocker.lock();
                    if (!realm)
                        gameConnection.close();
                    else
                        realmConnection.close();
                } finally {
                    myConnectionLocker.unlock();
                }
            } catch (Exception e) {
                Console.println("Erreur a la fermeture des connexions SQL:"
                        + e.getMessage(), Color.RED);
                e.printStackTrace();
            }
        }
    }

    public static void UPDATE_ACCOUNT_DATA(Account acc) {
        try {
            String baseQuery = "UPDATE accounts SET " +
                    "`level` = ?," +
                    "`pseudo` = ?," +
                    "`banned` = ?," +
                    "`banned_time` = ?," +
                    "`friends` = ?," +
                    "`enemy` = ?," +
                    "`mute_time` = ?," +
                    "`mute_raison` = ?," +
                    "`mute_pseudo` = ?," +
                    "`helper` = ?" +
                    " WHERE `guid` = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(true));
            SAVE_BANQUE(acc);
            p.setInt(1, acc.getGmLevel());
            p.setString(2, acc.getPseudo());
            p.setInt(3, (acc.isBanned() ? 1 : 0));
            p.setLong(4, acc.getBannedTime());
            p.setString(5, acc.parseFriendListToDB());
            p.setString(6, acc.parseEnemyListToDB());
            p.setLong(7, acc.getMuteTime());
            p.setString(8, acc.getMuteRaison());
            p.setString(9, acc.getMutePseudo());
            p.setInt(10, acc.isHelper ? 1 : 0);
            p.setInt(11, acc.getGuid());

            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void SAVE_BANQUE(Account compte) {
        try {
            String query = "REPLACE INTO banque(compte, objets, kamas) VALUES (?, ?, ?);";
            PreparedStatement p = newTransact(query, Connection(false));
            p.setInt(1, compte.getGuid());
            p.setString(2, compte.parseBankObjetsToDB());
            p.setLong(3, compte.getBankKamas());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void LOAD_QUIZZ_QUESTIONS() {
        try {
            String query = "SELECT * from event_intello;";
            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {

                EventQuizz.getCouples().put(RS.getInt("id"), new EventQuizz(
                        RS.getInt("id"),
                        RS.getString("question"),
                        RS.getString("reponse"),
                        RS.getInt("points"),
                        RS.getInt("orthographe") == 1 ? true : false));
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void LOAD_PORTES() {
        try {
            PreparedStatement p = newTransact("SELECT * FROM portes;", Connection(false));
            ResultSet RS = p.executeQuery();
            while (RS.next()) {
                Porte porte = new Porte(RS.getString("cellulesRequises"), RS.getInt("cellule"), RS.getInt("temps"), RS.getString("cellulesDebloque"));
                short mapID = RS.getShort("carteID");
                if (!World.portes.containsKey(mapID)) {
                    List<Porte> liste = new ArrayList<Porte>();
                    liste.add(porte);
                    World.portes.put(mapID, liste);
                } else {
                    World.portes.get(mapID).add(porte);
                }
            }

        } catch (SQLException e) {

        }
    }

    public static boolean PLAYER_EXIST(String name) {
        boolean exist = false;

        try {
            String query = "SELECT * FROM `personnages` WHERE `name` = ? AND `server` = ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, name);
            ps.setInt(2, GameServer.id);
            ResultSet RS = ps.executeQuery();
            while (RS.next())
                exist = true;
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }

        return exist;
    }

    public static boolean LOAD_CONFIG() {
        boolean wellLoaded = false;

        try {
            String query = "SELECT * FROM config;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                if (RS.getString("param").equalsIgnoreCase("rateXP")) {
                    Config.RATE_PVM = RS.getInt("value");
                    wellLoaded = true;
                }
            }
            closeResultSet(RS);
        } catch (Exception e) {
            GameServer.addToLog("SQL ERROR: " + e);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return wellLoaded;
    }

    public static void LOAD_CRAFTS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from crafts;", false);
            while (RS.next()) {
                ArrayList<Couple<Integer, Integer>> m = new ArrayList<Couple<Integer, Integer>>();

                boolean cont = true;
                for (String str : RS.getString("craft").split(";")) {
                    try {
                        int tID = Integer.parseInt(str.split("\\*")[0]);
                        int qua = Integer.parseInt(str.split("\\*")[1]);
                        m.add(new Couple<Integer, Integer>(tID, qua));
                    } catch (Exception e) {
                        e.printStackTrace();
                        cont = false;
                    }
                    ;
                }
                //s'il y a eu une erreur de parsing, on ignore cette recette
                if (!cont) continue;

                World.addCraft
                        (
                                RS.getInt("id"),
                                m
                        );
            }
            //if(i == 0) //Console.print("\r-crafts loaded : " + i, Color.GREEN);
            closeResultSet(RS);
            ;
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_CHALLENGES() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from challenge;", false);
            while (RS.next()) {
                StringBuilder chal = new StringBuilder();
                chal.append(RS.getInt("id")).append(",");
                chal.append(RS.getInt("gainXP")).append(",");
                chal.append(RS.getInt("gainDrop")).append(",");
                chal.append(RS.getInt("gainParMob")).append(",");
                chal.append(RS.getInt("conditions"));
                World.addChallenge(chal.toString());
            }
            //if(i == 0) //Console.print("\r-challenges loaded : " + i, Color.GREEN);
            closeResultSet(RS);
            ;
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void UPDATE_SUBAREA(SubArea subarea) {
        try {
            String baseQuery = "UPDATE `subarea_data` SET `alignement` = ?, `Prisme` = ? WHERE id = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, subarea.getalignement());
            p.setInt(2, subarea.getPrismeID());
            p.setInt(3, subarea.getID());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UPDATE_AREA(Area area) {
        try {
            String baseQuery = "UPDATE `area_data` SET `alignement` = ?, `Prisme` = ? WHERE id = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, area.getalignement());
            p.setInt(2, area.getPrismeID());
            p.setInt(3, area.getID());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int LOAD_PRISMES() {
        int numero = 0;
        try {
            ResultSet RS = executeQuery("SELECT * from prismes;",
                    false);
            while (RS.next()) {
                World.addPrisme(new Prism(RS.getInt("id"), RS
                        .getInt("alignement"), RS.getInt("level"), RS
                        .getShort("Carte"), RS.getInt("cell"), RS
                        .getInt("honor"), RS.getInt("area")));
            }
            //if(i == 0) //Console.print("\r-prismes loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
            e.printStackTrace();
            numero = 0;
        }
        return numero;
    }

    public static void ADD_PRISME(Prism Prisme) {
        try {
            String baseQuery = "INSERT INTO prismes(`id`,`alignement`,`level`,`Carte`,`cell`,`area`, `honor`) VALUES(?,?,?,?,?,?,?);";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, Prisme.getID());
            p.setInt(2, Prisme.getalignement());
            p.setInt(3, Prisme.getlevel());
            p.setInt(4, Prisme.getCarte());
            p.setInt(5, Prisme.getCell());
            p.setInt(6, Prisme.getAreaConquest());
            p.setInt(7, Prisme.getHonor());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void DELETE_PRISME(int id) {
        String baseQuery = "DELETE FROM prismes WHERE id = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Game: Query: " + baseQuery);
        }
    }

    public static void SAVE_PRISME(Prism Prisme) {
        String baseQuery = "UPDATE prismes SET `level` = ?, `honor` = ?, `area`= ? WHERE `id` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, Prisme.getlevel());
            p.setInt(2, Prisme.getHonor());
            p.setInt(3, Prisme.getAreaConquest());
            p.setInt(4, Prisme.getID());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Query: " + baseQuery);
            e.printStackTrace();
        }
    }

    public static void LOAD_GUILDS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from guilds;", false);
            while (RS.next()) {
                World.addGuild
                        (
                                new Guild(
                                        RS.getInt("id"),
                                        RS.getString("name"),
                                        RS.getString("emblem"),
                                        RS.getInt("lvl"),
                                        RS.getLong("xp"),
                                        RS.getInt("capital"),
                                        RS.getInt("nbrmax"),
                                        RS.getString("sorts"),
                                        RS.getString("stats")
                                ), false
                        );
            }
            //if(i == 0) //Console.print("\r-guilds loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_GUILD_MEMBERS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from guild_members;", false);
            while (RS.next()) {
                Guild G = World.getGuild(RS.getInt("guild"));
                if (G == null) continue;
                G.addMember(RS.getInt("guid"),
                        RS.getString("name"),
                        RS.getInt("level"),
                        RS.getInt("gfxid"),
                        RS.getInt("rank"),
                        RS.getByte("pxp"),
                        RS.getLong("xpdone"),
                        RS.getInt("rights"),
                        RS.getByte("align"),
                        RS.getString("lastConnection").toString().replaceAll("-", "~"));
            }
            //if(i == 0) //Console.print("\r-guild members loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_MOUNTS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from mounts_data;", false);
            while (RS.next()) {
                World.addDragodinde
                        (
                                new Mount
                                        (
                                                RS.getInt("id"),
                                                RS.getInt("color"),
                                                RS.getInt("sexe"),
                                                RS.getInt("amour"),
                                                RS.getInt("endurance"),
                                                RS.getInt("level"),
                                                RS.getLong("xp"),
                                                RS.getString("name"),
                                                RS.getInt("fatigue"),
                                                RS.getInt("energie"),
                                                RS.getInt("reproductions"),
                                                RS.getInt("maturite"),
                                                RS.getInt("serenite"),
                                                RS.getString("items"),
                                                RS.getString("ancetres"),
                                                RS.getString("ability")
                                        )
                        );
            }
            //if(i == 0) //Console.print("\r-mounts loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_DROPS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from drops;", false);
            while (RS.next()) {
                int monsterID = RS.getInt("monsterId");
                if (monsterID == 0) { // Drop possible sur tous les groupes de monstres
                    World.dropGlobal.add(new Drop(
                            RS.getInt("objectId"),
                            RS.getInt("ceil"),
                            RS.getFloat("percentGrade1"),
                            RS.getInt("max")
                    ));
                } else {
                    Monster MT = World.getMonstre(monsterID);
                    if (MT != null) {
                        LinkedList<Float> taux = new LinkedList<Float>();
                        for (int i = 1; i < 6; i++) {
                            float tauxPourCeGrade = 0;
                            try {
                                tauxPourCeGrade = RS.getFloat("percentGrade" + i);
                            } catch (Exception e) {
                            }
                            taux.add(tauxPourCeGrade);
                        }
                        MT.addDrop(new Drop(
                                RS.getInt("objectId"),
                                RS.getInt("ceil"),
                                taux,
                                RS.getInt("max")
                        ));
                    }
                }
            }
            //if(i == 0) //Console.print("\r-drops loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_ITEMSETS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from itemsets;", false);
            while (RS.next()) {
                World.addItemSet(
                        new ItemSet
                                (
                                        RS.getInt("id"),
                                        RS.getString("items"),
                                        RS.getString("bonus")
                                )
                );
            }
            //if(i == 0) //Console.print("\r-itemsets loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_IOTEMPLATE() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from interactive_objects_data;", false);
            while (RS.next()) {
                World.addIOTemplate(
                        new IOTemplate
                                (
                                        RS.getInt("id"),
                                        RS.getInt("respawn"),
                                        RS.getInt("duration"),
                                        RS.getInt("unknow"),
                                        RS.getInt("walkable") == 1
                                )
                );
            }
            //if(i == 0) //Console.print("\r-io template loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int LOAD_MOUNTPARKS() {
        int nbr = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from mountpark_data;", false);
            while (RS.next()) {
                Maps map = World.getCarte(RS.getShort("mapid"));
                if (map == null) continue;
                World.addMountPark(
                        new MountPark(
                                RS.getInt("owner"),
                                map,
                                RS.getInt("cellid"),
                                RS.getInt("size"),
                                RS.getString("data"),
                                RS.getInt("guild"),
                                RS.getInt("price")
                        ));
            }
            //if(i == 0) //Console.print("\r-mountparks loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            nbr = 0;
        }
        return nbr;
    }

    public static void LOAD_JOBS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from jobs_data;", false);
            while (RS.next()) {
                World.addJob(
                        new Job(
                                RS.getInt("id"),
                                RS.getString("tools"),
                                RS.getString("crafts")
                        )
                );
            }
            //if(i == 0) //Console.print("\r-jobs loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_AREA() {
        try {
            ResultSet RS = executeQuery("SELECT * from area_data;",
                    false);
            while (RS.next()) {
                Area A = new Area(RS.getInt("id"),
                        RS.getInt("superarea"), RS.getString("name"),
                        RS.getInt("alignement"), RS.getInt("Prisme"));

                World.addArea(A);

                A.getSuperArea().addArea(A);
            }
            //if(i == 0) //Console.print("\r-areas loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_SUBAREA() {
        try {
            ResultSet RS = executeQuery("SELECT * from subarea_data;",
                    false);
            while (RS.next()) {
                SubArea SA = new SubArea(RS.getInt("id"),
                        RS.getInt("area"), RS.getInt("alignement"),
                        RS.getString("name"), RS.getInt("isFree"),
                        RS.getInt("Prisme"));

                World.addSubArea(SA);

                if (SA.getArea() != null)
                    SA.getArea().addSubArea(SA);
            }
            //if(i == 0) //Console.print("\r-subareas loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int LOAD_NPCS() {
        int nbr = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from npcs;", false);
            while (RS.next()) {
                Maps map = World.getCarte(RS.getShort("mapid"));
                if (map == null) continue;
                map.addNpc(RS.getInt("npcid"), RS.getInt("cellid"), RS.getInt("orientation"), RS.getInt("move"), RS.getString("text"));
            }
            //if(i == 0) //Console.print("\r-npc loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            nbr = 0;
        }
        return nbr;
    }

    public static int LOAD_PERCEPTEURS() {
        int nbr = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from percepteurs;", false);
            while (RS.next()) {
                Maps map = World.getCarte(RS.getShort("mapid"));
                if (map == null) continue;

                World.addPerco(
                        new Collector(
                                RS.getInt("guid"),
                                RS.getShort("mapid"),
                                RS.getInt("cellid"),
                                RS.getByte("orientation"),
                                RS.getInt("guild_id"),
                                RS.getShort("N1"),
                                RS.getShort("N2"),
                                RS.getString("objets"),
                                RS.getLong("kamas"),
                                RS.getLong("xp")
                        ));
            }
            //if(i == 0) //Console.print("\r-percepteurs loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            nbr = 0;
        }
        return nbr;
    }

    public static int LOAD_HOUSES() {
        int nbr = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from houses;", false);
            while (RS.next()) {
                Maps map = World.getCarte(RS.getShort("map_id"));
                if (map == null) continue;

                World.addHouse(
                        new Houses(
                                RS.getInt("id"),
                                RS.getShort("map_id"),
                                RS.getInt("cell_id"),
                                RS.getInt("owner_id"),
                                RS.getInt("sale"),
                                RS.getInt("guild_id"),
                                RS.getInt("access"),
                                RS.getString("key"),
                                RS.getInt("guild_rights"),
                                RS.getInt("mapid"),
                                RS.getInt("caseid")
                        ));
            }
            //if(i == 0) //Console.print("\r-houses loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            nbr = 0;
        }
        return nbr;
    }

    public static int getNextPersonnageGuid() {
        try {
            ResultSet RS = executeQuery("SELECT guid FROM personnages ORDER BY guid DESC LIMIT 1;", false);
            if (!RS.first()) return 1;
            int guid = RS.getInt("guid");
            guid++;
            closeResultSet(RS);
            return guid;
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            Reboot.reboot();
        }
        return 0;
    }

    public static void LOAD_PLAYER_QUESTS(Player perso) {
        try {
            String query = "SELECT * FROM `quest_players` WHERE `player` = ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, perso.getGuid());

            ResultSet RS = ps.executeQuery();

            while (RS.next()) {
                perso.addQuestPerso(new QuestPlayer(RS.getInt("id"), RS.getInt("quest"), RS.getInt("finish") == 1, perso, RS.getString("stepsValidation")));
            }
            closeResultSet(RS);
        } catch (Exception e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_QUESTS() {
        try {
            String query = "SELECT * FROM `quests`;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ResultSet RS = ps.executeQuery();

            while (RS.next()) {
                Quest quest = new Quest(RS.getInt("id"), RS.getString("etapes"), RS.getString("objectif"), RS.getInt("npc"), RS.getString("action"), RS.getString("args"), (RS.getInt("deleteFinish") == 1), RS.getString("condition"));
                if (quest.getNpc_Tmpl() != null) {
                    quest.getNpc_Tmpl().setQuest(quest);
                    quest.getNpc_Tmpl().set_extraClip(4);
                }
                Quest.setQuestInList(quest);
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_PERSO_BY_ACCOUNT(int accID) {
        try {
            Account c = World.getCompte(accID);
            if (c != null) {
                Map<Integer, Player> persos = c.getPlayers();
                if (persos != null) {
                    for (Player p : persos.values()) {
                        if (p == null) continue;
                        World.verifyClone(p);
                    }
                }
            }
        } catch (Exception e) {
            Console.println("Anti clone fail : " + accID + " > " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
        try {
            String query = "SELECT * FROM personnages WHERE account = ? AND deleted = 0;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, accID);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                if (RS.getInt("server") != GameServer.id)
                    continue;
                Player p = World.getPlayer(RS.getInt("guid"));

                TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
                stats.put(Constant.STATS_ADD_VITA, RS.getInt("vitalite"));
                stats.put(Constant.STATS_ADD_FORC, RS.getInt("force"));
                stats.put(Constant.STATS_ADD_SAGE, RS.getInt("sagesse"));
                stats.put(Constant.STATS_ADD_INTE, RS.getInt("intelligence"));
                stats.put(Constant.STATS_ADD_CHAN, RS.getInt("chance"));
                stats.put(Constant.STATS_ADD_AGIL, RS.getInt("agilite"));

                if (p != null) {
                    if (p.getFight() != null) {
                        if (World.getCompte(accID) != null)
                            World.getCompte(accID).addPerso(p);
                        continue;
                    }
                }
                Player perso = new Player(
                        RS.getInt("guid"),
                        RS.getString("name"),
                        RS.getInt("sexe"),
                        RS.getInt("class"),
                        RS.getInt("color1"),
                        RS.getInt("color2"),
                        RS.getInt("color3"),
                        RS.getLong("kamas"),
                        RS.getInt("spellboost"),
                        RS.getInt("capital"),
                        RS.getInt("energy"),
                        RS.getInt("level"),
                        RS.getLong("xp"),
                        RS.getInt("size"),
                        RS.getInt("gfx"),
                        RS.getByte("alignement"),
                        RS.getInt("account"),
                        stats,
                        RS.getByte("seeFriend"),
                        RS.getByte("seeAlign"),
                        RS.getByte("seeSeller"),
                        RS.getString("canaux"),
                        RS.getShort("map"),
                        RS.getInt("cell"),
                        RS.getString("objets"),
                        RS.getString("storeObjets"),
                        RS.getInt("pdvper"),
                        RS.getString("spells"),
                        RS.getString("savepos"),
                        RS.getString("jobs"),
                        RS.getInt("mountxpgive"),
                        RS.getInt("mount"),
                        RS.getInt("honor"),
                        RS.getInt("deshonor"),
                        RS.getInt("alvl"),
                        RS.getString("zaaps"),
                        RS.getInt("title"),
                        RS.getInt("wife"),
                        RS.getInt("teamID"),
                        RS.getInt("server"),
                        RS.getInt("prestige"),
                        RS.getString("folowers"),
                        RS.getInt("currentFolower"),
                        RS.getInt("winKolizeum"),
                        RS.getInt("loseKolizeum"),
                        RS.getInt("winArena"),
                        RS.getInt("loseArena"),
                        RS.getInt("canExp"),
                        RS.getInt("pvpMod"),
                        RS.getString("candy_used"),
                        RS.getString("quest"),
                        RS.getInt("sFuerza"),
                        RS.getInt("sInteligencia"),
                        RS.getInt("sAgilidad"),
                        RS.getInt("sSuerte"),
                        RS.getInt("sVitalidad"),
                        RS.getInt("sSabiduria"),
                        RS.getInt("ornement"),
                        RS.getString("checkpoints"),
                        RS.getInt("restriction") > 0 ? true : false
                );
                //Vérifications pré-connexion
                perso.VerifAndChangeItemPlace();
                // Chargement de la progression des quêtes du joueur
                SQLManager.LOAD_PLAYER_QUESTS(perso);
                World.addPersonnage(perso);
                int guildId = isPersoInGuild(RS.getInt("guid"));
                if (guildId >= 0) {
                    perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
                }
                if (World.getCompte(accID) != null) {
                    World.getCompte(accID).addPerso(perso);
                    perso.setOrnementsList(LOAD_ORNEMENTS(perso.getGuid()));
                }
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            Reboot.reboot();
        }
    }

    public static Map.Entry<String, Long> LOAD_BANQUE(int compte) {
        String objets = "";
        long kamas = 0;
        try {
            java.sql.PreparedStatement ps = newTransact("SELECT * FROM banque WHERE compte = ?;", Connection(false));
            ps.setInt(1, compte);
            ResultSet RS = ps.executeQuery();
            if (RS.next()) {
                objets = RS.getString("objets");
                kamas = RS.getLong("kamas");
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return new AbstractMap.SimpleEntry<String, Long>(objets, kamas);
    }

    public static boolean DELETE_PERSO_IN_BDD(Player perso) {
        int guid = perso.getGuid();
        String baseQuery = "UPDATE personnages SET deleted = 1 WHERE guid = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, guid);

            p.execute();

            /*if (!perso.getItemsIDSplitByChar(",").equals("")) {
                baseQuery = "DELETE FROM items WHERE guid IN (?) AND server = ?;";
                p = newTransact(baseQuery, Connection(false));
                p.setString(1, perso.getItemsIDSplitByChar(","));
                p.setInt(2, GameServer.id);
                p.execute();
            }
            if (!perso.getStoreItemsIDSplitByChar(",").equals("")) {
                baseQuery = "DELETE FROM items WHERE guid IN (?) AND server = ?;";
                p = newTransact(baseQuery, Connection(false));
                p.setString(1, perso.getStoreItemsIDSplitByChar(","));
                p.setInt(2, GameServer.id);
                p.execute();
            }
            if (perso.getMount() != null) {
                baseQuery = "DELETE FROM mounts_data WHERE id = ?";
                p = newTransact(baseQuery, Connection(false));
                p.setInt(1, perso.getMount().get_id());

                p.execute();
                World.delDragoByID(perso.getMount().get_id());
            }*/

            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
            GameServer.addToLog("Game: Supression du personnage echouee");
            return false;
        }
    }

    public static boolean ADD_PERSO_IN_BDD(Player perso) {
        String baseQuery = "INSERT INTO personnages( `guid` , `name` , `sexe` , `class` , `color1` , `color2` , `color3` , `kamas` , `spellboost` , `capital` , `energy` , `level` , `xp` , `size` , `gfx` , `account`,`cell`,`map`,`spells`,`objets`, `storeObjets`, `server`, `prestige`, `folowers`, `canExp`, `pvpMod`, `candy_used`, `checkpoints`)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'', '', ?, ?, ?, ?, ?, ?, '');";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));

            p.setInt(1, perso.getGuid());
            p.setString(2, perso.getName());
            p.setInt(3, perso.get_sexe());
            p.setInt(4, perso.get_classe());
            p.setInt(5, perso.get_color1());
            p.setInt(6, perso.get_color2());
            p.setInt(7, perso.get_color3());
            p.setLong(8, perso.get_kamas());
            p.setInt(9, perso.get_spellPts());
            p.setInt(10, perso.get_capital());
            p.setInt(11, perso.get_energy());
            p.setInt(12, perso.getLevel());
            p.setLong(13, perso.get_curExp());
            p.setInt(14, perso.get_size());
            p.setInt(15, perso.get_gfxID());
            p.setInt(16, perso.getAccID());
            p.setInt(17, perso.get_curCell().getID());
            p.setInt(18, perso.getMap().get_id());
            p.setString(19, perso.parseSpellToDB());
            p.setInt(20, perso.getServer());
            p.setInt(21, perso.getPrestige());
            p.setString(22, perso.getFolowersString());
            p.setInt(23, perso.getCanExp());
            p.setInt(24, perso.getPvpMod());
            p.setString(25, perso.getCandyUsed());
            p.execute();
            closePreparedStatement(p);
            Console.println("Création d'un personage en bdd : succès !", Color.YELLOW);
            return true;
        } catch (SQLException e) {
            Console.println(e.getMessage(), Color.RED);
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
            GameServer.addToLog("Game: Creation du personnage echouee");
            return false;
        }
    }

    public static void LOAD_EXP() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from experience;", false);

            while (RS.next()) {
                World.addExpLevel(RS.getInt("lvl"),
                        new World.ExpLevel(RS.getLong("perso"),
                                RS.getInt("metier"),
                                RS.getInt("dinde"),
                                RS.getInt("pvp")));
            }
            //if(i == 0) //Console.print("\r-levels loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void RESET_MOUNTPARKS() {
        PreparedStatement p;
        String query = "UPDATE mountpark_data SET data='' WHERE mapid='8747';";
        try {
            p = newTransact(query, Connection(false));

            //Console.print("\r-mountpark reset", Color.GREEN);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void LOAD_TRIGGERS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM `scripted_cells`", false);
            while (RS.next()) {
                if (World.getCarte(RS.getShort("MapID")) == null) continue;
                if (World.getCarte(RS.getShort("MapID")).getCase(RS.getInt("CellID")) == null) continue;

                switch (RS.getInt("EventID")) {
                    case 1://Stop sur la case(triggers)
                        World.getCarte(RS.getShort("MapID")).getCase(RS.getInt("CellID")).addOnCellStopAction(RS.getInt("ActionID"), RS.getString("ActionsArgs"), RS.getString("Conditions"));
                        break;

                    default:
                        GameServer.addToLog("action event " + RS.getInt("EventID") + " not implanted");
                        break;
                }
            }
            //if(i == 0) //Console.print("\r-triggers loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void LOAD_EVENTS() {
        try {
            String query = "SELECT * from events;";
            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                if (!RS.getString("timestart").contains(",")) {
                    int time = (Integer.parseInt(RS.getString("timestart").split("h")[0]) * 60) + (Integer.parseInt(RS.getString("timestart").split("h")[1]));
                    Event.addEvent(new Event(RS.getInt("type"), RS.getInt("minplayers"), RS.getInt("maxplayers"), time, true));
                } else {
                    for (String s : RS.getString("timestart").split(",")) {
                        int time = (Integer.parseInt(s.split("h")[0]) * 60) + (Integer.parseInt(s.split("h")[1]));
                        Event.addEvent(new Event(RS.getInt("type"), RS.getInt("minplayers"), RS.getInt("maxplayers"), time, true));
                    }
                }
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void LOAD_GIFTS() {
        try {
            String query = "SELECT * from gifts ORDER BY giftTemplate ASC, chance ASC;";
            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                if (!World.cadeaux.containsKey(RS.getInt("giftTemplate"))) {
                    new Gift(RS.getInt("giftTemplate"), RS.getInt("template"), RS.getInt("limit"), RS.getInt("gain"), RS.getFloat("chance"), RS.getInt("qty"));
                } else {
                    World.cadeaux.get(RS.getInt("giftTemplate")).addProb(RS.getInt("template"), RS.getInt("limit"), RS.getInt("gain"), RS.getFloat("chance"), RS.getInt("qty"));
                }
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void LOAD_MAPS() {
        try {
            ResultSet RS;
            RS = SQLManager.executeQuery("SELECT * from maps LIMIT " + Constant.DEBUG_MAP_LIMIT + ";", false);
            while (RS.next()) {
                Maps carte = new Maps(
                        RS.getShort("id"),
                        RS.getString("date"),
                        RS.getByte("width"),
                        RS.getByte("heigth"),
                        RS.getString("key"),
                        RS.getString("places"),
                        RS.getString("mapData"),
                        RS.getString("cells"),
                        RS.getString("monsters"),
                        RS.getString("mappos"),
                        RS.getByte("numgroup"),
                        RS.getByte("groupmaxsize")
                );
                World.addCarte(carte);
                // Chargement des mobgroups sauvegardé
                String requete = "SELECT cellid, data FROM mobgroups_save WHERE mapid = ?;";
                PreparedStatement p = newTransact(requete, Connection(false));
                p.setInt(1, carte.get_id());
                ResultSet mobgroups = p.executeQuery();
                while (mobgroups.next()) {
                    carte.mobgroups_save.put(mobgroups.getInt("cellid"), mobgroups.getString("data"));
                    carte.addSavedGroup(mobgroups.getInt("cellid"));
                }
                carte.spawnGroupInit();
                closeResultSet(mobgroups);

                // Ajouts des portes interactives
                carte.portes = World.portes.get(carte.get_id());
            }
            //if(i == 0) //Console.print("\r-maps loaded : " + i, Color.GREEN);
            SQLManager.closeResultSet(RS);


            RS = SQLManager.executeQuery("SELECT * from mobgroups_fix;", false);
            while (RS.next()) {
                Maps c = World.getCarte(RS.getShort("mapid"));
                if (c == null) continue;
                if (c.getCase(RS.getInt("cellid")) == null) continue;
                String args = RS.getString("spawnTime");
                int spawnTimeMin = 0;
                int spawnTimeMax = 0;
                int ellap = RS.getInt("ellap");
                if (args.contains(";")) { // min / max set
                    String[] minMax = args.split(";");
                    spawnTimeMin = Integer.valueOf(minMax[0]);
                    spawnTimeMax = Integer.valueOf(minMax[1]);
                } else {
                    spawnTimeMin = Integer.valueOf(args);
                }
                int cellid = RS.getInt("cellid");
                if (!c.mobgroups_save.containsKey(cellid)) { // N'a pas été spawn
                    c.addStaticGroup(cellid, RS.getString("groupData"), spawnTimeMin, spawnTimeMax, ellap);
                }
            }
            SQLManager.closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void SAVE_MOBGROUP(Monster.MobGroup MG) {
        try {
            String query = "INSERT INTO mobgroups_save(`mapid`, `cellid`, `data`) VALUES(?, ?, ?);";
            PreparedStatement p = newTransact(query, Connection(false));
            p.setInt(1, MG.getMap().get_id());
            p.setInt(2, MG.getOriginCellID());
            p.setString(3, MG.getDataString());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void DELETE_SAVED_MOBGROUP(int mapid, int cellid) {
        try {
            String query = "DELETE FROM mobgroups_save WHERE mapid = ? AND cellid = ?;";
            PreparedStatement p = newTransact(query, Connection(false));
            p.setInt(1, mapid);
            p.setInt(2, cellid);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void LOAD_GMCOMMANDS() {
        try {
            String query = "SELECT * from gmcommands;";
            ResultSet RS = executeQuery(query, false);
            if (!GmCommand.gmCommands.isEmpty()) {
                GmCommand.gmCommands.clear();
            }
            while (RS.next()) {
                GmCommand.gmCommands.put(RS.getString("command").toLowerCase(),
                        RS.getInt("gmlevel"));
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void LOAD_SORTS_INTERDITS() {
        try {
            String query = "SELECT * from sorts_interdits;";
            ResultSet RS = executeQuery(query, false);
            List<Integer> pvp = new ArrayList<Integer>();
            List<Integer> pvm = new ArrayList<Integer>();
            while (RS.next()) {
                int idSort = RS.getInt("idSort");
                switch (RS.getInt("type")) {
                    case 1:
                        pvp.add(idSort);
                        break;
                    case 2:
                        pvm.add(idSort);
                        break;
                    case 3:
                        pvm.add(idSort);
                        pvp.add(idSort);
                        break;
                    default:
                        break;
                }
            }
            Constant.SORTS_INTERDITS_PVP = pvp;
            Constant.SORTS_INTERDITS_PVM = pvm;
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    public static void SAVE_PERSONNAGE(Player _perso, boolean saveItem) {
        String baseQuery = "UPDATE `personnages` SET " +
                "`name`= ?," +
                "`kamas`= ?," +
                "`spellboost`= ?," +
                "`capital`= ?," +
                "`energy`= ?," +
                "`level`= ?," +
                "`xp`= ?," +
                "`size` = ?," +
                "`gfx`= ?," +
                "`alignement`= ?," +
                "`honor`= ?," +
                "`deshonor`= ?," +
                "`alvl`= ?," +
                "`vitalite`= ?," +
                "`force`= ?," +
                "`sagesse`= ?," +
                "`intelligence`= ?," +
                "`chance`= ?," +
                "`agilite`= ?," +
                "`seeSpell`= ?," +
                "`seeFriend`= ?," +
                "`seeAlign`= ?," +
                "`seeSeller`= ?," +
                "`canaux`= ?," +
                "`map`= ?," +
                "`cell`= ?," +
                "`pdvper`= ?," +
                "`spells`= ?," +
                "`objets`= ?," +
                "`storeObjets`= ?," +
                "`savepos`= ?," +
                "`zaaps`= ?," +
                "`jobs`= ?," +
                "`mountxpgive`= ?," +
                "`mount`= ?," +
                "`title`= ?," +
                "`wife`= ?," +
                "`teamID` = ?," +
                "`prestige` = ?," +
                "`folowers` = ?," +
                "`currentFolower` = ?," +
                "`winKolizeum` = ?," +
                "`loseKolizeum` = ?," +
                "`winArena` = ?," +
                "`loseArena` = ?," +
                "`canExp` = ?," +
                "`pvpMod` = ?," +
                "`candy_used`= ?," +
                "`sFuerza`= ?," +
                "`sInteligencia`= ?," +
                "`sAgilidad`= ?," +
                "`sSuerte`= ?," +
                "`sVitalidad`= ?," +
                "`sSabiduria`= ?," +
                "`ornement`= ?," +
                "`quest`= ?" +
                " WHERE `personnages`.`guid` = ? AND `personnages`.`server` = ? LIMIT 1 ;";

        PreparedStatement p = null;

        try {
            p = newTransact(baseQuery, Connection(false));
            p.setString(1, _perso.getName());
            p.setLong(2, _perso.get_kamas());
            p.setInt(3, _perso.get_spellPts());
            p.setInt(4, _perso.get_capital());
            p.setInt(5, _perso.get_energy());
            p.setInt(6, _perso.getLevel());
            p.setLong(7, _perso.get_curExp());
            p.setInt(8, _perso.get_size());
            p.setInt(9, _perso.get_gfxID());
            p.setInt(10, _perso.get_align());
            p.setInt(11, _perso.get_honor());
            p.setInt(12, _perso.getDeshonor());
            p.setInt(13, _perso.getALvl());
            p.setInt(14, _perso.get_baseStats().getEffect(Constant.STATS_ADD_VITA));
            p.setInt(15, _perso.get_baseStats().getEffect(Constant.STATS_ADD_FORC));
            p.setInt(16, _perso.get_baseStats().getEffect(Constant.STATS_ADD_SAGE));
            p.setInt(17, _perso.get_baseStats().getEffect(Constant.STATS_ADD_INTE));
            p.setInt(18, _perso.get_baseStats().getEffect(Constant.STATS_ADD_CHAN));
            p.setInt(19, _perso.get_baseStats().getEffect(Constant.STATS_ADD_AGIL));
            p.setInt(20, (_perso.is_showSpells() ? 1 : 0));
            p.setInt(21, (_perso.is_showFriendConnection() ? 1 : 0));
            p.setInt(22, (_perso.is_showWings() ? 1 : 0));
            p.setInt(23, (_perso.is_showSeller() ? 1 : 0));
            p.setString(24, _perso.get_canaux());
            p.setInt(25, _perso.getMap().get_id());
            p.setInt(26, _perso.get_curCell().getID());
            p.setInt(27, _perso.get_pdvper());
            p.setString(28, _perso.parseSpellToDB());
            p.setString(29, _perso.parseObjetsToDB());
            p.setString(30, _perso.parseStoreItemstoBD());
            p.setString(31, _perso.get_savePos());
            p.setString(32, _perso.parseZaaps());
            p.setString(33, _perso.parseJobData());
            p.setInt(34, _perso.getMountXpGive());
            p.setInt(35, (_perso.getMount() != null ? _perso.getMount().get_id() : -1));
            p.setInt(36, (_perso.get_title()));
            p.setInt(37, _perso.getWife());
            p.setInt(38, _perso.getTeamID());
            p.setInt(39, _perso.getPrestige());
            p.setString(40, _perso.getFolowersString());
            p.setInt(41, _perso.getCurrentFolower());
            p.setInt(42, _perso.getWinKolizeum());
            p.setInt(43, _perso.getLoseKolizeum());
            p.setInt(44, _perso.getWinArena());
            p.setInt(45, _perso.getLoseArena());
            p.setInt(46, _perso.getCanExp());
            p.setInt(47, _perso.getPvpMod());
            p.setString(48, _perso.getCandyUsed().toString());
            p.setInt(49, _perso.getScrollFuerza());
            p.setInt(50, _perso.getScrollInteligencia());
            p.setInt(51, _perso.getScrollAgilidad());
            p.setInt(52, _perso.getScrollSuerte());
            p.setInt(53, _perso.getScrollVitalidad());
            p.setInt(54, _perso.getScrollSabiduria());
            p.setInt(55, _perso.get_ornement());
            p.setString(56, _perso.questsToString());
            p.setInt(57, _perso.getGuid());
            p.setInt(58, GameServer.id);

            p.executeUpdate();

            if (_perso.getGuildMember() != null)
                UPDATE_GUILDMEMBER(_perso.getGuildMember());
            if (_perso.getMount() != null)
                UPDATE_MOUNT_INFOS(_perso.getMount());
            GameServer.addToLog("Personnage " + _perso.getName() + " sauvegarde");
        } catch (Exception e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Requete: " + baseQuery);
            Console.println("Le personnage n'a pas ete sauvegarde");
            Reboot.reboot();
        }

        // Sauvegarde progression quêtes joueur
        if (_perso.getQuestPerso() != null) {
            for (QuestPlayer qP : _perso.getQuestPerso().values()) {
                SAVE_QUEST_PLAYER(qP, _perso);
            }
        }

        if (saveItem) {
            baseQuery = "UPDATE `items` SET qua = ?, pos= ?, stats = ?" +
                    " WHERE guid = ? AND server = ?;";
            try {
                p = newTransact(baseQuery, Connection(false));
            } catch (SQLException e1) {
                e1.printStackTrace();
            }

            for (String idStr : _perso.getItemsIDSplitByChar(":").split(":")) {
                try {
                    int guid = Integer.parseInt(idStr);
                    Item obj = World.getObjet(guid);
                    if (obj == null) continue;

                    p.setInt(1, obj.getQuantity());
                    p.setInt(2, obj.getPosition());
                    p.setString(3, obj.parseToSave());
                    p.setInt(4, Integer.parseInt(idStr));
                    p.setInt(5, GameServer.id);

                    p.execute();
                } catch (Exception e) {
                    continue;
                }
                ;

            }

            if (_perso.getAccount() == null)
                return;
            for (String idStr : _perso.getBankItemsIDSplitByChar(":").split(":")) {
                try {
                    int guid = Integer.parseInt(idStr);
                    Item obj = World.getObjet(guid);
                    if (obj == null) continue;

                    p.setInt(1, obj.getQuantity());
                    p.setInt(2, obj.getPosition());
                    p.setString(3, obj.parseToSave());
                    p.setInt(4, Integer.parseInt(idStr));
                    p.setInt(5, GameServer.id);

                    p.execute();
                } catch (Exception e) {
                    continue;
                }
                ;

            }
        }

        closePreparedStatement(p);
    }

    public static boolean SAVE_PERSONNAGE_ITEM(Player _perso) {
        String baseQuery = "UPDATE `personnages` SET " +
                "`objets`= ?," +
                "`storeObjets`= ?" +
                " WHERE `personnages`.`guid` = ? AND `personnages`.`server` = ? LIMIT 1 ;";

        PreparedStatement p = null;
        boolean fine = true;
        try {
            p = newTransact(baseQuery, Connection(false));
            p.setString(1, _perso.parseObjetsToDB());
            p.setString(2, _perso.parseStoreItemstoBD());
            p.setInt(3, _perso.getGuid());
            p.setInt(4, GameServer.id);

            p.executeUpdate();
        } catch (Exception e) {
            fine = false;
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Requete: " + baseQuery);
            Console.println("Le personnage n'a pas ete sauvegarde");
        }
        closePreparedStatement(p);
        return fine;
    }

    public static boolean SAVE_GIFT_GAIN(int giftTemplate, int template, int gain) {
        String baseQuery = "UPDATE gifts SET gain = ? WHERE giftTemplate = ? AND template = ?;";

        PreparedStatement p = null;
        boolean fine = true;
        try {
            p = newTransact(baseQuery, Connection(false));
            p.setInt(1, gain);
            p.setInt(2, giftTemplate);
            p.setInt(3, template);

            p.executeUpdate();
        } catch (Exception e) {
            fine = false;
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Requete: " + baseQuery);
        }
        closePreparedStatement(p);
        return fine;
    }

    public static void LOAD_SORTS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT  * from sorts;", false);
            while (RS.next()) {
                int id = RS.getInt("id");
                Spell sort = new Spell(id, RS.getInt("sprite"), RS.getString("spriteInfos"), RS.getString("effectTarget"));
                SortStats l1 = parseSortStats(id, 1, RS.getString("lvl1"));
                SortStats l2 = parseSortStats(id, 2, RS.getString("lvl2"));
                SortStats l3 = parseSortStats(id, 3, RS.getString("lvl3"));
                SortStats l4 = parseSortStats(id, 4, RS.getString("lvl4"));
                SortStats l5 = null;
                if (!RS.getString("lvl5").equalsIgnoreCase("-1"))
                    l5 = parseSortStats(id, 5, RS.getString("lvl5"));
                SortStats l6 = null;
                if (!RS.getString("lvl6").equalsIgnoreCase("-1"))
                    l6 = parseSortStats(id, 6, RS.getString("lvl6"));
                sort.addSortStats(1, l1);
                sort.addSortStats(2, l2);
                sort.addSortStats(3, l3);
                sort.addSortStats(4, l4);
                sort.addSortStats(5, l5);
                sort.addSortStats(6, l6);
                World.addSort(sort);
            }
            //if(i == 0) //Console.print("\r-spells loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void LOAD_SHOP() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM shop;", false);
            while (RS.next()) {
                for (String s : RS.getString("servers").split(",")) {
                    if (Integer.parseInt(s) == GameServer.id) {
                        ParseTool.getShop().put(RS.getInt("template"), RS.getInt("price"));
                    }
                }
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static List<Integer> LOAD_NPC_SHOP_ITEMS() {
        List<Integer> templateIds = new ArrayList<Integer>();
        try {
            String query = "SELECT `template` FROM shop WHERE servers = ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, String.valueOf(GameServer.id));
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                templateIds.add(RS.getInt("template"));
            }
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
        return templateIds;
    }

    public static void LOAD_OBJ_TEMPLATE() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT  * from item_template;", false);
            while (RS.next()) {
                try {
                    World.addObjTemplate
                            (
                                    new ObjTemplate
                                            (
                                                    RS.getInt("id"),
                                                    RS.getString("statsTemplate"),
                                                    RS.getString("name"),
                                                    RS.getInt("type"),
                                                    RS.getInt("level"),
                                                    RS.getInt("pod"),
                                                    RS.getInt("prix"),
                                                    RS.getInt("panoplie"),
                                                    RS.getString("condition"),
                                                    RS.getString("armesInfos"),
                                                    RS.getInt("sold"),
                                                    RS.getInt("avgPrice"),
                                                    RS.getInt("prestige")
                                            )
                            );
                } catch (Exception e) {
                }
            }

            //if(i == 0) //Console.print("\r-objects template loaded : " + i, Color.GREEN);
            closeResultSet(RS);
            RS = SQLManager.executeQuery("SELECT  * FROM mains;", false);
            while (RS.next()) {
                int id = RS.getInt("id_item");
                if (World.getObjTemplate(id) != null) {
                    World.getObjTemplate(id).setIsTwoHanded(true);
                }
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    private static SortStats parseSortStats(int id, int lvl, String str) {
        try {
            SortStats stats = null;
            String[] stat = str.split(",");
            String effets = stat[0];
            String CCeffets = stat[1];
            int PACOST = 6;
            try {
                PACOST = Integer.parseInt(stat[2].trim());
            } catch (NumberFormatException e) {
            }
            ;

            int POm = Integer.parseInt(stat[3].trim());
            int POM = Integer.parseInt(stat[4].trim());
            int TCC = Integer.parseInt(stat[5].trim());
            int TEC = Integer.parseInt(stat[6].trim());
            boolean line = stat[7].trim().equalsIgnoreCase("true");
            boolean LDV = stat[8].trim().equalsIgnoreCase("true");
            boolean emptyCell = stat[9].trim().equalsIgnoreCase("true");
            boolean MODPO = stat[10].trim().equalsIgnoreCase("true");
            //int unk = Integer.parseInt(stat[11]);//All 0
            int MaxByTurn = Integer.parseInt(stat[12].trim());
            int MaxByTarget = Integer.parseInt(stat[13].trim());
            int CoolDown = Integer.parseInt(stat[14].trim());
            String type = stat[15].trim();
            int level = Integer.parseInt(stat[stat.length - 2].trim());
            boolean endTurn = stat[19].trim().equalsIgnoreCase("true");
            List<Integer> etatRequis = new ArrayList<Integer>(), etatInterdit = new ArrayList<Integer>();
            try {
                String data = stat[16].replaceAll("\\s+", "");
                if (data.contains(";")) {
                    String[] d1 = data.split(";");
                    for (String s : d1) {
                        int etat = Integer.parseInt(s);
                        if (etat > -1) {
                            etatRequis.add(etat);
                        }
                    }
                } else {
                    int etat = Integer.parseInt(data);
                    if (etat > -1) {
                        etatRequis.add(etat);
                    }
                }
            } catch (Exception e) {

            }
            try {
                String data = stat[17].replaceAll("\\s+", "");
                if (data.contains(";")) {
                    String[] d2 = data.split(";");
                    for (String s : d2) {
                        int etat = Integer.parseInt(s);
                        if (etat > -1) {
                            etatInterdit.add(etat);
                        }
                    }
                } else {
                    int etat = Integer.parseInt(data);
                    if (etat > -1) {
                        etatInterdit.add(etat);
                    }
                }
            } catch (Exception e) {
            }
            stats = new SortStats(id, lvl, PACOST, POm, POM, TCC, TEC, line, LDV, emptyCell, MODPO, MaxByTurn, MaxByTarget, CoolDown, level, endTurn, effets, CCeffets, type, etatRequis, etatInterdit);
            return stats;
        } catch (Exception e) {
            e.printStackTrace();
            int nbr = 0;
            Console.println("[DEBUG]Sort " + id + " lvl " + lvl);
            for (String z : str.split(",")) {
                Console.println("[DEBUG]" + nbr + " " + z);
                nbr++;
            }
            Reboot.reboot();
            return null;
        }
    }

    public static void LOAD_MOB_TEMPLATE() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM monsters;", false);
            while (RS.next()) {
                int id = RS.getInt("id");
                int gfxID = RS.getInt("gfxID");
                int align = RS.getInt("align");
                String colors = RS.getString("colors");
                String grades = RS.getString("grades");
                String spells = RS.getString("spells");
                String stats = RS.getString("stats");
                String pdvs = RS.getString("pdvs");
                String pts = RS.getString("points");
                String inits = RS.getString("inits");
                int mK = RS.getInt("minKamas");
                int MK = RS.getInt("maxKamas");
                int IAType = RS.getInt("AI_Type");
                String xp = RS.getString("exps");
                boolean capturable;
                int scaleX = Integer.parseInt(RS.getString("size").split(",")[0]);
                int scaleY = Integer.parseInt(RS.getString("size").split(",")[1]);
                if (RS.getInt("capturable") == 1) {
                    capturable = true;
                } else {
                    capturable = false;
                }

                World.addMobTemplate
                        (
                                id,
                                new Monster
                                        (
                                                id,
                                                gfxID,
                                                align,
                                                colors,
                                                grades,
                                                spells,
                                                stats,
                                                pdvs,
                                                pts,
                                                inits,
                                                mK,
                                                MK,
                                                xp,
                                                IAType,
                                                capturable,
                                                scaleX,
                                                scaleY
                                        )
                        );
            }
            //if(i == 0) //Console.print("\r-monsters template loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void LOAD_NPC_TEMPLATE() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_template;", false);
            while (RS.next()) {
                int id = RS.getInt("id");
                int bonusValue = RS.getInt("bonusValue");
                int gfxID = RS.getInt("gfxID");
                int scaleX = RS.getInt("scaleX");
                int scaleY = RS.getInt("scaleY");
                int sex = RS.getInt("sex");
                int color1 = RS.getInt("color1");
                int color2 = RS.getInt("color2");
                int color3 = RS.getInt("color3");
                String access = RS.getString("accessories");
                int extraClip = RS.getInt("extraClip");
                int customArtWork = RS.getInt("customArtWork");
                String questions = RS.getString("initQuestion");
                String ventes = RS.getString("ventes");
                String quest = RS.getString("quests");
                World.addNpcTemplate
                        (
                                new NpcTemplate
                                        (
                                                id,
                                                bonusValue,
                                                gfxID,
                                                scaleX,
                                                scaleY,
                                                sex,
                                                color1,
                                                color2,
                                                color3,
                                                access,
                                                extraClip,
                                                customArtWork,
                                                questions,
                                                ventes,
                                                quest
                                        )
                        );
            }
            //if(i == 0) //Console.print("\r-npc template loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void SAVE_NEW_ITEM(Item item) {
        try {
            String baseQuery = "REPLACE INTO items(guid, template, qua, pos, stats, server) VALUES(?,?,?,?,?,?);";

            PreparedStatement p = newTransact(baseQuery, Connection(false));

            p.setInt(1, item.getGuid());
            p.setInt(2, item.getTemplate(false).getID());
            p.setInt(3, item.getQuantity());
            p.setInt(4, item.getPosition());
            p.setString(5, item.parseToSave());
            p.setInt(6, GameServer.id);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int INSERT_NEW_ITEM(Item item) {
        try {

            String sql = "INSERT INTO items (template, qua, pos, stats, server) VALUES (?, ?, ?, ?, ?);";
            PreparedStatement p = (PreparedStatement) Connection(false).prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            p.setInt(1, item.getTemplate(false).getID());
            p.setInt(2, item.getQuantity());
            p.setInt(3, item.getPosition());
            p.setString(4, item.parseToSave());
            p.setInt(5, GameServer.id);

            String baseQuery = "UPDATE `items` SET guid = id WHERE id = ?;";
            PreparedStatement prepareState = newTransact(baseQuery, Connection(false));
            p.executeUpdate();

            ResultSet rs = p.getGeneratedKeys();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt(1);
            }
            closeResultSet(rs);
            if (id > -1) {
                prepareState.setInt(1, id);
                prepareState.execute();
                closePreparedStatement(prepareState);
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void ASSIGN_OWNER_TO_ITEM(Item item, int player) {
        String baseQuery = "UPDATE items SET owner = ? WHERE guid = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, player);
            p.setInt(2, item.getGuid());
            p.executeUpdate();
            item.setNewInDatabase(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int INSERT_NEW_QUEST_PLAYER(QuestPlayer questPlayer) {
        try {

            String sql = "INSERT INTO quest_players(quest, finish, player, stepsValidation) VALUES (?, ?, ?, ?);";
            PreparedStatement p = (PreparedStatement) Connection(false).prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            p.setInt(1, questPlayer.getQuest().getId());
            p.setInt(2, questPlayer.isFinish() ? 1 : 0);
            p.setInt(3, questPlayer.getPlayer().getGuid());
            p.setString(4, questPlayer.getQuestEtapeString());
            p.execute();

            ResultSet rs = p.getGeneratedKeys();
            int id = -1;
            if (rs.next()) {
                id = rs.getInt(1);
            }
            closeResultSet(rs);
            if (id > -1) {
                return id;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean SAVE_QUEST_PLAYER(QuestPlayer questPlayer, Player player) {
        try {
            String baseQuery = "UPDATE `quest_players` SET `quest`= ?, `finish`= ?, `player` = ?, `stepsValidation` = ? WHERE `id` = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, questPlayer.getQuest().getId());
            p.setInt(2, questPlayer.isFinish() ? 1 : 0);
            p.setInt(3, player.getGuid());
            p.setString(4, questPlayer.getQuestEtapeString());
            p.setInt(5, questPlayer.getId());
            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean DELETE_QUEST_PLAYER(int id) {
        try {
            String baseQuery = "DELETE FROM `quest_players` WHERE `id` = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);
            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean SAVE_NEW_FIXGROUP(int mapID, int cellID, String groupData, int ellap) {
        try {
            String baseQuery = "REPLACE INTO mobgroups_fix(mapid, cellid, groupData) VALUES(?,?,?)";
            PreparedStatement p = newTransact(baseQuery, Connection(false));

            p.setInt(1, mapID);
            p.setInt(2, cellID);
            p.setString(3, groupData);
            ;

            p.execute();
            closePreparedStatement(p);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean SAVE_FIXGROUP(int mapID, int cellID, String groupData, int ellap) {
        try {
            String baseQuery = "UPDATE mobgroups_fix SET ellap = ? WHERE mapID = ? AND groupData = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));

            p.setInt(1, ellap);
            p.setInt(2, mapID);
            p.setString(3, groupData);
            ;

            p.execute();
            closePreparedStatement(p);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void LOAD_NPC_QUESTIONS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_questions;", false);
            while (RS.next()) {
                World.addNPCQuestion
                        (
                                new NPC_question
                                        (
                                                RS.getInt("ID"),
                                                RS.getString("responses"),
                                                RS.getString("params"),
                                                RS.getString("cond"),
                                                RS.getString("ifFalse")
                                        )
                        );
            }
            //if(i == 0) //Console.print("\r-npc question loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static void LOAD_NPC_ANSWERS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_reponses_actions;", false);
            while (RS.next()) {
                int id = RS.getInt("ID");
                int type = RS.getInt("type");
                String args = RS.getString("args");
                if (World.getNPCreponse(id) == null)
                    World.addNPCreponse(new NPC_reponse(id));
                World.getNPCreponse(id).addAction(new Action(type, args, ""));
            }
            //if(i == 0) //Console.print("\r-npc answer loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
    }

    public static int LOAD_ENDFIGHT_ACTIONS() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM endfight_action;", false);
            while (RS.next()) {
                Maps map = World.getCarte(RS.getShort("map"));
                if (map == null) continue;
                map.addEndFightAction(RS.getInt("fighttype"),
                        new Action(RS.getInt("action"), RS.getString("args"), RS.getString("cond")));
            }
            //if(i == 0) //Console.print("\r-end fight actions loaded : " + i, Color.GREEN);
            closeResultSet(RS);
            return 0;
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
        return 0;
    }

    public static int LOAD_ITEM_ACTIONS() {
        int nbr = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * FROM use_item_actions;", false);
            while (RS.next()) {
                int id = RS.getInt("template");
                int type = RS.getInt("type");
                String args = RS.getString("args");
                int itemRequis = RS.getInt("itemRequis");
                if (World.getObjTemplate(id) == null) continue;
                ObjTemplate template = World.getObjTemplate(id);
                template.addAction(new Action(type, args, ""));
                if (itemRequis > 0) {
                    template.setObjetRequisPourActions(itemRequis);
                }
            }
            //if(i == 0) //Console.print("\r-items actions loaded : " + i, Color.GREEN);
            closeResultSet(RS);
            return nbr;
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Reboot.reboot();
        }
        return nbr;
    }

    public static void LOAD_ITEMS(String ids) {
        String req = "SELECT * FROM items WHERE guid IN (?) AND server = ?;";
        try {
            java.sql.PreparedStatement ps = newTransact(req, Connection(false));
            ps.setString(1, ids);
            ps.setInt(2, GameServer.id);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                int guid = RS.getInt("guid");
                int tempID = RS.getInt("template");
                int qua = RS.getInt("qua");
                int pos = RS.getInt("pos");
                String stats = RS.getString("stats");
                World.addObjet
                        (
                                World.newObjet
                                        (
                                                guid,
                                                tempID,
                                                qua,
                                                pos,
                                                stats
                                        ),
                                false
                        );
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Requete: \n" + req);
            Reboot.reboot();
        }
    }

    public static void LOAD_ITEMS_FULL() {
        String req = "SELECT * FROM items WHERE server = '" + GameServer.id + "';";
        try {
            ResultSet RS = SQLManager.executeQuery(req, false);
            while (RS.next()) {
                try {
                    int guid = RS.getInt("guid");
                    int tempID = RS.getInt("template");
                    int qua = RS.getInt("qua");
                    int pos = RS.getInt("pos");
                    String stats = RS.getString("stats");
                    World.addObjet
                            (
                                    World.newObjet
                                            (
                                                    guid,
                                                    tempID,
                                                    qua,
                                                    pos,
                                                    stats
                                            ),
                                    false
                            );
                } catch (Exception e) {
                }
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Requete: \n" + req);
            Reboot.reboot();
        }
    }

    public static void DELETE_ITEM(int guid) {
        String baseQuery = "DELETE FROM items WHERE guid = ? AND server = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, guid);
            p.setInt(2, GameServer.id);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void SAVE_ITEM(Item item) {
        String baseQuery = "UPDATE `items` SET template = ?,qua = ?, pos = ?, stats = ? WHERE guid = ? AND server = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, item.getTemplate(false).getID());
            p.setInt(2, item.getQuantity());
            p.setInt(3, item.getPosition());
            p.setString(4, item.parseToSave());
            p.setInt(5, item.getGuid());
            p.setInt(6, GameServer.id);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void SAVE_ITEM_QUANTITY(int guid, int qua) {
        String baseQuery = "UPDATE `items` SET qua = ? WHERE guid = ? AND server = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, qua);
            p.setInt(2, guid);
            p.setInt(3, GameServer.id);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void CREATE_MOUNT(Mount DD) {
        String baseQuery = "REPLACE INTO `mounts_data`(`id`,`color`,`sexe`,`name`,`xp`,`level`," +
                "`endurance`,`amour`,`maturite`,`serenite`,`reproductions`,`fatigue`,`items`," +
                "`ancetres`,`energie`, `ability`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, DD.get_id());
            p.setInt(2, DD.get_color());
            p.setInt(3, DD.get_sexe());
            p.setString(4, DD.get_nom());
            p.setLong(5, DD.get_exp());
            p.setInt(6, DD.get_level());
            p.setInt(7, DD.get_endurance());
            p.setInt(8, DD.get_amour());
            p.setInt(9, DD.get_maturite());
            p.setInt(10, DD.get_serenite());
            p.setInt(11, DD.get_reprod());
            p.setInt(12, DD.get_fatigue());
            p.setString(13, DD.parseObjDB());
            p.setString(14, DD.get_ancetres());
            p.setInt(15, DD.get_energie());
            p.setString(16, DD.get_ability());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void REMOVE_MOUNT(int DID) {
        String baseQuery = "DELETE FROM `mounts_data` WHERE `id` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, DID);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void LOAD_ACCOUNT_BY_IP(String ip) {
        try {
            String query = "SELECT * from accounts WHERE `lastIP` = ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(true));
            ps.setString(1, ip);
            ResultSet RS = ps.executeQuery();
            //ResultSet RS = SQLManager.executeQuery("SELECT * from accounts;",false);
            String baseQuery = "UPDATE accounts " +
                    "SET `reload_needed` = 0 " +
                    "WHERE guid = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(true));

            while (RS.next()) {
                //Si le compte est déjà connecté, on zap
                if (World.getCompte(RS.getInt("guid")) != null)
                    if (World.getCompte(RS.getInt("guid")).isOnline()) continue;

                Account C = new Account(
                        RS.getInt("guid"),
                        RS.getString("account").toLowerCase(),
                        RS.getString("pass"),
                        RS.getString("pseudo"),
                        RS.getString("question"),
                        RS.getString("reponse"),
                        RS.getInt("level"),
                        RS.getInt("vip"),
                        (RS.getInt("banned") == 1),
                        RS.getLong("banned_time"),
                        RS.getString("lastIP"),
                        RS.getString("lastConnectionDate"),
                        RS.getString("bank"),
                        RS.getLong("bankKamas"),
                        RS.getString("friends"),
                        RS.getString("enemy"),
                        RS.getInt("cadeau"),
                        RS.getLong("mute_time"),
                        RS.getString("mute_raison"),
                        RS.getString("mute_pseudo"),
                        RS.getInt("vote"),
                        RS.getInt("helper") > 0 ? true : false
                );
                World.addAccount(C);
                World.ReassignAccountToChar(C);

                p.setInt(1, RS.getInt("guid"));
                p.executeUpdate();
            }

            closePreparedStatement(p);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static Account LOAD_ACCOUNT(int guid) {

        Account account = null;

        try {
            String query = "SELECT * FROM accounts WHERE guid = ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(true));
            ps.setInt(1, guid);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                account = new Account(
                        RS.getInt("guid"),
                        RS.getString("account").toLowerCase(),
                        RS.getString("pass"),
                        RS.getString("pseudo"),
                        RS.getString("question"),
                        RS.getString("reponse"),
                        RS.getInt("level"),
                        RS.getInt("vip"),
                        (RS.getInt("banned") == 1),
                        RS.getLong("banned_time"),
                        RS.getString("lastIP"),
                        RS.getString("lastConnectionDate"),
                        RS.getString("bank"),
                        RS.getLong("bankKamas"),
                        RS.getString("friends"),
                        RS.getString("enemy"),
                        RS.getInt("cadeau"),
                        RS.getLong("mute_time"),
                        RS.getString("mute_raison"),
                        RS.getString("mute_pseudo"),
                        RS.getInt("vote"),
                        RS.getInt("helper") > 0 ? true : false
                );
                World.addAccount(account);
                World.ReassignAccountToChar(account);
            }
            RS.getStatement().close();
            RS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return account;
    }

    public static Player LOAD_PLAYER(int guid) {

        Player player = null;

        try {
            String query = "SELECT * FROM personnages WHERE guid = ? AND deleted = 0;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, guid);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                if (RS.getInt("server") != GameServer.id)
                    continue;
                TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
                stats.put(Constant.STATS_ADD_VITA, RS.getInt("vitalite"));
                stats.put(Constant.STATS_ADD_FORC, RS.getInt("force"));
                stats.put(Constant.STATS_ADD_SAGE, RS.getInt("sagesse"));
                stats.put(Constant.STATS_ADD_INTE, RS.getInt("intelligence"));
                stats.put(Constant.STATS_ADD_CHAN, RS.getInt("chance"));
                stats.put(Constant.STATS_ADD_AGIL, RS.getInt("agilite"));

                int accID = RS.getInt("account");

                Player p = World.getPersos().get(guid);
                if (p != null) {
                    if (p.getFight() != null)
                        return null;
                }

                player = new Player(
                        RS.getInt("guid"),
                        RS.getString("name"),
                        RS.getInt("sexe"),
                        RS.getInt("class"),
                        RS.getInt("color1"),
                        RS.getInt("color2"),
                        RS.getInt("color3"),
                        RS.getLong("kamas"),
                        RS.getInt("spellboost"),
                        RS.getInt("capital"),
                        RS.getInt("energy"),
                        RS.getInt("level"),
                        RS.getLong("xp"),
                        RS.getInt("size"),
                        RS.getInt("gfx"),
                        RS.getByte("alignement"),
                        accID,
                        stats,
                        RS.getByte("seeFriend"),
                        RS.getByte("seeAlign"),
                        RS.getByte("seeSeller"),
                        RS.getString("canaux"),
                        RS.getShort("map"),
                        RS.getInt("cell"),
                        RS.getString("objets"),
                        RS.getString("storeObjets"),
                        RS.getInt("pdvper"),
                        RS.getString("spells"),
                        RS.getString("savepos"),
                        RS.getString("jobs"),
                        RS.getInt("mountxpgive"),
                        RS.getInt("mount"),
                        RS.getInt("honor"),
                        RS.getInt("deshonor"),
                        RS.getInt("alvl"),
                        RS.getString("zaaps"),
                        RS.getInt("title"),
                        RS.getInt("wife"),
                        RS.getInt("teamID"),
                        RS.getInt("server"),
                        RS.getInt("prestige"),
                        RS.getString("folowers"),
                        RS.getInt("currentFolower"),
                        RS.getInt("winKolizeum"),
                        RS.getInt("loseKolizeum"),
                        RS.getInt("winArena"),
                        RS.getInt("loseArena"),
                        RS.getInt("canExp"),
                        RS.getInt("pvpMod"),
                        RS.getString("candy_used"),
                        RS.getString("quest"),
                        RS.getInt("sFuerza"),
                        RS.getInt("sInteligencia"),
                        RS.getInt("sAgilidad"),
                        RS.getInt("sSuerte"),
                        RS.getInt("sVitalidad"),
                        RS.getInt("sSabiduria"),
                        RS.getInt("ornement"),
                        RS.getString("checkpoints"),
                        RS.getInt("restriction") > 0 ? true : false
                );
                //Vérifications pré-connexion
                player.VerifAndChangeItemPlace();
                // Chargement de la progression des quêtes du joueur
                SQLManager.LOAD_PLAYER_QUESTS(player);
                World.addPersonnage(player);
                int guildId = isPersoInGuild(RS.getInt("guid"));
                if (guildId >= 0) {
                    player.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
                }
                if (World.getCompte(accID) != null)
                    World.getCompte(accID).addPerso(player);
            }
            RS.getStatement().close();
            RS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return player;
    }

    public static Player LOAD_PLAYER_BY_NAME(String name) {

        Player player = null;

        try {
            String query = "SELECT * FROM personnages WHERE name = ? AND deleted = 0;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, name);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                if (RS.getInt("server") != GameServer.id)
                    continue;
                TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
                stats.put(Constant.STATS_ADD_VITA, RS.getInt("vitalite"));
                stats.put(Constant.STATS_ADD_FORC, RS.getInt("force"));
                stats.put(Constant.STATS_ADD_SAGE, RS.getInt("sagesse"));
                stats.put(Constant.STATS_ADD_INTE, RS.getInt("intelligence"));
                stats.put(Constant.STATS_ADD_CHAN, RS.getInt("chance"));
                stats.put(Constant.STATS_ADD_AGIL, RS.getInt("agilite"));

                int accID = RS.getInt("account");

                Player p = World.getPersos().get(name);
                if (p != null) {
                    if (p.getFight() != null)
                        return null;
                }
                player = new Player(
                        RS.getInt("guid"),
                        RS.getString("name"),
                        RS.getInt("sexe"),
                        RS.getInt("class"),
                        RS.getInt("color1"),
                        RS.getInt("color2"),
                        RS.getInt("color3"),
                        RS.getLong("kamas"),
                        RS.getInt("spellboost"),
                        RS.getInt("capital"),
                        RS.getInt("energy"),
                        RS.getInt("level"),
                        RS.getLong("xp"),
                        RS.getInt("size"),
                        RS.getInt("gfx"),
                        RS.getByte("alignement"),
                        accID,
                        stats,
                        RS.getByte("seeFriend"),
                        RS.getByte("seeAlign"),
                        RS.getByte("seeSeller"),
                        RS.getString("canaux"),
                        RS.getShort("map"),
                        RS.getInt("cell"),
                        RS.getString("objets"),
                        RS.getString("storeObjets"),
                        RS.getInt("pdvper"),
                        RS.getString("spells"),
                        RS.getString("savepos"),
                        RS.getString("jobs"),
                        RS.getInt("mountxpgive"),
                        RS.getInt("mount"),
                        RS.getInt("honor"),
                        RS.getInt("deshonor"),
                        RS.getInt("alvl"),
                        RS.getString("zaaps"),
                        RS.getInt("title"),
                        RS.getInt("wife"),
                        RS.getInt("teamID"),
                        RS.getInt("server"),
                        RS.getInt("prestige"),
                        RS.getString("folowers"),
                        RS.getInt("currentFolower"),
                        RS.getInt("winKolizeum"),
                        RS.getInt("loseKolizeum"),
                        RS.getInt("winArena"),
                        RS.getInt("loseArena"),
                        RS.getInt("canExp"),
                        RS.getInt("pvpMod"),
                        RS.getString("candy_used"),
                        RS.getString("quest"),
                        RS.getInt("sFuerza"),
                        RS.getInt("sInteligencia"),
                        RS.getInt("sAgilidad"),
                        RS.getInt("sSuerte"),
                        RS.getInt("sVitalidad"),
                        RS.getInt("sSabiduria"),
                        RS.getInt("ornement"),
                        RS.getString("checkpoints"),
                        RS.getInt("restriction") > 0 ? true : false
                );
                //Vérifications pré-connexion
                player.VerifAndChangeItemPlace();
                // Chargement de la progression des quêtes du joueur
                SQLManager.LOAD_PLAYER_QUESTS(player);
                World.addPersonnage(player);
                int guildId = isPersoInGuild(RS.getInt("guid"));
                if (guildId >= 0) {
                    player.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
                }
                if (World.getCompte(accID) != null)
                    World.getCompte(accID).addPerso(player);
            }
            RS.getStatement().close();
            RS.close();
        } catch (Exception e) {
            Logs.addToDebug(e.toString());
        }

        return player;
    }

    public static void UPDATE_LASTCONNECTION_INFO(Account compte) {
        String baseQuery = "UPDATE accounts SET " +
                "`lastIP` = ?," +
                "`lastConnectionDate` = ?" +
                " WHERE `guid` = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(true));

            p.setString(1, compte.getCurIp());
            p.setString(2, compte.getLastConnectionDate());
            p.setInt(3, compte.getGuid());
            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Query: " + baseQuery);
            e.printStackTrace();
        }
    }

    public static void UPDATE_ACCOUNT_VIP(Account compte) {
        String baseQuery = "UPDATE accounts SET " +
                "`vip` = ?" +
                " WHERE `guid` = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(true));

            p.setInt(1, compte.getVip());
            p.setInt(2, compte.getGuid());

            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Query: " + baseQuery);
            e.printStackTrace();
        }
    }

    public static void UPDATE_MOUNT_INFOS(Mount DD) {
        String baseQuery = "UPDATE mounts_data SET " +
                "`name` = ?," +
                "`xp` = ?," +
                "`level` = ?," +
                "`endurance` = ?," +
                "`amour` = ?," +
                "`maturite` = ?," +
                "`serenite` = ?," +
                "`reproductions` = ?," +
                "`fatigue` = ?," +
                "`energie` = ?," +
                "`ancetres` = ?," +
                "`items` = ?," +
                "`ability` = ?" +
                " WHERE `id` = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, DD.get_nom());
            p.setLong(2, 1262750);
            p.setInt(3, DD.get_level());
            p.setInt(4, DD.get_endurance());
            p.setInt(5, DD.get_amour());
            p.setInt(6, DD.get_maturite());
            p.setInt(7, DD.get_serenite());
            p.setInt(8, DD.get_reprod());
            p.setInt(9, DD.get_fatigue());
            p.setInt(10, DD.get_energie());
            p.setString(11, DD.get_ancetres());
            p.setString(12, DD.parseObjDB());
            p.setString(13, DD.get_ability());
            p.setInt(14, DD.get_id());

            p.execute();
            closePreparedStatement(p);

        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Query: " + baseQuery);
            e.printStackTrace();
        }
    }

    public static void SAVE_MOUNTPARK(MountPark MP) {
        String baseQuery = "REPLACE INTO `mountpark_data`( `mapid` , `cellid`, `size` , `owner` , `guild` , `price` , `data` )" +
                " VALUES (?,?,?,?,?,?,?);";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, MP.get_map().get_id());
            p.setInt(2, MP.get_cellid());
            p.setInt(3, MP.get_size());
            p.setInt(4, MP.get_owner());
            p.setInt(5, (MP.get_guild() == null ? -1 : MP.get_guild().get_id()));
            p.setInt(6, MP.get_price());
            p.setString(7, MP.parseDBData());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void UPDATE_MOUNTPARK(MountPark MP) {
        String baseQuery = "UPDATE `mountpark_data` SET " +
                "`data` = ?" +
                " WHERE mapid = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, MP.parseDBData());
            p.setShort(2, MP.get_map().get_id());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static boolean SAVE_TRIGGER(int mapID1, int cellID1, int action, int event, String args, String cond) {
        String baseQuery = "REPLACE INTO `scripted_cells`" +
                " VALUES (?,?,?,?,?,?);";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, mapID1);
            p.setInt(2, cellID1);
            p.setInt(3, action);
            p.setInt(4, event);
            p.setString(5, args);
            p.setString(6, cond);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean REMOVE_TRIGGER(int mapID, int cellID) {
        String baseQuery = "DELETE FROM `scripted_cells` WHERE " +
                "`MapID` = ? AND " +
                "`CellID` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, mapID);
            p.setInt(2, cellID);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean SAVE_MAP_DATA(Maps map) {
        String baseQuery = "UPDATE `maps` SET " +
                "`places` = ?, " +
                "`numgroup` = ? " +
                "WHERE id = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, map.get_placesStr());
            p.setInt(2, map.getMaxGroupNumb());
            p.setInt(3, map.get_id());

            p.executeUpdate();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean DELETE_NPC_ON_MAP(int m, int c) {
        String baseQuery = "DELETE FROM npcs WHERE mapid = ? AND cellid = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, m);
            p.setInt(2, c);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean DELETE_PERCO(int id) {
        String baseQuery = "DELETE FROM percepteurs WHERE guid = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean ADD_NPC_ON_MAP(int m, int id, int c, int o) {

        String baseQuery = "INSERT INTO `npcs`" +
                " VALUES (?,?,?,?,'0','');";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, m);
            p.setInt(2, id);
            p.setInt(3, c);
            p.setInt(4, o);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean ADD_PERCO_ON_MAP(int guid, int mapid, int guildID, int cellid, int o, short N1, short N2) {
        String baseQuery = "INSERT INTO `percepteurs`" +
                " VALUES (?,?,?,?,?,?,?,?,?,?);";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, guid);
            p.setInt(2, mapid);
            p.setInt(3, cellid);
            p.setInt(4, o);
            p.setInt(5, guildID);
            p.setShort(6, N1);
            p.setShort(7, N2);
            p.setString(8, "");
            p.setLong(9, 0);
            p.setLong(10, 0);
            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static void UPDATE_PERCO(Collector P) {
        String baseQuery = "UPDATE `percepteurs` SET " +
                "`objets` = ?," +
                "`kamas` = ?," +
                "`xp` = ?" +
                " WHERE guid = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, P.parseItemPercepteur());
            p.setLong(2, P.getKamas());
            p.setLong(3, P.getXp());
            p.setInt(4, P.getGuid());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static boolean ADD_ENDFIGHTACTION(int mapID, int type, int Aid, String args, String cond) {
        if (!DEL_ENDFIGHTACTION(mapID, type, Aid)) return false;
        String baseQuery = "INSERT INTO `endfight_action` " +
                "VALUES (?,?,?,?,?);";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, mapID);
            p.setInt(2, type);
            p.setInt(3, Aid);
            p.setString(4, args);
            p.setString(5, cond);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean DEL_ENDFIGHTACTION(int mapID, int type, int aid) {
        String baseQuery = "DELETE FROM `endfight_action` " +
                "WHERE map = ? AND " +
                "fighttype = ? AND " +
                "action = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, mapID);
            p.setInt(2, type);
            p.setInt(3, aid);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
            return false;
        }
    }

    public static void SAVE_NEWGUILD(Guild g) {
        String baseQuery = "INSERT INTO `guilds` " +
                "VALUES (?,?,?,1,0,0,0,?,?);";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, g.get_id());
            p.setString(2, g.get_name());
            p.setString(3, g.get_emblem());
            p.setString(4, "462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|");
            p.setString(5, "176;100|158;1000|124;100|");

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void DEL_GUILD(int id) {
        String baseQuery = "DELETE FROM `guilds` " +
                "WHERE `id` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void DEL_ALL_GUILDMEMBER(int guildid) {
        String baseQuery = "DELETE FROM `guild_members` " +
                "WHERE `guild` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, guildid);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void DEL_GUILDMEMBER(int id) {
        String baseQuery = "DELETE FROM `guild_members` " +
                "WHERE `guid` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void UPDATE_GUILD(Guild g) {
        String baseQuery = "UPDATE `guilds` SET " +
                "`lvl` = ?," +
                "`xp` = ?," +
                "`capital` = ?," +
                "`nbrmax` = ?," +
                "`sorts` = ?," +
                "`stats` = ?," +
                "`emblem` = ?," +
                "`name` = ?" +
                " WHERE id = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, g.get_lvl());
            p.setLong(2, g.get_xp());
            p.setInt(3, g.get_Capital());
            p.setInt(4, g.get_nbrPerco());
            p.setString(5, g.compileSpell());
            p.setString(6, g.compileStats());
            p.setString(7, g.get_emblem());
            p.setString(8, g.get_name());
            p.setInt(9, g.get_id());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void UPDATE_GUILDMEMBER(GuildMember gm) {
        String baseQuery = "REPLACE INTO `guild_members` " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?);";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, gm.getGuid());
            p.setInt(2, gm.getGuild().get_id());
            p.setString(3, gm.getName());
            p.setInt(4, gm.getLvl());
            p.setInt(5, gm.getGfx());
            p.setInt(6, gm.getRank());
            p.setLong(7, gm.getXpGave());
            p.setInt(8, gm.getPXpGive());
            p.setInt(9, gm.getRights());
            p.setInt(10, gm.getAlign());
            p.setString(11, gm.getLastCo());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static int isPersoInGuild(int guid) {
        int guildId = -1;

        try {
            String sql = "SELECT guild FROM `guild_members` WHERE guid = ?;";
            java.sql.PreparedStatement ps = newTransact(sql, Connection(false));
            ps.setInt(1, guid);
            ResultSet GuildQuery = ps.executeQuery();

            boolean found = GuildQuery.first();

            if (found)
                guildId = GuildQuery.getInt("guild");

            closeResultSet(GuildQuery);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        if (guildId >= 0) {
            if (!World.guildExist(guildId)) {
                guildId = -1;
                DEL_GUILDMEMBER(guid);
            }
        }

        return guildId;
    }

    public static int[] isPersoInGuild(String name) {
        int guildId = -1;
        int guid = -1;
        try {
            String query = "SELECT guild,guid FROM `guild_members` WHERE name=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, name);
            ResultSet GuildQuery = ps.executeQuery();
            boolean found = GuildQuery.first();

            if (found) {
                guildId = GuildQuery.getInt("guild");
                guid = GuildQuery.getInt("guid");
            }

            closeResultSet(GuildQuery);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        int[] toReturn = {guid, guildId};
        return toReturn;
    }

    public static boolean ADD_REPONSEACTION(int repID, int type, String args) {
        String baseQuery = "DELETE FROM `npc_reponses_actions` " +
                "WHERE `ID` = ? AND " +
                "`type` = ?;";
        PreparedStatement p;
        try {
            p = newTransact(baseQuery, Connection(false));
            p.setInt(1, repID);
            p.setInt(2, type);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        baseQuery = "INSERT INTO `npc_reponses_actions` " +
                "VALUES (?,?,?);";
        try {
            p = newTransact(baseQuery, Connection(false));
            p.setInt(1, repID);
            p.setInt(2, type);
            p.setString(3, args);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean UPDATE_INITQUESTION(int id, int q) {
        String baseQuery = "UPDATE `npc_template` SET " +
                "`initQuestion` = ? " +
                "WHERE `id` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, q);
            p.setInt(2, id);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static boolean UPDATE_NPCREPONSES(int id, String reps) {
        String baseQuery = "UPDATE `npc_questions` SET " +
                "`responses` = ? " +
                "WHERE `ID` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, reps);
            p.setInt(2, id);

            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static void LOG_OUT(int accID, int logged) {
        PreparedStatement p;
        String query = "UPDATE `accounts` SET logged=? WHERE `guid`=?;";
        try {
            p = newTransact(query, Connection(true));
            p.setInt(1, logged);
            p.setInt(2, accID);

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void SETONLINE(int accID) {
        if (GameServer.id == 7) { // Serveur officiel
            String query = "UPDATE `accounts` SET logged='1' WHERE `guid`=" + accID
                    + ";";
            try {
                PreparedStatement p = newTransact(query, Connection(true));
                p.execute();
            } catch (SQLException e) {
                GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
                GameServer.addToLog("Game: Query: " + query);
            }
        }
    }

    public static void SETOFFLINE(int accID) {
        if (GameServer.id == 7) { // Serveur officiel
            String query = "UPDATE `accounts` SET logged='0' WHERE `guid`=" + accID
                    + ";";
            try {
                PreparedStatement p = newTransact(query, Connection(true));
                p.execute();
            } catch (SQLException e) {
                GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
                GameServer.addToLog("Game: Query: " + query);
            }
        }
    }

    public static void LOGGED_ZERO() {
        if (GameServer.id == 7) {
            PreparedStatement p;
            String query = "UPDATE `accounts` SET logged=0;";
            try {
                p = newTransact(query, Connection(true));

                p.execute();
                closePreparedStatement(p);

                //Console.print("\r-logged reset", Color.GREEN);
            } catch (SQLException e) {
                GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
                GameServer.addToLog("Game: Query: " + query);
            }
        }

    }

    public static void TIMER(boolean start) {
        if (start) {
            timerCommit = new Timer();
            timerCommit.schedule(new TimerTask() {

                public void run() {
                    if (!needCommit) return;

                    commitTransacts();
                    //needCommit = false;

                }
            }, Config.CONFIG_DB_COMMIT, Config.CONFIG_DB_COMMIT);
        } else
            timerCommit.cancel();
    }

    public static void HOUSE_BUY(Player P, Houses h) {

        PreparedStatement p;
        String query = "UPDATE `houses` SET `sale`='0', `owner_id`=?, `guild_id`='0', `access`='0', `key`='-', `guild_rights`='0' WHERE `id`=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, P.getAccID());
            p.setInt(2, h.get_id());

            p.execute();
            closePreparedStatement(p);

            h.set_sale(0);
            h.set_owner_id(P.getAccID());
            h.set_guild_id(0);
            h.set_access(0);
            h.set_key("-");
            h.set_guild_rights(0);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }

        ArrayList<Trunk> trunks = Trunk.getTrunksByHouse(h);
        for (Trunk trunk : trunks) {
            trunk.set_owner_id(P.getAccID());
            trunk.set_key("-");
        }

        query = "UPDATE `coffres` SET `owner_id`=?, `key`='-' WHERE `id_house`=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, P.getAccID());
            p.setInt(2, h.get_id());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void HOUSE_SELL(Houses h, int price) {
        h.set_sale(price);

        PreparedStatement p;
        String query = "UPDATE `houses` SET `sale`=? WHERE `id`=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, price);
            p.setInt(2, h.get_id());

            p.execute();
            closePreparedStatement(p);

        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void HOUSE_CODE(Player P, Houses h, String packet) {
        PreparedStatement p;
        String query = "UPDATE `houses` SET `key`=? WHERE `id`=? AND owner_id=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setString(1, packet);
            p.setInt(2, h.get_id());
            p.setInt(3, P.getAccID());

            p.execute();
            closePreparedStatement(p);

            h.set_key(packet);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void HOUSE_GUILD(Houses h, int GuildID, int GuildRights) {
        PreparedStatement p;
        String query = "UPDATE `houses` SET `guild_id`=?, `guild_rights`=? WHERE `id`=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, GuildID);
            p.setInt(2, GuildRights);
            p.setInt(3, h.get_id());

            p.execute();
            closePreparedStatement(p);

            h.set_guild_id(GuildID);
            h.set_guild_rights(GuildRights);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void HOUSE_GUILD_REMOVE(int GuildID) {
        PreparedStatement p;
        String query = "UPDATE `houses` SET `guild_rights`='0', `guild_id`='0' WHERE `guild_id`=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, GuildID);

            p.execute();
            closePreparedStatement(p);

        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void UPDATE_HOUSE(Houses h) {
        String baseQuery = "UPDATE `houses` SET " +
                "`owner_id` = ?," +
                "`sale` = ?," +
                "`guild_id` = ?," +
                "`access` = ?," +
                "`key` = ?," +
                "`guild_rights` = ?" +
                " WHERE id = ?;";

        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, h.get_owner_id());
            p.setInt(2, h.get_sale());
            p.setInt(3, h.get_guild_id());
            p.setInt(4, h.get_access());
            p.setString(5, h.get_key());
            p.setInt(6, h.get_guild_rights());
            p.setInt(7, h.get_id());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static int GetNewIDPercepteur() {
        int i = -50;//Pour éviter les conflits avec touts autre NPC
        try {
            String query = "SELECT `guid` FROM `percepteurs` ORDER BY `guid` ASC LIMIT 0 , 1;";

            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                i = RS.getInt("guid") - 1;
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return i;
    }

    public static void LOAD_CHECKPOINT() {

        try {
            String query = "SELECT * FROM checkpoints ORDER BY id ASC;";

            ResultSet RS = executeQuery(query, false);
            boolean first = true;
            int donjonID = 0;
            Checkpoint last = null;
            synchronized (World.checkpoints) {
                World.checkpoints.clear();
                while (RS.next()) {
                    int d = RS.getInt("donjonID");
                    if (d != donjonID) { // Nouveau donjon
                        first = true;
                        donjonID = d;
                        last = null;
                    }
                    short mapID = (short) RS.getInt("mapID");
                    Checkpoint current = new Checkpoint(mapID, RS.getInt("cellID"), d);
                    if (first) {
                        first = false;
                    } else {
                        current.setPrev(last);
                        last.setNext(current);
                    }
                    World.checkpoints.put(mapID, current);
                    last = current;
                }
            }

            closeResultSet(RS);
        } catch (Exception e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void SAVE_PLAYER_CHECKPOINTS(Player P) {
        PreparedStatement p;
        String query = "UPDATE personnages SET checkpoints = ? WHERE guid = ?;";
        try {
            p = newTransact(query, Connection(false));
            String save = "";
            for (Checkpoint cp : P.checkpoints.values()) {
                save += cp.getMapID() + "|";
            }
            p.setString(1, save);
            p.setInt(2, P.getGuid());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void SAVE_PLAYER_RESTRICTION(Player P) {
        PreparedStatement p;
        String query = "UPDATE personnages SET restriction = ? WHERE guid = ?;";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, P.isRestricted ? 1 : 0);
            p.setInt(2, P.getGuid());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static int LOAD_ZAAPIS() {
        int i = 0;
        String Bonta = "";
        String Brak = "";
        String Neutre = "";
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT mapid, align from zaapi;", false);
            while (RS.next()) {
                if (RS.getInt("align") == Constant.ALIGNEMENT_BONTARIEN) {
                    Bonta += RS.getString("mapid");
                    if (!RS.isLast()) Bonta += ",";
                } else if (RS.getInt("align") == Constant.ALIGNEMENT_BRAKMARIEN) {
                    Brak += RS.getString("mapid");
                    if (!RS.isLast()) Brak += ",";
                } else {
                    Neutre += RS.getString("mapid");
                    if (!RS.isLast()) Neutre += ",";
                }
                i++;
                //Console.print("\r-zaapis loaded : " + i, Color.GREEN);
            }
            //if(i == 0) //Console.print("\r-zaapis loaded : " + i, Color.GREEN);
            Constant.ZAAPI.put(Constant.ALIGNEMENT_BONTARIEN, Bonta);
            Constant.ZAAPI.put(Constant.ALIGNEMENT_BRAKMARIEN, Brak);
            Constant.ZAAPI.put(Constant.ALIGNEMENT_NEUTRE, Neutre);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return i;
    }

    public static int LOAD_ZAAPS() {
        int i = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT mapID, cellID from zaaps;", false);
            while (RS.next()) {
                if (World.getCarte((short) RS.getInt("mapID")) != null)
                    Constant.ZAAPS.put(RS.getInt("mapID"), RS.getInt("cellID"));
                i++;
                //Console.print("\r-zaaps loaded : " + i, Color.GREEN);
            }
            //if(i == 0) //Console.print("\r-zaaps loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return i;
    }

    public static HashMap<Integer, Rune> LOAD_RUNES() {
        HashMap<Integer, Rune> runes = new HashMap<Integer, Rune>();
        try (ResultSet RS = SQLManager.executeQuery("SELECT * FROM runes;", false)) {
            while (RS.next()) {
                int id = RS.getInt("idRune");
                runes.put(id, new Rune(id, RS.getInt("poids"), RS.getInt("puissance"), RS.getInt("idEffet")));
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return runes;
    }

    public static int getNextObjetID() {
        try {
            ResultSet RS = executeQuery("SELECT MAX(guid) AS max FROM items;", false);

            int guid = 0;
            boolean found = RS.first();

            if (found)
                guid = RS.getInt("max");

            closeResultSet(RS);
            return guid;
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            Reboot.reboot();
        }
        return 0;
    }

    public static void LOAD_BANIP() {
        try {
            ResultSet RS = executeQuery("SELECT ip, time from banip;", true);
            while (RS.next()) {
                World.addBanip(RS.getString("ip"), RS.getLong("time"));
            }
            //if(i == 0) //Console.print("\r-banips loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean ADD_BANIP(String ip, long time) {
        String baseQuery = "INSERT INTO `banip`" +
                " VALUES (?,?);";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(true));
            p.setString(1, ip);
            p.setLong(2, time);
            p.execute();
            closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
        return false;
    }

    public static void REMOVE_BANIP(String ip) {
        String baseQuery = "DELETE FROM `banip` WHERE ip LIKE ?";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(true));
            p.setString(1, ip);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + baseQuery);
        }
    }

    public static void LOAD_HDVS() {
        try {
            ResultSet RS = executeQuery("SELECT * FROM `hdvs` ORDER BY id ASC", false);
            while (RS.next()) {
                World.addHdv(new AuctionHouse(
                        RS.getInt("map"),
                        RS.getFloat("sellTaxe"),
                        RS.getShort("sellTime"),
                        RS.getShort("accountItem"),
                        RS.getShort("lvlMax"),
                        RS.getString("categories")));
            }
            //if(i == 0) //Console.print("\r-hdv loaded : " + i, Color.GREEN);

            RS = executeQuery("SELECT id MAX FROM `hdvs`", false);
            RS.first();
            World.setNextHdvID(RS.getInt("MAX"));

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * public static void LOAD_HDVS_ITEMS()
     * {
     * try
     * {
     * ResultSet RS = executeQuery("SELECT i.*"+
     * " FROM `items` AS i,`hdvs_items` AS h"+
     * " WHERE i.guid = h.itemID",true);
     * <p/>
     * //Load items
     * while(RS.next())
     * {
     * int guid 	= RS.getInt("guid");
     * int tempID 	= RS.getInt("template");
     * int qua 	= RS.getInt("qua");
     * int pos		= RS.getInt("pos");
     * String stats= RS.getString("stats");
     * World.addObjet
     * (
     * World.newObjet
     * (
     * guid,
     * tempID,
     * qua,
     * pos,
     * stats
     * ),
     * false
     * );
     * }
     * //Load HDV entry
     * RS = executeQuery("SELECT * FROM `hdvs_items`",false);
     * while(RS.next())
     * {
     * AuctionHouse tempHdv = World.getHdv(RS.getInt("map"));
     * if(tempHdv == null)continue;
     * <p/>
     * <p/>
     * tempHdv.addEntry(new AuctionHouse.HdvEntry(
     * RS.getInt("price"),
     * RS.getByte("count"),
     * RS.getInt("ownerGuid"),
     * World.getObjet(RS.getInt("itemID"))));
     * }
     * //if(i == 0) //Console.print("\r-hdv entry loaded : " + i, Color.GREEN);
     * closeResultSet(RS);
     * }catch(SQLException e)
     * {
     * GameServer.addToLog("Game: SQL ERROR: "+e.getMessage());
     * e.printStackTrace();
     * }
     * }
     **/
    @Deprecated
    public static void SAVE_HDVS_ITEMS(ArrayList<HdvEntry> liste) {
        PreparedStatement queries = null;
        try {
            String emptyQuery = "TRUNCATE TABLE `hdvs_items`";
            PreparedStatement emptyTable = newTransact(emptyQuery, Connection(false));
            emptyTable.execute();
            closePreparedStatement(emptyTable);

            String baseQuery = "INSERT INTO `hdvs_items` " +
                    "(`map`,`ownerGuid`,`price`,`count`,`itemID`) " +
                    "VALUES(?,?,?,?,?);";
            queries = newTransact(baseQuery, Connection(false));
            for (HdvEntry curEntry : liste) {

                if (curEntry.getOwner() == -1) continue;
                queries.setInt(1, curEntry.getHdvID());
                queries.setInt(2, curEntry.getOwner());
                queries.setInt(3, curEntry.getPrice());
                queries.setInt(4, curEntry.getAmount(false));
                queries.setInt(5, curEntry.getObjet().getGuid());

                queries.execute();
            }
            closePreparedStatement(queries);
            SAVE_HDV_AVGPRICE();
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void SAVE_HDV_ITEM(HdvEntry objet, boolean conserver) {
        PreparedStatement queries = null;
        try {
            if (objet.getOwner() != -1) {
                if (conserver) {
                    String baseQuery = "INSERT INTO `hdvs_items` " +
                            "(`map`,`ownerGuid`,`price`,`count`,`itemID`) " +
                            "VALUES(?,?,?,?,?);";
                    queries = newTransact(baseQuery, Connection(false));
                    queries.setInt(1, objet.getHdvID());
                    queries.setInt(2, objet.getOwner());
                    queries.setInt(3, objet.getPrice());
                    queries.setInt(4, objet.getAmount(false));
                    queries.setInt(5, objet.getObjet().getGuid());

                    queries.execute();
                    closePreparedStatement(queries);
                } else {
                    String baseQuery = "DELETE FROM `hdvs_items` WHERE `itemID`=?";
                    queries = newTransact(baseQuery, Connection(false));
                    queries.setInt(1, objet.getObjet().getGuid());
                    queries.execute();
                    closePreparedStatement(queries);
                }
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            if (conserver) {
                GameServer.addToHdvLog("Erreur lors de la sauvegarde de l'item " + objet.getObjet().getGuid());
            } else {
                GameServer.addToHdvLog("** Erreur lors de la supression de l'item " + objet.getObjet().getGuid() + " ***");
            }
        }
    }

    public static void SAVE_HDV_AVGPRICE() {
        String baseQuery = "UPDATE `item_template`" +
                " SET sold = ?,avgPrice = ?" +
                " WHERE id = ?;";

        PreparedStatement queries = null;

        try {
            queries = newTransact(baseQuery, Connection(false));

            for (ObjTemplate curTemp : World.getObjTemplates()) {
                if (curTemp.getSold() == 0)
                    continue;

                queries.setLong(1, curTemp.getSold());
                queries.setInt(2, curTemp.getAvgPrice());
                queries.setInt(3, curTemp.getID());
                queries.executeUpdate();
            }
            closePreparedStatement(queries);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_ANIMATIONS() {
        try {
            ResultSet RS = executeQuery("SELECT * from animations;", false);
            while (RS.next()) {
                World.addAnimation(new Hustle(
                        RS.getInt("guid"),
                        RS.getInt("id"),
                        RS.getString("nom"),
                        RS.getInt("area"),
                        RS.getInt("action"),
                        RS.getInt("size")
                ));
            }
            //if(i == 0) //Console.print("\r-animations loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_RAPIDSTUFFS() {
        try {
            ResultSet RS = executeQuery("SELECT * from rapidstuff;", false);
            while (RS.next()) {
                RapidStuff.rapidStuffs.put(RS.getInt("id"), new RapidStuff(RS.getInt("id"), RS.getString("name"), RS.getString("items"), RS.getInt("owner")));
            }
            //if(i == 0) //Console.print("\r-rapidstuffs loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int LOAD_TRUNK() {
        int nbr = 0;
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from coffres;", false);
            while (RS.next()) {
                World.addTrunk(
                        new Trunk(
                                RS.getInt("id"),
                                RS.getInt("id_house"),
                                RS.getShort("mapid"),
                                RS.getInt("cellid"),
                                RS.getString("object"),
                                RS.getInt("kamas"),
                                RS.getString("key"),
                                RS.getInt("owner_id")
                        ));
            }
            //if(i == 0) //Console.print("\r-trunks loaded : " + i, Color.GREEN);
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            nbr = 0;
        }
        return nbr;
    }

    public static void TRUNK_CODE(Player P, Trunk t, String packet) {
        PreparedStatement p;
        String query = "UPDATE `coffres` SET `key`=? WHERE `id`=? AND owner_id=?;";
        try {
            p = newTransact(query, Connection(false));
            p.setString(1, packet);
            p.setInt(2, t.get_id());
            p.setInt(3, P.getAccID());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void UPDATE_TRUNK(Trunk t) {
        PreparedStatement p;
        String query = "UPDATE `coffres` SET `kamas`=?, `object`=? WHERE `id`=?";

        try {
            p = newTransact(query, Connection(false));
            p.setLong(1, t.get_kamas());
            p.setString(2, t.parseTrunkObjetsToDB());
            p.setInt(3, t.get_id());

            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            GameServer.addToLog("Game: Query: " + query);
        }
    }

    public static void ACTUALIZAR_REGALO(Account cuenta) {
        String baseQuery = "UPDATE accounts SET `cadeau` = 0 WHERE `guid` = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(true));
            p.setInt(1, cuenta.getGuid());
            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Query: " + baseQuery);
            e.printStackTrace();
        }
    }

    public static boolean ACCOUNT_IS_VIP(Account compte) {
        try {
            java.sql.PreparedStatement p = Connection(true).prepareStatement("SELECT * FROM accounts WHERE guid = ?");
            p.setInt(1, compte.getGuid());
            ResultSet RS = p.executeQuery();

            while (RS.next()) {
                return (RS.getInt("vip") == 1);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    public static void LOAD_PERSO_PACKS(Player p) {
        PreparedStatement pe;
        try {
            String query = "SELECT * FROM pack_actions WHERE personnage=? AND done=0";
            java.sql.PreparedStatement ps = newTransact(query, Connection(true));
            ps.setInt(1, p.getGuid());
            ResultSet RS = ps.executeQuery();

            //Load items
            while (RS.next()) {
                int id = RS.getInt("id");
                int pack = RS.getInt("pack");
                String params = RS.getString("params");
                if (ShopPack.executePack(p, id, pack, params)) {
                    try {
                        String sql = "UPDATE pack_actions SET done=1 WHERE id=?;";
                        pe = newTransact(sql, Connection(false));
                        ps.setInt(1, id);
                        pe.execute();
                        Console.println("CommandePack " + id + " livrée avec succès.");
                    } catch (SQLException e) {
                        GameServer.addToLog("SQL ERROR: " + e.getMessage());
                        Console.println("Error Delete From: " + e.getMessage(), Color.RED);
                        e.printStackTrace();
                    }
                }
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void CHANGER_SEX_CLASSE(Player perso) {
        PreparedStatement p;
        String query = "UPDATE `personnages` SET `sexe`=?, `class`= ?, `spells`= ? WHERE `guid`= ?";
        try {
            p = newTransact(query, Connection(false));
            p.setInt(1, perso.get_sexe());
            p.setInt(2, perso.get_classe());
            p.setString(3, perso.parseSpellToDB());
            p.setInt(4, perso.getGuid());
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            System.out.println("Game: SQL ERROR: " + e.getMessage());
            System.out.println("Game: Query: " + query);
        }
    }

    public static void SAVE_PERSONNAGE_COLORS(Player _perso) {
        String baseQuery = "UPDATE `personnages` SET " +
                "`name`= ?, " +
                "`color1`= ?, " +
                "`color2`= ?, " +
                "`color3`= ?" +
                " WHERE `personnages`.`guid` = ? LIMIT 1 ;";

        PreparedStatement p = null;

        try {
            p = newTransact(baseQuery, Connection(false));

            p.setString(1, _perso.getName());
            p.setInt(2, _perso.get_color1());
            p.setInt(3, _perso.get_color2());
            p.setInt(4, _perso.get_color3());
            p.setInt(5, _perso.getGuid());

            p.executeUpdate();

            GameServer.addToLog("Personnage " + _perso.getName() + " sauvegarde");
        } catch (Exception e) {
            Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
            Console.println("Requete: " + baseQuery);
            Console.println("Le personnage n'a pas ete sauvegarde");
            Reboot.reboot();
        }
        ;
        closePreparedStatement(p);
    }

    public static byte TotalMPGuild(int getId) {
        byte i = 0;
        try {
            String query = "SELECT *FROM mountpark_data WHERE guild=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, getId);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                i = (byte) (i + 1);
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return i;
    }

    public static void LOAD_COMMANDS() {
        try {
            String query = "SELECT * from commands;";
            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                String name = null;
                String description = null;

                if (RS.getString("name").contains("-")) {
                    description = RS.getString("name").split("-")[1];
                    name = RS.getString("name").split("-")[0];
                } else {
                    name = RS.getString("name");
                }
                CommandManager command = new CommandManager(RS.getInt("id"), name, description, RS.getString("functions"), RS.getString("conditions"), RS.getInt("count"));
                CommandManager.addCommand(command);
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e);
            e.printStackTrace();
        }
    }

    //TODO:Baskwo
    public static void LOAD_NPC_EXCHANGE() {
        try {
            ResultSet RS = SQLManager.executeQuery("SELECT * from npc_exchange;", false);
            while (RS.next()) {
                NpcTemplate npc = World.getNPCTemplate(RS.getInt("npcId"));
                ArrayList<NPC_Exchange> all = new ArrayList<NPC_Exchange>();
                String required_bdd = RS.getString("required");
                String gift_bdd = RS.getString("gift");
                int nbr_required = (required_bdd.contains("|") ? required_bdd.split("\\|").length : (required_bdd.length() > 0 ? 1 : 0));
                int nbr_gift = (gift_bdd.contains("|") ? gift_bdd.split("\\|").length : (gift_bdd.length() > 0 ? 1 : 0));
                if (nbr_required > nbr_gift) {
                    nbr_required = nbr_gift;
                } else if (nbr_gift > nbr_required) {
                    nbr_gift = nbr_required;
                }
                for (int a = 0; a < nbr_required; a++) {
                    String infos_required = required_bdd.split("\\|")[a];
                    Map<Integer, Couple<Integer, Integer>> required = new TreeMap<Integer, Couple<Integer, Integer>>();
                    for (String i : infos_required.split(";")) {
                        String[] infos = i.split(",");
                        int o = Integer.parseInt(infos[0]);
                        int oQ = Integer.parseInt(infos[1]);
                        required.put(o, new Couple<Integer, Integer>(o, oQ));
                    }
                    String infos_gift = gift_bdd.split("\\|")[a];
                    List<Couple<Integer, Integer>> gift = new ArrayList<Couple<Integer, Integer>>();
                    for (String i : infos_gift.split(";")) {
                        String[] infos = i.split(",");
                        int o = Integer.parseInt(infos[0]);
                        int oQ = Integer.parseInt(infos[1]);
                        gift.add(new Couple<Integer, Integer>(o, oQ));
                    }
                    NPC_Exchange exchange = new NPC_Exchange();
                    exchange.setRequired(required);
                    exchange.setGift(gift);
                    all.add(exchange);
                }
                npc.setExchange(all);
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Item verifStats(int templateID, String verif, Player p) //@Flow
    {
        Item objetStats = null;
        try {
            String query = "SELECT guid FROM items WHERE template = ? AND stats = ? AND server = ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, templateID);
            ps.setString(2, verif);
            ps.setInt(3, GameServer.id);
            ResultSet RS = ps.executeQuery();
            Map<Integer, Item> objetsJoueurs = p.getItems();
            while (RS.next()) {
                int idObjet = RS.getInt("guid");
                if (objetsJoueurs.containsKey(idObjet)) {
                    objetStats = objetsJoueurs.get(idObjet);
                    break;
                }
            }
            closeResultSet(RS);
        } catch (SQLException e) {
        }
        return objetStats;
    }

    public static Tickets SelectTicket() {
        Tickets ticket = new Tickets(0, "a", "b", "c", 1);
        try {
            String query = "SELECT * from ticketigs WHERE  valid = '0' LIMIT 0,1 ;";
            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                ticket = new Tickets(
                        RS.getInt("id"),
                        RS.getString("message"),
                        RS.getString("joueur"),
                        RS.getString("maitrejeu"),
                        RS.getInt("valid")
                );
            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
        return ticket;
    }


    public static int Verifticket(String pseudo) {

        int verif = 0;

        try {
            ResultSet RS;
            String query = "SELECT * from ticketigs WHERE `joueur` =? AND valid = '0';";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, pseudo);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                verif++;
            closeResultSet(rs);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }

        return verif;
    }

    public static void addticket(String pseudo, String message) {
        String baseQuery = "INSERT INTO `ticketigs` (`message`,`joueur`,`maitrejeu`,`valid`) VALUES(?,?,?,?);";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, message);
            p.setString(2, pseudo);
            p.setString(3, "null");
            p.setInt(4, 0);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("Game: SQL ERROR: ")).append(e.getMessage()).toString());
            GameServer.addToLog((new StringBuilder("Game: Query: ")).append(baseQuery).toString());
        }
    }

    public static void updateticketencour(String pseudoj, String pseudom) {
        try {
            String baseQuery = "UPDATE ticketigs SET `maitrejeu` = ? ,`valid` = 1 WHERE `valid` = 0 AND `joueur` = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, pseudom);
            p.setString(2, pseudoj);
            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
    }

    public static void deleteticket(String pseudoj) {
        try {
            String baseQuery = "DELETE FROM ticketigs WHERE `valid` = 0 AND `joueur` = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, pseudoj);
            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
    }

    public static void updateticketfini(String pseudom) {
        try {
            String baseQuery = "UPDATE ticketigs SET `valid` = 2 WHERE `valid` = 1 AND `maitrejeu` = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, pseudom);
            p.executeUpdate();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
    }

    public static String listeticket() {
        String ticket = "";
        ticket = "liste des tickets non traité : \n";
        try {
            ResultSet RS;
            for (RS = executeQuery("SELECT * from ticketigs WHERE valid = '0';", false); RS.next(); )
                ticket = (new StringBuilder(String.valueOf(ticket))).append(RS.getString("joueur")).append(" : ").append(RS.getString("message")).append("\n").toString();
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
        ticket = (new StringBuilder(String.valueOf(ticket))).append("liste des tickets en cour de traitements : \n").toString();
        try {
            ResultSet RS;
            for (RS = executeQuery("SELECT * from ticketigs WHERE valid = '1';", false); RS.next(); )
                ticket += "traité par " + RS.getString("maitrejeu") + "| joueur : " + RS.getString("joueur") + " : " + RS.getString("message") + "\n";
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("SQL ERROR: ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
        return ticket;
    }


    public static void LOAD_QUEST_STEPS() {
        try {
            ResultSet RST = SQLManager.executeQuery("SELECT * FROM `quest_steps`;", false);
            while (RST.next()) {
                Quest_Step QE = new Quest_Step(RST.getInt("id"), RST.getInt("type"), RST.getInt("objectif"), RST.getString("item"), RST.getInt("npc"), RST.getString("monster"), RST.getString("conditions"), RST.getInt("validationType"));
                Quest_Step.setQuestEtape(QE);
            }
            closeResultSet(RST);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_QUEST_OBJECTIVES() {
        try {
            ResultSet loc1 = SQLManager.executeQuery("SELECT * FROM `quest_objectives`;", false);

            while (loc1.next()) {
                Quest_Objective qObjectif = new Quest_Objective(loc1.getInt("id"), loc1.getInt("xp"), loc1.getInt("kamas"), loc1.getString("item"), loc1.getString("action"));
                Quest_Objective.setQuest_Objectif(qObjectif);
            }

            closeResultSet(loc1);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static int GetItemTemplateByID(int guid) {
        int TemplateId = 0;
        try {
            String query = "SELECT * FROM items WHERE guid=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, guid);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                TemplateId = RS.getInt("template");
            }
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("Erreur SQL : ")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
        return TemplateId;
    }

    public static String GetItemNameByID(int id) {
        String name = "";
        try {
            String query = "SELECT * FROM item_template WHERE id=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, id);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                name = RS.getString("name");
            }
        } catch (SQLException e) {
            GameServer.addToLog((new StringBuilder("Tu as fais du con :")).append(e.getMessage()).toString());
            e.printStackTrace();
        }
        return name;
    }


    public static int Guild_GetIdByName(String name) {
        int id = 0;
        try {
            String query = "SELECT * FROM guilds WHERE name=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, name);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                id = RS.getInt("id");
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return id;
    }

    /*public static int Guild_GetId(int id) {
        int ids = 0;
        try {
            String query = "SELECT * FROM guerre_victims WHERE guildID='" + id
                    + "';";

            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                ids = RS.getInt("guildID");
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return ids;
    }*/

    public static boolean Guild_VerifyExist(String name) {
        int id = 0;
        boolean exist = false;
        try {
            String query = "SELECT * FROM guilds WHERE name=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, name);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                id = RS.getInt("id");
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        if (id == 0) {
            exist = false;
        } else {
            exist = true;
        }
        return exist;
    }

   /* public static boolean Guild_VerifyInGuerre(int id) {
        int ids = 0;
        boolean exist = false;
        try {
            String query = "SELECT * FROM guerre_victims WHERE guildID='" + id
                    + "';";

            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                ids = RS.getInt("id");
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        if (ids == 0) {
            exist = false;
        } else {
            exist = true;
        }
        return exist;
    }*/

    public static boolean Guild_VerifyGuerre(String name) {
        boolean exist = false;
        try {
            String query = "SELECT * FROM guildesnoguerre WHERE name=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, name);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                exist = true;
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return exist;
    }

    public static void Guild_InsertNoGuerre(String name) {
        try {
            String baseQuery = "INSERT INTO guildesnoguerre(`name`) VALUES(?);";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, name);
            p.executeUpdate();
            closePreparedStatement(p);
            GameServer.addToLog("Guilde no guerre inserted for : " + name);
        } catch (SQLException e) {
            e.printStackTrace();
            GameServer.addToLog("Erreur à la créaton des stats : " + e.getMessage());
        }
    }

    public static void Guild_CreateGuerre(int id, String target) {
        try {
            String baseQuery = "INSERT INTO guerre_victims(`guildID`, `guildTarget`) VALUES(?, ?);";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);
            p.setString(2, target);
            p.executeUpdate();
            closePreparedStatement(p);
            GameServer.addToLog("Guilde created for the target : " + target);
        } catch (SQLException e) {
            e.printStackTrace();
            GameServer.addToLog("Erreur à la créaton des stats : " + e.getMessage());
        }
    }


    /*public static int[] Guild_CountVictims(int id) {
        int[] victims = new int[2];
        try {
            String query = "SELECT * FROM guerre_victims WHERE guildID='" + id + "';";
            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                victims[0] = RS.getInt("intvictims");
                victims[1] = RS.getInt("intciblewin");
            }
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }


        return victims;
    }

    public static void Guild_UpdateVictims(int id) {
        String baseQuery = "UPDATE `guerre_victims` SET `intvictims` = `intvictims` + ? WHERE guildID = ?";

        PreparedStatement p = null;

        try {
            p = newTransact(baseQuery, Connection(false));

            p.setInt(1, 1);
            p.setInt(2, id);
            p.executeUpdate();

            GameServer.addToLog("+1 victime pour :" + id);
        } catch (Exception e) {
        }
        ;
        closePreparedStatement(p);
    }*/

   /* public static void Guild_UpdateCibles(int id) {
        String baseQuery = "UPDATE `guerre_victims` SET `intciblewin` = `intciblewin` + ? WHERE guildID = ?";

        PreparedStatement p = null;

        try {
            p = newTransact(baseQuery, Connection(false));

            p.setInt(1, 1);
            p.setInt(2, id);
            p.executeUpdate();

            GameServer.addToLog("+1 victime pour :" + id);
        } catch (Exception e) {
        }
        ;
        closePreparedStatement(p);
    }*/

    /*public static void Guild_CreateTimer(String name) {
        try {
            String baseQuery = "INSERT INTO guerre_timer(`name`) VALUES(?);";
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, name);
            p.executeUpdate();
            closePreparedStatement(p);
            GameServer.addToLog("Guilde no guerre inserted for : " + name);
        } catch (SQLException e) {
            e.printStackTrace();
            GameServer.addToLog("Erreur à la créaton des stats : " + e.getMessage());
        }
    }*/

   /* public static void Guild_DeleteTimer(String name) {
        String baseQuery = "DELETE FROM guerre_timer WHERE name = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, name);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
        }
    }*/

    /*public static boolean Guild_SelectTimer(String name) {
        boolean exist = false;
        try {
            String query = "SELECT * FROM guerre_timer WHERE name='" + name
                    + "';";

            ResultSet RS = executeQuery(query, false);
            while (RS.next()) {
                exist = true;
            }

            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return exist;
    }*/

    /*public static void Guild_Delete(String name) {
        String baseQuery = "DELETE FROM guildesnoguerre WHERE name = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setString(1, name);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
        }
    }*/

    /*public static void Guild_CleanVictims(int id) {
        String baseQuery = "DELETE FROM guerre_victims WHERE guildID = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(false));
            p.setInt(1, id);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
        }
    }*/

    /*public static String getStatsTemplate2(int templateID) {
        String itemm = "";
        try {
            String query = "SELECT statsTemplate FROM item_template WHERE id='" + templateID + "' AND server='" + GameServer.id + "';";
            ResultSet RS = executeQuery(query, false);

            while (RS.next()) {
                itemm = RS.getString("statsTemplate");

            }
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return itemm;
    }*/

    public static boolean VERIFIER_TITRE_UTILISE(String titre) {
        boolean toReturn = false;
        try {
            ResultSet RS = executeQuery("SELECT * FROM `titres`", true);
            while (RS.next() && !toReturn) {
                if (RS.getString("titre").equalsIgnoreCase(titre)) toReturn = true;
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
            toReturn = true;
        }
        return toReturn;
    }

    public static int VERIFIER_TITRE_UTILISE_ID(String titre) {
        int toReturn = -1;
        try {
            ResultSet RS = executeQuery("SELECT * FROM `titres`", true);
            while (RS.next() && toReturn == -1) {
                if (RS.getString("titre").equalsIgnoreCase(titre)) {
                    toReturn = RS.getInt("id");
                }
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return toReturn;
    }

    public static int OBTENIR_ID_TITRE_VALIDATION() {
        try {
            int lastIndex = 0;
            ResultSet RS = executeQuery("SELECT * FROM `titres_attente`", true);
            while (RS.next()) {
                if (RS.getInt("id_validation") > lastIndex) lastIndex = RS.getInt("id_validation");
            }
            return lastIndex + 1;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public static int OBTENIR_ID_NOUVEAU_TITRE() {
        try {
            int lastIndex = 0;
            ResultSet RS = executeQuery("SELECT * FROM `titres`", true);
            while (RS.next()) {
                if (RS.getInt("id") > lastIndex) lastIndex = RS.getInt("id");
            }
            return lastIndex + 1;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public static byte AJOUTER_TITRE_EN_ATTENTE(String titre, String couleur, int idJoueur, String nomJoueur) {
        byte toReturn = 0;
        if (!VERIFIER_TITRE_UTILISE(titre)) {
            int idValidation = OBTENIR_ID_TITRE_VALIDATION();
            if (idValidation != -1) {
                PreparedStatement queries = null;
                try {
                    String baseQuery = "INSERT INTO `titres_attente` " +
                            "(`titre`,`couleur`,`id_joueur`,`id_validation`) " +
                            "VALUES(?,?,?,?);";
                    queries = newTransact(baseQuery, Connection(true));
                    queries.setString(1, titre);
                    queries.setString(2, couleur);
                    queries.setInt(3, idJoueur);
                    queries.setInt(4, idValidation);

                    queries.execute();
                    closePreparedStatement(queries);
                    toReturn = 1;
                    SocketManager.GAME_SEND_TITRE_VALIDATION_REQUEST(idValidation, titre, nomJoueur);
                } catch (SQLException e) {
                    GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                toReturn = 4;
            }
        } else {
            toReturn = 2;
        }
        return toReturn;
    }

    public static byte VALIDER_TITRE(int idValidation) {
        byte status = -1;
        Player perso = null;
        int points = 0;
        String titre = "";
        try {
            String couleur = "";
            int idJoueur = -1;
            ResultSet RS = executeQuery("SELECT * FROM `titres_attente`", true);
            while (RS.next()) {
                if (RS.getInt("id_validation") == idValidation) {
                    titre = RS.getString("titre");
                    couleur = RS.getString("couleur");
                    idJoueur = RS.getInt("id_joueur");
                    break;
                }
            }
            perso = World.getPlayer(idJoueur);
            points = Util.loadPointsByAccount(perso.getAccount());
            if (points < 60 && perso.getAccount().getGmLevel() < 1) {
                perso.sendText("Un membre du Staff a tenté de valider l'un de vos titres en attente, cependant vous n'aviez pas assez de points."
                        + " Votre titre restera en attente pendant encore une semaine, il sera supprimé par la suite");
                return 2;
            }
            int tempTitre = VERIFIER_TITRE_UTILISE_ID(titre);
            if (tempTitre != -1) {
                status = 1;
                if (perso.isOnline()) {
                    perso.set_title(tempTitre);
                }
                SAUVEGARDER_TITRE(tempTitre, perso.getGuid());
            } else {
                int nouveauTitreID = AJOUTER_NOUVEAU_TITRE(titre, couleur);
                if (nouveauTitreID != -1) {
                    status = 1;
                    if (perso.isOnline()) {
                        perso.set_title(nouveauTitreID);
                    }
                    SAUVEGARDER_TITRE(nouveauTitreID, perso.getGuid());
                }
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        if (status == 1) {
            int nouvelleSomme = points;
            if (perso.getAccount().getGmLevel() < 1) {
                nouvelleSomme -= 60;
                Util.updatePointsByAccount(perso.getAccount(), nouvelleSomme, "Validation du titre : " + titre);
            }
            SUPPRIMER_TITRE_EN_ATTENTE(idValidation);
            SAVE_PERSONNAGE(perso, false);
            if (perso.isOnline()) {
                perso.send("000C" + nouvelleSomme);
                perso.sendText("Votre titre a été validé !");
                try {
                    Thread.sleep(750);
                } catch (Exception e) {
                }
                if (perso.getFight() == null) {
                    SocketManager.GAME_SEND_ALTER_GM_PACKET(perso.getMap(), perso); // Actualiser le titre sans changement de map
                }
            }
        }
        return status;
    }

    public static int AJOUTER_NOUVEAU_TITRE(String titre, String couleur) {
        int toReturn = -1;
        int idTitreDisponible = OBTENIR_ID_NOUVEAU_TITRE();
        if (idTitreDisponible != 1) {
            PreparedStatement queries = null;
            try {

                String baseQuery = "INSERT INTO `titres` " +
                        "(`id`,`titre`,`couleur`) " +
                        "VALUES(?,?,?);";
                queries = newTransact(baseQuery, Connection(true));
                queries.setInt(1, idTitreDisponible);
                queries.setString(2, titre);
                queries.setString(3, couleur);

                queries.execute();
                closePreparedStatement(queries);
                toReturn = idTitreDisponible;
            } catch (SQLException e) {
                GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return toReturn;
    }

    public static void SUPPRIMER_TITRE_EN_ATTENTE(int idValidation) {
        String baseQuery = "DELETE FROM titres_attente WHERE id_validation = ?;";
        try {
            PreparedStatement p = newTransact(baseQuery, Connection(true));
            p.setInt(1, idValidation);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void REJETER_TITRE_EN_ATTENTE(int idValidation, String motif) {
        Player perso = null;
        try {
            ResultSet RS = executeQuery("SELECT * FROM `titres_attente`", true);
            while (RS.next()) {
                if (RS.getInt("id_validation") == idValidation) {
                    perso = World.getPlayer(RS.getInt("id_joueur"));
                    break;
                }
            }
            SUPPRIMER_TITRE_EN_ATTENTE(idValidation);
            perso.sendText("Votre titre a été refusé pour le motif suivant :" + motif);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void CHANGER_COULEUR_TITRE(int id, String couleur) {
        try {
            String baseQuery = "UPDATE `titres` SET `couleur` = ? WHERE id = ?;";
            PreparedStatement p = newTransact(baseQuery, Connection(true));
            p.setString(1, couleur);
            p.setInt(2, id);
            p.execute();
            closePreparedStatement(p);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean SAUVEGARDER_TITRE(int idTitre, int idJoueur) {
        boolean toReturn = false;
        PreparedStatement queries = null;
        try {

            String baseQuery = "INSERT INTO `titres_save` " +
                    "(`idJoueur`,`idTitre`) " +
                    "VALUES(?,?);";
            queries = newTransact(baseQuery, Connection(true));
            queries.setInt(1, idJoueur);
            queries.setInt(2, idTitre);

            queries.execute();
            closePreparedStatement(queries);
            toReturn = true;
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return toReturn;
    }

    public static Map<String, Integer> OBTENIR_LISTE_TITRE_SAUVEGARDE(int idJoueur) {
        //List<String> listeDesTitres = new ArrayList<String>();
        List<Integer> tempList = new ArrayList<Integer>();
        Map<String, Integer> titres = new LinkedHashMap<String, Integer>();
        try {
            String query = "SELECT * FROM `titres_save` WHERE idJoueur= ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(true));
            ps.setInt(1, idJoueur);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                tempList.add(RS.getInt("idTitre"));
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        if (!tempList.isEmpty()) {
            for (int i = 0; i < tempList.size(); i++) {
                try {
                    ResultSet RS = executeQuery("SELECT * FROM `titres`", true);
                    while (RS.next()) {
                        if (RS.getInt("id") == tempList.get(i)) {
                            titres.put(RS.getString("titre"), tempList.get(i));
                        }
                    }
                } catch (SQLException e) {
                    GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return titres;
    }

    public static boolean VERIFIER_SI_TITRE_SAUVEGARDE(int idJoueur, int idTitre) {
        boolean toReturn = false;
        try {
            String query = "SELECT * FROM `titres_save` WHERE idJoueur=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(true));
            ps.setInt(1, idJoueur);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                if (RS.getInt("idTitre") == idTitre) {
                    toReturn = true;
                }
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return toReturn;
    }

    public static void LOAD_HDVS_ITEMS() {
        try {
            long time1 = System.currentTimeMillis();    //TIME
            ResultSet RS = executeQuery("SELECT i.*" +
                    " FROM `items` AS i,`hdvs_items` AS h" +
                    " WHERE i.guid = h.itemID", false);

            //Load items
            while (RS.next()) {
                try {
                    int guid = RS.getInt("guid");
                    int tempID = RS.getInt("template");
                    int qua = RS.getInt("qua");
                    int pos = RS.getInt("pos");
                    String stats = RS.getString("stats");
                    World.addObjet
                            (
                                    World.newObjet
                                            (
                                                    guid,
                                                    tempID,
                                                    qua,
                                                    pos,
                                                    stats
                                            ),
                                    false
                            );
                } catch (Exception e) {
                }
            }

            //Load HDV entry
            RS = executeQuery("SELECT * FROM `hdvs_items`", false);
            while (RS.next()) {
                try {
                    AuctionHouse tempHdv = World.getHdv(RS.getInt("map"));
                    if (tempHdv == null) continue;


                    tempHdv.addEntry(new HdvEntry(
                            RS.getInt("price"),
                            RS.getByte("count"),
                            RS.getInt("ownerGuid"),
                            World.getObjet(RS.getInt("itemID"))));
                } catch (Exception e) {
                }
            }
            System.out.println(System.currentTimeMillis() - time1 + "ms pour loader les HDVS items");    //TIME
            closeResultSet(RS);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static ArrayList<Integer> LOAD_ORNEMENTS(int guid) {
        ArrayList<Integer> toReturn = new ArrayList<Integer>();
        try {
            String query = "SELECT * FROM `ornements` WHERE idJoueur=?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, guid);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                toReturn.add(RS.getInt("id"));
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return toReturn;
    }

    public static void SAVE_NEW_ORNEMENTS(int guid, int id) {
        PreparedStatement queries = null;
        try {

            String baseQuery = "INSERT INTO `ornements` " +
                    "(`id`,`idJoueur`) " +
                    "VALUES(?,?);";
            queries = newTransact(baseQuery, Connection(false));
            queries.setInt(1, id);
            queries.setInt(2, guid);

            queries.execute();
            closePreparedStatement(queries);
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void LOAD_ORNEMENTS_PRICE() {
        try {
            ResultSet RS = executeQuery("SELECT * FROM `ornements_prix`", false);
            while (RS.next()) {
                World.ajouterPrixOrnements(RS.getInt("id"), RS.getInt("prix"));
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static ArrayList<Integer> GET_NPCS_WITH_QUESTION_ID(int questionId) {
        ArrayList<Integer> toReturn = new ArrayList<Integer>();
        try {
            String query = "SELECT * FROM `npc_template` WHERE initQuestion= ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, questionId);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                toReturn.add(RS.getInt("id"));
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return toReturn;
    }

    public static boolean IS_A_GOOD_ANSWER_FOR_QUESTION(int questionId, int responseId) {
        try {
            String query = "SELECT * FROM `npc_questions` WHERE ID= ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, questionId);
            ResultSet RS = ps.executeQuery();
            String[] arr = null;
            while (RS.next()) {
                arr = RS.getString("responses").split(";");
            }
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    if (Integer.parseInt(arr[i]) == responseId) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static HashMap<Integer, Integer> OBTENIR_ITEMS_REQUIS_PAR_PNJ(int idPnj) {
        HashMap<Integer, Integer> toReturn = new HashMap<Integer, Integer>();
        try {
            String query = "SELECT * FROM `objets_requis_pnj` WHERE idPnj= ?;";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, idPnj);
            ResultSet RS = ps.executeQuery();
            while (RS.next()) {
                toReturn.put(RS.getInt("itemTemplate"), RS.getInt("qta"));
            }
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return toReturn;
    }

    public static void INSERT_PB_TRANSACT(String nomPerso, String description, int points) {
        try {
            String query = "INSERT INTO `achatsPB` (`date`, `nomPerso`, `description`, `points`) VALUES (now(), ?, ?, ?);";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setString(1, nomPerso);
            ps.setString(2, description);
            ps.setInt(3, points);
            ps.execute();
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param receiver > 0 = id d'un joueur, -1 = HDV, -2 = Banque, -3 = Sol, -4 = Item supprimé, -5 = commande gm
     */
    public static void INSERT_ITEM_HISTORY(int sender, int receiver, String ip1, String ip2, Item i, int qua) {
        try {
            String query = "INSERT INTO `historique_items` (`date`, `item`, `template`, `sender`, `receiver`, `ip1`, `ip2`, `qua`) VALUES (now(), ?, ?, ?, ?, ?, ?, ?);";
            java.sql.PreparedStatement ps = newTransact(query, Connection(false));
            ps.setInt(1, i.getGuid());
            ps.setInt(2, i.getTemplate(true).getID());
            ps.setInt(3, sender);
            ps.setInt(4, receiver);
            ps.setString(5, ip1);
            ps.setString(6, ip2);
            ps.setInt(7, qua);
            ps.execute();
        } catch (SQLException e) {
            GameServer.addToLog("Game: SQL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}