package gr.atc.t4m.organization_management.repository;

import java.util.List;

import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.model.Organization;

public interface OrganizationRepositoryCustom {
    List<Organization> filterProviders(ProviderSearchDTO filter);
}
