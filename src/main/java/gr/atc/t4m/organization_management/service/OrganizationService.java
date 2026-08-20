package gr.atc.t4m.organization_management.service;

import java.util.*;

import gr.atc.t4m.organization_management.dto.*;
import gr.atc.t4m.organization_management.model.EventType;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.t4m.organization_management.exception.DomainNotFoundException;
import gr.atc.t4m.organization_management.exception.InvalidOrganizationRoleException;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.model.OrganizationReview;
import gr.atc.t4m.organization_management.model.events.OrganizationRegistrationEvent;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;
import gr.atc.t4m.organization_management.repository.OrganizationReviewRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;

@Service
public class OrganizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationService.class);
    private static final String ORGANIZATION_WITH_ID = "Organization with id ";
    private static final String NOT_FOUND = " not found";
    private static final String ORGANIZATION_NOT_FOUND = "Organization not found: ";

    OrganizationRepository organizationRepository;
    OrganizationReviewRepository reviewRepository;
    ManufacturingResourceService manufacturingResourceService;
    ManufacturingDomainService manufacturingDomainService;
    ModelMapper modelMapper;
    MinioService minioService;

    private KafkaTemplate<String, EventDTO> kafkaTemplate;

    @Value("${kafka.topic.organization-registration}")
    private String organizationRegistrationTopic;

    @Value("${identity-provider.url}")
    private String identityProviderUrl;

    @Value("${identity-provider.user}")
    private String identityProviderUser;

    @Value("${identity-provider.password}")
    private String identityProviderPassword;

    private final RestTemplate restTemplate;
    private static final String ATTACHMENT_WITH_ID = "Attachment with ID ";

    public OrganizationService(OrganizationRepository organizationRepository,
                               ManufacturingResourceService manufacturingResourceService, ModelMapper modelMapper,
                               KafkaTemplate<String, EventDTO> kafkaTemplate,
                               OrganizationReviewRepository reviewRepository, RestTemplate restTemplate,
                               ManufacturingDomainService manufacturingDomainService,
                               MinioService minioService) {
        this.organizationRepository = organizationRepository;
        this.manufacturingResourceService = manufacturingResourceService;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.reviewRepository = reviewRepository;
        this.restTemplate = restTemplate;
        this.manufacturingDomainService = manufacturingDomainService;
        this.minioService = minioService;
    }

    public String issueVerifiableCredential(VerifiableCredentialInputDTO input) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String credentials = identityProviderUser + ":" + identityProviderPassword;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encodedCredentials);

        HttpEntity<VerifiableCredentialInputDTO> request = new HttpEntity<>(input, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                identityProviderUrl, request, String.class
        );

        return response.getBody();
    }

    public Organization createOrganization(Organization organization) {
        LOGGER.info("Creating Organization");
        organizationRepository.findByOrganizationName(organization.getOrganizationName()).ifPresent(org -> {
            throw new OrganizationAlreadyExistsException(
                    "Organization with name " + organization.getOrganizationName() + " already exists");

        });

          //checks if received manufacturing services are valid
          validateManufacturingServices(organization);

        if (organization.getManufacturingResources() != null) {
            organization.getManufacturingResources().forEach(mr -> {
                mr.getManufacturingResourceID();
                if (mr.getManufacturingResourceID() == null) {

                    throw new OrganizationAlreadyExistsException("Manufacturing Resource ID is required");
                } else {
                    Optional<ManufacturingResource> optManufacturingResource = manufacturingResourceService.findById(mr.getManufacturingResourceID());
                    if (optManufacturingResource.isEmpty()) {
                        manufacturingResourceService.save(mr);
                    }
                }
            });
        }
        organizationRepository.save(organization);
        return organization;
    }

    public Organization getOrganization(String id) throws OrganizationNotFoundException {
        Optional<Organization> optOrganization = organizationRepository.findById(id);
        if (optOrganization.isEmpty()) {
            throw new OrganizationNotFoundException(ORGANIZATION_WITH_ID + id + NOT_FOUND);
        }

        return optOrganization.get();
    }
    @Transactional
    public void deleteOrganizationById(String id) {
        Organization org = organizationRepository.findById(id)
        .orElseThrow(() -> new OrganizationNotFoundException(ORGANIZATION_WITH_ID + id + NOT_FOUND));

        deleteAllOrganizationFiles(org);


        //remove associated manufacturing resources
        if (org.getManufacturingResources() != null) {
            org.getManufacturingResources().forEach(mr -> {
                if (mr.getManufacturingResourceID() != null) {
                    Optional<ManufacturingResource> optManufacturingResource = manufacturingResourceService.findById(mr.getManufacturingResourceID());
                    optManufacturingResource.ifPresent(manufacturingResource ->
                            manufacturingResourceService.delete(mr.getManufacturingResourceID())
                    );
                }
            });
        }
        organizationRepository.delete(org);

    }

    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);

    }

    public Organization updateOrganization(String id, OrganizationDTO organizationDTO) {

        Organization existing = findOrganizationById(id);
        String existingId = existing.getOrganizationID();

        modelMapper.map(organizationDTO, existing);

        existing.setOrganizationID(existingId);
        if (organizationDTO.getMaasProvider() == null) {
            existing.setMaasProvider(null);
        }
        if (organizationDTO.getMaasConsumer() == null) {
            existing.setMaasConsumer(null);
        }

        //checks if received manufacturing services are valid
        validateManufacturingServices(existing);
        return organizationRepository.save(existing);
    }

    public List<Organization> getAllProviders() {
        return organizationRepository.findByMaasRoleContaining(MaasRole.PROVIDER.getName());
    }

    public List<Organization> searchProviders(ProviderSearchDTO filter) {
        return organizationRepository.filterProviders(filter);

    }

    public Organization getOrganizationByName(String name) {
        return organizationRepository.findByOrganizationName(name)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization with name " + name + NOT_FOUND));
    }

    //This procedure is called for event types CREATE,UPDATE,DELETE
    //FOr that reason the reviwerOrganizationName is null 
    public void createKafkaMessage(Organization organization, String userId, EventType eventType, String verifiableCredential) {

        OrganizationRegistrationEvent data = buildKafkaEventData(organization, userId, eventType, verifiableCredential);
        EventDTO event = setEventInformation(eventType, organization, data, null);
        sendKafkaEvent(event); 

    }


    private EventDTO setEventInformation(EventType eventType, Organization organization, OrganizationRegistrationEvent data, String reviewerOrganizationName) {
        EventDTO event = new EventDTO();

        switch (eventType) {
            case CREATE:
                event.setType("Organization_Onboarding");
                event.setDescription("Organization registration event for " + organization.getOrganizationName());

                break;
            case UPDATE:
                event.setType("Organization_Updated");
                event.setDescription("Organization update event for " + organization.getOrganizationName());
                break;
            case UPDATE_REVIEW:
                event.setType("Organization_Review_Updated");
                event.setDescription(" You have received a new review from " + reviewerOrganizationName +". You can view all the reviews you have received in your Organization Profile.");
                break;
            case DELETE:
                event.setType("Organization_Deleted");
                event.setDescription("Organization deletion event for " + organization.getOrganizationName());

                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonData = objectMapper.valueToTree(data);
        event.setData(jsonData);
        event.setSourceComponent("Organization Management");
        OffsetDateTime zonedDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        event.setTimestamp(zonedDateTime);
        event.setPriority("Mid");
        event.setOrganization(organization.getOrganizationName());
        return event;
    }

    public void addManufacturingResource(Organization organization, List<ManufacturingResource> manufacturingResource) {

        if (organization.getManufacturingResources() == null) {
            organization.setManufacturingResources(new ArrayList<>());
        }

        organization.getManufacturingResources().addAll(manufacturingResource);

        organizationRepository.save(organization);
    }

    public List<OrganizationDTO> getOrganizationsByCapabilities(String primaryCapability,
                                                                String secondaryCapability) {
        List<ManufacturingResource> manufacturingResources = manufacturingResourceService
                .findByCapabilities(primaryCapability, secondaryCapability);

        LOGGER.info("Found {} manufacturing resources with the specified capabilities",
                manufacturingResources.size());

        if (manufacturingResources.isEmpty()) {
            return List.of();
        }

        List<String> resourceIds = manufacturingResources.stream()
                .map(ManufacturingResource::getManufacturingResourceID)
                .toList();

        List<ObjectId> resourceObjectIds = resourceIds.stream()
                .map(ObjectId::new)
                .toList();

        List<Organization> organizations = organizationRepository
                .findByManufacturingResourceObjectIds(resourceObjectIds);

        LOGGER.info("Found {} organizations containing matching manufacturing resources", organizations.size());

        for (Organization org : organizations) {
            List<ManufacturingResource> matchedResources = org.getManufacturingResources().stream()
                    .filter(r -> resourceIds.contains(r.getManufacturingResourceID()))
                    .toList();
            org.setManufacturingResources(matchedResources);
        }

        return organizations.stream()
                .map(org -> modelMapper.map(org, OrganizationDTO.class))
                .toList();

    }

    public Organization save(Organization organization) {
        organizationRepository.save(organization);
        return organization;
    }

    @Transactional
    public OrganizationReview saveReview(String targetOrgId, String reviewerUserId, String reviewerOrgId, CreateReviewDTO dto) {
        LOGGER.info("Validating and saving flat review for organization: {}", targetOrgId);

        Organization targetOrg = getOrganization(targetOrgId);
        Organization reviewerOrg = getOrganization(reviewerOrgId);

        List<MaasRole> assignedRoles = targetOrg.getMaasRole();
        if (assignedRoles == null || !assignedRoles.contains(dto.getTargetRole())) {
            throw new InvalidOrganizationRoleException("Invalid role context.");
        }

        OrganizationReview review = new OrganizationReview();

        review.setTargetOrganizationId(targetOrg.getOrganizationID());
        review.setTargetOrganizationName(targetOrg.getOrganizationName());

        review.setReviewerOrganizationId(reviewerOrg.getOrganizationID());
        review.setReviewerOrganizationName(reviewerOrg.getOrganizationName());

        review.setReviewerUserId(reviewerUserId);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setTargetRole(dto.getTargetRole());

        reviewRepository.save(review);
        return review;
    }


    public OrganizationReviewsResponseDTO getReviewAnalytics(String orgId, MaasRole role, Pageable pageable) {
        ReviewAnalyticsDTO providerAnalytics = calculateRoleAnalytics(orgId, MaasRole.PROVIDER);
        ReviewAnalyticsDTO consumerAnalytics = calculateRoleAnalytics(orgId, MaasRole.CONSUMER);

        Page<OrganizationReview> paginatedReviews = reviewRepository
                .findByTargetOrganizationIdAndTargetRole(orgId, role, pageable);

        return new OrganizationReviewsResponseDTO(
                providerAnalytics,
                consumerAnalytics,
                paginatedReviews
        );
    }


    public ReviewAnalyticsDTO calculateRoleAnalytics(String orgId, MaasRole role) {
        List<Map<String, Object>> rawDistribution = reviewRepository.getStarCountDistribution(orgId, role.name());

        long t1 = 0;
        long t2 = 0;
        long t3 = 0;
        long t4 = 0;
        long t5 = 0;
        long totalCount = 0;
        double weightedSum = 0.0;

        for (Map<String, Object> row : rawDistribution) {
            Object idVal = row.get("_id");
            Object countVal = row.get("count");

            if (idVal != null && countVal != null) {
                int starRating = ((Number) idVal).intValue();
                long count = ((Number) countVal).longValue();

                totalCount += count;
                weightedSum += (starRating * count);

                switch (starRating) {
                    case 1 -> t1 = count;
                    case 2 -> t2 = count;
                    case 3 -> t3 = count;
                    case 4 -> t4 = count;
                    case 5 -> t5 = count;
                    default -> { /* Ignore invalid ratings */ }
                }
            }
        }

        double averageRating = (totalCount > 0) ? (weightedSum / totalCount) : 0.0;
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        return new ReviewAnalyticsDTO(averageRating, totalCount, t1, t2, t3, t4, t5);
    }

    @Transactional
    public OrganizationReview updateReview(String reviewId, String currentUserId, CreateReviewDTO editDto) {
        OrganizationReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        if (!review.getReviewerUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this review.");
        }
        review.setRating(editDto.getRating());
        review.setComment(editDto.getComment());
        review.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));


        return reviewRepository.save(review);
    }


    /**
     * Retrieves a paginated history trail of outbound reviews written by an organization,
     * optionally filtered by a specific target organization.
     */
    public Page<OrganizationReview> getReviewsPerformedByOrganization(
            String reviewerOrgId,
            String targetOrganizationId,
            Pageable pageable) {

        LOGGER.info("Fetching outbound reviews from reviewerOrg: {} to targetOrg: {} (Page: {}, Size: {})",
                reviewerOrgId, targetOrganizationId, pageable.getPageNumber(), pageable.getPageSize());

        if (targetOrganizationId != null && !targetOrganizationId.isBlank()) {

            if (!organizationRepository.existsById(targetOrganizationId)) {
                throw new OrganizationNotFoundException("Target organization not found with ID: " + targetOrganizationId);
            }

            return reviewRepository.findByReviewerOrganizationIdAndTargetOrganizationIdOrderByCreatedAtDesc(
                    reviewerOrgId, targetOrganizationId, pageable);
        }

        return reviewRepository.findByReviewerOrganizationIdOrderByCreatedAtDesc(reviewerOrgId, pageable);
    }

 public List<OrganizationLogoResponse> getLogosForOrganizations(List<String> organizationIds) {

List<Organization> organizations =
        organizationRepository.findAllById(organizationIds);

return organizations.stream()
        .filter(org -> org.getLogoUrl() != null)
        .map(org -> new OrganizationLogoResponse(
                org.getOrganizationID(),
                org.getLogoUrl()
        ))
        .toList();

}

