package org.example.membership.service.jpa;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Product;
import org.example.membership.repository.jpa.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JpaProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}