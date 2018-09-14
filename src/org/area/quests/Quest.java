package org.area.quests;

import org.area.client.Player;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.kernel.Config;
import org.area.object.Action;
import org.area.object.Item;
import org.area.object.NpcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Quest {

    /* Static List */
    public static Map<Integer, Quest> questDataList = new HashMap<Integer, Quest>();

    private int id;
    private ArrayList<Quest_Step> questEtapeList = new ArrayList<Quest_Step>();
    private ArrayList<Quest_Objective> questObjectifList = new ArrayList<Quest_Objective>();
    private NpcTemplate npc = null;
    private ArrayList<Action> actions = new ArrayList<Action>();
    private boolean delete;
    private World.Couple<Integer, Integer> condition = null;

    public Quest(int aId, String questEtape, String aObjectif, int aNpc,
                 String action, String args, boolean delete, String condition) {
        this.id = aId;
        this.delete = delete;
        try {
            if (!questEtape.equalsIgnoreCase("")) {
                String[] split = questEtape.split(";");

                if (split != null && split.length > 0) {
                    for (String qEtape : split) {
                        Quest_Step q_Etape = Quest_Step.getQuestEtapeById(Integer.parseInt(qEtape));
                        if (q_Etape != null) {
                            q_Etape.setQuestData(this);
                            questEtapeList.add(q_Etape);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (!aObjectif.equalsIgnoreCase("")) {
                String[] split = aObjectif.split(";");

                if (split != null && split.length > 0) {
                    for (String qObjectif : split) {
                        questObjectifList.add(Quest_Objective.getQuestObjectiveById(Integer.parseInt(qObjectif)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!condition.equalsIgnoreCase("")) {
            try {
                String[] split = condition.split(":");
                if (split != null && split.length > 0) {
                    this.condition = new World.Couple<Integer, Integer>(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.npc = World.getNPCTemplate(aNpc);
        try {
            if (!action.equalsIgnoreCase("") && !args.equalsIgnoreCase("")) {
                String[] arguments = args.split(";");
                int nbr = 0;
                for (String loc0 : action.split(",")) {
                    int actionId = Integer.parseInt(loc0);
                    String arg = arguments[nbr];
                    actions.add(new Action(actionId, arg, -1 + ""));
                    nbr++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Static function *
     */
    public static Map<Integer, Quest> getQuestDataList() {
        return questDataList;
    }

    public static Quest getQuestById(int id) {
        return questDataList.get(id);
    }

    public static void setQuestInList(Quest quest) {
        questDataList.put(quest.getId(), quest);
    }

    public boolean isDelete() {
        return this.delete;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Quest_Objective> getObjectifList() {
        return questObjectifList;
    }

    public NpcTemplate getNpc_Tmpl() {
        return npc;
    }

    public ArrayList<Quest_Step> getQuestEtapeList() {
        return questEtapeList;
    }

    public boolean haveRespectCondition(QuestPlayer qPerso, Quest_Step qEtape) {
        switch (qEtape.getCondition()) {
            case "1": //Valider les etapes d'avant
                boolean loc2 = true;
                for (Quest_Step aEtape : questEtapeList) {
                    if (aEtape == null)
                        continue;
                    if (aEtape.getId() == qEtape.getId())
                        continue;
                    if (!qPerso.isQuestEtapeIsValidate(aEtape))
                        loc2 = false;
                }
                return loc2;

            case "0":
                return true;
        }
        return false;
    }

    public String getGmQuestDataPacket(Player perso) {
        QuestPlayer qPerso = perso.getQuestPersoByQuest(this);
        int loc1 = getObjectifCurrent(qPerso);
        int loc2 = getObjectifPrevious(qPerso);
        int loc3 = getNextObjectif(Quest_Objective.getQuestObjectiveById(getObjectifCurrent(qPerso)));
        StringBuilder str = new StringBuilder();
        str.append(id).append("|");
        str.append(loc1 > 0 ? loc1 : "");
        str.append("|");

        StringBuilder str_prev = new StringBuilder();
        boolean loc4 = true;
        // Il y a une exeption dans le code ici pour la seconde étape de papotage
        for (Quest_Step qEtape : questEtapeList) {
            if (qEtape.getObjectif() != loc1)
                continue;
            if (!haveRespectCondition(qPerso, qEtape))
                continue;
            if (!loc4)
                str_prev.append(";");
            str_prev.append(qEtape.getId());
            str_prev.append(",");
            str_prev.append(qPerso.isQuestEtapeIsValidate(qEtape) ? 1 : 0);
            loc4 = false;
        }
        str.append(str_prev);
        str.append("|");
        str.append(loc2 > 0 ? loc2 : "").append("|");
        str.append(loc3 > 0 ? loc3 : "");
        if (npc != null) {
            str.append("|");
            str.append(npc.get_initQuestionID(perso.getMap().get_id())).append("|");
        }
        return str.toString();
    }

    public Quest_Step getQuestEtapeCurrent(QuestPlayer qPerso) {
        for (Quest_Step qEtape : getQuestEtapeList()) {
            if (!qPerso.isQuestEtapeIsValidate(qEtape))
                return qEtape;
        }
        return null;
    }

    public int getObjectifCurrent(QuestPlayer qPerso) {
        for (Quest_Step qEtape : questEtapeList) {
            if (qPerso.isQuestEtapeIsValidate(qEtape))
                continue;
            return qEtape.getObjectif();
        }
        return 0;
    }

    public int getObjectifPrevious(QuestPlayer qPerso) {
        if (questObjectifList.size() == 1)
            return 0;
        else {
            int previousqObjectif = 0;
            for (Quest_Objective qObjectif : questObjectifList) {
                if (qObjectif.getId() == getObjectifCurrent(qPerso))
                    return previousqObjectif;
                else
                    previousqObjectif = qObjectif.getId();
            }
        }
        return 0;
    }

    public int getNextObjectif(Quest_Objective qO) {
        if (qO == null)
            return 0;
        for (Quest_Objective qObjectif : questObjectifList) {
            if (qObjectif.getId() == qO.getId()) {
                int index = questObjectifList.indexOf(qObjectif);
                if (questObjectifList.size() <= index + 1)
                    return 0;
                return questObjectifList.get(index + 1).getId();
            }
        }
        return 0;
    }

    public void applyQuest(Player perso) {
        if (this.condition != null) {
            switch (this.condition.first) {
                case 1: // Niveau
                    if (perso.getLevel() < this.condition.second) {
                        SocketManager.GAME_SEND_POPUP(perso, "Vous n'avez pas le niveau pour apprendre la quête !");
                        return;
                    }
                    break;
            }
        }
        QuestPlayer qPerso = new QuestPlayer(id, false, perso.getGuid(), "");
        if (qPerso.getId() < 0) { // Erreur bdd
            SocketManager.GAME_SEND_POPUP(perso, "Une erreur est survenue, la quête n'a pas été appris !");
            return;
        }
        perso.addQuestPerso(qPerso);
        SocketManager.GAME_SEND_Im_PACKET(perso, "054;" + id);
        SocketManager.GAME_CLEAR_NPC_EXTRACLIP(perso, qPerso.getQuest().getNpc_Tmpl().get_id());


        if (!actions.isEmpty()) {
            for (Action aAction : actions) {
                aAction.apply(perso, perso, -1, -1);
            }
        }

        SQLManager.SAVE_PERSONNAGE(perso, false);
    }

    public void updateQuestData(Player perso, boolean validation, int type) {
        QuestPlayer qPerso = perso.getQuestPersoByQuest(this);
        for (Quest_Step qEtape : questEtapeList) {
            if (qEtape.getValidationType() != type)
                continue;

            boolean refresh = false;
            if (qPerso.isQuestEtapeIsValidate(qEtape)) //On a déjà validé l'étape on passe
                continue;

            if (qEtape.getObjectif() != getObjectifCurrent(qPerso))
                continue;

            if (!haveRespectCondition(qPerso, qEtape))
                continue;

            if (validation)
                refresh = true;
            int npcID = -1;
            if (perso.get_isTalkingWith() != 0) {
                NpcTemplate.NPC n = perso.getMap().getNPC(perso.get_isTalkingWith());
                if (n != null) {
                    npcID = n.get_template().get_id();
                }
            }
            boolean isTalkingWithNpc = qEtape.getNpc() != null ? (npcID == qEtape.getNpc().get_id()) : false;
            switch (qEtape.getType()) {

                case 3://Donner item au pnj
                    if (isTalkingWithNpc) {
                        for (Map.Entry<Integer, Integer> entry : qEtape.getItemNecessaryList().entrySet()) {
                            if (perso.hasItemTemplate(entry.getKey(), entry.getValue())) { //Il a l'item et la quantité
                                perso.removeByTemplateID(entry.getKey(), entry.getValue()); //On supprime donc
                                refresh = true;
                            }
                        }
                    }
                    break;

                case 0:
                case 1://Aller voir %
                case 9://Retourner voir %
                    if (qEtape.getCondition().equalsIgnoreCase("1")) { //Valider les questEtape avant
                        if (isTalkingWithNpc) {
                            if (haveRespectCondition(qPerso, qEtape)) {
                                refresh = true;
                            }
                        }
                    } else {
                        if (isTalkingWithNpc)
                            refresh = true;
                    }
                    break;

                case 6: // monstres
                    for (Map.Entry<Integer, Short> entry : qPerso.getMonsterKill().entrySet())
                        if (entry.getKey() == qEtape.getMonsterId() && entry.getValue() >= qEtape.getQua())
                            refresh = true;
                    break;

                case 10://Ramener prisonnier TODO Plus tard
                    /*if (isTalkingWithNpc) {
                        GameObject follower = perso.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR);
                        if (follower != null) {
                            Map<Integer, Integer> itemNecessaryList = qEtape.getItemNecessaryList();
                            for (Map.Entry<Integer, Integer> entry2 : itemNecessaryList.entrySet()) {
                                if (entry2.getKey() == follower.getTemplate().getId()) {
                                    refresh = true;
                                    perso.setMascotte(0);
                                }
                            }
                        }
                    }*/
                    break;
            }

            if (refresh) {
                Quest_Objective ansObjectif = Quest_Objective.getQuestObjectiveById(getObjectifCurrent(qPerso));
                qPerso.setQuestEtapeValidate(qEtape);
                SocketManager.GAME_SEND_Im_PACKET(perso, "055;" + id);
                if (haveFinish(qPerso, ansObjectif)) {
                    SocketManager.GAME_SEND_Im_PACKET(perso, "056;" + id);
                    applyButinOfQuest(perso, qPerso, ansObjectif);
                    qPerso.setFinish(true);
                } else {
                    if (getNextObjectif(ansObjectif) != 0) {
                        if (qPerso.overQuestEtape(ansObjectif))
                            applyButinOfQuest(perso, qPerso, ansObjectif);
                    }
                }
                SQLManager.SAVE_PERSONNAGE(perso, false);
            }
        }
    }

    public boolean haveFinish(QuestPlayer qPerso, Quest_Objective qO) {
        return qPerso.overQuestEtape(qO) && getNextObjectif(qO) == 0;
    }

    public void applyButinOfQuest(Player perso, QuestPlayer qPerso, Quest_Objective ansObjectif) {
        long aXp = 0;

        if ((aXp = ansObjectif.getXp()) > 0) { //Xp a donner
            perso.addXp(aXp * ((int) Config.RATE_PVM));
            SocketManager.GAME_SEND_Im_PACKET(perso, "08;" + (aXp * ((int) Config.RATE_PVM)));
            SocketManager.GAME_SEND_STATS_PACKET(perso);
        }

        if (ansObjectif.getItem().size() > 0) { //Item a donner
            for (Map.Entry<Integer, Integer> entry : ansObjectif.getItem().entrySet()) {
                Item.ObjTemplate objT = World.getObjTemplate(entry.getKey());
                int qua = entry.getValue();
                Item obj = objT.createNewItem(qua, false, -1);
                if (perso.addObjet(obj, true))
                    World.addObjet(obj, true);
                SocketManager.GAME_SEND_Im_PACKET(perso, "021;" + qua + "~" + objT.getID());
            }
        }

        int aKamas = 0;
        if ((aKamas = ansObjectif.getKamas()) > 0) { //Kams a donner
            perso.set_kamas(perso.get_kamas() + (long) aKamas);
            SocketManager.GAME_SEND_Im_PACKET(perso, "045;" + aKamas);
            SocketManager.GAME_SEND_STATS_PACKET(perso);
        }

        if (getNextObjectif(ansObjectif) != ansObjectif.getId()) { //On passe au nouveau objectif on applique les actions
            for (Action a : ansObjectif.getAction()) {
                a.apply(perso, null, 0, 0);
            }
        }

    }

    public int getQuestEtapeByObjectif(Quest_Objective qObjectif) {
        int nbr = 0;
        for (Quest_Step qEtape : getQuestEtapeList()) {
            if (qEtape.getObjectif() == qObjectif.getId())
                nbr++;
        }
        return nbr;
    }

    public ArrayList<Action> getActions() {
        return actions;
    }

    public void setActions(ArrayList<Action> actions) {
        this.actions = actions;
    }
}
