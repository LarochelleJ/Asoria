package org.area.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.area.fight.Fight;
import org.area.fight.Fighter;
import org.area.game.GameServer;
import org.area.game.GameThread.GameAction;
import org.area.kernel.*;
import org.area.object.Maps.Case;
import org.area.spell.Spell;
import org.area.spell.SpellEffect;
import org.area.spell.Spell.SortStats;
import org.area.timers.PeriodicRunnableCancellable;


public class IA {

    public static class IAThread implements Runnable {
        private Fight _fight;
        private Fighter _fighter;
        private static boolean stop = false;
        private static long _startT = 0;
        private Thread _t;

        public IAThread(Fighter fighter, Fight fight) {
            _fighter = fighter;
            _fight = fight;
            _startT = (long) System.currentTimeMillis() / 1000;
            _t = new Thread(Main.THREAD_IA, this);
            _t.setDaemon(true);
            try {
                _t.start();
            } catch (OutOfMemoryError e) {
                Logs.addToDebug("OutOfMemory dans le IA");
                e.printStackTrace();
                try {
                    Main.listThreads(true);
                } catch (Exception eq) {
                }
                try {
                    _t.start();
                } catch (OutOfMemoryError e1) {
                }
            }
        }

		 /*public IAThread(Fighter fighter, Fight fight)
            {
		        _fighter = fighter;
		        _fight = fight;
		        _t = new Thread(this);
		        _t.setDaemon(true);
		        _t.start();
		    }*/

        public Thread getThread() {
            return _t;
        }

