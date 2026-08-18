package gr.atc.t4m.organization_management.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record FileInformation(
        String id,

        @NotBlank(message = "Attachment title is required")
        String title,

        @NotBlank(message = "File URL is required")
        String fileUrl,

        boolean isPublic
) {
    public FileInformation(String title, String fileUrl, boolean isPublic) {
        this(UUID.randomUUID().toString(), title, fileUrl, isPublic);
    }

    public FileInformation {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("File URL cannot be blank");
        }
    }
}