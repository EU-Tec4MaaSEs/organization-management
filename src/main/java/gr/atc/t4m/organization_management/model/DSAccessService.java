package gr.atc.t4m.organization_management.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DSAccessService {
      @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("endpointDescription")
    private String endpointDescription;

    @JsonProperty("endpointURL")
    private String endpointUrl;
}
