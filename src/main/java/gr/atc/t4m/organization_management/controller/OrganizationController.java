package gr.atc.t4m.organization_management.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import gr.atc.t4m.organization_management.dto.*;
import gr.atc.t4m.organization_management.model.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.service.ManualSearchHistoryService;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/api/organization")
@Tag(name = "Organization manager Controller", description = "Handles the API requests for Organization Management")

public class OrganizationController {

    @Value("${organization.default.valueNetwork}")
    private String defaultValueNetwork;
    private final OrganizationService organizationService;
    private final ManualSearchHistoryService searchHistoryService;
    private final MinioService minioService;
    private static final String ORGANIZATION_ID = "organization_id";
    

    public OrganizationController(OrganizationService organizationService,
                                  MinioService minioService,
                                  ManualSearchHistoryService searchHistoryService) {
        this.organizationService = organizationService;
        this.minioService = minioService;
        this.searchHistoryService = searchHistoryService;
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
    @PostMapping(value = "create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Organization> createOrganization(
            @RequestPart("organization") @Valid OrganizationDTO organizationDTO,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "attachmentFiles", required = false) List<MultipartFile> attachmentFiles,
            @RequestParam(value = "attachmentTitles", required = false) List<String> attachmentTitles,
            @RequestParam(value = "attachmentIsPublic", required = false) List<Boolean> attachmentIsPublic,
            final HttpServletRequest request)
            throws OrganizationAlreadyExistsException {

        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String userId = jwtToken.getToken().getClaim("sub"); // or any custom claim

        // Handle validations
        if (organizationDTO.getOrganizationName() == null || organizationDTO.getOrganizationName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization name is required");
        }
        if (organizationDTO.getValueNetwork() == null || organizationDTO.getValueNetwork().trim().isEmpty()) {
            organizationDTO.setValueNetwork(defaultValueNetwork); //SET DEFAULT VALUE NETWORK
        }
        // Upload file (if present)
        String logoUrl = null;

        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = minioService.uploadFile(logoFile); // Store and return URL
        }

        List<FileInformation> processedAttachments = new ArrayList<>();
        if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
                for (int i = 0; i < attachmentFiles.size(); i++) {
                        MultipartFile file = attachmentFiles.get(i);

                        if (!file.isEmpty()) {
                                String fileUrl = minioService.uploadFile(file);

                                // Fetch title by index or fallback to original filename
                                String fileTitle;
                                if (attachmentTitles != null && i < attachmentTitles.size()
                                                && !attachmentTitles.get(i).isBlank()) {
                                        fileTitle = attachmentTitles.get(i).trim();
                                } else if (file.getOriginalFilename() != null
                                                && !file.getOriginalFilename().isBlank()) {
                                        fileTitle = file.getOriginalFilename();
                                } else {
                                        fileTitle = "attachment-" + (i + 1);
                                }
                                  // Extract isPublic flag by index (defaults to false / private if omitted)
                                boolean isPublic = false;
                               if (attachmentIsPublic != null && i < attachmentIsPublic.size() 
                                  && attachmentIsPublic.get(i) != null) {
                                  isPublic = attachmentIsPublic.get(i);
                                }
                               processedAttachments.add(new FileInformation(fileTitle, fileUrl,isPublic));
                        }
                }
        }

        // Copy data
        Organization organization = new Organization();
        BeanUtils.copyProperties(organizationDTO, organization);
        organization.setLogoUrl(logoUrl); // Save logo location
        organization.setAttachments(processedAttachments);
        Organization savedOrganization = organizationService.createOrganization(organization);

        // Trigger Kafka event for organization registration
        organizationService.createKafkaMessage(organization, userId, EventType.CREATE, organizationDTO.getVerifiableCredential());
        return ResponseEntity.ok(savedOrganization);
    }


    /**
     * Update an existing organization
     *
     * @param id
     * @return organization information
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

        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String userId = jwtToken.getToken().getClaim("sub"); // or any custom claim
        String userOrgId = jwtToken.getToken().getClaim(ORGANIZATION_ID);

        if (organizationDTO.getValueNetwork() == null || organizationDTO.getValueNetwork().trim().isEmpty()) {
            organizationDTO.setValueNetwork(defaultValueNetwork); //SET DEFAULT VALUE NETWORK
        }
        Organization updatedOrganization = organizationService.updateOrganization(id, organizationDTO);
        // Trigger Kafka event for organization update
        organizationService.createKafkaMessage(updatedOrganization, userId, EventType.UPDATE, organizationDTO.getVerifiableCredential());
        
        Organization responseOrg = maskPrivateAttachmentUrls(updatedOrganization, userOrgId);
        return ResponseEntity.ok(responseOrg);
    }

    /**
     * Issue a Verifiable Credential
     *
     * @return the String of the JWT
     */
