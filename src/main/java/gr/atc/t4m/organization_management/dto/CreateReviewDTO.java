package gr.atc.t4m.organization_management.dto;

import gr.atc.t4m.organization_management.model.MaasRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewDTO {

    @NotNull(message = "Rating score is required")
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    private Integer rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    @NotNull(message = "Target role context is required")
    private MaasRole targetRole; // Replaced with MaasRole
}