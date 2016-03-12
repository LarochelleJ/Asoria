package org.area.fight.object;

import org.area.fight.Fighter;
import org.area.spell.Spell.SortStats;

public class LaunchedSort
{
	private int _spellId = 0;
	private int _cooldown = 0;
	private Fighter _target = null;
	
	public LaunchedSort(Fighter t,SortStats SS)
	{
		_target = t;
		_spellId = SS.getSpellID();
		_cooldown = SS.getCoolDown(t);
	}
	
	public void ActuCooldown()
	{
		_cooldown--;
	}

	public int getCooldown()
	{
		return _cooldown;
	}
	public void setCooldown(int c)
	{
		_cooldown = c;
	}
	
	public int getId()
	{
		return _spellId;
	}
	
	public Fighter getTarget()
	{
		return _target;
	}
	
	public static boolean coolDownGood(Fighter fighter,int id)
	{
		
		for(LaunchedSort S : fighter.getLaunchedSorts())
		{
			if(S._spellId == id && S.getCooldown() > 0)
				return false;
		}
		return true;
	}
	
	public static int getNbLaunch(Fighter fighter,int id)
	{
		int nb = 0;
		for(LaunchedSort S : fighter.getLaunchedSorts())
		{
			if(S._spellId == id)
				nb++;
		}
		return nb;
	}
	
	public static int getNbLaunchTarget(Fighter fighter,Fighter target,int id)
	{
		int nb = 0;
		for(LaunchedSort S : fighter.getLaunchedSorts())
		{
			if(S._target == null || target == null)
				continue;
			if(S._spellId == id && S._target.getGUID() == target.getGUID())
				nb++;
		}
		return nb;
	}
	
}