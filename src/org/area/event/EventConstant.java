package org.area.event;

import org.area.common.World;
import org.area.object.Maps;

public class EventConstant {

	//General
	public final static int TIME_TO_STARTED = 60000; //1 mminute
	public final static int NPC_ID = 30012; //Pnj event
	//Event Quizz
	public final static int TIME_TO_START_QUIZZ = 30000; //30 secondes
	public final static int TIME_TO_WAIT_QUESTION = 10000; //10 secondes
	public final static int QUESTIONS_TO_ASK = 20;
	public final static Maps MAP_QUIZZ = World.getCarte((short)20130);
	public final static int CELL_QUIZZ = 208;
	//Event Zaap
	public final static int TIME_TO_START_ZAAP = 30000; //30 secondes
	public final static int ZAAP_COUNT = 10;
	public final static Maps MAP_ZAAP = World.getCarte((short)20130);
	public final static int CELL_ZAAP = 208;
	//Event Survivant
	public final static int TIME_TO_START_SURVIVANT = 30000; //30 secondes
	public final static Maps MAP_SURVIVANT = World.getCarte((short)20153);
	public final static int CELL_SURVIVANT = 458;
	public final static int CELL_FIGHT = 428;
	//Event cache cache
	public final static int TIME_TO_START_CACHE = 30000; //30 secondes
	public final static String MAPS_CACHE = "20147,20150,20149,20148,20139,20142,20141,20140,20185,20186,20163,20164,20165,20166,20143,20144,20145,20146,20171,20172,20173,20178,20179";
	public final static Maps getMap(int i) {
		switch(i)
		{
			case 1:
				return World.getCarte((short)20130);
			case 2:
				return World.getCarte((short)20130);
			case 3:
				return World.getCarte((short)20130);
			default:
				return World.getCarte((short)20130);
			
		}
	}
	public final static int getCell(int i) {
		switch(i)
		{
			case 1:
				return 208;
			case 2:
				return 208;
			case 3:
				return 208;
			
			default:
				return 208;
		}
	}
	
}
