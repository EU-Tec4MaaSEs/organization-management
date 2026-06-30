package gr.atc.t4m.organization_management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "manual_search_history")
public class ManualSearchHistory {

    @Id
    private String id;
    private String userId;
    private List<String> countryCodes;
    private List<String> manufacturingServices;
    private LocalDateTime searchedAt;
}