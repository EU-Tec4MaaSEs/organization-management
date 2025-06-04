package gr.atc.t4m.organization_management.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProviderSearchDTO {
    private List<String> countryCodes;
    private List<String> manufacturingServices;
}
