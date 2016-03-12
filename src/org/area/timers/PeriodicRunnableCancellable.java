package org.area.timers;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.area.game.GameServer;

public abstract class PeriodicRunnableCancellable implements Runnable { 
	private ScheduledFuture<?> future;
	
	public PeriodicRunnableCancellable(long time, TimeUnit timeUnit){
	    this.future = GameServer.fightExecutor.scheduleWithFixedDelay(this, time, time, timeUnit);
	}
	
	public void cancel() {
		if (!this.future.isCancelled())
			this.future.cancel(true);
	}
	
	public abstract void run();
}
