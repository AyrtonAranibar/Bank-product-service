package com.bank.ayrton.product.api.product;

import com.bank.ayrton.product.entity.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {

    Flux<Product> findAll();
    Mono<Product> findById(String id);
    Mono<Product> save(Product product);
    Mono<Product> update(String id, Product product);
    Mono<Void> delete(String id);
    Flux<Product> findByClientId(String id);
}
