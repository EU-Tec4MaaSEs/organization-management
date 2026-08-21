package gr.atc.t4m.organization_management.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

         List<Organization> organizations = mongoTemplate.find(query, Organization.class);

       // Filter by capabilities across all machines in the organization's pool
          return filterByCapabilities(organizations, filter);
}

private List<Organization> filterByCapabilities(List<Organization> organizations, ProviderSearchDTO filter) {
    if (filter != null && filter.getCapabilities() != null && !filter.getCapabilities().isEmpty()) {
        List<String> requiredCapabilities = filter.getCapabilities();

        return organizations.stream()
                .filter(org -> org != null && org.getManufacturingResources() != null
                        && !org.getManufacturingResources().isEmpty())
                .filter(org -> {
                    Set<String> providerCapabilities = org.getManufacturingResources().stream()
                            .filter(mr -> mr != null && mr.getCapabilities() != null)
                            .flatMap(mr -> mr.getCapabilities().stream())
                            .map(cap -> cap != null ? cap.getName() : null)
                            .filter(name -> name != null && !name.isBlank())
                            .collect(Collectors.toSet());

                    return providerCapabilities.containsAll(requiredCapabilities);
                })
                .toList();
    }

    return organizations;
}
}