package org.area.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.SystemUtils;
import org.area.check.FloodCheck;
import org.area.check.Security;
import org.area.client.Account;
import org.area.client.Player;
import org.area.client.Player.Group;
import org.area.client.tools.RandomCharacterName;
import org.area.command.GmCommand;
import org.area.command.PlayerCommand;
import org.area.common.ConditionParser;
import org.area.common.Constant;
import org.area.common.CryptManager;
import org.area.common.Formulas;
import org.area.common.Pathfinding;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.Couple;
import org.area.common.World.Exchange.NpcExchange;
import org.area.event.EventConstant;
import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.fight.object.Collector;
import org.area.fight.object.Prism;
import org.area.fight.object.Stalk;
import org.area.fight.object.Stalk.Traque;
import org.area.game.tools.AllColor;
import org.area.game.tools.ParseTool;
import org.area.game.tools.Util;
import org.area.kernel.Config;
import org.area.kernel.Console;
import org.area.kernel.Logs;
import org.area.kernel.Main;
import org.area.lang.Lang;
import org.area.object.AuctionHouse;
import org.area.object.AuctionHouse.HdvEntry;
import org.area.object.Guild;
import org.area.object.Guild.GuildMember;
import org.area.object.Houses;
import org.area.object.Item;
import org.area.object.Item.ObjTemplate;
import org.area.object.Maps;
import org.area.object.Maps.Case;
import org.area.object.Maps.MountPark;
import org.area.object.Mount;
import org.area.object.NpcTemplate;
import org.area.object.NpcTemplate.NPC;
import org.area.object.NpcTemplate.NPC_Exchange;
import org.area.object.NpcTemplate.NPC_question;
import org.area.object.NpcTemplate.NPC_reponse;
import org.area.object.Trunk;
import org.area.object.job.Job.StatsMetier;
import org.area.spell.Spell.SortStats;
import org.simplyfilter.filter.Filter;
import org.simplyfilter.filter.Filters;


public class GameThread implements Runnable {

    private BufferedReader in;
    private Filter filter = Filters.createNewSafe(20, 500);
    private Thread thread;
    private GameSendThread out;
    private Socket socket;
    private Account account;
    private Player player;
    private Map<Integer, GameAction> actions = new TreeMap<Integer, GameAction>();
    private long _timeLastTradeMsg = 0, _timeLastRecrutmentMsg = 0,
            _timeLastAlignMsg = 0, _timeLastIncarnamMsg = 0;
    private GmCommand command;

    public static class GameAction {
        public int _id;
        public int _actionID;
        public String _packet;
        public String _args;

        public GameAction(int aId, int aActionId, String aPacket) {
            _id = aId;
            _actionID = aActionId;
            _packet = aPacket;
        }
    }

    /**
     * @param sock
     */
    public GameThread(Socket sock) {
        try {
            socket = sock;
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

            try {
                out = new GameSendThread(this, socket, new PrintWriter(
                        socket.getOutputStream()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            thread = new Thread(Main.THREAD_GAME, this);
            thread.setDaemon(true);
            try {
                thread.start();
            } catch (OutOfMemoryError e) {
                Logs.addToDebug("OutOfMemory dans le Game");
                e.printStackTrace();
                try {
                    Main.listThreads(true);
                } catch (Exception ed) {
                }
                try {
                    thread.start();
                } catch (OutOfMemoryError e1) {
                }
            }
        } catch (IOException e) {
            try {
                GameServer.addToLog(e.getMessage());
                if (!socket.isClosed())
                    socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void run() {
        try {
            String packet = "";
            char charCur[] = new char[1];
            SocketManager.GAME_SEND_HELLOGAME_PACKET(out);

            while (in.read(charCur, 0, 1) != -1 && Main.isRunning) {
                if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r') {
                    packet += charCur[0];
                } else if (!packet.isEmpty()) {
                    packet = CryptManager.toUnicode(packet);
                    /*if(!IpCheck.onGamePacket(_s.getInetAddress().getHostAddress(), packet))
                        _s.close();*/
                    GameServer.addToSockLog("Game: Recu << " + packet);
                    if (Main.gameServer.encryptPacketKey != "0") {
                        try {
                            String packetTest = CryptManager.decryptPacket(packet);
                            GameServer.addToSockLog("Game: Packet decrypté: " + packetTest);
                        } catch (Exception e) {
                        }
                    }
                    ParseTool.parsePacket(packet, player);
                    parsePacket(packet);
                    packet = "";

                }
            }
        } catch (IOException e) {
            if (e.getClass() == SocketTimeoutException.class) {
                kick(1);
            } else {
                try {
                    GameServer.addToLog(e.getMessage());
                    if (in != null) {
                        in.close();
                        in = null;
                    }

                    if (out != null) {
                        out.close();
                        out = null;
                    }
                    if (account != null) {
                        account.setCurPerso(null);
                        account.setGameThread(null);
                    }
                    if (!socket.isClosed())
                        socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            GameServer.addToLog(e.getMessage());
        } finally {
            kick();
            GameServer.addToSockLog("-GameClientKicked-");
        }
    }


    @SuppressWarnings("unused")
    private boolean checked(String packet, String keyToCheck) {
        String key;
        if (packet.length() % 3 == 0) {
            key = sha1(sha1(md5(md5(packet))));
        } else if (packet.length() % 2 == 0) {
            key = md5(sha1(sha1(md5(packet))));
        } else {
            key = md5(md5(sha1(packet)));
        }
        return key.equals(keyToCheck);
    }

    private String md5(String string) {
        byte[] bytesKey;
        try {
            bytesKey = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
        return DigestUtils.md5Hex(bytesKey);
    }

    private String sha1(String string) {
        byte[] bytesKey;
        try {
            bytesKey = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
        return DigestUtils.sha1Hex(bytesKey);
    }

    /**
     * @param packet
     */
    private void parsePacket(String packet) {
        if (!this.filter.authorize(this.socket.getInetAddress().getHostAddress()))
            this.kick();
        if (player != null)
            player.refreshLastPacketTime();
        if (packet.length() > 1000)
            return;
        if (packet.length() > 3 && packet.substring(0, 4).equalsIgnoreCase("ping")) {
            if (out != null)
                SocketManager.GAME_SEND_PONG(out);
            return;
        }
        if (packet.length() > 4 && packet.substring(0, 5).equalsIgnoreCase("qping")) {
            if (out != null)
                SocketManager.GAME_SEND_QPONG(out);
            return;
        }
        if (out == null) {
            player.sendText("La connexion de sortie lié à votre thread est inexistante, veuillez le signaler à Flow.");
            GameSendThread o = player.getAccount().getGameThread().getOut();
            if (o != null) {
                out = o;
                player.sendText("Tentative de correction de la connexion.");
            } else {
                player.sendText("Impossible de corriger la connexion.");
            }

        }
        switch (packet.charAt(0)) {
            case 'A':
                parseAccountPacket(packet);
                break;
            case 'C':
                ParseConquetePacket(packet);
                break;
            case 'B':
                parseBasicsPacket(packet);
                break;

            case 'c':
                parseChanelPacket(packet);
                break;

            case 'D':
                parseDialogPacket(packet);
                break;

            case 'E':
                parseExchangePacket(packet);
                break;

            case 'e':
                parse_environementPacket(packet);
                break;

            case 'F':
                parse_friendPacket(packet);
                break;

            case 'f':
                parseFightPacket(packet);
                break;

            case 'G':
                parseGamePacket(packet);
                break;

            case 'g':
                parseGuildPacket(packet);
                break;

            case 'h':
                parseHousePacket(packet);
                break;

            case 'i':
                parse_enemyPacket(packet);
                break;

            case 'K':
                parseHouseKodePacket(packet);
                break;

            case 'O':
                parseObjectPacket(packet);
                break;

            case 'P':
                parseGroupPacket(packet);
                break;

            case 'Q':
                parseQuestPacket(packet);
                break;

            case 'R':
                parseMountPacket(packet);
                break;

            case 'S':
                parseSpellPacket(packet);
                break;

            case 'W':
                parseWaypointPacket(packet);
                break;

            case 'Z':
                parseSpecialPackets(packet);
                break;

            default: // Pour éviter les packets chinoix qui génères des <<Dofus ne
                // répond pas>>
                break;
        }
    }

    private void parseQuestPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'L': //Liste quête
                SocketManager.SEND_QUESTS_LIST_PACKET(player);
                break;
            case 'S'://Etapes d'une quete
                SocketManager.SEND_QUEST_STEPS_PACKET(player, Integer.parseInt(packet.substring(2)));
                break;
        }
    }

    private void parseHousePacket(String packet) {
        switch (packet.charAt(1)) {
            case 'B':// Acheter la maison
                packet = packet.substring(2);
                Houses.HouseAchat(player);
                break;
            case 'G':// Maison de guilde
                packet = packet.substring(2);
                if (packet.isEmpty())
                    packet = null;
                Houses.parseHG(player, packet);
                break;
            case 'Q':// Quitter/Expulser de la maison
                packet = packet.substring(2);
                Houses.Leave(player, packet);
                break;
            case 'S':// Modification du prix de vente
                packet = packet.substring(2);
                Houses.SellPrice(player, packet);
                break;
            case 'V':// Fermer fenetre d'achat
                Houses.closeBuy(player);
                break;
        }
    }

    private void parseHouseKodePacket(String packet) {
        switch (packet.charAt(1)) {
            case 'V':// Fermer fenetre du code
                Houses.closeCode(player);
                break;
            case 'K':// Envoi du code
                House_code(packet);
                break;
        }
    }

    private void House_code(String packet) {
        switch (packet.charAt(2)) {
            case '0':// Envoi du code || Boost
                packet = packet.substring(4);
                if (player.get_savestat() > 0) {
                    try {
                        int code = 0;
                        code = Integer.parseInt(packet);
                        if (code < 0)
                            return;
                        if (player.get_capital() < code)
                            code = player.get_capital();
                        player.boost(player.get_savestat(), code);
                    } catch (Exception e) {
                    } finally {
                        player.set_savestat(0);
                        SocketManager.GAME_SEND_KODE(player, "V");
                    }
                } else if (player.getInTrunk() != null)
                    Trunk.OpenTrunk(player, packet, false);
                else
                    Houses.OpenHouse(player, packet, false);
                break;
        }
    }

    private void ParseConquetePacket(String packet) {
        switch (packet.charAt(1)) {

            case 'b':
                SocketManager.SEND_Cb_CONQUETE(
                        player,
                        World.getBalanceMundo(player.get_align())
                                + ";"
                                + World.getBalanceArea(player.getMap().getSubArea()
                                .getArea(), player.get_align()));
                break;
            case 'B':
                double porc = World.getBalanceMundo(player.get_align());
                double porcN = Math.rint((player.getGrade() / 2.5) + 1);
                SocketManager.SEND_CB_BONUS_CONQUETE(player, porc + "," + porc
                        + "," + porc + ";" + porcN + "," + porcN + "," + porcN
                        + ";" + porc + "," + porc + "," + porc);
                break;
            case 'W':
                geoConquest(packet);
                break;
            case 'I':
                protectConquest(packet);
                break;
            case 'F':
                joinProtectorsOfPrism(packet);
                break;
        }
    }

    private void protectConquest(String packet) {
        switch (packet.charAt(2)) {
            case 'J':
                String str = player.parsePrisme();

                Prism Prismes = World.getPrisme(player.getMap().getSubArea()
                        .getPrismeID());
                if (Prismes != null) {
                    Prism.parseAttack(player);
                    Prism.parseDefense(player);
                }
                SocketManager.SEND_CIJ_INFO_JOIN_PRISME(player, str);
                break;
            case 'V':
                SocketManager.SEND_CIV_INFOS_CONQUETE(player);
                break;
        }
    }

    private void geoConquest(String packet) {
        switch (packet.charAt(2)) {
            case 'J':
                SocketManager.SEND_CW_INFO_CONQUETE(player,
                        World.PrismesGeoposition(player.get_align()));
                break;
            case 'V':
                SocketManager.SEND_CIV_INFOS_CONQUETE(player);
                break;
        }
    }

    private void joinProtectorsOfPrism(String packet) {
        switch (packet.charAt(2)) {
            case 'J':
                int PrismeID = player.getMap().getSubArea().getPrismeID();
                Prism Prismes = World.getPrisme(PrismeID);
                if (Prismes == null)
                    return;
                int FightID = -1;
                try {
                    FightID = Prismes.getFightID();
                } catch (Exception e) {
                }
                short CarteID = -1;
                try {
                    CarteID = Prismes.getCarte();
                } catch (Exception e) {
                }
                int celdaID = -1;
                try {
                    celdaID = Prismes.getCell();
                } catch (Exception e) {
                }
                if (PrismeID == -1 || FightID == -1 || CarteID == -1
                        || celdaID == -1)
                    return;
                if (Prismes.getalignement() != player.get_align())
                    return;
                if (player.getFight() != null)
                    return;
                if (player.getMap().get_id() != CarteID) {// System.out.println("Fuck5");
                    player.setMapProt(player.getMap());
                    player.setCellProt(player.get_curCell());
                    try {
                        Thread.sleep(200);
                        player.teleport(CarteID, celdaID);
                        Thread.sleep(400);
                    } catch (Exception e) {
                    }
                }

                World.getCarte(CarteID).getFight(FightID)
                        .joinPrismeFight(player, player.getGuid(), PrismeID);
                for (Player z : World.getOnlinePlayers()) {
                    if (z == null)
                        continue;
                    if (z.get_align() != player.get_align())
                        continue;
                    Prism.parseDefense(z);
                }
                break;
        }
    }

    private void parse_enemyPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'A':// Ajouter
                Enemy_add(packet);
                break;
            case 'D':// Delete
                Enemy_delete(packet);
                break;
            case 'L':// Liste
                SocketManager.GAME_SEND_ENEMY_LIST(player);
                break;
        }
    }

    private void Enemy_add(String packet) {
        if (player == null)
            return;
        int guid = -1;
        switch (packet.charAt(2)) {
            case '%':// Nom de perso
                packet = packet.substring(3);
                Player P = World.getPersoByName(packet);
                if (P == null) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = P.getAccID();

                break;
            case '*':// Pseudo
                packet = packet.substring(3);
                Account C = World.getCompteByPseudo(packet);
                if (C == null) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = C.getGuid();
                break;
            default:
                packet = packet.substring(2);
                Player Pr = World.getPersoByName(packet);
                if (Pr == null ? true : !Pr.isOnline()) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = Pr.getAccount().getGuid();
                break;
        }
        if (guid == -1) {
            SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
            return;
        }
        account.addEnemy(packet, guid);
    }

    private void Enemy_delete(String packet) {
        if (player == null)
            return;
        int guid = -1;
        switch (packet.charAt(2)) {
            case '%':// Nom de perso
                packet = packet.substring(3);
                Player P = World.getPersoByName(packet);
                if (P == null) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = P.getAccID();

                break;
            case '*':// Pseudo
                packet = packet.substring(3);
                Account C = World.getCompteByPseudo(packet);
                if (C == null) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = C.getGuid();
                break;
            default:
                packet = packet.substring(2);
                Player Pr = World.getPersoByName(packet);
                if (Pr == null ? true : !Pr.isOnline()) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = Pr.getAccount().getGuid();
                break;
        }
        if (guid == -1 || !account.isEnemyWith(guid)) {
            SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
            return;
        }
        account.removeEnemy(guid);
    }

    private void parseWaypointPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'U':// Use
                Waypoint_use(packet);
                break;
            case 'u':// use zaapi
                Zaapi_use(packet);
                break;
            case 'v':// quitter zaapi
                Zaapi_close();
                break;
            case 'V':// Quitter
                Waypoint_quit();
                break;
            case 'w':
                Prisme_close();
                break;
            case 'p':
                Prisme_use(packet);
                break;
        }
    }

    private synchronized void parseSpecialPackets(String packet) {
        switch (packet.charAt(1)) {
            /**
             * @Flow
             * Mimibiotes
             */
            case 'M': // @Flow - Optimisé
                int points = Util.loadPointsByAccount(player.getAccount());
                if (points >= 50) {
                    String[] mim = packet.substring(2).split(";");
                    int baseItem = 0;
                    int skinItem = 0;
                    String verif2 = "";
                    try {
                        String verif = mim[2];

                        int verifLastIndex = verif.lastIndexOf(',');
                        verif2 = verif.substring(0, verifLastIndex);

                    } catch (Exception e) {
                        player.sendText("Utilisez un item avec des statistiques !");
                        return;
                    }

                    try {
                        baseItem = Integer.parseInt(mim[0]);
                        skinItem = Integer.parseInt(mim[1]);
                    } catch (Exception e) {
                        return;
                    }
                    try {
                        int guidT = baseItem;
                        if (World.EchangeItemValue(guidT) > 0) {
                            player.sendText("Impossible de mimibioter cet objet !");
                            return;
                        }
                    } catch (Exception e) {
                    }
                    ObjTemplate targetItem = World.getObjTemplate(baseItem);
                    ObjTemplate itemSkinVerif = World.getObjTemplate(skinItem);
                    if (targetItem.getType() != Constant.ITEM_TYPE_COIFFE
                            && targetItem.getType() != Constant.ITEM_TYPE_CAPE
                            && targetItem.getType() != Constant.ITEM_TYPE_BOUCLIER) {
                        // && targetItem.getType() != Constant.ITEM_TYPE_FAMILIER Pour les familiers #Meow... Useless
                        player.sendText("Vous ne pouvez qu'associer des Capes, Chapeaux et Boucliers.");
                        return;
                    }
                    if (targetItem.getType() >= 2 && targetItem.getType() <= 8 || targetItem.getType() == Constant.ITEM_TYPE_HACHE) {
                        player.sendText("Vous ne pouvez pas mimibioter des armes.");
                        return;
                    }

                    if (targetItem.getType() != itemSkinVerif.getType()) // Merci RedLabel d'avoir abusé le système
                    {
                        player.sendText("Fusion mimibiote impossible !");
                        return;
                    }

                    if (player.hasItemTemplate(baseItem, 1) && player.hasItemTemplate(skinItem, 1)) {
                        //ObjTemplate OM2 = World.getObjTemplate(skinItem);
                        ObjTemplate OM1 = World.getObjTemplate(baseItem);
                        Item objetStats = SQLManager.verifStats(baseItem, verif2, player);
                        if (objetStats == null) {
                            player.sendText("Erreur: Packet corrompu ! Veuillez ré-essayer !");
                            return;
                        }
                        //player.removeByTemplateID(baseItem, 1);
                        player.removeByTemplateID(skinItem, 1);
                        Item obj = Item.createNewMorphItem(skinItem, baseItem, verif2);

                        obj.getStats().setOneStat(9000, 1);//Non echangeable = 9000 @Flow
                        /** Ajout d'une variable pour récupérer l'item stat de départ. Exemple bonus de panoplie **/
                        if (objetStats.getStats().getEffect(616161) > 0) { // Si déjà mimibioté
                            obj.getStats().setOneStat(616161, objetStats.getStats().getEffect(616161));
                        } else {
                            obj.getStats().setOneStat(616161, OM1.getID());
                        }
                        player.removeItem(objetStats.getGuid(), 1, true, true);
                        obj.addTxtStat(970, Integer.toHexString(OM1.getID()));
                        World.addObjet(obj, true);
                        player.addObjet(obj);
                        player.save(true);
                        SocketManager.GAME_SEND_OAKO_PACKET(player, obj);
                        SocketManager.GAME_SEND_Ow_PACKET(player);

                        Util.updatePointsByAccount(player.getAccount(), points - 50);
                        player.sendText("Le service <b>Mimibiote</b> vous a coûté 50 points.");
                    } else {
                        player.sendText("Impossible, nous n'avons pas trouvé l'item dans votre inventaire.");
                    }
                } else {
                    player.sendText("Il vous faut 50 points pour effectuer ceci !");

                }
                break;
            case 'C':
                points = Util.loadPointsByAccount(player.getAccount());
                if (points < 60 && !(player.getAccount().getGmLevel() > 0)) {
                    player.sendText("Vous n'avez pas assez de points, il vous en manque" + (60 - points) + " !");
                } else {
                    try {
                        String realPacket = packet.substring(2);
                        String[] donnees = realPacket.split(";");
                        player.set_colors(Integer.valueOf(donnees[0]), Integer.valueOf(donnees[1]), Integer.valueOf(donnees[2]));
                        if (player.isOnline()) {
                            if (!(player.getAccount().getGmLevel() > 0)) {
                                Util.updatePointsByAccount(player.getAccount(), points - 60);
                                player.send("000C" + (points - 60));
                                player.sendText("Vous avez perdu 60 points suite à votre changement de couleur !");
                            }

                            if (player.getFight() == null) {
                                player.teleport(player.getMap().get_id(), player.get_curCell().getID());
                                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.getMap(), player);
                                player.get_curCell().addPerso(player);
                                SQLManager.SAVE_PERSONNAGE_COLORS(player);
                            }
                        }
                    } catch (Exception e) {
                        player.sendText("Il y a eu une erreur lors de la tentative de changement de couleur !");
                    }
                }
                break;
        }
    }


    private void Zaapi_close() {
        player.Zaapi_close();
    }

    private void Prisme_close() {
        player.Prisme_close();
    }

    private void Prisme_use(String packet) {
        if (player.getDeshonor() >= 2) {
            SocketManager.GAME_SEND_Im_PACKET(player, "183");
            return;
        } else if (!player.usePrisme(packet)) {
            player.sendText("Téléportation étrange ? Un chausson aux pommes avec ça ?");
        }
    }

    private void Zaapi_use(String packet) {
        if (player.getDeshonor() >= 2) {
            SocketManager.GAME_SEND_Im_PACKET(player, "183");
        }
        player.Zaapi_use(packet);
    }

    private void Waypoint_quit() {
        player.stopZaaping();
    }

    private void Waypoint_use(String packet) {
        short id = -1;
        try {
            id = Short.parseShort(packet.substring(2));
        } catch (Exception e) {
        }
        ;
        if (id == -1)
            return;
        player.useZaap(id);
    }

    private void parseGuildPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'B':// Stats
                if (player.get_guild() == null)
                    return;
                Guild G = player.get_guild();
                if (!player.getGuildMember().canDo(Constant.G_BOOST))
                    return;
                switch (packet.charAt(2)) {
                    case 'p':// Prospec
                        if (G.get_Capital() < 1)
                            return;
                        if (G.get_Stats(176) >= 500)
                            return;
                        G.set_Capital(G.get_Capital() - 1);
                        G.upgrade_Stats(176, 1);
                        break;
                    case 'x':// Sagesse
                        if (G.get_Capital() < 1)
                            return;
                        if (G.get_Stats(124) >= 400)
                            return;
                        G.set_Capital(G.get_Capital() - 1);
                        G.upgrade_Stats(124, 1);
                        break;
                    case 'o':// Pod
                        if (G.get_Capital() < 1)
                            return;
                        if (G.get_Stats(158) >= 5000)
                            return;
                        G.set_Capital(G.get_Capital() - 1);
                        G.upgrade_Stats(158, 20);
                        break;
                    case 'k':// Nb Perco
                        if (G.get_Capital() < 10)
                            return;
                        if (G.get_nbrPerco() >= 50)
                            return;
                        G.set_Capital(G.get_Capital() - 10);
                        G.set_nbrPerco(G.get_nbrPerco() + 1);
                        break;
                }
                SQLManager.UPDATE_GUILD(G);
                SocketManager.GAME_SEND_gIB_PACKET(player, player.get_guild()
                        .parsePercotoGuild());
                break;
            case 'b':// Sorts
                if (player.get_guild() == null)
                    return;
                Guild G2 = player.get_guild();
                if (!player.getGuildMember().canDo(Constant.G_BOOST))
                    return;
                int spellID = Integer.parseInt(packet.substring(2));
                if (G2.getSpells().containsKey(spellID)) {
                    if (G2.get_Capital() < 5)
                        return;
                    G2.set_Capital(G2.get_Capital() - 5);
                    G2.boostSpell(spellID);
                    SQLManager.UPDATE_GUILD(G2);
                    SocketManager.GAME_SEND_gIB_PACKET(player, player.get_guild()
                            .parsePercotoGuild());
                } else {
                    GameServer.addToLog("[ERROR]Sort " + spellID + " non trouve.");
                }
                break;
            case 'C':// Creation
                guild_create(packet);
                break;
            case 'f':// Téléportation enclo de guilde
                guild_enclo(packet.substring(2));
                break;
            case 'F':// Retirer percepteur
                guild_remove_perco(packet.substring(2));
                break;
            case 'h':// Téléportation maison de guilde
                guild_house(packet.substring(2));
                break;
            case 'H':// Poser un percepteur
                guild_add_perco();
                break;
            case 'I':// Infos
                guild_infos(packet.charAt(2));
                break;
            case 'J':// Join
                guild_join(packet.substring(2));
                break;
            case 'K':// Kick
                guild_kick(packet.substring(2));
                break;
            case 'P':// Promote
                guild_promote(packet.substring(2));
                break;
            case 'T':// attaque sur percepteur
                guild_perco_join_fight(packet.substring(2));
                break;
            case 'V':// Ferme le panneau de création de guilde
                guild_CancelCreate();
                break;
        }
    }

    private void guild_perco_join_fight(String packet) {
        String PercoID = Integer.toString(Integer.parseInt(packet.substring(1)), 36);

        int TiD = -1;
        try {
            TiD = Integer.parseInt(PercoID);
        } catch (Exception e) {
        }

        Collector perco = World.getPerco(TiD);
        if (perco == null)
            return;
        switch (packet.charAt(0)) {
            case 'J':// Rejoindre
                int FightID = -1;
                try {
                    FightID = perco.get_inFightID();
                } catch (Exception e) {
                }

                short MapID = -1;
                try {
                    MapID = perco.get_mapID();
                } catch (Exception e) {
                }

                int CellID = -1;
                try {
                    CellID = perco.get_cellID();
                } catch (Exception e) {
                }

                if (Config.DEBUG) {
                    GameServer.addToLog("[DEBUG] Percepteur INFORMATIONS : TiD:"
                            + TiD + ", FightID:" + FightID + ", MapID:" + MapID
                            + ", CellID" + CellID);
                }
                if (TiD == -1 || FightID == -1 || MapID == -1 || CellID == -1) {
                    return;
                }
                if (player.getFight() == null && !player.is_away()) {
                    player.setLastMapInfo(player.getCurCarte().get_id(), player.getCurCell().getID());
                    Fight combat = World.getCarte(MapID).getFight(FightID); // FIXME getFight lorsqu'on tente de re-défendre retourne null
                    if (combat != null) {
                        combat.joinPercepteurFight(player, player.getGuid(), TiD);
                    }
                    if (player.getMap().get_id() != MapID) {
                        player.teleport(MapID, CellID);
                    }
                }
                break;
        }
        for (Player z : World.getGuild(perco.get_guildID()).getMembers()) // @Flow FIXME
        {
            if (z == null) continue;
            if (z.isOnline()) {
                SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
                Collector.parseAttaque(z, perco.get_guildID());
                Collector.parseDefense(z, perco.get_guildID());
            }
        }

    }

