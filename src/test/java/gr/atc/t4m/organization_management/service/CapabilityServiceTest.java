package gr.atc.t4m.organization_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.DatasetEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CapabilityServiceTest {

    private CapabilityService capabilityService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        capabilityService = new CapabilityService();
        mapper = new ObjectMapper();
    }

    // parseDatasets

    @Test
    void parseDatasets_shouldReturnDatasetEntries() throws IOException {
        String json = """
                {
                  "data": {
                    "dataset": [
                      {
                        "@id": "dataset1",
                        "@type": "Dataset",
                        "title": "Test Dataset",
                        "description": [
                            {"@language": "en", "@value": "Test description"}
                        ],
                        "keyword": ["kw1"],
                        "distribution": [],
                        "hasPolicy": [{
                            "@type": "Policy",
                            "providerid": "provider123",
                            "permission": [
                                {
                                    "@type": "Permission",
                                    "action": "read"
                                }
                            ]
                        }]
                      },
                      {
                        "@id": "dataset2",
                        "@type": "Dataset",
                        "title": "Second Dataset",
                        "description": [
                            {"@language": "en", "@value": "Second description"}
                        ],
                        "keyword": ["kw2"],
                        "distribution": [],
                        "hasPolicy": [{
                            "@type": "Policy",
                            "providerid": "provider456",
                            "permission": [
                                {
                                    "@type": "Permission",
                                    "action": "write"
                                }
                            ]
                        }]
                      }
                    ]
                  }
                }
                """;

        List<DatasetEntry> entries = capabilityService.parseDatasets(json);

        assertEquals(2, entries.size());

        DatasetEntry first = entries.get(0);
        assertEquals("dataset1", first.getId());
        assertEquals("Test Dataset", first.getTitle());
        assertEquals("Test description", first.getDescription().get(0).getValue());
        assertEquals("read", first.getHasPolicy().get(0).getPermissions().get(0).getAction());

        DatasetEntry second = entries.get(1);
        assertEquals("dataset2", second.getId());
        assertEquals("Second Dataset", second.getTitle());
        assertEquals("Second description", second.getDescription().get(0).getValue());
        assertEquals("write", second.getHasPolicy().get(0).getPermissions().get(0).getAction());
    }

    @Test
    void parseDatasets_shouldReturnEmptyListIfNoDatasets() throws IOException {
        String json = """
                { "data": { "dataset": [] } }
                """;

        List<DatasetEntry> entries = capabilityService.parseDatasets(json);
        assertTrue(entries.isEmpty());
    }

    @Test
    void parseDatasets_shouldHandleInvalidJson() {
        String invalidJson = "{ invalid }";
        assertThrows(IOException.class, () -> capabilityService.parseDatasets(invalidJson));
    }

    /**
     * When the "data" key is absent the "dataset" path resolves to MissingNode,
     * which isArray() returns false for — so the result should be an empty list
     * rather than an exception.
     */
    @Test
    void parseDatasets_shouldReturnEmptyListWhenDataKeyMissing() throws IOException {
        String json = """
                { "other": { "dataset": [] } }
                """;

        List<DatasetEntry> entries = capabilityService.parseDatasets(json);
        assertTrue(entries.isEmpty());
    }

    @Test
    void parseDatasets_shouldReturnEmptyListWhenDatasetKeyMissing() throws IOException {
        String json = """
                { "data": {} }
                """;

        List<DatasetEntry> entries = capabilityService.parseDatasets(json);
        assertTrue(entries.isEmpty());
    }

    // retrieveCapabilitiesInformation

    @Test
    void retrieveCapabilitiesInformation_shouldReturnEntries() throws IOException {
        String json = """
                {
                  "data": {
                    "dataset": [
                      {
                        "@id": "dataset1",
                        "@type": "Dataset",
                        "title": "Test Dataset",
                        "description": [
                          { "@language": "en", "@value": "Description here" }
                        ],
                        "keyword": ["keyword1"],
                        "distribution": [],
                        "hasPolicy": [
                          {
                            "@type": "Policy",
                            "providerid": "provider1",
                            "permission": [
                              {
                                "@type": "Permission",
                                "action": "read"
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        List<DatasetEntry> entries = capabilityService.retrieveCapabilitiesInformation(json);

        assertEquals(1, entries.size());
        DatasetEntry entry = entries.get(0);
        assertEquals("dataset1", entry.getId());
        assertEquals("Test Dataset", entry.getTitle());
        assertEquals("Description here", entry.getDescription().get(0).getValue());
        assertEquals("read", entry.getHasPolicy().get(0).getPermissions().get(0).getAction());
    }

    @Test
    void retrieveCapabilitiesInformation_shouldThrowIOExceptionIfEmpty() {
        String json = """
                { "data": { "dataset": [] } }
                """;

        assertThrows(IOException.class,
                () -> capabilityService.retrieveCapabilitiesInformation(json));
    }

    @Test
    void retrieveCapabilitiesInformation_shouldPropagateIOExceptionOnInvalidJson() {
        assertThrows(IOException.class,
                () -> capabilityService.retrieveCapabilitiesInformation("{ invalid }"));
    }

    // parseAASCapabilities — basic happy paths

    @Test
    void parseAASCapabilities_shouldReturnEmptyListWhenSubmodelElementsEmpty() throws IOException {
        String json = """
                { "submodelElements": [] }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);
        assertTrue(entries.isEmpty());
    }

    @Test
    void parseAASCapabilities_shouldHandleInvalidJson() {
        String invalidJson = "{ not valid json }";
        assertThrows(IOException.class, () -> capabilityService.parseAASCapabilities(invalidJson));
    }

    @Test
    void parseAASCapabilities_shouldParseCapabilityEntries() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "MillingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Milling",
                          "modelType": "Capability",
                          "qualifiers": [
                            { "type": "OFFERED",        "value": "true"    },
                            { "type": "CapabilityType", "value": "Primary" }
                          ],
                          "value": [
                            {
                              "modelType": "Capability",
                              "qualifiers": [
                                { "type": "OFFERED",        "value": "true",    "valueType": "xs:boolean" },
                                { "type": "CapabilityType", "value": "Primary", "valueType": "xs:string"  }
                              ],
                              "idShort": "Milling"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        CapabilityEntry entry = entries.get(0);
        assertEquals("Milling", entry.getName());
        assertEquals("Primary", entry.getType());
        assertTrue(entry.isOffered());
        assertNull(entry.getComment());
        assertTrue(entry.getProperties().isEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // parseAASCapabilities — comment (MultiLanguageProperty)
    // ─────────────────────────────────────────────────────────────

    @Test
    void parseAASCapabilities_shouldParseCapabilityComment() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "WeldingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Welding",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "CapabilityComment",
                              "modelType": "MultiLanguageProperty",
                              "value": [
                                { "language": "en", "text": "A welding capability." }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        assertEquals("A welding capability.", entries.get(0).getComment());
    }

    // parseAASCapabilities — properties

    @Test
    void parseAASCapabilities_shouldParseCapabilityEntriesWithProperties() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "MillingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "MillingPropertySet",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "MillingPropertySet",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "NumberOfAxesContainer",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "NumberOfAxes",
                                      "modelType": "Property",
                                      "value": "5",
                                      "valueType": "xs:int"
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        CapabilityEntry entry = entries.get(0);
        assertEquals("MillingPropertySet", entry.getName());
        assertEquals(1, entry.getProperties().size());
        assertEquals("NumberOfAxes", entry.getProperties().get(0).getName());
        assertEquals("5", entry.getProperties().get(0).getValue().toString());
        assertEquals("xs:int", entry.getProperties().get(0).getValueType());
    }

    // parseAASCapabilities — Range property type

    @Test
    @SuppressWarnings("unchecked")
    void parseAASCapabilities_shouldParseRangeProperty() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "LaserCuttingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "LaserCuttingPropertySet",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "LaserCuttingPropertySet",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "PowerRangeContainer",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "PowerRange",
                                      "modelType": "Range",
                                      "min": "100",
                                      "max": "500",
                                      "valueType": "xs:int"
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        var props = entries.get(0).getProperties();
        assertEquals(1, props.size());

        var prop = props.get(0);
        assertEquals("PowerRange", prop.getName());
        assertEquals("xs:int (range)", prop.getValueType());

        Map<String, Object> rangeValue = (Map<String, Object>) prop.getValue();
        assertEquals(100, rangeValue.get("min"));
        assertEquals(500, rangeValue.get("max"));
    }

    // parseAASCapabilities — SubmodelElementList (string list)

    @Test
    @SuppressWarnings("unchecked")
    void parseAASCapabilities_shouldParseSubmodelElementListProperty() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "SprayCoatingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "SprayCoatingPropertySet",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "SprayCoatingPropertySet",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "SupportedMaterialsContainer",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "SupportedMaterials",
                                      "modelType": "SubmodelElementList",
                                      "value": [
                                        { "idShort": "",  "modelType": "Property", "value": "Steel",    "valueType": "xs:string" },
                                        { "idShort": "",  "modelType": "Property", "value": "Aluminum", "valueType": "xs:string" },
                                        { "idShort": "",  "modelType": "Property", "value": "Titanium", "valueType": "xs:string" }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        var props = entries.get(0).getProperties();
        assertEquals(1, props.size());

        var prop = props.get(0);
        assertEquals("SupportedMaterials", prop.getName());
        assertEquals("xs:string[]", prop.getValueType());

        List<String> values = (List<String>) prop.getValue();
        assertEquals(3, values.size());
        assertTrue(values.contains("Steel"));
        assertTrue(values.contains("Aluminum"));
        assertTrue(values.contains("Titanium"));
    }

    // parseAASCapabilities — property comment (MultiLanguageProperty)

    @Test
    void parseAASCapabilities_shouldParsePropertyComment() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "GrindingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "GrindingPropertySet",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "GrindingPropertySet",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "WheelSpeedContainer",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "WheelSpeed",
                                      "modelType": "Property",
                                      "value": "3000",
                                      "valueType": "xs:int"
                                    },
                                    {
                                      "idShort": "PropertyComment",
                                      "modelType": "MultiLanguageProperty",
                                      "value": [
                                        { "language": "en", "text": "Speed of the grinding wheel in RPM." }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        var props = entries.get(0).getProperties();
        assertEquals(1, props.size());
        assertEquals("Speed of the grinding wheel in RPM.", props.get(0).getComment());
    }

    // parseAASCapabilities — CapacitySet

    @Test
    void parseAASCapabilities_shouldParseCapacitySet() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "AssemblyContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Assembly",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "CapacitySet",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "AvailableCapacityContainer",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "AvailableCapacity",
                                      "modelType": "RelationshipElement",
                                      "first": { "keys": [] },
                                      "second": {
                                        "keys": [
                                          { "type": "Submodel", "value": "calendar-ref-001" }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        assertNotNull(entries.get(0).getCapacitySet());
        assertEquals("calendar-ref-001", entries.get(0).getCapacitySet().getAvailableCapacityRef());
    }

    @Test
    void parseAASCapabilities_shouldHandleCapacitySetWithEmptySecondKeys() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "AssemblyContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Assembly",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "CapacitySet",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "AvailableCapacityContainer",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "AvailableCapacity",
                                      "modelType": "RelationshipElement",
                                      "first": { "keys": [] },
                                      "second": { "keys": [] }
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        // Entry should parse, but capacitySet should be null since keys are empty
        assertEquals(1, entries.size());
        assertNull(entries.get(0).getCapacitySet());
    }

    // parseAASCapabilities — GeneralizationRelation

    @Test
    void parseAASCapabilities_shouldParseGeneralizationRelation() throws IOException {
        // CapabilityRelations requires a wrapper list whose first element's value
        // contains a Relation with at least 4 keys in both first and second.
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "MillingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Milling",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "CapabilityRelations",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "GeneralizationWrapper",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "IsGeneralizedBy",
                                      "modelType": "RelationshipElement",
                                      "first": {
                                        "keys": [
                                          { "type": "Submodel",  "value": "k0" },
                                          { "type": "Submodel",  "value": "k1" },
                                          { "type": "Submodel",  "value": "k2" },
                                          { "type": "Capability","value": "Milling" }
                                        ]
                                      },
                                      "second": {
                                        "keys": [
                                          { "type": "Submodel",  "value": "k0" },
                                          { "type": "Submodel",  "value": "k1" },
                                          { "type": "Submodel",  "value": "k2" },
                                          { "type": "Capability","value": "MachiningGeneral" }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        var relation = entries.get(0).getGeneralizedBy();
        assertNotNull(relation);
        assertEquals("Milling", relation.getFirst());
        assertEquals("MachiningGeneral", relation.getSecond());
    }

    @Test
    void parseAASCapabilities_shouldReturnNullRelationWhenKeysTooShort() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "MillingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Milling",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "CapabilityRelations",
                              "modelType": "SubmodelElementCollection",
                              "value": [
                                {
                                  "idShort": "GeneralizationWrapper",
                                  "modelType": "SubmodelElementCollection",
                                  "value": [
                                    {
                                      "idShort": "IsGeneralizedBy",
                                      "modelType": "RelationshipElement",
                                      "first": {
                                        "keys": [
                                          { "type": "Submodel", "value": "k0" },
                                          { "type": "Submodel", "value": "k1" }
                                        ]
                                      },
                                      "second": {
                                        "keys": [
                                          { "type": "Submodel", "value": "k0" },
                                          { "type": "Submodel", "value": "k1" }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        assertNull(entries.get(0).getGeneralizedBy());
    }

    // parseAASCapabilities — qualifier edge cases

    @Test
    void parseAASCapabilities_shouldTolerateUnknownQualifierType() throws IOException {
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "MillingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Milling",
                          "modelType": "SubmodelElementCollection",
                          "value": [
                            {
                              "idShort": "Milling",
                              "modelType": "Capability",
                              "qualifiers": [
                                { "type": "OFFERED",          "value": "false"     },
                                { "type": "CapabilityType",   "value": "Secondary" },
                                { "type": "UNKNOWN_QUALIFIER","value": "whatever"  }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);

        assertEquals(1, entries.size());
        CapabilityEntry entry = entries.get(0);
        // Known qualifiers must still be parsed correctly
        assertFalse(entry.isOffered());
        assertEquals("Secondary", entry.getType());
        // No exception thrown — unknown type is silently skipped
    }

    @Test
    void parseAASCapabilities_shouldTolerateNullQualifiersList() {
        // Capability element with no qualifiers field at all
        String json = """
                {
                  "submodelElements": [
                    {
                      "idShort": "MillingContainer",
                      "modelType": "SubmodelElementCollection",
                      "value": [
                        {
                          "idShort": "Milling",
                          "modelType": "Capability",
                          "value": []
                        }
                      ]
                    }
                  ]
                }
                """;

        assertDoesNotThrow(() -> capabilityService.parseAASCapabilities(json));
    }

    // Integration — full Teknier fixture

    @Test
    void parseDatasets_shouldTestTeknikerOutcome() throws IOException {
        String json = Files.readString(Paths.get("src/test/resources/DSTecnikerResponse.json"));
        List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);
        CapabilityEntry entry = entries.get(0);

        assertEquals("Milling", entry.getName());
        assertEquals(11, entry.getProperties().size());
        assertEquals(
                "machining operation which consists of removing material by means of a rotary tool called a \"milling cutter\" of which there are several different types. Note 1 to entry: The typical milling operations mostly involve face milling or end milling. The tools are mounted either in the spindle taper or on the spindle front face. (ISO 8636-1:2000)",
                entry.getComment()
        );
        assertEquals("Primary", entry.getType());

        assertEquals("NumberOfAxis", entry.getProperties().get(0).getName());
        assertEquals("5", entry.getProperties().get(0).getValue());
        assertEquals("xs:int", entry.getProperties().get(0).getValueType());
        assertEquals(
                "Number of axes. It refers to the degrees of freedom or directions in which the machine can move its tool or workpiece.",
                entry.getProperties().get(0).getComment()
        );
    }
}