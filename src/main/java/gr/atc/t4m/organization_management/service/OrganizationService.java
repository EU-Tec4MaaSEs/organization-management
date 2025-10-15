package gr.atc.t4m.organization_management.service;

import java.util.List;
import java.util.Optional;
import gr.atc.t4m.organization_management.model.EventType;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.t4m.organization_management.dto.EventDTO;
import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.model.events.OrganizationRegistrationEvent;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
@Service
public class OrganizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationService.class);
    private static final String ORGANIZATION_WITH_ID = "Organization with id ";
    private static final String NOT_FOUND = " not found";

    OrganizationRepository organizationRepository;
    ManufacturingResourceService manufacturingResourceService;
    ModelMapper modelMapper;
    private KafkaTemplate<String, EventDTO> kafkaTemplate;

    @Value("${kafka.topic.organization-registration}")
    private String organizationRegistrationTopic;

    public OrganizationService(OrganizationRepository organizationRepository,
            ManufacturingResourceService manufacturingResourceService, ModelMapper modelMapper,
            KafkaTemplate<String, EventDTO> kafkaTemplate) {
        this.organizationRepository = organizationRepository;
        this.manufacturingResourceService = manufacturingResourceService;
        this.modelMapper = modelMapper;
        this.kafkaTemplate = kafkaTemplate;
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
                }
                else{
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


        public void createKafkaMessage(Organization organization, String userId, EventType eventType) {
    
            OrganizationRegistrationEvent data = new OrganizationRegistrationEvent();
            data.setId(organization.getOrganizationID());
            data.setUserId(userId);
            data.setName(organization.getOrganizationName());
            if (eventType != EventType.DELETE) {
                //The verifiable credential is set to a dummy value for demonstration purposes.
               data.setVerifiableCredential(Base64.getEncoder().encodeToString("Hello World".getBytes()));
               data.setContact(organization.getContact());
               data.setRole(organization.getMaasRole());
               data.setDataSpaceConnectorUrl(organization.getDsConnectorURL());
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
                   event.setType( "Organization_Onboarding");
                   event.setDescription("Organization registration event for " + organization.getOrganizationName());

                    break;
                case UPDATE:
                    event.setType( "Organization_Updated");
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

        public void addManufacturingResource(Organization organization, ManufacturingResource manufacturingResource) {

            if (organization.getManufacturingResources() == null) {
                organization.setManufacturingResources(new ArrayList<>());
            }

            organization.getManufacturingResources().add(manufacturingResource);

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

}
