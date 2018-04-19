package org.area.command.player;

import org.area.client.Player;
import org.area.common.Constant;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.game.tools.Util;
import org.area.lang.Lang;
import org.area.object.Item;
import org.area.spell.SpellEffect;

public class FmCac {
	
	public static boolean exec(Player player, String msg) {

		Item obj = player.getObjetByPos(Constant.ITEM_POS_ARME);

		if(player.getFight() != null) {
			player.sendMess(Lang.LANG_50);
			return true;
		
		} else if(obj == null) {
			player.sendMess(Lang.LANG_108);
			return true;
		}

		boolean containNeutre = false;
		
		for(SpellEffect effect : obj.getEffects()) {
			if(effect.getEffectID() == 100 || effect.getEffectID() == 95)
				containNeutre = true;
		}
		if(!containNeutre) {
			player.sendMess(Lang.LANG_109);
			return true;
		}
		
		String answer = msg;

		if(!answer.equalsIgnoreCase("air") && !answer.equalsIgnoreCase("terre") && !answer.equalsIgnoreCase("feu") && !answer.equalsIgnoreCase("eau")) {
			return true;
		}

		for(int i = 0; i < obj.getEffects().size(); i++) {
			
			if(obj.getEffects().get(i).getEffectID() == 100) {
				if(answer.equalsIgnoreCase("air"))
					obj.getEffects().get(i).setEffectID(98);
				
				if(answer.equalsIgnoreCase("feu"))
					obj.getEffects().get(i).setEffectID(99);
				
				if(answer.equalsIgnoreCase("terre"))
					obj.getEffects().get(i).setEffectID(97);
				
				if(answer.equalsIgnoreCase("eau"))
					obj.getEffects().get(i).setEffectID(96);
			}

			if(obj.getEffects().get(i).getEffectID() == 95) {
				if(answer.equalsIgnoreCase("air"))
					obj.getEffects().get(i).setEffectID(93);
				
				if(answer.equalsIgnoreCase("feu"))
					obj.getEffects().get(i).setEffectID(94);
					
				if(answer.equalsIgnoreCase("terre"))
					obj.getEffects().get(i).setEffectID(92);
				
				if(answer.equalsIgnoreCase("eau"))
					obj.getEffects().get(i).setEffectID(91);
				
			}
		}
		
		SocketManager.GAME_SEND_STATS_PACKET(player);
		player.sendMess(Lang.LANG_110);
		
		// Add retrait de pts by Tµ
		int points = Util.loadPointsByAccount(player.getAccount());
		int newPoints = points - 10;
		Util.updatePointsByAccount(player.getAccount(), newPoints, "FmCac");
		player.send("000C"+Util.loadPointsByAccount(player.getAccount()));
		SQLManager.SAVE_PERSONNAGE(player, false);
		/**GameThread.Object_move(_perso, _perso.get_compte().getGameThread().get_out(), 1, obj.getGuid(), 1);
		_perso.removeItem(obj.getGuid());
		_perso.addObjet(World.getObjet(obj.getGuid()));**/
		
		player.removeItem(obj.getGuid());
		World.removeItem(obj.getGuid());
		SQLManager.DELETE_ITEM(obj.getGuid());
		SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, obj.getGuid());
		
		if(player.addObjet(obj, true))// Si le joueur n'avait pas d'item
			World.addObjet(obj, true);
		
		SocketManager.GAME_SEND_STATS_PACKET(player);
		SocketManager.GAME_SEND_Ow_PACKET(player);
		return true;
	
	}
}
