package es.eltrueno.npc;

import es.eltrueno.npc.skin.TruenoNPCSkin;
import org.bukkit.Location;

public interface TruenoNPC {

    /**
     *
     *
     * @author el_trueno
     *
     *
     **/

    void delete();
    Location getLocation();
    int getEntityID();
    boolean isDeleted();
    int getNpcID();
    TruenoNPCSkin getSkin();
}
