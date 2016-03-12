package org.area.arena;

import java.util.ArrayList;
import java.util.Random;

import org.area.client.Player;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.fight.Fight;
import org.area.game.tools.AllColor;
import org.area.kernel.Config;

public class dm { 

	public synchronized static void addPlayer(Player player)
	{
		Team actualTeam = null;
		boolean hasFind = false;
		int lLevel = 0;
		
		if(Team.koliTeams.size() > 0)
		{
			for (Team team: Team.koliTeams)
			{
				int tLvl = team.getkLevel();
				int diff = player.getLevel() - tLvl;
				if (diff < 0)
					diff *= -1;
				
				if (diff < Config.DM_LEVEL && team.getkCharacters().size() < Config.DMMAX_PLAYER)
				{
					team.getkCharacters().add(player);
					actualTeam = team;
					player.setDeathMatch(0);
					hasFind = true;
					lLevel = team.getkLevel();
					SocketManager.GAME_SEND_MESSAGE(player, "<b>DeathMatch:</b> Inscription prise en compte !", AllColor.PINK);
					break;
				}
			}
		}
		
		if (!hasFind)
		{
			actualTeam = new Team(player, player.getLevel());
			Team.koliTeams.add(actualTeam);
			player.setDeathMatch(0);
			int level = Config.DM_LEVEL;
			if (player.getLevel() + level > 200){
				while(player.getLevel() + level > 200)
					level --;
			}
			SocketManager.GAME_SEND_MESSAGE(player, "<b>DeathMatch:</b> Inscription prise en compte !", AllColor.PINK);
			SocketManager.GAME_SEND_MESSAGE_TO_ALL( "<b>DeathMatch:</b> "+(Config.DMMAX_PLAYER - 1)+" joueurs à l'appel de niveau "+(player.getLevel()-Config.DM_LEVEL)+" - "+(player.getLevel()+level)+" pour compléter une team !", AllColor.PINK);
			return;
		}
		
		if (actualTeam.getkCharacters().size() == Config.DMMAX_PLAYER)
		{
			for (Team team: Team.koliTeams)
			{
				if (team.getkCharacters().contains(player))
					continue;
				
				int tLvl = team.getkLevel();
				int diff = actualTeam.getkLevel() - tLvl;
				if (diff < 0)
					diff *= -1;
				
				if (diff < Config.DM_LEVEL && team.getkCharacters().size() == Config.DMMAX_PLAYER)
				{
					if (kickBusyTeam(team))
						continue;
					else if (kickBusyTeam(actualTeam))
						return;
					
					newDeathMatch(actualTeam, team);
					Team.koliTeams.remove(team);
					Team.koliTeams.remove(actualTeam);
					String team1 = null;
					String team2 = null;
					
					for (Player c: team.getkCharacters()){
						if (team1 != null)
							team1 += " <b>,</b> " + c.getName();
						else
							team1 = c.getName();
					}
					for (Player c: actualTeam.getkCharacters()){
						if (team2 != null) 
							team2 += " <b>,</b> " + c.getName();
						else 
							team2 = c.getName();
						
					}
					SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>DeathMatch:</b> "+team1+" <b>VS</b> "+team2, AllColor.PINK);
					return;
				}
				
			}
			int level = Config.DM_LEVEL;
			int kLevel = lLevel;
			if (kLevel + level > 200){
				while(kLevel + level > 200)
					level --;
			}
			SocketManager.GAME_SEND_MESSAGE_TO_ALL( "<b>DeathMatch:</b> "+Config.DMMAX_PLAYER+" joueurs manquants "+(kLevel-Config.DM_LEVEL)+" - "+(kLevel+level)+" pour débuter un nouveau match !", AllColor.PINK);
		}
		return;
	}
	
	public synchronized static void newDeathMatch (Team team1, Team team2)
	{
		for (Player p: team1.getkCharacters()){
			p.setLastMapFight(p.getMap().get_id());
			p.setDeathMatch(1);
		}
		for (Player p: team2.getkCharacters()){
			p.setLastMapFight(p.getMap().get_id());
			p.setDeathMatch(1);
		}
		teleport(team1.getkCharacters(), team2.getkCharacters());
		SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(team1.getkCharacters().get(0).getMap(), team1.getkCharacters().get(0).getGuid(), team2.getkCharacters().get(0).getGuid());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(team2.getkCharacters().get(0).getMap(), team1.getkCharacters().get(0).getGuid(), team2.getkCharacters().get(0).getGuid());
		@SuppressWarnings("unused")
		Fight f = team1.getkCharacters().get(0).getMap().newKoli(team1.getkCharacters(), team2.getkCharacters());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		for (int i=1; i<Config.DMMAX_PLAYER; i++) {
			team1.getkCharacters().get(0).getFight().joinFight(team1.getkCharacters().get(i), team1.getkCharacters().get(0).getGuid());
			team2.getkCharacters().get(0).getFight().joinFight(team2.getkCharacters().get(i), team2.getkCharacters().get(0).getGuid());
		}
		return;
	}
	
	
	public synchronized static void delPlayer(Player player)
	{
		try {
			for(Team team: Team.koliTeams)
			{
				if (team.getkCharacters().contains(player))
				{
					team.getkCharacters().remove(player);
					if (team.getkCharacters().size() == 0)
						Team.koliTeams.remove(team);
					player.setDeathMatch(-1);
					SocketManager.GAME_SEND_MESSAGE(player, "<b>DeathMatch:</b> Désinscription acceptée !", AllColor.PINK);
				}
			}
		}catch (Exception e){}
			return;
	}
	
	public static boolean kickBusyTeam(Team t)
	{
		for (Player c: t.getkCharacters())
		{
			if (c.getFight() != null) {
				c.getFight().leftFight(null, c, false);
				try {
					Thread.sleep(2000);
				} catch(Exception e){}
			}
			if (c==null || c.getFight()!=null || !c.isOnline()) {
				c.setDeathMatch(-1);
				t.getkCharacters().remove(c);
				if (t.getkCharacters().size() == 0)
					Team.koliTeams.remove(t);
				SocketManager.GAME_SEND_MESSAGE(c, "<b>DeathMatch:</b> Vous avez été désinscris du DeathMatch pour indisponibilité !", AllColor.PINK);
				return true;
			}
		}
		return false;
	}
	
	private static int getRandomMap() {
		Random rand = new Random();
		int alea = rand.nextInt(Config.KOLIMAPS.split(",").length-1);
		return Integer.parseInt(Config.KOLIMAPS.split(",")[alea]);
	}

	private static void teleport(ArrayList<Player> team1, ArrayList<Player> team2) {
		short MAP_ID = (short) getRandomMap();
		for (Player p : team1)
			p.teleport(MAP_ID, World.getCarte(MAP_ID).getRandomFreeCellID());
		for (Player p : team2)
			p.teleport(MAP_ID, World.getCarte(MAP_ID).getRandomFreeCellID());
	}
}
