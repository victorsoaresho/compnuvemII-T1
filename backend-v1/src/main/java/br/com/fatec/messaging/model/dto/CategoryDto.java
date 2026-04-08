package br.com.fatec.messaging.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CategoryDto {
    private String id;
    private String name;

    @JsonProperty("sub_category")
    private SubCategoryDto subCategory;
}
