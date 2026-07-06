package gr.atc.t4m.organization_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.organization_management.dto.ManufacturingDomainRequest;
import gr.atc.t4m.organization_management.model.ManufacturingDomain;
import gr.atc.t4m.organization_management.service.ManufacturingDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@WebMvcTest(controllers = ManufacturingDomainController.class)
@AutoConfigureMockMvc(addFilters = false) 
class ManufacturingDomainControllerTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManufacturingDomainService domainService;

    private ManufacturingDomain sampleDomain;
    private ManufacturingDomainRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleDomain = new ManufacturingDomain();
        sampleDomain.setId("6a44f139cb65d112cdf5ae28");
        sampleDomain.setName("Machining");
        sampleDomain.setAbbreviation("MACH");
        sampleDomain.setDescription("CNC Services");

        sampleRequest = new ManufacturingDomainRequest();
        sampleRequest.setName("Machining");
        sampleRequest.setAbbreviation("MACH");
        sampleRequest.setDescription("CNC Services");
    }

    @Test
    void getAllDomains_ShouldReturnPaginatedData() throws Exception {
        var page = new PageImpl<>(List.of(sampleDomain), PageRequest.of(0, 10), 1);
        when(domainService.getAllDomains(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/manufacturing-domains")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("6a44f139cb65d112cdf5ae28"))
                .andExpect(jsonPath("$.content[0].abbreviation").value("MACH"));
    }

    @Test
    void getDomainById_WhenExists_ShouldReturnDomain() throws Exception {
        when(domainService.getDomainById("6a44f139cb65d112cdf5ae28")).thenReturn(Optional.of(sampleDomain));

        mockMvc.perform(get("/api/manufacturing-domains/6a44f139cb65d112cdf5ae28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Machining"))
                .andExpect(jsonPath("$.abbreviation").value("MACH"));
    }

    @Test
    void getDomainById_WhenNotExists_ShouldReturn404() throws Exception {
        when(domainService.getDomainById("invalid-id")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/manufacturing-domains/invalid-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDistinctAbbreviations_ShouldReturnList() throws Exception {
        when(domainService.getAllDistinctAbbreviations()).thenReturn(List.of("AMS", "MACH"));

        mockMvc.perform(get("/api/manufacturing-domains/abbreviations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("AMS"))
                .andExpect(jsonPath("$[1]").value("MACH"));
    }

    @Test
    void createDomain_ShouldReturn201() throws Exception {
        when(domainService.createDomain(any(ManufacturingDomainRequest.class))).thenReturn(sampleDomain);

        mockMvc.perform(post("/api/manufacturing-domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("6a44f139cb65d112cdf5ae28"));
    }

    @Test
    void updateDomain_WhenExists_ShouldReturnUpdatedDomain() throws Exception {
        when(domainService.updateDomain(eq("6a44f139cb65d112cdf5ae28"), any(ManufacturingDomainRequest.class)))
                .thenReturn(Optional.of(sampleDomain));

        mockMvc.perform(put("/api/manufacturing-domains/6a44f139cb65d112cdf5ae28")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abbreviation").value("MACH"));
    }

    @Test
    void updateDomain_WhenNotExists_ShouldReturn404() throws Exception {
        when(domainService.updateDomain(eq("invalid-id"), any(ManufacturingDomainRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/manufacturing-domains/invalid-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDomain_WhenExists_ShouldReturn204NoContent() throws Exception {
        when(domainService.deleteDomain("6a44f139cb65d112cdf5ae28")).thenReturn(true);

        mockMvc.perform(delete("/api/manufacturing-domains/6a44f139cb65d112cdf5ae28"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDomain_WhenNotExists_ShouldReturn404NotFound() throws Exception {
        when(domainService.deleteDomain("invalid-id")).thenReturn(false);

        mockMvc.perform(delete("/api/manufacturing-domains/invalid-id"))
                .andExpect(status().isNotFound());
    }
}