package gr.atc.t4m.organization_management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogDTO {
    private String providerUrl;
    private Integer page = 1;
    private Integer size = 10;
    @JsonProperty("datasetId")
    private String datasetId;
    private String title;
    private String description;
    private String language;
    private String rights;
    private String keywords;
    private Boolean refresh = true;
}
