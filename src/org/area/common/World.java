package org.area.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.area.arena.GdG;
import org.area.arena.Team;
import org.area.client.Account;
import org.area.client.Player;
import org.area.client.Player.Stats;
import org.area.fight.object.Collector;
import org.area.fight.object.Monster;
import org.area.fight.object.Prism;
import org.area.game.GameSendThread;
import org.area.game.GameServer;
import org.area.kernel.Config;
import org.area.kernel.Console;
import org.area.kernel.Logs;
import org.area.kernel.Console.Color;
import org.area.kernel.Reboot;
import org.area.object.*;
import org.area.object.NpcTemplate.NPC;
import org.area.object.NpcTemplate.NPC_Exchange;
import org.area.object.AuctionHouse.HdvEntry;
import org.area.object.NpcTemplate.NPC_question;
import org.area.object.NpcTemplate.NPC_reponse;
import org.area.object.Item.ObjTemplate;
import org.area.object.job.Job;
import org.area.object.job.Job.StatsMetier;
import org.area.spell.Spell;

public class World {
    private static Map<Integer, Account> Comptes = new TreeMap<Integer, Account>();
    private static Map<Integer, GdG> Guerre = new TreeMap<Integer, GdG>();
    private static ArrayList<BanIp> Banips = new ArrayList<BanIp>();
    private static Map<String, Integer> ComptebyName = new HashMap<String, Integer>();
    private static StringBuilder Challenges = new StringBuilder();
    private static Map<Integer, Player> Persos = new TreeMap<Integer, Player>();
    private static Map<Short, Maps> Cartes = new TreeMap<Short, Maps>();
    private static Map<Integer, Item> Objets = new HashMap<Integer, Item>();
    private static Map<Integer, ExpLevel> ExpLevels = new TreeMap<Integer, ExpLevel>();
    private static Map<Integer, Spell> Sorts = new TreeMap<Integer, Spell>();
    private static Map<Integer, ObjTemplate> ObjTemplates = new TreeMap<Integer, ObjTemplate>();
    private static Map<Integer, Monster> MobTemplates = new TreeMap<Integer, Monster>();
    private static Map<Integer, NpcTemplate> NPCTemplates = new TreeMap<Integer, NpcTemplate>();
    private static Map<Integer, NPC_question> NPCQuestions = new TreeMap<Integer, NPC_question>();
    private static Map<Integer, NPC_reponse> NPCReponses = new TreeMap<Integer, NPC_reponse>();
    private static Map<Integer, IOTemplate> IOTemplate = new TreeMap<Integer, IOTemplate>();
    private static Map<Integer, Mount> Dragodindes = new TreeMap<Integer, Mount>();
    private static Map<Integer, SuperArea> SuperAreas = new TreeMap<Integer, SuperArea>();
    private static Map<Integer, Area> Areas = new TreeMap<Integer, Area>();
    private static Map<Integer, SubArea> SubAreas = new TreeMap<Integer, SubArea>();
    private static Map<Integer, Job> Job = new TreeMap<Integer, Job>();
    private static Map<Integer, ArrayList<Couple<Integer, Integer>>> Crafts = new TreeMap<Integer, ArrayList<Couple<Integer, Integer>>>();
    private static Map<Integer, ItemSet> ItemSets = new TreeMap<Integer, ItemSet>();
    private static Map<Integer, Guild> Guildes = new TreeMap<Integer, Guild>();
    private static Map<Integer, AuctionHouse> Hdvs = new TreeMap<Integer, AuctionHouse>();
    private static Map<Integer, Map<Integer, ArrayList<HdvEntry>>> _hdvsItems = new HashMap<Integer, Map<Integer, ArrayList<HdvEntry>>>();
    private static Map<Integer, Player> Married = new TreeMap<Integer, Player>();
    private static Map<Integer, Hustle> Animations = new TreeMap<Integer, Hustle>();
    private static Map<Short, Maps.MountPark> MountPark = new TreeMap<Short, Maps.MountPark>();
    private static Map<Integer, Trunk> Trunks = new TreeMap<Integer, Trunk>();
    private static Map<Integer, Collector> Percepteurs = new ConcurrentHashMap<Integer, Collector>();
    private static Map<Integer, Houses> House = new TreeMap<Integer, Houses>();
    private static Map<Short, Collection<Integer>> Seller = new TreeMap<Short, Collection<Integer>>();
    public static Map<Integer, StatsMetier> upAlchi = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upBrico = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upCM = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upB = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFE = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upSA = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFM = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upCo = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upBi = new TreeMap<Integer, StatsMetier>(); // Bijou
    public static Map<Integer, StatsMetier> upFD = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upSB = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upSBg = new TreeMap<Integer, StatsMetier>(); // S
    // baguette
    public static Map<Integer, StatsMetier> upFP = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upM = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upBou = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upT = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upP = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFH = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFPc = new TreeMap<Integer, StatsMetier>(); // Pêcheurman...
    // 42..
    // Sydney...
    // avenue
    // Walaby
    public static Map<Integer, StatsMetier> upC = new TreeMap<Integer, StatsMetier>();// Chasseurs
    public static Map<Integer, StatsMetier> upFMD = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFME = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFMM = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFMP = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upFMH = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upSMA = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upSMB = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upSMBg = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upBouc = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upPO = new TreeMap<Integer, StatsMetier>(); // Poissonniers.
    public static Map<Integer, StatsMetier> upFBou = new TreeMap<Integer, StatsMetier>();
    public static Map<Integer, StatsMetier> upJM = new TreeMap<Integer, StatsMetier>(); // Joaillo
    public static Map<Integer, StatsMetier> upCRM = new TreeMap<Integer, StatsMetier>();// Cordomages
    public static ArrayList<Short> restrictedMaps = null;
    private static Map<Integer, Prism> Prismes = new TreeMap<Integer, Prism>(); // Prismes
    public final static HashMap<String, Maps> cartesByPos = new HashMap<String, Maps>();
    private static Map<Integer, Map<String, String>> quests = new HashMap<Integer, Map<String, String>>();
    private static Map<Integer, Map<String, String>> questSteps = new HashMap<Integer, Map<String, String>>();
    private static Map<Integer, Map<String, String>> questObjetives = new HashMap<Integer, Map<String, String>>();

    // Liste des mobs avec spawnTime variables
    public static List<Monster.MobGroup> variableMobGroup = new ArrayList<Monster.MobGroup>();

    // Liste des objets boutiques
    private static Map<Integer, Integer> _listeEchangeItem = new HashMap<Integer, Integer>();

    // Liste des runes avec id
    private static HashMap<Integer, Rune> runes;

    // Listes des cadeaux
    public static HashMap<Integer, Gift> cadeaux = new HashMap<Integer, Gift>();

    private static int nextHdvID; // Contient le derniere ID utilisé pour crée
    // un HDV, pour obtenir un ID non utilisé il
    // faut impérativement l'incrémenter
    private static int nextLigneID; // Contient le derniere ID utilisé pour crée
    // une ligne dans un HDV

    private static int saveTry = 1;
    // Statut du serveur 1: accesible; 0: inaccesible; 2: sauvegarde
    private static short _state = 1;

    // Prix ornements
    private static Map<Integer, Integer> prixOrnements = new HashMap<Integer, Integer>();

    // Checkpoints
    public static HashMap<Short, Checkpoint> checkpoints = new HashMap<Short, Checkpoint>();

    private static byte _GmAccess = 0;

    private static int nextObjetID; // Contient le derniere ID utilisé pour crée
    // un Objet

    public static class Drop {
        private int _itemID;
        private int _prosp;
        private float _taux;
        private int _max;

        public Drop(int itm, int p, float t, int m) {
            _itemID = itm;
            _prosp = p;
            _taux = t;
            _max = m;
        }

        public void setMax(int m) {
            _max = m;
        }

        public int get_itemID() {
            return _itemID;
        }

        public int getMinProsp() {
            return _prosp;
        }

        public float get_taux() {
            return _taux;
        }

        public int get_max() {
            return _max;
        }
    }


    public static class ItemSet {
        private int _id;
        private ArrayList<ObjTemplate> _itemTemplates = new ArrayList<ObjTemplate>();
        private ArrayList<Stats> _bonuses = new ArrayList<Stats>();

        public ItemSet(int id, String items, String bonuses) {
            _id = id;
            // parse items String
            for (String str : items.split(",")) {
                try {
                    ObjTemplate t = World.getObjTemplate(Integer.parseInt(str
                            .trim()));
                    if (t == null)
                        continue;
                    _itemTemplates.add(t);
                } catch (Exception e) {
                }
                ;
            }

            // on ajoute un bonus vide pour 1 item
            _bonuses.add(new Stats());
            // parse bonuses String
            for (String str : bonuses.split(";")) {
                Stats S = new Stats();
                // séparation des bonus pour un même nombre d'item
                for (String str2 : str.split(",")) {
                    try {
                        String[] infos = str2.split(":");
                        int stat = Integer.parseInt(infos[0]);
                        int value = Integer.parseInt(infos[1]);
                        // on ajoute a la stat
                        S.addOneStat(stat, value);
                    } catch (Exception e) {
                    }
                    ;
                }
                // on ajoute la stat a la liste des bonus
                _bonuses.add(S);
            }
        }

        public int getId() {
            return _id;
        }

        public Stats getBonusStatByItemNumb(int numb) {
            if (numb > _bonuses.size())
                return new Stats();
            return _bonuses.get(numb - 1);
        }

        public ArrayList<ObjTemplate> getItemTemplates() {
            return _itemTemplates;
        }
    }

    public static class Area {
        private int _id;
        private SuperArea _superArea;
        private String _nom;
        private ArrayList<SubArea> _subAreas = new ArrayList<SubArea>();
        private int _alignement;
        public static int _bontas = 0;
        public static int _brakmars = 0;
        private int _Prisme = 0;

        public Area(int id, int superArea, String nom, int alignement,
                    int Prisme) {
            _id = id;
            _nom = nom;
            _superArea = World.getSuperArea(superArea);
            if (_superArea == null) {
                _superArea = new SuperArea(superArea);
                World.addSuperArea(_superArea);
            }
            _alignement = 0;
            _Prisme = Prisme;
            if (World.getPrisme(Prisme) != null) {
                _alignement = alignement;
                _Prisme = Prisme;
            }
            if (_alignement == 1)
                _bontas++;
            else if (_alignement == 2)
                _brakmars++;
        }

        public static int subareasBontas() {
            return _bontas;
        }

        public static int subareasBrakmars() {
            return _brakmars;
        }

        public int getalignement() {
            return _alignement;
        }

        public int getPrismeID() {
            return _Prisme;
        }

        public void setPrismeID(int Prisme) {
            _Prisme = Prisme;
        }

        public void setalignement(int alignement) {
            if (_alignement == 1 && alignement == -1)
                _bontas--;
            else if (_alignement == 2 && alignement == -1)
                _brakmars--;
            else if (_alignement == -1 && alignement == 1)
                _bontas++;
            else if (_alignement == -1 && alignement == 2)
                _brakmars++;
            _alignement = alignement;
        }

        public String getnom() {
            return _nom;
        }

        public int getID() {
            return _id;
        }

        public SuperArea getSuperArea() {
            return _superArea;
        }

        public void addSubArea(SubArea sa) {
            _subAreas.add(sa);
        }

        public ArrayList<SubArea> getSubAreas() {
            return _subAreas;
        }

        public ArrayList<Maps> getMaps() {
            ArrayList<Maps> maps = new ArrayList<Maps>();
            for (SubArea SA : _subAreas)
                maps.addAll(SA.getCartes());
            return maps;
        }
    }

    public static class SubArea {
        private int _id;
        private Area _area;
        private int _alignement;
        private String _nom;
        private ArrayList<Maps> _Cartes = new ArrayList<Maps>();
        private boolean _canConquest;
        private int _Prisme;
        public static int _bontas = 0;
        public static int _brakmars = 0;

