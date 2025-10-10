package gr.atc.t4m.organization_management.repository;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.Organization;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public OrganizationRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Organization> filterProviders(ProviderSearchDTO filter) {
        Criteria criteria = Criteria.where("maasRole").is(MaasRole.PROVIDER);

        if (filter.getCountryCodes() != null && !filter.getCountryCodes().isEmpty()) {
            criteria = criteria.and("maasProvider.shippingCountries.countryCode")
                               .in(filter.getCountryCodes());
        }

        if (filter.getManufacturingServices() != null && !filter.getManufacturingServices().isEmpty()) {
            criteria = criteria.and("maasProvider.manufacturingServices")
                               .in(filter.getManufacturingServices());
        }

        Query query = new Query(criteria);
        return mongoTemplate.find(query, Organization.class);
    }
}