        public void run() {
            stop = false;

            if (_fighter.getMob() == null) {
                if (_fighter.isDouble()) {
                    apply_type5(_fighter, _fight);
                    new PeriodicRunnableCancellable(1000, TimeUnit.MILLISECONDS) {
                        public void run() {
                            if (_fight.get_curAction().equals("")) {
                                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(EndTurn #001", "0BF9B7");
                                _fight.endTurn();
                                this.cancel();
                            }
                        }
                    };
                } else if (_fighter.isPerco()) {
                    apply_typePerco(_fighter, _fight);
                    new PeriodicRunnableCancellable(1000, TimeUnit.MILLISECONDS) {
                        public void run() {
                            if (_fight.get_curAction().equals("")) {
                                _fight.endTurn();
                                this.cancel();
                            }
                        }
                    };
                } else if (_fighter.isPrisme()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    _fight.endTurn();

                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    ;
                    //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(EndTurn #002", "0BF9B7");
                    _fight.endTurn();
                }
            } else if (_fighter.getMob().getTemplate() == null) {
                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(EndTurn #003", "0BF9B7");
                _fight.endTurn();
            } else {
                try {
                    if (_fighter.estInvocationControllable()) {
                        apply_nothing(_fighter);
                    } else {
                        switch (_fighter.getMob().getTemplate().getIAType()) {
                            case 0:// Ne rien faire
                                apply_type0(_fighter, _fight);
                                break;
                            case 1:// Attaque, Buff soi-mï¿½me, Buff Alliï¿½s, Avancer
                                // vers ennemis.
                                apply_type1(_fighter, _fight);
                                break;
                            case 2:// Soutien
                                apply_type2(_fighter, _fight);
                                break;
                            case 3:// Avancer vers Alliï¿½s, Buff Alliï¿½s, Buff sois
                                // mï¿½me
                                apply_type3(_fighter, _fight);
                                break;
                            case 4:// Attaque, Fuite, Buff Alliï¿½s, Buff sois mï¿½me
                                apply_type4(_fighter, _fight);
                                break;
                            case 5:// Avancer vers ennemis
                                apply_type5(_fighter, _fight);
                                break;
                            case 6:// IA type invocations
                                apply_type6(_fighter, _fight);
                                break;
                            case 7: //IA type Tonneau
                                apply_type7(_fighter, _fight);
                                break;
                            case 8: //IA type Cadran Xelor
                                apply_type8(_fighter, _fight);
                                break;
                            case 9: //IA type Pandawasta
                                apply_type9(_fighter, _fight);
                            case 10: //IA Surpuissante, invocation + buff + fuite
                                apply_type10(_fighter, _fight);
                                break;
                            case 11: //IA Fourbe, attaque + fuite
                                apply_type11(_fighter, _fight);
                                break;
                            case 12: // Vortex
                                apply_type12(_fighter, _fight);
                                break;
                            case 13: // Dragonnet rouge Attaque + fuit
                                apply_type13(_fighter, _fight);
                                break;
                            default: // F.
                                apply_type1(_fighter, _fight);
                                break;
                        }
                    }
                    /**try {
                     Thread.sleep(2000); // C'est si lent dofus =O | Et ta soeur elle est lente ?
                     } catch (InterruptedException e) {
                     }
                     ;
                     _fight.endTurn(); **/
					/*new PeriodicRunnableCancellable(1000, TimeUnit.MILLISECONDS) {
						public void run() {
							if (_fight.get_curAction().equals("")) {
								SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(EndTurn #004", "0BF9B7");
								_fight.endTurn();
								this.cancel();
							}
						}
					};*/ // @Flow - Source des passages de tours
                    try {
                        if (_fighter.getMob().getTemplate().getIAType() == 0) {
                            Thread.sleep(250L);
                        } else {
                            Thread.sleep(500L);
                        }
                    } catch (InterruptedException localInterruptedException3) {
                    }
                    if (!_fighter.isDead()) {
                        _fight.endTurn();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private static void apply_type0(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                stop = true;
            }
        }

        private static void apply_nothing(Fighter F) {
            while (F.canPlay()) {
            }
        }

        /*private static void apply_type1(Fighter F, Fight fight) {
            try
            {
            int must_stop = 0;
            Fighter T = null;
            while (must_stop < 3 && F.canPlay()) {
                if (Thread.interrupted())throw new InterruptedException();
                int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
                Fighter T2 = getNearestFriend(fight, F); // Amis
                if (T == null || T.isDead()) {
                    T = getBestEnnemy(fight, F);
                }
                if (T == null)
                    return;
                if (PDVPER > 15) {
                    int attack = attackTargetIfPossible(fight, F, T); // Lors de l'invocation, rien ne sort ici et fige ici @Flow
                    //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(DEBUG) Type d'attaque "+ attack +"", "0BF9B7");
                    if (attack != 0)// Attaque
                    {
                        if (attack == 5)
                            must_stop = 3;// EC
                        if (!moveNearIfPossible(fight, F, T)) {
                            if (!buffIfPossible(fight, F, F))// auto-buff
                            {
                                if (!HealIfPossible(fight, F, false))// soin
                                                                        // alliï¿½
                                {
                                    if (!buffIfPossible(fight, F, T2))// buff
                                                                        // alliï¿½
                                    {
                                        if (!moveNearIfPossible(fight, F, T))// avancer
                                        {
                                            if (!invocIfPossible(fight, F))// invoquer
                                            {
                                                stop = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (!HealIfPossible(fight, F, true))// auto-soin
                    {
                        int attack = attackTargetIfPossible(fight, F, T);
                        if (attack != 0)// Attaque
                        {
                            if (attack == 5)
                                must_stop = 3;// EC
                            if (!buffIfPossible(fight, F, F))// auto-buff
                            {
                                if (!HealIfPossible(fight, F, false))// soin
                                                                        // alliï¿½
                                {
                                    if (!buffIfPossible(fight, F, T2))// buff
                                                                        // alliï¿½
                                    {
                                        if (!invocIfPossible(fight, F)) {
                                            if (!moveFarIfPossible(fight, F))// fuite
                                            {
                                                stop = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (stop) {
                    must_stop++;
                    stop = false;
                    T = null;
                }
                try{
                    Thread.sleep(100);
                }
                catch(Exception e){Logs.addToDebug("Interrompu dans le Repos Thread 1 : "+((long) (System.currentTimeMillis()/1000-_startT)));return;}
            }
            }
            catch(InterruptedException ie)
            {
                Logs.addToDebug("Interrompu dans le apply_type1 : "+((long) (System.currentTimeMillis()/1000-_startT)));
                return;
            }
        }
*/
        private static void apply_type1(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                //int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
                Fighter T = getNearestEnnemy(fight, F); // Ennemis
                Fighter T2 = getNearestFriend(fight, F); // Amis
                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Chargement des attaques", "0BF9B7");
                int attack = 0;
                try {
                    attack = attackIfPossible(fight, F); // F.
                } catch (Exception e) {
                }

                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(DEBUG) Type d'attaque "+ attack +"", "0BF9B7");

                if (attack != 0)//Attaque
                {
                    if (attack == 5) {
                        stop = true;//EC
                        break;
                    }
                    //if (!moveToAttackIfPossible(fight, F,null)) {
                    if (!buffIfPossible(fight, F, F))//auto-buff
                    {
                        if (!HealIfPossible(fight, F, false))//soin alli
                        {
                            if (!buffIfPossible(fight, F, T2))//buff allié
                            {
                                if (T == null || !moveNearIfPossible(fight, F, T))//avancer
                                {
                                    if (!invocIfPossible(fight, F))//invoquer
                                    {
                                        stop = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // }

        private static void apply_type10(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                //int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
                //Fighter T = getNearestEnnemy(fight, F); // Ennemis
                Fighter T2 = getNearestFriend(fight, F); // Amis
                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Chargement des attaques", "0BF9B7");
                //int attack = attackIfPossibleFlow(fight, F); // F.
                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(DEBUG) Type d'attaque "+ attack +"", "0BF9B7");

                if (!invocIfPossible(fight, F))//invoquer
                {
                    if (!buffIfPossible(fight, F, T2))//buff alliï¿½
                    {
                        if (!moveFarIfPossible(fight, F)) // fuite
                        {
                            stop = true;
                            break;
                        }
                    }
                }
            }
        }

        private static void apply_type11(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                //int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
                //Fighter T = getNearestEnnemy(fight, F); // Ennemis
                //Fighter T2 = getNearestFriend(fight, F); // Amis
                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Chargement des attaques", "0BF9B7");
                int attack = attackIfPossibleFlow(fight, F); // F.
                //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("(DEBUG) Type d'attaque "+ attack +"", "0BF9B7");

                if (attack != 0)//Attaque
                {
                    if (attack == 5) {
                        stop = true;//EC
                        break;
                    }
                    if (!moveToAttackIfPossible(fight, F, null)) {
                        if (!moveFarIfPossible(fight, F)) // fuite
                        {
                            stop = true;
                            break;
                        }
                    }
                }
            }
        }

        private static void apply_type12(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                Fighter T = getNearestEnnemy(fight, F); // Ennemis
                Fighter T2 = getNearestFriend(fight, F); // Amis
                int attack = 0;

                if (!buffIfPossible(fight, F, F))//auto-buff
                {
                    if (!invocIfPossible(fight, F))//invoquer
                    {
                        if (!HealIfPossible(fight, F, false))//soin alliï¿½
                        {
                            if (!buffIfPossible(fight, F, T2))//buff alliï¿½
                            {
                                try {
                                    attack = attackIfPossible(fight, F); // F.
                                } catch (Exception e) {
                                }
                                if (attack != 0)//Attaque
                                {
                                    if (attack == 5) {
                                        stop = true;//EC
                                        break;
                                    }
                                    if (T == null || !moveNearIfPossible(fight, F, T))//avancer
                                    {
                                        stop = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private static void apply_type13(Fighter F, Fight fight) {
            Fighter T = getNearestEnnemy(fight, F); // Ennemis
            boolean hasFighted = false;
            while (!stop && F.canPlay()) {
                int attack = 0;
                try {
                    attack = attackIfPossible(fight, F);
                } catch (Exception e) {
                }
                if (attack != 0)// Il n'y pas d'attaque
                {
                    if (attack == 5) { // EC
                        stop = true;
                    }
                    if (!hasFighted) { // n'a pas combatu on tente de se rapprocher
                        if (T == null || !moveNearIfPossible(fight, F, T)) {
                            stop = true;
                        }
                    } else { // a combatu on s'enfuit plus rien ï¿½ faire
                        if (!moveFarIfPossible(fight, F)) {
                            stop = true;
                        }
                    }
                } else { // attaque possible
                    hasFighted = true;
                }
            }
        }

        private static boolean moveToAttackIfPossible(Fight fight, Fighter fighter, HashMap<Fighter, Integer> coups) {
            ArrayList<Integer> cells = Pathfinding.getListCaseFromFighter(fight, fighter);
            if (cells == null) {
                return false;
            }
            int distMin = Pathfinding.getDistanceBetween(fight.get_map(), fighter.get_fightCell().getID(), getNearestEnnemy(fight, fighter).get_fightCell().getID());
            ArrayList<SortStats> sorts = getLaunchableSort(fighter, fight, distMin);
            if (sorts == null) {
                return false;
            }
            ArrayList<Fighter> targets = getPotentialTarget(fight, fighter, sorts, coups);
            if (targets == null) {
                return false;
            }

            int CellDest = 0;
            boolean found = false;
            boolean invisible = false;
            for (int i : cells) {
                for (SortStats S : sorts) {
                    for (Fighter T : targets) {
                        if (T.isHide()) { //si il est invisible
                            invisible = true;
                            continue;
                        }
                        if (fight.CanCastSpell(fighter, S, T.get_fightCell(), i)) {
                            CellDest = i;
                            found = true;
                        }
                        int targetVal = getBestTargetZone(fight, fighter, S, i);
                        if (targetVal > 0) {
                            int nbTarget = targetVal / 1000;
                            int cellID = targetVal - nbTarget * 1000;
                            if (fight.get_map().getCase(cellID) != null) {
                                if (fight.CanCastSpell(fighter, S, fight.get_map().getCase(cellID), i)) {
                                    CellDest = i;
                                    found = true;
                                }
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            //si aucuns joueurs valides et qu'il y en a un invisible, on se dÃ©place alÃ©atoirement
            if (!found && invisible) {
                cells = Pathfinding.getFullPMListCase(fight, fighter); //pour utiliser tout les PM
                if (cells == null)
                    return false;
                int i = Formulas.getRandomValue(0, cells.size() - 1);
                CellDest = cells.get(i);
                if (Config.DEBUG) {
                    System.out.println("Tentative de dÃ©placement alÃ©atoire");
                }
            }
            if (CellDest == 0) {
                return false;
            }
            ArrayList<Case> path = Pathfinding.getShortestPathBetween(fight.get_map(), fighter.get_fightCell().getID(), CellDest, 0);
            if (path == null) {
                return false;
            }
            String pathstr = "";
            try {
                int curCaseID = fighter.get_fightCell().getID();
                int curDir = 0;
                for (Case c : path) {
                    char d = Pathfinding.getDirBetweenTwoCase(curCaseID, c.getID(), fight.get_map(), true);
                    if (d == 0) {
                        return false;//Ne devrait pas arriver :O
                    }
                    if (curDir != d) {
                        if (path.indexOf(c) != 0) {
                            pathstr += CryptManager.cellID_To_Code(curCaseID);
                        }
                        pathstr += d;
                    }
                    curCaseID = c.getID();
                }
                if (curCaseID != fighter.get_fightCell().getID()) {
                    pathstr += CryptManager.cellID_To_Code(curCaseID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ;
            //Crï¿½ation d'une GameAction
            GameAction GA = new GameAction(0, 1, "");
            GA._args = pathstr;
            boolean result = fight.fighterDeplace(fighter, GA);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            return result;

        }

        private static ArrayList<SortStats> getLaunchableSort(Fighter fighter, Fight fight, int distMin) {
            ArrayList<SortStats> sorts = new ArrayList<SortStats>();
            if (fighter.getMob() == null) {
                return null;
            }
            for (Entry<Integer, SortStats> S : fighter.getMob().getSpells().entrySet()) {
                if (S.getValue().getPACost(null) > fighter.getCurPA(fight))//si PA insuffisant
                {
                    continue;
                }
                //if(S.getValue().getMaxPO() + fighter.getCurPM(fight) < distMin && S.getValue().getMaxPO() != 0)// si po max trop petite
                //continue;
                if (!org.area.fight.object.LaunchedSort.coolDownGood(fighter, S.getValue().getSpellID()))// si cooldown ok
                {
                    continue;
                }
                if (S.getValue().getMaxLaunchbyTurn(fighter) - org.area.fight.object.LaunchedSort.getNbLaunch(fighter, S.getValue().getSpellID()) <= 0 && S.getValue().getMaxLaunchbyTurn(fighter) > 0)// si nb tours ok
                {
                    continue;
                }
                if (calculInfluence(fight, S.getValue(), fighter, fighter) >= 0)// si sort pas d'attaque
                {
                    continue;
                }
                sorts.add(S.getValue());
            }
            ArrayList<SortStats> finalS = TriInfluenceSorts(fight, fighter, sorts);

            return finalS;
        }

        private static ArrayList<SortStats> TriInfluenceSorts(Fight fight, Fighter fighter, ArrayList<SortStats> sorts) {
            if (sorts == null) {
                return null;
            }

            ArrayList<SortStats> finalSorts = new ArrayList<SortStats>();
            Map<Integer, SortStats> copie = new TreeMap<Integer, SortStats>();

            for (SortStats S : sorts) {
                copie.put(S.getSpellID(), S);
            }

            int curInfl = 0;
            int curID = 0;
            char type;

            while (copie.size() > 0) {
                curInfl = 0;
                curID = 0;
                type = '9';
                for (Entry<Integer, SortStats> S : copie.entrySet()) {
                    int infl = -calculInfluence(fight, S.getValue(), fighter, fighter);
                    if (S.getValue().getPorteeType().charAt(0) == 'C' || type != 'C') {
                        if (S.getValue().getPorteeType().charAt(0) != 'C' && type != 'C' || (S.getValue().getPorteeType().charAt(0) == 'C' && type == 'C')) {
                            if (infl > curInfl) {
                                curID = S.getValue().getSpellID();
                                curInfl = infl;
                            }
                        }
                        else if (S.getValue().getPorteeType().charAt(0) == 'C' && type != 'C') {
                            curID = S.getValue().getSpellID();
                            curInfl = infl;
                            type = 'C';
                        }
                    }
                }
                if (curID == 0 || curInfl == 0) {
                    break;
                }
                finalSorts.add(copie.get(curID));
                copie.remove(curID);
            }

            return finalSorts;
        }

        private static ArrayList<Fighter> getPotentialTarget(Fight fight, Fighter fighter, ArrayList<SortStats> sorts, HashMap<Fighter, Integer> coups) {
            try {
                ArrayList<Fighter> targets = new ArrayList<Fighter>();
                int distMax = 0;
                for (SortStats S : sorts) {
                    if (S.getMaxPO(fighter) > distMax) {
                        distMax = S.getMaxPO(fighter);
                    }
                }
                distMax += fighter.getCurPM(fight);
                Map<Integer, Fighter> potentialsT = getLowHpEnnemyList(fight, fighter);
                if (potentialsT == null || potentialsT.isEmpty()) {
                    return new ArrayList<Fighter>();
                }
                for (Entry<Integer, Fighter> T : potentialsT.entrySet()) {
                    int dist = Pathfinding.getDistanceBetween(fight.get_map(), fighter.get_fightCell().getID(), T.getValue().get_fightCell().getID());
                    if (dist < distMax && coups.get(T) < 2) {
                        targets.add(T.getValue());
                    }
                }

                return targets;
            } catch (Exception e) {
                return new ArrayList<Fighter>();
            }
        }

        private static void apply_type2(Fighter F, Fight fight) {
            try {
                int must_stop = 0;
                boolean modeAttack = false;
                Fighter T = getNearestFriend(fight, F);
                Fighter E = null;
                Fighter C = null;
                if (E == null || E.isDead()) {
                    E = getBestEnnemy(fight, F);
                }
                if (Pathfinding.getDistanceBetween(fight.get_map(), F
                        .get_fightCell().getID(), T.get_fightCell().getID()) < 4 || E.isHide())
                    modeAttack = true;
                while (must_stop < 2 && F.canPlay()) {
                    if (Thread.interrupted()) throw new InterruptedException();
                    if (modeAttack) {
                        T = getNearestFriend(fight, F);
                        if (E == null || E.isDead()) {
                            E = getBestEnnemy(fight, F);
                        }
                        if (E.isHide()) {
                            C = T;
                        }
                        else {
                            C = E;
                        }
                        if (!HealIfPossible(fight, F, false))// soin alliï¿½
                        {
                            if (!buffIfPossible(fight, F, T))// buff alliï¿½
                            {
                                if (!HealIfPossible(fight, F, true))// auto-soin
                                {
                                    if (!buffIfPossible(fight, F, F))// auto-buff
                                    {
                                        if (!invocIfPossible(fight, F)) {
                                            if (!moveNearIfPossible(fight, F, C))// Avancer
                                            // vers
                                            // ennemie
                                            {
                                                int attack = attackTargetIfPossible(
                                                        fight, F, E);
                                                if (attack == 5)
                                                    must_stop = 3;// EC
                                                if (attack != 0)// Attaque
                                                {
                                                    stop = true;// EC
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        T = getNearestFriend(fight, F);
                        if (!HealIfPossible(fight, F, false))// soin alliï¿½
                        {
                            if (!buffIfPossible(fight, F, T))// buff alliï¿½
                            {
                                if (!moveNearIfPossible(fight, F, T))// Avancer vers
                                // allié
                                {
                                    if (!HealIfPossible(fight, F, true))// auto-soin
                                    {
                                        if (!buffIfPossible(fight, F, F))// auto-buff
                                        {
                                            if (!invocIfPossible(fight, F)) {
                                                T = getBestEnnemy(fight, F);
                                                int attack = attackIfPossible(
                                                        fight, F);
                                                //int attack = attackTargetIfPossible(
                                                        //fight, F, T);
                                                if (attack != 0)// Attaque
                                                {
                                                    if (attack == 5)
                                                        must_stop = 3;// EC
                                                    if (!moveFarIfPossible(fight, F))// fuite
                                                        stop = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (stop) {
                        must_stop++;
                        stop = false;
                        T = null;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Logs.addToDebug("Interrompu dans le Repos Thread 2 : " + ((long) (System.currentTimeMillis() / 1000 - _startT)));
                        return;
                    }
                }
            } catch (InterruptedException ie) {
                Logs.addToDebug("Interrompu dans le apply_type2 : " + ((long) (System.currentTimeMillis() / 1000 - _startT)));
                return;
            }
        }

        private static void apply_type3(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                Fighter T = getNearestFriend(fight, F);
                if (!moveNearIfPossible(fight, F, T))// Avancer vers alliï¿½
                {
                    if (!HealIfPossible(fight, F, false))// soin alliï¿½
                    {
                        if (!buffIfPossible(fight, F, T))// buff alliï¿½
                        {
                            if (!HealIfPossible(fight, F, true))// auto-soin
                            {
                                if (!invocIfPossible(fight, F)) {
                                    if (!buffIfPossible(fight, F, F))// auto-buff
                                    {
                                        stop = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private static void apply_type4(Fighter F, Fight fight) // IA propre La
        // Folle
        {
            while (!stop && F.canPlay()) {
                Fighter T = getNearestEnnemy(fight, F);
                if (T == null)
                    return;
                int attack = attackIfPossible(fight, F);
                if (attack != 0)// Attaque
                {
                    if (attack == 5)
                        stop = true;// EC
                    if (!moveFarIfPossible(fight, F))// fuite
                    {
                        if (!HealIfPossible(fight, F, false))// soin alliï¿½
                        {
                            if (!buffIfPossible(fight, F, T))// buff alliï¿½
                            {
                                if (!HealIfPossible(fight, F, true))// auto-soin
                                {
                                    if (!invocIfPossible(fight, F)) {
                                        if (!buffIfPossible(fight, F, F))// auto-buff
                                        {
                                            stop = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private static void apply_type5(Fighter F, Fight fight) // IA propre aux
        // ï¿½nus
        {
            while (!stop && F.canPlay()) {
                Fighter T = getNearestEnnemy(fight, F);
                if (T == null)
                    return;

                if (!moveNearIfPossible(fight, F, T))// Avancer vers enemis
                {
                    stop = true;
                }
            }
        }

        private static void apply_type6(Fighter F, Fight fight) {
            while (!stop && F.canPlay()) {
                if (!invocIfPossible(fight, F)) {
                    Fighter T = getNearestFriend(fight, F);
                    if (!HealIfPossible(fight, F, false))// soin alliï¿½
                    {
                        if (!buffIfPossible(fight, F, T))// buff alliï¿½
                        {
                            if (!buffIfPossible(fight, F, F))// buff alliï¿½
                            {
                                if (!HealIfPossible(fight, F, true)) {
                                    int attack = attackIfPossible(fight, F);
                                    if (attack != 0)// Attaque
                                    {
                                        if (attack == 5)
                                            stop = true;// EC
                                        if (!moveFarIfPossible(fight, F))// fuite
                                            stop = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        private static void apply_type7(Fighter F, Fight fight) {//Tonneau
            int chan = 0;
            while (!stop && F.canPlay()) {
                if (++chan >= 5)
                    stop = true;
				/*if (chan > 15)
					return;*/
                try {
                    if (!buffIfPossibleOther(fight, F, F)) {
                        stop = true;
                    }
                } catch (Exception e) {
                    stop = true;
                }
            }
        }


        private static void apply_type8(Fighter fighter, Fight fight) { //Cadran Xel
            int chan = 0;
            while (!stop && fighter.canPlay()) {
                if (++chan >= 12)
                    stop = true;
                if (chan > 15)
                    return;
                Fighter T = getBuff(fight, fighter);
                if (T == null)
                    return;
                int attack = attackIfPossibleOther(fight, fighter);
                while (attack == 0 && !stop) {
                    if (attack == 5)
                        stop = true;
                    attack = attackIfPossibleOther(fight, fighter);
                }
                stop = true;
            }
        }

        private static void apply_type9(Fighter fighter, Fight fight) { //Pandawasta
            int chan = 0;
            while (!stop && fighter.canPlay()) {
                if (++chan >= 12)
                    stop = true;
                if (chan > 15)
                    return;
                if (!buffIfPossibleOther(fight, fighter, fighter)) {
                    Fighter T = getBuff(fight, fighter);
                    if (T == null)
                        return;
                    int attack = attackIfPossibleOther(fight, fighter);
                    while (attack == 0 && !stop) {
                        if (attack == 5)
                            stop = true;
                        attack = attackIfPossibleOther(fight, fighter);
                    }
                    if (!Afunction(fight, fighter, T))
                        stop = true;
                }
            }
        }

        private static void apply_typePerco(Fighter F, Fight fight) {
            try {
                while (!stop && F.canPlay()) {
                    Fighter T = getNearestEnnemy(fight, F);
                    if (T == null)
                        return;
                    int attack = attackIfPossiblePerco(fight, F);
                    if (attack != 0)// Attaque
                    {
                        if (attack == 5)
                            stop = true;// EC
                        if (!moveFarIfPossible(fight, F))// fuite
                        {
                            if (!HealIfPossiblePerco(fight, F, false))// soin
                            // alliï¿½
                            {
                                if (!buffIfPossiblePerco(fight, F, T))// buff
                                // alliï¿½
                                {
                                    if (!HealIfPossiblePerco(fight, F, true))// auto-soin
                                    {
                                        if (!buffIfPossiblePerco(fight, F, F))// auto-buff
                                        {
                                            stop = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*private static boolean moveFarIfPossible(Fight fight, Fighter F) {

            int dist[] = { 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000,
                    1000, 1000 }, cell[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            for (int i = 0; i < 10; i++) {
                for (Fighter f : fight.getFighters(3)) {

                    if (f.isDead())
                        continue;
                    if (f == F || f.getTeam() == F.getTeam())
                        continue;
                    int cellf = f.get_fightCell().getID();
                    if (cellf == cell[0] || cellf == cell[1]
                            || cellf == cell[2] || cellf == cell[3]
                            || cellf == cell[4] || cellf == cell[5]
                            || cellf == cell[6] || cellf == cell[7]
                            || cellf == cell[8] || cellf == cell[9])
                        continue;
                    int d = 0;
                    d = Pathfinding.getDistanceBetween(fight.get_map(), F
                                    .get_fightCell().getID(), f.get_fightCell()
                                    .getID());
                    if (d == 0)
                        continue;
                    if (d < dist[i]) {
                        dist[i] = d;
                        cell[i] = cellf;
                    }
                    if (dist[i] == 1000) {
                        dist[i] = 0;
                        cell[i] = F.get_fightCell().getID();
                    }
                }
            }
            if (dist[0] == 0)
                return false;
            int dist2[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            int PM = F.getCurPM(fight), caseDepart = F.get_fightCell().getID(), destCase = F
                    .get_fightCell().getID();
            for (int i = 0; i <= PM; i++) {
                if (destCase > 0)
                    caseDepart = destCase;
                int curCase = caseDepart;
                curCase += 15;
                int infl = 0, inflF = 0;
                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(),
                            curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF && curCase > 0 && curCase < 478
                        && testCotes(destCase, curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }

                curCase = caseDepart + 14;
                infl = 0;

                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(),
                            curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF && curCase > 0 && curCase < 478
                        && testCotes(destCase, curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }

                curCase = caseDepart - 15;
                infl = 0;
                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(),
                            curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF && curCase > 0 && curCase < 478
                        && testCotes(destCase, curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }

                curCase = caseDepart - 14;
                infl = 0;
                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(),
                            curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF && curCase > 0 && curCase < 478
                        && testCotes(destCase, curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }
            }
            System.out.println("Test MOVEFAR : cell = " + destCase);
            if (destCase < 0 || destCase > 478
                    || destCase == F.get_fightCell().getID()
                    || !fight.get_map().getCase(destCase).isWalkable(false))
                return false;
            if (F.getPM() <= 0)
                return false;
            ArrayList<Case> path = Pathfinding.getShortestPathBetween(
                    fight.get_map(), F.get_fightCell().getID(), destCase, 0);
            if (path == null)
                return false;

            // DEBUG PATHFINDING
            /*
             * System.out.println("DEBUG PATHFINDING:");
             * System.out.println("startCell: "+F.get_fightCell().getID());
             * System.out.println("destinationCell: "+cellID);
             *
             * for(Case c : path) {
             * System.out.println("Passage par cellID: "+c.getID
             * ()+" walk: "+c.isWalkable(true)); }
             */
/*
			ArrayList<Case> finalPath = new ArrayList<Case>();
			for (int a = 0; a < F.getPM(); a++) {
				if (path.size() == a)
					break;
				finalPath.add(path.get(a));
			}
			String pathstr = "";
			int curCaseID = F.get_fightCell().getID();
			int curDir = 0;
			for (Case c : finalPath) {
				char d = Pathfinding.getDirBetweenTwoCase(curCaseID, c.getID(),
						fight.get_map(), true);
				if (d == 0)
					return false;// Ne devrait pas arriver :O
				if (curDir != d) {
					if (finalPath.indexOf(c) != 0)
						pathstr += CryptManager.cellID_To_Code(curCaseID);
					pathstr += d;
				}
				curCaseID = c.getID();
			}
			if (curCaseID != F.get_fightCell().getID())
				pathstr += CryptManager.cellID_To_Code(curCaseID);
			// Crï¿½ation d'une GameAction
			GameAction GA = new GameAction(0, 1, "");
			GA._args = pathstr;
			boolean result = fight.fighterDeplace(F, GA);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			return result;

		}
		*/
        private static boolean moveFarIfPossible(Fight fight, Fighter F) { // @Flow - Must change this
            if (fight == null || F == null)
                return true;
            if (fight.get_map() == null)
                return true;
            //On crï¿½er une liste de distance entre ennemi et de cellid, nous permet de savoir si un ennemi est collï¿½ a nous
            int dist[] = {1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000}, cell[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            for (int i = 0; i < 10; i++)//on repete 10 fois pour les 10 joueurs ennemis potentielle
            {
                for (Fighter f : fight.getFighters(3)) {

                    if (f.isDead())
                        continue;
                    if (f == F || f.getTeam() == F.getTeam())
                        continue;
                    int cellf = f.get_fightCell().getID();
                    if (cellf == cell[0] || cellf == cell[1] || cellf == cell[2]
                            || cellf == cell[3] || cellf == cell[4]
                            || cellf == cell[5] || cellf == cell[6]
                            || cellf == cell[7] || cellf == cell[8]
                            || cellf == cell[9])
                        continue;
                    int d = 0;
                    d = Pathfinding.getDistanceBetween(fight.get_map(), F.get_fightCell().getID(), f.get_fightCell().getID());
                    if (d < dist[i]) {
                        dist[i] = d;
                        cell[i] = cellf;
                    }
                    if (dist[i] == 1000) {
                        dist[i] = 0;
                        cell[i] = F.get_fightCell().getID();
                    }
                }
            }
            //if(dist[0] == 0)return false;//Si ennemi "collï¿½"

            int dist2[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int PM = F.getCurPM(fight), caseDepart = F.get_fightCell().getID(), destCase = F.get_fightCell().getID();
            ArrayList<Integer> caseUse = new ArrayList<Integer>();
            caseUse.add(caseDepart); // On ne revient pas a sa position de dï¿½part
            for (int i = 0; i <= PM; i++)//Pour chaque PM on analyse la meilleur case a prendre. C'est a dire la plus ï¿½liognï¿½e de tous.
            {
                if (destCase > 0)
                    caseDepart = destCase;
                int curCase = caseDepart;

                /** En +15 **/
                curCase += 15;
                int infl = 0, inflF = 0;
                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(), curCase, cell[a]);//pour chaque ennemi on calcul la nouvelle distance depuis cette nouvelle case (curCase)
                    if (dist2[a] > dist[a])//Si la cellule (curCase) demander et plus distante que la prï¿½cedente de l'ennemi alors on dirrige le mouvement vers elle
                        infl++;
                }

                if (infl > inflF
                        && curCase >= 15
                        && curCase <= 463
                        && testCotes(destCase, curCase)
                        && fight.get_map().getCase(curCase).isWalkable(false)
                        && fight.get_map().getCase(curCase).getFighters().isEmpty()
                        && !caseUse.contains(curCase))//Si l'influence (infl) est la plus forte en comparaison avec inflF on garde la case si celle-ci est valide
                {
                    inflF = infl;
                    destCase = curCase;
                }
                /** En +15 **/

                /** En +14 **/
                curCase = caseDepart + 14;
                infl = 0;

                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(), curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF
                        && curCase >= 15
                        && curCase <= 463
                        && testCotes(destCase, curCase)
                        && fight.get_map().getCase(curCase).isWalkable(false)
                        && fight.get_map().getCase(curCase).getFighters().isEmpty()
                        && !caseUse.contains(curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }
                /** En +14 **/

                /** En -15 **/
                curCase = caseDepart - 15;
                infl = 0;
                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(), curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF
                        && curCase >= 15
                        && curCase <= 463
                        && testCotes(destCase, curCase)
                        && fight.get_map().getCase(curCase).isWalkable(false)
                        && fight.get_map().getCase(curCase).getFighters().isEmpty()
                        && !caseUse.contains(curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }
                /** En -15 **/

                /** En -14 **/
                curCase = caseDepart - 14;
                infl = 0;
                for (int a = 0; a < 10 && dist[a] != 0; a++) {
                    dist2[a] = Pathfinding.getDistanceBetween(fight.get_map(), curCase, cell[a]);
                    if (dist2[a] > dist[a])
                        infl++;
                }

                if (infl > inflF
                        && curCase >= 15
                        && curCase <= 463
                        && testCotes(destCase, curCase)
                        && fight.get_map().getCase(curCase).isWalkable(false)
                        && fight.get_map().getCase(curCase).getFighters().isEmpty()
                        && !caseUse.contains(curCase)) {
                    inflF = infl;
                    destCase = curCase;
                }
                /** En -14 **/
                caseUse.add(destCase);
            }
            if (destCase < 15
                    || destCase > 463
                    || destCase == F.get_fightCell().getID()
                    || !fight.get_map().getCase(destCase).isWalkable(false))
                return false;

            if (F.getPM() <= 0)
                return false;
            ArrayList<Case> path = Pathfinding.getShortestPathBetween(fight.get_map(), F.get_fightCell().getID(), destCase, 0);
            if (path == null)
                return false;
            ArrayList<Case> finalPath = new ArrayList<Case>();
            for (int a = 0; a < F.getCurPM(fight); a++) {
                if (path.size() == a)
                    break;
                finalPath.add(path.get(a));
            }
            String pathstr = "";
            try {
                int curCaseID = F.get_fightCell().getID();
                int curDir = 0;
                for (Case c : finalPath) {
                    char d = Pathfinding.getDirBetweenTwoCase(curCaseID, c.getID(), fight.get_map(), true);
                    if (d == 0)
                        return false;//Ne devrait pas arriver :O
                    if (curDir != d) {
                        if (finalPath.indexOf(c) != 0)
                            pathstr += CryptManager.cellID_To_Code(curCaseID);
                        pathstr += d;
                    }
                    curCaseID = c.getID();
                }
                if (curCaseID != F.get_fightCell().getID())
                    pathstr += CryptManager.cellID_To_Code(curCaseID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Crï¿½ation d'une GameAction
            GameAction GA = new GameAction(0, 1, "");
            GA._args = pathstr;
            boolean result = fight.fighterDeplace(F, GA);

            return result;
        }

        // @Flow - ï¿½ rï¿½viser
        private static boolean testCotes(int cell1, int cell2)//Bord, hors zone. Cell 2 = Cellule oï¿½ on va et cell1 = Cellule oï¿½ nous sommes
        {
            if (cell1 == 15 || cell1 == 44 || cell1 == 73
                    || cell1 == 102 || cell1 == 131 || cell1 == 160
                    || cell1 == 189 || cell1 == 218 || cell1 == 247
                    || cell1 == 276 || cell1 == 305 || cell1 == 334
                    || cell1 == 363 || cell1 == 392 || cell1 == 421
                    || cell1 == 450) {
                if (cell2 == cell1 + 14 || cell2 == cell1 - 15)
                    return false;
            }
            if (cell1 == 28 || cell1 == 57 || cell1 == 86
                    || cell1 == 115 || cell1 == 144 || cell1 == 173
                    || cell1 == 202 || cell1 == 231 || cell1 == 260
                    || cell1 == 289 || cell1 == 318 || cell1 == 347
                    || cell1 == 376 || cell1 == 405 || cell1 == 434
                    || cell1 == 463) {
                if (cell2 == cell1 + 15 || cell2 == cell1 - 14)
                    return false;
            }

            if (cell1 >= 451 && cell1 <= 462) {
                if (cell2 == cell1 + 15 || cell2 == cell1 + 14)
                    return false;
            }
            if (cell1 >= 16 && cell1 <= 27) {
                if (cell2 == cell1 - 15 || cell2 == cell1 - 14)
                    return false;
            }
            return true;
        }

        private static boolean invocIfPossible(Fight fight, Fighter fighter) {
            Fighter nearest = getNearestEnnemy(fight, fighter);
            if (nearest == null)
                return false;
            int nearestCell = Pathfinding.getNearestCellAround(fight.get_map(),
                    fighter.get_fightCell().getID(), nearest.get_fightCell()
                            .getID(), null);
            if (nearestCell == -1)
                return false;
            SortStats spell = getInvocSpell(fight, fighter, nearestCell);
            if (spell == null)
                return false;
            int invoc = fight.tryCastSpell(fighter, spell, nearestCell);
            if (invoc != 0)
                return false;

            return true;
        }

        private static SortStats getInvocSpell(Fight fight, Fighter fighter,
                                               int nearestCell) {
            if (fighter.getMob() == null)
                return null;
            for (Entry<Integer, SortStats> SS : fighter.getMob().getSpells()
                    .entrySet()) {
                if (!fight.CanCastSpell(fighter, SS.getValue(), fight.get_map()
                        .getCase(nearestCell), -1))
                    continue;
                for (SpellEffect SE : SS.getValue().getEffects()) {
                    if (SE.getEffectID() == 181)
                        return SS.getValue();
                }
            }
            return null;
        }

        private static boolean HealIfPossible(Fight fight, Fighter f,
                                              boolean autoSoin)// boolean pour choisir entre auto-soin ou soin
        // alliï¿½
        {
            if (autoSoin && (f.getPDV() * 100) / f.getPDVMAX() > 95)
                return false;
            Fighter target = null;
            SortStats SS = null;
            if (autoSoin) {
                target = f;
                SS = getHealSpell(fight, f, target);
            } else// sï¿½lection joueur ayant le moins de pv
            {
                Fighter curF = null;
                int PDVPERmin = 100;
                SortStats curSS = null;
                for (Fighter F : fight.getFighters(3)) {
                    if (f.isDead())
                        continue;
                    if (F == f)
                        continue;
                    if (F.getTeam() == f.getTeam()) {
                        int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
                        if (PDVPER < PDVPERmin && PDVPER < 95) {
                            int infl = 0;
                            for (Entry<Integer, SortStats> ss : f.getMob()
                                    .getSpells().entrySet()) {
                                if (infl < calculInfluenceHeal(ss.getValue())
                                        && calculInfluenceHeal(ss.getValue()) != 0
                                        && fight.CanCastSpell(f, ss.getValue(),
                                        F.get_fightCell(), -1))// Si le
                                // sort
                                // est
                                // plus
                                // interessant
                                {
                                    infl = calculInfluenceHeal(ss.getValue());
                                    curSS = ss.getValue();
                                }
                            }
                            if (curSS != SS && curSS != null) {
                                curF = F;
                                SS = curSS;
                                PDVPERmin = PDVPER;
                            }
                        }
                    }
                }
                target = curF;
            }
            if (target == null)
                return false;
            if (SS == null)
                return false;
            int heal = fight
                    .tryCastSpell(f, SS, target.get_fightCell().getID());
            if (heal != 0)
                return false;

            return true;
        }

        private static boolean HealIfPossiblePerco(Fight fight, Fighter f,
                                                   boolean autoSoin)// boolean pour choisir entre auto-soin ou soin
        // alliï¿½
        {
            if (autoSoin && (f.getPDV() * 100) / f.getPDVMAX() > 95)
                return false;
            Fighter target = null;
            SortStats SS = null;
            if (autoSoin) {
                target = f;
                SS = getHealSpell(fight, f, target);
            } else// sï¿½lection joueur ayant le moins de pv
            {
                Fighter curF = null;
                int PDVPERmin = 100;
                SortStats curSS = null;
                for (Fighter F : fight.getFighters(3)) {
                    if (f.isDead())
                        continue;
                    if (F == f)
                        continue;
                    if (F.getTeam() == f.getTeam()) {
                        int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
                        if (PDVPER < PDVPERmin && PDVPER < 95) {
                            int infl = 0;
                            for (Entry<Integer, SortStats> ss : World
                                    .getGuild(f.getPerco().GetPercoGuildID())
                                    .getSpells().entrySet()) {
                                if (ss.getValue() == null)
                                    continue;
                                if (infl < calculInfluenceHeal(ss.getValue())
                                        && calculInfluenceHeal(ss.getValue()) != 0
                                        && fight.CanCastSpell(f, ss.getValue(),
                                        F.get_fightCell(), -1))// Si le
                                // sort
                                // est
                                // plus
                                // interessant
                                {
                                    infl = calculInfluenceHeal(ss.getValue());
                                    curSS = ss.getValue();
                                }
                            }
                            if (curSS != SS && curSS != null) {
                                curF = F;
                                SS = curSS;
                                PDVPERmin = PDVPER;
                            }
                        }
                    }
                }
                target = curF;
            }
            if (target == null)
                return false;
            if (SS == null)
                return false;
            int heal = fight
                    .tryCastSpell(f, SS, target.get_fightCell().getID());
            if (heal != 0)
                return false;

            return true;
        }

        private static boolean buffIfPossible(Fight fight, Fighter fighter,
                                              Fighter target) {
            if (target == null)
                return false;
            if (target.getTeam2() != fighter.getTeam2()) return false;
            SortStats SS = getBuffSpell(fight, fighter, target);
            if (SS == null)
                return false;
            int buff = fight.tryCastSpell(fighter, SS, target.get_fightCell()
                    .getID());
            if (buff != 0)
                return false;

            return true;
        }

        private static boolean buffIfPossiblePerco(Fight fight,
                                                   Fighter fighter, Fighter target) {
            if (target == null)
                return false;
            SortStats SS = getBuffSpellPerco(fight, fighter, target);
            if (SS == null)
                return false;
            int buff = fight.tryCastSpell(fighter, SS, target.get_fightCell()
                    .getID());
            if (buff != 0)
                return false;

            return true;
        }

        private static SortStats getBuffSpell(Fight fight, Fighter F, Fighter T) {
            int infl = 0;
            SortStats ss = null;
            for (Entry<Integer, SortStats> SS : F.getMob().getSpells()
                    .entrySet()) {
                if (infl < calculInfluence(fight, SS.getValue(), F, T)
                        && calculInfluence(fight, SS.getValue(), F, T) > 0
                        && fight.CanCastSpell(F, SS.getValue(),
                        T.get_fightCell(), -1))// Si le sort est plus
                // interessant
                {
                    infl = calculInfluence(fight, SS.getValue(), F, T);
                    ss = SS.getValue();
                }
            }
            return ss;
        }

        private static SortStats getBuffSpellPerco(Fight fight, Fighter F,
                                                   Fighter T) {
            int infl = 0;
            SortStats ss = null;
            for (Entry<Integer, SortStats> SS : World
                    .getGuild(F.getPerco().GetPercoGuildID()).getSpells()
                    .entrySet()) {
                if (SS.getValue() == null)
                    continue;
                if (infl < calculInfluence(fight, SS.getValue(), F, T)
                        && calculInfluence(fight, SS.getValue(), F, T) > 0
                        && fight.CanCastSpell(F, SS.getValue(),
                        T.get_fightCell(), -1))// Si le sort est plus
                // interessant
                {
                    infl = calculInfluence(fight, SS.getValue(), F, T);
                    ss = SS.getValue();
                }
            }
            return ss;
        }

        private static SortStats getHealSpell(Fight fight, Fighter F, Fighter T) {
            int infl = 0;
            SortStats ss = null;
            if (F.getMob() == null) return null;
            for (Entry<Integer, SortStats> SS : F.getMob().getSpells()
                    .entrySet()) {
                if (infl < calculInfluenceHeal(SS.getValue())
                        && calculInfluenceHeal(SS.getValue()) != 0
                        && fight.CanCastSpell(F, SS.getValue(),
                        T.get_fightCell(), -1))// Si le sort est plus
                // interessant
                {
                    infl = calculInfluenceHeal(SS.getValue());
                    ss = SS.getValue();
                }
            }
            return ss;
        }

        private static boolean moveNearIfPossible(Fight fight, Fighter F,
                                                  Fighter T) {
            try {
                if (F.getCurPM(fight) <= 0) return false;
                //T.getPersonnage().sendText(""+ F.getCurPM(fight) +" PM");
            } catch (Exception e) {
            }
            if (Pathfinding.isNextTo(F.get_fightCell().getID(), T
                    .get_fightCell().getID()))
                return false;

            if (Config.DEBUG)
                GameServer.addToLog("Tentative d'approche par "
                        + F.getPacketsName() + " de " + T.getPacketsName());

            int cellID = Pathfinding.getNearestCellAround(fight.get_map(), T
                    .get_fightCell().getID(), F.get_fightCell().getID(), null);
            // On demande le chemin plus court
            if (cellID == -1) {
                Map<Integer, Fighter> ennemys = getLowHpEnnemyList(fight, F);
                for (Entry<Integer, Fighter> target : ennemys.entrySet()) {
                    int cellID2 = Pathfinding.getNearestCellAround(
                            fight.get_map(), target.getValue().get_fightCell()
                                    .getID(), F.get_fightCell().getID(), null);
                    if (cellID2 != -1) {
                        cellID = cellID2;
                        break;
                    }
                }
            }
            ArrayList<Case> path = Pathfinding.getShortestPathBetween(
                    fight.get_map(), F.get_fightCell().getID(), cellID, 0);
            if (path == null || path.isEmpty())
                return false;
            // DEBUG PATHFINDING
			/*
			 * System.out.println("DEBUG PATHFINDING:");
			 * System.out.println("startCell: "+F.get_fightCell().getID());
			 * System.out.println("destinationCell: "+cellID);
			 *
			 * for(Case c : path) {
			 * System.out.println("Passage par cellID: "+c.getID
			 * ()+" walk: "+c.isWalkable(true)); }
			 */

            ArrayList<Case> finalPath = new ArrayList<Case>();
            for (int a = 0; a < F.getCurPM(fight); a++) {
                if (path.size() == a)
                    break;
                finalPath.add(path.get(a));
            }
            String pathstr = "";
            int curCaseID = F.get_fightCell().getID();
            int curDir = 0;
            for (Case c : finalPath) {
                char d = Pathfinding.getDirBetweenTwoCase(curCaseID, c.getID(),
                        fight.get_map(), true);
                if (d == 0)
                    return false;// Ne devrait pas arriver :O
                if (curDir != d) {
                    if (finalPath.indexOf(c) != 0)
                        pathstr += CryptManager.cellID_To_Code(curCaseID);
                    pathstr += d;
                }
                curCaseID = c.getID();
            }
            if (curCaseID != F.get_fightCell().getID())
                pathstr += CryptManager.cellID_To_Code(curCaseID);
            // Crï¿½ation d'une GameAction
            GameAction GA = new GameAction(0, 1, "");
            GA._args = pathstr;
            boolean result = fight.fighterDeplace(F, GA);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            return result;
        }

        private static Fighter VerificationSiTacle(Fighter f, Fight fight) {
            ArrayList<Fighter> tmptacle = Pathfinding.getEnemyFighterArround(f.get_fightCell().getID(), fight.get_map(), fight);
            ArrayList<Fighter> tacle = new ArrayList<Fighter>();
            if (tmptacle != null) {
                for (Fighter T : tmptacle) {
                    if (T.isHide()) continue;
                    tacle.add(T);
                }
            }
            if (!tacle.isEmpty()) {
                for (Fighter Z : tacle) {
                    boolean rejet = false;
                    for (Fighter X : tacle) {
                        if (X == Z) continue;
                        if (Z.getMob() != null && X.getMob() == null) rejet = true;
                        if (Z.getMob() == null && X.getMob() == null) {
                            if (X.getPDV() < Z.getPDV()) rejet = true;
                        }
                    }
                    if (!rejet) return Z;
                }
            }
            return null;
        }

        private static int attackIfPossibleOther(Fight fight, Fighter fighter) {
            Map<Integer, Fighter> ennemyList = getMinPDVEnnemy(fight, fighter);
            SortStats SH = null;
            Fighter f = null;
            if (fighter.isState(Constant.ETAT_PORTE)) {
                return 0;
            }
            for (Entry<Integer, Fighter> t : ennemyList.entrySet()) {
                SH = getBestSpell(fight, fighter, t.getValue());
                if (SH != null) {
                    f = t.getValue();
                    break;
                }
            }
            if (f == null || SH == null)
                return 666;
            int attack = fight.tryCastSpell(fighter, SH, f.get_fightCell().getID());
            if (attack != 0)
                return attack;
            return 0;
        }

        private static SortStats getBestSpell(Fight fight, Fighter fighter, Fighter ff) {
            SortStats ss = null;
            ArrayList<SortStats> possibleCible = new ArrayList<SortStats>();
            for (Entry<Integer, SortStats> SS : fighter.getMob().getSpells().entrySet()) {
                if (!fight.CanCastSpell(fighter, SS.getValue(), ff.get_fightCell(), -1))
                    continue;
                possibleCible.add(SS.getValue());
            }
            if (possibleCible.isEmpty())
                return ss;
            if (possibleCible.size() == 1)
                return possibleCible.get(0);
            ss = possibleCible.get(Formulas.getRandomValue(0, possibleCible.size() - 1));
            return ss;
        }

        private static Map<Integer, Fighter> getMinPDVEnnemy(Fight fight, Fighter fighter) {
            Fighter ennemyA = getEnnemyS(fight, fighter);
            Map<Integer, Fighter> ennemyList = new TreeMap<Integer, Fighter>();
            Map<Integer, Fighter> ennemyPlayer = new TreeMap<Integer, Fighter>();
            Map<Integer, Fighter> ennemyInvoc = new TreeMap<Integer, Fighter>();
            for (Fighter f : fight.getFighters(fighter.getOtherTeam())) { //TODO verifier le team2
                if (f.hasLeft())
                    continue;
                if (f.isDead())
                    continue;
                if (f.isInvocation())
                    ennemyInvoc.put(f.getPDV(), f);
                else
                    ennemyPlayer.put(f.getPDV(), f);
            }
            if (ennemyA != null)
                ennemyList.put(ennemyA.getPDV(), ennemyA);
            int i = 0, i2 = ennemyPlayer.size(), i3 = ennemyInvoc.size();
            int curHP;
            while (i < i2) {
                curHP = 200000;
                for (Entry<Integer, Fighter> t : ennemyPlayer.entrySet()) {
                    if (t.getValue().getPDV() < curHP)
                        curHP = t.getValue().getPDV();
                }
                Fighter test = ennemyPlayer.get(curHP);
                ennemyList.put(test.getPDV(), test);
                ennemyPlayer.remove(curHP);
                i++;
            }
            i = 0;
            while (i < i3) {
                curHP = 200000;
                for (Entry<Integer, Fighter> t : ennemyInvoc.entrySet()) {
                    if (t.getValue().getPDV() < curHP)
                        curHP = t.getValue().getPDV();
                }
                Fighter test = ennemyInvoc.get(curHP);
                ennemyList.put(test.getPDV(), test);
                ennemyInvoc.remove(curHP);
                i++;
            }
            return ennemyList;
        }

        private static Fighter getEnnemyS(Fight fight, Fighter fighter) {
            int dist = 1000;
            Fighter curF = null;
            for (Fighter target : fight.getFighters(fighter.getOtherTeam())) {
                if (target.isDead() || target.isHide())
                    continue;
                int d = Pathfinding.getDistanceBetween(fight.get_map(), fighter.get_fightCell().getID(), target
                        .get_fightCell().getID());
                if (d < dist) {
                    dist = d;
                    curF = target;
                }
            }
            return curF;
        }

        private static boolean buffIfPossibleOther(Fight fight, Fighter fighter, Fighter f) {
            if (f == null) {
                return false;
            }
            if (fighter.getMob().getTemplate().getID() == 2727) { // Tonneau
                if (!f.isState(Constant.ETAT_PORTE)) {
                    return false;
                }
            }
            SortStats SS = getBestBuff(fight, fighter, f);
            if (SS == null) {
                return false;
            }
            int buff = fight.tryCastSpell(fighter, SS, f.get_fightCell().getID());
            if (buff != 0) {
                return false;
            }
            return true;
        }

        private static SortStats getBestBuff(Fight fight, Fighter fighter, Fighter ff) {

            ArrayList<SortStats> f = new ArrayList<SortStats>();
            SortStats ss = null;
            Case FighterCell = ff.get_fightCell();
            for (Entry<Integer, SortStats> SS : fighter.getMob().getSpells().entrySet()) {
                if (fight.CanCastSpell(fighter, SS.getValue(), FighterCell, -1)) {
                    f.add(SS.getValue());
                }
                if (SS.getValue().getSpellID() == 22 || SS.getValue().getSpellID() == 2041)
                    f.add(SS.getValue());
            }
            if (f.size() <= 0)
                return null;
            if (f.size() == 1)
                return f.get(0);
            ss = f.get(Formulas.getRandomValue(0, f.size() - 1));
            return ss;
        }

        private static Fighter getBuff(Fight fight, Fighter fighter) {
            int dist = 1000;
            Fighter curF = null;
            for (Fighter target : fight.getFighters(fighter.getOtherTeam())) {
                if (target.isDead() || target.isHide())
                    continue;
                int d = Pathfinding.getDistanceBetween(fight.get_map(), fighter.get_fightCell().getID(), target
                        .get_fightCell().getID());
                if (d < dist) {
                    dist = d;
                    curF = target;
                }
            }
            return curF;
        }

        private static boolean Afunction(Fight fight, Fighter fighter, Fighter f) {
            if (fighter.getCurPM(fight) <= 0)
                return false;
            if (Pathfinding.isNextTo(fighter.get_fightCell().getID(), f.get_fightCell().getID()))
                return false;

            int cellID = Pathfinding.getNearestCellAround(fight.get_map(), f.get_fightCell().getID(), fighter
                    .get_fightCell().getID(), null);
            if (cellID == -1) {
                Map<Integer, Fighter> ennemys = getMinPDVEnnemy(fight, fighter);
                for (Entry<Integer, Fighter> target : ennemys.entrySet()) {
                    int cellID2 = Pathfinding.getNearestCellAround(fight.get_map(), target.getValue().get_fightCell().getID(),
                            fighter.get_fightCell().getID(), null);
                    if (cellID2 != -1) {
                        cellID = cellID2;
                        break;
                    }
                }
            }
            ArrayList<Case> path = Pathfinding.getShortestPathBetween(fight.get_map(), fighter.get_fightCell().getID(), cellID, 0);
            if (path == null || path.isEmpty())
                return false;
            ArrayList<Case> finalPath = new ArrayList<Case>();
            for (int a = 0; a < fighter.getCurPM(fight); a++) {
                if (path.size() == a)
                    break;
                finalPath.add(path.get(a));
            }
            String pathstr = "";
            try {
                int curCaseID = fighter.get_fightCell().getID();
                int curDir = 0;
                for (Case c : finalPath) {
                    char d = Pathfinding.getDirBetweenTwoCase(curCaseID, c.getID(), fight.get_map(), true);
                    if (d == 0)
                        return false;
                    if (curDir != d) {
                        if (finalPath.indexOf(c) != 0)
                            pathstr += CryptManager.cellID_To_Code(curCaseID);
                        pathstr += d;
                    }
                    curCaseID = c.getID();
                }
                if (curCaseID != fighter.get_fightCell().getID())
                    pathstr += CryptManager.cellID_To_Code(curCaseID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            GameAction GA = new GameAction(0, 1, "");
            GA._args = pathstr;
            boolean result = fight.fighterDeplace(fighter, GA);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            return result;
        }

        private static Fighter getNearestFriend(Fight fight, Fighter fighter) {
            int dist = 1000;
            Fighter curF = null;
            for (Fighter f : fight.getFighters(3)) {
                if (f.isDead())
                    continue;
                if (f == fighter)
                    continue;
                if (f.getTeam2() == fighter.getTeam2())// Si c'est un ami
                {
                    int d = Pathfinding.getDistanceBetween(fight.get_map(),
                            fighter.get_fightCell().getID(), f.get_fightCell()
                                    .getID());
                    if (d < dist) {
                        dist = d;
                        curF = f;
                    }
                }
            }
            return curF;
        }

        private static Fighter getNearestEnnemy(Fight fight, Fighter fighter) {
            int dist = 1000;
            Fighter curF = null;
            for (Fighter f : fight.getFighters(3)) {
                if (f.isDead())
                    continue;
                if (f.isHide())
                    continue;
                if (f.getTeam2() != fighter.getTeam2())// Si c'est un ennemis
                {
                    int d = Pathfinding.getDistanceBetween(fight.get_map(),
                            fighter.get_fightCell().getID(), f.get_fightCell()
                                    .getID());

                    if (d < dist) {
                        dist = d;
                        curF = f;
                    }
                }
            }
            if (curF == null) {
                for (Fighter f : fight.getFighters(3)) {
                    if (f.isDead())
                        continue;
                    if (f.getTeam2() != fighter.getTeam2())// Si c'est un ennemis
                    {
                        int d = Pathfinding.getDistanceBetween(fight.get_map(),
                                fighter.get_fightCell().getID(), f.get_fightCell()
                                        .getID());

                        if (d < dist) {
                            dist = d;
                            curF = f;
                        }
                    }
                }
            }
            return curF;
        }

        private static Map<Integer, Fighter> getLowHpEnnemyList(Fight fight,
                                                                Fighter fighter) { // @Algatrone
            Map<Integer, Fighter> list = new TreeMap<Integer, Fighter>();
            Map<Integer, Fighter> ennemy = new TreeMap<Integer, Fighter>();
            for (Fighter f : fight.getFighters(3)) {
                if (f.isDead())
                    continue;
                if (f.isHide())
                    continue;
                if (f == fighter)
                    continue;
                if (f.getTeam2() != fighter.getTeam2()) {
                    ennemy.put(f.getPDV(), f);
                }
            }
            int n = 0;
            int curHP = 10000;
            boolean addMobs = false;
            while (n < 2) { //@Poupou le premier tour place les joueurs et les mobs au deuxième tour. Permet à la IA de prioriser les joueurs.
                int i = 0, i2 = ennemy.size();
                while (i < i2) {
                    curHP = 200000;
                    Fighter test = null;
                    for (Entry<Integer, Fighter> t : ennemy.entrySet()) {
                        if (!addMobs) {
                            if (t.getValue().getMob() == null) {
                                if (t.getValue().getPDV() < curHP) {
                                    curHP = t.getValue().getPDV();
                                    test = t.getValue();
                                }
                            }
                        } else {
                            if (t.getValue().getPDV() < curHP) {
                                curHP = t.getValue().getPDV();
                                test = t.getValue();
                            }
                        }

                    }

                    if (test == null)
                        break;
                    list.put(test.getGUID(), test);
                    ennemy.remove(test.getGUID());
                    i++;
                }
                addMobs = true;
                n++;
            }
            return list;
        }


		/*private static Map<Integer, Fighter> getLowHpEnnemyList(Fight fight,
                Fighter fighter) {
if (fight == null || fighter == null)
return null;
Map<Integer, Fighter> list = new TreeMap<Integer, Fighter>();
Map<Integer, Fighter> ennemy = new TreeMap<Integer, Fighter>();
for (Fighter f : fight.getFighters(3)) {
if (f.isDead())
continue;
if (f == fighter)
continue;
if(f.isHide() && Formulas.getRandomValue(1, 10) != 1)
continue;
if (f.getTeam2() != fighter.getTeam2())
ennemy.put(f.getGUID(), f);
}
int i = 0, i2 = ennemy.size();
int curHP = 10000;
Fighter curEnnemy = null;

while (i < i2) {
curHP = 200000;
curEnnemy = null;
for (Entry<Integer, Fighter> t : ennemy.entrySet()) {
if (t.getValue().getPDV() < curHP) {
curHP = t.getValue().getPDV();
curEnnemy = t.getValue();
}
}
list.put(curEnnemy.getGUID(), curEnnemy);
ennemy.remove(curEnnemy.getGUID());
i++;
}
return list;
}
*/

        private static int attackIfPossibleFlow(Fight fight, Fighter fighter)// 0 = Rien, 5 = EC, 666 = NULL, 10 = SpellNull ou ActionEnCour ou Can'tCastSpell, 0 = AttaqueOK
        {
            /**
             * @Flow
             * Cette fonction tiens compte des personnages invisibles, elle va attaquer aléatoire
             * Malheuresement cette version n'est pas trï¿½s efficace avec tous les mobs
             */
            try {
                if (fight == null || fighter == null) {
                    return 0;
                }
                Map<Integer, Fighter> ennemyList = getLowHpEnnemyList(fight, fighter);
                SortStats SS = null;
                Fighter target = null;
                boolean invisible = false;
                if (ennemyList != null && !ennemyList.isEmpty()) {
                    for (Entry<Integer, Fighter> t : ennemyList.entrySet()) // pour chaque ennemi on cherche le meilleur sort
                    {
                        //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Recherche ennemi... pour "+ t.getValue() +"", "0BF9B7");
                        SS = getBestSpellForTarget(fight, fighter, t.getValue());
                        //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Sort trouvï¿½ !", "0BF9B7");
                        //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Nom du personnage sï¿½lectionnï¿½: "+ t.getValue().getPersonnage().getName() +"", "0BF9B7");
                        if (t.getValue().isHide()) { //si invisible, on passe
                            invisible = true;
                            //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("La cible n'est pas valide !", "0BF9B7");
                            continue;
                        }
                        if (SS != null) // Si il y a un sort
                        {
	                    	/*if (fight.CanCastSpell(fighter, SS, t.getValue().get_fightCell(), -1))
	                    	{*/
                            target = t.getValue();
                            //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("La cible est valide !", "0BF9B7");
                            //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Le joueur"+ target.getPersonnage().getName() +" a ï¿½tï¿½ sï¿½lectionnï¿½ comme cible.", "0BF9B7");
                            break;
	                    	/*}
	                    	else
	                    	{
	                    		//SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Erreur, impossible d'attaquer !", "0BF9B7");
	                    	}*/
                        }
                        //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Recommence la boucle", "0BF9B7");
                        //continue;
	                    /*if (SS != null) // s'il existe un sort pour un ennemi, on le prend pour cible
	                    {
	                        target = t.getValue();
	                        break;
	                    }*/
                    }
                    //SocketManager.GAME_SEND_MESSAGE_TO_ALL2("Est sorti de la boucle", "0BF9B7");
                    if (SS == null) {
                        return 10;
                    }
                }
                int curTarget = 0, cell = 0;
                SortStats SS2 = null;
                for (Entry<Integer, SortStats> S : fighter.getMob().getSpells().entrySet()) // pour chaque sort du mob
                {
                    int targetVal = getBestTargetZone(fight, fighter, S.getValue(), fighter.get_fightCell().getID()); // on détermine le meilleur
                    if (targetVal == -1 || targetVal == 0) // endroit pour lancer le sort de zone (ou non)
                    {
                        continue;
                    }
                    int nbTarget = targetVal / 1000;
                    int cellID = targetVal - nbTarget * 1000;
                    if (nbTarget > curTarget) {
                        curTarget = nbTarget;
                        cell = cellID;
                        SS2 = S.getValue();
                    }
                }
                if (curTarget > 0 && cell > 0 && cell < 480 && SS2 != null) // si la case sï¿½lectionnï¿½e est valide et qu'il y a au moins une cible
                {
                    int attack = fight.tryCastSpell(fighter, SS2, cell);
                    if (attack != 0) {
                        return attack;
                    }
                } else {
                    if (target == null || SS == null) {
                        if (invisible) { //si invisible
                            if (Config.DEBUG) {
                                System.out.println("sÃ©lection d'un sort de zone pour attaque alÃ©atoire");
                            }
                            int area = -1;
                            int curArea = -1;
                            int cellTarget = 0;
                            for (SortStats SS3 : getLaunchableSort(fighter, fight, 0)) { //on sÃ©lection le sort (lancable) avec plus grosse zone
                                if (SS3.getPorteeType().isEmpty()) {
                                    continue; //pas de porteeType
                                }
                                String p = SS3.getPorteeType();
                                int size = CryptManager.getIntByHashedValue(p.charAt(1)); //calcul la taille de la zone (en cases)
                                switch (p.charAt(0)) {
                                    case 'C': //en cercle
                                        curArea = 1;
                                        for (int n = 0; n < size; n++) {
                                            curArea += 4 * n;
                                        }
                                        break;
                                    case 'X': //en croix
                                        curArea = 4 * size + 1;
                                        break;
                                    case 'L': //en ligne
                                        curArea = size + 1;
                                        break;
                                    case 'P': //case simple
                                        curArea = 1;
                                        break;
                                    default:
                                        curArea = -1;
                                }

                                String args = SS3.isLineLaunch(fighter) ? "X" : "C";
                                char[] table = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v'};
                                if (SS3.getMaxPO(fighter) > 20) {
                                    args += "u";
                                } else {
                                    args += table[SS3.getMaxPO(fighter)];
                                }

                                if (curArea > area) { //si zone plus grande
                                    //sÃ©lection des cases possibles
                                    ArrayList<Case> possibleLaunch = Pathfinding.getCellListFromAreaString(fight.get_map(), fighter.get_fightCell().getID(), fighter.get_fightCell().getID(), args, 0, false);
                                    Collections.shuffle(possibleLaunch); //ajoute un peu d'alÃ©atoire
                                    for (Case possibleCell : possibleLaunch) {
                                        /**if(possibleCell.getFirstFighter() != null && possibleCell.getFirstFighter().getTeam2() == fighter.getTeam2()){
                                         continue;
                                         @Flow Cette condition empï¿½che les sorts qui attaques ses alliers, j'ai remarquï¿½ que ï¿½a cause un ï¿½norme problï¿½me
                                         Certain mob n'ont pas d'autres sorts.
                                         }**/
                                        if (!fight.CanCastSpell(fighter, SS3, fight.get_map().getCase(possibleCell.getID()), -1)) { //vÃ©rifie si il est lanÃ§able
                                            if (Config.DEBUG) {
                                                System.out.println("Cellule " + possibleCell.getID() + " non valide pour lancer le sort");
                                            }
                                            continue;
                                        }
                                        SS = SS3;
                                        area = curArea;
                                        cellTarget = possibleCell.getID(); //on met en mÃ©moire la cellule de lancÃ©
                                        if (Config.DEBUG) {
                                            System.out.println("Sort " + SS.getSpellID() + " sÃ©lectionnÃ©");
                                        }
                                        break;
                                    }
                                }
                            } //END FOREACH
                            return fight.tryCastSpell(fighter, SS, cellTarget); //lance le sort (dans le vide)
                        }
                        return 666;
                    }
                    int attack = fight.tryCastSpell(fighter, SS, target.get_fightCell().getID());
                    if (attack != 0) {
                        return attack;
                    }
                }
                return 0;
            } catch (NullPointerException e) {
                return 666;
            }
        }

        /**
         * @Flow Les invocations sont maintenants gérées.
         */
        private static int attackIfPossible(Fight fight, Fighter fighter)//
        {
            try {
                if (fight == null || fighter == null) {
                    return 0;
                }
                Map<Integer, Fighter> ennemyList = getLowHpEnnemyList(fight,
                        fighter);
                SortStats SS = null;
                SortStats cSS = null;
                Fighter target = null;
                int maxInfl = 0;
                int infl = 0;

                Fighter tacleurPossible = VerificationSiTacle(fighter, fight);
                if (tacleurPossible != null) {
                    if (fighter.getMob() != null && fighter.getMob().getTemplate().getID() == 239 && tacleurPossible.canBeDebuff()) { // Dragonnet rouge peut debuff
                        SortStats sortDebuff = World.getSort(479).getStatsByLevel(fighter.getMob().getLevel());
                        if (fight.CanCastSpell(fighter, sortDebuff, tacleurPossible.get_fightCell(), -1)) {
                            return fight.tryCastSpell(fighter, sortDebuff, tacleurPossible.get_fightCell().getID());
                        }
                    }
                    cSS = getBestSpellForTarget(fight, fighter, tacleurPossible);
                    if (cSS != null) {
                        int attack = fight.tryCastSpell(fighter, cSS, tacleurPossible.get_fightCell().getID());
                        return attack;
                    }
                }
                boolean invisible = true;
                for (Entry<Integer, Fighter> t : ennemyList.entrySet()) {
                    if (t.getValue().isHide()) {
                        continue;
                    } else {
                        invisible = false;
                    }
                    if (fighter.getMob() != null && fighter.getMob().getTemplate().getID() == 239 && t.getValue().canBeDebuff()) {
                        SortStats sortDebuff = World.getSort(479).getStatsByLevel(fighter.getMob().getLevel());
                        if (fight.CanCastSpell(fighter, sortDebuff, t.getValue().get_fightCell(), -1)) {
                            return fight.tryCastSpell(fighter, sortDebuff, t.getValue().get_fightCell().getID());
                        }
                    }
                    cSS = getBestSpellForTarget(fight, fighter, t.getValue());

                    if (cSS == null)
                        continue;

                    infl = calculInfluence(fight, cSS, fighter, t.getValue());
                    if (infl > maxInfl || fight.CanCastSpell(fighter, cSS, t.getValue().get_fightCell(), -1)) {
                        target = t.getValue();

                        maxInfl = infl;
                        SS = cSS;
                    }
                }
                int curTarget = 0, cell = 0;
                SortStats SS2 = null;
                for (Entry<Integer, SortStats> S : fighter.getMob().getSpells()
                        .entrySet()) // pour chaque sort du mob
                {
                    // TODO Etat sort condition
                    int targetVal = getBestTargetZone(fight, fighter, S.getValue(),
                            fighter.get_fightCell().getID()); // on dï¿½termine le
                    // meilleur
                    if (targetVal == -1 || targetVal == 0) // endroit pour lancer le
                        // sort de zone (ou non)
                        continue;
                    int nbTarget = targetVal / 1000;
                    int cellID = targetVal - nbTarget * 1000;
                    if (nbTarget > curTarget) {
                        curTarget = nbTarget;
                        cell = cellID;
                        SS2 = S.getValue();
                    }
                }

                if ((curTarget > 1 || (curTarget > 0 && SS == null))
                        && fight.get_map().getCase(cell) != null && SS2 != null) {
                    int attack = fight.tryCastSpell(fighter, SS2, cell);
                    if (attack != 0)
                        return attack;
                } else {
                    if (target == null || SS == null) { // Si il n'a pas de sort ou de cible
                        if (invisible) {
                            int area = -1;
                            int curArea = -1;
                            int cellTarget = 0;
                            for (SortStats SS3 : getLaunchableSort(fighter, fight, 0)) {
                                if (SS3.getPorteeType().isEmpty()) {
                                    continue; //pas de porteeType
                                }
                                String p = SS3.getPorteeType();
                                int size = CryptManager.getIntByHashedValue(p.charAt(1)); //calcul la taille de la zone (en cases)
                                switch (p.charAt(0)) {
                                    case 'C': //en cercle
                                        curArea = 1;
                                        for (int n = 0; n < size; n++) {
                                            curArea += 4 * n;
                                        }
                                        break;
                                    case 'X': //en croix
                                        curArea = 4 * size + 1;
                                        break;
                                    case 'L': //en ligne
                                        curArea = size + 1;
                                        break;
                                    case 'P': //case simple
                                        curArea = 1;
                                        break;
                                    default:
                                        curArea = -1;
                                }

                                String args = SS3.isLineLaunch(fighter) ? "X" : "C";
                                char[] table = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v'};
                                if (SS3.getMaxPO(fighter) > 20) {
                                    args += "u";
                                } else {
                                    args += table[SS3.getMaxPO(fighter)];
                                }

                                if (curArea > area) { //si zone plus grande
                                    ArrayList<Case> possibleLaunch = Pathfinding.getCellListFromAreaString(fight.get_map(), fighter.get_fightCell().getID(), fighter.get_fightCell().getID(), args, 0, false);
                                    Collections.shuffle(possibleLaunch);
                                    for (Case possibleCell : possibleLaunch) {
                                        if (possibleCell.getFirstFighter() != null && possibleCell.getFirstFighter().getTeam2() == fighter.getTeam2()) {
                                            continue;
                                        }
                                        if (!fight.CanCastSpell(fighter, SS3, fight.get_map().getCase(possibleCell.getID()), -1)) {
                                            if (Config.DEBUG) {
                                                System.out.println("Cellule " + possibleCell.getID() + " non valide pour lancer le sort");
                                            }
                                            continue;
                                        }
                                        SS = SS3;
                                        area = curArea;
                                        cellTarget = possibleCell.getID();
                                        if (Config.DEBUG) {
                                            System.out.println("Sort " + SS.getSpellID() + " sï¿½lectionnï¿½");
                                        }
                                        break;
                                    }
                                }
                            }
                            return fight.tryCastSpell(fighter, SS, cellTarget); //lance le sort (dans le vide)
                        }
                    }
                    //return 666;
                    int attack = fight.tryCastSpell(fighter, SS, target
                            .get_fightCell().getID());
                    if (attack != 0)
                        return attack;
                }
                return 0;
            } catch (NullPointerException e) {
                return 666;
            }
        }

        private static int attackIfPossiblePerco(Fight fight, Fighter fighter) {
            Map<Integer, Fighter> ennemyList = getLowHpEnnemyList(fight, fighter);
            SortStats SS = null;
            Fighter target = null;
            for (Entry<Integer, Fighter> t : ennemyList.entrySet()) {
                SS = getBestSpellForTargetPerco(fight, fighter, t.getValue());
                if (SS != null) {
                    target = t.getValue();
                    break;
                }
            }
            int curTarget = 0, cell = 0;
            SortStats SS2 = null;
            for (Entry<Integer, SortStats> S : World.getGuild(fighter.getPerco().GetPercoGuildID()).getSpells().entrySet()) {
                if (S.getValue() == null)
                    continue;
                int targetVal = getBestTargetZone(fight, fighter, S.getValue(), fighter.get_fightCell().getID());
                if (targetVal == -1 || targetVal == 0)
                    continue;
                int nbTarget = targetVal / 1000;
                int cellID = targetVal - nbTarget * 1000;
                if (nbTarget > curTarget) {
                    curTarget = nbTarget;
                    cell = cellID;
                    SS2 = S.getValue();
                }
            }
            if (curTarget > 0 && cell > 0 && cell < 480 && SS2 != null) {
                int attack = fight.tryCastSpell(fighter, SS2, cell);
                if (attack != 0)
                    return attack;
            } else {
                if (target == null || SS == null)
                    return 666;
                int attack = fight.tryCastSpell(fighter, SS, target.get_fightCell().getID());
                if (attack != 0)
                    return attack;
            }
            return 0;

        }

        private static SortStats getBestSpellForTarget(Fight fight, Fighter F,
                                                       Fighter T) {
            int inflMax = 0;
            SortStats ss = null;
            for (Entry<Integer, SortStats> SS : F.getMob().getSpells()
                    .entrySet()) {
                int curInfl = 0, Infl1 = 0, Infl2 = 0;
                int PA = F.getMob().getPA();
                int usedPA[] = {0, 0};
                if (!fight.CanCastSpell(F, SS.getValue(), T.get_fightCell(), -1))
                    continue;
                curInfl = calculInfluence(fight, SS.getValue(), F, T);
                if (curInfl == 0)
                    continue;
                if (curInfl > inflMax) {
                    ss = SS.getValue();
                    usedPA[0] = ss.getPACost(F);
                    Infl1 = curInfl;
                    inflMax = Infl1;
                }

                for (Entry<Integer, SortStats> SS2 : F.getMob().getSpells()
                        .entrySet()) {
                    if ((PA - usedPA[0]) < SS2.getValue().getPACost(F))
                        continue;
                    if (!fight.CanCastSpell(F, SS2.getValue(),
                            T.get_fightCell(), -1))
                        continue;
                    curInfl = calculInfluence(fight, SS2.getValue(), F, T);
                    if (curInfl == 0)
                        continue;
                    if ((Infl1 + curInfl) > inflMax) {
                        ss = SS.getValue();
                        usedPA[1] = SS2.getValue().getPACost(F);
                        Infl2 = curInfl;
                        inflMax = Infl1 + Infl2;
                    }
                    for (Entry<Integer, SortStats> SS3 : F.getMob().getSpells()
                            .entrySet()) {
                        if ((PA - usedPA[0] - usedPA[1]) < SS3.getValue()
                                .getPACost(F))
                            continue;
                        if (!fight.CanCastSpell(F, SS3.getValue(),
                                T.get_fightCell(), -1))
                            continue;
                        curInfl = calculInfluence(fight, SS3.getValue(), F, T);
                        if (curInfl == 0)
                            continue;
                        if ((curInfl + Infl1 + Infl2) > inflMax) {
                            ss = SS.getValue();
                            inflMax = curInfl + Infl1 + Infl2;
                        }
                    }
                }
            }
            return ss;

        }

        private static SortStats getBestSpellForTargetPerco(Fight fight,
                                                            Fighter F, Fighter T) {
            int inflMax = 0;
            SortStats ss = null;
            for (Entry<Integer, SortStats> SS : World
                    .getGuild(F.getPerco().GetPercoGuildID()).getSpells()
                    .entrySet()) {
                if (SS.getValue() == null)
                    continue;
                int curInfl = 0, Infl1 = 0, Infl2 = 0;
                int PA = 6;
                int usedPA[] = {0, 0};
                if (!fight.CanCastSpell(F, SS.getValue(), F.get_fightCell(), T
                        .get_fightCell().getID()))
                    continue;
                curInfl = calculInfluence(fight, SS.getValue(), F, T);
                if (curInfl == 0)
                    continue;
                if (curInfl > inflMax) {
                    ss = SS.getValue();
                    usedPA[0] = ss.getPACost(F);
                    Infl1 = curInfl;
                    inflMax = Infl1;
                }

                for (Entry<Integer, SortStats> SS2 : World
                        .getGuild(F.getPerco().GetPercoGuildID()).getSpells()
                        .entrySet()) {
                    if (SS2 == null || SS2.getValue() == null)
                        continue;
                    if ((PA - usedPA[0]) < SS2.getValue().getPACost(F))
                        continue;
                    if (!fight.CanCastSpell(F, SS2.getValue(),
                            F.get_fightCell(), T.get_fightCell().getID()))
                        continue;
                    curInfl = calculInfluence(fight, SS2.getValue(), F, T);
                    if (curInfl == 0)
                        continue;
                    if ((Infl1 + curInfl) > inflMax) {
                        ss = SS.getValue();
                        usedPA[1] = SS2.getValue().getPACost(F);
                        Infl2 = curInfl;
                        inflMax = Infl1 + Infl2;
                    }
                    for (Entry<Integer, SortStats> SS3 : World
                            .getGuild(F.getPerco().GetPercoGuildID())
                            .getSpells().entrySet()) {
                        if (SS3 == null || SS3.getValue() == null)
                            continue;
                        if ((PA - usedPA[0] - usedPA[1]) < SS3.getValue()
                                .getPACost(F))
                            continue;
                        if (!fight.CanCastSpell(F, SS3.getValue(),
                                F.get_fightCell(), T.get_fightCell().getID()))
                            continue;
                        curInfl = calculInfluence(fight, SS3.getValue(), F, T);
                        if (curInfl == 0)
                            continue;
                        if ((curInfl + Infl1 + Infl2) > inflMax) {
                            ss = SS.getValue();
                            inflMax = curInfl + Infl1 + Infl2;
                        }
                    }
                }
            }
            return ss;
        }

        private static int getBestTargetZone(Fight fight, Fighter fighter,
                                             SortStats spell, int launchCell) {
            if (spell.getPorteeType().isEmpty()
                    || (spell.getPorteeType().charAt(0) == 'P' && spell
                    .getPorteeType().charAt(1) == 'a')) {
                return 0;
            }
            ArrayList<Case> possibleLaunch = new ArrayList<Case>();
            int CellF = -1;
            if (spell.getMaxPO(fighter) != 0) {
                char arg1 = 'a';
                if (spell.isLineLaunch(fighter)) {
                    arg1 = 'X';
                } else {
                    arg1 = 'C';
                }
                char[] table = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                        'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v'};
                char arg2 = 'a';
                if (spell.getMaxPO(fighter) > 20) {
                    arg2 = 'u';
                } else {
                    arg2 = table[spell.getMaxPO(fighter)];
                }
                String args = Character.toString(arg1)
                        + Character.toString(arg2);
                possibleLaunch = Pathfinding
                        .getCellListFromAreaString(fight.get_map(), launchCell,
                                launchCell, args, 0, false);
            } else {
                possibleLaunch.add(fight.get_map().getCase(launchCell));
            }

            if (possibleLaunch == null) {
                return -1;
            }
            int nbTarget = 0;
            for (Case cell : possibleLaunch) {
                try {
                    if (!fight.CanCastSpell(fighter, spell, cell, launchCell))
                        continue;
                    int num = 0;
                    int curTarget = 0;
                    ArrayList<SpellEffect> test = new ArrayList<SpellEffect>();
                    test.addAll(spell.getEffects());

                    for (SpellEffect SE : test) {
                        try {
                            if (SE == null)
                                continue;
                            if (SE.getValue() == -1)
                                continue;
                            int POnum = num * 2;
                            ArrayList<Case> cells = Pathfinding
                                    .getCellListFromAreaString(fight.get_map(),
                                            cell.getID(), launchCell,
                                            spell.getPorteeType(), POnum, false);
                            for (Case c : cells) {
                                if (c.getFirstFighter() == null)
                                    continue;
                                if (c.getFirstFighter().getTeam2() != fighter
                                        .getTeam2())
                                    curTarget++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ;
                        num++;
                    }
                    if (curTarget > nbTarget) {
                        nbTarget = curTarget;
                        CellF = cell.getID();
                    }
                } catch (Exception E) {
                    E.printStackTrace();
                }
            }
            if (nbTarget > 0 && CellF != -1)
                return CellF + nbTarget * 1000;
            else
                return 0;
        }

        private static int calculInfluenceHeal(SortStats ss) {
            int inf = 0;
            for (SpellEffect SE : ss.getEffects()) {
                if (SE.getEffectID() != 108)
                    return 0;
                inf += 100 * Formulas.getMiddleJet(SE.getJet());
            }

            return inf;
        }

        private static int calculInfluence(Fight fight, SortStats ss,
                                           Fighter C, Fighter T) {
            // FIXME TODO
            int infTot = 0;
            double fact = 1;
            int num = 0, POnum = 0;
            int allies = 0, ennemies = 0;
            for (SpellEffect SE : ss.getEffects()) {
                if (SE == null)
                    continue;
                allies = 0;
                ennemies = 0;
                POnum = 2 * num;
                /** Dï¿½termine ï¿½ qui s'applique l'effet **/
                ArrayList<Case> cells = Pathfinding.getCellListFromAreaString(
                        fight.get_map(), T.get_fightCell().getID(), C
                                .get_fightCell().getID(), ss.getPorteeType(),
                        POnum, false);
                ArrayList<Case> finalCells = new ArrayList<Case>();
                int TE = 0;
                Spell S = ss.getSpell();
                // on prend le targetFlag corespondant au num de l'effet
                // si on peut
                if (S != null ? S.getEffectTargets().size() > num : false)
                    TE = S.getEffectTargets().get(num);

                for (Case C1 : cells) {
                    if (C1 == null)
                        continue;
                    Fighter F = C1.getFirstFighter();
                    if (F == null)
                        continue;
                    // Ne touche pas les alliï¿½s
                    if (((TE & 1) == 1) && (F.getTeam() == C.getTeam()))
                        continue;
                    // Ne touche pas le lanceur
                    if ((((TE >> 1) & 1) == 1) && (F.getGUID() == C.getGUID()))
                        continue;
                    // Ne touche pas les ennemies
                    if ((((TE >> 2) & 1) == 1) && (F.getTeam() != C.getTeam()))
                        continue;
                    // Ne touche pas les combatants (seulement invocations)
                    if ((((TE >> 3) & 1) == 1) && (!F.isInvocation()))
                        continue;
                    // Ne touche pas les invocations
                    if ((((TE >> 4) & 1) == 1) && (F.isInvocation()))
                        continue;
                    // N'affecte que le lanceur
                    if ((((TE >> 5) & 1) == 1) && (F.getGUID() != C.getGUID()))
                        continue;
                    // Si pas encore eu de continue, on ajoute la case
                    finalCells.add(C1);
                }
                // Si le sort n'affecte que le lanceur et que le lanceur n'est
                // pas dans la zone
                if (((TE >> 5) & 1) == 1)
                    if (!finalCells.contains(C.get_fightCell()))
                        finalCells.add(C.get_fightCell());
                ArrayList<Fighter> cibles = SpellEffect.getTargets(SE, fight,
                        finalCells);
                for (Fighter fighter : cibles) {
                    if (fighter.getTeam() == C.getTeam())
                        allies++;
                    else
                        ennemies++;
                }
                num++;
                /** Fin de la dï¿½termination **/
                // System.out.println("SpellEffect : "+SE.getEffectID()+"   Nbr ennemis : "+ennemies+"   Nbr alliï¿½s : "+allies);
                int inf = 0;
                switch (SE.getEffectID()) {
                    case 5:// repousse de X cases
                        inf = 100;
                        fact += 0.3 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 89:// dommages % vie neutre
                        inf = 500 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 91:// Vol de Vie Eau
                        inf = 200 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 92:// Vol de Vie Terre
                        inf = 200 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 93:// Vol de Vie Air
                        inf = 200 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 94:// Vol de Vie feu
                        inf = 200 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 95:// Vol de Vie neutre
                        inf = 200 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 96:// Dommage Eau
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 97:// Dommage Terre
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 98:// Dommage Air
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 99:// Dommage feu
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 100:// Dommage neutre
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 101:// retrait PA
                        inf = 5000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 127:// retrait PM
                        inf = 3000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 84:// vol PA
                        inf = 8000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 77:// vol PM
                        inf = 4000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 108:// soin
                        inf = -100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 111:// + PA
                        inf = -5000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 128:// + PM
                        inf = -3000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 121:// + Dom
                        inf = -500 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 131:// poison X pdv par PA
                        inf = 300 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 132:// dï¿½senvoute
                        inf = 6000;
                        break;
                    case 138:// + %Dom
                        inf = -150 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 150:// invisibilitï¿½
                        inf = -5000;
                        break;
                    case 168:// retrait PA non esquivable
                        inf = 10000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 169:// retrait PM non esquivable
                        inf = 8000 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 210:// rï¿½sistance
                        inf = -100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 211:// rï¿½sistance
                        inf = -100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 212:// rï¿½sistance
                        inf = -100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 213:// rï¿½sistance
                        inf = -100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 214:// rï¿½sistance
                        inf = -100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 215:// faiblesse
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 216:// faiblesse
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 217:// faiblesse
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 218:// faiblesse
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 219:// faiblesse
                        inf = 100 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 265:// rï¿½duction dommage
                        inf = -250 * Formulas.getMiddleJet(SE.getJet());
                        break;
                    case 786:// Arbre de vie
                        inf = -1000;
                        break;
                    case 765:// sacrifice
                        inf = -50000;
                        break;

                }
                if (C.getTeam() == T.getTeam())// Si Amis
                    infTot -= inf * (allies - ennemies);
                else
                    // Si ennemis
                    //if (T.isHide())
                        //return 0;
                infTot += inf * (ennemies - allies);
            }
            return (int) (((double) infTot) * fact);
        }

        // Nouvelles fonctions un peu plus "intelligentes"
        private static int attackTargetIfPossible(Fight fight, Fighter fighter,
                                                  Fighter target)// 0 = Rien, 5 = EC, 666 = NULL, 10 = SpellNull
        // ou ActionEnCour ou Can'tCastSpell, 0 =
        // AttaqueOK
        {
            try {
                if (!target.isHide()) {
                    SortStats SS = getBestSpellForTarget(fight, fighter, target);
                    int curTarget = 0, cell = 0;
                    SortStats SS2 = null;
                    for (Entry<Integer, SortStats> S : fighter.getMob().getSpells()
                            .entrySet()) // pour chaque sort du mob
                    {
                        if (Thread.interrupted()) throw new InterruptedException();
                        int targetVal = getBestTargetZone(fight, fighter, S.getValue(),
                                fighter.get_fightCell().getID()); // on dï¿½termine le
                        // meilleur
                        if (targetVal == -1 || targetVal == 0) // endroit pour lancer le
                            // sort de zone (ou non)
                            continue;
                        int nbTarget = targetVal / 1000;
                        int cellID = targetVal - nbTarget * 1000;
                        if (nbTarget > curTarget) {
                            curTarget = nbTarget;
                            cell = cellID;
                            SS2 = S.getValue();
                        }
                    }
                    if ((curTarget > 1 || (curTarget > 0 && SS == null))
                            && fight.get_map().getCase(cell) != null && SS2 != null) {
                        int attack = fight.tryCastSpell(fighter, SS2, cell);
                        if (attack != 0)
                            return attack;
                    } else {
                        if (target == null || SS == null)
                            return 666;
                        int attack = fight.tryCastSpell(fighter, SS, target
                                .get_fightCell().getID());
                        if (attack != 0)
                            return attack;
                    }
                }
                else {
                    SortStats SS = getBestSpellForTarget(fight, fighter, target);
                    int area = -1;
                    int curArea = -1;
                    int cellTarget = 0;
                    if (SS == null)
                        return 666;
                    if (SS.getPorteeType().isEmpty()) {
                        return 0;
                    }
                    String p = SS.getPorteeType();
                    int size = CryptManager.getIntByHashedValue(p.charAt(1)); //calcul la taille de la zone (en cases)
                    switch (p.charAt(0)) {
                        case 'C': //en cercle
                            curArea = 1;
                            for (int n = 0; n < size; n++) {
                                curArea += 4 * n;
                            }
                            break;
                        case 'X': //en croix
                            curArea = 4 * size + 1;
                            break;
                        case 'L': //en ligne
                            curArea = size + 1;
                            break;
                        case 'P': //case simple
                            curArea = 1;
                            break;
                        default:
                            curArea = -1;
                    }
                    String args = SS.isLineLaunch(fighter) ? "X" : "C";
                    char[] table = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v'};
                    if (SS.getMaxPO(fighter) > 20) {
                        args += "u";
                    } else {
                        args += table[SS.getMaxPO(fighter)];
                    }

                    if (curArea > area) { //si zone plus grande
                        ArrayList<Case> possibleLaunch = Pathfinding.getCellListFromAreaString(fight.get_map(), fighter.get_fightCell().getID(), fighter.get_fightCell().getID(), args, 0, false);
                        Collections.shuffle(possibleLaunch);
                        for (Case possibleCell : possibleLaunch) {
                            if (possibleCell.getFirstFighter() != null && possibleCell.getFirstFighter().getTeam2() == fighter.getTeam2()) {
                                continue;
                            }
                            if (!fight.CanCastSpell(fighter, SS, fight.get_map().getCase(possibleCell.getID()), -1)) {
                                if (Config.DEBUG) {
                                    System.out.println("Cellule " + possibleCell.getID() + " non valide pour lancer le sort");
                                }
                                continue;
                            }
                            //SS = SS3;
                            area = curArea;
                            cellTarget = possibleCell.getID();
                            if (Config.DEBUG) {
                                System.out.println("Sort " + SS.getSpellID() + " sélectionné");
                            }
                            break;
                        }
                    }
                    return fight.tryCastSpell(fighter, SS, cellTarget); //lance le sort (dans le vide)
                }
                return 0;
            } catch (InterruptedException ie) {
                Logs.addToDebug("Interrompu dans le attackTargetIfPossible : " + ((long) (System.currentTimeMillis() / 1000 - _startT)));
                return 666;
            }
        }

        private static Fighter getBestEnnemy(Fight fight, Fighter fighter) {
            try {
                Map<Integer, Fighter> ennemyList = getEnnemyList(fight, fighter);
                int maxVal = 0;
                int curVal = 0;
                SortStats SS = null;
                Fighter curF = null;
                try {
                    for (Fighter f : ennemyList.values()) {
                        if (f.isHide())
                            continue;
                        if (Thread.interrupted()) throw new InterruptedException();
                        SS = getBestSpellForTargetWithPM(fight, fighter, f,
                                fighter.getCurPM(fight));
                        if (SS == null) {
                            continue;
                        }

                        int d = Pathfinding.getDistanceBetween(fight.get_map(),
                                fighter.get_fightCell().getID(), f.get_fightCell()
                                        .getID());
                        curVal = 50000 - d * 1000;
                        if (f.isInvocation())
                            curVal -= 25000;
                        if (Pathfinding.isNextTo(fighter.get_fightCell().getID(), f
                                .get_fightCell().getID()))
                            curVal += 10000;
                        curVal += calculInfluence(fight, SS, fighter, f);
                        if (curVal > maxVal) {
                            curF = f;
                            maxVal = curVal;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (Thread.interrupted()) throw new InterruptedException();
                }
                if (curF == null) {
                    curF = getNearestEnnemy(fight, fighter);
                }
                return curF;
            } catch (InterruptedException ie) {
                Logs.addToDebug("Interrompu durant getBestEnnemy " + ((long) (System.currentTimeMillis() / 1000 - _startT)));
            }
            return null;
        }

        private static Map<Integer, Fighter> getEnnemyList(Fight fight, Fighter fighter) {
            Map<Integer, Fighter> ennemy = new TreeMap<Integer, Fighter>();
            try {
                for (Fighter f : fight.getFighters(3)) {
                    if (Thread.interrupted())
                        throw new InterruptedException();
                    if (f.isDead())
                        continue;
                    if (f.isHide())
                        continue;
                    if (f == fighter)
                        continue;
                    if (f.getTeam2() != fighter.getTeam2()) {
                        ennemy.put(f.getGUID(), f);
                    }
                }
                return ennemy;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return ennemy;
        }

        private static SortStats getBestSpellForTargetWithPM(Fight fight,
                                                             Fighter F, Fighter T, int PM) {
            try {
                SortStats retour = getBestSpellForTarget2(fight, F, T);
                if (retour != null)
                    return retour;

                // Sinon on doit utiliser nos PM
                int cellID = Pathfinding.getNearestCellAround(fight.get_map(),
                        T.get_fightCell().getID(), F.get_fightCell().getID(),
                        null);
                // On demande le chemin plus court
                if (cellID == -1)
                    return null;
                ArrayList<Case> path = Pathfinding.getShortestPathBetween(
                        fight.get_map(), F.get_fightCell().getID(), cellID, 0);
                if (path == null || path.isEmpty())
                    return null;
                Case cur_C = F.get_fightCell();
                int nb = 0;
                for (int a = 0; a < F.getCurPM(fight); a++) {
                    if (nb > 5) break;

                    if (Thread.interrupted())
                        throw new InterruptedException();
                    if (path.size() == a)
                        break;
                    if (path.get(a) == null)
                        break;
                    nb++;
                    F.set_fightCell(path.get(a));
                    retour = getBestSpellForTarget2(fight, F, T);
                    if (retour != null)
                        break;
                    Thread.sleep(50);
                }
                F.set_fightCell(cur_C);
                return retour;
            } catch (InterruptedException ie) {
                Logs.addToDebug("Interrompu durant getBestSpellForTargetWithPM " + ((long) (System.currentTimeMillis() / 1000 - _startT)));
                return null;
            }
        }

        private static SortStats getBestSpellForTarget2(Fight fight, Fighter F, Fighter T) {//Version optimisï¿½e
            int inflMax = 0;
            SortStats ss = null;
            int curInfl = 0;
            for (Entry<Integer, SortStats> SS : F.getMob().getSpells().entrySet()) {
                if (SS.getValue().getSpell().getSpellID() == 2041)
                    continue;
                //On voit si c'est possible de lancer le sort
                if (!fight.CanCastSpell(F, SS.getValue(), T.get_fightCell(), -1)) continue;
                curInfl = calculInfluence(fight, SS.getValue(), F, T);
                if (curInfl == 0) continue;
                if (inflMax < curInfl) {
                    inflMax = curInfl;
                    ss = SS.getValue();
                }
            }
            return ss;
        }
    }
}