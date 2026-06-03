package gr.atc.t4m.organization_management.dto;

import lombok.Data;

@Data
public class VerifiableCredentialInputDTO {
    String did;
    String name;
    String country;
}