private void validateManufacturingServices(Organization organization) {
    if (organization.getMaasProvider() != null 
            && organization.getMaasProvider().getManufacturingServices() != null) {
        
        for (String serviceCode : organization.getMaasProvider().getManufacturingServices()) {
            boolean domainExists = manufacturingDomainService.existsByAbbreviation(serviceCode);
            
            if (!domainExists) {
                LOGGER.error("Validation failed: Domain code '{}' does not exist in master data.", serviceCode);
                throw new DomainNotFoundException(
                        "Manufacturing Service or Domain with abbreviation " + serviceCode + " does not exist");
            }
        }
    }
}

    /**
     * Retrieves a list of organization names that are currently using a specific manufacturing service/domain.
     */
    public List<String> getOrganizationNamesUsingService(String serviceCode) {
        
        return organizationRepository.findByMaasProviderManufacturingServices(serviceCode)
                .stream()
                .map(Organization::getOrganizationName)
                .toList();
    }

    /**
     * Finds all organizations using an old service code and updates it to a new one.
     */
    public void cascadeServiceCodeUpdate(String oldCode, String newCode) {
        LOGGER.info("Cascading manufacturing service code update across organizations: {} -> {}", oldCode, newCode);
        
        // Fetch all organizations currently referencing the old code
        List<Organization> affectedOrgs = organizationRepository.findByMaasProviderManufacturingServices(oldCode);
        
        if (!affectedOrgs.isEmpty()) {
            for (Organization org : affectedOrgs) {
                if (org.getMaasProvider() != null && org.getMaasProvider().getManufacturingServices() != null) {
                    List<String> services = org.getMaasProvider().getManufacturingServices();
                    
                    // Replace the old abbreviation with the new one
                    int index = services.indexOf(oldCode);
                    while (index != -1) {
                        services.set(index, newCode);
                        index = services.indexOf(oldCode);
                    }
                    
                    organizationRepository.save(org);
                }
            }
        }
    }
