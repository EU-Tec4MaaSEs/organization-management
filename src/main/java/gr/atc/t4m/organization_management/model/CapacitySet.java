package gr.atc.t4m.organization_management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CapacitySet {
    private String availableCapacityRef;  // the production calendar submodel URL
}