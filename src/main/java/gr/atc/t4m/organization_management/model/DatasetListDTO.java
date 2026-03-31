package gr.atc.t4m.organization_management.model;

import java.util.List;

import lombok.Data;

@Data
public class DatasetListDTO {
    private int page;
    private int size;
    private int totalElements;
    private int totalPages;
    private List<String> data;
}