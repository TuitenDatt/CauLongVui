package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.CategoryDTO;
import com.example.CauLongVui.entity.Category;
import com.example.CauLongVui.exception.BadRequestException;
import com.example.CauLongVui.exception.ResourceNotFoundException;
import com.example.CauLongVui.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategories(boolean includeInactive) {
        List<Category> categories = includeInactive
                ? categoryRepository.findAll()
                : categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        return categories.stream()
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        return CategoryDTO.fromEntity(getCategory(id));
    }

    public CategoryDTO createCategory(CategoryDTO dto) {
        String name = normalizeName(dto.getName());
        if (findCategoryByName(name).isPresent()) {
            throw new BadRequestException("Danh muc da ton tai: " + name);
        }

        String slug = uniqueSlug(slugify(name), null);
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .active(dto.getActive() == null || dto.getActive())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        return CategoryDTO.fromEntity(categoryRepository.save(category));
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = getCategory(id);
        String name = normalizeName(dto.getName());

        findCategoryByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Danh muc da ton tai: " + name);
                });

        category.setName(name);
        category.setSlug(uniqueSlug(slugify(name), id));
        if (dto.getActive() != null) {
            category.setActive(dto.getActive());
        }
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        return CategoryDTO.fromEntity(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        Category category = getCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + id));
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Ten danh muc khong duoc de trong");
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    private java.util.Optional<Category> findCategoryByName(String name) {
        String key = categoryKey(name);
        return categoryRepository.findAll().stream()
                .filter(category -> categoryKey(category.getName()).equals(key))
                .findFirst();
    }

    private String categoryKey(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String uniqueSlug(String baseSlug, Long currentId) {
        String slug = baseSlug.isBlank() ? "category" : baseSlug;
        String candidate = slug;
        int suffix = 2;
        while (true) {
            var existing = categoryRepository.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(currentId)) {
                return candidate;
            }
            candidate = slug + "-" + suffix++;
        }
    }

    public static String slugify(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
