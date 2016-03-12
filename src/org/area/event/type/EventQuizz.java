package org.area.event.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.area.client.Player;
import org.area.common.Formulas;
import org.area.common.SocketManager;
import org.area.event.Event;
import org.area.event.EventConstant;
import org.area.lang.Lang;
import org.area.object.Maps;
import org.area.object.NpcTemplate.NPC;


public class EventQuizz extends Thread {

	//Event
	private Event event;
	private Player winner;
	private boolean waitingAnwser;
	private EventQuizz couple;
	//Static
	private int id;
	private String question;
	private String reponse;
	private int points;
	private boolean ortho;
	
	public static Map<Integer, EventQuizz> couples = new HashMap<Integer, EventQuizz>();

	public EventQuizz(int id, String question, String reponse, int points, boolean ortho)
	{
		setId(id);
		setQuestion(question);
		setReponse(reponse);
		setPoints(points);
		setOrtho(ortho);
	}
	
	public EventQuizz(Event event) {
		setEvent(event);
		setDaemon(true);
		start();
	}
	
	public void run() {
		
		int questions = 0;
		ArrayList<Integer> alreadyAsked = new ArrayList<Integer>();
		while (questions < EventConstant.QUESTIONS_TO_ASK)
		{
			int random;
			do {
				random = Formulas.getRandomValue(1, getCouples().size());
			} while (alreadyAsked.contains(random) || !getCouples().containsKey(random));
			alreadyAsked.add(random);
			long time = System.currentTimeMillis();
			boolean nobody = false;
			setCouple(getCouples().get(random));
			
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", getCouple().getQuestion() + " ("+getCouple().getPoints()+" pts)");
			setWaitingAnwser(true);
			while (isWaitingAnwser())
			{
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (System.currentTimeMillis() - (time + 60000) > 0) {
					setWaitingAnwser(false);
					nobody = true;
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", Lang.text(Lang.LANG_69)+" "+getCouple().getReponse()+Lang.text(Lang.LANG_70));
				} 
				else if (System.currentTimeMillis() - (time + 40000) > 0 && System.currentTimeMillis() - (time + 40000) < 450)
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", Lang.text(Lang.LANG_71)+" "+getCouple().getQuestion());
				
				else if (System.currentTimeMillis() - (time + 20000) > 0 && System.currentTimeMillis() - (time + 20000) < 450)
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", Lang.text(Lang.LANG_72)+"  "+getCouple().getQuestion());
				
			}
			if (!nobody) {
				getWinner().setEventPoints(getWinner().getEventPoints() + getCouple().getPoints());
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", Lang.text(Lang.LANG_73)+" "+getWinner().getName()+" "+ Lang.text(Lang.LANG_74) +" ("+getCouple().getReponse()+"). "+Lang.text(Lang.LANG_75)+" <b>"+getWinner().getEventPoints()+" "+Lang.text(Lang.LANG_76));
				setWinner(null);
			}
			
			try {
				Thread.sleep(EventConstant.TIME_TO_WAIT_QUESTION);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			questions++;
		}
		
		Player player = null;
		getEvent().kickBusys(getEvent().getPlayers());
		for (Player p: getEvent().getPlayers()) {
			if (player == null)
				player = p;
			else if (p.getEventPoints() > player.getEventPoints())
					player = p;
		}
		SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(EventConstant.MAP_QUIZZ, "", guid(EventConstant.MAP_QUIZZ), "Event", Lang.text(Lang.LANG_77)+" "+player.getName());
		
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(EventConstant.MAP_QUIZZ, guid(EventConstant.MAP_QUIZZ));
		EventConstant.MAP_QUIZZ.removeNpcOrMobGroup(guid(EventConstant.MAP_QUIZZ));
		getEvent().getWinners().add(player);
		getEvent().launch();
		interrupt();
	}
	
	public synchronized void checkAnwser(Player player, String response)
	{
		if (!getEvent().getPlayers().contains(player))
			return;
		
		if (!isWaitingAnwser()) {
			player.sendMess(Lang.LANG_78);
			return;
		} else if ((getCouple().isOrtho() && response.equalsIgnoreCase(getCouple().getReponse()) || (!getCouple().isOrtho() && getCouple().getReponse().toLowerCase().contains(response.toLowerCase())))) {
			if (getWinner() != null) {
				player.sendMess(Lang.LANG_79);
				return;
			} else {
				setWinner(player);
				setWaitingAnwser(false);
				player.sendMess(Lang.LANG_80);
			}
		} else {
			player.sendMess(Lang.LANG_81);
			return;
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

	public Player getWinner() {
		return winner;
	}

	public void setWinner(Player winner) {
		this.winner = winner;
	}

	public boolean isWaitingAnwser() {
		return waitingAnwser;
	}

	public void setWaitingAnwser(boolean waitingAnwser) {
		this.waitingAnwser = waitingAnwser;
	}

	public boolean isOrtho() {
		return ortho;
	}

	public void setOrtho(boolean ortho) {
		this.ortho = ortho;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public String getReponse() {
		return reponse;
	}

	public void setReponse(String reponse) {
		this.reponse = reponse;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public static Map<Integer, EventQuizz> getCouples() {
		return couples;
	}

	public static void setCouples(Map<Integer, EventQuizz> couples) {
		EventQuizz.couples = couples;
	}

	public EventQuizz getCouple() {
		return couple;
	}

	public void setCouple(EventQuizz couple) {
		this.couple = couple;
	}
}
