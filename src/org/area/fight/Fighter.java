package org.area.fight;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.area.client.Player;
import org.area.client.Player.Stats;
import org.area.common.Constant;
import org.area.common.Formulas;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.fight.object.Collector;
import org.area.fight.object.LaunchedSort;
import org.area.fight.object.Prism;
import org.area.fight.object.Monster.MobGrade;
import org.area.game.GameServer;
import org.area.kernel.Config;
import org.area.object.Guild;
import org.area.object.Maps.Case;
import org.area.spell.SpellEffect;
import org.area.spell.Spell.SortStats;

public class Fighter
{
	int _id = 0;
	private boolean _canPlay = false;
	private Fight _fight;
	private int _type = 0; // 1 : Personnage, 2 : Mob, 5 : Perco
	private MobGrade _mob = null;
	private Player _perso = null;
	Collector _Perco = null;
	Player _double = null;
	private int _team = -2;
	Case _cell;
	private ArrayList<SpellEffect> _fightBuffs = new ArrayList<SpellEffect>();
	private Map<Integer,Integer> _chatiValue = new TreeMap<Integer,Integer>();
	private int _orientation; 
	private Fighter _invocator;
	public int _nbInvoc = 0;
	private int _PDVMAX;
	private int _PDV;
	private boolean _isDead;
	private boolean _hasLeft;
	private int _gfxID;
	private Map<Integer,Integer> _state = new TreeMap<Integer,Integer>();
	private Fighter _isHolding;
	private Fighter _holdedBy;
	private ArrayList<LaunchedSort> _launchedSort = new ArrayList<LaunchedSort>();
	private Fighter _oldCible = null;
	private int _LifeLoose = 0;
	Prism _Prisme = null;
	private String _defenseurs = "";
	// Piège répulsif
	public int nbRepulsion = 0;
	public void setDefenseurs(String str) {
		_defenseurs = str;
	}
	
	public String getDefenseurs() {
		return _defenseurs;
	}
	private boolean _isDeconnected = false;
	private int _tourRestants = 0;
	private int _nbDeco = 0;
	
	private Player.BoostSpellStats _SpellBoost = null;
	
	public Fighter get_oldCible() {
		return _oldCible;
	}
	public void set_oldCible(Fighter cible) {
		_oldCible = cible;
	}
	
	public Fighter(Fight f, MobGrade mob)
	{
		_fight = f;
		_type = 2;
		_mob = mob;
		_id = mob.getInFightID();
		_PDVMAX = mob.getPDVMAX();
		_PDV = mob.getPDV();
		_gfxID = getDefaultGfx();
	}
	
	public Fighter(Fight f, Player perso)
	{
		_fight = f;
		if(perso._isClone)
		{
			_type = 10;
			set_double(perso);
		}else
		{
			_type = 1;
			_perso = perso;
		}
		_id = perso.getGuid();
		_PDVMAX = perso.get_PDVMAX();
		_PDV = perso.get_PDV();
		_gfxID = getDefaultGfx();
	}

	public void set_double(Player _double) {
		this._double = _double;
	}
	public Fighter(Fight Fight, Prism Prisme) {
		_fight = Fight;
		_type = 7;
		_Prisme = Prisme;
		_id = -1;
		_PDVMAX = Prisme.getlevel() * 10000;
		_PDV = Prisme.getlevel() * 10000;
		_gfxID = Prisme.getalignement() == 1 ? 8101 : 8100;
		Prisme.refreshStats();
	}
	public Fighter(Fight f, Collector Perco) {
		_fight = f;
		_type = 5;
		_Perco = Perco;
		_id = -1;
		_PDVMAX = (World.getGuild(Perco.get_guildID()).get_lvl()*100);
		_PDV = (World.getGuild(Perco.get_guildID()).get_lvl()*100);
		_gfxID = 6000;
	}
	public void set_PDV(int pdv)
	{
		_PDV = pdv;
	}
	public void set_PDVMAX(int pdv_max)
	{
		_PDVMAX = pdv_max;
	}
	public ArrayList<LaunchedSort> getLaunchedSorts()
	{
		return _launchedSort;
	}
	
