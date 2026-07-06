
package gr.atc.t4m.organization_management.controller;

import gr.atc.t4m.organization_management.dto.ManufacturingDomainRequest;
import gr.atc.t4m.organization_management.model.ManufacturingDomain;
import gr.atc.t4m.organization_management.service.ManufacturingDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/manufacturing-domains")
@RequiredArgsConstructor
@Tag(name = "Manufacturing Domains Administration", description = "Endpoints for Super-Admins to manage the manufacturing domains")
public class ManufacturingDomainController {

    private final ManufacturingDomainService domainService;

     /**
     * GET /api/manufacturing-domains?page=0&size=10
     */

@GetMapping
    @Operation(summary = "Get all manufacturing domains", description = "Retrieves a paginated list of all manufacturing domains.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the paginated list of domains."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Access denied! Insufficient administrative permissions.")
    })
    public ResponseEntity<Page<ManufacturingDomain>> getAllDomains(
            @ParameterObject
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        
        return ResponseEntity.ok(domainService.getAllDomains(pageable));
    }
    /**
     * GET /api/manufacturing-domains/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a manufacturing domain by ID", description = "Retrieves the  details of a specific manufacturing domain.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the domain details."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "Target manufacturing domain not found.")
    })
    public ResponseEntity<ManufacturingDomain> getDomainById(@PathVariable String id) {
        return domainService.getDomainById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manufacturing domain with ID '" + id + "' not found."));
    }


    @GetMapping("/abbreviations")
    @Operation(summary = "Get all distinct abbreviations", description = "Retrieves a list of all domain abbreviations currently registered in the platform.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<String>> getDistinctAbbreviations() {
        return ResponseEntity.ok(domainService.getAllDistinctAbbreviations());
    }
    /**
     * POST /api/manufacturing-domains
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new manufacturing domain", description = "Defines and registers a brand new manufacturing domain into the T4M platform.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manufacturing domain successfully created."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Access denied! Insufficient administrative permissions.")
    })
    public ResponseEntity<ManufacturingDomain> createDomain(@Valid @RequestBody ManufacturingDomainRequest request){
        ManufacturingDomain savedDomain = domainService.createDomain(request);
        return new ResponseEntity<>(savedDomain, HttpStatus.CREATED);
    }

    /**
     * PUT /api/manufacturing-domains/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update an existing manufacturing domain", description = "Modifies the  details of a service or a domain.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Manufacturing domain details updated successfully."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Access denied! Insufficient administrative permissions."),
            @ApiResponse(responseCode = "404", description = "Target manufacturing domain not found.")
    })
    public ResponseEntity<ManufacturingDomain> updateDomain(
            @PathVariable String id, 
            @Valid @RequestBody ManufacturingDomainRequest request) {
        
        return domainService.updateDomain(id, request)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target manufacturing domain not found."));
    }

    /**
     * DELETE /api/manufacturing-domains/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a manufacturing domain", description = "Permanently removes the target manufacturing domain after ensuring it is not referenced by active organizations.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Manufacturing domain safely deleted."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "403", description = "Access denied! Insufficient administrative permissions."),
            @ApiResponse(responseCode = "404", description = "Target manufacturing domain not found.")
    })
    public ResponseEntity<Void> deleteDomain(@PathVariable String id) {
        boolean isDeleted = domainService.deleteDomain(id);
        if (!isDeleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target manufacturing domain not found.");
        }
        return ResponseEntity.noContent().build();
    }
}