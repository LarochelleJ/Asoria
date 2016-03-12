package org.area.timers;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.area.fight.Fight;

// @Flow - Ce fichier ne sert plus selon moi, mais je vais garder...
public class TimerController {
	
	private static List<TimerController> controllerTimers = new CopyOnWriteArrayList<TimerController>();
	//private static final int FIGHTS_PER_THREAD = 1;
	
	private Timer timer;
	private Map<Fight, TimerTask> fights;
	
	public TimerController() {
		this.timer = new Timer("TimerController");
		this.fights = new ConcurrentHashMap<Fight, TimerTask>();
	}
	
	/*public static void dispatchController(Fight fight) {
		TimerController timerController = search();
		fight.setTimerController(timerController);
		controllerTimers.add(timerController);
	}*/
	
	public void scheduleNewTask(Fight fight, TimerTask task, int time) {
		if (this.timer == null)
			this.timer = new Timer();
		this.fights.put(fight, task);
		this.timer.schedule(task, time);
	}
	
	/**public void cancelTask(Fight fight) {
		if (this.fights.containsKey(fight)) 
			this.fights.get(fight).cancel();
	}**/
	
	/*public void onFightEnds(Fight fight) {
		if (this.fights.containsKey(fight))
			this.fights.remove(fight).cancel();
		if (this.fights.isEmpty()) {
			if (this.timer != null)
				this.timer.cancel();
			this.timer = null;
			controllerTimers.remove(this);
		}
	}*/
	
	/**private static TimerController search() {
		for (TimerController timerController: controllerTimers)
			if (timerController.getFights().size() < FIGHTS_PER_THREAD)
				return timerController;
		return new TimerController();
	}**/
	
	public void setTimer(Timer timer) {
		this.timer = timer;
	}

	public Timer getTimer() {
		return timer;
	}

	public static List<TimerController> getControllerTimers() {
		return controllerTimers;
	}

	public static void setControllerTimers(List<TimerController> controllerTimers) {
		TimerController.controllerTimers = controllerTimers;
	}

	public Map<Fight, TimerTask> getFights() {
		return fights;
	}

	public void setFights(Map<Fight, TimerTask> fights) {
		this.fights = fights;
	}
}
