package org.area.arena;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.area.client.Player;
import org.area.common.SocketManager;
import org.area.fight.Fight;
import org.area.kernel.Config;


public class GdG { /** Author: Return **/

	private int _idGuild;
	private String _target;
	private int _countVictims;
	private int _countCibles;
	private static Map<Integer,Player> 	_Attackers	= new TreeMap<Integer,Player>();
	private static Map<Integer,Player> 	_Deffenders	= new TreeMap<Integer,Player>();
	private static ArrayList<Player> _persos1 = new ArrayList<Player>();
	private static ArrayList<Player> _persos2 = new ArrayList<Player>();

	public GdG(int guildID, String target, int victimsNB, int cibleNB)
	{
		this._idGuild = guildID;
		this._target = target;
		this._countVictims = victimsNB;
		this._countCibles = cibleNB;
	}
	public int get_guildID()
	{
		return _idGuild;
	}
	public String get_Target()
	{
		return _target;
	}
	public int get_Victims()
	{
		return _countVictims;
	}
	public int get_Cibles()
	{
		return _countCibles;
	}
	public void set_guildID(int guildID)
	{
		this._idGuild = guildID;
	}
	public void set_Target(String target)
	{
		this._target = target;
	}
	public void set_victims(int victims)
	{
		this._countVictims = victims;
	}
	public void set_cibles(int cibles)
	{
		this._countCibles = cibles;
	}
	public void Add_Victims()
	{
		this._countVictims = _countVictims + 1;
	}
	public void Add_Cibles()
	{
		this._countCibles = _countCibles + 1;
	}
	public Map<Integer, Player> get_Attackers()
	{
		return _Attackers;
	}
	public Map<Integer, Player> get_Deffensers()
	{
		return _Deffenders;
	}
	public void put_Attackers(Player p)
	{
		_Attackers.put(p.getGuid(), p);
	}
	public void put_Deffenders(Player p)
	{
		_Deffenders.put(p.getGuid(), p);
	}
	
	//////////////////// Ajoute par Shisen //////////////////////////////////

	public static void addPlayer1(Player player)
	{
		synchronized(_persos1)
		{
			_persos1.add(player);
		}
	}
	
	public static void addPlayer2(Player player)
	{
		synchronized(_persos2)
		{
			_persos2.add(player);
		}
	}
	
	public synchronized static void newDeathMatch ()
	{
		Team team1 = new Team(_persos1, _persos1.size());
		Team team2 = new Team(_persos2, _persos2.size());
		for (Player p: team1.getkCharacters())
		{
			p.setLastMapFight(p.getMap().get_id());
			p.setGvG(1);
		}
		for (Player p: team2.getkCharacters())
		{
			p.setLastMapFight(p.getMap().get_id());
			p.setGvG(1);
		}
		teleport(team1.getkCharacters(), team2.getkCharacters());
		SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(team1.getkCharacters().get(0).getMap(), team1.getkCharacters().get(0).getGuid(), team2.getkCharacters().get(0).getGuid());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(team2.getkCharacters().get(0).getMap(), team1.getkCharacters().get(0).getGuid(), team2.getkCharacters().get(0).getGuid());
		@SuppressWarnings("unused")
		Fight f = team1.getkCharacters().get(0).getMap().newKoli(team1.getkCharacters(), team2.getkCharacters());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		
		for (int i=1; i < team1.getkCharacters().size(); i++) 
		{
			team1.getkCharacters().get(0).getFight().joinFight(team1.getkCharacters().get(i), team1.getkCharacters().get(0).getGuid());
		}
		for (int i=1; i < team2.getkCharacters().size(); i++)
		{
			team2.getkCharacters().get(0).getFight().joinFight(team2.getkCharacters().get(i), team2.getkCharacters().get(0).getGuid());
		}
		_persos1.removeAll(_persos1);
		_persos2.removeAll(_persos2);
		return;
	}
		
	private static int getRandomMap() {
		Random rand = new Random();
		switch(rand.nextInt(4)+1) {
		case 1 : return Integer.parseInt(Config.KOLIMAPS.split(",")[0]);
		case 2 : return Integer.parseInt(Config.KOLIMAPS.split(",")[1]);
		case 3 : return Integer.parseInt(Config.KOLIMAPS.split(",")[2]);
		case 4 : return Integer.parseInt(Config.KOLIMAPS.split(",")[3]);
		default : return Integer.parseInt(Config.KOLIMAPS.split(",")[0]);
		}
	}

	private static void teleport(ArrayList<Player> team1, ArrayList<Player> team2) {
		short MAP_ID = (short) getRandomMap();
		for (Player p : team1)
			p.teleport(MAP_ID, 1);
		for (Player p : team2)
			p.teleport(MAP_ID, 0);
	}
	
}