public void updateOrganizationsConsumerRating(String orgId, double averageRating) {
    Organization existing = findOrganizationById(orgId);
    
    if (existing.getMaasRole() != null && existing.getMaasRole().contains(MaasRole.CONSUMER)
            && existing.getMaasConsumer() != null) {
        existing.getMaasConsumer().setConsumerRating(averageRating);
        organizationRepository.save(existing);
    }
}

public void updateOrganizationsProviderRating(String orgId, double averageRating) {
    Organization existing = findOrganizationById(orgId);
    
    if (existing.getMaasRole() != null && existing.getMaasRole().contains(MaasRole.PROVIDER)
            && existing.getMaasProvider() != null) {
        existing.getMaasProvider().setProviderRating(averageRating);
        organizationRepository.save(existing);
    }
}

private Organization findOrganizationById(String id) {
    return organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException(
                    ORGANIZATION_WITH_ID + id + " not found. Update is aborted"));
}

@Transactional
    public void deleteAttachmentById(String organizationId, String fileId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(ORGANIZATION_NOT_FOUND + organizationId));

        if (org.getAttachments() != null) {
            Optional<FileInformation> attachmentToDelete = org.getAttachments().stream()
                    .filter(att -> fileId.equals(att.id()))
                    .findFirst();

            if (attachmentToDelete.isPresent()) {
                String fileUrl = attachmentToDelete.get().fileUrl();

                // Delete binary from MinIO
                deleteFileFromMinio(fileUrl);

                //Remove entry from DB collection
                org.getAttachments().remove(attachmentToDelete.get());
                organizationRepository.save(org);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, ATTACHMENT_WITH_ID + fileId + NOT_FOUND);
            }
        }
    }

    @Transactional
    public Organization addAttachments( String organizationId, List<MultipartFile> attachmentFiles, List<String> attachmentTitles, List<Boolean> attachmentIsPublic) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(ORGANIZATION_WITH_ID + organizationId + NOT_FOUND));

        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No attachment files provided.");
        }

        List<FileInformation> newAttachments = processAttachments(attachmentFiles, attachmentTitles, attachmentIsPublic);

        if (newAttachments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All provided attachment files were empty or invalid.");
        }

        // Append to existing list (initializing if currently null)
        if (org.getAttachments() == null) {
            org.setAttachments(new ArrayList<>());
        }
        org.getAttachments().addAll(newAttachments);

        return organizationRepository.save(org);
    }

    private List<FileInformation> processAttachments(List<MultipartFile> files, List<String> titles, List<Boolean> isPublicList) {

        List<FileInformation> attachments = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file != null && !file.isEmpty()) {
                String title = getElementOrNull(titles, i);
                Boolean isPublic = getElementOrNull(isPublicList, i);
                attachments.add(processAttachment(file, title, isPublic));
            }
        }
        return attachments;
    }