	public void ActualiseLaunchedSort()
	{
		ArrayList<LaunchedSort> copie = new ArrayList<LaunchedSort>();
		copie.addAll(_launchedSort);
		int i = 0;
		for(LaunchedSort S : copie)
		{
			S.ActuCooldown();
			if(S.getCooldown() <= 0)
			{
				_launchedSort.remove(i);
				i--;
			}
			i++;
		}
	}

	public void addLaunchedSort(Fighter target,SortStats sort)
	{
		LaunchedSort launched = new LaunchedSort(target,sort);
		_launchedSort.add(launched);
	}
	
	public void addLaunchedFakeSort(Fighter target,SortStats sort, int cooldown)
	{
		LaunchedSort launched = new LaunchedSort(target,sort);
		launched.setCooldown(cooldown);
		_launchedSort.add(launched);
	}
	
	public int getGUID()
	{
		return _id;
	}
	public Fighter get_isHolding() {
		return _isHolding;
	}

	public void set_isHolding(Fighter isHolding) {
		_isHolding = isHolding;
	}

	public Fighter get_holdedBy() {
		return _holdedBy;
	}

	public void set_holdedBy(Fighter holdedBy) {
		_holdedBy = holdedBy;
	}

	public int get_gfxID() {
		return _gfxID;
	}

	public void set_gfxID(int gfxID) {
		_gfxID = gfxID;
	}

	public ArrayList<SpellEffect> get_fightBuff()
	{
		return _fightBuffs;
	}
	public void set_fightCell(Case cell)
	{
		_cell = cell;
	}
	public boolean isHide()
	{
		return hasBuff(150);
	}
	public Case get_fightCell()
	{		
		return _cell;
	}
	public void setTeam(int i)
	{
		_team = i;
	}
	public boolean isDead() {
		return _isDead;
	}

	public void setDead(boolean isDead) {
		_isDead = isDead;
	}

	public boolean hasLeft() {
		return _hasLeft;
	}

	public void setLeft(boolean hasLeft) {
		_hasLeft = hasLeft;
	}

	public Player getPersonnage()
	{
		if(_type == 1)
			return _perso;
		return null;
	}
	
	public Collector getPerco()
	{
		if(_type == 5)
			return _Perco;
		return null;
	}
	public Prism getPrisme() {
		if (_type == 7)
			return _Prisme;
		return null;
	}
	public boolean testIfCC(int tauxCC)
	{
		if(tauxCC < 2)return false;
		int agi = getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
		if(agi <0)agi =0;
		tauxCC -= getTotalStats().getEffect(Constant.STATS_ADD_CC);
		tauxCC = (int)((tauxCC * 2.9901) / Math.log(agi +12));//Influence de l'agi
		if(tauxCC<2)tauxCC = 2;
		int jet = Formulas.getRandomValue(1, tauxCC);
		return (jet == tauxCC);
	}
	
	public Stats getTotalStats()
	{
		Stats stats = new Stats(new TreeMap<Integer,Integer>());
		if(_type == 1)//Personnage
			stats = _perso.getTotalStats();
		if(_type == 2)//Mob
			stats =_mob.getStats();
		if(_type == 5)//Percepteur
			stats = World.getGuild(_Perco.get_guildID()).getStatsFight();
		if (_type == 7)
			stats = _Prisme.getStats();
		if(_type == 10)//Double
			stats = _double.getTotalStats();
		
		stats = Stats.cumulStat(stats,getFightBuffStats());
		return stats;
	}
	
	
	public void initBuffStats()
	{
		if(_type == 1)
		{
			for(Entry<Integer, SpellEffect> entry : _perso.get_buff().entrySet())
			{
				_fightBuffs.add(entry.getValue());
			}
		}
	}
	
	private Stats getFightBuffStats()
	{
		Stats stats = new Stats();
		for(SpellEffect entry : _fightBuffs)
		{
			stats.addOneStat(entry.getEffectID(), entry.getValue());
		}
		return stats;
	}
	
