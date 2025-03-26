package gr.atc.t4m.organization_management.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;

@Service
public class OrganizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationService.class);
    OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;

    }

    public Organization createOrganization(Organization organization) {
        LOGGER.info("Creating Organization");
        organizationRepository.findByOrganizationName(organization.getOrganizationName()).ifPresent(org -> {
            throw new OrganizationAlreadyExistsException(
                    "Organization with name " + organization.getOrganizationName() + " already exists");

        });
        organizationRepository.save(organization);
        return organization;
    }

    public Organization getOrganization(String id) throws OrganizationNotFoundException {
        Optional<Organization> optOrganization = organizationRepository.findById(id);
        if (optOrganization.isEmpty()) {
            throw new OrganizationNotFoundException("Organization with id " + id + " not found");
        }

        return optOrganization.get();
    }

    public void deleteOrganizationById(String id) {
        Optional<Organization> optOrganization = organizationRepository.findById(id);
        if (optOrganization.isEmpty()) {
            throw new OrganizationNotFoundException("Organization with id " + id + " not found");
        }
        organizationRepository.delete(optOrganization.get());

    }

    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);

    }

    public Organization updateOrganization(String id, OrganizationDTO organizationDTO) {
        Optional<Organization> optOrganization = organizationRepository.findById(id);
        if (optOrganization.isEmpty()) {
            throw new OrganizationNotFoundException("Organization with id " + id + " not found. Update is aborted");
        }
        Organization existingOrganization = optOrganization.get();
        existingOrganization.setOrganizationName(organizationDTO.getOrganizationName());
        existingOrganization.setAddress(organizationDTO.getAddress());
        existingOrganization.setContact(organizationDTO.getContact());
        existingOrganization.setDsConnectorURL(organizationDTO.getDsConnectorURL());
        existingOrganization.setOrganizationRating(organizationDTO.getOrganizationRating());
        existingOrganization.setMaasRole(organizationDTO.getMaasRole());
        existingOrganization.setMaasConsumer(organizationDTO.getMaasConsumer());
        existingOrganization.setMaasProvider(organizationDTO.getMaasProvider());

        return organizationRepository.save(existingOrganization);
    }

}
