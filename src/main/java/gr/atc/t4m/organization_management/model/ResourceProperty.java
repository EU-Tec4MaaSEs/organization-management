package gr.atc.t4m.organization_management.model;

import lombok.Data;

@Data
public class ResourceProperty {
    private Long propertyID;
    private String propertyName;
    private Double value;
    private Double minValue;
    private Double maxValue;
}