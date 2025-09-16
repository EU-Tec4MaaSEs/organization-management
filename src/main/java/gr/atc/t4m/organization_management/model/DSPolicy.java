package gr.atc.t4m.organization_management.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DSPolicy {
        @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("odrl:providerId")
    private String providerId;

    @JsonProperty("odrl:permission")
    private List<DSPermission> permissions;
}
