package gr.atc.t4m.organization_management.service;

import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.repository.ManufacturingResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManufacturingResourceServiceTest {

    private ManufacturingResourceRepository manufacturingResourceRepo;
    private ManufacturingResourceService manufacturingResourceService;

    @BeforeEach
    void setUp() {
        manufacturingResourceRepo = Mockito.mock(ManufacturingResourceRepository.class);
        manufacturingResourceService = new ManufacturingResourceService(manufacturingResourceRepo);
    }

    @Test
    @DisplayName("getAllCapabilities returns flattened list of capabilities")
    void testGetAllCapabilities_success() {
        CapabilityEntry cap1 = new CapabilityEntry("Cutting", "Primary", true, "High precision", List.of(), null);
        CapabilityEntry cap2 = new CapabilityEntry("Welding", "Secondary", true, "Automated", List.of(), null);

        ManufacturingResource mr1 = new ManufacturingResource();
        mr1.setCapabilities(List.of(cap1));

        ManufacturingResource mr2 = new ManufacturingResource();
        mr2.setCapabilities(List.of(cap2));

        when(manufacturingResourceRepo.findAll()).thenReturn(List.of(mr1, mr2));

        List<CapabilityEntry> result = manufacturingResourceService.getAllCapabilities();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(cap1));
        assertTrue(result.contains(cap2));

        verify(manufacturingResourceRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllCapabilities returns empty list if no capabilities")
    void testGetAllCapabilities_empty() {
        ManufacturingResource mr = new ManufacturingResource();
        mr.setCapabilities(List.of()); // no capabilities

        when(manufacturingResourceRepo.findAll()).thenReturn(List.of(mr));

        List<CapabilityEntry> result = manufacturingResourceService.getAllCapabilities();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(manufacturingResourceRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllManufacturingResources returns all resources")
    void testGetAllManufacturingResources_success() {
        ManufacturingResource mr1 = new ManufacturingResource();
        ManufacturingResource mr2 = new ManufacturingResource();

        when(manufacturingResourceRepo.findAll()).thenReturn(List.of(mr1, mr2));

        List<ManufacturingResource> result = manufacturingResourceService.getAllManufacturingResources();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(mr1));
        assertTrue(result.contains(mr2));

        verify(manufacturingResourceRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllManufacturingResources returns empty list if no resources")
    void testGetAllManufacturingResources_empty() {
        when(manufacturingResourceRepo.findAll()).thenReturn(List.of());

        List<ManufacturingResource> result = manufacturingResourceService.getAllManufacturingResources();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(manufacturingResourceRepo, times(1)).findAll();
    }
}
