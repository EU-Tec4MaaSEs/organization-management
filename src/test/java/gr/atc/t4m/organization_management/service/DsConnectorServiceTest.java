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
import org.mockito.MockedConstruction;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DsConnectorServiceTest {

        @Mock
        private CapabilityService capabilityService;

        @InjectMocks
        @Spy
        private DsConnectorService dsConnectorServiceSpy;

        private CatalogDTO catalogDTO;
        private DatasetEntry datasetEntry;
        private List<CapabilityEntry> capabilityEntries;
        private String datasetId;


        @BeforeEach
        void setUp() {

                ReflectionTestUtils.setField(dsConnectorServiceSpy, "dsConnectorUrl", "http://test-ds-connector.com");

                catalogDTO = new CatalogDTO();
                catalogDTO.setProviderUrl("http://test-provider.com");
                catalogDTO.setPage(0);
                catalogDTO.setSize(10);
                catalogDTO.setRefresh(false);

                datasetEntry = new DatasetEntry();
                datasetEntry.setId("test-dataset-id");
                datasetEntry.setTitle("Test Title");

                capabilityEntries = Collections.singletonList(new CapabilityEntry());

                datasetId = "test-dataset-id";
            }
        

        @Test
        void retrieveCapabilities_Success_WithSpy() throws URISyntaxException, IOException {
                // Arrange
                ResponseEntity<String> catalogResponse = new ResponseEntity<>("catalog data", HttpStatus.OK);
                ResponseEntity<String> transferResponse = new ResponseEntity<>("transfer data", HttpStatus.OK);
                ResponseEntity<String> capabilitiesResponse = new ResponseEntity<>("capabilities data", HttpStatus.OK);

                // Stub the private methods to return our mock responses
                doReturn(catalogResponse).when(dsConnectorServiceSpy).fetchCatalog(any(CatalogDTO.class));
                doReturn(transferResponse).when(dsConnectorServiceSpy).requestDatasetTransfer(anyString());
                doReturn(capabilitiesResponse).when(dsConnectorServiceSpy).consumeCapabilities(anyString());

                // Mock the CapabilityService calls as usual
                when(capabilityService.retrieveCapabilitiesInformation(catalogResponse.getBody()))
                                .thenReturn(Collections.singletonList(datasetEntry));
                when(capabilityService.parseAASCapabilities(capabilitiesResponse.getBody()))
                                .thenReturn(capabilityEntries);

                // Act
                ManufacturingResource result = dsConnectorServiceSpy.retrieveCapabilities(catalogDTO);

                // Assert
                assertNotNull(result);
                assertEquals("test-dataset-id", result.getCapabilityDatasetID());
                assertEquals("Test Title", result.getManufacturingResourceTitle());
                assertEquals(capabilityEntries, result.getCapabilities());
        }

        @Test
        void validateOrganization_validResponse_returnsHttpStatus() {
   

                ResponseEntity<String> mockResponse = new ResponseEntity<>("OK", HttpStatus.OK);

                // Mock construction of RestTemplate
                try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                                (mock, context) -> {
                                        when(mock.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                                                        eq(String.class)))
                                                        .thenReturn(mockResponse);
                                })) {

                        HttpStatus status = dsConnectorServiceSpy.validateOrganization(catalogDTO);
                        assertEquals(HttpStatus.OK, status);

                        // Verify that exchange was called once
                        RestTemplate mockRestTemplate = mocked.constructed().get(0);
                        verify(mockRestTemplate, times(1))
                                        .exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                                                        eq(String.class));
                }
        }

        @Test
        void validateOrganization_invalidUri_throwsBadRequest() {
                catalogDTO.setProviderUrl("http://example.com");

                // Inject an invalid dsConnectorUrl to trigger URISyntaxException
                ReflectionTestUtils.setField(dsConnectorServiceSpy, "dsConnectorUrl", "::invalid::url");

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> dsConnectorServiceSpy.validateOrganization(catalogDTO));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertTrue(ex.getReason().contains("Invalid base URI"));
        }

        @Test
        void validateOrganization_httpError_throwsInternalServerError() {
                CatalogDTO dto = new CatalogDTO();
                dto.setProviderUrl("http://test.url");

                // Mock RestTemplate to throw exception
                try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                                (mock, context) -> {
                                        when(mock.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
                                                        eq(String.class)))
                                                        .thenThrow(new RuntimeException("Connection failed"));
                                })) {

                        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                        () -> dsConnectorServiceSpy.validateOrganization(dto));
                        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
                        assertTrue(ex.getReason().contains("Validation failed"));
                }
        }



        @Test
    void consumeCapabilities_returnsOk() throws URISyntaxException {
        // Mock the private RestTemplate call by spying the public method
        doReturn(new ResponseEntity<>("{}", HttpStatus.OK))
                .when(dsConnectorServiceSpy).consumeCapabilities(anyString());

        ResponseEntity<String> response = dsConnectorServiceSpy.consumeCapabilities("token123");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{}", response.getBody());
    }

    @Test
    void fetchCatalog_returnsOk() throws URISyntaxException {
        catalogDTO.setProviderUrl("http://test.url");
        catalogDTO.setPage(0);
        catalogDTO.setSize(10);
        catalogDTO.setRefresh(false);
        catalogDTO.setTitle("Test");

        // Spy the fetchCatalog call
        doReturn(new ResponseEntity<>("{}", HttpStatus.OK))
                .when(dsConnectorServiceSpy).fetchCatalog(any(CatalogDTO.class));

        ResponseEntity<String> response = dsConnectorServiceSpy.fetchCatalog(catalogDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{}", response.getBody());
    }

    @Test
    void requestDatasetTransfer_returnsOk() throws URISyntaxException {

        // Spy the requestDatasetTransfer call
        doReturn(new ResponseEntity<>("{}", HttpStatus.OK))
                .when(dsConnectorServiceSpy).requestDatasetTransfer(datasetId);

        ResponseEntity<String> response = dsConnectorServiceSpy.requestDatasetTransfer(datasetId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{}", response.getBody());
    }

     @Test
    void requestDatasetTransfer_Success() throws URISyntaxException {
        // Arrange
        String expectedUrl = "http://test-ds-connector.com/v1/request/transfer/dataset/test-dataset-id";
        ResponseEntity<String> successResponse = new ResponseEntity<>("Transfer successful", HttpStatus.OK);
        
        // Use doReturn to stub the method call on the spy
        doReturn(successResponse).when(dsConnectorServiceSpy).requestDatasetTransfer(any(String.class));

        // Act
        ResponseEntity<String> response = dsConnectorServiceSpy.requestDatasetTransfer(datasetId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Transfer successful", response.getBody());
    }

    @Test
    void requestDatasetTransfer_ThrowsException() throws URISyntaxException {
        // Arrange
        // We simulate an exception to ensure the method handles it correctly.
        doThrow(new URISyntaxException("invalid url", "Invalid syntax")).when(dsConnectorServiceSpy).requestDatasetTransfer(any(String.class));

        // Act & Assert
        assertThrows(URISyntaxException.class, () -> dsConnectorServiceSpy.requestDatasetTransfer(datasetId));
    }


    @Test
    void consumeCapabilities_Success() throws URISyntaxException {
        // Arrange
        String token = "valid-token";
        ResponseEntity<String> successResponse = new ResponseEntity<>("Capabilities Data", HttpStatus.OK);
        
        doReturn(successResponse).when(dsConnectorServiceSpy).consumeCapabilities(any(String.class));

        ResponseEntity<String> response = dsConnectorServiceSpy.consumeCapabilities(token);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Capabilities Data", response.getBody());
    }

    @Test
    void consumeCapabilities_ThrowsURISyntaxException() throws URISyntaxException {
        // Arrange
        String invalidToken = "\"invalid uri\"";
        
        doThrow(new URISyntaxException("invalid uri", "Invalid syntax")).when(dsConnectorServiceSpy).consumeCapabilities(invalidToken);

        assertThrows(URISyntaxException.class, () -> dsConnectorServiceSpy.consumeCapabilities(invalidToken));
    }

     @Test
    void fetchCatalog_Success() throws URISyntaxException {
        ResponseEntity<String> successResponse = new ResponseEntity<>("Catalog content", HttpStatus.OK);
        
        doReturn(successResponse).when(dsConnectorServiceSpy).fetchCatalog(any(CatalogDTO.class));

        ResponseEntity<String> response = dsConnectorServiceSpy.fetchCatalog(catalogDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Catalog content", response.getBody());
    }

    @Test
    void fetchCatalog_ThrowsURISyntaxException() throws URISyntaxException {
        doThrow(new URISyntaxException("invalid uri", "Invalid syntax")).when(dsConnectorServiceSpy).fetchCatalog(any(CatalogDTO.class));

        assertThrows(URISyntaxException.class, () -> dsConnectorServiceSpy.fetchCatalog(catalogDTO));
    }
}

