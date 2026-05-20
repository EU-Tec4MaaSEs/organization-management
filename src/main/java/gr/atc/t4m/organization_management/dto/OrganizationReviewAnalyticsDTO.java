package gr.atc.t4m.organization_management.dto;

import gr.atc.t4m.organization_management.model.OrganizationReview;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationReviewAnalyticsDTO {
    private ReviewAnalyticsDTO providerAnalytics;
    private List<OrganizationReview> providerReviews;
    
    private ReviewAnalyticsDTO consumerAnalytics;
    private List<OrganizationReview> consumerReviews;
}
