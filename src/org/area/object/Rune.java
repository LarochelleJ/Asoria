package org.area.object;

import lombok.Getter;

/**
 * @Author Flow
 */

public class Rune {
    @Getter private int id;
    @Getter private int poid;
    @Getter private int puissance;
    @Getter private int idEffet;

    public Rune(int id, int poid, int puissance, int idEffet) {
        this.id = id;
        this.poid = poid;
        this.puissance = puissance;
        this.idEffet = idEffet;
    }
}
