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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        // Since we are not using Mockito to inject the field, we need to set it manually.
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
}