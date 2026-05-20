
package gr.atc.t4m.organization_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAnalyticsDTO {
    
    private double averageRating;
    private long totalReviews;
    
    private long stars1;
    private long stars2;
    private long stars3;
    private long stars4;
    private long stars5;
}