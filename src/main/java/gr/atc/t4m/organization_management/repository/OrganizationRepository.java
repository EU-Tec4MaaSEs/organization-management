package gr.atc.t4m.organization_management.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import gr.atc.t4m.organization_management.model.Organization;

public interface OrganizationRepository extends MongoRepository<Organization, String> {
}

