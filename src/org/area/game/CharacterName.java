package org.area.game;

import org.area.check.Security;
import org.area.client.Account;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.kernel.Config;

public class CharacterName {
	
	public static void isValid(String packet, GameThread gameThread) {
		GameSendThread out = gameThread.getOut();
		String name = null;
		
		//verification des failles
		if(Security.isCompromised(packet, gameThread.getPlayer())) return;
		
		try {
			Account account = gameThread.getAccount();
			
			if(account.getCurPlayer() != null) {
				SocketManager.send(out, "BN");
				SocketManager.send(out, "AAE");
				return;
			}
			
			String[] infos = packet.substring(2).split("\\|");
			name = infos[0];
			int race = Integer.parseInt(infos[1]);
			
			if (SQLManager.PLAYER_EXIST(name)) {
				SocketManager.send(out, "BN");
				SocketManager.send(out, "AAEa");
				return;
			}
			
			if(name.toLowerCase().contains("modo") || 
					name.toLowerCase().contains("mj") || 
					name.toLowerCase().contains("admin")) {
				SocketManager.send(out, "BN");
				SocketManager.send(out, "AAEa");
				return;
			}
			
			//test if account is full
			if(account.getPlayers().size() > Config.MAX_PLAYERS_PER_ACCOUNT - 1) {
				SocketManager.send(out, "BN");
				SocketManager.send(out, "AAEf");
				return;
			}
			
			//for the wpe pro user :p
			if(race > 12 || race < 1) {
				SocketManager.send(out, "BN");
				SocketManager.send(out, "AAE");
				return;
			}
			if(account.createPerso(infos[0], // FIXME @Flow #Fixé
					Integer.parseInt(infos[2]), 
					Integer.parseInt(infos[1]), 
					Integer.parseInt(infos[3]),
					Integer.parseInt(infos[4]), 
					Integer.parseInt(infos[5]))) {System.out.println("aaa");
				SocketManager.send(out, "BN");
				SocketManager.send(out, "AAK");
				SocketManager.GAME_SEND_PERSO_LIST(out, account.getPlayers());
			} else SocketManager.GAME_SEND_CREATE_FAILED(out);
		} catch(Exception e) {
			SocketManager.send(out, "BN");
			SocketManager.send(out, "AAE");
			return;
		}
	}
	
	public static boolean VerifName(String name)
	{
		if(name.toLowerCase().contains("modo")	|| 
				name.toLowerCase().contains("mj") || 
				name.toLowerCase().contains("admin") ||
				name.toLowerCase().contains("[") ||
				name.toLowerCase().contains("]") ||
				name.toLowerCase().contains(";") ||
				name.toLowerCase().contains(",") ||
				name.toLowerCase().contains("!") ||
				name.toLowerCase().contains("*") ||
				name.toLowerCase().contains("/") ||
				name.toLowerCase().contains(".") ||
				name.toLowerCase().contains("'") ||
				name.toLowerCase().contains("µ") ||
				name.toLowerCase().contains("%") ||
				name.toLowerCase().contains("$") ||
				name.toLowerCase().contains("€") ||
				name.toLowerCase().contains("#") ||
				name.toLowerCase().contains("ë") ||
				name.toLowerCase().contains("*") ||
				name.toLowerCase().contains("|") ||
				name.toLowerCase().contains("¤") ||
				name.toLowerCase().contains("<") ||
				name.toLowerCase().contains(">") ||
				name.toLowerCase().contains("?") ||
				name.toLowerCase().contains("~")) {
			
			return false;
			
		}
		
		else
		{
			return true;
		}
	}
	
	public static boolean containsWhiteSpace(final String name){
	    if(name != null){
	        for(int i = 0; i < name.length(); i++){
	            if(Character.isWhitespace(name.charAt(i))){
	                return true;
	            }
	        }
	    }
	    return false;
	}
}
