package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private String imageUrl;
    private String category;
    private Long categoryId;

    public static ProductDTO fromEntity(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .category(product.getCategoryEntity() != null ? product.getCategoryEntity().getName() : product.getCategory())
                .categoryId(product.getCategoryEntity() != null ? product.getCategoryEntity().getId() : null)
                .build();
    }

    public Product toEntity() {
        return Product.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .stockQuantity(this.stockQuantity != null ? this.stockQuantity : 0)
                .imageUrl(this.imageUrl)
                .category(this.category)
                .build();
    }
}
