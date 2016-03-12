package org.area.event.type;

import java.util.ArrayList;

import org.area.client.Player;
import org.area.common.Formulas;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.event.Event;
import org.area.event.EventConstant;
import org.area.lang.Lang;
import org.area.object.Maps;
import org.area.object.NpcTemplate.NPC;

public class EventCache extends Thread{

	private Event event;
	private boolean waiting;
	private String lastWinner;
	private int mapp;
	
	public EventCache(Event event)
	{
		setEvent(event);
		setDaemon(true);
		start();
	}
	
	public void run() {
		for (int i=1;i<4;i++) {
			if (i>1) {
				NPC npc = EventConstant.getMap(i).addNpc(EventConstant.NPC_ID,
						EventConstant.getCell(i), 1,0,"");
				SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.getMap(i), npc);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if (!getLastWinner().isEmpty())
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.getMap(i), "", npc.get_guid(), "Event", Lang.text(Lang.LANG_57)+" "+getLastWinner()+".");
				else
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.getMap(i), "", npc.get_guid(), "Event", Lang.text(Lang.LANG_58));
				
				try {
					Thread.sleep(7000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.getMap(i), "", guid(EventConstant.getMap(i)), "Event", Lang.text(Lang.LANG_59)+i+" ! "+Lang.text(Lang.LANG_60));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(EventConstant.getMap(i), guid(EventConstant.getMap(i)));
			EventConstant.getMap(i).removeNpcOrMobGroup(guid(EventConstant.getMap(i)));
			
			
			ArrayList<Maps> maps = new ArrayList<Maps>();
			for (String s: EventConstant.MAPS_CACHE.split(","))
				maps.add(World.getCarte((short)Integer.parseInt(s)));
			int randomValue = Formulas.getRandomValue(0, maps.size()-1);
			Maps map = maps.get(randomValue);
			for (Player player: World.getOnlinePlayers())
				if (player.getAccount().getGmLevel() > 0)
					player.sendText("PNJ Cache cache sur la map: "+map.get_id());
			
			NPC npc = map.addNpc(EventConstant.NPC_ID,
					map.getRandomFreeCellID(), 1, 0,"");
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(map, npc);
			setWaiting(true);
			setMapp(map.get_id());
			long time = System.currentTimeMillis();
			
			while (isWaiting())
			{
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (System.currentTimeMillis() - (time + 60000*15) > 0) {
					for (Player player: getEvent().getPlayers())
						player.sendMess(Lang.LANG_61);
					setWaiting(false);
					setLastWinner("");
				}
				else if (System.currentTimeMillis() - (time + 60000*10) > 0 && System.currentTimeMillis() - (time + 20000) < 450){
					for (Player player: getEvent().getPlayers()) {
						player.sendMess(Lang.LANG_62);
						SocketManager.GAME_SEND_FLAG_PACKET(player, map);
					}
				}
				else if (System.currentTimeMillis() - (time + 60000*5) > 0 && System.currentTimeMillis() - (time + 60000*5) < 450) {
					for (Player player: getEvent().getPlayers())
						player.sendMess(Lang.LANG_63);
				}
			}
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(map, npc.get_guid());
			map.removeNpcOrMobGroup(npc.get_guid());
			for (Player player: getEvent().getPlayers()) {
				if (player != null && player.isOnline() && player.getFight() == null) {
					if (i < 3)
						player.sendBox("Event", Lang.text(Lang.LANG_64));
					else
						player.sendBox("Event", Lang.text(Lang.LANG_65));
				}
				else {
					player.sendMess(Lang.LANG_37);
					getEvent().removePlayer(player);
				}
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (Player player: getEvent().getPlayers()) {
				if (player != null && player.isOnline() && player.getFight() == null)
					player.teleport(EventConstant.getMap(i+1).get_id(), EventConstant.getMap(i+1).getRandomFreeCellID());
				else {
					player.sendMess(Lang.LANG_37);
					getEvent().removePlayer(player);
				}
			}
		}
		NPC npc = EventConstant.getMap(1).addNpc(EventConstant.NPC_ID,
				EventConstant.getMap(1).getRandomFreeCellID(), 1, 0,"");
		SocketManager.GAME_SEND_ADD_NPC_TO_MAP(EventConstant.getMap(1), npc);
		setMapp(0);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (getEvent().getWinners().size() > 0) {
			String gagnants = "";
			for (Player p: getEvent().getWinners()) {
				if (gagnants.isEmpty())
					gagnants = p.getName();
				else gagnants += "," + p.getName();
			}
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.getMap(1), "", guid(EventConstant.getMap(1)), "Event", Lang.text(Lang.LANG_66)+" "+gagnants+". "+Lang.text(Lang.LANG_67));
			
		} else {
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.getMap(1), "", guid(EventConstant.getMap(1)), "Event", Lang.text(Lang.LANG_68));
			
		}
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(EventConstant.getMap(1), npc.get_guid());
		EventConstant.getMap(1).removeNpcOrMobGroup(npc.get_guid());
		getEvent().launch();
		interrupt();
	}
	
	public synchronized void npcFinded(Player player) {
		setLastWinner(player.getName());
		setWaiting(false);
		if (!getEvent().getWinners().contains(player))
			getEvent().getWinners().add(player);
	}

	public Event getEvent() {
		return event;
	}
	
	public int guid(Maps map)
	{
		for (NPC npc: map.get_npcs().values())
			if (npc.get_template().get_id() == EventConstant.NPC_ID)
				return npc.get_guid();
		return -1;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}

	public String getLastWinner() {
		return lastWinner;
	}

	public void setLastWinner(String lastWinner) {
		this.lastWinner = lastWinner;
	}

	public int getMapp() {
		return mapp;
	}

	public void setMapp(int mapp) {
		this.mapp = mapp;
	}
}
