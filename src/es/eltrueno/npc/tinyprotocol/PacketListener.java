package es.eltrueno.npc.tinyprotocol;

import es.eltrueno.npc.TruenoNPC;
import es.eltrueno.npc.TruenoNPCApi;
import es.eltrueno.npc.event.TruenoNPCInteractEvent;
import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class PacketListener {

    private static TinyProtocol protocol = null;

    private static Class<?> EntityInteractClass = Reflection.getClass("{nms}.PacketPlayInUseEntity");
    private static Reflection.FieldAccessor<Integer> EntityID = Reflection.getField(EntityInteractClass, int.class, 0);
    private static ArrayList<Player> playerswhointeract = new ArrayList<Player>();



    public static void startListening(Plugin plugin){
        if(protocol==null) {
            protocol = new TinyProtocol(plugin) {
                @Override
                public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
                    if(EntityInteractClass.isInstance(packet)){
                        if(!playerswhointeract.contains(sender)){
                            for(TruenoNPC npc : TruenoNPCApi.getNPCs()){
                                if(npc.getEntityID(sender)==EntityID.get(packet)){
                                    TruenoNPCInteractEvent event = new TruenoNPCInteractEvent(sender, npc);
                                    Bukkit.getPluginManager().callEvent(event);
                                    break;
                                }
                            }
                            playerswhointeract.add(sender);
                            Bukkit.getScheduler().runTaskLaterAsynchronously(TruenoNPCApi.getPlugin(), new Runnable(){
                                @Override
                                public void run() {
                                    playerswhointeract.remove(sender);
                                }
                            }, 2);
                        }
                    }
                    return super.onPacketInAsync(sender, channel, packet);
                }
            };
        }
    }

}
