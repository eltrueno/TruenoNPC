package es.eltrueno.npc.nms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import es.eltrueno.npc.TruenoNPC;
import es.eltrueno.npc.TruenoNPCApi;
import es.eltrueno.npc.event.TruenoNPCDespawnEvent;
import es.eltrueno.npc.event.TruenoNPCSpawnEvent;
import es.eltrueno.npc.skin.*;
import es.eltrueno.npc.utils.StringUtils;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.*;

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
    private static boolean taskstarted = false;
    private static Plugin plugin;
    private PacketPlayOutScoreboardTeam scbpacket;
    private boolean deleted = false;
    private int npcid;
    private int entityID;
    private Location location;
    private GameProfile gameprofile;
    private TruenoNPCSkin skin;
    private List<Player> rendered = new ArrayList<Player>();
    private List<Player> waiting = new ArrayList<Player>();
    private HashMap<Player, GameProfile> player_profile = new HashMap<Player, GameProfile>();
    private HashMap<Player, SkinData> player_cache = new HashMap<Player, SkinData>();

    public static void startTask(Plugin plugin){
        if(!taskstarted){
            taskstarted = true;
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
                                    if(!nmsnpc.waiting.contains(pl)){
                                        nmsnpc.waiting.add(pl);
                                        nmsnpc.spawn(pl);
                                    }
                                }
                            }else{
                                nmsnpc.destroy(pl);
                            }
                        }
                    }
                }
            },0,30);

            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for(TruenoNPC_v1_8_r3 nmsnpc : npcs) {
                        for (Player pl : Bukkit.getOnlinePlayers()) {
                            nmsnpc.destroy(pl);
                        }
                    }
                }
            },20*(60*5),20*(60*5));
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
    public int getEntityID(Player p){
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


    private JsonObject getChacheFile(Plugin plugin){
        File file = new File(plugin.getDataFolder().getPath()+"/truenonpcdata.json");
        if(file.exists()){
            try{
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(new FileReader(file));
                return jsonElement.getAsJsonObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }else return null;
    }

    public static void removeCacheFile(){
        File file = new File(plugin.getDataFolder().getPath()+"/truenonpcdata.json");
        if(file.exists()){
            file.delete();
        }
    }

    private JsonSkinData getCachedSkin(){
        if(TruenoNPCApi.getCache() && this.skin.getSkinType()!= SkinType.PLAYER) {
            JsonObject jsonFile = getChacheFile(plugin);
            JsonArray skindata = null;
            try{
                skindata = jsonFile.getAsJsonArray("skindata");
            }catch(Exception ex){

            }
            JsonSkinData skin = null;
            if(skindata!=null){
                Iterator it = skindata.iterator();
                while(it.hasNext()){
                    JsonElement element = (JsonElement) it.next();
                    if(element.getAsJsonObject().get("id").getAsInt()==this.npcid){
                        String value = element.getAsJsonObject().get("value").getAsString();
                        String signature = element.getAsJsonObject().get("signature").getAsString();
                        long updated = element.getAsJsonObject().get("updated").getAsLong();
                        SkinData data = new SkinData(value, signature);
                        skin = new JsonSkinData(data, updated);
                    }
                }
            }
            return skin;
        }
        return null;
    }

    private void cacheSkin(SkinData skindata){
        if(TruenoNPCApi.getCache() && this.skin.getSkinType()!=SkinType.PLAYER) {
            JsonObject jsonFile = getChacheFile(plugin);
            JsonArray newskindata = new JsonArray();
            if (jsonFile != null) {
                JsonArray oldskindata = jsonFile.getAsJsonArray("skindata");
                Iterator it = oldskindata.iterator();
                while (it.hasNext()) {
                    JsonElement element = (JsonElement) it.next();
                    if (element.getAsJsonObject().get("id").getAsInt() == this.npcid) {
                    } else {
                        newskindata.add(element);
                    }
                }
            }
            JsonObject skin = new JsonObject();
            Date actualdate = new Date();
            skin.addProperty("id", this.npcid);
            skin.addProperty("value", skindata.getValue());
            skin.addProperty("signature", skindata.getSignature());
            skin.addProperty("updated", actualdate.getTime());
            newskindata.add(skin);

            JsonObject obj = new JsonObject();
            obj.add("skindata", newskindata);
            try {
                plugin.getDataFolder().mkdir();
                File file = new File(plugin.getDataFolder().getPath() + "/truenonpcdata.json");
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write(obj.toString());
                writer.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private GameProfile getGameProfile(String profilename, SkinData skindata){
        if(skindata!=null) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), profilename);
            profile.getProperties().put("textures", new Property("textures", skindata.getValue(), skindata.getSignature()));
            return profile;
        }else{
            GameProfile profile = new GameProfile(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"), profilename);
            profile.getProperties().put("textures", new Property("textures", "eyJ0aW1lc3RhbXAiOjE1MTUzMzczNTExMjk" +
                    "sInByb2ZpbGVJZCI6Ijg2NjdiYTcxYjg1YTQwMDRhZjU0NDU3YTk3MzRlZWQ3IiwicHJvZmlsZU5hbWUiOiJTdGV2ZSIsInNpZ2" +
                    "5hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQub" +
                    "mV0L3RleHR1cmUvNDU2ZWVjMWMyMTY5YzhjNjBhN2FlNDM2YWJjZDJkYzU0MTdkNTZmOGFkZWY4NGYxMTM0M2RjMTE4OGZlMTM4" +
                    "In0sIkNBUEUiOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iNzY3ZDQ4MzI1ZWE1MzI0NTY" +
                    "xNDA2YjhjODJhYmJkNGUyNzU1ZjExMTUzY2Q4NWFiMDU0NWNjMiJ9fX0", "oQHxJ9U7oi/JOeC5C9wtLcoqQ/Uj5j8mfSL" +
                    "aPo/zMQ1GP/IjB+pFmfy5JOOaX94Ia98QmLLd+AYacnja60DhO9ljrTtL/tM7TbXdWMWW7A2hJkEKNH/wnBkSIm0EH8WhH+m9+8" +
                    "2pkTB3h+iDGHyc+Qb9tFXWLiE8wvdSrgDHPHuQAOgGw6BfuhdSZmv2PGWXUG02Uvk6iQ7ncOIMRWFlWCsprpOw32yzWLSD8UeUU" +
                    "io6SlUyuBIO+nJKmTRWHnHJgTLqgmEqBRg0B3GdML0BncMlMHq/qe9x6gTlDCJATLTFJg4kDEF+kUa4+P0BDdPFrgApFUeK4Bz1" +
                    "w7Qxls4zKQQJNJw58nhvKk/2yQnFOOUqfRx/DeIDLCGSTEJr4VjKIVThnvkocUDsH8DLk4/Xt9qKWh3ZxXtxoKPDvFP5iyxIOfZ" +
                    "dkZu/H0qlgRTqF8RP8AnXf2lgnarfty8G7q7/4KQwWC1CIn9MmaMwv3MdFDlwdAjHhvpyBYYTnL11YDBSUg3b6+QmrWWm1DXcHr" +
                    "wkcS0HI82VHYdg8uixzN57B3DGRSlh2qBWHJTb0zF8uryveCZppHl/ULa/2vAt6XRXURniWU4cTQKQAGqjByhWSbUM0XHFgcuKj" +
                    "GFVlJ4HEzBiXgY3PtRF6NzfsUZ2gQI9o12x332USZiluYrf+OLhCa8="));
            return profile;
        }
    }

    public TruenoNPC_v1_8_r3(Location location, TruenoNPCSkin skin){
        entityID = (int)Math.ceil(Math.random() * 1000) + 2000;
        npcid = id++;
        this.skin = skin;
        this.location = location;
        if(!npcs.contains(this)){
            npcs.add(this);
        }
    }

    @Override
    public TruenoNPCSkin getSkin(){
        return this.skin;
    }

    @Override
    public void delete(){
        npcs.remove(this);
        for(Player p : Bukkit.getOnlinePlayers()) {
            destroy(p);
        }
        this.deleted = true;
    }

    private void setGameProfile(GameProfile profile){
        this.gameprofile = profile;
    }

    private void spawn(Player p){
        Date actualdate = new Date();
        JsonSkinData cachedskin = getCachedSkin();
        if(cachedskin==null || (((actualdate.getTime())-(getCachedSkin().getTimeUpdated())) >= 518400)){
            if(this.skin.getSkinType()== SkinType.PLAYER){
                this.skin.getSkinDataAsync(new SkinDataReply() {
                    @Override
                    public void done(SkinData skinData) {
                        GameProfile profile = getGameProfile(StringUtils.getRandomString(), skinData);
                        if(skinData!=null){
                            if(player_profile.containsKey(p)){
                                player_profile.replace(p, profile);
                            }else{
                                player_profile.put(p, profile);
                            }
                            if(player_cache.containsKey(p)){
                                player_cache.replace(p, skinData);
                            }else{
                                player_cache.put(p, skinData);
                            }
                            spawnEnttity(p, profile);
                        }else{
                            profile = getGameProfile(StringUtils.getRandomString(), null);
                            if(player_cache.containsKey(p)){
                                profile = getGameProfile(StringUtils.getRandomString(), player_cache.get(p));
                            }
                            if(player_profile.containsKey(p)){
                                player_profile.replace(p, profile);
                            }else{
                                player_profile.put(p, profile);
                            }
                            spawnEnttity(p, profile);
                        }
                    }
                }, p);
            }else{
                this.skin.getSkinDataAsync(new SkinDataReply() {
                    @Override
                    public void done(SkinData skinData) {
                        GameProfile profile = getGameProfile(StringUtils.getRandomString(), skinData);
                        if(skinData!=null){
                            setGameProfile(profile);
                            cacheSkin(skinData);
                            spawnEnttity(p);
                        } else{
                            profile = getGameProfile(StringUtils.getRandomString(), null);
                            setGameProfile(profile);
                            spawnEnttity(p);
                        }
                    }
                });
            }
        }else{
            GameProfile profile = getGameProfile(StringUtils.getRandomString(), cachedskin.getSkinData());
            setGameProfile(profile);
            spawnEnttity(p);
        }
    }

    private void spawnEnttity(Player p){
        GameProfile profile = this.gameprofile;
        PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();

        setValue(packet, "a", entityID);
        setValue(packet, "b", profile.getId());
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
            setValue(scbpacket, "b",profile.getName());
            setValue(scbpacket, "a",profile.getName());
            setValue(scbpacket, "e","never");
            setValue(scbpacket, "i",1);
            Field f = scbpacket.getClass().getDeclaredField("g");
            f.setAccessible(true);
            ((Collection) f.get(scbpacket)).add(profile.getName());
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
        },26);
        this.rendered.add(p);
        this.waiting.remove(p);
        TruenoNPCSpawnEvent event = new TruenoNPCSpawnEvent(p, (TruenoNPC) this);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void spawnEnttity(Player p, GameProfile profile){
        PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();

        setValue(packet, "a", entityID);
        setValue(packet, "b", profile.getId());
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
            setValue(scbpacket, "b",profile.getName());
            setValue(scbpacket, "a",profile.getName());
            setValue(scbpacket, "e","never");
            setValue(scbpacket, "i",1);
            Field f = scbpacket.getClass().getDeclaredField("g");
            f.setAccessible(true);
            ((Collection) f.get(scbpacket)).add(profile.getName());
            sendPacket(scbpacket, p);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        addToTablist(p, profile);
        sendPacket(packet, p);
        PacketPlayOutEntityHeadRotation rotationpacket = new PacketPlayOutEntityHeadRotation();
        setValue(rotationpacket, "a", entityID);
        setValue(rotationpacket, "b", (byte) ((int) (location.getYaw() * 256.0F / 360.0F)));
        sendPacket(rotationpacket, p);
        Bukkit.getScheduler().runTaskLater(TruenoNPC_v1_8_r3.plugin, new Runnable(){
            @Override
            public void run() {
                rmvFromTablist(p, player_profile.get(p));
            }
        },26);
        this.rendered.add(p);
        this.waiting.remove(p);
        TruenoNPCSpawnEvent event = new TruenoNPCSpawnEvent(p, (TruenoNPC) this);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void destroy(Player p){
        GameProfile profile = this.gameprofile;
        try{
            if(this.skin.getSkinType()==SkinType.PLAYER){
                if(this.player_profile.get(p)!=null){
                    profile = this.player_profile.get(p);
                    rmvFromTablist(p, profile);
                }
            }else {
                rmvFromTablist(p);
            }

            PacketPlayOutScoreboardTeam removescbpacket = new PacketPlayOutScoreboardTeam();
            Field f = removescbpacket.getClass().getDeclaredField("a");
            f.setAccessible(true);
            f.set(removescbpacket, profile.getName());
            f.setAccessible(false);
            Field f2 = removescbpacket.getClass().getDeclaredField("h");
            f2.setAccessible(true);
            f2.set(removescbpacket, 1);
            f2.setAccessible(false);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(removescbpacket);

            PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[] {entityID});
            sendPacket(packet, p);

            this.rendered.remove(p);
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

    private void addToTablist(Player p, GameProfile profile){
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
        PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(profile, 1, WorldSettings.EnumGamemode.NOT_SET, CraftChatMessage.fromString("§8[NPC] "+profile.getName())[0]);
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

    private void rmvFromTablist(Player p, GameProfile profile){
        PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
        PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(profile, 1, WorldSettings.EnumGamemode.NOT_SET, CraftChatMessage.fromString("§8[NPC] "+profile.getName())[0]);
        @SuppressWarnings("unchecked")
        List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getValue(packet, "b");
        players.add(data);

        setValue(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
        setValue(packet, "b", players);

        sendPacket(packet, p);
    }

}

