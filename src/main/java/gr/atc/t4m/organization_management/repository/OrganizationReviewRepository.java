package gr.atc.t4m.organization_management.repository;

import gr.atc.t4m.organization_management.model.OrganizationReview;
import gr.atc.t4m.organization_management.model.MaasRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface OrganizationReviewRepository extends MongoRepository<OrganizationReview, String> {

    List<OrganizationReview> findByTargetOrganizationIdAndTargetRoleOrderByUpdatedAtDesc(
            String targetOrganizationId, 
            MaasRole targetRole
    );

    @Aggregation(pipeline = {
        "{ '$match': { 'targetOrganizationId': ?0, 'targetRole': ?1 } }",
        "{ '$group': { '_id': '$rating', 'count': { '$sum': 1 } } }"
    })
    List<Map<String, Object>> getStarCountDistribution(String targetOrganizationId, String name);

    Page<OrganizationReview> findByReviewerOrganizationIdOrderByCreatedAtDesc(String reviewerOrgId, Pageable pageable);

    Page<OrganizationReview> findByReviewerOrganizationIdAndTargetOrganizationIdOrderByCreatedAtDesc(
            String reviewerOrgId, String targetOrganizationId, Pageable pageable);

    Page<OrganizationReview> findByTargetOrganizationIdAndTargetRole(String orgId, MaasRole role, Pageable pageable);
}