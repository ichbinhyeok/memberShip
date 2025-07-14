package org.example.membership.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.membership.entity.Product;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String categoryName;

    public static ProductResponse fromEntity(Product p) {
        ProductResponse dto = new ProductResponse();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPrice(p.getPrice());
        dto.setCategoryName(p.getCategory().getName().name());// LAZY 초기화됨
        return dto;
    }
}
