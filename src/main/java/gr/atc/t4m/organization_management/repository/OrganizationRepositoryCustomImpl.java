package gr.atc.t4m.organization_management.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.model.Organization;

public class OrganizationRepositoryCustomImpl implements OrganizationRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public OrganizationRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Organization> filterProviders(ProviderSearchDTO filter) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter != null) {

            if (filter.getCountryCodes() != null && !filter.getCountryCodes().isEmpty()) {
                criteriaList.add(Criteria.where("maasProvider.shippingCountries.countryCode")
                        .all(filter.getCountryCodes()));
            }

            if (filter.getManufacturingServices() != null && !filter.getManufacturingServices().isEmpty()) {
                criteriaList.add(Criteria.where("maasProvider.manufacturingServices")
                        .all(filter.getManufacturingServices()));
            }
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Organization.class);
    }
}