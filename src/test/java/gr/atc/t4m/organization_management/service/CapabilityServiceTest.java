
package gr.atc.t4m.organization_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.atc.t4m.organization_management.model.CapabilityEntry;
import gr.atc.t4m.organization_management.model.DatasetEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Paths;


class CapabilityServiceTest {

    private CapabilityService capabilityService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        capabilityService = new CapabilityService();
        mapper = new ObjectMapper();
    }

   @Test
void parseDatasets_shouldReturnDatasetEntries() throws IOException {
    String json = """
            {
              "data": {
                "dcat:dataset": [
                  {
                    "@id": "dataset1",
                    "@type": "Dataset",
                    "dct:title": "Test Dataset",
                    "dct:description": [
                        {"@language": "en", "@value": "Test description"}
                    ],
                    "dcat:keyword": ["kw1"],
                    "dcat:distribution": [],
                    "odrl:hasPolicy": {
                        "@type": "Policy",
                        "odrl:providerId": "provider123",
                        "odrl:permission": [
                            {
                                "@type": "Permission",
                                "odrl:action": "read"
                            }
                        ]
                    }
                  },
                  {
                    "@id": "dataset2",
                    "@type": "Dataset",
                    "dct:title": "Second Dataset",
                    "dct:description": [
                        {"@language": "en", "@value": "Second description"}
                    ],
                    "dcat:keyword": ["kw2"],
                    "dcat:distribution": [],
                    "odrl:hasPolicy": {
                        "@type": "Policy",
                        "odrl:providerId": "provider456",
                        "odrl:permission": [
                            {
                                "@type": "Permission",
                                "odrl:action": "write"
                            }
                        ]
                    }
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
    assertEquals("read", first.getPolicy().getPermissions().get(0).getAction());

    DatasetEntry second = entries.get(1);
    assertEquals("dataset2", second.getId());
    assertEquals("Second Dataset", second.getTitle());
    assertEquals("Second description", second.getDescription().get(0).getValue());
    assertEquals("write", second.getPolicy().getPermissions().get(0).getAction());
}


    @Test
    void parseDatasets_shouldReturnEmptyListIfNoDatasets() throws IOException {
        String json = """
                { "data": { "dcat:dataset": [] } }
                """;

        List<DatasetEntry> entries = capabilityService.parseDatasets(json);
        assertTrue(entries.isEmpty());
    }

    @Test
void retrieveCapabilitiesInformation_shouldReturnEntries() throws IOException {
    String json = """
            {
              "data": {
                "dcat:dataset": [
                  {
                    "@id": "dataset1",
                    "@type": "Dataset",
                    "dct:title": "Test Dataset",
                    "dct:description": [
                      {"@language": "en", "@value": "Description here"}
                    ],
                    "dcat:keyword": ["keyword1"],
                    "dcat:distribution": [],
                    "odrl:hasPolicy": {
                        "@type": "Policy",
                        "odrl:providerId": "provider1",
                        "odrl:permission": [
                          {
                            "@type": "Permission",
                            "odrl:action": "read"
                          }
                        ]
                    }
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
    assertEquals("read", entry.getPolicy().getPermissions().get(0).getAction());
}


    @Test
    void retrieveCapabilitiesInformation_shouldThrowIOExceptionIfEmpty() {
        String json = """
                { "data": { "dcat:dataset": [] } }
                """;

        assertThrows(IOException.class,
                () -> capabilityService.retrieveCapabilitiesInformation(json));
    }
@Test
void parseAASCapabilities_shouldParseCapabilityEntries() throws IOException {
String json = """
{
  "result": [
    {
      "idShort": "MillingContainer",
      "modelType": "SubmodelElementCollection",
      "value": [
        {
          "idShort": "Milling",
          "modelType": "Capability",
          "qualifiers": [
            {
              "type": "Offered",
              "value": "true"
            },
            {
              "type": "CapabilityType",
              "value": "Primary"
            }
          ],
         "value": [
              {
              "modelType": "Capability",
              "semanticId": {
                "keys": [
                  {
                    "type": "GlobalReference",
                    "value": "https://admin-shell.io/idta/CapabilityDescription/Capability/1/0"
                  }
                ],
                "type": "ExternalReference"
              },
              "supplementalSemanticIds": [
                {
                  "keys": [
                    {
                      "type": "GlobalReference",
                      "value": "http://www.semanticweb.org/mar94321/ontologies/2025/3/CAPT4M#Milling"
                    }
                  ],
                  "type": "ExternalReference"
                }
              ],
              "qualifiers": [
                {
                  "semanticId": {
                    "keys": [
                      {
                        "type": "GlobalReference",
                        "value": "https://admin-shell.io/idta/CapabilityDescription/CapabilityRoleQualifiers/Offered/1/0"
                      }
                    ],
                    "type": "ExternalReference"
                  },
                  "kind": "ValueQualifier",
                  "type": "Offered",
                  "value": "true",
                  "valueType": "xs:boolean"
                },
                {
                  "kind": "ConceptQualifier",
                  "type": "CapabilityType",
                  "value": "Primary",
                  "valueType": "xs:string"
                }
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
    assertEquals("Milling", entry.getName());     // matches idShort in JSON
    assertEquals("Primary", entry.getType());     // parsed from qualifier
    assertTrue(entry.isOffered());                // parsed from qualifier

    // No comment in JSON, so should be null
    assertNull(entry.getComment());

    // No properties in JSON, so should be empty
    assertTrue(entry.getProperties().isEmpty());
}


@Test
void parseAASCapabilities_shouldParseCapabilityEntriesWithProperties() throws IOException {
String json = """
{
  "result": [
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
        CapabilityEntry entry = entries.get(0);

        assertEquals("MillingPropertySet", entry.getName());
        assertEquals(entry.getProperties().size(),1);
        assertEquals("NumberOfAxes", entry.getProperties().get(0).getName());
    }

    @Test
    void parseAASCapabilities_shouldHandleInvalidJson() {
        String invalidJson = "{ not valid json }";
        assertThrows(IOException.class, () -> capabilityService.parseAASCapabilities(invalidJson));
    }

    @Test
    void parseDatasets_shouldHandleInvalidJson() {
        String invalidJson = "{ invalid }";
        assertThrows(IOException.class, () -> capabilityService.parseDatasets(invalidJson));
    }


    @Test
    void parseDatasets_shouldTestTeknikerOutcome() throws IOException {
          String json = Files.readString(Paths.get("src/test/resources/DSTecnikerResponse.json"));
                  List<CapabilityEntry> entries = capabilityService.parseAASCapabilities(json);
                        CapabilityEntry entry = entries.get(0);

        assertEquals("Milling", entry.getName());
        assertEquals(11,entry.getProperties().size());
        assertEquals("machining operation which consists of removing material by means of a rotary tool called a \"milling cutter\" of which there are several different types. Note 1 to entry: The typical milling operations mostly involve face milling or end milling. The tools are mounted either in the spindle taper or on the spindle front face. (ISO 8636-1:2000)", entry.getComment());
        assertEquals("Primary",entry.getType());

        assertEquals("NumberOfAxis", entry.getProperties().get(0).getName());
        assertEquals("5", entry.getProperties().get(0).getValue());
        assertEquals("xs:int", entry.getProperties().get(0).getValueType());
        assertEquals("Number of axes. It refers to the degrees of freedom or directions in which the machine can move its tool or workpiece.", entry.getProperties().get(0).getComment());




    }
}
