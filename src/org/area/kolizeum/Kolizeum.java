package org.area.kolizeum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.area.client.Player;
import org.area.common.Formulas;
import org.area.common.SocketManager;
import org.area.kolizeum.Manager.Params;
import org.area.lang.Lang;

public class Kolizeum {

    /**
     * All kolizeums in waiting/fight
     **/
    private static List<Kolizeum> kolizeums = Collections.synchronizedList(new ArrayList<Kolizeum>());
    /**
     * Parameters of a Kolizeum
     **/
    private ArrayList<Player> firstTeam = new ArrayList<Player>(), secondTeam = new ArrayList<Player>();
    private TimerTask timer;
    private boolean started;
    private int everageLevel;
    private boolean couldStart = false;

    public Kolizeum(Player player, boolean isGroup) {
        setEverageLevel(!isGroup ? player.getLevel() : player.getGroup().getGroupLevel());
        setStarted(false);
        getKolizeums().add(this);
    }

    public void check() {
        if (getTeams().size() >= Params.MAX_PLAYERS.intValue() * 2) {
            if (kolizeumReady() && checkPlayerState()) {
                startFight();
                setStarted(true);
            }
        } else if (!couldStart) {
            if (getTeams().size() == 4) {
                if (kolizeumReady() && checkPlayerState()) {
                    couldStart = true;
                    for (Player p : getTeams()) {
                        p.sendText("Kolizeum : Nous avons 4 joueurs, tapez .demarrer si vous souhaitez un Kolizeum 2 vs 2 au lieu de 3 vs 3.");
                    }
                }
            }
        }
    }

    public void verifStartNow() {
        if (couldStart) {
            if (getTeams().size() == 4) {
                int whoWantToStart = 0;
                for (Player p : getTeams()) {
                    if (p.startNow()) {
                        whoWantToStart++;
                    }
                }
                if (whoWantToStart == 4) {
                    if (firstTeam.size() >= 2) {
                        Player sw = firstTeam.get(firstTeam.size() - 1);
                        firstTeam.remove(sw);
                        secondTeam.add(sw);
                    } else {
                        Player sw = secondTeam.get(secondTeam.size() - 1);
                        secondTeam.remove(sw);
                        firstTeam.add(sw);
                    }
                    startFight();
                    setStarted(true);
                    for (Player p : getTeams()) {
                        p.sendMess(Lang.LANG_2);
                    }
                }
            }
        }
    }

    public synchronized boolean checkPlayerState() {
        boolean checked = true;
        for (Player player : getTeams()) {
            if (!player.isReady())
                checked = false;
        }

        if (checked) {
            if (getTimer().cancel())
                getTimer().cancel();
            setTimer(null);

            for (Player player : getTeams()) {
                player.setReady(false);
                player.send("001D");
            }
        }

        return checked;
    }

    public void startStateTimer() {

        Timer timer = new Timer();
        setTimer(new TimerTask() {
            public void run() {
                if (!isStarted() && getTeams().size() >= Params.MAX_PLAYERS.intValue()) {
                    boolean remove = false;
                    for (Player player : getTeams()) {
                        if (!player.isReady()) {
                            unsubscribe(player);
                            remove = true;
                        }
                    }
                    if (remove)
                        for (Player player : getTeams()) {
                            player.sendMess(Lang.LANG_85);
                            player.setReady(false);
                            player.send("001D");
                        }
                    check();
                }
                setTimer(null);
            }
        });
        timer.schedule(getTimer(), Params.BREAK_TIME.intValue());
    }

