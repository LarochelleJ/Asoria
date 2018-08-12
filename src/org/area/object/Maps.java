package org.area.object;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.Timer;

import org.area.client.Player;
import org.area.common.ConditionParser;
import org.area.common.Constant;
import org.area.common.CryptManager;
import org.area.common.Formulas;
import org.area.common.Pathfinding;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.IOTemplate;
import org.area.common.World.SubArea;
import org.area.event.Event;
import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.fight.object.Collector;
import org.area.fight.object.Monster.MobGrade;
import org.area.fight.object.Monster.MobGroup;
import org.area.fight.object.Prism;
import org.area.game.GameServer;
import org.area.game.GameThread.GameAction;
import org.area.kernel.Config;
import org.area.kernel.Reboot;
import org.area.kolizeum.Kolizeum;
import org.area.object.NpcTemplate.NPC;


public class Maps {
    private short _id;
    private String _date;
    private byte _w;
    private byte _h;
    private String _key;
    private String _placesStr;
    private Map<Integer, Case> _cases = new TreeMap<Integer, Case>();
    private Map<Integer, Player> _persos = new TreeMap<Integer, Player>();
    private Map<Integer, Fight> _fights = new TreeMap<Integer, Fight>();
    private ArrayList<MobGrade> _mobPossibles = new ArrayList<MobGrade>();
    private Map<Integer, MobGroup> _mobGroups = new TreeMap<Integer, MobGroup>();
    private Map<Integer, MobGroup> _fixMobGroups = new TreeMap<Integer, MobGroup>();
    private Map<Integer, NPC> _npcs = new TreeMap<Integer, NPC>();
    private int nextObjectID = -1;
    private byte _X = 0;
    private byte _Y = 0;
    private SubArea _subArea;
    private MountPark _mountPark;
    private byte _maxGroup = 3;
    private Map<Integer, ArrayList<Action>> _endFightAction = new TreeMap<Integer, ArrayList<Action>>();
    private byte _maxSize;
    private ArrayList<Integer> atelier = new ArrayList<Integer>();
    private long _muteTime = -1;
    private Map<Integer, Integer> _parleTime = new HashMap<Integer, Integer>();
    private long _tempsPourPosePercepteur;

    public static class MountPark {
        private int _owner;
        private InteractiveObject _door;
        private int _size;
        private ArrayList<Case> _cases = new ArrayList<Case>();
        private Guild _guild;
        private Maps _map;
        private int _cellid = -1;
        private int _price;
        private Map<Integer, Integer> MountParkDATA = new TreeMap<Integer, Integer>();//DragoID, IDperso

        public MountPark(int owner, Maps map, int cellid, int size, String data, int guild, int price) {
            _owner = owner;
            _door = map.getMountParkDoor();
            _size = size;
            _guild = World.getGuild(guild);
            _map = map;
            _cellid = cellid;
            _price = price;
            if (_map != null) _map.setMountPark(this);
            for (String firstCut : data.split(";"))//PosseseurID,DragoID;PosseseurID2,DragoID2;PosseseurID,DragoID3
            {
                try {
                    String[] secondCut = firstCut.split(",");
                    Mount DD = World.getDragoByID(Integer.parseInt(secondCut[1]));
                    if (DD == null) continue;
                    MountParkDATA.put(Integer.parseInt(secondCut[1]), Integer.parseInt(secondCut[0]));
                } catch (Exception E) {
                }
                ;
            }
        }

        public int get_owner() {
            return _owner;
        }

        public void set_owner(int AccID) {
            _owner = AccID;
        }

        public InteractiveObject get_door() {
            return _door;
        }

        public int get_size() {
            return _size;
        }

        public Guild get_guild() {
            return _guild;
        }

        public void set_guild(Guild guild) {
            _guild = guild;
        }

        public Maps get_map() {
            return _map;
        }

        public int get_cellid() {
            return _cellid;
        }

        public int get_price() {
            return _price;
        }

        public void set_price(int price) {
            _price = price;
        }

        public int getObjectNumb() {
            int n = 0;
            for (Case C : _cases) if (C.getObject() != null) n++;
            return n;
        }

        public String parseData(int PID, boolean isPublic) {
            if (MountParkDATA.isEmpty()) return "~";

            StringBuilder packet = new StringBuilder();
            for (Entry<Integer, Integer> MPdata : MountParkDATA.entrySet()) {
                if (MPdata.getValue() == PID)//Montrer que ses montures uniquement en public
                {
                    if (packet.length() > 0) packet.append(";");
                    packet.append(World.getDragoByID(MPdata.getKey()).parse());
                } else if (!isPublic) {
                    //if(packet.length() > 0)packet.append(";");
                    //packet.append(World.getDragoByID(MPdata.getKey()).parse());
                }
            }
            return packet.toString();
        }

        public String parseDBData() {
            StringBuilder str = new StringBuilder();
            if (MountParkDATA.isEmpty()) return "";

            for (Entry<Integer, Integer> MPdata : MountParkDATA.entrySet()) {
                if (str.length() > 0) str.append(";");
                str.append(MPdata.getValue()).append(",").append(World.getDragoByID(MPdata.getKey()).get_id());
            }
            return str.toString();
        }

        public void addData(int DID, int PID) {
            MountParkDATA.put(DID, PID);
        }

        public void removeData(int DID) {
            MountParkDATA.remove(DID);
        }

        public Map<Integer, Integer> getData() {
            return MountParkDATA;
        }

        public int MountParkDATASize() {
            return MountParkDATA.size();
        }

