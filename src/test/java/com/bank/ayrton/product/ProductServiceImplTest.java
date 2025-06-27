package com.bank.ayrton.product;

import com.bank.ayrton.product.api.product.ProductRepository;
import com.bank.ayrton.product.dto.ClientDto;
import com.bank.ayrton.product.dto.ClientSubtype;
import com.bank.ayrton.product.entity.Product;
import com.bank.ayrton.product.entity.ProductSubtype;
import com.bank.ayrton.product.service.product.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    /**  Deep-stubs para encadenar get()→uri()→retrieve()… */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient clientWebClient;

    @InjectMocks
    private ProductServiceImpl productService;

    /* ---------------------------------------------------------
       UTILIDADES
       --------------------------------------------------------- */

    /**
     * Mockea la llamada GET /api/v1/client/{id} devolviendo
     * un ClientDto concreto.
     */
    @SuppressWarnings("unchecked")
    private void mockClientService(String clientId, ClientDto dto) {
        when(clientWebClient.get()
                .uri("/api/v1/client/" + clientId)
                .retrieve()
                .bodyToMono(ClientDto.class))
                .thenReturn(Mono.just(dto));
    }

    /* ---------------------------------------------------------
       TESTS BÁSICOS findAll / findById
       --------------------------------------------------------- */

    @Test
    void findAll_shouldReturnAllProducts() {
        Product p1 = new Product(); p1.setId("1");
        Product p2 = new Product(); p2.setId("2");

        when(repository.findAll()).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(productService.findAll())
                .expectNext(p1).expectNext(p2).verifyComplete();

        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnProductById() {
        Product p = new Product(); p.setId("abc123");

        when(repository.findById("abc123")).thenReturn(Mono.just(p));

        StepVerifier.create(productService.findById("abc123"))
                .expectNext(p).verifyComplete();

        verify(repository).findById("abc123");
    }

    /* ---------------------------------------------------------
       VALIDACIONES save(...)
       --------------------------------------------------------- */

    @Test
    void save_shouldRejectWhenHasOverdueDebt() {
        Product req = new Product();
        req.setClientId("cli1"); req.setType("pasivo");

        Product overdue = new Product();
        overdue.setType("activo"); overdue.setStatus("vencido");

        when(repository.findByClientId("cli1")).thenReturn(Flux.just(overdue));

        StepVerifier.create(productService.save(req))
                .expectErrorSatisfies(e -> {
                    assertInstanceOf(ResponseStatusException.class, e);
                    assertEquals(HttpStatus.BAD_REQUEST,
                            ((ResponseStatusException) e).getStatusCode());
                })
                .verify();

        verify(repository).findByClientId("cli1");
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(clientWebClient);
    }

    @Test
    void save_shouldRejectWhenBalanceIsNegative() {
        Product req = new Product();
        req.setClientId("cli-neg"); req.setType("pasivo");
        req.setSubtype(ProductSubtype.SAVINGS); req.setBalance(-10.0);

        ClientDto cli = new ClientDto();
        cli.setId("cli-neg"); cli.setType("personal");
        cli.setSubtype(ClientSubtype.STANDARD);

        mockClientService("cli-neg", cli);
        when(repository.findByClientId("cli-neg")).thenReturn(Flux.empty());

        StepVerifier.create(productService.save(req))
                .expectErrorSatisfies(e ->
                        assertTrue(((ResponseStatusException) e)
                                .getReason().contains("negativo")))
                .verify();
    }

    @Test
    void save_shouldRejectPYMEWithoutCreditCard() {
        Product req = new Product();
        req.setClientId("emp-pyme"); req.setType("pasivo");
        req.setSubtype(ProductSubtype.CURRENT_ACCOUNT);

        ClientDto pyme = new ClientDto();
        pyme.setId("emp-pyme"); pyme.setType("empresarial");
        pyme.setSubtype(ClientSubtype.PYME);

        mockClientService("emp-pyme", pyme);

        // 1ª llamada: busca deudas (none), 2ª: busca tarjeta (none)
        when(repository.findByClientId("emp-pyme"))
                .thenReturn(Flux.empty())
                .thenReturn(Flux.empty());

        StepVerifier.create(productService.save(req))
                .expectErrorSatisfies(e ->
                        assertTrue(((ResponseStatusException) e)
                                .getReason().contains("tarjeta")))
                .verify();
    }

    @Test
    void save_shouldRejectVIPWithoutCreditCard() {
        Product req = new Product();
        req.setClientId("vip-1"); req.setType("pasivo");
        req.setSubtype(ProductSubtype.SAVINGS);

        ClientDto vip = new ClientDto();
        vip.setId("vip-1"); vip.setType("personal");
        vip.setSubtype(ClientSubtype.VIP);

        mockClientService("vip-1", vip);

        when(repository.findByClientId("vip-1"))
                .thenReturn(Flux.empty())   // deudas
                .thenReturn(Flux.empty());  // busca TC

        StepVerifier.create(productService.save(req))
                .expectErrorSatisfies(e ->
                        assertTrue(((ResponseStatusException) e)
                                .getReason().contains("tarjeta")))
                .verify();
    }

    @Test
    void save_shouldRejectCompanyWithSavingsOrFixedTerm() {
        Product req = new Product();
        req.setClientId("emp-bad"); req.setType("pasivo");
        req.setSubtype(ProductSubtype.FIXED_TERM);

        ClientDto emp = new ClientDto();
        emp.setId("emp-bad"); emp.setType("empresarial");

        mockClientService("emp-bad", emp);
        when(repository.findByClientId("emp-bad")).thenReturn(Flux.empty());

        StepVerifier.create(productService.save(req))
                .expectErrorSatisfies(e ->
                        assertTrue(((ResponseStatusException) e)
                                .getReason().contains("empresarial")))
                .verify();
    }

    @Test
    void save_shouldRejectDuplicatePassiveForPersonal() {
        Product req = new Product();
        req.setClientId("per-dup"); req.setType("pasivo");
        req.setSubtype(ProductSubtype.SAVINGS);

        Product existing = new Product();
        existing.setClientId("per-dup");
        existing.setSubtype(ProductSubtype.SAVINGS);

        ClientDto per = new ClientDto();
        per.setId("per-dup"); per.setType("personal");
        per.setSubtype(ClientSubtype.STANDARD);

        mockClientService("per-dup", per);

        when(repository.findByClientId("per-dup"))
                .thenReturn(Flux.empty())          // deudas
                .thenReturn(Flux.just(existing));  // existe SAVINGS

        StepVerifier.create(productService.save(req))
                .expectErrorSatisfies(e ->
                        assertTrue(((ResponseStatusException) e)
                                .getReason().contains("ya tiene")))
                .verify();
    }
    @Test
    void save_shouldSaveValidPersonalCredit() {
        Product req = new Product();
        req.setClientId("cli-credit");
        req.setType("activo");
        req.setSubtype(ProductSubtype.PERSONAL_CREDIT);

        ClientDto client = new ClientDto();
        client.setId("cli-credit");
        client.setType("personal");
        client.setSubtype(ClientSubtype.STANDARD);

        mockClientService("cli-credit", client);

        when(repository.findByClientId("cli-credit"))
                .thenReturn(Flux.empty()) // sin deudas
                .thenReturn(Flux.empty()); // sin crédito personal

        when(repository.save(any(Product.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(productService.save(req))
                .expectNextMatches(p -> p.getClientId().equals("cli-credit"))
                .verifyComplete();
    }

    @Test
    void update_shouldUpdateProduct() {
        Product existing = new Product();
        existing.setId("123");
        existing.setBalance(100.0);

        Product updated = new Product();
        updated.setBalance(500.0);

        when(repository.findById("123")).thenReturn(Mono.just(existing));
        when(repository.save(any(Product.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(productService.update("123", updated))
                .expectNextMatches(p -> p.getId().equals("123") && p.getBalance().equals(500.0))
                .verifyComplete();
    }

    @Test
    void delete_shouldDeleteProduct() {
        when(repository.deleteById("del-1")).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete("del-1"))
                .verifyComplete();

        verify(repository).deleteById("del-1");
    }
}
