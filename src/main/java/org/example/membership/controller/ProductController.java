package org.example.membership.controller;

import lombok.RequiredArgsConstructor;
import org.example.membership.entity.Product;
import org.example.membership.service.jpa.JpaProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {

    private final JpaProductService jpaProductService;

    @GetMapping("/list")
    public List<Product> list() {
        return jpaProductService.getAllProducts();
    }
}