    private void guild_remove_perco(String packet) {
        if (player.get_guild() == null || player.getFight() != null
                || player.is_away())
            return;
        if (!player.getGuildMember().canDo(Constant.G_POSPERCO))
            return;// On peut le retirer si on a le droit de le poser
        byte IDPerco = Byte.parseByte(packet);
        Collector perco = World.getPerco(IDPerco);
        if (perco == null || perco.get_inFight() > 0)
            return;
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), IDPerco);
        SQLManager.DELETE_PERCO(perco.getGuid());
        perco.DelPerco(perco.getGuid());
        for (Player z : player.get_guild().getMembers()) {
            if (z.isOnline()) {
                SocketManager.GAME_SEND_gITM_PACKET(z,
                        Collector.parsetoGuild(z.get_guild().get_id()));
                String str = "";
                str += "R" + perco.get_N1() + "," + perco.get_N2() + "|";
                str += perco.get_mapID() + "|";
                str += World.getCarte((short) perco.get_mapID()).getX() + "|"
                        + World.getCarte((short) perco.get_mapID()).getY()
                        + "|" + player.getName();
                SocketManager.GAME_SEND_gT_PACKET(z, str);
            }
        }
    }

    private void guild_add_perco() {
        if (player.get_guild() == null || player.getFight() != null
                || player.is_away())
            return;
        short mapID = player.getMap().get_id();
        if (mapID >= 17700 && mapID <= 17746 || mapID == 10812 || mapID >= 26105 && mapID <= 26108) { // Map interdite pour la pose
            player.sendText("Il est interdit de poser un percepteur sur cet carte. Contacter un administrateur si vous souhaitez en connaître la raison.");
            return;
        }
        if (player.getMap().getTempsPourPosePercepteur() > System.currentTimeMillis()) { // Restriction active
            int minutesRestanteAvantPose = (int)(player.getMap().getTempsPourPosePercepteur() - System.currentTimeMillis()) / 60000 ;
            player.sendText("Vous ne pouvez pas poser de percepteur sur cette carte. Il reste " + minutesRestanteAvantPose + " minutes avant de pouvoir poser un percepteur à nouveau sur cette carte.");
            return;
        }
        if (!player.getGuildMember().canDo(Constant.G_POSPERCO))
            return;// Pas le droit de le poser
        if (player.get_guild().getMembers().size() < 1)
            return;// Guilde invalide
        short price = (short) (1000 + 10 * player.get_guild().get_lvl());// Calcul
        // du
        // prix
        // du
        // percepteur
        if (player.get_kamas() < price)// Kamas insuffisants
        {
            SocketManager.GAME_SEND_Im_PACKET(player, "182");
            return;
        }
        if (Collector.GetPercoGuildID(player.getMap().get_id()) > 0)// La
        // carte
        // possède
        // un
        // perco
        {
            SocketManager.GAME_SEND_Im_PACKET(player, "1168;1");
            return;
        }
        if (player.getMap().get_placesStr().length() < 5)// La map ne
        // possède pas
        // de "places"
        {
            SocketManager.GAME_SEND_Im_PACKET(player, "113");
            return;
        }
        if (Collector.CountPercoGuild(player.get_guild().get_id()) >= player
                .get_guild().get_nbrPerco())
            return;// Limite de percepteur
        short random1 = (short) (Formulas.getRandomValue(1, 39));
        short random2 = (short) (Formulas.getRandomValue(1, 71));
        // Ajout du Perco.
        int id = SQLManager.GetNewIDPercepteur();
        Collector perco = new Collector(id, player.getMap().get_id(), player
                .get_curCell().getID(), (byte) 3, player.get_guild().get_id(),
                random1, random2, "", 0, 0);
        World.addPerco(perco);
        SocketManager.GAME_SEND_ADD_PERCO_TO_MAP(player.getMap());
        SQLManager.ADD_PERCO_ON_MAP(id, player.getMap().get_id(), player
                        .get_guild().get_id(), player.get_curCell().getID(), 3,
                random1, random2);
        for (Player z : player.get_guild().getMembers()) {
            if (z != null && z.isOnline()) {
                SocketManager.GAME_SEND_gITM_PACKET(z,
                        Collector.parsetoGuild(z.get_guild().get_id()));
                String str = "";
                str += "S" + perco.get_N1() + "," + perco.get_N2() + "|";
                str += perco.get_mapID() + "|";
                str += World.getCarte((short) perco.get_mapID()).getX() + "|"
                        + World.getCarte((short) perco.get_mapID()).getY()
                        + "|" + player.getName();
                SocketManager.GAME_SEND_gT_PACKET(z, str);
            }
        }
    }

    private void guild_enclo(String packet) {
        if (player.get_guild() == null) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1135");
            return;
        }

        if (player.getFight() != null || player.is_away())
            return;
        short MapID = Short.parseShort(packet);
        MountPark MP = World.getCarte(MapID).getMountPark();
        if (MP.get_guild().get_id() != player.get_guild().get_id()) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1135");
            return;
        }
        int CellID = World.getEncloCellIdByMapId(MapID);
        if (player.hasItemTemplate(9035, 1)) {
            player.removeByTemplateID(9035, 1);
            player.teleport(MapID, CellID);
        } else {
            SocketManager.GAME_SEND_Im_PACKET(player, "1159");
            return;
        }
    }

    private void guild_house(String packet) {
        if (player.get_guild() == null) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1135");
            return;
        }

        if (player.getFight() != null || player.is_away())
            return;
        int HouseID = Integer.parseInt(packet);
        Houses h = World.getHouses().get(HouseID);
        if (h == null)
            return;
        if (player.get_guild().get_id() != h.get_guild_id()) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1135");
            return;
        }
        if (!h.canDo(Constant.H_GTELE)) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1136");
            return;
        }
        if (player.hasItemTemplate(8883, 1)) {
            player.removeByTemplateID(8883, 1);
            player.teleport((short) h.get_mapid(), h.get_caseid());
        } else {
            SocketManager.GAME_SEND_Im_PACKET(player, "1137");
            return;
        }
    }

    private void guild_promote(String packet) {
        if (player.get_guild() == null)
            return; // Si le personnage envoyeur n'a même pas de guilde

        String[] infos = packet.split("\\|");

        int guid = Integer.parseInt(infos[0]);
        int rank = Integer.parseInt(infos[1]);
        byte xpGive = Byte.parseByte(infos[2]);
        int right = Integer.parseInt(infos[3]);

        Player p = World.getPlayer(guid); // Cherche le personnage a qui l'on
        // change les droits dans la mémoire
        GuildMember toChange;
        GuildMember changer = player.getGuildMember();

        // Récupération du personnage à changer, et verification de quelques
        // conditions de base
        if (p == null) // Arrive lorsque le personnage n'est pas chargé dans la
        // mémoire
        {
            int guildId = SQLManager.isPersoInGuild(guid); // Récupère l'id de
            // la guilde du
            // personnage qui
            // n'est pas dans la
            // mémoire

            if (guildId < 0)
                return; // Si le personnage à qui les droits doivent être
            // modifié n'existe pas ou n'a pas de guilde

            if (guildId != player.get_guild().get_id()) // Si ils ne sont pas
            // dans la même guilde
            {
                SocketManager.GAME_SEND_gK_PACKET(player, "Ed");
                return;
            }
            toChange = World.getGuild(guildId).getMember(guid);
        } else {
            if (p.get_guild() == null)
                return; // Si la personne à qui changer les droits n'a pas de
            // guilde
            if (player.get_guild().get_id() != p.get_guild().get_id()) // Si ils
            // ne
            // sont
            // pas
            // de la
            // meme
            // guilde
            {
                SocketManager.GAME_SEND_gK_PACKET(player, "Ea");
                return;
            }

            toChange = p.getGuildMember();
        }

        // Vérifie ce que le personnage changeur à le droit de faire

        if (changer.getRank() == 1) // Si c'est le meneur
        {
            if (changer.getGuid() == toChange.getGuid()) // Si il se modifie lui
            // même, reset tout
            // sauf l'XP
            {
                rank = -1;
                right = -1;
            } else // Si il modifie un autre membre
            {
                if (rank == 1) // Si il met un autre membre "Meneur"
                {
                    changer.setAllRights(2, (byte) -1, 29694); // Met le meneur
                    // "Bras droit"
                    // avec tout les
                    // droits

                    // Défini les droits à mettre au nouveau meneur
                    rank = 1;
                    xpGive = -1;
                    right = 1;
                }
            }
        } else // Sinon, c'est un membre normal
        {
            if (toChange.getRank() == 1) // S'il veut changer le meneur, reset
            // tout sauf l'XP
            {
                rank = -1;
                right = -1;
            } else // Sinon il veut changer un membre normal
            {
                if (!changer.canDo(Constant.G_RANK) || rank == 1) // S'il ne
                    // peut
                    // changer
                    // les rang
                    // ou qu'il
                    // veut
                    // mettre
                    // meneur
                    rank = -1; // "Reset" le rang

                if (!changer.canDo(Constant.G_RIGHT) || right == 1) // S'il ne
                    // peut
                    // changer
                    // les
                    // droits ou
                    // qu'il
                    // veut
                    // mettre
                    // les
                    // droits de
                    // meneur
                    right = -1; // "Reset" les droits

                if (!changer.canDo(Constant.G_HISXP)
                        && !changer.canDo(Constant.G_ALLXP)
                        && changer.getGuid() == toChange.getGuid()) // S'il ne
                    // peut
                    // changer
                    // l'XP de
                    // personne
                    // et qu'il
                    // est la
                    // cible
                    xpGive = -1; // "Reset" l'XP
            }

            if (!changer.canDo(Constant.G_ALLXP) && !changer.equals(toChange)) // S'il
                // n'a
                // pas
                // le
                // droit
                // de
                // changer
                // l'XP
                // des
                // autres
                // et
                // qu'il
                // n'est
                // pas
                // la
                // cible
                xpGive = -1; // "Reset" L'XP
        }

        toChange.setAllRights(rank, xpGive, right);

        SocketManager.GAME_SEND_gS_PACKET(player, player.getGuildMember());

        if (p != null && p.getGuid() != player.getGuid())
            SocketManager.GAME_SEND_gS_PACKET(p, p.getGuildMember());
    }

    private void guild_CancelCreate() {
        SocketManager.GAME_SEND_gV_PACKET(player);
    }

    private void guild_kick(String name) {
        if (player.get_guild() == null)
            return;
        Player P = World.getPersoByName(name);
        int guid = -1, guildId = -1;
        Guild toRemGuild;
        GuildMember toRemMember;
        if (P == null) {
            int infos[] = SQLManager.isPersoInGuild(name);
            guid = infos[0];
            guildId = infos[1];
            if (guildId < 0 || guid < 0)
                return;
            toRemGuild = World.getGuild(guildId);
            toRemMember = toRemGuild.getMember(guid);
        } else {
            toRemGuild = P.get_guild();
            if (toRemGuild == null)// La guilde du personnage n'est pas charger
            // ?
            {
                toRemGuild = World.getGuild(player.get_guild().get_id());// On
                // prend
                // la
                // guilde
                // du
                // perso
                // qui
                // l'éjecte
            }
            toRemMember = toRemGuild.getMember(P.getGuid());
            if (toRemMember == null)
                return;// Si le membre n'est pas dans la guilde.
            if (toRemMember.getGuild().get_id() != player.get_guild().get_id())
                return;// Si guilde différente
        }
        // si pas la meme guilde
        if (toRemGuild.get_id() != player.get_guild().get_id()) {
            SocketManager.GAME_SEND_gK_PACKET(player, "Ea");
            return;
        }
        // S'il n'a pas le droit de kick, et que ce n'est pas lui même la cible
        if (!player.getGuildMember().canDo(Constant.G_BAN)
                && player.getGuildMember().getGuid() != toRemMember.getGuid()) {
            SocketManager.GAME_SEND_gK_PACKET(player, "Ed");
            return;
        }
        // Si différent : Kick
        if (player.getGuildMember().getGuid() != toRemMember.getGuid()) {
            if (toRemMember.getRank() == 1) // S'il veut kicker le meneur
                return;

            toRemGuild.removeMember(toRemMember.getPerso());
            if (P != null)
                P.setGuildMember(null);

            SocketManager.GAME_SEND_gK_PACKET(player, "K" + player.getName()
                    + "|" + name);
            if (P != null && P.isOnline()) {
                SocketManager.GAME_SEND_gK_PACKET(P, "K" + player.getName());
                SocketManager
                        .GAME_SEND_ALTER_GM_PACKET(player.getMap(), player);
            }
        } else// si quitter
        {
            Guild G = player.get_guild();
            if (player.getGuildMember().getRank() == 1
                    && G.getMembers().size() > 1) // Si le meneur veut quitter
            // la guilde mais qu'il
            // reste d'autre joueurs
            {
                // TODO : Envoyer le message qu'il doit mettre un autre membre
                // meneur (Pas vraiment....)
                return;
            }
            G.removeMember(player);
            player.setGuildMember(null);
            // S'il n'y a plus personne
            if (G.getMembers().isEmpty())
                World.removeGuild(G.get_id());
            SocketManager.GAME_SEND_gK_PACKET(player, "K" + name + "|" + name);
            SocketManager.GAME_SEND_ALTER_GM_PACKET(player.getMap(), player);
        }
    }

    private void guild_join(String packet) {
        switch (packet.charAt(0)) {
            case 'R':// Nom perso
                Player P = World.getPersoByName(packet.substring(1));
                if (P == null || player.get_guild() == null) {
                    SocketManager.GAME_SEND_gJ_PACKET(player, "Eu");
                    return;
                }
                if (!P.isOnline()) {
                    SocketManager.GAME_SEND_gJ_PACKET(player, "Eu");
                    return;
                }
                if (P.is_away()) {
                    SocketManager.GAME_SEND_gJ_PACKET(player, "Eo");
                    return;
                }
                if (P.get_guild() != null) {
                    SocketManager.GAME_SEND_gJ_PACKET(player, "Ea");
                    return;
                }
                if (!player.getGuildMember().canDo(Constant.G_INVITE)) {
                    SocketManager.GAME_SEND_gJ_PACKET(player, "Ed");
                    return;
                }
                if (player.get_guild().getMembers().size() >= (40 + player
                        .get_guild().get_lvl()))// Limite membres max
                {
                    SocketManager.GAME_SEND_Im_PACKET(player, "155;"
                            + (40 + player.get_guild().get_lvl()));
                    return;
                }

                player.setInvitation(P.getGuid());
                P.setInvitation(player.getGuid());

                SocketManager
                        .GAME_SEND_gJ_PACKET(player, "R" + packet.substring(1));
                SocketManager.GAME_SEND_gJ_PACKET(P, "r" + player.getGuid() + "|"
                        + player.getName() + "|" + player.get_guild().get_name());
                break;
            case 'E':// ou Refus
                if (packet.substring(1).equalsIgnoreCase(
                        player.getInvitation() + "")) {
                    Player p = World.getPlayer(player.getInvitation());
                    if (p == null)
                        return;// Pas censé arriver
                    SocketManager.GAME_SEND_gJ_PACKET(p, "Ec");
                }
                break;
            case 'K':// Accepte
                if (packet.substring(1).equalsIgnoreCase(
                        player.getInvitation() + "")) {
                    Player p = World.getPlayer(player.getInvitation());
                    if (p == null)
                        return;// Pas censé arriver
                    Guild G = p.get_guild();
                    GuildMember GM = G.addNewMember(player);
                    SQLManager.UPDATE_GUILDMEMBER(GM);
                    player.setGuildMember(GM);
                    player.setInvitation(-1);
                    p.setInvitation(-1);
                    // Packet
                    SocketManager.GAME_SEND_gJ_PACKET(p, "Ka" + player.getName());
                    SocketManager.GAME_SEND_gS_PACKET(player, GM);
                    SocketManager.GAME_SEND_gJ_PACKET(player, "Kj");
                }
                break;
        }
    }

    private void guild_infos(char c) {
        switch (c) {
            case 'B':// Perco
                SocketManager.GAME_SEND_gIB_PACKET(player, player.get_guild()
                        .parsePercotoGuild());
                break;
            case 'F':// Enclos
                SocketManager.GAME_SEND_gIF_PACKET(player,
                        World.parseMPtoGuild(player.get_guild().get_id()));
                break;
            case 'G':// General
                SocketManager.GAME_SEND_gIG_PACKET(player, player.get_guild());
                break;
            case 'H':// House
                SocketManager.GAME_SEND_gIH_PACKET(player,
                        Houses.parseHouseToGuild(player));
                break;
            case 'M':// Members
                SocketManager.GAME_SEND_gIM_PACKET(player, player.get_guild(), '+');
                break;
            case 'T':// Perco
                SocketManager.GAME_SEND_gITM_PACKET(player,
                        Collector.parsetoGuild(player.get_guild().get_id()));
                Collector.parseAttaque(player, player.get_guild().get_id());
                Collector.parseDefense(player, player.get_guild().get_id());
                break;
        }
    }

    private void guild_create(String packet) {
        if (player == null)
            return;
        if (player.get_guild() != null || player.getGuildMember() != null) {
            SocketManager.GAME_SEND_gC_PACKET(player, "Ea");
            return;
        }
        if (player.getFight() != null) {
            SocketManager.GAME_SEND_gV_PACKET(player);
            return;
        }
        try {
            String[] infos = packet.substring(2).split("\\|");
            // base 10 => 36
            String bgID = Integer.toString(Integer.parseInt(infos[0]), 36);
            String bgCol = Integer.toString(Integer.parseInt(infos[1]), 36);
            String embID = Integer.toString(Integer.parseInt(infos[2]), 36);
            String embCol = Integer.toString(Integer.parseInt(infos[3]), 36);
            String name = infos[4];
            if (World.guildNameIsUsed(name)) {
                SocketManager.GAME_SEND_gC_PACKET(player, "Ean");
                return;
            }

            // Validation du nom de la guilde
            String tempName = name.toLowerCase();
            boolean isValid = true;
            // Vérifie d'abord si il contient des termes définit
            if (tempName.length() > 20 || tempName.contains("mj")
                    || tempName.contains("modo") || tempName.contains("admin")) {
                isValid = false;
            }
            // Si le nom passe le test, on vérifie que les caractère entré sont
            // correct.
            if (isValid) {
                int tiretCount = 0;
                for (char curLetter : tempName.toCharArray()) {
                    if (!((curLetter >= 'a' && curLetter <= 'z')
                            || curLetter == '-' || curLetter == ' ')) {
                        isValid = false;
                        break;
                    }
                    if (curLetter == '-') {
                        if (tiretCount >= 2) {
                            isValid = false;
                            break;
                        } else {
                            tiretCount++;
                        }
                    }
                }
            }
            // Si le nom est invalide
            if (!isValid) {
                SocketManager.GAME_SEND_gC_PACKET(player, "Ean");
                return;
            }
            // FIN de la validation
            String emblem = bgID + "," + bgCol + "," + embID + "," + embCol;// 9,6o5nc,2c,0;
            if (World.guildEmblemIsUsed(emblem)) {
                SocketManager.GAME_SEND_gC_PACKET(player, "Eae");
                return;
            }
            if (player.getMap().get_id() == 2196)// Temple de création de
            // guilde
            {
                if (!player.hasItemTemplate(1575, 1))// Guildalogemme
                {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14");
                    return;
                }
                player.removeByTemplateID(1575, 1);
            }
            Guild G = new Guild(player, name, emblem);
            GuildMember gm = G.addNewMember(player);
            gm.setAllRights(1, (byte) 0, 1);// 1 => Meneur (Tous droits)
            player.setGuildMember(gm);// On ajoute le meneur
            World.addGuild(G, true);
            SQLManager.UPDATE_GUILDMEMBER(gm);
            // Packets
            SocketManager.GAME_SEND_gS_PACKET(player, gm);
            SocketManager.GAME_SEND_gC_PACKET(player, "K");
            SocketManager.GAME_SEND_gV_PACKET(player);
            SocketManager.GAME_SEND_ALTER_GM_PACKET(player.getMap(), player);
        } catch (Exception e) {
            SocketManager.GAME_SEND_gV_PACKET(player);
            return;
        }
        ;
    }

    private void parseChanelPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'C':// Changement des Canaux
                Chanels_change(packet);
                break;
        }
    }

    private void Chanels_change(String packet) {
        String chan = packet.charAt(3) + "";
        switch (packet.charAt(2)) {
            case '+':// Ajout du Canal
                player.addChanel(chan);
                break;
            case '-':// Desactivation du canal
                player.removeChanel(chan);
                break;
        }
        SQLManager.SAVE_PERSONNAGE(player, false);
    }

    private void parseMountPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'b':// Achat d'un enclos
                SocketManager.GAME_SEND_R_PACKET(player, "v");// Fermeture du
                // panneau
                MountPark MP = player.getMap().getMountPark();
                Player Seller = World.getPlayer(MP.get_owner());
                if (MP.get_owner() == -1) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "196");
                    return;
                }
                if (MP.get_price() == 0) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "197");
                    return;
                }
                if (player.get_guild() == null) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "1135");
                    return;
                }
                if (player.getGuildMember().getRank() != 1) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "198");
                    return;
                }
                byte enclosMax = (byte) Math
                        .floor(player.get_guild().get_lvl() / 10);
                byte TotalEncloGuild = SQLManager.TotalMPGuild(player.get_guild()
                        .get_id());
                if (TotalEncloGuild >= enclosMax) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "1103");
                    return;
                }
                if (player.get_kamas() < MP.get_price()) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "182");
                    return;
                }
                long NewKamas = player.get_kamas() - MP.get_price();
                player.set_kamas(NewKamas);
                if (Seller != null) {
                    long NewSellerBankKamas = Seller.getBankKamas()
                            + MP.get_price();
                    Seller.setBankKamas(NewSellerBankKamas);
                    if (Seller.isOnline()) {
                        SocketManager.GAME_SEND_MESSAGE(player,
                                "Un enclo a ete vendu a " + MP.get_price() + ".",
                                Config.CONFIG_MOTD_COLOR);
                    }
                }
                MP.set_price(0);// On vide le prix
                MP.set_owner(player.getGuid());
                MP.set_guild(player.get_guild());
                SQLManager.SAVE_MOUNTPARK(MP);
                SQLManager.SAVE_PERSONNAGE(player, true);
                // On rafraichit l'enclo
                for (Player z : player.getMap().getPersos()) {
                    SocketManager.GAME_SEND_Rp_PACKET(z, MP);
                }
                break;

            case 'd':// Demande Description
                Mount_description(packet);
                break;

            case 'n':// Change le nom
                Mount_name(packet.substring(2));
                break;

            case 'r':// Monter sur la dinde
                Mount_ride();
                break;
            case 's':// Vendre l'enclo
                SocketManager.GAME_SEND_R_PACKET(player, "v");// Fermeture du
                // panneau
                int price = Integer.parseInt(packet.substring(2));
                MountPark MP1 = player.getMap().getMountPark();
                if (MP1.get_owner() == -1) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "194");
                    return;
                }
                if (MP1.get_owner() != player.getGuid()) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "195");
                    return;
                }
                MP1.set_price(price);
                SQLManager.SAVE_MOUNTPARK(MP1);
                SQLManager.SAVE_PERSONNAGE(player, true);
                // On rafraichit l'enclo
                for (Player z : player.getMap().getPersos()) {
                    SocketManager.GAME_SEND_Rp_PACKET(z, MP1);
                }
                break;
            case 'v':// Fermeture panneau d'achat
                SocketManager.GAME_SEND_R_PACKET(player, "v");
                break;
            case 'x':// Change l'xp donner a la dinde
                Mount_changeXpGive(packet);
                break;
        }
    }

    private void Mount_changeXpGive(String packet) {
        try {
            int xp = Integer.parseInt(packet.substring(2));
            if (xp < 0)
                xp = 0;
            if (xp > 90)
                xp = 90;
            player.setMountGiveXp(xp);
            SocketManager.GAME_SEND_Rx_PACKET(player);
        } catch (Exception e) {
        }
        ;
    }

    private void Mount_name(String name) {
        if (player.getMount() == null)
            return;
        player.getMount().setName(name);
        SocketManager.GAME_SEND_Rn_PACKET(player, name);
    }

    private void Mount_ride() {
        if (player.getLevel() < 60 || player.getMount() == null
                || !player.getMount().isMountable() || player._isGhosts) {
            SocketManager.GAME_SEND_Re_PACKET(player, "Er", null);
            return;
        }
        player.toogleOnMount();
    }

    private void Mount_description(String packet) {
        int DDid = -1;
        try {
            DDid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
            // on ignore le temps?
        } catch (Exception e) {
        }
        ;
        if (DDid == -1)
            return;
        Mount DD = World.getDragoByID(DDid);
        if (DD == null)
            return;
        SocketManager.GAME_SEND_MOUNT_DESCRIPTION_PACKET(player, DD);
    }

    private void parse_friendPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'A':// Ajouter
                Friend_add(packet);
                break;
            case 'D':// Effacer un ami
                Friend_delete(packet);
                break;
            case 'L':// Liste
                SocketManager.GAME_SEND_FRIENDLIST_PACKET(player);
                break;
            case 'O':
                switch (packet.charAt(2)) {
                    case '-':
                        player.SetSeeFriendOnline(false);
                        SocketManager.GAME_SEND_BN(player);
                        break;
                    case '+':
                        player.SetSeeFriendOnline(true);
                        SocketManager.GAME_SEND_BN(player);
                        break;
                }
                break;
            case 'J': // Wife
                FriendLove(packet);
                break;
        }
    }

    private void FriendLove(String packet) {
        Player Wife = World.getPlayer(player.getWife());
        if (Wife == null)
            return;
        player.RejoindeWife(Wife); // Correcion téléportation mariage par
        // Taparisse
        if (!Wife.isOnline()) {
            if (Wife.get_sexe() == 0)
                SocketManager.GAME_SEND_Im_PACKET(player, "140");
            else
                SocketManager.GAME_SEND_Im_PACKET(player, "139");

            SocketManager.GAME_SEND_FRIENDLIST_PACKET(player);
            return;
        }
        switch (packet.charAt(2)) {
            case 'S':// Teleportation
                if (player.getFight() != null)
                    return;
                else
                    player.meetWife(Wife);
                break;
            case 'C':// Suivre le deplacement
                if (packet.charAt(3) == '+') {// Si lancement de la traque
                    if (player._Follows != null) {
                        player._Follows._Follower.remove(player.getGuid());
                    }
                    SocketManager.GAME_SEND_FLAG_PACKET(player, Wife);
                    player._Follows = Wife;
                    Wife._Follower.put(player.getGuid(), player);
                } else {// On arrete de suivre
                    SocketManager.GAME_SEND_DELETE_FLAG_PACKET(player);
                    player._Follows = null;
                    Wife._Follower.remove(player.getGuid());
                }
                break;
        }
    }

    private void Friend_delete(String packet) {
        if (player == null)
            return;
        int guid = -1;
        switch (packet.charAt(2)) {
            case '%':// Nom de perso
                packet = packet.substring(3);
                Player P = World.getPersoByName(packet);
                if (P == null)// Si P est nul, ou si P est nonNul et P offline
                {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = P.getAccID();

                break;
            case '*':// Pseudo
                packet = packet.substring(3);
                Account C = World.getCompteByPseudo(packet);
                if (C == null) {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = C.getGuid();
                break;
            default:
                packet = packet.substring(2);
                Player Pr = World.getPersoByName(packet);
                if (Pr == null ? true : !Pr.isOnline())// Si P est nul, ou si P est
                // nonNul et P offline
                {
                    SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
                    return;
                }
                guid = Pr.getAccount().getGuid();
                break;
        }
        if (guid == -1 || !account.isFriendWith(guid)) {
            SocketManager.GAME_SEND_FD_PACKET(player, "Ef");
            return;
        }
        account.removeFriend(guid);
    }

    private void Friend_add(String packet) {
        if (player == null)
            return;
        int guid = -1;
        switch (packet.charAt(2)) {
            case '%':// Nom de perso
                packet = packet.substring(3);
                Player P = World.getPersoByName(packet);
                if (P == null ? true : !P.isOnline())// Si P est nul, ou si P est
                // nonNul et P offline
                {
                    SocketManager.GAME_SEND_FA_PACKET(player, "Ef");
                    return;
                }
                guid = P.getAccID();
                break;
            case '*':// Pseudo
                packet = packet.substring(3);
                Account C = World.getCompteByPseudo(packet);
                if (C == null ? true : !C.isOnline()) {
                    SocketManager.GAME_SEND_FA_PACKET(player, "Ef");
                    return;
                }
                guid = C.getGuid();
                break;
            default:
                packet = packet.substring(2);
                Player Pr = World.getPersoByName(packet);
                if (Pr == null ? true : !Pr.isOnline())// Si P est nul, ou si P est
                // nonNul et P offline
                {
                    SocketManager.GAME_SEND_FA_PACKET(player, "Ef");
                    return;
                }
                guid = Pr.getAccount().getGuid();
                break;
        }
        if (guid == -1) {
            SocketManager.GAME_SEND_FA_PACKET(player, "Ef");
            return;
        }
        account.addFriend(guid);
    }

    private void parseGroupPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'A':// Accepter invitation
                group_accept();
                break;

            case 'F':// Suivre membre du groupe PF+GUID
                Group g = player.getGroup();
                if (g == null)
                    return;

                int pGuid = -1;
                try {
                    pGuid = Integer.parseInt(packet.substring(3));
                } catch (NumberFormatException e) {
                    return;
                }
                ;

                if (pGuid == -1)
                    return;

                Player P = World.getPlayer(pGuid);

                if (P == null || !P.isOnline())
                    return;

                if (packet.charAt(2) == '+')// Suivre
                {
                    if (player._Follows != null) {
                        player._Follows._Follower.remove(player.getGuid());
                    }
                    SocketManager.GAME_SEND_FLAG_PACKET(player, P);
                    SocketManager.GAME_SEND_PF(player, "+" + P.getGuid());
                    player._Follows = P;
                    P._Follower.put(player.getGuid(), player);
                } else if (packet.charAt(2) == '-')// Ne plus suivre
                {
                    SocketManager.GAME_SEND_DELETE_FLAG_PACKET(player);
                    SocketManager.GAME_SEND_PF(player, "-");
                    player._Follows = null;
                    P._Follower.remove(player.getGuid());
                }
                break;
            case 'G':// Suivez le tous PG+GUID
                Group g2 = player.getGroup();
                if (g2 == null)
                    return;

                int pGuid2 = -1;
                try {
                    pGuid2 = Integer.parseInt(packet.substring(3));
                } catch (NumberFormatException e) {
                    return;
                }
                ;

                if (pGuid2 == -1)
                    return;

                Player P2 = World.getPlayer(pGuid2);

                if (P2 == null || !P2.isOnline())
                    return;

                if (packet.charAt(2) == '+')// Suivre
                {
                    for (Player T : g2.getPlayers()) {
                        if (T.getGuid() == P2.getGuid())
                            continue;
                        if (T._Follows != null) {
                            T._Follows._Follower.remove(player.getGuid());
                        }
                        SocketManager.GAME_SEND_FLAG_PACKET(T, P2);
                        SocketManager.GAME_SEND_PF(T, "+" + P2.getGuid());
                        T._Follows = P2;
                        P2._Follower.put(T.getGuid(), T);
                    }
                } else if (packet.charAt(2) == '-')// Ne plus suivre
                {
                    for (Player T : g2.getPlayers()) {
                        if (T.getGuid() == P2.getGuid())
                            continue;
                        SocketManager.GAME_SEND_DELETE_FLAG_PACKET(T);
                        SocketManager.GAME_SEND_PF(T, "-");
                        T._Follows = null;
                        P2._Follower.remove(T.getGuid());
                    }
                }
                break;

            case 'I':// inviation
                group_invite(packet);
                break;

            case 'R':// Refuse
                group_refuse();
                break;

            case 'V':// Quitter
                group_quit(packet);
                break;
            case 'W':// Localisation du groupe
                group_locate();
                break;
        }
    }

    private void group_locate() {
        if (player == null)
            return;
        Group g = player.getGroup();
        if (g == null)
            return;
        String str = "";
        boolean isFirst = true;
        for (Player GroupP : player.getGroup().getPlayers()) {
            if (!isFirst)
                str += "|";
            str += GroupP.getMap().getX() + ";" + GroupP.getMap().getY() + ";"
                    + GroupP.getMap().get_id() + ";2;" + GroupP.getGuid() + ";"
                    + GroupP.getName();
            isFirst = false;
        }
        SocketManager.GAME_SEND_IH_PACKET(player, str);
    }

    private void group_quit(String packet) {
        if (player == null)
            return;
        Group g = player.getGroup();
        if (g == null)
            return;
        if (packet.length() == 2)// Si aucun guid est spécifié, alors c'est que
        // le joueur quitte
        {
            g.leave(player);
            SocketManager.GAME_SEND_PV_PACKET(out, "");
            SocketManager.GAME_SEND_IH_PACKET(player, "");
        } else if (g.isChief(player.getGuid()))// Sinon, c'est qu'il kick un
        // joueur du groupe
        {
            int guid = -1;
            try {
                guid = Integer.parseInt(packet.substring(2));
            } catch (NumberFormatException e) {
                return;
            }
            ;
            if (guid == -1)
                return;
            Player t = World.getPlayer(guid);
            g.leave(t);
            SocketManager.GAME_SEND_PV_PACKET(t.getAccount().getGameThread()
                    .getOut(), "" + player.getGuid());
            SocketManager.GAME_SEND_IH_PACKET(t, "");
        }
    }

    private void group_invite(String packet) {
        if (player == null)
            return;
        String name = packet.substring(2);
        Player target = World.getPersoByName(name);
        if (target == null)
            return;
        if (!target.isOnline()) {
            SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(out, "n" + name);
            return;
        }
        if (target.getGroup() != null) {
            SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(out, "a" + name);
            return;
        }
        if (player.getGroup() != null
                && player.getGroup().getPersosNumber() == 8) {
            SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(out, "f");
            return;
        }
        target.setInvitation(player.getGuid());
        player.setInvitation(target.getGuid());
        SocketManager.GAME_SEND_GROUP_INVITATION(out, player.getName(), name);
        SocketManager.GAME_SEND_GROUP_INVITATION(target.getAccount()
                .getGameThread().getOut(), player.getName(), name);
    }

    private void group_refuse() {
        if (player == null)
            return;
        if (player.getInvitation() == 0)
            return;
        player.setInvitation(0);
        SocketManager.GAME_SEND_BN(out);
        Player t = World.getPlayer(player.getInvitation());
        if (t == null)
            return;
        t.setInvitation(0);
        SocketManager.GAME_SEND_PR_PACKET(t);
    }

    private void group_accept() {
        if (player == null)
            return;
        if (player.getInvitation() == 0)
            return;
        Player t = World.getPlayer(player.getInvitation());
        if (t == null)
            return;
        Group g = t.getGroup();
        if (g == null) {
            g = new Group(t, player);
            SocketManager.GAME_SEND_GROUP_CREATE(out, g);
            SocketManager.GAME_SEND_PL_PACKET(out, g);
            SocketManager.GAME_SEND_GROUP_CREATE(t.getAccount().getGameThread()
                    .getOut(), g);
            SocketManager.GAME_SEND_PL_PACKET(t.getAccount().getGameThread()
                    .getOut(), g);
            t.setGroup(g);
            SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(t.getAccount()
                    .getGameThread().getOut(), g);
        } else {
            SocketManager.GAME_SEND_GROUP_CREATE(out, g);
            SocketManager.GAME_SEND_PL_PACKET(out, g);
            SocketManager.GAME_SEND_PM_ADD_PACKET_TO_GROUP(g, player);
            g.addPerso(player);
        }
        player.setGroup(g);
        SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(out, g);
        SocketManager.GAME_SEND_PR_PACKET(t);
    }

    private void parseObjectPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'd':// Supression d'un objet
                Object_delete(packet);
                break;
            case 'D':// Depose l'objet au sol
                Object_drop(packet);
                //player.sendText("Action impossible");
                break;
            case 'M':// Bouger un objet (Equiper/déséquiper) // Associer obvijevan
                String[] infos = packet.substring(2).split("" + (char) 0x0A)[0]
                        .split("\\|");
                int qua = 1;
                int guid = Integer.parseInt(infos[0]);
                int pos = Integer.parseInt(infos[1]);
                if (infos.length > 2) {
                    try {
                        qua = Integer.parseInt(infos[2]);
                    } catch (Exception e) {
                    }
                }
                try {
                    Object_move(player, out, qua, guid, pos);
                } catch (Exception e) {
                    SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("", 0, "@", e.toString());
                }
                break;
            case 'U':// Utiliser un objet (potions)
                Object_use(packet);
                break;
            case 'x':
                Object_obvijevan_desassocier(packet);
                break;
            case 'f':
                Object_obvijevan_feed(packet);
                break;
            case 's':
                Object_obvijevan_changeApparence(packet);
        }
    }

    //@SuppressWarnings("unused")
    private void Object_drop(String packet) {
        // Verification des exploit
        if (Security.isCompromised(packet, player))
            return;

        int guid = -1;
        int qua = -1;
        try {
            guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
            qua = Integer.parseInt(packet.split("\\|")[1]);
        } catch (Exception e) {
        }
        ;
        if (guid == -1 || qua <= 0 || !player.hasItemGuid(guid)
                || player.getFight() != null || player.is_away())
            return;
        Item obj = World.getObjet(guid);
        if (obj == null) return;
        if ((obj.getStats().getEffect(9000) == 1)) {
            player.sendText("Cet objet ne peut être jeté !");
            return;
        }
        if ((obj.getStats().getEffect(252526) > 0)) {
            player.sendText("Cet objet ne peut être jeté ! Il est lié à votre compte !");
            return;
        }
        player.set_curCell(player.get_curCell());
        int cellPosition = Constant.getNearCellidUnused(player);
        if (cellPosition < 0) {
            SocketManager.GAME_SEND_Im_PACKET(player, "1145");
            return;
        }
        if (obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED) {
            obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
            SocketManager.GAME_SEND_OBJET_MOVE_PACKET(player, obj);
            if (obj.getPosition() == Constant.ITEM_POS_ARME
                    || obj.getPosition() == Constant.ITEM_POS_COIFFE
                    || obj.getPosition() == Constant.ITEM_POS_FAMILIER
                    || obj.getPosition() == Constant.ITEM_POS_CAPE
                    || obj.getPosition() == Constant.ITEM_POS_BOUCLIER
                    || obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
                SocketManager.GAME_SEND_ON_EQUIP_ITEM(player.getMap(), player);
        }
        if (qua >= obj.getQuantity()) {
            player.removeItem(guid);
            player.getMap()
                    .getCase(player.get_curCell().getID() + cellPosition)
                    .addDroppedItem(obj);
            obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, guid);
        } else {
            obj.setQuantity(obj.getQuantity() - qua);
            Item obj2 = Item.getCloneObjet(obj, qua);
            obj2.setPosition(Constant.ITEM_POS_NO_EQUIPED);
            player.getMap()
                    .getCase(player.get_curCell().getID() + cellPosition)
                    .addDroppedItem(obj2);
            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, obj);
        }
        SocketManager.GAME_SEND_Ow_PACKET(player);
        SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(player.getMap(), '+', player
                .getMap().getCase(player.get_curCell().getID() + cellPosition)
                .getID(), obj.getTemplate(false).getID(), 0);
        SocketManager.GAME_SEND_STATS_PACKET(player);
    }

    private void Object_use(String packet) {
        int guid = -1;
        int targetGuid = -1;
        short cellID = -1;
        Player Target = null;

        try {
            String[] infos = packet.substring(2).split("\\|");
            guid = Integer.parseInt(infos[0]);
            try {
                targetGuid = Integer.parseInt(infos[1]);
            } catch (Exception e) {
                targetGuid = -1;
            }
            ;
            try {
                cellID = Short.parseShort(infos[2]);
            } catch (Exception e) {
                cellID = -1;
            }
            ;
        } catch (Exception e) {
            return;
        }
        ;
        // Si le joueur n'a pas l'objet
        if (World.getPlayer(targetGuid) != null) {
            Target = World.getPlayer(targetGuid);
        }

        if (!player.hasItemGuid(guid) || player.getFight() != null
                || player.is_away())
            return;
        if (Target != null && (Target.getFight() != null || Target.is_away()))
            return;
        Item obj = World.getObjet(guid);
        if (obj == null)
            return;
        ObjTemplate T = obj.getTemplate(false);
        if (!obj.getTemplate(false).getConditions().equalsIgnoreCase("")
                && !ConditionParser.validConditions(player, obj.getTemplate(false)
                .getConditions()) && obj.getPosition() != Constant.ITEM_POS_BOUCLIER) {
            SocketManager.GAME_SEND_Im_PACKET(player, "119|43");
            return;
        }
        Console.print("\nObject use\n");
        T.applyAction(player, Target, guid, cellID);
        // Objectif quÃƒÂªte : Utiliser l'objet x
        player.confirmObjective(8, T.getID() + "", null);
    }

    /**
     * TODO Trouvé et corrigé problème !
     *
     * @param _perso
     * @param _out
     * @param qua
     * @param guid
     * @param pos
     */
    public static synchronized void Object_move(Player _perso,
                                                GameSendThread _out, int qua, int guid, int pos) {
        if (_out == null) {
            _perso.sendText("Erreur liée à la connexion, veuillez le signaler à Flow");
        }
        if (_perso == null) {
            _perso.sendText("Erreur liée au personnage, veuillez le signaler à Flow");
        }
        try {
            boolean ignore = false; //@Flow
            Item obj = null;
            try {
                obj = World.getObjet(guid);
            } catch (Exception e) {
                SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("", 0, "@", e.toString());
            }
            // LES VERIFS
            if (!_perso.hasItemGuid(guid) || obj == null) // item n'existe pas
                // ou perso n'a pas
                // l'item
                return;
            if (_perso.getFight() != null) // si en combat démarré
            {
                if (_perso.getFight().get_state() != Constant.FIGHT_STATE_PLACE)
                    return;
            }
            if (!Constant.isValidPlaceForItem(obj.getTemplate(false), pos)
                    && pos != Constant.ITEM_POS_NO_EQUIPED) // si mauvaise place
                return;
            int idCompteLie = obj.getStats().getEffect(252526);
            if (idCompteLie > 0 && idCompteLie != _perso.getAccID()) {
                _perso.sendText("Cet item est lié à un compte qui n'est pas le votre. Vous ne pouvez pas équipé cet item.");
                return;
            }
            if (obj.getStats().getEffect(1000000000) == 1) // @Flow - Pardonne moi dieu pour ce code... Heureusement on s'en sert plus pour les nouveaux mimi
            {
                boolean dont_stop = true;
                int number = 1000000001;
                int verif_level = 0;
                while (dont_stop == true) {
                    if (obj.getStats().getEffect(number) == 1) {
                        verif_level = number - 1000000000;
                        dont_stop = false;
                        ignore = true;
                        if (_perso.getLevel() < verif_level) {
                            _perso.sendText("Pour équiper cet item mimibioté, vous devez être niveau " + verif_level + " !");
                            return;
                        }
                    }
                    if (number > 1000000200) {
                        break;
                    }
                    number++;
                }
            }

            if (obj.getStats().getEffect(616161) > 0) // Si mimibioté
            {
                int templateMimibiote = obj.getStats().getEffect(616161);
                ObjTemplate objT = World.getObjTemplate(templateMimibiote);
                if (!objT.getConditions().equalsIgnoreCase("")
                        && !ConditionParser.validConditions(_perso, objT.getConditions()) && ignore == false) {
                    SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43"); // si le
                    // perso
                    // ne
                    // vérifie
                    // pas
                    // les
                    // conditions
                    // diverses
                    return;
                }
                if (objT.getPrestige() >= (_perso.getPrestige() + 1) && ignore == false) {
                    SocketManager.GAME_SEND_MESSAGE(_perso, "Cet équipement est réservé aux joueurs <b>prestige " + obj.getPrestige() + "</b> et vous êtes prestige " + _perso.getPrestige() + ".", Config.CONFIG_MOTD_COLOR);
                    return;
                }
                if (objT.getLevel() > _perso.getLevel() && ignore == false) {// si le
                    // perso
                    // n'a pas
                    // le level
                    SocketManager.GAME_SEND_OAEL_PACKET(_out);
                    return;
                }
                if (objT.getType() == Constant.ITEM_TYPE_BOUCLIER && ignore == false) {
                    Item cur_arme = _perso.getObjetByPos(Constant.ITEM_POS_ARME);
                    if (cur_arme != null && cur_arme.getTemplate(true) != null
                            && cur_arme.getTemplate(true).isTwoHanded()) {
                        _perso.setEquip(true);
                        // Cool on doit enlever le bouclier !
                        _perso.DesequiperItem(cur_arme);
                        SocketManager.GAME_SEND_Im_PACKET(_perso, "078");
                        if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                            SocketManager.GAME_SEND_OT_PACKET(_out, -1);
                    }
                }
                if (pos == Constant.ITEM_POS_ARME && ignore == false) {
                    Item cur_boubou = _perso
                            .getObjetByPos(Constant.ITEM_POS_BOUCLIER);
                    if (cur_boubou != null && obj.getTemplate(true) != null
                            && obj.getTemplate(true).isTwoHanded()) {
                        _perso.setEquip(true);
                        _perso.DesequiperItem(cur_boubou);
                        SocketManager.GAME_SEND_Im_PACKET(_perso, "079");
                        if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                            SocketManager.GAME_SEND_OT_PACKET(_out, -1);
                    }
                }
                // On ne peut équiper 2 items de panoplies identiques, ou 2 Dofus
                // identiques
                if (pos != Constant.ITEM_POS_NO_EQUIPED
                        && (objT.getPanopID() != -1 || objT.getType() == Constant.ITEM_TYPE_DOFUS)
                        && _perso.hasEquiped(objT.getID()) && ignore == false)
                    return;
                // FIN DES VERIFS

            } else { // pas mimibioté

                if (!obj.getTemplate(false).getConditions().equalsIgnoreCase("")
                        && !ConditionParser.validConditions(_perso, obj
                        .getTemplate(false).getConditions()) && ignore == false) {
                    SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43"); // si le
                    // perso
                    // ne
                    // vérifie
                    // pas
                    // les
                    // conditions
                    // diverses
                    return;
                }
                if (obj.getPrestige() >= (_perso.getPrestige() + 1) && ignore == false) {
                    SocketManager.GAME_SEND_MESSAGE(_perso, "Cet équipement est réservé aux joueurs <b>prestige " + obj.getPrestige() + "</b> et vous êtes prestige " + _perso.getPrestige() + ".", Config.CONFIG_MOTD_COLOR);
                    return;
                }
                if (obj.getTemplate(true).getLevel() > _perso.getLevel() && ignore == false) {// si le
                    // perso
                    // n'a pas
                    // le level
                    SocketManager.GAME_SEND_OAEL_PACKET(_out);
                    return;
                }
                if (obj.getTemplate(true).getType() == Constant.ITEM_TYPE_BOUCLIER && ignore == false) {
                    Item cur_arme = _perso.getObjetByPos(Constant.ITEM_POS_ARME);
                    if (cur_arme != null && cur_arme.getTemplate(true) != null
                            && cur_arme.getTemplate(true).isTwoHanded()) {
                        _perso.setEquip(true);
                        // Cool on doit enlever le bouclier !
                        _perso.DesequiperItem(cur_arme);
                        SocketManager.GAME_SEND_Im_PACKET(_perso, "078");
                        if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                            SocketManager.GAME_SEND_OT_PACKET(_out, -1);
                    }
                }
                if (pos == Constant.ITEM_POS_ARME && ignore == false) {
                    Item cur_boubou = _perso
                            .getObjetByPos(Constant.ITEM_POS_BOUCLIER);
                    if (cur_boubou != null && obj.getTemplate(true) != null
                            && obj.getTemplate(true).isTwoHanded()) {
                        _perso.setEquip(true);
                        _perso.DesequiperItem(cur_boubou);
                        SocketManager.GAME_SEND_Im_PACKET(_perso, "079");
                        if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                            SocketManager.GAME_SEND_OT_PACKET(_out, -1);
                    }
                }
                // On ne peut équiper 2 items de panoplies identiques, ou 2 Dofus
                // identiques
                if (pos != Constant.ITEM_POS_NO_EQUIPED
                        && (obj.getTemplate(true).getPanopID() != -1 || obj
                        .getTemplate(true).getType() == Constant.ITEM_TYPE_DOFUS)
                        && _perso.hasEquiped(obj.getTemplate(true).getID()) && ignore == false)
                    return;
                // FIN DES VERIFS
            }

            Item exObj = _perso.getObjetByPos(pos);// Objet a l'ancienne
            // position
            int objGUID = obj.getTemplate(false).getID();
            // CODE OBVI
            if (obj.getTemplate(false).getType() == 113) {
                // LES VERFIS
                if (exObj == null) {// si on place l'obvi sur un emplacement
                    // vide
                    SocketManager.send(_perso, "Im1161");
                    return;
                }
                if (exObj.getObvijevanPos() != 0) {// si il y a déjà un obvi
                    SocketManager.GAME_SEND_BN(_perso);
                    return;
                }
                // FIN DES VERIFS

                exObj.setObvijevanPos(obj.getObvijevanPos()); // L'objet qui
                // était en
                // place a
                // Configtenant
                // un obvi

                _perso.removeItem(obj.getGuid(), 1, false, false); // on enlève
                // l'existance
                // de l'obvi
                // en
                // lui-même
                SocketManager.send(_perso, "OR" + obj.getGuid()); // on le
                // précise
                // au
                // org.area.client

                StringBuilder cibleNewStats = new StringBuilder();
                int t = exObj.getTemplate(false).getID();
                if (t != 8714 && t != 8718 && t != 8638 && t != 8717
                        && t != 8719 && t != 8716 && t != 8725 && t != 8724
                        && t != 8723 && t != 8722 && t != 8721 && t != 8715
                        && t != 8720 && t != 8668 && t != 8667 && t != 8669
                        && t != 8665 && t != 8663 && t != 8664 && t != 8670
                        && t != 8713 && t != 8726 && t != 8727 && t != 8728
                        && t != 8666 && t != 8647 && t != 8642 && t != 8641
                        && t != 8640 && t != 8639 && t != 8643 && t != 8650
                        && t != 8644 && t != 8645 && t != 8648 && t != 8649
                        && t != 8646 && t != 8636 && t != 8630 && t != 8629
                        && t != 8631 && t != 8628 && t != 8619 && t != 8632
                        && t != 8635 && t != 8633 && t != 8634 && t != 8634
                        && t != 8637 && t != 8660 && t != 8657 && t != 8658
                        && t != 8651 && t != 8652 && t != 8656 && t != 8659
                        && t != 8662 && t != 8654 && t != 8653 && t != 8661
                        && t != 8655) {
                    cibleNewStats
                            .append(obj.parseStatsStringSansUserObvi(true))
                            .append(",")
                            .append(exObj.parseStatsStringSansUserObvi());
                    cibleNewStats.append(",3ca#")
                            .append(Integer.toHexString(objGUID))
                            .append("#0#0#0d0+").append(objGUID);
                } else {
                    cibleNewStats
                            .append(obj.parseStatsStringSansUserObvi(true))
                            .append(",")
                            .append(exObj.parseStatsStringSansUserObvi(true));
                    cibleNewStats.append(",3ca#")
                            .append(Integer.toHexString(objGUID))
                            .append("#0#0#0d0+").append(objGUID);
                }
                exObj.clearStats();
                exObj.parseStringToStats(cibleNewStats.toString());

                SocketManager.send(_perso, exObj.obvijevanOCO_Packet(pos));
                SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.getMap(), _perso); // Si
                // l'obvi
                // était
                // cape
                // ou
                // coiffe
                // :
                // packet
                // au
                // org.area.client
                // S'il y avait plusieurs objets
                if (obj.getQuantity() > 1) {
                    if (qua > obj.getQuantity())
                        qua = obj.getQuantity();

                    if (obj.getQuantity() - qua > 0)// Si il en reste #Logique Améliorée
                    {
                        int newItemQua = obj.getQuantity() - qua;
                        Item newItem = Item.getCloneObjet(obj, qua);
                        obj.setQuantity(newItemQua);
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso,
                                obj);
                        _perso.addObjet(newItem, pos == Constant.ITEM_POS_NO_EQUIPED ? true : false);
                        World.addObjet(newItem, true);
                    }
                }

                return; // on s'arrête là pour l'obvi
            } // FIN DU CODE OBVI

            if (exObj != null)// S'il y avait déja un objet sur cette place on
            // déséquipe
            {
                //_perso.sendText("Meow 1");
                _perso.setEquip(true);
                _perso.DesequiperItem(exObj);
                if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                    SocketManager.GAME_SEND_OT_PACKET(_out, -1);
            } else// getNumbEquipedItemOfPanoplie(exObj.getTemplate().getPanopID()
            {
                //_perso.sendText("Meow 2");
                Item obj2;
                // On a un objet similaire
                if ((obj2 = _perso.getSimilarItem(obj)) != null) {
                    //_perso.sendText("Meow 3");
                    if (qua > obj.getQuantity())
                        qua = obj.getQuantity();

                    obj2.setQuantity(obj2.getQuantity() + qua);
                    SocketManager
                            .GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);

                    if (obj.getQuantity() - qua > 0)// Si il en reste
                    {
                        //_perso.sendText("Meow 5");
                        obj.setQuantity(obj.getQuantity() - qua);
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso,
                                obj);
                    } else// Sinon on supprime
                    {
                        //_perso.sendText("Meow 6");
                        World.removeItem(obj.getGuid());
                        _perso.removeItem(obj.getGuid());
                        SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso,
                                obj.getGuid());
                    }
                } else// Pas d'objets similaires
                {
                    Item newItem = null;
                    //_perso.sendText("Meow 7");
                    if (obj.getQuantity() > 1) {
                        if (qua > obj.getQuantity())
                            qua = obj.getQuantity();

                        if (obj.getQuantity() - qua > 0)// Si il en reste
                        {
                            //_perso.sendText("Meow 8");
                            int newItemQua = obj.getQuantity() - qua;
                            newItem = Item.getCloneObjet(obj, qua);
                            obj.setQuantity(newItemQua);
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(
                                    _perso, obj);
                            _perso.addObjet(newItem, pos == Constant.ITEM_POS_NO_EQUIPED ? true : false);
                            World.addObjet(newItem, true);
                        }
                    }
                    if (newItem != null) {
                        obj = null;
                        obj = newItem;
                    }
                    obj.setPosition(pos);
                    SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso, obj);
                }
            }
            if (_perso.CheckItemConditions() != 0) {
                pos = obj.getPosition();
            }
            SocketManager.GAME_SEND_Ow_PACKET(_perso);
            //_perso.sendText("Meow 9");
            _perso.refreshStats();
            if (_perso.getGroup() != null) {
                SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(
                        _perso.getGroup(), _perso);
            }
            SocketManager.GAME_SEND_STATS_PACKET(_perso);
            if (obj.hasSpellBoostStats()) {
                SocketManager.GAME_SEND_SB_PACKET(_perso,
                        obj.getBoostSpellStats(),
                        (pos != Constant.ITEM_POS_NO_EQUIPED) ? true : false);
            }
            if (pos == Constant.ITEM_POS_ARME
                    || pos == Constant.ITEM_POS_COIFFE
                    || pos == Constant.ITEM_POS_FAMILIER
                    || pos == Constant.ITEM_POS_CAPE
                    || pos == Constant.ITEM_POS_BOUCLIER
                    || pos == Constant.ITEM_POS_NO_EQUIPED)
                SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.getMap(), _perso);

            // Si familier
            if (pos == Constant.ITEM_POS_FAMILIER && _perso.isOnMount())
                _perso.toogleOnMount();
            // Nourir dragodinde
            Mount DD = _perso.getMount();
            if (pos == Constant.ITEM_POS_DRAGODINDE) {
                if (_perso.getMount() == null) {
                    SocketManager
                            .GAME_SEND_MESSAGE(
                                    _perso,
                                    "Votre personnage ne possède pas de dragodinde sur lui, il ne peut donc en nourir ...",
                                    Config.CONFIG_MOTD_COLOR);
                } else {
                    if (obj.getTemplate(false).getType() == 41
                            || obj.getTemplate(false).getType() == 63) {
                        int totalwin = qua * 10;
                        if (DD.isInfatiguable() == true)
                            totalwin = qua * 10 * 2;
                        int winEnergie = DD.get_energie() + totalwin;
                        DD.setEnergie(winEnergie);
                        SocketManager.GAME_SEND_Re_PACKET(_perso, "+", DD);
                        _perso.deleteItem(guid);
                        World.removeItem(guid);
                        SocketManager.GAME_SEND_DELETE_STATS_ITEM_FM(_perso,
                                guid);
                        SocketManager.GAME_SEND_MESSAGE(_perso,
                                "Votre dragodinde a gagné " + totalwin
                                        + " en énergie.",
                                Config.CONFIG_MOTD_COLOR);
                    } else {
                        SocketManager.GAME_SEND_MESSAGE(_perso,
                                "Nourriture pour dragodinde incomestible !",
                                Config.CONFIG_MOTD_COLOR);
                    }
                }
            }
            // Verif pour les outils de métier
            if (pos == Constant.ITEM_POS_NO_EQUIPED
                    && _perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
                SocketManager.GAME_SEND_OT_PACKET(_out, -1);

            if (pos == Constant.ITEM_POS_ARME
                    && _perso.getObjetByPos(Constant.ITEM_POS_ARME) != null) {
                int ID = _perso.getObjetByPos(Constant.ITEM_POS_ARME)
                        .getTemplate(true).getID();
                for (Entry<Integer, StatsMetier> e : _perso.getMetiers()
                        .entrySet()) {
                    if (e.getValue().getTemplate().isValidTool(ID))
                        SocketManager.GAME_SEND_OT_PACKET(_out, e.getValue()
                                .getTemplate().getId());
                }
            }
            // Si objet de panoplie
            if (obj.getTemplate(true).getPanopID() > 0)
                SocketManager.GAME_SEND_OS_PACKET(_perso, obj.getTemplate(true)
                        .getPanopID());
            // Si en combat
            SQLManager.SAVE_PERSONNAGE(_perso, true);
            if (_perso.getFight() != null) {
                SocketManager.GAME_SEND_ON_EQUIP_ITEM_FIGHT(_perso, _perso
                        .getFight().getFighterByPerso(_perso), _perso
                        .getFight());
            }
        } catch (Exception e) {
            e.printStackTrace();
            SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
        }
    }

    private void Object_delete(String packet) {
        // Verification des exploit
        if (Security.isCompromised(packet, player))
            return;

        String[] infos = packet.substring(2).split("\\|");
        try {
            int guid = Integer.parseInt(infos[0]);
            int qua = 1;
            try {
                qua = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            Item obj = World.getObjet(guid);
            if (obj == null || !player.hasItemGuid(guid) || qua <= 0
                    || player.getFight() != null || player.is_away()) {
                SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(out);
                return;
            }
            int newQua = obj.getQuantity() - qua;
            if (newQua <= 0) {
                player.removeItem(guid);
                World.removeItem(guid);
                SQLManager.DELETE_ITEM(guid);
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, guid);
            } else {
                obj.setQuantity(newQua);
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, obj);
            }
            SocketManager.GAME_SEND_STATS_PACKET(player);
            SocketManager.GAME_SEND_Ow_PACKET(player);
        } catch (Exception e) {
            SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(out);
        }
    }

    private void parseDialogPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'C':// Demande de l'initQuestion
                Dialog_start(packet, player);
                break;

            case 'R':// Réponse du joueur
                Dialog_response(packet, player);
                break;

            case 'V':// Fin du dialog
                Dialog_end(player);
                break;
        }
    }

    public static void Dialog_response(String packet, Player _perso) {
        String[] infos = packet.substring(2).split("\\|");
        try {
            int qID = Integer.parseInt(infos[0]);
            int rID = Integer.parseInt(infos[1]);
            NPC_question quest = World.getNPCQuestion(qID, _perso);
            NPC_reponse rep = World.getNPCreponse(rID);
            //NpcTemplate npcVerif = World.getNPCTemplate(_perso.get_isTalkingWith());
            // Revu des vérifications (+ intelligente et efficacité 100%)
            ArrayList<Integer> npcWithThisQuestionId = SQLManager.GET_NPCS_WITH_QUESTION_ID(qID);
            boolean valid = false;
            Map<Integer, Integer> npcOnMap = new HashMap<Integer, Integer>();
            for (NPC npc : _perso.getCurCarte().get_npcs().values()) {
                npcOnMap.put(npc.get_template().get_id(), npc.get_guid());
            }
            for (int i = 0; i < npcWithThisQuestionId.size(); i++) {
                if (npcOnMap.containsKey(npcWithThisQuestionId.get(i)) && npcOnMap.get(npcWithThisQuestionId.get(i)) == _perso.get_isTalkingWith()) {
                    valid = true;
                    break;
                }
            }
            if (!valid || !SQLManager.IS_A_GOOD_ANSWER_FOR_QUESTION(qID, rID)) {
                _perso.sendText("Action impossible.");
                return;
            }

            // Quest
            Map<Integer, Map<String, String>> objectives = World.getObjectiveByOptAnswer(rID);
            if (objectives != null) // C'est une rÃƒÂ©ponse de quÃƒÂªte avec au moins 1 objectif
            {
                for (Entry<Integer, Map<String, String>> objective : objectives.entrySet()) {
                    if (_perso.hasObjective(objective.getKey())) // Si le perso ÃƒÂ  cet objectif
                    {
                        qID = Integer.parseInt(objective.getValue().get("optQuestion"));

                        if (_perso.canDoObjective(Integer.parseInt(objective.getValue().get("type")), objective.getValue().get("args"))) {
                            _perso.confirmObjective(Integer.parseInt(objective.getValue().get("type")), objective.getValue().get("args"), null);
                        }
                    }
                }
            }
            // Fin Quetes
            if (quest == null || rep == null || !rep.isAnotherDialog()) {
                SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso.getAccount().getGameThread().getOut());
            }
            rep.apply(_perso);
            _perso.set_isTalkingWith(0);
        } catch (Exception e) {
            SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso.getAccount().getGameThread().getOut());
        }
    }

    public static void Dialog_end(Player _perso) {
        SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso.getAccount().getGameThread().getOut());
        if (_perso.get_isTalkingWith() != 0) {
            _perso.set_isTalkingWith(0);
        }

    }

    public static void Dialog_start(String packet, Player _perso) {
        try {
            int npcID = Integer.parseInt(packet.substring(2).split((char) 0x0A + "")[0]);
            if (npcID <= -50) {
                Collector perco = World.getPerco(npcID);
                if (perco == null) return;
                SocketManager.GAME_SEND_DCK_PACKET(_perso.getAccount().getGameThread().getOut(), npcID);
                SocketManager.GAME_SEND_QUESTION_PACKET(_perso.getAccount().getGameThread().getOut(), perco.getDQ_Packet());
                _perso.set_isTalkingWith(npcID);
            } else {
                _perso.set_isTalkingWith(npcID);
                NPC npc = _perso.getMap().getNPC(npcID);
                if (npc == null) return;
                SocketManager.GAME_SEND_DCK_PACKET(_perso.getAccount().getGameThread().getOut(), npcID);
                // Objectif quÃƒÂªtes : Aller voir x
                _perso.confirmObjective(1, npc.get_template().get_id() + "", null);
                int qID = npc.get_template().get_initQuestionID();
                // Quests
                boolean pendingStep = false;
                // Quest Master
                String quests = npc.get_template().get_quests(); // Quetes du npc
                if (quests != null && !quests.isEmpty() && quests.length() > 0) // Npc a des quetes
                {
                    String[] splitQuests = quests.split(";");
                    for (String curQuest : splitQuests) // Boucle chaque quete du npc
                    {
                        Map<String, String> questPerso = _perso.get_quest(Integer.parseInt(curQuest));
                        Map<String, String> questDetails = World.getQuest(Integer.parseInt(curQuest));
                        if (questPerso != null && (questDetails.get("unique").equals("1") || questPerso.get("finishQuest").equals("0"))) // Le perso ÃƒÂ  la quÃƒÂªte
                        {
                            String curStep = questPerso.get("curStep");
                            if (!curStep.equals("-1")) // Si quete non terminee
                            {
                                Map<String, String> stepDetails = World.getStep(Integer.parseInt(curStep));
                                if (!questPerso.get("objectivesToDo").isEmpty() && !questPerso.get("objectivesToDo").equals(" ")) // Etape en cours
                                {
                                    //pendingStep = true;
                                    qID = Integer.parseInt(stepDetails.get("question"));
                                    break;
                                } else {
                                    String questSteps = questDetails.get("steps"); // Etapes de la quete
                                    if (questSteps.length() < questSteps.indexOf(curStep) + 1 + curStep.length() + 1) {
                                        _perso.upgradeQuest(Integer.parseInt(curQuest));
                                        qID = Integer.parseInt(questDetails.get("endQuestion")); // Question de fin de quete
                                    } else {
                                        qID = Integer.parseInt(stepDetails.get("question"));
                                    }
                                }
                                break;
                            } else { // Quete terminee
                                if (questPerso.get("finishQuest").equals("0")) {
                                    qID = Integer.parseInt(questDetails.get("endQuestion")); // Question de fin de quete
                                }
                                break;
                            }
                        } else {
                            qID = Integer.parseInt(questDetails.get("startQuestion"));
                            if (qID == -1) {
                                qID = npc.get_template().get_initQuestionID();
                            }
                            break;
                        }


                    }
                }
                // Pnj quete secondaire
                int newAnswer = -1;
                Map<Integer, Map<String, String>> objectives = World.getObjectiveByNpcTarget(npc.get_template().get_id());
                if (objectives != null) // Il y a un objectif liÃƒÂ© ÃƒÂ  ce pnj
                {
                    for (Entry<Integer, Map<String, String>> objective : objectives.entrySet()) {
                        int question = Integer.parseInt(objective.getValue().get("optQuestion"));
                        if (_perso.hasObjective(objective.getKey()) && question > 0) // Si le perso doit faire cet objectif
                        {
                            // Le perso ne peut pas accomplir l'objectif
                            if (!_perso.canDoObjective(Integer.parseInt(objective.getValue().get("type")), objective.getValue().get("args"))) {
                                pendingStep = true;
                            } else {
                                pendingStep = false;
                                qID = question;
                                newAnswer = Integer.parseInt(objective.getValue().get("optAnswer"));
                                break;
                            }
                        }
                    }
                }
                // End quests
                NPC_question quest = World.getNPCQuestion(qID);
                if (quest == null) {
                    SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso.getAccount().getGameThread().getOut());
                    return;
                }
                String DQPacket = quest.parseToDQPacket(_perso);
                if (pendingStep) // Etape de quete en cours: on remplace la rÃƒÂ©ponse par "terminer la discussion"
                {
                    DQPacket = DQPacket.split("\\|")[0] + "|4840"; // 4840 = "Terminer la discussion"
                } else if (newAnswer != -1) {
                    DQPacket = DQPacket.split("\\|")[0] + "|" + newAnswer;
                }
                SocketManager.GAME_SEND_QUESTION_PACKET(_perso.getAccount().getGameThread().getOut(), quest.parseToDQPacket(_perso));
                _perso.set_isTalkingWith(npcID);
            }
        } catch (NumberFormatException e) {
        }
        ;
    }

    private void parseExchangePacket(String packet) {
        switch (packet.charAt(1)) {
            case 'A':// Accepter demande d'échange
                Exchange_accept();
                break;
            case 'B':// Achat
                Exchange_onBuyItem(packet);
                break;

            case 'H':// Demande prix moyen + catégorie
                Exchange_HDV(packet);
                break;

            case 'K':// Ok
                Exchange_isOK();
                break;
            case 'L':// jobAction : Refaire le craft précedent
                Exchange_doAgain();
                break;

            case 'M':// Move (Ajouter//retirer un objet a l'échange)
                Exchange_onMoveItem(packet);
                break;

            case 'q':// Mode marchand @Flow - Mode marchant avant taxe
                // Je garde le vieux code on ne sait jamais

			/*if (player.get_isTradingWith() > 0 || player.getFight() != null
                    || player.is_away())
				return;
			if (player.getMap().get_id() != 953)
			{
				player.sendText("Vous ne pouvez passer en mode marchand sur cette map. Veuillez essayer en .marchand !");
				return;
			}
			if (player.getMap().getStoreCount() == 30) { // Anciennement 5
				SocketManager.GAME_SEND_Im_PACKET(player, "125;5");
				return;
			}
			if (player.parseStoreItemsList().isEmpty()) {
				SocketManager.GAME_SEND_Im_PACKET(player, "123");
				return;
			}
			int orientation = Formulas.getRandomValue(1, 3);
			player.set_orientation(orientation);
			Maps map = player.getMap();
			player.set_showSeller(true);
			World.addSeller(player);
			kick();
			for (Player z : map.getPersos()) {
				if (z != null && z.isOnline())
					SocketManager.GAME_SEND_MERCHANT_LIST(z, z.getMap()
							.get_id());

			}*/
                if (player.get_isTradingWith() > 0 || player.getFight() != null || player.is_away()) return;
                if (player.getMap().get_id() != 953) // Map marchand amakna = id 953
                {
                    player.sendText("Vous ne pouvez passer en mode marchand sur cette map. Veuillez essayer en .marchand !");
                    return;
                }
                if (player.getMap().getStoreCount() == 30) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "125;5");
                    return;
                }
                if (player.parseStoreItemsList().isEmpty()) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "123");
                    return;
                }
                //Calcul et envoie du packet pour la taxe
                int Apayer = player.storeAllBuy() / 1000;
                SocketManager.GAME_SEND_Eq_PACKET(player, Apayer);
                break;

            case 'Q'://Mode marchand (Si valider après la taxe)
                player.sendText("Le mode marchand est temporairement désactivé, vous pouvez utilisez l'HDV et retirer les items de votre mode marchand.");
                /*int Apayer2 = player.storeAllBuy() / 1000;

                if (player.get_kamas() < Apayer2) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "176");
                    return;
                }

                int orientation = Formulas.getRandomValue(1, 3);
                player.set_kamas(player.get_kamas() - Apayer2);
                player.set_orientation(orientation);
                Maps map = player.getMap();
                player.set_showSeller(true);
                World.addSeller(player);
                kick();
                for (Player z : map.getPersos()) {
                    if (z != null && z.isOnline())
                        SocketManager.GAME_SEND_MERCHANT_LIST(z, z.getMap().get_id());
                }*/
                break;
            case 'r':// Rides => Monture
                Exchange_mountPark(packet);
                break;

            case 'R':// liste d'achat NPC // demande d'échange
                Exchange_start(packet);
                break;
            case 'S':// Vente
                Exchange_onSellItem(packet);
                break;

            case 'V':// Fin de l'échange
                Exchange_finish_buy();
                break;
            case 'J':// Livre de métiers
                Book_open(packet.substring(3));
                break;
        }
    }

    private void Book_open(String packet) {
        int v = Integer.parseInt(packet);
        if (!player.getMap().hasatelierfor(v))
            return;
        switch (v) {
            case 2:
                for (Entry<Integer, StatsMetier> al : World.upB.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 2, i, al.getValue());
                }
                break;
            case 11:
                for (Entry<Integer, StatsMetier> al : World.upFE.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 11, i, al.getValue());
                }
                break;
            case 13:
                for (Entry<Integer, StatsMetier> al : World.upSA.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 13, i, al.getValue());
                }
                break;
            case 14:
                for (Entry<Integer, StatsMetier> al : World.upFM.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 14, i, al.getValue());
                }
                break;
            case 15:
                for (Entry<Integer, StatsMetier> al : World.upCo.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 15, i, al.getValue());
                }
                break;
            case 16:
                for (Entry<Integer, StatsMetier> al : World.upBi.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 16, i, al.getValue());
                }
                break;
            case 17:
                for (Entry<Integer, StatsMetier> al : World.upFD.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 17, i, al.getValue());
                }
                break;
            case 18:
                for (Entry<Integer, StatsMetier> al : World.upSB.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 18, i, al.getValue());
                }
                break;
            case 19:
                for (Entry<Integer, StatsMetier> al : World.upSBg.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 19, i, al.getValue());
                }
                break;
            case 20:
                for (Entry<Integer, StatsMetier> al : World.upFP.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 20, i, al.getValue());
                }
                break;
            case 24:
                for (Entry<Integer, StatsMetier> al : World.upM.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 24, i, al.getValue());
                }
                break;
            case 25:
                for (Entry<Integer, StatsMetier> al : World.upBou.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 25, i, al.getValue());
                }
                break;
            case 26:
                for (Entry<Integer, StatsMetier> al : World.upAlchi.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 26, i, al.getValue());
                }
                break;
            case 27:
                for (Entry<Integer, StatsMetier> al : World.upT.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 27, i, al.getValue());
                }
                break;
            case 28:
                for (Entry<Integer, StatsMetier> al : World.upP.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 28, i, al.getValue());
                }
                break;
            case 31:
                for (Entry<Integer, StatsMetier> al : World.upFH.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 31, i, al.getValue());
                }
                break;
            case 36:
                for (Entry<Integer, StatsMetier> al : World.upFPc.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 36, i, al.getValue());
                }
                break;
            case 41:
                for (Entry<Integer, StatsMetier> al : World.upC.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 41, i, al.getValue());
                }
                break;
            case 43:
                for (Entry<Integer, StatsMetier> al : World.upFMD.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 43, i, al.getValue());
                }
                break;
            case 44:
                for (Entry<Integer, StatsMetier> al : World.upFME.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 44, i, al.getValue());
                }
                break;
            case 45:
                for (Entry<Integer, StatsMetier> al : World.upFMM.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 45, i, al.getValue());
                }
                break;
            case 46:
                for (Entry<Integer, StatsMetier> al : World.upFMP.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 46, i, al.getValue());
                }
                break;
            case 47:
                for (Entry<Integer, StatsMetier> al : World.upFMH.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 47, i, al.getValue());
                }
                break;
            case 48:
                for (Entry<Integer, StatsMetier> al : World.upSMA.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 48, i, al.getValue());
                }
                break;
            case 49:
                for (Entry<Integer, StatsMetier> al : World.upSMB.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 49, i, al.getValue());
                }
                break;
            case 50:
                for (Entry<Integer, StatsMetier> al : World.upSMBg.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 50, i, al.getValue());
                }
                break;
            case 56:
                for (Entry<Integer, StatsMetier> al : World.upBouc.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 56, i, al.getValue());
                }
                break;
            case 58:
                for (Entry<Integer, StatsMetier> al : World.upPO.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 58, i, al.getValue());
                }
                break;
            case 60:
                for (Entry<Integer, StatsMetier> al : World.upFBou.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 60, i, al.getValue());
                }
                break;
            case 63:
                for (Entry<Integer, StatsMetier> al : World.upJM.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 62, i, al.getValue());
                }
                break;
            case 64:
                for (Entry<Integer, StatsMetier> al : World.upCRM.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 64, i, al.getValue());
                }
                break;
            case 65:
                for (Entry<Integer, StatsMetier> al : World.upBrico.entrySet()) {
                    int i = al.getKey();
                    SocketManager.GAME_SEND_EJ_PACKET(player, 65, i, al.getValue());
                }
                break;
            default:
                return;
        }
    }

    private void Exchange_HDV(String packet) {
        if (player.get_isTradingWith() > 0 || player.getFight() != null
                || player.is_away())
            return;
        int templateID;
        switch (packet.charAt(2)) {
            case 'B': // Confirmation d'achat
                String[] info = packet.substring(3).split("\\|");// ligneID|amount|price

                AuctionHouse curHdv = World.getHdv(Math.abs(player
                        .get_isTradingWith()));

                int ligneID = Integer.parseInt(info[0]);
                byte amount = Byte.parseByte(info[1]);

                // verification des failles
                if (Security.isCompromised(packet, player))
                    return;

                if (curHdv.buyItem(ligneID, amount, Integer.parseInt(info[2]),
                        player)) {
                    SocketManager.GAME_SEND_EHm_PACKET(player, "-", ligneID + "");// Enleve
                    // la
                    // ligne
                    if (curHdv.getLigne(ligneID) != null
                            && !curHdv.getLigne(ligneID).isEmpty())
                        SocketManager.GAME_SEND_EHm_PACKET(player, "+", curHdv
                                .getLigne(ligneID).parseToEHm());// Réajoute la
                    // ligne si elle
                    // n'est pas
                    // vide

				/*
                 * if(curHdv.getLigne(ligneID) != null) { String str =
				 * curHdv.getLigne(ligneID).parseToEHm();
				 * SocketManager.GAME_SEND_EHm_PACKET(_perso,"+",str); }
				 */

                    player.refreshStats();
                    SocketManager.GAME_SEND_Ow_PACKET(player);
                    SocketManager.GAME_SEND_Im_PACKET(player, "068");// Envoie le
                    // message
                    // "Lot acheté"
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "172");// Envoie un
                    // message
                    // d'erreur
                    // d'achat
                }
                break;
            case 'l':// Demande listage d'un template (les prix)
                templateID = Integer.parseInt(packet.substring(3));
                try {
                    SocketManager.GAME_SEND_EHl(player,
                            World.getHdv(Math.abs(player.get_isTradingWith())),
                            templateID);
                } catch (NullPointerException e)// Si erreur il y a, retire le
                // template de la liste chez le
                // org.area.client
                {
                    SocketManager
                            .GAME_SEND_EHM_PACKET(player, "-", templateID + "");
                }

                break;
            case 'P':// Demande des prix moyen
                templateID = Integer.parseInt(packet.substring(3));
                SocketManager.GAME_SEND_EHP_PACKET(player, templateID);
                break;
            case 'T':// Demande des template de la catégorie
                int categ = Integer.parseInt(packet.substring(3));
                String allTemplate = World.getHdv(
                        Math.abs(player.get_isTradingWith())).parseTemplate(categ);
                SocketManager.GAME_SEND_EHL_PACKET(player, categ, allTemplate);
                break;
        }
    }

    private void Exchange_mountPark(String packet) {
        // Si dans un enclos
        if (player.getInMountPark() != null) {
            MountPark MP = player.getInMountPark();
            if (player.get_isTradingWith() > 0 || player.getFight() != null)
                return;
            char c = packet.charAt(2);
            packet = packet.substring(3);
            int guid = -1;
            try {
                guid = Integer.parseInt(packet);
            } catch (Exception e) {
            }
            ;
            switch (c) {
                case 'C':// Parcho => Etable (Stocker)
                    if (guid == -1 || !player.hasItemGuid(guid))
                        return;
                /*if (MP.get_size() <= MP.MountParkDATASize()) { // Une boucle vide, on enlève @Flow
                    //SocketManager.GAME_SEND_Im_PACKET(player, "1145");
					//return;
				}*/
                    Item obj = World.getObjet(guid);
                    // on prend la DD demandée
                    int DDid = obj.getStats().getEffect(995);
                    Mount DD = World.getDragoByID(DDid);
                    // FIXME mettre return au if pour ne pas créer des nouvelles
                    // dindes
                    if (DD == null) {
                        int color = Constant.getMountColorByParchoTemplate(obj
                                .getTemplate(false).getID());
                        if (color < 1)
                            return;
                        DD = new Mount(color);
                    }
                    // On enleve l'objet du Monde et du Perso
                    player.removeItem(guid);
                    World.removeItem(guid);
                    // on ajoute la dinde a l'étable
                    MP.addData(DD.get_id(), player.getGuid());
                    SQLManager.UPDATE_MOUNTPARK(MP);
                    // On envoie les packet
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player,
                            obj.getGuid());
                    SocketManager.GAME_SEND_Ee_PACKET(player, '+', DD.parse());
                    break;
                case 'c':// Etable => Parcho(Echanger)
                    Mount DD1 = World.getDragoByID(guid);
                    // S'il n'a pas la dinde
                    if (DD1 == null || !MP.getData().containsKey(DD1.get_id()))
                        return;
                    if (MP.getData().get(DD1.get_id()) != player.getGuid()
                            && World.getPlayer(MP.getData().get(DD1.get_id()))
                            .get_guild() != player.get_guild()) {
                        // Pas la même guilde, pas le même perso
                        return;
                    }
                    if (MP.getData().get(DD1.get_id()) != player.getGuid()
                            && World.getPlayer(MP.getData().get(DD1.get_id()))
                            .get_guild() == player.get_guild()
                            && !player.getGuildMember().canDo(Constant.G_OTHDINDE)) {
                        // Même guilde, pas le droit
                        SocketManager.GAME_SEND_Im_PACKET(player, "1101");
                        return;
                    }
                    // on retire la dinde de l'étable
                    MP.removeData(DD1.get_id());
                    SQLManager.UPDATE_MOUNTPARK(MP);
                    // On créer le parcho
                    ObjTemplate T = Constant.getParchoTemplateByMountColor(DD1
                            .get_color());
                    Item obj1 = T.createNewItem(1, false, -1);
                    // On efface les stats
                    obj1.clearStats();
                    // on ajoute la possibilité de voir la dinde
                    obj1.getStats().addOneStat(995, DD1.get_id());
                    obj1.addTxtStat(996, player.getName());
                    obj1.addTxtStat(997, DD1.get_nom());

                    // On ajoute l'objet au joueur
                    World.addObjet(obj1, true);
                    player.addObjet(obj1, false);// Ne seras jamais identique de
                    // toute

                    // Packets
                    SocketManager.GAME_SEND_Ow_PACKET(player);
                    SocketManager.GAME_SEND_Ee_PACKET(player, '-', DD1.get_id()
                            + "");
                    break;
                case 'g':// Equiper
                    Mount DD3 = World.getDragoByID(guid);
                    DD3.addXp(1000000000);
                    // S'il n'a pas la dinde
                    if (DD3 == null || !MP.getData().containsKey(DD3.get_id())
                            || player.getMount() != null)
                        return;

                    if (World.getPlayer(MP.getData().get(DD3.get_id())) == null)
                        return;
                    if (MP.getData().get(DD3.get_id()) != player.getGuid()
                            && World.getPlayer(MP.getData().get(DD3.get_id()))
                            .get_guild() != player.get_guild()) {
                        // Pas la même guilde, pas le même perso
                        return;
                    }
                    if (MP.getData().get(DD3.get_id()) != player.getGuid()
                            && World.getPlayer(MP.getData().get(DD3.get_id()))
                            .get_guild() == player.get_guild()
                            && !player.getGuildMember().canDo(Constant.G_OTHDINDE)) {
                        // Même guilde, pas le droit
                        SocketManager.GAME_SEND_Im_PACKET(player, "1101");
                        return;
                    }

                    MP.removeData(DD3.get_id());
                    SQLManager.UPDATE_MOUNTPARK(MP);
                    player.setMount(DD3);

                    // Packets
                    SocketManager.GAME_SEND_Re_PACKET(player, "+", DD3);
                    SocketManager.GAME_SEND_Ee_PACKET(player, '-', DD3.get_id()
                            + "");
                    SocketManager.GAME_SEND_Rx_PACKET(player);
                    break;
                case 'p':// Equipé => Stocker
                    // Si c'est la dinde équipé
                    if (player.getMount() != null ? player.getMount().get_id() == guid
                            : false) {
                        // Si le perso est sur la monture on le fait descendre
                        if (player.isOnMount())
                            player.toogleOnMount();
                        // Si ca n'a pas réussie, on s'arrete là (Items dans le sac
                        // ?)
                        if (player.isOnMount())
                            return;

                        Mount DD2 = player.getMount();
                        MP.addData(DD2.get_id(), player.getGuid());
                        SQLManager.UPDATE_MOUNTPARK(MP);
                        player.setMount(null);

                        // Packets
                        SocketManager.GAME_SEND_Ee_PACKET(player, '+', DD2.parse());
                        SocketManager.GAME_SEND_Re_PACKET(player, "-", null);
                        SocketManager.GAME_SEND_Rx_PACKET(player);
                    } else// Sinon...
                    {

                    }
                    break;
            }
        }
    }

    private void Exchange_doAgain() {
        if (player.getCurJobAction() != null) {
            player.getCurJobAction().putLastCraftIngredients();
        }
    }

    private void Exchange_isOK() {
        if (player.get_echangePNJBoutique()) {
            if (player.getOffrePoints() <= 0) return;
            player.set_echangePNJBoutique(false);
            player.set_isTradingWith(0);
            player.set_away(false);
            SocketManager.GAME_SEND_EXCHANGE_VALID(player.getAccount().getGameThread().getOut(), 'a');
            int points = Util.loadPointsByAccount(player.getAccount());
            Util.updatePointsByAccount(player.getAccount(), points + player.getOffrePoints());
            player.deleteItemQuantity();
            player.sendText("Vous avez obtenu " + player.getOffrePoints() + " points boutique suite à cet échange !");
            player.resetOffrePoints();
        }
        if (player.getCurJobAction() != null && player.getMap().get_id() == 8731) { //@Flow - Fix formagie échange
            // Si pas action de craft, on s'arrete la
            if (!player.getCurJobAction().isCraft()) {
                return;
            }
            player.getCurJobAction().startCraft(player);
        }
        if (player.get_curExchange() == null) {
            return;
        }
        player.get_curExchange().toogleOK(player.getGuid());
    }

    private void Exchange_onMoveItem(String packet) {
        //PNJ Échangeur boutique @Flow
        if (player.get_echangePNJBoutique()) {
            switch (packet.charAt(2)) {
                case 'O':// Objet
                    if (packet.charAt(3) == '+') {
                        String[] infos = packet.substring(4).split("\\|");
                        try {
                            boolean baseFM = false;
                            int guid = Integer.parseInt(infos[0]);
                            int qua = Integer.parseInt(infos[1]);
                            // verification des failles
                            if (Security.isCompromised(packet, player))
                                return;
                            if (!player.hasItemGuid(guid))
                                return;
                            if (player.getItems().get(guid).getQuantity() < qua)
                                return;
                            Item obj = World.getObjet(guid);
                            if (obj == null)
                                return;
                            if (obj.getQuantity() < qua)
                                return;
                            if (qua <= 0)
                                return;
                            int guidT = obj.getTemplate(false).getID();
                            // Item mimibioté
                            if (obj.getStats().getEffect(616161) > 0) {
                                guidT = obj.getStats().getEffect(616161);
                                player.setOffrePoints(50);
                            }
                            if (World.EchangeItemValue(guidT) > 0) {
                                if (obj.getTemplate(true).getStrTemplate().contains("6f#") || obj.getTemplate(true).getStrTemplate().contains("80#")) {
                                    baseFM = true;
                                }
                                // @Flow - Vérifications si c'est un item fm en atelier.
                                if (!baseFM) {
                                    String statsTemplate = obj.getTemplate(true).getStrTemplate();
                                    String[] elementsBase = statsTemplate.split(",");
                                    int nbElementsBase = elementsBase.length;
                                    String stats = obj.convertStatsAString();
                                    //player.sendText(stats);
                                    String[] effectsList = stats.split(",");
                                    int otherThanFM = 0;
                                    for (int i = 0; i < effectsList.length; i++) {
                                        if (effectsList[i].contains("6f#") == false) {
                                            if (effectsList[i].contains("80#") == false) otherThanFM++;
                                        }
                                    }
                                    if (otherThanFM < nbElementsBase - 1) baseFM = true;
                                }
                                player.setOffrePoints(qua * World.EchangeItemValue(guidT));
                                if (!baseFM && obj.getStats().getEffect(111) > 0 || !baseFM && obj.getStats().getEffect(128) > 0) {
                                    player.setOffrePoints(100);
                                    try {
                                        player.itemBonusFM.add(guid);
                                    } catch (Exception e) {
                                    }
                                }
                                SocketManager.GAME_SEND_MESSAGE(player, "Je vous offre " + player.getOffrePoints() + " points pour tous vos équipements, acceptez-vous ?", "0BF9B7");
                            } else {
                                player.sendText("Désolé, je ne collectionne pas cet objet...");
                                return;
                            }
                            player.setItemQuantity(guid, qua);
                            qua = player.getItemQuantity(guid);
                            String str = guid + "|" + qua;
                            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "+", str);
                        } catch (NumberFormatException e) {
                        }
                        ;
                    } else {
                        String[] infos = packet.substring(4).split("\\|");
                        try {
                            int guid = Integer.parseInt(infos[0]);
                            int qua = Integer.parseInt(infos[1]);
                            System.out.println("2");
                            if (qua <= 0)
                                return;
                            if (!player.hasItemGuid(guid))
                                return;
                            if (player.getItems().get(guid).getQuantity() < qua)
                                return;
                            Item obj = World.getObjet(guid);
                            if (obj == null)
                                return;
                            if (obj.getQuantity() < qua)
                                return;
                            int guidT = obj.getTemplate(false).getID();
                            // Si mimibioté
                            if (obj.getStats().getEffect(616161) > 0) {
                                guidT = obj.getStats().getEffect(616161);
                                player.setOffrePoints(-50);
                            }
                            if (World.EchangeItemValue(guidT) > 0) {
                                player.setOffrePoints(-1 * qua * World.EchangeItemValue(guidT));
                                try {
                                    if (player.itemBonusFM.contains(guid)) {
                                        player.setOffrePoints(-100);
                                        //player.sendText("yolo");
                                        int index = player.itemBonusFM.indexOf(guid);
                                        player.itemBonusFM.remove(index);
                                    }
                                } catch (Exception e) {
                                }
                                if (player.getOffrePoints() > 0) {
                                    SocketManager.GAME_SEND_MESSAGE(player, "Je modifie mon offre, je vous offre " + player.getOffrePoints() + " points pour tous vos équipements, acceptez-vous ?", "0BF9B7");
                                } else {
                                    SocketManager.GAME_SEND_MESSAGE(player, "Qu'avez vous à m'offrir ?", "0BF9B7");
                                }
                            } else {
                                player.sendText("Désolé, je ne collectionne pas cet objet...");
                                return;
                            }
                            //int quaItem = player.getItemQuantity(guid);
                            player.setItemQuantity(guid, -1 * qua);
                            int newQua = player.getItemQuantity(guid);
                            if (newQua <= 0) {
                                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "-", "" + guid);
                            } else {
                                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(player, 'O', "+", "" + guid + "|" + newQua);
                            }
                        } catch (NumberFormatException e) {
                        }
                        ;
                    }
                    break;

                case 'G':// Kamas
                    try {
                        player.sendText("Je ne prends pas les kamas cher aventurier !");
                    } catch (NumberFormatException e) {
                    }
                    ;
                    break;
            }
            return;
        }
        /*// Dragodinde (inventaire) @Flow - Useless et source de failles + bug.
        if (player.isInDinde()) {
			Mount drago = player.getMount();
			if (drago == null)
				return;
			switch (packet.charAt(2)) {
			case 'O':// Objet
				int id = 0;
				int qua = 0;
				try {
					id = Integer.parseInt(packet.substring(4).split("\\|")[0]);
					qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);

					// verification des failles
					/** if(Security.isCompromised(packet, _perso)) return; **/
        // Echange impossible ! Bouleto, packet.contains("-"), il y
        // en aura toujours !
                /*
                } catch (Exception e) {
					e.printStackTrace();
				}
				if (id < 0 || qua <= 0)
					return;
				if (World.getObjet(id) == null) {
					SocketManager
							.GAME_SEND_MESSAGE(
									player,
									"Cet objet n'existe pas ou bug, changes de perso pour en être sûr.",
									Config.CONFIG_MOTD_COLOR);
					return;
				}

				switch (packet.charAt(3)) {
				case '+':
					System.out.println("Dinde 1!");
					drago.addObjToSac(id, qua, player);
					//player.sendText("Veuillez utiliser la banque comme inventaire.");
					System.out.println("Dinde 2!");
					break;
				case '-':
					drago.deleteFromSac(id, qua, player);
					break;
				}
				break;
			}
			return;
		}
		*/
        // Store - Mode marchand
        if (player.get_isTradingWith() == player.getGuid()) {
            switch (packet.charAt(2)) {
                case 'O':// Objets
                    if (packet.charAt(3) == '+') {
                        player.sendText("Le mode marchand est temporairement désactivé, vous pouvez utilisez l'HDV et retirer les items de votre mode marchand.");
                       /* String[] infos = packet.substring(4).split("\\|");
                        try {
                            // verification des failles
                            /** if(Security.isCompromised(packet, _perso)) return; **/
                        // Echange impossible ! Bouleto, packet.contains("-"),
                        // il y en aura toujours !
/*
                            int guid = Integer.parseInt(infos[0]);
                            int qua = Integer.parseInt(infos[1]);
                            int price = Integer.parseInt(infos[2]);

                            Item obj = World.getObjet(guid);
                            if (obj == null)
                                return;
                            // Prévention valeurs négatives @Flow
                            if (qua <= 0 || price <= 0)
                                return;
                            if (qua > obj.getQuantity())
                                qua = obj.getQuantity();

                            if ((obj.getStats().getEffect(9000) == 1)) {
                                player.sendText("Cet objet ne peut être échangé");
                                return;
                            }
                            player.addinStore(obj.getGuid(), price, qua);

                        } catch (NumberFormatException e) {
                        }
                        */
                    } else {
                        String[] infos = packet.substring(4).split("\\|");
                        try {
                            int guid = Integer.parseInt(infos[0]);
                            int qua = Integer.parseInt(infos[1]);

                            // verification des failles
                            /** if(Security.isCompromised(packet, _perso)) return; **/
                            // Echange impossible ! Bouleto, packet.contains("-"),
                            // il y en aura toujours !
                            // Un autre code à la con ci-dessus ^^ @Flow

                            if (qua <= 0)
                                return;

                            Item obj = World.getObjet(guid);
                            if (obj == null)
                                return;
                            if (qua > obj.getQuantity())
                                return;
                            if (qua < obj.getQuantity())
                                qua = obj.getQuantity();
                            if ((obj.getStats().getEffect(9000) == 1)) {
                                player.sendText("Cet objet ne peut être échangé");
                                return;
                            }
                            player.removeFromStore(obj.getGuid(), qua);
                        } catch (NumberFormatException e) {
                        }
                        ;
                    }
                    break;
            }
            return;
        }

        // Percepteur
        if (player.get_isOnPercepteurID() != 0) {
            Collector perco = World.getPerco(player.get_isOnPercepteurID());

            if (perco == null || perco.get_inFight() > 0)
                return;

            switch (packet.charAt(2)) {
                case 'G':// Kamas
                    if (packet.charAt(3) == '-') { // On retire
                        long P_Kamas = Integer.parseInt(packet.substring(4));
                        if (P_Kamas < 0)
                            return;
                        if (perco.getKamas() >= P_Kamas) {// FIXME: A tester Faille
                            // non connu ! :p
                            long P_Retrait = perco.getKamas() - P_Kamas;
                            perco.setKamas(perco.getKamas() - P_Kamas);
                            // verification des failles
                            /** if(Security.isCompromised(packet, _perso)) return; **/
                            // Echange impossible ! Bouleto, packet.contains("-"),
                            // il y en aura toujours !
                            if (P_Retrait < 0) {
                                P_Retrait = 0;
                                P_Kamas = perco.getKamas();
                            }
                            perco.setKamas(P_Retrait);
                            player.addKamas(P_Kamas);
                            SocketManager.GAME_SEND_STATS_PACKET(player);
                            SocketManager.GAME_SEND_EsK_PACKET(player,
                                    "G" + perco.getKamas());
                        }
                    }
                    break;

                case 'O':// Objets
                    if (packet.charAt(3) == '-') { // On retire
                        String[] infos = packet.substring(4).split("\\|");
                        int guid = 0;
                        int qua = 0;

                        try {
                            // verification des failles
                            /** if(Security.isCompromised(packet, _perso)) return; **/
                            // Echange impossible ! Bouleto, packet.contains("-"),
                            // il y en aura toujours !

                            guid = Integer.parseInt(infos[0]);
                            qua = Integer.parseInt(infos[1]);
                        } catch (NumberFormatException e) {
                        }
                        ;

                        if (guid <= 0 || qua <= 0)
                            return;

                        Item obj = World.getObjet(guid);
                        if (obj == null)
                            return;

                        if (perco.HaveObjet(guid))
                            perco.removeFromPercepteur(player, guid, qua);

                        perco.LogObjetDrop(guid, obj);
                    }
                    break;
            }
            player.get_guild().addXp(perco.getXp());
            perco.LogXpDrop(perco.getXp());
            perco.setXp(0);
            SQLManager.UPDATE_GUILD(player.get_guild());
            return;
        }
        // HDV
        if (player.get_isTradingWith() < 0) {
            if (player.getNpcExchange() == null) {
                switch (packet.charAt(3)) {
                    case '-':// Retirer un objet de l'HDV
                        int cheapestID = Integer.parseInt(packet.substring(4).split(
                                "\\|")[0]);
                        int count = Integer
                                .parseInt(packet.substring(4).split("\\|")[1]);
                        if (count <= 0)
                            return;

                        // verification des failles
                        /** if(Security.isCompromised(packet, _perso)) return; **/
                        // Echange impossible ! Bouleto, packet.contains("-"), il y en
                        // aura toujours !

                        player.getAccount().recoverItem(cheapestID, count);// Retire
                        // l'objet
                        // de la
                        // liste de
                        // vente du
                        // compte
                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(out, '-', "",
                                cheapestID + "");
                        break;

                    case '+':// Mettre un objet en vente
                        int itmID = Integer
                                .parseInt(packet.substring(4).split("\\|")[0]);
                        byte amount = Byte
                                .parseByte(packet.substring(4).split("\\|")[1]);
                        int price = Integer
                                .parseInt(packet.substring(4).split("\\|")[2]);
                        Item obj = World.getObjet(itmID);// Récupère l'item
                        if (amount <= 0 || price <= 0)
                            return;

                        // verification des failles
                        if (Security.isCompromised(packet, player))
                            return;
                        try {
                            int typeObjet = World.getObjet(itmID).getTemplate(false).getType();
                            if (player.getMap().get_id() == 27002 && World.getObjet(itmID).getTemplate(false).getID() != 470001 && typeObjet == 39) {
                                player.sendText("Vous ne pouvez vendre que des pierres précieuses (ID : " + typeObjet + "!");
                                return;
                            }
                        } catch (Exception E) {
                        }

                        if (obj.getStats().getEffect(252526) > 0) { // Item verouillé à un compte
                            player.sendText("Cet item est lié à votre compte, vous ne pouvez pas le vendre.");
                            return;
                        }

                        AuctionHouse curHdv = World.getHdv(Math.abs(player
                                .get_isTradingWith()));
                        //long taxe = Math.round((double)(price * (curHdv.getTaxe() / 100)));
                        float taxes = Math.round((price * (curHdv.getTaxe() / 100)));

                        if (!player.hasItemGuid(itmID))// Vérifie si le personnage a
                            // bien l'item spécifié et
                            // l'argent pour payer la taxe
                            return;

                        if (player.getAccount().countHdvItems(curHdv.getHdvID()) >= curHdv
                                .getMaxItemCompte()) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "058");
                            return;
                        }

                        if (player.get_kamas() < taxes) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "176");
                            return;
                        }
                        // Changement des vérifications, vraiment mal structuré... @Flow
                        if (amount > obj.getQuantity())// S'il veut mettre plus de cette
                            // objet en vente que ce qu'il
                            // possède

                            return;

                        if ((obj.getStats().getEffect(9000) == 1)) {
                            player.sendText("Cet objet ne peut être échangé");
                            return;
                        }

                        player.addKamas((long) taxes * -1);// Retire le montant de la taxe au
                        // personnage

                        SocketManager.GAME_SEND_STATS_PACKET(player); // Kamas update


                        int rAmount = (int) (Math.pow(10, amount) / 10);
                        int newQua = (obj.getQuantity() - rAmount);

                        if (newQua <= 0) { // Si c'est plusieurs objets ensemble enleve
                            // seulement la quantité de mise en vente
                            player.removeItem(itmID);// Enlève l'item de l'inventaire du
                            // personnage
                            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, itmID);// Envoie
                            // un
                            // packet
                            // au
                            // org.area.client
                            // pour
                            // retirer
                            // l'item
                            // de
                            // son
                            // inventaire
                        } else {
                            obj.setQuantity(obj.getQuantity() - rAmount);
                            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, obj);

                            Item newObj = Item.getCloneObjet(obj, rAmount);
                            World.addObjet(newObj, true);
                            obj = newObj;
                        }

                        HdvEntry toAdd = new HdvEntry(price, amount, player
                                .getAccount().getGuid(), obj);
                        curHdv.addEntry(toAdd); // Ajoute l'entry dans l'HDV
                        SQLManager.SAVE_HDV_ITEM(toAdd, true);

                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(out, '+', "",
                                toAdd.parseToEmK());
                        SocketManager.GAME_SEND_STATS_PACKET(player); // Kamas update
                        break;
                }
            } else {//TODO:Baskwo Exchange with npc
                switch (packet.charAt(3)) {
                    case '+':
                        int oId = Integer.parseInt(packet.substring(4, packet.indexOf("|")));
                        int oQ = Integer.parseInt(packet.split("\\|")[1]);

                        int oQIE = player.getNpcExchange().getQuaItem(oId);

                        if (!player.hasItemGuid(oId)) return;
                        Item obj = World.getObjet(oId);
                        if (obj == null) return;

                        if (oQ > obj.getQuantity() - oQIE)

                            oQ = obj.getQuantity() - oQIE;
                        if (oQ <= 0) return;

                        player.getNpcExchange().addItem(oId, oQ);
                        break;
                    case '-':
                        int Id = Integer.parseInt(packet.substring(4, packet.indexOf("|")));
                        int Q = Integer.parseInt(packet.split("\\|")[1]);

                        if (Q <= 0) return;
                        if (!player.hasItemGuid(Id)) return;

                        Item o = World.getObjet(Id);
                        if (o == null) return;
                        if (Q > player.getNpcExchange().getQuaItem(Id)) return;

                        player.getNpcExchange().removeItem(Id, Q);
                        break;
                }
            }

            return;
        }

		/*// Metier
        if (player.getCurJobAction() != null) {
			// Si pas action de craft, on s'arrete la
			if (!player.getCurJobAction().isCraft()) {
				SocketManager.GAME_SEND_MESSAGE(player,
						"Vous êtes déjà entrain de craft !",
						Config.CONFIG_MOTD_COLOR);
				return;
			}
			if (packet.charAt(2) == 'O')// Ajout d'objet
			{
				if (packet.charAt(3) == '+') {
					// FIXME gerer les packets du genre EMO+173|5+171|5+172|5
					// (split sur '+' ?:/)
					String[] infos = packet.substring(3).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua = Integer.parseInt(infos[1]);
						if (qua <= 0) {
							return;
						}

						// verification des failles
						/** if(Security.isCompromised(packet, _perso)) return; **/
        // Echange impossible ! Bouleto, packet.contains("-"),
        // il y en aura toujours !
