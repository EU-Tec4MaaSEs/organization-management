package gr.atc.t4m.organization_management.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DSTransferProcess {
    private String providerPid;
    private String consumerPid;
    private String callbackAddress;
    private String  state;
    @JsonProperty("@id")
    private String id;
    @JsonProperty("@type")
    private String type; 
}