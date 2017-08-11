package es.eltrueno.npc.nms;

import com.mojang.authlib.GameProfile;
import es.eltrueno.npc.TruenoNPC;
import es.eltrueno.npc.event.TruenoNPCDespawnEvent;
import es.eltrueno.npc.event.TruenoNPCSpawnEvent;
import es.eltrueno.npc.utils.GameProfileUtils;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TruenoNPC_v1_8_r3 implements TruenoNPC {

    /**
     *
     *
     * @author el_trueno
     *
     *
     **/

    private static List<TruenoNPC_v1_8_r3> npcs = new ArrayList<TruenoNPC_v1_8_r3>();
    private static int id = 0;
    private static boolean taststarted = false;
    private static Plugin plugin;
    private PacketPlayOutScoreboardTeam scbpacket;
    private boolean deleted = false;
    private int npcid;
    private int entityID;
    private Location location;
    private GameProfile gameprofile;
    private List<Player> rendered = new ArrayList<Player>();

    public static void startTask(Plugin plugin){
        if(!taststarted){
            taststarted = true;
            TruenoNPC_v1_8_r3.plugin = plugin;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for(TruenoNPC_v1_8_r3 nmsnpc : npcs){
                        for(Player pl : Bukkit.getOnlinePlayers()){
                            if(nmsnpc.location.getWorld().equals(pl.getWorld())){
                                if(nmsnpc.location.distance(pl.getLocation())>60 && nmsnpc.rendered.contains(pl)){
                                    nmsnpc.destroy(pl);
                                }else if(nmsnpc.location.distance(pl.getLocation())<60 && !nmsnpc.rendered.contains(pl)){
                                    nmsnpc.spawn(pl);
                                }
                            }else{
                                nmsnpc.destroy(pl);
                            }
                        }
                    }
                }
            },0,30);
        }
    }

    private void setValue(Object obj,String name,Object value){
        try{
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        }catch(Exception e){}
    }

    private Object getValue(Object obj,String name){
        try{
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        }catch(Exception e){}
        return null;
    }

    private void sendPacket(Packet<?> packet, Player player){
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
    }


    @Override
    public Location getLocation(){
        return this.location;
    }

    @Override
    public int getEntityID(){
        return this.entityID;
    }

    @Override
    public boolean isDeleted(){
        return deleted;
    }

    @Override
    public int getNpcID(){
        return npcid;
    }

    public TruenoNPC_v1_8_r3(Location location, String skin){
        entityID = (int)Math.ceil(Math.random() * 1000) + 2000;
        npcid = id++;
        gameprofile = GameProfileUtils.getGameProfileFromName(skin,"npc"+npcid);
        this.location = location;
        if(!npcs.contains(this)){
            npcs.add(this);
        }
    }

    @Override
    public void delete(){
        npcs.remove(this);
        this.deleted = true;
    }

    private void spawn(Player p){
        PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();

        setValue(packet, "a", entityID);
        setValue(packet, "b", gameprofile.getId());
        setValue(packet, "c", (int) MathHelper.floor(location.getX() * 32.0D));
        setValue(packet, "d", (int)MathHelper.floor(location.getY() * 32.0D));
        setValue(packet, "e", (int)MathHelper.floor(location.getZ() * 32.0D));
        setValue(packet, "f", (byte) ((int) (location.getYaw() * 256.0F / 360.0F)));
        setValue(packet, "g", (byte) ((int) (location.getPitch() * 256.0F / 360.0F)));
        DataWatcher w = new DataWatcher(null);
        w.a(10,(byte)127);
        setValue(packet, "i", w);
        try {
            scbpacket = new PacketPlayOutScoreboardTeam();
            setValue(scbpacket, "h",0);
            setValue(scbpacket, "b",gameprofile.getName());
            setValue(scbpacket, "a",gameprofile.getName());
            setValue(scbpacket, "e","never");
            setValue(scbpacket, "i",1);
            Field f = scbpacket.getClass().getDeclaredField("g");
            f.setAccessible(true);
            ((Collection) f.get(scbpacket)).add(gameprofile.getName());
            sendPacket(scbpacket, p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        addToTablist(p);
        sendPacket(packet, p);
        PacketPlayOutEntityHeadRotation rotationpacket = new PacketPlayOutEntityHeadRotation();
        setValue(rotationpacket, "a", entityID);
        setValue(rotationpacket, "b", (byte) ((int) (location.getYaw() * 256.0F / 360.0F)));
        sendPacket(rotationpacket, p);
        Bukkit.getScheduler().runTaskLater(TruenoNPC_v1_8_r3.plugin, new Runnable(){
            @Override
            public void run() {
                rmvFromTablist(p);
            }
        },13);
        rendered.add(p);
        TruenoNPCSpawnEvent event = new TruenoNPCSpawnEvent(p, (TruenoNPC) this);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void destroy(Player p){
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[] {entityID});
        rmvFromTablist(p);
        sendPacket(packet, p);
        try{
            PacketPlayOutScoreboardTeam removescbpacket = new PacketPlayOutScoreboardTeam();
            Field f = removescbpacket.getClass().getDeclaredField("a");
            f.setAccessible(true);
            f.set(removescbpacket, this.gameprofile.getName());
            f.setAccessible(false);
            Field f2 = removescbpacket.getClass().getDeclaredField("h");
            f2.setAccessible(true);
            f2.set(removescbpacket, 1);
            f2.setAccessible(false);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(removescbpacket);
            rendered.remove(p);
            TruenoNPCDespawnEvent event = new TruenoNPCDespawnEvent(p, (TruenoNPC) this);
            Bukkit.getPluginManager().callEvent(event);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private void addToTablist(Player p){
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
        PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(gameprofile, 1, WorldSettings.EnumGamemode.NOT_SET, CraftChatMessage.fromString("§8[NPC] "+gameprofile.getName())[0]);
        @SuppressWarnings("unchecked")
        List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getValue(packet, "b");
        players.add(data);

        setValue(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
        setValue(packet, "b", players);

        sendPacket(packet, p);
    }

    private void rmvFromTablist(Player p){
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
        PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(gameprofile, 1, WorldSettings.EnumGamemode.NOT_SET, CraftChatMessage.fromString("§8[NPC] "+gameprofile.getName())[0]);
        @SuppressWarnings("unchecked")
        List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getValue(packet, "b");
        players.add(data);

        setValue(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
        setValue(packet, "b", players);

        sendPacket(packet, p);
    }

}

