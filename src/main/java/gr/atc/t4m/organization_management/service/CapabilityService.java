package gr.atc.t4m.organization_management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.organization_management.model.*;
import gr.atc.t4m.organization_management.model.SubmodelWrapper.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service responsible for parsing and transforming AAS capabilities
 * from a JSON representation into structured CapabilityEntry objects.
 */
@Service
public class CapabilityService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOGGER = LoggerFactory.getLogger(CapabilityService.class);


    /**
     * Entry point for parsing capabilities from the static AAS JSON file.
     * The static file is a temporary solution.
     * In the future it will a response from a Dataspace connector endpoint.
     *
     * @return List of CapabilityEntry parsed from the JSON file.
     * @throws IOException if the JSON file cannot be read.
     */
public List<CapabilityEntry> parseAASCapabilities(String jsonResponse) throws IOException {
    // Parse JSON string directly instead of reading from file
    SubmodelWrapper wrapper = mapper.readValue(jsonResponse, SubmodelWrapper.class);

      List<SubmodelElement> submodelElements = wrapper.getSubmodelElements();
      if (submodelElements == null || submodelElements.isEmpty()) {
        return Collections.emptyList();
    }

        // Extract the first-level container (assumed to contain all capability sets)
        List<SubmodelElement> capabilitySets = mapper.convertValue(
                wrapper.getSubmodelElements().get(0).getValue(),
                new TypeReference<>() {
                }
        );

        List<CapabilityEntry> results = new ArrayList<>();
        for (SubmodelElement container : capabilitySets) {
            CapabilityEntry entry = parseCapabilityContainer(container);
            results.add(entry);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Parsed AAS Capabilities: {}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
        }
        return results;
    }

    /**
     * Parses a single capability container element into a CapabilityEntry.
     */
    private CapabilityEntry parseCapabilityContainer(SubmodelElement container) {
        CapabilityEntry entry = new CapabilityEntry();
        entry.setName(container.getIdShort().replace(IdShort.Container.name(), ""));

        List<SubmodelElement> elements = mapper.convertValue(container.getValue(), new TypeReference<>() {
        });

        for (SubmodelElement element : elements) {
            String type = element.getModelType();
            String idShort = element.getIdShort();

            // Extract qualifiers like type and offered
            if (Type.Capability.name().equals(type)) {
                extractQualifiers(element.getQualifiers(), entry);
            }

            // Extract top-level capability comment
            if (Type.MultiLanguageProperty.name().equals(type) && IdShort.CapabilityComment.name().equals(idShort)) {
                entry.setComment(extractComment(element));
            }

            // Extract the list of property sets
            if (Type.SubmodelElementCollection.name().equals(type) && idShort.endsWith(IdShort.PropertySet.name())) {
                List<Property> props = extractProperties(element);
                entry.getProperties().addAll(props);
            }

            // Extract generalization relation (e.g., "is generalized by")
            if (Type.SubmodelElementCollection.name().equals(type) && IdShort.CapabilityRelations.name().equals(idShort)) {
                GeneralizationRelation relation = extractRelation(element);
                entry.setGeneralizedBy(relation);
            }
        }

        return entry;
    }

    /**
     * Extracts qualifiers from a Capability element and updates the CapabilityEntry.
     */
    private void extractQualifiers(List<Qualifier> qualifiers, CapabilityEntry entry) {
        if (qualifiers == null) return;

        for (Qualifier qualifier : qualifiers) {
            if (qualifier == null) continue;
            try {
                switch (Type.valueOf(qualifier.getType())) {
                    case CapabilityType -> entry.setType(qualifier.getValue());
                    case Offered -> entry.setOffered(Boolean.parseBoolean(qualifier.getValue()));
                    default -> {
                        LOGGER.info("Unknown qualifier type: {}", qualifier.getType());
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Skip unknown qualifier types (robustness for future-proofing)
            }
        }
    }

    /**
     * Extracts the first language-specific comment string from a MultiLanguageProperty element.
     */
    private String extractComment(SubmodelElement element) {
        MultiLanguageProperty mlp = mapper.convertValue(element, MultiLanguageProperty.class);
        List<LangString> comments = mlp.getLocalizedValues();
        return (comments != null && !comments.isEmpty()) ? comments.get(0).getText() : null;
    }

    /**
     * Parses a SubmodelElement representing a PropertySet into a list of Property objects.
     */
    private List<Property> extractProperties(SubmodelElement element) {
        List<SubmodelElement> propertyContainers = mapper.convertValue(element.getValue(), new TypeReference<>() {});
        List<Property> props = new ArrayList<>();

        for (SubmodelElement container : propertyContainers) {
            Property prop = new Property();
            prop.setName(container.getIdShort().replace(IdShort.Container.name(), ""));

            List<SubmodelElement> valueElements = mapper.convertValue(container.getValue(), new TypeReference<>() {});

            for (SubmodelElement valueElement : valueElements) {
                try {
                    switch (Type.valueOf(valueElement.getModelType())) {
                        case Property -> {
                            WrapperProperty property = mapper.convertValue(valueElement, WrapperProperty.class);
                            prop.setValue(property.getPropertyValue());
                            prop.setValueType(property.getValueType());
                        }
                        case Range -> {
                            Map<String, Object> range = new HashMap<>();
                            if (valueElement.getMin() != null) range.put("min", valueElement.getMin());
                            if (valueElement.getMax() != null) range.put("max", valueElement.getMax());
                            prop.setValue(range);
                            prop.setValueType("xs:int (range)");
                        }
                        case SubmodelElementList -> {
                            List<String> values = extractStringList(valueElement);
                            prop.setValue(values);
                            prop.setValueType("xs:string[]");
                        }
                        case MultiLanguageProperty -> {
                            if (IdShort.PropertyComment.name().equals(valueElement.getIdShort())) {
                                prop.setComment(extractComment(valueElement));
                            }
                        }
                        default -> LOGGER.info("Other property type: {}", valueElement.getModelType());
                    }
                } catch (IllegalArgumentException ignored) {
                    // Skip unknown or unsupported value types
                }
            }

            props.add(prop);
        }

        return props;
    }

    /**
     * Extracts a list of string values from a SubmodelElementList.
     */
    private List<String> extractStringList(SubmodelElement valueElement) {
        List<String> values = new ArrayList<>();
        SubmodelElementList listWrapper = mapper.convertValue(valueElement, SubmodelElementList.class);

        if (listWrapper.getSubmodelElementValue() != null) {
            for (WrapperProperty p : listWrapper.getSubmodelElementValue()) {
                values.add(p.getPropertyValue() != null ? p.getPropertyValue().toString() : null);
            }
        }

        return values;
    }

    /**
     * Parses a relation from a CapabilityRelations element and builds a GeneralizationRelation.
     */
    private GeneralizationRelation extractRelation(SubmodelElement relationElement) {
        List<SubmodelElement> wrapperList = mapper.convertValue(relationElement.getValue(), new TypeReference<>() {
        });
        if (wrapperList.isEmpty() || wrapperList.get(0).getValue().isEmpty()) return null;

        Relation relation = mapper.convertValue(wrapperList.get(0).getValue().get(0), Relation.class);
        String first = relation.getFirst().getKeys().get(3).getValue();
        String second = relation.getSecond().getKeys().get(3).getValue();

        return new GeneralizationRelation(first, second);
    }

    public List<DatasetEntry> retrieveCapabilitiesInformation(String body) throws IOException {
        List<DatasetEntry> datasets = parseDatasets(body);

        if (datasets.isEmpty()) {
            throw new IOException("No datasets found in the response.");
        }
        return datasets;
    }

    public List<DatasetEntry> parseDatasets(String json) throws IOException {
    JsonNode root = mapper.readTree(json);
    JsonNode datasetsNode = root.path("data").path("dcat:dataset");

    List<DatasetEntry> datasets = new ArrayList<>();
    if (datasetsNode.isArray()) {
        for (JsonNode node : datasetsNode) {
            DatasetEntry entry = mapper.treeToValue(node, DatasetEntry.class);
            datasets.add(entry);
        }
    }

    return datasets;
}
}
