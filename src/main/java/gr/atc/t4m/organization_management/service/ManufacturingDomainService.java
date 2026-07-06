
package gr.atc.t4m.organization_management.service;

import gr.atc.t4m.organization_management.dto.ManufacturingDomainRequest;
import gr.atc.t4m.organization_management.exception.DomainAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.DomainInUseException;
import gr.atc.t4m.organization_management.model.ManufacturingDomain;
import gr.atc.t4m.organization_management.repository.ManufacturingDomainRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ManufacturingDomainService {

    private final ManufacturingDomainRepository domainRepository;
    private final OrganizationService organizationService;

    public ManufacturingDomainService(
            ManufacturingDomainRepository domainRepository,
            @Lazy OrganizationService organizationService) {
        this.domainRepository = domainRepository;
        this.organizationService = organizationService;
    }

    /**
     * Retrieves a paginated result of manufacturing domains from MongoDB.
     */
    public Page<ManufacturingDomain> getAllDomains(Pageable pageable) {
        log.debug("Fetching paginated manufacturing domains. Page: {}, Size: {}", pageable.getPageNumber(),
                pageable.getPageSize());
        return domainRepository.findAll(pageable);
    }

    /**
     * Creates and saves a new manufacturing domain document.
     */
    public ManufacturingDomain createDomain(ManufacturingDomainRequest request) {
        String normalizedAbbreviation = request.getAbbreviation() != null
                ? request.getAbbreviation().trim().toUpperCase()
                : "";
        log.info("Creating new manufacturing domain with abbreviation: {}", normalizedAbbreviation);

        domainRepository.findByAbbreviation(normalizedAbbreviation)
                .ifPresent(existing -> {
                    log.error("Failed to create domain: Abbreviation '{}' is already taken.", normalizedAbbreviation);
                    throw new DomainAlreadyExistsException(
                            "A manufacturing domain with abbreviation '" + normalizedAbbreviation
                                    + "' already exists.");
                });

        ManufacturingDomain domain = ManufacturingDomain.builder()
                .name(request.getName() != null ? request.getName().trim() : null)
                .abbreviation(normalizedAbbreviation)
                .description(request.getDescription())
                .build();

        return domainRepository.save(domain);
    }

    /**
     * Updates an existing manufacturing domain if found.
     */
    @Transactional
    public Optional<ManufacturingDomain> updateDomain(String id, ManufacturingDomainRequest request) {
        log.info("Updating manufacturing domain with ID: {}", id);

        String newAbbreviation = request.getAbbreviation() != null ? request.getAbbreviation().toUpperCase().trim() : "";

        domainRepository.findByAbbreviation(newAbbreviation)
                .ifPresent(conflictingDomain -> {
                    if (!conflictingDomain.getId().equals(id)) {
                        log.error("Failed to update domain ID: {}. Abbreviation '{}' is already used by domain ID: {}",
                                id, newAbbreviation, conflictingDomain.getId());
                        throw new DomainAlreadyExistsException(
                                "Cannot update domain. A manufacturing domain with abbreviation '" + newAbbreviation
                                        + "' already exists.");
                    }
                });

        return domainRepository.findById(id)
                .map(existingDomain -> {
                    String oldAbbreviation = existingDomain.getAbbreviation();

                    existingDomain.setName(request.getName() != null ? request.getName().trim() : null);
                    existingDomain.setAbbreviation(newAbbreviation);
                    existingDomain.setDescription(request.getDescription());

                    ManufacturingDomain savedDomain = domainRepository.save(existingDomain);

                    if (oldAbbreviation != null && !oldAbbreviation.equalsIgnoreCase(newAbbreviation)) {
                        log.warn("Domain abbreviation changed from '{}' to '{}'. Cascading updates to organizations...",
                                oldAbbreviation, newAbbreviation);
                        organizationService.cascadeServiceCodeUpdate(oldAbbreviation, newAbbreviation);
                    }

                    return savedDomain;
                });
    }

    /**
     * Performs a complete deletion of a domain from MongoDB.
     */
    public boolean deleteDomain(String id) {
        log.warn("Initiate deletion check for manufacturing domain ID: {}", id);

        return domainRepository.findById(id)
                .map(domain -> {
                    String abbreviation = domain.getAbbreviation();

                    // Check if this Domain is already used by organizations
                    List<String> blockingOrganizations = organizationService.getOrganizationNamesUsingService(abbreviation);

                    // Block deletion if any organization uses it.
                    if (!blockingOrganizations.isEmpty()) {
                        String formattedNames = String.join(", ", blockingOrganizations);
                        log.info("Deletion rejected: Domain '{}' is used by: [{}]", abbreviation, formattedNames);

                        throw new DomainInUseException(
                                "Cannot delete domain '" + abbreviation
                                        + "' because it is currently used by the following organization(s):"
                                        + formattedNames);
                    }

                    domainRepository.deleteById(id);
                    log.info("Manufacturing domain ID: {} ({}) has been successfully removed.", id, abbreviation);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Retrieves a unique list of all manufacturing domain abbreviations.
     */
    public List<String> getAllDistinctAbbreviations() {
        log.debug("Fetching all distinct manufacturing domain abbreviations.");
        return domainRepository.findDistinctAbbreviations();
    }

    /**
     * Retrieves a manufacturing domain document by its unique database ID.
     */
    public Optional<ManufacturingDomain> getDomainById(String id) {
        log.debug("Fetching manufacturing domain details for ID: {}", id);
        return domainRepository.findById(id);
    }

    /**
     * Checks if a specific domain abbreviation exists in the collection.
     */
    public boolean existsByAbbreviation(String abbreviation) {
        if (abbreviation == null)
            return false;
        return domainRepository.findByAbbreviation(abbreviation.trim().toUpperCase()).isPresent();
    }
}