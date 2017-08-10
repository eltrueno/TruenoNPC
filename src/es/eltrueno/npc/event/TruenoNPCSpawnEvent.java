package es.eltrueno.npc.event;

import es.eltrueno.npc.TruenoNPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class TruenoNPCSpawnEvent extends TruenoNPCEvent {

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public TruenoNPCSpawnEvent(Player player, TruenoNPC npc){
        super(player, npc);
    }

}
