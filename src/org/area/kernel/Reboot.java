package org.area.kernel;

import org.area.client.Player;
import org.area.common.SQLManager;
import org.area.common.World;

public class Reboot {
	
	public static final int TIME = 24*30; //hours
	
	public static void start() {
		if(!Config.IS_SAVING) 
			World.saveAll(null);
		
		Main.isRunning = false;
		
		try { Main.gameServer.kickAll(); } catch(Exception e) {}
		Main.gameServer.stop();
		Main.gameServer = null;
		Main.exchangeClient.stop();
		
		
		try { World.clearAllVar(); } catch(Exception e) {}	
		try { Main.listThreads(true); } catch(Exception e) {}
		
		SQLManager.closeCons(false);
		System.gc();
		
		Main.main(null);
	}
	
	public static void reboot() {
		Main.isRunning = false;
		Main.exchangeClient.stop();
		for (Player p : World.getPersos().values()) {
			p.save(true);
		}
		for (Player player: World.getOnlinePlayers()) {
			//player.save(true);
			player.send("000OUT");
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) { }
		
		System.exit(0);
	}
}
