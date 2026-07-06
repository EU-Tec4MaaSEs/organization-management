package gr.atc.t4m.organization_management;

import gr.atc.t4m.organization_management.dto.ManufacturingDomainRequest;
import gr.atc.t4m.organization_management.exception.DomainAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.DomainInUseException;
import gr.atc.t4m.organization_management.model.ManufacturingDomain;
import gr.atc.t4m.organization_management.repository.ManufacturingDomainRepository;
import gr.atc.t4m.organization_management.service.ManufacturingDomainService;
import gr.atc.t4m.organization_management.service.OrganizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManufacturingDomainServiceTest {

    @Mock
    private ManufacturingDomainRepository domainRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private ManufacturingDomainService domainService;

    private ManufacturingDomain sampleDomain;
    private ManufacturingDomainRequest sampleRequest;
    private final String targetId = "6a44f139cb65d112cdf5ae28";

    @BeforeEach
    void setUp() {
        sampleDomain = ManufacturingDomain.builder()
                .id(targetId)
                .name("Machining")
                .abbreviation("MACH")
                .description("CNC Services")
                .build();

        sampleRequest = new ManufacturingDomainRequest();
        sampleRequest.setName("Machining");
        sampleRequest.setAbbreviation("MACH");
        sampleRequest.setDescription("CNC Services");
    }

    // ==========================================
    // GET ALL DOMAINS Tests
    // ==========================================
    @Test
    void getAllDomains_ShouldReturnPaginatedData() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ManufacturingDomain> page = new PageImpl<>(List.of(sampleDomain));
        when(domainRepository.findAll(pageable)).thenReturn(page);

        Page<ManufacturingDomain> result = domainService.getAllDomains(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("MACH", result.getContent().get(0).getAbbreviation());
        verify(domainRepository, times(1)).findAll(pageable);
    }

    // ==========================================
    // CREATE DOMAIN Tests
    // ==========================================
    @Test
    void createDomain_WhenAbbreviationIsUnique_ShouldSaveSuccessfully() {
        when(domainRepository.findByAbbreviation("MACH")).thenReturn(Optional.empty());
        when(domainRepository.save(any(ManufacturingDomain.class))).thenReturn(sampleDomain);

        ManufacturingDomain result = domainService.createDomain(sampleRequest);

        assertNotNull(result);
        assertEquals("MACH", result.getAbbreviation());
        verify(domainRepository, times(1)).save(any(ManufacturingDomain.class));
    }

    @Test
    void createDomain_WhenAbbreviationExists_ShouldThrowException() {
        when(domainRepository.findByAbbreviation("MACH")).thenReturn(Optional.of(sampleDomain));

        assertThrows(DomainAlreadyExistsException.class, () -> domainService.createDomain(sampleRequest));
        verify(domainRepository, never()).save(any(ManufacturingDomain.class));
    }

    // ==========================================
    // UPDATE DOMAIN Tests
    // ==========================================
    @Test
    void updateDomain_WhenNoAbbreviationChange_ShouldUpdateWithoutCascading() {
        sampleRequest.setDescription("Updated Description");
        
        when(domainRepository.findByAbbreviation("MACH")).thenReturn(Optional.of(sampleDomain));
        when(domainRepository.findById(targetId)).thenReturn(Optional.of(sampleDomain));
        when(domainRepository.save(any(ManufacturingDomain.class))).thenReturn(sampleDomain);

        Optional<ManufacturingDomain> result = domainService.updateDomain(targetId, sampleRequest);

        assertTrue(result.isPresent());
        verify(domainRepository, times(1)).save(any(ManufacturingDomain.class));
        verifyNoInteractions(organizationService);
    }

    @Test
    void updateDomain_WhenAbbreviationChanges_ShouldCascadeToOrganizations() {
        sampleRequest.setAbbreviation("CNC-MACH");
        
        when(domainRepository.findByAbbreviation("CNC-MACH")).thenReturn(Optional.empty());
        when(domainRepository.findById(targetId)).thenReturn(Optional.of(sampleDomain));
        when(domainRepository.save(any(ManufacturingDomain.class))).thenReturn(sampleDomain);

        Optional<ManufacturingDomain> result = domainService.updateDomain(targetId, sampleRequest);

        assertTrue(result.isPresent());
        verify(domainRepository, times(1)).save(any(ManufacturingDomain.class));
        verify(organizationService, times(1)).cascadeServiceCodeUpdate("MACH", "CNC-MACH");
    }

    @Test
    void updateDomain_WhenAbbreviationTakenByDifferentDomain_ShouldThrowException() {
        ManufacturingDomain duplicateDomain = ManufacturingDomain.builder()
                .id("different-id-999")
                .abbreviation("MACH")
                .build();

        when(domainRepository.findByAbbreviation("MACH")).thenReturn(Optional.of(duplicateDomain));

        assertThrows(DomainAlreadyExistsException.class, () -> domainService.updateDomain(targetId, sampleRequest));
        verify(domainRepository, never()).findById(anyString());
    }

    // ==========================================
    // DELETE DOMAIN Tests
    // ==========================================
    @Test
    void deleteDomain_WhenNotUsedByOrganizations_ShouldDeleteAndReturnTrue() {
        when(domainRepository.findById(targetId)).thenReturn(Optional.of(sampleDomain));
        when(organizationService.getOrganizationNamesUsingService("MACH")).thenReturn(Collections.emptyList());

        boolean result = domainService.deleteDomain(targetId);

        assertTrue(result);
        verify(domainRepository, times(1)).deleteById(targetId);
    }

    @Test
    void deleteDomain_WhenUsedByOrganizations_ShouldBlockAndThrowException() {
        when(domainRepository.findById(targetId)).thenReturn(Optional.of(sampleDomain));
        when(organizationService.getOrganizationNamesUsingService("MACH")).thenReturn(List.of("Factory A", "Factory B"));

        assertThrows(DomainInUseException.class, () -> domainService.deleteDomain(targetId));
        verify(domainRepository, never()).deleteById(anyString());
    }

    @Test
    void deleteDomain_WhenNotFound_ShouldReturnFalse() {
        when(domainRepository.findById(targetId)).thenReturn(Optional.empty());

        boolean result = domainService.deleteDomain(targetId);

        assertFalse(result);
        verify(domainRepository, never()).deleteById(anyString());
    }

    @Test
    void getAllDistinctAbbreviations_ShouldReturnList() {
        List<String> expectedList = List.of("AMS", "MACH");
        when(domainRepository.findDistinctAbbreviations()).thenReturn(expectedList);

        List<String> result = domainService.getAllDistinctAbbreviations();

        assertEquals(2, result.size());
        assertTrue(result.contains("AMS"));
    }

    @Test
    void existsByAbbreviation_WhenExists_ShouldReturnTrue() {
        when(domainRepository.findByAbbreviation("MACH")).thenReturn(Optional.of(sampleDomain));

        boolean result = domainService.existsByAbbreviation("mach"); // Test case conversion logic

        assertTrue(result);
    }

    @Test
    void existsByAbbreviation_WhenNull_ShouldReturnFalse() {
        boolean result = domainService.existsByAbbreviation(null);
        assertFalse(result);
        verifyNoInteractions(domainRepository);
    }
}