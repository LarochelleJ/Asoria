package org.area.object;

import java.util.*;
import java.util.regex.Pattern;

import org.area.client.Player;
import org.area.common.ConditionParser;
import org.area.common.SQLManager;
import org.area.common.World;
import org.area.common.World.Couple;
import org.area.event.Event;
import org.area.object.Item.ObjTemplate;
import org.area.quests.Quest;


//import org.icore.common.ConditionParser;


public class NpcTemplate {
    private int _id;
    private int _bonusValue;
    private int _gfxID;
    private int _scaleX;
    private int _scaleY;
    private int _sex;
    private int _color1;
    private int _color2;
    private int _color3;
    private String _acces;
    private int _extraClip;
    private int _customArtWork;
    private HashMap<Integer, Integer> _initQuestions = new HashMap<Integer, Integer>(); // Réutilisation du même NPC mais sur plusieurs maps différente avec questions différentes
    private ArrayList<ObjTemplate> _ventes = new ArrayList<ObjTemplate>();
    private Quest quest;

    //TODO:Baskwo
    private ArrayList<NPC_Exchange> exchange;

    private String _quests;

    public ArrayList<NPC_Exchange> getExchange() {
        return exchange;
    }

    public void setExchange(ArrayList<NPC_Exchange> exchange) {
        this.exchange = exchange;
    }

    public static class NPC_Exchange {
        private Map<Integer, Couple<Integer, Integer>> required = new TreeMap<Integer, Couple<Integer, Integer>>();
        private List<Couple<Integer, Integer>> gift = new ArrayList<Couple<Integer, Integer>>();

        public Map<Integer, Couple<Integer, Integer>> getRequired() {
            return required;
        }

        public void setRequired(Map<Integer, Couple<Integer, Integer>> required) {
            this.required = required;
        }

        public List<Couple<Integer, Integer>> getGift() {
            return gift;
        }

        public void setGift(List<Couple<Integer, Integer>> gift) {
            this.gift = gift;
        }
    }

    public static class NPC_question {
        private int _id;
        private String _reponses;
        private String _args;
        private String _cond;
        private String falseQuestion;

        public NPC_question(int _id, String _reponses, String _args, String _cond, String falseQuestion) {
            this._id = _id;
            this._reponses = _reponses;
            this._args = _args;
            this._cond = _cond;
            this.falseQuestion = falseQuestion;
        }

        public int get_id() {
            return _id;
        }

        public String parseToDQPacket(Player perso) {
            if (!_cond.equals("")) {
                if (!ConditionParser.validConditions(perso, _cond)) {
                    if (this.falseQuestion.contains("|")) {
                        return World.getNPCQuestion(Integer.parseInt(this.falseQuestion.split("|")[0])).parseToDQPacket(perso);
                    } else {
                        return World.getNPCQuestion(Integer.parseInt(this.falseQuestion)).parseToDQPacket(perso);
                    }
                }
            }
            boolean mariage = false;
            boolean maried = false;
            if (_id == 50030 && //Si prêtre
                    (perso.get_curCell().getID() == 282 || perso.get_curCell().getID() == 297)) // Si un des deux à marier
                mariage = World.mariageok();
            if (_id == 50030 && perso.getWife() != 0)//Si prêtre et marrié
                maried = true;
            StringBuilder str = new StringBuilder();
            str.append(_id);
            if (!_args.equals("")) {
                // arguments dynamique
                String arg = "";
                if (_args.equals("[name]")) {
                    arg = perso.getName();
                } else {
                    arg = _args;
                }
                str.append(";").append(parseArguments(arg, perso));
            }
            if (!_reponses.equalsIgnoreCase("")) {
                str.append("|").append(_reponses);
            }
            if (mariage) str.append(";518");
            if (maried) str.append(";2582");
            return str.toString();
        }

        public String getReponses() {
            return _reponses;
        }

