package org.area.arena;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.area.client.Player;
import org.area.common.SQLManager;
import org.area.common.World;
import org.area.game.GameServer;
import org.area.lang.Lang;

import com.mysql.jdbc.PreparedStatement;

public class Team { /** Author Return **/

	//Arena 2v2
	private int id;
	private String name;
	private String characters; //Guid,Guid	
	private int cote;
	private int rank;
	//firstKolizeum
	private ArrayList<Player> kCharacters= new ArrayList<Player>(); 
	private int kLevel;
	
	public static Map<Integer, Team> Teams = new HashMap<Integer, Team>();
	public static ArrayList<Team> koliTeams = new ArrayList<Team>(); //Teams temporaires
	
	public Team (int id, String name, String characters, int quote, int rank)
	{
		setId(id);
		setName(name);
		setCharacters(characters);
		setCote(quote);
		setRank(rank);
	}
	
	public Team (Player player, int kLevel)
	{
		kCharacters.add(player);
		setkLevel(kLevel);
	}
	
	public Team (ArrayList<Player> player, int kLevel)
	{
		setkCharacters(player);
		setkLevel(kLevel);
	}
	
	public static void addTeamToMap(Team x)
	{
		Teams.put(x.getId(), x);
	}
	
	public static void removeTeam(Team team, Player p)
	{
		for(String c: team.getCharacters().split(","))
		{
			Player player = World.getPlayer(Integer.parseInt(c));
			player.setTeamID(-1);
			if(player.isOnline())
			{
				if (player != p)
					player.sendMess(Lang.LANG_106, Lang.LANG_105[player.getLang()]+" "+team.getName()+ " ", " "+p.getName()+" !");	
				else
					player.sendMess(Lang.LANG_107, "", " "+team.getName()+" !");
			}
			SQLManager.SAVE_PERSONNAGE(player, false);
		}
		deleteTeam(team.getId());
	}
	
	
	public static Team getTeamByCharacter(Player c)
	{
		for(Entry<Integer, Team> team: Teams.entrySet())
		{
			for(String guid: team.getValue().getCharacters().split(","))
			{
				if (Integer.parseInt(guid) == c.getGuid())
					return Teams.get(team.getKey());
			}
		}
		return null;
	}
	
	public static Player getPlayer(Team team, int number)
	{
		Player player = null;
		if (number == 1)
			player = World.getPlayer(Integer.parseInt(team.getCharacters().split(",")[0]));
		else if (number == 2)
			player = World.getPlayer(Integer.parseInt(team.getCharacters().split(",")[1]));
		return player;
	}
	
	public static ArrayList<Player> getPlayers(Team team)
	{
		ArrayList<Player> players = new ArrayList<Player>();
		if(team.getCharacters().length() <= 0)return players;
		for (String player: team.getCharacters().split(","))
		{
			players.add(World.getPlayer(Integer.parseInt(player)));
		}
		return players;
	}
	
	public static Team getTeamByID(int id)
	{
		if (Teams.containsKey(id))
			return Teams.get(id);
		return null;
	}
	
	 public static void loadTeams()
	  {
	      try
	      {
	          String query = "SELECT * from arena;";
				ResultSet RS = SQLManager.executeQuery(query, false);
			      while (RS.next()) 
			      {
			    	  Team x = new Team(
			    			  RS.getInt("id"),
			    			  RS.getString("name"),
			    			  RS.getString("players"),
			    			  RS.getInt("quote"),
			    			  RS.getInt("Rank")
			    			  );
			    	  	addTeamToMap(x);
			      }
	          SQLManager.closeResultSet(RS);
	      }
	      catch(SQLException e)
	      {
	          GameServer.addToLog("SQL ERROR: " + e);
	          e.printStackTrace();
	      }
	  }
	 
