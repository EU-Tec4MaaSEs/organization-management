package gr.atc.t4m.organization_management.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "organization_reviews")
public class OrganizationReview {

    @Id
    private String id;
    private String targetOrganizationId;
    private String targetOrganizationName;
    
    private String reviewerOrganizationId;
    private String reviewerOrganizationName;

    private String reviewerUserId; 

    private Integer rating;
    private String comment;

    private MaasRole targetRole;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

}