        private String parseArguments(String args, Player perso) {
            String arg = args;
            if (arg.contains("event")) {
                if (perso.getEvent() != null && perso.getEvent().getType() == 2 && perso.getEvent().getStatus() == Event.STARTED && perso.getEvent().getEventCache() != null && perso.getEvent().getEventCache().isWaiting() && perso.getEvent().getEventCache().getMapp() == perso.getMap().get_id()) {
                    perso.getEvent().getEventCache().npcFinded(perso);
                    arg = "WIN ! You will have an award.";
                } else if (perso.getEvent() != null && perso.getEvent().getType() == 3 && perso.getEvent().getStatus() == Event.STARTED && perso.getEvent().getEventZaap() != null && perso.getEvent().getEventZaap().isWaiting() && perso.getEvent().getEventZaap().getMap().get_id() == perso.getMap().get_id()) {
                    perso.getEvent().getEventZaap().npcFinded(perso);
                    arg = "GOOD ! \nYour score is " + perso.getEventPoints() + " !";
                } else {
                    arg = "Why do you click on me ? I'm not de pootch, mother fucker, fuck my dog okay !!";
                }
            }
            return arg;
        }

        public void setReponses(String reps) {
            _reponses = reps;
        }
    }

    public static class NPC {
        private NpcTemplate _template;
        private int _cellID;
        private int _guid;
        private byte _orientation;
        private int Move = 0;
        private int CellidBase = -1;
        private String _text[] = null;

        public NPC(NpcTemplate temp, int guid, int cell, byte o, int move, String text) {
            _template = temp;
            _guid = guid;
            _cellID = cell;
            _orientation = o;
            Move = move;
            _text = text.split("\\|");
        }

        public NpcTemplate get_template() {
            return _template;
        }

        public int get_cellID() {
            return _cellID;
        }

        public int get_guid() {
            return _guid;
        }

        public int get_orientation() {
            return _orientation;
        }

        public String parseGM() {
            String sock = "";
            sock = sock + "+";
            sock = sock + _cellID + ";";
            sock = sock + _orientation + ";";
            sock = sock + "0;";
            sock = sock + _guid + ";";
            sock = sock + _template.get_id() + ";";
            sock = sock + "-4;";
            String taille = "";
            if (_template.get_scaleX() == _template.get_scaleY()) {
                taille = "" + _template.get_scaleY();
            } else {
                taille = _template.get_scaleX() + "x" + _template.get_scaleY();
            }
            sock = sock + _template.get_gfxID() + "^" + taille + ";";
            sock = sock + _template.get_sex() + ";";
            sock = sock + (_template.get_color1() != -1 ? Integer.toHexString(_template.get_color1()) : "-1") + ";";
            sock = sock + (_template.get_color2() != -1 ? Integer.toHexString(_template.get_color2()) : "-1") + ";";
            sock = sock + (_template.get_color3() != -1 ? Integer.toHexString(_template.get_color3()) : "-1") + ";";
            sock = sock + _template.get_acces() + ";";
            sock = sock + (_template.get_extraClip() != -1 ? Integer.valueOf(_template.get_extraClip()) : "") + ";";
            sock = sock + _template.get_customArtWork();
            return sock;
        }

