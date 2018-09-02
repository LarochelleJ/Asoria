package org.area.quests;

import org.area.client.Player;
import org.area.common.SQLManager;
import org.area.common.World;

import java.util.HashMap;
import java.util.Map;

public class QuestPlayer {
    private int id;
    private Quest quest = null;
    private boolean finish;
    private Player player;
    private Map<Integer, Quest_Step> questEtapeListValidate = new HashMap<Integer, Quest_Step>();
    private Map<Integer, Short> monsterKill = new HashMap<Integer, Short>();

    public QuestPlayer(int qId, boolean aFinish, int pId, String qEtapeV) {
        this.quest = Quest.getQuestById(qId);
        this.finish = aFinish;
        this.player = World.getPlayer(pId);
        try {
            String[] split = qEtapeV.split(";");
            if (split != null && split.length > 0) {
                for (String loc1 : split) {
                    if (loc1.equalsIgnoreCase(""))
                        continue;
                    Quest_Step qEtape = Quest_Step.getQuestEtapeById(Integer.parseInt(loc1));
                    questEtapeListValidate.put(qEtape.getId(), qEtape);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.id = SQLManager.INSERT_NEW_QUEST_PLAYER(this);
    }

    public QuestPlayer(int aId, int qId, boolean aFinish, int pId, String qEtapeV) {
        this.id = aId;
        this.quest = Quest.getQuestById(qId);
        this.finish = aFinish;
        this.player = World.getPlayer(pId);
        try {
            String[] split = qEtapeV.split(";");
            if (split != null && split.length > 0) {
                for (String loc1 : split) {
                    if (loc1.equalsIgnoreCase(""))
                        continue;
                    Quest_Step qEtape = Quest_Step.getQuestEtapeById(Integer.parseInt(loc1));
                    questEtapeListValidate.put(qEtape.getId(), qEtape);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public Quest getQuest() {
        return quest;
    }

    public boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
        if (this.getQuest() != null && this.getQuest().isDelete()) {
            if (this.player != null && this.player.getQuestPerso() != null && this.player.getQuestPerso().containsKey(this.getId())) {
                this.player.delQuestPerso(this.getId());
                this.deleteQuestPerso();
            }
        } else if (this.getQuest() == null) {
            if (this.player.getQuestPerso().containsKey(this.getId())) {
                this.player.delQuestPerso(this.getId());
                this.deleteQuestPerso();
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isQuestEtapeIsValidate(Quest_Step qEtape) {
        return questEtapeListValidate.containsKey(qEtape.getId());
    }

    public void setQuestEtapeValidate(Quest_Step qEtape) {
        if (!questEtapeListValidate.containsKey(qEtape.getId()))
            questEtapeListValidate.put(qEtape.getId(), qEtape);
    }

    public String getQuestEtapeString() {
        StringBuilder str = new StringBuilder();
        int nb = 0;
        for (Quest_Step qEtape : questEtapeListValidate.values()) {
            nb++;
            str.append(qEtape.getId());
            if (nb < questEtapeListValidate.size())
                str.append(";");
        }
        return str.toString();
    }

    public Map<Integer, Short> getMonsterKill() {
        return monsterKill;
    }

    public boolean overQuestEtape(Quest_Objective qObjectif) {
        int nbrQuest = 0;
        for (Quest_Step qEtape : questEtapeListValidate.values()) {
            if (qEtape.getObjectif() == qObjectif.getId())
                nbrQuest++;
        }
        return qObjectif.getSizeUnique() == nbrQuest;
    }

    public boolean deleteQuestPerso() {
        return SQLManager.DELETE_QUEST_PLAYER(this.id);
    }
}
