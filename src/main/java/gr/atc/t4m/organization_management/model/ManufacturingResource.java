package gr.atc.t4m.organization_management.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "manufacturingResources")
public class ManufacturingResource {
    @Id
    private String manufacturingResourceID;
    private String manufacturingResourceCode;
    private String manufacturingResourceTitle;
    private String capabilityDatasetID;
    private List<CapabilityEntry> capabilities;

    private List<ResourceProperty> resourceProperties; 

}

