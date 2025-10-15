
package gr.atc.t4m.organization_management.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import gr.atc.t4m.organization_management.dto.EventDTO;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.EventType;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;

import org.apache.kafka.clients.producer.RecordMetadata;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;


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

        organizationService.createKafkaMessage(organization, userId, EventType.DELETE);

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
    organizationService.createKafkaMessage(updatedOrganization, userId, EventType.UPDATE);
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

        organizationService.createKafkaMessage(organization, userId, EventType.CREATE);

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

        organizationService.createKafkaMessage(organization, userId, EventType.CREATE);

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
}

