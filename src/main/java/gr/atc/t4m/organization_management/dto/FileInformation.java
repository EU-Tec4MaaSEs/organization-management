package gr.atc.t4m.organization_management.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record FileInformation(
        String id,

        @NotBlank(message = "Attachment title is required")
        String title,

        @NotBlank(message = "File URL is required")
        String fileUrl
) {
    public FileInformation(String title, String fileUrl) {
        this(UUID.randomUUID().toString(), title, fileUrl);
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