package org.area.kolizeum;

import java.util.ArrayList;

import org.area.client.Player;

public class Manager {

	public static void refreshTeamsInfos() {
		for (Kolizeum kolizeum: Kolizeum.getKolizeums()) {
			if (!kolizeum.isStarted()) {
				for (Player player: kolizeum.getTeams()) {
					StringBuilder infos = new StringBuilder();
					boolean inFirstTeam = kolizeum.getFirstTeam().contains(player);
					ArrayList<Player> team = inFirstTeam?kolizeum.getFirstTeam():kolizeum.getSecondTeam();
					ArrayList<Player> adverseTeam = inFirstTeam?kolizeum.getSecondTeam():kolizeum.getFirstTeam();
					
					infos.append(team.size()).append(",").append(Params.MAX_PLAYERS.intValue());
					infos.append("|");
					infos.append(adverseTeam.size()).append(",").append(Params.MAX_PLAYERS.intValue());
					infos.append("|");
					for (Player p: team) {
						infos.append(p.getName()).append(" (").append(p.getLevel()).append(")");
						if (infos.toString().split(",").length - 2 < team.size())
							infos.append(",");
					}
					player.send("001T"+infos.toString());
				}
			}
		}
	}
	
	public static void refreshRankInfos(Player player) {
		StringBuilder infos = new StringBuilder();
		infos.append(player.getWinKolizeum()).append("|");
		infos.append(player.getLoseKolizeum()).append("|");
		infos.append(player.getKolizeumRatio()).append("|");
		infos.append("TOP 100");
		player.send("001R"+infos.toString());
	}
	
	public static void refreshStateBlock(Kolizeum kolizeum) {
		int readyPlayers = 0;
		
		for (Player player: kolizeum.getTeams())
			if (player.isReady())
				readyPlayers++;
		
		for (Player player: kolizeum.getTeams())
			player.send("001P"+readyPlayers+"|"+Params.MAX_PLAYERS.intValue()*2);
	}
	
	public enum Params {
		GAP_LEVEL(1000), 
		MAX_PLAYERS(3),
		BREAK_TIME(20000),
		MAPS("28039,28040,28041,28042");
		
		private int intParams;
		private String stringParams;
		
		Params(int params){
			this.intParams = params;
		}
		
		Params(String params){
			this.stringParams = params;
		}
		
		public int intValue(){
			return intParams;
		}
		
		public String toString() {
			return stringParams;
		}
	}
	
}
