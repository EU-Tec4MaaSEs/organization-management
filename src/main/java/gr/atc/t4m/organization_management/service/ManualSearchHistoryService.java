package gr.atc.t4m.organization_management.service;

import gr.atc.t4m.organization_management.model.ManualSearchHistory;
import gr.atc.t4m.organization_management.repository.ManualSearchHistoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor 
public class ManualSearchHistoryService {

    private final ManualSearchHistoryRepository historyRepository;

    public void recordSearch(String userId, List<String> countries, List<String> services) {
        ManualSearchHistory history = ManualSearchHistory.builder()
                .userId(userId)
                .countryCodes(countries)
                .manufacturingServices(services)
                .searchedAt(LocalDateTime.now(ZoneOffset.UTC)) 
                .build();

        historyRepository.save(history);
    }

public Page<ManualSearchHistory> getUserSearchHistory(String userId, Pageable pageable) {
    return historyRepository.findByUserId(userId, pageable);
}


@Transactional
    public void clearUserSearchHistory(String userId) {
        historyRepository.deleteByUserId(userId);
    }


 @Transactional
public void deleteHistoryEntry(String id, String userId) {
    ManualSearchHistory history = historyRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History record not found with id: " + id));

    if (!history.getUserId().equals(userId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this history record.");
    }

    historyRepository.delete(history);
}
}