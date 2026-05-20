
package gr.atc.t4m.organization_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import gr.atc.t4m.organization_management.dto.CreateReviewDTO;
import gr.atc.t4m.organization_management.dto.EventDTO;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.OrganizationReviewsResponseDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.exception.InvalidOrganizationRoleException;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.EventType;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.model.OrganizationReview;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;
import gr.atc.t4m.organization_management.repository.OrganizationReviewRepository;

import org.apache.kafka.clients.producer.RecordMetadata;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;


@ExtendWith(MockitoExtension.class) 
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ModelMapper modelMapper; 

    @Mock
    private KafkaTemplate<String, EventDTO> kafkaTemplate;

    @Mock
    private ManufacturingResourceService manufacturingResourceService;

    @Captor
    private ArgumentCaptor<EventDTO> eventCaptor;

    @InjectMocks
    private OrganizationService organizationService; 

    @Mock
    private OrganizationReviewRepository reviewRepository;

    private static final String TOPIC = "dataspace-organization-onboarding";

    @BeforeEach
    void setUp() throws Exception {
        Field topicField = OrganizationService.class.getDeclaredField("organizationRegistrationTopic");
        topicField.setAccessible(true);
        topicField.set(organizationService, TOPIC);
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
        organization.setOrganizationName("Test Org");
        String userId = "user123";

        when(organizationRepository.findById("123")).thenReturn(Optional.of(organization));

        organizationService.deleteOrganizationById("123");

        verify(organizationRepository).delete(organization);

        organizationService.createKafkaMessage(organization, userId, EventType.DELETE, null);

        verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
        EventDTO sentEvent = eventCaptor.getValue();
        assertEquals("Organization_Deleted", sentEvent.getType());
        assertTrue(sentEvent.getDescription().contains("Test Org"));
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
    String userId = "user123";

    Organization existingOrganization = new Organization();
    existingOrganization.setOrganizationID("123");
    existingOrganization.setOrganizationName("ORG_OLD");

    OrganizationDTO organizationDTO = new OrganizationDTO();
    organizationDTO.setOrganizationName("ORG_NEW");

    when(organizationRepository.findById("123")).thenReturn(Optional.of(existingOrganization));

    doAnswer(invocation -> {
        OrganizationDTO dto = invocation.getArgument(0);
        Organization org = invocation.getArgument(1);
        org.setOrganizationName(dto.getOrganizationName());
        return null; // For void methods, return null
    }).when(modelMapper).map(any(OrganizationDTO.class), any(Organization.class));

    when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Organization updatedOrganization = organizationService.updateOrganization("123", organizationDTO);

    // Assert
    assertNotNull(updatedOrganization);
    assertEquals("ORG_NEW", updatedOrganization.getOrganizationName());

    // Kafka verification
    organizationService.createKafkaMessage(updatedOrganization, userId, EventType.UPDATE,"orgs verifiable credential");
    verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
    EventDTO sentEvent = eventCaptor.getValue();
    assertEquals("Organization_Updated", sentEvent.getType());
    assertTrue(sentEvent.getDescription().contains("ORG_NEW"));

    // Verify interactions
    verify(modelMapper).map(any(OrganizationDTO.class), any(Organization.class));
    verify(organizationRepository).save(any(Organization.class));
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

     @Test
    void testGetOrganizationByName_Success() {
        // Arrange
        Organization org = new Organization();
        org.setOrganizationName("Test Org");

        when(organizationRepository.findByOrganizationName("Test Org"))
            .thenReturn(Optional.of(org));

        // Act
        Organization result = organizationService.getOrganizationByName("Test Org");

        // Assert
        assertNotNull(result);
        assertEquals("Test Org", result.getOrganizationName());
    }

    @Test
    void testGetOrganizationByName_NotFound() {
        // Arrange
        when(organizationRepository.findByOrganizationName("Missing Org"))
            .thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(OrganizationNotFoundException.class, () -> {
            organizationService.getOrganizationByName("Missing Org");
        });

        assertEquals("Organization with name Missing Org not found", exception.getMessage());
    }


       @Test
    void testCreateKafkaMessage_Success() {
        Organization organization = new Organization();
        organization.setOrganizationID("org-123");
        organization.setOrganizationName("Test Org");
        organization.setContact("email@example.com");
        organization.setDsConnectorURL("http://ds.example.com");

        String userId = "user123";

        SendResult<String, EventDTO> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.partition()).thenReturn(1);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, EventDTO>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(TOPIC), any(EventDTO.class))).thenReturn(future);

        organizationService.createKafkaMessage(organization, userId, EventType.CREATE, "verifiable credential");

        verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
        EventDTO sentEvent = eventCaptor.getValue();
        assertEquals("Organization_Onboarding", sentEvent.getType());
        assertTrue(sentEvent.getDescription().contains("Test Org"));
    }

    @Test
    void testCreateKafkaMessage_Failure() {
        Organization organization = new Organization();
        String userId = "user123";

        CompletableFuture<SendResult<String, EventDTO>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka down"));

        when(kafkaTemplate.send(eq(TOPIC), any(EventDTO.class))).thenReturn(future);

        organizationService.createKafkaMessage(organization, userId, EventType.CREATE, "verifiable credential");

        verify(kafkaTemplate).send(eq(TOPIC), any(EventDTO.class));
        assertTrue(Thread.currentThread().isInterrupted());
    }
    
    @Test
    void testGetOrganizationsByCapabilities_ResourcesFound_OrganizationsReturned() {
        // Arrange
        ManufacturingResource resource = new ManufacturingResource();
        resource.setManufacturingResourceID("507f1f77bcf86cd799439011");

        Organization org = new Organization();
        org.setOrganizationID("org1");
        org.setOrganizationName("TestOrg");
        org.setManufacturingResources(List.of(resource));

        OrganizationDTO orgDto = new OrganizationDTO();
        orgDto.setOrganizationName("TestOrg");

        when(manufacturingResourceService.findByCapabilities(any(), any()))
                .thenReturn(List.of(resource));
        when(organizationRepository.findByManufacturingResourceObjectIds(anyList()))
                .thenReturn(List.of(org));
        when(modelMapper.map(any(Organization.class), eq(OrganizationDTO.class)))
                .thenReturn(orgDto);

        List<OrganizationDTO> result =
                organizationService.getOrganizationsByCapabilities("Cutting", "Drilling");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TestOrg", result.get(0).getOrganizationName());

        verify(modelMapper, times(1)).map(any(Organization.class), eq(OrganizationDTO.class));
    }
    @Test
    void testGetOrganizationsByCapabilities_NoMatchingResources_ReturnsEmptyList() {
        when(manufacturingResourceService.findByCapabilities(any(), any()))
                .thenReturn(List.of());

        List<OrganizationDTO> result =
                organizationService.getOrganizationsByCapabilities("Cutting", "Drilling");

        assertTrue(result.isEmpty());
        verify(organizationRepository, never()).findByManufacturingResourceObjectIds(anyList());
    }

    @Test
    void testGetOrganizationsByCapabilities_ModelMapperCalledForEachOrg() {
        // Arrange
        ManufacturingResource resource = new ManufacturingResource();
        resource.setManufacturingResourceID("507f1f77bcf86cd799439011");

        Organization org1 = new Organization();
        org1.setOrganizationID("org1");
        org1.setOrganizationName("Org1");
        org1.setManufacturingResources(List.of(resource));

        Organization org2 = new Organization();
        org2.setOrganizationID("org2");
        org2.setOrganizationName("Org2");
        org2.setManufacturingResources(List.of(resource));

        when(manufacturingResourceService.findByCapabilities(any(), any()))
                .thenReturn(List.of(resource));
        when(organizationRepository.findByManufacturingResourceObjectIds(anyList()))
                .thenReturn(List.of(org1, org2));
        when(modelMapper.map(any(Organization.class), eq(OrganizationDTO.class)))
                .thenAnswer(invocation -> {
                    Organization source = invocation.getArgument(0);
                    OrganizationDTO dto = new OrganizationDTO();
                    dto.setOrganizationName(source.getOrganizationName());
                    return dto;
                });

        List<OrganizationDTO> result =
                organizationService.getOrganizationsByCapabilities("Machining", "Welding");

        assertEquals(2, result.size());
        assertEquals("Org1", result.get(0).getOrganizationName());
        assertEquals("Org2", result.get(1).getOrganizationName());

        verify(modelMapper, times(2))
                .map(any(Organization.class), eq(OrganizationDTO.class));
    }


    // ==========================================
    // 5. saveReview(targetOrgId, ...) TESTS
    // ==========================================

    @Test
    void testSaveReview_Success_MapsAndSavesReview() {
        // Arrange
        String targetOrgId = "target-123";
        String reviewerOrgId = "reviewer-456";
        String reviewerUserId = "user-789";

        CreateReviewDTO dto = new CreateReviewDTO();
        dto.setRating(5);
        dto.setComment("Excellent collaboration!");
        dto.setTargetRole(MaasRole.PROVIDER);

        Organization targetOrg = new Organization();
        targetOrg.setOrganizationID(targetOrgId);
        targetOrg.setOrganizationName("Target Logistics Inc.");
        targetOrg.setMaasRole(List.of(MaasRole.PROVIDER));

        Organization reviewerOrg = new Organization();
        reviewerOrg.setOrganizationID(reviewerOrgId);
        reviewerOrg.setOrganizationName("Reviewer Consumer Corp.");

        when(organizationRepository.findById(targetOrgId)).thenReturn(Optional.of(targetOrg));
        when(organizationRepository.findById(reviewerOrgId)).thenReturn(Optional.of(reviewerOrg));

        OrganizationReview result = organizationService.saveReview(targetOrgId, reviewerUserId, reviewerOrgId, dto);

        assertNotNull(result);
        assertEquals(targetOrgId, result.getTargetOrganizationId());
        assertEquals("Target Logistics Inc.", result.getTargetOrganizationName());
        assertEquals(reviewerOrgId, result.getReviewerOrganizationId());
        assertEquals("Reviewer Consumer Corp.", result.getReviewerOrganizationName());
        assertEquals(reviewerUserId, result.getReviewerUserId());
        assertEquals(5, result.getRating());
        assertEquals("Excellent collaboration!", result.getComment());
        assertEquals(MaasRole.PROVIDER, result.getTargetRole());

        verify(reviewRepository, times(1)).save(any(OrganizationReview.class));
    }

    @Test
    void testSaveReview_InvalidRoleContext_ThrowsInvalidOrganizationRoleException() {
        String targetOrgId = "target-123";
        String reviewerOrgId = "reviewer-456";
        String reviewerUserId = "user-789";

        CreateReviewDTO dto = new CreateReviewDTO();
        dto.setRating(3);
        dto.setComment("Mismatched role review attempt");
        dto.setTargetRole(MaasRole.CONSUMER);

        Organization targetOrg = new Organization();
        targetOrg.setOrganizationID(targetOrgId);
        targetOrg.setMaasRole(List.of(MaasRole.PROVIDER));

        Organization reviewerOrg = new Organization();
        reviewerOrg.setOrganizationID(reviewerOrgId);

        when(organizationRepository.findById(targetOrgId)).thenReturn(Optional.of(targetOrg));
        when(organizationRepository.findById(reviewerOrgId)).thenReturn(Optional.of(reviewerOrg));

        assertThrows(InvalidOrganizationRoleException.class, () -> {
            organizationService.saveReview(targetOrgId, reviewerUserId, reviewerOrgId, dto);
        });

        verify(reviewRepository, never()).save(any(OrganizationReview.class));
    }

    // ==========================================
    // 6. getReviewAnalytics(...) TESTS
    // ==========================================

    @Test
    void testGetReviewAnalytics_Success_ReturnsCombinedAnalyticsAndPage() {
        // Arrange
        String orgId = "target-org-123";
        MaasRole requestedRole = MaasRole.CONSUMER;
        Pageable pageable = PageRequest.of(0, 10);

        // Mock DB distribution aggregation data for PROVIDER
        // [ { "_id": 5, "count": 4 }, { "_id": 4, "count": 1 } ] -> Avg: (20+4)/5 = 4.8
        java.util.Map<String, Object> provRow1 = java.util.Map.of("_id", 5, "count", 4L);
        java.util.Map<String, Object> provRow2 = java.util.Map.of("_id", 4, "count", 1L);
        List<java.util.Map<String, Object>> mockProviderDist = List.of(provRow1, provRow2);

        // Mock DB distribution aggregation data for CONSUMER
        // [ { "_id": 3, "count": 2 } ] -> Avg: 6/2 = 3.0
        java.util.Map<String, Object> consRow1 = java.util.Map.of("_id", 3, "count", 2L);
        List<java.util.Map<String, Object>> mockConsumerDist = List.of(consRow1);


        when(reviewRepository.getStarCountDistribution(orgId, MaasRole.PROVIDER.name()))
                .thenReturn(mockProviderDist);
        when(reviewRepository.getStarCountDistribution(orgId, MaasRole.CONSUMER.name()))
                .thenReturn(mockConsumerDist);


        OrganizationReview mockReview = new OrganizationReview();
        mockReview.setId("rev-99");
        mockReview.setComment("Excellent consumer experience!");
        mockReview.setRating(3);
        mockReview.setTargetRole(MaasRole.CONSUMER);
        Page<OrganizationReview> mockPage = new PageImpl<>(List.of(mockReview), pageable, 1);

        when(reviewRepository.findByTargetOrganizationIdAndTargetRole(orgId, requestedRole, pageable))
                .thenReturn(mockPage);


        OrganizationReviewsResponseDTO result = 
                organizationService.getReviewAnalytics(orgId, requestedRole, pageable);

        assertNotNull(result);
        
        assertNotNull(result.getProviderAnalytics());
        assertEquals(4.8, result.getProviderAnalytics().getAverageRating());
        assertEquals(5, result.getProviderAnalytics().getTotalReviews());
        assertEquals(0, result.getProviderAnalytics().getStars1()); // 0 one-star review
        assertEquals(0, result.getProviderAnalytics().getStars2()); // 0 two-star review
        assertEquals(0, result.getProviderAnalytics().getStars3()); // 0 three-star review
        assertEquals(1, result.getProviderAnalytics().getStars4()); // 1 four-star reviews
        assertEquals(4, result.getProviderAnalytics().getStars5()); // 4 five-star reviews

        // Verify Consumer Analytics Calculations
        assertNotNull(result.getConsumerAnalytics());
        assertEquals(3.0, result.getConsumerAnalytics().getAverageRating());
        assertEquals(2, result.getConsumerAnalytics().getTotalReviews());
        assertEquals(0, result.getConsumerAnalytics().getStars1()); //  0 one-star reviews
        assertEquals(0, result.getConsumerAnalytics().getStars2()); //  0 two-star reviews
        assertEquals(2, result.getConsumerAnalytics().getStars3()); //  2 three-star review
        assertEquals(0, result.getConsumerAnalytics().getStars4()); //  0 four-star reviews
        assertEquals(0, result.getConsumerAnalytics().getStars5()); // 0  five-star reviews


        assertNotNull(result.getReviews());
        assertEquals(1, result.getReviews().getTotalElements());
        assertEquals("rev-99", result.getReviews().getContent().get(0).getId());
        assertEquals("Excellent consumer experience!", result.getReviews().getContent().get(0).getComment());

        verify(reviewRepository, times(1)).getStarCountDistribution(orgId, MaasRole.PROVIDER.name());
        verify(reviewRepository, times(1)).getStarCountDistribution(orgId, MaasRole.CONSUMER.name());
        verify(reviewRepository, times(1)).findByTargetOrganizationIdAndTargetRole(orgId, requestedRole, pageable);
    }


    @Test
    void testUpdateReview_Success_MutatesAndSaves() {
        String reviewId = "review-777";
        String currentUserId = "author-user-id";

        CreateReviewDTO editDto = new CreateReviewDTO();
        editDto.setRating(5);
        editDto.setComment("Updated feedback text!");

        OrganizationReview existingReview = new OrganizationReview();
        existingReview.setId(reviewId);
        existingReview.setReviewerUserId(currentUserId);
        existingReview.setRating(2); // Old rating
        existingReview.setComment("Old placeholder feedback");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(OrganizationReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationReview result = organizationService.updateReview(reviewId, currentUserId, editDto);

        assertNotNull(result);
        assertEquals(reviewId, result.getId());
        assertEquals(5, result.getRating());
        assertEquals("Updated feedback text!", result.getComment());
        assertNotNull(result.getUpdatedAt());

        verify(reviewRepository, times(1)).save(existingReview);
    }

    @Test
    void testUpdateReview_UserDoesNotOwnReview_ThrowsResponseStatusException() {
        // Arrange
        String reviewId = "review-777";
        String currentUserId = "malicious-user-id"; // Different user

        CreateReviewDTO editDto = new CreateReviewDTO();
        editDto.setRating(5);
        editDto.setComment("Trying to edit someone else's text");

        OrganizationReview existingReview = new OrganizationReview();
        existingReview.setId(reviewId);
        existingReview.setReviewerUserId("original-author-id"); // Original author

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            organizationService.updateReview(reviewId, currentUserId, editDto);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("You do not own this review."));
        
        verify(reviewRepository, never()).save(any(OrganizationReview.class));
    }

    @Test
    void testUpdateReview_ReviewNotFound_ThrowsIllegalArgumentException() {
        // Arrange
        String reviewId = "non-existent-review-id";
        String currentUserId = "any-user-id";
        CreateReviewDTO editDto = new CreateReviewDTO();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            organizationService.updateReview(reviewId, currentUserId, editDto);
        });

        assertTrue(exception.getMessage().contains("Review not found with ID: " + reviewId));
        verify(reviewRepository, never()).save(any(OrganizationReview.class));
    }
    

    // ==========================================
    // 9. getReviewsPerformedByOrganization(...) TESTS
    // ==========================================

    @Test
    void testGetReviewsPerformedByOrganization_WithValidTargetFilter_ReturnsFilteredPage() {
        // Arrange
        String reviewerOrgId = "my-org-111";
        String targetOrgId = "target-org-222";
        Pageable pageable = PageRequest.of(0, 10);

        OrganizationReview mockReview = new OrganizationReview();
        mockReview.setId("rev-1");
        mockReview.setReviewerOrganizationId(reviewerOrgId);
        mockReview.setTargetOrganizationId(targetOrgId);
        Page<OrganizationReview> expectedPage = new PageImpl<>(List.of(mockReview), pageable, 1);

        // Target org exists check must return true
        when(organizationRepository.existsById(targetOrgId)).thenReturn(true);
        when(reviewRepository.findByReviewerOrganizationIdAndTargetOrganizationIdOrderByCreatedAtDesc(reviewerOrgId, targetOrgId, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<OrganizationReview> result = organizationService.getReviewsPerformedByOrganization(reviewerOrgId, targetOrgId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("rev-1", result.getContent().get(0).getId());

        verify(organizationRepository, times(1)).existsById(targetOrgId);
        verify(reviewRepository, times(1)).findByReviewerOrganizationIdAndTargetOrganizationIdOrderByCreatedAtDesc(reviewerOrgId, targetOrgId, pageable);
        verify(reviewRepository, never()).findByReviewerOrganizationIdOrderByCreatedAtDesc(anyString(), any(Pageable.class));
    }

    @Test
    void testGetReviewsPerformedByOrganization_NoTargetFilter_ReturnsGlobalOutboundPage() {
        String reviewerOrgId = "my-org-111";
        Pageable pageable = PageRequest.of(0, 10);

        OrganizationReview review1 = new OrganizationReview();
        review1.setId("rev-1");
        OrganizationReview review2 = new OrganizationReview();
        review2.setId("rev-2");
        Page<OrganizationReview> expectedPage = new PageImpl<>(List.of(review1, review2), pageable, 2);

        when(reviewRepository.findByReviewerOrganizationIdOrderByCreatedAtDesc(reviewerOrgId, pageable))
                .thenReturn(expectedPage);

        Page<OrganizationReview> resultWithNull = organizationService.getReviewsPerformedByOrganization(reviewerOrgId, null, pageable);
        Page<OrganizationReview> resultWithBlank = organizationService.getReviewsPerformedByOrganization(reviewerOrgId, "   ", pageable);

        assertNotNull(resultWithNull);
        assertEquals(2, resultWithNull.getTotalElements());
        assertNotNull(resultWithBlank);

        verify(organizationRepository, never()).existsById(anyString());
        verify(reviewRepository, times(2)).findByReviewerOrganizationIdOrderByCreatedAtDesc(reviewerOrgId, pageable);
        verify(reviewRepository, never()).findByReviewerOrganizationIdAndTargetOrganizationIdOrderByCreatedAtDesc(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void testGetReviewsPerformedByOrganization_TargetOrgNotFound_ThrowsOrganizationNotFoundException() {
        String reviewerOrgId = "my-org-111";
        String missingTargetOrgId = "ghost-org-999";
        Pageable pageable = PageRequest.of(0, 10);

        when(organizationRepository.existsById(missingTargetOrgId)).thenReturn(false);

        OrganizationNotFoundException exception = assertThrows(OrganizationNotFoundException.class, () -> {
            organizationService.getReviewsPerformedByOrganization(reviewerOrgId, missingTargetOrgId, pageable);
        });

        assertTrue(exception.getMessage().contains("Target organization not found with ID: " + missingTargetOrgId));

        verify(organizationRepository, times(1)).existsById(missingTargetOrgId);
        verifyNoInteractions(reviewRepository);
    }
}

