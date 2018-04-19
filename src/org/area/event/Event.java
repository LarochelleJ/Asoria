package org.area.event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.area.client.Player;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.event.type.EventCache;
import org.area.event.type.EventQuizz;
import org.area.event.type.EventSurvivor;
import org.area.event.type.EventZaap;
import org.area.game.tools.Util;
import org.area.kernel.Config;
import org.area.lang.Lang;
import org.area.object.Maps;
import org.area.object.NpcTemplate.NPC;


public class Event {

	//Event
	private int id;
	private int type;
	private int minPlayers;
	private int maxPlayers;
	private int status;
	private int startTime;
	private boolean automatic;
	private Map<Integer, Player> players = Collections.synchronizedMap(new ConcurrentHashMap<Integer, Player>());
	private ArrayList<Player> winners = new ArrayList<Player>();
	//Status
	public final static int OFFLINE = -1;
	public final static int SCHEDULED = 0;
	public final static int OPENED = 1;
	public final static int STARTED = 2;
	//Public
	public static Map<Integer, Event> events = new HashMap<Integer, Event>();
	private static Map<Integer, Event> disconnectedPlayers = new HashMap<Integer, Event>();
	//Private events
	private EventQuizz eventQuizz;
	private EventCache eventCache;
	private EventZaap eventZaap;
	private EventSurvivor eventSurvivor;
	
	public Event(int type, int minPlayers, int maxPlayers, int startTime, boolean automatic) {
		setId(getNextID());
		setType(type);
		setMinPlayers(minPlayers);
		setMaxPlayers(maxPlayers);
		setStartTime(startTime);
		setAutomatic(automatic);
		setStatus(OFFLINE);
	}
	
	public synchronized static int getNextID() {
		int max = 1;
		for(int a : getEvents().keySet())if(a > max)max = a;
		return max+1;
	}
	
	public static void addEvent(Event event) {
		getEvents().put(event.getId(), event);
	}
	
	public static void scheduleEvents() {
		for (Event event: getEvents().values())
			event.launch();
	}
	
	public void launch() {
		switch (getStatus()) {
			case OFFLINE:
				schedule();
				break;
			case SCHEDULED:
				schedule();
				sendSubscription();
				break;
			case OPENED:
				start();
				break;
			case STARTED:
				end();
				break;
		}
	}
	
	public synchronized void addPlayer(Player player) {
		kickBusys(getPlayers());
		if (getStatus() != OPENED) {
			player.sendMess(Lang.LANG_34);
			return;
		} else if (getPlayers().size() < getMaxPlayers() && player.getEvent() == null) {
			getMPlayers().put(player.getGuid(), player);
			player.setEvent(this);
			player.sendMess(Lang.LANG_35);
			return;
		} else {
			player.sendMess(Lang.LANG_36);
			return;
		}
	}
	
	public void kickBusys(List <Player> players) {
		if (players != null)
			for (Player i: players) {
				if (i == null || !i.isOnline() || i.getFight() != null) {
					if (i != null && i.isOnline())
						i.sendMess(Lang.LANG_37);
					removePlayer(i);
				}
			}
	}

	public void removePlayer(Player player) {
		getMPlayers().remove(player.getGuid());
		player.setEvent(null);
	}
	
