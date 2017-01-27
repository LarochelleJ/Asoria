package org.area.kernel;

import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.event.Event;
import org.area.exchange.ExchangeClient;
import org.area.game.GameServer;
import org.area.kernel.Console.Color;

public class Main {
	
	//Thread Groups
	public static ThreadGroup THREAD_GAME = null;
	public static ThreadGroup THREAD_GAME_SEND = null;
	public static ThreadGroup THREAD_REALM = null;
	public static ThreadGroup THREAD_IA = null;
	public static ThreadGroup THREAD_SAVE = null;
	
	//System
	public static double revision = 0.1;
	public static boolean isRunning = false;
	
	public static transient ExchangeClient exchangeClient;
	public static GameServer gameServer;
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Reboot.start();
			}
		});
		
		THREAD_GAME = new ThreadGroup(Thread.currentThread().getThreadGroup(), "GameThread");
		THREAD_REALM = new ThreadGroup(Thread.currentThread().getThreadGroup(), "RealmThread");
		THREAD_IA = new ThreadGroup(Thread.currentThread().getThreadGroup(), "IaThread");
		THREAD_SAVE = new ThreadGroup(Thread.currentThread().getThreadGroup(), "SaveThread");
		THREAD_GAME_SEND = new ThreadGroup(Thread.currentThread().getThreadGroup(), "GameThreadSend");
	}
	
	public static void main(String[] args) {
		Console.setTitle("Loading...");
		Console.clear(true);
		
		Console.setTitle("Configuration");
		Config.load();
		
		Console.setTitle("Database");
		if(!SQLManager.setUpConnexion(false)) {
			Console.println("[Database connexion : false]", Color.RED);
			Reboot.reboot();
		}
		
		//Chargement de la base de donn�e
		Console.setTitle("World");
		World.createWorld();
		
		isRunning = true;
		Console.setTitle("GameServer");
		gameServer = new GameServer();
		Console.println("GameServer on port " + Config.CONFIG_GAME_PORT, Color.GREEN); 
		
		Console.setTitle("Exchange");
		/*exchangeClient = new ExchangeClient();
		exchangeClient.start();*/
		//new ExchangeClient().start();
		Event.scheduleEvents();
		
		Console.setTitle("Area - GameServer "+GameServer.id);
		Console.print("\nArea started\n", Color.GREEN);
		Console.print("-------------------------------------------------------------------------------\n", Color.RED);
		Console.clear(false);
		new ConsoleInputAnalyzer();
	}
	
	public static void listThreads(boolean isError) throws Exception {
		Console.println("\nListage des threads", Color.YELLOW);
		
		if(isError) SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Thread", "La RAM est surcharg�e !");
		
		try {
			Console.println(gameServer.getPlayerNumber() + " player online", Color.GREEN);
			ThreadGroup threadg = Thread.currentThread().getThreadGroup();
			
			if(!Thread.currentThread().getThreadGroup().getName().equalsIgnoreCase("main")) threadg = threadg.getParent();
			
			Console.println(threadg.activeCount()+" thread active", Color.YELLOW);
			Thread[] threads = new Thread[threadg.activeCount()];
			threadg.enumerate(threads);
			
			for(Thread t : threads)
				if(!isError)
					Console.println(t.getThreadGroup().getName() + 
							" " + t.getName() + " (" + t.getState() + 
							") => " + t.getId(), Color.YELLOW);
		} catch(Exception e) { Console.println("listing error", Color.RED); }
		
		if(isError) {
			try {
				Console.println(THREAD_IA.activeCount() + " threads IA deleted", Color.YELLOW);
				Thread[] threadd = new Thread[THREAD_IA.activeCount()];
				THREAD_IA.enumerate(threadd);
				
				for(Thread t : threadd) t.interrupt();
				
				Console.println(THREAD_IA.activeCount() + " threads IA remaining", Color.GREEN);
			} catch(Exception e){
				e.printStackTrace();
				SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Thread", "Suppression impossible"); 
			} try {
				Console.println("attempt to purge the ram", Color.YELLOW);
				Runtime r = Runtime.getRuntime();
				r.runFinalization();
				r.gc();
				System.gc();
				Console.println("ram purged", Color.GREEN);
				gameServer.restartGameServer();
			} catch(Exception e1) { Console.println("impossible to purge the ram", Color.RED); }
		}
	}
}
