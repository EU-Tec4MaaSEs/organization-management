package gr.atc.t4m.organization_management.model;
import lombok.Data;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

import org.springframework.data.annotation.Id;

@Data
@Document(collection = "organizations")
public class Organization {
    @Id
    private String organizationID;
    private String organizationName;
    private String address;
    private String contact;
    private String dsConnectorURL;
    private double organizationRating;
    private List<MaasRole> maasRole;
    private MaasConsumer maasConsumer;
    private MaasProvider maasProvider;
     @DBRef
    private List<ManufacturingResource> manufacturingResources;
}
