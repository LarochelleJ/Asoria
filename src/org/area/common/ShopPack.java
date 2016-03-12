package org.area.common;

import org.area.client.Account;
import org.area.client.Player;
import org.area.game.GameServer;
import org.area.kernel.Config;
import org.area.kernel.Logs;
import org.area.object.Item;
import org.area.object.Item.ObjTemplate;


public class ShopPack {
	
	
	public static boolean executePack(Player p, int id, int pack_id, String params)
	{
		if (p.getFight() != null) return false;
		Logs.addToShopLog(new StringBuilder("Execution du pack ").append(id).append(" pour le personnage ").append(p.getGuid()).toString());
		switch(pack_id)
		{
			case 1://Pack VIP
				return pack1(p, params);
			case 2://Pack Runes
				return pack2(p, params);
			case 3://Pack Ultime
				return packUltime(p, params, true, true, true, true);
			case 4://Pack Exotique (+ exotique mini)
				return pack4(p, params);
			case 6://Pack Identité
				return pack6(p, params);
			case 7://Pack Débutant
				return packUltime(p, params, false, false, false, true);
			case 8://Pack Classique
				return packUltime(p, params, false, true, true, false);
			case 9://Pack Argent
				return packUltime(p, params, true, true, true, false);
			case 10://Pack Exo
				return pack10(p, params);
			case 11://Pack Sorts
				return pack11(p, params);
			case 12://Pack Mercenaire
				return pack12(p, params);
			case 13: //Ajout d'un item
				return pack13(p, params);
			case 14: //Ajout de Points de sort
				return pack14(p, params);
		}
		return false;
	}
	private static boolean pack1(Player p, String params)
	{
		Account c = p.getAccount();
		if(c == null) return false;
		if(c.getVip() == 0)
		{
			c.setVip(1);
			SQLManager.UPDATE_ACCOUNT_VIP(c);
		}
		//Titre
		p.set_title(1);
		//Tag
		String cur_name = p.getName();
		//On vérifie si y'a un tag
		if(cur_name.substring(0,1).equalsIgnoreCase("["))
		{
			if(cur_name.split("]", 2).length == 2){
				cur_name = cur_name.split("]", 2)[1];
			}
		}
		if(!params.isEmpty()) cur_name = new StringBuilder("[").append(params).append("]").append(cur_name).toString();
		p.set_name(cur_name);
		SQLManager.SAVE_PERSONNAGE(p, false);
		GameServer.addToSockLog("Tag "+params+" ajoute a "+p.getName());
		return true;
	}
	private static boolean pack2(Player p, String params)
	{
		ObjTemplate t = World.getObjTemplate(params.equalsIgnoreCase("PA")?1557:1558);
		if(t == null) return false;
		int count = params.equalsIgnoreCase("PA")?150:200;
		Item obj = t.createNewItem(count,true,-1); //Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
		if(obj == null)return false;
		if(p.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
		World.addObjet(obj,true);
		GameServer.addToSockLog("Objet Rune "+params+" ajoute a "+p.getName());
		SQLManager.SAVE_PERSONNAGE(p, true);
		//Si en ligne (normalement oui)
		if(p.isOnline())//on envoie le packet qui indique l'ajout//retrait d'un item
		{
			SocketManager.GAME_SEND_Ow_PACKET(p);
			SocketManager.GAME_SEND_Im_PACKET(p, "021;"+count+"~"+t.getID());
		}
		return true;
	}
	private static boolean packUltime(Player p, String params, boolean up199, boolean grade10, boolean obvi, boolean itemsp)
	{
		if(!p.isOnline()) return false;
		//Passage niveau 199
		if(p.getLevel() < 199 && up199)
		{
			while(p.getLevel() < 199)
			{
				p.levelUp(false,true);
			}
			SocketManager.GAME_SEND_SPELL_LIST(p);
			SocketManager.GAME_SEND_NEW_LVL_PACKET(p.getAccount().getGameThread().getOut(),p.getLevel());
			SocketManager.GAME_SEND_STATS_PACKET(p);
		}
		//Passage Grade 10 (18000)
		if(p.get_honor() < 18000 && grade10)
		{
			p.addHonor(18000-p.get_honor());
		}
		//Ajout des items
		if(!itemsp) params = "";
		if(obvi) params = new StringBuilder("9233,9234,").append(params).toString();
		if(!params.isEmpty())
		{
			String[] items = params.split(",");
			for(String item : items)
			{
				if(item.isEmpty() || Integer.parseInt(item) == 0) continue;
				ObjTemplate t = World.getObjTemplate(Integer.parseInt(item));
				if(t == null)
				{
					Logs.addToShopLog(new StringBuilder("L'item ").append(item).append(" n'existe pas (").append(p.getName()).append(")").toString());
				}
				Item obj = t.createNewItem(1,true,-1);
				if(p.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
					World.addObjet(obj,true);
				SocketManager.GAME_SEND_Im_PACKET(p, "021;1~"+t.getID());
			}
			SocketManager.GAME_SEND_Ow_PACKET(p);
		}
		StringBuilder sb = new StringBuilder("Pack (");
		if(itemsp) sb.append("I");
		if(obvi) sb.append("O");
		if(up199) sb.append("U");
		if(grade10) sb.append("G");
		Logs.addToShopLog(sb.append(") executé avec succes pour le personnage ").append(p.getName()).append(".").toString());
		SQLManager.SAVE_PERSONNAGE(p, true);
		return true;
	}
	private static boolean pack4(Player p, String params)
	{
		if(!p.isOnline()) return false;
		String[] items = params.split(",");
		for(String item : items)
		{
			if(item.isEmpty() || item.split("_").length != 2) continue;
			ObjTemplate t = World.getObjTemplate(Integer.parseInt(item.split("_")[0]));
			if(t == null)
			{
				Logs.addToShopLog(new StringBuilder("L'item ").append(item).append(" n'existe pas (").append(p.getName()).append(")").toString());
			}
			int spe = Integer.parseInt(item.split("_")[1]);
			if(spe != 1 && spe != 2)
			{
				Logs.addToShopLog(new StringBuilder("L'item ").append(item).append(" a une spé invalide (").append(p.getName()).append(")").toString());
				continue;
			}
			Item obj = t.createNewItem(1,true,spe);
			if(p.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
				World.addObjet(obj,true);
			SocketManager.GAME_SEND_Im_PACKET(p, "021;1~"+t.getID());
		}
		SocketManager.GAME_SEND_Ow_PACKET(p);
		Logs.addToShopLog(new StringBuilder("Pack Exotique executé avec succes pour le personnage ").append(p.getName()).append(".").toString());
		SQLManager.SAVE_PERSONNAGE(p, true);
		return true;
	}
	private static boolean pack6(Player p, String params)
	{
		if(!p.isOnline() || p.getAccount() == null)return false;
		String[] infos = params.split(",");
		if(infos.length != 5) return false;
		//New_name, new_pseudo, c1, c2, c3
		Logs.addToShopLog(new StringBuilder("Le personnage ").append(p.getName()).append(" s'apelle désormais ").append(infos[0].trim()).toString());
		p.set_name(infos[0].trim());
		p.set_colors(Integer.parseInt(infos[2]), Integer.parseInt(infos[3]), Integer.parseInt(infos[4]));
		SQLManager.SAVE_PERSONNAGE_COLORS(p);
		Account c = p.getAccount();
		Logs.addToShopLog(new StringBuilder("Le compte de pseudo ").append(c.getPseudo()).append(" a désormais poru pseudo ").append(infos[1].trim()).toString());
		c.setPseudo(infos[1].toString());
		SQLManager.UPDATE_ACCOUNT_DATA(c);
		
		return true;
	}
	private static boolean pack10(Player p, String params)
	{
		if(!p.isOnline())
		{
			return false;
		}
		if(params.split(",").length != 3)return false;
		String[] infos = params.split(",");
		int statsID = Integer.parseInt(infos[1]);
		int morphID = Integer.parseInt(infos[0]);
		int exo = Integer.parseInt(infos[2]);
		ObjTemplate tstats = World.getObjTemplate(statsID);
		if(tstats == null) return false;
		ObjTemplate tmorph = World.getObjTemplate(morphID);
		if(tmorph == null) return false;
		int qua =1;
		//tmorph.getStrTemplate()
		if(exo != 1 && exo != 2) exo = -1;
		
		Item obj = new Item(World.getNewItemGuid(), tmorph.getID(), qua, Constant.ITEM_POS_NO_EQUIPED, tstats.generateNewStatsFromTemplate(tstats.getStrTemplate(), true, exo), tstats.getEffectTemplate(tstats.getStrTemplate()), tstats.getBoostSpellStats(tstats.getStrTemplate()), tmorph.getPrestige());
		//tmorph.createNewItem(qua,useMax,-1);
		if(p.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
			World.addObjet(obj,true);
		SocketManager.GAME_SEND_Im_PACKET(p, "021;1~"+tmorph.getID());
		SocketManager.GAME_SEND_Ow_PACKET(p);
		SQLManager.SAVE_PERSONNAGE(p, true);
		return true;
	}
	private static boolean pack11(Player p, String params)
	{
		if(!p.isOnline()) return false;
		String sorts[] = params.split(",");
		for(String sort:sorts)
		{
			if(sort.isEmpty()) continue;
			if(World.getSort(Integer.parseInt(sort)) == null) return false;
			p.learnSpell(Integer.parseInt(sort), 1, false,true);
		}
		SQLManager.SAVE_PERSONNAGE(p, false);
		return true;
	}
	private static boolean pack12(Player p, String params)
	{
		if(!p.isOnline()) return false;
		p.modifAlignement((byte) 3);
		if(p.get_honor() < 18000)
		{
			p.addHonor(18000-p.get_honor());
		}
		p.set_title(100);
		ObjTemplate t = World.getObjTemplate(6971);
		Item obj = t.createNewItem(1, true, -1);
		if(p.addObjet(obj, true)) World.addObjet(obj,true);
		SocketManager.GAME_SEND_Im_PACKET(p, "021;1~6971");
		SocketManager.GAME_SEND_Ow_PACKET(p);
		SQLManager.SAVE_PERSONNAGE(p, true);
		return true;
	}
	private static boolean pack13(Player p, String params) {
		if(!p.isOnline()) return false;
		int itemID = Integer.parseInt(params.split(",")[0]);
		int useMax = Integer.parseInt(params.split(",")[1]);
		String phrase = "";
		if (useMax == 0){
			ObjTemplate t = World.getObjTemplate(itemID);
			Item obj = t.createNewItem(1, false, -1);
			if(p.addObjet(obj, true)) World.addObjet(obj,true);
			phrase = "L'objet <b>"+t.getName()+"</b> vient d'être ajouter a votre personnage";
		} else if (useMax == 1){
			ObjTemplate t = World.getObjTemplate(itemID);
			Item obj = t.createNewItem(1, true, -1);
			if(p.addObjet(obj, true)) World.addObjet(obj,true);
			phrase = "L'objet <b>"+t.getName()+"</b> sous ses effets maximum, vient d'être ajouté à votre personnage.";
		} else {
			GameServer.addToLog("Pack13 error, useMax "+useMax+" don't exist");
		}
		SocketManager.GAME_SEND_MESSAGE(p, phrase, Config.CONFIG_MOTD_COLOR);
		SQLManager.SAVE_PERSONNAGE(p, true);
		return true;
	}
	private static boolean pack14(Player p, String params) {
		if(!p.isOnline()) return false;
		int qua = Integer.parseInt(params);
		if (qua == 0) return false;
		p.addSpellPoint(qua);
		SocketManager.GAME_SEND_STATS_PACKET(p);
		SQLManager.SAVE_PERSONNAGE(p, false);
		SocketManager.GAME_SEND_MESSAGE(p, "Une quantité de " +qua+ " points de sort vient d'être ajouté à votre personnage.", Config.CONFIG_MOTD_COLOR);
		return true;
	}
}