	public void schedule() {
		int time;
		if (getStatus() == OFFLINE) {
			time = timeToSchedule(getStartTime())*60*1000;
			setStatus(SCHEDULED);
		} else {
			time = EventConstant.TIME_TO_STARTED;
			setStatus(OPENED);
		}
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask(){
		     public void run() {
		    	 launch();
		     }
		}, time);
	}
	
	public void start() {
		kickBusys(getPlayers());
		if (getPlayers().size() < getMinPlayers()) {
			SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Event: </b> "+getName()+" "+Lang.text(Lang.LANG_87), Config.CONFIG_MOTD_COLOR);
			cancel(getPlayers());
			return;
		}
		setStatus(STARTED);
		new EventStarted(this);
	}
	
	public void end() {
		if (!getWinners().isEmpty()) {
			sendRewards();
			if (getWinners().size() > 1) {
				String gagnants = "";
				for (Player p: getWinners()) {
					if (gagnants.isEmpty())
						gagnants = p.getName();
					else gagnants += "," + p.getName();
				}
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Event: </b> " + Lang.text(Lang.LANG_88) +" "+getName()+": "+gagnants, Config.CONFIG_MOTD_COLOR);
			} else
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Event: </b> " + Lang.text(Lang.LANG_88) +" "+getName()+": "+getWinners().get(0).getName(), Config.CONFIG_MOTD_COLOR);
		}
		cancel(getPlayers());
	}
	
	public void cancel(List<Player> toRemove) {
		for (Player i: toRemove) {
			i.setEventPoints(0);
			i.setEvent(null);
			removePlayer(i);
		}
		reset();
		if (isAutomatic())
			launch();
		else delete();
	}
	
	public void reset() {
		getPlayers().clear();
		getWinners().clear();
		getDisconnectedPlayers().clear();
		setEventQuizz(null);
		setEventCache(null);
		setEventZaap(null);
		setEventSurvivor(null);
		setStatus(OFFLINE);
	}
	
	public void delete() {
		getEvents().remove(getId());
	}	
	
	public void sendRewards() {
		int winPoints;
		switch (getType()) {
			case 1:
				winPoints = 0;
				break;
			case 2:
				winPoints = 0;
				break;
			case 3:
				winPoints = 0;
				break;
			case 4:
				winPoints = 0;
				break;
				
			default:
				for (Player p: getWinners()) {
					p.sendMess(Lang.LANG_90);
				}
				return;
		}
		for (Player player: getWinners()) {
			int playerPoints = Util.loadPointsByAccount(player.getAccount());
			int afterWin = playerPoints + winPoints;
			
			Util.updatePointsByAccount(player.getAccount(), afterWin, "Event.java");
			
			player.sendMess(Lang.LANG_91, "", " "+winPoints+" "+Lang.POINTS[player.getLang()]);
			player.send("000C"+afterWin); //Update served interface
		}
	}

	public int timeToSchedule(int eventTime) {
		int time = eventTime;
		time = getActualHour() - time;
		if (time > 0)
			time = (24*60)-(time);
		else time *= -1;
		
		return time;
	}
	
	public static int getActualHour() {
		Date actDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
		dateFormat = new SimpleDateFormat("HH");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
		String heure = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("mm");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
		String min = dateFormat.format(actDate);
		
		return (Integer.parseInt(heure)*60) + (Integer.parseInt(min)); //D�calage Horraire de 3h
		
	}
	
	public void sendSubscription() {
		for (Player player: World.getOnlinePlayers())
			player.send("005A"+getId()+"|"+getName());
	}
	
	public String getName() {
		String toReturn;
		switch(getType())
		{
			case 1:
				toReturn = "Fatal Quizz";
				break;
			case 2:
				toReturn = "Cash Cassh";
				break;
			case 3:
				toReturn = "Happ Zaap";
				break;
			case 4:
				toReturn = "Survivant";
				break;
			default:
				toReturn = "Undefined";
		}
		return toReturn;
	}
	
	public class EventStarted {
		private Event event;

		public EventStarted(Event event) {
			setEvent(event);
			start();
		}
		
		public int guid(Maps map) {
			for (NPC npc: map.get_npcs().values())
				if (npc.get_template().get_id() == EventConstant.NPC_ID)
					return npc.get_guid();
			return -1;
		}
		
		public void start()
		{
			switch (getEvent().getType())
			{
				case 1: //Quizz
				{
					for (Player player: getPlayers())
						player.teleport(EventConstant.MAP_QUIZZ.get_id(), EventConstant.MAP_QUIZZ.getRandomFreeCellID());
					NPC npc = EventConstant.MAP_QUIZZ.addNpc(EventConstant.NPC_ID,
							EventConstant.CELL_QUIZZ, 1,0,"");
					SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.MAP_QUIZZ, npc);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", Lang.text(Lang.LANG_92));
					
					Timer timer = new Timer();
					timer.schedule(new TimerTask(){
					     public void run() {
					    	 setEventQuizz(new EventQuizz(getEvent()));
					     }
					}, EventConstant.TIME_TO_START_QUIZZ);
					break;
				}
				case 2: //Cache cache
				{
					for (Player player: getPlayers())
						player.teleport(EventConstant.getMap(1).get_id(), EventConstant.getMap(1).getRandomFreeCellID());
					NPC npc = EventConstant.getMap(1).addNpc(EventConstant.NPC_ID,
							EventConstant.getCell(1), 1, 0,"");
					SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.getMap(1), npc);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.getMap(1), "", guid(EventConstant.getMap(1)), "Event", Lang.text(Lang.LANG_93));
					
					Timer timer = new Timer();
					timer.schedule(new TimerTask(){
					     public void run() {
					    	 setEventCache(new EventCache(getEvent()));
					     }
					}, EventConstant.TIME_TO_START_CACHE);
					break;
				}
				case 3: //Zaap
				{
					for (Player player: getPlayers())
						player.teleport(EventConstant.MAP_ZAAP.get_id(), EventConstant.MAP_ZAAP.getRandomFreeCellID());
					NPC npc = EventConstant.MAP_ZAAP.addNpc(EventConstant.NPC_ID,
							EventConstant.CELL_ZAAP, 1, 0,"");
					SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.MAP_ZAAP, npc);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_ZAAP, "", guid(EventConstant.MAP_ZAAP), "Event", Lang.text(Lang.LANG_94));
					try {
						Thread.sleep(8000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_ZAAP, "", guid(EventConstant.MAP_ZAAP), "Event", Lang.text(Lang.LANG_95));
					
					Timer timer = new Timer();
					timer.schedule(new TimerTask(){
					     public void run() {
					    	 setEventZaap(new EventZaap(getEvent()));
					     }
					}, EventConstant.TIME_TO_START_ZAAP);
					break;
				}
				case 4: //Survivor
				{
					//1003
					for (Player player: getPlayers())
						player.teleport(EventConstant.MAP_SURVIVANT.get_id(), EventConstant.MAP_SURVIVANT.getRandomFreeCellID());
					NPC npc = EventConstant.MAP_SURVIVANT.addNpc(EventConstant.NPC_ID,
							EventConstant.CELL_SURVIVANT, 1, 0,"");
					SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.MAP_SURVIVANT, npc);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_SURVIVANT, "", guid(EventConstant.MAP_SURVIVANT), "Event", Lang.text(Lang.LANG_96));
					
					Timer timer = new Timer();
					timer.schedule(new TimerTask(){
					     public void run() {
					    	 setEventSurvivor(new EventSurvivor(getEvent()));
					     }
					}, EventConstant.TIME_TO_START_SURVIVANT);
					break;
				}
			}
		}
		
		public Event getEvent() {
			return event;
		}

		public void setEvent(Event event) {
			this.event = event;
		}
		
		
	}
	
	public String getRules() {
		switch (getType()) {
			case 1:
				return Lang.text(Lang.LANG_97);
			case 2:
				return Lang.text(Lang.LANG_98);
			case 3:
				return Lang.text(Lang.LANG_99);
			case 4:
				return Lang.text(Lang.LANG_100);
				default:
					return Lang.text(Lang.LANG_101);
		}
	}
	
	public static void refreshList(Player player) {
		StringBuilder tosend = new StringBuilder("005E");
		for (Event event: Event.getEvents().values()) {
			tosend.append(event.getId()).append("|");
			tosend.append(event.getName()).append("|");
			tosend.append(event.getRules()).append("|");
			
			int mins = event.getStartTime(), hours=0;
			String hour;
			while (mins > 59) {
				mins -=60;
				hours+=1;
			}
			if (mins < 10)
				hour = hours+"h0"+mins;
			else
				hour = hours+"h"+mins;
			
			tosend.append(hour).append("|");
			tosend.append(event.getPlayers().contains(player)?"ON":"OFF");
			tosend.append(";");
		}
		player.send(tosend.toString());
	}
	
	public int getMaxPlayers() {
		return maxPlayers;
	}
	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getMinPlayers() {
		return minPlayers;
	}
	public void setMinPlayers(int minPlayers) {
		this.minPlayers = minPlayers;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public List<Player> getPlayers() {
		List<Player> l = new ArrayList<Player>();
		l.addAll(players.values());
		return l;
	}
	public Map<Integer, Player> getMPlayers() {
		return players;
	}
	public void setPlayers(Map<Integer, Player> players) {
		this.players = players;
	}

	public static Map<Integer, Event> getEvents() {
		return events;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	
	public ArrayList<Player> getWinners() {
		return winners;
	}

	public void setWinners(ArrayList<Player> winners) {
		this.winners = winners;
	}

	public EventQuizz getEventQuizz() {
		return eventQuizz;
	}

	public void setEventQuizz(EventQuizz eventQuizz) {
		this.eventQuizz = eventQuizz;
	}

	public EventCache getEventCache() {
		return eventCache;
	}

	public void setEventCache(EventCache eventCache) {
		this.eventCache = eventCache;
	}

	public boolean isAutomatic() {
		return automatic;
	}

	public void setAutomatic(boolean automatic) {
		this.automatic = automatic;
	}

	public static Map<Integer, Event> getDisconnectedPlayers() {
		return disconnectedPlayers;
	}

	public static void setDisconnectedPlayers(Map<Integer, Event> disconnectedPlayers) {
		Event.disconnectedPlayers = disconnectedPlayers;
	}

	public EventZaap getEventZaap() {
		return eventZaap;
	}

	public void setEventZaap(EventZaap eventZaap) {
		this.eventZaap = eventZaap;
	}

	public EventSurvivor getEventSurvivor() {
		return eventSurvivor;
	}

	public void setEventSurvivor(EventSurvivor eventSurvivor) {
		this.eventSurvivor = eventSurvivor;
	}
	
	
}
