package gr.atc.t4m.organization_management.service;

import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.DSTransferProcess;
import gr.atc.t4m.organization_management.model.DatasetEntry;
import gr.atc.t4m.organization_management.dto.DatasetListDTO;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.dto.ParticipantDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DsConnectorServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CapabilityService capabilityService;

    @InjectMocks
    @Spy
    private DsConnectorService dsConnectorService;

    private CatalogDTO catalogDTO;
    private DatasetEntry datasetEntry;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dsConnectorService, "dsConnectorUrl", "http://test-ds-connector.com/");
        ReflectionTestUtils.setField(dsConnectorService, "dsConnectorDataPlaneUrl", "https://test-ds-connector.com/api/data-plane/");

        catalogDTO = new CatalogDTO();
        catalogDTO.setProviderUrl("http://test-provider.com");
        catalogDTO.setPage(0);
        catalogDTO.setSize(10);

        datasetEntry = new DatasetEntry();
        datasetEntry.setId("test-dataset-id");
        datasetEntry.setTitle("Test Title");
    }

@Test
void fetchCatalog_Success() throws Exception {
    
    ParticipantDTO mockBody = new ParticipantDTO();
    mockBody.setParticipantId("expected-id-123");
    
    ResponseEntity<ParticipantDTO> responseEntity = new ResponseEntity<>(mockBody, HttpStatus.OK);

    when(restTemplate.exchange(
            any(URI.class), 
            eq(HttpMethod.POST), 
            any(HttpEntity.class), 
            eq(ParticipantDTO.class)
    )).thenReturn(responseEntity);

    ParticipantDTO result = dsConnectorService.fetchCatalog(catalogDTO);

    assertNotNull(result, "The result should not be null");
    assertEquals("expected-id-123", result.getParticipantId());
}

   @Test
void requestDatasetTransfer_Success() throws Exception {
    
    DSTransferProcess mockProcess = new DSTransferProcess();
    mockProcess.setConsumerPid("consumer-id-123");
 
    ResponseEntity<DSTransferProcess> responseEntity = 
        new ResponseEntity<>(mockProcess, HttpStatus.OK);

    when(restTemplate.exchange(
            any(URI.class),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(DSTransferProcess.class)
    )).thenReturn(responseEntity);

    DSTransferProcess result = dsConnectorService.requestDatasetTransfer("test-dataset-id");

    assertNotNull(result);
    assertEquals("consumer-id-123", result.getConsumerPid());
}
    @Test
    void consumeCapabilities_Success() throws Exception {
        ResponseEntity<String> expected = new ResponseEntity<>("Capabilities Data", HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expected);

        ResponseEntity<String> result = dsConnectorService.consumeCapabilities("valid-token");

        assertEquals("Capabilities Data", result.getBody());
    }

    @Test
    void consumeCapabilities_InvalidToken_ThrowsURISyntaxException() {
        assertThrows(URISyntaxException.class,
                () -> dsConnectorService.consumeCapabilities("\"bad uri\""));
    }

    @Test
    void validateOrganization_Success() {
        ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        HttpStatus status = dsConnectorService.validateOrganization(catalogDTO);

        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void validateOrganization_InvalidUri_ThrowsBadRequest() {
        ReflectionTestUtils.setField(dsConnectorService, "dsConnectorUrl", "::invalid::url");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> dsConnectorService.validateOrganization(catalogDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void validateOrganization_HttpError_ThrowsInternalServerError() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> dsConnectorService.validateOrganization(catalogDTO));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
    
   @Test
void checkDatasetAgreement_Success() throws Exception {
    Map<String, Object> mockBody = new HashMap<>();
    List<String> mockDataList = Collections.singletonList("agreement-id-123");
    mockBody.put("data", mockDataList);

    ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockBody, HttpStatus.OK);

    ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {};

    org.mockito.Mockito.doReturn(responseEntity)
            .when(restTemplate)
            .exchange(
                    any(URI.class),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(typeRef)
            );

    String result = dsConnectorService.checkDatasetAgreement("test-dataset-id");

    assertNotNull(result, "Result should not be null on success");
    assertEquals("agreement-id-123", result);
}

@Test
void checkDatasetAgreement_EmptyData_ReturnsNull() throws Exception {
    Map<String, Object> mockBody = new HashMap<>();
    mockBody.put("data", Collections.emptyList());

    ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockBody, HttpStatus.OK);

    ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {};

    org.mockito.Mockito.doReturn(responseEntity)
            .when(restTemplate)
            .exchange(
                    any(URI.class),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(typeRef)
            );

    String result = dsConnectorService.checkDatasetAgreement("test-dataset-id");

    assertNull(result, "Should return null if the data list is empty");
}

