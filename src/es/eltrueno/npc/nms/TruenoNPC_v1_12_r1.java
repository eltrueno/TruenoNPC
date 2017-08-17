package es.eltrueno.npc.nms;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import es.eltrueno.npc.TruenoNPC;
import es.eltrueno.npc.event.TruenoNPCDespawnEvent;
import es.eltrueno.npc.event.TruenoNPCSpawnEvent;
import es.eltrueno.npc.utils.GameProfileUtils;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class TruenoNPC_v1_12_r1 implements TruenoNPC {

    /**
     *
     *
     * @author el_trueno
     *
     *
     **/

    private static List<TruenoNPC_v1_12_r1> npcs = new ArrayList<TruenoNPC_v1_12_r1>();
    private static int id = 0;
    private static boolean taskstarted = false;
    private static Plugin plugin;
    private boolean deleted = false;
    private int npcid;
    private int entityID;
    private EntityPlayer npcentity;
    private Location location;
    private GameProfile gameprofile;
    private List<Player> rendered = new ArrayList<Player>();

    public static void startTask(Plugin plugin){
        if(!taskstarted){
            taskstarted = true;
            TruenoNPC_v1_12_r1.plugin = plugin;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for(TruenoNPC_v1_12_r1 nmsnpc : npcs){
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
            field.setAccessible(false);
        }catch(Exception e){}
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

    private String getRandomString (int lenght){
        String randStr = "";
        long milis = new java.util.GregorianCalendar().getTimeInMillis();
        Random r = new Random(milis);
        int i = 0;
        while ( i < lenght){
            char c = (char)r.nextInt(255);
            if ( (c >= '0' && c <='9') || (c >='A' && c <='Z') ){
                randStr += c;
                i ++;
            }
        }
        return randStr;
    }

    public TruenoNPC_v1_12_r1(Location location, String skin){
        entityID = (int)Math.ceil(Math.random() * 1000) + 2000;
        npcid = id++;
        gameprofile = GameProfileUtils.getGameProfileFromName(skin,"§8[NPC] "+getRandomString(8));
        this.location = location;
        if(!npcs.contains(this)){
            npcs.add(this);
        }
    }

    @Override
    public void delete(){
        npcs.remove(this);
        for(Player p : Bukkit.getOnlinePlayers()) {
            destroy(p);
        }
        this.deleted = true;
    }

    private void spawn(Player p){
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer nmsWorld = ((CraftWorld) getLocation().getWorld()).getHandle();
        EntityPlayer npcentity = new EntityPlayer(nmsServer, nmsWorld, gameprofile, new PlayerInteractManager(nmsWorld));
        npcentity.setLocation(location.getX(),location.getY(),location.getZ(),(byte)location.getYaw(),(byte)location.getPitch());
        this.entityID = npcentity.getId();
        this.npcentity = npcentity;
        PacketPlayOutNamedEntitySpawn spawnpacket = new PacketPlayOutNamedEntitySpawn(npcentity);
        DataWatcher watcher = npcentity.getDataWatcher();
        watcher.set(new DataWatcherObject<>(13, DataWatcherRegistry.a), (byte) 0xFF);
        setValue(spawnpacket, "h", watcher);

        PacketPlayOutScoreboardTeam scbpacket = new PacketPlayOutScoreboardTeam();
        try {
            Collection<String> plys = Lists.newArrayList();
            plys.add(gameprofile.getName());
            setValue(scbpacket, "i",0);
            setValue(scbpacket, "b",gameprofile.getName());
            setValue(scbpacket, "a",gameprofile.getName());
            setValue(scbpacket, "e","never");
            setValue(scbpacket, "j",1);
            setValue(scbpacket, "h", plys);
            sendPacket(scbpacket, p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        addToTablist(p);
        sendPacket(spawnpacket, p);

        PacketPlayOutEntityHeadRotation rotationpacket = new PacketPlayOutEntityHeadRotation();
        setValue(rotationpacket, "a", entityID);
        setValue(rotationpacket, "b", (byte) ((int) (location.getYaw() * 256.0F / 360.0F)));
        sendPacket(rotationpacket, p);
        Bukkit.getScheduler().runTaskLater(TruenoNPC_v1_12_r1.plugin, new Runnable(){
            @Override
            public void run() {
                rmvFromTablist(p);
            }
        },26);
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
            setValue(removescbpacket,"a", this.gameprofile.getName());
            setValue(removescbpacket,"i", 1);
            sendPacket(removescbpacket, p);
            rendered.remove(p);
            TruenoNPCDespawnEvent event = new TruenoNPCDespawnEvent(p, (TruenoNPC) this);
            Bukkit.getPluginManager().callEvent(event);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private void addToTablist(Player p){
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npcentity);
        sendPacket(packet, p);
    }

    private void rmvFromTablist(Player p){
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, npcentity);
        sendPacket(packet, p);
    }

}

