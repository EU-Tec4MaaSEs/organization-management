package gr.atc.t4m.organization_management.service;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import gr.atc.t4m.organization_management.dto.OrganizationDTO;
import gr.atc.t4m.organization_management.dto.ProviderSearchDTO;
import gr.atc.t4m.organization_management.exception.OrganizationAlreadyExistsException;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.MaasRole;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.repository.OrganizationRepository;

@Service
public class OrganizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationService.class);
    private static final String ORGANIZATION_WITH_ID = "Organization with id ";

    OrganizationRepository organizationRepository;
    ManufacturingResourceService manufacturingResourceService;
    ModelMapper modelMapper;

    public OrganizationService(OrganizationRepository organizationRepository,
            ManufacturingResourceService manufacturingResourceService, ModelMapper modelMapper) {
        this.organizationRepository = organizationRepository;
        this.manufacturingResourceService = manufacturingResourceService;
        this.modelMapper = modelMapper;

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
            throw new OrganizationNotFoundException(ORGANIZATION_WITH_ID + id + " not found");
        }

        return optOrganization.get();
    }

    public void deleteOrganizationById(String id) {
        Optional<Organization> optOrganization = organizationRepository.findById(id);
        if (optOrganization.isEmpty()) {
            throw new OrganizationNotFoundException(ORGANIZATION_WITH_ID + id + " not found");
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
                .orElseThrow(() -> new OrganizationNotFoundException("Organization with name " + name + " not found"));
    }

}
