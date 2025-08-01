package gr.atc.t4m.organization_management.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.InformationMessage;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.service.MinioService;
import gr.atc.t4m.organization_management.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;


@RestController
@RequestMapping("/api/organization")
@Tag(name = "Organization manager Controller", description = "Handles the API requests for Organization Management")

public class OrganizationController {

    private final OrganizationService organizationService;
    private final MinioService minioService;


    @Autowired
    public OrganizationController(OrganizationService organizationService, 
             MinioService minioService) {
        this.organizationService = organizationService;
        this.minioService = minioService;
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
     @Operation(summary = "Create a new Organization", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input value"),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "409", description = "Conflict - Organization already exists with the same name"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
   @PostMapping(value = "create", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
public ResponseEntity<Organization> createOrganization(
        @RequestBody(required = false) OrganizationDTO organizationDTOJson,  // For JSON Input
        @RequestPart(value = "organization", required = false) OrganizationDTO organizationDTOPart,  // For multipart if logo is present
        @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
        final HttpServletRequest request)
        throws OrganizationAlreadyExistsException {
        
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String userId = jwtToken.getToken().getClaim("sub"); // or any custom claim

    OrganizationDTO organizationDTO = (organizationDTOJson != null) ? organizationDTOJson : organizationDTOPart;
    if (organizationDTO == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization data is required");
    }
    // Handle validations
    if (organizationDTO.getOrganizationName() == null || organizationDTO.getOrganizationName().trim().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required");
    }

    // Upload file (if present)
    String logoUrl = null;
    
    if (logoFile != null && !logoFile.isEmpty()) {
        logoUrl = minioService.uploadLogo(logoFile); // Store and return URL
    }

    // Copy data
    Organization organization = new Organization();
    BeanUtils.copyProperties(organizationDTO, organization);
    organization.setLogoUrl(logoUrl); // Save logo location

    Organization savedOrganization = organizationService.createOrganization(organization);
    
    // Trigger Kafka event for organization registration
    organizationService.createKafkaMessage(organization, userId);
    return ResponseEntity.ok(savedOrganization);
}


    /**
     * Update an existing organization
    *  @param id
     * @return organization information
     * @throws OrganizationNotExists
     * @throws IllegalArgumentException
     */
    @Operation(summary = "Update an existing Organization", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization updated."),
        @ApiResponse(responseCode = "400", description = "Invalid input value."),
        @ApiResponse(responseCode = "401", description = "Authentication process failed!")

})
    @PutMapping(value = "/update/{id}")
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

    @Operation(summary = "Get Organization Information", description = "Returns the information of the organization", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization information."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "Organization not found."),

    })

    @GetMapping("/getOrganization/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable String id) throws OrganizationNotFoundException {
        Organization organization = organizationService.getOrganization(id);
        return ResponseEntity.ok(organization);
    }

     /**
     * Get organization information by name
     *
     * @return message of success or failure
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Get Organization Information by name", description = "Returns the information of the organization", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization information."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "Organization not found."),

    })

    @GetMapping("/getOrganizationByName/{name}")
    public ResponseEntity<Organization> getOrganizationByName(@PathVariable String name) throws OrganizationNotFoundException {
        Organization organization = organizationService.getOrganizationByName(name);
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

    @Operation(summary = "Get Organizations Information", description = "Returns the information of the organizations", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization information."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!")
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
     * Get All providers
     * 
     * @return message of success or failure
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Get All providers Information", description = "Returns a list of all providers", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Information for all providers."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })
    @GetMapping("/getAllProviders")
    public ResponseEntity<List<Organization>> getAllProviders(
            final HttpServletRequest request) {

        List<Organization> providers = organizationService.getAllProviders();

        return ResponseEntity.ok(providers);
    }

    /**
     * Search for providers by location,manufacturing service
     * 
     * @return message of success or failure
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Search for providers by location,manufacturing service", description = "Returns a list of all providers for specified criteria", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Information for providers for specified criteria."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })
    @PostMapping("/searchProviders")
    public ResponseEntity<List<Organization>> filterProviders(@RequestBody ProviderSearchDTO filter) {
        List<Organization> providers = organizationService.searchProviders(filter);
        return ResponseEntity.ok(providers);
    }
    /**
     * Delete organization by providing the id
     * 
     * @param id
     * @return message of successful deletion
     * @throws OrganizationNotFoundException
     */

    @Operation(summary = "Delete Organization", description = "Delete organization by providing the id", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful deletion."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "Organization not found. Nothing to delete."),

    })
    @DeleteMapping(value = "/deleteOrganization/{id}", produces = "application/json;charset=UTF-8")
    public ResponseEntity<InformationMessage> deleteOrganization(@PathVariable String id, final HttpServletRequest request) {
        organizationService.deleteOrganizationById(id);
        InformationMessage informationMessage = new InformationMessage();
        informationMessage.setMessage("Organization deleted successfully.");
        return ResponseEntity.ok(informationMessage);
    }

}
