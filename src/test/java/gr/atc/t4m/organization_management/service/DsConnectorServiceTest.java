package gr.atc.t4m.organization_management.service;

import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.DatasetEntry;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DsConnectorServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CapabilityService capabilityService;

    @InjectMocks
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
        ResponseEntity<String> expected = new ResponseEntity<>("Catalog content", HttpStatus.OK);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expected);

        ResponseEntity<String> result = dsConnectorService.fetchCatalog(catalogDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Catalog content", result.getBody());
    }

    @Test
    void requestDatasetTransfer_Success() throws Exception {

        when(restTemplate.exchange(
                any(URI.class),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(new ResponseEntity<>("Transfer OK", HttpStatus.OK));

        ResponseEntity<String> result = dsConnectorService.requestDatasetTransfer("test-dataset-id");

        assertEquals("Transfer OK", result.getBody());
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
    void retrieveCapabilities_Success() throws Exception {
        ResponseEntity<String> catalogResponse = new ResponseEntity<>("catalog", HttpStatus.OK);
        ResponseEntity<String> capabilitiesResponse = new ResponseEntity<>("capabilities", HttpStatus.OK);

        // Step 1: fetchCatalog (POST)
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(catalogResponse);

        when(restTemplate.exchange(
                argThat(uri -> uri != null && uri.toString().contains("/v1/consumer/")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(capabilitiesResponse);

        when(capabilityService.retrieveCapabilitiesInformation("catalog"))
                .thenReturn(Collections.singletonList(datasetEntry));
        List<CapabilityEntry> capabilities = Collections.singletonList(new CapabilityEntry());
        when(capabilityService.parseAASCapabilities("capabilities")).thenReturn(capabilities);

        ManufacturingResource result = dsConnectorService.retrieveCapabilities(catalogDTO);

        assertNotNull(result);
        assertEquals("test-dataset-id", result.getCapabilityDatasetID());
        assertEquals("Test Title", result.getManufacturingResourceTitle());
        assertEquals(capabilities, result.getCapabilities());
    }

}
