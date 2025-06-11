package gr.atc.t4m.organization_management.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.repository.ManufacturingResourceRepository;

@Service

public class ManufacturingResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManufacturingResourceService.class);
    ManufacturingResourceRepository manufacturingResourceRepo;

    public ManufacturingResourceService(ManufacturingResourceRepository manufacturingResourceRepo) {
        this.manufacturingResourceRepo = manufacturingResourceRepo;

    }

    public Optional<ManufacturingResource> findById(Long manufacturingResourceID) {
        LOGGER.info("Finding Manufacturing Resource by ID {}" , manufacturingResourceID);
    return manufacturingResourceRepo.findByManufacturingResourceID(manufacturingResourceID);
    }

    public void save(ManufacturingResource mr) {
        LOGGER.info("Saving Manufacturing Resource");
        
        manufacturingResourceRepo.save(mr);
    }
}
