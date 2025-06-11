package gr.atc.t4m.organization_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import gr.atc.t4m.organization_management.model.Organization;

public interface OrganizationRepository extends MongoRepository<Organization, String> {

    Optional<Organization> findByOrganizationName(String organizationName);

    Optional<Organization> findById(String organizationId);
    List<Organization> findByMaasRoleContaining(String role);

}

