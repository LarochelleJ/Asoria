package org.area.common;

import org.area.client.Player;
import org.area.game.GameServer;
import org.area.kernel.Config;



import com.singularsys.jep.Jep;
import com.singularsys.jep.JepException;
import org.area.quests.Quest;
import org.area.quests.QuestPlayer;
import org.area.quests.Quest_Step;

public class ConditionParser
{
	public static boolean validConditions(Player perso,String req)
	{
		if(req == null || req.equals(""))return true;
		if(req.contains("BI"))return false;
		
		Jep jep = new Jep();
		
		req = req.replace("&", "&&").replace("=", "==").replace("|", "||").replace("!", "!=").replace("~", "==");
		
		if(req.contains("PO"))
			req = havePO(req, perso);
		if(req.contains("PN"))
			req = canPN(req, perso);
		if(req.contains("Qa") || req.contains("QT"))
			return haveQa(req, perso);
		if(req.contains("QE"))
			return haveQE(req, perso);
		if (req.contains("QEt"))
			return haveQEt(req, perso);
	 	//TODO : Gérer PJ Pj
		try
		{
				//Stats stuff compris
				jep.addVariable("CI", perso.getTotalStats().getEffect(Constant.STATS_ADD_INTE));
			 	jep.addVariable("CV", perso.getTotalStats().getEffect(Constant.STATS_ADD_VITA));
			 	jep.addVariable("CA", perso.getTotalStats().getEffect(Constant.STATS_ADD_AGIL));
			 	jep.addVariable("CW", perso.getTotalStats().getEffect(Constant.STATS_ADD_SAGE));
			 	jep.addVariable("CC", perso.getTotalStats().getEffect(Constant.STATS_ADD_CHAN));
			 	jep.addVariable("CS", perso.getTotalStats().getEffect(Constant.STATS_ADD_FORC));
			 	jep.addVariable("PA", perso.getTotalStats().getEffect(Constant.STATS_ADD_PA));
			 	//Stats de bases
			 	jep.addVariable("Ci", perso.get_baseStats().getEffect(Constant.STATS_ADD_INTE));
			 	jep.addVariable("Cs", perso.get_baseStats().getEffect(Constant.STATS_ADD_FORC));
			 	jep.addVariable("Cv", perso.get_baseStats().getEffect(Constant.STATS_ADD_VITA));
			 	jep.addVariable("Ca", perso.get_baseStats().getEffect(Constant.STATS_ADD_AGIL));
			 	jep.addVariable("Cw", perso.get_baseStats().getEffect(Constant.STATS_ADD_SAGE));
			 	jep.addVariable("Cc", perso.get_baseStats().getEffect(Constant.STATS_ADD_CHAN));
			 	//Autre
			 	jep.addVariable("Ps", perso.get_align());
			 	jep.addVariable("Pa", perso.getALvl());
			 	jep.addVariable("PP", perso.getGrade());
			 	jep.addVariable("PL", perso.getLevel());
			 	jep.addVariable("PK", perso.get_kamas());
			 	jep.addVariable("PG", perso.get_classe());
			 	jep.addVariable("PS", perso.get_sexe());
			 	jep.addVariable("PZ", SQLManager.ACCOUNT_IS_VIP(perso.getAccount())); //Abonnement normalement
			 	jep.addVariable("PX", perso.getAccount().getGmLevel());
			 	jep.addVariable("PW", perso.getMaxPod());
			 	jep.addVariable("PB", perso.getMap().getSubArea().getID());
			 	jep.addVariable("PR", (perso.getWife()>0?1:0));
			 	jep.addVariable("SI", perso.getMap().get_id());
			 	jep.addVariable("PR", perso.getPrestige());
			 	//Les pierres d'ames sont lancables uniquement par le lanceur.
			 	jep.addVariable("MiS",perso.getGuid());
			 	
			 	jep.parse(req);
			 	Object result = jep.evaluate();
			 	boolean ok = false;
			 	if(result != null)ok = Boolean.valueOf(result.toString());
			 	return ok;
		} catch (JepException e)
		{
		}
		return true;
	}
	
