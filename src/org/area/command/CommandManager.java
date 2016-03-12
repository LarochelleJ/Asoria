package org.area.command;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class CommandManager { /** Author S **/

	private int id;
	private String name;
	private String description;
	private String function;
	private String cond;
	private int price;
	
	
	public static Map<Integer, CommandManager> command = new TreeMap<Integer, CommandManager>();
	public static Map<String, Integer> commandByName = new TreeMap<String, Integer>();
	public static String commandList = "";
	public static String vipCommandList = "";

	public CommandManager(int _id, String _name, String _description, String _function, String _cond, int _price)
	{
		setId(_id);
		setName(_name);
		setDescription(_description);
		setFunction(_function);
		setCond(_cond);
		setPrice(_price);
	}

	public static void addCommand(CommandManager x)
	{
		command.put(x.getId(), x);
		commandByName.put(x.getName().toLowerCase(), x.getId());
	}
	
	public static CommandManager getCommandById(int id)
	{
		return command.get(id);
	}
	
	public static Collection<CommandManager> getCommands()
	{
		return command.values();
	}
	
	public static String getCommandList(boolean isVip)
	{
		String toReturn = "";
		
		if (commandList == "" || vipCommandList == "")
		{
			for (CommandManager cm: getCommands())
			{
				if (cm.getFunction().equals("1") || cm.getFunction().equals("0"))
					continue;
				
				if (cm.getCond() != null && cm.getCond().contains("PZ"))
				{
					if (cm.getDescription() != null)
						vipCommandList += "\n<b>."+cm.getName()+"</b> - " + cm.getDescription();
					else
						vipCommandList += "\n<b>."+cm.getName()+"</b>";
					if (cm.getPrice() > 0)
						vipCommandList += " ["+cm.getPrice()+" pts]";
				}
				else
				{
					if (cm.getDescription() != null)
						commandList += "\n<b>."+cm.getName()+"</b> - " + cm.getDescription();
					else
						commandList += "\n<b>."+cm.getName()+"</b>";
					if (cm.getPrice() > 0)
						commandList += " ["+cm.getPrice()+" pts]";
				}
			}
			if (vipCommandList == "")
				vipCommandList = "Aucune commande V.I.P n'est disponible !";
			else if (commandList == "")
				commandList = "Aucune commande disponible !";
		}
		
		if (isVip)
			toReturn = "<b>\nCommandes V.I.P disponibles:</b>"+vipCommandList;
		else
			toReturn = "<b>\nCommandes disponibles:</b>"+commandList;
		
		return toReturn;
	}
	
	public static CommandManager getCommandByName(String name)
	{
		try {
			return command.get(commandByName.get(name.toLowerCase()));
		}
		catch(Exception e){
			for (CommandManager cm: getCommands())
			{
				if (name.split(" ")[0].equals(cm.getName().trim()))
					return cm;
			}
		}
		return null;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String funtion) {
		this.function = funtion;
	}

	public String getCond() {
		return cond;
	}

	public void setCond(String cond) {
		this.cond = cond;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	private void setDescription(String _description) {
		this.description = _description;		
	}
	
	public void setName(String name) {
		this.name = name;
	}
}