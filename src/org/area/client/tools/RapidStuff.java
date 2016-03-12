package org.area.client.tools;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.area.client.Player;
import org.area.common.SQLManager;
import org.area.common.World;
import org.area.object.Item;

import com.mysql.jdbc.PreparedStatement;

public class RapidStuff { /** Author: Return **/

	//Content
	private int id;
	private String name;
	private String itemGuids;
	private int owner;
	//AllRapidStuffs
	public static Map <Integer, RapidStuff> rapidStuffs = new HashMap <Integer, RapidStuff>();
	
	public RapidStuff (int _id, String _name, String _itemGuids, int _owner)
	{
		setId(_id);
		setName(_name);
		setItemGuids(_itemGuids);
		setOwner(_owner);
	}

	public String getItemGuids() {
		return itemGuids;
	}

	public static RapidStuff getRapidStuffByID(int id)
	{
		return (rapidStuffs.get(id));
	}
	
	public ArrayList<Item> getObjects() 
	{
		ArrayList<Item> toReturn = new ArrayList<Item>();
		for (String s: getItemGuids().split(","))
		{
			if (Integer.parseInt(s) != 0)
				toReturn.add(World.getObjet(Integer.parseInt(s)));
		}
		return toReturn;
	}
	
	public Item getObject(int place)
	{
		return World.getObjet(Integer.parseInt(getItemGuids().split(",")[(place-1)]));
	}
	
	public static boolean addRapidStuff(Player player, String name, String objects)
	{
		for (RapidStuff rs: player.getRapidStuffs())
		{
			if (rs.getName().equals(rs) || rs.getItemGuids().equals(objects))
				return false;
		}
		int nextID = getNextID();
		String Request = "INSERT INTO `rapidstuff` VALUES(?,?,?,?);";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection(false));
			PS.setInt(1, nextID);
			PS.setString(2, name);
			PS.setString(3, objects);
			PS.setInt(4, player.getGuid());
			PS.executeUpdate();
			
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
		rapidStuffs.put(nextID, new RapidStuff(nextID, name, objects, player.getGuid()));
		SQLManager.SAVE_PERSONNAGE(player, false);
		return true;
	}
	
	public static boolean removeRapidStuff(RapidStuff rs)
	{
		int id = rs.getId();
		String baseQuery = "DELETE FROM rapidstuff WHERE id = ?;";
		try {
			PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection(false));
			p.setInt(1, id);
			p.execute();
			SQLManager.closePreparedStatement(p);
		} catch (SQLException e) {
			return false; //Useless tout ça mais osef xD Flemme de changer
		}
		rapidStuffs.remove(id);
		return true;
	}
	
	public synchronized static int getNextID()
	{
		int max = 1;
		for(int a : rapidStuffs.keySet())if(a > max)max = a;
		return max+1;
	}
	
	public void setItemGuids(String itemGuids) {
		this.itemGuids = itemGuids;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getOwner() {
		return owner;
	}

	public void setOwner(int owner) {
		this.owner = owner;
	}
	
	public Player getCharacter() {
		return World.getPlayer(getOwner());
	}
}
