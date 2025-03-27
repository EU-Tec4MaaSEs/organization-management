package gr.atc.t4m.organization_management.dto;

import java.util.List;

import gr.atc.t4m.organization_management.model.MaasConsumer;
import gr.atc.t4m.organization_management.model.MaasProvider;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
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
    private List<MaasRole> maasRole;
    private MaasConsumer maasConsumer;
    private MaasProvider maasProvider;
    private List<ManufacturingResource> manufacturingResources;

}
