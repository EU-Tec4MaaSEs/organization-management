package gr.atc.t4m.organization_management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import gr.atc.t4m.organization_management.validation.ValidPriority;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event Object Representation", title = "Event")
public class EventDto {
    @JsonProperty("eventId")
    private String id;

    @JsonProperty("description")
    private String description;

    @NotEmpty(message = "Production Module cannot be empty")
    @JsonProperty("productionModule")
    private String productionModule;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @NotNull(message = "Priority cannot be empty")
    @ValidPriority
    @JsonProperty("priority")
    private String priority;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @NotNull(message = "Topic cannot be empty")
    @JsonProperty("topic")
    private String topic;

    @JsonProperty("results")
    private JsonNode results;
}
