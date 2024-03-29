package org.area.fight.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.area.client.Player;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.Drop;
import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.kernel.Config;
import org.area.object.Guild;
import org.area.object.Maps;
import org.area.object.Item;


public class Collector {
    private int _guid;
    private short _MapID;
    private int _cellID;
    private byte _orientation;
    private int _GuildID = 0;
    private short _N1 = 0;
    private short _N2 = 0;
    private int _exchangeWith = -1;
    private byte _inFight = 0;
    private int _inFightID = -1;
    private Map<Integer, Item> _objets = new TreeMap<Integer, Item>();
    private long _kamas = 0;
    private long _xp = 0;
    private boolean _inExchange = false;
    //Timer
    private long _timeTurn = 45000;
    //Les logs
    private Map<Integer, Item> _LogObjets = new TreeMap<Integer, Item>();
    private long _LogXP = 0;

    // Défenseurs
    private List<Player> defenseurs = new ArrayList<Player>();
    private boolean defenseursCanTP = false;
    private Fight fight = null;

    // Drops pp
    public int nbDePierresDrop = 0;
    public int getNbDePierresDropTotal = 0;

    public Collector(int guid, short map, int cellID, byte orientation, int GuildID,
                     short N1, short N2, String items, long kamas, long xp) {
        _guid = guid;
        _MapID = map;
        _cellID = cellID;
        _orientation = orientation;
        _GuildID = GuildID;
        _N1 = N1;
        _N2 = N2;
        //Mise en place de son inventaire
        for (String item : items.split("\\|")) {
            if (item.equals("")) continue;
            String[] infos = item.split(":");
            int id = Integer.parseInt(infos[0]);
            Item obj = World.getObjet(id);
            if (obj == null) continue;
            _objets.put(obj.getGuid(), obj);
        }
        _xp = xp;
        _kamas = kamas;
    }

    public ArrayList<Drop> getDrops() {
        ArrayList<Drop> toReturn = new ArrayList<World.Drop>();
        for (Item obj : _objets.values()) {
            toReturn.add(new Drop(obj.getTemplate(false).getID(), 0, 100, obj.getQuantity()));
        }
        return toReturn;
    }

    public int getExchangeWith() {
        return _exchangeWith;
    }

    public void setExchangeWith(int id) {
        _exchangeWith = id;
    }

    public long getKamas() {
        return _kamas;
    }

    public void setKamas(long kamas) {
        this._kamas = kamas;
    }

    public long getXp() {
        return _xp;
    }

    public void setXp(long xp) {
        this._xp = xp;
    }

    public boolean defenseursCanTP() {
        return defenseursCanTP;
    }

    public Map<Integer, Item> getObjets() {
        return _objets;
    }

    public void removeObjet(int guid) {
        _objets.remove(guid);
    }

    public boolean HaveObjet(int guid) {
        if (_objets.get(guid) != null) {
            return true;
        } else {
            return false;
        }
    }

    public void removeTurnTimer(long time) {
        _timeTurn -= time;
    }

    public void setTurnTime(long time) {
        _timeTurn = time;
    }

    public long get_turnTimer() {
        return _timeTurn;
    }

