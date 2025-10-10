package gr.atc.t4m.organization_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.service.DsConnectorService;
import gr.atc.t4m.organization_management.service.ManufacturingResourceService;
import gr.atc.t4m.organization_management.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/connector")
@Tag(name = "DS connector Controller", description = "Handles the API requests for DS Connector")

public class DsConnectorController {

    private final DsConnectorService dsConnectorService;
    private final OrganizationService organizationService;
    private final ManufacturingResourceService manufacturingResourceService;

    @Autowired
    public DsConnectorController(DsConnectorService dsConnectorService, OrganizationService organizationService, ManufacturingResourceService manufacturingResourceService) {
        this.dsConnectorService = dsConnectorService;
        this.organizationService = organizationService;
        this.manufacturingResourceService = manufacturingResourceService;

    }

    /**
     * Validate an organization through DS Connector
     *
     * @return message of success or failure
     */
    @Operation(summary = "Validate an organization through DS Connector", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization is Valid"),
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "500", description = "Internal server error")

    })
    @PostMapping(value = "validateOrganization")
    public ResponseEntity<Void> validateOrganization(
            @Valid @RequestBody CatalogDTO catalogDTO,
            final HttpServletRequest request) {

        HttpStatus status = dsConnectorService.validateOrganization(catalogDTO);
        return ResponseEntity.status(status).build();
    }


    @Operation(summary = "Retrieve organization capabilities through DS Connector for a specific organization and store it to mongo", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Capabilities retrieved and Stored successfully"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Missing required fields in the request body"),
            @ApiResponse(responseCode = "500", description = "Internal server error")

    })
    @PostMapping(value = "retrieveCapabilities")
    public ManufacturingResource  retrieveCapabilities(
            @Valid @RequestBody CatalogDTO catalogDTO,
            final HttpServletRequest request) {
                if (catalogDTO.getOrganizationName() == null || catalogDTO.getProviderUrl() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Organization Name and Provider URL must be provided"
                    );
                }
                Organization organization = organizationService.getOrganizationByName(catalogDTO.getOrganizationName());
                if (!catalogDTO.getProviderUrl().equals(organization.getDsConnectorURL())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "The provided URL differs from the organization's DS url"
                    );
                }
          
                ManufacturingResource manufacturingResource = dsConnectorService.retrieveCapabilities(catalogDTO);
                manufacturingResourceService.save(manufacturingResource);
               
                organizationService.addManufacturingResource(organization, manufacturingResource);

        return manufacturingResource;
    }



}
