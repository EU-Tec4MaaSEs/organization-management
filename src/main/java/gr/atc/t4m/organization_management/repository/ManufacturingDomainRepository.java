package gr.atc.t4m.organization_management.repository;

import gr.atc.t4m.organization_management.model.ManufacturingDomain;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface ManufacturingDomainRepository extends MongoRepository<ManufacturingDomain, String> {


    /**
     *  Find a domain specifically by its unique abbreviation index (e.g., "AMS").
     */
    Optional<ManufacturingDomain> findByAbbreviation(String abbreviation);

    @Aggregation(pipeline = {
        "{ '$group': { '_id': '$abbreviation' } }"
    })
    List<String> findDistinctAbbreviations();
}
