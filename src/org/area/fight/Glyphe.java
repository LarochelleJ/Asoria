package org.area.fight;

import org.area.common.Constant;
import org.area.common.SocketManager;
import org.area.object.Maps.Case;
import org.area.spell.Spell.SortStats;

public class Glyphe
{
	private Fighter _caster;
	private Case _cell;
	private byte _size;
	int _spell;
	private SortStats _trapSpell;
	private byte _duration;
	private Fight _fight;
	private int _color;
	
	public Glyphe(Fight fight, Fighter caster, Case cell, byte size, SortStats trapSpell, byte duration, int spell)
	{
		_fight = fight;
		_caster = caster;
		_cell =cell;
		_spell = spell;
		_size = size;
		_trapSpell = trapSpell;
		_duration = duration;
		_color = Constant.getGlyphColor(spell);
	}

	public Glyphe(Fight fight, Fighter caster, Case cell, byte size, SortStats trapSpell, byte duration, int spell, int color)
	{
		_fight = fight;
		_caster = caster;
		_cell =cell;
		_spell = spell;
		_size = size;
		_trapSpell = trapSpell;
		_duration = duration;
		_color = color;
	}

	public Case get_cell() {
		return _cell;
	}

	public byte get_size() {
		return _size;
	}

	public Fighter get_caster() {
		return _caster;
	}
	
	public byte get_duration() {
		return _duration;
	}

	public int decrementDuration()
	{
		_duration--;
		return _duration;
	}
	
	public void onTraped(Fighter target)
	{
		String str = _spell+","+_cell.getID()+",0,1,1,"+_caster.getGUID();
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 307, target.getGUID()+"", str);
		_trapSpell.applySpellEffectToFight(_fight,_caster,target.get_fightCell(),false);
		_fight.verifIfTeamAllDead();
	}

	public void desapear()
	{
		SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(_fight, 7, "-",_cell.getID(), _size, _color);
		SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(_fight, 7, _cell.getID());
	}
	
	public int get_color()
	{
		return _color;
	}
}