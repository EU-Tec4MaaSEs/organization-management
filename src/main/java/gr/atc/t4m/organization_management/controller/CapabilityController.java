package gr.atc.t4m.organization_management.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.service.ManufacturingResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/capability")
@Tag(name = "Capability Controller", description = "Handles the API requests for Capability Management")
public class CapabilityController {
    private final ManufacturingResourceService manufacturingResourceService;

    public CapabilityController(ManufacturingResourceService manufacturingResourceService) {
        this.manufacturingResourceService = manufacturingResourceService;
    }

    @Operation(summary = "Get All capabilities", description = "Retrieve all capabilities", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of capabilities."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "No capabilities found."),

    })
    @GetMapping("/all")
    public ResponseEntity<List<CapabilityEntry>> getAllCapabilities() throws OrganizationNotFoundException {

        List<CapabilityEntry> capabilities = manufacturingResourceService.getAllCapabilities();

        if (capabilities == null || capabilities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No stored capabilities found");
        }

        return ResponseEntity.ok(capabilities);
    }

    @Operation(summary = "Get All Manufacturing Resources", description = "Retrieve all manufacturing resources", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of manufacturing resources."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "No manufacturing resources found."),

    })
    @GetMapping("/allManufacturingResources")
    public ResponseEntity<List<ManufacturingResource>> getAllManufacturingResources() throws OrganizationNotFoundException {

        List<ManufacturingResource> resources = manufacturingResourceService.getAllManufacturingResources();

        if (resources == null || resources.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No stored manufacturing resources found");
        }

        return ResponseEntity.ok(resources);
    }

}