	public String getGmPacket(char c)
	{
		StringBuilder str = new StringBuilder();
		str.append("GM|").append(c);
		if(isHide()){
			str.append("0;");
		}
		else{
		str.append(_cell.getID()).append(";");
		}
		_orientation = 1;
		str.append(_orientation).append(";");
		str.append("0;");
		str.append(getGUID()).append(";");
		str.append(getPacketsName()).append(";");

		switch(_type)
		{
			case 1://Perso
				str.append(_perso.get_classe()).append(";");
				str.append(_perso.get_gfxID()).append("^").append(_perso.get_size()).append(";");
				str.append(_perso.get_sexe()).append(";");
				str.append(_perso.getLevel()).append(";");
				str.append(_perso.get_align()).append(",");
				str.append("0,");//TODO
				str.append((_perso.is_showWings()?_perso.getGrade():"0")).append(",");
				str.append(_perso.getGuid()).append(";");
				str.append((_perso.get_color1()==-1?"-1":Integer.toHexString(_perso.get_color1()))).append(";");
				str.append((_perso.get_color2()==-1?"-1":Integer.toHexString(_perso.get_color2()))).append(";");
				str.append((_perso.get_color3()==-1?"-1":Integer.toHexString(_perso.get_color3()))).append(";");
				str.append(_perso.getGMStuffString()).append(";");
				str.append(getPDV()).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_PA)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_PM)).append(";");
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU) >= 50) 
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(";");
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER) >= 50) 
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER)).append(";");
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU) >= 50)
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(";");	
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU) >= 50)
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(";");		
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR) >= 50)
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_AFLEE)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_MFLEE)).append(";");
				str.append(_team).append(";");
				if(_perso.isOnMount() && _perso.getMount() != null)
					str.append(_perso.getMount().get_color(_perso.parsecolortomount()));
				str.append(";");
			break;
			case 2://Mob
				str.append("-2;");
				String taille = "";
				if (!isInvocation()) {
				      if (_mob.getScaleX() == _mob.getScaleY())
				      {
				        taille = ""+_mob.getScaleY();
				      }
				      else {
				        taille = _mob.getScaleX() + "x" + _mob.getScaleY();
				      }
					} else {
						taille = "100";
					}
				str.append(_mob.getTemplate().getGfxID()).append("^"+taille+";");
				str.append(_mob.getGrade()).append(";");
				str.append(_mob.getTemplate().getColors().replace(",", ";")).append(";");
				str.append("0,0,0,0;");
				str.append(this.getPDVMAX()).append(";");
				str.append(_mob.getPA()).append(";");
				str.append(_mob.getPM()).append(";");
				str.append(_team);
			break;
			case 5://Perco
				str.append("-6;");//Perco
				str.append("6000^");//GFXID^
				Guild G = World.getGuild(Collector.GetPercoGuildID(_fight._mapOld.get_id()));
				str.append(50+G.get_lvl()).append(";"); // Size
				str.append(G.get_lvl()).append(";");
				str.append("1;");//FIXME
				str.append("2;4;");//FIXME
				str.append((int)Math.floor(G.get_lvl()/2)).append(";").append((int)Math.floor(G.get_lvl()/2)).append(";").append((int)Math.floor(G.get_lvl()/2)).append(";").append((int)Math.floor(G.get_lvl()/2)).append(";").append((int)Math.floor(G.get_lvl()/2)).append(";").append((int)Math.floor(G.get_lvl()/2)).append(";").append((int)Math.floor(G.get_lvl()/2)).append(";");//Résistances
				str.append(_team);
			break;
			case 7:// Prisme
				str.append("-2;");
				str.append((_Prisme.getalignement() == 1 ? 8101 : 8100) + "^100;");
				str.append(_Prisme.getlevel() + ";");
				str.append("-1;-1;-1;");
				str.append("0,0,0,0;");
				str.append(getPDVMAX() + ";");
				str.append(0 + ";");
				str.append(0 + ";");
				str.append(_team);
				break;
			case 10://Double
				str.append(get_double().get_classe()).append(";");
				str.append(get_double().get_gfxID()).append("^").append(get_double().get_size()).append(";");
				str.append(get_double().get_sexe()).append(";");
				str.append(get_double().getLevel()).append(";");
				str.append(get_double().get_align()).append(",");
				str.append("0,");//TODO
				str.append((get_double().is_showWings()?get_double().getALvl():"0")).append(",");
				str.append(get_double().getGuid()).append(";");	
				str.append((get_double().get_color1()==-1?"-1":Integer.toHexString(get_double().get_color1()))).append(";");
				str.append((get_double().get_color2()==-1?"-1":Integer.toHexString(get_double().get_color2()))).append(";");
				str.append((get_double().get_color3()==-1?"-1":Integer.toHexString(get_double().get_color3()))).append(";");
				str.append(get_double().getGMStuffString()).append(";");
				str.append(getPDV()).append(";");
				
				int pa = getTotalStats().getEffect(Constant.STATS_ADD_PA)>12?12:getTotalStats().getEffect(Constant.STATS_ADD_PA);
				int pm = getTotalStats().getEffect(Constant.STATS_ADD_PM)>6?6:getTotalStats().getEffect(Constant.STATS_ADD_PM);
				
				str.append(pa).append(";");
				str.append(pm).append(";");
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU) >= 50) 
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(";");
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER) >= 50) 
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER)).append(";");
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU) >= 50)
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(";");	
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU) >= 50)
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(";");		
				if(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR) >= 50)
					str.append("50").append(";");
				else 
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_AFLEE)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_MFLEE)).append(";");
				str.append(_team).append(";");
				if(get_double().isOnMount() && get_double().getMount() != null)str.append(get_double().getMount().get_color());
				str.append(";");
			break;
		}
		
		return str.toString();
	}
	
	public Player get_double() {
		return _double;
	}
	public void setState(int id, int t)
	{
		_state.remove(id);
		if(t != 0)
		_state.put(id, t);
	}
	public void sendState(Player p)
	{
		if(p.getAccount() == null || p.getAccount().getGameThread() == null) return;
		for(Entry<Integer, Integer> state : _state.entrySet())
		{
		SocketManager.GAME_SEND_GA_PACKET(p.getAccount().getGameThread().getOut(),7+"", 950+"", getGUID()+"", getGUID()+","+state.getKey()+",1");
		}
	}
	
	public boolean isState(int id)
	{
		if(_state.get(id) == null)return false;
		return _state.get(id) != 0;
	}
	
	public void decrementStates()
	{
		//Copie pour évident les modif concurrentes
		ArrayList<Entry<Integer,Integer>> entries = new ArrayList<Entry<Integer, Integer>>();
		entries.addAll(_state.entrySet());
		for(Entry<Integer,Integer> e : entries)
		{
			//Si la valeur est négative, on y touche pas
			if(e.getKey() < 0)continue;
			
			_state.remove(e.getKey());
			int nVal = e.getValue()-1;
			//Si 0 on ne remet pas la valeur dans le tableau
			if(nVal == 0)//ne pas mettre plus petit, -1 = infinie
			{
				//on envoie au org.area.client la desactivation de l'état
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, getGUID()+"", getGUID()+","+e.getKey()+",0");
				continue;
			}
			//Sinon on remet avec la nouvelle valeur
			_state.put(e.getKey(), nVal);
		}
	}
	
	public int getPDV()
	{
		int pdv = _PDV + getBuffValue(Constant.STATS_ADD_VITA);
		return pdv;
	}
	
	public void removePDV(int pdv)
	{
		_LifeLoose += pdv;
		int pdvLoose = (_LifeLoose * 3) /100;
		if(pdvLoose > 0)
		{
			_PDVMAX -= pdvLoose;
			_LifeLoose -= pdvLoose;
		}
		_PDV -= pdv;
		if(_PDV > _PDVMAX)
			_PDV = _PDVMAX;
		if (_PDV <= 0 && this.getPersonnage() != null){
			this.getPersonnage().getFight().onFighterDie(this, this);
		}
	}
	
	public void applyBeginningTurnBuff(Fight fight)
	{
		synchronized(_fightBuffs)
		{
			for(int effectID : Constant.BEGIN_TURN_BUFF)
			{
				//On évite les modifications concurrentes
				ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
				buffs.addAll(_fightBuffs);
				for(SpellEffect entry : buffs)
				{
					if(entry.getEffectID() == effectID)
					{
						if(Config.DEBUG) GameServer.addToLog("Effet de debut de tour : "+ effectID);
						entry.applyBeginingBuff(fight, this);
					}
				}
			}
		}
	}

	public SpellEffect getBuff(int id)
	{
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.getEffectID() == id && entry.getDuration() >0)
			{
				return entry;
			}
		}
		return null;
	}
	
	public boolean hasBuff(int id)
	{
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.getEffectID() == id && entry.getDuration() >0)
			{
				return true;
			}
		}
		return false;
	}
	
	public int getBuffValue(int id)
	{
		int value = 0;
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.getEffectID() == id)
				value += entry.getValue();
		}
		return value;
	}
	
	public int getMaitriseDmg(int id)
	{
		int value = 0;
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.getSpell() == id)
				value += entry.getValue();
		}
		return value;
	}

	
	public boolean getSpellValueBool(int id)
	{
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.getSpell() == id)
				return true;
		}
		return false;
	}

	public void refreshfightBuff()
	{
		//Copie pour contrer les modifications Concurentes
		ArrayList<SpellEffect> b = new ArrayList<SpellEffect>();
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.decrementDuration() > 0)//Si pas fin du buff
			{
				b.add(entry);
			}else
			{
				if(Config.DEBUG) GameServer.addToLog("Suppression du buff "+entry.getEffectID()+" sur le joueur Fighter ID= "+getGUID());
				switch(entry.getEffectID())
				{
					case 108:
						if(entry.getSpell() == 441)
						{
							//Baisse des pdvs max
							_PDVMAX = (_PDVMAX-entry.getValue());
							
							//Baisse des pdvs actuel
							int pdv = 0;
							if(_PDV-entry.getValue() <= 0){
								pdv = 0;
								_fight.onFighterDie(this, this);
								_fight.verifIfTeamAllDead();
							}
							else pdv = (_PDV-entry.getValue());
							_PDV = pdv;
						}
					break;
				
					case 150://Invisibilité
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, entry.getCaster().getGUID()+"",getGUID()+",0");
					break;
					
					case 950:
						String args = entry.getArgs();
						int id = -1;
						try
						{
							id = Integer.parseInt(args.split(";")[2]);
						}catch(Exception e){}
						if(id == -1)return;
						setState(id,0);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, entry.getCaster().getGUID()+"", entry.getCaster().getGUID()+","+id+",0");
					break;
				}
			}
		}
		_fightBuffs.clear();
		_fightBuffs.addAll(b);
	}
	
	public void addBuff(int id,int val,int duration,int turns,boolean debuff,int spellID,String args,Fighter caster, boolean isPoison)
	{
		/*if(spellID == 99 || 
		   spellID == 5 || 
		   spellID == 20 || 
		   spellID == 127 ||
		   spellID == 89 ||
		   spellID == 126 ||
		   spellID == 115 ||
		   spellID == 192 ||
		   spellID == 4 ||
		   spellID == 1 ||
		   spellID == 6 ||
		   spellID == 14 ||
		   spellID == 18 ||
		   spellID == 7 ||
		   spellID == 284 ||
		   spellID == 197 ||
		   spellID == 704
		   )
		{
			//Trêve
			//Immu
			//Prévention
			//Momification
			//Dévouement
			//Mot stimulant
			//Odorat
			//Ronce Apaisante
			//Renvoi de sort
			//Armure Incandescente
			//Armure Terrestre
			//Armure Venteuse
			//Armure Aqueuse
			//Bouclier Féca
			//Accélération Poupesque
			//Puissance Sylvestre
			//Pandanlku
			/*debuff = true;
		}*/
		debuff = true;
		isPoison = true;
		if(Config.CONFIG_SORT_INDEBUFFABLE != null || !Config.CONFIG_SORT_INDEBUFFABLE.isEmpty())
		{
			for(String split : Config.CONFIG_SORT_INDEBUFFABLE.split("\\|"))
			{
				String[] infos = split.split(":");
				if(!debuff)
					continue;
				int sortID = Integer.parseInt(infos[0]);
				if(spellID == sortID)
					debuff = false;
			}
		}
		//Si c'est le jouer actif qui s'autoBuff, on ajoute 1 a la durée
		if(id == 781)
		{
			if(caster.getGUID() == this.getGUID())
				return;
			_fightBuffs.add(new SpellEffect(id,val,(_canPlay?duration+1:duration),turns,debuff,caster,args,spellID, isPoison));
		}else
		{
			_fightBuffs.add(new SpellEffect(id,val,(_canPlay?duration+1:duration),turns,debuff,caster,args,spellID, isPoison));
		}
		
		if(Config.DEBUG) GameServer.addToLog("Ajout du Buff "+id+" sur le personnage Fighter ID = "+this.getGUID()+"("+caster.getGUID()+") val : "+val+" duration : "+duration+" turns : "+turns+" debuff : "+debuff+" spellid : "+spellID+" args : "+args);	
			
		switch(id)
		{
			case 6://Renvoie de sort
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), -1, val+"", "10", "", duration, spellID);
			break;
			
			case 79://Chance éca
				val = Integer.parseInt(args.split(";")[0]);
				String valMax = args.split(";")[1];
				String chance = args.split(";")[2];
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax, chance, "", duration, spellID);
			break;
			
			case 788://Fait apparaitre message le temps de buff sacri Chatiment de X sur Y tours
				val = Integer.parseInt(args.split(";")[1]);
				String valMax2 = args.split(";")[2];
				if(Integer.parseInt(args.split(";")[0]) == 108)return;
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, ""+val, ""+valMax2, "", duration, spellID);		
				
			break;

			case 98://Poison insidieux
			case 107://Mot d'épine (2à3), Contre(3)
			case 100://Flèche Empoisonnée, Tout ou rien
			case 108://Mot de Régénération, Tout ou rien
			case 165://Maîtrises
				val = Integer.parseInt(args.split(";")[0]);
				String valMax1 = args.split(";")[1];
				if(valMax1.compareTo("-1") == 0 || spellID == 82 || spellID == 94)
				{
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration, spellID);		
				}else if(valMax1.compareTo("-1") != 0)
				{
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax1, "", "", duration, spellID);
				}
				break;

			default:
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration, spellID);
			break;
		}
	}
	
	public int getInitiative()
	{
		if(_type == 1)
			return _perso.getInitiative();
		if(_type == 2)
			return _mob.getInit();
		if(_type == 5)
			return World.getGuild(_Perco.get_guildID()).get_lvl();
		if(_type == 10)
			return _double.getInitiative();
		
		return 0;
	}
	public int getPDVMAX()
	{
		return _PDVMAX + getBuffValue(Constant.STATS_ADD_VITA);
	}
	
	public int get_lvl() {
		if(_type == 1)
			return _perso.getLevel();
		if(_type == 2)
			return _mob.getLevel();
		if(_type == 5)
			return World.getGuild(_Perco.get_guildID()).get_lvl();
		if (_type == 7)
			return _Prisme.getlevel();
		if(_type == 10)
			return _double.getLevel();

		return 0;
	}
	public String xpString(String str)
	{
		if(_perso != null)
		{
			int max = _perso.getLevel()+1;
			if(max>World.getExpLevelSize())max = World.getExpLevelSize();
			return World.getExpLevel(_perso.getLevel()).perso+str+_perso.get_curExp()+str+World.getExpLevel(max).perso;		
		}
		return "0"+str+"0"+str+"0";
	}
	public String getPacketsName()
	{
		if(_type == 1)
			return _perso.getName();
		if(_type == 2)
			return _mob.getTemplate().getID()+"";
		if(_type == 5)
			return (_Perco.get_N1()+","+_Perco.get_N2());
		if (_type == 7)
			return (_Prisme.getalignement() == 1 ? 1111 : 1112) + "";
		if(_type == 10)
			return _double.getName();
		
		return "";
	}
	public MobGrade getMob()
	{
		if(_type == 2)
			return _mob;
		
		return null;
	}
	public int getTeam()
	{
		return _team;
	}
	public int getTeam2()
	{
		return _fight.getTeamID(_id);
	}
	public int getOtherTeam()
	{
		return _fight.getOtherTeamID(_id);
	}
	public boolean canPlay()
	{
		return _canPlay;
	}
	public void setCanPlay(boolean b)
	{
		_canPlay = b;
	}
	public ArrayList<SpellEffect> getBuffsByEffectID(int effectID)
	{
		ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
		for(SpellEffect buff : _fightBuffs)
		{
			if(buff.getEffectID() == effectID)
				buffs.add(buff);
		}
		return buffs;
	}
	public Stats getTotalStatsLessBuff()
	{
		Stats stats = new Stats(new TreeMap<Integer,Integer>());
		if(_type == 1)
			stats = _perso.getTotalStats(true);
		if(_type == 2)
			stats =_mob.getStats();
		if(_type == 5)
			stats = World.getGuild(_Perco.get_guildID()).getStatsFight();
		if (_type == 7)
			stats = _Prisme.getStats();
		if(_type == 10)
			stats = _double.getTotalStats(true);
		
		return stats;
	}
	public int getPA()
	{
		if(_type == 1)
			return getTotalStats().getEffect(Constant.STATS_ADD_PA);
		if(_type == 2)
			return getTotalStats().getEffect(Constant.STATS_ADD_PA) + _mob.getPA();
		if(_type == 5)
			return getTotalStats().getEffect(Constant.STATS_ADD_PM) + 6;
		if(_type == 10)
		{
			int PA = getTotalStats().getEffect(Constant.STATS_ADD_PA)>12?12:getTotalStats().getEffect(Constant.STATS_ADD_PA);
			PA += this.getBuffValue(Constant.STATS_ADD_PA);
			return PA;
		}
		
		return 0;
	}
	public int getPM()
	{
		if(_type == 1)
			return getTotalStats().getEffect(Constant.STATS_ADD_PM);
		if(_type == 2)
			return getTotalStats().getEffect(Constant.STATS_ADD_PM) + _mob.getPM();
		if(_type == 5)
			return getTotalStats().getEffect(Constant.STATS_ADD_PM) + 3;
		if(_type == 10)
		{
			int PM = getTotalStats().getEffect(Constant.STATS_ADD_PM)>6?6:getTotalStats().getEffect(Constant.STATS_ADD_PM);
			PM += this.getBuffValue(Constant.STATS_ADD_PM);
			return PM;
		}
		
		return 0;
	}
	

	public int getCurPA(Fight fight)
	{
		return fight._curFighterPA;
	}
	
	public int getCurPM(Fight fight)
	{
		return fight._curFighterPM;
	}
	
	public void setCurPM(Fight fight, int pm)
	{
		fight._curFighterPM = pm;
	}
	
	public void setCurPA(Fight fight, int pa)
	{
		fight._curFighterPA = pa;
	}
	
	public void setInvocator(Fighter caster)
	{
		_invocator = caster;
	}
	
	public Fighter getInvocator()
	{
		return _invocator;
	}
	
	public boolean isInvocation()
	{
		return (_invocator!=null);
	}
	
	public boolean isPerco()
	{
		return (_Perco!=null);
	}

    public boolean isDouble()
	{
		return (_double!=null);
	}
    public boolean isPrisme() {
		return (_Prisme != null);
	}
	public void debuff()
	{
		ArrayList<SpellEffect> newBuffs = new ArrayList<SpellEffect>();
		//on vérifie chaque buff en cours, si pas débuffable, on l'ajout a la nouvelle liste
		for(SpellEffect SE : _fightBuffs)
		{
			if(!SE.isDebuffabe())newBuffs.add(SE);
			//On envoie les Packets si besoin
			switch(SE.getEffectID())
			{
				case Constant.STATS_ADD_PA:
				case Constant.STATS_ADD_PA2:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 101,getGUID()+"",getGUID()+",-"+SE.getValue());
				break;
				
				case Constant.STATS_ADD_PM:
				case Constant.STATS_ADD_PM2:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 127,getGUID()+"",getGUID()+",-"+SE.getValue());
				break;
				case 150:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150,getGUID()+"",getGUID()+",0");
					//On actualise la position
					SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(_fight, 7,this);
				break;
				case 950:
					String args = SE.getArgs();
					int id = -1;
					try
					{
						id = Integer.parseInt(args.split(";")[2]);
					}catch(Exception e){}
					if(id == -1)return;
					setState(id,0);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, SE.getCaster().getGUID()+"", SE.getCaster().getGUID()+","+id+",0");
				break;
			}
		}
		_fightBuffs.clear();
		_fightBuffs.addAll(newBuffs);
		if(_perso != null && !_hasLeft)
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
	}
	
	public boolean canBeDebuff(){
		for(SpellEffect SE : _fightBuffs)
		{
			if(SE.isDebuffabe()){
				return true;
			}
		}
		return false;
	}

	public void fullPDV()
	{
		_PDV = _PDVMAX;
	}

	public void setIsDead(boolean b)
	{
		_isDead = b;
	}

	public void unHide(int spellid)
	{
		//on retire le buff invi
		if(spellid != -1)// -1 : CAC
		{
			switch(spellid) 
			{ 
					case 66: 
					case 71:
					case 181: 
					case 196: 
					case 200: 
					case 219: 
					return; 
			}
		}
		ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
		buffs.addAll(get_fightBuff());
		for(SpellEffect SE : buffs)
		{
			if(SE.getEffectID() == 150)
				get_fightBuff().remove(SE);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150,getGUID()+"",getGUID()+",0");
		//On actualise la position
		SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(_fight, 7,this);
	}

	public int getPdvMaxOutFight()
	{
		if(_perso != null)return _perso.get_PDVMAX();
		if(_mob != null)return _mob.getPDVMAX();
		return 0;
	}

	public Map<Integer, Integer> get_chatiValue() {
		return _chatiValue;
	}

	public int getDefaultGfx()
	{
		if(_perso != null)return _perso.get_gfxID();
		if(_mob != null)return _mob.getTemplate().getGfxID();
		return 0;
	}

	public long getXpGive()
	{
		if(_mob != null)return _mob.getBaseXp();
		return 0;
	}
	public void addPDV(int max) 
	{
		_PDVMAX = (_PDVMAX+max);
		_PDV = (_PDV+max);
	}
	public boolean canLaunchSpell(int spellID) {
		if(!this.getPersonnage().hasSpell(spellID))
			return false;
		else return LaunchedSort.coolDownGood(this,spellID);
	}

	public void deleteBuffByFighter(Fighter F)
	{
		ArrayList<SpellEffect> b = new ArrayList<SpellEffect>();
		for(SpellEffect entry : _fightBuffs)
		{
			if(entry.getCaster().getGUID() != F.getGUID())//Si pas fin du buff
			{
				b.add(entry);
			}
			else
			{
				if(Config.DEBUG) GameServer.addToLog("Suppression du buff "+entry.getEffectID()+" sur le joueur Fighter ID= "+getGUID());
				switch(entry.getEffectID())
				{
					case Constant.STATS_ADD_PA:
					case Constant.STATS_ADD_PA2:
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 101,getGUID()+"",getGUID()+",-"+entry.getValue());
					break;
					
					case Constant.STATS_ADD_PM:
					case Constant.STATS_ADD_PM2:
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 127,getGUID()+"",getGUID()+",-"+entry.getValue());
					break;
					case 108:
						if(entry.getSpell() == 441)
						{
							//Baisse des pdvs max
							_PDVMAX = (_PDVMAX-entry.getValue());
							
							//Baisse des pdvs actuel
							int pdv = 0;
							if(_PDV-entry.getValue() <= 0){
								pdv = 0;
								_fight.onFighterDie(this, entry.getCaster());
								_fight.verifIfTeamAllDead();
							}
							else pdv = (_PDV-entry.getValue());
							_PDV = pdv;
						}
					break;
				
					case 150://Invisibilité
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, entry.getCaster().getGUID()+"",getGUID()+",0");
					break;
					
					case 950:
						String args = entry.getArgs();
						int id = -1;
						try
						{
							id = Integer.parseInt(args.split(";")[2]);
						}catch(Exception e){}
						if(id == -1)return;
						setState(id,0);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, entry.getCaster().getGUID()+"", entry.getCaster().getGUID()+","+id+",0");
					break;
				}
			}
		}
		_fightBuffs.clear();
		_fightBuffs.addAll(b);
		if(_perso != null && !_hasLeft)
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
	}
	public void setSpellStats()
	{
		_SpellBoost = _perso.getTotalBoostSpellStats();
	}
	public boolean haveSpellStat(int spellID, int statID)
	{
		if(_SpellBoost == null) return false;
		return _SpellBoost.haveStat(spellID, statID);
	}
	public int getSpellStat(int spellID, int statID)
	{
		if(_SpellBoost == null) return 0;
		return _SpellBoost.getStat(spellID, statID);
	}
	//Déconnexion en combat
	public void Deconnect()
	{
		if(_isDeconnected) return;
		_isDeconnected = true;
		_tourRestants = 20;
		_nbDeco++;
	}
	public int getNBDeco()
	{
		return _nbDeco;
	}
	public void Reconnect()
	{
		_isDeconnected = false;
		_tourRestants = 0;
	}
	public boolean isDeconnected()
	{
		if(_hasLeft) return false;
		return _isDeconnected;
	}
	public int getToursRestants()
	{
		return _tourRestants;
	}
	public void newTurn()
	{
		_tourRestants--;
	}
	public int getPDVWithBuff() {
		return _PDV + getBuffValue(125);
	}
}