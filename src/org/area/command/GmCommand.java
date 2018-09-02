package org.area.command;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.swing.Timer;

import jdk.internal.jfr.events.SocketReadEvent;
import lombok.Synchronized;
import org.area.client.Account;
import org.area.client.Player;
import org.area.command.player.Tickets;
import org.area.common.Constant;
import org.area.common.CryptManager;
import org.area.common.Formulas;
import org.area.common.Pathfinding;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.ItemSet;
import org.area.event.Event;
import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.fight.object.Monster.MobGroup;
import org.area.game.GameSendThread;
import org.area.game.GameServer;
import org.area.game.GameThread;
import org.area.game.tools.AllColor;
import org.area.game.tools.ParseTool;
import org.area.game.tools.Util;
import org.area.kernel.Config;
import org.area.kernel.Logs;
import org.area.kernel.Main;
import org.area.kernel.Reboot;
import org.area.object.*;
import org.area.object.AuctionHouse.HdvEntry;
import org.area.object.Item.ObjTemplate;
import org.area.object.Maps.MountPark;
import org.area.object.NpcTemplate.NPC;
import org.area.object.NpcTemplate.NPC_question;
import org.area.object.NpcTemplate.NPC_reponse;
import org.area.object.job.Job.StatsMetier;

public class GmCommand {
    Account _compte;
    Player _perso;
    GameSendThread _out;
    Timer _timer;

    //commande event
    public static boolean event = false;
    public static short eventMap;
    public static int eventCell;

    //commande bloque
    public static boolean isActive = true;

    //commande espion
    public static boolean isWhisper = false;
    public static Player whisper;

    public static Map<String, Integer> gmCommands = new TreeMap<String, Integer>();
    public static List<Integer> mapInterdites = Arrays.asList(13085, 13086, 13087, 13088, 13089);

    public GmCommand(Player perso) {
        this._compte = perso.getAccount();
        this._perso = perso;
        this._out = _compte.getGameThread().getOut();
    }

    public void consoleCommand(String packet) {

        if (_compte.getGmLevel() < 1) {
            _compte.getGameThread().closeSocket();
            return;
        }

        String msg = packet.substring(2);
        String[] infos = msg.split(" ");
        if (infos.length == 0)
            return;
        String command = infos[0];

        int gmLvl = -1;
        try {
            gmLvl = gmCommands.get(command.toLowerCase());
        } catch (Exception e) {
        }
        if (command.contains("help")) {
            System.out.println("aa");
            String content = "\nCommandes disponibles pour Gm " + _compte.getGmLevel() + "\n";
            for (Entry<String, Integer> gm : gmCommands.entrySet())
                if (gm.getValue() <= _compte.getGmLevel())
                    content += "\n" + gm.getKey();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, content);
            return;
        } else if (_compte.getGmLevel() < gmLvl) {
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le rang " + gmLvl + " est nécessaire pour éxécuter cette commande ! HELP pour la liste");
            return;
        }

