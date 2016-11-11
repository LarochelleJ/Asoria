package org.area.object;


import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;


import org.area.client.Player;
import org.area.client.Player.BoostSpellStats;
import org.area.client.Player.Stats;
import org.area.common.Constant;
import org.area.common.Formulas;
import org.area.common.SQLManager;
import org.area.common.SocketManager;
import org.area.common.World;
import org.area.game.GameServer;
import org.area.kernel.Config;
import org.area.object.job.Job;
import org.area.spell.SpellEffect;

public class Item {

    public static class ObjTemplate implements Comparable<Object> {
        private int ID;
        private String StrTemplate;
        private String name;
        private int type;
        private int level;
        private int pod;
        private int Prestige;
        private int prix;
        private int panopID;
        private String conditions;
        private int PACost, POmin, POmax, TauxCC, TauxEC, BonusCC;
        private boolean isTwoHanded;
        private ArrayList<Action> onUseActions = new ArrayList<Action>();
        private long sold;
        private int avgPrice;
        private boolean isArm = false;

        @Override
        public int compareTo(Object arg0) {
            ObjTemplate o = (ObjTemplate) arg0;
            if (o.getLevel() == this.getLevel())
                return 0;
            else if (o.getLevel() > this.getLevel())
                return 1;
            else
                return -1;
        }

        public boolean statsContains(int idEffect) {
            boolean statExist = false;
            String[] effets = StrTemplate.split(",");
            for (String s : effets) {
                if (Integer.parseInt(s.split("#")[0], 16) == idEffect) {
                    statExist = true;
                    break;
                }
            }
            return statExist;
        }

        public ObjTemplate(int id, String strTemplate, String name, int type, int level, int pod, int prix, int panopID, String conditions, String armesInfos, int sold, int avgPrice, int prestige) {
            this.ID = id;
            this.StrTemplate = strTemplate;
            this.name = name;
            this.type = type;
            this.level = level;
            this.pod = pod;
            this.Prestige = prestige;
            this.prix = prix;
            this.panopID = panopID;
            this.conditions = conditions;
            this.PACost = -1;
            this.POmin = 1;
            this.POmax = 1;
            this.TauxCC = 100;
            this.TauxEC = 2;
            this.BonusCC = 0;
            this.sold = sold;
            this.avgPrice = avgPrice;

            try {
                String[] infos = armesInfos.split(";");
                PACost = Integer.parseInt(infos[0]);
                POmin = Integer.parseInt(infos[1]);
                POmax = Integer.parseInt(infos[2]);
                TauxCC = Integer.parseInt(infos[3]);
                TauxEC = Integer.parseInt(infos[4]);
                BonusCC = Integer.parseInt(infos[5]);
                isTwoHanded = infos[6].equals("1");
                setArm(true);
            } catch (Exception e) {
            }
            ;

        }


        public int get_obviType() {
            try {
                for (String sts : StrTemplate.split(",")) {
                    String[] stats = sts.split("#");
                    int statID = Integer.parseInt(stats[0], 16);
                    if (statID == 973) {
                        return Integer.parseInt(stats[3], 16);
                    }
                }
            } catch (Exception e) {
                GameServer.addToLog(e.getMessage());
                return Constant.ITEM_TYPE_OBJET_VIVANT;
            }
            return Constant.ITEM_TYPE_OBJET_VIVANT; //Si erreur on retourne le type de base
        }

        public void addAction(Action A) {
            onUseActions.add(A);
        }

        public boolean isTwoHanded() {
            return isTwoHanded;
        }

        public int getBonusCC() {
            return BonusCC;
        }

        public int getPOmin() {
            return POmin;
        }

        public int getPOmax() {
            return POmax;
        }

        public int getTauxCC() {
            return TauxCC;
        }

        public int getTauxEC() {
            return TauxEC;
        }

        public int getPACost() {
            return PACost;
        }

        public int getID() {
            return ID;
        }

        public int getPrestige() {
            return Prestige;
        }

