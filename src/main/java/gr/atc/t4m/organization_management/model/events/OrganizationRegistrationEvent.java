package gr.atc.t4m.organization_management.model.events;

import java.util.List;

import gr.atc.t4m.organization_management.model.MaasRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event triggered upon organization registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRegistrationEvent {
    private String id; //same as organization ID
    private String name;
    private String email;
    private String contact;
    private List<MaasRole> role;
    private String dataSpaceConnectorUrl;
    private String verifiableCredential;
    private String userId;

}

