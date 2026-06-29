package gr.atc.t4m.organization_management.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;


import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.DSNegotiationInfo;
import gr.atc.t4m.organization_management.model.DSTransferProcess;
import gr.atc.t4m.organization_management.model.DatasetEntry;
import gr.atc.t4m.organization_management.dto.DatasetListDTO;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.dto.ParticipantDTO;

@Service
public class DsConnectorService {

    @Value("${ds.connector.url}")
    private String dsConnectorUrl;
    @Value("${ds.connector.dataplane.url}")
    private String dsConnectorDataPlaneUrl;
    @Value("${ds.connector.participantId}")
    private String dsParticipantId;
    private final CapabilityService capabilityService;
    private final RestTemplate restTemplate;

    private static final String V1_REQUEST_CATALOGUE = "v1/catalog-control/request";
    private static final String V1_REQUEST_TRANSFER_DATASET = "v1/transfer-control/provider/request";
    private static final String V1_CONSUMER = "v1/consumer/";
    private static final String V1_NEGOTIATION_PROVIDER_REQUEST = "v1/negotiation-control/provider/request";
    private static final String V1_DATASETS_REQUESTED= "v1/datasets/requested/";
    private static final String V1_PARTICIPANTS= "v1/participants/";
    private static final String V1_CATALOG = "v1/catalog/";
    private static final String V1_CONTRACT = "v1/contract/";
    private static final String HEADER_X_REQUEST_ID = "x-request-id";
    private static final String V1_TRANSFER = "v1/transfer/";
    private static final String FINALIZED = "FINALIZED";
    private static final String TERMINATED = "TERMINATED";
    private static final String STARTED = "STARTED";
    private static final String DATASET_NOT_FOUND = "Dataset not found";

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
            headers.set(HEADER_X_REQUEST_ID, dsParticipantId);

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

public List<ManufacturingResource> retrieveUnifiedResources(CatalogDTO catalogDTO) 
        throws URISyntaxException, IOException {
    ParticipantDTO response = fetchCatalog(catalogDTO);
    String participantCatalogueId = searchParticipantCatalogue(response.getId()).replace("\"", "");

    DatasetListDTO parentMachines = retrieveCatalogDatasets(catalogDTO, participantCatalogueId);
    if (parentMachines == null || parentMachines.getData() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No parent machine profiles discovered.");
    }

    List<ManufacturingResource> manufacturingResources = new ArrayList<>();
    //Find children for every machine to extract data
    for (String parentMachineId : parentMachines.getData()) {
        DatasetEntry parentEntry = fetchDatasetMetadata(parentMachineId);
        if (parentEntry == null) {
            continue;
        }

         ManufacturingResource machineResource = new ManufacturingResource();
         machineResource.setManufacturingResourceTitle(parentEntry.getTitle());
         machineResource.setProviderUrl(catalogDTO.getProviderUrl());
        
        processMachineSubmodels(parentMachineId, machineResource);

        manufacturingResources.add(machineResource);
    }

    return manufacturingResources;
}


private void processMachineSubmodels(String parentMachineId, ManufacturingResource resource) throws IOException {
    List<String> childrenIds = getDatasetChildrenIds(parentMachineId);

    for (String childId : childrenIds) {
        DatasetEntry childEntry = fetchDatasetMetadata(childId);
        if (childEntry == null) {
            continue;
        }

        if ("CapabilityDescription".equalsIgnoreCase(childEntry.getTitle())) {
            handleCapabilityDescription(childEntry, resource);
        } else if ("ProductionCalendar".equalsIgnoreCase(childEntry.getTitle())) {
            handleProductionCalendar(childEntry, resource);
        }
    }
}

private void handleCapabilityDescription(DatasetEntry childEntry, ManufacturingResource resource) throws IOException {
    String jsonBody = fetchChildPayload(childEntry);
    List<CapabilityEntry> capabilities = capabilityService.parseAASCapabilities(jsonBody);

    resource.setCapabilityDatasetID(childEntry.getId());
    resource.setCapabilities(capabilities);
}

private void handleProductionCalendar(DatasetEntry childEntry, ManufacturingResource resource) {
    resource.setProductionCalendarDatasetID(childEntry.getId());
    
    // Parse the baseline file reference directly from this level
    String jsonBody = fetchChildPayload(childEntry);
    String icalFileRef = capabilityService.parseProductionCalendarFileRef(jsonBody);
    resource.setCalendarFileRef(icalFileRef);

    // Scan for deeper nested children to retrieve calendar dataset 
    List<String> grandchildIds = getDatasetChildrenIds(childEntry.getId());
    for (String grandchildId : grandchildIds) {
        DatasetEntry grandchildEntry = fetchDatasetMetadata(grandchildId);
        if (grandchildEntry != null && "calendar".equalsIgnoreCase(grandchildEntry.getTitle())) {
            processNestedCalendarLeaf(grandchildEntry, resource);
        }
    }
}

private void processNestedCalendarLeaf(DatasetEntry grandchildEntry, ManufacturingResource resource) {
    LOGGER.warn("SUCCESS: Triggered 'calendar' leaf node branch!");

    resource.setCalendarDatasetID(grandchildEntry.getId());

    try {
        String rawCalendarText = fetchChildPayload(grandchildEntry);
        resource.setRawCalendarContent(rawCalendarText);
    } catch (Exception e) {
        LOGGER.error("General error retrieving calendar payload for ID: {}", grandchildEntry.getId(), e);
    }
}
    private DatasetEntry fetchDatasetMetadata(String datasetId) {
    try {
        return getDatasetMetadata(datasetId);
    } catch (ResponseStatusException e) {
        LOGGER.warn("Dataset not found or invalid for ID: {} - {}", datasetId, e.getReason());
    } catch (Exception e) {
        LOGGER.error("Unexpected error while fetching dataset ID: {}", datasetId, e);
    }
    return null;
}