        public String getStrTemplate() {
            return StrTemplate;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public int getLevel() {
            return level;
        }

        public int getPod() {
            return pod;
        }

        public int getPrix() {
            return prix;
        }

        public int getPanopID() {
            return panopID;
        }

        public String getConditions() {
            return conditions;
        }

        public void setIsTwoHanded(boolean twohanded) {
            isTwoHanded = twohanded;
        }

        public BoostSpellStats getBoostSpellStats(String statsTemplate) {
            BoostSpellStats sstats = new BoostSpellStats();

            if (statsTemplate.equals("") || statsTemplate == null) return sstats;

            String[] splitted = statsTemplate.split(",");
            for (String s : splitted) {
                String[] stats = s.split("#");
                int statID = Integer.parseInt(stats[0], 16);
                if (Constant.isSpellStat(statID)) {
                    try {
                        sstats.addStat(Integer.parseInt(stats[1], 16), statID, Integer.parseInt(stats[3], 16));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        continue;
                    }
                }
            }
            return sstats;
        }

        public Item createNewItem(int qua, boolean useMax, int effet) {

            Item item = new Item(SQLManager.getNextObjetID() + 1, ID, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(StrTemplate, useMax, effet), getEffectTemplate(StrTemplate), getBoostSpellStats(StrTemplate), Prestige);
            return item;
        }

        public Stats generateNewStatsFromTemplate(String statsTemplate, boolean useMax, int effet) {
            Stats itemStats = new Stats(false, null);
            //Si stats Vides
            if (statsTemplate.equals("") || statsTemplate == null) return itemStats;
            String statsTemplates = statsTemplate;
            if (effet == 1 || effet == 3)
                statsTemplates += ",6f#1#0#0#0d0+1";
            if (effet == 2 || effet == 3)
                statsTemplates += ",80#1#0#0#0d0+1";

            String[] splitted = statsTemplates.split(",");
            for (String s : splitted) {
                String[] stats = s.split("#");
                int statID = Integer.parseInt(stats[0], 16);
                if (Constant.isSpellStat(statID)) continue;
                boolean follow = true;

                for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
                    if (a == statID)
                        follow = false;
                if (!follow) continue;//Si c'�tait un effet Actif d'arme

                String jet = "";
                int value = 1;
                try {
                    jet = stats[4];
                    value = Formulas.getRandomJet(jet);
                    if (useMax) {
                        try {
                            //on prend le jet max
                            int min = Integer.parseInt(stats[1], 16);
                            int max = Integer.parseInt(stats[2], 16);
                            value = min;
                            if (max != 0) value = max;
                        } catch (Exception e) {
                            value = Formulas.getRandomJet(jet);
                        }
                        ;
                    }
                } catch (Exception e) {
                }
                ;
                itemStats.addOneStat(statID, value);
            }
            return itemStats;
        }

        public int obtenirJetMaximum(int elementId) {
            int maxJet = 0;
            String[] statsTemplate = StrTemplate.split(",");
            for (String stat : statsTemplate) {
                String[] statsInfos = stat.split("#");
                try {
                    if (Integer.parseInt(statsInfos[0], 16) == elementId) {
                        int min = Integer.parseInt(statsInfos[1], 16);
                        int max = Integer.parseInt(statsInfos[2], 16);
                        if (max != 0) {
                            maxJet = max;
                        } else {
                            maxJet = min;
                        }
                        break;
                    }
                } catch (Exception e) {
                }
            }
            return maxJet;
        }

        public int obtenirPuitMaximumExploitable() {
            int poidPlusGros = 0;
            int poidPlusPetit = Integer.MAX_VALUE;
            String[] statsTemplate = StrTemplate.split(",");
            for (String stat : statsTemplate) {
                String[] statsInfos = stat.split("#");
                try {
                    int statID = Integer.parseInt(statsInfos[0], 16);
                    if (Constant.isSpellStat(statID)) continue;
                    int poid = (int)Constant.obtenirPoidsPuissance(statID);
                    if (poid > poidPlusGros) {
                        poidPlusGros = poid;
                    }
                    if (poid < poidPlusPetit) {
                        poidPlusPetit = poid;
                    }
                } catch (Exception e) {
                }
            }
            return poidPlusGros - poidPlusPetit;
        }

        public ArrayList<SpellEffect> getEffectTemplate(String statsTemplate) {
            ArrayList<SpellEffect> Effets = new ArrayList<SpellEffect>();
            if (statsTemplate.equals("") || statsTemplate == null) return Effets;

            String[] splitted = statsTemplate.split(",");
            for (String s : splitted) {
                String[] stats = s.split("#");
                int statID = Integer.parseInt(stats[0], 16);
                for (int a : Constant.ARMES_EFFECT_IDS) {
                    if (a == statID) {
                        int id = statID;
                        String min = stats[1];
                        String max = stats[2];
                        String jet = stats[4];
                        String args = min + ";" + max + ";-1;-1;0;" + jet;
                        Effets.add(new SpellEffect(id, args, 0, -1));
                    }
                }
            }
            return Effets;
        }

        /**public Stats generateNewStatsFromTemplate2(String statsTemplate, boolean useMax) //@Flow - �tait private avant
        {
            Stats itemStats = new Stats(false, null);
            //Si stats Vides
            if (statsTemplate.equals("") || statsTemplate == null) return itemStats;

            String[] splitted = statsTemplate.split(",");
            for (String s : splitted) {
                String[] stats = s.split("#");
                int statID = Integer.parseInt(stats[0], 16);
                boolean follow = true;

                for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
                    if (a == statID)
                        follow = false;
                if (!follow) continue;//Si c'était un effet Actif d'arme

                String jet = "";
                int value = 1;
                try {
                    jet = stats[4];
                    value = Formulas.getRandomJet(jet);
                    if (useMax) {
                        try {
                            //on prend le jet max
                            int min = Integer.parseInt(stats[1], 16);
                            int max = Integer.parseInt(stats[2], 16);
                            value = min;
                            if (max != 0) value = max;
                        } catch (Exception e) {
                            value = Formulas.getRandomJet(jet);
                        }
                        ;
                    }
                } catch (Exception e) {
                }
                ;
                itemStats.addOneStat(statID, value);
            }
            return itemStats;
        }**/


        public String parseItemTemplateStats() {
            return (this.ID + ";" + StrTemplate);
        }

        public void applyAction(Player perso, Player target, int objID, short cellid) {
            for (Action a : onUseActions) {
                a.apply(perso, target, objID, cellid);
            }
        }

        public int getAvgPrice() {
            return avgPrice;
        }

        public long getSold() {
            return this.sold;
        }

        public synchronized void newSold(int amount, int price) {
            long oldSold = sold;
            sold += amount;
            avgPrice = (int) ((avgPrice * oldSold + price) / sold);
        }

        public boolean isArm() {
            return isArm;
        }

        public void setArm(boolean isArm) {
            this.isArm = isArm;
        }
    }

