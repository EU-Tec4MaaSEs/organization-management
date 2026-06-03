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
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    OrganizationRepository organizationRepository;
    OrganizationReviewRepository reviewRepository;
    ManufacturingResourceService manufacturingResourceService;
    ModelMapper modelMapper;
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

    public OrganizationService(OrganizationRepository organizationRepository,
                               ManufacturingResourceService manufacturingResourceService, ModelMapper modelMapper,
                               KafkaTemplate<String, EventDTO> kafkaTemplate,
                               OrganizationReviewRepository reviewRepository, RestTemplate restTemplate) {
        this.organizationRepository = organizationRepository;
        this.manufacturingResourceService = manufacturingResourceService;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.reviewRepository = reviewRepository;
        this.restTemplate = restTemplate;
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

    public void deleteOrganizationById(String id) {
        Optional<Organization> optOrganization = organizationRepository.findById(id);
        if (optOrganization.isEmpty()) {
            throw new OrganizationNotFoundException(ORGANIZATION_WITH_ID + id + NOT_FOUND);
        }
        //remove associated manufacturing resources
        if (optOrganization.get().getManufacturingResources() != null) {
            optOrganization.get().getManufacturingResources().forEach(mr -> {
                if (mr.getManufacturingResourceID() != null) {
                    Optional<ManufacturingResource> optManufacturingResource = manufacturingResourceService.findById(mr.getManufacturingResourceID());
                    optManufacturingResource.ifPresent(manufacturingResource ->
                            manufacturingResourceService.delete(mr.getManufacturingResourceID())
                    );
                }
            });
        }
        organizationRepository.delete(optOrganization.get());

    }

    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);

    }

    public Organization updateOrganization(String id, OrganizationDTO organizationDTO) {
        Organization existing = organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        ORGANIZATION_WITH_ID + id + " not found. Update is aborted"));

        modelMapper.map(organizationDTO, existing);
        if (organizationDTO.getMaasProvider() == null) {
            existing.setMaasProvider(null);
        }
        if (organizationDTO.getMaasConsumer() == null) {
            existing.setMaasConsumer(null);
        }

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


    public void createKafkaMessage(Organization organization, String userId, EventType eventType, String verifiableCredential) {

        OrganizationRegistrationEvent data = new OrganizationRegistrationEvent();
        data.setId(organization.getOrganizationID());
        data.setUserId(userId);
        data.setName(organization.getOrganizationName());
        if (eventType != EventType.DELETE) {
            if (verifiableCredential != null && !verifiableCredential.isBlank()) {
                data.setVerifiableCredential(verifiableCredential);
            } else {
                data.setVerifiableCredential("Invalid Verifiable Credential");
            }
            data.setContact(organization.getContact());
            data.setRole(organization.getMaasRole());
            data.setDataSpaceConnectorUrl(organization.getDsConnectorURL());
            data.setValueNetwork(organization.getValueNetwork());
        }

        EventDTO event = setEventInformation(eventType, organization, data);

        try {
            SendResult<String, EventDTO> result = kafkaTemplate.send(organizationRegistrationTopic, event)
                    .get();
            RecordMetadata metadata = result.getRecordMetadata();
            LOGGER.info("Message sent to partition {} with offset {}", metadata.partition(), metadata.offset());
        } catch (Exception e) {
            LOGGER.error("Failed to send message: {}", e.getMessage());
            Thread.currentThread().interrupt();

        }

    }


    private EventDTO setEventInformation(EventType eventType, Organization organization, OrganizationRegistrationEvent data) {
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


    private ReviewAnalyticsDTO calculateRoleAnalytics(String orgId, MaasRole role) {
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
        review.setUpdatedAt(LocalDateTime.now());


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
}
