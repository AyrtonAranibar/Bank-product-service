package com.bank.ayrton.product.service.product;

import com.bank.ayrton.product.api.product.ProductService;
import com.bank.ayrton.product.entity.Product;
import com.bank.ayrton.product.api.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementación del servicio de productos.
 * Proporciona operaciones CRUD reactivas para productos bancarios.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    @Override
    public Flux<Product> findAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Product> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Mono<Product> save(Product product) {
        return repository.save(product);
    }

    @Override
    public Mono<Product> update(String id, Product product) {
        return repository.findById(id)
                .flatMap(existing -> {
                    product.setId(id); // asegura mantener el ID original
                    return repository.save(product);
                });
    }

    @Override
    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }
}