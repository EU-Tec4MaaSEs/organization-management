package gr.atc.t4m.organization_management.dto;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import gr.atc.t4m.organization_management.validation.ValidNotificationStatus;
import gr.atc.t4m.organization_management.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Notification Object Representation", title = "Notification")
public class NotificationDto{

    @JsonProperty("notificationId")
    private String id;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("notificationType")
    private String notificationType;

    @JsonProperty("relatedEvent")
    private String relatedEvent;

    @JsonProperty("relatedAssignment")
    private String relatedAssignment;

    @ValidNotificationStatus
    @JsonProperty("notificationStatus")
    private String notificationStatus;

    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @JsonProperty("description")
    private String description;

    @JsonProperty("productionModule")
    private String productionModule;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @ValidPriority
    @JsonProperty("priority")
    private String priority;
}
