package com.bank.ayrton.product.controller;

import com.bank.ayrton.product.api.product.ProductService;
import com.bank.ayrton.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor // crea los constructores con la inyeccion de dependencias
public class ProductController {

    private final ProductService service;

    //obtiene todos los productos
    @GetMapping
    public Flux<Product> findAll() {
        return service.findAll();
    }

    //obtiene un producto por ID
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> findById(@PathVariable String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    //crea un nuevo producto
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> save(@RequestBody Product product) {
        return service.save(product);
    }

    //actualiza un producto por ID

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> update(@PathVariable String id, @RequestBody Product product) {
        return service.update(id, product)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    //elimina un producto por ID
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    // el sistema permite consultar todos los productos que tiene un cliente
    @GetMapping("/client/{clientId}")
    public Flux<Product> findByClientId(@PathVariable String clientId) {
        return service.findByClientId(clientId);
    }
}