private FileInformation processAttachment(MultipartFile file, String title, Boolean isPublic) {
        String fileUrl = minioService.uploadFile(file);
        String resolvedTitle = resolveAttachmentTitle(file, title);
        boolean publicFlag = Boolean.TRUE.equals(isPublic);

        return new FileInformation(resolvedTitle, fileUrl, publicFlag);
    }

    private String resolveAttachmentTitle(MultipartFile file, String title) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename;
        }
        return "attachment";
    }

 
    private <T> T getElementOrNull(List<T> list, int index) {
        if (list != null && index < list.size()) {
            return list.get(index);
        }
        return null;
    }
    

       public void deleteAllOrganizationFiles(Organization org) {
        if (org == null) {
            return;
        }

        // Delete Logo
        deleteFileFromMinio(org.getLogoUrl());

        // Delete Attachment Files from MinIO
        if (org.getAttachments() != null) {
            for (FileInformation attachment : org.getAttachments()) {
                if (attachment != null) {
                    deleteFileFromMinio(attachment.fileUrl());
                }
            }
        }
    }

    private void deleteFileFromMinio(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            minioService.deleteFile(fileUrl);
        } catch (Exception e) {
            LOGGER.error("Failed to delete file {} from MinIO: {}", fileUrl, e.getMessage());
        }

    }

