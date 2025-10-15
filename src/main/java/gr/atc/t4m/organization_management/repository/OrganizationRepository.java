package gr.atc.t4m.organization_management.repository;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import gr.atc.t4m.organization_management.model.Organization;

public interface OrganizationRepository extends MongoRepository<Organization, String>, OrganizationRepositoryCustom {

    Optional<Organization> findByOrganizationName(String organizationName);

    Optional<Organization> findById(String organizationId);
    List<Organization> findByMaasRoleContaining(String role);

     @Query("{ 'manufacturingResources.$id': { $in: ?0 } }")
     List<Organization> findByManufacturingResourceObjectIds(List<ObjectId> ids);
}

