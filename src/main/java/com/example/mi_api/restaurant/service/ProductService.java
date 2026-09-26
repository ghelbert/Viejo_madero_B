package com.example.mi_api.restaurant.service;

import com.example.mi_api.restaurant.dto.product.ProductRequest;
import com.example.mi_api.restaurant.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Map<String, Object>> findAvailable() {
        return productRepository.findAvailable();
    }

    public List<Map<String, Object>> findAll() {
        return productRepository.findAll();
    }

    public List<Map<String, Object>> findCategories() {
        return productRepository.findCategories();
    }

    public Map<String, Object> create(ProductRequest request) {
        var id = productRepository.create(request);
        return Map.of("id", id, "name", request.name());
    }

    public void update(long id, ProductRequest request) {
        productRepository.update(id, request);
    }
}
