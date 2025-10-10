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

    @JsonProperty("dct:title")
    private String title;

    @JsonProperty("dct:description")
    private List<DSDescription> description;

    @JsonProperty("dcat:keyword")
    private List<String> keywords;
    @JsonProperty("dcat:distribution")
    private List<DSDistribution> distributions;

    @JsonProperty("odrl:hasPolicy")
    private DSPolicy policy;
}