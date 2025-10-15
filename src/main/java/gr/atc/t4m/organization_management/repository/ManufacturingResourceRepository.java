package gr.atc.t4m.organization_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import gr.atc.t4m.organization_management.model.ManufacturingResource;

public interface ManufacturingResourceRepository extends MongoRepository<ManufacturingResource, String> {
    Optional<ManufacturingResource> findByManufacturingResourceID(String manufacturingResourceId);

    @Query("{ 'capabilities': { $elemMatch: { name: ?0, type: ?1 } } }")
    List<ManufacturingResource> findByCapability(String capabilityName, String capabilityType);

    @Query("{ $and: [ " +
            "{ 'capabilities': { $elemMatch: { name: ?0, type: 'Primary' } } }, " +
            "{ 'capabilities': { $elemMatch: { name: ?1, type: 'Secondary' } } } " +
            "] }")
    List<ManufacturingResource> findByPrimaryAndSecondaryCapabilities(
            String primaryCapability,
            String secondaryCapability);

}