/*
                        if (!player.hasItemGuid(guid)) {
							return;
						}
						Item obj = World.getObjet(guid);
						if (obj == null) {
							return;
						}
						if (obj.getQuantity() < qua) {
							qua = obj.getQuantity();
						}
						player.getCurJobAction().modifIngredient(player, guid,
								qua);
					} catch (NumberFormatException e) {
					}
					;
				} else {
					String[] infos = packet.substring(4).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua = Integer.parseInt(infos[1]);

						if (qua <= 0) {
							return;
						}
						Item obj = World.getObjet(guid);
						if (obj == null) {
							return;
						}
						player.getCurJobAction().modifIngredient(player, guid,
								-qua);
					} catch (NumberFormatException e) {
					}
					;
				}

			} else if (packet.charAt(2) == 'R') {
				try {
					int c = Integer.parseInt(packet.substring(3));
					player.getCurJobAction().startRepeat(c, player);
					Logs.addToFmLog("Personnage " + player.getName()
							+ " has started FmRepeat.");
				} catch (Exception e) {
				}
				;
			} else if (packet.charAt(2) == 'r') { // Return | Skryn :D
				try {
					player.getCurJobAction().breakFM();
					Logs.addToFmLog("Personnage " + player.getName()
							+ " has broke Fm in Repeat.");
				} catch (Exception e) {
				}
				;
			}
			return;
		}
		*/

        //Metier @Flow - A revoir

        if (player.getCurJobAction() != null && player.getMap().get_id() == 8731) { // Doit être sur la map de craft, cause conflit avec la banque autrement @Flow
            //Si pas action de craft, on s'arrete la
            if (!player.getCurJobAction().isCraft()) {
                return;
            }
            if (packet.charAt(2) == 'O')//Ajout d'objet
            {
                if (packet.charAt(3) == '+') {
                    //FIXME gerer les packets du genre  EMO+173|5+171|5+172|5 (split sur '+' ?:/)
                    String[] infos = packet.substring(4).split("\\|");
                    try {
                        int guid = Integer.parseInt(infos[0]);
                        int qua = Integer.parseInt(infos[1]);
                        if (qua <= 0) {
                            return;
                        }
                        if (!player.hasItemGuid(guid)) {
                            return;
                        }
                        Item obj = World.getObjet(guid);
                        if (obj == null) {
                            return;
                        }
                        if (obj.getQuantity() < qua) {
                            qua = obj.getQuantity();
                        }
                        player.getCurJobAction().modifIngredient(player, guid, qua);
                    } catch (NumberFormatException e) {
                    }
                    ;
                } else {
                    String[] infos = packet.substring(4).split("\\|");
                    try {
                        int guid = Integer.parseInt(infos[0]);
                        int qua = Integer.parseInt(infos[1]);
                        if (qua <= 0) {
                            return;
                        }
                        Item obj = World.getObjet(guid);
                        if (obj == null) {
                            return;
                        }
                        player.getCurJobAction().modifIngredient(player, guid, -qua);
                    } catch (NumberFormatException e) {
                    }
                }

            } else if (packet.charAt(2) == 'R') {
                try {
                    int c = Integer.parseInt(packet.substring(3));
                    if (!Config.BETA) {
                        player.getCurJobAction().repeat(c, player);
                    } else {
                        player.getCurJobAction().repeatCraft(c, player);
                    }
                } catch (Exception e) {
                }
            } else if (packet.charAt(2) == 'r') {
                if (Config.BETA) {
                    player.getCurJobAction().stopRepeatCraft();
                } else {
                    player.getCurJobAction().breakFM();
                }
            }
            return;
        }
        // Banque
        if (player.isInBank()) {
            if (player.get_curExchange() != null)
                return;
            switch (packet.charAt(2)) {
                case 'G':// Kamas
                    long kamas = 0;
                    try {
                        //kamas = Integer.parseInt(packet.substring(3));
                        kamas = Long.valueOf(packet.substring(3)).longValue();
                    } catch (Exception e) {
                    }
                    ;
                    if (kamas == 0)
                        return;
                    if (kamas > 0) { // Si On ajoute des kamas a la banque
                        // verification des failles
                        /** if(Security.isCompromised(packet, _perso)) return; **/
                        // Echange impossible ! Bouleto, packet.contains("-"), il y
                        // en aura toujours !

                        if (player.get_kamas() < kamas)
                            kamas = player.get_kamas();
                        player.setBankKamas(player.getBankKamas() + kamas);// On
                        // ajoute
                        // les
                        // kamas
                        // a la
                        // banque
                        player.set_kamas(player.get_kamas() - kamas);// On retire
                        // les kamas
                        // du
                        // personnage
                        SocketManager.GAME_SEND_STATS_PACKET(player);
                        SocketManager.GAME_SEND_EsK_PACKET(player,
                                "G" + player.getBankKamas());
                    } else {
                        kamas = -kamas;// On repasse en positif
                        if (player.getBankKamas() < kamas)
                            kamas = player.getBankKamas();
                        player.setBankKamas(player.getBankKamas() - kamas);// On
                        // retire
                        // les
                        // kamas
                        // de la
                        // banque
                        player.set_kamas(player.get_kamas() + kamas);// On ajoute
                        // les kamas
                        // du
                        // personnage
                        SocketManager.GAME_SEND_STATS_PACKET(player);
                        SocketManager.GAME_SEND_EsK_PACKET(player,
                                "G" + player.getBankKamas());
                    }
                    break;

                case 'O':// Objet
                    int guid = 0;
                    int qua = 0;
                    try {
                        guid = Integer
                                .parseInt(packet.substring(4).split("\\|")[0]);
                        qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);

                        // verification des failles
                        /** if(Security.isCompromised(packet, _perso)) return; **/
                        // Echange impossible ! Bouleto, packet.contains("-"), il y
                        // en aura toujours !
                    } catch (Exception e) {
                    }
                    ;
                    if (guid == 0 || qua <= 0 || qua > 100000)
                        return;
                    Item obj = World.getObjet(guid); // Récupération stats
                    if (obj == null) return; // Pourquoi pas ^^ @Flow
                    if ((obj.getStats().getEffect(9000) == 1)) {
                        player.sendText("Cet objet ne peut être échangé");
                        return;
                    }
                    switch (packet.charAt(3)) {
                        case '+':// Ajouter a la banque
                            player.addInBank(guid, qua);
                            break;

                        case '-':// Retirer de la banque
                            player.removeFromBank(guid, qua);
                            break;
                    }
                    break;
            }
            return;
        }
        // Coffre
        if (player.getInTrunk() != null) {
            if (player.get_curExchange() != null)
                return;
            Trunk t = player.getInTrunk();
            if (t == null)
                return;

            switch (packet.charAt(2)) {
                case 'G':// Kamas
                    long kamas = 0;
                    try {
                        kamas = Integer.parseInt(packet.substring(3));

                    } catch (Exception e) {
                    }
                    ;
                    if (kamas == 0)
                        return;

                    if (kamas > 0) { // Si On ajoute des kamas au coffre
                        if (player.get_kamas() < kamas)
                            kamas = player.get_kamas();
                        t.set_kamas(t.get_kamas() + kamas);// On ajoute les kamas au
                        // coffre
                        player.set_kamas(player.get_kamas() - kamas);// On retire
                        // les kamas
                        // du
                        // personnage
                        SocketManager.GAME_SEND_STATS_PACKET(player);
                    } else { // On retire des kamas au coffre
                        kamas = -kamas;// On repasse en positif
                        if (t.get_kamas() < kamas)
                            kamas = t.get_kamas();
                        t.set_kamas(t.get_kamas() - kamas);// On retire les kamas de
                        // la banque
                        player.set_kamas(player.get_kamas() + kamas);// On ajoute
                        // les kamas
                        // du
                        // personnage
                        SocketManager.GAME_SEND_STATS_PACKET(player);
                    }

                    for (Player P : World.getOnlinePlayers())
                        if (P.getInTrunk() != null
                                && player.getInTrunk().get_id() == P.getInTrunk()
                                .get_id())
                            SocketManager.GAME_SEND_EsK_PACKET(P,
                                    "G" + t.get_kamas());
                    SQLManager.UPDATE_TRUNK(t);
                    break;

                case 'O':// Objet
                    int guid = 0;
                    int qua = 0;
                    try {
                        guid = Integer
                                .parseInt(packet.substring(4).split("\\|")[0]);
                        qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);

                        // verification des failles
                        /** if(Security.isCompromised(packet, _perso)) return; **/
                        // Echange impossible ! Bouleto, packet.contains("-"), il y
                        // en aura toujours !
                    } catch (Exception e) {
                    }
                    ;
                    if (guid == 0 || qua <= 0)
                        return;
                    Item obj = World.getObjet(guid);
                    if (obj == null) return;
                    if ((obj.getStats().getEffect(9000) == 1)) {
                        player.sendText("Cet objet ne peut être échangé");
                        return;
                    }
                    switch (packet.charAt(3)) {
                        case '+':// Ajouter a la banque
                            if (obj.getStats().getEffect(252526) > 0) { // Item verouillé au compte
                                player.sendText("Cet item est lié à votre compte, vous ne pouvez pas le déposer dans un coffre.");
                                return;
                            }
                            t.addInTrunk(guid, qua, player);
                            break;

                        case '-':// Retirer de la banque
                            t.removeFromTrunk(guid, qua, player);
                            break;
                    }
                    break;
            }
            return;
        }

        if (player.get_curExchange() == null) // Échange joueur
            return;
        switch (packet.charAt(2)) {
            case 'O':// Objet ?
                if (packet.charAt(3) == '+') {
                    String[] infos = packet.substring(4).split("\\|");
                    try {
                        int guid = Integer.parseInt(infos[0]);
                        int qua = Integer.parseInt(infos[1]);
                        // verification des failles
                        if (Security.isCompromised(packet, player))
                            return;
                        int quaInExch = player.get_curExchange().getQuaItem(guid,
                                player.getGuid());

                        if (!player.hasItemGuid(guid))
                            return;
                        if (player.getItems().get(guid).getQuantity() < qua)
                            return;
                        Item obj = World.getObjet(guid);
                        if (obj == null)
                            return;
                        if (obj.getQuantity() < qua)
                            return;
                        if (qua > obj.getQuantity() - quaInExch)
                            qua = obj.getQuantity() - quaInExch;
                        if (qua <= 0)
                            return;
                        if (obj.getStats().getEffect(252526) > 0) { // Item verouillé à un compte
                            player.sendText("Cet item est lié à votre compte, vous ne pouvez pas l'échanger.");
                            return;
                        }
                        player.get_curExchange().addItem(guid, qua,
                                player.getGuid());
                    } catch (NumberFormatException e) {
                    }
                    ;
                } else {
                    String[] infos = packet.substring(4).split("\\|");
                    try {
                        int guid = Integer.parseInt(infos[0]);
                        int qua = Integer.parseInt(infos[1]);

                        // verification des failles
                        // System.out.println(Security.isCompromised(packet,
                        // _perso)); //Already is TRUE contains '-' !
                        /**
                         * if(Security.isCompromised(packet, _perso)) // Echange
                         * impossible ! Bouleto, packet.contains("-"), il y en aura
                         * toujours ! return;
                         **/
                        System.out.println("2");
                        if (qua <= 0)
                            return;
                        if (!player.hasItemGuid(guid))
                            return;
                        if (player.getItems().get(guid).getQuantity() < qua)
                            return;
                        Item obj = World.getObjet(guid);
                        if (obj == null)
                            return;
                        if (obj.getQuantity() < qua)
                            return;
                        if (qua > player.get_curExchange().getQuaItem(guid,
                                player.getGuid()))
                            return;
                        player.get_curExchange().removeItem(guid, qua,
                                player.getGuid());
                    } catch (NumberFormatException e) {
                    }
                    ;
                }
                break;

            case 'G':// Kamas
                try {
                    long numb = Long.valueOf(packet.substring(3)).longValue(); // @Flow - Algatron = dumb, convertir un string en int pour le déclarer en long ? What about high value ? #Fixé
                    if (player.get_kamas() < numb) {
                        numb = player.get_kamas();
                    }
                    player.get_curExchange().setKamas(player.getGuid(), numb);
                } catch (NumberFormatException e) {
                }
                break;
        }
    }

    private void Exchange_accept() {
        if (player.get_isTradingWith() == 0)
            return;
        Player target = World.getPlayer(player.get_isTradingWith());
        if (target == null)
            return;
        SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(out, 1);
        SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(target.getAccount()
                .getGameThread().getOut(), 1);
        World.Exchange echg = new World.Exchange(target, player);
        player.setCurExchange(echg);
        player.set_isTradingWith(target.getGuid());
        target.setCurExchange(echg);
        target.set_isTradingWith(player.getGuid());
    }

    private void Exchange_onSellItem(String packet) {
        try {
            String[] infos = packet.substring(2).split("\\|");
            int guid = Integer.parseInt(infos[0]);
            int qua = Integer.parseInt(infos[1]);
            if (!player.hasItemGuid(guid)) {
                SocketManager.GAME_SEND_SELL_ERROR_PACKET(out);
                return;
            }
            player.sellItem(guid, qua);
        } catch (Exception e) {
            SocketManager.GAME_SEND_SELL_ERROR_PACKET(out);
        }
    }

    private void Exchange_onBuyItem(String packet) {
        String[] infos = packet.substring(2).split("\\|");

        if (player.get_isTradingWith() > 0) {
            Player seller = World.getPlayer(player.get_isTradingWith());
            if (seller != null) {
                int itemID = 0;
                int qua = 0;
                int price = 0;
                try {
                    itemID = Integer.valueOf(infos[0]);
                    qua = Integer.valueOf(infos[1]);
                } catch (Exception e) {
                    return;
                }

                if (!seller.getStoreItems().containsKey(itemID) || qua <= 0) {
                    SocketManager.GAME_SEND_BUY_ERROR_PACKET(out);
                    return;
                }
                price = seller.getStoreItems().get(itemID);
                Item itemStore = World.getObjet(itemID);
                if (itemStore == null)
                    return;

                if (qua > itemStore.getQuantity()) {
                    qua = itemStore.getQuantity();
                }
                int prixTotal = Math.abs(qua * price);
                if (player.get_kamas() < prixTotal) {
                    SocketManager.GAME_SEND_BUY_ERROR_PACKET(out);
                    return;
                }
                if (qua == itemStore.getQuantity()) { // si il achète tous les items du même template
                    seller.getStoreItems().remove(itemStore.getGuid());
                    player.addObjet(itemStore, true);
                } else {
                    seller.getStoreItems().remove(itemStore.getGuid());
                    itemStore.setQuantity(itemStore.getQuantity() - qua);
                    SQLManager.SAVE_ITEM(itemStore);
                    seller.addStoreItem(itemStore.getGuid(), price);

                    Item clone = Item.getCloneObjet(itemStore, qua);
                    World.addObjet(clone, true);
                    SQLManager.SAVE_NEW_ITEM(clone);
                    player.addObjet(clone, true);
                }

                player.addKamas(-1 * (prixTotal));
                seller.addKamas(prixTotal);
                SQLManager.SAVE_PERSONNAGE(seller, true);
                SQLManager.SAVE_PERSONNAGE(player, true);
                SocketManager.GAME_SEND_STATS_PACKET(player);
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, player);
                SocketManager.GAME_SEND_BUY_OK_PACKET(out);
                if (seller.getStoreItems().isEmpty()) {
                    if (World.getSeller(seller.getMap().get_id()) != null
                            && World.getSeller(seller.getMap().get_id())
                            .contains(seller.getGuid())) {
                        World.removeSeller(seller.getGuid(), seller.getMap()
                                .get_id());
                        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(
                                seller.getMap(), seller.getGuid());
                        Exchange_finish_buy();
                    }
                }
                for (int i = 0; i < 3; i++) {
                    if (SQLManager.SAVE_PERSONNAGE_ITEM(seller)) break;
                }
            }
            return;
        }

        try {
            int tempID = Integer.parseInt(infos[0]);
            int qua = Integer.parseInt(infos[1]);

            // verification des failles
            if (Security.isCompromised(packet, player))
                return;

            // Variable idPNJ
            int idPnj = player.getMap().getNPC(player.get_isTradingWith()).get_template().get_id();

            if (qua <= 0)
                return;

            ObjTemplate template = World.getObjTemplate(tempID);
            if (template == null)// Si l'objet demandé n'existe pas(ne devrait
            // pas arrivé)
            {
                GameServer.addToLog(player.getName()
                        + " tente d'acheter l'itemTemplate " + tempID
                        + " qui est inexistant");
                SocketManager.GAME_SEND_BUY_ERROR_PACKET(out);
                return;
            }
            if (!player.getMap().getNPC(player.get_isTradingWith())
                    .get_template().haveItem(tempID))// Si le PNJ ne vend pas
            // l'objet voulue
            {
                GameServer.addToLog(player.getName()
                        + " tente d'acheter l'itemTemplate " + tempID
                        + " que le present PNJ ne vend pas");
                SocketManager.GAME_SEND_BUY_ERROR_PACKET(out);
                return;
            }
            long prix = LongMath.checkedMultiply(template.getPrix(), qua); // ArithmeticException en cas d'erreur, donc échec achat
            //int prix = Math.abs(template.getPrix() * qua); // Va recommencer à Long.MIN_VALUE, mauvais car le test condition est prix <= kamas du joueur

            if (idPnj == 30226 || idPnj > 30233 && idPnj < 30238 || idPnj == 50031) {
                int prixPierres = Ints.checkedCast(prix); // Cas d'erreur: IllegalArgumentException
                if (!player.hasItemTemplate(470001, prixPierres)) {
                    player.sendText("Vous n'avez pas assez de pierres précieuses pour effectuer cet achat !");
                    return;
                } else {
                    Item newObj = template.createNewItem(qua, false, -1);
                    player.removeByTemplateID(470001, prixPierres);
                    if (player.addObjet(newObj, true))
                        World.addObjet(newObj, true);
                    SocketManager.GAME_SEND_BUY_OK_PACKET(out);
                    SocketManager.GAME_SEND_STATS_PACKET(player);
                    SocketManager.GAME_SEND_Ow_PACKET(player);
                    return;
                }
            } else if (idPnj == 50029) {
                int prixKoli = Ints.checkedCast(prix);
                if (!player.hasItemTemplate(11022, prixKoli)) {
                    player.sendText("Vous n'avez pas assez de Kolizeton pour effectuer cet achat !");
                    return;
                } else {
                    Item newObj = template.createNewItem(qua, false, -1);
                    player.removeByTemplateID(11022, prixKoli);
                    if (player.addObjet(newObj, true))
                        World.addObjet(newObj, true);
                    SocketManager.GAME_SEND_BUY_OK_PACKET(out);
                    SocketManager.GAME_SEND_STATS_PACKET(player);
                    SocketManager.GAME_SEND_Ow_PACKET(player);
                    return;
                }
            } else if (idPnj == 816) {
                int prixObj = Ints.checkedCast(prix);
                if (!player.hasItemTemplate(1749, prixObj)) {
                    player.sendText("Vous n'avez pas assez de jeton d'event pour effectuer cet achat !");
                    return;
                } else {
                    Item newObj = template.createNewItem(qua, false, -1);
                    player.removeByTemplateID(1749, prixObj);
                    if (player.addObjet(newObj, true))
                        World.addObjet(newObj, true);
                    SocketManager.GAME_SEND_BUY_OK_PACKET(out);
                    SocketManager.GAME_SEND_STATS_PACKET(player);
                    SocketManager.GAME_SEND_Ow_PACKET(player);
                    return;
                }
            } else if (player.get_kamas() < prix)// Si le joueur n'a pas assez de kamas et que le npc demande des kamas
            {
                GameServer.addToLog(player.getName()
                        + " tente d'acheter l'itemTemplate " + tempID
                        + " mais n'a pas l'argent necessaire");
                SocketManager.GAME_SEND_BUY_ERROR_PACKET(out);
                return;
            }

            Item newObj = template.createNewItem(qua, false, -1);
            long newKamas = player.get_kamas() - prix;
            player.set_kamas(newKamas);
            if (player.addObjet(newObj, true))// Return TRUE si c'est un nouvel
                // item
                World.addObjet(newObj, true);
            SocketManager.GAME_SEND_BUY_OK_PACKET(out);
            SocketManager.GAME_SEND_STATS_PACKET(player);
            SocketManager.GAME_SEND_Ow_PACKET(player);
        } catch (Exception e) {
            e.printStackTrace();
            SocketManager.GAME_SEND_BUY_ERROR_PACKET(out);
            return;
        }
    }

    private void Exchange_finish_buy() {
        if (player.get_echangePNJBoutique()) {
            player.resetItemQuantity();
            player.set_echangePNJBoutique(false);
            player.set_isTradingWith(0);
            player.set_away(false);
            player.resetOffrePoints();
            player.itemBonusFM.clear();
            SocketManager.GAME_SEND_EV_PACKET(out);
            return;
        }
        if (player.get_isTradingWith() == 0 && player.get_curExchange() == null
                && player.getCurJobAction() == null
                && player.getInMountPark() == null && !player.isInBank()
                && player.get_isOnPercepteurID() == 0
                && player.getInTrunk() == null)
            return;

        // Si échange avec un personnage
        if (player.get_curExchange() != null) {
            player.get_curExchange().cancel();
            player.set_isTradingWith(0);
            player.set_away(false);
            return;
        }
        // Si métier
        if (player.getCurJobAction() != null) {
            player.getCurJobAction().resetCraft();
        }
        // Si dans un enclos
        if (player.getInMountPark() != null)
            player.leftMountPark();
        // prop d'echange avec un joueur
        if (player.get_isTradingWith() > 0) {
            Player p = World.getPlayer(player.get_isTradingWith());
            if (p != null) {
                if (p.isOnline()) {
                    GameSendThread out = p.getAccount().getGameThread()
                            .getOut();
                    SocketManager.GAME_SEND_EV_PACKET(out);
                    p.set_isTradingWith(0);
                }
            }
        }
        // Si perco
        if (player.get_isOnPercepteurID() != 0) {
            Collector perco = World.getPerco(player.get_isOnPercepteurID());
            if (perco == null)
                return;
            for (Player z : World.getGuild(perco.get_guildID()).getMembers()) {
                if (z == null)
                    continue;
                if (z.isOnline()) {
                    SocketManager.GAME_SEND_gITM_PACKET(z,
                            Collector.parsetoGuild(z.get_guild().get_id()));
                    String str = "";
                    str += "G" + perco.get_N1() + "," + perco.get_N2();
                    str += "|.|"
                            + World.getCarte((short) perco.get_mapID()).getX()
                            + "|"
                            + World.getCarte((short) perco.get_mapID()).getY()
                            + "|";
                    str += player.getName() + "|";
                    str += perco.get_LogXp();
                    str += perco.get_LogItems();
                    SocketManager.GAME_SEND_gT_PACKET(z, str);
                }
            }
            player.getMap().RemoveNPC(perco.getGuid());
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(),
                    perco.getGuid());
            perco.DelPerco(perco.getGuid());
            SQLManager.DELETE_PERCO(perco.getGuid());
            player.set_isOnPercepteurID(0);
        }

        SocketManager.GAME_SEND_EV_PACKET(out);
        SQLManager.SAVE_PERSONNAGE(player, true);
        player.set_isTradingWith(0);
        player.set_away(false);
        player.setInBank(false);
        player.setInTrunk(null);
    }

    private void Exchange_start(String packet) {
        if (packet.substring(2, 4).equals("11"))// Ouverture HDV achat
        {
            if (player.get_isTradingWith() < 0)// Si déjà ouvert
                SocketManager.GAME_SEND_EV_PACKET(out);

            if (player.getDeshonor() >= 5) {
                SocketManager.GAME_SEND_Im_PACKET(player, "183");
                return;
            }

            AuctionHouse toOpen = World.getHdv(player.getMap().get_id());

            if (toOpen == null)
                return;

            String info = "1,10,100;" + toOpen.getStrCategories() + ";"
                    + toOpen.parseTaxe() + ";" + toOpen.getLvlMax() + ";"
                    + toOpen.getMaxItemCompte() + ";-1;" + toOpen.getSellTime();
            SocketManager.GAME_SEND_ECK_PACKET(player, 11, info);
            player.set_isTradingWith(0 - player.getMap().get_id()); // Récupère
            // l'ID
            // de
            // la
            // map
            // et
            // rend
            // cette
            // valeur
            // négative
            return;
        } else if (packet.substring(2, 4).equals("10"))// Ouverture HDV vente
        {
            if (player.get_isTradingWith() < 0)// Si déjà ouvert
                SocketManager.GAME_SEND_EV_PACKET(out);

            if (player.getDeshonor() >= 5) {
                SocketManager.GAME_SEND_Im_PACKET(player, "183");
                return;
            }

            AuctionHouse toOpen = World.getHdv(player.getMap().get_id());

            if (toOpen == null)
                return;

            String info = "1,10,100;" + toOpen.getStrCategories() + ";"
                    + toOpen.parseTaxe() + ";" + toOpen.getLvlMax() + ";"
                    + toOpen.getMaxItemCompte() + ";-1;" + toOpen.getSellTime();
            SocketManager.GAME_SEND_ECK_PACKET(player, 10, info);
            player.set_isTradingWith(0 - player.getMap().get_id()); // Récupère
            // l'ID
            // de
            // la
            // map
            // et
            // rend
            // cette
            // valeur
            // négative

            SocketManager.GAME_SEND_HDVITEM_SELLING(player);
            return;
        } else if (packet.substring(2, 4).equals("15")) {// Dinde (inventaire)
            try {
                Mount mount = player.getMount();
                int mountID = mount.get_id();
                SocketManager.GAME_SEND_ECK_PACKET(out, 15, player.getMount()
                        .get_id() + "");
                SocketManager.GAME_SEND_EL_MOUNT_INVENTAIRE(out, mount);
                SocketManager.GAME_SEND_MOUNT_PODS(player,
                        mount.getPodsActuels());
                player.set_isTradingWith(mountID);
                player.setInDinde(true);
                player.set_away(true);
            } catch (Exception e) {
            }
            return;
        }
        switch (packet.charAt(2)) {
            case '0':// Si NPC
                try {
                    int npcID = Integer.parseInt(packet.substring(4));
                    NpcTemplate.NPC npc = player.getMap().getNPC(npcID);


                    if (npc == null)
                        return;

                    SocketManager.GAME_SEND_ECK_PACKET(out, 0, npcID + "");
                    SocketManager.GAME_SEND_ITEM_VENDOR_LIST_PACKET(player, npc);
                    player.set_isTradingWith(npcID);
                    int idPnj = player.getMap().getNPC(player.get_isTradingWith()).get_template().get_id(); // je récupère la vrai id du pnj et non l'id qu'il a sur la map comme le fait NpcID
                    if (idPnj == 30226 || idPnj > 30233 && idPnj < 30238 || idPnj == 50031) // message notif pierres précieuses @Flow
                    {
                        SocketManager.GAME_SEND_POPUP(player, "Les prix affichés correspondent au nombre de pierres précieuses requis");
                        return;
                    } else if (idPnj == 50029) {
                        SocketManager.GAME_SEND_POPUP(player, "Les prix affichés correspondent au nombre de Kolizeton requis");
                        return;
                    } else if (idPnj == 816) {
                        SocketManager.GAME_SEND_POPUP(player, "Les prix affichés correspondent au nombre de jeton d'event Area requis");
                        return;
                    }
                } catch (NumberFormatException e) {
                }
                ;
                break;
            case '1':// Si joueur
                try {
                    int guidTarget = Integer.parseInt(packet.substring(4));
                    Player target = World.getPlayer(guidTarget);
                    if (target == null) {
                        SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(out, 'E');
                        return;
                    }
                    if (target.getMap() != player.getMap() || !target.isOnline())// Si
                    // les
                    // persos
                    // ne
                    // sont
                    // pas
                    // sur
                    // la
                    // meme
                    // map
                    {
                        SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(out, 'E');
                        return;
                    }
                    if (target.is_away() || player.is_away()
                            || target.get_isTradingWith() != 0) {
                        SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(out, 'O');
                        return;
                    }
                    if (player.getMap().get_id() == 8731) {
                        player.sendText("Échange impossible sur cette map. Veuillez quitter l'atelier.");
                        return;
                    }
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(out,
                            player.getGuid(), guidTarget, 1);
                    SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(target.getAccount()
                                    .getGameThread().getOut(), player.getGuid(),
                            guidTarget, 1);
                    player.set_isTradingWith(guidTarget);
                    target.set_isTradingWith(player.getGuid());
                } catch (NumberFormatException e) {
                }
                break;
            case '2':
                try {
                    int nID = Integer.parseInt(packet.substring(4));
                    NPC npc = player.getMap().getNPC(nID);
                    if (npc == null) return;
                    player.set_isTradingWith(nID);
                    player.setNpcExchange(new NpcExchange(player, npc));
                    SocketManager.GAME_SEND_ECK_PACKET(account.getGameThread().getOut(), 2, "" + nID);

                    //Listing item
                    ArrayList<NPC_Exchange> exchanges = npc.get_template().getExchange();
                    Map<Integer, String> liste_array = new HashMap<Integer, String>();
                    int pos = 0;
                    for (NPC_Exchange exchange : exchanges) {
                        for (Entry<Integer, Couple<Integer, Integer>> required : exchange.getRequired().entrySet()) {
                            ObjTemplate template = World.getObjTemplate(required.getKey());
                            if (liste_array.containsKey(pos)) {
                                liste_array.put(pos, liste_array.get(pos) + ", " + required.getValue().second + " " + template.getName());
                            } else {
                                liste_array.put(pos, "- " + required.getValue().second + " " + template.getName());
                            }
                        }
                        for (Couple<Integer, Integer> gift : exchange.getGift()) {
                            ObjTemplate template = World.getObjTemplate(gift.first);
                            if (liste_array.containsKey(pos) && liste_array.get(pos).contains("=")) {
                                liste_array.put(pos, liste_array.get(pos) + ", " + gift.second + " " + template.getName());
                            } else {
                                liste_array.put(pos, liste_array.get(pos) + " = " + gift.second + " " + template.getName());
                            }
                        }
                        pos++;
                    }
                    String liste = "Liste des echanges possible :\n";
                    for (Entry<Integer, String> item : liste_array.entrySet()) {
                        liste += item.getValue() + "\n";
                    }
                    SocketManager.GAME_SEND_BAIO_PACKET(player, liste);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case '4'://StorePlayer @Flow - A réviser
                int pID = 0;
                //int cellID = 0;//Inutile
                try {
                    pID = Integer.valueOf(packet.split("\\|")[1]);
                    //cellID = Integer.valueOf(packet.split("\\|")[2]);
                } catch (NumberFormatException e) {
                    return;
                }
                ;
                if (player.get_isTradingWith() > 0 || player.getFight() != null || player.is_away()) return;
                Player seller = World.getPlayer(pID);
                if (seller == null) return;
                player.set_isTradingWith(pID);
                SocketManager.GAME_SEND_ECK_PACKET(player, 4, seller.getGuid() + "");
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, player);
                break;
            case '6':// StoreItems
                if (player.get_isTradingWith() > 0 || player.getFight() != null
                        || player.is_away())
                    return;
                player.set_isTradingWith(player.getGuid());
                SocketManager.GAME_SEND_ECK_PACKET(player, 6, "");
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(player, player);
                break;
            case '8':// Si Percepteur
                try {
                    int PercepteurID = Integer.parseInt(packet.substring(4));
                    Collector perco = World.getPerco(PercepteurID);
                    if (perco == null || perco.get_inFight() > 0)
                        return;
                    if (player.get_guild().get_id() != perco.get_guildID()) {
                        return;
                    }
                    if (perco.get_Exchange()) {
                        Player p = World.getPlayer(perco.getExchangeWith());
                        if (p != null && p.isOnline()
                                && p.get_isOnPercepteurID() == PercepteurID) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "1180");
                            return;
                        } else {
                            perco.set_Exchange(false);
                            perco.setExchangeWith(-1);
                        }
                    }
                    perco.set_Exchange(true);
                    perco.setExchangeWith(player.getGuid());
                    SocketManager
                            .GAME_SEND_ECK_PACKET(out, 8, perco.getGuid() + "");
                    SocketManager.GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(out, perco);
                    player.set_isTradingWith(perco.getGuid());
                    player.set_isOnPercepteurID(perco.getGuid());
                } catch (NumberFormatException e) {
                }
                ;
                break;
        }
    }

    private void parse_environementPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'D':// Change direction
                Environement_change_direction(packet);
                break;

            case 'U':// Emote
                Environement_emote(packet);
                break;
        }
    }

    private void Environement_emote(String packet) {
        int emote = -1;
        try {
            emote = Integer.parseInt(packet.substring(2));
        } catch (Exception e) {
        }
        ;
        if (emote == -1)
            return;
        if (player == null)
            return;
        if (player.getFight() != null)
            return;// Pas d'émote en combat

        switch (emote)// effets spéciaux des émotes
        {
            case 19:// s'allonger
            case 1:// s'asseoir
                if (player.isSitted()) {
                    player.setSitted(false);
                } else {
                    player.setSitted(true);
                }
                break;
            default:
                // Si il fait une action autre que s'asseoir ou s'allonger, il n'est plus assis @Flow
                player.setSitted(false);
                break;
        }
        if (emote != 19 && emote != 1) {
            if (player.emoteActive() == emote) {
                player.setEmoteActive(0);
            } else {
                player.setEmoteActive(emote);
            }
        } else if (player.isSitted()) player.setEmoteActive(emote);
        SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(player.getMap(),
                player.getGuid(), player.emoteActive());
    }

    private void Environement_change_direction(String packet) {
        try {
            if (player.getFight() != null)
                return;
            int dir = Integer.parseInt(packet.substring(2));
            if (dir < 0 | dir > 7) return;
            SocketManager.GAME_SEND_eD_PACKET_TO_MAP(player.getMap(),
                    player.getGuid(), dir);
        } catch (NumberFormatException e) {
            return;
        }
        ;
    }

    private void parseSpellPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'B':
                boostSort(packet);
                break;
            case 'F':// Oublie de sort
                forgetSpell(packet);
                break;
            case 'M':
                addToSpellBook(packet);
                break;
        }
    }

    private void addToSpellBook(String packet) {
        try {
            int SpellID = Integer.parseInt(packet.substring(2).split("\\|")[0]);
            int Position = Integer
                    .parseInt(packet.substring(2).split("\\|")[1]);
            SortStats Spell = player.getSortStatBySortIfHas(SpellID);

            if (Spell != null) {
                player.set_SpellPlace(SpellID,
                        CryptManager.getHashedValueByInt(Position));
            }

            SocketManager.GAME_SEND_BN(out);
        } catch (Exception e) {
        }
        ;
    }

    private void boostSort(String packet) {
        try {
            int id = Integer.parseInt(packet.substring(2));
            GameServer.addToLog("Info: " + player.getName()
                    + ": Tente BOOST sort id=" + id);
            if (player.boostSpell(id)) {
                GameServer.addToLog("Info: " + player.getName()
                        + ": OK pour BOOST sort id=" + id);
                SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(out, id, player
                        .getSortStatBySortIfHas(id).getLevel());
                SocketManager.GAME_SEND_STATS_PACKET(player);
            } else {
                GameServer.addToLog("Info: " + player.getName()
                        + ": Echec BOOST sort id=" + id);
                SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(out);
                return;
            }
        } catch (NumberFormatException e) {
            SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(out);
            return;
        }
    }

    private void forgetSpell(String packet) {
        if (!player.isForgetingSpell())
            return;

        int id = Integer.parseInt(packet.substring(2));

        if (Config.DEBUG)
            GameServer.addToLog("Info: " + player.getName()
                    + ": Tente Oublie sort id=" + id);

        if (player.forgetSpell(id)) {
            if (Config.DEBUG)
                GameServer.addToLog("Info: " + player.getName()
                        + ": OK pour Oublie sort id=" + id);
            SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(out, id, player
                    .getSortStatBySortIfHas(id).getLevel());
            SocketManager.GAME_SEND_STATS_PACKET(player);
            player.setisForgetingSpell(false);
        }
    }

    private void parseFightPacket(String packet) {
        try {
            switch (packet.charAt(1)) {
                case 'D':// Détails d'un combat (liste des combats)
                    int key = -1;
                    try {
                        key = Integer.parseInt(packet.substring(2).replace(
                                ((int) 0x0) + "", ""));
                    } catch (Exception e) {
                    }
                    ;
                    if (key == -1)
                        return;
                    SocketManager.GAME_SEND_FIGHT_DETAILS(out, player.getMap()
                            .get_fights().get(key));
                    break;

                case 'H':// Aide
                    if (player.getFight() == null)
                        return;
                    player.getFight().toggleHelp(player.getGuid());
                    break;

                case 'L':// Lister les combats
                    SocketManager.GAME_SEND_FIGHT_LIST_PACKET(out, player.getMap());
                    break;
                case 'N':// Bloquer le combat
                    if (player.getFight() == null)
                        return;
                    player.getFight().toggleLockTeam(player.getGuid());
                    break;
                case 'P':// Seulement le groupe
                    if (player.getFight() == null || player.getGroup() == null)
                        return;
                    player.getFight().toggleOnlyGroup(player.getGuid());
                    break;
                case 'S':// Bloquer les specs
                    if (player.getFight() == null)
                        return;
                    player.getFight().toggleLockSpec(player.getGuid());
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;
    }

    private void parseBasicsPacket(String packet) {
        switch (packet.charAt(1)) {
            case 'A':// Console
                Basic_console(packet);
                break;
            case 'a': // Téléportation via géoposition... //-WalakaZ- & Skryn
                switch (packet.charAt(2)) {
                    case 'M':
                        try {
                            if (player.getAccount().getGmLevel() > 0) {

                                String posComplete = packet.substring(3).trim()
                                        + ","
                                        + 0;
                                if (World.cartesByPos.containsKey(posComplete)) {
                                    Maps c = World.cartesByPos.get(posComplete);
                                    if (Constant.getMapForbidden(c.get_id()))
                                        return;
                                    player.teleport((short) c.get_id(),
                                            c.getRandomFreeCellID());
                                }
                            } else
                                return;

                        } catch (Exception e) {
                        }
                        break;

                    default: // fuck
                        break;
                }
                break;
            case 'D':
                Basic_send_Date_Hour();
                break;
            case 'M':
                Basic_chatMessage(packet);
                break;
            case 'W':
                Basic_infosmessage(packet);
                break;
            case 'S':
                player.emoticone(packet.substring(2));
                break;
            case 'Y':
                Basic_state(packet);
                break;
        }
    }

    public void Basic_state(String packet) {
        switch (packet.charAt(2)) {
            case 'A': // Absent
                if (player._isAbsent) {

                    SocketManager.GAME_SEND_Im_PACKET(player, "038");

                    player._isAbsent = false;
                } else

                {
                    SocketManager.GAME_SEND_Im_PACKET(player, "037");
                    player._isAbsent = true;
                }
                break;
            case 'I': // Invisible
                if (player._isInvisible) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "051");
                    player._isInvisible = false;
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "050");
                    player._isInvisible = true;
                }
                break;
        }
    }

    public Player getPlayer() {
        return player;
    }

    private void Basic_console(String packet) {
        if (command == null)
            command = new GmCommand(player);
        command.consoleCommand(packet);
    }

    public void closeSocket() {
        try {
            this.socket.close();
        } catch (IOException e) {
        }
    }

    private void Basic_chatMessage(String packet) {
        String msg = "";
        String log_msg = "";
        if (player.isMuted()) {
            if (player.getAccount() != null) {
                player.getAccount().sendMutedIM();
            }
            return;
        }
        packet = packet.replace("<", "");
        packet = packet.replace(">", "");
        if (packet.length() == 3)
            return;
        switch (packet.charAt(2)) {
            case '*':// Canal noir
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                msg = packet.split("\\|", 2)[1];
                if (msg.isEmpty())
                    return;
                if (FloodCheck.isFlooding(player, msg))
                    return;
                if (player.getEvent() != null
                        && player.getEvent().getEventQuizz() != null)
                    if (player.getEvent().getEventQuizz().isWaitingAnwser()) {
                        player.getEvent().getEventQuizz()
                                .checkAnwser(player, msg.replace("|", ""));
                        return;
                    }

                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;
                if (player.getFight() == null) {
                    if (player.getMap().isMuted()
                            && player.getAccount().getGmLevel() == 0) {
                        player.sendMess(Lang.LANG_121);
                        return;
                    }
                    log_msg = "[Map " + player.getMap().get_id() + "] : " + msg;

                    if (GmCommand.isWhisper) {
                        GmCommand.whisper.sendText("[MAP] " + player.getName() + " : " + msg);
                    }

                    SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(player.getMap(), "",
                            player.getGuid(), player.getName(), msg);
                } else {
                    SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(player.getFight(),
                            7, "", player.getGuid(), player.getName(), msg);
                    log_msg = "[Map " + player.getMap().get_id() + " Combat "
                            + player.getFight().get_id() + "] : " + msg;
                }
                FloodCheck.updateFloodInfos(player, msg);
                break;
            case '#':// Canal Equipe
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                if (player.getFight() != null) {
                    msg = packet.split("\\|", 2)[1];
                    if (FloodCheck.isFlooding(player, msg))
                        return;
                    if (PlayerCommand.tryNewCommand(msg, player, out))
                        return;
                    int team = player.getFight().getTeamID(player.getGuid());
                    if (team == -1)
                        return;
                    SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(player.getFight(),
                            team, "#", player.getGuid(), player.getName(), msg);
                    log_msg = "[Map " + player.getMap().get_id() + " Combat "
                            + player.getFight().get_id() + " Equipe " + team
                            + "] : " + msg;
                    FloodCheck.updateFloodInfos(player, msg);
                }
                break;
            case '$':// Canal groupe
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                if (player.getGroup() == null)
                    break;
                msg = packet.split("\\|", 2)[1];
                if (FloodCheck.isFlooding(player, msg))
                    return;
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;

                if (GmCommand.isWhisper) {
                    GmCommand.whisper.sendText("[GROUP] " + player.getName() + " : " + msg);
                }

                SocketManager.GAME_SEND_cMK_PACKET_TO_GROUP(player.getGroup(), "$",
                        player.getGuid(), player.getName(), msg);
                log_msg = "[Groupe " + player.getGroup().getChief().getGuid()
                        + "] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                break;

            case ':':// Canal commerce
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                long l;
                if ((l = System.currentTimeMillis() - _timeLastTradeMsg) < Config.FLOOD_TIME
                        && player.getAccount().getGmLevel() < 3) {
                    l = (Config.FLOOD_TIME - l) / 1000;// On calcul la différence en
                    // secondes
                    SocketManager.GAME_SEND_Im_PACKET(player,
                            "0115;" + ((int) Math.ceil(l) + 1));
                    FloodCheck.updateFloodInfos(player, msg);
                    return;
                }
                _timeLastTradeMsg = System.currentTimeMillis();
                msg = packet.split("\\|", 2)[1];
                if (FloodCheck.isFlooding(player, msg))
                    return;
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;
                log_msg = "[Commerce] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                SocketManager.GAME_SEND_cMK_PACKET_TO_ALL(":", player.getGuid(),
                        player.getName(), msg);
                break;
            case '@':// Canal Admin
                if (player.getAccount().getGmLevel() == 0)
                    return;
                msg = packet.split("\\|", 2)[1];
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;
                if (FloodCheck.isFlooding(player, msg))
                    return;
                SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", player.getGuid(),
                        player.getName(), msg);
                log_msg = "[Admin] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                break;
            case '?':// Canal recrutement
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                long j;
                if ((j = System.currentTimeMillis() - _timeLastRecrutmentMsg) < Config.FLOOD_TIME
                        && player.getAccount().getGmLevel() < 3) {
                    j = (Config.FLOOD_TIME - j) / 1000;// On calcul la différence en
                    // secondes
                    SocketManager.GAME_SEND_Im_PACKET(player,
                            "0115;" + ((int) Math.ceil(j) + 1));
                    FloodCheck.updateFloodInfos(player, msg);
                    return;
                }
                _timeLastRecrutmentMsg = System.currentTimeMillis();
                msg = packet.split("\\|", 2)[1];
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;
                if (FloodCheck.isFlooding(player, msg))
                    return;
                SocketManager.GAME_SEND_cMK_PACKET_TO_ALL("?", player.getGuid(),
                        player.getName(), msg);
                log_msg = "[Recrutement] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                break;
            case '%':// Canal guilde
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                if (player.get_guild() == null)
                    return;
                msg = packet.split("\\|", 2)[1];
                if (FloodCheck.isFlooding(player, msg))
                    return;
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;

                if (GmCommand.isWhisper) {
                    GmCommand.whisper.sendText("[GROUPE] " + player.getName() + " : " + msg);
                }

                SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(player.get_guild(),
                        "%", player.getGuid(), player.getName(), msg);
                log_msg = "[Guide " + player.get_guild().get_name() + "] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                break;
            case 0xC2:// Canal
                break;
            case '!':// Alignement
                if (!player.get_canaux().contains(packet.charAt(2) + ""))
                    return;
                if (player.get_align() == 0)
                    return;
                if (player.getDeshonor() >= 1) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "183");
                    return;
                }
                long k;
                if ((k = System.currentTimeMillis() - _timeLastAlignMsg) < Config.FLOOD_TIME
                        && player.getAccount().getGmLevel() < 3) {
                    k = (Config.FLOOD_TIME - k) / 1000;// On calcul la différence en
                    // secondes
                    SocketManager.GAME_SEND_Im_PACKET(player,
                            "0115;" + ((int) Math.ceil(k) + 1));
                    return;
                }
                _timeLastAlignMsg = System.currentTimeMillis();
                msg = packet.split("\\|", 2)[1];
                if (FloodCheck.isFlooding(player, msg))
                    return;
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;

                if (GmCommand.isWhisper) {
                    GmCommand.whisper.sendText("[ALIGN] " + player.getName() + " : " + msg);
                }

                SocketManager.GAME_SEND_cMK_PACKET_TO_ALIGN("!", player.getGuid(),
                        player.getName(), msg, player);
                log_msg = "[Alignement " + player.get_align() + "] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                break;
            case '^':// Canal Incarnam
                msg = packet.split("\\|", 2)[1];
                long x;
                if ((x = System.currentTimeMillis() - _timeLastIncarnamMsg) < Config.FLOOD_TIME) {
                    x = (Config.FLOOD_TIME - x) / 1000;// Calculamos a diferença em
                    // segundos
                    SocketManager.GAME_SEND_Im_PACKET(player,
                            "0115;" + ((int) Math.ceil(x) + 1));
                    return;
                }
                _timeLastIncarnamMsg = System.currentTimeMillis();
                msg = packet.split("\\|", 2)[1];
                if (FloodCheck.isFlooding(player, msg))
                    return;
                if (PlayerCommand.tryNewCommand(msg, player, out))
                    return;
                SocketManager.GAME_SEND_cMK_PACKET_INCARNAM_CHAT(player, "^",
                        player.getGuid(), player.getName(), msg);
                log_msg = "[Incarnam] : " + msg;
                FloodCheck.updateFloodInfos(player, msg);
                break;
            case '¤': // Canal admin pas de retour @Flow
                return;
            default:
                String nom = packet.substring(2).split("\\|")[0];
                msg = packet.split("\\|", 2)[1];
                if (nom.length() <= 1)
                    GameServer.addToLog("ChatHandler: Chanel non gere : " + nom);
                if (FloodCheck.isFlooding(player, msg))
                    return;
                else {
                    if (nom.equalsIgnoreCase("[Staff]")) {
                        String[] donnes = msg.split(Pattern.quote(" "));
                        nom = donnes[0];
                        msg = donnes[1];
                    }
                    Player target = World.getPersoByName(nom);
                    if (target == null)// si le personnage n'existe pas
                    {
                        SocketManager.GAME_SEND_CHAT_ERROR_PACKET(out, nom);
                        return;
                    }
                    if (target.getAccount() == null) {
                        SocketManager.GAME_SEND_CHAT_ERROR_PACKET(out, nom);
                        return;
                    }
                    if (target.getAccount().getGameThread() == null)// si le perso
                    // n'est pas co
                    {
                        SocketManager.GAME_SEND_CHAT_ERROR_PACKET(out, nom);
                        return;
                    }
                    if (target.getAccount().isEnemyWith(
                            player.getAccount().getGuid()) == true
                            || !target.isDispo(player)) {
                        SocketManager.GAME_SEND_Im_PACKET(player,
                                "114;" + target.getName());
                        return;
                    }

                    if (GmCommand.isWhisper) {
                        GmCommand.whisper.sendText("[MESS] " + player.getName() + " <b>to</b> " + target.getName() + " : " + msg);
                    }

                    SocketManager.GAME_SEND_cMK_PACKET(target, "F",
                            player.getGuid(), player.getName(), msg);
                    SocketManager.GAME_SEND_cMK_PACKET(player, "T",
                            target.getGuid(), target.getName(), msg);
                    log_msg = "[MP à " + target.getName() + "] : " + msg;
                }
                break;
        }
        Logs.addToChatLog("[" + account.getCurIp() + "] (" + account.getName()
                + ", " + player.getName() + ") " + log_msg);
    }

    private void Basic_send_Date_Hour() {
        SocketManager.GAME_SEND_SERVER_DATE(out);
        SocketManager.GAME_SEND_SERVER_HOUR(out);
    }

    private void Basic_infosmessage(String packet) {
        packet = packet.substring(2);
        Player T = World.getPersoByName(packet);
        if (T == null)
            return;
        SocketManager.GAME_SEND_BWK(player, T.getAccount().getPseudo() + "|1|"
                + T.getName() + "|-1");
    }

    private void parseGamePacket(String packet) {
        switch (packet.charAt(1)) {
            case 'A':
                if (player == null)
                    return;
                parseGameActionPacket(packet);
                break;
            case 'C':
                if (player == null)
                    return;
                player.sendGameCreate();
                break;
            case 'd': // demande de reciblage challenge
                Game_on_Gdi_packet(packet);
            case 'f':
                Game_on_showCase(packet);
                break;
            case 'I':
                Game_on_GI_packet();
                break;
            case 'K':
                Game_on_GK_packet(packet);
                break;
            case 'P':// PvP Toogle
                if (player == null)
                    return;
                player.toggleWings(packet.charAt(2));
                break;
            case 'p':
                Game_on_ChangePlace_packet(packet);
                break;
            case 'Q':
                Game_onLeftFight(packet);
                break;
            case 'R':
                Game_on_Ready(packet);
                break;
            case 't':
                if (player == null)
                    return;
                if (player.getFight() == null)
                    return;
                //player.sendText("DEBUG: Le packet pour passer le tour a été reçu, attente du traitement...");
                player.getFight().playerPass(player);
                break;
        }
    }

    private void Game_onLeftFight(String packet) {
        int targetID = -1;
        if (!packet.substring(2).isEmpty()) {
            try {
                targetID = Integer.parseInt(packet.substring(2));
            } catch (Exception e) {
            }
            ;
        }
        if (player.getFight() == null)
            return;
        if (targetID > 0)// Expulsion d'un joueurs autre que soi-meme
        {
            if (player.getArena() == 1
                    || (player.getKolizeum() != null && player.getKolizeum()
                    .isStarted())) {
                player.sendMess(Lang.LANG_123);
                return;
            }
            Player target = World.getPlayer(targetID);
            // On ne quitte pas un joueur qui : est null, ne combat pas, n'est
            // pas de ça team.
            if (target == null
                    || target.getFight() == null
                    || target.getFight().getTeamID(target.getGuid()) != player
                    .getFight().getTeamID(player.getGuid()))
                return;
            player.getFight().leftFight(player, target, false);

        } else {
            if (player.getArena() == 1
                    || (player.getKolizeum() != null && player.getKolizeum()
                    .isStarted())) {
                player.sendMess(Lang.LANG_124);
                return;
            }
            player.getFight().leftFight(player, null, false);
        }
    }

    private void Game_on_showCase(String packet) {
        if (player == null)
            return;
        if (player.getFight() == null)
            return;
        if (player.getFight().get_state() != Constant.FIGHT_STATE_ACTIVE)
            return;
        int cellID = -1;
        try {
            cellID = Integer.parseInt(packet.substring(2));
        } catch (Exception e) {
        }
        ;
        if (cellID == -1)
            return;
        player.getFight().showCaseToTeam(player.getGuid(), cellID);
    }

    private void Game_on_Ready(String packet) {
        if (player.getFight() == null)
            return;
        if (player.getFight().get_state() != Constant.FIGHT_STATE_PLACE)
            return;
        player.set_ready(packet.substring(2).equalsIgnoreCase("1"));
        player.getFight().verifIfAllReady();
        SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(player.getFight(),
                3, player.getGuid(), packet.substring(2).equalsIgnoreCase("1"));
    }

    private void Game_on_ChangePlace_packet(String packet) {
        if (player.getFight() == null)
            return;
        try {
            int cell = Integer.parseInt(packet.substring(2));
            player.getFight().changePlace(player, cell);
        } catch (NumberFormatException e) {
            return;
        }
        ;
    }

    private void Game_on_Gdi_packet(String packet) {
        int chalID = 0;
        chalID = Integer.parseInt(packet.split("i")[1]);
        if (chalID != 0 && player.getFight() != null) {
            Fight fight = player.getFight();
            if (fight.get_challenges().containsKey(chalID))
                fight.get_challenges().get(chalID).show_cibleToPerso(player);
        }

    }

    private void Game_on_GK_packet(String packet) {
        int GameActionId = -1;
        String[] infos = packet.substring(3).split("\\|");
        try {
            GameActionId = Integer.parseInt(infos[0]);
        } catch (Exception e) {
            return;
        }
        ;
        if (GameActionId == -1) {
            return;
        }
        GameAction GA = actions.get(GameActionId);
        if (GA == null) {
            return;
        }
        boolean isOk = packet.charAt(2) == 'K';

        switch (GA._actionID) {
            case 1:// Deplacement
                if (isOk) {
                    // Hors Combat
                    if (player.getFight() == null) {
                        player.get_curCell().removePlayer(player.getGuid());
                        SocketManager.GAME_SEND_BN(out);
                        String path = GA._args;
                        // On prend la case ciblÃ¯Â¿Â½e
                        Case nextCell = player.getMap().getCase(
                                CryptManager.cellCode_To_ID(path.substring(path
                                        .length() - 2)));
                        Case targetCell = player.getMap().getCase(
                                CryptManager.cellCode_To_ID(GA._packet
                                        .substring(GA._packet.length() - 2)));
                        if (nextCell == null) {
                            nextCell = player.get_curCell();
                        }
                        if (targetCell == null) {
                            targetCell = player.get_curCell();
                        }
                        // On dÃ¯Â¿Â½finie la case et on ajoute le personnage sur la
                        // case
                        player.set_curCell(nextCell);// TODO
                        player.set_orientation(CryptManager
                                .getIntByHashedValue(path.charAt(path.length() - 3)));
                        player.get_curCell().addPerso(player);
                        if (!player._isGhosts)
                            player.set_away(false);
                        if (targetCell.getObject() != null) {
                            // Si c'est une "borne" comme Emotes, ou CrÃ¯Â¿Â½ation
                            // guilde
                            if (targetCell.getObject().getID() == 1324) {
                                Constant.applyPlotIOAction(player, player.getMap()
                                        .get_id(), targetCell.getID());
                            }
                            // Statues phoenix
                            else if (targetCell.getObject().getID() == 542) {
                                if (player._isGhosts)
                                    player.set_Alive();
                            }
                        }

                        player.getMap().onPlayerArriveOnCell(player,
                                player.get_curCell().getID());
                    } else// En combat
                    {
                        player.getFight().onGK(player);
                        return;
                    }

                } else {
                    // Si le joueur s'arrete sur une case
                    int newCellID = -1;
                    try {
                        newCellID = Integer.parseInt(infos[1]);
                    } catch (Exception e) {
                        return;
                    }
                    ;
                    if (newCellID == -1) {
                        return;
                    }
                    String path = GA._args;
                    player.get_curCell().removePlayer(player.getGuid());
                    player.set_curCell(player.getMap().getCase(newCellID));
                    player.set_orientation(CryptManager.getIntByHashedValue(path
                            .charAt(path.length() - 3)));
                    player.get_curCell().addPerso(player);
                    SocketManager.GAME_SEND_BN(out);
                }
                break;

            case 500:// Action Sur Map
                player.finishActionOnCell(GA);
                break;

        }
        removeAction(GA);
    }

    private void Game_on_GI_packet() {
        Maps map = player.getMap();
        int debug = 0;

        try {
            if (player.getFight() != null) {
                // Only percepteur
                SocketManager
                        .GAME_SEND_MAP_GMS_PACKETS(player.getMap(), player);
                SocketManager.GAME_SEND_GDK_PACKET(out);
                return;
            }

            SocketManager.GAME_SEND_Rp_PACKET(this.player, this.player.getMap()
                    .getMountPark());
            debug++;

            Houses.LoadHouse(this.player, this.player.getMap().get_id());
            debug++;

            SocketManager.GAME_SEND_MAP_GMS_PACKETS(this.player.getMap(),
                    this.player);
            debug++;
            SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS(this.player
                            .getAccount().getGameThread().getOut(),
                    this.player.getMap(), player);
            debug++;
            SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(this.out,
                    this.player.getMap());
            debug++;
            SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(this.out,
                    this.player.getMap());
            debug++;

            SocketManager.GAME_SEND_MAP_OBJECTS_GDS_PACKETS(this.out,
                    this.player.getMap());
            debug++;
            SocketManager.GAME_SEND_GDK_PACKET(this.out);
            debug++;
            SocketManager.GAME_SEND_MAP_FIGHT_COUNT(this.out,
                    this.player.getMap());
            debug++;
            SocketManager.GAME_SEND_MERCHANT_LIST(player, player.getMap()
                    .get_id());
            debug++;
            SocketManager.SEND_GM_PRISME_TO_MAP(this.out, map);
            debug++;
            Fight.FightStateAddFlag(this.player.getMap(), this.player);
            debug++;

            this.player.getMap().sendFloorItems(this.player);
            debug++;
            try {
                Thread.sleep(360);
            } catch (Exception e) {
            }
            SocketManager.GAME_SEND_eUK_PACKET_TO_PLAYER(this.player.getMap(), this.player);
        } catch (Exception e) {
            SocketManager.send(this.player, "cC+i");
            System.out.println("Erreur GI numéro: " + debug);
        }

    }

    /**
     * private void Game_on_GI_packet() { if(_perso.get_fight() != null) {
     * <p/>
     * //Only percepteur if(_perso.get_fight().get_type() ==
     * Constants.FIGHT_TYPE_PVT)
     * SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_fight().get_map(),
     * _perso); SocketManager.GAME_SEND_GDK_PACKET(out); return; } //Enclos
     * SocketManager.GAME_SEND_Rp_PACKET(_perso,
     * _perso.get_curCarte().getMountPark()); //Maisons House.LoadHouse(_perso,
     * _perso.get_curCarte().get_id()); //Objets sur la carte
     * SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
     * SocketManager
     * .GAME_SEND_MAP_MOBS_GMS_PACKETS(_perso.getAccount().getGameThread
     * ().get_out(), _perso.get_curCarte());
     * SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS
     * (_perso,_perso.get_curCarte());
     * SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(out,_perso.get_curCarte());
     * SocketManager
     * .GAME_SEND_MAP_OBJECTS_GDS_PACKETS(out,_perso.get_curCarte());
     * SocketManager.GAME_SEND_GDK_PACKET(out);
     * SocketManager.GAME_SEND_MAP_FIGHT_COUNT(out, _perso.get_curCarte());
     * SocketManager.GAME_SEND_MERCHANT_LIST(_perso,
     * _perso.get_curCarte().get_id()); //Les drapeau de combats
     * Fight.FightStateAddFlag(_perso.get_curCarte(), _perso); //items au sol
     * _perso.get_curCarte().sendFloorItems(_perso); }
     **/

    private void parseGameActionPacket(String packet) {
        int actionID;
        try {
            actionID = Integer.parseInt(packet.substring(2, 5));
        } catch (NumberFormatException e) {
            return;
        }
        ;

        int nextGameActionID = 0;
        if (actions.size() > 0) {
            // On prend le plus haut GameActionID + 1
            nextGameActionID = (Integer) (actions.keySet().toArray()[actions
                    .size() - 1]) + 1;
        }

        GameAction GA = new GameAction(nextGameActionID, actionID, packet);

        switch (actionID) {
            case 1:// Deplacement
                game_parseDeplacementPacket(GA);
                break;

            case 300:// Sort
                game_tryCastSpell(packet);
                break;

            case 303:// Attaque CaC
                game_tryCac(packet);
                break;

            case 500:// Action Sur Map
                game_action(GA);
                break;

            case 512: // Prismes
                if (player.get_align() == Constant.ALIGNEMENT_NEUTRE)
                    return;
                player.openPrismeMenu();
                break;

            case 507:// Panneau intérieur de la maison
                house_action(packet);
                break;

            case 618:// Mariage oui
                player.setisOK(Integer.parseInt(packet.substring(5, 6)));
                SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(player.getMap(), "",
                        player.getGuid(), player.getName(), "Oui");
                if (World.getMarried(0).getisOK() > 0
                        && World.getMarried(1).getisOK() > 0) {
                    World.Wedding(World.getMarried(0), World.getMarried(1), 1);
                }
                if (World.getMarried(0) != null && World.getMarried(1) != null) {
                    World.PriestRequest(
                            (World.getMarried(0) == player ? World.getMarried(1)
                                    : World.getMarried(0)),
                            (World.getMarried(0) == player ? World.getMarried(1)
                                    .getMap() : World.getMarried(0).getMap()),
                            player.get_isTalkingWith());
                }
                break;
            case 619:// Mariage non
                player.setisOK(0);
                SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(player.getMap(), "",
                        player.getGuid(), player.getName(), "Non");
                World.Wedding(World.getMarried(0), World.getMarried(1), 0);
                break;

            case 900:// Demande Defie
                game_ask_duel(packet);
                break;
            case 901:// Accepter Defie
                game_accept_duel(packet);
                break;
            case 902:// Refus/Anuler Defie
                game_cancel_duel();
                break;
            case 903:// Rejoindre combat
                game_join_fight(packet);
                break;
            case 906:// Agresser
                game_aggro(packet);
                break;
            case 909:// Perco
                game_perco(packet);
                break;
            case 912:// Attaquer prisme
                if (player.get_align() == Constant.ALIGNEMENT_NEUTRE)
                    return;
                Game_attaque_prisme(packet);
                break;
        }
    }

    private void Game_attaque_prisme(String packet) {
        try {
            if (player == null)
                return;
            if (player.getFight() != null)
                return;
            if (player.get_isTalkingWith() != 0
                    || player.get_isTradingWith() != 0
                    || player.getCurJobAction() != null) {
                return;
            }
            if (player.get_align() == Constant.ALIGNEMENT_NEUTRE)
                return;
            if (Constant.COMBAT_BLOQUE) {
                player.sendText("Les combats sont bloqués par les administrateurs.");
                return;
            } else if (player.estBloqueCombat()) {
                player.sendText("Temporisation en cours... Veuillez patienter...");
                return;
            }
            int id = Integer.parseInt(packet.substring(5));
            Prism Prisme = World.getPrisme(id);
            if ((Prisme.getInFight() == 0 || Prisme.getInFight() == -2))
                return;
            SocketManager.SEND_GA_Action_ALL_MAPS(player.getMap(), "", 909,
                    player.getGuid() + "", id + "");
            player.getMap().initFightVSPrisme(player, Prisme);

        } catch (Exception e) {
        }
    }

    private void house_action(String packet) {
        int actionID = Integer.parseInt(packet.substring(5));
        Houses h = player.getInHouse();
        if (h == null)
            return;
        switch (actionID) {
            case 81:// Vérouiller maison
                h.Lock(player);
                break;
            case 97:// Acheter maison
                h.BuyIt(player);
                break;
            case 98:// Vendre
            case 108:// Modifier prix de vente
                h.SellIt(player);
                break;
        }
    }

    private void game_perco(String packet) {
        try {
            if (player == null)
                return;
            if (player.getFight() != null)
                return;
            if (player.get_isTalkingWith() != 0
                    || player.get_isTradingWith() != 0
                    || player.getCurJobAction() != null
                    || player.get_curExchange() != null || player.is_away()) {
                return;
            }
            if (Constant.COMBAT_BLOQUE) {
                player.sendText("Les combats sont bloqués par les administrateurs.");
                return;
            } else if (player.estBloqueCombat()) {
                player.sendText("Temporisation en cours... Veuillez patienter...");
                return;
            }
            int id = Integer.parseInt(packet.substring(5));
            Collector target = World.getPerco(id);
            if (target == null || target.get_inFight() > 0)
                return;
            if (target.get_Exchange()) {
                Player p = World.getPlayer(target.getExchangeWith());
                if (p != null && p.isOnline() && p.get_isOnPercepteurID() == id) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "1180");
                    return;
                } else {
                    target.set_Exchange(false);
                    target.setExchangeWith(-1);
                }
            }
            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(player.getMap(), "", 909,
                    player.getGuid() + "", id + "");
            player.getMap().startFigthVersusPercepteur(player, target);
        } catch (Exception e) {
        }
        ;
    }

    private synchronized void game_aggro(String packet) {
        try {
            int id = Integer.parseInt(packet.substring(5));
            Player target = World.getPlayer(id);
            if (player == null)
                return;
            if (Constant.COMBAT_BLOQUE) {
                player.sendText("Les combats sont bloqués par les administrateurs.");
                return;
            } else if (target.estBloqueCombat()) {
                player.sendText("Votre cible est toujours en temporisation... Veuillez patienter...");
                return;
            }

            if (player.estBloqueCombat()) {
                player.sendText("Temporisation en cours... Veuillez patienter...");
                return;
            }

			/*
             * 	Possibilité d'aggro.
			 */
            Integer calcDifLevel = 0;
            Integer calcDifPrestige = 0;

            calcDifLevel = player.getLevel() - target.getLevel();
            calcDifPrestige = player.getPrestige() - target.getPrestige();

            if (target.getAccount().getCurIp().equals(player.getAccount().getCurIp()) && player.getAccount().getGmLevel() < 4) {
                player.sendText("Impossible de d'agresser ce personnage. (IP identique).");
                return;
            }

            if (calcDifLevel >= 15) {
                SocketManager.GAME_SEND_MESSAGE(player, "<b>Aggression impossible :</b> Le joueur " + target.getName() + " a plus de 15 niveaux de moins que vous.", AllColor.GREEN);
                return;
            }

            if (calcDifPrestige >= 3) {
                SocketManager.GAME_SEND_MESSAGE(player, "<b>Aggression impossible :</b> Le joueur " + target.getName() + " a plus de 3 prestiges de moins que vous.", AllColor.GREEN);
                return;
            }


            if (player.getFight() != null || target.getFight() != null)
                return; // Return / Skryn
            if (target == null || !target.isOnline()
                    || target.getFight() != null
                    || target.getMap().get_id() != player.getMap().get_id()
                    || target.get_align() == player.get_align()
                    || player.getMap().get_placesStr().equalsIgnoreCase("|")
                    || !target.canAggro())
                return;
            if (World.isRestrictedMap(player.getMap().get_id())) {
                Traque traque = Stalk.getTraqueByOwner(player);
                if (traque == null || traque.getTarget() != target.getGuid()) {
                    SocketManager
                            .GAME_SEND_MESSAGE(
                                    player,
                                    "Vous ne pouvez pas agresser d'autre joueurs sur cette map.",
                                    "A00000");
                    return;
                }
            }
            // Anti-mulage - Restriction aggro
            if (!player.verifIfResetTimeAggroList(target.getGuid())) {
                if (player.getNumberOfAggro(target.getGuid()) == 3) {
                    player.sendText("Vous avez le droit d'agresser que 3 fois par heure la même cible !");
                    return;
                }
            }

            player.setFirstTimeAggro(target.getGuid());
            target.setFirstTimeAggro(player.getGuid());
            player.ajouterAggroList(target.getGuid());
            target.ajouterAggroList(player.getGuid());

            if (target.get_align() == 0) {
                player.setDeshonor(player.getDeshonor() + 1);
                SocketManager.GAME_SEND_Im_PACKET(player, "084;1");
            }
            player.toggleWings('+');
            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(player.getMap(), "", 906,
                    player.getGuid() + "", id + "");
            player.getMap().newFight(player, target,
                    Constant.FIGHT_TYPE_AGRESSION);
        } catch (Exception e) {
        }
        ;
    }

    private void game_action(GameAction GA) {
        String packet = GA._packet.substring(5);
        int cellID = -1;
        int actionID = -1;

        try {
            cellID = Integer.parseInt(packet.split(";")[0]);
            actionID = Integer.parseInt(packet.split(";")[1]);
        } catch (Exception e) {
        }
        // Si packet invalide, ou cellule introuvable
        if (cellID == -1 || actionID == -1 || player == null
                || player.getMap() == null
                || player.getMap().getCase(cellID) == null)
            return;
        GA._args = cellID + ";" + actionID;
        player.getAccount().getGameThread().addAction(GA);
        player.startActionOnCell(GA);
    }

    private synchronized void game_tryCac(String packet) {
        try {
            if (player.getFight() == null)
                return;
            int cellID = -1;
            try {
                cellID = Integer.parseInt(packet.substring(5));
            } catch (Exception e) {
                return;
            }
            ;

            player.getFight().tryCaC(player, cellID);
        } catch (Exception e) {
        }
        ;
    }

	/*private synchronized void game_tryCastSpell(String packet) {
        try {
			String[] splt = packet.split(";");
			int spellID = Integer.parseInt(splt[0].substring(5));
			int caseID = Integer.parseInt(splt[1]);
			if (player.getFight() != null) {
				SortStats SS = player.getSortStatBySortIfHas(spellID);
				if (SS == null)
					return;
				player.getFight()
						.tryCastSpell(
								player.getFight().getFighterByPerso(player),
								SS, caseID);
			}
		} catch (NumberFormatException e) {
			return;
		}
		;
	}*/

    private void game_tryCastSpell(String packet) // @Flow
    {
        try {
            String[] splt = packet.split(";");
            int spellID = Integer.parseInt(splt[0].substring(5));
            int caseID = Integer.parseInt(splt[1]);
            if (player.getFight() != null) {

                SortStats SS = player.getSortStatBySortIfHas(spellID);
                if (SS == null) {
                    SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(player.getFight(), 7);
                    SocketManager.GAME_SEND_Im_PACKET(player, "1169");
                    SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(player.getFight(), 7, 0, player.getGuid());
                    return;
                }
                boolean canLaunch = true;
                if (player.getFight().get_type() != Constant.FIGHT_TYPE_PVM) {
                    if (Constant.SORTS_INTERDITS_PVP != null) {
                        if (Constant.SORTS_INTERDITS_PVP.contains(spellID)) {
                            canLaunch = false;
                        }
                    }
                }
                if (canLaunch) {
                    player.getFight().tryCastSpell(player.getFight().getFighterByPerso(player), SS, caseID);
                } else {
                    player.sendText("Vous ne pouvez utilisez ce sort uniquement en PvM.");
                    SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(player.getFight(), 7);
                }
            }
        } catch (NumberFormatException e) {
            return;
        }
    }

    private void game_join_fight(String packet) {
        if (player.getFight() != null)
            return;


        String[] infos = packet.substring(5).split(";");
        if (infos.length == 1) {
            try {
                Fight F = player.getMap().getFight(Integer.parseInt(infos[0]));
                F.joinAsSpect(player);

            } catch (Exception e) {
                return;
            }
        } else {
            try {
                int guid = Integer.parseInt(infos[1]);
                if (player.is_away()) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(out, 'o', guid);
                    return;
                }
                if (World.getPlayer(guid) == null) {
                    if (player.getMap().get_id() == EventConstant.MAP_SURVIVANT
                            .get_id()) {
                        for (Fight fight : EventConstant.MAP_SURVIVANT
                                .get_fights().values())
                            if (fight.getEvent() != null
                                    && fight.getEvent().getEventSurvivor() != null) {
                                if (player.getEvent() != null
                                        && player.getEvent().getEventSurvivor() != null)
                                    fight.joinEvent(player);
                                else
                                    player.sendMess(Lang.LANG_122);
                                break;
                            }
                    }
                    return;
                }
                Fight combat = World.getPlayer(guid).getFight();
                if (combat.get_map().get_id() == player.getMap().get_id()) {
                    combat.joinFight(player, guid);
                } else {
                    player.sendText("Nice try Wpe...");
                }

            } catch (Exception e) {
                return;
            }
        }
    }

    private void game_accept_duel(String packet) {
        int guid = -1;
        try {
            guid = Integer.parseInt(packet.substring(5));
        } catch (NumberFormatException e) {
            return;
        }
        ;
        if (player.get_duelID() != guid || player.get_duelID() == -1)
            return;
        SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(player.getMap(),
                player.get_duelID(), player.getGuid());

        Fight fight = player.getMap().newFight(
                World.getPlayer(player.get_duelID()), player,
                Constant.FIGHT_TYPE_CHALLENGE);
        player.set_fight(fight);
        World.getPlayer(player.get_duelID()).set_fight(fight);

    }

    private void game_cancel_duel() {
        try {
            if (player.get_duelID() == -1)
                return;
            SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(player.getMap(),
                    player.get_duelID(), player.getGuid());
            World.getPlayer(player.get_duelID()).set_away(false);
            World.getPlayer(player.get_duelID()).set_duelID(-1);
            player.set_away(false);
            player.set_duelID(-1);
        } catch (NumberFormatException e) {
            return;
        }
        ;
    }

    private void game_ask_duel(String packet) {
        if (player.getMap().get_placesStr().equalsIgnoreCase("|")) {
            SocketManager.GAME_SEND_DUEL_Y_AWAY(out, player.getGuid());
            return;
        }
        try {
            int guid = Integer.parseInt(packet.substring(5));
            if (player.is_away() || player.getFight() != null) {
                SocketManager.GAME_SEND_DUEL_Y_AWAY(out, player.getGuid());
                return;
            }
            Player Target = World.getPlayer(guid);
            if (Target == null)
                return;
            if (Target.getAccount().getCurIp().equals(player.getAccount().getCurIp()) && player.getAccount().getGmLevel() != 5) { //@Poupou Pour pouvoir test
                player.sendText("Impossible de défier ce personnage. (IP identique).");
                return;
            }
            if (Target.is_away() || Target.getFight() != null
                    || Target.getMap().get_id() != player.getMap().get_id()) {
                SocketManager.GAME_SEND_DUEL_E_AWAY(out, player.getGuid());
                return;
            }
            if (Constant.COMBAT_BLOQUE) {
                player.sendText("Les combats sont bloqués par les administrateurs.");
                return;
            } else if (Target.estBloqueCombat()) {
                player.sendText("Votre cible est toujours en temporisation... Veuillez patienter...");
                return;
            }
            if (player.estBloqueCombat()) {
                player.sendText("Temporisation en cours... Veuillez patienter...");
                return;
            }

            player.set_duelID(guid);
            player.set_away(true);
            World.getPlayer(guid).set_duelID(player.getGuid());
            World.getPlayer(guid).set_away(true);
            SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(player.getMap(),
                    player.getGuid(), guid);
        } catch (NumberFormatException e) {
            return;
        }
    }

    private synchronized void game_parseDeplacementPacket(GameAction GA) {
        String path = GA._packet.substring(5);
        if (player.getFight() == null) {
            if (player.getPodUsed() > player.getMaxPod()) {
                SocketManager.GAME_SEND_Im_PACKET(player, "112");
                SocketManager.GAME_SEND_GA_PACKET(out, "", "0", "", "");
                removeAction(GA);
                return;
            }
            if (player.getAccount().getGmLevel() > 0 && player.isGodmode()) {
                player.get_curCell().removePlayer(player.getGuid());
                SocketManager.GAME_SEND_BN(out);
                // On prend la case ciblÃ¯Â¿Â½e
                Case nextCell = player.getMap().getCase(
                        CryptManager.cellCode_To_ID(path.substring(path
                                .length() - 2)));
                Case targetCell = player.getMap().getCase(
                        CryptManager.cellCode_To_ID(GA._packet
                                .substring(GA._packet.length() - 2)));
                if (nextCell == null) {
                    nextCell = player.get_curCell();
                }
                if (targetCell == null) {
                    targetCell = player.get_curCell();
                }
                // On dÃ¯Â¿Â½finie la case et on ajoute le personnage sur la
                // case
                player.set_curCell(nextCell);// TODO
                player.set_orientation(CryptManager
                        .getIntByHashedValue(path.charAt(path.length() - 3)));
                player.get_curCell().addPerso(player);
                if (!player._isGhosts)
                    player.set_away(false);
                if (targetCell.getObject() != null) {
                    // Si c'est une "borne" comme Emotes, ou CrÃ¯Â¿Â½ation
                    // guilde
                    if (targetCell.getObject().getID() == 1324) {
                        Constant.applyPlotIOAction(player, player.getMap()
                                .get_id(), targetCell.getID());
                    }
                    // Statues phoenix
                    else if (targetCell.getObject().getID() == 542) {
                        if (player._isGhosts)
                            player.set_Alive();
                    }
                }
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.getMap(), player.getGuid());
                player.getMap().addPlayer(player);

                player.getMap().onPlayerArriveOnCell(player,
                        player.get_curCell().getID());

                removeAction(GA);
                return;
            }
            AtomicReference<String> pathRef = new AtomicReference<String>(path);
            int result = Pathfinding.isValidPath(player.getMap(), player
                    .get_curCell().getID(), pathRef, null);

            // Si déplacement inutile
            if (result == 0) {
                SocketManager.GAME_SEND_GA_PACKET(out, "", "0", "", "");
                removeAction(GA);
                return;
            }
            if (result != -1000 && result < 0)
                result = -result;

            // On prend en compte le nouveau path
            path = pathRef.get();
            // Si le path est invalide
            if (result == -1000) {
                GameServer.addToLog(player.getName() + "(" + player.getGuid()
                        + ") Tentative de  deplacement avec un path invalide");
                path = CryptManager.getHashedValueByInt(player
                        .get_orientation())
                        + CryptManager.cellID_To_Code(player.get_curCell()
                        .getID());
            }
            // On sauvegarde le path dans la variable
            GA._args = path;

            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(
                    player.getMap(),
                    "" + GA._id,
                    1,
                    player.getGuid() + "",
                    "a"
                            + CryptManager.cellID_To_Code(player.get_curCell()
                            .getID()) + path);
            addAction(GA);
            if (player.isSitted())
                player.setSitted(false);
            player.set_away(true);
        } else {
            Fighter F = player.getFight().getFighterByPerso(player);
            if (F == null)
                return;
            GA._args = path;
            player.getFight().fighterDeplace(F, GA);
        }
    }

    public void kick() {
        try {
            Main.gameServer.delClient(this);
            try {
                synchronized (account) {
                    if (account != null)
                        account.deconnexion();
                }
            } catch (NullPointerException e) {
            }
            if (socket != null) {
                if (!socket.isClosed())
                    socket.close();
            }
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void kick(int messageID, String... args) {
        String finalArg = "";
        boolean firstArg = true;
        for (String arg : args) {
            if (!firstArg) {
                finalArg += ";";
            } else {
                firstArg = false;
            }
            finalArg += arg;
        }
        SocketManager.REALM_SEND_MESSAGE_DECO(player, messageID, finalArg);
        kick();
    }

    public void kick(int messageID) {
        SocketManager.REALM_SEND_MESSAGE_DECO(player, messageID, "");
    }

    private void parseAccountPacket(String packet) {
        if (packet.charAt(1) != 'B' && player != null)
            return;
        switch (packet.charAt(1)) {
            case 'A':
                CharacterName.isValid(packet, this);
                break;

            case 'B':
                int stat = -1;
                try {
                    stat = Integer.parseInt(packet.substring(2).split("/u000A")[0]);
                    player.boostStat(stat);
                } catch (NumberFormatException e) {
                    return;
                }
                ;
                break;
            case 'g':// Cadeaux à la connexion
                int regalo = account.getCadeau();
                if (regalo != 0) {
                    String idModObjeto = Integer.toString(regalo, 16);
                    String efectos = World.getObjTemplate(regalo).getStrTemplate();
                    SocketManager.GAME_SEND_Ag_PACKET(out, regalo, "1~"
                            + idModObjeto + "~1~~" + efectos);
                }
                break;
            case 'G':
                cuenta_Entregar_Regalo(packet.substring(2));
                break;
            case 'D':
                String[] split = packet.substring(2).split("\\|");
                int GUID = Integer.parseInt(split[0]);
                String reponse = split.length > 1 ? split[1] : "";

                if (account.getPlayers().containsKey(GUID)) {
                    if (account.getPlayers().get(GUID).getLevel() < 20
                            || (account.getPlayers().get(GUID).getLevel() >= 20 && reponse
                            .equals(account.getAnwser()))) {
                        account.deletePerso(GUID);
                        SocketManager.GAME_SEND_PERSO_LIST(out,
                                account.getPlayers());
                    } else
                        SocketManager.GAME_SEND_DELETE_PERSO_FAILED(out);
                } else
                    SocketManager.GAME_SEND_DELETE_PERSO_FAILED(out);
                break;

            case 'f':
                int serverId = GameServer.id;
                SocketManager.MULTI_SEND_Af_PACKET(out, serverId);
                break;

            case 'i':
                account.setClientKey(packet.substring(2));
                break;

            case 'L':
                // TODO @Flow Fix getPlayers fonction
                Map<Integer, Player> persos = account.getPlayers();
                for (Player p : persos.values()) {
                    if (p.getFight() != null
                            && p.getFight().getFighterByPerso(p) != null) {
                        account.setGameThread(this);
                        player = p;
                        if (player != null) {
                            player.OnJoinGame();
                            return;
                        }
                    }
                }

                SocketManager.GAME_SEND_PERSO_LIST(out, persos);
                break;

            case 'S':
                int charID = Integer.parseInt(packet.substring(2));
                Player P = account.getPlayers().get(charID);

                if (P != null && P.getServer() == GameServer.id) {
                    account.setGameThread(this);
                    player = P;

                    if (player != null) {
                        player.OnJoinGame();
                        return;
                    }
                }
                SocketManager.GAME_SEND_PERSO_SELECTION_FAILED(out);
                break;

            case 'T':
                int guid;
                try {
                    guid = Integer.parseInt(packet.substring(2));
                } catch (Exception e) {
                    return;
                }

                account = World.getCompte(guid);

                if (account != null) {
                    String ip = socket.getInetAddress().getHostAddress();

                    if (account.getGameThread() != null) {
                        account.getGameThread().kick();
                    }
                    account.setGameThread(this);
                    account.setCurIP(ip);
                    SocketManager.GAME_SEND_ATTRIBUTE_SUCCESS(out);

                    SQLManager.SETONLINE(account.getGuid());
                } else {
                    SocketManager.GAME_SEND_ATTRIBUTE_FAILED(out);
                }
                break;

            case 'V':
                SocketManager.GAME_SEND_AV0(out);
                break;

            case 'P':
                SocketManager.send(out, "AP" + RandomCharacterName.get());
                break;
        }
    }

    private void cuenta_Entregar_Regalo(String packet) {
        String[] info = packet.split("\\|");
        int idObjeto = Integer.parseInt(info[0]);
        int idPj = Integer.parseInt(info[1]);
        Player pj = null;
        Item objeto = null;
        try {
            pj = World.getPlayer(idPj);
            objeto = World.getObjTemplate(idObjeto).createNewItem(1, true, -1);
        } catch (Exception e) {
        }
        if (pj == null || objeto == null) {
            return;
        }
        pj.addObjet(objeto, false);
        World.addObjet(objeto, true);
        account.setCadeau();
        SQLManager.ACTUALIZAR_REGALO(account);
        SocketManager.GAME_SEND_AGK_PACKET(out);
    }

    public Thread getThread() {
        return thread;
    }

    public void removeAction(GameAction GA) {
        // * DEBUG
        // System.out.println("Supression de la GameAction id = "+GA._id);
        // */
        actions.remove(GA._id);
    }

    public void addAction(GameAction GA) {
        actions.put(GA._id, GA);
        // * DEBUG
        // System.out.println("Ajout de la GameAction id = "+GA._id);
        // System.out.println("Packet: "+GA._packet);
        // */
    }

    private void Object_obvijevan_changeApparence(String packet) {
        int guid = -1;
        int pos = -1;
        int val = -1;
        try {
            guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
            pos = Integer.parseInt(packet.split("\\|")[1]);
            val = Integer.parseInt(packet.split("\\|")[2]);
        } catch (Exception e) {
            return;
        }
        if ((guid == -1) || (!player.hasItemGuid(guid)))
            return;
        Item obj = World.getObjet(guid);
        if ((val >= 21) || (val <= 0))
            return;

        obj.obvijevanChangeStat(972, val);
        SocketManager.send(player, obj.obvijevanOCO_Packet(pos));
        if (pos != -1)
            SocketManager.GAME_SEND_ON_EQUIP_ITEM(player.getMap(), player);
    }

    private void Object_obvijevan_feed(String packet) {
        int guid = -1;
        int pos = -1;
        int victime = -1;
        try {
            guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
            pos = Integer.parseInt(packet.split("\\|")[1]);
            victime = Integer.parseInt(packet.split("\\|")[2]);
        } catch (Exception e) {
            return;
        }

        if ((guid == -1) || (!player.hasItemGuid(guid)))
            return;
        Item obj = World.getObjet(guid);
        Item objVictime = World.getObjet(victime);
        obj.obvijevanNourir(objVictime);

        int qua = objVictime.getQuantity();
        if (qua <= 1) {
            player.removeItem(objVictime.getGuid());
            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player,
                    objVictime.getGuid());
        } else {
            objVictime.setQuantity(qua - 1);
            SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(player, objVictime);
        }
        SocketManager.send(player, obj.obvijevanOCO_Packet(pos));
    }

    private void Object_obvijevan_desassocier(String packet) {
        int guid = -1;
        int pos = -1;
        try {
            guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
            pos = Integer.parseInt(packet.split("\\|")[1]);
        } catch (Exception e) {
            return;
        }
        if ((guid == -1) || (!player.hasItemGuid(guid)))
            return;
        Item obj = World.getObjet(guid);
        int idOBVI = 0;

        if (obj.getObvijevanPos() != 0) { // On vérifie si il y a bien un obvi
            idOBVI = obj.getObviID();
        } else {
            SocketManager.GAME_SEND_MESSAGE(player, "ERROR", "000000");
        }

        Item.ObjTemplate t = World.getObjTemplate(idOBVI);
        Item obV = t.createNewItem(1, true, -1);
        String obviStats = obj.getObvijevanStatsOnly();
        if (obviStats == "") {
            SocketManager.GAME_SEND_MESSAGE(player, "ERROR", "000000");
            return;
        }
        obV.clearStats();
        obV.parseStringToStats(obviStats);
        if (player.addObjet(obV, true)) {
            World.addObjet(obV, true);
        }
        obj.removeAllObvijevanStats();
        SocketManager.send(player, obj.obvijevanOCO_Packet(pos));
        SocketManager.GAME_SEND_ON_EQUIP_ITEM(player.getMap(), player);
    }

    public Account getAccount() {
        return this.account;
    }

    public GameSendThread getOut() {
        return out;
    }
}
