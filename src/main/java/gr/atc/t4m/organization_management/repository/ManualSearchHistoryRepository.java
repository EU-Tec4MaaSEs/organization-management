package gr.atc.t4m.organization_management.repository;

import gr.atc.t4m.organization_management.model.ManualSearchHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ManualSearchHistoryRepository extends MongoRepository<ManualSearchHistory, String> {
    
    List<ManualSearchHistory> findByUserIdOrderBySearchedAtDesc(String userId);
    Page<ManualSearchHistory> findByUserId(String userId, Pageable pageable);
    void deleteByUserId(String userId);
    void deleteByIdAndUserId(String id, String userId);

}