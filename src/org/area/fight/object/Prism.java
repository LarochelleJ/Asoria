
package org.area.fight.object;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.area.client.Player;
import org.area.client.Player.Stats;
import org.area.common.Constant;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.Area;
import org.area.common.World.SubArea;
import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.object.Maps;
import org.area.spell.Spell;
import org.area.spell.Spell.SortStats;




public class Prism {
	private int _id;
	private int _alignement;
	private int _level;
	private short _Carte;
	private int _cell;
	private int _xY;
	private int _nom;
	private int _gfx;
	private int _inFight;
	private int _FightID;
	private int _turnTime = 60000;
	private int _honor = 0;
	private int _area = -1;
	private Map<Integer, Integer> _stats = new TreeMap<Integer, Integer>();
	private Map<Integer, SortStats> _Sorts = new TreeMap<Integer, SortStats>();
	
	public Prism(int id, int alignement, int lvl, short Carte, int cell, int honor, int area) {
		_id = id;
		_alignement = alignement;
		_level = lvl;
		_Carte = Carte;
		_cell = cell;
		_xY = 1;
		if (alignement == 1) {
			_nom = 1111;
			_gfx = 8101;
		} else {
			_nom = 1112;
			_gfx = 8100;
		}
		_inFight = -1;
		_FightID = -1;
		_honor = honor;
		_area = area;
	}
	
	public int getID() {
		return _id;
	}
	
	public int getAreaConquest() {
		return _area;
	}
	
	public void setAreaConquest(int area) {
		_area = area;
	}
	
	public int getalignement() {
		return _alignement;
	}
	
	public int getlevel() {
		return _level;
	}
	
	public short getCarte() {
		return _Carte;
	}
	
	public int getCell() {
		return _cell;
	}
	
	public Stats getStats() {
		return new Stats(_stats);
	}
	
	public Map<Integer, SortStats> getSorts() {
		return _Sorts;
	}
	
	public void refreshStats() {
		int feu = 1000 + (500 * _level);
		int intel = 1000 + (500 * _level);
		int agi = 1000 + (500 * _level);
		int sagesse = 1000 + (500 * _level);
		int chance = 1000 + (500 * _level);
		int resistance = 9 * _level;
		_stats.clear();
		_stats.put(Constant.STATS_ADD_FORC, feu);
		_stats.put(Constant.STATS_ADD_INTE, intel);
		_stats.put(Constant.STATS_ADD_AGIL, agi);
		_stats.put(Constant.STATS_ADD_SAGE, sagesse);
		_stats.put(Constant.STATS_ADD_CHAN, chance);
		_stats.put(Constant.STATS_ADD_RP_NEU, resistance);
		_stats.put(Constant.STATS_ADD_RP_FEU, resistance);
		_stats.put(Constant.STATS_ADD_RP_EAU, resistance);
		_stats.put(Constant.STATS_ADD_RP_AIR, resistance);
		_stats.put(Constant.STATS_ADD_RP_TER, resistance);
		_stats.put(Constant.STATS_ADD_AFLEE, resistance);
		_stats.put(Constant.STATS_ADD_MFLEE, resistance);
		_stats.put(Constant.STATS_ADD_PA, 6);
		_stats.put(Constant.STATS_ADD_PM, 0);
		_Sorts.clear();
		String Sorts = "56@6;24@6;157@6;63@6;8@6;81@6";
		String[] spellsArray = Sorts.split(";");
		for (String str : spellsArray) {
			if (str.equals(""))
				continue;
			String[] spellInfo = str.split("@");
			int SortID = 0;
			int Sortlevel = 0;
			try {
				SortID = Integer.parseInt(spellInfo[0]);
				Sortlevel = Integer.parseInt(spellInfo[1]);
			} catch (Exception e) {
				continue;
			}
			if (SortID == 0 || Sortlevel == 0)
				continue;
			Spell Sort = World.getSort(SortID);
			if (Sort == null)
				continue;
			SortStats SortStats = Sort.getStatsByLevel(Sortlevel);
			if (SortStats == null)
				continue;
			_Sorts.put(SortID, SortStats);
		}
	}
	
	public void setlevel(int level) {
		_level = level;
	}
	
	public int getInFight() {
		return _inFight;
	}
	
	public void setInFight(int Fight) {
		_inFight = Fight;
	}
	
	public int getFightID() {
		return _FightID;
	}
	
	public void setFightID(int Fight) {
		_FightID = Fight;
	}
	
	public void removeTurnTimer(int time) {
		_turnTime -= time;
	}
	
