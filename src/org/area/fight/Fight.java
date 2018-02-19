package org.area.fight;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.area.arena.Arena;
import org.area.arena.Team;
import org.area.client.Player;
import org.area.client.Player.Group;
import org.area.common.Constant;
import org.area.common.CryptManager;
import org.area.common.Formulas;
import org.area.common.IA.IAThread;
import org.area.common.Pathfinding;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.Couple;
import org.area.common.World.Drop;
import org.area.event.Event;
import org.area.event.EventConstant;
import org.area.fight.object.Challenge;
import org.area.fight.object.Collector;
import org.area.fight.object.LaunchedSort;
import org.area.fight.object.Piege;
import org.area.fight.object.Monster.MobGrade;
import org.area.fight.object.Monster.MobGroup;
import org.area.fight.object.Prism;
import org.area.fight.object.Stalk;
import org.area.fight.object.Stalk.Traque;
import org.area.game.GameSendThread;
import org.area.game.GameServer;
import org.area.game.GameThread.GameAction;
import org.area.kernel.Config;
import org.area.kernel.Console;
import org.area.kernel.Logs;
import org.area.kolizeum.Kolizeum;
import org.area.lang.Lang;
import org.area.object.Guild;
import org.area.object.Item;
import org.area.object.Item.ObjTemplate;
import org.area.object.Maps;
import org.area.object.Maps.Case;
import org.area.object.SoulStone;
import org.area.spell.Spell.SortStats;
import org.area.spell.SpellEffect;
import org.area.timers.PeriodicRunnableCancellable;
import org.area.timers.TimerController;


public class Fight {
    private String _defenseurs = "";
    private int _id;
    private boolean _isMob = false;
    private Map<Integer, Fighter> _team0 = new TreeMap<Integer, Fighter>();
    private Map<Integer, Fighter> _team1 = new TreeMap<Integer, Fighter>();
    private Map<Integer, Fighter> deadList = new TreeMap<Integer, Fighter>();
    // @Flow - Pour la laisse spirituelle
    private Fighter lastFighterDieTeam0 = null;
    private Fighter lastFighterDieTeam1 = null;
    // Sort prestige 13 - Dissipation
    public boolean dissipationHasBeenLaunch = false;
    private Map<Integer, Player> _spec = new TreeMap<Integer, Player>();
    private Maps _map;
    Maps _mapOld;
    private Fighter _init0;
    private Fighter _init1;
    private ArrayList<Case> _start0 = new ArrayList<Case>();
    private ArrayList<Case> _start1 = new ArrayList<Case>();
    private int _state = 0;
    private int _guildID = -1;
    private int _type = -1;
    private Kolizeum kolizeum;
    private boolean locked0 = false;
    private boolean onlyGroup0 = false;
    private boolean locked1 = false;
    private boolean onlyGroup1 = false;
    private boolean specOk = true;
    private boolean help1 = false;
    private boolean help2 = false;
    private int _st2;
    private int _st1;
    private int _curPlayer;
    private long _startTime = 0;
    int _curFighterPA;
    int _curFighterPM;
    private int _curFighterUsedPA;
    private int _curFighterUsedPM;
    private String _curAction = "";

    public String get_curAction() {
        return _curAction;
    }

    private List<Fighter> _ordreJeu = new ArrayList<Fighter>();
    private List<Glyphe> _glyphs = new ArrayList<Glyphe>();
    private List<Piege> _traps = new ArrayList<Piege>();
    private MobGroup _mobGroup;
    private Collector collector;
    private Prism prism;
    @SuppressWarnings("unused") // on enlève l'avertissement
    private IAThread _IAThreads = null;  // @Flow - Faut garder ça

    private ArrayList<Fighter> _captureur = new ArrayList<Fighter>(8);    //Création d'une liste de longueur 8. Les combats contiennent un max de 8 Attaquant
    private boolean isCapturable = false;
    private int captWinner = -1;
    private SoulStone pierrePleine;
    private Map<Integer, Challenge> _challenges = new TreeMap<Integer, Challenge>();
    private Map<Integer, Case> _raulebaque = new TreeMap<Integer, Case>();
    private long TimeStartTurn = 0L;

    //Approisement de DD
    private ArrayList<Fighter> _apprivoiseur = new ArrayList<Fighter>(8);
    private boolean CanCaptu = false;
    boolean ThereAreThree = true;
    boolean ThereAreAmandDore = true;
    boolean ThereAreAmandRousse = true;
    boolean ThereAreRousseDore = true;
    boolean ThereIsAmand = true;
    boolean ThereIsDore = true;
    boolean ThereIsRousse = true;

    //Fights
    private boolean fightIsStarted = false;
    //Anti coop/transpo
    private boolean hasUsedCoopTranspo = false;
    //Event
    private Event event;
    private long startRemaining = 0;
    private TimerController timerController;
    private ScheduledFuture<?> timerTask;

    // Guildes
    private ArrayList<Fighter> defenseursGuilde = new ArrayList<Fighter>();

    //Temporisation des Actions
    //Variables
    private int _spellCastDelay;
    // Formules xp
    public int _lvlWinners = 0;
    public int _lvlMax = 0;

    public void addSpellCastDelay(int delay) {
        _spellCastDelay += delay;
    }

    public synchronized void scheduleTimer(int time, final boolean isTurn) {
        final Fight fight = this;
        final int FighterID = _curPlayer;
        this.timerTask = GameServer.fightExecutor.schedule(new Runnable() { // F.
            public void run() {
                if (!isTurn) {
                    if (prism != null)
                        prism.removeTurnTimer(1000);
                    else if (collector != null)
                        collector.removeTurnTimer(1000);

                    if (!fightIsStarted)
                        fight.startFight();

                    if (prism != null)
                        prism.setTurnTime(60000);
                    else if (collector != null)
                        collector.setTurnTime(60000);
                } else if (FighterID == _curPlayer) {
                    fight.endTurn();
                }
            }
        }, time, TimeUnit.SECONDS);
        setStartRemaining(System.currentTimeMillis());
    }

    public long getRemaimingTime() {
        long toReturn = 0;
        if (this.collector != null || this.prism != null)
            toReturn = 60000 - (System.currentTimeMillis() - getStartRemaining());
        else
            toReturn = 45000 - (System.currentTimeMillis() - getStartRemaining());
        return toReturn;
    }

    public synchronized void cancelTask() {
        if (this.timerTask != null && !this.timerTask.isCancelled()) {
            this.timerTask.cancel(true);
        }
        this.timerTask = null;
    }

    public Fight getFight() {
        return this;
    }