    protected ObjTemplate template;
    protected int quantity = 1;
    protected int prestige = 0;
    protected int position = Constant.ITEM_POS_NO_EQUIPED;
    protected int guid;
    protected int obvijevan;
    protected int obvijevanLook;
    protected int obviID; //Return :)
    private BoostSpellStats SpellStats = new BoostSpellStats();
    private Player.Stats Stats = new Stats();
    private ArrayList<SpellEffect> Effects = new ArrayList<SpellEffect>();
    private Map<Integer, String> txtStats = new TreeMap<Integer, String>();
    //Speaking Item
    //private boolean isExchangeable = true;
    protected boolean isSpeaking = false;
    protected boolean isPet = false;
    //private Speaking linkedItem = null;
    //private int linkedItem_id = -1;
    //private Speaking linkedItem = null;
    //private boolean isLinked = false;

    // Constructeur pour le chargement des items au lancement du serveur
    public Item(int Guid, int template, int qua, int pos, String strStats, int gprestige) {
        this.guid = Guid;
        this.template = World.getObjTemplate(template);
        this.quantity = qua;
        this.position = pos;
        this.prestige = gprestige;

        Stats = new Stats();
        parseStringToStats(strStats);
    }

    // Pour les pierres d'ames, extended class...
    public Item() {

    }

    public int getObvijevanPos() {
        return obvijevan;
    }

    public void setObvijevanPos(int pos) {
        obvijevan = pos;

    }

    public int getObvijevanLook() {
        return obvijevanLook;
    }

    public void setObvijevanLook(int look) {
        obvijevanLook = look;
    }

    public void setObviLastItem(int look) {
        obviID = look;
    }

    public int getObviID() {
        return obviID;
    }


    public void parseStringToStats(String strStats) {
        String[] split = {};
        try {
            split = strStats.split(",");
        } catch (OutOfMemoryError e) {
        }
        if (split != null && split.length > 0) {
            for (String s : split) {
                try {
                    String[] stats = s.split("#");
                    int statID;
                    try {
                        statID = Integer.parseInt(stats[0], 16);
                    } catch (Exception e) {
                        continue;
                    }

                    //Boost spell stats
                    if (Constant.isSpellStat(statID)) {
                        SpellStats.addStat(Integer.parseInt(stats[1], 16), statID, Integer.parseInt(stats[3], 16));
                        continue;
                    }
                    //Stats sp�cials
                    if (statID == 997 || statID == 996) {
                        txtStats.put(statID, stats[4]);
                        continue;
                    }
                    //Si stats avec Texte (Signature, apartenance, etc)
                    if ((!stats[3].equals("") && !stats[3].equals("0"))) {
                        txtStats.put(statID, stats[3]);
                        continue;
                    }

                    String jet = stats[4];
                    boolean follow = true;
                    for (int a : Constant.ARMES_EFFECT_IDS) {
                        if (a == statID) {
                            int id = statID;
                            String min = stats[1];
                            String max = stats[2];
                            String args = min + ";" + max + ";-1;-1;0;" + jet;
                            Effects.add(new SpellEffect(id, args, 0, -1));
                            follow = false;
                        }
                    }
                    if (!follow) continue;//Si c'�tait un effet Actif d'arme ou une signature

                    int value;
                    try {
                        value = Integer.parseInt(stats[1], 16);
                    } catch (Exception e) {
                        continue;
                    }
                    Stats.addOneStat(statID, value);
                } catch (Exception e) {
                    System.out.println("Item bug stats : " + this.template.getName());
                    e.printStackTrace();
                    continue;
                }
                ;
            }
        }
    }

    public void addTxtStat(int i, String s) {
        txtStats.put(i, s);
    }

    public void setTxtStat(int i, String s) {
        if (txtStats.containsKey(i)) {
            txtStats.remove(i);
        }
        txtStats.put(i, s);
    }

    public String getTxtStat(int i) {
        String stat = "";
        try {
            stat = (String) txtStats.get(Integer.valueOf(i));
        } catch (Exception exception) {
        }
        if (stat == null) {
            return "";
        } else {
            return stat;
        }
    }


    public String getTraquedName() {
        for (Entry<Integer, String> entry : txtStats.entrySet()) {
            if (Integer.toHexString(entry.getKey()).compareTo("3dd") == 0) {

                return entry.getValue();
            }
        }
        return null;
    }

