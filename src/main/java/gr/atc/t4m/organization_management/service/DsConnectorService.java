package gr.atc.t4m.organization_management.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

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

@Service
public class DsConnectorService {

    @Value("${ds.connector.url}")
    private String dsConnectorUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(DsConnectorService.class);

    /**
     * Validates the organization by sending a request to the ds connector.
     *
     * @param catalogDTO The DTO containing the organization details.
     */

    public HttpStatus validateOrganization(CatalogDTO catalogDTO) {
        LOGGER.info("Validating organization URL: {}", catalogDTO.getProviderUrl());

        try {
            String baseUrl = dsConnectorUrl.endsWith("/") ? dsConnectorUrl : dsConnectorUrl + "/";
            URI baseUri = new URI(baseUrl + "v1/request/catalog");

            URI finalUri = UriComponentsBuilder.newInstance()
                    .uri(baseUri)
                    .queryParam("page", catalogDTO.getPage())
                    .queryParam("size", catalogDTO.getSize())
                    .queryParam("refresh", catalogDTO.getRefresh())
                    .build()
                    .toUri();

            RestTemplate restTemplate = new RestTemplate();

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



            LOGGER.info("Validation response status: {}", response.getStatusCode());
            LOGGER.debug("Response body: {}", response.getBody());

            return (HttpStatus) response.getStatusCode();

        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid base URI: " + e.getMessage(), e);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Validation failed: " + e.getMessage(), e);

        }
    }

}
