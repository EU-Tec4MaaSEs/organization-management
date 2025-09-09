package gr.atc.t4m.organization_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.service.DsConnectorService;
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

    @Autowired
    public DsConnectorController(DsConnectorService dsConnectorService) {
        this.dsConnectorService = dsConnectorService;

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

}
