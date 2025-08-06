package gr.atc.t4m.organization_management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import gr.atc.t4m.organization_management.validation.ValidPriority;
import jakarta.validation.constraints.NotNull;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event Object Representation", title = "Event")
public class EventDTO {
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    private OffsetDateTime timestamp;

    @NotNull(message = "Priority cannot be empty")
    @ValidPriority
    private String priority;
    private String type;
    private String sourceComponent;
    private JsonNode data;
    private String organization;
}