        public String parseGM(Player p) {
            String sock = "";
            sock = sock + "+";
            sock = sock + _cellID + ";";
            sock = sock + _orientation + ";";
            sock = sock + "0;";
            sock = sock + _guid + ";";
            sock = sock + _template.get_id() + ";";
            sock = sock + "-4;";
            String taille = "";
            int extraClip = _template.get_extraClip();
            if (extraClip == 4) {
                if (p.getQuestPersoByQuest(_template.getQuest()) != null) {
                    extraClip = -1;
                }
            }
            if (_template.get_scaleX() == _template.get_scaleY()) {
                taille = "" + _template.get_scaleY();
            } else {
                taille = _template.get_scaleX() + "x" + _template.get_scaleY();
            }
            sock = sock + _template.get_gfxID() + "^" + taille + ";";
            sock = sock + _template.get_sex() + ";";
            sock = sock + (_template.get_color1() != -1 ? Integer.toHexString(_template.get_color1()) : "-1") + ";";
            sock = sock + (_template.get_color2() != -1 ? Integer.toHexString(_template.get_color2()) : "-1") + ";";
            sock = sock + (_template.get_color3() != -1 ? Integer.toHexString(_template.get_color3()) : "-1") + ";";
            sock = sock + _template.get_acces() + ";";
            sock = sock + (extraClip != -1 ? extraClip : "") + ";";
            sock = sock + _template.get_customArtWork();
            return sock;
        }

        public String parseGMperco(String GuildName) {
            String sock = "+";
            sock += _cellID + ";";
            sock += "1;";//Orientation
            sock += "0;";
            sock += "-6;";//guid
            sock += "1f,2m;";
            sock += "-6" + ";";//type = NPC
            sock += "6000^125;";
            sock += "91;";
            sock += GuildName + ";9,qjtz,q,6y7ke";
            /*sock += _template.get_acces()+";";
            sock += (_template.get_extraClip()!=-1?(_template.get_extraClip()):(""))+";";
			sock += _template.get_customArtWork();*/
            return sock;
        }

        public void setCellID(int id) {
            _cellID = id;
        }

        public void setOrientation(byte o) {
            _orientation = o;
        }

        public int getMove() {
            return Move;
        }

        public void setMove(int move) {
            Move = move;
        }

        public int getCellidBase() {
            return CellidBase;
        }

        public void setCellidBase(int cellidBase) {
            CellidBase = cellidBase;
        }

        public String[] get_text() {
            return _text;
        }

        public void set_text(String _text[]) {
            this._text = _text;
        }

    }

    public static class NPC_reponse {
        private int _id;
        private ArrayList<Action> _actions = new ArrayList<Action>();

        public NPC_reponse(int id) {
            _id = id;
        }

        public int get_id() {
            return _id;
        }

        public void addAction(Action act) {
            ArrayList<Action> c = new ArrayList<Action>();
            c.addAll(_actions);
            for (Action a : c) if (a.getID() == act.getID()) _actions.remove(a);
            _actions.add(act);
        }

        public void apply(Player perso) {
            // Gestion des donjons avec plusieurs palliers, dès qu'il applique les actions, on sait qu'il quitte le pallier, donc on efface le checkpoint
            Checkpoint possible = World.checkpoints.get(perso.getMap().get_id());
            if (possible != null) {
                perso.checkpoints.remove(possible.getDonjonID());
            }
            for (Action act : _actions)
                act.apply(perso, null, -1, -1);
        }

        public boolean isAnotherDialog() {
            for (Action curAct : _actions) {
                if (curAct.getID() == 1) //1 = Discours NPC
                    return true;
            }

            return false;
        }
    }

    public NpcTemplate(int _id, int value, int _gfxid, int _scalex, int _scaley,
                       int _sex, int _color1, int _color2, int _color3, String _acces,
                       int clip, int artWork, String questions, String ventes, String quest) {
        super();
        this._id = _id;
        _bonusValue = value;
        _gfxID = _gfxid;
        _scaleX = _scalex;
        _scaleY = _scaley;
        this._quests = quest;
        this._sex = _sex;
        this._color1 = _color1;
        this._color2 = _color2;
        this._color3 = _color3;
        this._acces = _acces;
        _extraClip = clip;
        _customArtWork = artWork;
        String[] lesQuestions = questions.split(Pattern.quote("|"));
        if (lesQuestions.length > 1) {
            for (String question : lesQuestions) {
                String[] infos = question.split(Pattern.quote(","));
                _initQuestions.put(Integer.valueOf(infos[0]), Integer.valueOf(infos[1])); // 0: mapID, 1: initQuestionID
            }
        } else if (!questions.equals("")) {
            _initQuestions.put(0, Integer.valueOf(questions)); // Une seule question
        }
        if (_id == 21215) {
            for (Integer templateId : SQLManager.LOAD_NPC_SHOP_ITEMS()) {
                ObjTemplate temp = World.getObjTemplate(templateId);
                if (temp != null) {
                    _ventes.add(temp);
                }
            }
        } else if (!ventes.equals("")) {
            for (String obj : ventes.split("\\,")) {
                try {
                    int tempID = Integer.parseInt(obj);
                    ObjTemplate temp = World.getObjTemplate(tempID);
                    if (temp == null) continue;
                    _ventes.add(temp);
                } catch (NumberFormatException e) {
                    continue;
                }
                ;
            }
        }
    }

