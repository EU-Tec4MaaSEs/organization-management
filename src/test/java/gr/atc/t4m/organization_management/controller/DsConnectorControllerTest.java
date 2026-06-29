package gr.atc.t4m.organization_management.controller;

import gr.atc.t4m.organization_management.config.TestSecurityConfig;
import gr.atc.t4m.organization_management.dto.CatalogDTO;
import gr.atc.t4m.organization_management.exception.OrganizationNotFoundException;
import gr.atc.t4m.organization_management.model.ManufacturingResource;
import gr.atc.t4m.organization_management.model.Organization;
import gr.atc.t4m.organization_management.service.DsConnectorService;
import gr.atc.t4m.organization_management.service.ManufacturingResourceService;
import gr.atc.t4m.organization_management.service.OrganizationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(DsConnectorController.class)
@Import(TestSecurityConfig.class)

class DsConnectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DsConnectorService dsConnectorService;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private ManufacturingResourceService manufacturingResourceService;

     @Autowired
    private ObjectMapper objectMapper;

    private static final String CALENDAR_ID = "e02c1df3-c3fc-4945-84c4-7e52b327ac6d";
    private static final String ICAL_CONTENT = "BEGIN:VCALENDAR\nVERSION:2.0\nBEGIN:VEVENT\nSUMMARY:Available\nEND:VEVENT";
    @Test
    @DisplayName("validateOrganization should return 200 when service validates organization successfully")
    void validateOrganization_ok() throws Exception {
        Mockito.when(dsConnectorService.validateOrganization(any(CatalogDTO.class)))
                .thenReturn(HttpStatus.OK);

        String jsonBody = """
                {
                  "page": 1,
                  "size": 10,
                  "refresh": false,
                  "title": "FA3ST Catalog",
                  "providerUrl": "https://example.org/api"
                }
                """;

        mockMvc.perform(post("/api/connector/validateOrganization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("validateOrganization should return 204 when service returns NO_CONTENT")
    void validateOrganization_noContent() throws Exception {
        Mockito.when(dsConnectorService.validateOrganization(any(CatalogDTO.class)))
                .thenReturn(HttpStatus.NO_CONTENT);

        String jsonBody = """
                {
                  "page": 1,
                  "size": 10,
                  "refresh": false,
                  "title": "FA3ST Catalog",
                  "providerUrl": "https://test/api/dsp/v1/server"
                }
                """;

        mockMvc.perform(post("/api/connector/validateOrganization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isNoContent());
    }



    @Test
    void shouldReturn200_whenCapabilitiesRetrievedSuccessfully() throws Exception {
        // given
        CatalogDTO catalogDTO = new CatalogDTO();
        catalogDTO.setOrganizationName("TestOrg");
        catalogDTO.setProviderUrl("http://ds.connector");

        Organization org = new Organization();
        org.setOrganizationName("TestOrg");
        org.setDsConnectorURL("http://ds.connector");

        ManufacturingResource mr = new ManufacturingResource();
        mr.setManufacturingResourceID("res1");
        mr.setManufacturingResourceTitle("Test Resource");

        when(organizationService.getOrganizationByName("TestOrg")).thenReturn(org);
        when(dsConnectorService.retrieveUnifiedResources(any(CatalogDTO.class))).thenReturn(List.of(mr));

        doNothing().when(manufacturingResourceService).save(any(ManufacturingResource.class));

        mockMvc.perform(post("/api/connector/retrieveCapabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catalogDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].manufacturingResourceTitle").value("Test Resource"));    }

    @Test
    void shouldReturn400_whenMissingOrganizationNameOrUrl() throws Exception {
        CatalogDTO catalogDTO = new CatalogDTO();
        catalogDTO.setOrganizationName(null); // missing
        catalogDTO.setProviderUrl(null); // missing

      mockMvc.perform(post("/api/connector/retrieveCapabilities")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(catalogDTO)))
    .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenUrlDiffersFromOrganization() throws Exception {
        CatalogDTO catalogDTO = new CatalogDTO();
        catalogDTO.setOrganizationName("TestOrg");
        catalogDTO.setProviderUrl("http://wrong.url");

        Organization org = new Organization();
        org.setOrganizationName("TestOrg");
        org.setDsConnectorURL("http://ds.connector");

        when(organizationService.getOrganizationByName("TestOrg")).thenReturn(org);

        mockMvc.perform(post("/api/connector/retrieveCapabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catalogDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404_whenOrganizationNotFound() throws Exception {
        CatalogDTO catalogDTO = new CatalogDTO();
        catalogDTO.setOrganizationName("UnknownOrg");
        catalogDTO.setProviderUrl("http://ds.connector");

         when(organizationService.getOrganizationByName("UnknownOrg"))
        .thenThrow(new OrganizationNotFoundException("Organization with name UnknownOrg not found"));

        mockMvc.perform(post("/api/connector/retrieveCapabilities")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(catalogDTO)))
               .andExpect(status().isNotFound());

    }

    @Test
    void getRawCalendarContext_Returns200AndRawContent() throws Exception {
        when(dsConnectorService.fetchCalendarContent(CALENDAR_ID)).thenReturn(ICAL_CONTENT);
        doNothing().when(manufacturingResourceService).updateRawCalendarContent(CALENDAR_ID, ICAL_CONTENT);

        mockMvc.perform(get("/api/connector/getCalendar/{calendarDatasetID}", CALENDAR_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(ICAL_CONTENT));

        verify(dsConnectorService, times(1)).fetchCalendarContent(CALENDAR_ID);
        verify(manufacturingResourceService, times(1)).updateRawCalendarContent(CALENDAR_ID, ICAL_CONTENT);
    }


    @Test
    void getRawCalendarContext_EmptyId_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/connector/getCalendar/{calendarDatasetID}", "   "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(dsConnectorService);
        verifyNoInteractions(manufacturingResourceService);
    }


    @Test
    void getRawCalendarContext_UnknownDatasetId_Returns404NotFound() throws Exception {
        // Arrange
        when(dsConnectorService.fetchCalendarContent(CALENDAR_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset ID unknown"));

        mockMvc.perform(get("/api/connector/getCalendar/{calendarDatasetID}", CALENDAR_ID))
                .andExpect(status().isNotFound());

        verify(manufacturingResourceService, never()).updateRawCalendarContent(anyString(), anyString());
    }


    @Test
    void getRawCalendarContext_ServiceThrowsGenericException_Returns500InternalServerError() throws Exception {
        when(dsConnectorService.fetchCalendarContent(CALENDAR_ID))
                .thenThrow(new RuntimeException("Connection timeout over active data plane channel"));

        mockMvc.perform(get("/api/connector/getCalendar/{calendarDatasetID}", CALENDAR_ID))
                .andExpect(status().isInternalServerError());

        verify(manufacturingResourceService, never()).updateRawCalendarContent(anyString(), anyString());
    }
 
}
    


