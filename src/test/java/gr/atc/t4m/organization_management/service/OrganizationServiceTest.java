
package gr.atc.t4m.organization_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;

class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ManufacturingResourceService manufacturingResourceService;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
}