@Operation(
        summary = "Issue a Verifiable Credential",
        description = "Forwards the request to the configured Identity Provider and returns the raw credential response",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credential issued successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — bad Identity Provider credentials"),
            @ApiResponse(responseCode = "500", description = "Identity Provider unreachable or internal error")

    })
    @PostMapping("/issue-verifiable-credential")
    public ResponseEntity<String> issueCredential(@RequestBody VerifiableCredentialInputDTO request) {
        String result = organizationService.issueVerifiableCredential(request);
        return ResponseEntity.ok(result);
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
    public ResponseEntity<Organization> getOrganization(
            @PathVariable String id,
            JwtAuthenticationToken jwtToken) throws OrganizationNotFoundException {

        String userOrgId = (jwtToken != null) ? jwtToken.getToken().getClaim(ORGANIZATION_ID) : null;
        Organization organization = organizationService.getOrganization(id);
        Organization responseOrg = maskPrivateAttachmentUrls(organization, userOrgId);

        return ResponseEntity.ok(responseOrg);
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
    public ResponseEntity<Organization> getOrganizationByName(@PathVariable String name,JwtAuthenticationToken jwtToken) throws OrganizationNotFoundException {
        String userOrgId = (jwtToken != null) ? jwtToken.getToken().getClaim(ORGANIZATION_ID) : null;
        Organization organization = organizationService.getOrganizationByName(name);
        Organization responseOrg = maskPrivateAttachmentUrls(organization, userOrgId);
        return ResponseEntity.ok(responseOrg);
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
            JwtAuthenticationToken jwtToken,
            final HttpServletRequest request) {

        String userOrgId = (jwtToken != null && jwtToken.getToken() != null)
                ? jwtToken.getToken().getClaimAsString(ORGANIZATION_ID): null;
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Organization> organizations = organizationService.getAllOrganizations(pageable);
        organizations.forEach(org -> maskPrivateAttachmentUrls(org, userOrgId));

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
            JwtAuthenticationToken jwtToken,
            final HttpServletRequest request) {
        String userOrgId = (jwtToken != null && jwtToken.getToken() != null)
                ? jwtToken.getToken().getClaimAsString(ORGANIZATION_ID): null;

        List<Organization> providers = organizationService.getAllProviders();
        providers.forEach(prov -> maskPrivateAttachmentUrls(prov, userOrgId));


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
    public ResponseEntity<List<Organization>> filterProviders(@RequestBody ProviderSearchDTO filter,
                    JwtAuthenticationToken jwtToken) {
            List<Organization> providers = organizationService.searchProviders(filter);
            String userId = jwtToken.getToken().getClaim("sub");
            String userOrgId = (jwtToken != null && jwtToken.getToken() != null)
                ? jwtToken.getToken().getClaimAsString(ORGANIZATION_ID): null;
            searchHistoryService.recordSearch(userId, filter.getCountryCodes(), filter.getManufacturingServices());
            providers.forEach(prov -> maskPrivateAttachmentUrls(prov, userOrgId));

            return ResponseEntity.ok(providers);
    }

    @Operation(summary = "Retrieve search history for providers", description = "Retrieves the paginated search history for the user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
                    @ApiResponse(responseCode = "200", description = "Successful retrieval of search history."),
                    @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })

    @GetMapping("/searchProvidersHistory")
    public ResponseEntity<Page<ManualSearchHistory>> getSearchHistory(
                    @PageableDefault(size = 10, sort = "searchedAt", direction = Sort.Direction.DESC) Pageable pageable,
                    JwtAuthenticationToken jwtToken) {

            if (jwtToken == null) {
               return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String userId = jwtToken.getToken().getClaim("sub");


            Page<ManualSearchHistory> historyPage = searchHistoryService.getUserSearchHistory(userId, pageable);
            return ResponseEntity.ok(historyPage);
    }


    @Operation(summary = "Delete all search history for the user", description = "Permanently removes all recent search records matching the user's ID", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Search history successfully cleared."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })
    @DeleteMapping("/deleteProvidersHistory")
    public ResponseEntity<Void> deleteSearchHistory(JwtAuthenticationToken jwtToken) {
        String userId = jwtToken.getToken().getClaim("sub");
        
        searchHistoryService.clearUserSearchHistory(userId);
        
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a specific search history record", description = "Permanently removes a single search log entry by its unique ID", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Search history item successfully deleted."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })
    @DeleteMapping("/deleteProvidersHistory/{id}")
    public ResponseEntity<Void> deleteSearchHistoryEntry(
            @PathVariable String id, 
            JwtAuthenticationToken jwtToken) {
        
        if (jwtToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtToken.getToken().getClaim("sub");
        
        searchHistoryService.deleteHistoryEntry(id, userId);
        
        return ResponseEntity.noContent().build();
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
        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String userId = jwtToken.getToken().getClaim("sub");

        Organization organizationToBeDeleted = organizationService.getOrganization(id);

        organizationService.deleteOrganizationById(id);


        InformationMessage informationMessage = new InformationMessage();
        informationMessage.setMessage("Organization deleted successfully.");
        // Trigger Kafka event for organization deletion
        organizationService.createKafkaMessage(organizationToBeDeleted, userId, EventType.DELETE, null);
        return ResponseEntity.ok(informationMessage);
    }

    @Operation(summary = "Retrieves the stored capabilities for an organization", description = "Returns a list of capabilities related to an organization", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of capabilities for the organization."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
            @ApiResponse(responseCode = "404", description = "Organization not found or no capabilities found."),
    })

    @GetMapping("/{orgName}/capabilities")
    public ResponseEntity<List<CapabilityEntry>> getOrganizationCapabilities(
            @PathVariable String orgName) throws OrganizationNotFoundException {

        Organization organization = organizationService.getOrganizationByName(orgName);

        if (organization.getManufacturingResources() == null || organization.getManufacturingResources().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No manufacturing resources found for organization: " + orgName);
        }

        List<CapabilityEntry> capabilities = organization.getManufacturingResources().stream()
                .filter(mr -> mr.getCapabilities() != null && !mr.getCapabilities().isEmpty())
                .flatMap(mr -> mr.getCapabilities().stream())
                .toList();

        if (capabilities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No capabilities found for organization: " + orgName);
        }

        return ResponseEntity.ok(capabilities);
    }

    @Operation(summary = "Retrieves the list of organizations by capability", description = "Returns a list of organizations related to a specific capability", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of organizations for the specified capability."),
            @ApiResponse(responseCode = "400", description = "Bad request."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!"),
    })
    @GetMapping(value = "/by-capability", produces = "application/json;charset=UTF-8")
    public ResponseEntity<List<OrganizationDTO>> getOrganizationsByCapabilities(
            @RequestParam Optional<String> primaryCapability,
            @RequestParam Optional<String> secondaryCapability) {

        if (primaryCapability.isEmpty() && secondaryCapability.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one of primaryCapability or secondaryCapability must be provided.");
        }

        List<OrganizationDTO> organizations =
                organizationService.getOrganizationsByCapabilities(
                        primaryCapability.orElse(null),
                        secondaryCapability.orElse(null));


        return organizations == null || organizations.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(organizations);
    }


    @Operation(summary = "Update organization's logo", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization logo updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input value"),
            @ApiResponse(responseCode = "401", description = "Authentication failed"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping(value = "/{organizationId}/update-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Organization> updateOrganizationLogo(
            @PathVariable String organizationId,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            JwtAuthenticationToken jwtToken) {
            String userOrgId = (jwtToken != null && jwtToken.getToken() != null)
                ? jwtToken.getToken().getClaimAsString(ORGANIZATION_ID): null;
        // Validate file
        if (logoFile == null || logoFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo file is required");
        }

        // Fetch existing organization
        Organization organization = organizationService.getOrganization(organizationId);
        if (organization == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
        }

        String logoUrl = minioService.uploadFile(logoFile);
        organization.setLogoUrl(logoUrl);

        Organization updated = organizationService.save(organization);

        return ResponseEntity.ok(maskPrivateAttachmentUrls(updated, userOrgId));
    }


    @Operation(
        summary = "Delete an attachment from an organization",
        description = "Deletes a specific attachment file from MinIO storage and removes its record from the organization.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Attachment deleted successfully",
            content = @Content(schema = @Schema(implementation = InformationMessage.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Missing or invalid JWT token"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Organization or Attachment ID not found"
        )
    })
     @DeleteMapping(value = "/{organizationId}/attachments/{fileId}", produces = "application/json;charset=UTF-8")
      public ResponseEntity<InformationMessage> deleteAttachment(
            @PathVariable String organizationId,
            @PathVariable String fileId,
            final HttpServletRequest request) {

        organizationService.deleteAttachmentById(organizationId, fileId);

        InformationMessage message = new InformationMessage();
        message.setMessage("Attachment deleted successfully.");
        return ResponseEntity.ok(message);
    }

    /**
     * Submit a new rating/review for a target organization
     */
    @Operation(summary = "Submit an organization review", description = "Allows an authenticated user to review a target organization",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review submitted successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input or data payload."),
            @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })
    @PostMapping("/{orgId}/reviews")
    public ResponseEntity<OrganizationReview> createReview(
            @PathVariable String orgId,
            @RequestBody @Valid CreateReviewDTO reviewDto) {

        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext()
                .getAuthentication();
        String userId = jwtToken.getToken().getClaim("sub");
        String reviewerOrgId = jwtToken.getToken().getClaim(ORGANIZATION_ID);

        if (orgId.equals(reviewerOrgId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An organization cannot review itself.");
        }

        OrganizationReview savedReview = organizationService.saveReview(orgId, userId, reviewerOrgId, reviewDto);
        ReviewAnalyticsDTO reviewAnalytics = organizationService.calculateRoleAnalytics(orgId,
                        reviewDto.getTargetRole());
        if (MaasRole.PROVIDER == reviewDto.getTargetRole()) {
                organizationService.updateOrganizationsProviderRating(orgId, reviewAnalytics.getAverageRating());
        } else if (MaasRole.CONSUMER == reviewDto.getTargetRole()) {
                organizationService.updateOrganizationsConsumerRating(orgId, reviewAnalytics.getAverageRating());
        }
        organizationService.triggerKafkaMessageForReview(orgId, userId, reviewerOrgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);
}

    @Operation(
            summary = "Get review analytics and paginated feed by role",
            description = "Returns star distributions and averages for both roles, alongside a paginated list of reviews filtered by the specified role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review analytics and paginated feed retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid role parameter or pagination values provided.")
    })
    @GetMapping("/{orgId}/reviews")
    public ResponseEntity<OrganizationReviewsResponseDTO> getReviewAnalytics(
            @PathVariable String orgId,
            @RequestParam MaasRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        OrganizationReviewsResponseDTO analytics = organizationService.getReviewAnalytics(orgId, role, pageable);

        return ResponseEntity.ok(analytics);
    }

    @Operation(summary = "Edit an existing organization review", description = "Allows the original author of a review to update its rating score and text comment. "
            +
            "The target organization and target role context cannot be mutated.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input payload data (e.g., rating out of 1-5 range, comment text limits exceeded)."),
            @ApiResponse(responseCode = "401", description = "Authentication failed! Invalid or expired Bearer token."),
            @ApiResponse(responseCode = "403", description = "Forbidden! You are authenticated but you are not the original author of this review."),
            @ApiResponse(responseCode = "404", description = "Review not found with the provided reviewId identifier.")
    })
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<OrganizationReview> updateReview(
                    @PathVariable String reviewId,
                    @RequestBody @Valid CreateReviewDTO editDto) {

            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext()
                            .getAuthentication();
            String currentUserId = jwtToken.getToken().getClaim("sub");
            String reviewerOrgId = jwtToken.getToken().getClaim(ORGANIZATION_ID);

            OrganizationReview updatedReview = organizationService.updateReview(reviewId, currentUserId, editDto);
            ReviewAnalyticsDTO reviewAnalytics = organizationService.calculateRoleAnalytics(
                                    updatedReview.getTargetOrganizationId(), editDto.getTargetRole());
            if (MaasRole.PROVIDER == updatedReview.getTargetRole()) {
                    organizationService.updateOrganizationsProviderRating(updatedReview.getTargetOrganizationId(),
                                    reviewAnalytics.getAverageRating());
            } else if (MaasRole.CONSUMER == updatedReview.getTargetRole()) {
                    organizationService.updateOrganizationsConsumerRating(updatedReview.getTargetOrganizationId(),
                                    reviewAnalytics.getAverageRating());
            }
            organizationService.triggerKafkaMessageForReview(updatedReview.getTargetOrganizationId(), currentUserId, reviewerOrgId);

            return ResponseEntity.ok(updatedReview);
    }

    @Operation(summary = "Get reviews performed by the current authenticated organization", description = "Returns a paginated list of reviews written by the caller's organization. Supports an optional query parameter filter to isolate reviews for a specific target company.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated outbound review history retrieved successfully."),
            @ApiResponse(responseCode = "401", description = "Authentication failed! Invalid or expired Bearer token.")
    })
    @GetMapping("/reviews/performed")
    public ResponseEntity<Page<OrganizationReview>> getReviewsPerformedByMyOrganization(
            @RequestParam(required = false) String targetOrganizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) SecurityContextHolder.getContext()
                .getAuthentication();
        String reviewerOrgId = jwtToken.getToken().getClaim(ORGANIZATION_ID);

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        Page<OrganizationReview> performedReviews = organizationService.getReviewsPerformedByOrganization(
                reviewerOrgId, targetOrganizationId, pageable);

        return ResponseEntity.ok(performedReviews);
    }

    /**
     * 
     * Get logos for a list of organization IDs
     *
     * @param organizationIds List of IDs to fetch logos for
     * @return List of organization logo responses
     */
    @Operation(summary = "Get logos for multiple organizations", description = "Accepts a list of organization IDs and returns their corresponding logo URLs", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
                    @ApiResponse(responseCode = "200", description = "Logos retrieved successfully.", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Invalid payload or empty ID list."),
                    @ApiResponse(responseCode = "401", description = "Authentication process failed!")
    })
    @PostMapping("/logos")
    public ResponseEntity<List<OrganizationLogoResponse>> getOrganizationLogos(
                    @RequestBody List<String> organizationIds) {

            if (organizationIds == null || organizationIds.isEmpty()) {
                    throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "The list of organization IDs cannot be empty");
            }

            List<OrganizationLogoResponse> response = organizationService.getLogosForOrganizations(organizationIds);

            return ResponseEntity.ok(response);
    }


    @Operation(
        summary = "Add attachments to an organization",
        description = "Uploads and appends one or more new attachment files to an existing organization without overwriting existing ones.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Attachments added successfully",
            content = @Content(schema = @Schema(implementation = Organization.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad Request - No valid files provided"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Organization not found"
        )
    })
    @PostMapping(value = "/{organizationId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Organization> addAttachmentsToOrganization(
            @PathVariable String organizationId,
            @RequestParam("attachmentFiles") List<MultipartFile> attachmentFiles,
            @RequestParam(value = "attachmentTitles", required = false) List<String> attachmentTitles,
            @RequestParam(value = "attachmentIsPublic", required = false) List<Boolean> attachmentIsPublic,
            JwtAuthenticationToken jwtToken, final HttpServletRequest request) {

            String userOrgId = (jwtToken != null) ? jwtToken.getToken().getClaimAsString(ORGANIZATION_ID) : null;

            if (userOrgId == null || !userOrgId.equals(organizationId)) {
               throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You do not have permission to modify attachments for this organization.");
            }
            Organization updatedOrg = organizationService.addAttachments(organizationId, attachmentFiles, attachmentTitles, attachmentIsPublic);

            return ResponseEntity.ok(updatedOrg);
    }


    @Operation(
            summary = "Update attachment metadata",
            description = "Updates the title and visibility (isPublic) status of a specific attachment.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attachment updated successfully",
                    content = @Content(schema = @Schema(implementation = Organization.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Authentication failed"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller does not belong to this organization"),
            @ApiResponse(responseCode = "404", description = "Organization or Attachment ID not found")
    })

    @PatchMapping(value = "/{organizationId}/attachments/{fileId}", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Organization> updateAttachment(
            @PathVariable String organizationId,
            @PathVariable String fileId,
            @RequestBody @Valid UpdateFileInformationDTO updateDto,
            JwtAuthenticationToken jwtToken) {

        String userOrgId = (jwtToken != null)
                ? jwtToken.getToken().getClaimAsString(ORGANIZATION_ID)
                : null;

        if (userOrgId == null || !userOrgId.equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to update attachments for this organization.");
        }

        Organization updatedOrg = organizationService.updateAttachmentMetadata(
                organizationId,
                fileId,
                updateDto
        );

        return ResponseEntity.ok(updatedOrg);
    }


    private Organization maskPrivateAttachmentUrls(Organization org, String userOrgId) {
    if (org == null || org.getAttachments() == null || org.getAttachments().isEmpty()) {
        return org;
    }

    // If the authenticated user belongs to the same organization, keep URLs intact
    boolean isMember = userOrgId != null && userOrgId.equals(org.getOrganizationID());
    if (isMember) {
        return org;
    }

    // Mask the file info for non-public attachments
    List<FileInformation> filteredAttachments = org.getAttachments().stream()
        .filter(att -> Boolean.TRUE.equals(att.isPublic()))
        .toList();

    org.setAttachments(new ArrayList<>(filteredAttachments));
    return org;
}
}
