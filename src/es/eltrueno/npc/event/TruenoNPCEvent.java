package es.eltrueno.npc.event;

import es.eltrueno.npc.TruenoNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class TruenoNPCEvent extends Event{

    private Player player;
    private TruenoNPC npc;

    public TruenoNPCEvent(Player player, TruenoNPC npc) {
        this.player = player;
        this.npc = npc;
    }


    public Player getPlayer() {
        return this.player;
    }

    public TruenoNPC getNPC(){
        return npc;
    }
}