    public static String parseGM(Maps map) {
        StringBuilder sock = new StringBuilder();
        sock.append("GM|");
        boolean isFirst = true;
        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue()._inFight > 0) continue;//On affiche pas le perco si il est en combat
            if (perco.getValue()._MapID == map.get_id()) {
                if (!isFirst) sock.append("|");
                sock.append("+");
                sock.append(perco.getValue()._cellID).append(";");
                sock.append(perco.getValue()._orientation).append(";");
                sock.append("0").append(";");
                sock.append(perco.getValue()._guid).append(";");
                sock.append(perco.getValue()._N1).append(",").append(perco.getValue()._N2).append(";");
                sock.append("-6").append(";");
                sock.append("6000^100;");
                Guild G = World.getGuild(perco.getValue()._GuildID);
                sock.append(G.get_lvl()).append(";");
                sock.append(G.get_name()).append(";" + G.get_emblem());
                isFirst = false;
            } else {
                continue;
            }
        }
        return sock.toString();
    }

    public int get_guildID() {
        return _GuildID;
    }

    public void DelPerco(int percoGuid) {
        Collector perco = World.getPerco(percoGuid);
        //On supprime les objets non ramasser/drop
        for (Item obj : _objets.values()) {
            World.removeItem(obj.getGuid());
        }
        // On ajoute une restriction de pose sur la carte
        Maps cartePerco = World.getCarte(perco.get_mapID());
        cartePerco.setTempsPourPosePercepteur(System.currentTimeMillis() + 3600000);
        World.getPercos().remove(percoGuid);
    }

    public void set_fight(Fight f) {
        fight = f;
    }

    public Fight get_fight() {
        return fight;
    }

    public int get_inFight() {
        return _inFight;
    }

    public void set_inFight(final byte fight) {
        _inFight = fight;
        if (fight == 1) {
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            defenseursCanTP = true;
                            for (Player player : defenseurs) {
                                rejoindreCombat(player);
                            }
                        }
                    },
                    15000 //15s
            );
        } else {
            if (fight == 0) {
                for (Player perso : defenseurs) {
                    perso.endfigh = true;
                    SocketManager.GAME_SEND_gITP_PACKET(perso, parseRemoveDefenseurToGuild(perso));
                    if (perso.getFight() == get_fight()) { // Toujours dans le combat du perco
                        perso.teleport(perso.getLastMapID(), perso.getLastCellID());
                        perso.set_fight(null);
                    }
                    perso.percoDefendre = null;
                }
                defenseurs.clear();
            }
            defenseursCanTP = false;
        }
    }

    public void rejoindreCombat(Player player) {
        if (player.getFight() == null && !player.is_away()) {
            player.setLastMapInfo(player.getCurCarte().get_id(), player.getCurCell().getID());
            Fight combat = get_fight();
            if (combat != null) {
                SocketManager.GAME_SEND_gITP_PACKET(player, parseRemoveDefenseurToGuild(player)); // Éviter la popup "vous quitter la défense.."
                player.percoDefendre = this;
                if (!player.playerWhoFollowMe.isEmpty()) {
                    SocketManager.GAME_SEND_MESSAGE(player, "Vos personnages suiveurs ne vous suivront plus puisque que vous avez rejoins une défense percepteur", "009900");
                    player.playerWhoFollowMe.clear();
                }
                try {
                    Thread.sleep(100); // Pour être sûr que le client a traité le packet gITP
                } catch (Exception e) {
                }
                if (player.getMap().get_id() != get_mapID()) {
                    player.teleport(get_mapID(), get_cellID());
                } else {
                    combat.joinPercepteurFight(player, player.getGuid(), getGuid(), this);
                }
            }
        } else {
            removeDefenseur(player);
        }
    }

    public int getGuid() {
        return _guid;
    }

    public int get_cellID() {
        return _cellID;
    }

    public void set_inFightID(int ID) {
        _inFightID = ID;
    }

    public int get_inFightID() {
        return _inFightID;
    }

    public short get_mapID() {
        return _MapID;
    }

    public int get_N1() {
        return _N1;
    }

    public int get_N2() {
        return _N2;
    }

    public static String parsetoGuild(int GuildID) {
        StringBuilder packet = new StringBuilder();
        boolean isFirst = true;
        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_guildID() == GuildID) {
                Maps map = World.getCarte((short) perco.getValue().get_mapID());
                if (isFirst)
                    packet.append("+");
                if (!isFirst) packet.append("|");
                packet.append(perco.getValue().getGuid()).append(";").append(perco.getValue().get_N1()).append(",").append(perco.getValue().get_N2()).append(";");

                packet.append(Integer.toString(map.get_id(), 36)).append(",").append(map.getX()).append(",").append(map.getY()).append(";");
                packet.append(perco.getValue().get_inFight()).append(";");
                if (perco.getValue().get_inFight() == 1) {
                    if (map.getFight(perco.getValue().get_inFightID()) == null) {
                        packet.append(60000).append(";");//TimerActuel
                    } else {
                        packet.append(perco.getValue().get_fight().getRemaimingTime()).append(";");//TimerActuel
                    }
                    packet.append(60000).append(";");//TimerInit
                    packet.append(String.valueOf(perco.getValue().get_fight().nombreDePlace(1)-1)); // -1 aux nombres de places fit que le perco en utilise une
                    packet.append(";");
                    packet.append("?,?,");//?
                } else {
                    packet.append("0;");
                    packet.append(60000).append(";");
                    packet.append("7;");
                    packet.append("?,?,");
                }
                packet.append("1,2,3,4,5");

                //	?,?,callername,startdate(Base 10),lastHarvesterName,lastHarvestDate(Base 10),nextHarvestDate(Base 10)
                isFirst = false;
            } else {
                continue;
            }
        }
        if (packet.length() == 0) packet = new StringBuilder("null");
        return packet.toString();
    }

    public static int GetPercoGuildID(int _id) {

        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_mapID() == _id) {
                return perco.getValue().get_guildID();
            }
        }
        return 0;
    }

    public int GetPercoGuildID() {

        return get_guildID();
    }

    public static Collector GetPercoByMapID(short _id) {

        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_mapID() == _id) {
                return World.getPercos().get(perco.getValue().getGuid());
            }
        }
        return null;
    }

    public static int CountPercoGuild(int GuildID) {
        int i = 0;
        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_guildID() == GuildID) {
                i++;
            }
        }
        return i;
    }

    public static void parseAttaque(Player perso, int guildID) {
        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_inFight() > 0 && perco.getValue().get_guildID() == guildID) {
                SocketManager.GAME_SEND_gITp_PACKET(perso, parseAttaqueToGuild(perco.getValue().getGuid(), perco.getValue().get_mapID(), perco.getValue().get_inFightID()));
            }
        }
    }

    public static void parseDefense(Player perso, int guildID) {
        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_inFight() > 0 && perco.getValue().get_guildID() == guildID) {
                SocketManager.GAME_SEND_gITP_PACKET(perso, perco.getValue().parseDefenseToGuild());
            }
        }
    }

    public static String parseAttaqueToGuild(int guid, short mapid, int fightid) {
        StringBuilder str = new StringBuilder();
        str.append("+").append(guid);

        for (Entry<Integer, Fight> F : World.getCarte(mapid).get_fights().entrySet()) {
            //Je boucle les combats de la map bien qu'inutile :/
            //Mais cela évite le bug F.getValue().getFighters(1) == null
            if (F.getValue().get_id() == fightid) {
                for (Fighter f : F.getValue().getFighters(1))//Attaquants
                {
                    if (f.getPersonnage() == null) continue;
                    str.append("|");
                    str.append(Integer.toString(f.getPersonnage().getGuid(), 36)).append(";");
                    str.append(f.getPersonnage().getName()).append(";");
                    str.append(f.getPersonnage().getLevel()).append(";");
                    str.append("0;");
                }
            }
        }
        return str.toString();
    }

	/*public static String parseDefenseToGuild(int guid, short mapid, int fightid)
    {
		StringBuilder str = new StringBuilder();
		str.append("+").append(guid);
			
		for(Entry<Integer, Fight> F : World.getCarte(mapid).get_fights().entrySet())
		{
			//Je boucle les combats de la map bien qu'inutile :/
			//Mais cela évite le bug F.getValue().getFighters(2) == null
				if(F.getValue().get_id() == fightid)
				{
					for(Fighter f : F.getValue().getFighters(2))//Defenseurs
					{
						if(f.getPersonnage() == null) continue;//On sort le percepteur
						str.append("|");
						str.append(Integer.toString(f.getPersonnage().getGuid(), 36)).append(";");
						str.append(f.getPersonnage().getName()).append(";");
						str.append(f.getPersonnage().get_gfxID()).append(";");
						str.append(f.getPersonnage().getLevel()).append(";");
						str.append(Integer.toString(f.getPersonnage().get_color1(), 36)).append(";");
						str.append(Integer.toString(f.getPersonnage().get_color2(), 36)).append(";");
						str.append(Integer.toString(f.getPersonnage().get_color3(), 36)).append(";");
						str.append("0;");
					}
				}
		}
		return str.toString();
	}*/

    public String parseDefenseToGuild() {
        StringBuilder str = new StringBuilder();
        str.append("+").append(getGuid());

        for (Player player : defenseurs) {
            if (player != null) {
                str.append("|");
                str.append(Integer.toString(player.getGuid(), 36)).append(";");
                str.append(player.getName()).append(";");
                str.append(player.get_gfxID()).append(";");
                str.append(player.getLevel()).append(";");
                str.append(Integer.toString(player.get_color1(), 36)).append(";");
                str.append(Integer.toString(player.get_color2(), 36)).append(";");
                str.append(Integer.toString(player.get_color3(), 36)).append(";");
            }
        }

        return str.toString();
    }

    public String getItemPercepteurList() {
        StringBuilder items = new StringBuilder();
        if (!_objets.isEmpty()) {
            for (Item obj : _objets.values()) {
                items.append("O").append(obj.parseItem()).append(";");
            }
        }
        if (_kamas != 0) items.append("G").append(_kamas);
        return items.toString();
    }

    public String parseItemPercepteur() {
        String items = "";
        for (Item obj : _objets.values()) {
            items += obj.getGuid() + "|";
        }
        return items;
    }


    public void removeFromPercepteur(Player P, int guid, int qua) {
        Item PercoObj = World.getObjet(guid);
        Item PersoObj = P.getSimilarItem(PercoObj);

        int newQua = PercoObj.getQuantity() - qua;

        if (PersoObj == null)//Si le joueur n'avait aucun item similaire
        {
            //S'il ne reste rien
            if (newQua <= 0) {
                //On retire l'item
                removeObjet(guid);
                //On l'ajoute au joueur
                P.addObjet(PercoObj);

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(P, PercoObj);
                String str = "O-" + guid;
                SocketManager.GAME_SEND_EsK_PACKET(P, str);

            } else //S'il reste des objets
            {
                //On crée une copy de l'item
                PersoObj = Item.getCloneObjet(PercoObj, qua);
                //On l'ajoute au monde
                World.addObjet(PersoObj, true);
                //On retire X objet
                PercoObj.setQuantity(newQua);
                //On l'ajoute au joueur
                P.addObjet(PersoObj);

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(P, PersoObj);
                String str = "O+" + PercoObj.getGuid() + "|" + PercoObj.getQuantity() + "|" + PercoObj.getTemplate(false).getID() + "|" + PercoObj.parseStatsString();
                SocketManager.GAME_SEND_EsK_PACKET(P, str);

            }
        } else {
            //S'il ne reste rien
            if (newQua <= 0) {
                //On retire l'item
                this.removeObjet(guid);
                World.removeItem(PercoObj.getGuid());
                //On Modifie la quantité de l'item du sac du joueur
                PersoObj.setQuantity(PersoObj.getQuantity() + PercoObj.getQuantity());

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj);
                String str = "O-" + guid;
                SocketManager.GAME_SEND_EsK_PACKET(P, str);

            } else//S'il reste des objets
            {
                //On retire X objet
                PercoObj.setQuantity(newQua);
                //On ajoute X objets
                PersoObj.setQuantity(PersoObj.getQuantity() + qua);

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj);
                String str = "O+" + PercoObj.getGuid() + "|" + PercoObj.getQuantity() + "|" + PercoObj.getTemplate(false).getID() + "|" + PercoObj.parseStatsString();
                SocketManager.GAME_SEND_EsK_PACKET(P, str);

            }
        }
        SocketManager.GAME_SEND_Ow_PACKET(P);
        SQLManager.SAVE_PERSONNAGE(P, true);
    }

    public void LogXpDrop(long Xp) {
        _LogXP += Xp;
    }

    public void LogObjetDrop(int guid, Item obj) {
        _LogObjets.put(guid, obj);
    }

    public long get_LogXp() {
        return _LogXP;
    }

    public String get_LogItems() {
        StringBuilder str = new StringBuilder();
        if (_LogObjets.isEmpty()) return "";
        for (Item obj : _LogObjets.values())
            str.append(";").append(obj.getTemplate(false).getID()).append(",").append(obj.getQuantity());
        return str.toString();
    }

    public void addObjet(Item newObj) {
        _objets.put(newObj.getGuid(), newObj);
    }

    public void set_Exchange(boolean Exchange) {
        _inExchange = Exchange;
    }

    public boolean get_Exchange() {
        return _inExchange;
    }

    public static void removePercepteur(int GuildID) {
        for (Entry<Integer, Collector> perco : World.getPercos().entrySet()) {
            if (perco.getValue().get_guildID() == GuildID) {
                World.getPercos().remove(perco.getKey());
                for (Player p : World.getCarte((short) perco.getValue().get_mapID()).getPersos()) {
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.getMap(), perco.getValue().getGuid());//Suppression visuelle
                }
                SQLManager.DELETE_PERCO(perco.getKey());//Supprime les percepteurs
            } else {
                continue;
            }
        }
    }

    public void addDefenseur(Player perso) {
        if (!defenseurs.contains(perso)) {
            defenseurs.add(perso);
        }
    }

    public int nombreDeDefenseurs() {
        return defenseurs.size();
    }

    public void removeDefenseur(Player perso) {
        if (defenseurs.contains(perso)) {
            defenseurs.remove(perso);
            for (Player z : World.getGuild(get_guildID()).getMembers()) {
                if (z == null) continue;
                if (z.isOnline()) {
                    SocketManager.GAME_SEND_gITP_PACKET(z, parseRemoveDefenseurToGuild(perso));
                }
            }
        }
    }

    public String parseRemoveDefenseurToGuild(Player player) {
        StringBuilder str = new StringBuilder();
        str.append("-").append(getGuid());
        if (player != null) {
            str.append("|");
            str.append(Integer.toString(player.getGuid(), 36)).append(";");
            str.append(player.getName()).append(";");
            str.append(player.get_gfxID()).append(";");
            str.append(player.getLevel()).append(";");
            str.append(Integer.toString(player.get_color1(), 36)).append(";");
            str.append(Integer.toString(player.get_color2(), 36)).append(";");
            str.append(Integer.toString(player.get_color3(), 36)).append(";");
        }
        return str.toString();
    }

    public void set_cellID(int cell) {
        _cellID = cell;
    }

    public String getDQ_Packet() {
        int guild_id = get_guildID();
        Guild guild = World.getGuild(guild_id);
        String name = guild.get_name();
        String pod = Integer.toString(guild.get_Stats(158));
        String prospection = Integer.toString(guild.get_Stats(176));
        String sagesse = Integer.toString(guild.get_Stats(124));
        String nbPerco = Integer.toString(Collector.CountPercoGuild(guild.get_id())); // DQ1;Sacrifice,1000,100,400,39.
        String str = "1;" + name + "," + pod + "," + prospection + "," + sagesse + "," + nbPerco;
        return str;
    }
}