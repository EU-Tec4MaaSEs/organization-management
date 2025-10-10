package gr.atc.t4m.organization_management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneralizationRelation {
    private String first;
    private String second;
}
