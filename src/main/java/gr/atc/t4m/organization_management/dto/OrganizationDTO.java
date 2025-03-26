package gr.atc.t4m.organization_management.dto;

import gr.atc.t4m.organization_management.model.MaasConsumer;
import gr.atc.t4m.organization_management.model.MaasProvider;
import gr.atc.t4m.organization_management.model.MaasRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDTO {
    private String organizationName;
    private String address;
    private String contact;
    private String dsConnectorURL;
    private double organizationRating;
    private MaasRole maasRole;
    private MaasConsumer maasConsumer;
    private MaasProvider maasProvider;
}