    private DSNegotiationInfo negotiateDataset(String datasetId){

    String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";

    try{

    //  Start negotiation
    URI startUri = new URI(baseUrl +
            V1_NEGOTIATION_PROVIDER_REQUEST + "?datasetid=" + datasetId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(HEADER_X_REQUEST_ID, dsParticipantId);

    ResponseEntity<String> startResponse =
            restTemplate.postForEntity(startUri, new HttpEntity<>(headers), String.class);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode startNode = mapper.readTree(startResponse.getBody());

    String negotiationId = startNode.get("@id").asText();

    LOGGER.info("negotiationId: {}", negotiationId);

    JsonNode stateNode;
    String state;

    do {
        if (!sleepSafely(3000)) {
            throw new IllegalStateException("Thread interrupted during negotiation polling");
        }

        URI statusUri = new URI(baseUrl + V1_CONTRACT + negotiationId);

        ResponseEntity<String> statusResponse =
                restTemplate.getForEntity(statusUri, String.class);

        stateNode = mapper.readTree(statusResponse.getBody());
        state = stateNode.get("state").asText();

    } while(!FINALIZED.equals(state) && !TERMINATED.equals(state));



    DSNegotiationInfo info = new DSNegotiationInfo();
    info.setId(negotiationId);
    info.setProviderPid(stateNode.get("providerPid").asText());
    info.setConsumerPid(stateNode.get("consumerPid").asText());
    info.setCallbackAddress(stateNode.get("callbackAddress").asText());
    info.setState(state);

    return info;
        } catch (URISyntaxException e) {
        throw new IllegalStateException("Invalid URI for dataset negotiation", e);

    } catch (Exception e) {
        throw new IllegalStateException("Dataset negotiation failed for id: " + datasetId, e);
    }
}

    public ResponseEntity<String> consumeCapabilities(String token) throws URISyntaxException {
     String baseUrl = dsConnectorDataPlaneUrl;

    String sanitizedToken = token.replace("\"", "");

    URI baseUri = new URI(baseUrl + V1_CONSUMER + "client/"+ sanitizedToken);

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


    public ParticipantDTO fetchCatalog(CatalogDTO catalogDTO) throws URISyntaxException {
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
    headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
    headers.set("providerUrl", catalogDTO.getProviderUrl());

    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

   
    ResponseEntity<ParticipantDTO> response = restTemplate.exchange(
            finalUri,
            HttpMethod.POST,
            requestEntity,
            ParticipantDTO.class
    );

    return response.getBody();
}

 public DSTransferProcess requestDatasetTransfer(String datasetId) throws URISyntaxException {
        String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
        URI baseUri = new URI(baseUrl + V1_REQUEST_TRANSFER_DATASET + "?datasetid=" + datasetId);


    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
    headers.set(HEADER_X_REQUEST_ID, dsParticipantId);

    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

    ResponseEntity<DSTransferProcess> response= restTemplate.exchange(
            baseUri,
            HttpMethod.POST,
            requestEntity,
            DSTransferProcess.class
    );
    return response.getBody();

}


public String checkDatasetAgreement(String datasetId) {
    try {
        String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
        URI baseUri = new URI(baseUrl + V1_DATASETS_REQUESTED + datasetId + "/agreements");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUri, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<Map<String, Object>>() {});
               
        Map<String, Object> resp = response.getBody();

        if (resp == null) {
            return null;
        } 
        if (response.getStatusCode().is2xxSuccessful()) {
            Object dataObj = resp.get("data");
            
            if (dataObj instanceof List<?> dataList && !dataList.isEmpty()) {
                return dataList.get(0).toString();
            }
        }

        return null;

    } catch (URISyntaxException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URI: " + e.getMessage(), e);
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to get agreement: " + e.getMessage(), e);
    }
}

public String searchParticipantCatalogue(String participantId) {
    try {
        String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
        
        URI baseUri = new URI(baseUrl + V1_PARTICIPANTS + participantId + "/catalog");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUri, 
            HttpMethod.GET, 
            requestEntity, 
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        LOGGER.warn("Participant not found for ID: {}", participantId);
        return null;

    } catch (URISyntaxException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URI: " + e.getMessage(), e);
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to get participant data: " + e.getMessage(), e);
    }
}
public DatasetListDTO retrieveCatalogDatasets(CatalogDTO catalogDTO, String catalogId) {
    try {
       
        URI finalUri = UriComponentsBuilder.fromUriString(dsConnectorUrl)
                .path(V1_CATALOG)          
                .pathSegment(catalogId)
                .pathSegment("datasets") 
                .queryParam("scope", "parents")
                .queryParam("page", catalogDTO.getPage())
                .queryParam("size", catalogDTO.getSize())
                .queryParam("description", catalogDTO.getDescription())
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<DatasetListDTO> response = restTemplate.exchange(
                finalUri,
                HttpMethod.GET,
                requestEntity,
                DatasetListDTO.class
        );

        if (response.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No datasets found in catalog");
        }

        return response.getBody();

    } catch (HttpClientErrorException.NotFound e) {
     
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog not found", e);
    } catch (HttpClientErrorException.BadRequest e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request parameters", e);
    } catch (Exception e) {

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "External service error", e);
    }
}

public DatasetEntry getDatasetMetadata(String datasetId) {
    String sanitizedId = (datasetId != null) ? datasetId.replace("\"", "").trim() : "";

    try {

        URI finalUri = UriComponentsBuilder.fromUriString(dsConnectorUrl)
                .path(V1_DATASETS_REQUESTED)
                .pathSegment(sanitizedId)
                .queryParam("full", "true")   
                .encode()               
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<DatasetEntry> response = restTemplate.exchange(
                finalUri, 
                HttpMethod.GET, 
                requestEntity, 
                DatasetEntry.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        LOGGER.warn("Dataset metadata body was empty for ID: {}", sanitizedId);
        return null;

    } catch (HttpClientErrorException.NotFound e) {

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, DATASET_NOT_FOUND, e);
        
    } catch (Exception e) {

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to retrieve dataset metadata from connector", e);
    }
}



public String getContractAgreement(String contractId) {
    String sanitizedId = (contractId != null) ? contractId.replace("\"", "").trim() : "";

    try {
  
    // Result: https://dsc.t4m.atc.gr/api/management/v1/contract/{sanitizedId}/agreement
    URI finalUri = UriComponentsBuilder.fromUriString(dsConnectorUrl)
            .path(V1_CONTRACT)        
            .pathSegment(sanitizedId) 
            .pathSegment("agreement")
            .encode()
            .build()
            .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                finalUri, 
                HttpMethod.GET, 
                requestEntity, 
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        LOGGER.warn("Contract Agreement metadata body was empty for ID: {}", sanitizedId);
        return null;

    } catch (HttpClientErrorException.NotFound e) {
    
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, DATASET_NOT_FOUND, e);
        
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to retrieve Contract Agreement metadata from connector", e);
    }
}

