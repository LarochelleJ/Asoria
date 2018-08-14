package org.area.object;

import lombok.Getter;
import org.area.client.Player;
import org.area.common.SocketManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by Meow on 2018-08-12.
 */
public class Porte {
    private List<Integer> cellulesRequises = new ArrayList<Integer>();
    @Getter
    private List<Integer> cellulesDebloquees = new ArrayList<Integer>();
    @Getter
    private int cellue;
    @Getter
    private boolean porteOuverte = false;
    private int tempsOuverture = 30;

    public Porte(String requises, int cellule, int temps, String cellulesDeboque) {
        for (String s : requises.split(Pattern.quote(","))) {
            cellulesRequises.add(Integer.valueOf(s));
        }
        for (String s : cellulesDeboque.split(Pattern.quote(","))) {
            cellulesDebloquees.add(Integer.valueOf(s));
        }
        this.cellue = cellule;
        tempsOuverture = temps;
    }

    public void ouvrir(final Maps o) {
        if (porteOuverte) return;
        boolean success = true;
        for (int cellule : cellulesRequises) {
            boolean vide = true;
            for (Player p : o.getPersos()) {
                if (p != null && p.isOnline()) {
                    if (p.getCurCell().getID() == cellule) {
                        vide = false;
                        break;
                    }
                }
            }
            if (vide) {
                success = false;
                break;
            }
        }

        if (success) {
            SocketManager.GAME_SEND_CELLULE_DEBLOQUEE_TO_MAP(o, cellulesDebloquees, true);
            SocketManager.GAME_SEND_OUVERTURE_PORTE_TO_MAP(o, cellue, 2);
            porteOuverte = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    porteOuverte = false;
                    SocketManager.GAME_SEND_OUVERTURE_PORTE_TO_MAP(o, cellue, 4);
                    SocketManager.GAME_SEND_CELLULE_DEBLOQUEE_TO_MAP(o, cellulesDebloquees, false);
                }
            }, tempsOuverture * 1000); // SECONDES
        }
    }
}
