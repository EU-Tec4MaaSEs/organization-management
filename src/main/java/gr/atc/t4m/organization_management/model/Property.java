package gr.atc.t4m.organization_management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Property {
    private String name;
    private Object value;
    private String valueType;
    private String comment;
}