public DSNegotiationInfo getContractMetadata(String contractId) {
    String sanitizedId = (contractId != null) ? contractId.replace("\"", "").trim() : "";

    try {
  
    
    URI finalUri = UriComponentsBuilder.fromUriString(dsConnectorUrl)
            .path(V1_CONTRACT)        
            .pathSegment(sanitizedId) 
            .encode()
            .build()
            .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<DSNegotiationInfo> response = restTemplate.exchange(
                finalUri, 
                HttpMethod.GET, 
                requestEntity, 
                DSNegotiationInfo.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        LOGGER.warn("Contract Agreement metadata body was empty for ID: {}", sanitizedId);
        return null;

    } catch (HttpClientErrorException.NotFound e) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, DATASET_NOT_FOUND, e);
        
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to retrieve Contract Agreement metadata from connector", e);
    }
}

public String getTransferState(String transferId) throws URISyntaxException, JsonProcessingException {
    String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";

    URI statusUri = new URI(baseUrl + V1_TRANSFER + transferId);

    ResponseEntity<String> response = restTemplate.getForEntity(statusUri, String.class);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(response.getBody());
    JsonNode stateNode = root.get("state");
    if (stateNode == null) {
       return "incorrect-state";
    }
   return stateNode.asText();
}

private boolean sleepSafely(long millis) {
    try {
        Thread.sleep(millis);
        return true;
    } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        LOGGER.error("Thread interrupted while waiting for transfer state", ie);
        return false;
    }
}