@Transactional
public Organization updateAttachmentMetadata(String organizationId, String fileId, UpdateFileInformationDTO updateDto) {
    Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new OrganizationNotFoundException(ORGANIZATION_NOT_FOUND + organizationId));

    if (org.getAttachments() == null || org.getAttachments().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, ATTACHMENT_WITH_ID + fileId + NOT_FOUND);
    }

    boolean found = false;
    List<FileInformation> updatedList = new ArrayList<>();

    for (FileInformation att : org.getAttachments()) {
        if (fileId.equals(att.id())) {
            updatedList.add(new FileInformation(
                    att.id(),
                    updateDto.title().trim(),
                    att.fileUrl(),
                    updateDto.isPublic()
            ));
            found = true;
        } else { //keep al the other existing attachments  
            updatedList.add(att);
        }
    }

    if (!found) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, ATTACHMENT_WITH_ID + fileId + NOT_FOUND);
    }

    org.setAttachments(updatedList);
    return organizationRepository.save(org);
}

public void triggerKafkaMessageForReview(String orgId, String userId, String reviewerOrgId) {
        Organization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new OrganizationNotFoundException(ORGANIZATION_NOT_FOUND + orgId));
        Organization reviewerOrg = organizationRepository.findById(reviewerOrgId)
            .orElseThrow(() -> new OrganizationNotFoundException(ORGANIZATION_NOT_FOUND + reviewerOrgId));
        createKafkaMessageForReview(org, userId, reviewerOrg.getOrganizationName());
}

