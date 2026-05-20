package gr.atc.t4m.organization_management.dto;

import gr.atc.t4m.organization_management.model.OrganizationReview;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Page;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationReviewsResponseDTO {
    private ReviewAnalyticsDTO providerAnalytics;
    private ReviewAnalyticsDTO consumerAnalytics;
    
    private Page<OrganizationReview> reviews;
}