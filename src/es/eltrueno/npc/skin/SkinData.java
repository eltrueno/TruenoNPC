package es.eltrueno.npc.skin;

public class SkinData {

    private String value;
    private String signature;

    public SkinData(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }
}
