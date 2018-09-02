package org.area.quests;

import org.area.object.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Quest_Objective {

    public static Map<Integer, Quest_Objective> questObjectivesList = new HashMap<Integer, Quest_Objective>();

    private int id;

    // Récompense objectif
    private int xp;
    private int kamas;
    private Map<Integer, Integer> items = new HashMap<Integer, Integer>();
    private ArrayList<Action> actionList = new ArrayList<Action>();
    private ArrayList<Quest_Step> questSteps = new ArrayList<Quest_Step>();

    public Quest_Objective(int aId, int aXp, int aKamas, String aItems, String aAction) {
        this.id = aId;
        this.xp = aXp;
        this.kamas = aKamas;
        try {
            if (!aItems.equalsIgnoreCase("")) {
                String[] split = aItems.split(";");
                if (split != null && split.length > 0) {
                    for (String loc1 : split) {
                        if (loc1.equalsIgnoreCase(""))
                            continue;
                        if (loc1.contains(",")) {
                            String[] loc2 = loc1.split(",");
                            this.items.put(Integer.parseInt(loc2[0]), Integer.parseInt(loc2[1]));
                        } else {
                            this.items.put(Integer.parseInt(loc1), 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (aAction != null && !aAction.equalsIgnoreCase("")) {
                String[] split = aAction.split(";");
                if (split != null & split.length > 0) {
                    for (String loc1 : split) {
                        String[] loc2 = loc1.split("\\|");
                        int actionId = Integer.parseInt(loc2[0]);
                        String args = loc2[1];
                        Action action = new Action(actionId, args, "-1");
                        actionList.add(action);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Quest_Objective getQuestObjectiveById(int id) {
        return questObjectivesList.get(id);
    }

    public static Map<Integer, Quest_Objective> getQuestObjectivesList() {
        return questObjectivesList;
    }

    public static void setQuest_Objectif(Quest_Objective qObjectif) {
        if (!questObjectivesList.containsKey(qObjectif.getId()) && !questObjectivesList.containsValue(qObjectif)) {
            questObjectivesList.put(qObjectif.getId(), qObjectif);
        }
    }

    public int getId() {
        return id;
    }

    public int getXp() {
        return xp;
    }

    public int getKamas() {
        return kamas;
    }

    public Map<Integer, Integer> getItem() {
        return items;
    }

    public ArrayList<Action> getAction() {
        return actionList;
    }

    public int getSizeUnique() {
        int cpt = 0;
        ArrayList<Integer> id = new ArrayList<Integer>();
        for (Quest_Step qe : questSteps) {
            if (!id.contains(qe.getId())) {
                id.add(qe.getId());
                cpt++;
            }
        }
        return cpt;
    }

    public ArrayList<Quest_Step> getQuestEtapeList() {
        return questSteps;
    }

    public void setEtape(Quest_Step qEtape) {
        if (!questSteps.contains(qEtape))
            questSteps.add(qEtape);
    }
}
