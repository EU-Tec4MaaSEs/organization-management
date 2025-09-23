package gr.atc.t4m.organization_management.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import gr.atc.t4m.organization_management.model.ManufacturingResource;

public interface ManufacturingResourceRepository extends MongoRepository<ManufacturingResource, String> {
    Optional<ManufacturingResource> findByManufacturingResourceID(String manufacturingResourceId);

}


