package gr.atc.t4m.organization_management.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ManufacturingServices {
    AM(1, "AM"),
    MACH(2, "MACH"),
    PIM(3, "PIM");

    private final int value;
    private final String name;

    ManufacturingServices(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static ManufacturingServices fromValue(Object input) {
        if (input instanceof Integer integer) { // Enhanced instanceof check
            for (ManufacturingServices service : values()) {
                if (service.value == integer) {
                    return service;
                }
            }
            throw new IllegalArgumentException("Invalid ManufacturingServices value: " + integer);
        } else if (input instanceof String strValue) { // Enhanced instanceof check
            for (ManufacturingServices service : values()) {
                if (service.name.equalsIgnoreCase(strValue)) {
                    return service;
                }
            }
            throw new IllegalArgumentException("Invalid ManufacturingServices name: " + strValue);
        }
        throw new IllegalArgumentException("Invalid ManufacturingServices input type: " + input);
    }
}
