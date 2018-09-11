package es.eltrueno.npc.skin;

import org.bukkit.plugin.Plugin;

public class TruenoNPCSkinBuilder {

    public static TruenoNPCSkin fromUsername(Plugin plugin, String username){
        return new TruenoNPCSkin(plugin, SkinType.IDENTIFIER, username);
    }

    public static TruenoNPCSkin fromUUID(Plugin plugin, String uuid){
        return new TruenoNPCSkin(plugin, SkinType.IDENTIFIER, uuid);
    }

    public static TruenoNPCSkin fromMineskin(Plugin plugin, int mineskinid){
        return new TruenoNPCSkin(plugin, SkinType.MINESKINID, String.valueOf(mineskinid));
    }

    public static TruenoNPCSkin fromPlayer(Plugin plugin){
        return new TruenoNPCSkin(plugin, SkinType.PLAYER);
    }

}