    // Clone objet / Cr�ation nouvel objet
    public Item(int Guid, int template, int qua, int pos, Stats stats, ArrayList<SpellEffect> effects, BoostSpellStats sp, int prestige) {

        /*if (World.getObjets().containsKey(Guid)) {
            this.guid = SQLManager.getNextObjetID() + 1;
        } else {
            this.guid = Guid;
        }*/
        this.template = World.getObjTemplate(template);
        this.quantity = qua;
        this.position = pos;
        this.Stats = stats;
        this.Effects = effects;
        this.SpellStats = sp;
        this.obvijevan = 0;
        this.obvijevanLook = 0;
        this.prestige = prestige;
        this.guid = SQLManager.INSERT_NEW_ITEM(this);

        if (this.guid > -1) { // Item crée en bdd
            try {
                World.addObjet(this, false);
            } catch (Exception e) {
            }
        }
       /* // 3 tentatives de cr�ation
        for (int i = 0; i <= 3; i++) {
            if (SQLManager.INSERT_NEW_ITEM(this)) {
                break;
            } else {
                this.guid = SQLManager.getNextObjetID() + 1;
            }
        }
        try {
            World.addObjet(this, false);
        } catch (Exception e) {
        }*/
    }

    public Player.Stats getStats() {
        return Stats;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            World.removeItem(this.guid);
            SQLManager.DELETE_ITEM(this.guid);
        } else {
            this.quantity = quantity;
            SQLManager.SAVE_ITEM_QUANTITY(this.guid, quantity);
        }
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public ObjTemplate getTemplate(boolean getRealTemplateBehindMimibiote) {
        if (this.getStats().getEffect(616161) > 0 && getRealTemplateBehindMimibiote) {
            try {
                ObjTemplate template = World.getObjTemplate(this.getStats().getEffect(616161));
                if (template != null) {
                    return template;
                }
            } catch (Exception e) {
            }
        }
        return template;
    }

    public int getPrestige() {
        return prestige;
    }

    public int getGuid() {
        return guid;
    }

    public void setGuid(int guid) {
        this.guid = guid;
    }

    public String parseItem() {
        StringBuilder str = new StringBuilder();
        String posi = position == Constant.ITEM_POS_NO_EQUIPED ? "" : Integer.toHexString(position);
        str.append(Integer.toHexString(guid)).append("~").append(Integer.toHexString(template.getID())).append("~").append(Integer.toHexString(quantity)).append("~").append(posi).append("~").append(parseStatsString()).append(";");
        return str.toString();
    }

    public String convertStatsAString() {
        int TemplateType = template.getType();
        if (TemplateType == 83)
            return template.getStrTemplate();
        if ((Stats.getMap().isEmpty()) && (Effects.isEmpty())
                && (txtStats.isEmpty()))
            return "";
        String stats = "";
        boolean first = false;
        for (SpellEffect EH : Effects) {
            if (first)
                stats += ",";
            String[] infos = EH.getArgs().split(";");
            try {
                stats += Integer.toHexString(EH.getEffectID()) + "#" + infos[0] + "#" + infos[1] + "#0#" + infos[5];
            } catch (Exception e) {
                continue;
            }
            first = true;
        }
        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            int statID = (entry.getKey());
            if ((statID == 998) || (statID == 997) || (statID == 996) || (statID == 994) || (statID == 988)
                    || (statID == 987) || (statID == 986) || (statID == 985) || (statID == 983) || (statID == 960) || (statID == 961)
                    || (statID == 962) || (statID == 963) || (statID == 964))
                continue;
            if (first)
                stats += ",";
            String jet = "0d0+" + entry.getValue();
            stats += Integer.toHexString(statID) + "#" + Integer.toHexString((entry.getValue())) + "#0#0#" + jet;
            first = true;
        }
        for (Entry<Integer, String> entry : txtStats.entrySet()) {
            int statID = (entry.getKey());
            if (first)
                stats += ",";
            if ((statID == 800) || (statID == 811) || (statID == 961) || (statID == 962) || (statID == 960)
                    || (statID == 950) || (statID == 951))
                stats += Integer.toHexString(statID) + "#0#0#" + entry.getValue();
            else {
                stats += Integer.toHexString(statID) + "#0#0#0#" + entry.getValue();
            }
            first = true;
        }
        return stats;
    }

    public String parseStatsString() {
        if (getTemplate(false).getType() == 83)    //Si c'est une pierre d'�me vide
            return getTemplate(false).getStrTemplate();

        StringBuilder stats = new StringBuilder();
        boolean isFirst = true;
        for (SpellEffect SE : Effects) {
            if (!isFirst)
                stats.append(",");

            String[] infos = SE.getArgs().split(";");
            try {
                stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ;

            isFirst = false;
        }

        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            if (!isFirst)
                stats.append(",");
            int statID = ((Integer) entry.getKey()).intValue();

            if ((statID == 970) || (statID == 971) || (statID == 972) || (statID == 973) || (statID == 974)) {
                int jet = ((Integer) entry.getValue()).intValue();
                if ((statID == 974) || (statID == 972) || (statID == 970))
                    stats.append(Integer.toHexString(statID)).append("#0#0#").append(Integer.toHexString(jet));
                else {
                    stats.append(Integer.toHexString(statID)).append("#0#0#").append(jet);
                }
                if (statID == 973) setObvijevanPos(jet);
                if (statID == 972) setObvijevanLook(jet);
                if (statID == 970) setObviLastItem(jet);
            } else {
                String jet = "0d0+" + entry.getValue();
                stats.append(Integer.toHexString(statID)).append("#");
                stats.append(Integer.toHexString(((Integer) entry.getValue()).intValue())).append("#0#0#").append(jet);
            }
            //String jet = "0d0+"+entry.getValue();
            //stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue()));
            //stats.append("#0#0#").append(jet);
            isFirst = false;
        }
        for (Entry<Integer, Map<Integer, Integer>> entry : SpellStats.getAllEffects().entrySet()) {
            if (entry == null || entry.getValue() == null) continue;
            for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
                if (!isFirst) stats.append(",");
                stats.append(Integer.toHexString(stat.getKey())).append("#").append(Integer.toHexString(entry.getKey())).append("#0#").append(Integer.toHexString(stat.getValue())).append("#0d0+").append(entry.getKey());
                isFirst = false;
            }
        }

