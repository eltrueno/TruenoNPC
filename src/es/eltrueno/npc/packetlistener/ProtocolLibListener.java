package es.eltrueno.npc.packetlistener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import es.eltrueno.npc.TruenoNPC;
import es.eltrueno.npc.TruenoNPCApi;
import es.eltrueno.npc.event.TruenoNPCInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class ProtocolLibListener implements PacketListener{

    private static ProtocolManager protocolManager;

    private static ArrayList<Player> playerswhointeract = new ArrayList<Player>();

    public static void setup(){
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void startListening(Plugin plugin){
        if(protocolManager==null){
            setup();
        }
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event){
                if(event.getPacketType() == PacketType.Play.Client.USE_ENTITY){
                    Player p = event.getPlayer();
                    try {
                        PacketContainer packet = event.getPacket();
                        int id = packet.getIntegers().read(0);
                        if(!playerswhointeract.contains(p)){
                            for(TruenoNPC npc : TruenoNPCApi.getNPCs()){
                                if(npc.getEntityID(p)==id){
                                    TruenoNPCInteractEvent interactevent = new TruenoNPCInteractEvent(p, npc);
                                    Bukkit.getPluginManager().callEvent(interactevent);
                                    break;
                                }
                            }
                            playerswhointeract.add(p);
                            Bukkit.getScheduler().runTaskLaterAsynchronously(TruenoNPCApi.getPlugin(), new Runnable(){
                                @Override
                                public void run() {
                                    playerswhointeract.remove(p);
                                }
                            }, 2);
                        }
                    } catch (Exception e){}
                }
            }
        });
    }
}
