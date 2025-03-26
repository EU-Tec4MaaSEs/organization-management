package gr.atc.t4m.organization_management.model;

import lombok.Data;
import java.util.List;

@Data
public class MaasProvider {
    private double providerRating;
    private int minimumOrderQuantity;
    private int nominalRate;
    private String qualityStandard;
    private List<ShippingCountry> shippingCountries;
}
