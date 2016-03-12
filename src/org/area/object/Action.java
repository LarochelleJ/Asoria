package org.area.object;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

import org.area.client.Player;
import org.area.client.Player.Stats;
import org.area.common.ConditionParser;
import org.area.common.Constant;
import org.area.common.Formulas;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.common.World.Area;
import org.area.common.World.SubArea;
import org.area.common.World.Exchange.NpcExchange;
import org.area.fight.object.Prism;
import org.area.fight.object.Monster.MobGroup;
import org.area.game.GameServer;
import org.area.game.tools.ParseTool;
import org.area.kernel.Config;
import org.area.object.NpcTemplate.NPC;
import org.area.object.NpcTemplate.NPC_question;
import org.area.object.Item.ObjTemplate;
import org.area.object.job.Job.StatsMetier;




public class Action {

	private int ID;
	private String args;
	private String cond;
	
	public Action(int id, String args, String cond)
	{
		this.ID = id;
		this.args = args;
		this.cond = cond;
	}

	
	
	public void execute_item_action (int ID, Player perso) {
		switch(ID) {
			default:
				break;
			case 684: // Parchemin de sort.
				perso.addSpellPoint(1);
				SocketManager.GAME_SEND_STATS_PACKET(perso);
				break;
			case 29004: // Bonbon d'xp x2 72h
				perso.setCandy(29004);
				break;
		}
	}
	
