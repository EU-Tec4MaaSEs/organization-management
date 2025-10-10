package gr.atc.t4m.organization_management.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DSDistribution {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("dcat:mediaType")
    private String mediaType;
    @JsonProperty("dcat:accessService")
    private DSAccessService accessService;

}