    public void startFight() {
        teleport();
        final Kolizeum kolizeum = this;
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(getFirstTeam().get(0).getMap(),
                        getFirstTeam().get(0).getGuid(), getSecondTeam().get(0).getGuid());

                SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(getSecondTeam().get(0).getMap(),
                        getFirstTeam().get(0).getGuid(), getSecondTeam().get(0).getGuid());
                getFirstTeam().get(0).getMap().newKolizeum(kolizeum, getFirstTeam(), getSecondTeam());

                for (int i = 1; i < Params.MAX_PLAYERS.intValue() * 2; i++) {
                    try {
                        getFirstTeam().get(0).getFight().joinFight(getFirstTeam().get(i), getFirstTeam().get(0).getGuid());
                        getSecondTeam().get(0).getFight().joinFight(getSecondTeam().get(i), getSecondTeam().get(0).getGuid());
                    } catch (IndexOutOfBoundsException e) {
                    }
                }
            }
        }, 3000);
    }

    public void teleport() {
        /*ArrayList<Integer> allMaps = new ArrayList<Integer>();
        for (String map : Params.MAPS.toString().split(","))
            allMaps.add(Integer.parseInt(map));

        int randomValue = Formulas.getRandomValue(0, allMaps.size() - 1);
        int fightMap = allMaps.get(randomValue);*/

        for (Player player : getTeams()) {
            player.saveLastMap();
            player.teleport((short) 12621, 1);
        }
    }

    public static synchronized void subscribe(Player player, boolean isGroup) {
        Kolizeum kolizeum = search(player, isGroup);
        // Précautions
        player.setWantToStartNow(false);
        if (!isGroup) {
            int maxPlayers = Params.MAX_PLAYERS.intValue();
            if (kolizeum.getFirstTeam().size() < maxPlayers) {
                kolizeum.getFirstTeam().add(player);
            } else {
                kolizeum.getSecondTeam().add(player);
            }
            player.setKolizeum(kolizeum);

        } else {
            boolean inFirstTeam = kolizeum.getFirstTeam().isEmpty();
            for (Player p : player.getGroup().getPlayers()) {
                if (inFirstTeam) {
                    kolizeum.getFirstTeam().add(p);
                } else {
                    kolizeum.getSecondTeam().add(p);
                }
                p.setKolizeum(kolizeum);
            }
        }
        kolizeum.check();
        kolizeum.sendRemainingPlayers();
        Manager.refreshTeamsInfos();
    }

    public static synchronized void unsubscribe(Player player) {
        Kolizeum kolizeum = player.getKolizeum();

        if (kolizeum.getFirstTeam().contains(player)) {
            kolizeum.getFirstTeam().remove(player);
        } else {
            kolizeum.getSecondTeam().remove(player);
        }
        if (kolizeum.isEmpty())
            kolizeum.remove();

        player.setKolizeum(null);
        player.setReady(false);
        Manager.refreshTeamsInfos();
        player.send("001CLEAR");
    }

    public static Kolizeum search(Player player, boolean isGroup) {
        for (Kolizeum kolizeum : getKolizeums())
            if (!kolizeum.isStarted() &&
                    synchronizedLevels(player, kolizeum, isGroup) && kolizeum.hasPlace(isGroup))
                return kolizeum;
        return new Kolizeum(player, isGroup);
    }

    /**
     * If the kolizeum has some place
     **/
    public boolean hasPlace(boolean isGroup) {
        if (isGroup)
            return getFirstTeam().isEmpty() || getSecondTeam().isEmpty();
        else
            return getFirstTeam().size() < Params.MAX_PLAYERS.intValue() || getSecondTeam().size() < Params.MAX_PLAYERS.intValue();
    }

    public static boolean synchronizedLevels(Player player, Kolizeum kolizeum, boolean isGroup) {
        int everageLevel = kolizeum.getEverageLevel();
        int levelToVerify = isGroup ? player.getGroup().getGroupLevel() : player.getLevel();
        int gapLevel = levelToVerify - everageLevel < 0 ? (levelToVerify - everageLevel) * -1 :
                levelToVerify - everageLevel;

        return gapLevel < Params.GAP_LEVEL.intValue();
    }

    public void sendRemainingPlayers() {
        for (Player player : getTeams()) {
            if (isStarted())
                player.sendMess(Lang.LANG_2);
            else
                player.sendMess(Lang.LANG_3, getRemainingPlayers() + " ", "");
        }
    }

    public boolean kolizeumReady() {
        boolean ready = true;

        for (Player player : getTeams()) {
            if (player == null || player.getFight() != null || !player.isOnline()) {
                unsubscribe(player);
                ready = false;
            }
        }
        boolean toReturn = completed() && ready;

        if (toReturn && getTimer() == null) {
            startStateTimer();
            for (Player player : getTeams()) {
                player.send("001P");
            }
            Manager.refreshStateBlock(this);
        }
        return toReturn;
    }

    public boolean completed() {
        return getTeams().size() >= Params.MAX_PLAYERS.intValue();
    }

    public void pause(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getRemainingPlayers() {
        return Params.MAX_PLAYERS.intValue() * 2 - (getFirstTeam().size() + getSecondTeam().size());
    }

    public boolean isEmpty() {
        return getFirstTeam().isEmpty() && getSecondTeam().isEmpty();
    }

    public void remove() {
        getKolizeums().remove(this);
    }

    public ArrayList<Player> getTeams() {
        ArrayList<Player> teams = new ArrayList<Player>();
        teams.addAll(getFirstTeam());
        teams.addAll(getSecondTeam());
        return teams;
    }

    public ArrayList<Player> getSecondTeam() {
        return secondTeam;
    }

    public void setSecondTeam(ArrayList<Player> secondTeam) {
        this.secondTeam = secondTeam;
    }

    public ArrayList<Player> getFirstTeam() {
        return firstTeam;
    }

    public void setFirstTeam(ArrayList<Player> firstTeam) {
        this.firstTeam = firstTeam;
    }

    public int getEverageLevel() {
        return everageLevel;
    }

    public void setEverageLevel(int everageLevel) {
        this.everageLevel = everageLevel;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public static List<Kolizeum> getKolizeums() {
        return kolizeums;
    }

    public static void setKolizeums(List<Kolizeum> kolizeums) {
        Kolizeum.kolizeums = kolizeums;
    }

    public TimerTask getTimer() {
        return timer;
    }

    public void setTimer(TimerTask timer) {
        this.timer = timer;
    }

    public boolean canStart() {
        return couldStart;
    }
}