	public static String havePO(String cond,Player perso)//On remplace les PO par leurs valeurs si possession de l'item
	{
		boolean Jump = false;
		boolean ContainsPO = false;
		boolean CutFinalLenght = true;
		String copyCond = "";
		int finalLength = 0;
		
		if(Config.DEBUG) GameServer.addToLog("Entered Cond : "+cond);
		
		if(cond.contains("&&"))
		{
			for(String cur : cond.split("&&"))
			{
				if(cond.contains("=="))
				{
					for(String cur2 : cur.split("=="))
					{
						if(cur2.contains("PO")) 
						{
							ContainsPO = true;
							continue;
						}
						if(Jump)
						{
							copyCond += cur2;
							Jump = false;
							continue;
						}
						if(!cur2.contains("PO") && !ContainsPO)
						{
							copyCond += cur2+"==";
							Jump = true;
							continue;
						}
						if(cur2.contains("!=")) continue;
						ContainsPO = false;
						if(perso.hasItemTemplate(Integer.parseInt(cur2), 1))
						{
							copyCond += Integer.parseInt(cur2)+"=="+Integer.parseInt(cur2);
						}else
						{
							copyCond += Integer.parseInt(cur2)+"=="+0;
						}
					}
				}
				if(cond.contains("!="))
				{
					for(String cur2 : cur.split("!="))
					{
						if(cur2.contains("PO")) 
						{
							ContainsPO = true;
							continue;
						}
						if(Jump)
						{
							copyCond += cur2;
							Jump = false;
							continue;
						}
						if(!cur2.contains("PO") && !ContainsPO)
						{
							copyCond += cur2+"!=";
							Jump = true;
							continue;
						}
						if(cur2.contains("==")) continue;
						ContainsPO = false;
						if(perso.hasItemTemplate(Integer.parseInt(cur2), 1))
						{
							copyCond += Integer.parseInt(cur2)+"!="+Integer.parseInt(cur2);
						}else
						{
							copyCond += Integer.parseInt(cur2)+"!="+0;
						}
					}
				}
				copyCond += "&&";
			}
		}else if(cond.contains("||"))
		{
			for(String cur : cond.split("\\|\\|"))
			{
				if(cond.contains("=="))
				{
					for(String cur2 : cur.split("=="))
					{
						if(cur2.contains("PO")) 
						{
							ContainsPO = true;
							continue;
						}
						if(Jump)
						{
							copyCond += cur2;
							Jump = false;
							continue;
						}
						if(!cur2.contains("PO") && !ContainsPO)
						{
							copyCond += cur2+"==";
							Jump = true;
							continue;
						}
						if(cur2.contains("!=")) continue;
						ContainsPO = false;
						if(perso.hasItemTemplate(Integer.parseInt(cur2), 1))
						{
							copyCond += Integer.parseInt(cur2)+"=="+Integer.parseInt(cur2);
						}else
						{
							copyCond += Integer.parseInt(cur2)+"=="+0;
						}
					}
				}
				if(cond.contains("!="))
				{
					for(String cur2 : cur.split("!="))
					{
						if(cur2.contains("PO")) 
						{
							ContainsPO = true;
							continue;
						}
						if(Jump)
						{
							copyCond += cur2;
							Jump = false;
							continue;
						}
						if(!cur2.contains("PO") && !ContainsPO)
						{
							copyCond += cur2+"!=";
							Jump = true;
							continue;
						}
						if(cur2.contains("==")) continue;
						ContainsPO = false;
						if(perso.hasItemTemplate(Integer.parseInt(cur2), 1))
						{
							copyCond += Integer.parseInt(cur2)+"!="+Integer.parseInt(cur2);
						}else
						{
							copyCond += Integer.parseInt(cur2)+"!="+0;
						}
					}
				}
					copyCond += "||";
			}
		}else
		{
			CutFinalLenght = false;
			if(cond.contains("=="))
			{
				for(String cur : cond.split("=="))
				{
					if(cur.contains("PO")) 
					{
						continue;
					}
					if(cur.contains("!=")) continue;
					if(perso.hasItemTemplate(Integer.parseInt(cur), 1))
					{
						copyCond += Integer.parseInt(cur)+"=="+Integer.parseInt(cur);
					}else
					{
						copyCond += Integer.parseInt(cur)+"=="+0;
					}
				}
			}
			if(cond.contains("!="))
			{
				for(String cur : cond.split("!="))
				{
					if(cur.contains("PO")) 
					{
						continue;
					}
					if(cur.contains("==")) continue;
					if(perso.hasItemTemplate(Integer.parseInt(cur), 1))
					{
						copyCond += Integer.parseInt(cur)+"!="+Integer.parseInt(cur);
					}else
					{
						copyCond += Integer.parseInt(cur)+"!="+0;
					}
				}
			}
		}
		if(CutFinalLenght)
		{
			finalLength = (copyCond.length()-2);//On retire les deux derniers carractères (|| ou &&)
			copyCond = copyCond.substring(0, finalLength);
		}
		if(Config.DEBUG) GameServer.addToLog("Returned Cond : "+copyCond);
		return copyCond;
	}
	
	public static String canPN(String cond,Player perso)//On remplace le PN par 1 et si le nom correspond == 1 sinon == 0
	{
		String copyCond = "";
		for(String cur : cond.split("=="))
		{
			if(cur.contains("PN")) 
			{
				copyCond += "1==";
				continue;
			}
			if(perso.getName().toLowerCase().compareTo(cur) == 0)
			{
				copyCond += "1";
			}else
			{
				copyCond += "0";
			}
		}
		return copyCond;
	}

	// Avoir la quête en cours
	private static boolean haveQa(String req, Player player) {
		int id = Integer.parseInt((req.contains("==") ? req.split("==")[1] : req.split("!=")[1]));
		Quest q = Quest.getQuestById(id);
		if (q == null)
			return (!req.contains("=="));

		QuestPlayer qp = player.getQuestPersoByQuest(q);
		if (qp == null)
			return (!req.contains("=="));

		return !qp.isFinish() || (!req.contains("=="));

	}

	// Quête non complétée ==, != complété
	private static boolean haveQE(String req, Player player) {
		if (player == null)
			return false;
		int id = Integer.parseInt((req.contains("==") ? req.split("==")[1] : req.split("!=")[1]));
		QuestPlayer qp = player.getQuestPersoByQuestId(id);
		if (req.contains("==")) {
			return qp != null && !qp.isFinish();
		} else {
			return qp == null || qp.isFinish();
		}
	}

	// L'étape en cours de la quête doit être : id
	private static boolean haveQEt(String req, Player player) {
		int id = Integer.parseInt((req.contains("==") ? req.split("==")[1] : req.split("!=")[1]));
		Quest_Step qe = Quest_Step.getQuestEtapeById(id);
		if (qe != null) {
			Quest q = qe.getQuestData();
			if (q != null) {
				QuestPlayer qp = player.getQuestPersoByQuest(q);
				if (qp != null) {
					Quest_Step current = q.getQuestEtapeCurrent(qp);
					if (current == null)
						return false;
					if (current.getId() == qe.getId())
						return (req.contains("=="));
				}
			}
		}
		return false;
	}
}