        public static void removeMountPark(int GuildID) {
            try {
                for (Entry<Short, MountPark> mp : World.getMountPark().entrySet())//Pour chaque enclo si ils en ont plusieurs
                {
                    if (mp.getValue().get_guild().get_id() == GuildID) {
                        if (!mp.getValue().getData().isEmpty()) {
                            for (Entry<Integer, Integer> MPdata : mp.getValue().getData().entrySet()) {
                                World.removeDragodinde(MPdata.getKey());//Suppression des dindes dans le world
                                SQLManager.REMOVE_MOUNT(MPdata.getKey());//Suppression des dindes dans chaque enclo
                            }
                        }
                        mp.getValue().getData().clear();
                        mp.getValue().set_owner(0);
                        mp.getValue().set_guild(null);
                        mp.getValue().set_price(3000000);
                        SQLManager.SAVE_MOUNTPARK(mp.getValue());
                        for (Player p : mp.getValue().get_map().getPersos()) {
                            SocketManager.GAME_SEND_Rp_PACKET(p, mp.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class InteractiveObject {
        private int _id;
        private int _state;
        private Maps _map;
        private Case _cell;
        private boolean _interactive = true;
        private Timer _respawnTimer;
        private IOTemplate _template;

        public InteractiveObject(Maps a_map, Case a_cell, int a_id) {
            _id = a_id;
            _map = a_map;
            _cell = a_cell;
            _state = Constant.IOBJECT_STATE_FULL;
            int respawnTime = 10000;
            _template = World.getIOTemplate(_id);
            if (_template != null) respawnTime = _template.getRespawnTime();
            //définition du timer
            _respawnTimer = new Timer(respawnTime,
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            _respawnTimer.stop();
                            _state = Constant.IOBJECT_STATE_FULLING;
                            _interactive = true;
                            SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(_map, _cell);
                            _state = Constant.IOBJECT_STATE_FULL;
                        }
                    }
            );
        }

        public int getID() {
            return _id;
        }

        public boolean isInteractive() {
            return _interactive;
        }

        public void setInteractive(boolean b) {
            _interactive = b;
        }

        public int getState() {
            return _state;
        }

        public void setState(int state) {
            _state = state;
        }

        public int getUseDuration() {
            int duration = 1500;
            if (_template != null) {
                duration = _template.getDuration();
            }
            return duration;
        }

        public void startTimer() {
            if (_respawnTimer == null) return;
            _state = Constant.IOBJECT_STATE_EMPTY2;
            _respawnTimer.restart();
        }

        public int getUnknowValue() {
            int unk = 4;
            if (_template != null) {
                unk = _template.getUnk();
            }
            return unk;
        }

        public boolean isWalkable() {
            if (_template == null) return false;
            return _template.isWalkable() && _state == Constant.IOBJECT_STATE_FULL;
        }

        public Object getTemplate() {
            return getTemplate();
        }
    }

    public static class Case {
        private int _id;
        private Map<Integer, Player> _persos;        //= new TreeMap<Integer, Personnage>();
        private Map<Integer, Fighter> _fighters;    //= new TreeMap<Integer, Fighter>();
        private boolean _Walkable = true;
        private boolean _LoS = true;
        private short _map;
        private Maps _mapOb;
        //private ArrayList<Action> _onCellPass;
        //private ArrayList<Action> _onItemOnCell;
        private ArrayList<Action> _onCellStop;// = new ArrayList<Action>();
        private InteractiveObject _object;
        public Item _droppedItem;

        public Case(Maps a_map, int id, boolean _walk, boolean LoS, int objID) {
            _map = a_map.get_id();
            _mapOb = a_map;
            _id = id;
            _Walkable = _walk;
            _LoS = LoS;
            if (objID == -1) return;
            _object = new InteractiveObject(a_map, this, objID);
            a_map.add_atelier(Constant.JOB_ATELIER(objID));
        }

        public InteractiveObject getObject() {
            return _object;
        }

        public Item getDroppedItem() {
            return _droppedItem;
        }

        public boolean canDoAction(int id) {
            switch (id) {
                //Moudre et egrenner - Paysan
                case 122:
                case 47:
                    return _object.getID() == 7007;
                //Faucher Blé
                case 45:
                    switch (_object.getID()) {
                        case 7511://Blé
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Orge
                case 53:
                    switch (_object.getID()) {
                        case 7515://Orge
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;

                //Faucher Avoine
                case 57:
                    switch (_object.getID()) {
                        case 7517://Avoine
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Houblon
                case 46:
                    switch (_object.getID()) {
                        case 7512://Houblon
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Lin
                case 50:
                case 68:
                    switch (_object.getID()) {
                        case 7513://Lin
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Riz
                case 159:
                    switch (_object.getID()) {
                        case 7550://Riz
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Seigle
                case 52:
                    switch (_object.getID()) {
                        case 7516://Seigle
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Malt
                case 58:
                    switch (_object.getID()) {
                        case 7518://Malt
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Faucher Chanvre - Cueillir Chanvre
                case 69:
                case 54:
                    switch (_object.getID()) {
                        case 7514://Chanvre
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Scier - Bucheron
                case 101:
                    return _object.getID() == 7003;
                //Couper Frêne
                case 6:
                    switch (_object.getID()) {
                        case 7500://Frêne
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Châtaignier
                case 39:
                    switch (_object.getID()) {
                        case 7501://Châtaignier
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Noyer
                case 40:
                    switch (_object.getID()) {
                        case 7502://Noyer
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Chêne
                case 10:
                    switch (_object.getID()) {
                        case 7503://Chêne
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Oliviolet
                case 141:
                    switch (_object.getID()) {
                        case 7542://Oliviolet
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Bombu
                case 139:
                    switch (_object.getID()) {
                        case 7541://Bombu
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Erable
                case 37:
                    switch (_object.getID()) {
                        case 7504://Erable
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Bambou
                case 154:
                    switch (_object.getID()) {
                        case 7553://Bambou
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper If
                case 33:
                    switch (_object.getID()) {
                        case 7505://If
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Merisier
                case 41:
                    switch (_object.getID()) {
                        case 7506://Merisier
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Ebène
                case 34:
                    switch (_object.getID()) {
                        case 7507://Ebène
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Kalyptus
                case 174:
                    switch (_object.getID()) {
                        case 7557://Kalyptus
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Charme
                case 38:
                    switch (_object.getID()) {
                        case 7508://Charme
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Orme
                case 35:
                    switch (_object.getID()) {
                        case 7509://Orme
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Bambou Sombre
                case 155:
                    switch (_object.getID()) {
                        case 7554://Bambou Sombre
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Couper Bambou Sacré
                case 158:
                    switch (_object.getID()) {
                        case 7552://Bambou Sacré
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Puiser
                case 102:
                    switch (_object.getID()) {
                        case 7519://Puits
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Polir
                case 48:
                    return _object.getID() == 7005;//7510
                //Moule/Fondre - Mineur
                case 32:
                    return _object.getID() == 7002;
                //Miner Fer
                case 24:
                    switch (_object.getID()) {
                        case 7520://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Cuivre
                case 25:
                    switch (_object.getID()) {
                        case 7522://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Bronze
                case 26:
                    switch (_object.getID()) {
                        case 7523://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Kobalte
                case 28:
                    switch (_object.getID()) {
                        case 7525://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Manga
                case 56:
                    switch (_object.getID()) {
                        case 7524://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Sili
                case 162:
                    switch (_object.getID()) {
                        case 7556://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Etain
                case 55:
                    switch (_object.getID()) {
                        case 7521://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Argent
                case 29:
                    switch (_object.getID()) {
                        case 7526://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Bauxite
                case 31:
                    switch (_object.getID()) {
                        case 7528://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Or
                case 30:
                    switch (_object.getID()) {
                        case 7527://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Miner Dolomite
                case 161:
                    switch (_object.getID()) {
                        case 7555://Miner
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Fabriquer potion - Alchimiste
                case 23:
                    return _object.getID() == 7019;
                //Cueillir Trèfle
                case 71:
                    switch (_object.getID()) {
                        case 7533://Trèfle
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Cueillir Menthe
                case 72:
                    switch (_object.getID()) {
                        case 7534://Menthe
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Cueillir Orchidée
                case 73:
                    switch (_object.getID()) {
                        case 7535:// Orchidée
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Cueillir Edelweiss
                case 74:
                    switch (_object.getID()) {
                        case 7536://Edelweiss
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Cueillir Graine de Pandouille
                case 160:
                    switch (_object.getID()) {
                        case 7551://Graine de Pandouille
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Vider - Pêcheur
                case 133:
                    return _object.getID() == 7024;
                //Pêcher Petits poissons de mer
                case 128:
                    switch (_object.getID()) {
                        case 7530://Petits poissons de mer
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Petits poissons de rivière
                case 124:
                    switch (_object.getID()) {
                        case 7529://Petits poissons de rivière
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Pichon
                case 136:
                    switch (_object.getID()) {
                        case 7544://Pichon
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Ombre Etrange
                case 140:
                    switch (_object.getID()) {
                        case 7543://Ombre Etrange
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Poissons de rivière
                case 125:
                    switch (_object.getID()) {
                        case 7532://Poissons de rivière
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Poissons de mer
                case 129:
                    switch (_object.getID()) {
                        case 7531://Poissons de mer
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Gros poissons de rivière
                case 126:
                    switch (_object.getID()) {
                        case 7537://Gros poissons de rivière
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Gros poissons de mers
                case 130:
                    switch (_object.getID()) {
                        case 7538://Gros poissons de mers
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Poissons géants de rivière
                case 127:
                    switch (_object.getID()) {
                        case 7539://Poissons géants de rivière
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Pêcher Poissons géants de mer
                case 131:
                    switch (_object.getID()) {
                        case 7540://Poissons géants de mer
                            return _object.getState() == Constant.IOBJECT_STATE_FULL;
                    }
                    return false;
                //Boulanger
                case 109://Pain
                case 27://Bonbon
                    return _object.getID() == 7001;
                //Poissonier
                case 135://Faire un poisson (mangeable)
                    return _object.getID() == 7022;
                //Chasseur
                case 134:
                    return _object.getID() == 7023;
                //Boucher
                case 132:
                    return _object.getID() == 7025;
                case 157:
                    return (_object.getID() == 7030 || _object.getID() == 7031);
                case 44://Sauvegarder le Zaap
                case 114://Utiliser le Zaap
                    switch (_object.getID()) {
                        //Zaaps
                        case 7000:
                        case 7026:
                        case 7029:
                        case 4287:
                            return true;
                    }
                    return false;

                case 175://Accéder
                case 176://Acheter
                case 177://Vendre
                case 178://Modifier le prix de vente
                    switch (_object.getID()) {
                        //Enclos
                        case 6763:
                        case 6766:
                        case 6767:
                        case 6772:
                            return true;
                    }
                    return false;

                //Se rendre à incarnam
                case 183:
                    switch (_object.getID()) {
                        case 1845:
                        case 1853:
                        case 1854:
                        case 1855:
                        case 1856:
                        case 1857:
                        case 1858:
                        case 1859:
                        case 1860:
                        case 1861:
                        case 1862:
                        case 2319:
                            return true;
                    }
                    return false;

                //Enclume magique
                case 1:
                case 113:
                case 115:
                case 116:
                case 117:
                case 118:
                case 119:
                case 120:
                    return _object.getID() == 7020;

                //Enclume
                case 19:
                case 143:
                case 145:
                case 144:
                case 142:
                case 146:
                case 67:
                case 21:
                case 65:
                case 66:
                case 20:
                case 18:
                    return _object.getID() == 7012;

                //Costume Mage
                case 167:
                case 165:
                case 166:
                    return _object.getID() == 7036;

                //Coordo Mage
                case 164:
                case 163:
                    return _object.getID() == 7037;

                //Joai Mage
                case 168:
                case 169:
                    return _object.getID() == 7038;

                //Bricoleur
                case 171:
                case 182:
                    return _object.getID() == 7039;

                //Forgeur Bouclier
                case 156:
                    return _object.getID() == 7027;

                //Coordonier
                case 13:
                case 14:
                    return _object.getID() == 7011;

                //Tailleur (Dos)
                case 123:
                case 64:
                    return _object.getID() == 7015;


                //Sculteur
                case 17:
                case 16:
                case 147:
                case 148:
                case 149:
                case 15:
                    return _object.getID() == 7013;

                //Tailleur (Haut)
                case 63:
                    return (_object.getID() == 7014 || _object.getID() == 7016);
                //Atelier : Créer Amu // Anneau
                case 11:
                case 12:
                    return (_object.getID() >= 7008 && _object.getID() <= 7010);
                //Maison
                case 81://Vérouiller
                case 84://Acheter
                case 97://Entrer
                case 98://Vendre
                case 108://Modifier le prix de vente
                    return (_object.getID() >= 6700 && _object.getID() <= 6776);
                //Coffre
                case 104://Ouvrir
                case 105://Code
                    return (_object.getID() == 7350 || _object.getID() == 7351 || _object.getID() == 7353);
                case 170://Livre de métiers
                    return (_object.getID() == 7035);
                //Action ID non trouvé
                default:
                    GameServer.addToLog("MapActionID non existant dans Case.canDoAction: " + id);
                    return false;
            }
        }

        public int getID() {
            return _id;
        }

        public void addOnCellStopAction(int id, String args, String cond) {
            if (_onCellStop == null) _onCellStop = new ArrayList<Action>();

            _onCellStop.add(new Action(id, args, cond));
        }

        public void applyOnCellStopActions(Player perso) {
            if (_onCellStop == null) return;

            for (Action act : _onCellStop) {
                act.apply(perso, null, -1, -1);
            }
        }

        public boolean containsTrigger() {
            boolean containsTrigger = false;
            if (_onCellStop != null) {
                for (Action act : _onCellStop) {
                    if (act.getID() == 0) {
                        containsTrigger = true;
                        break;
                    }
                }
            }
            return containsTrigger;
        }

        public synchronized void addPerso(Player perso) {
            if (_persos == null) _persos = new TreeMap<Integer, Player>();
            _persos.put(perso.getGuid(), perso);
            _mapOb.addPerso(perso);

        }

        public synchronized void addFighter(Fighter fighter) {
            if (_fighters == null) _fighters = new TreeMap<Integer, Fighter>();
            _fighters.put(fighter.getGUID(), fighter);
        }

        public synchronized void removeFighter(Fighter fighter) {
            _fighters.remove(fighter.getGUID());
        }

        public boolean isWalkable(boolean useObject) {
            if (_object != null && useObject)
                return _Walkable && _object.isWalkable();
            return _Walkable;
        }

        public boolean blockLoS() {
            if (_fighters == null) return _LoS;
            boolean fighter = true;
            for (Entry<Integer, Fighter> f : _fighters.entrySet()) {
                if (!f.getValue().isHide()) fighter = false;
                if (f.getValue().isState(6)) fighter = false;
            }
            return _LoS && fighter;
        }

        public boolean isLoS() {
            return _LoS;
        }

        public synchronized void removePlayer(int _guid) {
            if (_persos == null) return;
            _persos.remove(_guid);
            _mapOb.removePerso(_guid);
            if (_persos.isEmpty()) _persos = null;
        }

        public Map<Integer, Player> getPersos() {
            if (_persos == null) return new TreeMap<Integer, Player>();
            Map<Integer, Player> p = new TreeMap<Integer, Player>();
            p.putAll(_persos);
            return p;
        }

        public Map<Integer, Fighter> getFighters() {
            if (_fighters == null) return new TreeMap<Integer, Fighter>();
            return _fighters;
        }

        public Fighter getFirstFighter() {
            if (_fighters == null)
                return null;
            for (Entry<Integer, Fighter> entry : _fighters.entrySet())
                return entry.getValue();
            return null;
        }

        public void startAction(Player perso, GameAction GA) {
            int actionID = -1;
            short CcellID = -1;
            try {
                actionID = Integer.parseInt(GA._args.split(";")[1]);
                CcellID = Short.parseShort(GA._args.split(";")[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (actionID == -1) return;
            if (Constant.isJobAction(actionID)) {
                perso.doJobAction(actionID, _object, GA, this);
                return;
            }
            switch (actionID) {
                case 44://Sauvegarder pos
                    String str = _map + "," + _id;
                    perso.set_savePos(str);
                    perso.save(false);
                    SocketManager.GAME_SEND_Im_PACKET(perso, "06");
                    break;

                case 102://Puiser
                    if (!_object.isInteractive()) return;//Si l'objet est utilisé
                    if (_object.getState() != Constant.IOBJECT_STATE_FULL) return;//Si le puits est vide
                    _object.setState(Constant.IOBJECT_STATE_EMPTYING);
                    _object.setInteractive(false);
                    SocketManager.GAME_SEND_GA_PACKET_TO_MAP(perso.getMap(), "" + GA._id, 501, perso.getGuid() + "", _id + "," + _object.getUseDuration() + "," + _object.getUnknowValue());
                    SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(perso.getMap(), this);
                    break;
                case 114://Utiliser (zaap)
                    String data = _map + "," + _id;
                    perso.openZaapMenu(data);
                    perso.getAccount().getGameThread().removeAction(GA);
                    break;
                case 170: //Livre de métiers
                    String l = "";
                    boolean i = false;
                    for (int b : perso.getMap().get_atelier()) {
                        if (i) l += ";";
                        l += b;
                        i = true;
                    }
                    SocketManager.GAME_SEND_ECK_PACKET(perso, 14, l);
                    perso.set_isTradingWith(14);
                    break;
                case 157: //Zaapis
                    String ZaapiList = "";
                    String[] Zaapis;
                    int count = 0;
                    int price = 20;

                    if (perso.getMap()._subArea.getArea().getID() == 7 && (perso.get_align() == 1 || perso.get_align() == 0 || perso.get_align() == 3))//Ange, Neutre ou Sérianne
                    {
                        Zaapis = Constant.ZAAPI.get(Constant.ALIGNEMENT_BONTARIEN).split(",");
                        if (perso.get_align() == 1) price = 10;
                    } else if (perso.getMap()._subArea.getArea().getID() == 11 && (perso.get_align() == 2 || perso.get_align() == 0 || perso.get_align() == 3))//Démons, Neutre ou Sérianne
                    {
                        Zaapis = Constant.ZAAPI.get(Constant.ALIGNEMENT_BRAKMARIEN).split(",");
                        if (perso.get_align() == 2) price = 10;
                    } else {
                        Zaapis = Constant.ZAAPI.get(Constant.ALIGNEMENT_NEUTRE).split(",");
                    }

                    if (Zaapis.length > 0) {
                        for (String s : Zaapis) {
                            if (count == Zaapis.length)
                                ZaapiList += s + ";" + price;
                            else
                                ZaapiList += s + ";" + price + "|";
                            count++;
                        }
                        perso.SetZaaping(true);
                        SocketManager.GAME_SEND_ZAAPI_PACKET(perso, ZaapiList);
                    }
                    break;
                case 175://Acceder a un enclos
                    if (_object.getState() != Constant.IOBJECT_STATE_EMPTY) ;
                    //SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(perso.get_curCarte(),this);
                    perso.openMountPark();
                    break;
                case 176://Achat enclo
                    MountPark MP = perso.getMap().getMountPark();
                    if (MP.get_owner() == -1)//Public
                    {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "196");
                        return;
                    }
                    if (MP.get_price() == 0)//Non en vente
                    {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "197");
                        return;
                    }
                    if (perso.get_guild() == null)//Pas de guilde
                    {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "1135");
                        return;
                    }
                    if (perso.getGuildMember().getRank() != 1)//Non meneur
                    {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "198");
                        return;
                    }
                    SocketManager.GAME_SEND_R_PACKET(perso, "D" + MP.get_price() + "|" + MP.get_price());
                    break;
                case 177://Vendre enclo
                case 178://Modifier prix de vente
                    MountPark MP1 = perso.getMap().getMountPark();
                    if (MP1.get_owner() == -1) {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "194");
                        return;
                    }
                    if (MP1.get_owner() != perso.getGuid()) {
                        SocketManager.GAME_SEND_Im_PACKET(perso, "195");
                        return;
                    }
                    SocketManager.GAME_SEND_R_PACKET(perso, "D" + MP1.get_price() + "|" + MP1.get_price());
                    break;
                case 81://Vérouiller maison
                    Houses h = Houses.get_house_id_by_coord(perso.getMap().get_id(), CcellID);
                    if (h == null) return;
                    perso.setInHouse(h);
                    h.Lock(perso);
                    break;
                case 84://Rentrer dans une maison
                    Houses h2 = Houses.get_house_id_by_coord(perso.getMap().get_id(), CcellID);
                    if (h2 == null) return;
                    perso.setInHouse(h2);
                    h2.HopIn(perso);
                    break;
                case 97://Acheter maison
                    Houses h3 = Houses.get_house_id_by_coord(perso.getMap().get_id(), CcellID);
                    if (h3 == null) return;
                    perso.setInHouse(h3);
                    h3.BuyIt(perso);
                    break;

                case 104://Ouvrir coffre privé
                    Trunk trunk = Trunk.get_trunk_id_by_coord(perso.getMap().get_id(), CcellID);
                    if (trunk == null) {
                        GameServer.addToLog("Game: INVALID TRUNK ON MAP : " + perso.getMap().get_id() + " CELLID : " + CcellID);
                        return;
                    }
                    perso.setInTrunk(trunk);
                    trunk.HopIn(perso);
                    break;
                case 105://Vérouiller coffre
                    Trunk t = Trunk.get_trunk_id_by_coord(perso.getMap().get_id(), CcellID);
                    if (t == null) {
                        GameServer.addToLog("Game: INVALID TRUNK ON MAP : " + perso.getMap().get_id() + " CELLID : " + CcellID);
                        return;
                    }
                    perso.setInTrunk(t);
                    t.Lock(perso);
                    break;

                case 98://Vendre
                case 108://Modifier prix de vente
                    Houses h4 = Houses.get_house_id_by_coord(perso.getMap().get_id(), CcellID);
                    if (h4 == null) return;
                    perso.setInHouse(h4);
                    h4.SellIt(perso);
                    break;

                case 183: // Statue zone incarnam
                    perso.statue = true;
                    break;

                default:
                    GameServer.addToLog("Case.startAction non definie pour l'actionID = " + actionID);
                    break;
            }
        }

        public void finishAction(Player perso, GameAction GA) {
            int actionID = -1;
            try {
                actionID = Integer.parseInt(GA._args.split(";")[1]);
            } catch (Exception e) {
            }
            if (actionID == -1) return;

            if (Constant.isJobAction(actionID)) {
                perso.finishJobAction(actionID, _object, GA, this);
                return;
            }
            switch (actionID) {
                case 44://Sauvegarder a un zaap
                case 81://Vérouiller maison
                case 84://ouvrir maison
                case 97://Acheter maison.
                case 98://Vendre
                case 104://Ouvrir coffre
                case 105://Code coffre
                case 108://Modifier prix de vente
                case 157://Zaapi
                    break;
                case 102://Puiser
                    _object.setState(Constant.IOBJECT_STATE_EMPTY);
                    _object.setInteractive(false);
                    _object.startTimer();
                    SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(perso.getMap(), this);
                    int qua = Formulas.getRandomValue(1, 10);//On a entre 1 et 10 eaux
                    Item obj = World.getObjTemplate(311).createNewItem(qua, false, -1);
                    if (perso.addObjet(obj, true))
                        World.addObjet(obj, true);
                    SocketManager.GAME_SEND_IQ_PACKET(perso, perso.getGuid(), qua);
                    break;

                case 183:
                    break;

                default:
                    GameServer.addToLog("[FIXME]Case.finishAction non definie pour l'actionID = " + actionID);
                    break;
            }
        }

        public void clearOnCellAction() {
            //_onCellStop.clear();
            _onCellStop = null;
        }

        public void addDroppedItem(Item obj) {
            _droppedItem = obj;
        }

        public void clearDroppedItem() {
            _droppedItem = null;
        }
    }

    public Maps(short _id, String _date, byte _w, byte _h, String _key, String places, String dData, String cellsData, String monsters, String mapPos, byte maxGroup, byte maxSize) {
        this._id = _id;
        this._date = _date;
        this._w = _w;
        this._h = _h;
        this._key = _key;
        this._placesStr = places;
        this._maxGroup = maxGroup;
        this._maxSize = maxSize;
        String[] mapInfos = mapPos.split(",");
        try {
            this._X = Byte.parseByte(mapInfos[0]);
            this._Y = Byte.parseByte(mapInfos[1]);
            int subArea = Integer.parseInt(mapInfos[2]);
            _subArea = World.getSubArea(subArea);
            if (_subArea != null) _subArea.addCarte(this);
        } catch (Exception e) {
            GameServer.addToLog("Erreur de chargement de la map " + _id + ": Le champ MapPos est invalide");
            Reboot.reboot();
        }

        if (!dData.isEmpty()) // @Flow - Editing start here
        {
            this._cases = CryptManager.DecompileMapData(this, dData);
        } else {
            String[] cellsDataArray = cellsData.split("\\|");

            for (String o : cellsDataArray) {

                boolean Walkable = true;
                boolean LineOfSight = true;
                int Number = -1;
                int obj = -1;
                String[] cellInfos = o.split(",");
                try {
                    Walkable = cellInfos[2].equals("1");
                    LineOfSight = cellInfos[1].equals("1");
                    Number = Integer.parseInt(cellInfos[0]);
                    if (!cellInfos[3].trim().equals("")) {
                        obj = Integer.parseInt(cellInfos[3]);
                    }
                } catch (Exception d) {
                }
                ;
                if (Number == -1) continue;

                this._cases.put(Number, new Case(this, Number, Walkable, LineOfSight, obj));
            }
        }
        for (String mob : monsters.split("\\|")) {
            if (mob.equals("")) continue;
            int id = 0;
            int lvl = 0;

            try {
                id = Integer.parseInt(mob.split(",")[0]);
                lvl = Integer.parseInt(mob.split(",")[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            ;
            if (id == 0 || lvl == 0) continue;
            if (World.getMonstre(id) == null) continue;
            if (World.getMonstre(id).getGradeByLevel(lvl) == null) continue;
            _mobPossibles.add(World.getMonstre(id).getGradeByLevel(lvl));
        }
        if (_cases.isEmpty()) return;

        if (Config.CONFIG_USE_MOBS) {
            if (_maxGroup == 0) return;
            spawnGroup(Constant.ALIGNEMENT_NEUTRE, _maxGroup, false, -1);//Spawn des groupes d'alignement neutre
            spawnGroup(Constant.ALIGNEMENT_BONTARIEN, 1, false, -1);//Spawn du groupe de gardes bontarien s'il y a
            spawnGroup(Constant.ALIGNEMENT_BRAKMARIEN, 1, false, -1);//Spawn du groupe de gardes brakmarien s'il y a
        }
    }

    public void setTempsPourPosePercepteur(long temps){
        _tempsPourPosePercepteur = temps;
    }

    public long getTempsPourPosePercepteur() {
        return _tempsPourPosePercepteur;
    }

    public void applyEndFightAction(int type, Player perso) {
        if (_endFightAction.get(type) == null)
            return;
        for (Action A : _endFightAction.get(type)) {
            try {
                A.apply(perso, null, -1, -1);
            } catch (Exception e) {
            }
        }

    }

    public boolean hasEndFightAction(int type) {
        return (_endFightAction.get(type) == null ? false : true);
    }

    public void addEndFightAction(int type, Action A) {
        if (_endFightAction.get(type) == null) _endFightAction.put(type, new ArrayList<Action>());
        //On retire l'action si elle existait déjà
        delEndFightAction(type, A.getID());
        _endFightAction.get(type).add(A);
    }

    public void delEndFightAction(int type, int aType) {
        if (_endFightAction.get(type) == null) return;
        ArrayList<Action> copy = new ArrayList<Action>();
        copy.addAll(_endFightAction.get(type));
        for (Action A : copy) if (A.getID() == aType) _endFightAction.get(type).remove(A);
    }

    public void setMountPark(MountPark mountPark) {
        _mountPark = mountPark;
    }

    public MountPark getMountPark() {
        return _mountPark;
    }

    public Maps(short id, String date, byte w, byte h, String key, String places) {
        _id = id;
        _date = date;
        _w = w;
        _h = h;
        _key = key;
        _placesStr = places;
        _cases = new TreeMap<Integer, Case>();
    }

    public SubArea getSubArea() {
        return _subArea;
    }

    public int getX() {
        return _X;
    }

    public int getY() {
        return _Y;
    }

    public Map<Integer, NPC> get_npcs() {
        return _npcs;
    }

    public NPC addNpc(int npcID, int cellID, int dir, int move, String text) {
        NpcTemplate temp = World.getNPCTemplate(npcID);
        if (temp == null) return null;
        if (getCase(cellID) == null) return null;
        NPC npc = new NPC(temp, nextObjectID, cellID, (byte) dir, move, text);
        _npcs.put(nextObjectID, npc);
        nextObjectID--;
        return npc;
    }

    public void spawnGroup(int align, int nbr, boolean log, int cellID) {
        if (nbr < 1) return;
        if (_mobGroups.size() - _fixMobGroups.size() >= _maxGroup) return;
        for (int a = 1; a <= nbr; a++) {
            MobGroup group = new MobGroup(nextObjectID, align, _mobPossibles, this, cellID, this._maxSize);
            if (group.getMobs().isEmpty()) continue;
            _mobGroups.put(nextObjectID, group);
            if (log) {
                GameServer.addToLog("Groupe de monstres ajoutes sur la map: " + _id + " alignement: " + align + " ID: " + nextObjectID);
                SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
            }
            nextObjectID--;
        }
    }

    public void spawnNewGroup(boolean timer, int cellID, String groupData, String condition) {
        MobGroup group = new MobGroup(nextObjectID, cellID, groupData, 0, 0, 0);
        if (group.getMobs().isEmpty()) return;
        _mobGroups.put(nextObjectID, group);
        group.setCondition(condition);
        group.setIsFix(false);

        if (Config.DEBUG) GameServer.addToLog("Groupe de monstres ajoutes sur la map: " + _id + " ID: " + nextObjectID);

        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
        nextObjectID--;

        if (timer)
            group.startCondTimer();
    }

    public MobGroup spawnGroupOnCommand(int cellID, String groupData) {
        MobGroup group = new MobGroup(nextObjectID, cellID, groupData, 0, 0, 0);
        if (group.getMobs().isEmpty()) return null;
        _mobGroups.put(nextObjectID, group);
        group.setIsFix(false);

        if (Config.DEBUG) GameServer.addToLog("Groupe de monstres ajoutes sur la map: " + _id + " ID: " + nextObjectID);

        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
        nextObjectID--;

        return group;
    }

    public void addStaticGroup(int cellID, String groupData, int minSpawnTime, int maxSpawnTime, int ellap) {
        MobGroup group = new MobGroup(nextObjectID, cellID, groupData, minSpawnTime, maxSpawnTime, ellap);
        if (group.getMobs().isEmpty()) return;
        group.setMap(this);
        if (group.haveSpawnTime()) {
            World.variableMobGroup.add(group);
            return;
        }
        _mobGroups.put(nextObjectID, group);
        nextObjectID--;
        _fixMobGroups.put(-1000 + nextObjectID, group);
        SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, group);
    }

    public void setPlaces(String place) {
        _placesStr = place;
    }

    public void removeFight(int id) {
        try {
            _fights.remove(id);
        } catch (Exception e) {
        }
    }

    public NPC getNPC(int id) {
        return _npcs.get(id);
    }

    public NPC RemoveNPC(int id) {
        return _npcs.remove(id);
    }

    public Case getCase(int id) {
        return _cases.get(id);
    }

    public ArrayList<Player> getPersos() {
        ArrayList<Player> persos = new ArrayList<Player>();
        synchronized (_persos) {
            persos.addAll(_persos.values());
        }
        return persos;
        /*try
		{
		for(Case c : _cases.values())
			for(Personnage entry : c.getPersos().values())
				persos.add(entry);
		}
		catch(Exception e){ e.printStackTrace(); Ancestra.addToDebug("Bug getPeros"); }
		return persos;*/
    }

    public short get_id() {
        return _id;
    }

    public String get_date() {
        return _date;
    }

    public byte get_w() {
        return _w;
    }

    public byte get_h() {
        return _h;
    }

    public String get_key() {
        return _key;
    }

    public String get_placesStr() {
        return _placesStr;
    }

    public void addPlayer(Player perso) {
        SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this, perso);
        perso.get_curCell().addPerso(perso);
    }

    public void sendGMsPackets(Player p_out) {/*
		StringBuilder packet = new StringBuilder();
		ArrayList<Case> cases = new ArrayList<Case>();
		synchronized(_cases){ cases.addAll(_cases.values()); }
		for(Case cell : cases)for(Personnage perso : cell.getPersos().values())packet.append("GM|+").append(perso.parseToGM()).append('\u0000');
		return packet.toString();*/
        StringBuilder packet = new StringBuilder("GM");
        int count = 0;
        //Le but est de faire des packets compact de 20 joueurs max chacun
        ArrayList<Player> persos = new ArrayList<Player>();
        synchronized (_persos) {
            persos.addAll(_persos.values());
        }
        for (Player p : persos) {
            if (p == null || !p.isOnline()) continue;
            if (p.get_size() == 0) continue;;
            packet.append("|+").append(p.parseToGM());
            count++;
            if (count >= 20) {
                SocketManager.GAME_SEND_MAP_GMS_PACKETS(p_out, packet.toString());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                packet = new StringBuilder("GM");
                count = 0;
            }
        }
        if (count > 0) {
            SocketManager.GAME_SEND_MAP_GMS_PACKETS(p_out, packet.toString());
        }
    }

    public String getFightersGMsPackets() {
        StringBuilder packet = new StringBuilder();
        ArrayList<Case> cases = new ArrayList<Case>();
        synchronized (_cases) {
            cases.addAll(_cases.values());
        }
        for (Case cell : cases) {
            for (Entry<Integer, Fighter> f : cell.getFighters().entrySet()) {
                packet.append(f.getValue().getGmPacket('+')).append('\u0000');
            }
        }
        return packet.toString();
    }

    public String getMobGroupGMsPackets(Player player) {
        if (_mobGroups.isEmpty()) return "";

        StringBuilder packet = new StringBuilder();
        packet.append("GM|");
        boolean isFirst = true;
        for (MobGroup entry : _mobGroups.values()) {
            String GM = entry.parseGM(player);
            if (GM.equals("")) continue;

            if (!isFirst)
                packet.append("|");

            packet.append(GM);
            isFirst = false;
        }
        //System.out.println(packet.toString());
        return packet.toString();
    }

    public String getPrismeGMPacket() {
        if (World.AllPrisme() == null) {
            return "";
        }
        String str = "";
        Prism prisme = trouverPrisme();
        if (prisme != null) {
            str = prisme.getGMPrisme();
        }
        return str;
    }

    public Prism trouverPrisme() {
        Prism prisme = null;
        for (Prism Prisme : World.AllPrisme()) {
            if (Prisme.getCarte() == _id) {
                prisme = Prisme;
                break;
            }
        }
        return prisme;
    }

    public String getNpcsGMsPackets() {
        if (_npcs.isEmpty()) return "";

        StringBuilder packet = new StringBuilder();
        packet.append("GM|");
        boolean isFirst = true;
        for (Entry<Integer, NPC> entry : _npcs.entrySet()) {
            String GM = entry.getValue().parseGM();
            if (GM.equals("")) continue;

            if (!isFirst)
                packet.append("|");

            packet.append(GM);
            isFirst = false;
        }
        return packet.toString();
    }

    public String getObjectsGDsPackets() {
        StringBuilder toreturn = new StringBuilder();
        boolean first = true;
        for (Entry<Integer, Case> entry : _cases.entrySet()) {
            if (entry.getValue().getObject() != null) {
                if (!first) toreturn.append((char) 0x00);
                first = false;
                int cellID = entry.getValue().getID();
                InteractiveObject object = entry.getValue().getObject();
                toreturn.append("GDF|").append(cellID).append(";").append(object.getState()).append(";").append((object.isInteractive() ? "1" : "0"));
            }
        }
        return toreturn.toString();
    }

    public int getNbrFight() {
        return _fights.size();
    }

    public Map<Integer, Fight> get_fights() {
        return _fights;
    }

    public synchronized Map<Integer, Fight> get_fights2() {
        Map<Integer, Fight> f = new TreeMap<Integer, Fight>();
        f.putAll(_fights);
        return f;
    }

    public Fight newFight(Player init1, Player init2, int type) {
        while (!init1.estBloqueCombat()) {
            init1.mettreCombatBloque(true);
        }
        while (!init2.estBloqueCombat()) {
            init2.mettreCombatBloque(true);
        }
        int id = 1;
        if (!_fights.isEmpty())
            id = ((Integer) (_fights.keySet().toArray()[_fights.size() - 1])) + 1;

        Fight f = new Fight(type, id, this, init1, init2);
        _fights.put(id, f);
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
        return f;
    }

    public void verifEndedFight() {
        List<Entry<Integer, Fight>> values = new ArrayList<Entry<Integer, Fight>>(_fights.entrySet());
        for (int i = 0; i < values.size(); i++) {
            Fight f = values.get(i).getValue();
            if (!f.isFightStarted() && f.get_state() != Constant.FIGHT_STATE_PLACE){
                _fights.remove(values.get(i).getKey());
            }
        }
    }

    public void clearFights() {
        _fights.clear();
    }

    public Fight newKolizeum(Kolizeum kolizeum, List<Player> list, List<Player> list2) {
        int id = 1;
        if (!_fights.isEmpty())
            id = ((Integer) (_fights.keySet().toArray()[_fights.size() - 1])) + 1;
        ArrayList<Player> team1 = new ArrayList<Player>();
        team1.addAll(list);
        ArrayList<Player> team2 = new ArrayList<Player>();
        team2.addAll(list2);
        Fight f = new Fight(0, this, team1, team2, kolizeum);
        _fights.put(id, f);
        return f;
    }

    public Fight newKoli(ArrayList<Player> team1, ArrayList<Player> team2) {
        int id = 1;
        if (!_fights.isEmpty())
            id = ((Integer) (_fights.keySet().toArray()[_fights.size() - 1])) + 1;
        Fight f = new Fight(0, this, team1, team2, null);
        _fights.put(id, f);
        return f;
    }

    public int getRandomFreeCellID() {
        ArrayList<Integer> freecell = new ArrayList<Integer>();
        for (Entry<Integer, Case> entry : _cases.entrySet()) {
            //Si la case n'est pas marchable
            if (!entry.getValue().isWalkable(true)) continue;
            //Si la case est prise par un groupe de monstre
            boolean ok = true;
            for (Entry<Integer, MobGroup> mgEntry : _mobGroups.entrySet()) {
                if (mgEntry.getValue().getCellID() == entry.getValue().getID())
                    ok = false;
            }
            if (!ok) continue;
            //Si la case est prise par un npc
            ok = true;
            for (Entry<Integer, NPC> npcEntry : _npcs.entrySet()) {
                if (npcEntry.getValue().get_cellID() == entry.getValue().getID())
                    ok = false;
            }
            if (!ok) continue;
            //Si la case est prise par un joueur
            if (!entry.getValue().getPersos().isEmpty()) continue;
            //Sinon
            freecell.add(entry.getValue().getID());
        }
        if (freecell.isEmpty()) {
            GameServer.addToLog("Aucune cellulle libre n'a ete trouve sur la map " + _id + " : groupe non spawn");
            return -1;
        }
        int rand = Formulas.getRandomValue(0, freecell.size() - 1);
        return freecell.get(rand);
		/*
		int max =  _cases.size()-_w;
		int rand = 0;
		int lim = 0;
		boolean isOccuped;
		
		do
		{
			isOccuped = false;
			rand = Formulas.getRandomValue(_w,max);
			if(lim >50)
				return 0;
			for(Entry<Integer,MobGroup> group : _mobGroups.entrySet())
			{
				if (group.getValue().getCellID() != 0)
				{
					if(group.getValue().getCellID() == _cases.get(_cases.keySet().toArray()[rand]).getID())
						isOccuped = true;
				}
			}
			for(Entry<Integer,NPC> npc : _npcs.entrySet())
			{
				if(npc.getValue().get_cellID() == _cases.get(_cases.keySet().toArray()[rand]).getID())
					isOccuped = true;
			}
			
			if (_cases.get(_cases.keySet().toArray()[rand]).isWalkable() && !isOccuped)
			{
				return _cases.get(_cases.keySet().toArray()[rand]).getID();
			}
			
			lim++;
		}while(!_cases.get(_cases.keySet().toArray()[rand]).isWalkable() && !isOccuped);
		
		return 0;
		//*/
    }

    public void refreshSpawns() {
        for (int id : _mobGroups.keySet()) {
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this, id);
        }
        _mobGroups.clear();
        _mobGroups.putAll(_fixMobGroups);
        for (MobGroup mg : _fixMobGroups.values()) SocketManager.GAME_SEND_MAP_MOBS_GM_PACKET(this, mg);
        if (!this.getPersos().isEmpty())
            for (Player p : this.getPersos()) {
                if (p != null && p.getAccount() != null && p.getAccount().getGameThread() != null)
                    SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS(p.getAccount().getGameThread().getOut(), this, p);
            }
        spawnGroup(Constant.ALIGNEMENT_NEUTRE, _maxGroup, true, -1);//Spawn des groupes d'alignement neutre
        spawnGroup(Constant.ALIGNEMENT_BONTARIEN, 1, true, -1);//Spawn du groupe de gardes bontarien s'il y a
        spawnGroup(Constant.ALIGNEMENT_BRAKMARIEN, 1, true, -1);//Spawn du groupe de gardes brakmarien s'il y a
    }

    public void onPlayerArriveOnCell(Player perso, int caseID) {
        if (_cases.get(caseID) == null) return;
        Case caseCible = _cases.get(caseID);
        synchronized (caseCible) {
            Item obj = caseCible._droppedItem;
            if (obj != null) {
                synchronized (obj) {
                    if (perso.addObjet(obj, true))
                        World.addObjet(obj, true);
                    SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this, '-', caseID, 0, 0);
                    SocketManager.GAME_SEND_Ow_PACKET(perso);
                    caseCible.clearDroppedItem();
                }
            }
            caseCible.applyOnCellStopActions(perso);
        }
        if (perso.getHasEndFight()) {
            perso.setHasEndFight(false);
            //return; @Flow - Is it needed, well I don't think so ^^ ? bug du double clique
        }
        if (_placesStr.equalsIgnoreCase("|")) return;
        //Si le joueur a changer de map ou ne peut etre aggro
        if (perso.getMap().get_id() != _id || !perso.canAggro()) return;
        for (MobGroup group : _mobGroups.values()) {
            if (Pathfinding.getDistanceBetween(this, caseID, group.getCellID()) <= group.getAggroDistance())//S'il y aggro
            {
                if ((group.getAlignement() == -1 || ((perso.get_align() == 1 || perso.get_align() == 2) && (perso.get_align() != group.getAlignement()))) && ConditionParser.validConditions(perso, group.getCondition())) {
                    if (Constant.COMBAT_BLOQUE) {
                        //perso.sendText("Les combats sont bloqués par les administrateurs, un redémarrage du serveur va avoir lieu une fois les combats terminés");
                        SocketManager.GAME_SEND_POPUP(perso, "Les combats sont bloqués par les administrateurs, un redémarrage du serveur va avoir lieu une fois les combats terminés !");
                        return;
                    } else if (perso.estBloqueCombat()) {
                        perso.sendText("Temporisation en cours... Veuillez patienter...");
                        return;
                    }
                    GameServer.addToLog(perso.getName() + " lance un combat contre le groupe " + group.getID() + " sur la map " + _id);
                    if ((perso._Follows != null && !perso._Follows.playerWhoFollowMe.contains(perso)) || perso._Follows == null) {
                        startFigthVersusMonstres(perso, group);
                    }
                    Fight f = perso.getFight();
                    if (f != null) {
                        for (Player followMe : perso.playerWhoFollowMe) {
                            if (followMe.getMap().get_id() == f.get_map().get_id()) {
                                if (f.joinFight(followMe, perso.getGuid())) {
                                    perso.playerWhoFightWithMe.add(followMe);
                                }
                            }
                        }
                    }
                    return;
                }
            }
        }
    }

    public void startFigthVersusMonstres(Player perso, MobGroup group) //Vérifier à chaque combat l'ID et voir si c'est le même. on verra donc si ya colision
    {
        while (!perso.estBloqueCombat()) {
            perso.mettreCombatBloque(true);
        }
        int id = 1;
        if (!_fights.isEmpty())
            id = ((Integer) (_fights.keySet().toArray()[_fights.size() - 1])) + 1;
        if (!group.isFix()) _mobGroups.remove(group.getID());
        else SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(this);
        _fights.put(id, new Fight(id, this, perso, group));
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
    }

    public void startFigthVersusPercepteur(Player perso, Collector perco) {
        while (!perso.estBloqueCombat()) {
            perso.mettreCombatBloque(true);
        }
        int id = 1;
        if (!_fights.isEmpty())
            id = ((Integer) (_fights.keySet().toArray()[_fights.size() - 1])) + 1;

        _fights.put(id, new Fight(id, this, perso, perco));
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
    }

    public void initFightVSPrisme(Player perso, Prism Prisme) {
        int id = 1;
        while (!perso.estBloqueCombat()) {
            perso.mettreCombatBloque(true);
        }
        if (!_fights.isEmpty())
            id = ((Integer) (_fights.keySet().toArray()[_fights.size() - 1])) + 1;

        _fights.put(id, new Fight(id, this, perso, Prisme));
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);

    }

    public Maps getMapCopy() {
        Map<Integer, Case> cases = new TreeMap<Integer, Case>();

        Maps map = new Maps(_id, _date, _w, _h, _key, _placesStr);

        for (Entry<Integer, Case> entry : _cases.entrySet())
            cases.put(entry.getKey(),
                    new Case(
                            map,
                            entry.getValue().getID(),
                            entry.getValue().isWalkable(false),
                            entry.getValue().isLoS(),
                            (entry.getValue().getObject() == null ? -1 : entry.getValue().getObject().getID())
                    )
            );
        map.setCases(cases);
        return map;
    }

    private void setCases(Map<Integer, Case> cases) {
        _cases = cases;
    }

    public InteractiveObject getMountParkDoor() {
        for (Case c : _cases.values()) {
            if (c.getObject() == null) continue;
            //Si enclose
            if (c.getObject().getID() == 6763
                    || c.getObject().getID() == 6766
                    || c.getObject().getID() == 6767
                    || c.getObject().getID() == 6772)
                return c.getObject();

        }
        return null;
    }

    public Map<Integer, MobGroup> getMobGroups() {
        return _mobGroups;
    }

    public void removeNpcOrMobGroup(int id) {
        _npcs.remove(id);
        _mobGroups.remove(id);
    }

    public int getMaxGroupNumb() {
        return _maxGroup;
    }

    public void setMaxGroup(byte id) {
        _maxGroup = id;
    }

    public Fight getFight(int id) {
        return _fights.get(id);
    }

    public void sendFloorItems(Player perso) {
        for (Case c : _cases.values()) {
            if (c.getDroppedItem() != null)
                SocketManager.GAME_SEND_GDO_PACKET(perso, '+', c.getID(), c.getDroppedItem().getTemplate(false).getID(), 0);
        }
    }

    public Map<Integer, Case> GetCases() {
        return _cases;
    }

    public int getStoreCount() {
        return (World.getSeller(get_id()) == null ? 0 : World.getSeller(get_id()).size());
    }

    public boolean hasatelierfor(int id) {
        for (int a : atelier) {
            if (a == id) return true;
        }
        return false;
    }

    public void add_atelier(ArrayList<Integer> job) {
        for (int a : job) {
            if (a == -1) continue;
            if (!atelier.contains(a)) atelier.add(a);
        }
    }

    public ArrayList<Integer> get_atelier() {
        return atelier;
    }

    public boolean isMuted() {
        if (_muteTime == -1) return false;
        if (_muteTime < (long) System.currentTimeMillis() / 1000) {
            _muteTime = -1;
            return false;
        }
        return true;
    }

    public void muteMap(long time) {
        if (time < 0) return;
        _muteTime = ((long) System.currentTimeMillis() / 1000) + time;
    }

    public void unMuteMap() {
        _muteTime = -1;
    }

    public synchronized void addPerso(Player P) {
        _persos.put(P.getGuid(), P);
    }

    public synchronized void removePerso(int P) {
        if (_persos.containsKey(P)) _persos.remove(P);
    }

    public void startEventFight(MobGroup group, Event event) { //Vérifier à chaque combat l'ID et voir si c'est le même. on verra donc si ya colision
        int id = 0;
        if (!_fights.isEmpty())
            id = 0;//((Integer)(_fights.keySet().toArray()[_fights.size()-1]))+1;

        if (!group.isFix()) _mobGroups.remove(group.getID());
        else SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(this);
        _fights.put(id, new Fight(id, this, group, event));
        SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
    }

    public int get_nextObjectID() {
        return nextObjectID;
    }

    public void PnjParle() {
        int nbr_parole = 0;
        for (Entry<Integer, NPC> pnj : _npcs.entrySet()) {
            if (nbr_parole == 3) return;
            if (pnj.getValue() == null) continue;
            NPC infos = pnj.getValue();
            int i = Formulas.getRandomValue(1, 3);
            if (i != 1) {
                continue;
            }
            String[] list = infos.get_text();
            int num = Formulas.getRandomValue(0, list.length - 1);
            if (list.length > 0 && infos.get_text()[num].length() > 0) {
                if (list.length > 1) {
                    boolean ok = false;
                    try {
                        if (_parleTime != null && _parleTime.entrySet() != null) {
                            for (Entry<Integer, Integer> time : _parleTime.entrySet()) {
                                if (time.getKey() == infos.get_guid()) {
                                    ok = true;
                                    while (num == time.getValue()) {
                                        num = Formulas.getRandomValue(0, list.length - 1);
                                    }
                                    if (num != time.getValue()) {
                                        _parleTime.remove(time.getKey());
                                        _parleTime.put(time.getKey(), num);
                                    }
                                }
                            }
                        }
                    } catch (ConcurrentModificationException e) {
                    }
                    ;
                    if (!ok) {
                        _parleTime.put(infos.get_guid(), num);
                    }
                }
                String msg = list[num];
                if (msg.contains("/think ")) {
                    msg = "!THINK!" + msg.substring(7);
                }
                String name = "PNJ";
                if (name.length() < 1) {
                    name = "PNJ";
                }
                try {
                    Thread.sleep((Formulas.getRandomValue(5, 10) * 1000));
                } catch (Exception e) {
                }
                SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this, "", infos.get_guid(), name, msg);
                nbr_parole++;
            }
        }
    }

    public void AllDeplacement() {
        int nbr_deplacement = 0;
        //Groupe de monster
        if (getMobGroups() != null && getMobGroups().size() > 0) {
            for (Entry<Integer, MobGroup> entry : getMobGroups().entrySet()) {
                try {
                    if (_persos.size() > 0) {
                        if (entry.getValue() == null) continue;
                        if (nbr_deplacement == 3) return;
                        int i = Formulas.getRandomValue(0, 1);
                        if (i != 1) continue;
                        nbr_deplacement++;
                        int cell = getRandomCellid(entry.getValue().getCellID(), false);
                        String pathstr;
                        try {
                            pathstr = Pathfinding.getShortestStringPathBetween(this, entry.getValue().getCellID(), cell, 0);
                        } catch (Exception e) {
                            continue;
                        }
                        if (pathstr == null) continue;
                        entry.getValue().setCellID(cell);
                        try {
                            Thread.sleep((Formulas.getRandomValue(10, 50) * 100));
                        } catch (Exception e) {
                            continue;
                        }
                        for (Entry<Integer, Player> z : _persos.entrySet()) {
                            if (z.getValue() == null || z.getValue().getAccount() == null || z.getValue().getAccount().getGameThread() == null || z.getValue().getAccount().getGameThread().getOut() == null)
                                continue;
                            SocketManager.GAME_SEND_GA_PACKET(z.getValue().getAccount().getGameThread().getOut(), "0", "1", entry.getValue().getID() + "", pathstr);
                        }
                    } else if (_persos.size() == 0) {
                        int i = Formulas.getRandomValue(0, 1);
                        if (i != 1) continue;
                        int cell = getRandomCellid(entry.getValue().getCellID(), false);
                        entry.getValue().setCellID(cell);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        //Percepteur
        int MoveOrNot = Formulas.getRandomValue(1, 2);
        if (MoveOrNot == 1) {
            if (_persos.size() > 0) {
                Collector perco = Collector.GetPercoByMapID(get_id());
                if (perco != null && perco.get_inFight() <= 0) {
                    int cell = getRandomCellid(perco.get_cellID(), false);
                    String pathstr;
                    try {
                        pathstr = Pathfinding.getShortestStringPathBetween(this, perco.get_cellID(), cell, 0);
                    } catch (Exception e) {
                        return;
                    }
                    if (pathstr == null) return;

                    perco.set_cellID(cell);
                    try {
                        Thread.sleep((Formulas.getRandomValue(10, 50) * 100));
                    } catch (Exception e) {
                    }
                    for (Entry<Integer, Player> z : _persos.entrySet()) {
                        if (z.getValue() == null || z.getValue().getAccount() == null || z.getValue().getAccount().getGameThread() == null || z.getValue().getAccount().getGameThread().getOut() == null)
                            continue;
                        SocketManager.GAME_SEND_GA_PACKET(z.getValue().getAccount().getGameThread().getOut(), "0", "1", perco.getGuid() + "", pathstr);
                    }
                }
            } else if (_persos.size() == 0) {
                Collector perco = Collector.GetPercoByMapID(get_id());
                if (perco != null && perco.get_inFight() <= 0) {
                    int cell = getRandomCellid(perco.get_cellID(), false);
                    perco.set_cellID(cell);
                }
            }
        }
        nbr_deplacement = 0;
        //Npc
        for (Entry<Integer, NPC> pnj : _npcs.entrySet()) {
            if (_persos.size() > 0) {
                if (pnj.getValue() == null) continue;
                if (nbr_deplacement == 3) return;
                NPC infos = pnj.getValue();
                if (infos.getMove() != 1) continue;
                int i = Formulas.getRandomValue(0, 1);
                if (i != 1) continue;
                nbr_deplacement++;
                int cell = getRandomCellid(infos.getCellidBase(), true);
                String pathstr;
                try {
                    pathstr = Pathfinding.getShortestStringPathBetween(this, infos.get_cellID(), cell, 0);
                } catch (Exception e) {
                    continue;
                }
                if (pathstr == null) continue;
                infos.setCellID(cell);
                try {
                    Thread.sleep((Formulas.getRandomValue(10, 50) * 100));
                } catch (Exception e) {
                    continue;
                }
                for (Entry<Integer, Player> z : _persos.entrySet()) {
                    if (z.getValue() == null || z.getValue().getAccount() == null || z.getValue().getAccount().getGameThread() == null || z.getValue().getAccount().getGameThread().getOut() == null)
                        continue;
                    SocketManager.GAME_SEND_GA_PACKET(z.getValue().getAccount().getGameThread().getOut(), "", "1", infos.get_guid() + "", pathstr);
                }
            } else if (_persos.size() == 0) {
                if (pnj == null) continue;
                NPC infos = pnj.getValue();
                if (infos.getMove() != 1) continue;
                int i = Formulas.getRandomValue(0, 1);
                if (i != 1) continue;
                int cell = getRandomCellid(infos.getCellidBase(), true);
                infos.setCellID(cell);
            }
        }
    }

    public int getRandomCellid(int cellid, boolean pnj)//Amélioré par Clemon
    {
        ArrayList<Integer> freecell = new ArrayList<Integer>();
        ArrayList<Integer> cases = new ArrayList<Integer>();
        cases.add((cellid + 14));
        cases.add((cellid - 14));
        cases.add((cellid + 15));
        cases.add((cellid - 15));
        cases.add((cellid + 28));
        cases.add((cellid - 28));
        cases.add((cellid + 29));
        cases.add((cellid - 29));
        cases.add((cellid + 30));
        cases.add((cellid - 30));
        if (pnj) {
            cases.add(cellid);
        }
        if (!pnj) {
            cases.add((cellid + 1));
            cases.add((cellid - 1));
            cases.add((cellid + 2));
            cases.add((cellid - 2));
            cases.add((cellid + 16));
            cases.add((cellid - 16));
            cases.add((cellid + 27));
            cases.add((cellid - 27));
            cases.add((cellid + 31));
            cases.add((cellid - 31));
            cases.add((cellid + 42));
            cases.add((cellid - 42));
            cases.add((cellid + 43));
            cases.add((cellid - 43));
            cases.add((cellid + 44));
            cases.add((cellid - 44));
            cases.add((cellid + 45));
            cases.add((cellid - 45));
            cases.add((cellid + 57));
            cases.add((cellid - 57));
            cases.add((cellid + 58));
            cases.add((cellid - 58));
            cases.add((cellid + 59));
            cases.add((cellid - 59));
        }
        ArrayList<Integer> finish = new ArrayList<Integer>();
        for (int a = 0; a < cases.size(); a++) {
            int chiffre = 0;
            do {
                chiffre = cases.get(Formulas.getRandomValue(0, cases.size() - 1));
            } while (finish.contains(chiffre));
            if (_cases.get(chiffre) == null || !_cases.get(chiffre).isWalkable(true)) continue;
            finish.add(chiffre);
            //Si la case est prise par un groupe de monstre
            boolean ok = true;
            for (Entry<Integer, MobGroup> mgEntry : _mobGroups.entrySet()) {
                if (mgEntry.getValue().getCellID() == _cases.get(chiffre).getID())
                    ok = false;
            }
            if (!ok) continue;
            //Si la case est prise par un npc
            ok = true;
            for (Entry<Integer, NPC> npcEntry : _npcs.entrySet()) {
                if (npcEntry.getValue().get_cellID() == _cases.get(chiffre).getID())
                    ok = false;
            }
            if (!ok) continue;
            //Si la case est prise par un joueur
            if (!_cases.get(chiffre).getPersos().isEmpty()) continue;
            //Sinon
            freecell.add(_cases.get(chiffre).getID());
        }
        if (freecell.isEmpty()) {
            GameServer.addToLog("Aucune cellulle libre n'a ete trouve sur la map " + _id + " groupe non déplacé");
            return -1;
        }
        int rand = Formulas.getRandomValue(0, freecell.size() - 1);
        return freecell.get(rand);
    }

}
