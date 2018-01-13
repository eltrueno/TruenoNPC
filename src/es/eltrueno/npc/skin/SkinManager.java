package es.eltrueno.npc.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class SkinManager {

    private static JsonObject getJsonResponse(String url){
        URL ipAdress;
        JsonObject rootobj = null;
        try {
            ipAdress = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(ipAdress.openStream()));
            String jsonresponse = in.readLine();
            JsonParser jsonParser = new JsonParser();
            JsonElement root = jsonParser.parse(jsonresponse);
            rootobj = root.getAsJsonObject();
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return rootobj;
    }

    public static void getUUIDFromName(final Plugin plugin, String name, Callback<String> callback){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                long unixTime = System.currentTimeMillis() / 1000L;
                JsonObject jsonresponse = getJsonResponse("https://api.mojang.com/users/profiles/minecraft/"+name+"?at="+unixTime);
                if(jsonresponse!=null && jsonresponse.get("error")==null){
                    callback.call(jsonresponse.get("id").getAsString());
                }else{
                    callback.call(null);
                }
            }
        });
    }

    public static void getSkinFromMojangAsync(final Plugin plugin, final String identifier, final SkinDataReply skinreply){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                if(identifier.length()>16){
                    JsonObject jsonresponse = getJsonResponse("https://sessionserver.mojang.com/session/minecraft/profile/"+identifier+"?unsigned=false");
                    if(jsonresponse!=null && !jsonresponse.has("error")){
                        JsonObject prop = jsonresponse.getAsJsonArray("properties").get(0).getAsJsonObject();
                        String value = prop.get("value").getAsString();
                        String signature = prop.get("signature").getAsString();
                        skinreply.done(new SkinData(value, signature));
                    }else{
                        skinreply.done(null);
                    }
                }else{
                    getUUIDFromName(plugin, identifier, new Callback<String>() {
                        @Override
                        public void call(String uuid) {
                            if(uuid!=null){
                                JsonObject jsonresponse = getJsonResponse("https://sessionserver.mojang.com/session/minecraft/profile/"+uuid+"?unsigned=false");
                                if(jsonresponse!=null && !jsonresponse.has("error")){
                                    JsonObject prop = jsonresponse.getAsJsonArray("properties").get(0).getAsJsonObject();
                                    String value = prop.get("value").getAsString();
                                    String signature = prop.get("signature").getAsString();
                                    skinreply.done(new SkinData(value, signature));
                                }else{
                                    skinreply.done(null);
                                }
                            }else{
                                skinreply.done(null);
                            }
                        }
                    });
                }
            }
        });
    }

    public static void getSkinFromMCAPIAsync(final Plugin plugin, final String identifier, final SkinDataReply skinreply){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                JsonObject jsonresponse = getJsonResponse("https://mcapi.de/api/user/"+identifier);
                if(jsonresponse!=null
                        && jsonresponse.get("result").getAsJsonObject().get("status").getAsString().equalsIgnoreCase("Ok")
                        && jsonresponse.get("premium").getAsBoolean()){
                    JsonObject textureProperty = jsonresponse.getAsJsonObject("properties").getAsJsonArray("raw").get(0).getAsJsonObject();
                    String value = textureProperty.get("value").getAsString();
                    String signature = textureProperty.get("signature").getAsString();
                    skinreply.done(new SkinData(value, signature));
                }else{
                    skinreply.done(null);
                }
            }
        });
    }

    public static void getSkinFromMineskinAsync(final Plugin plugin, final int mineskinid, final SkinDataReply skinreply){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                JsonObject jsonresponse = getJsonResponse("https://api.mineskin.org/get/id/"+mineskinid);
                if(jsonresponse!=null && !jsonresponse.has("error")){
                    JsonObject textureProperty = jsonresponse.getAsJsonObject("data").getAsJsonObject("texture");
                    String value = textureProperty.get("value").getAsString();
                    String signature = textureProperty.get("signature").getAsString();
                    skinreply.done(new SkinData(value, signature));
                }else{
                    skinreply.done(null);
                }
            }
        });
    }


}
