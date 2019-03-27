package org.area.common;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.area.client.Account;
import org.area.client.Player;
import org.area.client.Player.Group;
import org.area.common.World.ItemSet;
import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.fight.object.Collector;
import org.area.fight.object.Monster;
import org.area.fight.object.Monster.MobGroup;
import org.area.fight.object.Prism;
import org.area.game.GameSendThread;
import org.area.game.GameServer;
import org.area.kernel.*;
import org.area.object.AuctionHouse;
import org.area.object.AuctionHouse.HdvEntry;
import org.area.object.Guild;
import org.area.object.Guild.GuildMember;
import org.area.object.Item;
import org.area.object.Item.ObjTemplate;
import org.area.object.Maps;
import org.area.object.Maps.Case;
import org.area.object.Maps.InteractiveObject;
import org.area.object.Maps.MountPark;
import org.area.object.Mount;
import org.area.object.NpcTemplate.NPC;
import org.area.object.Trunk;
import org.area.object.job.Job.StatsMetier;
import org.area.quests.Quest;

public class SocketManager {

    public static void send(Player p, String packet) {
        if (p == null || p.getAccount() == null) return;
        if (p.getAccount().getGameThread() == null) return;
        GameSendThread out = p.getAccount().getGameThread().getOut();
        if (out != null) {
            if (out.encrypt.isActive()) {
                packet = out.encrypt.prepareData(packet);
            }
            //packet = SlowBase64.encode(packet);
            //packet = CryptManager.toUtf(packet);
            out.send(packet);
        }
    }

    public static void send(GameSendThread out, String packet) {
        if (out != null) {
            if (out.encrypt.isActive()) {
                packet = out.encrypt.prepareData(packet);
            }
            //packet = CryptManager.toUtf(packet);
            //packet = SlowBase64.encode(packet);
            out.send(packet);
        }
    }

    public static void send(GameSendThread out, String packet, boolean useCrypted) {
        if (out != null) {
            packet = CryptManager.toUtf(packet);
            //packet = SlowBase64.encode(packet);
            out.send(packet);
        }
    }

    public static void MULTI_SEND_Af_PACKET(GameSendThread out, int serverID) {
        StringBuilder packet = new StringBuilder();
        packet.append("Af1|0|0|1|").append(serverID);
        send(out, packet.toString());
    }

