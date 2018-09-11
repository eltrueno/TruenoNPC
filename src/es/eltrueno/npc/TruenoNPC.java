package es.eltrueno.npc;

import es.eltrueno.npc.skin.TruenoNPCSkin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

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
    int getEntityID(Player p);
    boolean isDeleted();
    int getNpcID();
    TruenoNPCSkin getSkin();
}
