
package gr.atc.t4m.organization_management.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Data
@Document(collection = "manufacturing_domains")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManufacturingDomain {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String abbreviation;      

    private String description;       

}