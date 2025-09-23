package gr.atc.t4m.organization_management.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DSDescription {
    @JsonProperty("@value")
    private String value;

    @JsonProperty("@language")
    private String language;

}