        public SubArea(int id, int areaID, int alignement, String nom,
                       int conquistable, int Prisme) {
            _id = id;
            _nom = nom;
            _area = World.getArea(areaID);
            _alignement = alignement;
            _canConquest = conquistable == 0;
            _Prisme = Prisme;
            if (World.getPrisme(Prisme) != null) {
                _alignement = alignement;
                _Prisme = Prisme;
            }
            if (_alignement == 1)
                _bontas++;
            else if (_alignement == 2)
                _brakmars++;
        }

        public String getnom() {
            return _nom;
        }

        public int getPrismeID() {
            return _Prisme;
        }

        public void setPrismeID(int Prisme) {
            _Prisme = Prisme;
        }

        public boolean getConquistable() {
            return _canConquest;
        }

        public int getID() {
            return _id;
        }

        public Area getArea() {
            return _area;
        }

        public int getalignement() {
            return _alignement;
        }

        public void setalignement(int alignement) {
            if (_alignement == 1 && alignement == -1)
                _bontas--;
            else if (_alignement == 2 && alignement == -1)
                _brakmars--;
            else if (_alignement == -1 && alignement == 1)
                _bontas++;
            else if (_alignement == -1 && alignement == 2)
                _brakmars++;
            _alignement = alignement;
        }

        public ArrayList<Maps> getCartes() {
            return _Cartes;
        }

        public void addCarte(Maps Carte) {
            _Cartes.add(Carte);
        }

        public static int subareasBontas() {
            return _bontas;
        }

        public static int subareasBrakmars() {
            return _brakmars;
        }
    }

    public static class SuperArea {
        private int _id;
        private ArrayList<Area> _areas = new ArrayList<Area>();

        public SuperArea(int a_id) {
            _id = a_id;
        }

        public void addArea(Area A) {
            _areas.add(A);
        }

        public int getID() {
            return _id;
        }
    }

    public static class Couple<L, R> {
        public L first;
        public R second;

        public Couple(L s, R i) {
            this.first = s;
            this.second = i;
        }
    }

    public static class IOTemplate {
        private int _id;
        private int _respawnTime;
        private int _duration;
        private int _unk;
        private boolean _walkable;

        public IOTemplate(int a_i, int a_r, int a_d, int a_u, boolean a_w) {
            _id = a_i;
            _respawnTime = a_r;
            _duration = a_d;
            _unk = a_u;
            _walkable = a_w;
        }

        public int getId() {
            return _id;
        }

        public boolean isWalkable() {
            return _walkable;
        }

        public int getRespawnTime() {
            return _respawnTime;
        }

        public int getDuration() {
            return _duration;
        }

        public int getUnk() {
            return _unk;
        }
    }

    public static class Exchange {
        private Player perso1;
        private Player perso2;
        private long kamas1 = 0;
        private long kamas2 = 0;
        private ArrayList<Couple<Integer, Integer>> items1 = new ArrayList<Couple<Integer, Integer>>();
        private ArrayList<Couple<Integer, Integer>> items2 = new ArrayList<Couple<Integer, Integer>>();
        private boolean ok1;
        private boolean ok2;

        public Exchange(Player p1, Player p2) {
            perso1 = p1;
            perso2 = p2;
        }

        synchronized public long getKamas(int guid) {
            int i = 0;
            if (perso1.getGuid() == guid)
                i = 1;
            else if (perso2.getGuid() == guid)
                i = 2;

            if (i == 1)
                return kamas1;
            else if (i == 2)
                return kamas2;
            return 0;
        }

