
package gr.atc.t4m.organization_management.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.t4m.organization_management.config.TestSecurityConfig;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.service.MinioService;
import gr.atc.t4m.organization_management.service.OrganizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;


@WebMvcTest(OrganizationController.class)
@Import(TestSecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private MinioService minioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
void setupSecurityContext() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("sub", "test-user-id") // or any userId you want to simulate
        .build();

    Authentication auth = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(auth);
}

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
        MockMultipartFile organizationPart = new MockMultipartFile(
            "organization",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(organizationDTO)
    );
        when(organizationService.createOrganization(any(Organization.class))).thenReturn(organization);


        mockMvc.perform(multipart("/api/organization/create")
        .file(organizationPart) // only the JSON part
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .accept(MediaType.APPLICATION_JSON))
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

        mockMvc.perform(put("/api/organization/update/123")
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
    mockMvc.perform(post("/api/organization/create")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .content(objectMapper.writeValueAsString(organizationDTO)))
            .andExpect(status().isBadRequest());
}

    @Test
    void testGetAllProviders_ReturnsListOfProviders() throws Exception {
        // Arrange
        Organization org1 = new Organization();
        org1.setOrganizationName("Provider One");
        org1.setMaasRole(List.of(MaasRole.PROVIDER));


        Organization org2 = new Organization();
        org2.setOrganizationName("Provider Two");
        org2.setMaasRole(List.of(MaasRole.PROVIDER));
        Organization org3 = new Organization();
        org3.setOrganizationName("Provider Three");
        org3.setMaasRole(List.of(MaasRole.CONSUMER));


        when(organizationService.getAllProviders()).thenReturn(List.of(org1, org2));

        // Act & Assert
        mockMvc.perform(get("/api/organization/getAllProviders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].organizationName").value("Provider One"))
                .andExpect(jsonPath("$[1].organizationName").value("Provider Two"));
    }

     @Test
    void testFilterProviders_ReturnsFilteredOrganizations() throws Exception {
        // Arrange
        ProviderSearchDTO filter = new ProviderSearchDTO();
        filter.setCountryCodes(List.of("GR", "DE"));
        filter.setManufacturingServices(List.of("AM"));

        Organization org1 = new Organization();
        org1.setOrganizationName("ATC Provider");

        when(organizationService.searchProviders(filter))
                .thenReturn(List.of(org1));

        // Act & Assert
        mockMvc.perform(post("/api/organization/searchProviders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationName").value("ATC Provider"));
    }

    @Test
    void testGetOrganizationByName() throws Exception {
        Organization organization = new Organization();
        organization.setOrganizationID("123");
        organization.setOrganizationName("Test Organization");

        when(organizationService.getOrganizationByName("Test Organization")).thenReturn(organization);

        mockMvc.perform(get("/api/organization/getOrganizationByName/Test Organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationID").value("123"))
                .andExpect(jsonPath("$.organizationName").value("Test Organization"));
    }

}

