package gr.atc.t4m.organization_management.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DSPermission {
    @JsonProperty("@type")
    private String type;

    @JsonProperty("odrl:action")
    private String action;

    @JsonProperty("odrl:constraint")
    private List<String> constraint;
}
