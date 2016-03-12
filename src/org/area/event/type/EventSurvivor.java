package org.area.event.type;

import org.area.common.SocketManager;
import org.area.event.Event;
import org.area.event.EventConstant;
import org.area.fight.object.Monster.MobGroup;
import org.area.lang.Lang;
import org.area.object.Maps;
import org.area.object.NpcTemplate.NPC;

public class EventSurvivor extends Thread{

	private Event event;

	public EventSurvivor(Event event) {
		setEvent(event);
		setDaemon(true);
		start();
	}
	
	public void run() {
		SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_SURVIVANT, "", guid(EventConstant.MAP_SURVIVANT), "Event", Lang.text(Lang.LANG_82));
		pause(4000);
		MobGroup group  = new MobGroup(EventConstant.MAP_SURVIVANT.get_nextObjectID(),EventConstant.CELL_SURVIVANT,"236,1,1");
		EventConstant.MAP_SURVIVANT.startEventFight(group, getEvent());
		SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_SURVIVANT, "", guid(EventConstant.MAP_SURVIVANT), "Event", Lang.text(Lang.LANG_83));
		pause(3000);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(EventConstant.MAP_SURVIVANT, guid(EventConstant.MAP_SURVIVANT));
		EventConstant.MAP_SURVIVANT.removeNpcOrMobGroup(guid(EventConstant.MAP_SURVIVANT));
		interrupt();
	}
	
	public void pause(int time) {
		try {
			Thread.sleep(time);
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
	
	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}
	
}
