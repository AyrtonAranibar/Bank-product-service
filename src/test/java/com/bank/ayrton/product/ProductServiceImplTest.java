package com.bank.ayrton.product;

import com.bank.ayrton.product.api.product.ProductRepository;
import com.bank.ayrton.product.entity.Product;
import com.bank.ayrton.product.service.product.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private WebClient clientWebClient;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void shouldFailToSaveIfClientHasOverdueDebt() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setClientId("client123");
        newProduct.setType("pasivo");

        Product existingCredit = new Product();
        existingCredit.setType("activo");
        existingCredit.setStatus("vencido");

        // Simula productos ya existentes con deuda vencida
        Mockito.when(repository.findByClientId("client123"))
                .thenReturn(Flux.just(existingCredit));

        // Act
        Mono<Product> result = service.save(newProduct);

        // Assert
        StepVerifier.create(result)
                .expectErrorMatches(error -> error instanceof ResponseStatusException &&
                        ((ResponseStatusException) error).getReason().contains("deudas vencidas"))
                .verify();
    }
}
