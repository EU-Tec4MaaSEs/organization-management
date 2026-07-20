package gr.atc.t4m.organization_management.model;
import java.util.List;
import lombok.Data;

@Data
public class MaasConsumer {
    private double consumerRating;

    private List<String> importOrigins;
}