    public Fight(int id, Maps Carte, Player perso, Prism Prisme) {

        Prisme.setInFight(0);
        Prisme.setFightID(id);
        _type = Constant.FIGHT_TYPE_CONQUETE; // (0: Desafio) (4: Pvm) (1:PVP) 5:Perco
        _id = id;
        _map = Carte.getMapCopy();
        _mapOld = Carte;
        _init0 = new Fighter(this, perso);
        prism = Prisme;
        _team0.put(perso.getGuid(), _init0);
        Fighter lPrisme = new Fighter(this, Prisme);
        _team1.put(-1, lPrisme);
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 60000, _type);
        SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
        scheduleTimer(60, false);
        Random teams = new Random();
        if (teams.nextBoolean()) {
            _start0 = parsePlaces(0);
            _start1 = parsePlaces(1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
            _st1 = 0;
            _st2 = 1;
        } else {
            _start0 = parsePlaces(1);
            _start1 = parsePlaces(0);
            _st1 = 1;
            _st2 = 0;
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
        }
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + ","
                + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + ","
                + Constant.ETAT_PORTEUR + ",0");
        List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
        e.addAll(_team1.entrySet());
        for (Entry<Integer, Fighter> entry : e) {
            Fighter f = entry.getValue();
            Case cell = getRandomCell(_start1);
            if (cell == null) {
                _team1.remove(f.getGUID());
                continue;
            }
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + ","
                    + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTEUR
                    + ",0");
            f.set_fightCell(cell);
            f.get_fightCell().addFighter(f);
            f.setTeam(1);
            f.fullPDV();
        }
        _init0.set_fightCell(getRandomCell(_start0));
        _init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().getGuid());
        _init0.get_fightCell().addFighter(_init0);
        _init0.getPersonnage().set_fight(this);
        _init0.setTeam(0);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID());
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), Prisme.getID());
        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), 0, _init0.getGUID(), Prisme.getID(),
                _init0.getPersonnage().get_curCell().getID(), "0;" + _init0.getPersonnage().get_align(), Prisme.getCell(),
                "0;" + Prisme.getalignement());
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID(), _init0);
        for (Fighter f : _team1.values()) {
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), Prisme.getID(), f);
        }
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);
        set_state(Constant.FIGHT_STATE_PLACE);

        String str = "";
        if (prism != null)
            str = Prisme.getCarte() + "|" + Prisme.getX() + "|" + Prisme.getY();
        for (Player z : World.getOnlinePlayers()) {
            if (z == null)
                continue;
            if (z.get_align() != Prisme.getalignement())
                continue;
            SocketManager.SEND_CA_ATTAQUE_MESSAGE_PRISME(z, str);
        }
    }

    public Fight(int type, int id, Maps map, Player init1, Player init2) // PvP
    {
        _type = type; //0: Défie (4: Pvm) 1:PVP (5:Perco)
        _id = id;
        _map = map.getMapCopy();
        _mapOld = map;
        _init0 = new Fighter(this, init1);
        _init1 = new Fighter(this, init2);
        _team0.put(init1.getGuid(), _init0);
        _team1.put(init2.getGuid(), _init1);
        //on desactive le timer de regen coté org.area.client
        SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
        SocketManager.GAME_SEND_ILF_PACKET(init2, 0);

        int cancelBtn = _type == Constant.FIGHT_TYPE_CHALLENGE ? 1 : 0;
        long time = _type == Constant.FIGHT_TYPE_CHALLENGE ? 0 : Config.FIGHT_START_TIME;
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 7, 2, cancelBtn, 1, 0, time, _type);
        if (time != 0) {
            scheduleTimer((int) time / 1000, false);     //Thread Timer
        }
        Random teams = new Random();
        if (teams.nextBoolean()) {
            _start0 = parsePlaces(0);
            _start1 = parsePlaces(1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, _map.get_placesStr(), 1);
            _st1 = 0;
            _st2 = 1;
        } else {
            _start0 = parsePlaces(1);
            _start1 = parsePlaces(0);
            _st1 = 1;
            _st2 = 0;
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, _map.get_placesStr(), 0);
        }
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.getGuid() + "", init1.getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.getGuid() + "", init1.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.getGuid() + "", init2.getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.getGuid() + "", init2.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");

        _init0.set_fightCell(getRandomCell(_start0));
        _init1.set_fightCell(getRandomCell(_start1));

        _init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
        _init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());

        _init0.get_fightCell().addFighter(_init0);
        _init1.get_fightCell().addFighter(_init1);
        _init0.getPersonnage().set_fight(this);
        _init0.setTeam(0);
        _init1.getPersonnage().set_fight(this);
        _init1.setTeam(1);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID());
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().getMap(), _init1.getGUID());
        if (_type == 1) {
            SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), 0, _init0.getGUID(), _init1.getGUID(), _init0.getPersonnage().get_curCell().getID(), "0;" + _init0.getPersonnage().get_align(), _init1.getPersonnage().get_curCell().getID(), "0;" + _init1.getPersonnage().get_align());
        } else {
            SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), 0, _init0.getGUID(), _init1.getGUID(), _init0.getPersonnage().get_curCell().getID(), "0;-1", _init1.getPersonnage().get_curCell().getID(), "0;-1");
        }
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID(), _init0);
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init1.getGUID(), _init1);

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

        set_state(Constant.FIGHT_STATE_PLACE);
    }

    public Fight(int id, Maps map, Player init1, MobGroup group) //@Flow - Combat Type Pvm
    {
        _mobGroup = group;
        _type = Constant.FIGHT_TYPE_PVM; //(0: Défie) 4: Pvm (1:PVP) (5:Perco)
        _id = id;
        _map = map.getMapCopy();
        _mapOld = map;
        _init0 = new Fighter(this, init1);

        _team0.put(init1.getGuid(), _init0);
        for (Entry<Integer, MobGrade> entry : group.getMobs().entrySet()) {
            entry.getValue().setInFightID(entry.getKey());
            Fighter mob = new Fighter(this, entry.getValue());
            _team1.put(entry.getKey(), mob);
        }
        //on desactive le timer de regen coté org.area.client
        SocketManager.GAME_SEND_ILF_PACKET(init1, 0);

        // on envoie le timer ?
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, Config.FIGHT_START_TIME, _type);
        scheduleTimer(55, false);
        /*Random teams = new Random();
        if(teams.nextBoolean())
		{*/
        _start0 = parsePlaces(0);
        _start1 = parsePlaces(1);
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
        _st1 = 0;
        _st2 = 1;
        /*}else
        {
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),1);
		}*/
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.getGuid() + "", init1.getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.getGuid() + "", init1.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");

        List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
        e.addAll(_team1.entrySet());
        for (Entry<Integer, Fighter> entry : e) {
            Fighter f = entry.getValue();
            Case cell = getRandomCell(_start1);
            if (cell == null) {
                _team1.remove(f.getGUID());
                continue;
            }

            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
            f.set_fightCell(cell);
            f.get_fightCell().addFighter(f);
            f.setTeam(1);
            f.fullPDV();
        }
        _init0.set_fightCell(getRandomCell(_start0));

        _init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().getGuid());

        _init0.get_fightCell().addFighter(_init0);

        _init0.getPersonnage().set_fight(this);
        _init0.setTeam(0);

        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID());
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), group.getID());

        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), 4, _init0.getGUID(), group.getID(), (_init0.getPersonnage().get_curCell().getID() + 1), "0;-1", group.getCellID(), "1;-1");
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID(), _init0);

        for (Fighter f : _team1.values()) {
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), group.getID(), f);
        }

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

        set_state(Constant.FIGHT_STATE_PLACE);
    }

    public Fight(int id, Maps map, Player perso, Collector perco) {
        set_guildID(perco.get_guildID());
        perco.set_inFight((byte) 1);
        perco.set_inFightID((byte) id);
        perco.set_fight(this);

        _type = Constant.FIGHT_TYPE_PVT; //(0: Défie) (4: Pvm) (1:PVP) 5:Perco
        _id = id;
        _map = map.getMapCopy();
        _mapOld = map;
        _init0 = new Fighter(this, perso);
        collector = perco;
        //on desactive le timer de regen coté org.area.client
        SocketManager.GAME_SEND_ILF_PACKET(perso, 0);

        _team0.put(perso.getGuid(), _init0);

        Fighter percoF = new Fighter(this, perco);
        _team1.put(-1, percoF);

        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 60000, _type); //  timer de combat
        scheduleTimer(60, false);     //Thread Timer
        Random teams = new Random();
        if (teams.nextBoolean()) {
            _start0 = parsePlaces(0);
            _start1 = parsePlaces(1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
            _st1 = 0;
            _st2 = 1;
        } else {
            _start0 = parsePlaces(1);
            _start1 = parsePlaces(0);
            _st1 = 1;
            _st2 = 0;
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
        }
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");

        List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
        e.addAll(_team1.entrySet());
        for (Entry<Integer, Fighter> entry : e) {
            Fighter f = entry.getValue();
            Case cell = getRandomCell(_start1);
            if (cell == null) {
                _team1.remove(f.getGUID());
                continue;
            }

            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
            f.set_fightCell(cell);
            f.get_fightCell().addFighter(f);
            f.setTeam(1);
            f.fullPDV();

        }
        _init0.set_fightCell(getRandomCell(_start0));

        _init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().getGuid());

        _init0.get_fightCell().addFighter(_init0);

        _init0.getPersonnage().set_fight(this);
        _init0.setTeam(0);

        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID());
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), perco.getGuid());

        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), 5, _init0.getGUID(), perco.getGuid(), (_init0.getPersonnage().get_curCell().getID() + 1), "0;-1", perco.get_cellID(), "3;-1");
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID(), _init0);

        for (Fighter f : _team1.values()) {
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), perco.getGuid(), f);
        }

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);
        set_state(Constant.FIGHT_STATE_PLACE);

        String str = "A" + collector.get_N1() + "," + collector.get_N2()
                + "|.|" + World.getCarte(collector.get_mapID()).getX()
                + "|" + World.getCarte(collector.get_mapID()).getY();
        //On actualise la guilde+Message d'attaque
        for (Player z : World.getGuild(_guildID).getMembers()) {
            if (z == null) continue;
            if (z.isOnline()) {
                SocketManager.SEND_gA_PERCEPTEUR(z, str);
                /*SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
                Collector.parseAttaque(z, _guildID);
                Collector.parseDefense(z, _guildID);*/
                //z.sendBox("ALERTE", Lang.LANG_114[z.getLang()]);
            }
        }
    }


    public Fight(int id, Maps map, ArrayList<Player> team1, ArrayList<Player> team2, Kolizeum kolizeum) {
        setKolizeum(kolizeum);
        _type = 0;
        _id = id;
        _map = map.getMapCopy();
        _mapOld = map;
        _init0 = new Fighter(this, team1.get(0));
        _init1 = new Fighter(this, team2.get(0));
        _team0.put(team1.get(0).getGuid(), _init0);
        _team1.put(team2.get(0).getGuid(), _init1);
        SocketManager.GAME_SEND_ILF_PACKET(team1.get(0), 0);
        SocketManager.GAME_SEND_ILF_PACKET(team2.get(0), 0);
        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 7, 2, 0, 1, 0, 45000L, -10);// (Skin Epee invisible)
        scheduleTimer(60, false);     //Thread Timer
        _start0 = parsePlaces(0);
        _start1 = parsePlaces(1);
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, _map.get_placesStr(), 1);
        _st1 = 0;
        _st2 = 1;
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team1.get(0).getGuid() + "", team1.get(0).getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team1.get(0).getGuid() + "", team1.get(0).getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team2.get(0).getGuid() + "", team2.get(0).getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team2.get(0).getGuid() + "", team2.get(0).getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
        _init0.set_fightCell(getRandomCell(_start0));
        _init1.set_fightCell(getRandomCell(_start1));
        _init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
        _init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());
        _init0.get_fightCell().addFighter(_init0);
        _init1.get_fightCell().addFighter(_init1);
        _init0.getPersonnage().set_fight(this);
        _init0.setTeam(0);
        _init1.getPersonnage().set_fight(this);
        _init1.setTeam(1);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID());
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().getMap(), _init1.getGUID());
        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), 0, _init0.getGUID(), _init1.getGUID(), _init0.getPersonnage().get_curCell().getID(), "0;-1", _init1.getPersonnage().get_curCell().getID(), "0;-1");
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID(), _init0);
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init1.getGUID(), _init1);

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

        set_state(Constant.FIGHT_STATE_PLACE);
    }


    public Fight(int id, Maps map, MobGroup group, Event event) {
        setEvent(event);
        _mobGroup = group;
        _type = Constant.FIGHT_TYPE_PVM; //(0: Défie) 4: Pvm (1:PVP) (5:Perco)
        _id = id;
        _map = map.getMapCopy();
        _mapOld = map;


        for (Entry<Integer, MobGrade> entry : group.getMobs().entrySet()) {
            entry.getValue().setInFightID(entry.getKey());
            Fighter mob = new Fighter(this, entry.getValue());
            _team1.put(entry.getKey(), mob);
        }

        SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 45000, _type);
        scheduleTimer(60, false);     //Thread Timer
        Random teams = new Random();
        if (teams.nextBoolean()) {
            _start0 = parsePlaces(0);
            _start1 = parsePlaces(1);
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
            _st1 = 0;
            _st2 = 1;
        } else {
            _start0 = parsePlaces(1);
            _start1 = parsePlaces(0);
            _st1 = 1;
            _st2 = 0;
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
        }

        List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
        e.addAll(_team1.entrySet());
        for (Entry<Integer, Fighter> entry : e) {
            Fighter f = entry.getValue();
            Case cell = getRandomCell(_start1);
            if (cell == null) {
                _team1.remove(f.getGUID());
                continue;
            }

            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
            f.set_fightCell(cell);
            f.get_fightCell().addFighter(f);
            f.setTeam(1);
            f.fullPDV();
        }
        SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(EventConstant.MAP_SURVIVANT, 4, 0, group.getID(), (EventConstant.CELL_SURVIVANT + 1), "0;-1", group.getCellID(), "1;-1");
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(EventConstant.MAP_SURVIVANT, 0, null);

        for (Fighter f : _team1.values()) {
            SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(EventConstant.MAP_SURVIVANT, group.getID(), f);
        }

        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

        set_state(Constant.FIGHT_STATE_PLACE);
    }

    public Maps get_map() {
        return _map;
    }

    public boolean isFightStarted() {
        return fightIsStarted;
    }

    public void setFightStarted(boolean fightStarted) {
        fightIsStarted = fightStarted;
    }

    public List<Piege> get_traps() {
        return _traps;
    }

    public List<Glyphe> get_glyphs() {
        return _glyphs;
    }

    private Case getRandomCell(List<Case> cells) {
        Random rand = new Random();
        Case cell;
        if (cells.isEmpty()) return null;
        int limit = 0;
        do {
            int id = rand.nextInt(cells.size());
            cell = cells.get(id);
            limit++;
        } while ((cell == null || !cell.getFighters().isEmpty()) && limit < 80);
        if (limit == 80) {
            if (Config.DEBUG) GameServer.addToLog("Case non trouve dans la liste");
            return null;
        }
        return cell;
    }

    private ArrayList<Case> parsePlaces(int num) {
        return CryptManager.parseStartCell(_map, num);
    }

    public int nombreDePlace(int equipe) {
        if (equipe == 0) {
            return _start0.size();
        } else {
            return _start1.size();
        }
    }

    public int get_id() {
        return _id;
    }

    public ArrayList<Fighter> getFighters(int teams)//teams entre 0 et 7, binaire([spec][t2][t1]);
    {
        ArrayList<Fighter> fighters = new ArrayList<Fighter>();

        if (teams - 4 >= 0) {
            for (Entry<Integer, Player> entry : _spec.entrySet()) {
                fighters.add(new Fighter(this, entry.getValue()));
            }
            teams -= 4;
        }
        if (teams - 2 >= 0) {
            for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
                fighters.add(entry.getValue());
            }
            teams -= 2;
        }
        if (teams - 1 >= 0) {
            for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
                fighters.add(entry.getValue());
            }
        }
        return fighters;
    }

    public synchronized void changePlace(Player perso, int cell) {
        Fighter fighter = getFighterByPerso(perso);
        int team = getTeamID(perso.getGuid()) - 1;
        if (fighter == null) return;
        if (_map.getCase(cell) == null || _map.getCase(cell).isWalkable(true) == false) return;
        if (get_state() != 2 || isOccuped(cell) || perso.is_ready() || (team == 0 && !groupCellContains(_start0, cell)) || (team == 1 && !groupCellContains(_start1, cell)))
            return;

        fighter.get_fightCell().getFighters().clear();
        fighter.set_fightCell(_map.getCase(cell));

        _map.getCase(cell).addFighter(fighter);
        SocketManager.GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(this, 3, _map, perso.getGuid(), cell);
    }

    public boolean isOccuped(int cell) {
        if (_map.getCase(cell) == null) return true;
        return _map.getCase(cell).getFighters().size() > 0;
    }

    private boolean groupCellContains(ArrayList<Case> cells, int cell) {
        for (int a = 0; a < cells.size(); a++) {
            if (cells.get(a).getID() == cell)
                return true;
        }
        return false;
    }

    public void verifIfAllReady() {
        boolean val = true;
        for (int a = 0; a < _team0.size(); a++) {
            if (!_team0.get(_team0.keySet().toArray()[a]).getPersonnage().is_ready())
                val = false;
        }
        if (_type != Constant.FIGHT_TYPE_PVM && _type != Constant.FIGHT_TYPE_PVT && _type != Constant.FIGHT_TYPE_CONQUETE) {
            for (int a = 0; a < _team1.size(); a++) {
                if (!_team1.get(_team1.keySet().toArray()[a]).getPersonnage().is_ready())
                    val = false;
            }
        }
        if (_type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_CONQUETE)
            val = false;//Evite de lancer le combat trop vite
        if (getEvent() != null && getEvent().getEventSurvivor() != null)
            val = false;
        if (val) {
            startFight();
        }
    }

    private void startFight() {
        if (_state >= Constant.FIGHT_STATE_ACTIVE)
            return;
        if (getEvent() != null && getEvent().getEventSurvivor() != null) {
            for (Player player : getEvent().getPlayers()) {
                if (player.getFight() == null || player.getFight().get_id() != this.get_id()) {
                    getEvent().removePlayer(player);
                    SocketManager.GAME_SEND_POPUP(player, "Vous avez été exclus de l'event car vous n'êtes pas entré dans le combat !");
                }
            }
        }
        if (_type == Constant.FIGHT_TYPE_PVT) {
            collector.set_inFight((byte) 2);
            //On actualise la guilde+Message d'attaque
            String packet = Collector.parsetoGuild(_guildID);
            for (Player z : World.getGuild(_guildID).getMembers()) {
                if (z == null) continue;
                if (z.isOnline()) {
                    SocketManager.GAME_SEND_gITM_PACKET(z, packet);
                    Collector.parseAttaque(z, _guildID);
                    Collector.parseDefense(z, _guildID);
                    //z.sendBox("ALERTE", Lang.LANG_120[z.getLang()]);
                    //SocketManager.SEND_gA_PERCEPTEUR(z, str);
                }
            }
        }

        _state = Constant.FIGHT_STATE_ACTIVE;
        //Pour le sort corruption
        // @Poupou et perdition(sort mob 2.7)
        // @Flow - Attaquant guilde fix
        for (Fighter f : getFighters(3)) {
            Player player = f.getPersonnage();
            if (f == null || player == null) continue;
            f.setSpellStats();
            f.getPersonnage().sendLimitationIm();
            if (!f.getPersonnage().hasSpell(59)) continue;
            f.addLaunchedFakeSort(null, f.getPersonnage().getSortStatBySortIfHas(59), 3);
        }

        if (_type == Constant.FIGHT_TYPE_PVM) {
            // Formules xp pvm - variables
            for (Fighter entry : _team0.values()) {
                _lvlWinners += entry.get_lvl();
                if (entry.get_lvl() > _lvlMax)
                    _lvlMax = entry.get_lvl();
            }
            for (Fighter f : getAllFighters()) {
                if (f.getType() == 2) { // C'est un mob
                    if (f.getMob().getSpells().containsKey(4102)) {
                        f.addLaunchedFakeSort(null, f.getMob().getSpells().get(4102), 10);
                    }
                }
            }
        }


        set_startTime(System.currentTimeMillis());
        SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(_init0.getPersonnage().getMap(), _init0.getGUID());
        if (_type == Constant.FIGHT_TYPE_PVM) {
            int align = -1;
            if (_team1.size() > 0) {
                _team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
            }
            //Si groupe non fixe
            if (!_mobGroup.isFix() && !_mobGroup.haveSpawnTime())
                World.getCarte(_map.get_id()).spawnGroup(align, 1, true, _mobGroup.getCellID());//Respawn d'un groupe
        }
        if (_type == Constant.FIGHT_TYPE_CONQUETE) {
            prism.setInFight(-2);
            for (Player z : World.getOnlinePlayers()) {
                if (z == null)
                    continue;
                if (z.get_align() == prism.getalignement()) {
                    Prism.parseAttack(z);
                    Prism.parseDefense(z);
                }
            }
        }
        setFightStarted(true);
        GameServer.addToLog(">Un combat vient de débuter");
        SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(this, 7);
        SocketManager.GAME_SEND_GS_PACKET_TO_FIGHT(this, 7);
        InitOrdreJeu();
        _curPlayer = -1;
        SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this, 7);
        SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
        cancelTask();
        if (Config.DEBUG) GameServer.addToLog("Debut du combat");
        for (Fighter F : getFighters(3)) {
            Player perso = F.getPersonnage();
            if (perso == null) continue;
            if (perso.isOnMount())
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_CHEVAUCHANT + ",1");

        }

        try {
            if (this._type == 4) {

                boolean hasMale = false, hasFemale = false;
                boolean hasCawotte = false, hasChafer = false, hasRoulette = false, hasArakne = false;
                boolean hasBoss = false, inDungeon = false;
                for (Fighter f : _team0.values()) {
                    if (f.getPersonnage() != null) {
                        Player perso = f.getPersonnage();
                        if (perso.hasSpell(367))
                            hasCawotte = true;
                        if (perso.hasSpell(373))
                            hasChafer = true;
                        if (perso.hasSpell(101))
                            hasRoulette = true;
                        if (perso.hasSpell(370))
                            hasArakne = true;
                        if (perso.get_sexe() == 0)
                            hasMale = true;
                        if (perso.get_sexe() == 1)
                            hasFemale = true;
                        if (perso.getMap().hasEndFightAction(_type))
                            inDungeon = true;
                    }
                }
                //BR,tournesol affamé, Mob l'éponge, scara doré, bworker, blops royaux, wa wab,
                //rat noir, rat blanc, spincter, skeunk, croca, toror, tot, meulou, DC, CM, AA
                //Ougah, Krala
                String IDisBoss = ";147;799;928;1001;797;478;1184;1185;1186;1187;1188;180;939;940;943;780;854;121;827;232;113;257;173;1159;423;";
                for (Fighter f : _team1.values()) {
                    if (IDisBoss.contains(";" + f.getMob().getTemplate().getID() + ";"))
                        hasBoss = true;
                }

                boolean severalEnnemies, severalAllies, bothSexes, EvenEnnemies, MoreEnnemies;
                severalEnnemies = (_team1.size() < 2 ? false : true);
                severalAllies = (_team0.size() < 2 ? false : true);
                bothSexes = (!hasMale || !hasFemale ? false : true);
                EvenEnnemies = (_team1.size() % 2 == 0 ? true : false);
                MoreEnnemies = (_team1.size() < _team0.size() ? false : true);

                String challenges = World.getChallengeFromConditions(severalEnnemies,
                        severalAllies, bothSexes, EvenEnnemies, MoreEnnemies,
                        hasCawotte, hasChafer, hasRoulette, hasArakne, hasBoss);

                String[] chalInfo;
                int challengeID, challengeXP, challengeDP, bonusGroupe;
                int challengeNumber = (inDungeon ? 2 : 1);

                for (String chalInfos : World.getRandomChallenge(challengeNumber, challenges)) {
                    chalInfo = chalInfos.split(",");
                    challengeID = Integer.parseInt(chalInfo[0]);
                    challengeXP = Integer.parseInt(chalInfo[1]);
                    challengeDP = Integer.parseInt(chalInfo[2]);
                    bonusGroupe = Integer.parseInt(chalInfo[3]);
                    bonusGroupe *= this._team1.size();
                    this._challenges.put(challengeID, new Challenge(this, challengeID, challengeXP + bonusGroupe, challengeDP + bonusGroupe));
                }

                for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                    if (c.getValue() == null)
                        continue;
                    c.getValue().onFight_start();
                    SocketManager.GAME_SEND_PACKET_TO_FIGHT(this, 7, c.getValue().parseToPacket());
                }

            }

        } catch (Exception localException) {
            localException.printStackTrace(System.out);
        }
        /*for(Fighter F : getFighters(3)) // @Flow - État chevauchant, je l'ai pas vu encore #Doublon
        {
			Player perso1 = F.getPersonnage();
			if(perso1 == null)continue;
			if(perso1.isOnMount())
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso1.getGuid()+"", perso1.getGuid()+","+Constant.ETAT_CHEVAUCHANT+",1");

		}
		*/
        try // @Flow - Bien plus propre ainsi
        {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        startTurn();

        for (Fighter F : getFighters(3)) {
            if (F == null)
                continue;
            _raulebaque.put(F.getGUID(), F.get_fightCell());
        }
        /*startTurn();

		for(Fighter F : getFighters(3)){
			if (F == null)
				continue;
			_raulebaque.put(F.getGUID(), F.get_fightCell());
		}*/
    }

    protected synchronized void startTurn() {
        if (!verifyStillInFight())
            verifIfTeamAllDead();

        if (_state >= Constant.FIGHT_STATE_FINISHED) return;
        _curPlayer++;
        _curAction = "";
        if (_ordreJeu == null)
            return;
        int nbVivant = 0;
        for (Fighter F : _ordreJeu) {
            if (F.getPDV() > 0) {
                nbVivant++;
            }
        }
        if (nbVivant == 1) {
            verifIfTeamAllDead();
        }
        if (_curPlayer >= _ordreJeu.size())
            _curPlayer = 0;
        //on reset a chaque debut de tours
        _curFighterPA = _ordreJeu.get(_curPlayer).getPA();
        _curFighterPM = _ordreJeu.get(_curPlayer).getPM();
        _curFighterUsedPA = 0;
        _curFighterUsedPM = 0;
        Fighter curPlayer = _ordreJeu.get(_curPlayer);
        curPlayer.canCac = true;
        if (curPlayer.isDeconnected()) {
            curPlayer.newTurn();
            if (curPlayer.getToursRestants() <= 0) {
                if (curPlayer.getPersonnage() != null) {
                    leftFight(curPlayer.getPersonnage(), null, false);
                    //curPlayer.getPersonnage().DeconnexionCombat();
                } else {
                    onFighterDie(curPlayer, curPlayer);
                    curPlayer.setLeft(true);
                }
            } else {
                SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, new StringBuilder("0162;").append(curPlayer.getPacketsName()).append("~").append(curPlayer.getToursRestants()).toString());
                endTurn();
                return;
            }
        }
        if (_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead())//Si joueur mort
        {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
            endTurn();
            return;
        }

        _ordreJeu.get(_curPlayer).applyBeginningTurnBuff(this);
        if (_state == Constant.FIGHT_STATE_FINISHED) return;
        if (_ordreJeu.get(_curPlayer).getPDV() <= 0) {
            onFighterDie(_ordreJeu.get(_curPlayer), _ordreJeu.get(_curPlayer));
        }

        //On actualise les sorts launch
        _ordreJeu.get(_curPlayer).ActualiseLaunchedSort();
        // Fix arbre sadida renvoi de sort @Flow
        try {
            if (_ordreJeu.get(_curPlayer).getPersonnage().arbreEnModeRenvoi) {
                Fighter arbreToDebuff = _ordreJeu.get(_curPlayer).getPersonnage().arbreEnQuestion;
                arbreToDebuff.deleteBuffByFighter(_ordreJeu.get(_curPlayer));
                _ordreJeu.get(_curPlayer).getPersonnage().arbreEnModeRenvoi = false;
            }
        } catch (Exception e) {
        }
        // Pour les pandas uniquement @Flow
        try {
            if (_ordreJeu.get(_curPlayer).getPersonnage().get_classe() == 12) {
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, _ordreJeu.get(_curPlayer).getGUID());
            }
        } catch (Exception e) {
        }
        //reset des Max des Chatis
        _ordreJeu.get(_curPlayer).get_chatiValue().clear();
        //Gestion des glyphes
        ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
        glyphs.addAll(_glyphs);

        for (Glyphe g : glyphs) {
            if (_state >= Constant.FIGHT_STATE_FINISHED) return;
            //Si c'est ce joueur qui l'a lancÃ©
            if (g.get_caster().getGUID() == _ordreJeu.get(_curPlayer).getGUID()) {
                //on rÃ©duit la durÃ©e restante, et si 0, on supprime
                if (g.decrementDuration() == 0) {
                    _glyphs.remove(g);
                    g.desapear();
                    continue;//Continue pour pas que le joueur active le glyphe s'il Ã©tait dessus
                }
            }
            //Si dans le glyphe
            int dist = Pathfinding.getDistanceBetween(_map, _ordreJeu.get(_curPlayer).get_fightCell().getID(), g.get_cell().getID());
            if (dist <= g.get_size() && g._spell != 476)//476 a effet en fin de tour
            {
                //Alors le joueur est dans le glyphe
                g.onTraped(_ordreJeu.get(_curPlayer));
                if (curPlayer.isDead())//Si joueur mort
                {
                    if (Config.DEBUG)
                        GameServer.addToLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
                    endTurn();
                    return;
                }
            }
        }
        if (_ordreJeu == null) return;
        if (_ordreJeu.size() < _curPlayer) return;
        if (_ordreJeu.get(_curPlayer) == null) return;
        if (_ordreJeu.get(_curPlayer).isDead())//Si joueur mort
        {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
            endTurn();
            return;
        }
        if (_ordreJeu.get(_curPlayer).getPersonnage() != null)
            SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
        if (_ordreJeu.get(_curPlayer).hasBuff(Constant.EFFECT_PASS_TURN))//Si il doit passer son tour
        {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID() + " passe son tour");
            endTurn();
            return;
        }
        if (Config.DEBUG)
            GameServer.addToLog("(" + _curPlayer + ")Debut du tour de Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID());
        if (_ordreJeu.get(_curPlayer).estInvocationControllable()) {
            SocketManager.GAME_SEND_SPELL_LIST(_ordreJeu.get(_curPlayer).getInvocator().getPersonnage(), _ordreJeu.get(_curPlayer).getMob());
        } else if (_ordreJeu.get(_curPlayer).getPersonnage() != null && _ordreJeu.get(_curPlayer).getPersonnage().controleUneInvocation) {
            SocketManager.GAME_SEND_SPELL_LIST(_ordreJeu.get(_curPlayer).getPersonnage());
        }
        SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer).getGUID(), Constant.TIME_BY_TURN, _ordreJeu.get(_curPlayer).estInvocationControllable() ? _ordreJeu.get(_curPlayer).getInvocator().getGUID() : _ordreJeu.get(_curPlayer).getGUID());
        scheduleTimer(55, true);
        GameServer.addToLog("(scheduledTime) startTurn():" + _curPlayer);
        /*}else{
		getTurnTimer().restart();
		try {
			Thread.sleep(650);
		} catch (InterruptedException e1) {e1.printStackTrace();}}*/
        _ordreJeu.get(_curPlayer).setCanPlay(true);


		/*try @Flow - Je crois pas que c'est nécessaire...
		{*/
        if ((this._type == 4) && (this._challenges.size() > 0) && !this._ordreJeu.get(this._curPlayer).isInvocation() && !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()) {
            for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                if (c.getValue() == null)
                    continue;
                c.getValue().onPlayer_startTurn(this._ordreJeu.get(this._curPlayer));
            }
        }

        if (_ordreJeu.get(_curPlayer).getPersonnage() == null || _ordreJeu.get(_curPlayer)._double != null || _ordreJeu.get(_curPlayer).getPerco() != null || (((Fighter) this._ordreJeu.get(this._curPlayer)).getPrisme() != null))//Si ce n'est pas un joueur
        {
            try {
                Thread.sleep(100L);
            } catch (Exception localException4) {
            }
            new IAThread(_ordreJeu.get(_curPlayer), this);
        }
		/*} catch (Exception e) {
			e.printStackTrace(System.out);
		}*/
    }

    @Deprecated
   /* protected synchronized void startTurn2() // Nouveau startTurn() @Flow
    {
        setHasUsedCoopTranspo(false);
        if (Thread.interrupted()) {
            try {
                throw new InterruptedException();
            } catch (InterruptedException ie) {
                boolean havePlayers = false;
                ArrayList<Fighter> fighters = this.getFighters(3);
                for (Fighter f : fighters) {
                    if (f.isDouble() || f.getPersonnage() == null || f.hasLeft()) continue;
                    Player p = f.getPersonnage();
                    if (p == null || p.getAccount() == null || p.getAccount().getGameThread() == null || p.getAccount().getGameThread().getOut() == null)
                        continue;
                    havePlayers = true;
                    break;
                }
                if (!havePlayers) {
                    Logs.addToDebug("Un combat vide trouvé en : " + _map.get_id());
                    _state = Constant.FIGHT_STATE_FINISHED;
                    //on vire les spec du combat
                    for (Player perso : _spec.values()) {
                        //on remet le perso sur la map
                        perso.getMap().addPlayer(perso);
                        //SocketManager.GAME_SEND_GV_PACKET(perso);	//Mauvaise ligne apparemment
                        perso.refreshMapAfterFight();
                    }

                    World.getCarte(_map.get_id()).removeFight(_id);
                    SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
                    _map = null;
                    _ordreJeu = null;
                    _team0.clear();
                    _team1.clear();
                    return;
                }
            }
        }
        if (!verifyStillInFight()) verifIfTeamAllDead();

        if (_state >= Constant.FIGHT_STATE_FINISHED) return;

        final Fight fight = this;
        //GameServer.fightExecutor.schedule(new Runnable() {
        //public void run() {
        _curPlayer++;
        _curAction = "";
        if (_ordreJeu == null) return;
        if (_curPlayer >= _ordreJeu.size()) _curPlayer = 0;

        _curFighterPA = _ordreJeu.get(_curPlayer).getPA();
        _curFighterPM = _ordreJeu.get(_curPlayer).getPM();
        _curFighterUsedPA = 0;
        _curFighterUsedPM = 0;
        setTimeStartTurn(System.currentTimeMillis());
        Fighter curPlayer = _ordreJeu.get(_curPlayer);
        if (curPlayer.isDeconnected()) {
            curPlayer.newTurn();
            if (curPlayer.getToursRestants() <= 0) {
                if (curPlayer.getPersonnage() != null) {
                    leftFight(curPlayer.getPersonnage(), null, false);
                    //curPlayer.getPersonnage().DeconnexionCombat();
                } else {
                    onFighterDie(curPlayer, curPlayer);
                    curPlayer.setLeft(true);
                }
            } else {
                //On envois les IM il reste X tours
                SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(fight, 7, new StringBuilder("0162;").append(curPlayer.getPacketsName()).append("~").append(curPlayer.getToursRestants()).toString());
            }
        }
        if (_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead())//Si joueur mort
        {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
            endTurn();
            return;
        }
        Fighter curFighter = _ordreJeu.get(_curPlayer);

        curFighter.applyBeginningTurnBuff(fight);
        if (_state == Constant.FIGHT_STATE_FINISHED) return;
        if (curFighter.getPDV() <= 0 && !curFighter.isDead()) {
            onFighterDie(_ordreJeu.get(_curPlayer), _ordreJeu.get(_curPlayer));
        }
        //On actualise les sorts launch
        curFighter.ActualiseLaunchedSort();
        //reset des Max des Chatis
        curFighter.get_chatiValue().clear();
        //Gestion des glyphes
        ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
        glyphs.addAll(_glyphs);

        for (Glyphe g : glyphs) {
            if (_state >= Constant.FIGHT_STATE_FINISHED) return;
            if (curFighter.isDead()) break;
            //Si c'est ce joueur qui l'a lancé
            if (g.get_caster().getGUID() == curFighter.getGUID()) {
                //on réduit la durée restante, et si 0, on supprime
                if (g.decrementDuration() == 0) {
                    _glyphs.remove(g);
                    g.desapear();
                    continue;//Continue pour pas que le joueur active le glyphe s'il était dessus
                }
            }
            //Si dans le glyphe
            int dist = Pathfinding.getDistanceBetween(_map, curFighter.get_fightCell().getID(), g.get_cell().getID());
            if (dist <= g.get_size() && g._spell != 476)//476 a effet en fin de tour
            {
                //Alors le joueur est dans le glyphe
                g.onTraped(curFighter);
            }
        }
        if (_ordreJeu == null) return;
        if (_ordreJeu.size() < _curPlayer) {
            _curPlayer = 0;
        }
        if (_ordreJeu.get(_curPlayer) != curFighter || curFighter.isDead())//Si joueur mort
        {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
            endTurn();
            return;
        }
        if (_ordreJeu.get(_curPlayer).getPersonnage() != null) {
            SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
        }
        if (_ordreJeu.get(_curPlayer).hasBuff(Constant.EFFECT_PASS_TURN))//Si il doit passer son tour
        {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID() + " passe son tour");
            endTurn();
            return;
        }
        if (Config.DEBUG)
            GameServer.addToLog("(" + _curPlayer + ")Debut du tour de Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID());
        SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(fight, 7, _ordreJeu.get(_curPlayer).getGUID(), Constant.TIME_BY_TURN, _ordreJeu.get(_curPlayer).getGUID());
        scheduleTimer(60, true);
        verifIfTeamAllDead();

        if (_ordreJeu == null) return;
        _ordreJeu.get(_curPlayer).setCanPlay(true);

        if (_ordreJeu.get(_curPlayer).getPersonnage() == null || _ordreJeu.get(_curPlayer)._double != null || _ordreJeu.get(_curPlayer).getPerco() != null || (((Fighter) this._ordreJeu.get(this._curPlayer)).getPrisme() != null))//Si ce n'est pas un joueur
        {
            try {
                Thread.sleep(100L);
            } catch (Exception localException4) {
            }
            new IAThread(_ordreJeu.get(_curPlayer), this);
        }
        try {
            if ((fight._type == 4) && (fight._challenges.size() > 0) && !fight._ordreJeu.get(fight._curPlayer).isInvocation() && !fight._ordreJeu.get(fight._curPlayer).isDouble() && !fight._ordreJeu.get(fight._curPlayer).isPerco()) {
                for (Entry<Integer, Challenge> c : fight._challenges.entrySet()) {
                    if (c.getValue() == null)
                        continue;
                    c.getValue().onPlayer_startTurn(fight._ordreJeu.get(fight._curPlayer));
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    //}, 500, TimeUnit.MILLISECONDS);

    //}*/

    public synchronized void endTurn() // Révision de la fonction endTurn() @Flow
    {
        try {
            boolean bug = false;
            if (_state >= Constant.FIGHT_STATE_FINISHED) return;
            if (_curPlayer == -1) {
                bug = true;
                _curPlayer = _ordreJeu.size() - 1;
            }
            if (_curPlayer >= _ordreJeu.size()) {
                _curPlayer = _ordreJeu.size() - 1;
            }
            if (_ordreJeu.get(_curPlayer) == null) {
                boolean noplayer = true;
                List<Fighter> n_list = new ArrayList<Fighter>();
                for (Fighter f : _ordreJeu) {
                    if (f == null) continue;
                    if (f.getPersonnage() != null && !f.isDouble()) noplayer = false;
                    n_list.add(f);
                }
                if (noplayer) return;
                _ordreJeu = n_list;
                bug = true;
                _curPlayer = _ordreJeu.size() - 1;
                startTurn();
                return;
            }
            if (bug) {
                endTurn();
                return;
            }
            if (_ordreJeu == null)
                return;//Je veux bien être magicien et réparer les combats, mais faut pas abuser non plus >.>
            if (_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead()) {
                startTurn();
                return;
            }
            cancelTask();
            if (_ordreJeu == null) return; //Si l'ordre de jeu est null .
            _ordreJeu.get(_curPlayer).setCanPlay(false);
            final Fight fight = this;
            new PeriodicRunnableCancellable(250, TimeUnit.MILLISECONDS) {
                public void run() {
                    if (_curAction.isEmpty()) {
                        SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(fight, 7, _ordreJeu.get(_curPlayer).getGUID());
                        for (SpellEffect SE : _ordreJeu.get(_curPlayer).getBuffsByEffectID(131)) {
                            int pas = SE.getValue();
                            int val = -1;
                            try {
                                val = Integer.parseInt(SE.getArgs().split(";")[1]);
                            } catch (Exception e) {
                            }
                            ;
                            if (val == -1) continue;

                            int nbr = (int) Math.floor((double) _curFighterUsedPA / (double) pas);
                            int dgt = val * nbr;
                            //Si poison paralysant
                            if (SE.getSpell() == 200) {
                                int inte = SE.getCaster().getTotalStats().getEffect(Constant.STATS_ADD_INTE);
                                if (inte < 0) inte = 0;
                                int pdom = SE.getCaster().getTotalStats().getEffect(Constant.STATS_ADD_PERDOM);
                                if (pdom < 0) pdom = 0;
                                //on applique le boost
                                dgt = (int) (((100 + inte + pdom) / 100) * dgt);
                            }
                            if (_ordreJeu.get(_curPlayer).hasBuff(184)) {
                                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, _ordreJeu.get(_curPlayer).getGUID() + "", _ordreJeu.get(_curPlayer).getGUID() + "," + _ordreJeu.get(_curPlayer).getBuff(184).getValue());
                                dgt = dgt - _ordreJeu.get(_curPlayer).getBuff(184).getValue();//Réduction physique
                            }
                            if (_ordreJeu.get(_curPlayer).hasBuff(105)) {
                                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, _ordreJeu.get(_curPlayer).getGUID() + "", _ordreJeu.get(_curPlayer).getGUID() + "," + _ordreJeu.get(_curPlayer).getBuff(105).getValue());
                                dgt = dgt - _ordreJeu.get(_curPlayer).getBuff(105).getValue();//Immu
                            }
                            if (dgt <= 0) continue;

                            if (dgt > _ordreJeu.get(_curPlayer).getPDV())
                                dgt = _ordreJeu.get(_curPlayer).getPDV();//va mourrir
                            _ordreJeu.get(_curPlayer).removePDV(dgt);
                            dgt = -(dgt);
                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, SE.getCaster().getGUID() + "", _ordreJeu.get(_curPlayer).getGUID() + "," + dgt);

                        }
                        ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
                        glyphs.addAll(_glyphs);
                        for (Glyphe g : glyphs) {
                            if (_state >= Constant.FIGHT_STATE_FINISHED) return;
                            //Si dans le glyphe
                            int dist = Pathfinding.getDistanceBetween(_map, _ordreJeu.get(_curPlayer).get_fightCell().getID(), g.get_cell().getID());
                            if (dist <= g.get_size() && g._spell == 476)//476 a effet en fin de tour
                            {
                                //Alors le joueur est dans le glyphe
                                g.onTraped(_ordreJeu.get(_curPlayer));
                            }
                        }
                        if (_ordreJeu.get(_curPlayer).getPDV() <= 0) {
                            onFighterDie(_ordreJeu.get(_curPlayer), _ordreJeu.get(_curPlayer));
                        }

                        if ((fight._type == 4) && (fight._challenges.size() > 0) && !fight._ordreJeu.get(fight._curPlayer).isInvocation() && !fight._ordreJeu.get(fight._curPlayer).isDouble() && !fight._ordreJeu.get(fight._curPlayer).isPerco() && (fight._ordreJeu.get(fight._curPlayer).getTeam() == 0)) {
                            for (Entry<Integer, Challenge> c : fight._challenges.entrySet()) {
                                if (c.getValue() == null)
                                    continue;
                                c.getValue().onPlayer_endTurn(fight._ordreJeu.get(fight._curPlayer));
                            }
                        }

                        //reset des valeurs
                        _curFighterUsedPA = 0;
                        _curFighterUsedPM = 0;
                        _curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PA);
                        _curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PM);
                        _ordreJeu.get(_curPlayer).refreshfightBuff();
                        if (_ordreJeu.get(_curPlayer).getPersonnage() != null)
                            if (_ordreJeu.get(_curPlayer).getPersonnage().isOnline())
                                SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());

                        if (_ordreJeu == null) {
                            this.cancel();
                            return;
                        }
                        if (_curPlayer >= _ordreJeu.size()) _curPlayer = 0;
                        SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(fight, 7);
                        SocketManager.GAME_SEND_GTR_PACKET_TO_FIGHT(fight, 7, _ordreJeu.get(_curPlayer == _ordreJeu.size() ? 0 : _curPlayer).getGUID());
                        if (Config.DEBUG)
                            GameServer.addToLog("(" + _curPlayer + ")Fin du tour de Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID());
                        startTurn();
                        this.cancel();
                    }
                }
            };
        } catch (NullPointerException e) {
            e.printStackTrace();
            endTurn();
        }
    }

    private void InitOrdreJeu() {
        Fighter curMax = null;
        boolean team1_ready = false;
        boolean team2_ready = false;
        ArrayList<Fighter> fightTeam1 = new ArrayList<Fighter>();
        ArrayList<Fighter> fightTeam2 = new ArrayList<Fighter>();
        int size = 0;
        int y1 = 0;
        int y2 = 0;
        boolean maxTeam1 = false;
        boolean maxTeam2 = false;
        int aleatoire = 0;

        if (!team1_ready) {
            team1_ready = true;
            for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
                if (_ordreJeu.contains(entry.getValue())) continue;
                team1_ready = false;

                fightTeam1.add(0, entry.getValue());
            }
        }

        if (!team2_ready) {
            team2_ready = true;
            for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
                if (_ordreJeu.contains(entry.getValue())) continue;
                team2_ready = false;

                fightTeam2.add(0, entry.getValue());
            }
        }
        if (fightTeam2.get(fightTeam2.size() - 1).getInitiative() == fightTeam1.get(fightTeam1.size() - 1).getInitiative()) {
            aleatoire = Formulas.getRandomValue(1, 2);
        }
        if (fightTeam2.get(fightTeam2.size() - 1).getInitiative() > fightTeam1.get(fightTeam1.size() - 1).getInitiative() || aleatoire == 2) {
            ArrayList<Fighter> inverseArray = fightTeam1;
            fightTeam1 = fightTeam2;
            fightTeam2 = inverseArray;
        }

        y1 = fightTeam1.size() - 1;
        y2 = fightTeam2.size() - 1;

        if (fightTeam1.size() >= fightTeam2.size())
            size = fightTeam1.size();
        else
            size = fightTeam2.size();

        ArrayList<Fighter> sortByIni = new ArrayList<Fighter>();
        for (int i = 0; i < y1 + 1; i++) {
            int maxIni = 100000000;
            Fighter curFight = null;
            int indexRemove = 0;

            for (int y = 0; y < fightTeam1.size(); y++) {
                if (fightTeam1.get(y).getInitiative() <= maxIni) {
                    maxIni = fightTeam1.get(y).getInitiative();
                    curFight = fightTeam1.get(y);
                    indexRemove = y;
                }
            }

            sortByIni.add(curFight);
            fightTeam1.remove(indexRemove);
        }

        fightTeam1.clear();
        fightTeam1.addAll(sortByIni);
        sortByIni.clear();

        for (int i = 0; i < y2 + 1; i++) {
            int maxIni = 100000000;
            Fighter curFight = null;
            int indexRemove = 0;

            for (int y = 0; y < fightTeam2.size(); y++) {
                if (fightTeam2.get(y).getInitiative() <= maxIni) {
                    maxIni = fightTeam2.get(y).getInitiative();
                    curFight = fightTeam2.get(y);
                    indexRemove = y;
                }
            }

            sortByIni.add(curFight);
            fightTeam2.remove(indexRemove);
        }

        fightTeam2.clear();
        fightTeam2.addAll(sortByIni);

        for (int i = 0; i < size; i++) {
            if (!maxTeam1) {
                curMax = fightTeam1.get(y1);
                if (i == fightTeam1.size() - 1) {
                    maxTeam1 = true;
                }
                if (curMax != null)
                    _ordreJeu.add(curMax);
                curMax = null;
            }

            if (!maxTeam2) {
                curMax = fightTeam2.get(y2);

                if (i == fightTeam2.size() - 1) {
                    maxTeam2 = true;
                }

                if (curMax != null)
                    _ordreJeu.add(curMax);
                curMax = null;
            }

            y1--;
            y2--;
        }
    }

    public void joinEvent(Player perso) {
        if (perso.getFight() != null) {
            perso.sendText("Il est impossible de rejoindre un combat si vous êtes deja dans un combat");
            return;
        }
        if (_state == Constant.FIGHT_STATE_ACTIVE || _state == Constant.FIGHT_STATE_FINISHED) {
            perso.getAccount().getGameThread().kick();
            return;
        }
        if (_init0 == null)
            _init0 = new Fighter(this, perso);
        Fighter current_Join = null;
        Case cell = getRandomCell(_start0);
        if (cell == null) return;

        if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
            if (perso.getArena() != -1 || perso.getKolizeum() != null || (perso.getEvent() != null && perso.getEvent().getEventSurvivor() != null))
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
            else
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, getRemaimingTime(), _type);
        } else {
            SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
        }
        SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), _map.get_placesStr(), _st1);
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTE + ",0");
        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());

        Fighter f = new Fighter(this, perso);
        current_Join = f;
        f.setTeam(0);
        _team0.put(perso.getGuid(), f);
        perso.set_fight(this);
        f.set_fightCell(cell);
        f.get_fightCell().addFighter(f);
        //Désactive le timer de regen
        SocketManager.GAME_SEND_ILF_PACKET(perso, 0);

        perso.get_curCell().removePlayer(perso.getGuid());
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.getMap(), (current_Join.getTeam() == 0 ? _init0 : _init1).getGUID(), current_Join);
        SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, current_Join);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, perso);
    }

    public boolean joinFight(Player perso, int guid) {
        boolean aRejoinsLeCombat = false;
        if (_state == Constant.FIGHT_STATE_ACTIVE || _state == Constant.FIGHT_STATE_FINISHED) {
            perso.getAccount().getGameThread().kick();
            return false;
        }
        Fighter current_Join = null;
        if (_team0.containsKey(guid)) {
            Case cell = getRandomCell(_start0);
            if (cell == null) {
                return false;
            }

            if (onlyGroup0) {
                Group g = _init0.getPersonnage().getGroup();
                if (g != null) {
                    if (!g.getPlayers().contains(perso)) {
                        SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                        return false;
                    }
                }
            }
            if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE) {
                if (perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                    return false;
                }
                if (_init0.getPersonnage().get_align() != perso.get_align()) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                    return false;
                }
            }
            if (_guildID > -1 && perso.get_guild() != null) {
                if (get_guildID() == perso.get_guild().get_id()) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                    return false;
                }
            }
            if (locked0) {
                SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                return false;
            }
            if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
                if (perso.getArena() != -1 || (perso.getKolizeum() != null && perso.getKolizeum().isStarted()))
                    SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
                else
                    SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, getRemaimingTime(), _type);
            } else {
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
            }
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), _map.get_placesStr(), _st1);
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());

            Fighter f = new Fighter(this, perso);
            current_Join = f;
            f.setTeam(0);
            _team0.put(perso.getGuid(), f);
            // Pour la faille de fin de combat
            while (!perso.estBloqueCombat()) {
                perso.mettreCombatBloque(true);
            }
            perso.set_fight(this);
            f.set_fightCell(cell);
            f.get_fightCell().addFighter(f);
            aRejoinsLeCombat = true;
            //Désactive le timer de regen
            SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
        } else if (_team1.containsKey(guid)) {
            Case cell = getRandomCell(_start1);
            if (cell == null) {
                perso.sendText("Grosse merde #2");
                return false;
            }

            if (onlyGroup1) {
                Group g = _init1.getPersonnage().getGroup();
                if (g != null) {
                    if (!g.getPlayers().contains(perso)) {
                        SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                        return false;
                    }
                }
            }
            if (_type == Constant.FIGHT_TYPE_AGRESSION) {
                if (perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                    return false;
                }
                if (_init1.getPersonnage().get_align() != perso.get_align()) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                    return false;
                }
            }
            if (_type == Constant.FIGHT_TYPE_CONQUETE) {
                if (perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'a', guid);
                    return false;
                }
                if (_init1.getPrisme().getalignement() != perso.get_align()) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'a', guid);
                    return false;
                }
                perso.toggleWings('+');
            }
            if (_guildID > -1 && perso.get_guild() != null) {
                if (get_guildID() == perso.get_guild().get_id()) {
                    SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                    return false;
                }
            }
            if (locked1) {
                SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.getAccount().getGameThread().getOut(), 'f', guid);
                return false;
            }
            if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
                if (perso.getArena() != -1 || (perso.getKolizeum() != null && perso.getKolizeum().isStarted()))
                    SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
                else
                    SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, getRemaimingTime(), _type);
            } else {
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
            }
            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), _map.get_placesStr(), _st2);
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
            Fighter f = new Fighter(this, perso);
            current_Join = f;
            f.setTeam(1);
            _team1.put(perso.getGuid(), f);
            perso.set_fight(this);
            f.set_fightCell(cell);
            f.get_fightCell().addFighter(f);
            aRejoinsLeCombat = true;
        }
        perso.get_curCell().removePlayer(perso.getGuid());
        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.getMap(), (current_Join.getTeam() == 0 ? _init0 : _init1).getGUID(), current_Join);
        SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, current_Join);
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, perso);
        if (collector != null) {
            for (Player z : World.getGuild(_guildID).getMembers()) {
                if (z.isOnline()) {
                    Collector.parseAttaque(z, _guildID);
                    Collector.parseDefense(z, _guildID);
                }
            }
        }
        if (this.prism != null) {
            for (Player z : World.getOnlinePlayers()) {
                if (z == null)
                    continue;
                if (z.get_align() != prism.getalignement())
                    continue;
                Prism.parseAttack(perso);
            }
        }
        return aRejoinsLeCombat;
    }

    public void joinPrismeFight(final Player perso, int id, final int PrismeID) {
        final Fight fight = this;
        GameServer.fightExecutor.schedule(new Runnable() {
            public void run() {
                Fighter current_Join = null;
                Case cell = getRandomCell(_start1);
                if (cell == null)
                    return;
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
                SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), _map.get_placesStr(), _st2);
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 3, 950, perso.getGuid() + "", perso.getGuid() + ","
                        + Constant.ETAT_PORTE + ",0");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 3, 950, perso.getGuid() + "", perso.getGuid() + ","
                        + Constant.ETAT_PORTEUR + ",0");
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
                Fighter f = new Fighter(fight, perso);
                current_Join = f;
                f.setTeam(1);
                _team1.put(perso.getGuid(), f);
                perso.set_fight(fight);
                f.set_fightCell(cell);
                f.get_fightCell().addFighter(f);
                SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
                perso.get_curCell().removePlayer(perso.getGuid());
                SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.getMap(), PrismeID, current_Join);
                SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(fight, 7, current_Join);
                SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(fight, _map, perso);
            }
        }, 700, TimeUnit.MILLISECONDS);
    }

    public void joinPercepteurFight(final Player perso, int guid, final int percoID, final Collector perco) {
        final Fight fight = this;
        GameServer.fightExecutor.schedule(new Runnable() {
            public void run() {
                Fighter current_Join = null;
                Case cell = getRandomCell(_start1);
                if (cell == null) return;
                SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
                SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), _map.get_placesStr(), _st2);
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTE + ",0");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
                Fighter f = new Fighter(fight, perso);
                current_Join = f;
                f.setTeam(1);
                get_team1().put(perso.getGuid(), f);
                perso.set_fight(fight);
                f.set_fightCell(cell);
                f.get_fightCell().addFighter(f);
                //defenseursGuilde.add(f);
                SocketManager.GAME_SEND_ILF_PACKET(perso, 0);

                perso.get_curCell().removePlayer(perso.getGuid());
                SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.getMap(), percoID, current_Join);
                SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(fight, 7, current_Join);
                SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(fight, _map, perso);
                SocketManager.GAME_SEND_GDF_PACKET_TO_FIGHT(perso, fight.get_map().GetCases().values());

                // Fix "vous quitté la défense"
                SocketManager.GAME_SEND_gITP_PACKET(perso, perco.parseDefenseToGuild());
            }
        }, 250, TimeUnit.MILLISECONDS);
    }

    public void toggleLockTeam(int guid) {
        if (_init0 != null && _init0.getGUID() == guid) {
            locked0 = !locked0;
            if (Config.DEBUG)
                GameServer.addToLog(locked0 ? "L'equipe 1 devient bloquee" : "L'equipe 1 n'est plus bloquee");
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().getMap(), locked0 ? '+' : '-', 'A', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, locked0 ? "095" : "096");
        } else if (_init1 != null && _init1.getGUID() == guid) {
            locked1 = !locked1;
            if (Config.DEBUG)
                GameServer.addToLog(locked1 ? "L'equipe 2 devient bloquee" : "L'equipe 2 n'est plus bloquee");
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().getMap(), locked1 ? '+' : '-', 'A', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, locked1 ? "095" : "096");
        }
    }

    public void toggleOnlyGroup(int guid) {
        if (_init0 != null && _init0.getGUID() == guid) {
            onlyGroup0 = !onlyGroup0;
            if (Config.DEBUG)
                GameServer.addToLog(locked0 ? "L'equipe 1 n'accepte que les membres du groupe" : "L'equipe 1 n'est plus bloquee");
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().getMap(), onlyGroup0 ? '+' : '-', 'P', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, onlyGroup0 ? "093" : "094");
        } else if (_init1 != null && _init1.getGUID() == guid) {
            onlyGroup1 = !onlyGroup1;
            if (Config.DEBUG)
                GameServer.addToLog(locked1 ? "L'equipe 2 n'accepte que les membres du groupe" : "L'equipe 2 n'est plus bloquee");
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().getMap(), onlyGroup1 ? '+' : '-', 'P', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, onlyGroup1 ? "095" : "096");
        }
    }

    public void toggleLockSpec(int guid) {
        if ((_init0 != null && _init0.getGUID() == guid) || (_init1 != null && _init1.getGUID() == guid)) {
            specOk = !specOk;
            if (!specOk) {
                for (Player p : _spec.values()) {
                    if (p == null) continue;
                    SocketManager.GAME_SEND_GV_PACKET(p);
                    p.setSitted(false);
                    p.set_fight(null);
                    p.set_away(false);
                }
                _spec.clear();
            }
            if (Config.DEBUG)
                GameServer.addToLog(specOk ? "Le combat accepte les spectateurs" : "Le combat n'accepte plus les spectateurs");
            if (_init0 != null && _init0.getPersonnage() != null)
                SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().getMap(), specOk ? '+' : '-', 'S', _init0.getGUID());
            if (_init1 != null && _init1.getPersonnage() != null)
                SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().getMap(), specOk ? '+' : '-', 'S', _init1.getGUID());
            SocketManager.GAME_SEND_Im_PACKET_TO_MAP(_map, specOk ? "039" : "040");
        }
    }

    public void toggleHelp(int guid) {
        if (_init0 != null && _init0.getGUID() == guid) {
            help1 = !help1;
            if (Config.DEBUG)
                GameServer.addToLog(help2 ? "L'equipe 1 demande de l'aide" : "L'equipe 1s ne demande plus d'aide");
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().getMap(), locked0 ? '+' : '-', 'H', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, help1 ? "0103" : "0104");
        } else if (_init1 != null && _init1.getGUID() == guid) {
            help2 = !help2;
            if (Config.DEBUG)
                GameServer.addToLog(help2 ? "L'equipe 2 demande de l'aide" : "L'equipe 2 ne demande plus d'aide");
            SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().getMap(), locked1 ? '+' : '-', 'H', guid);
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, help2 ? "0103" : "0104");
        }
    }

    private void set_state(int _state) {
        this._state = _state;
    }

    private void set_guildID(int guildID) {
        this._guildID = guildID;
    }

    public int get_state() {
        return _state;
    }

    public int get_guildID() {
        return _guildID;
    }

    public int get_type() {
        return _type;
    }

    public List<Fighter> get_ordreJeu() {
        return _ordreJeu;
    }

    public Map<Integer, Case> get_raulebaque() {
        return _raulebaque;
    }

    public Map<Integer, Challenge> get_challenges() {
        return this._challenges;
    }

    public synchronized boolean fighterDeplace(Fighter f, GameAction GA) {
        String path = GA._args;
        if (path.equals("")) {
            if (Config.DEBUG) GameServer.addToLog("Echec du deplacement: chemin vide");
            return false;
        }
        if (_ordreJeu.size() <= _curPlayer) return false;
        if (_ordreJeu.get(_curPlayer) == null) return false;

        if (Config.DEBUG)
            GameServer.addToLog("(" + _curPlayer + ")Tentative de deplacement de Fighter ID= " + f.getGUID() + " a partir de la case " + f.get_fightCell().getID());
        if (Config.DEBUG) GameServer.addToLog("Path: " + path);
        if (!_curAction.equals("") || _ordreJeu.get(_curPlayer).getGUID() != f.getGUID() || _state != Constant.FIGHT_STATE_ACTIVE) {
            if (!_curAction.equals(""))
                if (Config.DEBUG) GameServer.addToLog("Echec du deplacement: il y deja une action en cours");
            if (_ordreJeu.get(_curPlayer).getGUID() != f.getGUID())
                if (Config.DEBUG) GameServer.addToLog("Echec du deplacement: ce n'est pas a ce joueur de jouer");
            if (_state != Constant.FIGHT_STATE_ACTIVE)
                if (Config.DEBUG) GameServer.addToLog("Echec du deplacement: le combat n'est pas en cours");
            return false;
        }

        ArrayList<Fighter> tmptacle = Pathfinding.getEnemyFighterArround(f.get_fightCell().getID(), _map, this);
        ArrayList<Fighter> tacle = new ArrayList<Fighter>();
        if (tmptacle != null && !f.isState(6) && !f.isHide())//Tentative de Tacle : Si stabilisation alors pas de tacle possible
        {
            boolean mustTacle = false;
            for (Fighter T : tmptacle)//Les stabilisés ne taclent pas
            {
                if (T.isHide()) continue;
                tacle.add(T);
                if (T.isState(6)) {
                    mustTacle = true;
                }
            }
            if (!tacle.isEmpty())//Si tous les tacleur ne sont pas stabilisés
            {
                if (Config.DEBUG)
                    GameServer.addToLog("Le personnage est a cote de (" + tacle.size() + ") ennemi(s)");// ("+tacle.getPacketsName()+","+tacle.get_fightCell().getID()+") => Tentative de tacle:");
                int chance = Formulas.getTacleChance(f, tacle);
                int rand = Formulas.getRandomValue(0, 99);
                if (rand > chance || mustTacle) {
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "104", _ordreJeu.get(_curPlayer).getGUID() + ";", "");//Joueur taclé
                    int pertePA = _curFighterPA * chance / 100;

                    if (pertePA < 0) pertePA = -pertePA;
                    if (_curFighterPM < 0) _curFighterPM = 0; // -_curFighterPM :: 0 c'est plus simple :)
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "129", f.getGUID() + "", f.getGUID() + ",-" + _curFighterPM);
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "102", f.getGUID() + "", f.getGUID() + ",-" + pertePA);

                    _curFighterPM = 0;
                    _curFighterPA -= pertePA;
                    if (Config.DEBUG) GameServer.addToLog("Echec du deplacement: fighter tacle");
                    return false;
                }
            }
        }

        //*
        AtomicReference<String> pathRef = new AtomicReference<String>(path);
        int nStep = Pathfinding.isValidPath(_map, f.get_fightCell().getID(), pathRef, this);
        String newPath = pathRef.get();

        Player client = f.getPersonnage();
        if (f.estInvocationControllable()) {
            client = f.getInvocator().getPersonnage();
        }
        if (nStep > _curFighterPM || nStep == -1000) {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID() + " a demander un chemin inaccessible ou trop loin");
            if (client != null && client.getAccount() != null && client.getAccount().getGameThread() != null) {
                SocketManager.GAME_SEND_GA_PACKET(client.getAccount().getGameThread().getOut(), "" + 151, "" + f.getGUID(), "-1", "");
            }
            return false;
        }

        _curFighterPM -= nStep;
        _curFighterUsedPM += nStep;

        int nextCellID = CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2));
        //les monstres n'ont pas de GAS//GAF
        if (_ordreJeu.get(_curPlayer).getPersonnage() != null)
            SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer).getGUID());
        //Si le joueur n'est pas invisible
        if (!_ordreJeu.get(_curPlayer).isHide())
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "1", _ordreJeu.get(_curPlayer).getGUID() + "", "a" + CryptManager.cellID_To_Code(f.get_fightCell().getID()) + newPath);
        else//Si le joueur est planqué x)
        {
            if (_ordreJeu.get(_curPlayer).getPersonnage() != null) {
                //On envoie le path qu'au joueur qui se déplace
                GameSendThread out = _ordreJeu.get(_curPlayer).getPersonnage().getAccount().getGameThread().getOut();
                SocketManager.GAME_SEND_GA_PACKET(out, GA._id + "", "1", _ordreJeu.get(_curPlayer).getGUID() + "", "a" + CryptManager.cellID_To_Code(f.get_fightCell().getID()) + newPath);
            }
        }

        //Si porté
        Fighter po = _ordreJeu.get(_curPlayer).get_holdedBy();
        if (po != null
                && _ordreJeu.get(_curPlayer).isState(Constant.ETAT_PORTE)
                && po.isState(Constant.ETAT_PORTEUR)) {

            //si le joueur va bouger
            if (nextCellID != po.get_fightCell().getID()) {
                //on retire les états
                po.setState(Constant.ETAT_PORTEUR, 0);
                _ordreJeu.get(_curPlayer).setState(Constant.ETAT_PORTE, 0);
                //on retire dé lie les 2 fighters
                po.set_isHolding(null);
                _ordreJeu.get(_curPlayer).set_holdedBy(null);
                //La nouvelle case sera définie plus tard dans le code
                //On envoie les packets
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, po.getGUID() + "", po.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, _ordreJeu.get(_curPlayer).getGUID() + "", _ordreJeu.get(_curPlayer).getGUID() + "," + Constant.ETAT_PORTE + ",0");
            }
        }

        _ordreJeu.get(_curPlayer).get_fightCell().getFighters().clear();
        if (Config.DEBUG)
            GameServer.addToLog("(" + _curPlayer + ") Fighter ID= " + f.getGUID() + " se deplace de la case " + _ordreJeu.get(_curPlayer).get_fightCell().getID() + " vers " + CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2)));
        _ordreJeu.get(_curPlayer).set_fightCell(_map.getCase(nextCellID));
        _ordreJeu.get(_curPlayer).get_fightCell().addFighter(_ordreJeu.get(_curPlayer));
        if (po != null) po.get_fightCell().addFighter(po);// même erreur que tantôt, bug ou plus de fighter sur la case
        if (nStep < 0) {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Fighter ID= " + f.getGUID() + " nStep negatives, reconversion");
            nStep = nStep * (-1);
        }
        _curAction = "GA;129;" + _ordreJeu.get(_curPlayer).getGUID() + ";" + _ordreJeu.get(_curPlayer).getGUID() + ",-" + nStep;

        //Si porteur
        po = _ordreJeu.get(_curPlayer).get_isHolding();
        if (po != null
                && _ordreJeu.get(_curPlayer).isState(Constant.ETAT_PORTEUR)
                && po.isState(Constant.ETAT_PORTE)) {
            //on déplace le porté sur la case
            po.set_fightCell(_ordreJeu.get(_curPlayer).get_fightCell());
            if (Config.DEBUG) GameServer.addToLog(po.getPacketsName() + " se deplace vers la case " + nextCellID);
        }
        /*try{
        if (f.getPersonnage() == null) { // @Flow - Arrêt de l'utilisation d'un thread.sleep
        	//final String curAction = _curAction;
            new PeriodicRunnableCancellable (900+150*nStep ,TimeUnit.MILLISECONDS) {
				public void run() {
					SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(getFight(),7,_curAction);
		    		_curAction = "";
		    		ArrayList<Piege> P = new ArrayList<Piege>();
		    		P.addAll(_traps);
		    		for(Piege p : P)
		    		{
		    			Fighter F = _ordreJeu.get(_curPlayer);
		    			int dist = Pathfinding.getDistanceBetween(_map,p.get_cell().getID(),F.get_fightCell().getID());
		    			//on active le piege
		    			if(dist <= p.get_size())p.onTraped(F);
				}
				}
		};
            return true;
        }
        }
        catch(Exception e){}*/

        if (client == null || f.estInvocationControllable()) // Pour les mobs @Flow Modifications nécessaires #Temporisation
        {
            if (client == null) {
                try {
                    Thread.sleep(1000 + 150 * nStep);//Estimation de la durée du déplacement
                } catch (InterruptedException e) {
                }
            }

            SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, _curAction);
            _curAction = "";
            ArrayList<Piege> P = new ArrayList<Piege>();
            P.addAll(_traps);
            for (Piege pi : P) {
                Fighter F = _ordreJeu.get(_curPlayer);
                int dist = Pathfinding.getDistanceBetween(_map, pi.get_cell().getID(), F.get_fightCell().getID());
                //on active le piege
                if (dist <= pi.get_size()) pi.onTraped(F);
            }
            return true;
        }

        client.getAccount().getGameThread().addAction(GA);

        if ((this._type == 4) && (this._challenges.size() > 0) && !this._ordreJeu.get(this._curPlayer).isInvocation() && !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()) {
            for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                if (c.getValue() == null)
                    continue;
                c.getValue().onPlayer_move(f);
            }
        }

        return true;
    }

    public void onGK(Player perso) {
        try {
            if (_curAction.equals("") || _ordreJeu.get(_curPlayer).getGUID() != perso.getGuid() || _state != Constant.FIGHT_STATE_ACTIVE)
                return;
        } catch (Exception e) {
            return;
        }
        if (Config.DEBUG)
            GameServer.addToLog("(" + _curPlayer + ")Fin du deplacement de Fighter ID= " + perso.getGuid());
        SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, _curAction);
        SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 2, _ordreJeu.get(_curPlayer).getGUID());
        //copie
        ArrayList<Piege> P = (new ArrayList<Piege>());
        P.addAll(_traps);
        for (Piege p : P) {
            Fighter F = getFighterByPerso(perso);
            int dist = Pathfinding.getDistanceBetween(_map, p.get_cell().getID(), F.get_fightCell().getID());
            //on active le piege
            if (dist <= p.get_size())
                p.onTraped(F);
            if (_state == Constant.FIGHT_STATE_FINISHED) break;
        }
        _curAction = "";
    }

    public void playerPass(Player _perso) {
        Fighter f = getFighterByPerso(_perso);
        Fighter cur = _perso.getFight().getCurFighter();
        if (cur == null) return;
        if (cur.isInvocation() && cur.getInvocator() == f && cur.estInvocationControllable()) {
            f = _perso.getFight().getCurFighter();
        }
        if (f == null) return;
        if (!f.canPlay()) return;
        if (!_curAction.equals("")) return;
        endTurn();
    }

    public synchronized int tryCastSpell(Fighter fighter, SortStats Spell, int caseID) // @Flow révision
    {
        if (!_curAction.equals(""))
            return 10;
        if (Spell == null)
            return 10;
        //ticMyTimer();
        //if(fighter == null || Spell == null) return 10;
        // @Flow - Fix de j'ai pas de temps à perdre
        Case cellTemp = null;
        for (Fighter f : getAllFighters()) {
            if (f.isDead()) {
                cellTemp = f.get_fightCell();
                if (cellTemp != null) {
                    cellTemp.getFighters().clear();
                }
            }
        }
        for (Fighter f : getAllFighters()) {
            if (!f.isDead() && f.get_holdedBy() == null) { // Les portés sont intouchables
                f.get_fightCell().addFighter(f);
            }
        }
        Case Cell = _map.getCase(caseID);
        _curAction = "casting";
        _spellCastDelay = 80;//Ont ajoutes un delay pour eviter les actions qui finissent trop vite
        if (fighter.getMob() != null && !fighter.estInvocationControllable()) {
            addSpellCastDelay(900);
        }
        if (CanCastSpell(fighter, Spell, Cell, -1) != false) // @Flow, ça foire ici. #Fixé
        {
            if (fighter.getPersonnage() != null) {
                SocketManager.GAME_SEND_STATS_PACKET(fighter.getPersonnage());
            }

            if (Config.DEBUG)
                GameServer.addToLog(fighter.getPacketsName() + " tentative de lancer le sort " + Spell.getSpellID() + " sur la case " + caseID);
            _curFighterPA -= Spell.getPACost(fighter);
            _curFighterUsedPA += Spell.getPACost(fighter);
            SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.getGUID());
            boolean isEc = Spell.getTauxEC() != 0 && Formulas.getRandomValue(1, Spell.getTauxEC()) == Spell.getTauxEC();
            if (isEc) {
                if (Config.DEBUG)
                    GameServer.addToLog(fighter.getPacketsName() + " Echec critique sur le sort " + Spell.getSpellID());
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.getGUID() + "", Spell.getSpellID() + "");
                //Il est ici le problÃ¨me

            } else {

                if ((this._type == Constant.FIGHT_TYPE_PVM) && (this._challenges.size() > 0) && !this._ordreJeu.get(this._curPlayer).isInvocation() && !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()) {
                    for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                        if (c.getValue() == null) continue;
                        c.getValue().onPlayer_action(this._ordreJeu.get(this._curPlayer), Spell.getSpellID());
                        c.getValue().onPlayer_spell(this._ordreJeu.get(this._curPlayer));
                    }
                }

                boolean isCC = fighter.testIfCC(Spell.getTauxCC(fighter));
                String sort = Spell.getSpellID() + "," + caseID + "," + Spell.getSpriteID() + "," + Spell.getLevel() + "," + Spell.getSpriteInfos();
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.getGUID() + "", sort);
                if (isCC) {
                    if (Config.DEBUG)
                        GameServer.addToLog(fighter.getPacketsName() + " Coup critique sur le sort " + Spell.getSpellID());
                    try {
                        Thread.sleep(15);
                    } catch (Exception e) {
                    }
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.getGUID() + "", sort);
                }
                //Si le joueur est invi, on montre la case
                if (fighter.isHide()) {
                    if (Spell.getSpellID() == 0)// Si le coup est Coup de Poing alors on refait apparaitre le personnage
                    {
                        fighter.unHide(caseID);
                    } else {
                        showCaseToAll(fighter.getGUID(), fighter.get_fightCell().getID());
                    }
                }
                //on applique les effets de l'arme, lancement de sort
                Spell.applySpellEffectToFight(this, fighter, Cell, isCC);
            }
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.getGUID() + "", fighter.getGUID() + ",-" + Spell.getPACost(fighter));
            SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
            //Refresh des Stats
            //refreshCurPlayerInfos();
            if (!isEc) fighter.addLaunchedSort(Cell.getFirstFighter(), Spell);
            //fighter.addLaunchedSort(Cell.getFirstFighter(),Spell);
            if (Spell.getSpellID() == 696) // @Flow - Si c'est le sort Chamrak on temporise
            {
                addSpellCastDelay(2000);
            }
            /*try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }*/
            if ((isEc && Spell.isEcEndTurn())) {
                _curAction = "";
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                ;
                if (fighter.getMob() != null || fighter.isInvocation())//Mob, Invoque
                {
                    return 5;
                } else {
                    endTurn();
                    return 5;
                }
            }
            verifIfTeamAllDead();
        } else if (!fighter.estInvocationControllable() && (fighter.getMob() != null || fighter.isInvocation())) {
            return 10;
        }
       try {
            Thread.sleep(_spellCastDelay);
        } catch (InterruptedException e) {
        }
        _curAction = "";
        return 0;
    }

	/*public synchronized int tryCastSpell(Fighter fighter,SortStats Spell, int caseID)
	{
		if(!_curAction.equals(""))return 10;
		if(Spell == null)return 10;

		Case Cell = _map.getCase(caseID);

		if(CanCastSpell(fighter,Spell,Cell, -1))
		{
			_curAction = "casting";
			if(fighter.getPersonnage() != null)
				SocketManager.GAME_SEND_STATS_PACKET(fighter.getPersonnage()); // envoi des stats du lanceur

			if(Config.DEBUG)
				GameServer.addToLog(fighter.getPacketsName()+" tentative de lancer le sort "+Spell.getSpellID()+" sur la case "+caseID);
			_curFighterPA -= Spell.getPACost(fighter);
			_curFighterUsedPA += Spell.getPACost(fighter);
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.getGUID()); // infos concernant la dépense de PA ?
			boolean isEc = Spell.getTauxEC() != 0 && Formulas.getRandomValue(1, Spell.getTauxEC()) == Spell.getTauxEC();
			if(isEc)
			{
				if(Config.DEBUG)
					GameServer.addToLog(fighter.getPacketsName()+" Echec critique sur le sort "+Spell.getSpellID());
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.getGUID()+"", Spell.getSpellID()+""); // envoi de l'EC
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			}else
			{
				try
				{
					if ((this._type == 4) && (this._challenges.size() > 0)
							&& !this._ordreJeu.get(this._curPlayer).isInvocation()
							&& !this._ordreJeu.get(this._curPlayer).isDouble()
							&& !this._ordreJeu.get(this._curPlayer).isPerco())
					{
						for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
							if (c.getValue() == null)
								continue;
							c.getValue().onPlayer_action(this._ordreJeu.get(this._curPlayer), Spell.getSpellID());
							c.getValue().onPlayer_spell(this._ordreJeu.get(this._curPlayer));

						}
					}
				} catch (Exception e) {
					e.printStackTrace(System.out);
				}

				//Tentative de lancer coop avec transpo dans le même tour
				if (Spell.getSpellID() == 438 || Spell.getSpellID() == 445)
				{
					//setHasUsedCoopTranspo(true);
				}

				boolean isCC = fighter.testIfCC(Spell.getTauxCC(fighter));
				String sort = Spell.getSpellID()+","+caseID+","+Spell.getSpriteID()+","+Spell.getLevel()+","+Spell.getSpriteInfos();
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.getGUID()+"", sort); // xx lance le sort
				if(isCC)
				{
					if(Config.DEBUG) GameServer.addToLog(fighter.getPacketsName()+" Coup critique sur le sort "+Spell.getSpellID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.getGUID()+"", sort); // CC !
				}
				if(fighter.isHide() && Spell.getSpellID() == 446)
					fighter.unHide(446);

				//Si le joueur est invi, on montre la case
				if(fighter.isHide())
					showCaseToAll(fighter.getGUID(), fighter.get_fightCell().getID());
				//on applique les effets de l'arme
				Spell.applySpellEffectToFight(this,fighter,Cell,isCC);
			}
			// le org.area.client ne peut continuer sans l'envoi de ce packet qui annonce le coût en PA
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102,fighter.getGUID()+"",fighter.getGUID()+",-"+Spell.getPACost(fighter));
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			//Refresh des Stats
			//refreshCurPlayerInfos();
			if(!isEc)
				fighter.addLaunchedSort(Cell.getFirstFighter(),Spell);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {};
			if((isEc && Spell.isEcEndTurn()))
			{
				_curAction = "";
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {};
				if(fighter.getMob() != null || fighter.isInvocation())//Mob, Invoque
				{
					return 5;
				}else
				{
					endTurn();
					return 5;
				}
			}
			verifIfTeamAllDead();
		}else if (fighter.getMob() != null || fighter.isInvocation())
		{
			return 10;
		}
		if(fighter.getPersonnage() != null)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102,fighter.getGUID()+"",fighter.getGUID()+",-0"); // annonce le coût en PA

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {};
		_curAction = "";
		return 0;
	}*/

    public synchronized boolean CanCastSpell(Fighter fighter, SortStats spell, Case cell, int launchCase) {
        // Vérification pour éviter des calculs inutiles @Flow
        if (_ordreJeu == null || _curPlayer < 0) {
            return false;
        }
        Player perso = null;
        if (fighter != null) {
            if (fighter.estInvocationControllable()) {
                perso = fighter.getInvocator().getPersonnage();
            } else {
                perso = fighter.getPersonnage();
            }
        } else {
            return false;
        }
        // Etat requis ou interdit
        if (!spell.getEtatRequis().isEmpty()) {
            for (int etat : spell.getEtatRequis()) {
                if (!fighter.isState(etat)) {
                    if (perso != null) {
                        SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                        SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
                    }
                    return false;
                }
            }
        }

        if (!spell.getEtatInterdit().isEmpty()) {
            for (int etat : spell.getEtatInterdit()) {
                if (fighter.isState(etat)) {
                    if (perso != null) {
                        SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                        SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
                    }
                    return false;
                }
            }
        }

        int ValidlaunchCase;
        if (launchCase <= -1) {
            ValidlaunchCase = fighter.get_fightCell().getID();
        } else {
            ValidlaunchCase = launchCase;
        }
        if (_curPlayer >= _ordreJeu.size()) { // index out of bound
            return false;
        }
        if (_ordreJeu == null || _ordreJeu.isEmpty() || _ordreJeu.get(_curPlayer) == null) return false;
        Fighter f = _ordreJeu.get(_curPlayer);
        if (f == null) return false;
        //Si le sort n'est pas existant
        if (spell == null) {
            if (Config.DEBUG) GameServer.addToLog("(" + _curPlayer + ") Sort non existant");
            if (perso != null) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1169");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //Si ce n'est pas au joueur de jouer
        if (f == null || f.getGUID() != fighter.getGUID()) {
            if (Config.DEBUG)
                GameServer.addToLog("Ce n'est pas au joueur. Doit jouer :(" + f.getGUID() + "). Fautif :(" + fighter.getGUID() + ")");
            if (perso != null) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1175");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //Si le joueur n'a pas assez de PA
        if (_curFighterPA < spell.getPACost(fighter)) {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Le joueur n'a pas assez de PA (" + _curFighterPA + "/" + spell.getPACost(fighter) + ")");
            if (perso != null) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1170;" + _curFighterPA + "~" + spell.getPACost(fighter));
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }

        //Si la cellule visée n'existe pas
        if (cell == null) {
            if (Config.DEBUG) GameServer.addToLog("(" + _curPlayer + ") La cellule visee n'existe pas");
            if (perso != null) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1172");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //Si la cellule visée n'est pas alignée avec le joueur alors que le sort le demande
        if (spell.isLineLaunch(fighter) && !Pathfinding.casesAreInSameLine(_map, ValidlaunchCase, cell.getID(), 'z')) {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Le sort demande un lancer en ligne, or la case n'est pas alignee avec le joueur");
            if (perso != null) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1173");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //Si tentative d'ajout d'un objet/invoc sur un joueur invisible |Return,Skryn
        try {
            for (SpellEffect s : spell.getEffects()) {
                if (s.getEffectID() == 185 || s.getEffectID() == 181 || s.getEffectID() == 180 || s.getEffectID() == 400 || s.getEffectID() == 780) {
                    for (Fighter p : getAllFighters()) {
                        if (cell == p.get_fightCell() && p.isDead() == false) { // @Flow - Si il est mort, on ignore
                            if (perso != null) {
                                perso.sendMess(Lang.LANG_111);
                                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
                            }
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (perso != null) {
                perso.sendText("Il y a quelques choses d'invisible sous cette cellule.");
            }
        }
        /**    if (isMob){
         for (Fighter p: getAllFighters()){
         if (cell == p.get_fightCell()){
         if (fighter.getTeam2() != p.getTeam2()){
         if (spell.getSpellID() == 22 || spell.getSpellID() == 2041){
         return false;
         }
         }
         }
         }
         }**/
        /**Si tentative de transfert de vie quand dérobade actif, false |Return,Skryn, j'ai refais le buff 9 ;)
         if (spell.getSpellID() == 435){
         if (fighter.hasBuff(9)){
         SocketManager.GAME_SEND_MESSAGE(perso, "Le sort Transfert de Vie est innutilisable lorsque vous êtes sous l'emprise du sort Dérobade.", Manager.CONFIG_MOTD_COLOR);
         return false;
         }
         }**/
        //Si le sort demande une ligne de vue et que la case demandée n'en fait pas partie
        if (spell.hasLDV(fighter) && !Pathfinding.checkLoS(_map, ValidlaunchCase, cell.getID(), fighter, false, null)) {
            if (Config.DEBUG)
                GameServer.addToLog("(" + _curPlayer + ") Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur");
            if (perso != null) {
                SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }

        // Pour peur si la personne poussée a la ligne de vue vers la case
        char dir = Pathfinding.getDirBetweenTwoCase(ValidlaunchCase, cell.getID(), _map, true);
        if (spell.getSpellID() == 67)
            if (!Pathfinding.checkLoS(_map, Pathfinding.GetCaseIDFromDirrection(ValidlaunchCase, dir, _map, true), cell.getID(), null, true, getAllFighters())) {
                if (Config.DEBUG)
                    GameServer.addToLog("(" + _curPlayer + ") Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur");
                if (perso != null) {
                    SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
                    SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                    SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
                }
                return false;
            }
        int dist = Pathfinding.getDistanceBetween(_map, ValidlaunchCase, cell.getID());
        int MaxPO = spell.getMaxPO(fighter);

        if (fighter.getPersonnage() != null) {
            if (MaxPO < 1)
                MaxPO = 1;
            if (dist < 1)
                dist = 1;
        }

        if (spell.isModifPO(fighter)) {
            MaxPO += fighter.getTotalStats().getEffect(Constant.STATS_ADD_PO);
            MaxPO = MaxPO <= 0 ? 1 : MaxPO;// Petit chaton #Say Meow
        }
        if (MaxPO < spell.getMinPO()) // Petit fix #Say Meow
        {
            MaxPO = spell.getMinPO();
        }
        //Vérification Portée mini / maxi
        if (dist < spell.getMinPO() || dist > MaxPO) {
            for (Fighter p : getAllFighters()) {
                if (cell == p.get_fightCell() && p.isHide() == false) { //@Poupou: Si la cible est invisible, on continue.
                    if (Config.DEBUG)
                        GameServer.addToLog("(" + _curPlayer + ") La case est trop proche ou trop eloignee Min: " + spell.getMinPO() + " Max: " + spell.getMaxPO(fighter) + " Dist: " + dist);
                    if (perso != null) {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "1171;" + spell.getMinPO() + "~" + MaxPO + "~" + dist);
                        SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                        SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
                        //perso.sendText(""+ dist +" (distance) "+ MaxPO +" (Maximal) "+ spell.getMinPO() +" (minimal)");
                    }
                    return false;
                }
            }
        }
        //vérification cooldown @Flow - On fix l'erreur d'un incompétent...
        if (!LaunchedSort.coolDownGood(fighter, spell.getSpellID())) {
            if (fighter.getPersonnage() != null) {
                if (spell.getSpellID() == 59) {
                    SocketManager.GAME_SEND_MESSAGE(fighter.getPersonnage(), "Vous ne pouvez lancer le sort Corruption qu'au bout de votre troisième tour de jeu.", "A00000");
                }
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //vérification nombre de lancer par tour
        int nbLancer = spell.getMaxLaunchbyTurn(fighter);
        if (nbLancer - LaunchedSort.getNbLaunch(fighter, spell.getSpellID()) <= 0 && nbLancer > 0) {
            if (perso != null) {
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //vérification nombre de lancer par cible
        Fighter target = cell.getFirstFighter();
        int nbLancerT = spell.getMaxLaunchbyByTarget(fighter);
        if (nbLancerT - LaunchedSort.getNbLaunchTarget(fighter, target, spell.getSpellID()) <= 0 && nbLancerT > 0) {
            if (perso != null) {
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        //Tentative de lancer coop avec transpo dans le même tour
        if ((spell.getSpellID() == 438 || spell.getSpellID() == 445) && HasUsedCoopTranspo()) {
            if (perso != null) {
                perso.sendMess(Lang.LANG_112);
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        if (spell.getSpellID() == 212124 && this._type != Constant.FIGHT_TYPE_PVM) {
            if (perso != null) {
                perso.sendText("Vous ne pouvez pas lancer ce sort dans ce type de combat.");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }
        if (spell.getSpellID() == 212124 && this.dissipationHasBeenLaunch) {
            if (perso != null) {
                perso.sendText("La dissipation ne peut être ré-utilisée pour ce combat.");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }

        // Laisse spirituelle
        if (spell.getSpellID() == 420 && this.getLastFighterDie(fighter.getTeam()) == null) {
            if (perso != null) {
                perso.sendText("Personne de votre équipe n'est encore mort !");
                SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(perso.getFight(), 7, 0, perso.getGuid());
            }
            return false;
        }

        return true;
    }


    public ArrayList<Fighter> getAllFighters() {
        ArrayList<Fighter> fighters = new ArrayList<Fighter>();

        for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
            fighters.add(entry.getValue());
        }
        for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
            fighters.add(entry.getValue());
        }
        return fighters;
    }

    public synchronized String GetGE(int win) {
        long time = System.currentTimeMillis() - get_startTime();
        int initGUID = _init0.getGUID();

        int type = Constant.FIGHT_TYPE_CHALLENGE;// toujours 0
        if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE)//Sauf si gain d'honneur
            type = _type;

        StringBuilder Packet = new StringBuilder();
        Packet.append("GE").append(time);
        //String Packet = "GE"+time;
        // si c'est un combat PVM alors bonus potentiel en étoiles
        if (_type == Constant.FIGHT_TYPE_PVM && _mobGroup != null)
            Packet.append(";").append(_mobGroup.getStarBonus());
        Packet.append("|").append(initGUID).append("|").append(type).append("|");
        ArrayList<Fighter> TEAM1 = new ArrayList<Fighter>();
        ArrayList<Fighter> TEAM2 = new ArrayList<Fighter>();
        if (win == 1) {
            TEAM1.addAll(_team0.values());
            TEAM2.addAll(_team1.values());
        } else {
            TEAM1.addAll(_team1.values());
            TEAM2.addAll(_team0.values());
        }
        // Triages des équipes
        for (int i = 0; i < TEAM1.size(); i++) {
            Fighter f = TEAM1.get(i);
            if (f.getPersonnage() != null) {
                if (f.getPersonnage().getFight() != this || f.getPersonnage().getAccount().getGmLevel() > 0 && !Config.BETA) {
                    TEAM1.remove(i);
                }
            }
        }
        for (int i = 0; i < TEAM2.size(); i++) {
            Fighter f = TEAM2.get(i);
            if (f.getPersonnage() != null) {
                if (f.getPersonnage().getFight() != this || f.getPersonnage().getAccount().getGmLevel() > 0 && !Config.BETA) {
                    TEAM2.remove(i);
                }
            }
        }
        //Calculs des niveaux de groupes
        //int TEAM1lvl = 0;
        // int TEAM2lvl = 0;
        //Traque
        if (_type == Constant.FIGHT_TYPE_AGRESSION) {
            Player curp = null;
            int nb_perso = 0;
            //Evaluation du level
            for (Fighter F : TEAM1) {
                if (F.isInvocation() || F.isDouble()) continue;
                if (F.getPersonnage() != null) {
                    curp = F.getPersonnage();
                    nb_perso++;
                }
                //TEAM1lvl += F.get_lvl();
            }
            //Evaluation de la présence de la traque
            Traque traque = null;
            ArrayList<Traque> traqued_by = null;
            if (curp != null && nb_perso == 1) {
                traque = Stalk.getTraqueByOwner(curp);
                traqued_by = Stalk.getTraquesByTarget(curp);
                if (traqued_by.size() == 0) traqued_by = null;
            }
            for (Fighter F : TEAM2) {
                if (F.isInvocation()) continue;
                if (F.getPersonnage() != null && traque != null) {
                    if (traque.getTarget() == F.getPersonnage().getGuid()) {
                        F.getPersonnage().sendMess(Lang.LANG_115);

                        traque.valider();
                    }
                }
                /**    if(F.getPersonnage() != null && traqued_by != null)
                 {
                 for(Traque t : traqued_by)
                 {
                 if(t.getOwner() == F.getPersonnage().get_GUID())
                 {
                 //Le mec a perdu face à sa traque
                 SocketManager.GAME_SEND_MESSAGE(F.getPersonnage(), "Vous venez de perdre contre votre traque.", "A00000");
                 SocketManager.GAME_SEND_MESSAGE(curp, "Vous venez de gagner contre une personne qui vous traquait.", "0000A0");
                 t.reset();
                 curp.addKamas(10000000);
                 long xp = curp.get_curExp()*5/100;
                 curp.addXp(xp);
                 SocketManager.GAME_SEND_STATS_PACKET(curp);
                 SocketManager.GAME_SEND_MESSAGE(curp, new StringBuilder("Vous avez reçu 10 000 000 Kamas et ").append(xp).append(" points d'expérience suite à cette victoire").toString(), "00A000");
                 }
                 }
                 } **/
                //TEAM2lvl += F.get_lvl();
            }
        }
        //fin
        /* DEBUG
        Console.print("TEAM1: lvl="+TEAM1lvl);
        Console.print("TEAM2: lvl="+TEAM2lvl);
        //*/
        //DROP SYSTEM

        //Challenge augmente la PP totale (atteint plus facilement les seuils)
        double factChalDrop = 100;
        if ((this._type == 4) && (this._challenges.size() > 0)) {
            try {
                for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                    if ((c.getValue() == null) || (!((Challenge) c.getValue()).get_win()))
                        continue;
                    factChalDrop += c.getValue().get_gainDrop();
                }
            } catch (Exception e) {
            }
            //factChalDrop += _mobGroup.getStarBonus(); // on ajoute le bonus en étoiles
        }
        factChalDrop /= 100;
        //Calcul de la PP de groupe
        int groupPP = 0, minkamas = 0, maxkamas = 0;
        for (Fighter F : TEAM1) {
            if (!F.isInvocation() || (F.getMob() != null && F.getMob().getTemplate().getID() == 285)) {
                groupPP += F.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
            }
        }
        if (groupPP < 0) groupPP = 0;
        groupPP *= factChalDrop;
        //Calcul des drops possibles
        ArrayList<Drop> possibleDrops = new ArrayList<Drop>();
        for (Fighter F : TEAM2) {
            //Evaluation de l'argent à gagner
            if (F.isInvocation() || F.getMob() == null) continue;
            minkamas += F.getMob().getTemplate().getMinKamas();
            maxkamas += F.getMob().getTemplate().getMaxKamas();
            //Evaluation de la liste des drops droppable
            for (Drop D : F.getMob().getDrops()) {
                if (D.getMinProsp() <= groupPP) {
                    int taux = (int) (D.get_taux() * Config.RATE_DROP);
                    possibleDrops.add(new Drop(D.get_itemID(), 0, taux, D.get_max()));
                }
            }
        }
        if (_type == Constant.FIGHT_TYPE_PVT) {
            minkamas = (int) collector.getKamas() / TEAM1.size();
            maxkamas = minkamas;
            possibleDrops = collector.getDrops();
        }
        //On Réordonne la liste des combattants en fonction de la PP
        ArrayList<Fighter> Temp = new ArrayList<Fighter>();
        Fighter curMax = null;
        while (Temp.size() < TEAM1.size()) {
            int curPP = -1;
            for (Fighter F : TEAM1) {
                //S'il a plus de PP et qu'il n'est pas listé
                if (F.getTotalStats().getEffect(Constant.STATS_ADD_PROS) > curPP && !Temp.contains(F)) {
                    curMax = F;
                    curPP = F.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
                }
            }
            Temp.add(curMax);
        }
        //On enleve les invocs
        TEAM1.clear();
        TEAM1.addAll(Temp);
	        /* DEBUG
	        Console.print("DROP: PP ="+groupPP);
	        Console.print("DROP: nbr="+possibleDrops.size());
	        Console.print("DROP: Kam="+totalkamas);
	        //*/
        //FIN DROP SYSTEM
        //XP SYSTEM @Flow Devrait fonctionner
        long totalXP = 0;
        for (Fighter F : TEAM2) {
            if (F.isInvocation() || F.getMob() == null) continue;
            totalXP += F.getMob().getBaseXp();
        }
        if ((this._type == 4) && (this._challenges.size() > 0)) {
            try {
                long totalGainXp = 0;
                for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                    if ((c.getValue() == null) || (!((Challenge) c.getValue()).get_win()))
                        continue;
                    totalGainXp += c.getValue().get_gainXp();
                }
     	    	   /*totalGainXp += _mobGroup.get_bonusValue(); // on ajoute le bonus en étoiles*/
                totalXP *= 100L + totalGainXp; //on multiplie par la somme des boost chal
                totalXP /= 100L;

            } catch (Exception e) {
            }

        }
        //FIN XP SYSTEM
        //Capture d'âmes
        boolean mobCapturable = true;
        for (Fighter F : TEAM2) {
            try {
                mobCapturable &= F.getMob().getTemplate().isCapturable();
            } catch (Exception e) {
                mobCapturable = false;
                break;
            }
        }
        isCapturable |= mobCapturable;
        List<Integer> mapsCaptureInterdite = Arrays.asList(28039, 28040, 28041, 28042);
        if (mapsCaptureInterdite.contains(get_map().get_id())) {
            isCapturable = false;
        }
        if (isCapturable) {
            boolean isFirst = true;
            int maxLvl = 0;
            String pierreStats = "";


            for (Fighter F : TEAM2)    //Création de la pierre et verifie si le groupe peut être capturé
            {
                if (!isFirst)
                    pierreStats += "|";

                pierreStats += F.getMob().getTemplate().getID() + "," + F.get_lvl();//Converti l'ID du monstre en Hex et l'ajoute au stats de la futur pierre d'âme

                isFirst = false;

                if (F.get_lvl() > maxLvl)    //Trouve le monstre au plus haut lvl du groupe (pour la puissance de la pierre)
                    maxLvl = F.get_lvl();
            }
            pierrePleine = new SoulStone(-1, 1, 7010, Constant.ITEM_POS_NO_EQUIPED, pierreStats);    //Crée la pierre d'âme

            for (Fighter F : TEAM1)    //Récupère les captureur
            {
                if (!F.isInvocation() && F.isState(Constant.ETAT_CAPT_AME)) {
                    _captureur.add(F);
                }
            }
            if (_captureur.size() > 0 && !World.isArenaMap(get_map().get_id()))    //S'il y a des captureurs
            {
                Collections.shuffle(_captureur);
                for (int i = 0; i < _captureur.size(); i++) {
                    try {
                        Fighter f = _captureur.get(Formulas.getRandomValue(0, _captureur.size() - 1));    //Récupère un captureur au hasard dans la liste
                        if (!(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getTemplate(false).getType() == Constant.ITEM_TYPE_PIERRE_AME)) {
                            _captureur.remove(f);
                            continue;
                        }


                        if (f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getTemplate(false).getID() != 9718) {
                            Couple<Integer, Integer> pierreJoueur = Formulas.decompPierreAme(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME));//Récupère les stats de la pierre équippé
                            if (pierreJoueur.second < maxLvl)    //Si la pierre est trop faible
                            {
                                _captureur.remove(f);
                                continue;
                            }
                            int captChance = Formulas.totalCaptChance(pierreJoueur.first, f.getPersonnage());
                            if (Formulas.getRandomValue(1, 100) > captChance) continue;
                        }
                        //Retire la pierre vide au personnage et lui envoie ce changement
                        int pierreVide = f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getGuid();
                        f.getPersonnage().deleteItem(pierreVide);
                        SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPersonnage(), pierreVide);

                        captWinner = f._id;
                        break;
                    } catch (NullPointerException e) {
                        continue;
                    }
                }
            }
        }
        //Fin Capture
	        /* Testons Configtenant de  l'apprivoisement des DD
	         * comme le système de capture ^^"
	         */
        boolean mobCanAppri = true;

        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().isApprivoisable();
            } catch (Exception e) {
                mobCanAppri = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereAreThree();
            } catch (Exception e) {
                ThereAreThree = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereAreAmandRousse();
            } catch (Exception e) {
                ThereAreAmandRousse = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereAreAmandDore();
            } catch (Exception e) {
                ThereAreAmandDore = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereAreRousseDore();
            } catch (Exception e) {
                ThereAreRousseDore = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereIsRousse();
            } catch (Exception e) {
                ThereIsRousse = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereIsDore();
            } catch (Exception e) {
                ThereIsDore = false;
                break;
            }
        }
        for (Fighter F : TEAM2) {
            try {
                mobCanAppri &= F.getMob().getTemplate().ThereIsAmand();
            } catch (Exception e) {
                ThereIsAmand = false;
                break;
            }
        }
        CanCaptu |= mobCanAppri;

        if (CanCaptu) {

            for (Fighter F : TEAM1)    //Récupère les captureur
            {
                if (!F.isInvocation() && F.isState(Constant.ETAT_APPRIVOISEMENT)) {
                    _apprivoiseur.add(F);
                }
            }
            for (int i = 0; i < _apprivoiseur.size(); i++) {
                if (_apprivoiseur.size() > 0) // Si il y a des captureurs
                {
                    try {
                        Fighter f = _apprivoiseur.get(Formulas.getRandomValue(0, _apprivoiseur.size() - 1)); //Récupère un captureur au hasard dans la liste
                        if (!(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getTemplate(false).getType() == Constant.ITEM_TYPE_FILET_CAPTURE))// Qui possèdent un filet
                        {
                            _apprivoiseur.remove(f);
                            continue;
                        }

                        int captChance = Formulas.totalAppriChance(5, f.getPersonnage());

                        if (Formulas.getRandomValue(1, 100) <= captChance)//Si le joueur obtiens la capture tengu
                        {
                            //Retire la pierre vide au personnage et lui envoie ce changement
                            int pierreVide = f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getGuid();
                            int tID = 0;
                            for (Fighter F : TEAM2)
                                if (F.getMob().getTemplate().ThereAreThree() == true) {
                                    tID = Formulas.ChoseIn3Time(7819, 7817, 7811);
                                } else {
                                    if (F.getMob().getTemplate().ThereAreAmandDore() == true) {
                                        tID = Formulas.getRandomValue(7819, 7817);
                                    } else {
                                        if (F.getMob().getTemplate().ThereAreAmandRousse() == true) {
                                            tID = Formulas.getRandomValue(7819, 7811);
                                        } else {
                                            if (F.getMob().getTemplate().ThereAreRousseDore() == true) {
                                                tID = Formulas.getRandomValue(7811, 7817);
                                            } else {
                                                if (F.getMob().getTemplate().ThereIsAmand() == true) {
                                                    tID = 7819;
                                                } else {
                                                    if (F.getMob().getTemplate().ThereIsRousse() == true) {
                                                        tID = 7811;
                                                    } else {
                                                        if (F.getMob().getTemplate().ThereIsDore() == true) {
                                                            tID = 7817;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            f.getPersonnage().deleteItem(pierreVide);
                            SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPersonnage(), pierreVide);
                            ObjTemplate T = World.getObjTemplate(tID);
                            Item O = T.createNewItem(0, false, -1);
                            //Si retourne true, on l'ajoute au monde
                            if (f.getPersonnage().addObjet(O, true))
                                World.addObjet(O, true);

                            break;
                        }
                    } catch (NullPointerException e) {
                        continue;
                    }
                }
            }
        }

        //Fin de l'apprivoisement
        //Début pierre précieuse
        int nombreDePierresPrecieuses = 0;
        for (Fighter i : TEAM1) {
            if (i.hasLeft()) continue;//Si il abandonne, il ne gagne pas d'xp
            if (i._double != null) {
                continue;//Pas de double dans les gains
            }
            // drop pierres précieuses @Flow
            if (type == Constant.FIGHT_TYPE_CHALLENGE && _mobGroup != null && i.isDead() == false) {
                int mapID = i.getPersonnage().getMap().get_id();
                if (i.getPersonnage() != null && mapID != 534 && mapID != 27012) {
                    int max = Formulas.getKamasWin(i, TEAM1, minkamas, maxkamas) / 100;
                    if (max <= 0) // Valeurs négatives
                    {
                        max = 10;
                    }
                    int prospection;
                    prospection = Formulas.getPros(i) / 100; // prospection
                    if (prospection <= 0) // Valeurs négatives
                    {
                        prospection = 1;
                    } else if (prospection * 200 == 400) {
                        prospection = 2;
                    }
                    int maxi = max * prospection;
                    if (maxi <= 0) {
                        maxi = 2;
                    } // Comment ?
                    int round = Math.round(maxi);
                    Random rand = new Random();
                    int min = 1;
                    if (round > 80) {
                        round = 80;
                    }
                    // Modification drop en fin de donjon
                    switch (i.getPersonnage().getMap().get_id()) {
                        /*case 15006:
                            min = 150;
                            round = 250;
                            break;
                        case 15014:
                            min = 200;
                            round = 350;
                            break;
                        case 15024:
                            min = 250;
                            round = 450;
                            break;
                        case 15035:
                            min = 300;
                            round = 550;
                            break;
                        case 15056:
                            min = 320;
                            round = 600;
                            break;
                        case 15134:
                            min = 350;
                            round = 850;
                            break;
                        case 15124:
                            min = 400;
                            round = 850;
                            break;
                        case 15114:
                            min = 500;
                            round = 950;
                            break;
                        case 15104:
                            min = 600;
                            round = 1000;
                        case 15144:
                            min = 700;
                            round = 1200;
                            break;
                        case 26004:
                            min = 700;
                            round = 950;
                            break;
                        case 26103:
                            min = 1000;
                            round = 2000;*/
                        default:
                            break;
                    }
                    int randomNum = 1;
                    try {
                        randomNum = rand.nextInt((round - min) + 1) + min; // @Flow - Merci Aspyrino pour le crash negative bound
                    } catch (Exception E) {
                        randomNum = 1;
                    }
	    		/*i.getPersonnage().sendText("Vous avez récolté "+ (randomNum) +" pierres précieuses !");
				ObjTemplate t = World.getObjTemplate(470001);
		    	Item obj = t.createNewItem(randomNum, false, -1);
				if(i.getPersonnage().addObjet(obj, true))
					World.addObjet(obj,true);*/
                    Collector perco = Collector.GetPercoByMapID(_map.get_id());
                    if (perco != null && _type == 4) {
                        int tauxCollectorPP = rand.nextInt(16) + 10;
                        nombreDePierresPrecieuses = (100 - tauxCollectorPP) * randomNum / 100;
                        perco.nbDePierresDrop += (randomNum - nombreDePierresPrecieuses);
                    } else {
                        nombreDePierresPrecieuses = randomNum;
                    }
                }
            }

            if (type == Constant.FIGHT_TYPE_CHALLENGE) {
                if (i.isInvocation() && i.getMob() != null && i.getMob().getTemplate().getID() != 285) continue;
                boolean canDropAndXp = true;
                if (_type == Constant.FIGHT_TYPE_PVT) {
                    if (i.isPerco()) {
                        canDropAndXp = false;
                    } else {
                        Collector perco = Collector.GetPercoByMapID(_map.get_id());
                        if (perco != null && i.getPersonnage() != null) {
                            if (perco.GetPercoGuildID() == i.getPersonnage().get_guild().get_id()) { // Les défenseurs et le perco n'a pas de gains en défense perco
                                canDropAndXp = false;
                            }
                        }
                    }
                }
                long winxp = Formulas.getXpWinPvm2(i, TEAM1, TEAM2, totalXP, this);
                AtomicReference<Long> XP = new AtomicReference<Long>();
                XP.set(winxp);
                long guildxp = canDropAndXp ? Formulas.getGuildXpWin(i, XP) : 0;
                long mountxp = 0;

                if (i.getPersonnage() != null && i.getPersonnage().isOnMount() && canDropAndXp) {
                    mountxp = Formulas.getMountXpWin(i, XP);
                    i.getPersonnage().getMount().addXp(mountxp);
                    SocketManager.GAME_SEND_Re_PACKET(i.getPersonnage(), "+", i.getPersonnage().getMount());
                }
                String drops = "";
                long winKamas = Formulas.getKamasWin(i, TEAM1, minkamas, maxkamas);
                if (i.getPersonnage() != null && i.getPersonnage().getKolizeum() != null && i.getPersonnage().getKolizeum().isStarted()) {
                    /** Drop **/
                    drops += Config.COINS_ID + "~" + 1 * Config.RATE_COINS;
                    ObjTemplate t = World.getObjTemplate(Config.COINS_ID);
                    Item obj = t.createNewItem(Config.RATE_COINS, false, -1);
                    if (i.getPersonnage().addObjet(obj, true)) {//Si le joueur n'avait pas d'item similaire
                        World.addObjet(obj, true);
                    }
                    if (i.getPersonnage().get_align() != Constant.ALIGNEMENT_NEUTRE) {
                        i.getPersonnage().sendText("Vous avez obtenu 200 points d'honneur !");
                        i.getPersonnage().addHonor(200);
                    }
                    setFightStarted(false);
                    /** Desinscription kolizeum + message winner**/
                    i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(), World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
                    Kolizeum.unsubscribe(i.getPersonnage());
                    i.getPersonnage().setWinKolizeum(i.getPersonnage().getWinKolizeum() + 1);
                    i.getPersonnage().sendMess(Lang.LANG_116);
                }

                if (i.getPersonnage() != null && i.getPersonnage().getArena() == 1) {
                    Arena.sendReward(Team.getTeamByID(i.getPersonnage().getTeamID()), Team.getTeamByID(TEAM2.get(1).getPersonnage().getTeamID()));
                    i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(), World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
                    i.getPersonnage().setArena(-1);
                    i.getPersonnage().setWinArena(i.getPersonnage().getWinArena() + 1);
                }

                //Drop system

                ArrayList<Drop> temp = new ArrayList<Drop>();
                temp.addAll(possibleDrops);
                Map<Integer, Integer> itemWon = new TreeMap<Integer, Integer>();
                int PP = i.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
                boolean allIsDropped = false;
                while (!allIsDropped && canDropAndXp) {
                    for (Drop D : temp) {
                        int t = (int) (D.get_taux() * PP);//Permet de gerer des taux>0.01
                        t = (int) ((double) t * factChalDrop);
                        if (_type == Constant.FIGHT_TYPE_PVT)
                            t = 10000 / TEAM1.size();
                        int jet = Formulas.getRandomValue(0, 100 * 100);
                        //	Console.print("PP : "+PP+"    chance : "+t+"    jet : "+jet);
                        if (jet < t) {
                            ObjTemplate OT = World.getObjTemplate(D.get_itemID());
                            if (OT == null) continue;
                            //	on ajoute a la liste
                            itemWon.put(OT.getID(), (itemWon.get(OT.getID()) == null ? 0 : itemWon.get(OT.getID())) + 1);

                            D.setMax(D.get_max() - 1);
                            if (D.get_max() == 0) possibleDrops.remove(D);
                        }
                    }
                    allIsDropped = (_type == Constant.FIGHT_TYPE_PVT ? false : true);
                    if (possibleDrops.isEmpty())
                        allIsDropped = true;
                }
                if (i._id == captWinner && pierrePleine != null)    //S'il à capturé le groupe
                {
                    if (drops.length() > 0) drops += ",";
                    drops += pierrePleine.getTemplate(false).getID() + "~" + 1;
                    i.getPersonnage().addObjet(pierrePleine, false);
                }
                // Ajout du nombre de Pierres précieuses gagnées dans les drops
                if (nombreDePierresPrecieuses > 0) {
                    // bonbon pp
                    if (i.getPersonnage().askCandyActive(7804)) { // 25% pp
                        nombreDePierresPrecieuses += (0.25 * nombreDePierresPrecieuses);
                    } else if (i.getPersonnage().askCandyActive(7803)) { // 50% pp
                        nombreDePierresPrecieuses += (0.50 * nombreDePierresPrecieuses);
                    }
                    itemWon.put(470001, nombreDePierresPrecieuses);
                }
                for (Entry<Integer, Integer> entry : itemWon.entrySet()) {
                    ObjTemplate OT = World.getObjTemplate(entry.getKey());
                    if (OT == null) continue;
                    if (drops.length() > 0) drops += ",";
                    drops += entry.getKey() + "~" + entry.getValue();
                    Item obj = OT.createNewItem(entry.getValue(), false, -1);
                    if (i.getPersonnage() != null && i.getPersonnage().addObjet(obj, true))
                        World.addObjet(obj, true);
                    else if (i.isInvocation() && i.getMob().getTemplate().getID() == 285 && i.getInvocator().getPersonnage().addObjet(obj, true))
                        World.addObjet(obj, true);
                }
                //fin drop system
                winxp = XP.get();
                if (!canDropAndXp) {
                    winxp = 0;
                    winKamas = 0;
                }
                if (winxp != 0 && i.getPersonnage() != null) {
                    // Taux suppresion xp dû au prestiges @Flow
                    int tauxDiminution = Constant.obtenir_taux_xp_prestige(i.getPersonnage().getPrestige());
                    if (tauxDiminution != 0) {
                        winxp -= (tauxDiminution * winxp / 100);
                    }
                    i.getPersonnage().addXp(winxp);
                }
                if (winKamas != 0 && i.getPersonnage() != null)
                    i.getPersonnage().addKamas(winKamas);
                else if (winKamas != 0 && i.isInvocation() && i.getInvocator().getPersonnage() != null)
                    i.getInvocator().getPersonnage().addKamas(winKamas);
                if (guildxp > 0 && i.getPersonnage().getGuildMember() != null)
                    i.getPersonnage().getGuildMember().giveXpToGuild(guildxp);

                Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
                Packet.append(i.xpString(";")).append(";");


                Packet.append((winxp == 0 ? "" : winxp)).append(";");
                Packet.append((guildxp == 0 ? "" : guildxp)).append(";");
                Packet.append((mountxp == 0 ? "" : mountxp)).append(";");
                Packet.append(drops).append(";");//Drop
                Packet.append((winKamas == 0 ? "" : winKamas)).append("|");
            } else {
                // Si c'est un neutre, on ne gagne pas de points
                if (i.isInvocation() && i.getPersonnage() == null)
                    continue;// Le bug de pvp
                int winH = 0;
                int winD = 0;
                if (type == Constant.FIGHT_TYPE_AGRESSION) {
                    if (_init1.getPersonnage().get_align() != 0 && _init0.getPersonnage().get_align() != 0) {
                        if (_init1.getPersonnage().getAccount().getCurIp().compareTo(_init0.getPersonnage().getAccount().getCurIp()) != 0 || Config.ALLOW_MULE_PVP) {
                            // calcul du temps de combat, système anti-mulage @WINNER @Flow
                            Long timeFight = (System.currentTimeMillis() - this.get_startTime()) / 1000;
                            Integer TimeAntiMulage = 60;
                            /*if (timeFight <= TimeAntiMulage) {

                                SocketManager.GAME_SEND_MESSAGE(i.getPersonnage(), "<b>Tentative de mulage : </b> Le combat a été trop rapide, pour cette raison, vous n'avez gagner aucun points d'honneur.", Config.CONFIG_MOTD_COLOR);
                                winH = 0;

                            } else if (timeFight > TimeAntiMulage) {
                                winH = Formulas.calculHonorWin(TEAM1, TEAM2, i);
                            }*/
                            winH = Formulas.calculHonorWin(TEAM1, TEAM2, i);
                        }
                        if (i.getPersonnage().getDeshonor() > 0) winD = -1;
                    }

                    Player P = i.getPersonnage();
                    if (P.get_honor() + winH < 0) winH = -P.get_honor();
                    P.addHonor(winH);
                    P.setDeshonor(P.getDeshonor() + winD);
                    Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
                    Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? World.getExpLevel(P.getGrade()).pvp : 0)).append(";");
                    Packet.append(P.get_honor()).append(";");
                    int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
                    if (maxHonor == -1) maxHonor = World.getExpLevel(P.getGrade()).pvp;
                    Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
                    Packet.append(winH).append(";");
                    Packet.append(P.getGrade()).append(";");
                    Packet.append(P.getDeshonor()).append(";");
                    Packet.append(winD);
                    Packet.append(";;0;0;0;0;0|");
                } else if (_type == Constant.FIGHT_TYPE_CONQUETE) {
                    Player P = i.getPersonnage();
                    if (P != null) {
                        winH = 150;
                        if (P.get_honor() + winH < 0) winH = -P.get_honor();
                        P.addHonor(winH);
                        P.setDeshonor(P.getDeshonor() + winD);
                        Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
                        Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? World.getExpLevel(P.getGrade()).pvp : 0)).append(";");
                        Packet.append(P.get_honor()).append(";");
                        int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
                        if (maxHonor == -1) maxHonor = World.getExpLevel(P.getGrade()).pvp;
                        Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
                        Packet.append(winH).append(";");
                        Packet.append(P.getGrade()).append(";");
                        Packet.append(P.getDeshonor()).append(";");
                        Packet.append(winD);
                        Packet.append(";;0;0;0;0;0|");
                    } else {
                        Prism prisme = i.getPrisme();
                        winH = 200;
                        if (prisme.getHonor() + winH < 0)
                            winH = -prisme.getHonor();
                        winH *= 3;
                        prisme.addHonor(winH);
                        Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";")
                                .append((i.isDead() ? "1" : "0")).append(";");
                        Packet.append(World.getExpLevel(prisme.getlevel()).pvp).append(";");
                        Packet.append(prisme.getHonor()).append(";");
                        int maxHonor = World.getExpLevel(prisme.getlevel() + 1).pvp;
                        if (maxHonor == -1)
                            maxHonor = World.getExpLevel(prisme.getlevel()).pvp;
                        Packet.append(maxHonor).append(";");
                        Packet.append(winH).append(";");
                        Packet.append(prisme.getlevel()).append(";");
                        Packet.append("0;0;;0;0;0;0;0|");
                    }
                }
            }
        }
        for (Fighter i : TEAM2) {
            if (i.getPersonnage() != null && i.getPersonnage().getKolizeum() != null && i.getPersonnage().getKolizeum().isStarted()) {
                i.getPersonnage().sendMess(Lang.LANG_117);
                Kolizeum.unsubscribe(i.getPersonnage());
                i.getPersonnage().setLoseKolizeum(i.getPersonnage().getLoseKolizeum() + 1);
                i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(), World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
            }

            if (i.getPersonnage() != null && i.getPersonnage().getArena() == 1) {
                Arena.withdrawPoints(Team.getTeamByID(i.getPersonnage().getTeamID()), Team.getTeamByID(TEAM1.get(1).getPersonnage().getTeamID()));
                i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(), World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
                i.getPersonnage().setArena(-1);
                i.getPersonnage().setLoseArena(i.getPersonnage().getLoseArena() + 1);
            }


            if (i._double != null) continue;//Pas de double dans les gains
            if (i.isInvocation() && i.getMob().getTemplate().getID() != 285) continue;//On affiche pas les invocs

            if (_type != Constant.FIGHT_TYPE_AGRESSION && _type != Constant.FIGHT_TYPE_CONQUETE) {
                if (i.getPDV() == 0 || i.hasLeft()) {
                    Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";1").append(";").append(i.xpString(";")).append(";;;;|");
                } else {
                    Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";0").append(";").append(i.xpString(";")).append(";;;;|");
                }
            } else {
                // Si c'est un neutre, on ne gagne pas de points
                int winH = 0;
                int winD = 0;
                if (_type == Constant.FIGHT_TYPE_AGRESSION) {
                    if (_init1.getPersonnage().get_align() != 0 && _init0.getPersonnage().get_align() != 0) {
                        if (_init1.getPersonnage().getAccount().getCurIp().compareTo(_init0.getPersonnage().getAccount().getCurIp()) != 0 || Config.ALLOW_MULE_PVP) {
                            // calcul du temps de combat, système anti-mulage #Removed
                            winH = Formulas.calculHonorWin(TEAM1, TEAM2, i);
                        }
                    }

                    Player P = i.getPersonnage();
                    if (P.get_honor() + winH < 0) winH = -P.get_honor();
                    P.addHonor(winH);
                    if (P.getDeshonor() - winD < 0) winD = 0;
                    P.setDeshonor(P.getDeshonor() - winD);
                    Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
                    Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? World.getExpLevel(P.getGrade()).pvp : 0)).append(";");
                    Packet.append(P.get_honor()).append(";");
                    int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
                    if (maxHonor == -1) maxHonor = World.getExpLevel(P.getGrade()).pvp;
                    Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
                    Packet.append(winH).append(";");
                    Packet.append(P.getGrade()).append(";");
                    Packet.append(P.getDeshonor()).append(";");
                    Packet.append(winD);
                    Packet.append(";;0;0;0;0;0|");
                } else if (_type == Constant.FIGHT_TYPE_CONQUETE) {
                    winH = Formulas.calculHonorWinPrisms(TEAM1, TEAM2, i);
                    Player P = i.getPersonnage();
                    if (P != null) {
                        winH = -500;
                        if (P.get_honor() - 500 < 0)
                            P.set_honor(0);
                        else
                            P.addHonor(winH);
                        if (P.getDeshonor() - winD < 0)
                            winD = 0;
                        P.setDeshonor(P.getDeshonor() - winD);
                        Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";")
                                .append((i.isDead() ? "1" : "0")).append(";");
                        Packet.append("0;");
                        Packet.append(P.get_honor()).append(";");
                        int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
                        if (maxHonor == -1)
                            maxHonor = 0;
                        Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
                        Packet.append(winH).append(";");
                        Packet.append(P.getGrade()).append(";");
                        Packet.append(P.getDeshonor()).append(";");
                        Packet.append(winD);
                        Packet.append(";;0;0;0;0;0|");
                    } else {
                        Prism Prisme = i.getPrisme();
                        if (Prisme.getHonor() + winH < 0)
                            winH = -Prisme.getHonor();
                        Prisme.addHonor(winH);
                        Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";").append(i.get_lvl()).append(";")
                                .append((i.isDead() ? "1" : "0")).append(";");
                        Packet.append(World.getExpLevel(Prisme.getlevel()).pvp).append(";");
                        Packet.append(Prisme.getHonor()).append(";");
                        int maxHonor = World.getExpLevel(Prisme.getlevel() + 1).pvp;
                        if (maxHonor == -1)
                            maxHonor = World.getExpLevel(Prisme.getlevel()).pvp;
                        Packet.append(maxHonor).append(";");
                        Packet.append(winH).append(";");
                        Packet.append(Prisme.getlevel()).append(";");
                        Packet.append("0;0;;0;0;0;0;0|");

                    }
                }

            }
            if (i.getPersonnage() != null) {
                Player perso = i.getPersonnage();
                SocketManager.GAME_SEND_GV_PACKET(perso);
                perso.set_duelID(-1);
                perso.set_ready(false);
                perso.fullPDV();
                perso.set_fight(null);
                SocketManager.GAME_SEND_GV_PACKET(perso);
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
                perso.get_curCell().addPerso(perso);
            }
        }
        if (Collector.GetPercoByMapID(_map.get_id()) != null && _type == 4)//On a un percepteur ONLY PVM ? Drop du percepteur
        {

            Collector p = Collector.GetPercoByMapID(_map.get_id());
            long winxp = (int) Math.floor(Formulas.getXpWinPerco(p, TEAM1, TEAM2, totalXP) / 100);
            long winkamas = (int) Math.floor(Formulas.getKamasWinPerco(minkamas, maxkamas) / 100);
            p.setXp(p.getXp() + winxp);
            p.setKamas(p.getKamas() + winkamas);
            Packet.append("5;").append(p.getGuid()).append(";").append(p.get_N1()).append(",").append(p.get_N2()).append(";").append(World.getGuild(p.get_guildID()).get_lvl()).append(";0;");
            Guild G = World.getGuild(p.get_guildID());
            Packet.append(G.get_lvl()).append(";");
            Packet.append(G.get_xp()).append(";");
            Packet.append(World.getGuildXpMax(G.get_lvl())).append(";");
            Packet.append(";");//XpGagner
            Packet.append(winxp).append(";");//XpGuilde
            Packet.append(";");//Monture

            String drops = "";
            ArrayList<Drop> temp = new ArrayList<Drop>();
            temp.addAll(possibleDrops);
            Map<Integer, Integer> itemWon = new TreeMap<Integer, Integer>();

            for (Drop D : temp) {
                int t = (int) (D.get_taux() * 100);//Permet de gerer des taux>0.01
                int jet = Formulas.getRandomValue(0, 100 * 100);
                if (jet < t) {
                    ObjTemplate OT = World.getObjTemplate(D.get_itemID());
                    if (OT == null) continue;
                    //on ajoute a la liste
                    itemWon.put(OT.getID(), (itemWon.get(OT.getID()) == null ? 0 : itemWon.get(OT.getID())) + 1);

                    D.setMax(D.get_max() - 1);
                    if (D.get_max() == 0) possibleDrops.remove(D);
                }
            }
            // Drop pp
           /* if (p.getNbDePierresDropTotal < 601) {
                itemWon.put(470001, p.nbDePierresDrop);
                p.getNbDePierresDropTotal += p.nbDePierresDrop;
            }*/
            p.nbDePierresDrop = 0;
            for (Entry<Integer, Integer> entry : itemWon.entrySet()) {
                ObjTemplate OT = World.getObjTemplate(entry.getKey());
                if (OT == null) continue;
                if (drops.length() > 0) drops += ",";
                drops += entry.getKey() + "~" + entry.getValue();
                Item obj = OT.createNewItem(entry.getValue(), false, -1);
                p.addObjet(obj);
                World.addObjet(obj, true);
            }
            Packet.append(drops).append(";");//Drop
            Packet.append(winkamas).append("|");

            SQLManager.UPDATE_PERCO(p);
        }
        return Packet.toString();
    }

    public boolean verifIfTeamIsDead() {
        boolean fini = true;
        for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
            if (entry.getValue().isInvocation()) continue;
            if (!entry.getValue().isDead()) {
                fini = false;
                break;
            }
        }
        return fini;
    }

    public boolean eventFinish() {
        if (getEvent() != null && getEvent().getEventSurvivor() != null) {
            int vivants = 0;
            Player player = null;
            for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
                Fighter fighter = entry.getValue();
                if (fighter.isInvocation() || fighter.getPersonnage() == null) continue;
                if (!fighter.isDead()) {
                    vivants++;
                    player = fighter.getPersonnage();
                }
            }
            if (vivants > 1) {
                return false;
            } else {
                if (player != null)
                    getEvent().getWinners().add(player);
                getEvent().launch();
                return true;
            }
        } else
            return true;
    }

    public void verifIfTeamAllDead() {
        if (_state >= Constant.FIGHT_STATE_FINISHED) return;
        boolean team0 = true;
        boolean team1 = true;
        for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
            if (entry.getValue().isInvocation()) continue;
            if (!entry.getValue().isDead()) {
                team0 = false;
                break;
            }
        }
        for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
            if (entry.getValue().isInvocation()) continue;
            if (!entry.getValue().isDead()) {
                team1 = false;
                break;
            }
        }
        if ((team0 || team1 || !verifyStillInFight()) && eventFinish()) { // Fin du combat
            try {
                if ((this._type == 4) && (this._challenges.size() > 0)) {
                    for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                        if (c.getValue() == null)
                            continue;
                        c.getValue().onFight_end();
                    }
                }
            } catch (Exception e) {
            }
            setTimeStartTurn(0L);
            _state = Constant.FIGHT_STATE_FINISHED;
            int winner = team0 ? 2 : 1;
            if (Config.DEBUG) GameServer.addToLog("L'equipe " + winner + " gagne !");

            //timerController.onFightEnds(this);
            cancelTask();
            //On despawn tous le monde
            _curPlayer = -1;
            for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
            }
            for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
            }
            this._init0.getPersonnage().getMap().removeFight(this._id);

            try {
               this.sendGE(winner); // Envoi des statistiques de fin de combat
            } catch (Exception e) {
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            //SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(this,7,winner);

            for (Entry<Integer, Fighter> entry : _team0.entrySet())//Team joueurs
            {
                Player perso = entry.getValue().getPersonnage();
                if (perso == null) continue;
                perso.set_duelID(-1);
                perso.set_ready(false);
                perso.set_fight(null);
                GameServer.addToLog("Le Personnage " + perso.getName() + " vient de terminer un combat");
            }

            switch (_type)//Team mobs sauf en défi/aggro
            {
                case Constant.FIGHT_TYPE_CHALLENGE://Défie
                    for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
                        Player perso = entry.getValue().getPersonnage();
                        if (perso == null) continue;
                        perso.set_duelID(-1);
                        perso.set_ready(false);
                        perso.set_fight(null);
                        GameServer.addToLog("Le Personnage " + perso.getName() + " vient de terminer un combat");

                    }
                    break;
                case Constant.FIGHT_TYPE_AGRESSION://Aggro
                    for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
                        Player perso = entry.getValue().getPersonnage();
                        if (perso == null) continue;
                        perso.set_duelID(-1);
                        perso.set_ready(false);
                        perso.set_fight(null);
                    }
                    break;
                case Constant.FIGHT_TYPE_CONQUETE: //Conquete prisme
                    for (Entry<Integer, Fighter> entry : get_team1().entrySet()) {
                        Player perso = entry.getValue().getPersonnage();
                        if (perso == null) continue;
                        perso.set_duelID(-1);
                        perso.set_ready(false);
                        perso.set_fight(null);
                    }
                    break;
                case Constant.FIGHT_TYPE_PVM://PvM
                    if (_team1.get(-1) == null) return;
                    break;
            }
            setFightStarted(false);

            GameServer.addToLog(">Un combat vient de se terminer avec succes");
            //on vire les spec du combat
            for (Player perso : _spec.values()) {
                //on remet le perso sur la map
                perso.getMap().addPlayer(perso);
                //SocketManager.GAME_SEND_GV_PACKET(perso);	//Mauvaise ligne apparemment
                perso.refreshMapAfterFight();
            }

            World.getCarte(_map.get_id()).removeFight(_id);
            SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
            _map = null;
            _ordreJeu = null;
            ArrayList<Fighter> winTeam = new ArrayList<Fighter>();
            ArrayList<Fighter> looseTeam = new ArrayList<Fighter>();
            if (team0) {
                looseTeam.addAll(_team0.values());
                winTeam.addAll(_team1.values());
            } else {
                winTeam.addAll(_team0.values());
                looseTeam.addAll(_team1.values());
            }

            //Pour les gagnants, on active les endFight actions
            String str = "";
            if (prism != null)
                str = prism.getCarte() + "|" + prism.getX() + "|" + prism.getY();

            // Trier
            LinkedList<Fighter> winTeamOrder = new LinkedList<Fighter>();
            ArrayList<Fighter> addAtLast = new ArrayList<Fighter>();
            for (Fighter F : winTeam) {
                if (F.getPersonnage() != null && !F.getPersonnage().playerWhoFollowMe.isEmpty()) { // si meneur
                    addAtLast.add(F);
                } else {
                    winTeamOrder.add(F);
                }
            }
            for (Fighter F : addAtLast) {
                winTeamOrder.add(F);
            }

            for (Fighter F : winTeamOrder) {

                if (F._Perco != null) {
                    //On actualise la guilde+Message d'attaque
                    for (Player z : World.getGuild(_guildID).getMembers()) {
                        if (z == null) continue;
                        if (z.isOnline()) {
                            SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
                            SocketManager.GAME_SEND_PERCO_INFOS_PACKET(z, F._Perco, "S");
                            //z.sendBox("ALERTE", Lang.LANG_118[z.getLang()]);
                        }
                    }
                    F._Perco.set_inFight((byte) 0);
                    F._Perco.set_inFightID((byte) -1);
                    for (Player z : World.getCarte((short) F._Perco.get_mapID()).getPersos()) {
                        if (z == null) continue;
                        if (z.getAccount() == null || z.getAccount().getGameThread() == null) continue;
                        SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(z.getAccount().getGameThread().getOut(), z.getMap());
                    }
                }
                if (F._Prisme != null) {
                    for (Player z : World.getOnlinePlayers()) {
                        if (z == null)
                            continue;
                        if (z.get_align() != prism.getalignement())
                            continue;
                        SocketManager.SEND_CS_SURVIVRE_MESSAGE_PRISME(z, str);
                    }
                    F._Prisme.setInFight(-1);
                    F._Prisme.setFightID(-1);
                    for (Player z : World.getCarte((short) F._Prisme.getCarte()).getPersos()) {
                        if (z == null)
                            continue;
                        SocketManager.SEND_GM_PRISME_TO_MAP(z.getAccount().getGameThread().getOut(), z.getMap());
                    }
                }
                if (F.getPersonnage() == null) continue;
                if (F.isInvocation()) continue;
                if (F.hasLeft() && _type != Constant.FIGHT_TYPE_PVT) {
                    F.getPersonnage().warpToSavePos();
                    continue;
                }
                if (!F.getPersonnage().isOnline()) {
                    if (_type != Constant.FIGHT_TYPE_CHALLENGE && _type != Constant.FIGHT_TYPE_PVT) {
                        F.getPersonnage().getMap().applyEndFightAction(_type, F.getPersonnage());
                    }
                    continue;
                }
                if (_type == 2) {
                    if (prism != null)
                        SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(F.getPersonnage(), this.getDefenseurs());
                }
                if (_type != Constant.FIGHT_TYPE_CHALLENGE) {
                    if (F.getPDV() <= 0) {
                        F.getPersonnage().set_PDV(1);
                    } else {
                        F.getPersonnage().set_PDV(F.getPDV());
                    }
                }

                if (_type != Constant.FIGHT_TYPE_CHALLENGE && _type != Constant.FIGHT_TYPE_PVT) {
                    final Maps map = F.getPersonnage().getMap();
                    final int fightType = _type;
                    F.getPersonnage().scheduleEndFighActions(map, fightType);
                    /*if (F.getPersonnage()._Follows != null) {
                        if (!F.getPersonnage()._Follows.playerWhoFollowMe.contains(F.getPersonnage())) { // si c'est pas un suiveur
                            F.getPersonnage().scheduleEndFighActions(map, fightType);
                        }
                    } else {
                        F.getPersonnage().scheduleEndFighActions(map, fightType);
                    }*/
                }

                final Player player = F.getPersonnage();
                GameServer.fightExecutor.schedule(new Runnable() {
                    public void run() {
                        player.mettreCombatBloque(false);
                        if (_type != Constant.FIGHT_TYPE_PVT) {
                            player.refreshMapAfterFight();
                        }
                    }
                }, 200, TimeUnit.MILLISECONDS);
            }
            //Pour les perdant on TP au point de sauvegarde
            for (Fighter F : looseTeam) {

                if (F._Perco != null) { // Défense percepteur
                    F._Perco.set_inFight((byte) 0);
                    _mapOld.RemoveNPC(F._Perco.getGuid());
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_mapOld, F._Perco.getGuid());
                    // Supression percepteur Edited @Flow
                    collector.DelPerco(F._Perco.getGuid());
                    SQLManager.DELETE_PERCO(F._Perco.getGuid());
                    //On actualise la guilde+Message d'attaque
                    for (Player z : World.getGuild(_guildID).getMembers()) {
                        if (z == null) continue;
                        if (z.isOnline()) {
                            SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
                            SocketManager.GAME_SEND_PERCO_INFOS_PACKET(z, F._Perco, "D");
                            //z.sendBox("ALERTE", Lang.LANG_119[z.getLang()]);
                        }
                    }
                }
                if (F._Prisme != null) {
                    org.area.common.World.SubArea subarea = _mapOld.getSubArea();
                    for (Player z : World.getOnlinePlayers()) {
                        if (z == null)
                            continue;
                        if (z.get_align() == 0) {
                            SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subarea.getID() + "|-1|1");
                            continue;
                        }
                        if (z.get_align() == prism.getalignement())
                            SocketManager.SEND_CD_MORT_MESSAGE_PRISME(z, str);
                        SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subarea.getID() + "|-1|0");
                        if (prism.getAreaConquest() != -1) {
                            SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(z, subarea.getArea().getID() + "|-1");
                            subarea.getArea().setPrismeID(0);
                            subarea.getArea().setalignement(0);
                        }
                    }
                    int PrismeID = F._Prisme.getID();
                    subarea.setPrismeID(0);
                    subarea.setalignement(0);
                    _mapOld.RemoveNPC(PrismeID);
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_mapOld, PrismeID);
                    World.removePrisme(PrismeID);
                    SQLManager.DELETE_PRISME(PrismeID);
                }
                if (F.getPersonnage() == null) continue;
                if (F.isInvocation()) continue;
                if ((F.hasLeft() || !F.getPersonnage().isOnline()) && _type != Constant.FIGHT_TYPE_PVT) {
                    F.getPersonnage().warpToSavePos();
                    continue;
                }
                if (F.getPersonnage().controleUneInvocation) {
                    SocketManager.GAME_SEND_SPELL_LIST(F.getPersonnage());
                    F.getPersonnage().controleUneInvocation = false;
                }
                if (_type != Constant.FIGHT_TYPE_CHALLENGE && _type != Constant.FIGHT_TYPE_PVT) {
                    F.getPersonnage().warpToSavePos();
                    F.getPersonnage().set_PDV(1);
                }
                final Player player = F.getPersonnage();
                GameServer.fightExecutor.schedule(new Runnable() {
                    public void run() {
                        if (_type != Constant.FIGHT_TYPE_PVT) {
                            player.refreshMapAfterFight();
                        }
                        player.mettreCombatBloque(false);
                    }
                }, 200, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void sendGE(int winner) {
        /**
         * A l'aide des mêmes addresses IP ont peut déterminer le nombre de Dofus qui fonctionne chez lui et temporiser en fonction de cette quantité.
         * Soit environ 700ms * nb de client ouvert sur la même IP.
         * Les clients sont ensuite classés en ordre de priorité, ceux qui ne nécessite pas de temporisation sont classé en premier.
         * De ce fait, ils n'ont pas à attendre après ceux qui multicompte.
         * Cette vérification ne s'applique qu'en PvM.
         */
        final String packetToSend = this.GetGE(winner);
        final ArrayList<Fighter> F = this.getFighters(7);
        if (_type == 4) { // Temporisation PvM uniquement.
            Map<String, Long> IPTotalTime = new LinkedHashMap<String, Long>();
            ArrayList<Fighter> higherToLowerPriority = new ArrayList<Fighter>();
            ArrayList<Fighter> lowerPriority = new ArrayList<Fighter>();
            for (Fighter perso : F) {
                if (perso.getPersonnage() != null) {
                    String IP = perso.getPersonnage().getAccount().getCurIp();
                    if (!IPTotalTime.containsKey(IP)) {
                        int IPUsed = 0;
                        for (Fighter perso2 : F) {
                            if (perso2 != perso) {
                                if (perso2.getPersonnage() != null) {
                                    if (perso2.getPersonnage().getAccount().getCurIp() == IP) {
                                        IPUsed++;
                                    }
                                }
                            }
                        }
                        if (IPUsed != 0) {
                            long ms = 700 * IPUsed;
                            IPTotalTime.put(IP, ms);
                            lowerPriority.add(perso);
                        } else {
                            higherToLowerPriority.add(perso);
                        }
                    } else {
                        lowerPriority.add(perso);
                    }
                }
            }
            if (!lowerPriority.isEmpty()) {
                for (Fighter f : lowerPriority) {
                    higherToLowerPriority.add(f);
                }
            }
            int timePast = 0;
            String lastIP = "";
            for (Fighter f : higherToLowerPriority) {
                String IP = f.getPersonnage().getAccount().getCurIp();
                if (IPTotalTime.containsKey(IP)) {
                    long time = IPTotalTime.get(IP);
                    if (lastIP != IP) {
                        time -= (timePast - 150); // On temporise un peu plus
                        timePast = 0;
                    }
                    lastIP = IP;
                    if (time > 0) {
                        try {
                            Thread.sleep(time);
                            timePast += time;
                        } catch (Exception e) {
                        }
                    }
                }
                if (!f.hasLeft() && f.getPersonnage() != null && f.getPersonnage().isOnline()) {
                    SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(packetToSend, f);
                }
            }
        } else { // Autre que pvm
            for (Fighter f : F) {
                if (f.getPersonnage() != null) {
                    SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(packetToSend, f);
                }
            }
        }
    }

    public void onFighterDie(Fighter target, Fighter caster) { // Lorsque qu'un personnage meurt
        target.setIsDead(true);
        if (!target.hasLeft()) deadList.put(target.getGUID(), target);//on ajoute le joueur à la liste des cadavres ;)
        setLastFighterDie(target, target.getTeam()); // @Flow - Laisse spirituelle
        if (target.getPersonnage() != null && target == _ordreJeu.get(_curPlayer)) { // Si il meurt pendant son tour
            try {
                SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(this, 7, target.getGUID());
            } catch (Exception e) {
            }
        }
        try { // @Flow - On enlève le combatant de la case
            target.get_fightCell().removeFighter(target);
        } catch (Exception e) {
        }
        SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(this, 7, target.getGUID());
        target.get_fightCell().getFighters().clear();// Supprime tout causait bug si porté/porteur

        if (target.isState(Constant.ETAT_PORTEUR)) {
            Fighter f = target.get_isHolding();
            f.setState(Constant.ETAT_PORTE, 0);
            target.setState(Constant.ETAT_PORTEUR, 0);
            f.set_holdedBy(null);
            target.set_isHolding(null);
            f.get_fightCell().addFighter(f);
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, f.getGUID() + "", f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, target.getGUID() + "", target.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
        }
        // @Flow - Petit fix
        else if (target.isState(8)) {
            Fighter f = target.get_isHolding();
            f.set_fightCell(f.get_fightCell());
            f.get_fightCell().addFighter(f);
            f.setState(8, 0);
            target.setState(3, 0);
            f.set_holdedBy(null);
            target.set_isHolding(null);
        }
        if ((this._type == 4) && (this._challenges.size() > 0)) {
            for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                if (c.getValue() == null)
                    continue;
                c.getValue().onFighter_die(target);
            }
        }


        if (target.getTeam() == 0) {
            TreeMap<Integer, Fighter> team = new TreeMap<Integer, Fighter>();
            team.putAll(_team0);
            for (Entry<Integer, Fighter> entry : team.entrySet()) {
                if (entry.getValue().getInvocator() == null) continue;
                if (entry.getValue().hasLeft()) continue;
                if (entry.getValue().getPDV() == 0) continue;
                if (entry.getValue().isDead()) continue;
                if (entry.getValue().getInvocator().getGUID() == target.getGUID())//si il a été invoqué par le joueur mort
                {
                    onFighterDie(entry.getValue(), caster);

                    int index;
                    try {
                        index = _ordreJeu.indexOf(entry.getValue());
                    } catch (NullPointerException e) {
                        index = -1;
                    }
                    if (index != -1)
                        _ordreJeu.remove(index);

                    if (_team0.containsKey(entry.getValue().getGUID())) _team0.remove(entry.getValue().getGUID());
                    else if (_team1.containsKey(entry.getValue().getGUID())) _team1.remove(entry.getValue().getGUID());
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID() + "", getGTL());
                }
            }
        } else if (target.getTeam() == 1) {
            TreeMap<Integer, Fighter> team = new TreeMap<Integer, Fighter>();
            team.putAll(_team1);
            for (Entry<Integer, Fighter> entry : team.entrySet()) {
                if (entry.getValue().getInvocator() == null) continue;
                if (entry.getValue().getPDV() == 0) continue;
                if (entry.getValue().isDead()) continue;
                if (entry.getValue().hasLeft()) continue;
                if (entry.getValue().getInvocator().getGUID() == target.getGUID())//si il a été invoqué par le joueur mort
                {
                    onFighterDie(entry.getValue(), caster);

                    int index;
                    try {
                        index = _ordreJeu.indexOf(entry.getValue());
                    } catch (NullPointerException e) {
                        index = -1;
                    }
                    if (index != -1) _ordreJeu.remove(index);

                    if (_team0.containsKey(entry.getValue().getGUID())) _team0.remove(entry.getValue().getGUID());
                    else if (_team1.containsKey(entry.getValue().getGUID())) _team1.remove(entry.getValue().getGUID());
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID() + "", getGTL());
                }
            }
        }
        if (target.getMob() != null) {
            try {
                Iterator<?> iterator = getFighters(target.getTeam2()).iterator();
                while (iterator.hasNext()) {
                    Fighter dMob = (Fighter) iterator.next();
                    if (dMob.getPersonnage() != null || dMob.isDead() || dMob.isDouble() || dMob.isHide() || Formulas.getRandomValue(1, 2) != 2) {
                        continue;
                    }
                    int emo = 1;
                    int Chance = Formulas.getRandomValue(1, 5);
                    if (Chance == 2) {
                        switch (Formulas.getRandomValue(1, 9)) {
                            default:
                                continue;
                            case 1:
                                emo = 12;
                                break;
                            case 2:
                                emo = 7;
                                break;
                            case 3:
                                emo = 3;
                                break;
                            case 4:
                                emo = 8;
                                break;
                            case 5:
                                emo = 5;
                                break;
                            case 6:
                                emo = 10;
                                break;
                            case 7:
                                emo = 4;
                                break;
                            case 8:
                                emo = 9;
                                break;
                            case 9:
                                emo = 11;
                                break;
                        }
                        SocketManager.GAME_SEND_EMOTICONE_TO_FIGHT(this, 7, dMob.getGUID(), emo);
                    }
                }
            } catch (Exception exception) {
            }
            //Si c'est une invocation, on la retire de la liste
            try {
                boolean isStatic = false;
                for (int id : Constant.STATIC_INVOCATIONS)
                    if (id == target.getMob().getTemplate().getID()) isStatic = true;
                if (target.isInvocation() && !isStatic) {
                    //Il ne peut plus jouer, et est mort on revient au joueur prï¿½cedent pour que le startTurn passe au suivant
                    if (!target.canPlay() && _ordreJeu.get(_curPlayer).getGUID() == target.getGUID()) {
                        _curPlayer--;
                    }
                    //Il peut jouer, et est mort alors on passe son tour pour que l'autre joue, puis on le supprime de l'index sans problï¿½mes
                    if (target.canPlay() && _ordreJeu.get(_curPlayer).getGUID() == target.getGUID()) {
                        endTurn();
                    }

                    //On ne peut pas supprimer l'index tant que le tour du prochain joueur n'est pas lancï¿½
                    int index;
                    try {
                        index = _ordreJeu.indexOf(target);
                    } catch (NullPointerException e) {
                        index = -1;
                    }
                    //Si le joueur courant a un index plus ï¿½levï¿½, on le diminue pour ï¿½viter le outOfBound
                    if (_curPlayer > index) _curPlayer--;

                    if (index != -1) _ordreJeu.remove(index);


                    if (get_team0().containsKey(target.getGUID())) get_team0().remove(target.getGUID());
                    else if (get_team1().containsKey(target.getGUID())) get_team1().remove(target.getGUID());
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID() + "", getGTL());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ;
        }

        //on supprime les glyphes du joueur
        ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();//Copie du tableau
        glyphs.addAll(_glyphs);
        for (Glyphe g : glyphs) {
            //Si c'est ce joueur qui l'a lancé
            if (g.get_caster().getGUID() == target.getGUID()) {
                SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(this, 7, "-", g.get_cell().getID(), g.get_size(), 4);
                SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(this, 7, g.get_cell().getID());
                _glyphs.remove(g);
            }
        }
        //On supprime les buff lancés par le joueur
        ArrayList<Fighter> tmpTeam = new ArrayList<Fighter>();
        tmpTeam.addAll(_team0.values());
        tmpTeam.addAll(_team1.values());
        for (Fighter ft0 : tmpTeam) {
            if (ft0.isDead() || target.getGUID() == ft0.getGUID()) continue;
            ft0.deleteBuffByFighter(target);
        }
        //on supprime les pieges du joueur
        try {
            synchronized (_traps) {
                for (Piege p : _traps) {
                    if (p.get_caster().getGUID() == target.getGUID()) {
                        p.desappear();
                        _traps.remove(p);
                    }
                }
            }
        } catch (Exception e) {
        }
        try {
            if (target.isPerco()) {
                for (Fighter f : this.getFighters(target.getTeam2())) {
                    if (f.isDead()) continue;
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    this.onFighterDie(f, target);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    verifIfTeamAllDead();
                }
            }
        } catch (NullPointerException e) {
        }
        ;
        try {
            if (target.canPlay() && _ordreJeu.get(_curPlayer) != null)
                if (_ordreJeu.get(_curPlayer).getGUID() == target.getGUID())
                    endTurn();
        } catch (NullPointerException e) {
        }
        ;
       /* try { // @Flow - Tant qu'a faire de la temporisation ^^
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }*/
    }

    public Map<Integer, Fighter> get_team1() {
        return _team1;
    }

    public Map<Integer, Fighter> get_team0() {
        return _team0;
    }

    public int getTeamID(int guid) {
        if (_team0.containsKey(guid))
            return 1;
        if (_team1.containsKey(guid))
            return 2;
        if (_spec.containsKey(guid))
            return 4;
        return -1;
    }

    public int getOtherTeamID(int guid) {
        if (_team0.containsKey(guid))
            return 2;
        if (_team1.containsKey(guid))
            return 1;
        return -1;
    }

    public synchronized void tryCaC(Player perso, int cellID) {
        Fighter caster = getFighterByPerso(perso);

        if (caster == null) return;
        /*if (!caster.canCac) {
            SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
            return;
        }*/
        if (this._type != Constant.FIGHT_TYPE_PVM) {
            perso.sendText("L'utilisation des armes est autorisée uniquement en PvM.");
            SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(perso.getFight(), 7);
            return;
        }
        if (_ordreJeu.get(_curPlayer).getGUID() != caster.getGUID())//Si ce n'est pas a lui de jouer
            return;
        // Pour les challenges, vérif sur CaC
        if ((this._type == 4) && (this._challenges.size() > 0) && !this._ordreJeu.get(this._curPlayer).isInvocation() && !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()) {
            for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                if (c.getValue() == null)
                    continue;
                c.getValue().onPlayer_cac(this._ordreJeu.get(this._curPlayer));
            }
        }
        // @Flow - Fix de j'ai pas de temps à perdre
        Case cellTemp = null;
        for (Fighter f : getAllFighters()) {
            if (f.isDead()) {
                cellTemp = f.get_fightCell();
                if (cellTemp != null) {
                    cellTemp.getFighters().clear();
                }
            }
        }
        for (Fighter f : getAllFighters()) {
            if (!f.isDead() && f.get_holdedBy() == null) {
                f.get_fightCell().addFighter(f);
            }
        }
        // Fin Challenges
        if (perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)//S'il n'a pas de CaC
        {
            if (_curFighterPA < 4)//S'il n'a pas assez de PA
                return;

            SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, perso.getGuid());

            //Si le joueur est invisible
            if (caster.isHide())
                caster.unHide(-1);

            Fighter target = _map.getCase(cellID).getFirstFighter();

            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.getGuid() + "", cellID + "");

            if (target != null) {
                int dmg = Formulas.getRandomJet("1d5+0");
                int finalDommage = Formulas.calculFinalDommage(this, caster, target, Constant.ELEMENT_NEUTRE, dmg, false, true, -1, false);
                finalDommage = SpellEffect.applyOnHitBuffs(finalDommage, target, caster, this, false);//S'il y a des buffs spéciaux

                if (finalDommage > target.getPDV())
                    finalDommage = target.getPDV();//Target va mourir
                target.removePDV(finalDommage);
                finalDommage = -(finalDommage);
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, caster.getGUID() + "", target.getGUID() + "," + finalDommage);
            }
            _curFighterPA -= 4;
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, perso.getGuid() + "", perso.getGuid() + ",-4");
            SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.getGuid());

            if (target.getPDV() <= 0)
                onFighterDie(target, caster);
            verifIfTeamAllDead();
        } else {
            Item arme = perso.getObjetByPos(Constant.ITEM_POS_ARME);

            //Pierre d'âmes = EC
            if (arme.getTemplate(false).getType() == 83) {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.getGuid() + "", "");//Echec Critique Cac
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.getGuid());//Fin de l'action
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                //endTurn();
            }

            int PACost = arme.getTemplate(true).getPACost();

            if (_curFighterPA < PACost)//S'il n'a pas assez de PA
                return;
            if (!Pathfinding.canUseCaConPO(this, _ordreJeu.get(_curPlayer).get_fightCell().getID(), cellID, arme)) {
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.getGuid());
                return;
            }


            SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, perso.getGuid());

            boolean isEc = arme.getTemplate(false).getTauxEC() != 0 && Formulas.getRandomValue(1, arme.getTemplate(false).getTauxEC()) == arme.getTemplate(false).getTauxEC();
            if (isEc) {
                if (Config.DEBUG) GameServer.addToLog(perso.getName() + " Echec critique sur le CaC ");
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.getGuid() + "", "");//Echec Critique Cac
                _curFighterPA -= PACost;
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, perso.getGuid() + "", perso.getGuid() + ",-" + PACost);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.getGuid());//Fin de l'action
                caster.canCac = false;
                perso.sendText("Échec critique : Vous ne pouvez plus utiliser votre arme pour le tour actuel !");
                //endTurn(); // On ne met plus fin au tour en cas d'EC
            } else {
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.getGuid() + "", cellID + "");
                boolean isCC = caster.testIfCC(arme.getTemplate(false).getTauxCC());
                if (isCC) {
                    if (Config.DEBUG) GameServer.addToLog(perso.getName() + " Coup critique sur le CaC");
                    SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, perso.getGuid() + "", "0");
                }

                //Si le joueur est invisible
                if (caster.isHide()) caster.unHide(-1);

                ArrayList<SpellEffect> effets = arme.getEffects();
                if (isCC) {
                    effets = arme.getCritEffects();
                }
                for (SpellEffect SE : effets) {
                    if (_state != Constant.FIGHT_STATE_ACTIVE) break;
                    ArrayList<Fighter> cibles = Pathfinding.getCiblesByZoneByWeapon(this, arme.getTemplate(false).getType(), _map.getCase(cellID), caster.get_fightCell().getID());
                    SE.setTurn(0);
                    /**if (caster.hasBuff(9)){
                     if (SE.getEffectID() == 90){
                     SocketManager.GAME_SEND_MESSAGE(caster.getPersonnage(), "Sort innutilisable quand dérobage est actif !", Manager.CONFIG_MOTD_COLOR);
                     }
                     }**/
                    SE.applyToFight(this, caster, cibles, true);
                }
                /**7172 Baguette Rhon
                 * 7156 Marteau Ronton
                 * 1355 Arc Hidsad
                 * 7182 Racine Hécouanone
                 * 7040 Arc de Kuri
                 * 6539 Pelle Gicque
                 * 6519 Baguette de Kouartz
                 * 8118 Baguette du Scarabosse Doré
                 */
                int idArme = arme.getTemplate(true).getID();
                int basePdvSoin = 1;
                int pdvSoin = -1;
                if (idArme == 7172 || idArme == 7156 || idArme == 1355 || idArme == 7182
                        || idArme == 7040 || idArme == 6539 || idArme == 6519 || idArme == 8118) {
                    pdvSoin = Constant.getArmeSoin(idArme);
                    if (pdvSoin != -1) {
                        if (isCC) {
                            basePdvSoin = basePdvSoin + arme.getTemplate(true).getBonusCC();
                            pdvSoin = pdvSoin + arme.getTemplate(true).getBonusCC();
                        }
                        int intel = perso.get_baseStats().getEffect(Constant.STATS_ADD_INTE) + perso.getStuffStats().getEffect(Constant.STATS_ADD_INTE) + perso.getDonsStats().getEffect(Constant.STATS_ADD_INTE) + perso.getBuffsStats().getEffect(Constant.STATS_ADD_INTE);
                        int soins = perso.get_baseStats().getEffect(Constant.STATS_ADD_SOIN) + perso.getStuffStats().getEffect(Constant.STATS_ADD_SOIN) + perso.getDonsStats().getEffect(Constant.STATS_ADD_SOIN) + perso.getBuffsStats().getEffect(Constant.STATS_ADD_SOIN);
                        int minSoin = basePdvSoin * (100 + intel) / 100 + soins;
                        int maxSoin = pdvSoin * (100 + intel) / 100 + soins;
                        int finalSoin = Formulas.getRandomValue(minSoin, maxSoin);
                        Fighter target = _map.getCase(cellID).getFirstFighter();
                        if ((finalSoin + target.getPDV()) > target.getPDVMAX())
                            finalSoin = target.getPDVMAX() - target.getPDV();//Target va mourrir
                        target.removePDV(-finalSoin);
                        SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, target.getGUID() + "", target.getGUID() + ",+" + finalSoin);
                    }
                }
                _curFighterPA -= PACost;
                SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, perso.getGuid() + "", perso.getGuid() + ",-" + PACost);
                SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.getGuid());
                verifIfTeamAllDead();
            }
        }
    }

    public Fighter getFighterByPerso(Player perso) {
        Fighter fighter = null;
        if (_team0.get(perso.getGuid()) != null)
            fighter = _team0.get(perso.getGuid());
        if (_team1.get(perso.getGuid()) != null)
            fighter = _team1.get(perso.getGuid());
        return fighter;
    }

    public Fighter getCurFighter() {
        return _ordreJeu.get(_curPlayer);
    }

    public void refreshCurPlayerInfos() {
        _curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PA) - _curFighterUsedPA;
        _curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PM) - _curFighterUsedPM;
    }

    /*public boolean reconnexion(Player perso) // @Flow #Old
	{
		Fighter f = getFighterByPerso(perso);
		if(f == null) return false;
		if(get_state() == Constant.FIGHT_STATE_INIT)
			return false;
		f.Reconnect();
		if(get_state() == Constant.FIGHT_STATE_FINISHED)
			return false;
		//Si combat en cours on envois des im
		SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, new StringBuilder("1184;").append(f.getPacketsName()).toString());
		try
		{
			Thread.sleep(200);
		}
		catch(Exception e){}
		if(get_state() == Constant.FIGHT_STATE_ACTIVE) SocketManager.GAME_SEND_GJK_PACKET(perso,get_state(),0,0,0,0,get_type());//Join Fight => getState(), pas d'anulation...
		else
		{
			if(get_type() == Constant.FIGHT_TYPE_CHALLENGE)
			{
					SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,0,get_type());
			}else
			{
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,0,1,0,0,get_type());
			}
		}
		try
		{
			Thread.sleep(200);
		}
		catch(Exception e){}
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(perso, get_map(), (f.getTeam()==0?_init0:_init1).getGUID(), f);//Indication de la team
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(perso, f);
		SocketManager.GAME_SEND_STATS_PACKET(perso);
		try
		{
			Thread.sleep(1500);
		}
		catch(Exception e){}
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, get_map(), perso);
		try
		{
			Thread.sleep(1500);
		}
		catch(Exception e){}
		if(get_state() == Constant.FIGHT_STATE_PLACE)
		{
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), get_map().get_placesStr(), _st1);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid()+"", perso.getGuid()+","+Constant.ETAT_PORTE+",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.getGuid()+"", perso.getGuid()+","+Constant.ETAT_PORTEUR+",0");
		}else
		{
			SocketManager.GAME_SEND_GS_PACKET(perso);//Début du jeu
			SocketManager.GAME_SEND_GTL_PACKET(perso,this);//Liste des tours
			SocketManager.GAME_SEND_GAMETURNSTART_PACKET(perso,_ordreJeu.get(_curPlayer).getGUID(),Constant.TIME_BY_TURN);
		   	for(Fighter f1 : getFighters(3))
		   	{
		   	  f1.sendState(perso);
		   	}
		}
		SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		return true;
	}

	*/
    public boolean reconnexion(final Player perso) {
        final Fighter f = getFighterByPerso(perso);
        if (f == null) return false;
        if (_state == Constant.FIGHT_STATE_INIT) return false;
        f.Reconnect();
        if (_state == Constant.FIGHT_STATE_FINISHED) return false;
        //Si combat en cours on envois des im
        SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, new StringBuilder("1184;").append(f.getPacketsName()).toString());
        final Fight fight = this;
        GameServer.fightExecutor.schedule(new Runnable() {
            public void run() {

                if (_state == Constant.FIGHT_STATE_ACTIVE)
                    SocketManager.GAME_SEND_GJK_PACKET(perso, _state, 0, 0, 0, getRemaimingTime(), _type);//Join Fight => _state, pas d'anulation...
                else {
                    if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
                        if (perso.getArena() != -1 || (perso.getKolizeum() != null && perso.getKolizeum().isStarted()))
                            SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
                        else
                            SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, getRemaimingTime(), _type);
                    } else {
                        SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, getRemaimingTime(), _type);
                    }
                }
                GameServer.fightExecutor.schedule(new Runnable() {
                    public void run() {
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(perso, _map, (f.getTeam() == 0 ? _init0 : _init1).getGUID(), f);//Indication de la team
                        SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(perso, f);
                        SocketManager.GAME_SEND_STATS_PACKET(perso);
                        GameServer.fightExecutor.schedule(new Runnable() {
                            public void run() {
                                SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(fight, _map, perso);
                                GameServer.fightExecutor.schedule(new Runnable() {
                                    public void run() {
                                        if (_state == Constant.FIGHT_STATE_PLACE) {
                                            SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.getAccount().getGameThread().getOut(), _map.get_placesStr(), _st1);
                                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTE + ",0");
                                            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 3, 950, perso.getGuid() + "", perso.getGuid() + "," + Constant.ETAT_PORTEUR + ",0");
                                        } else {
                                            SocketManager.GAME_SEND_GS_PACKET(perso);//Début du jeu
                                            SocketManager.GAME_SEND_GTL_PACKET(perso, fight);//Liste des tours
                                            SocketManager.GAME_SEND_GAMETURNSTART_PACKET(perso, _ordreJeu.get(_curPlayer).getGUID(), (int) getRemaimingTime(), _ordreJeu.get(_curPlayer).getGUID());
                                            for (Entry<Integer, Challenge> c : fight._challenges.entrySet()) {
                                                if (c.getValue() == null)
                                                    continue;
                                                c.getValue().onFight_start();
                                                SocketManager.send(perso, c.getValue().parseToPacket());
                                            }
                                            for (Fighter f1 : getFighters(3)) {
                                                f1.sendState(perso);
                                            }
                                        }
                                        SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
                                    }
                                }, 1500, TimeUnit.MILLISECONDS);
                            }
                        }, 1500, TimeUnit.MILLISECONDS);
                    }
                }, 200, TimeUnit.MILLISECONDS);
            }
        }, 200, TimeUnit.MILLISECONDS);
        return true;
    }

    public boolean deconnexion(Player perso, boolean verif)//True si entre en mode déconnexion en combat, false sinon
    {
        Fighter f = getFighterByPerso(perso);
        if (f == null) return false;
        if (_state == Constant.FIGHT_STATE_INIT || _state == Constant.FIGHT_STATE_FINISHED) {
            if (!verif)
                leftFight(perso, null, false);
            return false;
        }
        if (f.getNBDeco() >= 5) {
            if (!verif) {
                leftFight(perso, null, false);
                for (Fighter e : this.getFighters(7)) {
                    if (e.getPersonnage() == null || e.getPersonnage().isOnline() == false) continue;
                    SocketManager.GAME_SEND_MESSAGE(e.getPersonnage(), f.getPacketsName() + " " + Lang.LANG_113[e.getPersonnage().getLang()], "A00000");
                }
            }
            return false;
        }
        if (!verif) {
            if (isFightStarted() == false) {
                perso.set_ready(true);
                perso.getFight().verifIfAllReady();
                SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(perso.getFight(), 3, perso.getGuid(), true);
            }
        }
        if (!verif) {
            f.Deconnect();
            //Si combat en cours on envois des im
            SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, new StringBuilder("1182;").append(f.getPacketsName()).append("~").append(f.getToursRestants()).toString());
        }
        return true;
    }

    public void leftFight(Player perso, Player target, boolean isDebug) //@Flow - À vérifier
    {
        if (perso == null || _ordreJeu == null || _curPlayer < 0) return; // @Flow
        if (_curPlayer >= _ordreJeu.size()) _curPlayer = 0; // Tout simple
        Fighter F = this.getFighterByPerso(perso);
        Fighter T = null;
        if (target != null) T = this.getFighterByPerso(target);
        //cancelTask(); // Logique ? 0.o
        if (Config.DEBUG) {
            if (target != null && T != null) {
                GameServer.addToLog(perso.getName() + " expulse " + T.getPersonnage().getName());
            } else {
                GameServer.addToLog(perso.getName() + " a quitter le combat");
            }
        }
        if (F != null && !F.hasLeft()) {
            if (!F.getPersonnage().playerWhoFollowMe.isEmpty()) {
                F.getPersonnage().playerWhoFollowMe.clear();
            }
            F.getPersonnage().mettreCombatBloque(false);
            if (F.getPersonnage().controleUneInvocation) {
                SocketManager.GAME_SEND_SPELL_LIST(F.getPersonnage());
                F.getPersonnage().controleUneInvocation = false;
            }
            switch (_type) {
                case Constant.FIGHT_TYPE_CHALLENGE://Défie
                case Constant.FIGHT_TYPE_AGRESSION://PVP
                case Constant.FIGHT_TYPE_PVM://PVM
                case Constant.FIGHT_TYPE_PVT://Perco
                case Constant.FIGHT_TYPE_CONQUETE://Prismes

                    if (_state >= Constant.FIGHT_STATE_ACTIVE) {
                        onFighterDie(F, F);
                        boolean StillInFight = false;
                        if (_type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_CONQUETE) {
                            StillInFight = verifyStillInFightTeam(F.getGUID());

                        } else {
                            StillInFight = verifyStillInFight();
                        }

                        if (!StillInFight)//S'arrête ici si il ne reste plus personne dans le combat et dans la team
                        {
                            //Met fin au combat
                            verifIfTeamAllDead();
                        } else {
                            F.setLeft(true);
                            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());

                            final Player P = F.getPersonnage();
                            P.set_duelID(-1);
                            P.set_ready(false);
                            P.fullPDV();
                            P.set_fight(null);
                            P.setSitted(false);
                            P.set_away(false);

                            if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE || _type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_PVT) {
								/*int EnergyLoos = Formulas.getLoosEnergy(P.get_lvl(), _type==1, _type==5);
								int Energy = P.get_energy() - EnergyLoos;
								if(Energy < 0) Energy = 0;
								P.set_energy(Energy);
								if(P.isOnline())
									SocketManager.GAME_SEND_Im_PACKET(P, "034;"+EnergyLoos);

								*/
                                if (_type == Constant.FIGHT_TYPE_AGRESSION) {
                                    if (isDebug)
                                        return;
                                    int honor = P.get_honor() - 500;
                                    if (honor < 0) honor = 0;
                                    P.set_honor(honor);
                                    if (P.isOnline())
                                        SocketManager.GAME_SEND_Im_PACKET(P, "076;" + honor);
                                }

                                //On le supprime de la team
                                if (_type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_CONQUETE) {
                                    if (_team0.containsKey(F.getGUID())) {
                                        F._cell.removeFighter(F);
                                        _team0.remove(F.getGUID());
                                    } else if (_team1.containsKey(F.getGUID())) {
                                        F._cell.removeFighter(F);
                                        _team1.remove(F.getGUID());
                                    }
                                }
								/*if(Energy == 0)
								{
									P.set_Ghosts();
								}else
								{*/
                                if (P.percoDefendre != null && P.percoDefendre.get_fight() == this) { // Il s'agit d'une défense percepteur
                                    P.teleport(P.getLastMapID(), P.getLastCellID());
                                    P.percoDefendre = null;
                                } else {
                                    P.warpToSavePos();
                                }
                                P.set_PDV(1);
                                // }
                            }

                            if (P.isOnline()) {
                                GameServer.fightExecutor.schedule(new Runnable() {
                                    public void run() {
                                        SocketManager.GAME_SEND_GV_PACKET(P);
                                        P.refreshMapAfterFight();
                                    }
                                }, 500, TimeUnit.MILLISECONDS);
                            }

                            //si c'était a son tour de jouer
                            if (_ordreJeu == null || _ordreJeu.size() <= _curPlayer) return;
                            if (_ordreJeu.get(_curPlayer) == null) return;
                            if (_ordreJeu.get(_curPlayer).getGUID() == F.getGUID()) {
                                endTurn();
                            }
                        }
                    } else if (_state == Constant.FIGHT_STATE_PLACE) {
                        boolean isValid1 = false;
                        if (T != null) {
                            if (_init0 != null && _init0.getPersonnage() != null) {
                                if (F.getPersonnage().getGuid() == _init0.getPersonnage().getGuid()) {
                                    isValid1 = true;
                                }
                            }
                            if (_init1 != null && _init1.getPersonnage() != null) {
                                if (F.getPersonnage().getGuid() == _init1.getPersonnage().getGuid()) {
                                    isValid1 = true;
                                }
                            }
                        }

                        if (isValid1)//Celui qui fait l'action a lancer le combat et leave un autre personnage
                        {
                            if ((T.getTeam() == F.getTeam()) && (T.getGUID() != F.getGUID())) {
                                if (Config.DEBUG) Console.print("EXLUSION DE : " + T.getPersonnage().getName());
                                SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, T.getPersonnage().getGuid(), getTeamID(T.getGUID()));
                                if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_PVT)
                                    SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, T.getPersonnage().getGuid(), getOtherTeamID(T.getGUID()));
                                final Player P = T.getPersonnage();
                                P.set_duelID(-1);
                                P.set_ready(false);
                                P.fullPDV();
                                P.set_fight(null);
                                P.setSitted(false);
                                P.set_away(false);

                                if (P.isOnline()) {
                                    GameServer.fightExecutor.schedule(new Runnable() {
                                        public void run() {
                                            SocketManager.GAME_SEND_GV_PACKET(P);
                                            P.refreshMapAfterFight();
                                        }
                                    }, 500, TimeUnit.MILLISECONDS);
                                }

                                //On le supprime de la team
                                if (_team0.containsKey(T.getGUID())) {
                                    T._cell.removeFighter(T);
                                    _team0.remove(T.getGUID());
                                } else if (_team1.containsKey(T.getGUID())) {
                                    T._cell.removeFighter(T);
                                    _team1.remove(T.getGUID());
                                }
                                for (Player z : _mapOld.getPersos()) FightStateAddFlag(this._mapOld, z);
                            }
                        } else if (T == null)//Il leave de son plein gré donc (T = null)
                        {
                            boolean isValid2 = false;
                            if (_init0 != null && _init0.getPersonnage() != null) {
                                if (F.getPersonnage().getGuid() == _init0.getPersonnage().getGuid()) {
                                    isValid2 = true;
                                }
                            }
                            if (_init1 != null && _init1.getPersonnage() != null) {
                                if (F.getPersonnage().getGuid() == _init1.getPersonnage().getGuid()) {
                                    isValid2 = true;
                                }
                            }

                            if (isValid2)//Soit il a lancer le combat => annulation du combat
                            {
                                for (Fighter f : this.getFighters(F.getTeam2())) {
                                    final Player P = f.getPersonnage();
                                    P.set_duelID(-1);
                                    P.set_ready(false);
                                    P.fullPDV();
                                    P.set_fight(null);
                                    P.setSitted(false);
                                    P.set_away(false);

                                    if (F.getPersonnage().getGuid() != f.getPersonnage().getGuid())//Celui qui a join le fight revient sur la map
                                    {
                                        if (P.isOnline()) {
                                            GameServer.fightExecutor.schedule(new Runnable() {
                                                public void run() {
                                                    SocketManager.GAME_SEND_GV_PACKET(P);
                                                    P.refreshMapAfterFight();
                                                }
                                            }, 200, TimeUnit.MILLISECONDS);
                                        }
                                    } else//Celui qui a fait le fight meurt + perte honor
                                    {
                                        if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE) {
                                            startFight();
                                            onFighterDie(F, F);
                                            verifIfTeamAllDead();
                                            F.setLeft(true);
                                            //si le combat n'est pas terminé
                                            if (_state == Constant.FIGHT_STATE_ACTIVE) {
                                                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());
                                                if (perso.isOnline()) {
                                                    SocketManager.GAME_SEND_GV_PACKET(perso);
                                                }
                                                //si c'était a son tour de jouer
                                                if (_ordreJeu.get(_curPlayer) == null) return;
                                                if (_ordreJeu.get(_curPlayer).getGUID() == F.getGUID()) {
                                                    endTurn();
                                                }
                                            }
                                        } else if (_type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_PVT) {
											/*int EnergyLoos = Formulas.getLoosEnergy(P.get_lvl(), _type==1, _type==5);
											int Energy = P.get_energy() - EnergyLoos;
											if(Energy < 0) Energy = 0;
											P.set_energy(Energy);
											if(P.isOnline())
												SocketManager.GAME_SEND_Im_PACKET(P, "034;"+EnergyLoos);

											*/
											/*if(Energy == 0)
											{
												P.set_Ghosts();
											}else
											{*/
                                            P.warpToSavePos();
                                            P.set_PDV(1);
                                            //}
                                        }

                                        if (P.isOnline()) {
                                            GameServer.fightExecutor.schedule(new Runnable() {
                                                public void run() {
                                                    SocketManager.GAME_SEND_GV_PACKET(P);
                                                    P.refreshMapAfterFight();
                                                }
                                            }, 200, TimeUnit.MILLISECONDS);
                                        }
                                    }
                                }
                                //timerController.onFightEnds(this);
                                cancelTask();
                                if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE || _type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_PVT) {
                                    for (Fighter f : this.getFighters(F.getOtherTeam())) {
                                        if (f.getPersonnage() == null) continue;
                                        final Player P = f.getPersonnage();
                                        P.set_duelID(-1);
                                        P.set_ready(false);
                                        P.fullPDV();
                                        P.set_fight(null);
                                        P.setSitted(false);
                                        P.set_away(false);

                                        if (P.isOnline()) {
                                            GameServer.fightExecutor.schedule(new Runnable() {
                                                public void run() {
                                                    SocketManager.GAME_SEND_GV_PACKET(P);
                                                    P.refreshMapAfterFight();
                                                }
                                            }, 200, TimeUnit.MILLISECONDS);
                                        }
                                    }
                                }
                                _state = 4;//Nous assure de ne pas démarrer le combat
                                if (_map == null) return;
                                World.getCarte(_map.get_id()).removeFight(_id);
                                SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
                                SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(this._mapOld, _init0.getGUID());
                                if (_type == Constant.FIGHT_TYPE_CONQUETE) {
                                    String str = prism.getCarte() + "|" + prism.getX() + "|" + prism.getY();
                                    for (Player z : World.getOnlinePlayers()) {
                                        if (z == null)
                                            continue;
                                        if (z.get_align() != prism.getalignement())
                                            continue;
                                        SocketManager.SEND_CS_SURVIVRE_MESSAGE_PRISME(z, str);
                                    }
                                    prism.setInFight(-1);
                                    prism.setFightID(-1);
                                    if (perso != null) {
                                        if (prism != null)
                                            SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(perso, getDefenseurs());
                                    }
                                    for (Player z : World.getCarte((short) prism.getCarte()).getPersos()) {
                                        if (z == null)
                                            continue;
                                        SocketManager.SEND_GM_PRISME_TO_MAP(z.getAccount().getGameThread().getOut(),
                                                z.getMap());
                                    }
                                }
                                if (_type == Constant.FIGHT_TYPE_PVT) {
                                    //On actualise la guilde+Message d'attaque
                                    for (Player z : World.getGuild(_guildID).getMembers()) {
                                        if (z == null) continue;
                                        if (z.isOnline()) {
                                            SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
                                            //z.sendBox("ALERTE", Lang.LANG_118[z.getLang()]);
                                        }
                                    }
                                    collector.set_inFight((byte) 0);
                                    collector.set_inFightID((byte) -1);
                                    for (Player z : World.getCarte((short) collector.get_mapID()).getPersos()) {
                                        if (z == null) continue;
                                        if (z.getAccount() == null || z.getAccount().getGameThread() == null) continue;
                                        SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(z.getAccount().getGameThread().getOut(), z.getMap());
                                    }
                                }
                                if (_type == Constant.FIGHT_TYPE_PVM) {
                                    int align = -1;
                                    if (_team1.size() > 0) {
                                        _team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
                                    }
                                    //Si groupe non fixe
                                    if (!_mobGroup.isFix() && !_mobGroup.haveSpawnTime())
                                        World.getCarte(_map.get_id()).spawnGroup(align, 1, true, _mobGroup.getCellID());//Respawn d'un groupe
                                }
                                _map = null;
                                _ordreJeu = null;
                            } else//Soit il a rejoin le combat => Left de lui seul
                            {
                                SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, F.getPersonnage().getGuid(), getTeamID(F.getGUID()));
                                if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE || _type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_PVT)
                                    SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, F.getPersonnage().getGuid(), getOtherTeamID(F.getGUID()));
                                final Player P = F.getPersonnage();
                                P.set_duelID(-1);
                                P.set_ready(false);
                                P.fullPDV();
                                P.set_fight(null);
                                P.setSitted(false);
                                P.set_away(false);

                                if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE || _type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_PVT) {
									/*int EnergyLoos = Formulas.getLoosEnergy(P.get_lvl(), _type==1, _type==5);
									int Energy = P.get_energy() - EnergyLoos;
									if(Energy < 0) Energy = 0;
									P.set_energy(Energy);
									if(P.isOnline())
										SocketManager.GAME_SEND_Im_PACKET(P, "034;"+EnergyLoos);*/

                                    if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE) {
                                        if (isDebug)
                                            return; //@Flow c'est quoi ça ? #OnGarde
                                        int honor = P.get_honor() - 500;
                                        if (honor < 0) honor = 0;
                                        P.set_honor(honor);
                                        if (P.isOnline())
                                            SocketManager.GAME_SEND_Im_PACKET(P, "076;" + honor);
                                    }
									/*if(Energy == 0)
									{
										P.set_Ghosts();
									}else
									{*/
                                    P.warpToSavePos();
                                    P.set_PDV(1);
                                    //}
                                }

                                if (P.isOnline()) {
                                    GameServer.fightExecutor.schedule(new Runnable() {
                                        public void run() {
                                            SocketManager.GAME_SEND_GV_PACKET(P);
                                            P.refreshMapAfterFight();
                                        }
                                    }, 200, TimeUnit.MILLISECONDS);
                                }

                                //On le supprime de la team
                                if (_team0.containsKey(F.getGUID())) {
                                    F._cell.removeFighter(F);
                                    _team0.remove(F.getGUID());
                                } else if (_team1.containsKey(F.getGUID())) {
                                    F._cell.removeFighter(F);
                                    _team1.remove(F.getGUID());
                                }
                                for (Player z : _mapOld.getPersos()) FightStateAddFlag(this._mapOld, z);
                            }
                        }
                    } else {
                        if (Config.DEBUG)
                            GameServer.addToLog("Phase de combat non geree, type de combat:" + _type + " T:" + T + " F:" + F);
                    }
                    break;
                default:
                    if (Config.DEBUG)
                        GameServer.addToLog("Type de combat non geree, type de combat:" + _type + " T:" + T + " F:" + F);
                    break;
            }
        } else//Si perso en spec
        {
            SocketManager.GAME_SEND_GV_PACKET(perso);
            _spec.remove(perso.getGuid());
            perso.setSitted(false);
            perso.set_fight(null);
            perso.set_away(false);
        }
        if (T != null && fightIsStarted) {
            try {
                Set<Entry<Integer, Fighter>> copy0 = _team0.entrySet();
                Set<Entry<Integer, Fighter>> copy1 = _team0.entrySet();
                for (Entry<Integer, Fighter> en : copy0) {
                    if (en.getValue().equals(F)) {
                        _team0.remove(en.getKey());
                        break;
                    }
                }
                for (Entry<Integer, Fighter> en : copy1) {
                    if (en.getValue().equals(F)) {
                        _team1.remove(en.getKey());
                        break;
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public String getGTL() //@Flow - Mais ou était passé les boucles ? ^^
    {
        String packet = "GTL";
        if (get_ordreJeu() != null) {//**
            for (Fighter f : get_ordreJeu()) {//*
                packet += "|" + f.getGUID();
            }//*
        }//**
        return packet + (char) 0x00;
    }

    public int getNextLowerFighterGuid() {
        int g = -1;
        for (Fighter f : getFighters(3)) {
            if (f.getGUID() < g)
                g = f.getGUID();
        }
        g--;
        return g;
    }

    public void addFighterInTeam(Fighter f, int team) {
        if (team == 0)
            _team0.put(f.getGUID(), f);
        else if (team == 1)
            _team1.put(f.getGUID(), f);
    }

    public String parseFightInfos() {
        StringBuilder infos = new StringBuilder();
        infos.append(_id).append(";");
        long time = System.nanoTime() - get_startTime();
        infos.append((get_startTime() == 0 ? "-1" : time)).append(";");
        //Team1
        infos.append("0,");//0 car toujours joueur :)
        switch (_type) {
            case Constant.FIGHT_TYPE_CHALLENGE:
                infos.append("0,");
                infos.append(_team0.size()).append(";");
                //Team2
                infos.append("0,");
                infos.append("0,");
                infos.append(_team1.size()).append(";");
                break;

            case Constant.FIGHT_TYPE_AGRESSION:
                infos.append(_init0.getPersonnage().get_align()).append(",");
                infos.append(_team0.size()).append(";");
                //Team2
                infos.append("0,");
                infos.append(_init1.getPersonnage().get_align()).append(",");
                infos.append(_team1.size()).append(";");
                break;

            case Constant.FIGHT_TYPE_PVM:
                infos.append("0,");
                infos.append(_team0.size()).append(";");
                //Team2
                infos.append("1,");
                infos.append(_team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign()).append(",");
                infos.append(_team1.size()).append(";");
                break;

            case Constant.FIGHT_TYPE_PVT:
                infos.append("0,");
                infos.append(_team0.size()).append(";");
                //Team2
                infos.append("4,");
                infos.append("0,");
                infos.append(_team1.size()).append(";");
                break;
            case Constant.FIGHT_TYPE_CONQUETE:
                infos.append(get_init1().getPersonnage().get_align() + ",");
                infos.append(_team0.size()).append(";");
                infos.append("0,");
                infos.append(prism.getalignement() + ",");
                infos.append(_team1.size() + ";");
                break;
        }
        return infos.toString();
    }

    public Fighter get_init1() {
        return _init1;
    }

    public void showCaseToTeam(int guid, int cellID) {
        int teams = getTeamID(guid) - 1;
        if (teams == 4) return;//Les spectateurs ne montrent pas
        ArrayList<GameSendThread> PWs = new ArrayList<GameSendThread>();
        if (teams == 0) {
            for (Entry<Integer, Fighter> e : _team0.entrySet()) {
                if (e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getGameThread() != null)
                    PWs.add(e.getValue().getPersonnage().getAccount().getGameThread().getOut());
            }
        } else if (teams == 1) {
            for (Entry<Integer, Fighter> e : _team1.entrySet()) {
                if (e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getGameThread() != null)
                    PWs.add(e.getValue().getPersonnage().getAccount().getGameThread().getOut());
            }
        }
        SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
    }

    public void showCaseToAll(int guid, int cellID) {
        ArrayList<GameSendThread> PWs = new ArrayList<GameSendThread>();
        for (Entry<Integer, Fighter> e : _team0.entrySet()) {
            if (e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getGameThread() != null)
                PWs.add(e.getValue().getPersonnage().getAccount().getGameThread().getOut());
        }
        for (Entry<Integer, Fighter> e : _team1.entrySet()) {
            if (e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getGameThread() != null)
                PWs.add(e.getValue().getPersonnage().getAccount().getGameThread().getOut());
        }
        for (Entry<Integer, Player> e : _spec.entrySet()) {
            PWs.add(e.getValue().getAccount().getGameThread().getOut());
        }
        SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
    }

    public void joinAsSpect(Player p) {
        if (p.getFight() != null)//Le mec tente de nous arnaquer
        {
            return;
        }
        if (!specOk || _state != Constant.FIGHT_STATE_ACTIVE) {
            SocketManager.GAME_SEND_Im_PACKET(p, "157");
            return;
        }
        p.get_curCell().removePlayer(p.getGuid());
        SocketManager.GAME_SEND_GJK_PACKET(p, _state, 0, 0, 1, getRemaimingTime(), _type);
        SocketManager.GAME_SEND_GS_PACKET(p);
        SocketManager.GAME_SEND_GTL_PACKET(p, this);
        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.getMap(), p.getGuid());
        SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, p);
        SocketManager.GAME_SEND_GAMETURNSTART_PACKET(p, _ordreJeu.get(_curPlayer).getGUID(), Constant.TIME_BY_TURN, _ordreJeu.get(_curPlayer).getGUID());
        _spec.put(p.getGuid(), p);
        p.set_fight(this);
        SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "036;" + p.getName());
        if ((this._type == Constant.FIGHT_TYPE_PVM) && (this._challenges.size() > 0)) {
            for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
                if (c.getValue() == null)
                    continue;
                SocketManager.send(p, c.getValue().parseToPacket());
                if (!c.getValue().getAlive()) {
                    if (c.getValue().get_win()) SocketManager.send(p, "GdOK" + c.getValue().getType());
                    else SocketManager.send(p, "GdKO" + c.getValue().getType());
                }
            }
        }

    }

    public boolean verifyStillInFight()//Return true si au moins un joueur est encore dans le combat
    {
        for (Fighter f : _team0.values()) {
            if (f.isPerco()) return true;
            if (f.isInvocation()
                    || f.isDead()
                    || f.getPersonnage() == null
                    || f.getMob() != null
                    || f._double != null
                    || f.hasLeft()) {
                continue;
            }
            if (f.getPersonnage() != null && f.getPersonnage().getFight() != null
                    && f.getPersonnage().getFight().get_id() == this.get_id()) //Si il n'est plus dans ce combat
            {
                return true;
            }
        }
        for (Fighter f : _team1.values()) {
            if (f.isPerco()) return true;
            if (f.isInvocation()
                    || f.isDead()
                    || f.getPersonnage() == null
                    || f.getMob() != null
                    || f._double != null
                    || f.hasLeft()) {
                continue;
            }
            if (f.getPersonnage() != null && f.getPersonnage().getFight() != null
                    && f.getPersonnage().getFight().get_id() == this.get_id()) //Si il n'est plus dans ce combat
            {
                return true;
            }
        }

        return false;
    }

    public boolean verifyStillInFightTeam(int guid)//Return true si au moins un joueur est encore dans la team
    {
        if (_team0.containsKey(guid)) {
            for (Fighter f : _team0.values()) {
                if (f.isPerco()) return true;
                if (f.isInvocation()
                        || f.isDead()
                        || f.getPersonnage() == null
                        || f.getMob() != null
                        || f._double != null
                        || f.hasLeft()) {
                    continue;
                }
                if (f.getPersonnage() != null && f.getPersonnage().getFight() != null
                        && f.getPersonnage().getFight().get_id() == this.get_id()) //Si il n'est plus dans ce combat
                {
                    return true;
                }
            }
        } else if (_team1.containsKey(guid)) {
            for (Fighter f : _team1.values()) {
                if (f.isPerco()) return true;
                if (!f.isInvocation()
                        || f.isDead()
                        || f.getPersonnage() == null
                        || f.getMob() != null
                        || f._double != null
                        || f.hasLeft()) {
                    continue;
                }
                if (f.getPersonnage() != null && f.getPersonnage().getFight() != null
                        && f.getPersonnage().getFight().get_id() == this.get_id()) //Si il n'est plus dans ce combat
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static void FightStateAddFlag(Maps _map, Player P) {
        for (Entry<Integer, Fight> fight : _map.get_fights().entrySet()) {
            if (fight.getValue()._state == Constant.FIGHT_STATE_PLACE) {
                if (fight.getValue()._type == Constant.FIGHT_TYPE_CHALLENGE) {
                    SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), 0, fight.getValue()._init0.getGUID(), fight.getValue()._init1.getGUID(), fight.getValue()._init0.getPersonnage().get_curCell().getID(), "0;-1", fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;-1");
                    for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
                        if (Config.DEBUG) Console.print(F.getValue().getPersonnage().getName());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), fight.getValue()._init0.getGUID(), fight.getValue()._init0);
                    }
                    for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
                        if (Config.DEBUG) Console.print(F.getValue().getPersonnage().getName());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init1.getPersonnage().getMap(), fight.getValue()._init1.getGUID(), fight.getValue()._init1);
                    }
                } else if (fight.getValue()._type == Constant.FIGHT_TYPE_AGRESSION) {
                    SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), 0, fight.getValue()._init0.getGUID(), fight.getValue()._init1.getGUID(), fight.getValue()._init0.getPersonnage().get_curCell().getID(), "0;" + fight.getValue()._init0.getPersonnage().get_align(), fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;" + fight.getValue()._init1.getPersonnage().get_align());
                    for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
                        if (Config.DEBUG) Console.print(F.getValue().getPersonnage().getName());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), fight.getValue()._init0.getGUID(), fight.getValue()._init0);
                    }
                    for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
                        if (Config.DEBUG) Console.print(F.getValue().getPersonnage().getName());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init1.getPersonnage().getMap(), fight.getValue()._init1.getGUID(), fight.getValue()._init1);
                    }
                } else if (fight.getValue()._type == Constant.FIGHT_TYPE_PVM) {
                    SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), 4, fight.getValue()._init0.getGUID(), fight.getValue()._mobGroup.getID(), (fight.getValue()._init0.getPersonnage().get_curCell().getID() + 1), "0;-1", fight.getValue()._mobGroup.getCellID(), "1;-1");
                    for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
                        if (Config.DEBUG) Console.print("PVM1: " + F.getValue().getPersonnage().getName());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), fight.getValue()._init0.getGUID(), fight.getValue()._init0);
                    }
                    for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
                        if (Config.DEBUG) Console.print("PVM2: " + F.getValue());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map, fight.getValue()._mobGroup.getID(), F.getValue());
                    }
                } else if (fight.getValue()._type == Constant.FIGHT_TYPE_PVT) {
                    SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), 5, fight.getValue()._init0.getGUID(), fight.getValue().collector.getGuid(), (fight.getValue()._init0.getPersonnage().get_curCell().getID() + 1), "0;-1", fight.getValue().collector.get_cellID(), "3;-1");
                    for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
                        if (Config.DEBUG) Console.print("PVT1: " + F.getValue().getPersonnage().getName());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(), fight.getValue()._init0.getGUID(), fight.getValue()._init0);
                    }
                    for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
                        if (Config.DEBUG) Console.print("PVT2: " + F.getValue());
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map, fight.getValue().collector.getGuid(), F.getValue());
                    }
                } else if (fight.getValue()._type == Constant.FIGHT_TYPE_CONQUETE) {
                    SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(),
                            0, fight.getValue()._init0.getGUID(), fight.getValue().prism.getID(), fight.getValue()._init0
                                    .getPersonnage().get_curCell().getID(), "0;"
                                    + fight.getValue()._init0.getPersonnage().get_align(),
                            fight.getValue().prism.getCell(), "0;" + fight.getValue().prism.getalignement());
                    SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().getMap(),
                            fight.getValue()._init0.getGUID(), fight.getValue()._init0);
                    for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
                        SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map,
                                fight.getValue().prism.getID(), F.getValue());
                    }
                }
            }
        }
    }

    public boolean getisMob() {
        return this._isMob;
    }

    public static int getFightIDByFighter(Maps _map, int guid) {
        for (Entry<Integer, Fight> fight : _map.get_fights().entrySet()) {
            for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
                if (F.getValue().getPersonnage() != null && F.getValue().getGUID() == guid) {
                    return fight.getValue().get_id();
                }
            }
        }
        return 0;
    }

    public Map<Integer, Fighter> getDeadList() {
        return deadList;
    }

    public boolean isLastFighterDie(Fighter f, int team) {
        if (team == 0) {
            if (lastFighterDieTeam0 == f) return true;
        } else if (team == 1) {
            if (lastFighterDieTeam1 == f) return true;
        }
        return false;
    }

    public Fighter getLastFighterDie(int team) {
        if (team == 0) {
            return lastFighterDieTeam0;
        } else if (team == 1) {
            return lastFighterDieTeam1;
        }
        return null;
    }

    public void setLastFighterDie(Fighter f, int team) {
        if (team == 0) {
            lastFighterDieTeam0 = f;
        } else if (team == 1) {
            lastFighterDieTeam1 = f;
        }
    }

    public void delOneDead(Fighter target) {
        deadList.remove(target.getGUID());
    }

    public void setTimeStartTurn(long a) {
        this.TimeStartTurn = a;
    }

    public long getTimeStartTurn() {
        return TimeStartTurn;
    }

    public void set_startTime(long _startTime) {
        this._startTime = _startTime;
    }

    public long get_startTime() {
        return _startTime;
    }

    public boolean HasUsedCoopTranspo() {
        return hasUsedCoopTranspo;
    }

    public void setHasUsedCoopTranspo(boolean hasUsedCoopTranspo) {
        this.hasUsedCoopTranspo = hasUsedCoopTranspo;
    }

    public Kolizeum getKolizeum() {
        return kolizeum;
    }

    public void setKolizeum(Kolizeum kolizeum) {
        this.kolizeum = kolizeum;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setDefenseurs(String str) {
        _defenseurs = str;
    }

    public String getDefenseurs() {
        return _defenseurs;
    }

    public TimerController getTimerController() {
        return timerController;
    }

    public void setTimerController(TimerController timerController) {
        this.timerController = timerController;
    }

    public long getStartRemaining() {
        return startRemaining;
    }

    public void setStartRemaining(long startRemaining) {
        this.startRemaining = startRemaining;
    }
}