@Test
void getTransferState_Success() throws Exception {

    String mockJsonResponse = "{\"state\": \"STARTED\", \"id\": \"test-id\"}";
    ResponseEntity<String> responseEntity = new ResponseEntity<>(mockJsonResponse, HttpStatus.OK);

    when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
            .thenReturn(responseEntity);

    String state = dsConnectorService.getTransferState("transfer-123");

    assertNotNull(state);
    assertEquals("STARTED", state);
}
@Test
void getTransferState_MissingKey_ReturnsIncorrectState() throws Exception {
    // JSON is valid, but the "state" key is missing
    String invalidJson = "{ \"other_key\": \"some_value\" }";
    ResponseEntity<String> responseEntity = new ResponseEntity<>(invalidJson, HttpStatus.OK);

    when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
            .thenReturn(responseEntity);

    String state = dsConnectorService.getTransferState("transfer-123");

    assertEquals("incorrect-state", state);
}


@Test
void getDatasetMetadata_Success() {
    // 1. Setup Mock DTO and ID with quotes to test sanitization
    String datasetIdWithQuotes = "\"test-dataset-id\"";
    DatasetEntry mockEntry = new DatasetEntry();
    mockEntry.setId("test-dataset-id");

    ResponseEntity<DatasetEntry> responseEntity = new ResponseEntity<>(mockEntry, HttpStatus.OK);

    when(restTemplate.exchange(
            any(URI.class),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(DatasetEntry.class)
    )).thenReturn(responseEntity);

    DatasetEntry result = dsConnectorService.getDatasetMetadata(datasetIdWithQuotes);
    assertNotNull(result);
    assertEquals("test-dataset-id", result.getId());
}


@Test
void getDatasetMetadata_UnexpectedError_Throws500() {
    
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(DatasetEntry.class)))
            .thenThrow(new RuntimeException("Connection failed"));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
        dsConnectorService.getDatasetMetadata("any-id");
    });

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertTrue(exception.getReason().contains("Failed to retrieve dataset metadata"));
}
@Test
void retrieveCapabilities_FullSuccess() throws Exception {
    CatalogDTO catalogDTO = new CatalogDTO();
    catalogDTO.setProviderUrl("http://provider.com");

    ParticipantDTO mockParticipant = new ParticipantDTO();
    mockParticipant.setId("participant-123");

    // This represents the Parent Machine discovered in step 1
    DatasetListDTO mockParentMachineList = new DatasetListDTO();
    mockParentMachineList.setData(Collections.singletonList("machine-parent-id"));

    // The metadata of the Parent Machine Node
    DatasetEntry mockParentEntry = new DatasetEntry();
    mockParentEntry.setId("machine-parent-id");
    mockParentEntry.setTitle("F_005-DMG_MORI_DMF_360L"); // Provides the title engine!

    // The metadata of the child node matching CapabilityDescription
    DatasetEntry mockChildEntry = new DatasetEntry();
    mockChildEntry.setId("dataset-001");
    mockChildEntry.setTitle("CapabilityDescription");

    DSTransferProcess mockTransfer = new DSTransferProcess();
    mockTransfer.setId("transfer-999");

    doReturn(mockParticipant).when(dsConnectorService).fetchCatalog(any(CatalogDTO.class));
    doReturn("participant-123").when(dsConnectorService).searchParticipantCatalogue(anyString());
    
    doReturn(mockParentMachineList).when(dsConnectorService)
            .retrieveCatalogDatasets(any(CatalogDTO.class), anyString());
    
    doReturn(mockParentEntry).when(dsConnectorService).getDatasetMetadata("machine-parent-id");

    List<String> mockChildrenIds = Collections.singletonList("dataset-001");
    doReturn(mockChildrenIds).when(dsConnectorService).getDatasetChildrenIds("machine-parent-id");
    
    doReturn(mockChildEntry).when(dsConnectorService).getDatasetMetadata("dataset-001");
    doReturn("agreement-abc").when(dsConnectorService).checkDatasetAgreement("dataset-001");
    doReturn(mockTransfer).when(dsConnectorService).requestDatasetTransfer("dataset-001");
    doReturn("STARTED").when(dsConnectorService).getTransferState("transfer-999");
    
    ResponseEntity<String> mockResponse = new ResponseEntity<>("RAW_AAS_JSON", HttpStatus.OK);
    doReturn(mockResponse).when(dsConnectorService).consumeCapabilities("dataset-001");
    
    List<CapabilityEntry> mockCaps = Collections.singletonList(new CapabilityEntry());
    when(capabilityService.parseAASCapabilities("RAW_AAS_JSON")).thenReturn(mockCaps);

    
    List<ManufacturingResource> results = dsConnectorService.retrieveUnifiedResources(catalogDTO);


    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("dataset-001", results.get(0).getCapabilityDatasetID()); // Populated perfectly from the loop child!
    assertEquals("F_005-DMG_MORI_DMF_360L", results.get(0).getManufacturingResourceTitle()); // Clean human title verified
    
    verify(dsConnectorService, times(1)).requestDatasetTransfer(anyString());
    verify(capabilityService, times(1)).parseAASCapabilities(anyString());
}

}