public List<String> getDatasetChildrenIds(String parentId) {
    List<String> childIds = new ArrayList<>();
    try {
        // Targets: v1/datasets/requested/{parentId}/child?page=1&size=100
        URI finalUri = UriComponentsBuilder.fromUriString(dsConnectorUrl)
                .path(V1_DATASETS_REQUESTED)
                .pathSegment(parentId.replace("\"", "").trim())
                .pathSegment("child")
                .queryParam("page", 1)
                .queryParam("size", 100)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                finalUri, 
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> respBody = response.getBody();
        if (respBody != null && respBody.get("data") instanceof List<?> dataList) {
            for (Object obj : dataList) {
                if (obj != null) {
                    childIds.add(obj.toString());
                }
            }
        }
    } catch (Exception e) {
        LOGGER.debug("No children found for dataset ID: {}", parentId);
    }
    return childIds;
}



/**
 * A helper method that encapsulates the full  data space pipeline:
 * checks agreements, negotiates if necessary, requests transfer, polls for state,
 * and grabs the final payload from the data plane proxy.
 */

private String fetchChildPayload(DatasetEntry childEntry) {
    return fetchDatasetPayload(childEntry.getId());
}
/**
     * Fetches  production calendar text payload directly from the Data Space data plane.
     * @param calendarDatasetID The static ID of the target calendar dataset
     * @return Raw JSON string containing the real-time scheduling data
     */
   public String fetchCalendarContent(String calendarDatasetId) {
    String payload = fetchDatasetPayload(calendarDatasetId);

    if (payload == null) {
        throw new IllegalStateException(
            "Failed to retrieve calendar payload from the Data Space.");
    }

    return payload;
}


private String fetchDatasetPayload(String datasetId) {
    try {
        LOGGER.info("Fetching payload for dataset {}", datasetId);

        String agreementId = checkDatasetAgreement(datasetId);
																							  
        if (agreementId == null) {
            DSNegotiationInfo negotiation = negotiateDataset(datasetId);

            if (negotiation == null || !FINALIZED.equals(negotiation.getState())) {
			   LOGGER.error("Contract negotiation failed or timed out for  dataset: {}", datasetId);
																							   
                return null;
            }
        }
												 
        DSTransferProcess transfer = requestDatasetTransfer(datasetId);
        if (transfer == null) {
            return null;
        }

        String state = "";				   
        int attempts = 0;

        while (!STARTED.equals(state) && attempts < 10) {
            if (!sleepSafely(2000)) {
                return null;
            }

            state = getTransferState(transfer.getId());
            attempts++;
        }

																					
        if (!STARTED.equals(state)) {
            LOGGER.warn("Transfer for dataset {} never reached STARTED. Current state: {}", datasetId, state);
            return null;
        }

        ResponseEntity<String> response = consumeCapabilities(datasetId);
        return response != null ? response.getBody() : null;

    } catch (Exception e) {
        LOGGER.error("Failed to retrieve payload for dataset {}", datasetId, e);
        return null;
    }
				
}
}
