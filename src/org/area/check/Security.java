package org.area.check;

import org.area.client.Account;
import org.area.client.Player;
import org.area.common.World;

public class Security {

	public static boolean isCompromised(String packet, Player character) {
		// on bannis si test de aille?
		boolean banned = false;
		boolean exploit = false;

		// suceptible d'�tre faill�
		switch (packet.substring(0, 2)) {
		case "AA": // Cr�ation de perso
			break;

		case "DR": // Dialogue R�ponse
			switch (packet.substring(3)) {
			case "DR677|605": // Chacha + tp
				if (character.getMap().get_id() != 2084)
					exploit = true;
				break;

			case "DR3234|2874": // kamas + oeuf de tofu ob�se
				exploit = true;
				break;

			case "DR333|414": // Dofus cawotte TODO : � corriger des
								// l'implantation en jeux!
				exploit = true;
				break;

			case "DR318|259": // Ouverture de la banque
				String map = character.getMap().get_id() + "";
				String banqueMap = "7549 8366 1674 10216 10217 10370";

				if (!banqueMap.contains(map))
					exploit = true;
				break;
			}
			break;

		case "OD": // Objet au sol
		case "Od": // Objet D�truire
			if (packet.contains("-"))
				exploit = true;
			break;

		default: // Valeur n�gative
			if (packet.contains("-"))
				exploit = true;
		}

		if (exploit && banned) { // Banned is always false -_- 
			// on viol le joueur
			Account account = character.getAccount();
			World.Banip(account, 0);

			try {
				// kick du serveur de jeu
				/** TODO : � changer lors du passage sous Mina du serveur de jeu **/
				if (account.getGameThread() != null)
					account.getGameThread().kick();
			} catch (Exception e) {
			}
		}

		return exploit;
	}

	public static boolean estTitreValide(String titre) {
		String[] chainesInterdites = { "*", "-", ".", "modo", "Admin", "[", "]", ";", ",", "/", "�", "%", "$", "�", "|", "#", "�", "<", ">", "~" };
		for (int i = 0; i < chainesInterdites.length; i++) {
			if (titre.toLowerCase().contains(chainesInterdites[i]))
				return false;
		}
		return true;
	}
}
