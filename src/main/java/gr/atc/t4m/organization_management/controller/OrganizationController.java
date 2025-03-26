package gr.atc.t4m.organization_management.controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/organization")

public class OrganizationController {

    private final OrganizationService organizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Operation(summary = "Health Check", description = "Returns a health check message for the Organization Management service")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Welcome message for Organization Management."),
    })
    @GetMapping("/health")
    public String health() {
        return "Welcome to organization Management for T4M!";
    }

    /**
     * Creation of a new organization
     *
     * @return message of success or failure
     * @throws OrganizationAlreadyExistsException
     */
    @Operation(summary = "Create a new Organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - Organization name is required"),
            @ApiResponse(responseCode = "409", description = "Conflict - Organization already exists with the same name"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "createOrganization", consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Organization> createOrganization(@RequestBody OrganizationDTO organizationDTO)
            throws OrganizationAlreadyExistsException {

        if (organizationDTO.getOrganizationName() == null || organizationDTO.getOrganizationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required");
        }

        Organization organization = new Organization();
        BeanUtils.copyProperties(organizationDTO, organization);

        Organization savedOrganization = organizationService.createOrganization(organization);

        return ResponseEntity.ok(savedOrganization);
    }

    /**
     * Update an existing organization
    *  @param id
     * @return organization information
     * @throws OrganizationAlreadyExistsException
     */
    @Operation(summary = "Update an existing Organization")
    @PutMapping(value = "/updateOrganization/{id}")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable String id,
            @RequestBody @Valid OrganizationDTO organizationDTO) {

        Organization updatedOrganization = organizationService.updateOrganization(id, organizationDTO);
        return ResponseEntity.ok(updatedOrganization);
    }

    /**
     * Get organization information
     *
     * @return message of success or failure
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Get Organization Information", description = "Returns the information of the organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization information."),
            @ApiResponse(responseCode = "404", description = "Organization not found."),

    })

    @GetMapping("/getOrganization/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable String id) throws OrganizationNotFoundException {
        Organization organization = organizationService.getOrganization(id);
        return ResponseEntity.ok(organization);
    }

    /**
     * Get organizations information
     * 
     * @param page
     * @param size
     * @param sortBy
     * @param sortDir
     * @return message of success or failure
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Get Organizations Information", description = "Returns the information of the organizations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization information.")
    })
    @GetMapping("/getOrganization/all")
    public ResponseEntity<Page<Organization>> getAllOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "organizationName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            final HttpServletRequest request) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Organization> organizations = organizationService.getAllOrganizations(pageable);

        return ResponseEntity.ok(organizations);
    }

    /**
     * Delete organization by providing the id
     * 
     * @param id
     * @return message of successful deletion
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Delete Organization", description = "Delete organization by providing the id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful deletion."),
            @ApiResponse(responseCode = "404", description = "Organization not found. Nothing to delete."),

    })
    @DeleteMapping(value = "/deleteOrganization/{id}", produces = "application/json;charset=UTF-8")
    public String deleteOrganization(@PathVariable String id, final HttpServletRequest request) {
        organizationService.deleteOrganizationById(id);

        return "Organization with id: " + id + " deleted successfully";
    }

}