        if (isActive) {
            commandGmOne(command, infos, msg);
            commandGmTwo(command, infos, msg);
            commandGmThree(command, infos, msg);
            commandGmFour(command, infos, msg);

            if (_compte.getName().equalsIgnoreCase("owner")) {
                commandGmFive(command, infos, msg);
            }
        } else if (!isActive) {
            if (_compte.getName().equalsIgnoreCase("owner")) {
                commandGmOne(command, infos, msg);
                commandGmTwo(command, infos, msg);
                commandGmThree(command, infos, msg);
                commandGmFour(command, infos, msg);
                commandGmFive(command, infos, msg);

                _perso.sendText(_compte.getName());
            } else {
                _perso.sendText("Les commandes ont été temporairement désactivées.");
                return;
            }

        }
    }

    public boolean commandGmOne(String command, String[] infos, String msg) {

        if (command.equalsIgnoreCase("VER")) {
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "V00002");
            return true;
        } else if (command.equalsIgnoreCase("STARTEVENT")) {

            if (_perso.getFight() != null) {
                String str = "Vous êtes en combat";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            if (!event) {

                String EventName = null;

                for (int i = 1; i < infos.length; i++) {
                    if (i == 1) {
                        EventName = infos[i];
                    } else {
                        EventName = EventName + " " + infos[i];
                    }
                }

                if (EventName == null) {
                    return true;
                }

                eventMap = _perso.getMap().get_id();
                if (mapInterdites.contains(eventMap)) {
                    return true;
                }
                eventCell = _perso.get_curCell().getID();
                event = true;

                SocketManager.GAME_SEND_MESSAGE_TO_ALL("Un event " + EventName + " est sur le point de débuter. Tapez .event pour y participer.", "CCFF00");
            } else if (event) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Un event est déjà en cours");
            }

        } else if (command.equalsIgnoreCase("ENDEVENT")) {
            String gagnant = infos[1];
            String events = infos[2];
            if (event) {
                event = false;
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Event terminé avec succès !");
                SocketManager.GAME_SEND_MESSAGE_TO_ALL("Bravo à " + gagnant + " qui remporte l'évent " + events + " ! Merci d'avoir participé.", "CCFF00");
            } else if (!event) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Aucun event n'est en cours. Veuillez entrer la commande STARTEVENT pour débuter un event");
            }
        } else if (command.equalsIgnoreCase("CELLCOMBAT")) {
            if (_perso.getFight() != null) {
                Fighter f = _perso.getFight().getFighterByPerso(_perso);
                int cellule = f.get_fightCell().getID();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Cellule id: " + cellule);
                if (cellule > 0) {
                    f.get_fightCell().getFighters().clear();
                    f.get_fightCell().addFighter(f);
                }
            }
        }
        if (command.equalsIgnoreCase("INFOS")) {
            long uptime = System.currentTimeMillis()
                    - Main.gameServer.getStartTime();
            int jour = (int) (uptime / (1000 * 3600 * 24));
            uptime %= (1000 * 3600 * 24);
            int hour = (int) (uptime / (1000 * 3600));
            uptime %= (1000 * 3600);
            int min = (int) (uptime / (1000 * 60));
            uptime %= (1000 * 60);
            int sec = (int) (uptime / (1000));

            String mess = jour + "j " + hour + "h " + min + "m " + sec + "s\n"
                    + "Joueurs en ligne : "
                    + Main.gameServer.getPlayerNumber() + "\n"
                    + "Record de joueurs connectés : "
                    + Main.gameServer.getMaxPlayer() + "\n";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("REFRESHMOBS")) {
            _perso.getMap().refreshSpawns();
            String mess = "Mob Spawn refreshed!";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        }
        /*else if (command.equalsIgnoreCase("VITALITE")) {
            _perso.get_baseStats().addOneStat(Constant.STATS_ADD_VITA, 100000000);
			_perso.refreshStats();
			return true;
		}*/
        /*else if (command.equalsIgnoreCase("ANTIFLOOD"))
        {
			if (infos[1] == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Syntax ERROR: ANTIFLOOD + PLAYER");
				return true;
			}
			Player P = World.getPersoByName(infos[1]);
			if (P == null) {
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
				return true;
			}
			if (!P.getAccount().isAFlooder())
			{
				P.getAccount().setAFlooder(true);
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("Le joueur <b>"+P.getName()+"</b> est désormais soumis à l'antiflood du serveur par le modérateur "+_perso.getName()+" !", Config.CONFIG_MOTD_COLOR);
			}
			else
			{
				P.getAccount().setAFlooder(false);
				P.getAccount().setFloodGrade(0);
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("Le modérateur <b>"+_perso.getName()+"</b> a désactivé l'antiflood actif sur le joueur "+P.getName()+" !", Config.CONFIG_MOTD_COLOR);
			}
			return true;
		}*/

        else if (command.equalsIgnoreCase("MAPINFO")) {
            String mess = "==========\n" + "Liste des Npcs de la carte:";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            Maps map = _perso.getMap();
            for (Entry<Integer, NPC> entry : map.get_npcs().entrySet()) {
                mess = entry.getKey() + " "
                        + entry.getValue().get_template().get_id() + " "
                        + entry.getValue().get_cellID() + " "
                        + entry.getValue().get_template().get_initQuestionID(map.get_id());
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
            mess = "Liste des groupes de monstres:";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            for (Entry<Integer, MobGroup> entry : map.getMobGroups().entrySet()) {
                mess = entry.getKey() + " " + entry.getValue().getCellID()
                        + " " + entry.getValue().getAlignement() + " "
                        + entry.getValue().getSize();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
            mess = "==========";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("COMBATSACTIFS")) {
            int nombreCombats = 0;
            for (GameThread GT : Main.gameServer.getClients()) {
                try {
                    Player p = GT.getPlayer();
                    if (p.getFight() != null) {
                        nombreCombats++;
                    }
                } catch (Exception e) {
                }
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Nombre de joueurs en combat: " + nombreCombats);
        } else if (command.equalsIgnoreCase("WHO")) {
            String mess = "==========\n" + "Liste des joueurs en ligne:";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            int nbIterration = 30, diff = 0;
            boolean full = false;
            if (infos.length > 1) {
                if (infos[1].equalsIgnoreCase("FULL")) {
                    full = true;
                }
            } else {
                diff = Main.gameServer.getClients().size() - 30;
            }
            for (GameThread GT : Main.gameServer.getClients()) {
                if (!full && nbIterration-- < 1) break;
                Player P = GT.getPlayer();
                if (P == null)
                    continue;
                mess = P.getName() + "(" + P.getGuid() + ") ";

                switch (P.get_classe()) {
                    case Constant.CLASS_FECA:
                        mess += "Fec";
                        break;
                    case Constant.CLASS_OSAMODAS:
                        mess += "Osa";
                        break;
                    case Constant.CLASS_ENUTROF:
                        mess += "Enu";
                        break;
                    case Constant.CLASS_SRAM:
                        mess += "Sra";
                        break;
                    case Constant.CLASS_XELOR:
                        mess += "Xel";
                        break;
                    case Constant.CLASS_ECAFLIP:
                        mess += "Eca";
                        break;
                    case Constant.CLASS_ENIRIPSA:
                        mess += "Eni";
                        break;
                    case Constant.CLASS_IOP:
                        mess += "Iop";
                        break;
                    case Constant.CLASS_CRA:
                        mess += "Cra";
                        break;
                    case Constant.CLASS_SADIDA:
                        mess += "Sad";
                        break;
                    case Constant.CLASS_SACRIEUR:
                        mess += "Sac";
                        break;
                    case Constant.CLASS_PANDAWA:
                        mess += "Pan";
                        break;
                    case Constant.CLASS_OUGINAK:
                        mess += "Oug";
                        break;
                    default:
                        mess += "Unk";
                }
                mess += " ";
                mess += (P.get_sexe() == 0 ? "M" : "F") + " ";
                mess += P.getLevel() + " ";
                mess += P.getMap().get_id() + "("
                        + P.getMap().getX() + "/"
                        + P.getMap().getY() + ") ";
                mess += P.getFight() == null ? "" : "Combat ";
                mess += "Pseudo : " + P.getAccount().getPseudo() + " IP: " + (P.getAccount().getGmLevel() > 0 ? "Inconnue" : P.getAccount().getCurIp());
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
            if (diff > 0) {
                mess = "Et " + diff + " autres personnages";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
            mess = "==========\n";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("SHOWFIGHTPOS")) {
            String mess = "Liste des StartCell [teamID][cellID]:";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            String places = _perso.getMap().get_placesStr();
            if (places.indexOf('|') == -1 || places.length() < 2) {
                mess = "Les places n'ont pas ete definies";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            String team0 = "", team1 = "";
            String[] p = places.split("\\|");
            try {
                team0 = p[0];
            } catch (Exception e) {
            }
            ;
            try {
                team1 = p[1];
            } catch (Exception e) {
            }
            ;
            mess = "Team 0:\n";
            for (int a = 0; a <= team0.length() - 2; a += 2) {
                String code = team0.substring(a, a + 2);
                mess += CryptManager.cellCode_To_ID(code);
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            mess = "Team 1:\n";
            for (int a = 0; a <= team1.length() - 2; a += 2) {
                String code = team1.substring(a, a + 2);
                mess += CryptManager.cellCode_To_ID(code) + " , ";
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("DEBUGSS")) {
            Config.DEBUG = true;
        } else if (command.equalsIgnoreCase("DEBUG"))

        {
            Player perso = _perso;
            try {
                perso = World.getPersoByName(infos[1]);
            } catch (Exception e) {
                perso = _perso;
            }
            if (perso == null) {
                String str = "Le personnage n'a pas ete trouve";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            SocketManager.GAME_SEND_GV_PACKET(perso);
            perso.set_duelID(-1);
            perso.set_ready(false);
            perso.fullPDV();
            try {
                perso.getFight().leftFight(perso, null, true);
            } catch (Exception e) {
            }
            perso.set_fight(null);
            SocketManager.GAME_SEND_GV_PACKET(perso);
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
            perso.get_curCell().addPerso(perso);
            return true;
        } else if (command.equalsIgnoreCase("POPALL")) {
            String message = "";
            message = infos[1];
            for (Player P : World.getOnlinePlayers())
                SocketManager.GAME_SEND_POPUP(P, message);
            return true;
        } else if (command.equalsIgnoreCase("PING")) {
            Player target = infos.length > 1 ? World.getPersoByName(infos[1]) : null;
            if (target == null) {
                target = _perso;
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le ping du joueur " + target.getName() + " est de " + target.ping + " ms");
            return true;
        } else if (command.equalsIgnoreCase("THREAD")) {
            Player target = infos.length > 1 ? World.getPersoByName(infos[1]) : null;
            if (target == null) {
                target = _perso;
            }
            long tid = 0;
            Account a = target.getAccount();
            if (a == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Compte null !");
            }
            try {
                tid = a.getGameThread().getThread().getId();
            } catch (Exception e) {}
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le TID du joueur " + target.getName() + " est " + tid);
            return true;
        }else if (command.equalsIgnoreCase("DISABLE_CHECKPOINTS")) {
            synchronized (World.checkpoints) {
                World.checkpoints.clear();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Les checkpoints sont désactivé, pour ré-activer les checkpoints utilisez la commande LOAD_CHECKPOINTS");
            }
            return true;
        } else if (command.equalsIgnoreCase("LOAD_CHECKPOINTS")) {
            synchronized (World.checkpoints) {
                SQLManager.LOAD_CHECKPOINT();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Les checkpoints ont bien été chargés !");
            }
            return true;
        } else if (command.equalsIgnoreCase("POPUP")) {
            String message = "";
            message = msg.split(" ", 4)[2];
            Player target = World.getPersoByName(infos[1]);
            SocketManager.GAME_SEND_POPUP(target, message);
            return true;
        } else if (command.equalsIgnoreCase("CREATEGUILD")) {
            Player perso = _perso;
            if (infos.length > 1) {
                perso = World.getPersoByName(infos[1]);
            }

            if (perso == null) {
                String mess = "Le personnage n'existe pas.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }

            if (!perso.isOnline()) {
                String mess = "Le personnage " + perso.getName()
                        + " n'etait pas connecte";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            if (perso.get_guild() != null || perso.getGuildMember() != null) {
                String mess = "Le personnage " + perso.getName()
                        + " a deja une guilde";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            perso.potionGuilde = false;
            SocketManager.GAME_SEND_gn_PACKET(perso);
            String mess = perso.getName()
                    + ": Panneau de creation de guilde ouvert";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("TOOGLEAGGRO")) {
            Player perso = _perso;

            String name = null;
            try {
                name = infos[1];
            } catch (Exception e) {
            }
            ;

            perso = World.getPersoByName(name);

            if (perso == null) {
                String mess = "Le personnage n'existe pas.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }

            perso.set_canAggro(!perso.canAggro());
            String mess = perso.getName();
            if (perso.canAggro())
                mess += " peut Configtenant etre aggresser";
            else
                mess += " ne peut plus etre agresser";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);

            if (!perso.isOnline()) {
                mess = "(Le personnage " + perso.getName()
                        + " n'etait pas connecte)";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
        } else if (command.equalsIgnoreCase("SPEED")) {
            int speed = Integer.valueOf(infos[2]);
            Player cible = World.getPersoByName(infos[1]);
            cible.set_Speed(speed);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "La vitesse de " + cible.getName() + " est maintenant de " + speed);
        } else if (command.equalsIgnoreCase("PACKET")) {
            String packet = String.valueOf(infos[1]);
            SocketManager.send(_perso, packet);
        } else if (command.equalsIgnoreCase("MUTEGLOBAL")) {
            try {
                Player perso = World.getPersoByName(infos[1]);
                int time = Integer.valueOf(infos[2]);
                if (perso == null) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Ce joueur n'existe pas.");
                } else if (time == 0) {
                    perso.isMuteFromGlobal = true;
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "" + infos[1] + " ne pourra plus parler dans le canal global.");
                } else {
                    perso.isMuteFromGlobal = true;
                    perso.timeMuted = System.currentTimeMillis() + Math.abs((time * 60000));
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez mute du canal global le joueur " + infos[1] + " pour " + time + " minutes.");
                }
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "La syntaxe est MUTEGLOBAL [NOM JOUEUR] [TEMPS EN MINUTES (0) si infini]");
            }
        } else if (command.equalsIgnoreCase("DEMUTEGLOBAL")) {
            try {
                Player perso = World.getPersoByName(infos[1]);
                if (perso == null) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Ce joueur n'existe pas.");
                } else {
                    perso.isMuteFromGlobal = false;
                    perso.timeMuted = -1;
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez bien demute du canal global le joueur" + infos[1]);
                }
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "La syntaxe est la suivante: DEMUTEGLOBAL + NOM DU JOUEUR");

            }
        } else if (command.equalsIgnoreCase("RELOADPRIXORNEMENTS")) {
            World.clearPrixOrnements();
            SQLManager.LOAD_ORNEMENTS_PRICE();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Les prix des ornements ont été rechargé.");
            for (Player p : World.getOnlinePlayers()) {
                if (p != null)
                    p.send("000Z" + World.obtenirListePrixData());
            }
        } else if (command.equalsIgnoreCase("CLEARCOMBATLISTE")) {
            try {
                _perso.getMap().clearFights();
            } catch (Exception e) {
            }
        } else if (command.equalsIgnoreCase("DEMORPH")) {
            Player target = _perso;
            if (infos.length > 1)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[1]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            int morphID = target.get_classe() * 10 + target.get_sexe();
            target.set_gfxID(morphID);
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.getMap(),
                    target.getGuid());
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.getMap(),
                    target);
            String str = "Le joueur a ete transforme";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("NAMEANNOUNCE")) {
            infos = msg.split(" ", 2);
            String prefix = "cs<font color='#CC0000'><b>[" + _perso.getName() + "]: </b></font>"
                    + "<font color='#FF6600'>" + infos[1] + "</font>";
            for (Player P : World.getOnlinePlayers())
                SocketManager.send(P, prefix);

            return true;
        }
        return false;
    }

    public boolean commandGmTwo(String command, String[] infos, String msg) {
        if (command.equalsIgnoreCase("MUTEMAP")) {
            if (infos.length < 2) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Vous devez préciser un temps en secondes");
                return true;
            }
            long time = Long.parseLong(infos[1]);
            Maps map = _perso.getMap();
            map.muteMap(time);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Vous avez effectué un mute sur la map actuelle.");
            return true;
        } else if (command.equalsIgnoreCase("ANNOUNCE")) {
            infos = msg.split(" ", 2);
            String mess = infos[1];
            SocketManager.GAME_SEND_MESSAGE_TO_ALL(mess,
                    Config.CONFIG_MOTD_COLOR);
            return true;
        } else if (command.equalsIgnoreCase("UNMUTEMAP")) {
            Maps map = _perso.getMap();
            if (map.isMuted()) {
                map.unMuteMap();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "La map a été démutée");
            } else {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "La map n'est pas mutée");
            }
            return true;

        } else if (command.equalsIgnoreCase("GONAME")
                || command.equalsIgnoreCase("JOIN")) {
            Player P = World.getPersoByName(infos[1]);
            if (P == null) {
                String str = "Le personnage n'existe pas";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            short mapID = P.getMap().get_id();
            int cellID = P.get_curCell().getID();

            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
                if (target.getFight() != null) {
                    String str = "La cible est en combat";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            if (!P.getMap().get_npcs().isEmpty() && target.getAccount().getGmLevel() < 4) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous ne pouvez pas téléporter un joueur sur une carte possédant un pnj !");
                return true;
            }

            if (_perso.getAccount().getGmLevel() < 2 && World.checkpoints.containsKey(mapID)) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Aucune téléportation donjon n'est permise sur votre compte !");
                return true;
            }
            target.teleport(mapID, cellID);
            String str = "Le joueur a ete teleporte";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("MENEUR-SUIVEUR")) {
            Config.MENEUR_SUIVEUR_ACTIVE = !Config.MENEUR_SUIVEUR_ACTIVE;
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, Config.MENEUR_SUIVEUR_ACTIVE ? "La fonctionnalité Meneur-suiveur est activé !" : "La fonctionnalité Meneur-suiveur est désactivé !");
        } else if (command.equalsIgnoreCase("TEST_UI")) {
            _perso.send("000X1");
        } else if (command.equalsIgnoreCase("NAMEGO")) {
            Player target = World.getPersoByName(infos[1]);
            if (target == null) {
                String str = "Le personnage n'existe pas";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            if (target.getFight() != null) {
                String str = "La cible est en combat";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player P = _perso;
            /*if (infos.length > 2)// Si un nom de perso est spécifié
            {
                P = World.getPersoByName(infos[2]);
                if (P == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }*/
            if (P.isOnline()) {
                short mapID = P.getMap().get_id();
                int cellID = P.get_curCell().getID();
                if (!P.getMap().get_npcs().isEmpty() && P.getAccount().getGmLevel() < 5) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous ne pouvez pas téléporter un joueur sur une carte possédant un pnj !");
                    return true;
                }
                if (_perso.getAccount().getGmLevel() < 2 && World.checkpoints.containsKey(mapID)) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Aucune téléportation donjon n'est permise sur votre compte !");
                    return true;
                }
                target.teleport(mapID, cellID);
                String str = "Le joueur a ete teleporte";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            } else {
                String str = "Le joueur n'est pas en ligne";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            }
        } else if (command.equalsIgnoreCase("TELEPORT")) {
            short mapID = -1;
            int cellID = -1;
            try {
                mapID = Short.parseShort(infos[1]);
                cellID = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }

            if (mapID == -1 || cellID == -1 || World.getCarte(mapID) == null) {
                String str = "MapID ou cellID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            if (World.getCarte(mapID).getCase(cellID) == null) {
                String str = "MapID ou cellID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (target.getAccount().getGmLevel() < 5 && mapInterdites.contains(mapID)) {
                return true;
            }
            if (_perso.getAccount().getGmLevel() < 2 && World.checkpoints.containsKey(mapID)) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Aucune téléportation donjon n'est permise sur votre compte !");
                return true;
            }
            if (infos.length > 3)// Si un nom de perso est spécifié
            {
                if (_perso.getAccount().getGmLevel() < 5) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous ne pouvez pas téléporter une personne autre que vous-même. Téléportez-vous d'abord et utilisez NAMEGO.");
                    return true;
                }
                target = World.getPersoByName(infos[3]);
                if (target == null || target.getFight() != null) {
                    String str = "Le personnage n'a pas ete trouve ou est en combat";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            target.teleport(mapID, cellID);
            String str = "Le joueur a ete teleporte";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("GOMAP")) {
            int mapX = 0;
            int mapY = 0;
            int cellID = 311;
            int contID = 0;// Par défaut Amakna
            try {
                mapX = Integer.parseInt(infos[1]);
                mapY = Integer.parseInt(infos[2]);
                cellID = Integer.parseInt(infos[3]);
                contID = Integer.parseInt(infos[4]);
            } catch (Exception e) {
            }
            ;
            Maps map = World.getCarteByPosAndCont(mapX, mapY, contID);
            if (map == null) {
                String str = "Position ou continent invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            if (map.getCase(cellID) == null) {
                String str = "CellID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 5)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[5]);
                if (target == null || target.getFight() != null) {
                    String str = "Le personnage n'a pas ete trouve ou est en combat";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
                if (target.getFight() != null) {
                    String str = "La cible est en combat";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            target.teleport(map.get_id(), cellID);
            String str = "Le joueur a ete teleporte";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("MAPANNOUNCE")) {
            if (infos.length >= 2) {
                String message = msg.split(" ", 2)[1];
                message = BeautifullMessage(message);
                SocketManager.GAME_SEND_Im_PACKET_TO_MAP(_perso.getMap(),
                        new StringBuilder("1243;").append(_perso.getName())
                                .append("~").append(message).toString());
                for (Fight f : _perso.getMap().get_fights2().values()) {
                    SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(f, 7,
                            new StringBuilder("1243;")
                                    .append(_perso.getName()).append("~")
                                    .append(message).toString());
                }
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Message envoyé.");
            } else
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Vous n'avez pas précisé de message.");
            return true;
        } else if (command.equalsIgnoreCase("MUTE")) {
            Player perso = _perso;
            String message = "";
            String name = null;
            int time = 0;
            try {
                name = infos[1];
                time = Integer.parseInt(infos[2]);
                if (infos.length >= 4)
                    message = msg.split(" ", 4)[3];
            } catch (Exception e) {
            }
            ;

            perso = World.getPersoByName(name);
            if (perso == null || time <= 0) {
                String mess = "Le personnage n'existe pas ou la duree est invalide.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            String mess = "Vous avez mute " + perso.getName() + " pour "
                    + time + " minutes";
            if (perso.getAccount() == null) {
                mess = "(Le personnage " + perso.getName()
                        + " n'etait pas connecte)";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            perso.getAccount().mute(time, message, _perso.getName());
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);

            if (!perso.isOnline()) {
                mess = "(Le personnage " + perso.getName()
                        + " n'etait pas connecte)";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            } else {
                //String temps_str = time+" minute" + ((time > 1)?"s":"");
                /*SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1242;"
                        + perso.getName() + "~" + _perso.getName() + "~" + temps_str + "~"
						+ message);*/

                SocketManager.GAME_SEND_MESSAGE_TO_ALL("Le joueur <b>" + perso.getName() + "</b> a été mute par " + _perso.getName() + " pour la raison suivante : " + message, AllColor.RED);
            }
            return true;
        } else if (command.equalsIgnoreCase("UNMUTE")) {
            Player perso = _perso;

            String name = null;
            try {
                name = infos[1];
            } catch (Exception e) {
            }
            ;

            perso = World.getPersoByName(name);
            if (perso == null) {
                String mess = "Le personnage n'existe pas.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }

            perso.getAccount().unMute();
            String mess = "Vous avez unmute " + perso.getName();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);

            if (!perso.isOnline()) {
                mess = "(Le personnage " + perso.getName()
                        + " n'etait pas connecte)";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
        } else if (command.equalsIgnoreCase("KICK")) {
            Player perso = _perso;
            String name = null;
            String message = "";
            try {
                name = infos[1];

                if (infos.length >= 3)
                    message = msg.split(" ", 3)[2];

            } catch (Exception e) {
            }

            perso = World.getPersoByName(name);
            if (perso == null) {
                String mess = "Le personnage n'existe pas.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            if (perso.getAccount() != null) {
                if (perso.getAccount().getGmLevel() > _perso.getAccount().getGmLevel()) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Impossible su un GM supérieur à vous");
                    return true;
                }
            }
            if (perso.isOnline()) {
                perso.getAccount().getGameThread().kick(18, _perso.getName(), "Voici la raison de l'expulsion: " + message);
                String mess = "Vous avez kick " + perso.getName();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            } else {
                String mess = "Le personnage " + perso.getName()
                        + " n'est pas connecte";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                SocketManager.GAME_SEND_MESSAGE_TO_ALL("Le joueur <b>" + perso.getName() + "</b> a été kick pour la raison suivante : " + message, AllColor.RED);
            }
        } else if (command.equalsIgnoreCase("KILL")) {
            Player perso = _perso;
            String name = null;
            try {
                name = infos[1];
            } catch (Exception e) {
            }

            perso = World.getPersoByName(name);
            if (perso == null) {
                String mess = "Le personnage n'existe pas.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            if (perso.isOnline()) {
                perso.resetVars();
                SQLManager.SAVE_PERSONNAGE(perso, true);
                World.unloadPerso(perso.getGuid());
                perso.refresh();
                //perso.getAccount().getGameThread().kick();
                String mess = "Vous avez tuer le thread de " + perso.getName();
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            } else {
                String mess = "Le personnage " + perso.getName()
                        + " n'est pas connecte";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            }
        } else if (command.equalsIgnoreCase("SETALIGN")) {
            byte align = -1;
            try {
                align = Byte.parseByte(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (align < Constant.ALIGNEMENT_NEUTRE
                    || align > Constant.ALIGNEMENT_MERCENAIRE) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }

            target.modifAlignement(align);

            String str = "L'alignement du joueur a ete modifie";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("H")) {
            if (infos.length > 1)// Si un nom de perso est spécifié
            {
                Player target = World.getPersoByName(infos[1]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
                target.getAccount().isHelper = !target.getAccount().isHelper;
                SQLManager.UPDATE_ACCOUNT_DATA(target.getAccount());
                String str;
                if (target.getAccount().isHelper) {
                    str = target.getName() + " est désormais un helper !";
                } else {
                    str = target.getName() + " n'est plus un helper !";
                }
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            } else {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Commande incompléte, entrer un nom de personnage !");
            }
        } else if (command.equalsIgnoreCase("RESTREINT")) {
            if (infos.length > 1)// Si un nom de perso est spécifié
            {
                Player target = World.getPersoByName(infos[1]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
                target.isRestricted = true;
                SQLManager.UPDATE_ACCOUNT_DATA(target.getAccount());
                String str = "";
                if (target.isRestricted) {
                    str = target.getName() + " est désormais restreint ! Pour enlever la restriction, contacter un administrateur !";
                }
                SQLManager.SAVE_PLAYER_RESTRICTION(target);
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            } else {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Commande incompléte, entrer un nom de personnage !");
            }
        } else if (command.equalsIgnoreCase("SETREPONSES")) {
            if (infos.length < 3) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Il manque un/des arguments");
                return true;
            }
            int id = 0;
            try {
                id = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            String reps = infos[2];
            NPC_question Q = World.getNPCQuestion(id);
            String str = "";
            if (id == 0 || Q == null) {
                str = "QuestionID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Q.setReponses(reps);
            boolean a = SQLManager.UPDATE_NPCREPONSES(id, reps);
            str = "Liste des reponses pour la question " + id + ": "
                    + Q.getReponses();
            if (a)
                str += "(sauvegarde dans la BDD)";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            return true;
        } else if (command.equalsIgnoreCase("SHOWREPONSES")) {
            int id = 0;
            try {
                id = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            NPC_question Q = World.getNPCQuestion(id);
            String str = "";
            if (id == 0 || Q == null) {
                str = "QuestionID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            str = "Liste des reponses pour la question " + id + ": "
                    + Q.getReponses();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            return true;
        } else if (command.equalsIgnoreCase("ADDJOBXP")) {
            int job = -1;
            int xp = -1;
            try {
                job = Integer.parseInt(infos[1]);
                xp = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            if (job == -1 || xp < 0) {
                String str = "Valeurs invalides";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 3)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[3]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            StatsMetier SM = target.getMetierByID(job);
            if (SM == null) {
                String str = "Le joueur ne connais pas le metier demande";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            SM.addXp(target, xp);

            String str = "Le metier a ete experimenter";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("LEARNJOB")) {
            int job = -1;
            try {
                job = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (job == -1 || World.getMetier(job) == null) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }

            target.learnJob(World.getMetier(job));

            String str = "Le metier a ete appris";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("SIZE")) {
            int size = -1;
            try {
                size = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (size == -1) {
                String str = "Taille invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            target.set_size(size);
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.getMap(),
                    target.getGuid());
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.getMap(),
                    target);
            String str = "La taille du joueur a ete modifiee";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("MORPH")) {
            int morphID = -1;
            try {
                morphID = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (morphID == -1) {
                String str = "MorphID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            target.set_gfxID(morphID);
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.getMap(),
                    target.getGuid());
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.getMap(),
                    target);
            String str = "Le joueur a ete transforme";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        }
        if (command.equalsIgnoreCase("MOVENPC")) {
            int id = 0;
            try {
                id = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            NPC npc = _perso.getMap().getNPC(id);
            if (id == 0 || npc == null) {
                String str = "Npc GUID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            int exC = npc.get_cellID();
            // on l'efface de la map
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(),
                    id);
            // on change sa position/orientation
            npc.setCellID(_perso.get_curCell().getID());
            npc.setOrientation((byte) _perso.get_orientation());
            // on envoie la modif
            SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.getMap(), npc);
            String str = "Le PNJ a ete deplace";
            if (_perso.get_orientation() == 0 || _perso.get_orientation() == 2
                    || _perso.get_orientation() == 4
                    || _perso.get_orientation() == 6)
                str += " mais est devenu invisible (orientation diagonale invalide).";
            if (SQLManager.DELETE_NPC_ON_MAP(_perso.getMap().get_id(),
                    exC)
                    && SQLManager.ADD_NPC_ON_MAP(
                    _perso.getMap().get_id(), npc.get_template()
                            .get_id(), _perso.get_curCell().getID(),
                    _perso.get_orientation()))
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            else
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Erreur au moment de sauvegarder la position");
        } else if (command.equalsIgnoreCase("BAN")) {
            String message = "";
            int nb_heures = -1;
            Player P = null;
            try {
                P = World.getPersoByName(infos[1]);
                nb_heures = Integer.parseInt(infos[2]);

                if (infos.length >= 4) message = msg.split(" ", 4)[3];

            } catch (Exception e) {
            }
            if (nb_heures < 0) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Commande ban : ban PSEUDO TEMPS_EN_HEURES RAISON");
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Un temps de 0 équivaut à un ban définitf");
                return true;
            }
            if (P == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le personnage n'existe pas.");
                return true;
            }
            if (P.getAccount() == null) SQLManager.LOAD_ACCOUNT(P.getAccID());
            if (P.getAccount() == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le compte du personnage n'existe plus.");
                return true;
            }
            /*if (P.getAccount().getGmLevel() >= _perso.getAccount().getGmLevel()) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Impossible sur un GM supérieur ou égal à vous");
                return true;
            }*/
            //On peut le bannir pour de bon
            StringBuilder im_mess = new StringBuilder("1241;").append(P.getName()).append("~").append(_perso.getName()).append("~");
            if (nb_heures != 0) {
                if (nb_heures > 1) im_mess.append(nb_heures).append(" heures");
                else im_mess.append(nb_heures).append(" heure");
                P.getAccount().ban(nb_heures * 3600, false);
            } else {
                im_mess.append("définitivement");
                P.getAccount().ban(-1, false);
            }
            im_mess.append("~").append(message);
            String duree = "permanente !";
            if (nb_heures > 0) {
                duree = nb_heures + " heures.";
            }
            if (P.getAccount().getGameThread() != null)
                P.getAccount().getGameThread().kick(40, "Le membre du staff " + _perso.getName() + " vous a banni pour la raison suivante: " + message + ". La durée du ban est " + duree);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez banni " + P.getName());
            SocketManager.GAME_SEND_MESSAGE_TO_STAFF("Le joueur <b>" + P.getName() + "</b> a été banni pour la raison suivante : " + message, AllColor.RED);
            return true;
        } else if (command.equalsIgnoreCase("PDVPER")) {
            int count = 0;
            try {
                count = Integer.parseInt(infos[1]);
                if (count < 0)
                    count = 0;
                if (count > 100)
                    count = 100;
                Player perso = _perso;
                if (infos.length == 3)// Si le nom du perso est spécifié
                {
                    String name = infos[2];
                    perso = World.getPersoByName(name);
                    if (perso == null)
                        perso = _perso;
                }
                int newPDV = perso.get_PDVMAX() * count / 100;
                perso.set_PDV(newPDV);
                if (perso.isOnline())
                    SocketManager.GAME_SEND_STATS_PACKET(perso);
                String mess = "Vous avez fixer le pourcentage de pdv de "
                        + perso.getName() + " a " + count;
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Valeur incorecte");
                return true;
            }
            ;
        } else if (command.equalsIgnoreCase("MEOW")) {
            String name = infos[1];
            Player perso = World.getPersoByName(name);
            if (perso == null)
                perso = _perso;
            int guid = perso.getGuid();
            SocketManager.MEOW(name, guid, perso);
        } else if (command.equalsIgnoreCase("KAMAS")) {
            long count = 0;
            try {
                count = Long.parseLong(infos[1]);
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Valeur incorecte");
                return true;
            }
            if (count == 0)
                return true;
            if (_perso.getAccount().getGmLevel() < 4) {
                if (_perso.getAccount().kamasGiven > 199999) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                            "Vous avez atteint la limite de kamas que vous pouvez distribuer");
                    return true;
                }
            }
            Player perso = _perso;
            if (infos.length == 3)// Si le nom du perso est spécifié
            {
                String name = infos[2];
                perso = World.getPersoByName(name);
                if (perso == null)
                    perso = _perso;
            }
            perso.addKamas(count);
            _perso.getAccount().kamasGiven += count;
            Logs.addToMjLog(_perso.getName() + " a distribué " + count + "kamas à " + perso.getAccount().getGuid());
            if (perso.isOnline())
                SocketManager.GAME_SEND_STATS_PACKET(perso);
            String mess = "Vous avez ";
            mess += (count < 0 ? "retirer" : "ajouter") + " ";
            mess += Math.abs(count) + " kamas a " + perso.getName();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
        } else if (command.equalsIgnoreCase("SPAWN")) {
            String Mob = null;
            try {
                Mob = infos[1];
            } catch (Exception e) {
            }
            ;
            if (Mob == null)
                return true;
            _perso.getMap().spawnGroupOnCommand(
                    _perso.get_curCell().getID(), Mob);
        } else if (command.equalsIgnoreCase("RELOADENCLOS")) {
            System.out
                    .println("Suppression des dragodindes de l'enclos sur la MapID .enclos.");
            SQLManager.RESET_MOUNTPARKS();
            SQLManager.LOAD_MOUNTPARKS();
            System.out
                    .println("Suppression des dragodindes de l'enclos sur la MapID .enclos : OK !");
            SocketManager
                    .GAME_SEND_Im_PACKET_TO_ALL("116;<b>(Information)</b> : L'enclos publique en <b>.enclos</b> vient d'être vidé.");
        } else if (command.equalsIgnoreCase("EXIT")) {
            Reboot.reboot();
        } else if (command.equalsIgnoreCase("TITLE")) {
            Player target = null;
            int TitleID = 0;
            try {
                target = World.getPersoByName(infos[1]);
                TitleID = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;

            if (target == null) {
                String str = "Le personnage n'a pas ete trouve";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            target.set_title(TitleID);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Titre mis en place.");
            SQLManager.SAVE_PERSONNAGE(target, false);
            if (target.getFight() == null)
                SocketManager.GAME_SEND_ALTER_GM_PACKET(target.getMap(),
                        target);
        }
        return false;
    }

    public boolean commandGmThree(String command, String[] infos, String msg) {

        if (command.equalsIgnoreCase("SPAWNSTARS")) {

        } else if (command.equalsIgnoreCase("SAVE") && !Config.IS_SAVING) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1164");
                    World.saveAll(null);
                    SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1165");
                }
            });
            t.start();
            String mess = "Sauvegarde lancee!";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("GETCOORD")) {
            int cell = _perso.get_curCell().getID();
            String mess = "["
                    + Pathfinding.getCellXCoord(_perso.getMap(), cell)
                    + ","
                    + Pathfinding.getCellYCoord(_perso.getMap(), cell)
                    + "]";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("DELFIGHTPOS")) {
            int cell = -1;
            try {
                cell = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            if (cell < 0 || _perso.getMap().getCase(cell) == null) {
                cell = _perso.get_curCell().getID();
            }
            String places = _perso.getMap().get_placesStr();
            String[] p = places.split("\\|");
            String newPlaces = "";
            String team0 = "", team1 = "";
            try {
                team0 = p[0];
            } catch (Exception e) {
            }
            ;
            try {
                team1 = p[1];
            } catch (Exception e) {
            }
            ;

            for (int a = 0; a <= team0.length() - 2; a += 2) {
                String c = p[0].substring(a, a + 2);
                if (cell == CryptManager.cellCode_To_ID(c))
                    continue;
                newPlaces += c;
            }
            newPlaces += "|";
            for (int a = 0; a <= team1.length() - 2; a += 2) {
                String c = p[1].substring(a, a + 2);
                if (cell == CryptManager.cellCode_To_ID(c))
                    continue;
                newPlaces += c;
            }
            _perso.getMap().setPlaces(newPlaces);
            if (!SQLManager.SAVE_MAP_DATA(_perso.getMap()))
                return true;
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Les places ont ete modifiees (" + newPlaces + ")");
            return true;
        } else if (command.equalsIgnoreCase("UNBAN")) {
            Player P = World.getPersoByName(infos[1]);
            String message = "";
            if (infos.length >= 3) message = msg.split(" ", 3)[2];
            if (P == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Personnage non trouve");
                return true;
            }
            if (P.getAccount() == null)
                SQLManager.LOAD_ACCOUNT(P.getAccID());
            if (P.getAccount() == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur");
                return true;
            }
            P.getAccount().unBan();
            StringBuilder im_mess = new StringBuilder("1244;").append(P.getName()).append("~").append(_perso.getName()).append("~").append(message);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez debanni " + P.getName());
            SocketManager.GAME_SEND_Im_PACKET_TO_ALL(im_mess.toString());
            return true;
        } else if (command.equalsIgnoreCase("NBSUIVEURS")) {
            Integer nb = Integer.valueOf(infos[1]);
            _perso.nbSuiveurs = nb;
            _perso.refresh();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le nombre de suiveur a été modifié !");
            return true;
        } else if (command.equalsIgnoreCase("SETSUIVEUR")) {
            Integer suiveur = Integer.valueOf(infos[1]);
            _perso.setCurrentFolower(suiveur);
            _perso.refresh();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le nombre de suiveur a été modifié !");
            return true;
        } else if (command.equalsIgnoreCase("ADDFIGHTPOS")) {
            int team = -1;
            int cell = -1;
            try {
                team = Integer.parseInt(infos[1]);
                cell = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            if (team < 0 || team > 1) {
                String str = "Team ou cellID incorects";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            if (cell < 0 || _perso.getMap().getCase(cell) == null
                    || !_perso.getMap().getCase(cell).isWalkable(true)) {
                cell = _perso.get_curCell().getID();
            }
            String places = _perso.getMap().get_placesStr();
            String[] p = places.split("\\|");
            boolean already = false;
            String team0 = "", team1 = "";
            try {
                team0 = p[0];
            } catch (Exception e) {
            }
            ;
            try {
                team1 = p[1];
            } catch (Exception e) {
            }
            ;
            for (int a = 0; a <= team0.length() - 2; a += 2)
                if (cell == CryptManager.cellCode_To_ID(team0.substring(a,
                        a + 2)))
                    already = true;
            for (int a = 0; a <= team1.length() - 2; a += 2)
                if (cell == CryptManager.cellCode_To_ID(team1.substring(a,
                        a + 2)))
                    already = true;
            if (already) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "La case est deja dans la liste");
                return true;
            }
            if (team == 0)
                team0 += CryptManager.cellID_To_Code(cell);
            else if (team == 1)
                team1 += CryptManager.cellID_To_Code(cell);

            String newPlaces = team0 + "|" + team1;

            _perso.getMap().setPlaces(newPlaces);
            if (!SQLManager.SAVE_MAP_DATA(_perso.getMap()))
                return true;
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Les places ont ete modifiees (" + newPlaces + ")");
            return true;
        } else if (command.equalsIgnoreCase("SETMAXGROUP")) {
            infos = msg.split(" ", 4);
            byte id = -1;
            try {
                id = Byte.parseByte(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (id == -1) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            String mess = "Le nombre de groupe a ete fixe";
            _perso.getMap().setMaxGroup(id);
            boolean ok = SQLManager.SAVE_MAP_DATA(_perso.getMap());
            if (ok)
                mess += " et a ete sauvegarder a la BDD";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
        } else if (command.equalsIgnoreCase("ADDREPONSEACTION")) {
            infos = msg.split(" ", 4);
            int id = -30;
            int repID = 0;
            String args = infos[3];
            try {
                repID = Integer.parseInt(infos[1]);
                id = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            NPC_reponse rep = World.getNPCreponse(repID);
            if (id == -30 || rep == null) {
                String str = "Au moins une des valeur est invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            String mess = "L'action a ete ajoute";

            rep.addAction(new Action(id, args, ""));
            boolean ok = SQLManager.ADD_REPONSEACTION(repID, id, args);
            if (ok)
                mess += " et ajoute a la BDD";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
        } else if (command.equalsIgnoreCase("SETINITQUESTION")) {
            infos = msg.split(" ", 4);
            int id = -30;
            int q = 0;
            try {
                q = Integer.parseInt(infos[2]);
                id = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (id == -30) {
                String str = "NpcID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            String mess = "L'action a ete ajoute";
            NpcTemplate npc = World.getNPCTemplate(id);

            npc.setInitQuestion(q);
            boolean ok = SQLManager.UPDATE_INITQUESTION(id, q);
            if (ok)
                mess += " et ajoute a la BDD";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
        } else if (command.equalsIgnoreCase("ADDENDFIGHTACTION")) {
            infos = msg.split(" ", 4);
            int id = -30;
            int type = 0;
            String args = infos[3];
            String cond = infos[4];
            try {
                type = Integer.parseInt(infos[1]);
                id = Integer.parseInt(infos[2]);

            } catch (Exception e) {
            }
            ;
            if (id == -30) {
                String str = "Au moins une des valeur est invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            String mess = "L'action a ete ajoute";
            _perso.getMap().addEndFightAction(type,
                    new Action(id, args, cond));
            boolean ok = SQLManager.ADD_ENDFIGHTACTION(_perso.getMap()
                    .get_id(), type, id, args, cond);
            if (ok)
                mess += " et ajoute a la BDD";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            return true;
        } else if (command.equalsIgnoreCase("SPAWNFIX")) {
            String groupData = infos[1];

            _perso.getMap().addStaticGroup(_perso.get_curCell().getID(),
                    groupData, 0, 0, 0);
            String str = "Le grouppe a ete fixe";
            // Sauvegarde DB de la modif
            if (SQLManager.SAVE_NEW_FIXGROUP(_perso.getMap().get_id(),
                    _perso.get_curCell().getID(), groupData, 0))
                str += " et a ete sauvegarde dans la BDD";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            return true;
        } else if (command.equalsIgnoreCase("ADDNPC")) {
            int id = 0;
            try {
                id = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (id == 0 || World.getNPCTemplate(id) == null) {
                String str = "NpcID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            NPC npc = _perso.getMap().addNpc(id,
                    _perso.get_curCell().getID(), _perso.get_orientation(), 0, "");
            SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.getMap(), npc);
            String str = "Le PNJ a ete ajoute";
            if (_perso.get_orientation() == 0 || _perso.get_orientation() == 2
                    || _perso.get_orientation() == 4
                    || _perso.get_orientation() == 6)
                str += " mais est invisible (orientation diagonale invalide).";

            if (SQLManager.ADD_NPC_ON_MAP(_perso.getMap().get_id(), id,
                    _perso.get_curCell().getID(), _perso.get_orientation()))
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            else
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Erreur au moment de sauvegarder la position");
        } else if (command.equalsIgnoreCase("DELNPC")) {
            int id = 0;
            try {
                id = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            NPC npc = _perso.getMap().getNPC(id);
            if (id == 0 || npc == null) {
                String str = "Npc GUID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            int exC = npc.get_cellID();
            // on l'efface de la map
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(),
                    id);
            _perso.getMap().removeNpcOrMobGroup(id);

            String str = "Le PNJ a ete supprime";
            if (SQLManager.DELETE_NPC_ON_MAP(_perso.getMap().get_id(),
                    exC))
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            else
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Erreur au moment de sauvegarder la position");

        } else if (command.equalsIgnoreCase("VIEWSTUFF")) {
            Player P = null;
            try {
                P = World.getPersoByName(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (P == null || !P.isOnline()) {
                String str = "Le personnage n'a pas ete trouve.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            //Récupération du stuff
            int nb_ob = 0;
            StringBuilder mess = new StringBuilder();
            StringBuilder mess_items = new StringBuilder();
            Item obj = P.getObjetByPos(Constant.ITEM_POS_COIFFE);
            if (obj != null) {
                mess.append("Â°0, ");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de coiffe, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_CAPE);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de cape, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_FAMILIER);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de familier, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_AMULETTE);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas d'amulette, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_CEINTURE);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de ceinture, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_BOTTES);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de bottes.");
            SocketManager.GAME_SEND_cMK_PACKET(_perso, "F", P.getGuid(), P.getName(), mess.append("|").append(mess_items).toString());
            //SUITE DES ITEMS
            nb_ob = 0;
            mess = new StringBuilder();
            mess_items = new StringBuilder();
            obj = P.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
            if (obj != null) {
                mess.append("Â°0, ");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas d'anneau 1, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas d'anneau 2, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_ARME);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas d'arme, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_DOFUS1);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de dofus 1, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_DOFUS2);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de dofus 2, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_DOFUS3);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de dofus 3.");
            SocketManager.GAME_SEND_cMK_PACKET(_perso, "F", P.getGuid(), P.getName(), mess.append("|").append(mess_items).toString());
            //SUITE ET FIN ITEMS
            nb_ob = 0;
            mess = new StringBuilder();
            mess_items = new StringBuilder();
            obj = P.getObjetByPos(Constant.ITEM_POS_BOUCLIER);
            if (obj != null) {
                mess.append("Â°0, ");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de bouclier, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_DOFUS4);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de dofus 4, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_DOFUS5);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de dofus 5, ");
            obj = P.getObjetByPos(Constant.ITEM_POS_DOFUS6);
            if (obj != null) {
                mess.append("Â°").append(nb_ob).append(", ");
                if (nb_ob != 0) mess_items.append("!");
                mess_items.append(obj.getTemplate(false).getID()).append("!").append(obj.parseStatsString());
                nb_ob++;
            } else mess.append("Pas de dofus 6.");
            SocketManager.GAME_SEND_cMK_PACKET(_perso, "F", P.getGuid(), P.getName(), mess.append("|").append(mess_items).toString());
            return true;
        } else if (command.equalsIgnoreCase("BANIP")) {
            String message = "";
            int nb_heures = -1;
            Player P = null;
            try {
                P = World.getPersoByName(infos[1]);
                nb_heures = Integer.parseInt(infos[2]);
                if (infos.length >= 4) message = msg.split(" ", 4)[3];
            } catch (Exception e) {
            }
            if (nb_heures < 0) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Commande banip : banip PSEUDO TEMPS_EN_HEURES RAISON");
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Un temps de 0 équivaut à un ban définitif.");
                return true;
            }
            if (P == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le personnage n'existe pas.");
                return true;
            }
            if (P.getAccount() == null) SQLManager.LOAD_ACCOUNT(P.getAccID());
            if (P.getAccount() == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le compte du personnage n'existe plus.");
                return true;
            }
            if (P.getAccount().getGmLevel() >= _perso.getAccount().getGmLevel()) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Impossible sur un GM supérieur ou égal à vous");
                return true;
            }
            //On peut le bannir pour de bon
            StringBuilder im_mess = new StringBuilder("1245;").append(P.getName()).append("~").append(_perso.getName()).append("~");
            if (nb_heures != 0) {
                if (nb_heures > 1) im_mess.append(nb_heures).append(" heures");
                else im_mess.append(nb_heures).append(" heure");
                World.Banip(P.getAccount(), nb_heures);
            } else {
                im_mess.append("définitivement");
                World.Banip(P.getAccount(), 0);
            }
            im_mess.append("~").append(message);
            if (P.getAccount().getGameThread() != null) P.getAccount().getGameThread().kick();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez banni l'ip de " + P.getName());
            SocketManager.GAME_SEND_Im_PACKET_TO_ALL(im_mess.toString());
            SocketManager.GAME_SEND_MESSAGE_TO_ALL("Le joueur <b>" + P.getName() + "</b> a été banni pour la raison suivante : " + message, AllColor.RED);
            return true;
        } else if (command.equalsIgnoreCase("UNBANIP")) {
            Player P = World.getPersoByName(infos[1]);
            String message = "";
            if (infos.length >= 3) message = msg.split(" ", 3)[2];
            if (P == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Personnage non trouve");
                return true;
            }
            if (P.getAccount() == null)
                SQLManager.LOAD_ACCOUNT(P.getAccID());
            if (P.getAccount() == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur");
                return true;
            }
            World.unBanip(P.getAccount());
            StringBuilder im_mess = new StringBuilder("1246;").append(P.getName()).append("~").append(_perso.getName()).append("~").append(message);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez debanni l'ip de " + P.getName());
            SocketManager.GAME_SEND_Im_PACKET_TO_ALL(im_mess.toString());
            return true;
        } else if (command.equalsIgnoreCase("DELTRIGGER")) {
            int cellID = -1;
            try {
                cellID = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (cellID == -1 || _perso.getMap().getCase(cellID) == null) {
                String str = "CellID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            _perso.getMap().getCase(cellID).clearOnCellAction();
            boolean success = SQLManager.REMOVE_TRIGGER(_perso.getMap()
                    .get_id(), cellID);
            String str = "";
            if (success)
                str = "Le trigger a ete retire";
            else
                str = "Le trigger n'a pas ete retire";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("ADDTRIGGER")) {
            int actionID = -1;
            String args = "", cond = "";
            try {
                actionID = Integer.parseInt(infos[1]);
                args = infos[2];
                cond = infos[3];
            } catch (Exception e) {
            }
            ;
            if (args.equals("") || actionID <= -3) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            _perso.get_curCell().addOnCellStopAction(actionID, args, cond);
            boolean success = SQLManager.SAVE_TRIGGER(_perso.getMap()
                            .get_id(), _perso.get_curCell().getID(), actionID, 1, args,
                    cond);
            String str = "";
            if (success)
                str = "Le trigger a ete ajoute";
            else
                str = "Le trigger n'a pas ete ajoute";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("DELNPCITEM")) {
            if (_compte.getGmLevel() < 3)
                return true;
            int npcGUID = 0;
            int itmID = -1;
            try {
                npcGUID = Integer.parseInt(infos[1]);
                itmID = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            NpcTemplate npc = _perso.getMap().getNPC(npcGUID).get_template();
            if (npcGUID == 0 || itmID == -1 || npc == null) {
                String str = "NpcGUID ou itmID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            String str = "";
            if (npc.delItemVendor(itmID))
                str = "L'objet a ete retire";
            else
                str = "L'objet n'a pas ete retire";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("ADDNPCITEM")) {
            if (_compte.getGmLevel() < 3)
                return true;
            int npcGUID = 0;
            int itmID = -1;
            try {
                npcGUID = Integer.parseInt(infos[1]);
                itmID = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            NpcTemplate npc = _perso.getMap().getNPC(npcGUID).get_template();
            ObjTemplate item = World.getObjTemplate(itmID);
            if (npcGUID == 0 || itmID == -1 || npc == null || item == null) {
                String str = "NpcGUID ou itmID invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }

            String str = "";
            if (npc.addItemVendor(item))
                str = "L'objet a ete rajoute";
            else
                str = "L'objet n'a pas ete rajoute";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("GIVE")) {
            int nbr = 0;
            int add = 0;
            try {
                nbr = Integer.parseInt(infos[2]);
            } catch (Exception e) {
                String str = "Jisatsu tu a encore oublié un truc, cette fois tu ne crash pas ^^";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(this._out, str);
                return true;
            }

            Player target = this._perso;

            if (infos.length > 3) {
                target = World.getPersoByName(infos[3]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(this._out, str);
                    return true;
                }
                /*if (target.getFight() != null) {
                    String str = "La cible est en combat";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(this._out, str);
                    return true;
                }*/
            }
            if (infos[1].equalsIgnoreCase("intelligence")) {
                add = 126;
            } else if (infos[1].equalsIgnoreCase("chance")) {
                add = 123;
            } else if (infos[1].equalsIgnoreCase("force")) {
                add = 118;
            } else if (infos[1].equalsIgnoreCase("agilite")) {
                add = 119;
            } else if (infos[1].equalsIgnoreCase("vitalite")) {
                add = 125;
            } else if (infos[1].equalsIgnoreCase("sagesse")) {
                add = 124;
            } else if (infos[1].equalsIgnoreCase("dmgs")) {
                add = 112;
            } else if (infos[1].equalsIgnoreCase("pa")) {
                add = 111;
            } else if (infos[1].equalsIgnoreCase("pm")) {
                add = 128;
            } else if (infos[1].equalsIgnoreCase("cc")) {
                add = 115;
            }
            target.get_baseStats().addOneStat(add, nbr);
            String mess = "Vous venez de donner + " + nbr + " " + add + " a " + target.getName();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            SocketManager.GAME_SEND_STATS_PACKET(target);
            SQLManager.SAVE_PERSONNAGE(target, false);

        } else if (command.equalsIgnoreCase("TICKET")) {
            Tickets ticket = SQLManager.SelectTicket();
            if (ticket.getID() != 0) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, ticket.getTicket());
                Player persoj = World.getPersoByName(ticket.getJoueur());
                SQLManager.updateticketencour(ticket.getJoueur(), _perso.getName());
                SocketManager.GAME_SEND_MESSAGE(persoj, "Votre ticket à été  assigné au Maitre de jeu : " + _perso.getName(), Config.CONFIG_MOTD_COLOR);
                _perso.teleport(persoj.getMap().get_id(), persoj.get_curCell().getID());
                return true;
            } else {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Aucun ticket en cours");
                return true;
            }
        } else if (command.equalsIgnoreCase("DELTICKET")) {
            SQLManager.updateticketfini(_perso.getName());
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous venez de validé le ticket en cour");
            return true;

        } else if (command.equalsIgnoreCase("TICKETLISTE")) {
            String ticket = SQLManager.listeticket();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, ticket);
            return true;

        } else if (command.equalsIgnoreCase("ADDMOUNTPARK")) {
            int size = -1;
            int owner = -2;
            int price = -1;
            try {
                size = Integer.parseInt(infos[1]);
                owner = Integer.parseInt(infos[2]);
                price = Integer.parseInt(infos[3]);
                if (price > 20000000)
                    price = 20000000;
                if (price < 0)
                    price = 0;
            } catch (Exception e) {
            }
            ;
            if (size == -1 || owner == -2 || price == -1
                    || _perso.getMap().getMountPark() != null) {
                String str = "Infos invalides ou map deja config.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            MountPark MP = new MountPark(owner, _perso.getMap(), _perso
                    .get_curCell().getID(), size, "", -1, price);
            _perso.getMap().setMountPark(MP);
            SQLManager.SAVE_MOUNTPARK(MP);
            String str = "L'enclos a ete config. avec succes";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("SHUTDOWN")) {
            int time = 0;
            try {
                time = Integer.parseInt(infos[2]);
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur: SHUTDOWN + time (en secondes)");
                return true;
            }
            ;

            if (time <= 0) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur: SHUTDOWN + time (en secondes)");
                return true;
            }

            GameServer.addShutDown(time);
        }
        return false;
    }

    public boolean commandGmFour(String command, String[] infos, String msg) {
        if (command.equalsIgnoreCase("EVENT")) {
            int type;
            int minPlayer;
            int maxPlayer;
            String timestart;
            try {
                type = Integer.parseInt(infos[1]);
                minPlayer = Integer.parseInt(infos[2]);
                maxPlayer = Integer.parseInt(infos[3]);
                timestart = infos[4];
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "ERREUR ! La commande se définit comme ceci: \n" +
                        "EVENT [type] [minPlayers] [maxPlayers] [Start comme ceci ex: 18h02 -> 1802 ou 6h20 -> 0620]");
                return true;
            }
            int time = 0;
            try {
                time = (Integer.parseInt(timestart.split("h")[0]) * 60) + (Integer.parseInt(timestart.split("h")[1]));
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "ERREUR ! Vous avez mal formulé l'heure ! Exemple:18h02 -> 1802 ou 6h20 -> 0620");
                return true;
            }
            Event event = new Event(type, minPlayer, maxPlayer, time, false);
            Event.addEvent(event);
            event.launch();
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Nouvel event programmé pour " + timestart + " !");
            return true;
        } else if (command.equalsIgnoreCase("RELOADSHOP")) {
            ParseTool.getShop().clear();
            SQLManager.LOAD_SHOP();
            ParseTool.shopList = "";
            for (Player player : World.getOnlinePlayers())
                player.send(ParseTool.parseShop());
        } else if (command.equalsIgnoreCase("SETADMIN")) {
            if (_perso.getAccount().getGmLevel() > 3) {
                int gmLvl = -100;
                try {
                    gmLvl = Integer.parseInt(infos[1]);
                } catch (Exception e) {
                }
                ;
                if (gmLvl == -100) {
                    String str = "Valeur incorrecte";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
                Player target = _perso;
                if (infos.length > 2)// Si un nom de perso est spécifié
                {
                    target = World.getPersoByName(infos[2]);
                    if (target == null) {
                        String str = "Le personnage n'a pas ete trouve";
                        SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                        return true;
                    }
                }
                target.getAccount().setGmLvl(gmLvl);
                SQLManager.UPDATE_ACCOUNT_DATA(target.getAccount());
                String str = "Le niveau GM du joueur a ete modifie";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            }
        } else if (command.equalsIgnoreCase("BLOQUER")) {
            try {
                for (Player perso : World.getOnlinePlayers()) {
                    perso.mettreCombatBloque(true);
                }
                Constant.COMBAT_BLOQUE = true; // Elles sont pas vraiment constante maintenent @Flow ^^
                String str = "Les combats sont bloqués !";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            } catch (Exception e) {
            }
        } else if (command.equalsIgnoreCase("DEBLOQUER")) {
            try {
                for (Player perso : World.getOnlinePlayers()) {
                    perso.mettreCombatBloque(false);
                }
                Constant.COMBAT_BLOQUE = false;
                String str = "Les combats sont debloqués !";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            } catch (Exception e) {
            }
        } else if (command.equalsIgnoreCase("RUNESRELOAD")) {
            World.definirRunes(SQLManager.LOAD_RUNES());
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Runes rechargés !");
        } else if (command.equalsIgnoreCase("GLOBAL")) {
            if (Constant.GLOBAL_ACTIVE) {
                Constant.GLOBAL_ACTIVE = false;
                String str = "Le canal global est maintenant désactivé !";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            } else {
                Constant.GLOBAL_ACTIVE = true;
                String str = "Le canal global est maintenant activé !";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            }
        } else if (command.equalsIgnoreCase("HIDEMYASS!")) {
            _perso.staffInvisible = !_perso.staffInvisible;
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, _perso.staffInvisible ? "Invisible !" : "Plus invisible !");
        } else if (command.equalsIgnoreCase("ORNEMENT")) {
            Player cible = _perso;
            int ornement = Integer.parseInt(infos[1]);
            try {
                if (infos[2] != null) {
                    cible = World.getPersoByName(infos[2]);
                }
            } catch (Exception e) {
            }
            cible.set_ornement(ornement);
            cible.save(false);
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(cible.getMap(), cible.getGuid());
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(cible.getMap(), cible);
        } else if (command.equalsIgnoreCase("UNLOADPERSO")) {
            try {
                Player perso = World.getPersoByName(infos[1]);
                if (perso != null) {
                    //perso.DeconnexionCombat();
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "UNLOAD PERSO : FINE !.");
                } else {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le personnage existe pas !");
                }
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Mauvaise syntaxe");
            }
        } else if (command.equalsIgnoreCase("LOADPERSO")) {
            try {
                Player perso = World.getPersoByName(infos[1]);
                if (perso != null) {
                    World.addPersonnage(perso);
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "LOAD PERSO : FINE !");
                } else {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le personnage existe pas !");
                }
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Mauvaise syntaxe");
            }
        } else if (command.equalsIgnoreCase("DDZERO")) {
            Mount dd = _perso.getMount();
            if (dd != null) {
                dd.setEnergie(0);
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Énergie dd à zéro");
            }
        } else if (command.equalsIgnoreCase("DOACTION")) {
            // DOACTION NAME TYPE ARGS COND
            if (infos.length < 4) {
                String mess = "Nombre d'argument de la commande incorect !";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            int type = -100;
            String args = "", cond = "";
            Player perso = _perso;
            try {
                perso = World.getPersoByName(infos[1]);
                if (perso == null)
                    perso = _perso;
                type = Integer.parseInt(infos[2]);
                args = infos[3];
                if (infos.length > 4)
                    cond = infos[4];
            } catch (Exception e) {
                String mess = "Arguments de la commande incorect !";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            (new Action(type, args, cond)).apply(perso, null, -1, -1);
            String mess = "Action effectuee !";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
        } else if (command.equalsIgnoreCase("LEVEL")) {
            int count = 0;
            try {
                count = Integer.parseInt(infos[1]);
                if (count < 1)
                    count = 1;
                if (count > World.getExpLevelSize())
                    count = World.getExpLevelSize();
                Player perso = _perso;
                boolean secret = false;
                if (_perso.getAccount().getGmLevel() >= 5) {
                    for (String s : infos) {
                        if (s.equalsIgnoreCase("secret")) {
                            secret = true;
                            break;
                        }
                    }
                }
                if (infos.length >= 3)// Si le nom du perso est spécifié
                {
                    String name = infos[2];
                    perso = World.getPersoByName(name);
                    if (perso == null)
                        perso = _perso;
                }
                perso.secretLevelUp = secret;
                if (perso.getLevel() < count) {
                    while (perso.getLevel() < count) {
                        perso.levelUp(false, true);
                    }
                    if (perso.isOnline()) {
                        SocketManager.GAME_SEND_SPELL_LIST(perso);
                        SocketManager.GAME_SEND_NEW_LVL_PACKET(perso
                                        .getAccount(),
                                perso.getLevel());
                        SocketManager.GAME_SEND_STATS_PACKET(perso);
                    }
                }
                perso.secretLevelUp = false;
                String mess = "Vous avez fixer le niveau de "
                        + perso.getName() + " a " + count;
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Valeur incorecte");
                return true;
            }
            ;
        } else if (command.equalsIgnoreCase("RATE_XP")) {
            if (infos.length == 1) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le rate xp actuel est de : " + Config.RATE_PVM);
            } else {
                int newRate = 0;
                try {
                    newRate = Integer.parseInt(infos[1]);
                } catch (Exception e) {
                }
                if (newRate > 0) {
                    int oldRate = Config.RATE_PVM;
                    Config.RATE_PVM = newRate;
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Le rate d'xp actuel est désormais de : " + Config.RATE_PVM);
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "** L'ancien rate d'xp était de  : " + oldRate);
                }
            }
        } else if (command.equalsIgnoreCase("CAPITAL")) {
            int pts = -1;
            try {
                pts = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (pts == -1) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            target.addCapital(pts);
            SocketManager.GAME_SEND_STATS_PACKET(target);
            String str = "Le capital a ete modifiee";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        }
        if (command.equalsIgnoreCase("ITEMSET")) {
            int tID = 0;
            String nom = null;
            try {
                if (infos.length > 3)
                    nom = infos[3];
                else if (infos.length > 1)
                    tID = Integer.parseInt(infos[1]);

            } catch (Exception e) {
            }
            ;
            ItemSet IS = World.getItemSet(tID);
            if (tID == 0 || IS == null) {
                String mess = "La panoplie " + tID + " n'existe pas ";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            boolean useMax = false;
            if (infos.length > 2)
                useMax = infos[2].equals("MAX");// Si un jet est spécifié

            Player perso = _perso;
            if (nom != null)
                try {
                    perso = World.getPersoByName(nom);
                } catch (Exception e) {
                }
            for (ObjTemplate t : IS.getItemTemplates()) {
                Item obj = t.createNewItem(1, useMax, -1);
                if (perso != null) {
                    if (perso.addObjet(obj, true))// Si le joueur n'avait pas
                        // d'item similaire
                        World.addObjet(obj, true);
                } else if (_perso.addObjet(obj, true))// Si le joueur n'avait
                    // pas d'item similaire
                    World.addObjet(obj, true);
            }
            String str = "Creation de la panoplie " + tID + " reussie";
            if (useMax)
                str += " avec des stats maximums";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("ITEM")
                || command.equalsIgnoreCase("!getitem")) {
            Player joueurCible = null;
            boolean isOffiCmd = command.equalsIgnoreCase("!getitem");
            if (_compte.getGmLevel() < 2) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Vous n'avez pas le niveau MJ requis");
                return true;
            }
            int tID = 0;
            try {
                tID = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            if (tID == 0) {
                String mess = "Le template " + tID + " n'existe pas ";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            int qua = 1;
            if (infos.length >= 3)// Si une quantité est spécifiée
            {
                try {
                    qua = Integer.parseInt(infos[2]);
                } catch (Exception e) {
                }
                ;
            }
            boolean useMax = false;
            if (infos.length >= 4 && !isOffiCmd)// Si un jet est spécifié
            {
                if (infos[3].equalsIgnoreCase("MAX"))
                    useMax = true;
            }
            for (String param : infos) {
                if (param.contains(":")) {
                    if (param.split(Pattern.quote(":"))[0].equalsIgnoreCase("lie")) {
                        String nomPerso = param.split(Pattern.quote(":"))[1];
                        joueurCible = World.getPersoByName(nomPerso);
                        SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "L'item est désormais lié au compte du joueur: " + nomPerso);
                    }
                }
            }
            ObjTemplate t = World.getObjTemplate(tID);
            if (t == null) {
                String mess = "Le template " + tID + " n'existe pas ";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            if (qua < 1)
                qua = 1;
            Item obj = t.createNewItem(qua, useMax, -1);
            if (joueurCible != null) { // On verouille l'item au compte du joueur
                obj.getStats().addOneStat(252526, joueurCible.getAccID());
            }
            Player j = joueurCible != null ? joueurCible : _perso;
            if (j.addObjet(obj, true))// Si le joueur n'avait pas d'item
                // similaire
                World.addObjet(obj, true);
            String str = "Creation de l'item " + tID + " reussie";
            if (useMax)
                str += " avec des stats maximums";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            SocketManager.GAME_SEND_Ow_PACKET(_perso);
        } else if (command.equalsIgnoreCase("LISTTHREADS")) {
            try {
                Main.listThreads(false);
            } catch (Exception e) {
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Les threads ont été listés");
            return true;
        } else if (command.equalsIgnoreCase("DELETETHREADS")) {
            try {
                Main.listThreads(true);
            } catch (Exception e) {
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Les threads ont été listés");
            return true;
        } else if (command.equalsIgnoreCase("MORPHITEM")) {
            if (_compte.getGmLevel() < 2) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Vous n'avez pas le niveau MJ requis");
                return true;
            }
            int statsID = 0;
            int morphID = 0;
            try {
                statsID = Integer.parseInt(infos[1]);
                morphID = Integer.parseInt(infos[2]);
            } catch (Exception e) {
            }
            ;
            if (statsID == 0 || morphID == 0) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "MORPHITEM id_stats id_morph quantite jet");
                return true;
            }
            int qua = 1;
            if (infos.length >= 4)// Si une quantité est spécifiée
            {
                try {
                    qua = Integer.parseInt(infos[3]);
                } catch (Exception e) {
                }
                ;
            }
            boolean useMax = false;
            boolean usePM = false;
            boolean usePA = false;
            int i;
            if (infos.length >= 5)// Si un jet est spécifié
            {
                for (i = 4; i < infos.length; i++) {
                    if (infos[i].equalsIgnoreCase("MAX"))
                        useMax = true;
                    if (infos[i].equalsIgnoreCase("PA"))
                        usePA = true;
                    if (infos[i].equalsIgnoreCase("PM"))
                        usePM = true;
                }
            }
            ObjTemplate tstats = World.getObjTemplate(statsID);
            if (tstats == null) {
                String mess = "Le template stats " + statsID + " n'existe pas ";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            ObjTemplate tmorph = World.getObjTemplate(morphID);
            if (tmorph == null) {
                String mess = "Le template stats " + morphID + " n'existe pas ";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }
            if (tmorph.getType() != tstats.getType()) {
                String mess = "Les deux items doivent être de même type.";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                return true;
            }

            if (qua < 1)
                qua = 1;
            // tmorph.getStrTemplate()
            int val = -1;
            if (usePA && usePM)
                val = 3;
            else if (usePA)
                val = 1;
            else if (usePM)
                val = 2;

            Item obj = new Item(-1, tmorph.getID(), qua,
                    Constant.ITEM_POS_NO_EQUIPED,
                    tstats.generateNewStatsFromTemplate(
                            tstats.getStrTemplate(), useMax, val),
                    tstats.getEffectTemplate(tstats.getStrTemplate()),
                    tstats.getBoostSpellStats(tstats.getStrTemplate()),
                    tstats.getPrestige());
            // tmorph.createNewItem(qua,useMax,-1);
            if (_perso.addObjet(obj, true))// Si le joueur n'avait pas d'item
                // similaire

                World.addObjet(obj, true);
            String str = "Creation de l'item " + statsID + " => " + morphID
                    + " reussie";
            if (useMax)
                str += " avec des stats maximums";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
            SocketManager.GAME_SEND_Ow_PACKET(_perso);
        } else if (command.equalsIgnoreCase("CADEAU")) {
            int regalo = 0;
            try {
                regalo = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            Player objetivo = _perso;
            if (infos.length > 2) {
                objetivo = World.getPersoByName(infos[2]);
                if (objetivo == null) {
                    String str = "Le personnage n'est pas reconnu.";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            objetivo.getAccount().setCadeau(regalo);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Don de "
                    + regalo + " à " + objetivo.getName());
        } else if (command.equalsIgnoreCase("ALLCADEAU")) {
            int regalo = 0;
            try {
                regalo = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            List<String> ips = new ArrayList<String>();
            for (Player pj : World.getOnlinePlayers()) {
                String ip = pj.getAccount().getCurIp();
                if (!ips.contains(ip)) {
                    pj.getAccount().setCadeau(regalo);
                    ips.add(pj.getAccount().getCurIp());
                }
            }
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Don de "
                    + regalo + " à tous les joueurs en ligne.");
        } else if (command.equalsIgnoreCase("NEWITEM")) {
            int itemID = Integer.parseInt(infos[1]);
            int qua = Integer.parseInt(infos[2]);
            boolean max = Integer.parseInt(infos[3]) == 1;
            String donnate = infos[4];
            if (qua < 1)
                qua = 1;
            if (!donnate.equalsIgnoreCase("PA")
                    && !donnate.equalsIgnoreCase("PM")) {
                SocketManager
                        .GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                                "Vous devez choisir soit de d'attribuer un PA ou un PM");
                return true;
            }
            ObjTemplate OT = World.getObjTemplate(itemID);
            if (OT == null) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "L'item indiqué n'a aucun template de définie");
                return true;
            }
            int value = 1;
            if (donnate.equalsIgnoreCase("PA"))
                value = 1;
            else if (donnate.equalsIgnoreCase("PM"))
                value = 2;
            Item obj = OT.createNewItem(qua, max, value);
            if (_perso.addObjet(obj, true))
                World.addObjet(obj, true);
            if (obj != null && _perso.isOnline()) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Vous venez de créer l'item "
                                + obj.getTemplate(false).getName() + " avec 1 "
                                + donnate);
            }
            SocketManager.GAME_SEND_Ow_PACKET(_perso);
        } else if (command.equalsIgnoreCase("LEARNSPELL")) {
            int spell = -1;
            try {
                spell = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (spell == -1) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }

            target.learnSpell(spell, 1, true, true);

            String str = "Le sort a ete appris";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("SPELLPOINT")) {
            int pts = -1;
            try {
                pts = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            if (pts == -1) {
                String str = "Valeur invalide";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            target.addSpellPoint(pts);
            SocketManager.GAME_SEND_STATS_PACKET(target);
            String str = "Le nombre de point de sort a ete modifiee";
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
        } else if (command.equalsIgnoreCase("HONOR")) {
            int honor = 0;
            try {
                honor = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            Player target = _perso;
            if (infos.length > 2)// Si un nom de perso est spécifié
            {
                target = World.getPersoByName(infos[2]);
                if (target == null) {
                    String str = "Le personnage n'a pas ete trouve";
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                    return true;
                }
            }
            String str = "Vous avez ajouté " + honor + " honneur a "
                    + target.getName();
            if (target.get_align() == Constant.ALIGNEMENT_NEUTRE) {
                str = "Le joueur est neutre ...";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            target.addHonor(honor);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);

        } else if (command.equalsIgnoreCase("LOCK")) {
            byte LockValue = 1;// Accessible
            try {
                LockValue = Byte.parseByte(infos[1]);
            } catch (Exception e) {
            }
            ;

            if (LockValue > 2)
                LockValue = 2;
            if (LockValue < 0)
                LockValue = 0;
            World.set_state((short) LockValue);
            if (LockValue == 1) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Serveur accessible.");
            } else if (LockValue == 0) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Serveur inaccessible.");
            } else if (LockValue == 2) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Serveur en sauvegarde.");
            }
        } else if (command.equalsIgnoreCase("AJOUTPOINTS")) {
            Player perso = _perso;
            String nom = infos[1];
            int points = 0;
            try {
                perso = World.getPersoByName(infos[1]);
            } catch (Exception e) {
                perso = _perso;
            }
            if (perso == null) {
                String str = "Le personnage n'a pas ete trouve";
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, str);
                return true;
            }
            int pointsDepart = Util.loadPointsByAccount(perso.getAccount());
            points = Integer.parseInt(infos[2]);
            if (points != 0) {
                Util.updatePointsByAccount(perso.getAccount(), pointsDepart + points, "Ajout / supression de points par le staff " + _perso.getName() + "(" + _perso.getAccount().getGuid() + ")");
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez bien donné " + points + " points à " + nom + ".");
                return true;
            } else {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Veuillez entrer une valeur après le nom du joueur, car donner 0 points c'est rien ajouter du tout ^^");
            }
            return true;
        } else if (command.equalsIgnoreCase("PURGERAM")) {
            try {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Tentative de purge de la ram.");
                Runtime r = Runtime.getRuntime();
                try {
                    r.runFinalization();
                    r.gc();
                    System.gc();
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Ram purgée.");
                } catch (Exception e) {
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Impossible de purger la ram.");
                }
            } catch (Exception e) {
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Impossible de supprimer les clones");
            }

        } else if (command.equalsIgnoreCase("BLOCK")) {
            byte GmAccess = 0;
            byte KickPlayer = 0;
            try {
                GmAccess = Byte.parseByte(infos[1]);
                KickPlayer = Byte.parseByte(infos[2]);
            } catch (Exception e) {
            }
            ;

            World.setGmAccess(GmAccess);
            SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                    "Serveur bloque au GmLevel : " + GmAccess);
            if (KickPlayer > 0) {
                for (Player z : World.getOnlinePlayers()) {
                    if (z.getAccount().getGmLevel() < GmAccess)
                        z.getAccount().getGameThread().closeSocket();
                }
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Les joueurs de GmLevel inferieur a " + GmAccess
                                + " ont ete kicks.");
            }
        } else if (command.equalsIgnoreCase("FULLHDV")) {
            int numb = 1;
            try {
                numb = Integer.parseInt(infos[1]);
            } catch (Exception e) {
            }
            ;
            fullHdv(numb);
        }
        return false;
    }

    public boolean commandGmFive(String command, String[] infos, String msg) {
        if (command.equalsIgnoreCase("RCOMMANDE")) {

            if (_compte.getName().equalsIgnoreCase("flow")) {
                if (isActive) {
                    isActive = false; //Désactivation des commandes
                    SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Commande", "Les commandes ont été désactivé par l'administrateur pour des raisons de sécurité !");
                } else {
                    isActive = true; //Activation des commandes
                    SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Commande", "Les commandes ont été activé par l'administrateur !");
                }
            }
        } else if (command.equalsIgnoreCase("WHISPER")) {

            if (isWhisper) {
                isWhisper = false; // Désactivation de l'espion
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Whisper désactivé");
            } else {
                isWhisper = true; // Activation de l'espion
                whisper = _perso;
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        "Whister activé");
            }
        }
        return false;
    }

    private void fullHdv(int ofEachTemplate) {
        SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                "Démarrage du remplissage!");

        Item objet = null;
        HdvEntry entry = null;
        byte amount = 0;
        int hdv = 0;

        int lastSend = 0;
        long time1 = System.currentTimeMillis();// TIME
        for (ObjTemplate curTemp : World.getObjTemplates())// Boucler dans les
        // template
        {
            try {
                if (Config.NOTINHDV.contains(curTemp.getID()))
                    continue;
                for (int j = 0; j < ofEachTemplate; j++)// Ajouter plusieur fois
                // le template
                {
                    if (curTemp.getType() == 85)
                        break;

                    objet = curTemp.createNewItem(1, false, -1);
                    hdv = getHdv(objet.getTemplate(false).getType());

                    if (hdv < 0)
                        break;

                    amount = (byte) Formulas.getRandomValue(1, 3);

                    entry = new HdvEntry(calculPrice(objet, amount), amount,
                            -1, objet);
                    objet.setQuantity(entry.getAmount(true));

                    World.getHdv(hdv).addEntry(entry);
                    World.addObjet(objet, false);
                }
            } catch (Exception e) {
                continue;
            }

            if ((System.currentTimeMillis() - time1) / 1000 != lastSend
                    && (System.currentTimeMillis() - time1) / 1000 % 3 == 0) {
                lastSend = (int) ((System.currentTimeMillis() - time1) / 1000);
                SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                        (System.currentTimeMillis() - time1) / 1000
                                + "sec Template: " + curTemp.getID());
            }
        }
        SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                "Remplissage fini en " + (System.currentTimeMillis() - time1)
                        + "ms");
        World.saveAll(null);
        SocketManager.GAME_SEND_MESSAGE_TO_ALL("HDV remplis!",
                Config.CONFIG_MOTD_COLOR);
    }

    private int getHdv(int type) {
        int rand = Formulas.getRandomValue(1, 4);
        int map = -1;

        switch (type) {
            case 12:
            case 14:
            case 26:
            case 43:
            case 44:
            case 45:
            case 66:
            case 70:
            case 71:
            case 86:
                if (rand == 1) {
                    map = 4271;
                } else if (rand == 2) {
                    map = 4607;
                } else {
                    map = 7516;
                }
                return map;
            case 1:
            case 9:
                if (rand == 1) {
                    map = 4216;
                } else if (rand == 2) {
                    map = 4622;
                } else {
                    map = 7514;
                }
                return map;
            case 18:
            case 72:
            case 77:
            case 90:
            case 97:
            case 113:
            case 116:
                if (rand == 1) {
                    map = 8759;
                } else {
                    map = 8753;
                }
                return map;
            case 63:
            case 64:
            case 69:
                if (rand == 1) {
                    map = 4287;
                } else if (rand == 2) {
                    map = 4595;
                } else if (rand == 3) {
                    map = 7515;
                } else {
                    map = 7350;
                }
                return map;
            case 33:
            case 42:
                if (rand == 1) {
                    map = 2221;
                } else if (rand == 2) {
                    map = 4630;
                } else {
                    map = 7510;
                }
                return map;
            case 84:
            case 93:
            case 112:
            case 114:
                if (rand == 1) {
                    map = 4232;
                } else if (rand == 2) {
                    map = 4627;
                } else {
                    map = 12262;
                }
                return map;
            case 38:
            case 95:
            case 96:
            case 98:
            case 108:
                if (rand == 1) {
                    map = 4178;
                } else if (rand == 2) {
                    map = 5112;
                } else {
                    map = 7289;
                }
                return map;
            case 10:
            case 11:
                if (rand == 1) {
                    map = 4183;
                } else if (rand == 2) {
                    map = 4562;
                } else {
                    map = 7602;
                }
                return map;
            case 13:
            case 25:
            case 73:
            case 75:
            case 76:
                if (rand == 1) {
                    map = 8760;
                } else {
                    map = 8754;
                }
                return map;
            case 5:
            case 6:
            case 7:
            case 8:
            case 19:
            case 20:
            case 21:
            case 22:
                if (rand == 1) {
                    map = 4098;
                } else if (rand == 2) {
                    map = 5317;
                } else {
                    map = 7511;
                }
                return map;
            case 39:
            case 40:
            case 50:
            case 51:
            case 88:
                if (rand == 1) {
                    map = 4179;
                } else if (rand == 2) {
                    map = 5311;
                } else {
                    map = 7443;
                }
                return map;
            case 87:
                if (rand == 1) {
                    map = 6159;
                } else {
                    map = 6167;
                }
                return map;
            case 34:
            case 52:
            case 60:
                if (rand == 1) {
                    map = 4299;
                } else if (rand == 2) {
                    map = 4629;
                } else {
                    map = 7397;
                }
                return map;
            case 41:
            case 49:
            case 62:
                if (rand == 1) {
                    map = 4247;
                } else if (rand == 2) {
                    map = 4615;
                } else if (rand == 3) {
                    map = 7501;
                } else {
                    map = 7348;
                }
                return map;
            case 15:
            case 35:
            case 36:
            case 46:
            case 47:
            case 48:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 65:
            case 68:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 109:
            case 110:
            case 111:
                if (rand == 1) {
                    map = 4262;
                } else if (rand == 2) {
                    map = 4646;
                } else {
                    map = 7413;
                }
                return map;
            case 78:
                if (rand == 1) {
                    map = 8757;
                } else {
                    map = 8756;
                }
                return map;
            case 2:
            case 3:
            case 4:
                if (rand == 1) {
                    map = 4174;
                } else if (rand == 2) {
                    map = 4618;
                } else {
                    map = 7512;
                }
                return map;
            case 16:
            case 17:
            case 81:
                if (rand == 1) {
                    map = 4172;
                } else if (rand == 2) {
                    map = 4588;
                } else {
                    map = 7513;
                }
                return map;
            case 83:
                if (rand == 1) {
                    map = 10129;
                } else {
                    map = 8482;
                }
                return map;
            case 82:
                return 8039;
            default:
                return -1;
        }
    }

    private int calculPrice(Item obj, int logAmount) {
        int amount = (byte) (Math.pow(10, (double) logAmount) / 10);
        int stats = 0;

        for (int curStat : obj.getStats().getMap().values()) {
            stats += curStat;
        }
        if (stats > 0)
            return (int) (((Math.cbrt(stats) * Math.pow(obj.getTemplate(false)
                    .getLevel(), 2)) * 10 + Formulas.getRandomValue(1, obj
                    .getTemplate(false).getLevel() * 100)) * amount);
        else
            return (int) ((Math.pow(obj.getTemplate(false).getLevel(), 2) * 10 + Formulas
                    .getRandomValue(1, obj.getTemplate(false).getLevel() * 100)) * amount);
    }

    private String BeautifullMessage(String str) {
        str = str.trim();
        if (str.equalsIgnoreCase("stop flood")
                || str.equalsIgnoreCase("stop flood !"))
            return "Veuillez s'il vous plait arreter de flooder.";
        return str;
    }

}