	public void setTurnTime(int time) {
		_turnTime = time;
	}
	
	public int getTurnTime() {
		return _turnTime;
	}
	
	public int getX() {
		Maps Carte = World.getCarte(_Carte);
		return Carte.getX();
	}
	
	public int getY() {
		Maps Carte = World.getCarte(_Carte);
		return Carte.getY();
	}
	
	public SubArea getSubArea() {
		Maps Carte = World.getCarte(_Carte);
		return Carte.getSubArea();
	}
	
	public Area getArea() {
		Maps Carte = World.getCarte(_Carte);
		return Carte.getSubArea().getArea();
	}
	
	public int getAlinSubArea() {
		Maps Carte = World.getCarte(_Carte);
		return Carte.getSubArea().getalignement();
	}
	
	public int getAlinArea() {
		Maps Carte = World.getCarte(_Carte);
		return Carte.getSubArea().getalignement();
	}
	
	public int getHonor() {
		return _honor;
	}
	
	public void addHonor(int honor) {
		_honor += honor;
		if (_honor >= 25000) {
			_level = 10;
			_honor = 25000;
		}
		for (int n = 1; n <= 10; n++) {
			if (_honor < World.getExpLevel(n).pvp) {
				_level = n - 1;
				break;
			}
		}
	}
	
	public void setCell(int cell) {
		_cell = cell;
	}
	
	public String getGMPrisme() {
		if (_inFight != -1)
			return "";
		String str = "GM|+";
		str += _cell + ";";
		str += _xY + ";0;" + _id + ";" + _nom + ";-10;" + _gfx + "^100;" + _level + ";" + _level + ";" + _alignement;
		return str;
	}
	
	public static void parseAttack(Player perso) {
		for (Prism Prisme : World.AllPrisme()) {
			if ( (Prisme._inFight == 0 || Prisme._inFight == -2) && perso.get_align() == Prisme.getalignement()) {
				SocketManager.SEND_Cp_INFO_ATTAQUANT_PRISME(perso, attackerOfPrism(Prisme._id, Prisme._Carte, Prisme._FightID));
			}
		}
	}
	
	public static void parseDefense(Player perso) {
		for (Prism Prisme : World.AllPrisme()) {
			if ( (Prisme._inFight == 0 || Prisme._inFight == -2) && perso.get_align() == Prisme.getalignement()) {
				SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(perso,
						prismProtectors(Prisme._id, Prisme._Carte, Prisme._FightID));
			}
		}
	}
	
	public static String attackerOfPrism(int id, short CarteId, int FightId) {
		String str = "+";
		str += Integer.toString(id, 36);
		for (Entry<Integer, Fight> Fight : World.getCarte(CarteId).get_fights().entrySet()) {
			if (Fight.getValue().get_id() == FightId) {
				for (Fighter fighter : Fight.getValue().getFighters(1)) {
					if (fighter.getPersonnage() == null)
						continue;
					str += "|";
					str += Integer.toString(fighter.getPersonnage().getGuid(), 36) + ";";
					str += fighter.getPersonnage().getName() + ";";
					str += fighter.getPersonnage().getLevel() + ";";
					str += "0;";
				}
			}
		}
		return str;
	}
	
	public static String prismProtectors(int id, short CarteId, int FightId) {
		String str = "+";
		String stra = "";
		str += Integer.toString(id, 36);
		for (Entry<Integer, Fight> Fight : World.getCarte(CarteId).get_fights().entrySet()) {
			if (Fight.getValue().get_id() == FightId) {
				for (Fighter fighter : Fight.getValue().getFighters(2)) {
					if (fighter.getPersonnage() == null)
						continue;
					str += "|";
					str += Integer.toString(fighter.getPersonnage().getGuid(), 36) + ";";
					str += fighter.getPersonnage().getName() + ";";
					str += fighter.getPersonnage().get_gfxID() + ";";
					str += fighter.getPersonnage().getLevel() + ";";
					str += Integer.toString(fighter.getPersonnage().get_color1(), 36) + ";";
					str += Integer.toString(fighter.getPersonnage().get_color2(), 36) + ";";
					str += Integer.toString(fighter.getPersonnage().get_color3(), 36) + ";";
					if (Fight.getValue().getFighters(2).size() > 7)
						str += "1;";
					else
						str += "0;";
				}
				stra = str.substring(1);
				stra = "-" + stra;
				Fight.getValue().setDefenseurs(stra);
			}
		}
		return str;
	}
}
