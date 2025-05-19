package com.bank.ayrton.product.service.product;

import com.bank.ayrton.product.api.product.ProductService;
import com.bank.ayrton.product.dto.ClientDto;
import com.bank.ayrton.product.entity.Product;
import com.bank.ayrton.product.api.product.ProductRepository;
import com.bank.ayrton.product.entity.ProductSubtype;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final WebClient clientWebClient; //llamamos al cliente para verificar su tipo cuando haga falta

    @Override
    public Flux<Product> findAll() {
        log.info("Obteniendo todos los productos");
        return repository.findAll();
    }

    @Override
    public Mono<Product> findById(String id) {
        log.info("Buscando producto con ID: {}", id);
        return repository.findById(id);
    }

    @Override
    public Mono<Product> save(Product product) {
        log.info("Intentando guardar producto: {}", product);

        return clientWebClient.get()
                .uri("/api/v1/client/" + product.getClientId())
                .retrieve()
                .bodyToMono(ClientDto.class)
                .flatMap(client -> {
                    String clientType = client.getType().toLowerCase();
                    ProductSubtype subtype = product.getSubtype();

                    log.info("Cliente recibido para validación: {} (tipo: {})", client.getId(), clientType);

                    // Empresa no puede tener ahorro o plazo fijo
                    if (clientType.equals("empresarial") &&
                            (subtype == ProductSubtype.SAVINGS || subtype == ProductSubtype.FIXED_TERM)) {
                        log.warn("Cliente empresarial intentó registrar cuenta de tipo no permitido: {}", subtype);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cliente empresarial no puede tener cuentas de ahorro ni de plazo fijo"));
                    }

                    //  cliente personal solo una cuenta de cada tipo pasivo
                    if (clientType.equals("personal") && product.getType().equalsIgnoreCase("pasivo")) {
                        return repository.findByClientId(product.getClientId())
                                .filter(p -> p.getSubtype() == subtype)
                                .hasElements()
                                .flatMap(exists -> {
                                    if (exists) {
                                        log.warn("Cliente personal ya tiene una cuenta de tipo {}", subtype);
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Cliente personal ya tiene una cuenta de tipo " + subtype));
                                    }
                                    log.info("Guardando nuevo producto para cliente personal: {}", product.getClientId());
                                    return repository.save(product);
                                });
                    }

                    // Solo un credito personal por cliente
                    if (subtype == ProductSubtype.PERSONAL_CREDIT) {
                        return repository.findByClientId(product.getClientId())
                                .filter(p -> p.getSubtype() == ProductSubtype.PERSONAL_CREDIT)
                                .hasElements()
                                .flatMap(exists -> {
                                    if (exists) {
                                        log.warn("Cliente ya tiene un crédito personal");
                                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Un cliente personal solo puede tener un crédito personal"));
                                    }
                                    log.info("Guardando nuevo crédito personal para cliente: {}", product.getClientId());
                                    return repository.save(product);
                                });
                    }

                    // Si no hay restricciones aplicables, guardar
                    log.info("Guardando producto sin restricciones adicionales");
                    return repository.save(product);
                });
    }

    @Override
    public Mono<Product> update(String id, Product product) {
        log.info("Actualizando producto con ID: {}", id);
        return repository.findById(id)
                .flatMap(existing -> {
                    product.setId(id); // asegura mantener el ID original
                    return repository.save(product);
                });
    }

    @Override
    public Mono<Void> delete(String id) {
        log.info("Eliminando producto con ID: {}", id);
        return repository.deleteById(id);
    }

    @Override
    public Flux<Product> findByClientId(String id) {
        log.info("Buscando productos por clientId: {}", id);
        return repository.findByClientId(id);
    }
}