    public static void GAME_SEND_HELLOGAME_PACKET(GameSendThread out) {
        String packet = "HG";
        send(out, packet, true);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Eq_PACKET(Player Personnage, long Prix) {
        StringBuilder Packet = new StringBuilder();
        Packet.append("Eq1|1|" + Prix);
        send(Personnage, Packet.toString());
    }

    public static void GAME_SEND_ATTRIBUTE_FAILED(GameSendThread out) {
        String packet = "ATE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ATTRIBUTE_SUCCESS(GameSendThread out) {
        String packet = "ATK";
        if (out.encrypt.isActive()){
            packet += "1" + out.encrypt.key;
        }
        send(out, packet, true);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_AV0(GameSendThread out) {
        String packet = "AV0";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_HIDE_GENERATE_NAME(GameSendThread out) {
        String packet = "APE2";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_PERSO_LIST(GameSendThread out,
                                            Map<Integer, Player> persos) {
        StringBuilder packet = new StringBuilder();
        packet.append("ALK31536000000|").append(persos.size());
        for (Entry<Integer, Player> entry : persos.entrySet()) {
            packet.append(entry.getValue().parseALK());

        }
        send(out, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());

    }

    /**
     * TODO: Packets prismes
     **/
    public static void GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(Player perso, String str) {
        String packet = "am" + str;
        send(perso, packet);

    }

    public static void GAME_SEND_aM_ALIGN_PACKET_TO_AREA(Player perso, String str) {
        String packet = "aM" + str;
        send(perso, packet);

    }

    public static void SEND_Cb_CONQUETE(Player perso, String str) {
        String packet = "Cb" + str;
        send(perso, packet);

    }

    public static void SEND_GA_Action_ALL_MAPS(Maps Carte, String gameActionID, int actionID, String s1, String s2) {
        String packet = "GA" + gameActionID + ";" + actionID + ";" + s1;
        if (!s2.equals(""))
            packet += ";" + s2;
        for (Player z : Carte.getPersos())
            send(z, packet);

    }

    public static void SEND_CIV_INFOS_CONQUETE(Player perso) {
        String packet = "CIV";
        send(perso, packet);

    }

    public static void SEND_CW_INFO_CONQUETE(Player perso, String str) {
        String packet = "CW" + str;
        send(perso, packet);

    }

    public static void SEND_CB_BONUS_CONQUETE(Player perso, String str) {
        String packet = "CB" + str;
        send(perso, packet);

    }

    public static void SEND_Wp_MENU_Prisme(Player perso) {
        String packet = "Wp" + perso.parsePrismesList();
        send(perso.getAccount().getGameThread().getOut(), packet);
    }

    public static void SEND_Ww_CLOSE_Prisme(Player out) {
        String packet = "Ww";
        send(out, packet);
    }

    public static void SEND_CP_INFO_DEFENSEURS_PRISME(Player perso, String str) {
        String packet = "CP" + str;
        send(perso, packet);
    }

    public static void SEND_Cp_INFO_ATTAQUANT_PRISME(Player perso, String str) {
        String packet = "Cp" + str;
        send(perso, packet);
    }

    public static void SEND_CA_ATTAQUE_MESSAGE_PRISME(Player perso, String str) {
        String packet = "CA" + str;
        send(perso, packet);

    }

    public static void SEND_CS_SURVIVRE_MESSAGE_PRISME(Player perso, String str) {
        String packet = "CS" + str;
        send(perso, packet);

    }

    public static void SEND_CD_MORT_MESSAGE_PRISME(Player perso, String str) {
        String packet = "CD" + str;
        send(perso, packet);

    }

    public static void SEND_CIJ_INFO_JOIN_PRISME(Player perso, String str) {
        String packet = "CIJ" + str;
        send(perso, packet);

    }

    public static void GAME_SEND_OCO_PACKET_REMOVE(Player out, Item obj) {
        String packet = "OCO" + obj.parseItem() + "*" + obj.getGuid();
        send(out, packet);
        if (Config.DEBUG)
            Logs.addToDebug((new StringBuilder("Game: Send>>")).append(packet).toString());
    }

    public static void SEND_GM_PRISME_TO_MAP(GameSendThread _out, Maps Carte) {
        String packet = Carte.getPrismeGMPacket();
        if (packet == "" || packet.isEmpty())
            return;
        send(_out, packet);
    }

    public static void GAME_SEND_PRISME_TO_MAP(Maps Carte, Prism Prisme) {
        String packet = Prisme.getGMPrisme();
        for (Player z : Carte.getPersos())
            send(z, packet);

    }

    public static void GAME_SEND_OUVERTURE_PORTE_TO_MAP(Maps carte, int cell, int action) {
        String packet = "GDF|" + cell + ";" + action;
        for (Player p : carte.getPersos()) {
            send(p, packet);
        }
    }

    public static void GAME_SEND_CELLULE_DEBLOQUEE_TO_MAP(Maps carte, List<Integer> cellules, boolean debloque) {
        List<String> packets = new ArrayList<String>();
        for (int i : cellules) {
            String packet = "GDC" + i + (debloque ? ";aaGaaaaaaa801;1 " : ";aaaaaaaaaa801;1");
            packets.add(packet);
        }
        for (Player p : carte.getPersos()) {
            for (String s : packets) {
                send(p, s);
            }
        }
    }

    public static void GAME_SEND_CELLULE_DEBLOQUEE(Player p, List<Integer> cellules, boolean debloque) {
        for (int i : cellules) {
            String packet = "GDC" + i + (debloque ? ";aaGaaaaaaa801;1 " : ";aaaaaaaaaa801;1");
            send (p, packet);
        }
    }

    public static void GAME_SEND_OUVERTURE_PORTE(Player p, int cell, int action) {
        String packet = "GDF|" + cell + ";" + action;
        send(p, packet);
    }

    public static void REALM_SEND_MESSAGE_DECO(Player P, int MSG_ID, String args) {
        String packet = "M0" + MSG_ID + "|" + args;
        send(P, packet);
    }

    /**
     * TODO: Fin packets prismes
     **/

    public static void GAME_SEND_VOTE_BLOC(Player perso, String str) {

        String packet = "BAIO" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("GAME: Send>>  " + packet);
    }

    public static void GAME_SEND_NAME_ALREADY_EXIST(GameSendThread out) {
        String packet = "AAEa";
        send(out, packet);
        if (Config.DEBUG)

            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_CREATE_PERSO_FULL(GameSendThread out) {
        String packet = "AAEf";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_CREATE_OK(GameSendThread out) {
        String packet = "AAK";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_DELETE_PERSO_FAILED(GameSendThread out) {
        String packet = "ADE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_CREATE_FAILED(GameSendThread out) {
        String packet = "AAEF";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);

    }

    public static void GAME_SEND_PERSO_SELECTION_FAILED(GameSendThread out) {
        String packet = "ASE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_STATS_PACKET(Player perso) {
        String packet = perso.getAsPacket();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Rx_PACKET(Player out) {
        String packet = "Rx" + out.getMountXpGive();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Rn_PACKET(Player out, String name) {
        String packet = "Rn" + name;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Re_PACKET(Player out, String sign, Mount DD) {
        String packet = "Re" + sign;
        if (sign.equals("+")) packet += DD.parse();

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ASK(GameSendThread out, Player perso) {
        StringBuilder packet = new StringBuilder();
        packet.append("ASK|").append(perso.getGuid()).append("|").append(perso.getName()).append("|");
        packet.append(perso.getLevel()).append("|").append(perso.get_classe()).append("|").append(perso.get_sexe());
        packet.append("|").append(perso.get_gfxID()).append("|").append((perso.get_color1() == -1 ? "-1" : Integer.toHexString(perso.get_color1())));
        packet.append("|").append((perso.get_color2() == -1 ? "-1" : Integer.toHexString(perso.get_color2()))).append("|");
        packet.append((perso.get_color3() == -1 ? "-1" : Integer.toHexString(perso.get_color3()))).append("|");
        packet.append(perso.parseItemToASK());

        send(out, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ALIGNEMENT(GameSendThread out, int alliID) {
        String packet = "ZS" + alliID;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ADD_CANAL(GameSendThread out, String chans) {
        String packet = "cC+" + chans;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ZONE_ALLIGN_STATUT(GameSendThread out) {
        String packet = "al|" + World.getSousZoneStateString();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SEESPELL_OPTION(GameSendThread out, boolean spells) {
        String packet = "SLo" + (spells ? "+" : "-");
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_RESTRICTIONS(GameSendThread out) {
        String packet = "AR6bk";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Ow_PACKET(Player perso) {
        String packet = "Ow" + perso.getPodUsed() + "|" + perso.getMaxPod();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_OT_PACKET(GameSendThread out, int id) {
        String packet = "OT";
        if (id > 0) packet += id;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SEE_FRIEND_CONNEXION(GameSendThread out, boolean see) {
        String packet = "FO" + (see ? "+" : "-");
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GAME_CREATE(GameSendThread out, String _name) {
        String packet = "GCK|1|" + _name;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SERVER_HOUR(GameSendThread out) {
        String packet = GameServer.getServerTime();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SERVER_DATE(GameSendThread out) {
        String packet = GameServer.getServerDate();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MAPDATA(GameSendThread out, int id, String date, String key) {
        String packet = "GDM|" + id + "|" + date + "|" + key;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GDK_PACKET(GameSendThread out) {
        String packet = "GDK";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_MOBS_GMS_PACKETS(GameSendThread out, Maps carte, Player player) {
        String packet = carte.getMobGroupGMsPackets(player);
        if (packet.equals("")) return;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_OBJECTS_GDS_PACKETS(GameSendThread out, Maps carte) {
        String packet = carte.getObjectsGDsPackets();
        if (packet.equals("")) return;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_NPCS_GMS_PACKETS(Player p, Maps carte) {
        String packet = carte.getNpcsGMsPackets(p);
        if (packet.equals("") && packet.length() < 4)
            return;
        send(p, packet);
        if (Config.DEBUG)
            Logs.addToDebug((new StringBuilder("Game: Send>>")).append(packet).toString());
    }

    public static void GAME_SEND_MAP_PERCO_GMS_PACKETS(GameSendThread out, Maps carte) {
        String packet = Collector.parseGM(carte);
        if (packet.length() < 5) return;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }
    /*
    public static void GAME_SEND_MAP_GMS_PACKETS(GameSendThread out, Carte carte)
	{
		String packet = carte.getGMsPackets();
		send(out,packet);
		if(Ancestra.debug)
			GameServer.addToSockLog("Game: Send>>"+packet);
	}*/

    public static void GAME_SEND_ERASE_ON_MAP_TO_MAP(Maps map, int guid) {
        String packet = "GM|-" + guid;
        if (map == null || map.getPersos() == null) return;
        for (Player z : map.getPersos()) {
            if (z.getAccount().getGameThread() == null) continue;
            send(z.getAccount().getGameThread().getOut(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map " + map.get_id() + ": Send>>" + packet);
    }

    public static void GAME_SEND_ERASE_ON_MAP_TO_FIGHT(Fight f, int guid) {
        String packet = "GM|-" + guid;
        for (int z = 0; z < f.getFighters(1).size(); z++) {
            if (f.getFighters(1).get(z).getPersonnage().getAccount().getGameThread() == null) continue;
            send(f.getFighters(1).get(z).getPersonnage(), packet);
        }
        for (int z = 0; z < f.getFighters(2).size(); z++) {
            if (f.getFighters(2).get(z).getPersonnage().getAccount().getGameThread() == null) continue;
            send(f.getFighters(2).get(z).getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fighter ID " + f.get_id() + ": Send>>" + packet);
    }

    public static void GAME_SEND_ON_FIGHTER_KICK(Fight f, int guid, int team) {
        String packet = "GM|-" + guid;
        for (Fighter F : f.getFighters(team)) {
            if (F.getPersonnage() == null || F.getPersonnage().getAccount().getGameThread() == null || F.getPersonnage().getGuid() == guid)
                continue;
            send(F.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fighter ID " + f.get_id() + ": Send>>" + packet);
    }

    public static void GAME_SEND_ALTER_FIGHTER_MOUNT(Fight fight, Fighter fighter, int guid, int team, int otherteam) {
        StringBuilder packet = new StringBuilder();
        packet.append("GM|-").append(guid).append((char) 0x00).append(fighter.getGmPacket('~'));
        for (Fighter F : fight.getFighters(team)) {
            if (F.getPersonnage() == null || F.getPersonnage().getAccount().getGameThread() == null || !F.getPersonnage().isOnline())
                continue;
            send(F.getPersonnage(), packet.toString());
        }
        if (otherteam > -1) {
            for (Fighter F : fight.getFighters(otherteam)) {
                if (F.getPersonnage() == null || F.getPersonnage().getAccount().getGameThread() == null || !F.getPersonnage().isOnline())
                    continue;
                send(F.getPersonnage(), packet.toString());
            }
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight ID " + fight.get_id() + ": Send>>" + packet);
    }

    public static void GAME_SEND_ADD_PLAYER_TO_MAP(Maps map, Player perso) {
        if (perso.get_size() != 0) {
            String packet = "GM|+" + perso.parseToGM();
            for (Player z : map.getPersos()) send(z, packet);
            if (Config.DEBUG)
                GameServer.addToSockLog("Game: Map " + map.get_id() + ": Send>>" + packet);
        }
    }

    public static void GAME_SEND_DUEL_Y_AWAY(GameSendThread out, int guid) {
        String packet = "GA;903;" + guid + ";o";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_DUEL_E_AWAY(GameSendThread out, int guid) {
        String packet = "GA;903;" + guid + ";z";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_NEW_DUEL_TO_MAP(Maps map, int guid, int guid2) {
        String packet = "GA;900;" + guid + ";" + guid2;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map " + map.get_id() + ": Send>>" + packet);
    }

    public static void GAME_SEND_CANCEL_DUEL_TO_MAP(Maps map, int guid, int guid2) {
        String packet = "GA;902;" + guid + ";" + guid2;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_START_DUEL_TO_MAP(Maps map, int guid, int guid2) {
        String packet = "GA;901;" + guid + ";" + guid2;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_FIGHT_COUNT(GameSendThread out, Maps map) {
        String packet = "fC" + map.getNbrFight();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight fight, int teams, int state, int cancelBtn, int duel, int spec, long time, int type) {
        StringBuilder packet = new StringBuilder();
        packet.append("GJK").append(state).append("|");
        packet.append(cancelBtn).append("|").append(duel).append("|");
        packet.append(spec).append("|").append(time).append("|").append(type);
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft())
                continue;
            send(f.getPersonnage(), packet.toString());
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet.toString());
    }

    public static void GAME_SEND_ACTUAL_TURN_TO_FIGHT(Fight fight) {
        String packet = "000T" + fight.tourActuel;
        for (Fighter f : fight.getAllFighters()) {
            if (f.getPersonnage() != null && f.getPersonnage().isOnline() && !f.hasLeft()) {
                send(f.getPersonnage(), packet);
            }
        }
    }

    public static void GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight fight, int teams, String places, int team) {
        String packet = "GP" + places + "|" + team;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(Maps map) {
        String packet = "fC" + map.getNbrFight();
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(Maps map, int arg1, int guid1, int guid2, int cell1, String str1, int cell2, String str2) {
        StringBuilder packet = new StringBuilder();
        packet.append("Gc+").append(guid1).append(";").append(arg1).append("|").append(guid1).append(";").append(cell1).append(";").append(str1).append("|").append(guid2).append(";").append(cell2).append(";").append(str2);
        for (Player z : map.getPersos()) send(z, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet.toString());
    }

    public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(Player p, Maps map, int arg1, int guid1, int guid2, int cell1, String str1, int cell2, String str2) {
        StringBuilder packet = new StringBuilder();
        packet.append("Gc+").append(guid1).append(";").append(arg1).append("|").append(guid1).append(";").append(cell1).append(";").append(str1).append("|").append(guid2).append(";").append(cell2).append(";").append(str2);
        send(p, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet.toString());
    }

    public static void GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(Maps map, int guid) {
        String packet = "Gc-" + guid;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(Maps map, int teamID, Fighter perso) {
        StringBuilder packet = new StringBuilder();
        if (perso != null)
            packet.append("Gt").append(teamID).append("|+").append(perso.getGUID()).append(";").append(perso.getPacketsName()).append(";").append(perso.get_lvl());
        else
            packet.append("Gt").append(teamID).append("|+").append(0).append(";").append("Event").append(";").append("Rejoignez");

        for (Player z : map.getPersos()) send(z, packet.toString());
    }

    public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(Player p, Maps map, int teamID, Fighter perso) {
        StringBuilder packet = new StringBuilder();
        packet.append("Gt").append(teamID).append("|+").append(perso.getGUID()).append(";").append(perso.getPacketsName()).append(";").append(perso.get_lvl());
        send(p, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet.toString());
    }

    public static void GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(Maps map, int teamID, Fighter perso) {
        StringBuilder packet = new StringBuilder();
        packet.append("Gt").append(teamID).append("|-").append(perso.getGUID()).append(";").append(perso.getPacketsName()).append(";").append(perso.get_lvl());
        for (Player z : map.getPersos()) send(z, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet.toString());
    }

    public static void GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(Maps map) {
        String packet = map.getMobGroupGMsPackets(null); // Un par un comme sa lors du respawn :)
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_MOBS_GM_PACKET(Maps map, MobGroup current_Mobs) {
        if (!Main.isRunning) return;
        String packet = "GM|";
        packet += current_Mobs.parseGM(null);// Un par un comme sa lors du respawn :)
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_GMS_PACKETS(Maps map, Player _perso) {
        map.sendGMsPackets(_perso);
    }

    public static void GAME_CLEAR_NPC_EXTRACLIP(Player p, int template) {
        NPC npc = p.getMap().getNpcByTemplate(template);
        if (npc != null) {
            send(p, "GX-|" + npc.get_guid());
        }
    }

    public static void GAME_SEND_MAP_GMS_PACKETS(Player _perso, String packet) {
        send(_perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ON_EQUIP_ITEM(Maps map, Player _perso) {
        String packet = _perso.parseToOa();
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_ON_EQUIP_ITEM_FIGHT(Player _perso, Fighter f, Fight F) {
        String packet = _perso.parseToOa();
        for (Fighter z : F.getFighters(f.getTeam2())) {
            if (z.getPersonnage() == null) continue;
            send(z.getPersonnage(), packet);
        }
        for (Fighter z : F.getFighters(f.getOtherTeam())) {
            if (z.getPersonnage() == null) continue;
            send(z.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(Fight fight, int teams, Maps map, int guid, int cell) {
        String packet = "GIC|" + guid + ";" + cell + ";1";
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(Maps map, char s, char option, int guid) {
        String packet = "Go" + s + option + guid;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(Fight fight, int teams, int guid, boolean b) {
        String packet = "GR" + (b ? "1" : "0") + guid;
        if (fight.get_state() != 2) return;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            if (f.hasLeft()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_GJK_PACKET(Player out, int state, int cancelBtn, int duel, int spec, long time, int unknown) {
        StringBuilder packet = new StringBuilder();
        packet.append("GJK").append(state).append("|").append(cancelBtn).append("|").append(duel).append("|").append(spec).append("|").append(time).append("|").append(unknown);
        send(out, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_FIGHT_PLACES_PACKET(GameSendThread out, String places, int team) {
        String packet = "GP" + places + "|" + team;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Im_PACKET_TO_ALL(String s) {
        String packet = "Im" + s;
        for (Player perso : World.getOnlinePlayers())
            send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Im_PACKET(Player out, String str) {
        String packet = "Im" + str;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(final Fight fight, final int teams) // @Flow pour les spells fight
    {
        String packet = "GA;0";
        for (final Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft() || f.getPersonnage() == null
                    || !f.getPersonnage().isOnline())
                continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet.toString());
    }

    public static void GAME_SEND_ILS_PACKET(Player out, int i) {
        String packet = "ILS" + i;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ILF_PACKET(Player P, int i) {
        String packet = "ILF" + i;
        send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Im_PACKET_TO_MAP(Maps map, String id) {
        String packet = "Im" + id;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_eUK_PACKET_TO_MAP(Maps map, int guid, int emote) {
        String packet = "eUK" + guid + "|" + emote;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_eUK_PACKET_TO_PLAYER(Maps map, Player p) {
        for (Player z : map.getPersos()) {
            if (z.getGuid() != p.getGuid() && z.isOnline()) {
                if (z.isSitted() && z.isOnMount()) {
                    int guid = z.getGuid();
                    int emote = z.emoteActive();
                    String packet = "eUK" + guid + "|" + emote;
                    send(p, packet);
                }
            }
        }
    }

    public static void GAME_SEND_Im_PACKET_TO_FIGHT(Fight fight, int teams, String id) {
        String packet = "Im" + id;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_MESSAGE(Player out, String mess, String color) {
        String packet = "cs<font color='#" + color + "'>" + mess + "</font>";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MESSAGE_TO_MAP(Maps map, String mess, String color) {
        String packet = "cs<font color='#" + color + "'>" + mess + "</font>";
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_GA903_ERROR_PACKET(GameSendThread out, char c, int guid) {
        String packet = "GA;903;" + guid + ";" + c;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GIC_PACKETS_TO_FIGHT(Fight fight, int teams) {
        StringBuilder packet = new StringBuilder();
        packet.append("GIC|");
        for (Fighter p : fight.getFighters(3)) {
            if (p.get_fightCell() == null) continue;
            packet.append(p.getGUID()).append(";").append(p.get_fightCell().getID()).append(";1|");
        }
        for (Fighter perso : fight.getFighters(teams)) {
            if (perso.hasLeft()) continue;
            if (perso.getPersonnage() == null || !perso.getPersonnage().isOnline()) continue;
            send(perso.getPersonnage(), packet.toString());
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet.toString());
    }

    public static void GAME_SEND_GIC_PACKET_TO_FIGHT(Fight fight, int teams, Fighter f) {
        StringBuilder packet = new StringBuilder();
        packet.append("GIC|").append(f.getGUID()).append(";").append(f.get_fightCell().getID()).append(";1|");

        for (Fighter perso : fight.getFighters(teams)) {
            if (perso.hasLeft()) continue;
            if (perso.getPersonnage() == null || !perso.getPersonnage().isOnline()) continue;
            send(perso.getPersonnage(), packet.toString());
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet.toString());
    }

    public static void GAME_SEND_GS_PACKET_TO_FIGHT(Fight fight, int teams) {
        String packet = "GS";
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            f.initBuffStats();
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GS_PACKET(Player out) {
        String packet = "GS";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GTL_PACKET_TO_FIGHT(Fight fight, int teams) {
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), fight.getGTL());
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + fight.getGTL());
    }

    public static void GAME_SEND_GTL_PACKET(Player out, Fight fight) {
        String packet = fight.getGTL();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GTM_PACKET_TO_FIGHT(Fight fight, int teams) {
        StringBuilder packet = new StringBuilder();
        packet.append("GTM");
        for (Fighter f : fight.getFighters(3)) {
            packet.append("|").append(f.getGUID()).append(";");
            if (f.isDead()) {
                packet.append("1");
                continue;
            } else
                packet.append("0;").append(f.getPDV() + ";").append(f.getPA() + ";").append(f.getPM() + ";");
            packet.append((f.isHide() ? "-1" : f.get_fightCell().getID())).append(";");//On envoie pas la cell d'un invisible :p
            packet.append(";");//??
            packet.append(f.getPDVMAX());
        }
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet.toString());
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet.toString());
    }

    public static void GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(Fight fight, int teams, int guid, int time, int guidControl) {
        String packet = "GTS" + guid + "|" + time + "|" + guidControl;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GAMETURNSTART_PACKET(Player P, int guid, int time, int guidControl) {
        String packet = "GTS" + guid + "|" + time + "|" + guidControl;
        send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GV_PACKET(Player P) {
        String packet = "GV";
        send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_PONG(GameSendThread out) {
        String packet = "pong";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_QPONG(GameSendThread out) {
        String packet = "qpong";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GAS_PACKET_TO_FIGHT(Fight fight, int teams, int guid) {
        String packet = "GAS" + guid;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight, int teams, int actionID, String s1, String s2) {
        String packet = "GA;" + actionID + ";" + s1;
        if (!s2.equals(""))
            packet += ";" + s2;
        /*if (actionID == 100) {
            try {
                Thread.sleep(75);
            } catch (Exception e) {
            }
        }*/
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight(" + fight.getFighters(teams).size() + ") : Send>>" + packet);
    }

    public static void GAME_SEND_GA_PACKET(GameSendThread out, String actionID, String s0, String s1, String s2) {
        String packet = "GA" + actionID + ";" + s0;
        if (!s1.equals(""))
            packet += ";" + s1;
        if (!s2.equals(""))
            packet += ";" + s2;

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight, int teams, int gameActionID, String s1, String s2, String s3) {
        String packet = "GA" + gameActionID + ";" + s1 + ";" + s2 + ";" + s3;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }


    public static void GAME_SEND_GAMEACTION_TO_FIGHT(Fight fight, int teams, String packet) {
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GAF_PACKET_TO_FIGHT(Fight fight, int teams, int i1, int guid) {
        String packet = "GAF" + i1 + "|" + guid;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_BN(Player out) {
        String packet = "BN";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_BN(GameSendThread out) {
        String packet = "BN";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(Fight fight, int teams, int guid) {
        String packet = "GTF" + guid;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_GTR_PACKET_TO_FIGHT(Fight fight, int teams, int guid) {
        String packet = "GTR" + guid;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_EMOTICONE_TO_MAP(Maps map, int guid, int id) {
        String packet = "cS" + guid + "|" + id;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_SPELL_UPGRADE_FAILED(GameSendThread _out) {
        String packet = "SUE";
        send(_out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SPELL_UPGRADE_SUCCED(GameSendThread _out, int spellID, int level) {
        String packet = "SUK" + spellID + "~" + level;
        send(_out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SPELL_LIST(Player perso) {
        String packet = perso.parseSpellList();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SPELL_LIST(Player perso, Monster.MobGrade mb) {
        String packet = mb.parseSpellList();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(Fight fight, int teams, int guid) {
        String packet = "GA;103;" + guid + ";" + guid;
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft() || f.getPersonnage() == null) continue;
            if (f.getPersonnage().isOnline())
                send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(final String packet, Fighter f) {
        try {
        /*final String packet = fight.GetGE(win);
        for(final Fighter f : fight.getFighters(teams)){
			try{
				Thread.sleep(750);
			}
			catch(Exception e){}
					if(!f.hasLeft() && f.getPersonnage() != null
							&& f.getPersonnage().isOnline()){*/
            if (!f.getPersonnage().noCrash) {
                send(f.getPersonnage(), packet);
            }
					/*}
		}
		try{
			Thread.sleep(1500);
		}catch (Exception e){}*/
            if (Config.DEBUG) {
                GameServer.addToSockLog("Game: Fight : Send>>" + packet + " [scheduled]");
            }
        } catch (Exception e) {
        }
    }

    public static void GAME_SEND_FIGHT_GIE_TO_FIGHT(Fight fight, int teams, int mType, int cible, int value, String mParam2, String mParam3, String mParam4, int turn, int spellID) {
        StringBuilder packet = new StringBuilder();
        packet.append("GIE").append(mType).append(";").append(cible).append(";").append(value).append(";").append(mParam2).append(";").append(mParam3).append(";").append(mParam4).append(";").append(turn).append(";").append(spellID);
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft() || f.getPersonnage() == null) continue;
            if (f.getPersonnage().isOnline())
                send(f.getPersonnage(), packet.toString());
        }
        /*try {
            Thread.sleep(75);
        } catch (Exception e) {
        }*/
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet.toString());
    }

    public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight fight, int teams, Maps map) {
        String packet = map.getFightersGMsPackets();
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }

        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight fight, Maps map, Player _perso) {
        String packet = map.getFightersGMsPackets();
        send(_perso, packet);

        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_PLAYER_JOIN(Fight fight, int teams, Fighter _fighter) {
        String packet = _fighter.getGmPacket('+');

        for (Fighter f : fight.getFighters(teams)) {
            if (f != _fighter) {
                if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
                if (f.getPersonnage() != null && f.getPersonnage().getAccount().getGameThread() != null)
                    send(f.getPersonnage(), packet);
            }
        }

        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET(Player perso, String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_TITRE_VALIDATION_REQUEST(int idValidation, String titre, String nomJoueur) {
        String msg = "Titre: " + titre + " par " + nomJoueur + ". Faites .valider " + idValidation + " pour valider ce titre ou .refuser " + idValidation + " + [votre motif].";
        String packet = "cMK@|1|ValidationTitre|" + msg;
        for (Player perso : World.getOnlinePlayers()) {
            if (perso.getAccount().getGmLevel() != 0) {
                send(perso, packet);
            }
        }
    }

    public static void GAME_SEND_FIGHT_LIST_PACKET(GameSendThread out, Maps map) {
        StringBuilder packet = new StringBuilder();
        packet.append("fL");
        for (Entry<Integer, Fight> entry : map.get_fights().entrySet()) {
            if (packet.length() > 2) {
                packet.append("|");
            }
            packet.append(entry.getValue().parseFightInfos());
        }
        send(out, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_cMK_PACKET_TO_MAP(Maps map, String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        for (Player z : map.getPersos()) {
            if (!z.getAccount().isEnemyWith(guid)) {
                send(z, packet);
            }
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_TO_GUILD(Guild g, String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        for (Player perso : g.getMembers()) {
            if (perso == null || !perso.isOnline()) continue;
            send(perso, packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Guild: Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_TO_ALL(String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        for (Player perso : World.getOnlinePlayers())
            send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL(" + World.getOnlinePlayers().size() + "): Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_TO_ALIGN(String suffix, int guid, String name, String msg, Player _perso) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        for (Player perso : World.getOnlinePlayers()) {
            if (perso.get_align() == _perso.get_align()) {
                send(perso, packet);
            }
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL(" + World.getOnlinePlayers().size() + "): Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_TO_ADMIN(String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        for (Player perso : World.getOnlinePlayers())
            if (perso.isOnline())
                if (perso.getAccount() != null) if (perso.getAccount().getGmLevel() > 0) send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL(" + World.getOnlinePlayers().size() + "): Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_TO_FIGHT(Fight fight, int teams, String suffix, int guid, String name, String msg) {
        String packet = (new StringBuilder("cMK")).append(suffix).append("|").append(guid).append("|").append(name).append("|").append(msg).toString();
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_GDZ_PACKET_TO_FIGHT(Fight fight, int teams, String suffix, int cell, int size, int unk) {
        String packet = "GDZ" + suffix + cell + ";" + size + ";" + unk;

        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_GDC_PACKET_TO_FIGHT(Fight fight, int teams, int cell) {
        String packet = "GDC" + cell;

        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_GA2_PACKET(GameSendThread out, int guid) {
        String packet = "GA;2;" + guid + ";";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_CHAT_ERROR_PACKET(GameSendThread out, String name) {
        String packet = "cMEf" + name;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_eD_PACKET_TO_MAP(Maps map, int guid, int dir) {
        String packet = "eD" + guid + "|" + dir;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_ECK_PACKET(Player out, int type, String str) {
        String packet = "ECK" + type;
        if (!str.equals("")) packet += "|" + str;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ECK_PACKET(GameSendThread out, int type, String str) {
        String packet = "ECK" + type;
        if (!str.equals("")) packet += "|" + str;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ITEM_VENDOR_LIST_PACKET(Player p, NPC npc) {
        String packet = "EL" + npc.get_template().getItemVendorList();
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>EL PACKET");
    }

    public static void GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(GameSendThread out, Collector perco) {
        String packet = "EL" + perco.getItemPercepteurList();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ITEM_LIST_PACKET_SELLER(Player p, Player out) {
        String packet = "EL" + p.parseStoreItemsList();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EV_PACKET(GameSendThread out) {
        String packet = "EV";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EV_PACKET(Player out) {
        String packet = "EV";
        send(out, packet);
    }

    public static void GAME_SEND_DCK_PACKET(GameSendThread out, int id) {
        String packet = "DCK" + id;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_QUESTION_PACKET(GameSendThread out, String str) {
        String packet = "DQ" + str;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_END_DIALOG_PACKET(GameSendThread out) {
        String packet = "DV";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_CONSOLE_MESSAGE_PACKET(GameSendThread out, String mess) {
        String packet = "BAT2" + mess;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_BUY_ERROR_PACKET(GameSendThread out) {
        String packet = "EBE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SELL_ERROR_PACKET(GameSendThread out) {
        String packet = "ESE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_BUY_OK_PACKET(GameSendThread out) {
        String packet = "EBK";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_OBJECT_QUANTITY_PACKET(Player out, Item obj) {
        String packet = "OQ" + obj.getGuid() + "|" + obj.getQuantity();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_OAKO_PACKET(Player out, Item obj) {
        String packet = "OAKO" + obj.parseItem();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ESK_PACKEt(Player out) {
        String packet = "ESK";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_REMOVE_ITEM_PACKET(Player out, int guid) {
        String packet = "OR" + guid;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_DELETE_OBJECT_FAILED_PACKET(GameSendThread out) {
        String packet = "OdE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_OBJET_MOVE_PACKET(Player out, Item obj) {
        String packet = "OM" + obj.getGuid() + "|";
        if (obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED)
            packet += obj.getPosition();

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EMOTICONE_TO_FIGHT(Fight fight, int teams, int guid, int id) {
        String packet = (new StringBuilder("cS")).append(guid).append("|").append(id).toString();
        for (Fighter f : fight.getFighters(teams)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_OAEL_PACKET(GameSendThread out) {
        String packet = "OAEL";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_NEW_LVL_PACKET(Account a, int lvl) {
        String packet = "AN" + lvl;
        GameSendThread out = a.getGameThread().getOut();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
        GameServer.addToLog("Le joueur " + a.getCurPlayer().getName() + " a atteint le niveau " + lvl);
    }

    public static void GAME_SEND_MESSAGE_TO_ALL(String msg, String color) {
        String packet = "cs<font color='#" + color + "'>" + msg + "</font>";
        for (Player P : World.getOnlinePlayers()) {
            send(P, packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL: Send>>" + packet);
    }

    public static void GAME_SEND_MESSAGE_TO_STAFF(String msg, String color) {
        String packet = "cs<font color='#" + color + "'>" + msg + "</font>";
        for (Player P : World.getOnlinePlayers()) {
            if (P.getAccount().getGmLevel() > 0) {
                send(P, packet);
            }
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL: Send>>" + packet);
    }

    public static void GAME_SEND_MESSAGE_TO_ALL2(String msg, String color) {
        String packet = "cMK|0|Global|<font color='#" + color + "'>" + msg + "</font>|";
        for (Player P : World.getOnlinePlayers()) {
            send(P, packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL: Send>>" + packet);
    }

    public static void MEOW(String name, int guid, Player nom) {
        String packet = "cMK|" + guid + "|" + name + "|Meow ?|";

        send(nom, packet);

        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_REQUEST_OK(GameSendThread out, int guid, int guidT, int msgID) {
        String packet = "ERK" + guid + "|" + guidT + "|" + msgID;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_REQUEST_ERROR(GameSendThread out, char c) {
        String packet = "ERE" + c;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_CONFIRM_OK(GameSendThread out, int type) {
        String packet = "ECK" + type;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_MOVE_OK(Player out, char type, String signe, String s1) {
        String packet = "EMK" + type + signe;
        if (!s1.equals(""))
            packet += s1;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_MOVE_OK_FM(Player out, char type, String signe, String s1) {
        String packet = "EmK" + type + signe;
        if (!s1.equals(""))
            packet += s1;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_OTHER_MOVE_OK(GameSendThread out, char type, String signe, String s1) {
        String packet = "EmK" + type + signe;
        if (!s1.equals(""))
            packet += s1;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_OTHER_MOVE_OK_FM(GameSendThread out, char type, String signe, String s1) {
        String packet = "EMK" + type + signe;
        if (!s1.equals(""))
            packet += s1;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_OK(GameSendThread out, boolean ok, int guid) {
        String packet = "EK" + (ok ? "1" : "0") + guid;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EXCHANGE_VALID(GameSendThread out, char c) {
        String packet = "EV" + c;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GROUP_INVITATION_ERROR(GameSendThread out, String s) {
        String packet = "PIE" + s;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GROUP_INVITATION(GameSendThread out, String n1, String n2) {
        String packet = "PIK" + n1 + "|" + n2;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GROUP_CREATE(GameSendThread out, Group g) {
        String packet = "PCK" + g.getChief().getName();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Groupe: Send>>" + packet);
    }

    public static void GAME_SEND_PL_PACKET(GameSendThread out, Group g) {
        String packet = "PL" + g.getChief().getGuid();
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Groupe: Send>>" + packet);
    }

    public static void GAME_SEND_PR_PACKET(Player out) {
        String packet = "PR";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_PV_PACKET(GameSendThread out, String s) {
        String packet = "PV" + s;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ALL_PM_ADD_PACKET(GameSendThread out, Group g) {
        StringBuilder packet = new StringBuilder();
        packet.append("PM+");
        boolean first = true;
        for (Player p : g.getPlayers()) {
            if (!first) packet.append("|");
            packet.append(p.parseToPM());
            first = false;
        }
        send(out, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_PM_ADD_PACKET_TO_GROUP(Group g, Player p) {
        String packet = "PM+" + p.parseToPM();
        for (Player P : g.getPlayers()) send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Groupe: Send>>" + packet);
    }

    public static void GAME_SEND_PM_MOD_PACKET_TO_GROUP(Group g, Player p) {
        String packet = "PM~" + p.parseToPM();
        for (Player P : g.getPlayers()) send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Groupe: Send>>" + packet);
    }

    public static void GAME_SEND_PM_DEL_PACKET_TO_GROUP(Group g, int guid) {
        String packet = "PM-" + guid;
        for (Player P : g.getPlayers()) send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Groupe: Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_TO_GROUP(Group g, String s, int guid, String name, String msg) {
        String packet = "cMK" + s + "|" + guid + "|" + name + "|" + msg + "|";
        for (Player P : g.getPlayers()) send(P, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Groupe: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_DETAILS(GameSendThread out, Fight fight) {
        if (fight == null) return;
        StringBuilder packet = new StringBuilder();
        packet.append("fD").append(fight.get_id()).append("|");
        for (Fighter f : fight.getFighters(1))
            packet.append(f.getPacketsName()).append("~").append(f.get_lvl()).append(";");
        packet.append("|");
        for (Fighter f : fight.getFighters(2))
            packet.append(f.getPacketsName()).append("~").append(f.get_lvl()).append(";");
        send(out, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_IQ_PACKET(Player perso, int guid, int qua) {
        String packet = "IQ" + guid + "|" + qua;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_JN_PACKET(Player perso, int jobID, int lvl) {
        String packet = "JN" + jobID + "|" + lvl;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GDF_PACKET_TO_MAP(Maps map, Case cell) {
        int cellID = cell.getID();
        InteractiveObject object = cell.getObject();
        String packet = "GDF|" + cellID + ";" + object.getState() + ";" + (object.isInteractive() ? "1" : "0");
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_GA_PACKET_TO_MAP(Maps map, String gameActionID, int actionID, String s1, String s2) {
        String packet = "GA" + gameActionID + ";" + actionID + ";" + s1;
        if (!s2.equals("")) packet += ";" + s2;

        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_EL_BANK_PACKET(Player perso) {
        String packet = "EL" + perso.parseBankPacket();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EL_TRUNK_PACKET(Player perso, Trunk t) {
        String packet = "EL" + t.parseToTrunkPacket();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_JX_PACKET(Player perso, ArrayList<StatsMetier> SMs) {
        StringBuilder packet = new StringBuilder();
        packet.append("JX");
        for (StatsMetier sm : SMs) {
            packet.append("|").append(sm.getTemplate().getId()).append(";").append(sm.get_lvl()).append(";").append(sm.getXpString(";")).append(";");
        }
        send(perso, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_JO_PACKET(Player perso, ArrayList<StatsMetier> SMs) {
        for (StatsMetier sm : SMs) {
            String packet = "JO" + sm.getID() + "|" + sm.getOptBinValue() + "|2";//FIXME 2=?
            send(perso, packet);
            if (Config.DEBUG)
                GameServer.addToSockLog("Game: Send>>" + packet);
        }
    }

    public static void GAME_SEND_JS_PACKET(Player perso, ArrayList<StatsMetier> SMs) {
        String packet = "JS";
        for (StatsMetier sm : SMs) {
            packet += sm.parseJS();
        }
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EsK_PACKET(Player perso, String str) {
        String packet = "EsK" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_SHOW_CASE(ArrayList<GameSendThread> PWs, int guid, int cellID) {
        String packet = "Gf" + guid + "|" + cellID;
        for (GameSendThread PW : PWs) {
            send(PW, packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_Ea_PACKET(Player perso, String str) {
        String packet = "Ea" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EA_PACKET(Player perso, String str) {
        String packet = "EA" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Ec_PACKET(Player perso, String str) {
        String packet = "Ec" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Em_PACKET(Player perso, String str) {
        String packet = "Em" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_IO_PACKET_TO_MAP(Maps map, int guid, String str) {
        String packet = "IO" + guid + "|" + str;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_FRIENDLIST_PACKET(Player perso) {
        String packet = "FL" + perso.getAccount().parseFriendList();
        send(perso, packet);
        if (perso.getWife() != 0) {
            String packet2 = "FS" + perso.get_wife_friendlist();
            send(perso, packet2);
            if (Config.DEBUG)
                GameServer.addToSockLog("Game: Send>>" + packet2);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FRIEND_ONLINE(Player logando, Player amigo) {
        String packet = "Im0143;" + logando.getAccount().getPseudo() + " (<b><a href='asfunction:onHref,ShowPlayerPopupMenu," + logando.getName() + "'>" + logando.getName() + "</a></b>)";
        send(amigo, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FA_PACKET(Player perso, String str) {
        String packet = "FA" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FD_PACKET(Player perso, String str) {
        String packet = "FD" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Rp_PACKET(Player perso, MountPark MP) {
        StringBuilder packet = new StringBuilder();
        if (MP == null) return;

        packet.append("Rp").append(MP.get_owner()).append(";").append(MP.get_price()).append(";").append(MP.get_size()).append(";").append(MP.getObjectNumb()).append(";");

        Guild G = MP.get_guild();
        //Si une guilde est definie
        if (G != null) {
            packet.append(G.get_name()).append(";").append(G.get_emblem());
        } else {
            packet.append(";");
        }

        send(perso, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_OS_PACKET(Player perso, int pano) {
        StringBuilder packet = new StringBuilder();
        packet.append("OS");
        int num = perso.getNumbEquipedItemOfPanoplie(pano);
        if (num <= 0) packet.append("-").append(pano);
        else {
            packet.append("+").append(pano).append("|");
            ItemSet IS = World.getItemSet(pano);
            if (IS != null) {
                StringBuilder items = new StringBuilder();
                //Pour chaque objet de la pano
                for (ObjTemplate OT : IS.getItemTemplates()) {
                    //Si le joueur l'a équipé
                    if (perso.hasEquiped(OT.getID())) {
                        //On l'ajoute au packet
                        if (items.length() > 0) items.append(";");
                        items.append(OT.getID());
                    }
                }
                packet.append(items.toString()).append("|").append(IS.getBonusStatByItemNumb(num).parseToItemSetStats());
            }
        }
        send(perso, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_MOUNT_DESCRIPTION_PACKET(Player perso, Mount DD) {
        String packet = "Rd" + DD.parse();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_Rr_PACKET(Player perso, String str) {
        String packet = "Rr" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ALTER_GM_PACKET(Maps map, Player perso) {
        if (perso.get_size() != 0) {
            String packet = "GM|~" + perso.parseToGM();
            for (Player z : map.getPersos()) send(z, packet);
            if (Config.DEBUG)
                GameServer.addToSockLog("Game: Map: Send>>" + packet);
        }
    }

    public static void GAME_SEND_Ee_PACKET(Player perso, char c, String s) {
        String packet = "Ee" + c + s;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_cC_PACKET(Player perso, char c, String s) {
        String packet = "cC" + c + s;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ADD_NPC_TO_MAP(Maps map, NPC npc) {
        String packet = "GM|" + npc.parseGM();
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            Logs.addToDebug("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_ADD_PERCO_TO_MAP(Maps map) {
        String packet = "GM|" + Collector.parseGM(map);
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_GDO_PACKET_TO_MAP(Maps map, char c, int cell, int itm, int i) {
        String packet = "GDO" + c + cell + ";" + itm + ";" + i;
        for (Player z : map.getPersos()) send(z, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Map: Send>>" + packet);
    }

    public static void GAME_SEND_GDO_PACKET(Player p, char c, int cell, int itm, int i) {
        String packet = "GDO" + c + cell + ";" + itm + ";" + i;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ZC_PACKET(Player p, int a) {
        String packet = "ZC" + a;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GIP_PACKET(Player p, int a) {
        String packet = "GIP" + a;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gn_PACKET(Player p) {
        String packet = "gn";
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gC_PACKET(Player p, String s) {
        String packet = "gC" + s;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gV_PACKET(Player p) {
        String packet = "gV";
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gIM_PACKET(Player p, Guild g, char c) {
        String packet = "gIM" + c;
        switch (c) {
            case '+':
                packet += g.parseMembersToGM();
                break;
        }
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gIB_PACKET(Player p, String infos) {
        String packet = "gIB" + infos;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gIH_PACKET(Player p, String infos) {
        String packet = "gIH" + infos;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gS_PACKET(Player p, GuildMember gm) {
        StringBuilder packet = new StringBuilder();
        packet.append("gS").append(gm.getGuild().get_name()).append("|").append(gm.getGuild().get_emblem().replace(',', '|')).append("|").append(gm.parseRights());
        send(p, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_gJ_PACKET(Player p, String str) {
        String packet = "gJ" + str;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gK_PACKET(Player p, String str) {
        String packet = "gK" + str;
        send(p, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gIG_PACKET(Player p, Guild g) {
        long xpMin = World.getExpLevel(g.get_lvl()).guilde;
        long xpMax;
        if (World.getExpLevel(g.get_lvl() + 1) == null) {
            xpMax = -1;
        } else {
            xpMax = World.getExpLevel(g.get_lvl() + 1).guilde;
        }
        StringBuilder packet = new StringBuilder();
        packet.append("gIG").append((g.getSize() > 9 ? 1 : 0)).append("|").append(g.get_lvl()).append("|").append(xpMin).append("|").append(g.get_xp()).append("|").append(xpMax);
        send(p, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void REALM_SEND_MESSAGE(GameSendThread out, String args) {
        String packet = "M" + args;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_WC_PACKET(Player perso) {
        String packet = "WC" + perso.parseZaapList();
        send(perso.getAccount().getGameThread().getOut(), packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_WV_PACKET(Player out) {
        String packet = "WV";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ZAAPI_PACKET(Player perso, String list) {
        String packet = "Wc" + perso.getMap().get_id() + "|" + list;
        send(perso, packet);
        GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_CLOSE_ZAAPI_PACKET(Player out) {
        String packet = "Wv";
        send(out, packet);
        GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_WUE_PACKET(Player out) {
        String packet = "WUE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EMOTE_LIST(Player perso, String s, String s1) {
        String packet = "eL" + s + "|" + s1;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_NO_EMOTE(Player out) {
        String packet = "eUE";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ADD_ENEMY(Player out, Player pr) {

        String packet = "iAK" + pr.getAccount().getName() + ";2;" + pr.getName() + ";36;10;0;100.FL.";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_iAEA_PACKET(Player out) {

        String packet = "iAEA.";
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_ENEMY_LIST(Player perso) {

        String packet = "iL" + perso.getAccount().parseEnemyList();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_iD_COMMANDE(Player perso, String str) {
        String packet = "iD" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_BWK(Player perso, String str) {
        String packet = "BWK" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_KODE(Player perso, String str) {
        String packet = "K" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_hOUSE(Player perso, String str) {
        String packet = "h" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);

    }

    public static void GAME_SEND_FORGETSPELL_INTERFACE(char sign, Player perso) {
        String packet = "SF" + sign;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_R_PACKET(Player perso, String str) {
        String packet = "R" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gIF_PACKET(Player perso, String str) {
        String packet = "gIF" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gITM_PACKET(Player perso, String str) {
        String packet = "gITM" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gITp_PACKET(Player perso, String str) {
        String packet = "gITp" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gITP_PACKET(Player perso, String str) {
        String packet = "gITP" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_IH_PACKET(Player perso, String str) {
        String packet = "IH" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FLAG_PACKET(Player perso, Player cible) {
        String packet = "IC" + cible.getMap().getX() + "|" + cible.getMap().getY();
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_DELETE_FLAG_PACKET(Player perso) {
        String packet = "IC|";
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_gT_PACKET(Player perso, String str) {
        String packet = "gT" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GUILDHOUSE_PACKET(Player perso) {
        String packet = "gUT";
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_GUILDENCLO_PACKET(Player perso) {
        String packet = "gUF";
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    /**
     * HDV
     **/
    public static void GAME_SEND_EHm_PACKET(Player out, String sign, String str) {
        String packet = "EHm" + sign + str;

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EHM_PACKET(Player out, String sign, String str) {
        String packet = "EHM" + sign + str;

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EHP_PACKET(Player out, int templateID)    //Packet d'envoie du prix moyen du template (En réponse a un packet EHP)
    {

        String packet = "EHP" + templateID + "|" + World.getObjTemplate(templateID).getAvgPrice();

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EHl(Player out, AuctionHouse seller, int templateID) {
        String packet = "EHl" + seller.parseToEHl(templateID);
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EHL_PACKET(Player out, int categ, String templates)    //Packet de listage des templates dans une catégorie (En réponse au packet EHT)
    {
        String packet = "EHL" + categ + "|" + templates;

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_EHL_PACKET(Player out, String items)    //Packet de listage des objets en vente
    {
        String packet = "EHL" + items;

        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_HDVITEM_SELLING(Player perso) {
        String packet = "EL";
        HdvEntry[] entries = perso.getAccount().getHdvItems(Math.abs(perso.get_isTradingWith()));    //Récupère un tableau de tout les items que le personnage à en vente dans l'HDV où il est
        boolean isFirst = true;
        for (HdvEntry curEntry : entries) {
            if (curEntry == null)
                break;
            if (!isFirst) {
                packet += "|";
            }
            packet += curEntry.parseToEL();

            isFirst = false;
        }
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_WEDDING(Maps c, int action, int homme, int femme, int parlant) {
        String packet = "GA;" + action + ";" + homme + ";" + homme + "," + femme + "," + parlant;
        Player Homme = World.getPlayer(homme);
        send(Homme, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_PF(Player perso, String str) {
        String packet = "PF" + str;
        send(perso, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_MERCHANT_LIST(Player P, short mapID) // @Flow - Sérieux... Il y a un problème ici.
    {
        StringBuilder packet = new StringBuilder();
        packet.append("GM|"); // anciennement GM|~
        if (World.getSeller(P.getMap().get_id()) == null) return;
        for (Integer pID : World.getSeller(P.getMap().get_id())) {
            if (!World.getPlayer(pID).isOnline() && World.getPlayer(pID).is_showSeller()) {
                //packet.append(World.getPlayer(pID).parseToMerchant()).append("|");
                packet.append("~").append(World.getPlayer(pID).parseToMerchant()).append("|");
            }
        }
        if (packet.length() < 5) return;
        send(P, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet.toString());
    }

    public static void GAME_SEND_cMK_PACKET_INCARNAM_CHAT(Player perso, String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        if (perso.getLevel() > 15) {
            GAME_SEND_BN(perso);
            return;
        }
        for (Player perso1 : World.getOnlinePlayers()) {
            send(perso1, packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL(" + World.getOnlinePlayers().size() + "): Send>>" + packet);
    }

    public static void GAME_SEND_cMK_PACKET_GLOBAL_CHAT(Player perso, String suffix, int guid, String name, String msg) {
        String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
        for (Player perso1 : World.getOnlinePlayers()) {
            if (!perso1.neVeutPasVoirMessage) {
                send(perso1, packet);
            }
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: ALL(" + World.getOnlinePlayers().size() + "): Send>>" + packet);
    }

    public static void GAME_SEND_PACKET_TO_FIGHT(Fight fight, int i, String packet) {
        for (Fighter f : fight.getFighters(i)) {
            if (f.hasLeft()) continue;
            if (f.getPersonnage() == null || !f.getPersonnage().isOnline()) continue;
            send(f.getPersonnage(), packet);
        }
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight : Send>>" + packet);
    }

    public static void GAME_SEND_CIN_Packet(Player perso, String num) { // Jouer une cinématique
        send(perso, "GA;2;" + perso.getGuid() + ";" + num);
    }

    public static void GAME_SEND_Carte_ACTUALIZAR_CELDA(Maps Carte, String str) { //Actualiser une cellule
        String packet = "GDO+" + str;
        for (Player z : Carte.getPersos())
            send(z, packet);
    }

    public static void ENVIAR_GDO_PONER_OBJETO_CRIA_EN_MAPA(Maps mapa, String str) { //Actualiser une cellule 3.0
        String packet = "GDO+" + str;
        for (Player z : mapa.getPersos())
            send(z, packet);
    }

    public static void GAME_SEND_DELETE_STATS_ITEM_FM(Player perso, int id) {
        String packet = "OR" + id;
        send(perso, packet);
    }

    public static void GAME_SEND_Ew_PACKET(Player perso, int pods, int podsMax) { //Pods de la dinde
        String packet = "Ew" + pods + ";" + podsMax + "";
        send(perso, packet);
    }

    public static void GAME_SEND_BAIO_PACKET(Player perso, String str) { // Afficher le panel d'info
        String packet = "BAIO" + str;
        send(perso, packet);
    }

    public static void GAME_WELCOME(Player perso) {
        send(perso, "TB");
    }

    public static void GAME_SEND_EJ_PACKET(Player perso, int metid, int pid, StatsMetier sm) { //Regarder un livre de métier
        Player p = World.getPlayer(pid);
        if (p == null) return;
        String a = p.parse_tojobbook(metid);
        if (a == null) return;
        send(perso, "EJ+" + metid + ";" + pid + ";" + a);
    }

    public static void GAME_SEND_Ag_PACKET(GameSendThread out, int idObjeto, String codObjeto) { //Cadeau à la connexion
        String packet = "Ag1|" + idObjeto
                + "|Cadeau Dofus| Voilà un joli cadeau pour vous ! "
                + "Un jeune aventurier comme vous sera sans servir de la meilleur façon ! "
                + "Bonne continuation avec ceci ! |DOFUS|"
                + codObjeto;
        send(out, packet);
    }

    public static void GAME_SEND_AGK_PACKET(GameSendThread out) { //Cadeau à la connexion
        String packet = "AGK";
        send(out, packet);
    }

    public static void GAME_SEND_dCK_PACKET(Player out, String id) //Ouvrir un livre
    {
        String packet = "dCK" + id;
        send(out, packet);
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_SB_PACKET(Player p, Player.BoostSpellStats stats, boolean isAdd) {
        StringBuilder packet = new StringBuilder();
        boolean isFirst = false;
        Player.BoostSpellStats trueStats = p.getTotalBoostSpellStats();
        int trueval;
        if (!isAdd) {
            for (Entry<Integer, Map<Integer, Integer>> entry : stats.getAllEffects().entrySet()) {
                if (entry == null || entry.getValue() == null) continue;
                for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
                    if (!isFirst) packet.append("\0");
                    packet.append("SB").append(stat.getKey()).append(";").append(entry.getKey()).append(";-1");
                    isFirst = false;
                }
            }
        } else {
            for (Entry<Integer, Map<Integer, Integer>> entry : stats.getAllEffects().entrySet()) {
                if (entry == null || entry.getValue() == null) continue;
                for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
                    if (!isFirst) packet.append("\0");
                    switch (stat.getKey()) {
                        case Constant.STATS_BOOST_SPELL_CASTOUTLINE:
                        case Constant.STATS_BOOST_SPELL_NOLINEOFSIGHT:
                        case Constant.STATS_BOOST_SPELL_RANGEABLE:
                            packet.append("SB").append(stat.getKey()).append(";").append(entry.getKey()).append(";1");
                            break;
                        default:
                            trueval = trueStats.getStat(entry.getKey(), stat.getKey());
                            packet.append("SB").append(stat.getKey()).append(";").append(entry.getKey()).append(";").append(trueval);
                    }
                    isFirst = false;
                }
            }
        }
        send(p, packet.toString());
        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Send>>" + packet);
    }

    public static void GAME_SEND_FIGHT_PLAYER_JOIN(Player perso, Fighter f) {
        String packet = f.getGmPacket('+');
        send(perso, packet);

        if (Config.DEBUG)
            GameServer.addToSockLog("Game: Fight: Send>>" + packet);
    }

    public static void GAME_SEND_MOUNT_PODS(Player perso, int pods) {
        String packet = "Ew" + pods + ";1000";
        send(perso, packet);
    }

    public static void GAME_SEND_EL_MOUNT_INVENTAIRE(GameSendThread _out, Mount DD) {
        String packet = "EL" + DD.getInventaire();
        send(_out, packet);
    }

    public static void send(PrintWriter out, String packet) {
        long t = System.currentTimeMillis();
        if (out != null && !packet.equals("") && !packet.equals("\0")) {
            packet = CryptManager.toUtf(packet);
            out.print((new StringBuilder(String.valueOf(packet))).append('\0').toString());
            out.flush();
        }
        if (System.currentTimeMillis() - t > 5000L)
            GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "DEBUG-SOCKET-OUT", (new StringBuilder("SocketManager.send( ____OUT ); = ")).append(System.currentTimeMillis() - t).append("; ").toString());
    }

    public static void GAME_SEND_OCO_PACKET(Player out, Item obj) {
        String packet = "OCO" + obj.parseItem();
        send(out, packet);
    }

    public static void GAME_SEND_FLAG_PACKET(Player perso, Maps cible) {
        String packet = "IC" + cible.getX() + "|" + cible.getY();
        send(perso, packet);
    }

    public static void GAME_SEND_POPUP(Player perso, String str) {

        String packet = "BAIO" + str;
        send(perso, packet);
    }

    public static long GAME_PING_PLAYER(Player perso) {
        long time = System.currentTimeMillis();
        String packet = "rpong" + time;
        send(perso, packet);
        return time;
    }

    public static void GAME_PING_PLAYERS() {
        long time = System.currentTimeMillis();
        String packet = "rpong" + time;
        for (Player p : World.getOnlinePlayers()) {
            send(p, packet);
        }
    }

    public static void SEND_QUESTS_LIST_PACKET(Player P) {
        /*
		 * Explication packet : QL + QuestID ; Finish ? 1 : 0 ;
		 */
        String packet = "QL" + P.getQuestGmPacket();
        send(P, packet);
    }

    public static void SEND_QUEST_STEPS_PACKET(Player P, int questId) {
        //String packet = "QS209|386|830,1;831,1;832,0;833,0;834,0||387|417";

        String packet = P.parseQuestStep(questId);
        if (packet.isEmpty()) return;

        send(P, packet);
    }

    public static void GAME_SEND_PERCO_INFOS_PACKET(Player z, Collector perco, String car) {
        StringBuilder str = new StringBuilder();
        str.append("gA").append(car).append(perco.get_N1()).append(",").append(perco.get_N2()).append("|");
        str.append("-1").append("|");
        str.append(World.getCarte(perco.get_mapID()).getX()).append("|").append(World.getCarte(perco.get_mapID()).getY());
        send(z, str.toString());
    }

    public static void SEND_gA_PERCEPTEUR(Player z, String str) {
        String packet = "gA" + str;
        send(z, packet);
    }

    public static void GAME_SEND_GDF_PACKET_TO_FIGHT(Player perso, Collection<Case> values) {
        String packet = "GDF|";
        for (Case cell : values) {
            if (cell.getObject() == null)
                continue;
            if (cell.getObject().getTemplate() == null)
                continue;

            switch (((ItemSet) cell.getObject().getTemplate()).getId()) {
                case 7515:
                case 7511:
                case 7517:
                case 7512:
                case 7513:
                case 7516:
                case 7550:
                case 7518:
                case 7534:
                case 7535:
                case 7533:
                case 7551:
                case 7500:
                case 7536:
                case 7501:
                case 7502:
                case 7503:
                case 7542:
                case 7541:
                case 7504:
                case 7553:
                case 7505:
                case 7506:
                case 7507:
                case 7557:
                case 7554:
                case 7508:
                case 7509:
                case 7552:
                    cell.getObject().setState(Constant.IOBJECT_STATE_EMPTY2);
                    packet += cell.getID() + ";" + cell.getObject().getState()
                            + ";"
                            + (cell.getObject().isInteractive() ? "1" : "0")
                            + "|";
                    break;
            }
        }
        send(perso, packet);
    }

    public static void QuestGep(Quest quest, Player perso) {
		/*
		 * Explication packet : aQuestId | aObjectifCurrent |
		 * aEtapeId,aFinish;aEtapeId,aFinish... | aPreviousObjectif |
		 * aNextObjectif | aDialogId | aDialogParams
		 */
        String packet = "QS" + quest.getGmQuestDataPacket(perso);
        send(perso, packet);
    }
}
