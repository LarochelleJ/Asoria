package org.area.event.type;

import java.util.Map.Entry;

import org.area.client.Player;
import org.area.common.Constant;
import org.area.common.Formulas;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.event.Event;
import org.area.event.EventConstant;
import org.area.lang.Lang;
import org.area.object.Maps;
import org.area.object.NpcTemplate.NPC;

public class EventZaap extends Thread{

	private Event event;
	private Maps map;
	private boolean waiting;
	private Player lastWinner;
	
	public EventZaap(Event event) {
		setEvent(event);
		setDaemon(true);
		start();
	}
	
	public void run()
	{
		int count = 0;
		
		while (count < EventConstant.ZAAP_COUNT)
		{
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_ZAAP, "", guid(EventConstant.MAP_ZAAP), "Event", Lang.text(Lang.LANG_59)+" "+count+" ! "+Lang.text(Lang.LANG_84));
			pause(5000);
			count++;
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(EventConstant.MAP_ZAAP, guid(EventConstant.MAP_ZAAP));
			EventConstant.MAP_ZAAP.removeNpcOrMobGroup(guid(EventConstant.MAP_ZAAP));
		
			
			int randomValue = Formulas.getRandomValue(0,Constant.ZAAPS.size()), i = 0;
			Maps zaap = null;
			
			for (Entry<Integer, Integer> entry: Constant.ZAAPS.entrySet()) {
				if (i == randomValue) {
					zaap = World.getCarte((short)(int)entry.getKey());
				} i++;
			}
			
			NPC npc = zaap.addNpc(EventConstant.NPC_ID,
					zaap.getRandomFreeCellID(), 1,0,"");
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(zaap, npc);
			setWaiting(true);
			setMap(zaap);
			
			for (Player player: getEvent().getPlayers())
				SocketManager.GAME_SEND_FLAG_PACKET(player, zaap);
			long time = System.currentTimeMillis();
				
			while (isWaiting()) {
				pause(300);
				if (System.currentTimeMillis() - (time + 60000) > 0)
					setWaiting(false);
			}
			
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(zaap, npc.get_guid());
			zaap.removeNpcOrMobGroup(npc.get_guid());
			
			for (Player player: getEvent().getPlayers()) {
				if (player != null && player.isOnline() && player.getFight() == null) {
					if (count != EventConstant.ZAAP_COUNT)
						player.sendBox("Event", Lang.text(Lang.LANG_64));
					else
						player.sendBox("Event", Lang.text(Lang.LANG_65));
				}
				else {
					player.sendMess(Lang.LANG_37);
					getEvent().removePlayer(player);
				}
			}
			pause(5000);
			for (Player player: getEvent().getPlayers()) {
				if (player != null && player.isOnline() && player.getFight() == null)
					player.teleport(EventConstant.MAP_ZAAP.get_id(), EventConstant.MAP_ZAAP.getRandomFreeCellID());
				else {
					player.sendMess(Lang.LANG_37);
					getEvent().removePlayer(player);
				}
			}
			
			NPC npcs = EventConstant.MAP_ZAAP.addNpc(EventConstant.NPC_ID,
					EventConstant.CELL_ZAAP, 1, 0,"");
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.MAP_ZAAP, npcs);
			pause(2000);
			
			if (getLastWinner() != null)
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_ZAAP, "", npcs.get_guid(), "Event", Lang.text(Lang.LANG_57)+" "+getLastWinner().getName()+" ! "+Lang.text(Lang.LANG_75)+" "+getLastWinner().getEventPoints()+" "+ Lang.text(Lang.POINTS));
			else
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_ZAAP, "", npcs.get_guid(), "Event", Lang.text(Lang.LANG_58));
			pause(7000);
			setLastWinner(null);
		}
		
		Player player = null;
		getEvent().kickBusys(getEvent().getPlayers());
		for (Player p: getEvent().getPlayers()) {
			if (player == null)
				player = p;
			else if (p.getEventPoints() > player.getEventPoints())
					player = p;
		}
		SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_ZAAP, "", guid(EventConstant.MAP_ZAAP), "Event", Lang.text(Lang.LANG_66)+" "+player.getName()+" !");
		
		pause(3000);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(EventConstant.MAP_ZAAP, guid(EventConstant.MAP_ZAAP));
		EventConstant.MAP_ZAAP.removeNpcOrMobGroup(guid(EventConstant.MAP_ZAAP));
		getEvent().getWinners().add(player);
		getEvent().launch();
		interrupt();
	}
	
	public synchronized void npcFinded(Player player) {
		setWaiting(false);
		setLastWinner(player);
		player.setEventPoints(player.getEventPoints()+1);
	}
	
	public void pause(int value) {
		try {
			Thread.sleep(value);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public int guid(Maps map)
	{
		for (NPC npc: map.get_npcs().values())
			if (npc.get_template().get_id() == EventConstant.NPC_ID)
				return npc.get_guid();
		return -1;
	}
	
	public boolean isWaiting() {
		return waiting;
	}
	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}
	public Maps getMap() {
		return map;
	}
	public void setMap(Maps map) {
		this.map = map;
	}
	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public void setLastWinner(Player player) {
		this.lastWinner = player;
	}
	public Player getLastWinner() {
		return this.lastWinner;
	}
}
