package org.area.arena;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.area.client.Player;
import org.area.common.Constant;
import org.area.common.Formulas;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.kolizeum.Manager.Params;
import org.area.lang.Lang;

public class Arena { /** Author: Return **/

	//Teams en attente
	public static Map<Integer, Integer> teamInWaiting = new TreeMap<Integer, Integer>(); //TeamID | LevelMoyen
	
	
	public synchronized static void addTeam(Team team)
	{
		Player first = Team.getPlayer(team, 1);
		Player second = Team.getPlayer(team, 2);
		int actualLevelTeam = (first.getLevel() + second.getLevel()) / 2;
		
		teamInWaiting.put(team.getId(), actualLevelTeam);
		for (Player c: Team.getPlayers(team)){
			c.setArena(0);
		}
		for (Entry<Integer, Integer> t: teamInWaiting.entrySet())
		{
			if (team.getId() == Team.getTeamByID(t.getKey()).getId())
				continue;
			
			int tLvl = t.getValue();
			int diff = actualLevelTeam - tLvl;
			if (diff < 0)
				diff *= -1;
			
			if (diff < 200)
			{
				if (kickBusyTeam(Team.getTeamByID(t.getKey())))
					continue;
				else if (kickBusyTeam(team))
					return;
				
				newArena(Team.getTeamByID(t.getKey()), team);
				teamInWaiting.remove(t.getKey());
				teamInWaiting.remove(team.getId());
				return;
			}
			
		}
		return;
	}
	
	public synchronized static void newArena (final Team team1, final Team team2)
	{
		for (Player p: Team.getPlayers(team1)){
			p.saveLastMap();
			p.setArena(1);
		}
		for (Player p: Team.getPlayers(team2)){
			p.saveLastMap();
			p.setArena(1);
		}
		teleport(Team.getPlayers(team1), Team.getPlayers(team2));
		Timer timer = new Timer();
		timer.schedule(new TimerTask(){
		     public void run() {
				SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(Team.getPlayers(team1).get(0).getMap(), Team.getPlayers(team1).get(0).getGuid(), Team.getPlayers(team2).get(0).getGuid());
				try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
				SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(Team.getPlayers(team2).get(0).getMap(), Team.getPlayers(team1).get(0).getGuid(), Team.getPlayers(team2).get(0).getGuid());
				
				Team.getPlayers(team1).get(0).getMap().newKolizeum(null, Team.getPlayers(team1), Team.getPlayers(team2));
				try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
				for (int i=1; i<2; i++) {
					Team.getPlayers(team1).get(0).getFight().joinFight(Team.getPlayers(team1).get(i), Team.getPlayers(team1).get(0).getGuid());
					Team.getPlayers(team2).get(0).getFight().joinFight(Team.getPlayers(team2).get(i), Team.getPlayers(team2).get(0).getGuid());
				}
		     }
		}, 3000);
	}
	
	public static void delTeam(Team team)
	{
		if (teamInWaiting.containsKey(team.getId()))
		{
			for (Player c: Team.getPlayers(team))
				c.setArena(-1);
			teamInWaiting.remove(team.getId());
			return;
		}
	}
	
	public static boolean kickBusyTeam(Team t)
	{
		for (String cc: t.getCharacters().split(","))
		{
			Player c = World.getPlayer(Integer.parseInt(cc));
			if (c.getFight() != null) {
				c.getFight().leftFight(null, c, false);
				try {
					Thread.sleep(2000);
				} catch(Exception e){}
			}
			if (c==null || c.getFight()!=null || !c.isOnline()) {
				for (String ccc: t.getCharacters().split(","))
				{
					Player cccc = World.getPlayer(Integer.parseInt(ccc));
					cccc.setArena(-1);
				}
				teamInWaiting.remove(t.getId());
				return true;
			}
			c.setArena(1);
		}
		return false;
	}

	private static void teleport(ArrayList<Player> team1, ArrayList<Player> team2) {
		ArrayList<Integer> allMaps = new ArrayList<Integer>();
		for (String map: Params.MAPS.toString().split(","))
			allMaps.add(Integer.parseInt(map));
		
		int randomValue = Formulas.getRandomValue(0, allMaps.size()-1);
		int fightMap = allMaps.get(randomValue);
		
		for (Player player: team1) {
			player.saveLastMap();
			player.teleport((short)fightMap, 1);
		}
		for (Player player: team2) {
			player.saveLastMap();
			player.teleport((short)fightMap, 1);
		}
	}
	
	public static void sendReward(Team winners, Team loosers) {
		
		for (Player c: Team.getPlayers(winners))
			if (c.getArena() == -1)
				return;
			
		
		int points;
		if (loosers.getCote() - winners.getCote() < 0)
			points = 25;
		else if (winners.getCote() - loosers.getCote() < 0)
			points = 75;
		else
			points = 50;
		winners.setCote(winners.getCote() + points);
		Team.updateTeam(winners.getId());
		
		for (Player c: Team.getPlayers(winners)) 
			c.sendMess(Lang.LANG_54, ""," "+points+ " "+Lang.LANG_55[c.getLang()]);
		
		return;
	}
	
	public static void withdrawPoints(Team loosers, Team winners) {
		
		for (Player c: Team.getPlayers(loosers))
			if (c.getArena() == -1)
				return;
			
		
		int points;
		if (loosers.getCote() - winners.getCote() < 0)
			points = 25;
		else if (winners.getCote() - loosers.getCote() < 0)
			points = 75;
		else
			points = 50;
		if (loosers.getCote() - points > 0)
			loosers.setCote(loosers.getCote() - points);
		else
			loosers.setCote(0);
		Team.updateTeam(loosers.getId());
		
		for (Player c: Team.getPlayers(loosers))
			c.sendMess(Lang.LANG_56, ""," "+points+ " "+Lang.LANG_55[c.getLang()]);
		
		return;
	}
	
	public static boolean isVerifiedTeam (int class1, int class2)
	{
		String paliers = Constant.CLASS_OSAMODAS+","+Constant.CLASS_SACRIEUR+","+Constant.CLASS_ENIRIPSA+","+Constant.CLASS_XELOR;
		for (String classes: paliers.split(","))
		{
			if (class1 == Integer.parseInt(classes))
			{
				for (String classes2: paliers.split(","))
				{
					if (class1 == Integer.parseInt(classes2))
						continue;
					else if (class2 == Integer.parseInt(classes2))
						return false;
				}
			}
		}
		return true;
	}
}