        synchronized public void toogleOK(int guid) {
            int i = 0;
            if (perso1.getGuid() == guid)
                i = 1;
            else if (perso2.getGuid() == guid)
                i = 2;

            if (i == 1) {
                ok1 = !ok1;
                SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                        .getGameThread().getOut(), ok1, guid);
                SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                        .getGameThread().getOut(), ok1, guid);
            } else if (i == 2) {
                ok2 = !ok2;
                SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                        .getGameThread().getOut(), ok2, guid);
                SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                        .getGameThread().getOut(), ok2, guid);
            } else
                return;

            if (ok1 && ok2)
                apply();
        }

        synchronized public void setKamas(int guid, long k) {
            ok1 = false;
            ok2 = false;

            int i = 0;
            if (perso1.getGuid() == guid)
                i = 1;
            else if (perso2.getGuid() == guid)
                i = 2;
            SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                    .getGameThread().getOut(), ok1, perso1.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                    .getGameThread().getOut(), ok1, perso1.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                    .getGameThread().getOut(), ok2, perso2.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                    .getGameThread().getOut(), ok2, perso2.getGuid());
            if (k < 0)
                return;
            if (i == 1) {
                kamas1 = k;
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'G', "", k
                        + "");
                SocketManager
                        .GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.getAccount()
                                .getGameThread().getOut(), 'G', "", k + "");
            } else if (i == 2) {
                kamas2 = k;
                SocketManager
                        .GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.getAccount()
                                .getGameThread().getOut(), 'G', "", k + "");
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'G', "", k
                        + "");
            }
        }

        synchronized public void cancel() {
            if (perso1.getAccount() != null)
                if (perso1.getAccount().getGameThread() != null)
                    SocketManager.GAME_SEND_EV_PACKET(perso1.getAccount()
                            .getGameThread().getOut());
            if (perso2.getAccount() != null)
                if (perso2.getAccount().getGameThread() != null)
                    SocketManager.GAME_SEND_EV_PACKET(perso2.getAccount()
                            .getGameThread().getOut());
            perso1.set_isTradingWith(0);
            perso2.set_isTradingWith(0);
            perso1.setCurExchange(null);
            perso2.setCurExchange(null);
        }

        synchronized public void apply() {
            // Gestion des Kamas
            boolean echangeKamasOK = false;
            if ((perso2.get_kamas() >= kamas2) && (perso1.get_kamas() >= kamas1)) {
                perso1.addKamas(kamas2);
                perso2.addKamas(kamas1);
                echangeKamasOK = true;
            }
            if (echangeKamasOK) {
                perso1.addKamas(-kamas1);
                perso2.addKamas(-kamas2);
            }
            for (Couple<Integer, Integer> couple : items1) {
                if (couple.second == 0)
                    continue;
                if (!perso1.hasItemGuid(couple.first))// Si le perso n'a pas
                // l'item (Ne devrait
                // pas arriver)
                {
                    couple.second = 0;// On met la quantité a 0 pour éviter les
                    // problemes
                    continue;
                }
                Item obj = World.getObjet(couple.first);
                if ((obj.getQuantity() - couple.second) < 1)// S'il ne reste
                // plus d'item apres
                // l'échange
                {
                    perso1.removeItem(couple.first);
                    couple.second = obj.getQuantity();
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso1,
                            couple.first);
                    if (!perso2.addObjet(obj, true))// Si le joueur avait un
                        // item similaire
                        World.removeItem(couple.first);// On supprime l'item
                    // inutile
                } else {
                    obj.setQuantity(obj.getQuantity() - couple.second);
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso1, obj);
                    Item newObj = Item.getCloneObjet(obj, couple.second);
                    if (perso2.addObjet(newObj, true))// Si le joueur n'avait
                        // pas d'item similaire
                        World.addObjet(newObj, true);// On ajoute l'item au
                    // World
                }
            }
            for (Couple<Integer, Integer> couple : items2) {
                if (couple.second == 0)
                    continue;
                if (!perso2.hasItemGuid(couple.first))// Si le perso n'a pas
                // l'item (Ne devrait
                // pas arriver)
                {
                    couple.second = 0;// On met la quantité a 0 pour éviter les
                    // problemes
                    continue;
                }
                Item obj = World.getObjet(couple.first);
                if ((obj.getQuantity() - couple.second) < 1)// S'il ne reste
                // plus d'item apres
                // l'échange
                {
                    perso2.removeItem(couple.first);
                    couple.second = obj.getQuantity();
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso2,
                            couple.first);
                    if (!perso1.addObjet(obj, true))// Si le joueur avait un
                        // item similaire
                        World.removeItem(couple.first);// On supprime l'item
                    // inutile
                } else {
                    obj.setQuantity(obj.getQuantity() - couple.second);
                    SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso2, obj);
                    Item newObj = Item.getCloneObjet(obj, couple.second);
                    if (perso1.addObjet(newObj, true))// Si le joueur n'avait
                        // pas d'item similaire
                        World.addObjet(newObj, true);// On ajoute l'item au
                    // World
                }
            }
            // Fin
            perso1.set_isTradingWith(0);
            perso2.set_isTradingWith(0);
            perso1.setCurExchange(null);
            perso2.setCurExchange(null);
            SocketManager.GAME_SEND_Ow_PACKET(perso1);
            SocketManager.GAME_SEND_Ow_PACKET(perso2);
            SocketManager.GAME_SEND_STATS_PACKET(perso1);
            SocketManager.GAME_SEND_STATS_PACKET(perso2);
            SocketManager.GAME_SEND_EXCHANGE_VALID(perso1.getAccount()
                    .getGameThread().getOut(), 'a');
            SocketManager.GAME_SEND_EXCHANGE_VALID(perso2.getAccount()
                    .getGameThread().getOut(), 'a');
            SQLManager.SAVE_PERSONNAGE(perso1, true);
            SQLManager.SAVE_PERSONNAGE(perso2, true);
        }

        synchronized public void addItem(int guid, int qua, int pguid) {
            ok1 = false;
            ok2 = false;

            Item obj = World.getObjet(guid);
            int i = 0;

            if (perso1.getGuid() == pguid)
                i = 1;
            if (perso2.getGuid() == pguid)
                i = 2;

            if (qua == 1)
                qua = 1;
            String str = guid + "|" + qua;
            if (obj == null)
                return;
            String add = "|" + obj.getTemplate(false).getID() + "|"
                    + obj.parseStatsString();
            SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                    .getGameThread().getOut(), ok1, perso1.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                    .getGameThread().getOut(), ok1, perso1.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                    .getGameThread().getOut(), ok2, perso2.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                    .getGameThread().getOut(), ok2, perso2.getGuid());
            if (i == 1) {
                Couple<Integer, Integer> couple = getCoupleInList(items1, guid);
                if (couple != null) {
                    couple.second += qua;
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+",
                            "" + guid + "|" + couple.second);
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2
                                    .getAccount().getGameThread().getOut(), 'O', "+",
                            "" + guid + "|" + couple.second + add);
                    return;
                }
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", str);
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2
                        .getAccount().getGameThread().getOut(), 'O', "+", str
                        + add);
                items1.add(new Couple<Integer, Integer>(guid, qua));
            } else if (i == 2) {
                Couple<Integer, Integer> couple = getCoupleInList(items2, guid);
                if (couple != null) {
                    couple.second += qua;
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+",
                            "" + guid + "|" + couple.second);
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1
                                    .getAccount().getGameThread().getOut(), 'O', "+",
                            "" + guid + "|" + couple.second + add);
                    return;
                }
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", str);
                SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1
                        .getAccount().getGameThread().getOut(), 'O', "+", str
                        + add);
                items2.add(new Couple<Integer, Integer>(guid, qua));
            }
        }

        //TODO:Baskwo
        public static class NpcExchange {
            private Player perso;
            private NPC npc;
            private ArrayList<Couple<Integer, Integer>> iPerso = new ArrayList<Couple<Integer, Integer>>();
            private ArrayList<Couple<Integer, Integer>> iNpc = new ArrayList<Couple<Integer, Integer>>();
            private boolean okPerso = false;
            private boolean okNpc = false;

            public NpcExchange(Player perso, NPC npc) {
                this.perso = perso;
                this.npc = npc;
            }

            public void addItem(int oId, int oQ) {
                okPerso = false;
                okNpc = false;

                Object obj = World.getObjet(oId);


                if (oQ == 1) oQ = 1;
                String str = oId + "|" + oQ;
                if (obj == null) return;
                SocketManager.GAME_SEND_EXCHANGE_OK(perso.getAccount().getGameThread().getOut(), okPerso, perso.getGuid());
                SocketManager.GAME_SEND_EXCHANGE_OK(perso.getAccount().getGameThread().getOut(), okNpc, npc.get_guid());
                Couple<Integer, Integer> couple = getCoupleInList(iPerso, oId);
                if (couple != null) {
                    couple.second += oQ;
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso, 'O', "+", "" + oId + "|" + couple.second);
                    if (npc.get_template().getExchange() != null) {
                        verifRequired(npc.get_template().getExchange());
                    }
                    return;
                }
                SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso, 'O', "+", str);
                iPerso.add(new Couple<Integer, Integer>(oId, oQ));
                System.out.println(1);
                if (npc.get_template().getExchange() != null) {
                    System.out.println(2);
                    verifRequired(npc.get_template().getExchange());
                }

            }

            synchronized public void removeItem(int guid, int qua) {
                okPerso = false;
                okNpc = false;

                SocketManager.GAME_SEND_EXCHANGE_OK(perso.getAccount().getGameThread().getOut(), okPerso, perso.getGuid());
                SocketManager.GAME_SEND_EXCHANGE_OK(perso.getAccount().getGameThread().getOut(), okNpc, npc.get_guid());

                Object obj = World.getObjet(guid);
                if (obj == null) return;
                Couple<Integer, Integer> couple = getCoupleInList(iPerso, guid);
                int newQua = couple.second - qua;
                if (newQua < 1)//Si il n'y a pu d'item
                {
                    iPerso.remove(couple);
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso, 'O', "-", "" + guid + "|0");
                } else {
                    couple.second = newQua;
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso, 'O', "+", "" + guid + "|" + newQua);
                }
                if (npc.get_template().getExchange() != null) {
                    verifRequired(npc.get_template().getExchange());
                }
            }

            public void verifRequired(ArrayList<NPC_Exchange> arrayList) {
                boolean verif = false;
                int nbr = -1;
                int pos = 0;
                for (NPC_Exchange exchange : arrayList) {
                    if (nbr != -1) continue;
                    if (iPerso.size() == exchange.getRequired().size()) {
                        for (Couple<Integer, Integer> i : iPerso) {
                            // 	Main.printDebug(i.first + " : " + i.second);
                            int o = World.getObjet(i.first).getTemplate(true).getID();
                            int oQ = i.second;
                            if (exchange.getRequired().containsKey(o)) {
                                if (exchange.getRequired().get(o).second == oQ) {
                                    verif = true;
                                    continue;
                                } else {
                                    verif = false;
                                    break;
                                }
                            } else {
                                verif = false;
                                break;
                            }
                        }
                        if (verif) nbr = pos;
                    }
                    pos++;
                }

                //  Main.printDebug(""+verif);

                if (verif) {
                    if (iNpc.size() > 0) {
                        for (Couple<Integer, Integer> i : iNpc) {
                            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso.getAccount().getGameThread().getOut(), 'O', "-", "" + i.first);
                        }
                        iNpc.clear();
                    }
                    iNpc.addAll(arrayList.get(nbr).getGift());
                    for (Couple<Integer, Integer> i : iNpc) {
                        ObjTemplate o = World.getObjTemplate(i.first);
                        String add = "|" + o.getID() + "|" + o.getStrTemplate();
                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso.getAccount().getGameThread().getOut(), 'O', "+", "" + i.first + "|" + i.second + add);
                    }
                } else {
                    for (Couple<Integer, Integer> i : iNpc) {
                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso.getAccount().getGameThread().getOut(), 'O', "-", "" + i.first);
                    }
                    iNpc.clear();
                }
            }

            synchronized public void toogleOK() {
                okPerso = true;
                okNpc = true;
                SocketManager.GAME_SEND_EXCHANGE_OK(perso.getAccount().getGameThread().getOut(), okPerso, perso.getGuid());
                SocketManager.GAME_SEND_EXCHANGE_OK(perso.getAccount().getGameThread().getOut(), okNpc, npc.get_guid());
                apply();
            }

            synchronized public void cancel() {
                SocketManager.GAME_SEND_EV_PACKET(perso.getAccount().getGameThread().getOut());
                perso.set_isTradingWith(0);
                perso.setNpcExchange(null);
            }

            synchronized public void apply() {
                //Gestion des Kamas
                for (Couple<Integer, Integer> couple : iPerso) {
                    if (couple.second == 0) continue;
                    if (!perso.hasItemGuid(couple.first))//Si le perso n'a pas l'item (Ne devrait pas arriver)
                    {
                        couple.second = 0;//On met la quantité a 0 pour éviter les problemes
                        continue;
                    }
                    Item obj = World.getObjet(couple.first);
                    if ((obj.getQuantity() - couple.second) < 1)//S'il ne reste plus d'item apres l'échange
                    {
                        perso.removeItem(couple.first);
                        couple.second = obj.getQuantity();
                        SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso, couple.first);
                    } else {
                        obj.setQuantity(obj.getQuantity() - couple.second);
                        SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso, obj);
                        //Item newObj = Item.getCloneObjet(obj, couple.second);
                    }
                }
                for (Couple<Integer, Integer> couple : iNpc) {
                    Item obj = World.getObjTemplate(couple.first).createNewItem(couple.second, false, 0);
                    if (perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
                        World.addObjet(obj, true);//On ajoute l'item au World
                }
                //Fin
                perso.set_isTradingWith(0);
                perso.setNpcExchange(null);
                SocketManager.GAME_SEND_Ow_PACKET(perso);
                SocketManager.GAME_SEND_STATS_PACKET(perso);
                SocketManager.GAME_SEND_EXCHANGE_VALID(perso.getAccount().getGameThread().getOut(), 'a');
                SQLManager.SAVE_PERSONNAGE(perso, true);
            }

            public synchronized int getQuaItem(int oId) {

                for (Couple<Integer, Integer> curCoupl : iPerso) {
                    if (curCoupl.first == oId) {
                        return curCoupl.second;
                    }
                }

                return 0;
            }

            synchronized private Couple<Integer, Integer> getCoupleInList(ArrayList<Couple<Integer, Integer>> items, int guid) {
                for (Couple<Integer, Integer> couple : items) {
                    if (couple.first == guid)
                        return couple;
                }
                return null;
            }
        }

        synchronized public void removeItem(int guid, int qua, int pguid) {
            int i = 0;
            if (perso1.getGuid() == pguid)
                i = 1;
            else if (perso2.getGuid() == pguid)
                i = 2;
            ok1 = false;
            ok2 = false;

            SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                    .getGameThread().getOut(), ok1, perso1.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                    .getGameThread().getOut(), ok1, perso1.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso1.getAccount()
                    .getGameThread().getOut(), ok2, perso2.getGuid());
            SocketManager.GAME_SEND_EXCHANGE_OK(perso2.getAccount()
                    .getGameThread().getOut(), ok2, perso2.getGuid());

            Item obj = World.getObjet(guid);
            if (obj == null)
                return;
            String add = "|" + obj.getTemplate(false).getID() + "|"
                    + obj.parseStatsString();
            if (i == 1) {
                Couple<Integer, Integer> couple = getCoupleInList(items1, guid);
                int newQua = couple.second - qua;
                if (newQua < 1)// Si il n'y a pu d'item
                {
                    items1.remove(couple);
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "-",
                            "" + guid);
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2
                                    .getAccount().getGameThread().getOut(), 'O', "-",
                            "" + guid);
                } else {
                    couple.second = newQua;
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+",
                            "" + guid + "|" + newQua);
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2
                                    .getAccount().getGameThread().getOut(), 'O', "+",
                            "" + guid + "|" + newQua + add);
                }
            } else if (i == 2) {
                Couple<Integer, Integer> couple = getCoupleInList(items2, guid);
                int newQua = couple.second - qua;

                if (newQua < 1)// Si il n'y a pu d'item
                {
                    items2.remove(couple);
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1
                                    .getAccount().getGameThread().getOut(), 'O', "-",
                            "" + guid);
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "-",
                            "" + guid);
                } else {
                    couple.second = newQua;
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1
                                    .getAccount().getGameThread().getOut(), 'O', "+",
                            "" + guid + "|" + newQua + add);
                    SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+",
                            "" + guid + "|" + newQua);
                }
            }
        }

        public void verifRequired(ArrayList<NPC_Exchange> arrayList) {
            boolean verif = false;
            int nbr = -1;
            int pos = 0;
            for (NPC_Exchange exchange : arrayList) {
                if (nbr != -1) continue;
                if (items1.size() == exchange.getRequired().size()) {
                    for (Couple<Integer, Integer> i : items1) {
                        int o = World.getObjet(i.first).getTemplate(true).getID();
                        int oQ = i.second;
                        if (exchange.getRequired().containsKey(o)) {
                            if (exchange.getRequired().get(o).second == oQ) {
                                verif = true;
                                continue;
                            } else {
                                verif = false;
                                break;
                            }
                        } else {
                            verif = false;
                            break;
                        }
                    }
                    if (verif) nbr = pos;
                }
                pos++;
            }


            if (verif) {
                if (items2.size() > 0) {
                    for (Couple<Integer, Integer> i : items2) {
                        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.getAccount().getGameThread().getOut(), 'O', "-", "" + i.first);
                    }
                    items2.clear();
                }
                items2.addAll(arrayList.get(nbr).getGift());
                for (Couple<Integer, Integer> i : items2) {
                    ObjTemplate o = World.getObjTemplate(i.first);
                    String add = "|" + o.getID() + "|" + o.getStrTemplate();
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.getAccount().getGameThread().getOut(), 'O', "+", "" + i.first + "|" + i.second + add);
                }
            } else {
                for (Couple<Integer, Integer> i : items2) {
                    SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.getAccount().getGameThread().getOut(), 'O', "-", "" + i.first);
                }
                items2.clear();
            }
        }

        synchronized private Couple<Integer, Integer> getCoupleInList(
                ArrayList<Couple<Integer, Integer>> items, int guid) {
            for (Couple<Integer, Integer> couple : items) {
                if (couple.first == guid)
                    return couple;
            }
            return null;
        }

        public synchronized int getQuaItem(int itemID, int playerGuid) {
            ArrayList<Couple<Integer, Integer>> items;
            if (perso1.getGuid() == playerGuid)
                items = items1;
            else
                items = items2;

            for (Couple<Integer, Integer> curCoupl : items) {
                if (curCoupl.first == itemID) {
                    return curCoupl.second;
                }
            }

            return 0;
        }

    }

    public static class ExpLevel {
        public long perso;
        public int metier;
        public int dinde;
        public int pvp;
        public long guilde;

        public ExpLevel(long c, int m, int d, int p) {
            perso = c;
            metier = m;
            dinde = d;
            pvp = p;
            guilde = perso * 10;
        }

    }

    public static void createWorld() {

        Console.bright();
        Console.println(
                "-------------------------------------------------------------------------------\n"
                        + "                               [Load the World]                                \n",
                Color.RED);
        Console.println(
                "-------------------------------------------------------------------------------\n",
                Color.RED);
        SQLManager.LOAD_EXP();
        Console.println("|| Expérience : Ok !", Color.CYAN);
        SQLManager.LOAD_GMCOMMANDS();
        Console.println("|| Commandes GM : Ok !", Color.CYAN);
        SQLManager.LOAD_SORTS();
        Console.println("|| Sorts : Ok !", Color.CYAN);
        SQLManager.LOAD_EVENTS();
        Console.println("|| Événement : Ok !", Color.CYAN);
        SQLManager.LOAD_SHOP();
        Console.println("|| Shop / boutique : Ok !", Color.CYAN);
        SQLManager.LOAD_MOB_TEMPLATE();
        Console.println("|| Template des monstres : Ok !", Color.CYAN);
        SQLManager.LOAD_OBJ_TEMPLATE();
        Console.println("|| Template des objets : Ok !", Color.CYAN);
        SQLManager.LOAD_NPC_TEMPLATE();
        Console.println("|| Template des PNJ : Ok !", Color.CYAN);
        SQLManager.LOAD_NPC_QUESTIONS();
        Console.println("|| Question PNJ : Ok !", Color.CYAN);
        SQLManager.LOAD_NPC_ANSWERS();
        Console.println("|| Répondes PNJ : Ok !", Color.CYAN);
        SQLManager.LOAD_QUIZZ_QUESTIONS();
        Console.println("|| Questions Quizz : Ok !", Color.CYAN);
        SQLManager.LOAD_AREA();
        Console.println("|| Zones : Ok !", Color.CYAN);
        SQLManager.LOAD_SUBAREA();
        Console.println("|| Sous-zones : Ok !", Color.CYAN);
        SQLManager.LOAD_IOTEMPLATE();
        Console.println("|| IOTemplate : Ok !", Color.CYAN);
        SQLManager.LOAD_ITEMSETS();
        Console.println("|| Panoplies : Ok !", Color.CYAN);
        SQLManager.LOAD_MAPS();
        Console.println("|| Carte : Ok !", Color.CYAN);
        SQLManager.LOAD_PRISMES();
        Console.println("|| Prisme : Ok !", Color.CYAN);
        SQLManager.LOAD_COMMANDS();
        Console.println("|| Commandes joueurs : Ok !", Color.CYAN);
        SQLManager.LOAD_CRAFTS();
        Console.println("|| Recettes craft : Ok !", Color.CYAN);
        SQLManager.LOAD_JOBS();
        Console.println("|| Métier : Ok !", Color.CYAN);
        SQLManager.LOAD_TRIGGERS();
        Console.println("|| Triggers : Ok !", Color.CYAN);
        SQLManager.LOAD_ENDFIGHT_ACTIONS();
        Console.println("|| Endfight actions : Ok !", Color.CYAN);
        SQLManager.LOAD_NPC_EXCHANGE();
        Console.println("|| Échange PNJ : Ok !", Color.CYAN);
        SQLManager.LOAD_NPCS();
        Console.println("|| PNJ : Ok !", Color.CYAN);
        SQLManager.LOAD_QUESTS();
        Console.println("|| Quêtes : Ok !", Color.CYAN);
        SQLManager.LOAD_QUEST_STEPS();
        Console.println("|| Étapes quêtes : Ok !", Color.CYAN);
        SQLManager.LOAD_QUEST_OBJECTIVES();
        Console.println("|| Objectifs quêtes : Ok !", Color.CYAN);
        SQLManager.LOAD_ITEM_ACTIONS();
        Console.println("|| Item action : Ok !", Color.CYAN);
        SQLManager.LOAD_DROPS();
        Console.println("|| Drops : Ok !", Color.CYAN);
        SQLManager.LOAD_ANIMATIONS();
        Console.println("|| Animation : Ok !", Color.CYAN);

        SQLManager.LOGGED_ZERO();
        SQLManager.LOAD_MOUNTS();
        Console.println("|| Montures : Ok !", Color.CYAN);
        SQLManager.LOAD_ITEMS_FULL(); // @Flow
        Console.println("|| Items : Ok !", Color.CYAN);
        SQLManager.LOAD_GUILDS();
        Console.println("|| Guildes : Ok !", Color.CYAN);
        SQLManager.LOAD_GUILD_MEMBERS();
        Console.println("|| Membres guilde : Ok !", Color.CYAN);
        SQLManager.LOAD_CHALLENGES();
        Console.println("|| Challenge : Ok !", Color.CYAN);
        SQLManager.LOAD_MOUNTPARKS();
        Console.println("|| Enclos : Ok !", Color.CYAN);
        SQLManager.LOAD_PERCEPTEURS();
        Console.println("|| Percepteurs : Ok !", Color.CYAN);
        SQLManager.LOAD_HOUSES();
        Console.println("|| Maisons : Ok !", Color.CYAN);
        SQLManager.LOAD_TRUNK();
        SQLManager.LOAD_ZAAPS();
        Console.println("|| Zaaps : Ok !", Color.CYAN);
        Team.loadTeams();
        SQLManager.LOAD_RAPIDSTUFFS();
        SQLManager.LOAD_ZAAPIS();
        Console.println("|| Zaapi : Ok !", Color.CYAN);
        SQLManager.LOAD_BANIP();
        SQLManager.LOAD_HDVS();
        Console.println("|| Hdvs : Ok !", Color.CYAN);
        SQLManager.LOAD_HDVS_ITEMS();
        Console.println("|| Item hdvs : Ok !", Color.CYAN);
        SQLManager.RESET_MOUNTPARKS();
        Console.println("|| Montures en enclos : Ok !", Color.CYAN);
        SQLManager.LOAD_SORTS_INTERDITS();
        Console.println("|| Sorts interdits : Ok !", Color.CYAN);
        SQLManager.LOAD_GIFTS();
        Console.println("|| Cadeaux : Ok !", Color.CYAN);
        //nextObjetID = SQLManager.getNextObjetID()+1;
        updateListeEchangeItem(); // @Flow
        Constant.initialiserTableauPoidsParPuissance();
        runes = SQLManager.LOAD_RUNES();
        SQLManager.LOAD_CHECKPOINT();
        Console.println("|| Checkpoints : Ok !", Color.CYAN);
        SQLManager.LOAD_ORNEMENTS_PRICE();
    }

    public static Area getArea(int areaID) {
        return getAreas().get(areaID);
    }

    public static SuperArea getSuperArea(int areaID) {
        return getSuperAreas().get(areaID);
    }

    public static SubArea getSubArea(int areaID) {
        return getSubAreas().get(areaID);
    }

    public static void addArea(Area area) {
        getAreas().put(area.getID(), area);
    }

    public static void addSuperArea(SuperArea SA) {
        getSuperAreas().put(SA.getID(), SA);
    }

    public static void addSubArea(SubArea SA) {
        getSubAreas().put(SA.getID(), SA);
    }

    public static void addNPCreponse(NPC_reponse rep) {
        getNPCReponses().put(rep.get_id(), rep);
    }

    public static NPC_reponse getNPCreponse(int guid) {
        return getNPCReponses().get(guid);
    }

    public static int getExpLevelSize() {
        return getExpLevels().size();
    }

    public static void addExpLevel(int lvl, ExpLevel exp) {
        getExpLevels().put(lvl, exp);
    }

    public static Account getCompte(int guid) {
        Account toReturn = null;
        try {
            if (getComptes().get(guid) == null) {
                SQLManager.LOAD_ACCOUNT(guid);
            }
            toReturn = getComptes().get(guid);
        } catch (Exception e) {
            GameServer.addToLog("Erreur chargement compte ->" + guid);
        }
        return toReturn;
    }

    public static void addNPCQuestion(NPC_question quest) {
        getNPCQuestions().put(quest.get_id(), quest);
    }

    public static NPC_question getNPCQuestion(int guid) {
        return getNPCQuestions().get(guid);
    }

    public static NpcTemplate getNPCTemplate(int guid) {
        return getNPCTemplates().get(guid);
    }

    public static void addNpcTemplate(NpcTemplate temp) {
        getNPCTemplates().put(temp.get_id(), temp);
    }

    public static Maps getCarte(short id) {
        return getCartes().get(id);
    }

    public static void addCarte(Maps map) {
        if (!getCartes().containsKey(Short.valueOf(map.get_id())))
            getCartes().put(Short.valueOf(map.get_id()), map);
        try {
            cartesByPos.put(map.getX() + "," + map.getY() + ","
                    + map.getSubArea().getArea().getSuperArea().getID(), map);
        } catch (Exception e) {
        }
    }

    public static void delCarte(Maps map) {
        if (getCartes().containsKey(map.get_id()))
            getCartes().remove(map.get_id());
    }

    public static Account getCompteByName(String name) {
        return (getComptebyName().get(name.toLowerCase()) != null ? getComptes()
                .get(getComptebyName().get(name.toLowerCase())) : null);
    }

    public static Player getPlayer(int guid) {
        if (!getPersos().containsKey(guid))
            SQLManager.LOAD_PLAYER(guid);
        return getPersos().get(guid);
    }

    public static void addAccount(Account compte) {
        getComptes().put(compte.getGuid(), compte);
        getComptebyName().put(compte.getName().toLowerCase(), compte.getGuid());
    }

    public static void addGuerre(GdG ga) {
        Guerre.put(ga.get_guildID(), ga);
    }

    public static void addChallenge(String chal) {
        // ChalID,gainXP,gainDrop,gainParMob,Conditions;...
        if (!getChallenges().toString().isEmpty())
            getChallenges().append(";");
        getChallenges().append(chal);
    }

    public static String getChallengeFromConditions(boolean sevEnn,
                                                    boolean sevAll, boolean bothSex, boolean EvenEnn, boolean MoreEnn,
                                                    boolean hasCaw, boolean hasChaf, boolean hasRoul, boolean hasArak,
                                                    boolean isBoss) {
        String noBossChals = ";2;5;9;17;19;24;38;47;50;"; // ceux impossibles
        // contre boss
        StringBuilder toReturn = new StringBuilder();
        boolean isFirst = true, isGood = false;
        int cond = 0;
        for (String chal : getChallenges().toString().split(";")) {
            if (!isFirst && isGood)
                toReturn.append(";");
            isGood = true;
            cond = Integer.parseInt(chal.split(",")[4]);
            // Nécessite plusieurs ennemis
            if (((cond & 1) == 1) && !sevEnn)
                isGood = false;
            // Nécessite plusieurs alliés
            if ((((cond >> 1) & 1) == 1) && !sevAll)
                isGood = false;
            // Nécessite les deux sexes
            if ((((cond >> 2) & 1) == 1) && !sevAll)
                isGood = false;
            // Nécessite un nombre pair d'ennemis
            if ((((cond >> 3) & 1) == 1) && !sevAll)
                isGood = false;
            // Nécessite plus d'ennemis que d'alliés
            if ((((cond >> 4) & 1) == 1) && !sevAll)
                isGood = false;
            // Jardinier
            if (!hasCaw && (Integer.parseInt(chal.split(",")[0]) == 7))
                isGood = false;
            // Fossoyeur
            if (!hasChaf && (Integer.parseInt(chal.split(",")[0]) == 12))
                isGood = false;
            // Casino Royal
            if (!hasRoul && (Integer.parseInt(chal.split(",")[0]) == 14))
                isGood = false;
            // Araknophile
            if (!hasArak && (Integer.parseInt(chal.split(",")[0]) == 15))
                isGood = false;
            // Contre un boss de donjon
            if (noBossChals.contains(";" + chal.split(",")[0] + ";"))
                isGood = false;
            if (isGood)
                toReturn.append(chal);
            isFirst = false;
        }
        return toReturn.toString();
    }

    public static ArrayList<String> getRandomChallenge(int nombreChal,
                                                       String challenges) {
        String MovingChals = ";1;2;8;36;37;39;40;"; // Challenges de
        // déplacements
        // incompatibles
        boolean hasMovingChal = false;
        String TargetChals = ";3;4;10;25;31;32;34;35;38;42;"; // ceux qui
        // ciblent
        boolean hasTargetChal = false;
        String SpellChals = ";5;6;9;11;19;20;24;41;"; // ceux qui obligent à
        // caster spécialement
        boolean hasSpellChal = false;
        String KillerChals = ";28;29;30;44;45;46;48;"; // ceux qui disent qui
        // doit tuer
        boolean hasKillerChal = false;
        String HealChals = ";18;43;"; // ceux qui empêchent de soigner
        boolean hasHealChal = false;

        int compteur = 0, i = 0;
        ArrayList<String> toReturn = new ArrayList<String>();
        String chal = new String();
        while (compteur < 100 && toReturn.size() < nombreChal) {

            compteur++;
            i = Formulas.getRandomValue(1, challenges.split(";").length);
            chal = challenges.split(";")[i - 1]; // challenge au hasard dans la
            // liste

            if (!toReturn.contains(chal)) {// si le challenge n'y était pas
                // encore
                if (MovingChals.contains(";" + chal.split(",")[0] + ";")) // s'il
                    // appartient
                    // à
                    // une
                    // liste
                    if (!hasMovingChal) { // et qu'aucun de la liste n'a été
                        // choisi déjà
                        hasMovingChal = true;
                        toReturn.add(chal);
                        continue;
                    } else
                        continue;
                if (TargetChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasTargetChal) {
                        hasTargetChal = true;
                        toReturn.add(chal);
                        continue;
                    } else
                        continue;
                if (SpellChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasSpellChal) {
                        hasSpellChal = true;
                        toReturn.add(chal);
                        continue;
                    } else
                        continue;
                if (KillerChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasKillerChal) {
                        hasKillerChal = true;
                        toReturn.add(chal);
                        continue;
                    } else
                        continue;
                if (HealChals.contains(";" + chal.split(",")[0] + ";"))
                    if (!hasHealChal) {
                        hasHealChal = true;
                        toReturn.add(chal);
                        continue;
                    } else
                        continue;
                toReturn.add(chal); // s'il n'appartient à aucune liste

            }
            compteur++;
        }
        return toReturn;
    }

    public static void addAccountbyName(Account compte) {
        getComptebyName().put(compte.getName(), compte.getGuid());
    }

    public static void addPersonnage(Player perso) {
        synchronized (getPersos()) {
            getPersos().put(perso.getGuid(), perso);
        }
    }

    public static Player getPersoByName(String name) { // @Flow - Un test
        ArrayList<Player> Ps = new ArrayList<Player>();
        synchronized (getPersos()) {
            Ps.addAll(getPersos().values());
        }
        for (Player P : Ps)
            if (P.getName().equalsIgnoreCase(name))
                return P;
        return SQLManager.LOAD_PLAYER_BY_NAME(name);
    }

	/*public static Player getPersoByName(String name) {
        ArrayList<Player> Ps = new ArrayList<Player>();
        Ps.addAll(Persos.values());
        for (Player P : Ps)
            if (P.getName().equalsIgnoreCase(name))
                return P;
        return null;
   }*/

    public static void deletePerso(Player perso) {
        if (perso.get_guild() != null) {
            if (perso.get_guild().getMembers().size() <= 1)// Il est tout seul
            // dans la guilde :
            // Supression
            {
                World.removeGuild(perso.get_guild().get_id());
            } else if (perso.getGuildMember().getRank() == 1)// On passe les
            // pouvoir a
            // celui qui a
            // le plus de
            // droits si il
            // est meneur
            {
                int curMaxRight = 0;
                Player Meneur = null;
                for (Player newMeneur : perso.get_guild().getMembers()) {
                    if (newMeneur == perso)
                        continue;
                    if (newMeneur.getGuildMember().getRights() < curMaxRight) {
                        Meneur = newMeneur;
                    }
                }
                perso.get_guild().removeMember(perso);
                Meneur.getGuildMember().setRank(1);
            } else// Supression simple
            {
                perso.get_guild().removeMember(perso);
            }
        }
        perso.remove();// Supression BDD Perso, items, monture.
        World.unloadPerso(perso.getGuid());// UnLoad du perso+item
    }

    public static String getSousZoneStateString() {
        String data = "";
        /* TODO: Sous Zone Alignement */
        return data;
    }

    public static long getPersoXpMin(int _lvl) {
        if (_lvl > getExpLevelSize())
            _lvl = getExpLevelSize();
        if (_lvl < 1)
            _lvl = 1;
        return getExpLevels().get(_lvl).perso;
    }

    public static long getPersoXpMax(int _lvl) {
        if (_lvl >= getExpLevelSize())
            _lvl = (getExpLevelSize() - 1);
        if (_lvl <= 1)
            _lvl = 1;
        return getExpLevels().get(_lvl + 1).perso;
    }

    public static void addSort(Spell sort) {
        getSorts().put(sort.getSpellID(), sort);
    }

    public static void addObjTemplate(ObjTemplate obj) {
        ObjTemplates.put(obj.getID(), obj);
    }

    public static Spell getSort(int id) {
        return getSorts().get(id);
    }

    public static ObjTemplate getObjTemplate(int id) {
        return ObjTemplates.get(id);
    }

    public synchronized static int getNewItemGuid() {
        //return nextObjetID++;
        return SQLManager.getNextObjetID() + 1;
    }

    public static void addMobTemplate(int id, Monster mob) {
        getMobTemplates().put(id, mob);
    }

    public static Monster getMonstre(int id) {
        return getMobTemplates().get(id);
    }

    public static int countPersoOnMap(short mapid) {
        Maps map = getCarte(mapid);
        if (map == null)
            return 0;
        if (map.getPersos() == null)
            return 0;
        return map.getPersos().size();
    }

    public static List<Player> getOnlinePlayers() {
        Map<Integer, Player> persos = new TreeMap<Integer, Player>();
        synchronized (getPersos()) {
            persos.putAll(getPersos());
        }
        List<Player> online = new ArrayList<Player>();
        for (Entry<Integer, Player> perso : persos.entrySet()) {
            if (perso.getValue() != null && perso.getValue().isOnline()
                    && perso.getValue().getAccount() != null
                    && perso.getValue().getAccount().getGameThread() != null) {
                if (perso.getValue().getAccount().getGameThread().getOut() != null) {
                    online.add(perso.getValue());
                } else {
                    World.verifyClone(perso.getValue());
                }
            }
        }
        return online;
    }

    public static void addObjet(Item item, boolean saveSQL) {
        if (!getObjets().containsKey(item.getGuid())) {
            getObjets().put(item.getGuid(), item);
            if (saveSQL)
                SQLManager.SAVE_NEW_ITEM(item);
        }
    }

    public static Item getObjet(int guid) {
        if (getObjets().containsKey(guid)) {
            return getObjets().get(guid);
        } else {
            return null;
        }
    }

    public static void removeItem(int guid) {
        getObjets().remove(guid);
        SQLManager.DELETE_ITEM(guid);
    }

    public static void addIOTemplate(IOTemplate IOT) {
        IOTemplate.put(IOT.getId(), IOT);
    }

    public static Mount getDragoByID(int id) {
        return getDragodindes().get(id);
    }

    public static void addDragodinde(Mount DD) {
        getDragodindes().put(DD.get_id(), DD);
    }

    public static void removeDragodinde(int DID) {
        getDragodindes().remove(DID);
    }

    public static void saveAll(Player saver) {
        GameSendThread _out = null;
        if (saver != null)
            _out = saver.getAccount().getGameThread().getOut();

        set_state((short) 2);

        try {
            Console.print("\nSauvegarde en cours...\n", Color.RED);
            Config.IS_SAVING = true;
            SQLManager.commitTransacts();
            SQLManager.TIMER(false);// Arrête le timer d'enregistrement SQL

            // Thread.sleep(10000);

            for (Player perso : getPersos().values()) {
                if (!perso.isOnline())
                    continue;
                // Thread.sleep(10);//0.1 sec. pour 1 objets
                SQLManager.SAVE_PERSONNAGE(perso, true);// sauvegarde des persos
                // et de leurs items
            }

            // Thread.sleep(250);

            for (Guild guilde : getGuildes().values()) {
                // Thread.sleep(10);//0.1 sec. pour 1 guilde
                SQLManager.UPDATE_GUILD(guilde);
            }

            // Thread.sleep(250);

            for (Collector perco : getPercepteurs().values()) {
                if (perco.get_inFight() > 0)
                    continue;
                // Thread.sleep(10);//0.1 sec. pour 1 percepteur
                SQLManager.UPDATE_PERCO(perco);
            }

            // Thread.sleep(250);
            for (Prism Prisme : Prismes.values()) {
                boolean toDelete = true;
                for (SubArea subarea : getSubAreas().values()) {
                    if (subarea.getPrismeID() == Prisme.getID())
                        toDelete = false;
                }
                if (toDelete)
                    SQLManager.DELETE_PRISME(Prisme.getID());
                else
                    SQLManager.SAVE_PRISME(Prisme);
            }
            for (Houses house : getHouse().values()) {
                if (house.get_owner_id() > 0) {
                    // Thread.sleep(100);//0.1 sec. pour 1 maison
                    SQLManager.UPDATE_HOUSE(house);
                }
            }

            // Thread.sleep(250);

            for (Trunk t : getTrunks().values()) {
                if (t.get_owner_id() > 0) {
                    // Thread.sleep(10);//0.1 sec. pour 1 coffre
                    SQLManager.UPDATE_TRUNK(t);
                }
            }

            // Thread.sleep(250);

            for (Maps.MountPark mp : getMountPark().values()) {
                if (mp.get_owner() > 0 || mp.get_owner() == -1) {
                    // Thread.sleep(10);//0.1 sec. pour 1 enclo
                    SQLManager.UPDATE_MOUNTPARK(mp);
                }
            }
            for (Area area : getAreas().values()) {
                SQLManager.UPDATE_AREA(area);
            }
            for (SubArea subarea : getSubAreas().values()) {
                SQLManager.UPDATE_SUBAREA(subarea);
            }
            // Thread.sleep(250);

            /**ArrayList<HdvEntry> toSave = new ArrayList<HdvEntry>();
             for (AuctionHouse curHdv : getHdvs().values()) {
             toSave.addAll(curHdv.getAllEntry());
             }
             SQLManager.SAVE_HDVS_ITEMS(toSave);**/
            SQLManager.SAVE_HDV_AVGPRICE();

            // Thread.sleep(100);

            Console.print("Sauvegarde effectuee avec succes !\n", Color.RED);

            set_state((short) 1);
            // TODO : Rafraichir

        } catch (ConcurrentModificationException e) {
            if (saveTry < 10) {
                GameServer.addToLog("Nouvelle tentative de sauvegarde");
                if (saver != null && _out != null)
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,
                            "Erreur. Nouvelle tentative de sauvegarde");
                saveTry++;
                saveAll(saver);
            } else {
                set_state((short) 1);
                // TODO : Rafraichir
                String mess = "Echec de la sauvegarde apres " + saveTry
                        + " tentatives";
                if (saver != null && _out != null)
                    SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
                GameServer.addToLog(mess);
            }

        } catch (Exception e) {
            GameServer.addToLog("Erreur lors de la sauvegarde : "
                    + e.getMessage());
            e.printStackTrace();
        } finally {
            SQLManager.commitTransacts();
            SQLManager.TIMER(true); // Redémarre le timer d'enregistrement SQL
            Config.IS_SAVING = false;
            saveTry = 1;
        }

    }

    public static void RefreshAllMob() {
        SocketManager.GAME_SEND_MESSAGE_TO_ALL(
                "Recharge des Mobs en cours, des latences peuvent survenir.",
                Config.CONFIG_MOTD_COLOR);
        for (Maps map : getCartes().values()) {
            map.refreshSpawns();
        }
        SocketManager
                .GAME_SEND_MESSAGE_TO_ALL(
                        "Recharge des Mobs finie. La prochaine recharge aura lieu dans 5heures.",
                        Config.CONFIG_MOTD_COLOR);
    }

    public static ExpLevel getExpLevel(int lvl) {
        return getExpLevels().get(lvl);
    }

    public static IOTemplate getIOTemplate(int id) {
        return IOTemplate.get(id);
    }

    public static Job getMetier(int id) {
        return getJob().get(id);
    }

    public static void addJob(Job metier) {
        getJob().put(metier.getId(), metier);
    }

    public static void addCraft(int id, ArrayList<Couple<Integer, Integer>> m) {
        getCrafts().put(id, m);
    }

    public static ArrayList<Couple<Integer, Integer>> getCraft(int i) {
        return getCrafts().get(i);
    }

    public static int getObjectByIngredientForJob(ArrayList<Integer> list,
                                                  Map<Integer, Integer> ingredients) {
        if (list == null)
            return -1;
        for (int tID : list) {
            ArrayList<Couple<Integer, Integer>> craft = World.getCraft(tID);
            if (craft == null) {
                GameServer.addToLog("/!\\Recette pour l'objet " + tID
                        + " non existante !");
                continue;
            }
            if (craft.size() != ingredients.size())
                continue;
            boolean ok = true;
            for (Couple<Integer, Integer> c : craft) {
                // si ingredient non présent ou mauvaise quantité
                if (ingredients.get(c.first) != c.second)
                    ok = false;
            }
            if (ok)
                return tID;
        }
        return -1;
    }

    public static Account getCompteByPseudo(String p) {
        for (Account C : getComptes().values())
            if (C.getPseudo().equals(p))
                return C;
        return null;
    }

    public static void addItemSet(ItemSet itemSet) {
        getItemSets().put(itemSet.getId(), itemSet);
    }

    public static ItemSet getItemSet(int tID) {
        return getItemSets().get(tID);
    }

    public static int getItemSetNumber() {
        return getItemSets().size();
    }

    public static int getNextIdForMount() {
        int max = 1;
        for (int a : getDragodindes().keySet())
            if (a > max)
                max = a;
        return max + 1;
    }

    public static Maps getCarteByPosAndCont(int mapX, int mapY, int contID) {
        for (Maps map : getCartes().values()) {
            if (map.getX() == mapX
                    && map.getY() == mapY
                    && map.getSubArea().getArea().getSuperArea().getID() == contID)
                return map;
        }
        return null;
    }

    public synchronized static int getNextIDPrisme() {
        int max = 1;
        for (int a : Prismes.keySet())
            if (a > max)
                max = a;
        return max + 1;
    }

    public synchronized static void addPrisme(Prism Prisme) {
        Prismes.put(Prisme.getID(), Prisme);
    }

    public static Prism getPrisme(int id) {
        return Prismes.get(id);
    }

    public static void removePrisme(int id) {
        Prismes.remove(id);
    }

    public static Collection<Prism> AllPrisme() {
        if (Prismes.size() > 0)
            return Prismes.values();
        return null;
    }

    public static String PrismesGeoposition(int alignement) {
        String str = "";
        boolean first = false;
        int subareas = 0;
        for (SubArea subarea : getSubAreas().values()) {
            if (!subarea.getConquistable())
                continue;
            if (first)
                str += ";";
            str += subarea.getID()
                    + ","
                    + (subarea.getalignement() == 0 ? -1 : subarea
                    .getalignement()) + ",0,";
            if (World.getPrisme(subarea.getPrismeID()) == null)
                str += 0 + ",1";
            else
                str += (subarea.getPrismeID() == 0 ? 0 : World.getPrisme(
                        subarea.getPrismeID()).getCarte())
                        + ",1";
            first = true;
            subareas++;
        }
        if (alignement == 1)
            str += "|" + Area._bontas;
        else if (alignement == 2)
            str += "|" + Area._brakmars;
        str += "|" + getAreas().size() + "|";
        first = false;
        for (Area area : getAreas().values()) {
            if (area.getalignement() == 0)
                continue;
            if (first)
                str += ";";
            str += area.getID() + "," + area.getalignement() + ",1,"
                    + (area.getPrismeID() == 0 ? 0 : 1);
            first = true;
        }
        if (alignement == 1)
            str = Area._bontas + "|" + subareas + "|"
                    + (subareas - (SubArea._bontas + SubArea._brakmars)) + "|"
                    + str;
        else if (alignement == 2)
            str = Area._brakmars + "|" + subareas + "|"
                    + (subareas - (SubArea._bontas + SubArea._brakmars)) + "|"
                    + str;
        return str;
    }

    public static void showPrismes(Player perso) {
        for (SubArea subarea : getSubAreas().values()) {
            if (subarea.getalignement() == 0)
                continue;
            SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(perso,
                    subarea.getID() + "|" + subarea.getalignement() + "|1");
        }
    }

    public static void addGuild(Guild g, boolean save) {
        getGuildes().put(g.get_id(), g);
        if (save)
            SQLManager.SAVE_NEWGUILD(g);
    }

    public static int getNextHighestGuildID() {
        if (getGuildes().isEmpty())
            return 1;
        int n = 0;
        for (int x : getGuildes().keySet())
            if (n < x)
                n = x;
        return n + 1;
    }

    public static boolean guildNameIsUsed(String name) {
        for (Guild g : getGuildes().values())
            if (g.get_name().equalsIgnoreCase(name))
                return true;
        return false;
    }

    public static boolean guildEmblemIsUsed(String emb) {
        for (Guild g : getGuildes().values()) {
            if (g.get_emblem().equals(emb))
                return true;
        }
        return false;
    }

    public static Guild getGuild(int i) {
        return getGuildes().get(i);
    }

    public static boolean guildExist(int i) {
        return getGuildes().containsKey(i);
    }

    public static long getGuildXpMax(int _lvl) {
        if (_lvl >= 200)
            _lvl = 199;
        if (_lvl <= 1)
            _lvl = 1;
        return getExpLevels().get(_lvl + 1).guilde;
    }

    public static void ReassignAccountToChar(Account C) {
        C.getPlayers().clear();
        SQLManager.LOAD_PERSO_BY_ACCOUNT(C.getGuid());
        Map<Integer, Player> persos = new TreeMap<Integer, Player>();
        synchronized (getPersos()) {
            persos.putAll(getPersos());
        }
        for (Player P : persos.values()) {
            if (P.getAccID() == C.getGuid()) {
                C.addPerso(P);
                P.setAccount(C);
            }
        }
    }

    public static int getZaapCellIdByMapId(short i) {
        for (Entry<Integer, Integer> zaap : Constant.ZAAPS.entrySet()) {
            if (zaap.getKey() == i)
                return zaap.getValue();
        }
        return -1;
    }

    public static int getEncloCellIdByMapId(short i) {
        if (World.getCarte(i).getMountPark() != null) {
            if (World.getCarte(i).getMountPark().get_cellid() > 0) {
                return World.getCarte(i).getMountPark().get_cellid();
            }
        }

        return -1;
    }

    public static void delDragoByID(int getId) {
        getDragodindes().remove(getId);
    }

    public static void removeGuild(int id) {
        if (World.getGuild(id) == null)
            return;
        // Maison de guilde+SQL
        Houses.removeHouseGuild(id);
        // Enclo+SQL
        Maps.MountPark.removeMountPark(id);
        // Percepteur+SQL
        Collector.removePercepteur(id);
        // Guilde
        getGuildes().remove(id);
        SQLManager.DEL_ALL_GUILDMEMBER(id);// Supprime les membres
        SQLManager.DEL_GUILD(id);// Supprime la guilde
    }

    public static boolean ipIsUsed(String ip) {
        for (Account c : getComptes().values())
            if (c.getCurIp().compareTo(ip) == 0)
                return true;
        return false;
    }

    public static void unloadPerso(int g) {
        Player toRem;
        synchronized (getPersos()) {
            toRem = getPersos().get(g);
        }
        if (toRem == null)
            return;
        if (!toRem.getItems().isEmpty()) {
            for (Entry<Integer, Item> curObj : toRem.getItems().entrySet()) {
                getObjets().remove(curObj.getKey());
            }
        }
        // Persos.remove(g);
    }

    public static boolean isArenaMap(int mapID) {
        for (int curID : Config.ARENA_MAPS) {
            if (curID == mapID)
                return true;
        }
        return false;
    }

    public static Item newObjet(int Guid, int template, int qua, int pos, String strStats) {
        if (World.getObjTemplate(template) == null) {
            System.out.println("ItemTemplate " + template
                    + " inexistant, GUID dans la table `items`:" + Guid);
            Reboot.reboot();
        }

        if (World.getObjTemplate(template).getType() == 85)
            return new SoulStone(Guid, qua, template, pos, strStats);
        else
            return new Item(Guid, template, qua, pos, strStats, World.getObjTemplate(template).getPrestige());
    }

    public static short get_state() {
        return _state;
    }

    public static void set_state(short state) {
        _state = state;
    }

    public static byte getGmAccess() {
        return _GmAccess;
    }

    public static void setGmAccess(byte GmAccess) {
        _GmAccess = GmAccess;
    }

    public static AuctionHouse getHdv(int mapID) {
        return getHdvs().get(mapID);
    }

    public synchronized static int getNextHdvID()// ATTENTION A NE PAS EXECUTER
    // POUR RIEN CETTE METHODE
    // CHANGE LE PROCHAIN ID DE
    // L'HDV LORS DE SON
    // EXECUTION
    {
        nextHdvID++;
        return nextHdvID;
    }

    public synchronized static void setNextHdvID(int nextID) {
        nextHdvID = nextID;
    }

    public synchronized static int getNextLigneID() {
        nextLigneID++;
        return nextLigneID;
    }

    public synchronized static void setNextLigneID(int ligneID) {
        nextLigneID = ligneID;
    }

    public static void addHdvItem(int compteID, int hdvID, HdvEntry toAdd) {
        if (get_hdvsItems().get(compteID) == null) // Si le compte n'est pas
            // dans la memoire
            get_hdvsItems().put(compteID,
                    new HashMap<Integer, ArrayList<HdvEntry>>()); // Ajout du
        // compte
        // clé:compteID
        // et un
        // nouveau
        // map<hdvID,items<>>

        if (get_hdvsItems().get(compteID).get(hdvID) == null)
            get_hdvsItems().get(compteID).put(hdvID, new ArrayList<HdvEntry>());

        get_hdvsItems().get(compteID).get(hdvID).add(toAdd);
    }

    public static void removeHdvItem(int compteID, int hdvID, HdvEntry toDel) {
        get_hdvsItems().get(compteID).get(hdvID).remove(toDel);
    }

    public static int getHdvNumber() {
        return getHdvs().size();
    }

    public static int getHdvObjetsNumber() {
        int size = 0;

        for (Map<Integer, ArrayList<HdvEntry>> curCompte : get_hdvsItems()
                .values()) {
            for (ArrayList<HdvEntry> curHdv : curCompte.values()) {
                size += curHdv.size();
            }
        }
        return size;
    }

    public static void addHdv(AuctionHouse toAdd) {
        getHdvs().put(toAdd.getHdvID(), toAdd);
    }

    public static Map<Integer, ArrayList<HdvEntry>> getMyItems(int compteID) {
        if (get_hdvsItems().get(compteID) == null)// Si le compte n'est pas dans
            // la memoire
            get_hdvsItems().put(compteID,
                    new HashMap<Integer, ArrayList<HdvEntry>>());// Ajout du
        // compte
        // clé:compteID
        // et un
        // nouveau
        // map<hdvID,items

        return get_hdvsItems().get(compteID);
    }

    public static Collection<ObjTemplate> getObjTemplates() {
        return ObjTemplates.values();
    }

    // PNJ Échangeur boutique @Flow
    private static void updateListeEchangeItem() {
        _listeEchangeItem.put(22241, 27);
        _listeEchangeItem.put(22242, 27);
        _listeEchangeItem.put(22243, 27);
        _listeEchangeItem.put(22244, 27);
        _listeEchangeItem.put(22267, 27);
        _listeEchangeItem.put(22268, 27);
        _listeEchangeItem.put(22269, 27);
        _listeEchangeItem.put(22270, 27);
        _listeEchangeItem.put(22261, 27);
        _listeEchangeItem.put(22262, 27);
        _listeEchangeItem.put(22263, 27);
        _listeEchangeItem.put(22264, 27);
        _listeEchangeItem.put(22273, 27);
        _listeEchangeItem.put(22274, 27);
        _listeEchangeItem.put(22275, 27);
        _listeEchangeItem.put(22276, 27);
        _listeEchangeItem.put(22266, 27);
        _listeEchangeItem.put(22272, 27);
        _listeEchangeItem.put(22278, 27);
        _listeEchangeItem.put(22279, 27);
        _listeEchangeItem.put(42108, 27);
        _listeEchangeItem.put(42107, 27);
        _listeEchangeItem.put(42106, 27);
        _listeEchangeItem.put(42105, 27);
        _listeEchangeItem.put(21582, 56);
        _listeEchangeItem.put(12051, 105);
        _listeEchangeItem.put(11625, 243);
    }

    public static Rune obtenirRune(int idRune) {
        Rune rune = null;
        if (runes.containsKey(idRune)) {
            rune = runes.get(idRune);
        }
        return rune;
    }

    public static void definirRunes(HashMap<Integer, Rune> runes) {
        World.runes = runes;
    }

    public static int EchangeItemValue(int guid) {
        if (_listeEchangeItem.containsKey(guid)) {
            return _listeEchangeItem.get(guid);
        }
        return 0;
    }

    public static boolean mariageok() { // Le mariage est-il ok ?
        boolean a = false;
        boolean b = false;
        try {
            if (getMarried().get(1) != null)
                a = true;
            if (getMarried().get(2) != null)
                b = true;
        } catch (Exception e) {

        }
        if (a == true && b == true)
            return true;
        return false;
    }

    public static Player getMarried(int ordre) {
        return Married.get(ordre);
    }

    public static void AddMarried(int ordre, Player perso) {
        Player Perso = getMarried().get(ordre);
        if (Perso != null) {
            if (perso.getGuid() == Perso.getGuid()) // Si c'est le meme
                // joueur...
                return;
            if (Perso.isOnline())// Si perso en ligne...
            {
                getMarried().remove(ordre);
                getMarried().put(ordre, perso);
                return;
            }

            return;
        } else {
            getMarried().put(ordre, perso);
            return;
        }
    }

    public static void PriestRequest(Player perso, Maps carte, int IdPretre) {
        Player Homme = getMarried().get(0);
        Player Femme = getMarried().get(1);
        if (Homme.getWife() != 0) {
            SocketManager.GAME_SEND_MESSAGE_TO_MAP(carte, Homme.getName()
                    + " est deja marier!", Config.CONFIG_MOTD_COLOR);
            return;
        }
        if (Femme.getWife() != 0) {
            SocketManager.GAME_SEND_MESSAGE_TO_MAP(carte, Femme.getName()
                    + " est deja marier!", Config.CONFIG_MOTD_COLOR);
            return;
        }
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(perso.getMap(), "", -1,
                "Prêtre", perso.getName() + " acceptez-vous d'épouser "
                        + getMarried((perso.get_sexe() == 1 ? 0 : 1)).getName()
                        + " ?");
        SocketManager.GAME_SEND_WEDDING(carte, 617,
                (Homme == perso ? Homme.getGuid() : Femme.getGuid()),
                (Homme == perso ? Femme.getGuid() : Homme.getGuid()), IdPretre);
    }

    public static void Wedding(Player Homme, Player Femme, int isOK) {
        if (isOK > 0) {
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(Homme.getMap(), "", -1,
                    "Prêtre",
                    "Je déclare " + Homme.getName() + " et " + Femme.getName()
                            + " unis par les liens sacrés du mariage.");
            Homme.MarryTo(Femme);
            Femme.MarryTo(Homme);
        } else {
            SocketManager.GAME_SEND_Im_PACKET_TO_MAP(Homme.getMap(), "048;"
                    + Homme.getName() + "~" + Femme.getName());
        }
        getMarried().get(0).setisOK(0);
        getMarried().get(1).setisOK(0);
        getMarried().clear();
    }

    public static Hustle getAnimation(int AnimationId) {
        return getAnimations().get(AnimationId);
    }

    public static void addAnimation(Hustle animation) {
        getAnimations().put(animation.getId(), animation);
    }

    public static void addHouse(Houses house) {
        getHouse().put(house.get_id(), house);
    }

    public static Map<Integer, Houses> getHouses() {
        return getHouse();
    }

    public static Houses getHouse(int id) {
        return House.get(id);
    }

    public static void addPerco(Collector perco) {
        getPercepteurs().put(perco.getGuid(), perco);
    }

    public static Collector getPerco(int percoID) {
        return getPercepteurs().get(percoID);
    }

    public static Map<Integer, Collector> getPercos() {
        return getPercepteurs();
    }

    public static void addTrunk(Trunk trunk) {
        getTrunks().put(trunk.get_id(), trunk);
    }

    public static Trunk getTrunk(int id) {
        return getTrunks().get(id);
    }

    public static Map<Integer, Trunk> getTrunks() {
        return Trunks;
    }

    public static void addMountPark(Maps.MountPark mp) {
        getMountPark().put(mp.get_map().get_id(), mp);
    }

    public static Map<Short, Maps.MountPark> getMountPark() {
        return MountPark;
    }

    public static String parseMPtoGuild(int GuildID) {
        Guild G = World.getGuild(GuildID);
        byte enclosMax = (byte) Math.floor(G.get_lvl() / 10);
        StringBuilder packet = new StringBuilder();
        packet.append(enclosMax);

        for (Entry<Short, Maps.MountPark> mp : getMountPark().entrySet()) {
            if (mp.getValue().get_guild() != null
                    && mp.getValue().get_guild().get_id() == GuildID) {
                packet.append("|").append(mp.getValue().get_map().get_id())
                        .append(";").append(mp.getValue().get_size())
                        .append(";").append(mp.getValue().getObjectNumb());// Nombre
                // d'objets
                // pour
                // le
                // dernier
            } else {
                continue;
            }
        }
        return packet.toString();
    }

    public static int totalMPGuild(int GuildID) {
        int i = 0;
        for (Entry<Short, Maps.MountPark> mp : getMountPark().entrySet()) {
            if (mp.getValue().get_guild().get_id() == GuildID) {
                i++;
            } else {
                continue;
            }
        }
        return i;
    }

    public static void addSeller(Player p) {
        if (Seller.get(p.getMap().get_id()) == null) {
            ArrayList<Integer> PersoID = new ArrayList<Integer>();
            PersoID.add(p.getGuid());
            Seller.put(p.getMap().get_id(), PersoID);
        } else {
            ArrayList<Integer> PersoID = new ArrayList<Integer>();
            PersoID.addAll(Seller.get(p.getMap().get_id()));
            PersoID.add(p.getGuid());
            Seller.remove(p.getMap().get_id());
            Seller.put(p.getMap().get_id(), PersoID);
            //SocketManager.GAME_SEND_MESSAGE_TO_ALL2((new StringBuilder("<b> ")).append("DEBUG").append("</b> : ").append("Un nouveau marchand a été ajouté. Est-ce que le client a bien traité le packet ?").toString(), "0BF9B7");
        }
    }

    public static Collection<Integer> getSeller(short mapID) {
        return Seller.get(mapID);
    }

    public static void removeSeller(int pID, short mapID) {
        getSeller().get(mapID).remove(pID);
    }

    public static boolean isRestrictedMap(short mapid) {
        if (restrictedMaps == null) {
            restrictedMaps = new ArrayList<Short>();
            for (String map : Config.RESTRICTED_MAPS.split(",")) {
                if (map.isEmpty())
                    continue;
                restrictedMaps.add(Short.parseShort(map));
            }
        }
        return restrictedMaps.contains(mapid);
    }

    public static void verifyClone(Player p) {
        if (p.get_curCell() != null && p.getFight() == null) {
            if (p.get_curCell().getPersos().containsKey(p.getGuid())) {
                p.set_Online(false);
                Logs.addToDebug(p.getName() + " avait un clone.");
                p.get_curCell().removePlayer(p.getGuid());
                SQLManager.SAVE_PERSONNAGE(p, true);
            }
        }
        if (p.isOnline()) {
            p.set_Online(false);
            SQLManager.SAVE_PERSONNAGE(p, true);
        }
    }

    public static void Banip(Account c, int nb_heures) {
        // Action de bannir une ip
        if (nb_heures < 0)
            return;
        if (nb_heures == 0)
            nb_heures = -1;
        else
            nb_heures = nb_heures * 3600;
        String ip = c.getCurIp();
        if (ip.equals(""))
            ip = c.getLastIp();
        if (ip.equals(""))
            return;
        SQLManager.LOAD_ACCOUNT_BY_IP(ip);
        BanIp ban;
        if (nb_heures == -1)
            ban = new BanIp(ip, nb_heures);
        else
            ban = new BanIp(ip,
                    (long) (System.currentTimeMillis() / 1000 + nb_heures));
        getBanips().add(ban);
        SQLManager.ADD_BANIP(ip, ban.getTime());
        for (Account compte : getComptes().values()) {
            if ((compte.getLastIp().equals(ip) || compte.getCurIp().equals(ip))
                    && compte.getGmLevel() < 4) {
                compte.ban(ban.getTime(), true);
                if (compte.getGameThread() != null)
                    compte.getGameThread().kick();
            }
        }
    }

    public static void unBanip(Account c) {
        String ip = c.getLastIp();
        if (ip.equals(""))
            return;
        c.unBan();
        SQLManager.LOAD_ACCOUNT_BY_IP(ip);
        removeBanip(ip);
        SQLManager.REMOVE_BANIP(ip);
        for (Account compte : getComptes().values()) {
            if (compte.getLastIp().equals(ip) || compte.getCurIp().equals(ip)) {
                compte.unBan();
            }
        }
    }

    public static void addBanip(String ip, long time) {
        if (time < System.currentTimeMillis() / 1000 && time != -1) {
            SQLManager.REMOVE_BANIP(ip);
            return;
        }
        getBanips().add(new BanIp(ip, time));
    }

    public static void removeBanip(String ip) {
        ArrayList<BanIp> bans = new ArrayList<BanIp>();
        for (BanIp ban : getBanips()) {
            if (!ban.isBanned()) {
                SQLManager.REMOVE_BANIP(ban.getIp());
                continue;
            }
            if (ban.getIp().equalsIgnoreCase(ip))
                continue;
            bans.add(ban);
        }
        getBanips().clear();
        getBanips().addAll(bans);
    }

    public static boolean isIpBanned(String ip, Account c) {
        boolean isIpBanned = false;
        ArrayList<BanIp> bans = new ArrayList<BanIp>();
        for (BanIp ban : getBanips()) {
            if (!ban.isBanned()) {
                SQLManager.REMOVE_BANIP(ban.getIp());
                continue;
            }
            if (ban.getIp().equalsIgnoreCase(ip)) {
                if (!c.isBanned())
                    c.ban(ban.getTime(), true);
                isIpBanned = true;
            }
            bans.add(ban);
        }
        getBanips().clear();
        getBanips().addAll(bans);
        return isIpBanned;
    }

    public static class BanIp {
        private String _ip;
        private long _time;

        public BanIp(String ip, long time) {
            _ip = ip;
            _time = time;
        }

        public String getIp() {
            return _ip;
        }

        public long getTime() {
            return _time;
        }

        public boolean isBanned() {
            if (_time == -1)
                return true;
            if (_time < System.currentTimeMillis() / 1000)
                return false;
            return true;
        }
    }

    public static double getBalanceArea(Area area, int alignement) {
        int cant = 0;
        for (SubArea subarea : getSubAreas().values()) {
            if (subarea.getArea() == area
                    && subarea.getalignement() == alignement)
                cant++;
        }
        if (cant == 0)
            return 0;
        return Math.rint((1000 * cant / (area.getSubAreas().size())) / 10);
    }

    public static double getBalanceMundo(int alignement) {
        int cant = 0;
        for (SubArea subarea : getSubAreas().values()) {
            if (subarea.getalignement() == alignement)
                cant++;
        }
        if (cant == 0)
            return 0;
        return Math.rint((10 * cant / 4) / 10);
    }

    public static Map<Integer, Account> getComptes() {
        return Comptes;
    }

    public static void setComptes(Map<Integer, Account> comptes) {
        Comptes = comptes;
    }

    public static ArrayList<BanIp> getBanips() {
        return Banips;
    }

    public static void setBanips(ArrayList<BanIp> banips) {
        Banips = banips;
    }

    public static Map<String, Integer> getComptebyName() {
        return ComptebyName;
    }

    public static void setComptebyName(Map<String, Integer> comptebyName) {
        ComptebyName = comptebyName;
    }

    public static StringBuilder getChallenges() {
        return Challenges;
    }

    public static void setChallenges(StringBuilder challenges) {
        Challenges = challenges;
    }

    public static Map<Integer, Player> getPersos() {
        return Persos;
    }

    public static void setPersos(Map<Integer, Player> persos) {
        Persos = persos;
    }

    public static Map<Short, Maps> getCartes() {
        return Cartes;
    }

    public static void setCartes(Map<Short, Maps> cartes) {
        Cartes = cartes;
    }

    public static Map<Integer, Item> getObjets() {
        return Objets;
    }

    public static Map<Integer, ExpLevel> getExpLevels() {
        return ExpLevels;
    }

    public static void setExpLevels(Map<Integer, ExpLevel> expLevels) {
        ExpLevels = expLevels;
    }

    public static Map<Integer, Spell> getSorts() {
        return Sorts;
    }

    public static void setSorts(Map<Integer, Spell> sorts) {
        Sorts = sorts;
    }

    public static void setObjTemplates(Map<Integer, ObjTemplate> objTemplates) {
        ObjTemplates = objTemplates;
    }

    public static Map<Integer, Monster> getMobTemplates() {
        return MobTemplates;
    }

    public static void setMobTemplates(Map<Integer, Monster> mobTemplates) {
        MobTemplates = mobTemplates;
    }

    public static Map<Integer, NpcTemplate> getNPCTemplates() {
        return NPCTemplates;
    }

    public static void setNPCTemplates(Map<Integer, NpcTemplate> nPCTemplates) {
        NPCTemplates = nPCTemplates;
    }

    public static Map<Integer, NPC_question> getNPCQuestions() {
        return NPCQuestions;
    }

    public static void setNPCQuestions(Map<Integer, NPC_question> nPCQuestions) {
        NPCQuestions = nPCQuestions;
    }

    public static Map<Integer, NPC_reponse> getNPCReponses() {
        return NPCReponses;
    }

    public static void setNPCReponses(Map<Integer, NPC_reponse> nPCReponses) {
        NPCReponses = nPCReponses;
    }

    public static Map<Integer, IOTemplate> getIOTemplate() {
        return IOTemplate;
    }

    public static void setIOTemplate(Map<Integer, IOTemplate> nIOTemplate) {
        IOTemplate = nIOTemplate;
    }

    public static Map<Integer, Mount> getDragodindes() {
        return Dragodindes;
    }

    public static void setDragodindes(Map<Integer, Mount> dragodindes) {
        Dragodindes = dragodindes;
    }

    public static Map<Integer, SuperArea> getSuperAreas() {
        return SuperAreas;
    }

    public static void setSuperAreas(Map<Integer, SuperArea> superAreas) {
        SuperAreas = superAreas;
    }

    public static Map<Integer, Area> getAreas() {
        return Areas;
    }

    public static void setAreas(Map<Integer, Area> areas) {
        Areas = areas;
    }

    public static Map<Integer, SubArea> getSubAreas() {
        return SubAreas;
    }

    public static void setSubAreas(Map<Integer, SubArea> subAreas) {
        SubAreas = subAreas;
    }

    public static Map<Integer, Job> getJob() {
        return Job;
    }

    public static void setJob(Map<Integer, Job> job) {
        Job = job;
    }

    public static Map<Integer, ArrayList<Couple<Integer, Integer>>> getCrafts() {
        return Crafts;
    }

    public static void setCrafts(
            Map<Integer, ArrayList<Couple<Integer, Integer>>> crafts) {
        Crafts = crafts;
    }

    public static Map<Integer, ItemSet> getItemSets() {
        return ItemSets;
    }

    public static void setItemSets(Map<Integer, ItemSet> itemSets) {
        ItemSets = itemSets;
    }

    public static Map<Integer, Guild> getGuildes() {
        return Guildes;
    }

    public static void setGuildes(Map<Integer, Guild> guildes) {
        Guildes = guildes;
    }

    public static Map<Integer, AuctionHouse> getHdvs() {
        return Hdvs;
    }

    public static void setHdvs(Map<Integer, AuctionHouse> hdvs) {
        Hdvs = hdvs;
    }

    public static Map<Integer, Map<Integer, ArrayList<HdvEntry>>> get_hdvsItems() {
        return _hdvsItems;
    }

    public static void set_hdvsItems(
            Map<Integer, Map<Integer, ArrayList<HdvEntry>>> _hdvsItems) {
        World._hdvsItems = _hdvsItems;
    }

    public static Map<Integer, Player> getMarried() {
        return Married;
    }

    public static void setMarried(Map<Integer, Player> married) {
        Married = married;
    }

    public static Map<Integer, Hustle> getAnimations() {
        return Animations;
    }

    public static void setAnimations(Map<Integer, Hustle> animations) {
        Animations = animations;
    }

    public static void setMountPark(Map<Short, Maps.MountPark> mountPark) {
        MountPark = mountPark;
    }

    public static void setTrunks(Map<Integer, Trunk> trunks) {
        Trunks = trunks;
    }

    public static Map<Integer, Collector> getPercepteurs() {
        return Percepteurs;
    }

    public static void setPercepteurs(Map<Integer, Collector> percepteurs) {
        Percepteurs = percepteurs;
    }

    public static Map<Integer, Houses> getHouse() {
        return House;
    }

    public static void setHouse(Map<Integer, Houses> house) {
        House = house;
    }

    public static Map<Short, Collection<Integer>> getSeller() {
        return Seller;
    }

    public static void setSeller(Map<Short, Collection<Integer>> seller) {
        Seller = seller;
    }

    public static void clearAllVar() {
        Comptes.clear();
        Banips.clear();
        ComptebyName.clear();
        Challenges = null;
        Persos.clear();
        Cartes.clear();
        Objets.clear();
        ExpLevels.clear();
        Sorts.clear();
        ObjTemplates.clear();
        MobTemplates.clear();
        NPCTemplates.clear();
        NPCQuestions.clear();
        IOTemplate.clear();
        Dragodindes.clear();
        SuperAreas.clear();
        Areas.clear();
        SubAreas.clear();
        Job.clear();
        Crafts.clear();
        ItemSets.clear();
        Guildes.clear();
        Hdvs.clear();
        _hdvsItems.clear();
        Married.clear();
        Animations.clear();
        MountPark.clear();
        Trunks.clear();
        Percepteurs.clear();
        House.clear();
        Seller.clear();
    }

    //Quests
    public static void addQuest(int questId, String steps, int startQuestion, int endQuestion,
                                int minLvl, int questRequired, int unique) {
        if (quests.get(questId) != null)
            return;

        quests.put(questId, new HashMap<String, String>());
        quests.get(questId).put("steps", steps);
        quests.get(questId).put("startQuestion", startQuestion + "");
        quests.get(questId).put("endQuestion", endQuestion + "");
        quests.get(questId).put("minLvl", minLvl + "");
        quests.get(questId).put("questRequired", questRequired + "");
        quests.get(questId).put("unique", unique + "");
    }

    public static void addQuestStep(int stepId, String objectives,
                                    int question, int gainExp, int gainKamas, String gainItems) {
        if (questSteps.get(stepId) != null)
            return;
        questSteps.put(stepId, new HashMap<String, String>());
        questSteps.get(stepId).put("objectives", objectives);
        questSteps.get(stepId).put("question", question + "");
        questSteps.get(stepId).put("gainExp", gainExp + "");
        questSteps.get(stepId).put("gainKamas", gainKamas + "");
        questSteps.get(stepId).put("gainItems", gainItems);
    }

    public static void addQuestObjective(int id, String type, String args,
                                         int npcTarget, int questionTarget, int answerTarget) {
        if (questObjetives.get(id) != null)
            return;
        questObjetives.put(id, new HashMap<String, String>());
        questObjetives.get(id).put("type", type);
        questObjetives.get(id).put("args", args);
        questObjetives.get(id).put("npcTarget", npcTarget + "");
        questObjetives.get(id).put("optQuestion", questionTarget + "");
        questObjetives.get(id).put("optAnswer", answerTarget + "");
    }

    public static Map<String, String> getQuest(int questId) {
        return quests.get(questId);
    }

    public static Map<String, String> getStep(int StepId) {
        return questSteps.get(StepId);
    }

    public static Map<String, String> getObjective(int objectiveId) {
        return questObjetives.get(objectiveId);
    }

    public static Map<Integer, Map<String, String>> getObjectiveByNpcTarget(int npcId) {
        Map<Integer, Map<String, String>> results = new HashMap<Integer, Map<String, String>>();

        for (Entry<Integer, Map<String, String>> entry : questObjetives.entrySet()) {
            if (entry.getValue().get("npcTarget").equals(npcId + "")) {
                results.put(entry.getKey(), entry.getValue());
            }
        }
        if (!results.isEmpty())
            return results;

        return null; // Aucun objectif avec le pnj
    }

    public static Map<Integer, Map<String, String>> getObjectiveByOptAnswer(int answerId) {

        Map<Integer, Map<String, String>> results = new HashMap<Integer, Map<String, String>>();

        for (Entry<Integer, Map<String, String>> entry : questObjetives.entrySet()) {
            if (entry.getValue().get("optAnswer").equals(answerId + ""))
                results.put(entry.getKey(), entry.getValue());
        }
        if (!results.isEmpty())
            return results;

        return null; // Aucun objectif avec le pnj
    }

    public static Entry<Integer, Map<String, String>> getObjectiveByOptQuestion(int questionId) {

        for (Entry<Integer, Map<String, String>> entry : questObjetives.entrySet()) {
            if (entry.getValue().get("optQuestion").equals(questionId + "")) {
                return entry;
            }
        }

        return null; // Aucun objectif avec le pnj
    }

    //quests
    public static NPC_question getNPCQuestion(int guid, Player perso) {
        NPC_question baseQuestion = NPCQuestions.get(guid);

        Entry<Integer, Map<String, String>> questObjective = getObjectiveByOptQuestion(guid);
        if (questObjective != null && perso.hasObjective(questObjective.getKey())) // Il y a un objectif de quete avec cette question
        {
            NPC_question questQuestion = new NPC_question(guid, questObjective.getValue().get("optAnswer"), "", "", 0);
            return questQuestion;
        }

        return baseQuestion;
    }

    public static void MoveMobsOnMaps() {
        for (Maps map : Cartes.values()) {
            if (map == null || map.getPersos().size() <= 0) continue;
            map.AllDeplacement();
        }
    }

    public static void PnjParoleOnMaps() {
        for (Maps map : Cartes.values()) {
            if (map == null || map.getPersos().size() <= 0) continue;
            map.PnjParle();
        }
    }

    public static void ajouterPrixOrnements(int id, int prix) {
        if (!prixOrnements.containsKey(id)) {
            prixOrnements.put(id, prix);
        }
    }

    public static void clearPrixOrnements() {
        prixOrnements.clear();
    }

    public static int obtenirPrixOrnement(int id) {
        if (prixOrnements.containsKey(id)) {
            return prixOrnements.get(id);
        }
        return 0;
    }

    public static String obtenirListePrixData() {
        String toReturn = "";
        try {
            for (int i = 0; i < prixOrnements.size(); i++) {
                toReturn += "" + prixOrnements.get(i) + ",";
            }
        } catch (Exception e) {
        }
        return toReturn;
    }

}