	public void apply(Player perso, Player target, int itemID, int cellid)
	{
		if(perso == null)return;
		org.area.game.GameSendThread out = null;

		/*
		 * Conditions des nouveaux items.
		 */
		
		if(!cond.equalsIgnoreCase("") && !cond.equalsIgnoreCase("-1")&& !ConditionParser.validConditions(perso,cond))
		{
			SocketManager.GAME_SEND_Im_PACKET(perso, "119");
			return;
		}
		//if(perso.getAccount().getGameThread() == null && ID != 0) return; // On autorise les t�l�portations, m�me lorsque d�connect�. (Donjon endfight actions)
		if (perso.getAccount().getGameThread() != null){
		out = perso.getAccount().getGameThread().getOut();	
		}
		else if (ID != 0){
			return;
		}
		switch(ID)
		{
		case -2://cr�er guilde
			if(perso.is_away())return;
			if(perso.get_guild() != null || perso.getGuildMember() != null)
			{
				SocketManager.GAME_SEND_gC_PACKET(perso, "Ea");
				return;
			}
			if(perso.hasItemGuid(1575)) {
			SocketManager.GAME_SEND_gn_PACKET(perso);
			perso.removeByTemplateID(1575,-1);
			SocketManager.GAME_SEND_Im_PACKET(perso, "022;"+-1+"~"+1575);
			} else {
				SocketManager.GAME_SEND_MESSAGE(perso, "Pour pouvoir cr�er une guilde, il faut poss�der une Guildalogemme", Config.CONFIG_MOTD_COLOR);
			}
		break;
			case -1://Ouvrir banque
				//Sauvagarde du perso et des item avant.
				SQLManager.SAVE_PERSONNAGE(perso,true);
				if(perso.getDeshonor() >= 1) 
				{
					SocketManager.GAME_SEND_Im_PACKET(perso, "183");
					return;
				}
				int cost = perso.getBankCost();
				if(cost > 0)
				{
					long nKamas = perso.get_kamas() - cost;
					if(nKamas <0)//Si le joueur n'a pas assez de kamas pour ouvrir la banque
					{
						SocketManager.GAME_SEND_Im_PACKET(perso, "1128;"+cost);
						return;
					}
					perso.set_kamas(nKamas);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SocketManager.GAME_SEND_Im_PACKET(perso, "020;"+cost);
				}
				SocketManager.GAME_SEND_ECK_PACKET(perso.getAccount().getGameThread().getOut(), 5, "");
				SocketManager.GAME_SEND_EL_BANK_PACKET(perso);
				perso.set_away(true);
				perso.setInBank(true);
			break;
			
			case 0://T�l�portation
				try
				{
					short newMapID = Short.parseShort(args.split(",",2)[0]);
					int newCellID = Integer.parseInt(args.split(",",2)[1]);
					if (perso.getCurCarte().get_id() != newMapID){
					perso.teleport(newMapID,newCellID);	
					}
				}catch(Exception e ){return;};
			break;
			
			case 1://Discours NPC
				out = perso.getAccount().getGameThread().getOut();
				if(args.equalsIgnoreCase("DV"))
				{
					SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
					perso.set_isTalkingWith(0);
				}else
				{
					int qID = -1;
					try
					{
						qID = Integer.parseInt(args);
					}catch(NumberFormatException e){};
					
					NPC_question  quest = World.getNPCQuestion(qID);
					if(quest == null)
					{
						SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
						perso.set_isTalkingWith(0);
						return;
					}
					SocketManager.GAME_SEND_QUESTION_PACKET(out, quest.parseToDQPacket(perso));
				}
			break;
			
			case 4://Kamas
				try
				{
					int count = Integer.parseInt(args);
					long curKamas = perso.get_kamas();
					long newKamas = curKamas + count;
					if(newKamas <0) newKamas = 0;
					perso.set_kamas(newKamas);
					
					//Si en ligne (normalement oui)
					if(perso.isOnline())
						SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 5://objet
				try
				{
					int tID = Integer.parseInt(args.split(",")[0]);
					int count = Integer.parseInt(args.split(",")[1]);
					boolean send = true;
					if(args.split(",").length >2)send = args.split(",")[2].equals("1");
					
					//Si on ajoute
					if(count > 0)
					{
						ObjTemplate T = World.getObjTemplate(tID);

						if(T == null)return;
						Item O = T.createNewItem(count, false, -1);
						//Si retourne true, on l'ajoute au monde
						if(perso.addObjet(O, true))
							World.addObjet(O, true);
					}else
					{
						/* Action item perso */
						execute_item_action(tID, perso);

						if(perso.getCandyUsed() != "") {
							perso.sendText("Un bonbon sp�cial est toujours actif.");
							return;
						}
						perso.removeByTemplateID(tID,-count);
					}
					//Si en ligne (normalement oui)
					if(perso.isOnline())//on envoie le packet qui indique l'ajout//retrait d'un item
					{
						SocketManager.GAME_SEND_Ow_PACKET(perso);
						if(send)
						{
							if(count >= 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "021;"+count+"~"+tID);
							}
							else if(count < 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "022;"+-count+"~"+tID);
							}
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 6://Apprendre un m�tier
				try
				{
					int mID = Integer.parseInt(args);
					if(World.getMetier(mID) == null)return;
					// Si c'est un m�tier 'basic' :
					if(mID == 	2 || mID == 11 ||
					   mID == 13 || mID == 14 ||
					   mID == 15 || mID == 16 ||
					   mID == 17 || mID == 18 ||
					   mID == 19 || mID == 20 ||
					   mID == 24 || mID == 25 ||
					   mID == 26 || mID == 27 ||
					   mID == 28 || mID == 31 ||
					   mID == 36 || mID == 41 ||
					   mID == 56 || mID == 58 ||
					   mID == 60 || mID == 65)
					{
						if(perso.getMetierByID(mID) != null)//M�tier d�j� appris
						{
							SocketManager.GAME_SEND_Im_PACKET(perso, "111");
						}
						
						if(perso.totalJobBasic() > 2)//On compte les m�tiers d�ja acquis si c'est sup�rieur a 2 on ignore
						{
							SocketManager.GAME_SEND_Im_PACKET(perso, "19");
						}else//Si c'est < ou = � 2 on apprend
						{
							perso.learnJob(World.getMetier(mID));
						}
					}
					// Si c'est une specialisations 'FM' :
					if(mID == 	43 || mID == 44 ||
					   mID == 45 || mID == 46 ||
					   mID == 47 || mID == 48 ||
					   mID == 49 || mID == 50 ||
					   mID == 62 || mID == 63 ||
					   mID == 64)
					{
						//M�tier simple level 65 n�cessaire
						if(perso.getMetierByID(17) != null && perso.getMetierByID(17).get_lvl() >= 65 && mID == 43
						|| perso.getMetierByID(11) != null && perso.getMetierByID(11).get_lvl() >= 65 && mID == 44
						|| perso.getMetierByID(14) != null && perso.getMetierByID(14).get_lvl() >= 65 && mID == 45
						|| perso.getMetierByID(20) != null && perso.getMetierByID(20).get_lvl() >= 65 && mID == 46
						|| perso.getMetierByID(31) != null && perso.getMetierByID(31).get_lvl() >= 65 && mID == 47
						|| perso.getMetierByID(13) != null && perso.getMetierByID(13).get_lvl() >= 65 && mID == 48
						|| perso.getMetierByID(19) != null && perso.getMetierByID(19).get_lvl() >= 65 && mID == 49
						|| perso.getMetierByID(18) != null && perso.getMetierByID(18).get_lvl() >= 65 && mID == 50
						|| perso.getMetierByID(15) != null && perso.getMetierByID(15).get_lvl() >= 65 && mID == 62
						|| perso.getMetierByID(16) != null && perso.getMetierByID(16).get_lvl() >= 65 && mID == 63
						|| perso.getMetierByID(27) != null && perso.getMetierByID(27).get_lvl() >= 65 && mID == 64)
						{
							//On compte les specialisations d�ja acquis si c'est sup�rieur a 2 on ignore
							if(perso.getMetierByID(mID) != null)//M�tier d�j� appris
							{
								SocketManager.GAME_SEND_Im_PACKET(perso, "111");
							}
							
							if(perso.totalJobFM() > 2)//On compte les m�tiers d�ja acquis si c'est sup�rieur a 2 on ignore
							{
								SocketManager.GAME_SEND_Im_PACKET(perso, "19");
							}
							else//Si c'est < ou = � 2 on apprend
							{
								perso.learnJob(World.getMetier(mID));
								perso.getMetierByID(mID).addXp(perso, 582000);//Level 100 direct
							}	
						}else
						{
							SocketManager.GAME_SEND_Im_PACKET(perso, "12");
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 7://retour au point de sauvegarde
				perso.warpToSavePos();
			break;
			case 8://Ajouter une Stat
				try
				{
					int statID = Integer.parseInt(args.split(",",2)[0]);
					int number = Integer.parseInt(args.split(",",2)[1]);
					perso.get_baseStats().addOneStat(statID, number);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					int messID = 0;
					switch(statID)
					{
						case Constant.STATS_ADD_INTE: messID = 14;break;
					}
					if(perso.CheckItemConditions() != 0)
					{
						SocketManager.GAME_SEND_Ow_PACKET(perso);
						perso.refreshStats();
						if(perso.getGroup() != null)
						{
							SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(perso.getGroup(),perso);
						}
						SocketManager.GAME_SEND_STATS_PACKET(perso);
					}
					if(messID>0)
						SocketManager.GAME_SEND_Im_PACKET(perso, "0"+messID+";"+number);
				}catch(Exception e ){return;};
			break;
			case 9://Apprendre un sort
				try
				{
					int sID = Integer.parseInt(args);
					if(World.getSort(sID) == null)return;
					perso.learnSpell(sID,1, true,true);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 10://Pain/potion/viande/poisson
				try
				{
					int min = Integer.parseInt(args.split(",",2)[0]);
					int max = Integer.parseInt(args.split(",",2)[1]);
					if(max == 0) max = min;
					int val = Formulas.getRandomValue(min, max);
					if(target != null)
					{
						if(target.get_PDV() + val > target.get_PDVMAX())val = target.get_PDVMAX()-target.get_PDV();
						target.set_PDV(target.get_PDV()+val);
						SocketManager.GAME_SEND_STATS_PACKET(target);
					}
					else
					{
						if(perso.get_PDV() + val > perso.get_PDVMAX())val = perso.get_PDVMAX()-perso.get_PDV();
						perso.set_PDV(perso.get_PDV()+val);
						SocketManager.GAME_SEND_STATS_PACKET(perso);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 11://Definir l'alignement
				try
				{
					byte newAlign = Byte.parseByte(args.split(",",2)[0]);
					boolean replace = Integer.parseInt(args.split(",",2)[1]) == 1;
					//Si le perso n'est pas neutre, et qu'on doit pas remplacer, on passe
					if(perso.get_align() != Constant.ALIGNEMENT_NEUTRE && !replace)return;
					perso.modifAlignement(newAlign);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			/* TODO: autres actions */
			case 12://Spawn d'un groupe de monstre
				try
				{
					boolean delObj = args.split(",")[0].equals("true");
					boolean inArena = args.split(",")[1].equals("true");

					if(inArena && !World.isArenaMap(perso.getMap().get_id()))return;	//Si la map du personnage n'est pas class� comme �tant dans l'ar�ne

					SoulStone pierrePleine = (SoulStone)World.getObjet(itemID);

					String groupData = pierrePleine.parseGroupData();
					String condition = "MiS = "+perso.getGuid();	//Condition pour que le groupe ne soit lan�able que par le personnage qui � utiliser l'objet
					perso.getMap().spawnNewGroup(true, perso.get_curCell().getID(), groupData,condition);

					if(delObj)
					{
						perso.removeItem(itemID, 1, true, true);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
		    case 13: //Reset Carac
		        try
		        {
		          perso.get_baseStats().addOneStat(125, -perso.get_baseStats().getEffect(125));
		          perso.get_baseStats().addOneStat(124, -perso.get_baseStats().getEffect(124));
		          perso.get_baseStats().addOneStat(118, -perso.get_baseStats().getEffect(118));
		          perso.get_baseStats().addOneStat(123, -perso.get_baseStats().getEffect(123));
		          perso.get_baseStats().addOneStat(119, -perso.get_baseStats().getEffect(119));
		          perso.get_baseStats().addOneStat(126, -perso.get_baseStats().getEffect(126));
		          perso.addCapital((perso.getLevel() - 1) * 5 - perso.get_capital());
					if(perso.CheckItemConditions() != 0)
					{
						SocketManager.GAME_SEND_Ow_PACKET(perso);
						perso.refreshStats();
						if(perso.getGroup() != null)
						{
							SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(perso.getGroup(),perso);
						}
					}
		          SocketManager.GAME_SEND_STATS_PACKET(perso);
		        }catch(Exception e){GameServer.addToLog(e.getMessage());};
		    break;
		    case 14://Ouvrir l'interface d'oublie de sort
		    	perso.setisForgetingSpell(true);
				SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', perso);
			break;
			case 15://T�l�portation donjon
				try
				{
					short newMapID = Short.parseShort(args.split(",")[0]);
					int newCellID = Integer.parseInt(args.split(",")[1]);
					int ObjetNeed = Integer.parseInt(args.split(",")[2]);
					int MapNeed = Integer.parseInt(args.split(",")[3]);
					if(ObjetNeed == 0)
					{
						//T�l�portation sans objets
						perso.teleport(newMapID,newCellID);
					}else if(ObjetNeed > 0)
					{
					if(MapNeed == 0)
					{
						//T�l�portation sans map
						perso.teleport(newMapID,newCellID);
					}else if(MapNeed > 0)
					{
					if (perso.hasItemTemplate(ObjetNeed, 1) && perso.getMap().get_id() == MapNeed) // Le object need c'est juste l'id de l'objet, sinon c'ets pas grave, c'est pas tr�s important, a la place de "clef on met "objet" et puis c'est tout ^^ Ou sinon encore plus simple
					{
						//Le perso a l'item
						//Le perso est sur la bonne map
						//On t�l�porte, on supprime apr�s
						perso.teleport(newMapID,newCellID);
						perso.removeByTemplateID(ObjetNeed, 1);
						SocketManager.GAME_SEND_Ow_PACKET(perso);
					}
					else if(perso.getMap().get_id() != MapNeed)
					{
						//Le perso n'est pas sur la bonne map
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous n'etes pas sur la bonne map du donjon pour etre teleporter.", "009900");
					}
					else
					{
						//Le perso ne poss�de pas l'item
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous ne possedez pas l'objet necessaire.", "009900"); // Je voudrais modifier "clef" par le nom de l'item en question, tu sais comment faire ? je regarde vite fais
					}
					}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 16://Ajout d'honneur HonorValue
				try
				{
					if(perso.get_align() != 0)
					{
						int AddHonor = Integer.parseInt(args);
						int ActualHonor = perso.get_honor();
						perso.set_honor(ActualHonor+AddHonor);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 17://Xp m�tier JobID,XpValue
				try
				{
					int JobID = Integer.parseInt(args.split(",")[0]);
					int XpValue = Integer.parseInt(args.split(",")[1]);
					if(perso.getMetierByID(JobID) != null)
					{
						perso.getMetierByID(JobID).addXp(perso, XpValue);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 18://T�l�portation chez sois
				if(Houses.AlreadyHaveHouse(perso))//Si il a une maison
				{
					Item obj = World.getObjet(itemID);
					if (perso.hasItemTemplate(obj.getTemplate(false).getID(), 1))
					{
						perso.removeByTemplateID(obj.getTemplate(false).getID(),1);
						Houses h = Houses.get_HouseByPerso(perso);
						if(h == null) return;
						perso.teleport((short)h.get_mapid(), h.get_caseid());
					}
				}
			break;
			case 19://T�l�portation maison de guilde (ouverture du panneau de guilde)
				SocketManager.GAME_SEND_GUILDHOUSE_PACKET(perso);
			break;
			case 20://+Points de sorts
				try
				{
					int pts = Integer.parseInt(args);
					if(pts < 1) return;
					perso.addSpellPoint(pts);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 21://+Energie
				try
				{
					int Energy = Integer.parseInt(args);
					if(Energy < 1) return;
					
					int EnergyTotal = perso.get_energy()+Energy;
					if(EnergyTotal > 10000) EnergyTotal = 10000;
					
					perso.set_energy(EnergyTotal);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 22://+Xp
				try
				{
					long XpAdd = Integer.parseInt(args);
					if(XpAdd < 1) return;
					
					long TotalXp = perso.get_curExp()+XpAdd;
					perso.set_curExp(TotalXp);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 23://UnlearnJob
				try
				{
					int Job = Integer.parseInt(args);
					if(Job < 1) return;
					StatsMetier m = perso.getMetierByID(Job);
					if(m == null) return;
					perso.unlearnJob(m.getID());
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SQLManager.SAVE_PERSONNAGE(perso, false);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 24://SimpleMorph
				try
				{
					int morphID = Integer.parseInt(args);
					if(morphID < 0)return;
					perso.set_gfxID(morphID);
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 25://SimpleUnMorph
				int UnMorphID = perso.get_classe()*10 + perso.get_sexe();
				perso.set_gfxID(UnMorphID);
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
				SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
			break;
			case 26://T�l�portation enclo de guilde (ouverture du panneau de guilde)
				SocketManager.GAME_SEND_GUILDENCLO_PACKET(perso);
			break;
			case 27://startFigthVersusMonstres args : monsterID,monsterLevel| ...
				String ValidMobGroup = "";
				try
		        {
					for(String MobAndLevel : args.split("\\|"))
					{
						int monsterID = -1;
						int monsterLevel = -1;
						String[] MobOrLevel = MobAndLevel.split(",");
						monsterID = Integer.parseInt(MobOrLevel[0]);
						monsterLevel = Integer.parseInt(MobOrLevel[1]);
						
						if(World.getMonstre(monsterID) == null || World.getMonstre(monsterID).getGradeByLevel(monsterLevel) == null)
						{
							if(Config.DEBUG) GameServer.addToLog("Monstre invalide : monsterID:"+monsterID+" monsterLevel:"+monsterLevel);
							continue;
						}
						ValidMobGroup += monsterID+","+monsterLevel+","+monsterLevel+";";
					}
					if(ValidMobGroup.isEmpty()) return;
					MobGroup group  = new MobGroup(perso.getMap().get_nextObjectID(),perso.get_curCell().getID(),ValidMobGroup);
					perso.getMap().startFigthVersusMonstres(perso, group);
		        }catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 100://Donner l'abilit� 'args' � une dragodinde
                Mount mount = perso.getMount();
                World.addDragodinde(
                  new Mount(
                 mount.get_id(),
                 mount.get_color(),
                 mount.get_sexe(),
                 mount.get_amour(),
                 mount.get_endurance(),
                 mount.get_level(),
                 mount.get_exp(),
                 mount.get_nom(),
                 mount.get_fatigue(),
                 mount.get_energie(),
                 mount.get_reprod(),
                 mount.get_maturite(),
                 mount.get_serenite(),
                 mount.parseObjDB(),
                 mount.get_ancetres(),
                 args));
                 perso.setMount(World.getDragoByID(mount.get_id()));
                 SocketManager.GAME_SEND_Re_PACKET(perso, "+", World.getDragoByID(mount.get_id()));
                 SQLManager.UPDATE_MOUNT_INFOS(mount);
                 break;
			case 101://Arriver sur case de mariage
				if((perso.get_sexe() == 0 && perso.get_curCell().getID() == 282) || (perso.get_sexe() == 1 && perso.get_curCell().getID() == 297))
				{
					World.AddMarried(perso.get_sexe(), perso);
				}else 
				{
					SocketManager.GAME_SEND_Im_PACKET(perso, "1102");
				}
			break;
			case 102://Marier des personnages
				World.PriestRequest(perso, perso.getMap(), perso.get_isTalkingWith());
			break;
			case 103://Divorce
				if(perso.get_kamas() < 50000)
				{
					return;
				}else
				{
					perso.set_kamas(perso.get_kamas()-50000);
					Player wife = World.getPlayer(perso.getWife());
					wife.Divorce();
					perso.Divorce();
				}
			break;
			case 104://Don d'objet (cadeau � la connexion, arg : IDdelitem
				int item = Integer.parseInt(args); 
				perso.getAccount().setCadeau(item);
				SocketManager.GAME_SEND_MESSAGE(perso, "Vous avez re�u un cadeau sur votre compte !", Config.CONFIG_MOTD_COLOR);
			break;
			case 228://Faire animation Hors Combat
				try
				{
					int AnimationId = Integer.parseInt(args);
					Hustle animation = World.getAnimation(AnimationId);
					if(perso.getFight() != null) return;
					perso.changeOrientation(1);
					SocketManager.GAME_SEND_GA_PACKET_TO_MAP(perso.getMap(), "0", 228, perso.getGuid()+";"+cellid+","+Hustle.PrepareToGA(animation), "");
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 40: // Give quest
				int questId = Integer.parseInt(args);
				perso.addNewQuest(questId);
			break;
			case 41: // Confirm objective
				String[] splitArgs = args.split("\\|");
				perso.confirmObjective(Integer.parseInt(splitArgs[0]), splitArgs[1],null);
			break;
			case 42: // Monte prochaine �tape quete ou termine
				int quest = Integer.parseInt(args);
				perso.upgradeQuest(quest);
			break;
			case 43://Jouer une cin�matique
				String num = args;
				SocketManager.GAME_SEND_CIN_Packet(perso, num);
				break;
			case 44://Ouvrir un livre.
				String infoi = args;
				SocketManager.GAME_SEND_dCK_PACKET(perso, infoi);
			break;
			case 45://Changer de classe
				try {
					int classe = Integer.parseInt(args);
					if (classe == perso.get_classe()) {
						perso.sendText("IMPOSSIBLE : Votre personnage est d�j� de cette classe.");
						return;
					}
					int niveau = perso.getLevel();
					perso.set_classe(classe);
					Stats baseStats = perso.get_baseStats();
					baseStats.addOneStat(125, -perso._baseStats.getEffect(125));
					baseStats.addOneStat(124, -perso._baseStats.getEffect(124));
					baseStats.addOneStat(118, -perso._baseStats.getEffect(118));
					baseStats.addOneStat(123, -perso._baseStats.getEffect(123));
					baseStats.addOneStat(119, -perso._baseStats.getEffect(119));
					baseStats.addOneStat(126, -perso._baseStats.getEffect(126));
					baseStats.addOneStat(125, perso.getScrollVitalidad());
					baseStats.addOneStat(124, perso.getScrollSabiduria());
					baseStats.addOneStat(118, perso.getScrollFuerza());
					baseStats.addOneStat(123, perso.getScrollSuerte());
					baseStats.addOneStat(119, perso.getScrollAgilidad());
					baseStats.addOneStat(126, perso.getScrollInteligencia());
					Thread.sleep(150);
					perso.setCapital(0);
					perso.set_spellPts(0);
					perso.setHechizos(Constant.getStartSorts(classe));
					Thread.sleep(150);
					perso.set_lvl(1);
					while (perso.getLevel() < niveau) {
						perso.levelUp(false, false);
					}
					int deformaID = classe * 10 + perso.get_sexe();
					perso.set_gfxID(deformaID);
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SocketManager.GAME_SEND_ASK(out, perso);
					SocketManager.GAME_SEND_SPELL_LIST(perso);
					Thread.sleep(150);
					SQLManager.CHANGER_SEX_CLASSE(perso);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case 46://Changer de sex
				try {
					perso.change_sexe();
					Thread.sleep(300);
					int morphID = perso.get_classe() * 10 + perso.get_sexe();
					perso.set_gfxID(morphID);
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
					SQLManager.CHANGER_SEX_CLASSE(perso);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case 47://Changer de nom
				
				if(perso.getFight() != null)
					return;
				
				String name = args;
				perso.set_name(name);
				SQLManager.SAVE_PERSONNAGE(perso, false);
				SocketManager.GAME_SEND_ALTER_GM_PACKET(perso.getMap(), perso);
				break;
				
			case 48://loterie
				
				if(perso.getFight() != null)
					return;
				
				int nombreAleatoire = 0 + (int)(Math.random() * ((100 - 0) + 1));
				
				if(nombreAleatoire == 60)
				{
					ArrayList<ObjTemplate> collection = new ArrayList<ObjTemplate>();
					
					for(Entry<Integer, Integer> entry: ParseTool.getShop().entrySet())
						collection.add(World.getObjTemplate(entry.getKey()));
					
					Collections.sort(collection);
					
					int itemAleatoire = 0 + (int)(Math.random() * ((collection.size() - 0) + 1));
					
					Item items = collection.get(itemAleatoire).createNewItem(1, true, -1);
					
					if (perso.addObjet(items, true))
						World.addObjet(items, true);
					
					SocketManager.GAME_SEND_Ow_PACKET(perso);
					
					perso.sendText("Vous avez gagn� un <b>[" + items.getTemplate(false).getName() +"]</b>. Bravo !");
				}
				
				else
				{
					perso.sendText("Votre coupon est perdant, retentez votre chance une prochaine fois !");
					break;
				}
				
			case 170: // Ajouter un titre au joueur
				try
			    {				
					int titre = Integer.parseInt(args); 
					target= World.getPersoByName(perso.getName());
					target.set_title(titre);
					
                    
			    	
			    	    SocketManager.GAME_SEND_MESSAGE(perso, "<b>Tu poss�des un nouveau titre !</b>", Config.CONFIG_MOTD_COLOR);
						SocketManager.GAME_SEND_STATS_PACKET(perso);
						SQLManager.SAVE_PERSONNAGE(perso, false);
					}catch(Exception e){GameServer.addToLog(e.getMessage());};
				
			    break;
			case 201:// Poser un Prisme
				try {
					int cellperso = perso.get_curCell().getID();
					Maps tCarte = perso.getMap();
					SubArea subarea = tCarte.getSubArea();
					Area area = subarea.getArea();
					int alignement = perso.get_align();
					if (cellperso <= 0) {
						break;
					}
					if (alignement == 0 || alignement == 3) {
						SocketManager.GAME_SEND_MESSAGE(perso,"Vous ne poss�dez pas l'alignement n�cessaire pour poser un prisme.", Config.CONFIG_MOTD_COLOR);
						break;
					}
					if (!perso.is_showWings()) {
						SocketManager.GAME_SEND_MESSAGE(perso,"Vos ailes doivent �tre actives afin de poser un prisme.", Config.CONFIG_MOTD_COLOR);
						break;
					}
					if ((subarea.getalignement() != 0 && subarea.getalignement() != -1) || !subarea.getConquistable()) {
						SocketManager.GAME_SEND_MESSAGE(perso,"L'alignement de cette sous-zone est en conqu�te ou n'est pas neutre !", Config.CONFIG_MOTD_COLOR);
						break;
					}
					Prism Prisme = new Prism(World.getNextIDPrisme(), alignement, 1, tCarte.get_id(), cellperso, 0, -1);
					subarea.setalignement(alignement);
					subarea.setPrismeID(Prisme.getID());
					for (Player z : World.getOnlinePlayers()) {
						if (z == null)
							continue;
						if (z.get_align() == 0) {
							SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subarea.getID() + "|" + alignement + "|1");
							if (area.getalignement() == 0)
								SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(z, area.getID() + "|" + alignement);
							continue;
						}
						SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subarea.getID() + "|" + alignement + "|0");
						if (area.getalignement() == 0)
							SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(z, area.getID() + "|" + alignement);
					}
					if (area.getalignement() == 0) {
						area.setPrismeID(Prisme.getID());
						area.setalignement(alignement);
						Prisme.setAreaConquest(area.getID());
					}
					World.addPrisme(Prisme);
					SQLManager.ADD_PRISME(Prisme);
					SocketManager.GAME_SEND_PRISME_TO_MAP(tCarte, Prisme);
				} catch (Exception e) {}
				break;
			case 600: 
				int levelWant = Integer.parseInt(args);
				perso.goUpto(levelWant);		
				break;
				
			case 212121://Changement apparence Dofus 2 @Flow
				if(perso.getFight() != null)break;
				    perso.set_gfxID(perso.get_classe()*100000 + perso.get_sexe());
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
					perso.sendText("Apparence de votre personnage : Dofus 2");
			break;
			
			case 212122://Changement apparence dofus 1.29.1 @Flow
				if(perso.getFight() != null)break;
					perso.set_gfxID(perso.get_classe()*10 + perso.get_sexe());
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.getMap(), perso.getGuid());
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.getMap(), perso);
					perso.sendText("Apparence de votre personnage : Dofus 1.29.1");
			break;
			
			case 212123://PNJ �changeur boutique
				int nID = 30233;
				perso.set_isTradingWith(nID);
				perso.set_away(true);
				perso.set_echangePNJBoutique(true);
                SocketManager.GAME_SEND_ECK_PACKET(perso.getAccount().getGameThread().getOut(), 2, ""+nID);
				break;
				
			case 212124: // Ornement
            int ornement = Integer.parseInt(args.split(",")[0]);
            if (!perso.haveOrnement(ornement)){
            	perso.addOrnement(ornement);
            	perso.set_ornement(ornement);
            	perso.save(false);
            	perso.sendText("Vous avez obtenu un nouvel ornement !");
            }
		    break;
			
			case 260://T�l�portation en parlant a un pnj avec level minimum requis : Taparisse
				try
				{
					short newMapID = Short.parseShort(args.split(",")[0]);
					int newCellID = Integer.parseInt(args.split(",")[1]);
					int levelperso = Integer.parseInt(args.split(",")[2]);
					
					if (perso.getLevel() >= levelperso)
					{
						perso.teleport(newMapID,newCellID);
					}
					else 
					{
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous n'avez pas le level requis : "+levelperso+".", Config.CONFIG_MOTD_COLOR);
					}
			
				}catch(Exception e ){return;};
				
			default:
				GameServer.addToLog("Action ID="+ID+" non implantee");
				break;
			}
		}


	public int getID()
	{
		return ID;
	}
}
