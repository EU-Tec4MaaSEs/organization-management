package gr.atc.t4m.organization_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateFileInformationDTO(
        @NotBlank(message = "Attachment title cannot be empty")
        String title,

        @NotNull(message = "Visibility flag must be specified")
        Boolean isPublic
) {}