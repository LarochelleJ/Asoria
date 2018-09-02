package org.area.quests;

import org.area.common.World;
import org.area.object.NpcTemplate;

import java.util.HashMap;
import java.util.Map;

public class Quest_Step {

    public static Map<Integer, Quest_Step> questStepsList = new HashMap<Integer, Quest_Step>();

    private int id;
    private short type;
    private int objectif;
    private Quest quest = null;
    private Map<Integer, Integer> itemNecessary = new HashMap<Integer, Integer>();        //ItemId,Qua
    private NpcTemplate npc = null;
    private int monsterId;
    private short qua;
    private String condition = null;
    private int validationType;

    public Quest_Step(int aId, int aType, int aObjectif, String itemN, int aNpc, String aMonster, String aCondition, int validationType) {
        this.id = aId;
        this.type = (short) aType;
        this.objectif = aObjectif;
        try {
            if (!itemN.equalsIgnoreCase("")) {
                String[] split = itemN.split(";");
                if (split != null && split.length > 0) {
                    for (String infos : split) {
                        String[] loc1 = infos.split(",");
                        this.itemNecessary.put(Integer.parseInt(loc1[0]), Integer.parseInt(loc1[1]));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.npc = World.getNPCTemplate(aNpc);
        try {
            if (aMonster.contains(",") && !aMonster.equals(0)) {
                String[] loc0 = aMonster.split(",");
                setMonsterId(Integer.parseInt(loc0[0]));
                setQua(Short.parseShort(loc0[1])); // Des quêtes avec le truc vide ! ><
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setValidationType(validationType);
        this.condition = aCondition;
        try {
            Quest_Objective qo = Quest_Objective.getQuestObjectiveById(this.objectif);
            if (qo != null)
                qo.setEtape(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Static function *
     */
    public static Map<Integer, Quest_Step> getQuestStepsList() {
        return questStepsList;
    }

    public static Quest_Step getQuestEtapeById(int id) {
        return questStepsList.get(id);
    }

    public static void setQuestEtape(Quest_Step qEtape) {
        questStepsList.put(qEtape.getId(), qEtape);
    }

    public int getId() {
        return id;
    }

    public short getType() {
        return type;
    }

    public int getObjectif() {
        return objectif;
    }

    public Quest getQuestData() {
        return quest;
    }

    public void setQuestData(Quest aQuest) {
        quest = aQuest;
    }

    public Map<Integer, Integer> getItemNecessaryList() {
        return itemNecessary;
    }

    public NpcTemplate getNpc() {
        return npc;
    }

    public String getCondition() {
        return condition;
    }

    public int getMonsterId() {
        return monsterId;
    }

    public void setMonsterId(int monsterId) {
        this.monsterId = monsterId;
    }

    public short getQua() {
        return qua;
    }

    public void setQua(short qua) {
        this.qua = qua;
    }

    public int getValidationType() {
        return validationType;
    }

    public void setValidationType(int aValidationType) {
        this.validationType = aValidationType;
    }
}