    public int get_id() {
        return _id;
    }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public int get_bonusValue() {
        return _bonusValue;
    }

    public int get_gfxID() {
        return _gfxID;
    }

    public int get_scaleX() {
        return _scaleX;
    }

    public int get_scaleY() {
        return _scaleY;
    }

    public int get_sex() {
        return _sex;
    }

    public int get_color1() {
        return _color1;
    }

    public int get_color2() {
        return _color2;
    }

    public int get_color3() {
        return _color3;
    }

    public String get_acces() {
        return _acces;
    }

    public int get_extraClip() {
        return _extraClip;
    }

    public void set_extraClip(int clip) {
        _extraClip = clip;
    }

    public int get_customArtWork() {
        return _customArtWork;
    }

    public int get_initQuestionID(int mapID) {
        int initQuestion = 0;
        if (_initQuestions.containsKey(mapID)) {
            initQuestion = _initQuestions.get(mapID);
        } else if (_initQuestions.containsKey(0)) { // une seule question
            initQuestion = _initQuestions.get(0);
        }
        return initQuestion;
    }

    public String getItemVendorList() {
        StringBuilder items = new StringBuilder();
        if (_ventes.isEmpty()) return "";
        for (ObjTemplate obj : _ventes) {
            items.append(obj.parseItemTemplateStats(_id == 21215)).append(";").append(getMoneyType()).append("|");
        }
        return items.toString();
    }

    private int getMoneyType() {
        int type = 0;
        int idPnj = get_id();
        if (idPnj == 30226 || (idPnj != 30235 && idPnj > 30233 && idPnj < 30238) || idPnj == 50031 || idPnj == 50099) { // pierres précieuses
            type = 470001;
        } else if (idPnj == 50029) { // Kolizeton
            type = 11022;
        } else if (idPnj == 816) { // jeton event
            type = 1749;
        } else if (idPnj == 50100) { // Relique Déese Anachore
            type = 895783;
        } else if (idPnj == 50075 || idPnj == 30235) { // fragments anachore
            type = 895607;
        } else if (idPnj == 50086) { // kams nowel
            type = 895734;
        } else if (idPnj == 21215) {
            type = 1; // point boutique
        }
        return type;
    }

    public boolean addItemVendor(ObjTemplate T) {
        if (_ventes.contains(T)) return false;
        _ventes.add(T);
        return true;
    }

    public boolean delItemVendor(int tID) {
        ArrayList<ObjTemplate> newVentes = new ArrayList<ObjTemplate>();
        boolean remove = false;
        for (ObjTemplate T : _ventes) {
            if (T.getID() == tID) {
                remove = true;
                continue;
            }
            newVentes.add(T);
        }
        _ventes = newVentes;
        return remove;
    }

    public void setInitQuestion(int q) {
        // TODO Faire cette méthode
        //_initQuestionID = q;
    }

    public String get_quests() {
        return _quests;
    }

    public boolean haveItem(int templateID) {
        for (ObjTemplate curTemp : _ventes) {
            if (curTemp.getID() == templateID)
                return true;
        }

        return false;
    }
}
