package org.area.exchange;

import org.apache.mina.core.session.IoSession;
import org.area.client.Account;
import org.area.common.World;
import org.area.game.GameServer;
import org.area.kernel.Config;
import org.area.kernel.Console;
import org.area.kernel.Console.Color;
import org.area.kernel.Main;
import org.area.kernel.Reboot;

public class ExchangePacketHandler {
	
	ExchangeClient exchangeClient;
	public static GameServer gameServer = Main.gameServer;
	
	public static void parser(String packet, IoSession ioSession) {
		
		switch(packet.charAt(0)) {
		
		case 'A' : { //Kick player
			int guid = -1;
			try {
				guid = Integer.parseInt(packet.substring(1));
			} catch (Exception e) {break;}
			Account account = World.getCompte(guid);
			
			if (account.isOnline())
				account.getGameThread().kick();
			break;
		}
		
		case 'F' : //Free places
			switch(packet.charAt(1)) {
			case '?' : //Required
				int i = Config.CONFIG_PLAYER_LIMIT - gameServer.getClients().size();
				Main.exchangeClient.send("F" + i, ioSession);
				break;
			}
			break;
			
		case 'S' : //Server
			switch(packet.charAt(1)) {
			case 'H' : //Host
				switch(packet.charAt(2)) {
				case 'K' : //Ok
					GameServer.state = 1;
					Console.setTitle("GameServer connected - area");
					break;
				}
				break;
				
			case 'K' : //Key
				switch(packet.charAt(2)) {
				case '?' : //Required
					int i = gameServer.getMaxPlayer() - gameServer.getClients().size();
					Main.exchangeClient.send("SK" + GameServer.id + ";" + GameServer.key + ";" + i, ioSession);
					break;
					
				case 'K' : //Ok
					Console.println("server accepted by the login", Color.GREEN);
					Main.exchangeClient.send("SH" + GameServer.ip + ";" + Config.CONFIG_GAME_PORT, ioSession);
					break;
					
				case 'R' : //Refused
					Console.println("server refused by the login", Color.RED);
					Reboot.reboot();
					break;
				}
				break;
			}
			break;
		}
	}
}
