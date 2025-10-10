package gr.atc.t4m.organization_management.controller;

import gr.atc.t4m.organization_management.config.TestSecurityConfig;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.service.ManufacturingResourceService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CapabilityController.class)
@Import(TestSecurityConfig.class)

class CapabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManufacturingResourceService manufacturingResourceService;

    @Test
    @DisplayName("GET /api/capability/all - should return list of capabilities")
    void testGetAllCapabilities_success() throws Exception {
        CapabilityEntry cap1 = new CapabilityEntry("Milling", "Primary", true, "High precision", List.of(), null);
        CapabilityEntry cap2 = new CapabilityEntry("Drilling", "Secondary", true, "Automated", List.of(), null);

        when(manufacturingResourceService.getAllCapabilities())
                .thenReturn(List.of(cap1, cap2));

        mockMvc.perform(get("/api/capability/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Milling"))
                .andExpect(jsonPath("$[1].name").value("Drilling"));
    }

    @Test
    @DisplayName("GET /api/capability/all - should return 404 when no capabilities found")
    void testGetAllCapabilities_notFound() throws Exception {
        when(manufacturingResourceService.getAllCapabilities())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/capability/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/capability/allManufacturingResources - should return list of manufacturing resources")
    void testGetAllManufacturingResources_success() throws Exception {
        ManufacturingResource res1 = new ManufacturingResource();
        res1.setManufacturingResourceID("MR1");
        res1.setManufacturingResourceTitle("Laser Cutter");

        ManufacturingResource res2 = new ManufacturingResource();
        res2.setManufacturingResourceID("MR2");
        res2.setManufacturingResourceTitle("3D Printer");

        when(manufacturingResourceService.getAllManufacturingResources())
                .thenReturn(List.of(res1, res2));

        mockMvc.perform(get("/api/capability/allManufacturingResources")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].manufacturingResourceTitle").value("Laser Cutter"))
                .andExpect(jsonPath("$[1].manufacturingResourceTitle").value("3D Printer"));
    }

    @Test
    @DisplayName("GET /api/capability/allManufacturingResources - should return 404 when no resources found")
    void testGetAllManufacturingResources_notFound() throws Exception {
        when(manufacturingResourceService.getAllManufacturingResources())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/capability/allManufacturingResources")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

