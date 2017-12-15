package org.area.object;

import org.area.common.SQLManager;
import org.area.common.World;

import java.util.LinkedList;
import java.util.Random;

/**
 * Created by Meow on 2017-12-14.
 */
public class Gift {
    private Random r = new Random();
    private int giftTemplate;
    private LinkedList<Prob> prob = new LinkedList<Prob>();

    public Gift(int giftTemplate, int template, int limit, int gain, float chance) {
        this.giftTemplate = giftTemplate;
        World.cadeaux.put(giftTemplate, this);
        prob.add(new Prob(template, limit, gain, chance));
    }

    public void addProb(int template, int limit, int gain, float chance) {
        prob.add(new Prob(template, limit, gain, chance));
    }

    public Item.ObjTemplate open() {
        Item.ObjTemplate itemWon = null;
        while (itemWon == null) {
            for (Prob p : prob) {
                if (p.tryToWin(r)) {
                    itemWon = p.getTemplate();
                    SQLManager.SAVE_GIFT_GAIN(giftTemplate, p.getTemplate().getID(), p.getGain());
                    break;
                }
            }
        }
        return  itemWon;
    }
}

class Prob {
    private Item.ObjTemplate template;
    private int limit;
    private int gain;
    private float chance;

    Prob(int template, int limit, int gain, float chance) {
        this.template = World.getObjTemplate(template);
        this.limit = limit;
        this.gain = gain;
        this.chance = chance / 100F;
    }

    boolean tryToWin(Random r) {
        boolean win = false;
        if ((limit != 0 && gain < limit || limit == 0) && r.nextFloat() <= chance) {
            win = true;
            gain++;
        }
        return win;
    }

    Item.ObjTemplate getTemplate() {
        return template;
    }

    int getGain() {
        return gain;
    }


}
