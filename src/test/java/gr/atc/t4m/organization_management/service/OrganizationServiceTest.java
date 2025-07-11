
package gr.atc.t4m.organization_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import gr.atc.t4m.organization_management.dto.EventDTO;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;

class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    private ModelMapper modelMapper;


    @Mock
    private ManufacturingResourceService manufacturingResourceService;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.openMocks(this);
      modelMapper = new ModelMapper();
      KafkaTemplate<String, EventDTO> kafkaTemplate = mock(KafkaTemplate.class);
     organizationService = new OrganizationService(organizationRepository, manufacturingResourceService, modelMapper, kafkaTemplate);

    }

    @Test
    void testCreateOrganization_Success() {
        Organization organization = new Organization();
        organization.setOrganizationName("TEST_ORG");

        when(organizationRepository.findByOrganizationName("TEST_ORG")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        Organization createdOrganization = organizationService.createOrganization(organization);

        assertNotNull(createdOrganization);
        assertEquals("TEST_ORG", createdOrganization.getOrganizationName());
        verify(organizationRepository).save(organization);
    }

    @Test
    void testCreateOrganization_WhenOrganizationExists_ShouldThrowException() {
        Organization organization = new Organization();
        organization.setOrganizationName("TEST_ORG_EXIST");

        when(organizationRepository.findByOrganizationName("TEST_ORG_EXIST")).thenReturn(Optional.of(organization));

        assertThrows(OrganizationAlreadyExistsException.class, () -> organizationService.createOrganization(organization));
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void testGetOrganization_WhenExists_ShouldReturnOrganization() {
        Organization organization = new Organization();
        organization.setOrganizationID("123");

        when(organizationRepository.findById("123")).thenReturn(Optional.of(organization));

        Organization foundOrganization = organizationService.getOrganization("123");

        assertNotNull(foundOrganization);
        assertEquals("123", foundOrganization.getOrganizationID());
    }

    @Test
    void testGetOrganization_WhenNotExists_ShouldThrowException() {
        when(organizationRepository.findById("123")).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class, () -> organizationService.getOrganization("123"));
    }

    @Test
    void testDeleteOrganizationById_WhenExists_ShouldDeleteOrganization() {
        Organization organization = new Organization();
        organization.setOrganizationID("123");

        when(organizationRepository.findById("123")).thenReturn(Optional.of(organization));

        organizationService.deleteOrganizationById("123");

        verify(organizationRepository).delete(organization);
    }

    @Test
    void testDeleteOrganizationById_WhenNotExists_ShouldThrowException() {
        when(organizationRepository.findById("123")).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class, () -> organizationService.deleteOrganizationById("123"));
    }

    @Test
    void testGetAllOrganizations_ShouldReturnPagedOrganizations() {
        Organization organization1 = new Organization();
        Organization organization2 = new Organization();
        Page<Organization> page = new PageImpl<>(List.of(organization1, organization2));

        when(organizationRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Organization> result = organizationService.getAllOrganizations(Pageable.unpaged());

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void testUpdateOrganization_WhenExists_ShouldUpdateAndReturnOrganization() {
        Organization existingOrganization = new Organization();
        existingOrganization.setOrganizationID("123");
        existingOrganization.setOrganizationName("ORG_OLD");

        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setOrganizationName("ORG_NEW");

        when(organizationRepository.findById("123")).thenReturn(Optional.of(existingOrganization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(existingOrganization);

        Organization updatedOrganization = organizationService.updateOrganization("123", organizationDTO);

        assertNotNull(updatedOrganization);
        assertEquals("ORG_NEW", updatedOrganization.getOrganizationName());
    }

    @Test
    void testUpdateOrganization_WhenNotExists_ShouldThrowException() {
        OrganizationDTO organizationDTO = new OrganizationDTO();
        when(organizationRepository.findById("123")).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class, () -> organizationService.updateOrganization("123", organizationDTO));
    }

     @Test
    void testGetAllProviders_ReturnsProviderOrganizations() {
        // Arrange
        Organization org1 = new Organization();
        org1.setOrganizationName("Provider 1");

        when(organizationRepository.findByMaasRoleContaining(MaasRole.PROVIDER.getName()))
                .thenReturn(List.of(org1));

        // Act
        List<Organization> result = organizationService.getAllProviders();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Provider 1", result.get(0).getOrganizationName());
        verify(organizationRepository, times(1)).findByMaasRoleContaining(MaasRole.PROVIDER.getName());
    }

    @Test
    void testSearchProviders_WithValidFilter_ReturnsFilteredOrganizations() {
        // Arrange
        ProviderSearchDTO filter = new ProviderSearchDTO();
        filter.setCountryCodes(List.of("GR"));
        filter.setManufacturingServices(List.of("AM"));

        Organization org = new Organization();
        org.setOrganizationName("Greek Provider for AM services");

        when(organizationRepository.filterProviders(filter)).thenReturn(List.of(org));

        // Act
        List<Organization> result = organizationService.searchProviders(filter);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Greek Provider for AM services", result.get(0).getOrganizationName());
        verify(organizationRepository, times(1)).filterProviders(filter);
    }
}

