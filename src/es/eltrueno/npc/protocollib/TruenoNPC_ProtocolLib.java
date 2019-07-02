package es.eltrueno.npc.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
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
import es.eltrueno.npc.protocollib.utils.MathHelper;
import es.eltrueno.npc.protocollib.wrapper.WrapperPlayServerEntityDestroy;
import es.eltrueno.npc.protocollib.wrapper.WrapperPlayServerPlayerInfo;
import es.eltrueno.npc.protocollib.wrapper.WrapperPlayServerScoreboardTeam;
import es.eltrueno.npc.skin.*;
import es.eltrueno.npc.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class TruenoNPC_ProtocolLib implements TruenoNPC {

    /**
     *
     *
     * @author el_trueno
     *
     *
     **/

    private static List<TruenoNPC_ProtocolLib> npcs = new ArrayList<TruenoNPC_ProtocolLib>();
    private static int id = 0;
    private static boolean taskstarted = false;
    private static Plugin plugin;
    private boolean deleted = false;
    private int npcid;
    private int entityID;
    private Location location;
    private String name;
    private GameProfile gameprofile;
    private TruenoNPCSkin skin;
    private List<Player> rendered = new ArrayList<Player>();
    private List<Player> waiting = new ArrayList<Player>();
    private HashMap<Player, GameProfile> player_profile = new HashMap<Player, GameProfile>();
    private HashMap<Player, SkinData> player_cache = new HashMap<Player, SkinData>();
    private List<Player> teams_created = new ArrayList<Player>();

    public static void startTask(Plugin plugin){
        if(!taskstarted){
            taskstarted = true;
            TruenoNPC_ProtocolLib.plugin = plugin;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for(TruenoNPC_ProtocolLib nmsnpc : npcs){
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
                                //final TruenoNPCDespawnEvent event = new TruenoNPCDespawnEvent(pl, nmsnpc);
                                //Bukkit.getPluginManager().callEvent(event);
                                //nmsnpc.rendered.remove(pl);
                            }
                        }
                    }
                }
            },0,30);

            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for(TruenoNPC_ProtocolLib nmsnpc : npcs) {
                        for (Player pl : Bukkit.getOnlinePlayers()) {
                            if(nmsnpc.rendered.contains(pl)) {
                                nmsnpc.destroy(pl);
                            }
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
        if(TruenoNPCApi.getCache() && this.skin.getSkinType()!=SkinType.PLAYER) {
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

    public TruenoNPC_ProtocolLib(Location location, TruenoNPCSkin skin){
        entityID = (int)Math.ceil(Math.random() * 1000) + 2000;
        npcid = id++;
        this.skin = skin;
        this.name = StringUtils.getRandomString();
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
                        GameProfile profile = getGameProfile(name, skinData);
                        if(skinData!=null){
                            if(player_profile.containsKey(p)){
                                player_profile.replace(p, profile);
                            }else{
                                player_profile.put(p, profile);
                            }
                            spawnEnttity(p, profile);
                        }else{
                            profile = getGameProfile(name, null);
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
                        GameProfile profile = getGameProfile(name, skinData);
                        if(skinData!=null){
                            setGameProfile(profile);
                            cacheSkin(skinData);
                            spawnEnttity(p, gameprofile);
                        } else{
                            profile = getGameProfile(name, null);
                            setGameProfile(profile);
                            spawnEnttity(p, gameprofile);
                        }
                    }
                });
            }
        }else{
            GameProfile profile = getGameProfile(name, cachedskin.getSkinData());
            setGameProfile(profile);
            spawnEnttity(p, gameprofile);
        }
    }

    private void spawnEnttity(Player p, GameProfile profile){
        final PacketContainer packet = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        StructureModifier<Object> spawnPacketModifier = packet.getModifier();

        spawnPacketModifier.write(0, this.entityID);
        spawnPacketModifier.write(1, profile.getId());
        spawnPacketModifier.write(2, MathHelper.floor(this.location.getX() * 32.0));
        spawnPacketModifier.write(3, MathHelper.floor(this.location.getY() * 32.0));
        spawnPacketModifier.write(4, MathHelper.floor(this.location.getZ() * 32.0));
        spawnPacketModifier.write(5, (byte) (this.location.getYaw() * 256.0f / 360.0f));
        spawnPacketModifier.write(6, (byte) (this.location.getPitch() * 256.0f / 360.0f));

        WrappedDataWatcher w = new WrappedDataWatcher();
        w.setObject(10, (Object) (byte) 127);
        packet.getDataWatcherModifier().write(0, w);
        try {
            addToTablist(p, profile);

            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet, false);
            if(!teams_created.contains(p)){
                PacketContainer scbpacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
                WrapperPlayServerScoreboardTeam scoreMods = new WrapperPlayServerScoreboardTeam(scbpacket);
                scoreMods.setMode(WrapperPlayServerScoreboardTeam.Mode.TEAM_CREATED);
                scoreMods.setName(profile.getName());
                scoreMods.setDisplayName(profile.getName());
                //scoreMods.setPrefix(profile.getName());
                //scoreMods.setSuffix(profile.getName());
                scoreMods.setNameTagVisibility("never");
                List<String> list = new ArrayList<>();
                list.add(profile.getName());
                scoreMods.setPlayers(list);
                ProtocolLibrary.getProtocolManager().sendServerPacket(p, scbpacket, false);
                teams_created.add(p);
            }
            final PacketContainer rotationpacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            StructureModifier<Object> rotationModifier = rotationpacket.getModifier();
            rotationModifier.write(0, this.entityID);
            rotationModifier.write(1, (byte) (this.location.getYaw() * 256.0f / 360.0f));
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, rotationpacket, false);
            Bukkit.getScheduler().runTaskLater(TruenoNPC_ProtocolLib.plugin, new Runnable() {
                @Override
                public void run() {
                    TruenoNPC_ProtocolLib.this.rmvFromTablist(p, profile);
                }
            }, 26L);
            this.rendered.add(p);
            this.waiting.remove(p);
            final TruenoNPCSpawnEvent event = new TruenoNPCSpawnEvent(p, this);
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void destroy(Player p){
        GameProfile profile = this.gameprofile;
        try{
            if(this.skin.getSkinType()==SkinType.PLAYER){
                if(this.player_profile.get(p)!=null){
                    profile = this.player_profile.get(p);
                    //rmvFromTablist(p, profile);
                }
            }else {
                //rmvFromTablist(p);
            }

           /* PacketPlayOutScoreboardTeam removescbpacket = new PacketPlayOutScoreboardTeam();
            Field f = removescbpacket.getClass().getDeclaredField("a");
            f.setAccessible(true);
            f.set(removescbpacket, profile.getName());
            f.setAccessible(false);
            Field f2 = removescbpacket.getClass().getDeclaredField("h");
            f2.setAccessible(true);
            f2.set(removescbpacket, 1);
            f2.setAccessible(false);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(removescbpacket);*/

            //PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(new int[] {entityID});
            destroyEntity(p);

            this.rendered.remove(p);
            TruenoNPCDespawnEvent event = new TruenoNPCDespawnEvent(p, (TruenoNPC) this);
            Bukkit.getPluginManager().callEvent(event);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private void destroyEntity(Player p){
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        WrapperPlayServerEntityDestroy packetEditor = new WrapperPlayServerEntityDestroy(packet);
        packetEditor.setEntityIds(new int[]{this.entityID});
        try {
            /*WrapperPlayServerScoreboardTeam scoreMods = new WrapperPlayServerScoreboardTeam(this.scbpacket);
            scoreMods.setMode(WrapperPlayServerScoreboardTeam.Mode.TEAM_REMOVED);
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, scoreMods.getHandle(), false);*/
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet, false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    private void addToTablist(Player p){
        final PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        WrapperPlayServerPlayerInfo packet_editor = new WrapperPlayServerPlayerInfo(packet);

        WrappedGameProfile profile = WrappedGameProfile.fromHandle(gameprofile);

        PlayerInfoData newData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.NOT_SET, WrappedChatComponent.fromText("§8[NPC] " + gameprofile.getName()));

        List<PlayerInfoData> players = (List<PlayerInfoData>) packet.getPlayerInfoDataLists().read(0);
        players.add(newData);

        packet_editor.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        packet_editor.setData(players);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet, false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void addToTablist(Player p, GameProfile gprofile){
        final PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        WrapperPlayServerPlayerInfo packet_editor = new WrapperPlayServerPlayerInfo(packet);

        WrappedGameProfile profile = WrappedGameProfile.fromHandle(gprofile);

        PlayerInfoData newData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.NOT_SET, WrappedChatComponent.fromText("§8[NPC] " + gprofile.getName()));

        List<PlayerInfoData> players = (List<PlayerInfoData>) packet.getPlayerInfoDataLists().read(0);
        players.add(newData);

        packet_editor.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        packet_editor.setData(players);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet, false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void rmvFromTablist(Player p){
        final PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        WrapperPlayServerPlayerInfo packet_editor = new WrapperPlayServerPlayerInfo(packet);

        WrappedGameProfile profile = WrappedGameProfile.fromHandle(gameprofile);
        PlayerInfoData newData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.NOT_SET, WrappedChatComponent.fromText("§8[NPC] " + gameprofile.getName()));

        List<PlayerInfoData> players = (List<PlayerInfoData>) packet.getPlayerInfoDataLists().read(0);
        players.add(newData);

        packet_editor.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        packet_editor.setData(players);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet, false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void rmvFromTablist(Player p, GameProfile gprofile){
        final PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        WrapperPlayServerPlayerInfo packet_editor = new WrapperPlayServerPlayerInfo(packet);

        WrappedGameProfile profile = WrappedGameProfile.fromHandle(gprofile);
        PlayerInfoData newData = new PlayerInfoData(profile, 1, EnumWrappers.NativeGameMode.NOT_SET, WrappedChatComponent.fromText("§8[NPC] " + gprofile.getName()));

        List<PlayerInfoData> players = (List<PlayerInfoData>) packet.getPlayerInfoDataLists().read(0);
        players.add(newData);

        packet_editor.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        packet_editor.setData(players);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet, false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


}

