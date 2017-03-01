package org.area.kernel;

import java.io.Console;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.area.client.Player;
import org.area.common.*;
import org.area.exchange.ExchangeClient;
import org.area.game.GameServer.SaveThread;
import org.area.kernel.Config;
import org.area.kernel.Console.Color;

public class ConsoleInputAnalyzer implements Runnable {
    private Thread _t;
    Player _perso;

    public ConsoleInputAnalyzer() {
        this._t = new Thread(this);
        _t.setDaemon(true);
        _t.start();
    }

    @Override
    public void run() {
        while (Main.isRunning) {
            Console console = System.console();
            String command;
            try {
                command = console.readLine();
            } catch (Exception e) {
                continue;
            }
            try {
                evalCommand(command);
            } catch (Exception e) {
            } finally {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void evalCommand(String command) {
        String[] args = command.split(" ");
        String fct = args[0].toUpperCase();
        if (fct.equals("SAVE")) {
            Thread t = new Thread(new SaveThread());
            t.start();
        } else if (fct.equals("EXIT")) {

            Reboot.reboot();

        } else if (fct.equals("STAFF")) {

            try {

                sendInfo("------------ Staff en ligne ----------------");
                ResultSet RS = SQLManager.executeQuery("SELECT pseudo from accounts WHERE logged = '1' AND level > '0';", false);
                while (RS.next()) {
                    sendInfo("- " + RS.getString("pseudo"));
                }
                RS.getStatement().close();
                RS.close();

                sendInfo("----------------------------------------");
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else if (fct.equals("REALM")) {
            // On tente de forcer la fermeture
            try {
                Main.exchangeClient.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ExchangeClient().start();
        } else if (fct.equals("BLOQUER")) {
            try {
                for (Player perso : World.getOnlinePlayers()) {
                    perso.mettreCombatBloque(true);
                }
                Constant.COMBAT_BLOQUE = !Constant.COMBAT_BLOQUE; // Elles sont pas vraiment constante maintenent @Flow ^^
                String str;
                if (Constant.COMBAT_BLOQUE) {
                    str = "Les combats sont bloqués !";
                } else {
                    str = "Les combats sont débloqués !";
                }
                sendInfo(str);
            } catch (Exception e) {
            }
        } else if (fct.equals("TOOGLE_DEBUG")) {

            Config.DEBUG = !Config.DEBUG;
            if (Config.DEBUG) {
                sendInfo("Debug active !");
            } else {
                sendInfo("Debug desactive !");
            }

        } else if (fct.equals("TOOGLE_LOG")) {

            Config.LOGS = !Config.LOGS;
            if (Config.LOGS) {
                sendInfo("Log active !");
            } else {
                sendInfo("Log desactive !");
            }
        } else if (fct.equalsIgnoreCase("ANNOUNCE")) {
            String announce = command.substring(9);
            String PrefixConsole = "<b>Serveur</b> : ";
            SocketManager.GAME_SEND_MESSAGE_TO_ALL(PrefixConsole + announce, Config.CONFIG_MOTD_COLOR);
            sendEcho("<Announce:> " + announce);
        } else if (fct.equals("CLS")) {

            org.area.kernel.Console.clear(false);

        } else if (fct.equals("KICK")) {

            Player perso = _perso;
            String name = null;
            try {
                name = command.substring(5);
            } catch (Exception e) {
            }
            ;
            perso = World.getPersoByName(name);
            if (perso == null) {
                String mess = "Le personnage n'existe pas.";
                sendEcho(mess);
                return;
            }
            if (perso.isOnline()) {
                perso.getAccount().getGameThread().kick();
                String mess = "Vous avez kick " + perso.getName();
                sendEcho(mess);
            } else {
                String mess = "Le personnage " + perso.getName() + " n'est pas connecte";
                sendEcho(mess);
            }

        } else if (fct.equals("KEY")) {
            String newKey = "0";
            try {
                newKey = command.substring(4);
            } catch (Exception e) {
            }
            Main.gameServer.encryptPacketKey = newKey;
            sendEcho("La clé d'encryption des packets est maintenant: " + newKey);
        } else if (fct.equals("DECRYPT")) {
            try {
                String toDecrypt = command.substring(8);
                sendEcho("Chaine decryptée: " + CryptManager.decryptPacket(toDecrypt));
            } catch (Exception e) {
                sendEcho("Impossible de décrypter ceci.");
            }

        } else if (fct.equals("RELOADSERV")) {
            sendEcho("Rechargement de la configuration");
            Config.load();
            sendEcho("Chargement des items : Ok");
            SQLManager.LOAD_MOB_TEMPLATE();
            sendEcho("Chargement des mobs : Ok");
            SQLManager.LOAD_NPC_TEMPLATE();
            SQLManager.LOAD_NPC_QUESTIONS();
            SQLManager.LOAD_NPC_ANSWERS();
            sendEcho("Chargement des NPC complets : Ok");
            SQLManager.LOAD_MOUNTPARKS();
            SQLManager.LOAD_MOUNTS();
            sendEcho("Chargement des montures et enclos : Ok");
            SQLManager.LOAD_TRIGGERS();
            sendEcho("Chargement des triggers : Ok");
            SQLManager.LOAD_OBJ_TEMPLATE();
            SQLManager.LOAD_ITEM_ACTIONS();
            sendEcho("Chargement des item actions : Ok");
            SQLManager.LOAD_SORTS();
            sendEcho("Chargement des sorts : Ok");
            SQLManager.LOAD_GMCOMMANDS();
            sendEcho("Chargement des commandes GM : Ok");
            SQLManager.LOAD_SORTS_INTERDITS();
            sendEcho("Chargement des sorts interdits en PvP : Ok");
            SQLManager.LOAD_COMMANDS();
            sendEcho("Chargement des commandes joueurs terminée !");
            sendEcho("Chargement des données terminée, Good :)");

        } else if (fct.equals("?") || command.equals("HELP")) {

            sendInfo("------------Commandes:------------");
            sendInfo("- SAVE pour sauvegarder le serveur.");
            sendInfo("- EXIT pour fermer le serveur.");
            sendInfo("- TOOGLE_DEBUG pour activer/desactiver le mode DEBUG.");
            sendInfo("- TOOGLE_SMALL pour actier/desactiver le mode DEBUG 2.");
            sendInfo("- TOOGLE_LOG pour activer/desactiver le systeme de logs");
            sendInfo("- INFOS pour afficher les informations comme en jeu.");
            sendInfo("- STAFF lister les membres du staff co.");
            sendInfo("- ANNOUNCE pour envoyer un message aux joueurs.");
            sendInfo("- KICK [Pseudo] pour kicker le joueur.");
            sendInfo("- RELOADSERV pour reload le serveur.");
            sendInfo("- CLS pour effacer le contenu de la console");
            sendInfo("- BlOQUER pour bloquer / débloquer combat");
            sendInfo("- REALM pour reset la connexion realm");
            sendInfo("- Touches CTRL+C pour stop le serveur.");
            sendInfo("- HELP ou ? pour afficher cette liste.");
            sendInfo("----------------------------------");

        } else if (fct.equals("INFOS")) {

            long uptime = System.currentTimeMillis() - Main.gameServer.getStartTime();
            int jour = (int) (uptime / (1000 * 3600 * 24));
            uptime %= (1000 * 3600 * 24);
            int hour = (int) (uptime / (1000 * 3600));
            uptime %= (1000 * 3600);
            int min = (int) (uptime / (1000 * 60));
            uptime %= (1000 * 60);
            int sec = (int) (uptime / (1000));

            String mess = "Area  " + Constant.RELEASE + "\n"
                    + "Uptime: " + jour + "d " + hour + "h " + min + "m " + sec + "s\n"
                    + "Joueurs En Ligne: " + Main.gameServer.getPlayerNumber() + "\n"
                    + "Record de Connexions: " + Main.gameServer.getMaxPlayer() + "\n";
            sendInfo(mess);

        } else if (fct.equals("ECHO")) {
            try {
                String message = command.substring(5);
                sendEcho(message);
            } catch (Exception e) {
            }

        } else {

            sendError("Commande non reconnue ou incomplete.");

        }
    }

    public static void sendInfo(String msg) {
        org.area.kernel.Console.println(msg, Color.GREEN);
    }

    public static void sendError(String msg) {
        org.area.kernel.Console.println(msg, Color.RED);
    }

    public static void send(String msg) {
        org.area.kernel.Console.println(msg);
    }

    public static void sendDebug(String msg) {
        if (Config.DEBUG) org.area.kernel.Console.println(msg, Color.YELLOW);
    }

    public static void sendEcho(String msg) {
        org.area.kernel.Console.println(msg, Color.BLUE);
    }

}
