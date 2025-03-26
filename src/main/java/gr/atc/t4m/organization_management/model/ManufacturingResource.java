package gr.atc.t4m.organization_management.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "manufacturingResources")
public class ManufacturingResource {
    @Id
    private String id;
    private long manufacturingResourceID;
    private String manufacturingResourceCode;
    private String manufacturingResourceName;
    private long capabilityDatasetID;
    @DBRef
    private List<Capability> primaryCapabilities;
    
    @DBRef
    private List<Capability> secondaryCapabilities;

    private List<ResourceProperty> resourceProperties; 

}

