
package gr.atc.t4m.organization_management.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.t4m.organization_management.config.TestSecurityConfig;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@WebMvcTest(OrganizationController.class)
@Import(TestSecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Spring will inject MockMvc

    @MockBean  // This ensures the mock is injected into the controller
    private OrganizationService organizationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/organization/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to organization Management for T4M!"));
    }

    @Test
    void testCreateOrganization() throws Exception {
        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setOrganizationName("Test Organization");

        Organization organization = new Organization();
        organization.setOrganizationID("123");
        organization.setOrganizationName("Test Organization");

        when(organizationService.createOrganization(any(Organization.class))).thenReturn(organization);

        mockMvc.perform(post("/api/organization/createOrganization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(organizationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationID").value("123"))
                .andExpect(jsonPath("$.organizationName").value("Test Organization"));
    }

   @Test
    void testGetOrganization() throws Exception {
        Organization organization = new Organization();
        organization.setOrganizationID("123");
        organization.setOrganizationName("Test Organization");

        when(organizationService.getOrganization("123")).thenReturn(organization);

        mockMvc.perform(get("/api/organization/getOrganization/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationID").value("123"))
                .andExpect(jsonPath("$.organizationName").value("Test Organization"));
    }

    @Test
    void testUpdateOrganization() throws Exception {
        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setOrganizationName("TEST_ORGANIZATION");

        Organization updatedOrganization = new Organization();
        updatedOrganization.setOrganizationID("123");
        updatedOrganization.setOrganizationName("TEST_ORGANIZATION");

        when(organizationService.updateOrganization(eq("123"), any(OrganizationDTO.class))).thenReturn(updatedOrganization);

        mockMvc.perform(put("/api/organization/updateOrganization/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(organizationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationID").value("123"))
                .andExpect(jsonPath("$.organizationName").value("TEST_ORGANIZATION"));
    }

    @Test
    void testDeleteOrganization() throws Exception {
        doNothing().when(organizationService).deleteOrganizationById("123");

        mockMvc.perform(delete("/api/organization/deleteOrganization/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Organization deleted successfully."));
    }

    @Test
void testGetAllOrganizations() throws Exception {
    // Mocked organization data
    Organization organization1 = new Organization();
    organization1.setOrganizationID("1");
    organization1.setOrganizationName("Org One");

    Organization organization2 = new Organization();
    organization2.setOrganizationID("2");
    organization2.setOrganizationName("Org Two");

    List<Organization> organizationList = Arrays.asList(organization1, organization2);
    Page<Organization> organizationPage = new PageImpl<>(organizationList);

    // Mock service method
    when(organizationService.getAllOrganizations(any(Pageable.class))).thenReturn(organizationPage);

    // Perform GET request
    mockMvc.perform(get("/api/organization/getOrganization/all")
            .param("page", "0")
            .param("size", "10")
            .param("sortBy", "organizationName")
            .param("sortDir", "asc")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].organizationID").value("1"))
            .andExpect(jsonPath("$.content[0].organizationName").value("Org One"))
            .andExpect(jsonPath("$.content[1].organizationID").value("2"))
            .andExpect(jsonPath("$.content[1].organizationName").value("Org Two"));

    // Verify service method was called
    verify(organizationService, times(1)).getAllOrganizations(any(Pageable.class));
}

@Test
void testCreateOrganization_WhenOrganizationNameIsNull_ShouldReturnBadRequest() throws Exception {

    OrganizationDTO organizationDTO = new OrganizationDTO();
    organizationDTO.setOrganizationName(null); // Simulating missing name

    // When & Then: Perform POST request and expect Bad Request (400)
    mockMvc.perform(post("/api/organization/createOrganization")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(organizationDTO)))
            .andExpect(status().isBadRequest());
}

}

