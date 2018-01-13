package es.eltrueno.npc.skin;

public class JsonSkinData {

    private int id;
    private String value;
    private String signature;

    public JsonSkinData(int id, String value, String signature) {
        this.id = id;
        this.value = value;
        this.signature = signature;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public int getID(){
        return this.id;
    }

}
