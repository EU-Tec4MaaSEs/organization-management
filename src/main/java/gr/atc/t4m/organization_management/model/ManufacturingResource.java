package gr.atc.t4m.organization_management.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "manufacturingResources")
public class ManufacturingResource {
    @Id
    private String manufacturingResourceID;
    private String manufacturingResourceCode;
    private String manufacturingResourceTitle;
    private String capabilityDatasetID;
    private List<CapabilityEntry> capabilities;
    private String providerUrl;
    @Builder.Default
    private Instant timestamp = Instant.now();

}

