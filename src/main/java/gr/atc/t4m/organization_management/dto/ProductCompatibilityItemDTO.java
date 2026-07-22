package gr.atc.t4m.organization_management.dto;

import lombok.Data;

@Data
public class ProductCompatibilityItemDTO {
    private String product;
    private Double processingTime;

    public ProductCompatibilityItemDTO(String product) {
        this.product = product;
    }
}
