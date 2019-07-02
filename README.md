# TruenoNPC
API for create NPCs without nametags in Spigot/Bukkit. It's compatible with 1.8 to 1.12 versions

## **NEW: Now works with ProtocolLib and its HIGHLY RECOMMENDED to use it with!!**


**Please if you are going to use this api, give me credits c:**

## Usage
1. Clone the repo and integrate it in your proyect.
2. Remove nms classes that you don't be using from .nms package and TruenoNPCApi (If your plugin is not compatible 1.8 - 1.12).
3. Create your npcs!
```java
//First create the skin
TruenoNPCSkin skin = TruenoNPCSkinBuilder.fromUsername(this.plugin, p.getName());
TruenoNPCSkin skin = TruenoNPCSkinBuilder.fromUUID(this.plugin, p.getUniqueId().toString());
TruenoNPCSkin skin = TruenoNPCSkinBuilder.fromMineskin(this.plugin, 131234);

//Now create the NPC ussing the skin
TruenoNPC npc = TruenoNPCApi.createNPC(main.getPlugin(), p.getLocation(), skin);
```
Once you create the npc, you don't have to worry about more, it will show new players, will respawn when it's necessary, etc.
There are three events to listen: on spawn, on despawn and on interact.

## Thanks to
* Thanks to **aadnk** for TinyProtocol:
https://github.com/aadnk/ProtocolLib/tree/master/modules/TinyProtocol
* Thanks to **Yonas** for McAPI.de: https://mcapi.de/
* Thanks to **dmulloy2** for ProtocolLib: https://github.com/dmulloy2/ProtocolLib/

## Donate me
If you like my work, please, consider donating 

[![](https://i.imgur.com/GMqXvEZ.png?1)](https://www.paypal.me/eltrueno)

## Contact me

[![](https://www.asian-voice.com/bundles/core/images/icon-twitter.png?v4)](http://twitter.eltrueno.es)
