package org.area.object;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author Flow
 */

public class Checkpoint {
    @Getter @Setter private Checkpoint prev = null;
    @Getter @Setter private Checkpoint next = null;
    @Getter private short mapID;
    @Getter private int cellID;
    @Getter private int donjonID;

    public Checkpoint(short mapID, int cellID, int donjonID) {
        this.mapID = mapID;
        this.cellID = cellID;
        this.donjonID = donjonID;
    }
}