	public static void deleteTeam(int teamID)
	{
		String baseQuery = "DELETE FROM arena WHERE id = ?;";
		try {
			PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection(false));
			p.setInt(1, teamID);
			p.execute();
			SQLManager.closePreparedStatement(p);
		} catch (SQLException e) {
		}
		Teams.remove(teamID);
	}
	
	public synchronized static boolean addTeam(String name, String players, int quote, int rank)
	{
		
		int nextID = 0;
		if (Teams.size() < 1)
			nextID = 1;
		if (World.getPlayer(Integer.parseInt(players.split(",")[0])).getTeamID() != -1 || World.getPlayer(Integer.parseInt(players.split(",")[1])).getTeamID() != -1)
		{
			for (String player: players.split(","))
			{
				World.getPlayer(Integer.parseInt(player)).sendMess(Lang.LANG_102, World.getPlayer(Integer.parseInt(player)).getName()+ " ", "");
				return false;
			}
		}
		for (Entry<Integer, Team> team: Team.getTeams().entrySet()){
			
			if (team.getValue().getId() >= nextID)
				nextID = team.getValue().getId()+1;
			
			if (team.getValue().getName().equals(name))
			{
				for (String player: players.split(","))
				{
					if (World.getPlayer(Integer.parseInt(player)).isOnline())
						World.getPlayer(Integer.parseInt(player)).sendMess(Lang.LANG_104, Lang.LANG_103[World.getPlayer(Integer.parseInt(player)).getLang()]+" <b>'"+name+"</b> ", "");
				}
				return false;
			}
		}
		int newRank = Teams.size()+1;
		String Request = "INSERT INTO `arena` VALUES(?,?,?,?,?);";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection(false));
			PS.setInt(1, nextID);
			PS.setString(2, name);
			PS.setString(3, players);
			PS.setInt(4, quote);
			PS.setInt(5, newRank);
			PS.executeUpdate();
			
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		Teams.put(nextID, new Team(nextID, name, players, quote, newRank));
		for (String player: players.split(","))
		{
			World.getPlayer(Integer.parseInt(player)).setTeamID(nextID);
			SQLManager.SAVE_PERSONNAGE(World.getPlayer(Integer.parseInt(player)), false);
		}
		return true;
	}
	
	public static void calculAllRank()
	{
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM arena ORDER BY quote DESC LIMIT 0, 1000", false);
			int rank = 0;
			while(RS.next()){
				rank++;
				Team.getTeamByID(RS.getInt("id")).setRank(rank);
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
	}
	
	public static void updateTeam(int teamID)
	{
		
		Team team = getTeamByID(teamID);
		try {
			String baseQuery = "UPDATE `arena` SET `quote` = ? WHERE id = ?;";
			PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection(false));
			p.setInt(1, team.getCote());
			p.setInt(2, team.getId());
			p.execute();
			SQLManager.closePreparedStatement(p);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		updateAllRank();
	}
	
	public static void updateAllRank()
	{
		calculAllRank();
		for (Entry<Integer, Team> team: Team.Teams.entrySet())
		{
			try {
				String baseQuery = "UPDATE `arena` SET `rank` = ? WHERE id = ?;";
				PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection(false));
				p.setInt(1, team.getValue().getRank());
				p.setInt(2, team.getValue().getId());
				p.execute();
				SQLManager.closePreparedStatement(p);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void refreshInfos(Player players) {
		if (players.getTeamID() > 0) {
			Team team = Team.getTeamByID(players.getTeamID());
			for (Player player: Team.getPlayers(team)) {
				Player coep = Team.getPlayer(Team.getTeamByID(player.getTeamID()), 1);
				if (Team.getPlayer(Team.getTeamByID(player.getTeamID()), 1) == player)
					coep = Team.getPlayer(Team.getTeamByID(player.getTeamID()), 2);
				String status = coep.isOnline()?"En ligne":"Déconnecté";
				player.send("003I" + coep.getName() + " ("+status+")" + "|"+ player.getWinArena() + "|" + player.getLoseArena()
						+ "|" + Team.getTeamByID(player.getTeamID()).getCote()
						+ "|" + "TOP 100"+ "|" + Team.getTeamByID(player.getTeamID()).getName());
			}
		} else
			players.send("003I-");
	}
	
	public static void refreshState(Team team) {
		for (Player player: Team.getPlayers(team))
			player.send("003S"+player.getArena());
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCharacters() {
		return characters;
	}

	public void setCharacters(String characters) {
		this.characters = characters;
	}

	public int getCote() {
		return cote;
	}

	public void setCote(int cote) {
		this.cote = cote;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public static Map<Integer, Team> getTeams() {
		return Teams;
	}

	public static void setTeams(Map<Integer, Team> teams) {
		Teams = teams;
	}

	public ArrayList<Player> getkCharacters() {
		return kCharacters;
	}

	public void setkCharacters(ArrayList<Player> kCharacters) {
		this.kCharacters = kCharacters;
	}

	public int getkLevel() {
		return kLevel;
	}

	public void setkLevel(int kLevel) {
		this.kLevel = kLevel;
	}

}
