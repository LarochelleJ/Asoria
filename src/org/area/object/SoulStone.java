package org.area.object;
import java.util.ArrayList;

import org.area.common.Constant;
import org.area.common.SQLManager;
import org.area.common.World;
import org.area.common.World.Couple;


public class SoulStone extends Item{
	private ArrayList<Couple<Integer, Integer>> _monsters;
	
	public SoulStone (int Guid, int qua,int template, int pos, String strStats, boolean nouvelleCapture)
	{
		if (nouvelleCapture){
			if (World.getObjets().containsKey(Guid)){
				this.guid = SQLManager.getNextObjetID()+1;
			}
			else {
			this.guid = Guid;
			}
		}
		else{
		this.guid = Guid;
		}
		this.template = World.getObjTemplate(template);	//7010 = Pierre d'ame pleine
		this.quantity = qua;
		this.position = Constant.ITEM_POS_NO_EQUIPED;
		
		_monsters = new ArrayList<Couple<Integer, Integer>>();	//Couple<MonstreID,Level>
		parseStringToStats(strStats);
		if (nouvelleCapture){
		    // 3 tentatives de création
		    for (int i = 0; i <= 3; i++) {
		    	if (SQLManager.INSERT_NEW_ITEM(this)){
		    		break;
		    	}
		    	else{
		    		this.guid = SQLManager.getNextObjetID()+1;
		    	}
		    }
		    try{
		    	World.addObjet(this, false);
		    } catch (Exception e) {}
		}
	}
	
	public void parseStringToStats(String monsters) //Dans le format "monstreID,lvl|monstreID,lvl..."
	{
		String[] split = monsters.split("\\|");
		for(String s : split)
		{	
			try
			{
				int monstre = Integer.parseInt(s.split(",")[0]);
				int level = Integer.parseInt(s.split(",")[1]);
				
				_monsters.add(new Couple<Integer, Integer>(monstre, level));
				
			}catch(Exception e){continue;};
		}
	}
	
	public String parseStatsString()
	{
		StringBuilder stats = new StringBuilder();
		boolean isFirst = true;
		for(Couple<Integer, Integer> coupl : _monsters)
		{
			if(!isFirst)
				stats.append(",");
			
			try
			{
				stats.append("26f#0#0#").append(Integer.toHexString(coupl.first));
			}catch(Exception e)
			{
				e.printStackTrace();
				continue;
			};
			
			isFirst = false;
		}
		return stats.toString();
	}
	
	public String parseGroupData()//Format : id,lvlMin,lvlMax;id,lvlMin,lvlMax...
	{
		StringBuilder toReturn = new StringBuilder();
		boolean isFirst = true;
		for(Couple<Integer, Integer> curMob : _monsters)
		{
			if(!isFirst)
				toReturn.append(";");
			
			toReturn.append(curMob.first).append(",").append(curMob.second).append(",").append(curMob.second);
			
			isFirst = false;
		}
		return toReturn.toString();
	}
	
	public String parseToSave()
	{
		StringBuilder toReturn = new StringBuilder();
		boolean isFirst = true;
		for(Couple<Integer, Integer> curMob : _monsters)
		{
			if(!isFirst)
				toReturn.append("|");
			toReturn.append(curMob.first).append(",").append(curMob.second);
			isFirst = false;
		}
		return toReturn.toString();
	}
}
