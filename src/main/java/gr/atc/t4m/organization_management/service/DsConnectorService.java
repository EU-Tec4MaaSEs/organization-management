package gr.atc.t4m.organization_management.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;


import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.DatasetEntry;
import gr.atc.t4m.organization_management.model.ManufacturingResource;

@Service
public class DsConnectorService {

    @Value("${ds.connector.url}")
    private String dsConnectorUrl;
    @Value("${ds.connector.dataplane.url}")
    private String dsConnectorDataPlaneUrl;
    private final CapabilityService capabilityService;
    private final RestTemplate restTemplate;

    private static final String V1_REQUEST_CATALOGUE = "v1/request/catalog";
    private static final String V1_REQUEST_TRANSFER_DATASET = "v1/request/transfer/dataset/";
    private static final String V1_CONSUMER = "v1/consumer/";

    private static final Logger LOGGER = LoggerFactory.getLogger(DsConnectorService.class);
    public DsConnectorService(CapabilityService capabilityService, RestTemplate restTemplate) {
        this.capabilityService = capabilityService;
        this.restTemplate = restTemplate;
    }


    /**
     * Validates the organization by sending a request to the ds connector.
     *
     * @param catalogDTO The DTO containing the organization details.
     */

    public HttpStatus validateOrganization(CatalogDTO catalogDTO) {
        LOGGER.info("Validating organization URL: {}", catalogDTO.getProviderUrl());

        try {
            String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
            URI baseUri = new URI(baseUrl + V1_REQUEST_CATALOGUE);

            URI finalUri = UriComponentsBuilder.newInstance()
                    .uri(baseUri)
                    .queryParam("page", catalogDTO.getPage())
                    .queryParam("size", catalogDTO.getSize())
                    .queryParam("refresh", catalogDTO.getRefresh())
                    .build()
                    .toUri();


            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("providerUrl", catalogDTO.getProviderUrl());
            HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    finalUri,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);



            return (HttpStatus) response.getStatusCode();

        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base URI: " + e.getMessage(), e);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Validation failed: " + e.getMessage(), e);

        }
    }

    public ManufacturingResource retrieveCapabilities(CatalogDTO catalogDTO) {
         LOGGER.info("Validating organization URL: {}", catalogDTO.getProviderUrl());

        try {
            
           
           ResponseEntity<String> response = fetchCatalog(catalogDTO);
    
            List<DatasetEntry>  datasetEntries = capabilityService.retrieveCapabilitiesInformation(response.getBody());

            ManufacturingResource manufacturingResource = new ManufacturingResource();
            manufacturingResource.setCapabilityDatasetID(datasetEntries.get(0).getId());
            manufacturingResource.setManufacturingResourceTitle(datasetEntries.get(0).getTitle());
              ResponseEntity<String>  transferResponse = requestDatasetTransfer(datasetEntries.get(0).getId());
                LOGGER.info("Transfer response status: {}", transferResponse.getStatusCode());

            ResponseEntity<String> capabilitiesResult =  consumeCapabilities(datasetEntries.get(0).getId());
             List<CapabilityEntry> capabilities =  capabilityService.parseAASCapabilities(capabilitiesResult.getBody());
             manufacturingResource.setCapabilities(capabilities);
            return manufacturingResource;


        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base URI: " + e.getMessage(), e);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Validation failed: " + e.getMessage(), e);

        }
    }

    public ResponseEntity<String> consumeCapabilities(String token) throws URISyntaxException {
     String baseUrl = dsConnectorDataPlaneUrl;

    String sanitizedToken = token.replace("\"", "");

    URI baseUri = new URI(baseUrl + V1_CONSUMER + sanitizedToken);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

    ResponseEntity<String> response = restTemplate.exchange(
            baseUri,
            HttpMethod.GET,
            requestEntity,
            String.class
    );

    LOGGER.info("Consume response status: {}", response.getStatusCode());
    return response;
    }


    public ResponseEntity<String> fetchCatalog(CatalogDTO catalogDTO) throws URISyntaxException {
        String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
        URI baseUri = new URI(baseUrl + V1_REQUEST_CATALOGUE);

    URI finalUri = UriComponentsBuilder.newInstance()
            .uri(baseUri)
            .queryParam("page", catalogDTO.getPage())
            .queryParam("size", catalogDTO.getSize())
            .queryParam("refresh", catalogDTO.getRefresh())
            .queryParam("title", catalogDTO.getTitle())
            .build()
            .toUri();


    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("providerUrl", catalogDTO.getProviderUrl());

    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

    // Execute POST request
    return restTemplate.exchange(
            finalUri,
            HttpMethod.POST,
            requestEntity,
            String.class
    );

}

 public ResponseEntity<String> requestDatasetTransfer(String datasetId) throws URISyntaxException {
        String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
        URI baseUri = new URI(baseUrl + V1_REQUEST_TRANSFER_DATASET + datasetId);


    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

    return restTemplate.exchange(
            baseUri,
            HttpMethod.POST,
            requestEntity,
            String.class
    );

}

}
