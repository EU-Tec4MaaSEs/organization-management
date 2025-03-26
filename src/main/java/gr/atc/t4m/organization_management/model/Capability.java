package gr.atc.t4m.organization_management.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "capabilities")

public class Capability {
       @Id
    private String id;
    private long capabilityID;
    private String capabilityName;
}

