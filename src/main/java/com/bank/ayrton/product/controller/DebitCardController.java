package com.bank.ayrton.product.controller;

import com.bank.ayrton.product.api.product.DebitCardRepository;
import com.bank.ayrton.product.api.product.ProductRepository;
import com.bank.ayrton.product.dto.DebitCard;
import com.bank.ayrton.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/debit-card")
@RequiredArgsConstructor
@Slf4j
public class DebitCardController {

    private final DebitCardRepository repository;
    private final ProductRepository productRepository;

    @PostMapping
    public Mono<ResponseEntity<DebitCard>> create(@RequestBody DebitCard card) {
        log.info("Creando tarjeta de débito: {}", card);
        return repository.save(card)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DebitCard>> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<DebitCard> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}/main-account-balance")
    public Mono<ResponseEntity<Double>> getMainAccountBalance(@PathVariable String id) {
        return repository.findById(id)
                .flatMap(card -> productRepository.findById(card.getMainAccountId())
                        .map(Product::getBalance)
                        .map(ResponseEntity::ok)
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}