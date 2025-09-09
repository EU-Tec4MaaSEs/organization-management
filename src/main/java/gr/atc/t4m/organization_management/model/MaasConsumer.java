package gr.atc.t4m.organization_management.model;
import java.util.List;
import lombok.Data;

@Data
public class MaasConsumer {
    private double paymentRating;
    private List<String> importOrigins;
}