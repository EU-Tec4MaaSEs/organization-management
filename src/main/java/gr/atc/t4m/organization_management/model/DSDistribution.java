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

    @JsonProperty("mediaType")
    private String mediaType;
    @JsonProperty("accessService")
    private DSAccessService accessService;

    private String format;
}
