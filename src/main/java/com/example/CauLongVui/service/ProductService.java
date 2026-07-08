package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.ProductDTO;
import com.example.CauLongVui.entity.Category;
import com.example.CauLongVui.entity.Product;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.CategoryRepository;
import com.example.CauLongVui.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return ProductDTO.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = productDTO.toEntity();
        applyCategory(product, productDTO);
        return ProductDTO.fromEntity(productRepository.save(product));
    }

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        existing.setName(productDTO.getName());
        existing.setDescription(productDTO.getDescription());
        existing.setPrice(productDTO.getPrice());
        existing.setStockQuantity(productDTO.getStockQuantity());
        existing.setImageUrl(productDTO.getImageUrl());
        applyCategory(existing, productDTO);
        return ProductDTO.fromEntity(productRepository.save(existing));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id);
        }
        productRepository.deleteById(id);
    }

    private void applyCategory(Product product, ProductDTO productDTO) {
        Category category = resolveCategory(productDTO);
        product.setCategoryEntity(category);
        product.setCategory(category != null ? category.getName() : productDTO.getCategory());
    }

    private Category resolveCategory(ProductDTO productDTO) {
        if (productDTO.getCategoryId() != null) {
            return categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + productDTO.getCategoryId()));
        }
        if (productDTO.getCategory() == null || productDTO.getCategory().trim().isEmpty()) {
            return null;
        }
        String name = productDTO.getCategory().trim().replaceAll("\\s+", " ");
        return findCategoryByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .slug(uniqueSlug(CategoryService.slugify(name)))
                        .active(true)
                        .sortOrder(0)
                        .build()));
    }

    private java.util.Optional<Category> findCategoryByName(String name) {
        String key = categoryKey(name);
        return categoryRepository.findAll().stream()
                .filter(category -> categoryKey(category.getName()).equals(key))
                .findFirst();
    }

    private String categoryKey(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String uniqueSlug(String baseSlug) {
        String slug = baseSlug == null || baseSlug.isBlank() ? "category" : baseSlug;
        String candidate = slug;
        int suffix = 2;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + suffix++;
        }
        return candidate;
    }
}
