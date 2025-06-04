
package gr.atc.t4m.organization_management.repository;

import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private OrganizationRepositoryImpl organizationRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        organizationRepository = new OrganizationRepositoryImpl(mongoTemplate);
    }

    @Test
    void filterProviders_WithNoFilters_ReturnsAllProviders() {
        // Arrange
        ProviderSearchDTO filter = new ProviderSearchDTO(); // No filters set

        Organization org = new Organization();
        org.setOrganizationName("Provider A");

        when(mongoTemplate.find(any(Query.class), eq(Organization.class)))
                .thenReturn(List.of(org));

        List<Organization> result = organizationRepository.filterProviders(filter);


        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Provider A", result.get(0).getOrganizationName());

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Organization.class));
    }

    @Test
    void filterProviders_WithCountryAndServiceFilters_ReturnsFilteredProviders() {
    
        ProviderSearchDTO filter = new ProviderSearchDTO();
        filter.setCountryCodes(List.of("GR", "DE"));
        filter.setManufacturingServices(List.of("AM", "CNC"));

        Organization org = new Organization();
        org.setOrganizationName("Provider 1");

        when(mongoTemplate.find(any(Query.class), eq(Organization.class)))
                .thenReturn(List.of(org));

        List<Organization> result = organizationRepository.filterProviders(filter);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Provider 1", result.get(0).getOrganizationName());

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Organization.class));
    }
}
