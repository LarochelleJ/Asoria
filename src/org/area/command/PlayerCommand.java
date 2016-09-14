package org.area.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.area.kolizeum.Kolizeum;
import org.area.object.Maps.MountPark;
import org.area.arena.Arena;
import org.area.arena.GdG;
import org.area.arena.Team;
import org.area.arena.dm;
import org.area.check.Security;
import org.area.client.Account;
import org.area.client.Player;
import org.area.client.Player.Stats;
import org.area.client.tools.RapidStuff;
import org.area.common.ConditionParser;
import org.area.common.Constant;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.fight.object.Stalk;
import org.area.game.GameSendThread;
import org.area.game.GameServer;
import org.area.game.GameThread;
import org.area.game.tools.Util;
import org.area.kernel.Config;
import org.area.kernel.Main;
import org.area.object.Item;
import org.area.object.Item.ObjTemplate;
import org.area.object.Maps;
import org.area.object.job.Job.StatsMetier;
import org.area.spell.Spell.SortStats;
import org.area.spell.SpellEffect;
import org.area.game.CharacterName;


public class PlayerCommand {
    public static boolean tryNewCommand(String msg, Player _perso, GameSendThread _out) {

        if (msg.charAt(0) == '.') {
            CommandManager command = CommandManager.getCommandByName(msg.substring(1, msg.length() - 1));

            if (command != null) {
                if (command.getCond() != null && !ConditionParser.validConditions(_perso, command.getCond())) {

                    //PR=6
                    String condition = command.getCond().substring(0, 2);

                    if (condition.equalsIgnoreCase("PR")) {
                        _perso.sendText("Vous ne disposez pas du prestige nécessaire pour utiliser cette commande");
                    } else {
                        _perso.sendText("Vous ne remplissez pas les conditions nécessaires pour éxécuter cette commande !");
                    }

                    return true;
                }

                int points = Util.loadPointsByAccount(_perso.getAccount());
                int price = command.getPrice();
                int diff = 0;

                if (command.getPrice() > 0) {

                    if (points < price) {
                        _perso.sendText("Vous n'avez pas assez de points, il vous manque " + (price - points) + " points !");
                        return true;
                    }
                }

                for (String f : command.getFunction().split(";")) {
                    int type;
                    String args = null;

                    if (f.contains("-")) {
                        args = f.split("-")[1];
                        type = Integer.parseInt(f.split("-")[0]);
                    } else {
                        type = Integer.parseInt(f);
                    }

                    label:
                    switch (type) {
                        case 0: //Liste des commandes
                            String listeCommande0 = "   "
                                    + "\n   ";
                            String listeCommande = "\n   "


                                    + "\n <b> Les Commandes De Téléportation :</b>"
                                    + "\n   "
                                    + "\n .start- Vous téléporte à  la map start"
                                    + "\n .shop - Vous téléporte à  la map shop"
                                    + "\n .pvp - Vous téléporte à  la map  PVP"
                                    + "\n .pvm - Vous téléporte à  la map  PvM "
                                    + "\n .enclos - Vous ouvre l'interface enclos"
                                    + "\n .detente - Vous téléporte à  la maps detente"
                                    + "\n .event - Vous téléporte à  l'event en cours"
                                    + "\n .arene - Vous téléporte à  l'arêne de Bonta"
                                    + "\n .poutch - Vous téléporte à  la map PountchIngall"
                                    + "\n .atelier - Vous téléporte à  la map atelier"
                                    + "\n   ";
                            String List3 = "\n <b> Les Commandes Alignements :</b> "
                                    + "\n   "
                                    + "\n .ange - Vous devenez ange."
                                    + "\n .demon - Vous devenez demon."
                                    + "\n .neutre - Vous permet de redevenir neutre"
                                    + "\n .traque - Obtenir une traque"
                                    + "\n .traquerecompense - Obtenir la récompense de la traque"
                                    + "\n .traqueposition - Obtenir la position de sa traque"
                                    + "\n   ";
                            String List5 = "\n <b> Les Diverses Commandes :</b>"
                                    + "\n   "
                                    + "\n .safe - EmpÃªcher les crashs de fin de combat"
                                    + "\n .save - Sauvegarde votre personnage"
                                    + "\n .savetitre - Sauvegarder son titre actuel [5 pts]"
                                    + "\n .mestitres - Voir ses titres sauvegardés"
                                    + "\n .creationtitre - Créer un titre personnalisé [60 pts]"
                                    + "\n .changercouleurtitre - [15 pts]"
                                    + "\n .vie - Récupérer sa vie au complet"
                                    + "\n .ticket + [Message] - Contacter l'équipe"
                                    + "\n .points - Permet de consulter son nombre de Points"
                                    + "\n .m - Ceci est le chat sans limitation"
                                    + "\n .banque - Ouvre votre compte bancaire"
                                    + "\n .staff - Membre de l'équipe en ligne"
                                    + "\n .infos - Informations sur le serveur"
                                    + "\n .changenom + [NAME] - Changer de nom [50 pts]"
                                    + "\n .guilde - Vous permet de créer une guilde"
                                    + "\n .rapidstuff - Commande de stuff rapide."
                                    + "\n   ";
                            String List6 = "\n <b> Les Commandes FM [100 pts] :</b> "
                                    + "\n   "
                                    + "\n .fmpm coiffe - FM votre coiffe + 1PM"
                                    + "\n .fmpa coiffe - FM votre coiffe + 1PA"
                                    + "\n .fmpa cape - FM votre cape + 1PA"
                                    + "\n .fmpm cape - FM votre cape + 1PM"
                                    + "\n .fmpa ceinture - FM votre ceinture + 1PA"
                                    + "\n .fmpm ceinture - FM votre ceinture + 1PM"
                                    + "\n .fmpm bottes - FM vos bottes + 1PM"
                                    + "\n .fmpa bottes - FM vos bottes + 1PA"
                                    + "\n .fmpm anneaugauche - FM votre anneau + 1PM"
                                    + "\n .fmpa anneaugauche - FM votre anneau + 1PA"
                                    + "\n .fmpa anneaudroit - FM votre anneau + 1PA"
                                    + "\n .fmpm anneaudroit - FM votre anneau + 1PM"
                                    + "\n .fmpm amulette - FM votre amu + 1PM"
                                    + "\n .fmpa amulette - FM votre amu + 1PA"
                                    + "\n   ";
                          /*String List7 = "\n <b> Les Titres :</b> " // TODO Load title from table
                          + "\n   "
					      + "\n .titre1 - Titre = Dieu parmis les hommes [100 pts]"
					      + "\n .titre2 - Titre = Massacreur de Noobs [100 pts]"
					      + "\n .titre3 - Titre = The BOSS [100 pts]"
					      + "\n .titre4 - Titre = Héros Légendaire [80 pts]"
					      + "\n .titre5 - Titre = Pro Skillz [50 pts]"
					      + "\n .titre6 - Titre = The King [70 pts]"
					      + "\n .titre7 - Titre = Aerien [10 pts]"
					      + "\n .titre8 - Titre = Terreur de la presqu'île [90 pts]"
					      + "\n .titre9 - Titre = Le Boss D'Area [120 pts]"
					      + "\n   ";*/
                            String List7 = "\n <b> Les Morphs :</b> "
                                    + "\n   "
                                    + "\n .morph1 - Fantome Faenor [30PB]"
                                    + "\n .morph2 - Fantome Croum  [30PB]"
                                    + "\n .morph3 - Robot Vert [50PB]"
                                    + "\n .morph4 - Garde Bontarien [60PB] (Condition : Être bontarien)"
                                    + "\n .morph5 - Garde Brakmarien [60PB] (Condition : Être brakmarien)"
                                    + "\n .morph6 - Mercenaire Exilé [60PB] (Condition : Être mercenaire)"
                                    + "\n .morph7 - Chevalier Noir [150PB]"
                                    + "\n .morph8 - Zobal (seulement la morph) [200PB]"
                                    + "\n .demorph - Vous revenez en apparence 1.29 [0PB]"
                                    + "\n   ";
                            String List9 = "\n <b> Les équipements Rapide :</b> "
                                    + "\n   "
                                    + "\n .rapidstuff create [NAME] - Créé un stuff rapide"
                                    + "\n .rapidstuff remove [NAME] - Supprimer un stuff"
                                    + "\n .rapidstuff vieuw [NAME] - Voir ces stuff rapide"
                                    + "\n .rapidstuff equip [NAME] - Equiper un stuff créé"
                                    + "\n   ";
                            String List10 = "\n <b> Les commandes du DeathMatch :</b> "
                                    + "\n "
                                    + "\n .deathmatch on - Vous inscrit au DeathMatch"
                                    + "\n .deathmatch off - Vous désinscrit du DeathMatch"
                                    + "\n   ";
					      /*String List11 = "\n <b> La commande de guerre de guilde :</b> "
					      + "\n "
					      + "\n .gvg + nom de la guilde à  attaquer - Attaquer une guilde";*/

                            _perso.sendText(listeCommande0);
                            _perso.sendText(listeCommande);
                            _perso.sendText(List3);
                            _perso.sendText(List5);
                            _perso.sendText(List6);
                            _perso.sendText(List7);
                            //_perso.sendText(List9);
                            //_perso.sendText(List10);
                            // _perso.sendText(List11);
                            break;
                        case 1: //Liste des commandes VIP
                            _perso.sendText(CommandManager.getCommandList(true));
                            break;
                        case 2: //Envoyer ne message
                            String mess = args.split(",")[0];
                            _perso.sendText(mess);
                            break;
                        case 3: //Envoyer un popup
                            SocketManager.GAME_SEND_POPUP(_perso, args);
                            break;
                        case 4: //Informations serveur
                            String text = "\n<img src='UI_FightOptionBlockJoinerExceptPartyMemberUp'/><br/><b>Area V.2.6</b>\n\n"
                                    + "\n   "
                                    + GameServer.uptime()
                                    + "Joueurs en ligne : <b>" + Main.gameServer.getPlayerNumber() + "</b>\n"
                                    + "Record de connexion : <b>" + Main.gameServer.getMaxPlayer() + "</b>";
                            SocketManager.GAME_SEND_POPUP(_perso, text);
                            return true;
                        case 5: //Staff en ligne
                            String staff = "Membre de l'équipe connectés :\n";
                            boolean allOffline = true;

                            for (int i = 0; i < World.getOnlinePlayers().size(); i++) {
                                if (World.getOnlinePlayers().get(i).getAccount().getGmLevel() > 0) {
                                    staff += "- <b><a href='asfunction:onHref,ShowPlayerPopupMenu," + World.getOnlinePlayers().get(i).getName() + "'>" + World.getOnlinePlayers().get(i).getName() + "</a></b> (";
                                    if (World.getOnlinePlayers().get(i).getAccID() == 6569) {
                                        staff += "Game designer)";
                                    } else if (World.getOnlinePlayers().get(i).getAccID() == 6465) {
                                        staff += "Administrateur / Développeur)";
                                    } else if (World.getOnlinePlayers().get(i).getAccount().getGmLevel() == 1)
                                        staff += "Animateur)";
                                    else if (World.getOnlinePlayers().get(i).getAccount().getGmLevel() == 2)
                                        staff += "Modérateur)";
                                    else if (World.getOnlinePlayers().get(i).getAccount().getGmLevel() == 3)
                                        staff += "Community manager)";
                                    else if (World.getOnlinePlayers().get(i).getAccount().getGmLevel() == 4)
                                        staff += "Administrateur)";
                                    else if (World.getOnlinePlayers().get(i).getAccount().getGmLevel() == 5)
                                        staff += "Fondateur)";
                                    else
                                        staff += "Unknown";
                                    staff += "\n";
                                    allOffline = false;
                                }
                            }
                            if (!staff.isEmpty() && !allOffline) {
                                SocketManager.GAME_SEND_POPUP(_perso, staff);
                            } else if (allOffline) {
                                SocketManager.GAME_SEND_POPUP(_perso, "Aucun membre de l'équipe est présent !");
                            }
                            break;
                        case 6: //Sauvegarde du personnage
                            if (_perso.getFight() != null)
                                break;
                            SQLManager.SAVE_PERSONNAGE(_perso, true);
                            _perso.sendText("Votre personnage <b>" + _perso.getName() + "</b> est sauvegardé.");
                            break;
                        case 7: //Rafraichissement de Map
                            Maps map = _perso.getMap();
                            map.refreshSpawns();
                            break;
                        case 8: //Vitalité Max
                            int count = 100;
                            int newPDV = (_perso.get_PDVMAX() * count) / 100;

                            _perso.set_PDV(newPDV);
                            if (_perso.isOnline())
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            _perso.sendText("Votre vie est désormais au maximum !");
                            break;
                        case 9: //Création de guilde
                            if (_perso.get_guild() != null || _perso.getGuildMember() != null || !_perso.isOnline() || _perso == null) {
                                break;
                            } else {
                                SocketManager.GAME_SEND_gn_PACKET(_perso);
                            }
                            break;
                        case 10: //Parchotage
                            if (_perso.getFight() != null)
                                break;
                            int nombrePoints = Util.loadPointsByAccount(_perso.getAccount());
                            if (nombrePoints < 15) {
                                _perso.sendText("Il vous manque " + (15 - nombrePoints) + " points !");
                                return true;
                            }
                            int nbreElement = 0;

                            if (_perso.get_baseStats().getEffect(125) < 101) {
                                _perso.get_baseStats().addOneStat(125, 101 - _perso.get_baseStats().getEffect(125));
                                nbreElement++;
                            }

                            if (_perso.get_baseStats().getEffect(124) < 101) {
                                _perso.get_baseStats().addOneStat(124, 101 - _perso.get_baseStats().getEffect(124));
                                nbreElement++;
                            }

                            if (_perso.get_baseStats().getEffect(118) < 101) {
                                _perso.get_baseStats().addOneStat(118, 101 - _perso.get_baseStats().getEffect(118));
                                if (nbreElement == 0)
                                    nbreElement++;
                            }

                            if (_perso.get_baseStats().getEffect(126) < 101) {
                                _perso.get_baseStats().addOneStat(126, 101 - _perso.get_baseStats().getEffect(126));
                                if (nbreElement == 0)
                                    nbreElement++;
                            }

                            if (_perso.get_baseStats().getEffect(119) < 101) {
                                _perso.get_baseStats().addOneStat(119, 101 - _perso.get_baseStats().getEffect(119));
                                if (nbreElement == 0)
                                    nbreElement++;
                            }

                            if (_perso.get_baseStats().getEffect(123) < 101) {
                                _perso.get_baseStats().addOneStat(123, 101 - _perso.get_baseStats().getEffect(123));
                                if (nbreElement == 0)
                                    nbreElement++;
                            }

                            if (nbreElement == 0) {
                                SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<i>Serveur: </i>Vous êtes déjà  parchotté dans tous les éléments !");
                            } else {
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                                Util.updatePointsByAccount(_perso.getAccount(), nombrePoints - 15);
                                _perso.sendText("Le parchotage vous a coûté 15 points !");
                                _perso.send("000C" + Util.loadPointsByAccount(_perso.getAccount()));
                            }
                            break;
                        case 11: //Apprendre un sort
                            int spellID = Integer.parseInt(args.split(",")[0]);
                            int level = Integer.parseInt(args.split(",")[1]);
                            _perso.learnSpell(spellID, level, true, true);
                            break;
                        case 12: //Alignement
                            byte align = (byte) Integer.parseInt(args);
                            if (_perso.getFight() != null) {
                                _perso.sendText("Vous ne pouvez pas changer d'alignement lorsque vous êtes en combat.");
                            } else {
                                _perso.modifAlignement(align);
                            }
                            if (_perso.isOnline())
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            break;
                        case 13: //Ajouter Kamas
                            int kamas = Integer.parseInt(args);
                            _perso.addKamas(kamas);
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            break;
                        case 14: //Ajouter Item
                            int tID = Integer.parseInt(args.split(",")[0]);
                            int nbr = Integer.parseInt(args.split(",")[1]);
                            boolean isMax = Boolean.parseBoolean(args.split(",")[2]);

                            ObjTemplate t = World.getObjTemplate(tID);
                            Item obj = t.createNewItem(nbr, isMax, -1);
                            if (_perso.addObjet(obj, true))
                                World.addObjet(obj, true);
                            break;
                        case 15: //Devenir VIP
                            Account account = _perso.getAccount();
                            if (account == null) return true;
                            if (account.getVip() != 0)
                                break;
                            if (account.getVip() == 0) {
                                account.setVip(1);
                                SQLManager.UPDATE_ACCOUNT_VIP(account);
                            }
                            break;
                        case 16: //Ajouter un titre
                            _perso.set_title((byte) Integer.parseInt(args));
                            if (_perso.getFight() == null)
                                SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                            break;
                        case 17: //Reset des caractéristiques
                            _perso.get_baseStats().addOneStat(125, -_perso.get_baseStats().getEffect(125));
                            _perso.get_baseStats().addOneStat(124, -_perso.get_baseStats().getEffect(124));
                            _perso.get_baseStats().addOneStat(118, -_perso.get_baseStats().getEffect(118));
                            _perso.get_baseStats().addOneStat(123, -_perso.get_baseStats().getEffect(123));
                            _perso.get_baseStats().addOneStat(119, -_perso.get_baseStats().getEffect(119));
                            _perso.get_baseStats().addOneStat(126, -_perso.get_baseStats().getEffect(126));
                            _perso.addCapital((_perso.getLevel() - 1) * 5 - _perso.get_capital());
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            break;
                        case 18: //Lancer une traque
                            Stalk.newTraque(_perso);
                            break;
                        case 19: //Récompense de traque
                            Stalk.getRecompense(_perso);
                            break;
                        case 20: //Géoposition de la cible
                            Stalk.getTraquePosition(_perso);
                            break;
                        case 21: //Désaprendre un sort
                            _perso.setisForgetingSpell(true);
                            SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', _perso);
                            break;
                        case 22: //Demorph
                            int morphID = _perso.get_classe() * 10 + _perso.get_sexe();
                            _perso.set_gfxID(morphID);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(), _perso.getGuid());
                            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(), _perso);
                            break;
                        case 23: //Donner une statistique
                            int statID = Integer.parseInt(args.split(",")[0]);
                            int value = Integer.parseInt(args.split(",")[1]);
                            _perso.get_baseStats().addOneStat(statID, value);
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            break;
                        case 24: //Ouvrir la banque
                            //Sauvagarde du perso et des item avant.
                            if (_perso.getFight() != null)
                                break;
                            SQLManager.SAVE_PERSONNAGE(_perso, true);
                            if (_perso.getDeshonor() >= 1) {
                                SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
                                break;
                            }
                            int cost = _perso.getBankCost();
                            if (cost > 0) {
                                long nKamas = _perso.get_kamas() - cost;
                                if (nKamas < 0)//Si le joueur n'a pas assez de kamas pour ouvrir la banque
                                {
                                    SocketManager.GAME_SEND_Im_PACKET(_perso, "1128;" + cost);
                                    break;
                                }
                                _perso.set_kamas(nKamas);
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                                SocketManager.GAME_SEND_Im_PACKET(_perso, "020;" + cost);
                            }
                            SocketManager.GAME_SEND_ECK_PACKET(_perso.getAccount().getGameThread().getOut(), 5, "");
                            SocketManager.GAME_SEND_EL_BANK_PACKET(_perso);
                            _perso.set_away(true);
                            _perso.setInBank(true);

                            //_perso.sendText("La banque s'efface d'elle même après un certain temps, nous travaillons à résoudre ce bug.");
                            break;
                        case 25: //Ajouter des points de sort
                            int spellpoints = Integer.parseInt(args);
                            _perso.addSpellPoint(spellpoints);
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            break;
                        case 26: //Ajouter des points de capital
                            int capital = Integer.parseInt(args);
                            _perso.addCapital(capital);
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            break;
                        case 28: //Canal sans limite
						/*String RangP = "";
						if (_perso.getAccount().getGmLevel() == 0 && _perso.getAccount().getVip() == 0)
							RangP = "Joueur";
						if (_perso.getAccount().getGmLevel() >= 1)
							RangP = "Staff";
						if (_perso.getAccount().getVip() >= 1 && _perso.getAccount().getGmLevel() == 0)
							RangP = "VIP";*/
                            String prefix = _perso.getName();
                            String sp;
                            try {
                                sp = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                return true;
                            }
                            if (_perso.timeMuted > 0) {
                                if (_perso.timeMuted < System.currentTimeMillis()) {
                                    _perso.isMuteFromGlobal = false;
                                }
                            }
                            if (!Constant.GLOBAL_ACTIVE && _perso.getAccount().getGmLevel() == 0) {
                                _perso.sendText("Le canal global est temporairement désactivé par l'équipe d'administration.");
                            } else {
                                if (!_perso.isMuteFromGlobal) {
                                    long tempEcoule = (System.currentTimeMillis() - _perso.tempAncienMessage) / 1000;
                                    if (tempEcoule < 7 && _perso.getAccount().getGmLevel() == 0) {
                                        _perso.sendText("Ce canal est restreint pour améliorer sa lisibilité. Vous pourrez envoyer un nouveau message dans " + Math.abs(7 - tempEcoule) + " secondes.");
                                    } else {
                                        _perso.tempAncienMessage = System.currentTimeMillis();
                                        String clicker_name = "<a href='asfunction:onHref,ShowPlayerPopupMenu," + _perso.getName() + "'>" + prefix + "</a>";
                                        SocketManager.GAME_SEND_MESSAGE_TO_ALL2((new StringBuilder("<b> ")).append(clicker_name).append("</b> : ").append(sp).toString(), "0BCFF9");
                                    }
                                } else {
                                    if (_perso.timeMuted < 0) {
                                        _perso.sendText("Vous ne pouvez plus parler en canal global pour une durée indéterminée.");
                                    } else {
                                        int timeEnSeconds = Math.abs(Math.round((_perso.timeMuted - System.currentTimeMillis()) / 1000));
                                        String tempRestant = "";
                                        if (timeEnSeconds >= 3600) { // heure
                                            int nbHeure = timeEnSeconds / 3600;
                                            if (nbHeure > 0) {
                                                tempRestant += "" + nbHeure + " heures ";
                                            } else {
                                                tempRestant += "" + nbHeure + " heure ";
                                            }
                                            timeEnSeconds -= nbHeure * 3600;
                                        }
                                        if (timeEnSeconds >= 60) {
                                            int nbMinute = timeEnSeconds / 60;
                                            if (nbMinute > 0) {
                                                tempRestant += "" + nbMinute + " minutes ";
                                            } else {
                                                tempRestant += "" + nbMinute + " minute ";
                                            }
                                            timeEnSeconds -= nbMinute * 60;
                                        }
                                        if (timeEnSeconds > 0) {
                                            tempRestant += "" + timeEnSeconds + " secondes";
                                        }
                                        _perso.sendText("Vous n'êtes plus autorisé à parler dans le canal global. La parole vous sera rendue dans " + tempRestant + " !");
                                    }
                                }
                            }
                            break;
                        case 29: //Création de Team Arena
                            String s;
                            try {
                                s = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("Pour créer une équipe, invitez votre partenaire dans votre groupe, puis éxécutez .createteam + Nom de votre Team");
                                break;
                            }
                            if (s.length() > 20 || s.contains("#") || s.contains(",") || s.contains(";") || s.contains("/") || s.contains("!") || s.contains(".")
                                    || s.contains("'") || s.contains("*") || s.contains("$") || s.contains("+") || s.contains("-") || s.contains("|")
                                    || s.contains("~") || s.contains("(") || s.contains(")") || s.contains("[") || s.contains("]") || s.contains("%"))

                            {
                                _perso.sendText("Le nom de votre équipe ne doit pas contenir de caractères spéciaux !");
                                break;
                            }
                            if (_perso.getGroup() != null) {
                                if (_perso.getGroup().getPlayers().size() > 2) {
                                    _perso.sendText("Votre groupe comporte plus de 2 joueurs ! L'arène est de type 2v2, ne l'oubliez pas !");
                                    break;
                                }
                                if (!_perso.getGroup().isChief(_perso.getGuid())) {
                                    _perso.sendText("Vous n'êtes pas chef de groupe !");
                                    break;
                                }
                                String players = "";
                                boolean first = true;
                                int classe = 0;
                                for (Player c : _perso.getGroup().getPlayers()) {
                                    if (classe == c.get_classe()) {
                                        _perso.sendText("Vous ne pouvez pas créer de team avec deux mêmes classes !");
                                        break;
                                    }
                                    if (!Arena.isVerifiedTeam(_perso.get_classe(), c.get_classe())) {
                                        _perso.sendText("Vous ne pouvez pas créer de team avec deux classes type pillier ! (Xélor, Sacrieur, Eniripsa, Osamodas)");
                                        break;
                                    }
                                    if (first) {
                                        classe = c.get_classe();
                                        players = String.valueOf(c.getGuid());
                                        first = false;
                                    } else if (!first) {
                                        players += "," + c.getGuid();
                                    }
                                }
                                if (Team.addTeam(s, players, 0, 0)) {
                                    for (Player c : _perso.getGroup().getPlayers()) {
                                        c.sendText("La Team '<b>" + Team.getTeamByID(_perso.getTeamID()).getName() + "</b>' a été créée avec succès !");
                                    }
                                }
                                break;
                            } else {
                                _perso.sendText("Vous n'avez pas de groupe, et par conséquent, aucun partenaire à  ajouter dans votre Team !");
                                break;
                            }
                        case 30: //Delete de Team Arena
                            String string;
                            try {
                                string = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                if (_perso.getTeamID() != -1)
                                    _perso.sendText("Etes vous sà»r de vouloir détruire votre team ? (Quà´te d'arène: " + Team.getTeamByID(_perso.getTeamID()).getCote() + ")\n Si oui, faites .removeteam ok !");
                                else
                                    _perso.sendText("Vous n'avez actuellement aucune team !");
                                break;
                            }
                            if (string.equals("ok")) {
                                if (_perso.getArena() > -1)
                                    _perso.sendText("Action impossible, vous êtes inscris, ou déjà  en combat d'arène !");
                                else
                                    Team.removeTeam(Team.getTeamByID(_perso.getTeamID()), _perso);
                                break;
                            } else {
                                _perso.sendText("Tà¢che non reconnue, faites ." + command.getName() + " pour avoir des informations sur la commande.");
                                break;
                            }
                        case 31: //Informations de Team Arena
                            if (_perso.getTeamID() != -1) {
                                Player coep = Team.getPlayer(Team.getTeamByID(_perso.getTeamID()), 1);
                                if (Team.getPlayer(Team.getTeamByID(_perso.getTeamID()), 1) == _perso)
                                    coep = Team.getPlayer(Team.getTeamByID(_perso.getTeamID()), 2);
                                _perso.sendText("<b>" + Team.getTeamByID(_perso.getTeamID()).getName() + "</b>\n" +
                                        "Partenaire: <b>" + coep.getName() + "</b>\n" +
                                        "Cà´te: <b>" + Team.getTeamByID(_perso.getTeamID()).getCote() + "</b>\n" +
                                        "Rang: <b>" + Team.getTeamByID(_perso.getTeamID()).getRank() + "</b>");
                                break;
                            } else {
                                _perso.sendText("Vous n'avez pas d'équipe d'arène 2v2 !");
                                break;
                            }
                        case 32: //Arena Inscription/Désinscription
                            String arena;
                            try {
                                arena = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                if (_perso.getArena() == 0)
                                    _perso.sendText("Vous êtes déjà  inscris au tournoi d'arène 2v2 ! Faites |<b> ." + command.getName() + " off </b>| pour vous désinscrire...");
                                else if (_perso.getArena() == 1)
                                    _perso.sendText("Vous êtes en combat d'arène !");
                                else
                                    _perso.sendText("Vous n'êtes pas inscris, faites |<b> ." + command.getName() + " on </b>| pour vous inscrire !");
                                break;
                            }
                            switch (arena) {
                                case "on":
                                    if (_perso.getTeamID() < 0) {
                                        _perso.sendText("Vous ne possédez aucune Team ! (<b>." + command.getName() + "</b> pour plus d'informations)");
                                        break label;
                                    } else if (_perso.getGroup() == null) {
                                        _perso.sendText("Vous devez être grouppé avec votre partenaire de Team ! (<b>." + command.getName() + "</b> pour plus d'informations)");
                                        break label;
                                    } else {
                                        if (_perso.getGroup().getPlayers().size() > 2 || _perso.getGroup().getPlayers().size() < 2) {
                                            _perso.sendText("Votre groupe doit contenir exactement deux joueurs ! Vous, et votre partenaire de Team !");
                                            break label;
                                        } else if (!_perso.getGroup().isChief(_perso.getGuid())) {
                                            _perso.sendText("Vous devez être le chef de groupe pour vous inscrire !");
                                            break label;
                                        }
                                        for (Player c : _perso.getGroup().getPlayers()) {
                                            try {
                                                if (!Team.getPlayers(Team.getTeamByID(_perso.getTeamID())).contains(c)) {
                                                    _perso.sendText("Le joueur " + c.getName() + " n'est pas votre partenaire de Team !");
                                                    break;
                                                } else if (!c.isOnline()) {
                                                    _perso.sendText("Le joueur " + c.getName() + " n'est pas connecté !");
                                                    break;
                                                } else if (c.getFight() != null) {
                                                    _perso.sendText("Le joueur " + c.getName() + " est en combat !");
                                                    break;
                                                } else if (c.getArena() != -1) {
                                                    _perso.sendText("Le joueur " + c.getName() + " est déjà  inscris au tournoi d'arène 2v2 !");
                                                    break;
                                                } else {
                                                    Arena.addTeam(Team.getTeamByID(_perso.getTeamID()));
                                                    break;
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                    break;
                                case "off":
                                    try {
                                        if (_perso.getTeamID() < 0) {
                                            _perso.sendText("Vous ne possédez aucune Team ! (<b>." + command.getName() + "</b> pour plus d'informations)");
                                            break;
                                        } else if (_perso.getArena() == 1 && _perso.getFight() != null) {
                                            _perso.sendText("Vous êtes en plein tournoi !");
                                            break;
                                        } else {
                                            Arena.delTeam(Team.getTeamByID(_perso.getTeamID()));
                                            break;
                                        }
                                    } catch (Exception e) {
                                    }
                                    break;
                                default:
                                    _perso.sendText("Tà¢che non reconnue, faites ." + command.getName() + " pour avoir des informations sur la commande.");
                                    break label;
                            }
                            break;
                        case 33: //RapidStuffs
                            String rs;
                            try {
                                rs = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("<b>Equipements rapides: </b>\n" +
                                        "<b>." + command.getName() + " create [name]</b> pour créer un nouveau stuff\n" +
                                        "<b>." + command.getName() + " remove [name]</b> pour supprimer un stuff\n" +
                                        "<b>." + command.getName() + " view</b> pour voir tous vos stuffs rapides disponibles\n" +
                                        "<b>." + command.getName() + " equip [name]</b> pour équiper un stuff rapidement"
                                );
                                break;
                            }

                            if (msg.length() >= command.getName().length() + 8 && msg.substring(command.getName().length() + 2, command.getName().length() + 8).equals("create")) {
                                if (_perso.getRapidStuffs().size() > 9) {
                                    _perso.sendText("Vous ne pouvez pas avoir plus de 10 stuffs rapides !");
                                    break;
                                }
                                String name = "";
                                try {
                                    name = msg.substring(command.getName().length() + 9, msg.length() - 1);
                                } catch (Exception e) {
                                    _perso.sendText("Erreur ! Entrez un nom à  votre stuff rapide: ." + command.getName() + " create [name]");
                                    break;
                                }

                                int coiffe = _perso.getObjetByPosSpece(Constant.ITEM_POS_COIFFE);
                                int cape = _perso.getObjetByPosSpece(Constant.ITEM_POS_CAPE);
                                int arme = _perso.getObjetByPosSpece(Constant.ITEM_POS_ARME);
                                int anneau1 = _perso.getObjetByPosSpece(Constant.ITEM_POS_ANNEAU1);
                                int amulette = _perso.getObjetByPosSpece(Constant.ITEM_POS_AMULETTE);
                                int ceinture = _perso.getObjetByPosSpece(Constant.ITEM_POS_CEINTURE);
                                int bottes = _perso.getObjetByPosSpece(Constant.ITEM_POS_BOTTES);
                                int bouclier = _perso.getObjetByPosSpece(Constant.ITEM_POS_BOUCLIER);
                                int anneau2 = _perso.getObjetByPosSpece(Constant.ITEM_POS_ANNEAU2);
                                int dofus1 = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS1);
                                int dofus2 = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS2);
                                int dofus3 = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS3);
                                int dofus4 = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS4);
                                int dofus5 = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS5);
                                int dofus6 = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS6);
                                int familier = _perso.getObjetByPosSpece(Constant.ITEM_POS_FAMILIER);

                                if (!RapidStuff.addRapidStuff(_perso, name, coiffe + "," + cape + "," + anneau1 + "," + amulette + "," + ceinture + "," + bottes + "," + familier + "," + bouclier + "," + arme + "," + anneau2 + "," + dofus1 + "," + dofus2 + "," + dofus3 + "," + dofus4 + "," + dofus5 + "," + dofus6))
                                    _perso.sendText("Erreur ! Un stuff rapide est identique ou le nom est déjà  utilisé !");
                                else
                                    _perso.sendText("Nouveau stuff enregistré avec succès !");
                                break;
                            } else if (msg.length() >= command.getName().length() + 8 && msg.substring(command.getName().length() + 2, command.getName().length() + 8).equals("remove")) {
                                String name = "";
                                try {
                                    name = msg.substring(command.getName().length() + 9, msg.length() - 1);
                                } catch (Exception e) {
                                    _perso.sendText("Erreur ! Entrez le nom du stuff rapide à  supprimer: " + command.getName() + " remove [name]");
                                    break;
                                }
                                if (_perso.getRapidStuffByName(name) != null && RapidStuff.removeRapidStuff(_perso.getRapidStuffByName(name)))
                                    _perso.sendText("Le stuff <b>" + name + "</b> a été supprimé avec succès !");
                                else
                                    _perso.sendText("Le stuff rapide <b>" + name + "</b> est innéxistant ! Faites ." + command.getName() + " view pour avoir la liste.");
                                break;
                            } else if (rs.equals("view")) {
                                String list = null;
                                if (_perso.getRapidStuffs().isEmpty()) {
                                    _perso.sendText("Vous n'avez aucun équipement rapide !");
                                    break;
                                } else {
                                    for (RapidStuff ss : _perso.getRapidStuffs()) {
                                        if (list == null) {
                                            list = "-" + ss.getName();
                                        } else {
                                            list += "\n-" + ss.getName();
                                        }
                                    }
                                    _perso.sendText("\n\n<b>Faites " + command.getName() + " equip + [name] :</b>\n" + list);
                                    break;
                                }
                            } else if (msg.length() >= command.getName().length() + 7 && msg.substring(command.getName().length() + 2, command.getName().length() + 7).equals("equip")) {
                                String name = "";
                                try {
                                    name = msg.substring(command.getName().length() + 8, msg.length() - 1);
                                } catch (Exception e) {
                                    _perso.sendText("Erreur ! Entrez le nom du stuff rapide à  équiper: " + command.getName() + " equip [name]");
                                    break;
                                }

                                if (_perso.getRapidStuffByName(name) != null) {
                                    boolean first = true;
                                    int number = 1;

                                    for (Item rapidStuff : _perso.getRapidStuffByName(name).getObjects()) {
                                        if (!_perso.hasItemGuid(rapidStuff.getGuid())) {
                                            _perso.sendText("L'item <b>" + World.getObjet(rapidStuff.getGuid()).getTemplate(false).getName() + "</b> ne vous appartient plus et n'a pas pu être équipé !");
                                            continue;
                                        } else {
                                            int pos = Constant.getObjectPosByType(rapidStuff.getTemplate(false).getType()); //Bordel de pos, à§a m'aura bien fais chié
                                            if (rapidStuff.getTemplate(true).isArm())
                                                pos = 1;
                                            if (rapidStuff.getTemplate(true).getType() == Constant.ITEM_TYPE_ANNEAU) {
                                                if (first) {
                                                    pos = 2;
                                                    first = false;
                                                } else
                                                    pos = 4;
                                            }
                                            if (rapidStuff.getTemplate(true).getType() == Constant.ITEM_TYPE_DOFUS) {
                                                switch (number) {
                                                    case 1:
                                                        pos = Constant.ITEM_POS_DOFUS1;
                                                        number++;
                                                        break;
                                                    case 2:
                                                        pos = Constant.ITEM_POS_DOFUS2;
                                                        number++;
                                                        break;
                                                    case 3:
                                                        pos = Constant.ITEM_POS_DOFUS3;
                                                        number++;
                                                        break;
                                                    case 4:
                                                        pos = Constant.ITEM_POS_DOFUS4;
                                                        number++;
                                                        break;
                                                    case 5:
                                                        pos = Constant.ITEM_POS_DOFUS5;
                                                        number++;
                                                        break;
                                                    case 6:
                                                        number = Constant.ITEM_POS_DOFUS6;
                                                        break;
                                                }
                                            }
                                            GameThread.Object_move(_perso, _out, 1, rapidStuff.getGuid(), pos); //On double pour les déséquipements autos
                                            if (_perso.isEquip()) { //C'est balow
                                                GameThread.Object_move(_perso, _out, 1, rapidStuff.getGuid(), pos);
                                                _perso.setEquip(false);
                                            }
                                        }
                                    }
                                    break;
                                } else {
                                    _perso.sendText("Erreur ! Nom incorrect: " + command.getName() + " equip [name]");
                                    break;
                                }
                            } else {
                                _perso.sendText("<b>Equipements rapides: </b>\n\n" +
                                        "<b>" + command.getName() + " create [name]</b> pour créer un nouveau stuff\n" +
                                        "<b>" + command.getName() + " remove [name]</b> pour supprimer un stuff\n" +
                                        "<b>" + command.getName() + " view</b> pour voir tous vos stuffs rapides disponbiles\n" +
                                        "<b>" + command.getName() + " equip [name]</b> pour équiper un stuff rapidement"
                                );
                                break;
                            }
                        case 34: //Affichage des points du compte
                            int accountPoints = Util.loadPointsByAccount(_perso.getAccount());
                            _perso.sendText("Votre compte contient " + accountPoints + " points boutiques !");
                            break;
                        case 35: //Job levelUp
                            String st;
                            int job = 0;
                            int item = 0;
                            try {
                                st = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("<b>Liste(Faites .learn + Job):<br /></b> Cordomage,joaillomage,costumage,dague,epee,marteau,pelle,hache,arc,baguette,baton");
                                return true;
                            }

                            if (st.equalsIgnoreCase("cordomage")) {
                                item = 7495;
                                job = 62;
                            } else if (st.equalsIgnoreCase("joaillomage")) {
                                item = 7493;
                                job = 63;
                            } else if (st.equalsIgnoreCase("costumage")) {
                                item = 7494;
                                job = 64;
                            } else if (st.equalsIgnoreCase("dague")) {
                                item = 1520;
                                job = 43;
                            } else if (st.equalsIgnoreCase("epee")) {
                                item = 1339;
                                job = 44;
                            } else if (st.equalsIgnoreCase("marteau")) {
                                item = 1561;
                                job = 45;
                            } else if (st.equalsIgnoreCase("pelle")) {
                                item = 1560;
                                job = 46;
                            } else if (st.equalsIgnoreCase("hache")) {
                                item = 1562;
                                job = 47;
                            } else if (st.equalsIgnoreCase("arc")) {
                                item = 1563;
                                job = 48;
                            } else if (st.equalsIgnoreCase("baguette")) {
                                item = 1564;
                                job = 49;
                            } else if (st.equalsIgnoreCase("baton")) {
                                item = 1565;
                                job = 50;
                            } else {
                                _perso.sendText("Metier non reconnu, faites .learn pour avoir la liste");
                                return true;
                            }

                            _perso.learnJob(World.getMetier(job));
                            ObjTemplate a = World.getObjTemplate(item);
                            Item objs = a.createNewItem(1, false, -1);
                            if (_perso.addObjet(objs, true))//Si le joueur n'avait pas d'item similaire
                                World.addObjet(objs, true);

                            StatsMetier SM = _perso.getMetierByID(job);
                            SM.addXp(_perso, 1000000);
                            _perso.sendText("Vous avez appris le métier avec succès et avez reà§u l'arme de FM disponibles dans votre inventaire");
                            break;
                        case 36: //Modifier la taille du personnage
                            int size = Integer.parseInt(msg.substring(command.getName().length() + 2, msg.length() - 1));
                            int lock = Integer.parseInt(args);
                            if (size > lock)
                                size = lock;
                            _perso.set_size(size);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(), _perso.getGuid());
                            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(), _perso);
                            break;
                        case 37: //FmCac
                            Item object = _perso.getObjetByPos(Constant.ITEM_POS_ARME);

                            if (_perso.getFight() != null) {
                                _perso.sendText("Action impossible : vous ne devez pas être en combat");
                                return true;

                            } else if (object == null) {
                                SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous ne portez pas d'arme", Config.CONFIG_MOTD_COLOR);
                                return true;
                            }

                            boolean containNeutre = false;

                            for (SpellEffect effect : object.getEffects()) {
                                if (effect.getEffectID() == 100 || effect.getEffectID() == 95)
                                    containNeutre = true;
                            }
                            if (!containNeutre) {
                                SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : votre arme n'a pas de dégats neutre", Config.CONFIG_MOTD_COLOR);
                                return true;
                            }

                            String answer;

                            try {
                                answer = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous n'avez pas spécifié l'élément (air, feu, terre, eau) qui remplacera les dégats/vols de vies neutres", Config.CONFIG_MOTD_COLOR);
                                return true;
                            }

                            if (!answer.equalsIgnoreCase("air") && !answer.equalsIgnoreCase("terre") && !answer.equalsIgnoreCase("feu") && !answer.equalsIgnoreCase("eau")) {
                                SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : l'élément " + answer + " n'existe pas ! (dispo : air, feu, terre, eau)", Config.CONFIG_MOTD_COLOR);
                                return true;
                            }

                            for (int i = 0; i < object.getEffects().size(); i++) {
                                if (object.getEffects().get(i).getEffectID() == 100) {
                                    if (answer.equalsIgnoreCase("air"))
                                        object.getEffects().get(i).setEffectID(98);

                                    if (answer.equalsIgnoreCase("feu"))
                                        object.getEffects().get(i).setEffectID(99);

                                    if (answer.equalsIgnoreCase("terre"))
                                        object.getEffects().get(i).setEffectID(97);

                                    if (answer.equalsIgnoreCase("eau"))
                                        object.getEffects().get(i).setEffectID(96);
                                }
                                if (object.getEffects().get(i).getEffectID() == 95) {
                                    if (answer.equalsIgnoreCase("air"))
                                        object.getEffects().get(i).setEffectID(93);

                                    if (answer.equalsIgnoreCase("feu"))
                                        object.getEffects().get(i).setEffectID(94);

                                    if (answer.equalsIgnoreCase("terre"))
                                        object.getEffects().get(i).setEffectID(92);

                                    if (answer.equalsIgnoreCase("eau"))
                                        object.getEffects().get(i).setEffectID(91);
                                }
                            }

                            SocketManager.GAME_SEND_OCO_PACKET(_perso, object);
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            _perso.sendText("Votre objet <b>" + object.getTemplate(false).getName() + "</b> a été forgemagé avec succès en " + answer + " !");
                            //_perso.resetItemsList(); // Ouch !
                            break;
					/*case 37: //FmCac
						Item object = _perso.getObjetByPos(Constant.ITEM_POS_ARME);
						int fmprice = Integer.parseInt(args);
						
						if(_perso.get_kamas() < fmprice) {
							_perso.sendText("Action impossible : vous avez moins de "+fmprice+" k");
							return true;
							
						} else if(_perso.getFight() != null) {
							_perso.sendText("Action impossible : vous ne devez pas être en combat");
							return true;
						
						} else if(object == null) {
							_perso.sendText("Action impossible : vous ne portez pas d'arme");
							return true;
						}
			
						boolean containNeutre = false;
						
						for(SpellEffect effect : object.getEffects()) {
							if(effect.getEffectID() == 100 || effect.getEffectID() == 95)
								containNeutre = true;
						}
						if(!containNeutre) {
							_perso.sendText("Action impossible : votre arme n'a pas de dégats neutre");
							return true;
						}
			
						String answer;
			
						try {
							answer = msg.substring(command.getName().length()+2, msg.length() - 1);
						} catch(Exception e) {
							_perso.sendText("Action impossible : vous n'avez pas spécifié l'élément (air, feu, terre, eau) qui remplacera les dégats/vols de vies neutres");
							return true;
						}
			
						if(!answer.equalsIgnoreCase("air") && !answer.equalsIgnoreCase("terre") && !answer.equalsIgnoreCase("feu") && !answer.equalsIgnoreCase("eau")) {
							_perso.sendText("Action impossible : l'élément " + answer + " n'existe pas ! (dispo : air, feu, terre, eau)");
							return true;
						}
			
						for(int i = 0; i < object.getEffects().size(); i++) {
							
							if(object.getEffects().get(i).getEffectID() == 100) {
								if(answer.equalsIgnoreCase("air"))
									object.getEffects().get(i).setEffectID(98);
								
								if(answer.equalsIgnoreCase("feu"))
									object.getEffects().get(i).setEffectID(99);
								
								if(answer.equalsIgnoreCase("terre"))
									object.getEffects().get(i).setEffectID(97);
								
								if(answer.equalsIgnoreCase("eau"))
									object.getEffects().get(i).setEffectID(96);
							}
				
							if(object.getEffects().get(i).getEffectID() == 95) {
								if(answer.equalsIgnoreCase("air"))
									object.getEffects().get(i).setEffectID(93);
								
								if(answer.equalsIgnoreCase("feu"))
									object.getEffects().get(i).setEffectID(94);
									
								if(answer.equalsIgnoreCase("terre"))
									object.getEffects().get(i).setEffectID(92);
								
								if(answer.equalsIgnoreCase("eau"))
									object.getEffects().get(i).setEffectID(91);
								
							}
						}
			
						long new_kamas = _perso.get_kamas() - fmprice ;
						if(new_kamas < 0) new_kamas = 0;
						_perso.set_kamas(new_kamas);
			
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						_perso.sendText("Votre objet <b>" + object.getTemplate().getName() + "</b> a été forgemagé avec succès en " + answer);
						_perso.sendText("Pensez à  vous déconnecter pour voir les modifications apportés à  votre arme !");
						break;*/
                        case 38: //Téléportation

                            if (_perso.getFight() != null) {
                                break;
                            }
                            short mapID = (short) Integer.parseInt(args.split(",")[0]);
                            int cellID = Integer.parseInt(args.split(",")[1]);
                            _perso.teleport(mapID, cellID);
                            break;
                        case 39: //Type LevelUp des sorts
                            String[] mySplit = args.split(",");
                            int levelUp = Integer.parseInt(mySplit[0]) > 6 ? (6) : (Integer.parseInt(mySplit[0]));
                            boolean isFree = Boolean.parseBoolean(mySplit[1].toLowerCase());

                            for (SortStats spell : _perso.getSorts()) {
                                int curLevel = _perso.getSortStatBySortIfHas(spell.getSpellID()).getLevel();
                                if (curLevel != 6 || curLevel < levelUp) {
                                    while (curLevel < levelUp) {
                                        if (!isFree) {
                                            if (_perso.get_spellPts() >= curLevel && World.getSort(spell.getSpellID()).getStatsByLevel(curLevel + 1).getReqLevel() <= _perso.getLevel()) {
                                                if (_perso.learnSpell(spell.getSpellID(), curLevel + 1, false, false)) {
                                                    _perso.set_spellPts(_perso.get_spellPts() - curLevel);
                                                }
                                            }
                                            curLevel++;
                                        } else {
                                            if (World.getSort(spell.getSpellID()).getStatsByLevel(curLevel + 1).getReqLevel() <= _perso.getLevel())
                                                _perso.learnSpell(spell.getSpellID(), curLevel + 1, false, false);
                                            curLevel++;
                                        }
                                    }
                                }
                            }
                            SocketManager.GAME_SEND_STATS_PACKET(_perso);
                            SocketManager.GAME_SEND_SPELL_LIST(_perso);
                            break;
                        case 40://event
                            if (_perso.getFight() != null)
                                break;
                            if (!GmCommand.event) {
                                _perso.sendText("Aucun event n'est en cours de préparation.");
                                break;
                            } else if (GmCommand.event) {
                                _perso.teleport(GmCommand.eventMap, GmCommand.eventCell);
                                _perso.sendText("Vous venez d'être téléporté vers l'event");
                            }
                            break;
                        case 41://Changer de nom
                            if (_perso.getFight() != null)
                                return true;
                            int nombrePoint = Util.loadPointsByAccount(_perso.getAccount());
                            if (nombrePoint < 50) {
                                _perso.sendText("Il vous manque " + (50 - nombrePoint) + " points !");
                                return true;
                            }
                            String name = null;

                            try {
                                name = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("Action impossible : vous n'avez pas spécifié votre nouveau nom");
                                return true;
                            }


                            if (!CharacterName.VerifName(name)) {
                                _perso.sendText("Caractère spéciaux interdit !");
                                return true;

                            }

                            if (CharacterName.containsWhiteSpace(name)) {
                                _perso.sendText("Votre nom ne doit pas contenir d'espace.");
                                return true;
                            }

                            if (SQLManager.PLAYER_EXIST(name)) {
                                _perso.sendText("Ce nom est déjà utilisé.");
                                return true;
                            }

                            if (name.length() > 15) {
                                _perso.sendText("Votre nom contient trop de caractères.");
                                return true;
                            }

                            if (name.length() < 3) {
                                _perso.sendText("Votre nom doit contenir au moins 3 caractères.");
                                return true;
                            }

                            if (!name.matches(".*[a-zA-Z]+.*")) {
                                _perso.sendText("Votre nom doit contenir au moins une lettre.");
                                return true;
                            }
                            _perso.set_name(name);
                            SQLManager.SAVE_PERSONNAGE(_perso, false);
                            SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                            Util.updatePointsByAccount(_perso.getAccount(), nombrePoint - 50);
                            _perso.send("000C" + Util.loadPointsByAccount(_perso.getAccount()));
                            break;
                        case 42: //Point d'honneur
                            if (_perso.getFight() != null)
                                break;

                            int honor = Integer.parseInt(args);

                            if (_perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
                                _perso.sendText("Vous êtes neutre !");
                                return true;
                            }
                            _perso.addHonor(honor);
                            break;
                        case 43: //morph avec args spécifique
                            if (_perso.getFight() != null)
                                return true;

                            morphID = Integer.parseInt(args);

                            _perso.set_gfxID(morphID);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(),
                                    _perso.getGuid());
                            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(),
                                    _perso);
                            _perso.sendText("Vous avez été transformé !");
                            //return true;
                            break;
                        case 44: //Fm pa
                            String choix = null;
                            Item items = null;

                            if (_perso.getFight() != null) {
                                _perso.sendText("Commande inutilisable en combat.");
                                return true;
                            }


                            try {
                                choix = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("<b> Liste (Faites .fmpa + l'item (à  porter sur le personnage) que vous voulez fm PA) : <br /></b> Coiffe, Cape, AnneauDroite, AnneauGauche, Amulette, Ceinture, Bottes");
                                return true;
                            }

                            if (choix.equalsIgnoreCase("Coiffe")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_COIFFE);
                            } else if (choix.equalsIgnoreCase("Cape")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_CAPE);
                            } else if (choix.equalsIgnoreCase("AnneauDroite")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
                            } else if (choix.equalsIgnoreCase("AnneauGauche")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
                            } else if (choix.equalsIgnoreCase("Ceinture")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_CEINTURE);
                            } else if (choix.equalsIgnoreCase("Bottes")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_BOTTES);
                            } else if (choix.equalsIgnoreCase("Amulette")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_AMULETTE);
                            } else if (items == null) {
                                _perso.sendText("Vous ne portez pas l'item néccéssaire.");
                                return true;
                            }

                            if (items.getObvijevanLook() != 0) {
                                _perso.sendText("Vous ne pouvez pas FM un objet vivant ! Dissociez l'objet avant !");
                                return true;
                            }
                            Stats stats = items.getStats();

                            if (stats.getEffect(111) > 0 || stats.getEffect(128) > 0) {
                                _perso.sendText("Ton item te donne déjà  1 PA ou 1 PM.");
                                return true;
                            } else {
                                items.getStats().addOneStat(111, 1);
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                                _perso.sendText("Votre " + (items).getTemplate(false).getName() + " donne désormais +1 PA en plus de ses jets habituels !");
                            }
                            break;

                        case 45: //Fm pm
                            items = null;

                            if (_perso.getFight() != null) {
                                _perso.sendText("Commande inutilisable en combat.");
                                return true;
                            }


                            try {
                                choix = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("<b> Liste (Faites .fmpa + l'item (à  porter sur le personnage) que vous voulez fm PA) : <br /></b> Coiffe, Cape, AnneauDroite, AnneauGauche, Amulette, Ceinture, Bottes");
                                return true;
                            }

                            if (choix.equalsIgnoreCase("Coiffe")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_COIFFE);
                            } else if (choix.equalsIgnoreCase("Cape")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_CAPE);
                            } else if (choix.equalsIgnoreCase("AnneauDroite")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
                            } else if (choix.equalsIgnoreCase("AnneauGauche")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
                            } else if (choix.equalsIgnoreCase("Ceinture")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_CEINTURE);
                            } else if (choix.equalsIgnoreCase("Bottes")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_BOTTES);
                            } else if (choix.equalsIgnoreCase("Amulette")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_AMULETTE);
                            } else if (items == null) {
                                _perso.sendText("Vous ne portez pas l'item neccéssaire.");
                                return true;
                            }

                            if (items.getObvijevanLook() != 0) {
                                _perso.sendText("Vous ne pouvez pas FM un objet vivant ! Dissociez l'objet avant !");
                                return true;
                            }
                            stats = items.getStats();

                            if (stats.getEffect(111) > 0 || stats.getEffect(128) > 0) {
                                _perso.sendText("Ton item te donne déjà  1 PA ou 1 PM.");
                                return true;
                            } else {
                                items.getStats().addOneStat(128, 1);
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                                _perso.sendText("Votre " + ((Item) items).getTemplate(false).getName() + " donne désormais +1 PA en plus de ses jets habituels !");
                            }
                            break;
                        case 49: //Taille personnage fixe
                            size = Integer.parseInt(args);
                            _perso.set_size(size);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(), _perso.getGuid());
                            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(), _perso);
                            break;
                        case 50: //Ticket support

                            int verif = SQLManager.Verifticket(_perso.getName());
                            String ticket;

                            try {
                                ticket = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("Vous n'avez saisie aucun message");
                                break;
                            }

                            if (verif == 0) {
                                SQLManager.addticket(_perso.getName(), ticket);
                                _perso.sendText("Votre ticket à  bien été pris en compte , il sera traité lorsque qu'un maitre de jeu sera disponible");
                                SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Ticket", "Un ticket vient d'être envoyé de la part de " + _perso.getName() + ".");
                                break;
                            } else {
                                _perso.sendText("Vous avez déjà Â  un ticket en cours . Merci de patienter");
                                break;
                            }
                        case 51: // Morph Item
                            if (_perso.getFight() != null)
                                break;

                            String params[] = {};
                            items = null;

                            try {
                                choix = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                _perso.sendText("<b> Liste (Faites .morphitem + l'item (à  porter sur le personnage) + l'id de la morph item) : <br /></b> Coiffe, Cape, AnneauDroite, AnneauGauche, Amulette, Ceinture, Bottes");
                                return true;
                            }

                            try {
                                params = choix.split(" ");
                            } catch (Exception e) {
                                _perso.sendText("Vous n'avez pas précisé l'ID de l'item dissocier.");
                            }

                            if (params[0].equalsIgnoreCase("Coiffe")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_COIFFE);
                            } else if (params[0].equalsIgnoreCase("Cape")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_CAPE);
                            } else if (params[0].equalsIgnoreCase("AnneauDroite")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
                            } else if (params[0].equalsIgnoreCase("AnneauGauche")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
                            } else if (params[0].equalsIgnoreCase("Ceinture")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_CEINTURE);
                            } else if (params[0].equalsIgnoreCase("Bottes")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_BOTTES);
                            } else if (params[0].equalsIgnoreCase("Amulette")) {
                                items = _perso.getObjetByPos(Constant.ITEM_POS_AMULETTE);
                            } else if (items == null) {
                                _perso.sendText("Vous ne portez pas l'item neccéssaire.");
                                return true;
                            }

                            //On vérifie si le joueur porte une coiffe ou cape obvijevan ?
                            if (params[0].equalsIgnoreCase("Coiffe")) {
                                if (_perso.getObjetByPos(Constant.ITEM_POS_COIFFE).getObvijevanPos() == 10) {
                                    _perso.sendText("Veuillez <b>dissocier</b> votre coiffe obvijevan de l'équipement.");
                                    return true;
                                }
                            } else if (params[0].equalsIgnoreCase("Cape")) {
                                if (_perso.getObjetByPos(Constant.ITEM_POS_CAPE).getObvijevanPos() == 11) {
                                    _perso.sendText("Veuillez <b>dissocier</b> votre cape obvijevan de l'équipement.");
                                    return true;
                                }
                            }

                            morphID = Integer.parseInt(params[1]);

                            ObjTemplate tmorph = World.getObjTemplate(morphID);
                            ObjTemplate tstats = items.getTemplate(true);

                            if (tmorph == null) {
                                mess = "Le template stats " + morphID + " n'existe pas ";
                                _perso.sendText(mess);
                                return true;
                            }

                            if (tmorph.getType() != tstats.getType()) {
                                mess = "Les deux items doivent être de même type.";
                                _perso.sendText(mess);
                                return true;
                            }

                            if (tmorph.getID() == tstats.getID()) {
                                _perso.sendText("Les deux items ne doivent être les mêmes.");
                                return true;
                            }

                            //Si toute les conditions sont respecté
                            _perso.DesequiperItem(items);

                            obj = new Item(World.getNewItemGuid(), tmorph.getID(), 1,
                                    Constant.ITEM_POS_NO_EQUIPED,
                                    items.getStats(),
                                    tstats.getEffectTemplate(tstats.getStrTemplate()),
                                    tstats.getBoostSpellStats(tstats.getStrTemplate()),
                                    tstats.getPrestige());
                            // tmorph.createNewItem(qua,useMax,-1);
                            if (_perso.addObjet(obj, true))// Si le joueur n'avait pas d'item simulaire
                                World.addObjet(obj, true);

                            //Suppression de l'ancien item
                            _perso.deleteItem(items.getGuid());
                            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, items.getGuid());

                            String str = "Creation de l'item " + tstats.getID() + " => " + morphID
                                    + " reussie";

                            _perso.sendText(str);
                            SocketManager.GAME_SEND_Ow_PACKET(_perso);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(), _perso.getAccID());
                            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(), _perso);
                            break;

                        case 52: //DeathMatch
                            String splits = "";
                            try {
                                splits = msg.substring(command.getName().length() + 2, msg.length() - 1);
                            } catch (Exception e) {
                                if (_perso.getDeathMatch() == 0)
                                    //_perso.sendText("Vous êtes déjà  inscris en <b>DeathMatch</b> ! Faites |<b> ."+command.getName()+" off </b>| pour vous désinscrire...");
                                    _perso.sendText("You are already register !</b> ! Make |<b> ." + command.getName() + " off </b>| for quit...");

                                else if (_perso.getDeathMatch() == 1)
                                    _perso.sendText("You are currently on fight.");
                                else
                                    _perso.sendText("You don't joined , make |<b> ." + command.getName() + " on </b>| for register to !");
                                break;
                            }
                            if (splits.equals("on")) {
                                if (_perso.getDeathMatch() != -1) {
                                    _perso.sendText("Already registered or currently on fight");
                                    break;
                                } else {
                                    if (_perso.getFight() != null) {
                                        _perso.sendText("You are currently on fight !");
                                        break;
                                    } else {
                                        dm.addPlayer(_perso);
                                        break;
                                    }
                                }
                            } else if (splits.equals("off")) {
                                try {

                                    if (_perso.getDeathMatch() == 1 && _perso.getFight() != null) {
                                        _perso.sendText("Already registered");
                                        break;
                                    } else if (_perso.getDeathMatch() == -1) {
                                        _perso.sendText("You aren't registered");
                                        break;
                                    } else {
                                        dm.delPlayer(_perso);
                                        break;
                                    }
                                } catch (Exception e) {
                                }

                            } else if (splits.equals("infos")) {
                                if (_perso.getDeathMatch() == 1 && _perso.getFight() != null) {
                                    _perso.sendText("Currently on Deathmatch");
                                    break;
                                } else if (_perso.getDeathMatch() == -1) {
                                    _perso.sendText("Currently not registered to Deathmatch");
                                    break;
                                } else {
                                    String content = null;

                                    content = "You are registered ! Waiting Opponents ...";

                                    _perso.sendText(content);
                                    break;
                                }
                            } else {
                                _perso.sendText("Unknown task, faites ." + command.getName() + " for get more infos about this command.");
                                break;
                            }

                            break;
                        case 53: //Steamer

                            int classe = 14;
                            int niveau = _perso.getLevel();
                            int deformaID = classe * 10 + _perso.get_sexe();

                            try {

                                if (classe == _perso.get_classe()) {
                                    _perso.sendText("IMPOSSIBLE : Votre personnage est déjà  de cette classe.");
                                    return true;
                                }

                                _perso.set_classe(classe);
                                Stats baseStats = _perso.get_baseStats();
                                baseStats.addOneStat(125, -_perso._baseStats.getEffect(125));
                                baseStats.addOneStat(124, -_perso._baseStats.getEffect(124));
                                baseStats.addOneStat(118, -_perso._baseStats.getEffect(118));
                                baseStats.addOneStat(123, -_perso._baseStats.getEffect(123));
                                baseStats.addOneStat(119, -_perso._baseStats.getEffect(119));
                                baseStats.addOneStat(126, -_perso._baseStats.getEffect(126));
                                _perso.set_gfxID(deformaID);
                                Thread.sleep(150);
                                _perso.set_lvl(1);
                                _perso.setCapital(0);
                                _perso.set_spellPts(0);
                                SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                                Thread.sleep(150);

                                _perso.setHechizos(Constant.getStartSorts(classe));

                                while (_perso.getLevel() < niveau) {
                                    _perso.levelUp(false, true);
                                }

                                SocketManager.GAME_SEND_SPELL_LIST(_perso);
                                SocketManager.GAME_SEND_NEW_LVL_PACKET(_perso.getAccount().getGameThread().getOut(), _perso.getLevel());
                                Thread.sleep(150);
                                SQLManager.CHANGER_SEX_CLASSE(_perso);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case 54: //Steamer

                            classe = 15;
                            niveau = _perso.getLevel();
                            deformaID = classe * 10 + _perso.get_sexe();

                            try {

                                if (classe == _perso.get_classe()) {
                                    _perso.sendText("IMPOSSIBLE : Votre personnage est déjà  de cette classe.");
                                    return true;
                                }

                                _perso.set_classe(classe);
                                Stats baseStats = _perso.get_baseStats();
                                baseStats.addOneStat(125, -_perso._baseStats.getEffect(125));
                                baseStats.addOneStat(124, -_perso._baseStats.getEffect(124));
                                baseStats.addOneStat(118, -_perso._baseStats.getEffect(118));
                                baseStats.addOneStat(123, -_perso._baseStats.getEffect(123));
                                baseStats.addOneStat(119, -_perso._baseStats.getEffect(119));
                                baseStats.addOneStat(126, -_perso._baseStats.getEffect(126));
                                _perso.set_gfxID(deformaID);
                                Thread.sleep(150);
                                _perso.set_lvl(1);
                                _perso.setCapital(0);
                                _perso.set_spellPts(0);
                                SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                                SocketManager.GAME_SEND_STATS_PACKET(_perso);
                                Thread.sleep(150);

                                _perso.setHechizos(Constant.getStartSorts(classe));

                                while (_perso.getLevel() < niveau) {
                                    _perso.levelUp(false, true);
                                }

                                SocketManager.GAME_SEND_SPELL_LIST(_perso);
                                SocketManager.GAME_SEND_NEW_LVL_PACKET(_perso.getAccount().getGameThread().getOut(), _perso.getLevel());
                                Thread.sleep(150);
                                SQLManager.CHANGER_SEX_CLASSE(_perso);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case 2007://GO 1.29 @Flow
                            try {
                                if (_perso.getFight() != null) break;
                                int UnMorphID = _perso.get_classe() * 10 + _perso.get_sexe();
                                _perso.set_gfxID(UnMorphID);
                                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(), _perso.getGuid());
                                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(), _perso);
                                _perso.sendText("Apparence de votre personnage : Dofus 1.29");
                            } catch (Exception e) {
                            }
                            break;

                        case 2008://GO 2.0 @Flow
                            try {
                                if (_perso.getFight() != null) break;
                                int UnMorphID = _perso.get_classe() * 100000 + _perso.get_sexe();
                                _perso.set_gfxID(UnMorphID);
                                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.getMap(), _perso.getGuid());
                                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(_perso.getMap(), _perso);
                                _perso.sendText("Apparence de votre personnage : Dofus 2.0");
                            } catch (Exception e) {
                            }
                            break;

                        case 2010://Enclos @Flow
                            try {
                                SocketManager.GAME_SEND_POPUP(_perso, "Bienvenue dans l'enclo !"
                                        + "\n Vous pouvez y stockez vos montures,"
                                        + "\n <u> Personne d'autre peut accèder à  cette enclo</u>"
                                        + "\n En appuyant sur Ok. Nous ne sommes pas responsable en cas de perte.");
                                _perso.set_away(true);
                                MountPark park = World.getCarte((short) 8745).getMountPark();
                                _perso.setPark(park);
                                String strf = park.parseData(_perso.getGuid(), (park.get_owner() == -1 ? true : false));
                                SocketManager.GAME_SEND_ECK_PACKET(_perso, 16, strf);
                            } catch (Exception e) {
                            }
                            break;
						
					/*case 2011:
						try{
						String option = msg.substring(command.getName().length()+2, msg.length() - 1);
						if (option.equalsIgnoreCase("ON")){
							_perso.modeTemporisation = true;
							_perso.sendText("Mode temporisation activé pour le personnage : <b>"+_perso.getName()+"</b>");
						}
						else if(option.equalsIgnoreCase("OFF")){
							_perso.modeTemporisation = false;
							_perso.sendText("Mode temporisation désactivé pour le personnage : <b>"+_perso.getName()+"</b>");
						}
						}
						catch(Exception e){
							_perso.sendText("Options disponibles: .temporisation on | .temprosation off");
						}
						break;*/
                        case 2012:
                            try {
                                String[] arg = msg.split(" ");
                                int nombreDeCaracteres = 0;
                                for (int i = 0; i < arg.length - 1; i++) {
                                    nombreDeCaracteres += arg[i].length();
                                }
                                if (arg.length < 3 || arg[2] == null) {
                                    _perso.sendText("Utilisez cette syntaxe: .creationtitre (titre souhaité) (couleur souhaité)");
                                } else if (arg.length > 5) {
                                    _perso.sendText("Votre titre contient trop de mots !");
                                } else if (nombreDeCaracteres - command.getName().length() > 24) {
                                    _perso.sendText("Votre titre contient trop de caractères !");
                                } else {
                                    String titre = "";
                                    for (int i = 1; i < arg.length - 1; i++) {
                                        if (i == arg.length - 2) { // Fin du titre
                                            titre += arg[i];
                                        } else {
                                            titre += arg[i] + " ";
                                        }
                                    }
                                    if (!Security.estTitreValide(titre)) {
                                        _perso.sendText("Votre titre contient une séquence interdite !");
                                    } else {
                                        String couleur = "";
                                        switch (arg[arg.length - 1].toUpperCase().substring(0, arg[arg.length - 1].length() - 1)) {
                                            case "VERT":
                                                couleur = "50944";
                                                break;
                                            case "ORANGE":
                                                couleur = "16754432";
                                                break;
                                            case "MAUVE":
                                                couleur = "13238525";
                                                break;
                                            case "ROSE-VIOLET":
                                                couleur = "13238525";
                                                break;
                                            case "KAKI":
                                                couleur = "13159424";
                                                break;
                                            case "BLANC":
                                                couleur = "16777215";
                                                break;
                                            default:
                                                _perso.sendText("Cette couleur n'est pas disponible, voici la liste des couleurs disponibles : Vert, Orange, Mauve, Rose-violet, Kaki, Blanc");
                                                break;
                                        }
                                        if (!Objects.equals(couleur, "")) {
                                            byte statusAjout = SQLManager.AJOUTER_TITRE_EN_ATTENTE(titre, couleur, _perso.getGuid(), _perso.getName());
                                            switch (statusAjout) {
                                                case 1:
                                                    _perso.sendText("Votre titre est maintenant en attente de validation par un membre du staff, vos points seront utilisés lorsque que votre titre sera validé. Bon jeu !");
                                                    break;
                                                case 2:
                                                    _perso.sendText("Une personne possède déjà ce titre, vérifiez si vous pouvez l'acheter dans nos titres offerts en .commandes");
                                                    break;
                                                default:
                                                    _perso.sendText("Il y a eu une erreur lors de la création de votre titre, vous conservez vos points par conséquent.");
                                                    break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                _perso.sendText("Utilisez cette syntaxe: .creationtitre (titre souhaité) (couleur souhaité)");
                            }
                            break;

                        case 2013:
                            if (_perso.getAccount().getGmLevel() != 0) {
                                try {
                                    int idValidation = Integer.valueOf(msg.substring(command.getName().length() + 2, msg.length() - 1));
                                    byte statusTitre = 0;
                                    try {
                                        statusTitre = SQLManager.VALIDER_TITRE(idValidation);
                                    } catch (Exception e) {
                                    }
                                    if (statusTitre == 1) {
                                        _perso.sendText("Le titre a bien été validé !");
                                    } else if (statusTitre == -1) {
                                        _perso.sendText("Il y a eu une erreur lors de la validation");
                                    } else if (statusTitre == 0) {
                                        _perso.sendText("Une personne a probablement déjà valider ce titre.");
                                    } else {
                                        _perso.sendText("Le joueur n'avait pas assez de points, le titre reste en attente.");
                                    }
                                } catch (Exception e) {
                                    _perso.sendText("N'oublier pas que la commande s'écrit de cette façon : .valider [id de validation]");
                                }
                            }
                            break;
                        case 2014:
                            if (_perso.getAccount().getGmLevel() != 0) {
                                try {
                                    String[] arg = msg.split(" ");
                                    String motif = "";
                                    int idValidation = Integer.valueOf(arg[1]);
                                    for (int i = 2; i < arg.length; i++) {
                                        if (i != arg.length - 1) {
                                            motif += arg[i] + " ";
                                        } else {
                                            motif += arg[i].substring(0, arg[i].length() - 1);
                                        }
                                    }
                                    SQLManager.REJETER_TITRE_EN_ATTENTE(idValidation, motif);
                                } catch (Exception e) {
                                    _perso.sendText("N'oublier pas que la commande s'écrit de cette façon : . refuser [id de validation] [motif]");
                                }
                            }
                            break;
                        case 2015:
                            int pointsJoueur = Util.loadPointsByAccount(_perso.getAccount());
                            if (pointsJoueur < 15) {
                                _perso.sendText("Vous n'avez pas assez de points !");
                            } else {
                                try {
                                    String couleurVoulue = msg.substring(command.getName().length() + 2, msg.length() - 1);
                                    String couleur = "";
                                    switch (couleurVoulue.toUpperCase()) {
                                        case "VERT":
                                            couleur = "50944";
                                            break;
                                        case "ORANGE":
                                            couleur = "16754432";
                                            break;
                                        case "MAUVE":
                                            couleur = "13238525";
                                            break;
                                        case "ROSE-VIOLET":
                                            couleur = "13238525";
                                            break;
                                        case "KAKI":
                                            couleur = "13159424";
                                            break;
                                        case "BLANC":
                                            couleur = "16777215";
                                            break;
                                        default:
                                            _perso.sendText("Cette couleur n'est pas disponible, voici la liste des couleurs disponibles : Vert, Orange, Mauve, Rose-violet, Kaki, Blanc");
                                            break;
                                    }
                                    if (!Objects.equals(couleur, "")) {
                                        int id = _perso.get_title();
                                        SQLManager.CHANGER_COULEUR_TITRE(id, couleur);
                                        int nouvelleSomme = Util.loadPointsByAccount(_perso.getAccount()) - 15;
                                        Util.updatePointsByAccount(_perso.getAccount(), nouvelleSomme);
                                        if (_perso.isOnline()) {
                                            _perso.send("000C" + nouvelleSomme);
                                            _perso.sendText("Votre titre affiche désormais une nouvelle couleur !");
                                            try {
                                                Thread.sleep(750);
                                            } catch (Exception e) {
                                            }
                                            if (_perso.getFight() == null) {
                                                SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    _perso.sendText("N'oubliez pas que commande fonctionne aisni .changercouleutitre (Couleur souhaitée)");
                                }
                            }

                            break;
                        case 2016:
                            try {
                                int nbPoints = Util.loadPointsByAccount(_perso.getAccount());
                                int idTitre = _perso.get_title();
                                if (nbPoints < 5) {
                                    _perso.sendText("Vous n'avez pas assez de points pour sauvegarder ce titre !");
                                } else if (idTitre == 0) {
                                    _perso.sendText("Vous ne possèder aucun titre");
                                } else if (SQLManager.VERIFIER_SI_TITRE_SAUVEGARDE(_perso.getGuid(), idTitre)) {
                                    _perso.sendText("Vous avez déjà sauvegardé ce titre.");
                                } else {
                                    SQLManager.SAUVEGARDER_TITRE(idTitre, _perso.getGuid());
                                    _perso.sendText("Votre titre a été sauvegardé !");
                                    Util.updatePointsByAccount(_perso.getAccount(), nbPoints - 5);
                                    _perso.send("000C" + (nbPoints - 5));
                                }
                            } catch (Exception e) {
                                _perso.sendText("Il y a eu une erreur lors de la sauvegarde de votre titre.");
                            }
                            break;
                        case 2017:
                            try {
                                Map<String, Integer> mesTitres = SQLManager.OBTENIR_LISTE_TITRE_SAUVEGARDE(_perso.getGuid());
                                if (mesTitres.isEmpty()) {
                                    _perso.sendText("Votre liste est vide.");
                                } else {
                                    List<String> tempList = new ArrayList<String>(mesTitres.keySet());
                                    String strToSend = "";
                                    for (int i = 0; i < tempList.size(); i++) {
                                        if (i == mesTitres.size() - 1) {
                                            strToSend += "" + i + " : " + tempList.get(i) + "";
                                        } else {
                                            strToSend += "" + i + " : " + tempList.get(i) + "\n";
                                        }
                                    }
                                    _perso.sendText(strToSend);
                                    _perso.sendText("Rappel : .selectiontitre + (numéro du titre) pour sélectionner votre titre.");
                                }
                            } catch (Exception e) {
                                _perso.sendText("Il y a eu une erreur lors de l'affichage de vos titres sauvegardés. Vous n'avez peut-être aucun titre de sauvegardé.");
                            }
                            break;
                        case 2018:
                            try {
                                int idDuTitreSelectionne = Math.abs(Integer.valueOf(msg.substring(command.getName().length() + 2, msg.length() - 1)));
                                Map<String, Integer> titres = SQLManager.OBTENIR_LISTE_TITRE_SAUVEGARDE(_perso.getGuid());
                                if (titres.isEmpty()) {
                                    _perso.sendText("Votre liste de titre sauvegardé est vide.");
                                } else {
                                    List<Integer> temp = new ArrayList<Integer>(titres.values());
                                    if (idDuTitreSelectionne >= temp.size()) { // Bon raisonnement
                                        _perso.sendText("Vous n'avez aucun titre sauvegardé ayant cet id.");
                                    } else {
                                        int titreChoisi = temp.get(idDuTitreSelectionne);
                                        _perso.set_title(titreChoisi);
                                        _perso.sendText("Votre titre a été changé avec succès !");
                                        try {
                                            Thread.sleep(750);
                                        } catch (Exception e) {
                                        }
                                        if (_perso.getFight() == null) {
                                            SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                _perso.sendText("Votre titre n'a pu être sélectionné.");
                            }
                            break;
                        case 2019:
                            try {
                                int titreSelection = Math.abs(Integer.valueOf(msg.substring(command.getName().length() + 2, msg.length() - 1)));
                                _perso.set_ornement(titreSelection);
                                _perso.sendText("Ornements changé !");
                                try {
                                    Thread.sleep(750);
                                } catch (InterruptedException ignored) {
                                }
                                if (_perso.getFight() == null) {
                                    SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.getMap(), _perso);
                                }
                            } catch (Exception e) {
                                _perso.sendText("Mauvaise syntaxe !");
                            }
                            break;
                        case 2020:
                            if (_perso.getFight() == null) {
                                Kolizeum k = _perso.getKolizeum();
                                if (k != null) {
                                    if (k.canStart()) {
                                        _perso.setWantToStartNow(true);
                                        _perso.sendText("Si les 3 autres joueurs veulent demarrer le kolizeum maintenant, il sera demarré !");
                                        k.verifStartNow();
                                    }
                                }
                            }
                            break;
                        case 2121:
                            _perso.noCrash = !_perso.noCrash;
                            if (_perso.noCrash) {
                                _perso.sendText("Le mode anti-crash est maintenant activé, celui-ci est un prototype.");
                            } else {
                                _perso.sendText("Le mode anti-crash est maintenant désactivé.");
                            }
                            break;
                        case 2222:
                            _perso.addObjet(World.getObjTemplate(470001).createNewItem(1000, true, -1));
                            _perso.sendText("Vous avez reçu 10000 PP ! Bon testing !");
                            break;
                        case 55: //Guerre de Guilde

                            String Target = null;
                            try {
                                Target = msg.substring(command.getName().length() + 2, msg.length() - 1).trim();
                            } catch (Exception e) {
                                _perso.sendText("Vous n'avez pas entré le nom de la guilde");
                            }

                            int idTarget;
                            if (SQLManager.Guild_VerifyExist(Target)) {
                                idTarget = SQLManager.Guild_GetIdByName(Target);
                            } else {
                                _perso.sendText("Cette guilde n'existe pas.");
                                break;
                            }

                            if (_perso.getFight() != null) {
                                _perso.sendText("Impossible en combat.");
                            }
                            if (_perso.get_guild() == null) {
                                _perso.sendText("Vous devez avoir une guilde pour lancer une guerre.");
                                break;
                            }
                            if (SQLManager.Guild_VerifyGuerre(_perso.get_guild().get_name()) || SQLManager.Guild_VerifyGuerre(Target)) {
                                _perso.sendText("Votre guilde ou la guilde attaqué est déjà  en guerre.");
                                break;
                            }
                            if (_perso.get_guild().getMember(_perso.getGuid()).getRank() == 0)

                            {
                                _perso.sendText("Vous devez être le meneur de la guilde pour lancer une guerre.");
                                break;
                            }
                            if (Target.equals(_perso.get_guild().get_name())) {
                                _perso.sendText("Vous ne pouvez attaquer votre propre guilde.");
                                break;
                            }

                            for (Player g : World.getGuild(_perso.get_guild().get_id()).getMembers()) {
                                if (g.getFight() != null) {
                                    SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(g.get_guild(), "", 0, "Guild Battle", "Le personnage " + g.getName() + " est en combat, il ne pourra rejoindre l'agression.");
                                }

                                if (g.isOnline()) {
                                    g.sendText("Votre meneur à  lancer une guerre contre : <b>" + Target + "</b>");
                                    GdG.addPlayer1(g);
                                }

                            }

                            GdG ge = new GdG(_perso.get_guild().get_id(), Target, 0, 0);
                            GdG gx = new GdG(SQLManager.Guild_GetIdByName(Target), _perso.get_guild().get_name(), 0, 0);
                            World.addGuerre(ge);
                            World.addGuerre(gx);

                            for (Player T : World.getGuild(idTarget).getMembers()) {
                                if (T.getFight() != null) {
                                    SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(T.get_guild(), "", 0, "Guild Battle", "Le personnage " + T.getName() + " est en combat, il ne pourra rejoindre l'agression.");
                                }

                                if (T.isOnline() && T.getFight() == null) {
                                    T.sendText("Votre guilde a été attaqué par : <b>" + _perso.get_guild().get_name() + "</b> !!");
                                    SQLManager.Guild_InsertNoGuerre(Target);
                                    GdG.addPlayer2(T);
                                }

                            }
                            GdG.newDeathMatch();
                        default:
                            break;
                    }
                }

                if (command.getPrice() > 0) {
                    diff = (points - price);
                    Util.updatePointsByAccount(_perso.getAccount(), diff);
                    _perso.send("000C" + diff);
                }
                SQLManager.SAVE_PERSONNAGE(_perso, true);
                return true;
            } else {
                _perso.sendText("Commande non reconnue ou incomplète !");
                return true;
            }

        }
        return false;
    }
}