private void createKafkaMessageForReview(Organization org, String userId, String reviewerOrganizationName) {
          OrganizationRegistrationEvent data = buildKafkaEventData(org, userId, EventType.UPDATE_REVIEW, null);
          EventDTO event = setEventInformation(EventType.UPDATE_REVIEW, org, data, reviewerOrganizationName);
          sendKafkaEvent(event);    
}

private OrganizationRegistrationEvent buildKafkaEventData(Organization organization, String userId, EventType eventType,
        String verifiableCredential) {
    OrganizationRegistrationEvent data = new OrganizationRegistrationEvent();
    data.setId(organization.getOrganizationID());
    data.setUserId(userId);
    data.setName(organization.getOrganizationName());

    if (eventType != EventType.DELETE) {
        if (eventType != EventType.UPDATE_REVIEW) {
            if (verifiableCredential != null && !verifiableCredential.isBlank()) {
                data.setVerifiableCredential(verifiableCredential);
            } else {
                data.setVerifiableCredential("Invalid Verifiable Credential");
            }
        }
        data.setContact(organization.getContact());
        data.setRole(organization.getMaasRole());
        data.setDataSpaceConnectorUrl(organization.getDsConnectorURL());
        data.setValueNetwork(organization.getValueNetwork());
    }

    return data;
}
private void sendKafkaEvent(EventDTO event) {
        try {
            SendResult<String, EventDTO> result = kafkaTemplate.send(organizationRegistrationTopic, event).get();
            RecordMetadata metadata = result.getRecordMetadata();
            LOGGER.info("Kafka event {} sent to partition {} with offset {}", 
                    event.getType(), metadata.partition(), metadata.offset());
        } catch (Exception e) {
           LOGGER.error("Failed to send message: {}", e.getMessage());
           Thread.currentThread().interrupt();
        }
    }
}
