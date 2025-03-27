package gr.atc.t4m.organization_management.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MaasRole {
    CONSUMER(1,"CONSUMER"),
    PROVIDER(2, "PROVIDER");

   private final int value;
    private final String name;

    MaasRole(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static MaasRole fromValue(Object input) {
        if (input instanceof Integer integer) {
            for (MaasRole role : values()) {
                if (role.value == integer) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Invalid MaasRole value: " + integer);
        } else if (input instanceof String strValue) { //
            for (MaasRole role : values()) {
                if (role.name.equalsIgnoreCase(strValue)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Invalid MaasRole name: " + strValue);
        }
        throw new IllegalArgumentException("Invalid MaasRole input type: " + input);
    }
}