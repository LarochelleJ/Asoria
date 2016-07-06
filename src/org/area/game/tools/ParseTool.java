package org.area.game.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.area.arena.Arena;
import org.area.arena.Team;
import org.area.client.Player;
import org.area.client.Player.Stats;
import org.area.command.player.FmCac;
import org.area.common.Constant;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.event.Event;
import org.area.kernel.Config;
//import org.area.game.GameServer;
//import org.area.kernel.Reboot;
import org.area.kolizeum.Kolizeum;
import org.area.kolizeum.Manager;
import org.area.kolizeum.Manager.Params;
import org.area.lang.Lang;
import org.area.object.Item;
import org.area.object.Item.ObjTemplate;
import org.area.object.job.Job.StatsMetier;

/**
 * @author: Return
 **/

public class ParseTool {

    private static Map<Integer, Integer> shop = new HashMap<Integer, Integer>();

    public static synchronized void parsePacket(String packet, Player player) {
        if (player != null && packet.length() >= 3) {

            switch (packet.charAt(0)) {
                case '0': {
                    switch (packet.charAt(1)) {
                        case '0': {
                            String parse = packet.substring(3); //005|ON
                            String[] params = {};

                            if (parse.contains("|")) {
                                String[] split = parse.split("\\|");
                                parse = split[0];
                                params = split;
                            }
                            switch (packet.charAt(2)) {
                                case '0': { /** Init component **/
                                    initComponent(player, params);
                                }
                                case '1': { /** Kolizeum **/
                                    parseKolizeum(player, parse, params);
                                    break;
                                }
                                case '2': { /** Prestiges **/
                                    parsePrestige(player, parse, params);
                                    break;
                                }
                                case '3': { /** Arena **/
                                    parseArena(player, parse, params);
                                    break;
                                }
                                case '4': { /** Served **/
                                    parseServed(player, parse, params);
                                    break;
                                }
                                case '5': {  /** Events **/
                                    parseEvent(player, parse, params);
                                    break;
                                }
                                case '6': { /** Tools **/
                                    parseTools(player, parse, params);
                                }
                                case '7': { /** Stats 2.0 **/
                                    parseStats(player, parse, params);
                                    break;
                                }
                                case '9': { /** Ornements **/
                                    parseOrnements(player, parse, params);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    private static void initComponent(Player player, String[] params) {
        player.send("000C" + Util.loadPointsByAccount(player.getAccount()));
        player.send("000X" + player.getCanExp());
        player.send("000M" + player.getPvpMod());
        player.checkCandyValidation();
        player.send("000B" + player.getCandyUsed());
        player.send(ParseTool.parseShop());
        player.send("002P" + player.getPrestige());
        player.setLang(Integer.parseInt(params[1]));

    }

    private static void parseKolizeum(Player player, String packet, String[] params) {
        switch (packet) {
            case "SUBSCRIBE": /** Subscription **/
                if (player.getKolizeum() != null)
                    player.sendMess(Lang.LANG_4);
                else {
                    Kolizeum.subscribe(player, false);
                    player.sendMess(Lang.LANG_5);
                }
                break;
            case "GSUBSCRIBE": /** Group subscription **/
                if (player.getGroup() == null)
                    player.sendMess(Lang.LANG_6);
                else {
                    if (!player.getGroup().isChief(player.getGuid()))
                        player.sendMess(Lang.LANG_7);
                    else if (player.getGroup().getPlayers().size() != Params.MAX_PLAYERS.intValue()) {
                        player.sendMess(Lang.LANG_8);
                        player.sendText(Params.MAX_PLAYERS.intValue() + Lang.PLAYER[player.getLang()]);
                    } else {
                        StringBuilder errors = new StringBuilder();
                        for (Player gPlayer : player.getGroup().getPlayers()) {
                            if (gPlayer == null)
                                errors.append(Lang.LANG_9[player.getLang()]);
                            else if (gPlayer.getKolizeum() != null)
                                errors.append(gPlayer.getName() + " " + Lang.LANG_10[player.getLang()] + ".\n");
                            else if (gPlayer.getFight() != null)
                                errors.append(gPlayer.getName() + " " + Lang.LANG_11[player.getLang()] + ".\n");
                            else if (!gPlayer.isOnline())
                                errors.append(gPlayer.getName() + " " + Lang.LANG_12[player.getLang()] + ".\n");
                        }
                        if (!errors.toString().isEmpty()) {
                            player.sendText(errors.append(Lang.LANG_49[player.getLang()]).toString());
                            player.send("001CLEAR");
                        } else {
                            Kolizeum.subscribe(player, true);
                            player.sendMess(Lang.LANG_13);
                        }
                    }
                }
                break;
            case "UNSUBSCRIBE": /** Unsubscription **/
                if (player.getKolizeum() == null)
                    player.sendMess(Lang.LANG_14);
                else if (player.getFight() != null)
                    player.sendMess(Lang.LANG_15);
                else {
                    Kolizeum.unsubscribe(player);
                    player.sendMess(Lang.LANG_16);
                }
                break;
            case "READY": /** Ready to fight ? **/
                if (player.getKolizeum() == null)
                    break;
                boolean ready = params[1].equals("YES") ? true : false;
                if (!ready) {
                    if (player.getKolizeum().getTimer() != null) {
                        if (!player.getKolizeum().getTimer().cancel())
                            player.getKolizeum().getTimer().cancel();
                        player.getKolizeum().setTimer(null);

                        for (Player p : player.getKolizeum().getTeams()) {
                            p.sendMess(Lang.LANG_1);
                            p.setReady(false);
                            p.send("001D");
                        }
                        Kolizeum.unsubscribe(player);
                    }
                } else {
                    player.setReady(true);
                    Manager.refreshStateBlock(player.getKolizeum());
                    player.getKolizeum().check();
                }
                break;
            case "REFRESH": /** Refresh rank infos **/
                Manager.refreshRankInfos(player);
                break;
        }
    }

    private static void parsePrestige(Player player, String packet, String[] params) {
        switch (packet) {
            case "UP": /** Up your prestige **/
                if (player.getFight() != null) {
                    player.sendMess(Lang.LANG_50);
                }
                else if (player.getLevel() != 200) { // World.getExpLevelSize()
                    player.sendMess(Lang.LANG_39);
                }
                else if (player.getPrestige() == Config.MAX_PRESTIGES) {
                    player.sendMess(Lang.LANG_40);
                }
                else {
                    player.upPrestige();
                    player.sendMess(Lang.LANG_41, "", " " + player.getPrestige() + ".");
                    player.send("002P" + player.getPrestige());
                }
                break;
            default:
                player.sendText("Wpe...");
                break;
        }
    }

    private static void parseArena(Player player, String packet, String[] params) {
        switch (packet) {
            case "CREATE": /** Create a team **/
                String name;
                try {
                    name = params[1];
                } catch (Exception e) {
                    player.sendMess(Lang.LANG_17);
                    break;
                }
                if (player.getGroup() != null) {
                    if (player.getGroup().getPlayers().size() > 2)
                        player.sendMess(Lang.LANG_18);
                    else if (!player.getGroup().isChief(player.getGuid()))
                        player.sendMess(Lang.LANG_7);
                    else {
                        String players = "";
                        boolean first = true;
                        int classe = 0;
                        for (Player c : player.getGroup().getPlayers()) {
                            if (classe == c.get_classe()) {
                                player.sendMess(Lang.LANG_19);
                                break;
                            } else if (!Arena.isVerifiedTeam(player.get_classe(), c.get_classe())) {
                                player.sendMess(Lang.LANG_20);
                                break;
                            }
                            if (first) {
                                classe = c.get_classe();
                                players = String.valueOf(c.getGuid());
                                first = false;
                            } else {
                                players += "," + c.getGuid();
                            }
                        }
                        if (Team.addTeam(name, players, 0, 0)) {
                            for (Player c : player.getGroup().getPlayers()) {
                                c.sendMess(Lang.LANG_21, "'<b>" + Team.getTeamByID(player.getTeamID()).getName() + "</b>' ", "");
                                Team.refreshInfos(player);
                            }
                        }
                        break;
                    }
                } else {
                    player.sendMess(Lang.LANG_22);
                    break;
                }
                break;
            case "REMOVE": /** Remove the team **/
                if (player.getArena() > -1)
                    player.sendMess(Lang.LANG_23);
                else if (player.getTeamID() < 1)
                    player.sendMess(Lang.LANG_24);
                else
                    Team.removeTeam(Team.getTeamByID(player.getTeamID()), player);
                Team.refreshInfos(player);
                break;
            case "RENAME": /** Rename the Team **/
                player.sendText("Not available");
                Team.refreshInfos(player);
                break;
            case "SUBSCRIPTION": /** Subscribtion **/
                if (player.getTeamID() < 0) {
                    player.sendMess(Lang.LANG_24);
                    break;
                } else if (player.getGroup() == null) {
                    player.sendMess(Lang.LANG_25);
                    break;
                } else {
                    if (player.getGroup().getPlayers().size() > 2 || player.getGroup().getPlayers().size() < 2) {
                        player.sendMess(Lang.LANG_26);
                        break;
                    } else if (!player.getGroup().isChief(player.getGuid())) {
                        player.sendMess(Lang.LANG_7);
                        break;
                    } else {
                        for (Player c : player.getGroup().getPlayers()) {
                            try {
                                if (!Team.getPlayers(Team.getTeamByID(player.getTeamID())).contains(c)) {
                                    player.sendMess(Lang.LANG_27, c.getName() + " ", "");
                                    break;
                                } else if (!c.isOnline()) {
                                    player.sendMess(Lang.LANG_12, c.getName() + " ", "");
                                    break;
                                } else if (c.getFight() != null) {
                                    player.sendMess(Lang.LANG_11, c.getName() + " ", "");
                                    break;
                                } else if (c.getArena() != -1) {
                                    player.sendMess(Lang.LANG_28, c.getName() + " ", "");
                                    break;
                                } else {
                                    Arena.addTeam(Team.getTeamByID(player.getTeamID()));
                                    Team.refreshState(Team.getTeamByID(player.getTeamID()));
                                    break;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                break;
            case "UNSUBSCRIPTION": /** Unsubscription **/
                try {
                    if (player.getTeamID() < 0)
                        player.sendMess(Lang.LANG_24);
                    else if (player.getArena() == 1 && player.getFight() != null)
                        player.sendMess(Lang.LANG_29);
                    else
                        Arena.delTeam(Team.getTeamByID(player.getTeamID()));
                    Team.refreshState(Team.getTeamByID(player.getTeamID()));
                } catch (Exception e) {
                }
                break;
            case "INFOS": /** Information panel **/
                Team.refreshInfos(player);
                break;
        }
    }

    private static void parseServed(Player player, String packet, String[] params) {
        final String folowers = "1180,1181,1182," +
                "1001,10013,10024,1003,1260,8004,1205,9001,1211,1188,1109,1073,1566,8003,1281,1288,8014,8013,8017";
        switch (packet) {
            case "LIST": /** Infos Sprites **/
                switch (Integer.parseInt(params[1])) {
                    case 1: //Folowers
                        player.send("004L1|" + folowers + "|" + player.getFolowersString());
                        break;
                    default:
                        return;
                }
                break;
            case "BUY": /** Buy a served **/
                switch (Integer.parseInt(params[1])) {
                    case 1: //Folowers
                        try {
                            for (String s : folowers.split(","))
                                if (Integer.parseInt(params[2]) == Integer.parseInt(s)) {
                                    int points = Util.loadPointsByAccount(player.getAccount());
                                    if (points < 400)
                                        player.sendMess(Lang.LANG_42);
                                    else {
                                        int newPoints = points - 200;
                                        Util.updatePointsByAccount(player.getAccount(), newPoints);
                                        player.getFolowers().add(Integer.parseInt(params[2]));
                                        player.sendMess(Lang.LANG_43, "", " 200 " + Lang.POINTS[player.getLang()]);
                                        player.send("004L1|" + folowers + "|" + player.getFolowersString());
                                        player.send("000C" + Util.loadPointsByAccount(player.getAccount()));
                                        player.save(false);
                                    }
                                    return;
                                }
                        } catch (NumberFormatException e) {
                            player.sendText("Error, please try again");
                            return;
                        }
                        player.sendText("Wpe..");
                        break;
                    case 2: //Template Shop
                        int template = Integer.parseInt(params[2]);
                        if (!ParseTool.getShop().containsKey(template))
                            break;
                        int price = ParseTool.getShop().get((Integer) template);
                        int points = Util.loadPointsByAccount(player.getAccount());

                        if (points < price)
                            player.sendMess(Lang.LANG_44, "", " " + (price - points) + " " + Lang.POINTS[player.getLang()]);
                        else {
                            ObjTemplate objTemplate = World.getObjTemplate(template);
                            Item object = objTemplate.createNewItem(1, true, -1);
                            if (player.addObjet(object, true))
                                World.addObjet(object, true);
                            int remaining = points - price;
                            Util.updatePointsByAccount(player.getAccount(), remaining);
                            player.send("000C" + remaining);
                            player.sendMess(Lang.LANG_45, "", " " + objTemplate.getName() + ".");
                            player.sendMess(Lang.LANG_51, "", " " + price + " " + Lang.POINTS[player.getLang()] + ". " + Lang.LANG_52[player.getLang()] + " " + (points - price) + ".");
                        }
                        break;
                    default:
                        return;
                }
                break;
            case "ACTIVATE": /** Active a folower **/
                if (player.getFight() == null) {
                    for (String s : player.getFolowersString().split(","))
                        if (Integer.parseInt(params[1]) == Integer.parseInt(s)) {
                            player.setCurrentFolower(Integer.parseInt(params[1]));
                            player.refresh();
                            player.sendMess(Lang.LANG_46);
                            player.save(false);
                            return;
                        }
                    player.sendText("Wpe..");
                } else
                    player.sendMess(Lang.LANG_50);
                break;
            case "DISABLE": /** Disable a folower **/
                player.setCurrentFolower(-1);
                player.save(false);
                player.refresh();
                player.sendMess(Lang.LANG_47);
                break;
            default:
                break;
        }
    }

    private static void parseEvent(Player player, String packet, String[] params) {
        switch (packet) {
            case "REQUEST": /** Send events infos to the client **/
                Event.refreshList(player);
                break;
            case "SUB": /** Subscribe to an Event **/
                int id = Integer.parseInt(params[1]);
                Event event = Event.getEvents().get(id);
                if (event.getStatus() != Event.OPENED)
                    player.sendMess(Lang.LANG_34);
                else
                    event.addPlayer(player);
                break;
            default:
                break;
        }
    }

    private static void parseTools(Player player, String packet, String[] params) {
        System.out.println(packet);
        switch (packet) {
            case "MOD":
                Integer modEnable = 0;
                if (player.getPvpMod() == 0) {
                    modEnable = 1;
                } else {
                    modEnable = 0;
                }
                player.setPvpMod(modEnable);
                player.send("000M" + modEnable);
                player.save(true);
                break;
            case "XP":
                Integer goExp = 0;
                if (player.getCanExp() == 0) {
                    goExp = 1;
                } else {
                    goExp = 0;
                }
                player.setCanExp(goExp);
                player.send("000X" + goExp);
                player.save(true);
                break;
            case "DEBUG": /** Debug a character **/
                if (player.getKolizeum() != null) {
                    player.sendText("Impossible de quitter un combat en Kolizeum.");
                    break;
                }
                if (player.getFight() != null && player.getKolizeum() == null && player.getArena() == -1) {
                    player.getFight().leftFight(player, null, true);
                }
                // player.getMap().getRandomFreeCellID()
                player.teleport(player.getMap().get_id(), player.get_curCell().getID());
                SocketManager.GAME_SEND_GV_PACKET(player);
                player.set_duelID(-1);
                player.set_ready(false);
                try {
                    player.getFight().leftFight(player, null, true);
                } catch (Exception e) {
                }
                player.set_fight(null);
                SocketManager.GAME_SEND_GV_PACKET(player);
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                player.get_curCell().addPerso(player);

                if (player.getFight() == null && player.estBloqueCombat() && !Constant.COMBAT_BLOQUE) {
                    player.mettreCombatBloque(false);
                }
                // Partie debug prestige @Flow
                Stats baseStats = player.get_baseStats();
                int pa = player._baseStats.getEffect(111);
                int pm = player._baseStats.getEffect(128);
                int prestige = player.getPrestige();
                if (prestige >= 2 && prestige < 10 && pm == 3) {
                    pm += 1;
                }
                if (prestige >= 5 && pa == 6) {
                    pa += 1;
                }
                if (prestige >= 5 && pa == 7 && player.getLevel() >= 100) {
                    pa += 1;
                }
                if (prestige >= 9 && pm <= 4) {
                    pm = 5;
                }
                if (prestige >= 5 && !player.haveOrnement(4)) {
                    player.set_ornement(4);
                    player.save(false);
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                    player.sendText("Vous possèdez un nouvel ornement !");
                    player.addOrnement(4);
                    player.send("000A" + player.getOrnementsStringData());
                }
                if (prestige >= 10 && !player.haveOrnement(5)) {
                    player.set_ornement(5);
                    player.save(false);
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                    player.sendText("Vous possèdez un nouvel ornement !");
                    player.addOrnement(5);
                    player.send("000A" + player.getOrnementsStringData());
                }
                if (prestige == 15 && !player.haveOrnement(6)) {
                    player.set_ornement(6);
                    player.save(false);
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                    player.sendText("Vous possèdez un nouvel ornement !");
                    player.addOrnement(6);
                    player.send("000A" + player.getOrnementsStringData());
                }
                if (prestige >= 13 && player.getSortStatBySortIfHas(212124) == null) {
                    player.learnSpell(212124, 6, true, true);
                }
                baseStats.addOneStat(111, -player._baseStats.getEffect(111));
                baseStats.addOneStat(128, -player._baseStats.getEffect(128));
                player.get_baseStats().addOneStat(111, pa);
                player.get_baseStats().addOneStat(128, pm);
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                }
                player.set_lvl(player.getLevel());
                Player perso = player;
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
                SocketManager.GAME_SEND_STATS_PACKET(perso);
                break;
            case "FMCAC": /** FM the cac **/
                //FmCac.exec(player, params[1]);
                player.sendText("Veuillez utiliser la commande .fmcac. Celle-ci coûte 10 points.");
                break;
            case "RESTAT": /** Reset stats **/
                player.sendText("Veuillez utiliser la commande .restat. Celle-ci coûte 50 points.");
                /*try {
			        player.get_baseStats().addOneStat(125, -player.get_baseStats().getEffect(125));
			        player.get_baseStats().addOneStat(124, -player.get_baseStats().getEffect(124));
			        player.get_baseStats().addOneStat(118, -player.get_baseStats().getEffect(118));
			        player.get_baseStats().addOneStat(123, -player.get_baseStats().getEffect(123));
			        player.get_baseStats().addOneStat(119, -player.get_baseStats().getEffect(119));
			        player.get_baseStats().addOneStat(126, -player.get_baseStats().getEffect(126));
	        	
		        	player.addCapital((player.getLevel() - 1) * 5 - player.get_capital());
		        	
					if(player.CheckItemConditions() != 0) {
						SocketManager.GAME_SEND_Ow_PACKET(player);
						player.refreshStats();
						if(player.getGroup() != null)
							SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(player.getGroup(),player);
					}
					SocketManager.GAME_SEND_STATS_PACKET(player);
			    } catch(Exception e){GameServer.addToLog(e.getMessage());};*/
                break;
            case "JOB": /** Learn Job **/
                int jobId = Integer.parseInt(params[1]);
                int itemID;

                switch (jobId) {
                    case 62:
                        itemID = 7495;
                        break;
                    case 63:
                        itemID = 7493;
                        break;
                    case 64:
                        itemID = 7494;
                        break;
                    case 43:
                        itemID = 1520;
                        break;
                    case 44:
                        itemID = 1339;
                        break;
                    case 45:
                        itemID = 1561;
                        break;
                    case 46:
                        itemID = 1560;
                        break;
                    case 47:
                        itemID = 1562;
                        break;
                    case 48:
                        itemID = 1563;
                        break;
                    case 49:
                        itemID = 1564;
                        break;
                    case 50:
                        itemID = 1565;
                        break;
                    default:
                        player.sendText("Wpe..");
                        return;
                }
                player.learnJob(World.getMetier(jobId));
                ObjTemplate a = World.getObjTemplate(itemID);
                Item objs = a.createNewItem(1, false, -1);
                if (player.addObjet(objs, true))
                    World.addObjet(objs, true);

                StatsMetier SM = player.getMetierByID(jobId);
                SM.addXp(player, 1000000);
                ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
                list.addAll(player.getMetiers().values());
                SocketManager.GAME_SEND_JS_PACKET(player, list);
                SocketManager.GAME_SEND_JX_PACKET(player, list);
                SocketManager.GAME_SEND_JO_PACKET(player, list);
                SocketManager.GAME_SEND_STATS_PACKET(player);
                player.sendMess(Lang.LANG_48);
                player.save(true);
                break;
            default:
                break;
        }
    }

    private static void parseStats(Player player, String packet, String[] params) {
        switch (packet) {
            case "ADD": /** Add stat **/
                int stat = Integer.parseInt(params[1]), value = Integer.parseInt(params[2]);
                if (value <= player.get_capital() && value > 0)
                    player.boost(stat, value);
                break;
            case "PREVIEW": /** To do **/
                break;
        }
    }

    private static void parseOrnements(Player player, String packet, String[] params) {
        switch (packet) {
            case "BUY": /** Buy an ornement **/
                int ornement = Integer.parseInt(params[1]);
                if (player.haveOrnement(ornement)) {
                    player.sendText("Vous avez déjà cet ornement !");
                } else {
                    if (ornement > 0 && ornement <= 46) {
                        int pointsJoueur = Util.loadPointsByAccount(player.getAccount());
                        int prixOrnement = World.obtenirPrixOrnement(ornement);
                        if (prixOrnement == 0) {
                            player.sendText("Cet ornement est disponible gratuitement en jeu d'une manière méritoire.");
                        } else {
                            if (pointsJoueur >= prixOrnement) {
                                player.set_ornement(ornement);
                                player.save(false);
                                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                                player.sendText("Vous possèdez un nouvel ornement !");
                                Util.updatePointsByAccount(player.getAccount(), pointsJoueur - prixOrnement);
                                player.send("000C" + (pointsJoueur - prixOrnement));
                                player.addOrnement(ornement);
                                player.send("000A" + player.getOrnementsStringData());
                            } else {
                                player.sendText("Il vous manque " + (prixOrnement - pointsJoueur) + " points pour acheter cet ornement.");
                            }
                        }
                    }
                }
                break;
            case "ACTIVE": /** Active an ornement **/
                ornement = Integer.parseInt(params[1]);
                if (player.haveOrnement(ornement)) {
                    player.set_ornement(ornement);
                    player.save(false);
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                    player.sendText("Nouvel ornement mis en place !");
                } else {
                    player.sendText("Vous ne possèdez pas cette ornement.");
                }
                break;
            case "LIST": /** Show player's ornements **/
                player.send("000A" + player.getOrnementsStringData());
                break;
            case "PRIX": /** Send price list to client **/
                player.send("000Z" + World.obtenirListePrixData());
                break;
            case "EXCLUS": /** Send excluded ornements list **/
                player.send("000Y" + Config.ORNEMENT_EXCLUS);
                break;
            case "CURRENT": /** Send current ornement id **/
                player.send("000R" + player.get_ornement());
                break;
            case "DESACTIVE": /** Remove current ornement **/
                player.set_ornement(0);
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                player.sendText("Ornement retiré !");
                break;
            default:
                break;
        }
    }

    public static Map<Integer, Integer> getShop() {
        return shop;
    }

    public static void setShop(Map<Integer, Integer> shop) {
        ParseTool.shop = shop;
    }

    public static String shopList = "";

    public static String parseShop() {
        if (shopList.isEmpty()) {
            String string = "006L";
            ArrayList<ObjTemplate> collection = new ArrayList<ObjTemplate>();

            for (Entry<Integer, Integer> entry : ParseTool.getShop().entrySet())
                collection.add(World.getObjTemplate(entry.getKey()));

            Collections.sort(collection);

            for (ObjTemplate object : collection) {
                Item item = object.createNewItem(1, true, -1);
                int price = ParseTool.getShop().get((Integer) object.getID());
                string += object.getID() + ";;" + price + ";;" + item.parseStatsString() + "|;";
            }
            shopList = string;
        }
        return shopList;
    }
}
