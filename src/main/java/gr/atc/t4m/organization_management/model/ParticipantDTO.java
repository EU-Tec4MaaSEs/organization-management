package gr.atc.t4m.organization_management.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDTO {
    

    @JsonProperty("@id")
    private String id;

    private String role;

    private String participantId;

    private OffsetDateTime activeSince;


}