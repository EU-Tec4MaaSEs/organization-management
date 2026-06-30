
package gr.atc.t4m.organization_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.t4m.organization_management.config.TestSecurityConfig;
import gr.atc.t4m.organization_management.dto.CreateReviewDTO;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.OrganizationLogoResponse;
import gr.atc.t4m.organization_management.dto.OrganizationReviewsResponseDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.dto.ReviewAnalyticsDTO;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManualSearchHistory;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.model.OrganizationReview;
import gr.atc.t4m.organization_management.service.CapabilityService;
import gr.atc.t4m.organization_management.service.ManualSearchHistoryService;
import gr.atc.t4m.organization_management.service.MinioService;
import gr.atc.t4m.organization_management.service.OrganizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.util.Collections;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(OrganizationController.class)
@Import(TestSecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private MinioService minioService;

    @MockitoBean
    private CapabilityService capabilityService;
    @MockitoBean
     private ManualSearchHistoryService searchHistoryService;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
void setupSecurityContext() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("sub", "test-user-id") // or any userId you want to simulate
        .claim("organization_id", "test-org")
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
    ProviderSearchDTO filter = new ProviderSearchDTO();
    filter.setCountryCodes(List.of("GR", "DE"));
    filter.setManufacturingServices(List.of("AM"));

    Organization org1 = new Organization();
    org1.setOrganizationName("ATC Provider");

    when(organizationService.searchProviders(any(ProviderSearchDTO.class)))
            .thenReturn(List.of(org1));

    mockMvc.perform(post("/api/organization/searchProviders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(filter))
                    .with(jwt().jwt(j -> j.claim("sub", "1234567890"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].organizationName").value("ATC Provider"));

    verify(searchHistoryService).recordSearch("1234567890", List.of("GR", "DE"), List.of("AM"));
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


    @Test
    void testGetOrganizationCapabilities_success() throws Exception {
        CapabilityEntry cap1 = new CapabilityEntry("Cutting", "Primary", true, "High precision", List.of(), null, null);
        CapabilityEntry cap2 = new CapabilityEntry("Welding", "Secondary", true, "Automated", List.of(), null, null);

        ManufacturingResource resource = new ManufacturingResource();
        resource.setManufacturingResourceID("MR1");
        resource.setCapabilities(List.of(cap1, cap2));

        Organization org = new Organization();
        org.setOrganizationName("TestOrg");
        org.setManufacturingResources(List.of(resource));

        when(organizationService.getOrganizationByName("TestOrg")).thenReturn(org);

        mockMvc.perform(get("/api/organization/TestOrg/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cutting"))
                .andExpect(jsonPath("$[1].name").value("Welding"));
    }

    @Test
    void testGetOrganizationCapabilities_noResources() throws Exception {
        Organization org = new Organization();
        org.setOrganizationName("TestOrg");
        org.setManufacturingResources(List.of()); // empty

        when(organizationService.getOrganizationByName("TestOrg")).thenReturn(org);

        mockMvc.perform(get("/api/organization/TestOrg/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetOrganizationCapabilities_noCapabilities() throws Exception {
        ManufacturingResource resource = new ManufacturingResource();
        resource.setManufacturingResourceID("MR1");
        resource.setCapabilities(List.of()); // no capabilities

        Organization org = new Organization();
        org.setOrganizationName("TestOrg");
        org.setManufacturingResources(List.of(resource));

        when(organizationService.getOrganizationByName("TestOrg")).thenReturn(org);

        mockMvc.perform(get("/api/organization/TestOrg/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetOrganizationCapabilities_orgNotFound() throws Exception {
        when(organizationService.getOrganizationByName("UnknownOrg"))
                .thenThrow(new OrganizationNotFoundException("Organization not found"));

        mockMvc.perform(get("/api/organization/UnknownOrg/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

 @Test
void testGetOrganizationsByPrimaryCapability_ReturnsOk() throws Exception {
    OrganizationDTO orgDto = new OrganizationDTO();
    orgDto.setOrganizationName("TestOrg");

    when(organizationService.getOrganizationsByCapabilities(eq("Cutting"), any()))
            .thenReturn(List.of(orgDto));

    // Act & Assert
    mockMvc.perform(get("/api/organization/by-capability")
                    .param("primaryCapability", "Cutting")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$[0].organizationName").value("TestOrg"));
}

   @Test
void testGetOrganizationsBySecondaryCapability_ReturnsOk() throws Exception {
    OrganizationDTO orgDto = new OrganizationDTO();
    orgDto.setOrganizationName("OrgB");

    when(organizationService.getOrganizationsByCapabilities(nullable(String.class), eq("Welding")))
            .thenReturn(List.of(orgDto));

    mockMvc.perform(get("/api/organization/by-capability")
                    .param("secondaryCapability", "Welding")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$[0].organizationName").value("OrgB"));
}


@Test
void testGetOrganizationsByCapability_NoParams_ReturnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/organization/by-capability")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
}


    @Test
    void testGetOrganizationsByCapability_EmptyList_ReturnsNoContent() throws Exception {
        when(organizationService.getOrganizationsByCapabilities(eq("Unknown"), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/organization/by-capability")
                        .param("primaryCapability", "Unknown")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

@Test
    void testUpdateOrganizationLogo_Success() throws Exception {
        String orgId = "123";
        String logoUrl = "https://minio.example.com/logo.png";

        Organization org = new Organization();
        org.setOrganizationID(orgId);

        // Mock service responses
        when(organizationService.getOrganization(orgId)).thenReturn(org);
        when(minioService.uploadLogo(any())).thenReturn(logoUrl);
        when(organizationService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "logoFile",
                "logo.png",
                "image/png",
                "fake-content".getBytes()
        );

        mockMvc.perform(multipart("/api/organization/123/update-logo", orgId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(logoUrl));
    }

    @Test
    void testUpdateOrganizationLogo_MissingFile_ShouldReturnBadRequest() throws Exception {
        String orgId = "123";

        mockMvc.perform(multipart("/api/organization/{orgId}/update-logo", orgId)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateOrganizationLogo_OrganizationNotFound_ShouldReturnNotFound() throws Exception {
        String orgId = "123";

        MockMultipartFile file = new MockMultipartFile(
                "logoFile",
                "logo.png",
                "image/png",
                "fake-content".getBytes()
        );

        when(organizationService.getOrganization(orgId)).thenReturn(null);

        mockMvc.perform(multipart("/api/organization/{orgId}/update-logo", orgId)
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isNotFound());
    }


    @Test
    void testCreateReview_Success_Returns201() throws Exception {
        String targetOrgId = "target-org-123";
        String mockUserId = "test-user-id";
        String reviewerOrgId = "test-org";

        CreateReviewDTO validReviewDto = new CreateReviewDTO();
        validReviewDto.setRating(4);
        validReviewDto.setComment("This is a verified platform review");
        validReviewDto.setTargetRole(MaasRole.CONSUMER);


        mockMvc.perform(post("/api/organization/" + targetOrgId + "/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(validReviewDto)))
                .andExpect(status().isCreated());

        verify(organizationService).saveReview(
                eq(targetOrgId), 
                eq(mockUserId), 
                eq(reviewerOrgId), 
                any(CreateReviewDTO.class)
        );
    }

@Test
    void testCreateReview_SelfReview_Returns400BadRequest() throws Exception {
        String selfOrgId = "test-org";
        CreateReviewDTO validReviewDto = new CreateReviewDTO();
        validReviewDto.setRating(4);
        validReviewDto.setComment("This is a verified platform review");
        validReviewDto.setTargetRole(MaasRole.CONSUMER);

        setupSecurityContext();

        // 2. Execute MockMvc performance verification check
        mockMvc.perform(post("/api/organization/" + selfOrgId + "/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(validReviewDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
    }


    // ==========================================
    // 2. GET /{orgId}/reviews
    // ==========================================

    @Test
    void testGetReviewAnalytics_Success_Returns200AndDTOData() throws Exception {
        String orgId = "target-org-123";
        Pageable expectedPageable = PageRequest.of(0, 10);

        ReviewAnalyticsDTO mockProvider = new ReviewAnalyticsDTO(4.5, 10, 0, 0, 1, 3, 6);
        ReviewAnalyticsDTO mockConsumer = new ReviewAnalyticsDTO(4.0, 5, 0, 1, 0, 2, 2);
        
        OrganizationReview reviewSample = new OrganizationReview();
        reviewSample.setId("rev-1");
        reviewSample.setComment("Nice consumer experience");
        Page<OrganizationReview> mockPage = new PageImpl<>(List.of(reviewSample), expectedPageable, 1);

        OrganizationReviewsResponseDTO responseDTO = new OrganizationReviewsResponseDTO(mockProvider, mockConsumer, mockPage);

        when(organizationService.getReviewAnalytics(eq(orgId), eq(MaasRole.CONSUMER), any(Pageable.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/api/organization/" + orgId + "/reviews")
                        .param("role", "CONSUMER")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerAnalytics.averageRating").value(4.5))
                .andExpect(jsonPath("$.consumerAnalytics.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviews.content[0].id").value("rev-1"))
                .andExpect(jsonPath("$.reviews.content[0].comment").value("Nice consumer experience"));
    }

    @Test
    void testGetReviewAnalytics_InvalidRole_Returns400BadRequest() throws Exception {
        String orgId = "target-org-123";

        mockMvc.perform(get("/api/organization/" + orgId + "/reviews")
                        .param("role", "INVALID_ROLE_NAME")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(organizationService);
    }


    @Test
    void testUpdateReview_Success_Returns200AndUpdatedReviewBody() throws Exception {
        String reviewId = "review-abc";
        CreateReviewDTO editDto = new CreateReviewDTO(5, "Updated comment text!", MaasRole.PROVIDER);

        OrganizationReview updatedReview = new OrganizationReview();
        updatedReview.setId(reviewId);
        updatedReview.setComment("Updated comment text!");
        updatedReview.setRating(5);

        when(organizationService.updateReview(eq(reviewId), eq("test-user-id"), any(CreateReviewDTO.class)))
                .thenReturn(updatedReview);

        mockMvc.perform(put("/api/organization/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.comment").value("Updated comment text!"))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void testUpdateReview_NotAuthor_Returns403Forbidden() throws Exception {
        String reviewId = "review-abc";
        CreateReviewDTO editDto = new CreateReviewDTO(5, "Sneaky edit attempt", MaasRole.PROVIDER);

        when(organizationService.updateReview(eq(reviewId), eq("test-user-id"), any(CreateReviewDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this review"));

        mockMvc.perform(put("/api/organization/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(editDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateReview_NotFound_Returns404NotFound() throws Exception {
        String reviewId = "non-existent-id";
        CreateReviewDTO editDto = new CreateReviewDTO(5, "Lost edit attempt", MaasRole.PROVIDER);

        when(organizationService.updateReview(eq(reviewId), eq("test-user-id"), any(CreateReviewDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

        mockMvc.perform(put("/api/organization/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(editDto)))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // 4. GET /reviews/performed
    // ==========================================

    @Test
    void testGetReviewsPerformedByMyOrganization_WithTargetOrgFilter_Returns200Page() throws Exception {
        String targetOrgId = "isolated-target-company";
        Pageable expectedPageable = PageRequest.of(0, 5);

        OrganizationReview outboundReview = new OrganizationReview();
        outboundReview.setId("outbound-1");
        outboundReview.setReviewerOrganizationId("test-org");
        outboundReview.setTargetOrganizationId(targetOrgId);

        Page<OrganizationReview> mockHistoryPage = new PageImpl<>(List.of(outboundReview), expectedPageable, 1);

        when(organizationService.getReviewsPerformedByOrganization(eq("test-org"), eq(targetOrgId), any(Pageable.class)))
                .thenReturn(mockHistoryPage);

        mockMvc.perform(get("/api/organization/reviews/performed")
                        .param("targetOrganizationId", targetOrgId)
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("outbound-1"))
                .andExpect(jsonPath("$.content[0].targetOrganizationId").value(targetOrgId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetReviewsPerformedByMyOrganization_NoFilter_Returns200Page() throws Exception {
        Pageable expectedPageable = PageRequest.of(0, 10);
        Page<OrganizationReview> emptyPage = new PageImpl<>(Collections.emptyList(), expectedPageable, 0);

        when(organizationService.getReviewsPerformedByOrganization(eq("test-org"), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/organization/reviews/performed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetOrganizationLogos_ReturnsListOfLogoResponses() throws Exception {

            OrganizationLogoResponse dto1 = new OrganizationLogoResponse("org1", "https://test.logo1.png");

            OrganizationLogoResponse dto2 = new OrganizationLogoResponse("org2", "https://test.logo2.png");

            when(organizationService.getLogosForOrganizations(anyList()))
                            .thenReturn(List.of(dto1, dto2));

            mockMvc.perform(post("/api/organization/logos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(List.of("org1", "org2"))))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$[0].organizationId").value("org1"))
                            .andExpect(jsonPath("$[0].logoUrl").value("https://test.logo1.png"))
                            .andExpect(jsonPath("$[1].organizationId").value("org2"))
                            .andExpect(jsonPath("$[1].logoUrl").value("https://test.logo2.png"));

            verify(organizationService, times(1))
                            .getLogosForOrganizations(anyList());

    }

    @Test
    void testGetSearchHistory_ReturnsPaginatedHistorySuccessfully() throws Exception {
        // Arrange
        String mockUserId = "user-uuid-12345";
        
        ManualSearchHistory historyItem = ManualSearchHistory.builder()
                .id("doc-id-1")
                .userId(mockUserId)
                .countryCodes(List.of("GR", "DE"))
                .manufacturingServices(List.of("AM"))
                .searchedAt(LocalDateTime.now())
                .build();
                
        Page<ManualSearchHistory> mockPage = new PageImpl<>(List.of(historyItem));

        // Stub the service layer to look for our specific mockUserId
        when(searchHistoryService.getUserSearchHistory(eq(mockUserId), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/organization/searchProvidersHistory")
                        .accept(MediaType.APPLICATION_JSON)
                        // Mock the JWT token parameter extraction
                        .with(jwt().jwt(j -> j.claim("sub", mockUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("doc-id-1"))
                .andExpect(jsonPath("$.content[0].userId").value(mockUserId))
                .andExpect(jsonPath("$.content[0].countryCodes[0]").value("GR"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(searchHistoryService).getUserSearchHistory(eq(mockUserId), any(Pageable.class));
    }

    @Test
    void testDeleteSearchHistory_ReturnsNoContent() throws Exception {
        // Arrange
        String mockUserId = "user-uuid-12345";
        
        // Stub the void service deletion method
        doNothing().when(searchHistoryService).clearUserSearchHistory(mockUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/organization/deleteProvidersHistory")
                        // Mock the JWT token parameter extraction
                        .with(jwt().jwt(j -> j.claim("sub", mockUserId))))
                .andExpect(status().isNoContent()); // Verifies HTTP 204 status

        // Verify the interaction with our service mock
        verify(searchHistoryService).clearUserSearchHistory(mockUserId);
    }
    
    @Test
    void testGetSearchHistory_WithoutToken_ReturnsUnauthorized() throws Exception {
        // Act & Assert (Tests fallback security rules if token is missing entirely)
        mockMvc.perform(get("/api/organization/searchProvidersHistory")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void testDeleteSearchHistoryEntry_Success_ReturnsNoContent() throws Exception {
        // Arrange
        String mockUserId = "user-uuid-12345";
        String targetHistoryId = "doc-id-999";
        
        doNothing().when(searchHistoryService).deleteHistoryEntry(targetHistoryId, mockUserId);

        mockMvc.perform(delete("/api/organization/deleteProvidersHistory/{id}", targetHistoryId)
                        .with(jwt().jwt(j -> j.claim("sub", mockUserId))))
                .andExpect(status().isNoContent()); // 204 No Content

        verify(searchHistoryService).deleteHistoryEntry(targetHistoryId, mockUserId);
    }

    @Test
    void testDeleteSearchHistoryEntry_NotOwner_ReturnsForbidden() throws Exception {
        // Arrange
        String mockUserId = "attacker-uuid-666";
        String targetHistoryId = "doc-id-999";
        
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this history record."))
                .when(searchHistoryService).deleteHistoryEntry(targetHistoryId, mockUserId);

        mockMvc.perform(delete("/api/organization/deleteProvidersHistory/{id}", targetHistoryId)
                        .with(jwt().jwt(j -> j.claim("sub", mockUserId))))
                .andExpect(status().isForbidden()); // 403 Forbidden

        verify(searchHistoryService).deleteHistoryEntry(targetHistoryId, mockUserId);
    }

    @Test
    void testDeleteSearchHistoryEntry_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String mockUserId = "user-uuid-12345";
        String nonExistentId = "invalid-id-000";
        
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "History record not found with id: " + nonExistentId))
                .when(searchHistoryService).deleteHistoryEntry(nonExistentId, mockUserId);

        mockMvc.perform(delete("/api/organization/deleteProvidersHistory/{id}", nonExistentId)
                        .with(jwt().jwt(j -> j.claim("sub", mockUserId))))
                .andExpect(status().isNotFound());

        verify(searchHistoryService).deleteHistoryEntry(nonExistentId, mockUserId);
    }
}


