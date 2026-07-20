package gr.atc.t4m.organization_management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import gr.atc.t4m.organization_management.model.SubmodelWrapper.Qualifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CapabilityEntry {
    private String name;
    private String type; // Primary / Secondary
    private boolean offered;
    private String comment;
    private List<Property> properties = new ArrayList<>();
    private GeneralizationRelation generalizedBy;
    private CapacitySet capacitySet;
    private String semanticId;
    private String supplementalSemanticIds;
    private List<Qualifier> qualifiers = new ArrayList<>();
}

