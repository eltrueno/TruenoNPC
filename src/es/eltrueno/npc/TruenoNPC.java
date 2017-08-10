package es.eltrueno.npc;

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

}