        for (Entry<Integer, String> entry : txtStats.entrySet()) {
            if (!isFirst)
                stats.append(",");

            if (entry.getKey() == Constant.CAPTURE_MONSTRE) {
                stats.append(Integer.toHexString(entry.getKey())).append("#0#0#").append(entry.getValue());
            } else {
                stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
            }
            isFirst = false;
        }
        return stats.toString();
    }

    public String parseStatsStringSansUserObvi() {
        return parseStatsStringSansUserObvi(false);
    }

    public String parseStatsStringSansUserObvi(boolean isObj) {
        if (getTemplate(false).getType() == 83)    //Si c'est une pierre d'�me vide
            return getTemplate(false).getStrTemplate();

        StringBuilder stats = new StringBuilder();
        boolean isFirst = true;
        for (SpellEffect SE : Effects) {
            if (!isFirst)
                stats.append(",");

            String[] infos = SE.getArgs().split(";");
            try {
                stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ;

            isFirst = false;
        }

        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            if (!isFirst)
                stats.append(",");
            String jet = "0d0+" + entry.getValue();
            stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue()));
            stats.append("#0#0#").append(jet);
            isFirst = false;
        }
        if (!isObj) {
            for (Entry<Integer, Map<Integer, Integer>> entry : SpellStats.getAllEffects().entrySet()) {
                if (entry == null || entry.getValue() == null) continue;
                for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
                    if (!isFirst) stats.append(",");
                    stats.append(Integer.toHexString(stat.getKey())).append("#").append(Integer.toHexString(entry.getKey())).append("#0#").append(Integer.toHexString(stat.getValue())).append("#0d0+").append(entry.getKey());
                    isFirst = false;
                }
            }
        }

        for (Entry<Integer, String> entry : txtStats.entrySet()) {
            if (!isFirst)
                stats.append(",");

            if (entry.getKey() == Constant.CAPTURE_MONSTRE) {
                stats.append(Integer.toHexString(entry.getKey())).append("#0#0#").append(entry.getValue());
            } else {
                stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
            }
            isFirst = false;
        }
        return stats.toString();
    }

    public String parseToSave() {
        return parseStatsStringSansUserObvi();
    }

    public String obvijevanOCO_Packet(int pos) {
        String strPos = String.valueOf(pos);
        if (pos == -1) strPos = "";
        String upPacket = "OCO";
        upPacket = upPacket + Integer.toHexString(getGuid()) + "~";
        upPacket = upPacket + Integer.toHexString(getTemplate(false).getID()) + "~";
        upPacket = upPacket + Integer.toHexString(getQuantity()) + "~";
        upPacket = upPacket + strPos + "~";
        upPacket = upPacket + parseStatsString();
        return upPacket;
    }

    public void obvijevanNourir(Item obj) {
        if (obj == null)
            return;
        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            if (entry.getKey().intValue() != 974) // on ne boost que la stat de l'exp�rience de l'obvi
                continue;
            if (entry.getValue().intValue() > 500) // si le boost a une valeur sup�rieure � 500 (irr�aliste)
                return;
            entry.setValue(10000); // valeur d'origine + ObjLvl / 32
            // s'il mange un obvi, on r�cup�re son exp�rience
            /*if (obj.getTemplate().getID() == getTemplate().getID()) {
				for(Map.Entry<Integer, Integer> ent : obj.getStats().getMap().entrySet()) {
					if (entry.getKey().intValue() != 974) // on ne consid�re que la stat de l'exp�rience de l'obvi
						continue; 
					entry.setValue(Integer.valueOf(entry.getValue().intValue() + Integer.valueOf(ent.getValue().intValue())));
				}
			}*/
        }
    }

    public void obvijevanChangeStat(int statID, int val) {
        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            if (((Integer) entry.getKey()).intValue() != statID) continue;
            entry.setValue(Integer.valueOf(val));
        }
    }

    public void removeAllObvijevanStats() {
        setObvijevanPos(0);
        Player.Stats StatsSansObvi = new Stats();
        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            int statID = ((Integer) entry.getKey()).intValue();
            if ((statID == 970) || (statID == 971) || (statID == 972) || (statID == 973) || (statID == 974))
                continue;
            StatsSansObvi.addOneStat(statID, ((Integer) entry.getValue()).intValue());
        }
        Stats = StatsSansObvi;
    }

    public void removeAll_ExepteObvijevanStats() {
        setObvijevanPos(0);
        Player.Stats StatsSansObvi = new Stats();
        for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
            int statID = ((Integer) entry.getKey()).intValue();
            if ((statID != 971) && (statID != 972) && (statID != 973) && (statID != 974))
                continue;
            StatsSansObvi.addOneStat(statID, ((Integer) entry.getValue()).intValue());
        }
        Stats = StatsSansObvi;
        this.SpellStats = new BoostSpellStats();
    }

    public String getObvijevanStatsOnly() {
        Item obj = getCloneObjet(this, 1);
        obj.removeAll_ExepteObvijevanStats();
        return obj.parseStatsStringSansUserObvi(true);
    }
	
	/*public String parseToSave()
	{
		return parseStatsString();
	}*/


    public boolean hasSpellBoostStats() {
        return this.SpellStats.haveStats();
    }

    public String parseFMStatsString(String statsstr, Item obj, int add, boolean negatif) {
        StringBuilder stats = new StringBuilder("");
        boolean isFirst = true;
        for (SpellEffect SE : obj.Effects) {
            if (!isFirst) {
                stats.append(",");
            }

            String[] infos = SE.getArgs().split(";");
            try {
                stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ;

            isFirst = false;
        }

        for (Entry<Integer, Integer> entry : obj.Stats.getMap().entrySet()) {
            if (!isFirst) {
                stats.append(",");
            }
            if (Integer.toHexString(entry.getKey()).compareTo(statsstr) == 0) {
                int newstats = 0;
                if (negatif) {
                    newstats = entry.getValue() - add;
                    if (newstats < 1) {
                        continue;
                    }
                } else {
                    newstats = entry.getValue() + add;
                }
                stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue() + add)).append("#0#0#").append("0d0+").append(newstats);
            } else {
                stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue())).append("#0#0#").append("0d0+").append(entry.getValue());
            }
            isFirst = false;
        }

        for (Entry<Integer, String> entry : obj.txtStats.entrySet()) {
            if (!isFirst) {
                stats.append(",");
            }
            stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
            isFirst = false;
        }
        return stats.toString();
    }

    public String parseFMEchecStatsString(Item obj, double poid) {
        StringBuilder stats = new StringBuilder("");
        boolean isFirst = true;
        for (SpellEffect SE : obj.Effects) {
            if (!isFirst) {
                stats.append(",");
            }

            String[] infos = SE.getArgs().split(";");
            try {
                stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ;

            isFirst = false;
        }

        for (Entry<Integer, Integer> entry : obj.Stats.getMap().entrySet()) {
            //En cas d'echec les stats n�gatives Chance,Agi,Intel,Force,Portee,Vita augmentes
            int newstats = 0;

            if (entry.getKey() == 152
                    || entry.getKey() == 154
                    || entry.getKey() == 155
                    || entry.getKey() == 157
                    || entry.getKey() == 116
                    || entry.getKey() == 153) {
                float a = (float) ((entry.getValue() * poid) / 100);
                if (a < 1) {
                    a = 1;
                }
                float chute = (float) (entry.getValue() + a);
                newstats = (int) Math.floor(chute);
                //On limite la chute du n�gatif a sont maximum
                if (newstats > Job.getBaseMaxJet(obj.getTemplate(true).getID(), Integer.toHexString(entry.getKey()))) {
                    newstats = Job.getBaseMaxJet(obj.getTemplate(true).getID(), Integer.toHexString(entry.getKey()));
                }
            } else {
                if (entry.getKey() == 127 || entry.getKey() == 101) {
                    continue;//PM, pas de n�gatif ainsi que PA
                }
                float chute = (float) (entry.getValue() - ((entry.getValue() * poid) / 100));
                newstats = (int) Math.floor(chute);
            }
            if (newstats < 1) {
                continue;
            }
            if (!isFirst) {
                stats.append(",");
            }
            stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(newstats)).append("#0#0#").append("0d0+").append(newstats);
            isFirst = false;
        }

        for (Entry<Integer, String> entry : obj.txtStats.entrySet()) {
            if (!isFirst) {
                stats.append(",");
            }
            stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
            isFirst = false;
        }
		/*if (is_linked())
		{
		if(!isFirst)stats+=",";
		stats += linkedItem.parse_speakingStates();
		isFirst = false;
		}*/
        return stats.toString();
    }

    public Stats generateNewStatsFromTemplate(String statsTemplate, boolean useMax) {
        Stats itemStats = new Stats(false, null);
        //Si stats Vides
        if (statsTemplate.equals("") || statsTemplate == null) {
            return itemStats;
        }

        String[] splitted = statsTemplate.split(",");
        for (String s : splitted) {
            String[] stats = s.split("#");
            int statID = Integer.parseInt(stats[0], 16);
            boolean follow = true;

            for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
            {
                if (a == statID) {
                    follow = false;
                }
            }
            if (!follow) {
                continue;//Si c'�tait un effet Actif d'arme
            }
            String jet = "";
            int value = 1;
            try {
                jet = stats[4];
                value = Formulas.getRandomJet(jet);
                if (useMax) {
                    try {
                        //on prend le jet max
                        int min = Integer.parseInt(stats[1], 16);
                        int max = Integer.parseInt(stats[2], 16);
                        value = min;
                        if (max != 0) {
                            value = max;
                        }
                    } catch (Exception e) {
                        value = Formulas.getRandomJet(jet);
                    }
                }
            } catch (Exception e) {
            }
            itemStats.addOneStat(statID, value);
        }
        return itemStats;
    }

    public void setStats(Stats SS) {
        Stats = SS;
    }

    public static int getPoidOfActualItem(String statsTemplate)//Donne le poid de l'item actuel
    {
        int poid = 0;
        int somme = 0;
        String[] splitted = statsTemplate.split(",");
        for (String s : splitted) {
            String[] stats = s.split("#");
            int statID = Integer.parseInt(stats[0], 16);
            boolean follow = true;

            for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
            {
                if (a == statID) {
                    follow = false;
                }
            }
            if (!follow) {
                continue;//Si c'�tait un effet Actif d'arme
            }
            String jet = "";
            int value = 1;
            try {
                jet = stats[4];
                value = Formulas.getRandomJet(jet);
                try {
                    //on prend le jet max
                    int min = Integer.parseInt(stats[1], 16);
                    int max = Integer.parseInt(stats[2], 16);
                    value = min;
                    if (max != 0) {
                        value = max;
                    }
                } catch (Exception e) {
                    value = Formulas.getRandomJet(jet);
                }
                ;
            } catch (Exception e) {
            }
            ;

            int multi = 1;
            if (statID == 118 || statID == 126 || statID == 125 || statID == 119 || statID == 123 || statID == 158 || statID == 174)//Force,Intel,Vita,Agi,Chance,Pod,Initiative
            {
                multi = 1;
            } else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)//Domages %,Domage renvoy�,Pi�ge %
            {
                multi = 2;
            } else if (statID == 124 || statID == 176)//Sagesse,Prospec
            {
                multi = 3;
            } else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)//R� Feu, Air, Eau, Terre, Neutre
            {
                multi = 4;
            } else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)//R� % Feu, Air, Eau, Terre, Neutre
            {
                multi = 5;
            } else if (statID == 225)//Pi�ge
            {
                multi = 15;
            } else if (statID == 178 || statID == 112)//Soins,Dommage
            {
                multi = 20;
            } else if (statID == 115 || statID == 182)//Cri,Invoc
            {
                multi = 30;
            } else if (statID == 117)//PO
            {
                multi = 50;
            } else if (statID == 128)//PM
            {
                multi = 90;
            } else if (statID == 111)//PA
            {
                multi = 100;
            }
            poid = value * multi; //poid de la carac
            somme += poid;
        }
        return somme;
    }

    public static int getPoidOfBaseItem(int i)//Donne le poid de l'item actuel
    {

        int poid = 0;
        int somme = 0;
        ObjTemplate t = World.getObjTemplate(i);
        String[] splitted = t.getStrTemplate().split(",");

        if (t.getStrTemplate().isEmpty()) {
            return 0;
        }
        for (String s : splitted) {
            String[] stats = s.split("#");
            int statID = Integer.parseInt(stats[0], 16);
            boolean follow = true;

            for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
            {
                if (a == statID) {
                    follow = false;
                }
            }
            if (!follow) {
                continue;//Si c'�tait un effet Actif d'arme
            }
            String jet = "";
            int value = 1;
            try {
                jet = stats[4];
                value = Formulas.getRandomJet(jet);
                try {
                    //on prend le jet max
                    int min = Integer.parseInt(stats[1], 16);
                    int max = Integer.parseInt(stats[2], 16);
                    value = min;
                    if (max != 0) {
                        value = max;
                    }
                } catch (Exception e) {
                    value = Formulas.getRandomJet(jet);
                }
                ;
            } catch (Exception e) {
            }
            ;

            int multi = 1;
            if (statID == 118 || statID == 126 || statID == 125 || statID == 119 || statID == 123 || statID == 158 || statID == 174)//Force,Intel,Vita,Agi,Chance,Pod,Initiative
            {
                multi = 1;
            } else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)//Domages %,Domage renvoy�,Pi�ge %
            {
                multi = 2;
            } else if (statID == 124 || statID == 176)//Sagesse,Prospec
            {
                multi = 3;
            } else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)//R� Feu, Air, Eau, Terre, Neutre
            {
                multi = 4;
            } else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)//R� % Feu, Air, Eau, Terre, Neutre
            {
                multi = 5;
            } else if (statID == 225)//Pi�ge
            {
                multi = 15;
            } else if (statID == 178 || statID == 112)//Soins,Dommage
            {
                multi = 20;
            } else if (statID == 115 || statID == 182)//Cri,Invoc
            {
                multi = 30;
            } else if (statID == 117)//PO
            {
                multi = 50;
            } else if (statID == 128)//PM
            {
                multi = 90;
            } else if (statID == 111)//PA
            {
                multi = 100;
            }
            poid = value * multi; //poid de la carac
            somme += poid;
        }
        return somme;
    }
	/* *********FM SYSTEM********* */

    public ArrayList<SpellEffect> getEffects() {
        return Effects;
    }

    public ArrayList<SpellEffect> cloneEffects() {
        ArrayList<SpellEffect> toReturn = new ArrayList<SpellEffect>();
        for (SpellEffect SE : Effects) {
            SpellEffect temp = new SpellEffect(SE.getEffectID(), SE.getArgs(), SE.getSpell(), SE.getSpellLvl());
            toReturn.add(temp);
        }
        return toReturn;
    }

    public boolean isSameEffects(Item compareTo) {
        int nbSe = this.Effects.size();
        int nbSeCompareTo = compareTo.Effects.size();
        if (nbSe == nbSeCompareTo) {
            int sameSeCount = 0;
            for (SpellEffect SE : Effects) {
                for (SpellEffect SE2 : compareTo.Effects) {
                    if (SE.isSameSpellEffect(SE2)) {
                        sameSeCount++;
                    }
                }
            }
            if (sameSeCount == nbSe) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<SpellEffect> getCritEffects() {
        ArrayList<SpellEffect> effets = new ArrayList<SpellEffect>();
        for (SpellEffect SE : Effects) {
            try {
                boolean boost = true;
                for (int i : Constant.NO_BOOST_CC_IDS) if (i == SE.getEffectID()) boost = false;
                String[] infos = SE.getArgs().split(";");
                if (!boost) {
                    effets.add(SE);
                    continue;
                }
                int min = Integer.parseInt(infos[0], 16) + (boost ? template.getBonusCC() : 0);
                int max = Integer.parseInt(infos[1], 16) + (boost ? template.getBonusCC() : 0);
                if (max < min) max = min;
                String jet = "1d" + (max - min + 1) + "+" + (min - 1);
                //exCode: String newArgs = Integer.toHexString(min)+";"+Integer.toHexString(max)+";-1;-1;0;"+jet;
                //osef du minMax, vu qu'on se sert du jet pour calculer les d�gats
                String newArgs = "0;0;0;-1;0;" + jet;
                effets.add(new SpellEffect(SE.getEffectID(), newArgs, 0, -1));
            } catch (Exception e) {
                continue;
            }
            ;
        }
        return effets;
    }

    public BoostSpellStats getBoostSpellStats() {
        return SpellStats;
    }

    public static Item getCloneObjet(Item obj, int qua) {
        Item ob = new Item(SQLManager.getNextObjetID() + 1, obj.getTemplate(false).getID(), qua, Constant.ITEM_POS_NO_EQUIPED, obj.getStats(), obj.cloneEffects(), new BoostSpellStats(obj.getBoostSpellStats()), obj.getPrestige());
        return ob;
    }

    public void clearStats() {
        //On vide l'item de tous ces effets
        Stats = new Stats();
        Effects.clear();
        txtStats.clear();
    }

    public String parseStringStatsEC_FM(Item obj, double poid) {
        String stats = "";
        boolean first = false;
        for (SpellEffect EH : obj.Effects) {
            if (first)
                stats += ",";
            String[] infos = EH.getArgs().split(";");
            try {
                stats += Integer.toHexString(EH.getEffectID()) + "#" + infos[0] + "#" + infos[1] + "#0#" + infos[5];
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            first = true;
        }
        for (Entry<Integer, Integer> entry : obj.Stats.getMap().entrySet()) {
            int newstats = 0;
            int statID = (entry.getKey());
            int value = (entry.getValue());
            if ((statID == 152) || (statID == 154) || (statID == 155) || (statID == 157) || (statID == 116)
                    || (statID == 153)) {
                float a = (float) (value * poid / 100.0D);
                if (a < 1.0F)
                    a = 1.0F;
                float chute = value + a;
                newstats = (int) Math.floor(chute);
                if (newstats > Job.getBaseMaxJet(obj.getTemplate(true).getID(), Integer.toHexString(entry.getKey()))) {
                    newstats = Job.getBaseMaxJet(obj.getTemplate(true).getID(), Integer.toHexString(entry.getKey()));
                }
            } else {
                if ((statID == 127) || (statID == 101))
                    continue;
                float chute = (float) (value - value * poid / 100.0D);
                newstats = (int) Math.floor(chute);
            }
            if (newstats < 1)
                continue;
            String jet = "0d0+" + newstats;
            if (first)
                stats += ",";
            stats += Integer.toHexString(statID) + "#" + Integer.toHexString(newstats) + "#0#0#" + jet;
            first = true;
        }
        for (Entry<Integer, String> entry : obj.txtStats.entrySet()) {
            if (first)
                stats += ",";
            stats += Integer.toHexString((entry.getKey())) + "#0#0#0#" + entry.getValue();
            first = true;
        }
        return stats;
    }

    public static Item createNewMorphItem(int skinItem, int baseItem, String verif) { //@Flow
        ObjTemplate skin = World.getObjTemplate(skinItem);
        ObjTemplate stats = World.getObjTemplate(baseItem);
        Item obj = new Item(SQLManager.getNextObjetID() + 1, skin.getID(), 1,
                Constant.ITEM_POS_NO_EQUIPED,
                stats.generateNewStatsFromTemplate(
                        verif, true, -1),
                stats.getEffectTemplate(stats.getStrTemplate()),
                stats.getBoostSpellStats(stats.getStrTemplate()),
                stats.getPrestige());
        return obj;
    }

}
