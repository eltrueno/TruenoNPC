package es.eltrueno.npc.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class GameProfileUtils {

    /**
     *
     *
     * @author el_trueno
     *
     *
     **/

    private static JsonObject getJsonResponse(String name){
        URL ipAdress;
        JsonObject rootobj = null;
        try {
            ipAdress = new URL("https://mcapi.de/api/user/"+name);
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

    private static String[] getMcApiSkinData(String name) {
        JsonObject jsonresponse = getJsonResponse(name);
        if(jsonresponse!=null) {
            if (jsonresponse.getAsJsonObject("result").get("status").getAsString().equalsIgnoreCase("Ok")) {
                JsonObject textureProperty = jsonresponse.getAsJsonObject("properties").getAsJsonArray("raw").get(0).getAsJsonObject();
                String texture = textureProperty.get("value").getAsString();
                String signature = textureProperty.get("signature").getAsString();
                return new String[]{texture, signature};
            } else {
                return null;
            }
        }else{
            return null;
        }
    }

    public static GameProfile getGameProfileFromName(String skinname, String profilename){
        String[] a = getMcApiSkinData(skinname);
        if(a!=null) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), profilename);
            profile.getProperties().put("textures", new Property("textures", a[0], a[1]));
            return profile;
        }else{
            GameProfile profile = new GameProfile(UUID.randomUUID(), profilename);
            return profile;
        }
    }

}
