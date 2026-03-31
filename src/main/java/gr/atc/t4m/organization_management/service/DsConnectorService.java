package gr.atc.t4m.organization_management.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import gr.atc.t4m.organization_management.model.DatasetListDTO;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.ParticipantDTO;

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

   
    public List<ManufacturingResource> retrieveCapabilities(CatalogDTO catalogDTO) {
        LOGGER.info("Validating organization URL: {}", catalogDTO.getProviderUrl());

        try {
            // 1. Fetch Catalog & Participant Info
            ParticipantDTO response = fetchCatalog(catalogDTO);
            String participantCatalogueId = searchParticipantCatalogue(response.getId());
            String cleanIParticipantId = participantCatalogueId.replace("\"", "");

            // 2. Retrieve Dataset IDs

            DatasetListDTO datasetLists = retrieveCatalogDatasets(catalogDTO, cleanIParticipantId);
            List<DatasetEntry> allDatasetEntries = new ArrayList<>();
            if (datasetLists == null || datasetLists.getData() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No datasets found in provider catalog");
            }
            for (String datasetId : datasetLists.getData()) {
                DatasetEntry entry = fetchDatasetMetadata(datasetId);
                if (entry != null)
                    allDatasetEntries.add(entry);
            }

            if (allDatasetEntries.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No datasets found in provider catalog");
            }

            // 3. Separate datasets into those with agreements and those without
            List<DatasetEntry> datasetWithAggrements = new ArrayList<>();
            List<DatasetEntry> datasetWithNoAgreements = new ArrayList<>();

            for (DatasetEntry entry : allDatasetEntries) {
                processAgreementCheck(entry, datasetWithAggrements, datasetWithNoAgreements);
            }

            // 4. Trigger Negotiations for those without agreements

            for (DatasetEntry entry : datasetWithNoAgreements) {
                handleNegotiation(entry, datasetWithAggrements);
            }

            // 5. PROCESS ALL VALID DATASETS INTO THE FINAL LIST
            List<ManufacturingResource> allResources = new ArrayList<>();

            for (DatasetEntry entry : datasetWithAggrements) {
                ManufacturingResource resource = processDatasetTransfer(entry, catalogDTO);

                if (resource != null) {
                    allResources.add(resource);
                }
            }

            if (allResources.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No capability data could be retrieved");
            }

            return allResources;

        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base URI: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Validation failed: " + e.getMessage(),
                    e);
        }
    }


private void processAgreementCheck(DatasetEntry entry,
                                   List<DatasetEntry> datasetWithAggrements,
                                   List<DatasetEntry> datasetWithNoAgreements) {
    try {
        String agreementId = checkDatasetAggrement(entry.getId());

        if (agreementId != null) {
            datasetWithAggrements.add(entry);
        } else {
            datasetWithNoAgreements.add(entry);
        }

    } catch (Exception e) {
        LOGGER.error("Agreement check failed for ID: {}", entry.getId(), e);
    }
}


    private ManufacturingResource processDatasetTransfer(DatasetEntry entry,  CatalogDTO catalogDTO ) {
          try {
                LOGGER.info("Processing Dataset ID: {}", entry.getId());
                
                // A. Request Data Transfer
                DSTransferProcess transferResponse = requestDatasetTransfer(entry.getId());
                
                if (transferResponse != null) {
                    //  Wait for Transfer to be STARTED (Step 7 of Spec)
                    String tState = "";
                    int attempts = 0;
                   while (!STARTED.equals(tState) && attempts < 10) {
                     if (!sleepSafely(2000)) {
                         break;
                     }

                   tState = getTransferState(transferResponse.getId());
                   attempts++;
                }

                    if (STARTED.equals(tState)) {
                        // B. Consume the AAS Submodel JSON
                        ResponseEntity<String> capabilitiesResult = consumeCapabilities(entry.getId());
                        
                        if (capabilitiesResult != null && capabilitiesResult.getBody() != null) {
                            // C. Parse the AAS JSON
                            List<CapabilityEntry> capabilities = capabilityService.parseAASCapabilities(capabilitiesResult.getBody());
                            
                            // D. Create the Resource Document
                            ManufacturingResource resource = new ManufacturingResource();
                            resource.setCapabilityDatasetID(entry.getId());
                            resource.setManufacturingResourceTitle(entry.getTitle());
                            resource.setCapabilities(capabilities);
                            resource.setProviderUrl(catalogDTO.getProviderUrl());
                            
                            return resource;
                        }
                    } else {
                        LOGGER.warn("Transfer for {} timed out or terminated in state: {}", entry.getId(), tState);
                    }
                }

            } catch (Exception e) {
                LOGGER.error("Data transfer/parsing failed for Dataset ID: {}. Error: {}", entry.getId(), e.getMessage());
            }
            return null;
    
}


   private void handleNegotiation(DatasetEntry entry,
                              List<DatasetEntry> datasetWithAggrements) {
    try {
        DSNegotiationInfo info = negotiateDataset(entry.getId());

        if (info != null && FINALIZED.equals(info.getState())) {
            datasetWithAggrements.add(entry);
        }

    } catch (Exception e) {
        LOGGER.error("Failed negotiation for dataset ID: {}", entry.getId(), e);
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


public String checkDatasetAggrement(String datasetId) {
    try {
        String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
        URI baseUri = new URI(baseUrl + V1_DATASETS_REQUESTED + datasetId + "/agreements");

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HEADER_X_REQUEST_ID, dsParticipantId);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(baseUri, HttpMethod.GET, requestEntity, Map.class);
               
        Map resp = response.getBody();

        if (resp == null){
            return null;
        } 
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
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
                .queryParam("scope", "all")
                .queryParam("page", catalogDTO.getPage())
                .queryParam("size", catalogDTO.getSize())
                .queryParam("title", catalogDTO.getTitle())
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


}
