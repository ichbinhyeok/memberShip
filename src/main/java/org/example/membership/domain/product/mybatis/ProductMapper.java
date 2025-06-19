package org.example.membership.domain.product.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.membership.domain.product.Product;

import java.util.List;

@Mapper
public interface ProductMapper {
    void insert(Product product);
    Product findById(@Param("id") Long id);
    List<Product> findAll();
    void update(Product product);
    void deleteById(@Param("id") Long id);
}