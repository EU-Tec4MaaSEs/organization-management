package gr.atc.t4m.organization_management.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatasetEntry {
    @JsonProperty("@id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private List<DSDescription> description;

    @JsonProperty("keyword")
    private List<String> keywords;
    @JsonProperty("distribution")
    private List<DSDistribution> distributions;

    @JsonProperty("hasPolicy")
    private List<DSPolicy> hasPolicy;
}