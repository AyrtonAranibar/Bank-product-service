package com.bank.ayrton.product.service.product;

import com.bank.ayrton.product.api.product.ProductService;
import com.bank.ayrton.product.entity.Product;
import com.bank.ayrton.product.api.product.ProductRepository;
import com.bank.ayrton.product.entity.ProductSubtype;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


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
        //validaciones
        //clientes empresariales no pueden tener cuentas de ahorro o plazo fijo
        if (product.getType().equalsIgnoreCase("pasivo") &&
                (product.getSubtype() == ProductSubtype.SAVINGS ||
                        product.getSubtype() == ProductSubtype.FIXED_TERM)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente empresarial no puede tener cuentas de ahorro ni de plazo fijo"));
        }

        //un cliente personal solo puede tener una cuenta de ahorro, corriente y plazo fijo
        if (product.getType().equalsIgnoreCase("pasivo")) {
            return repository.findAll()
                    .filter(p -> p.getClientId().equals(product.getClientId()))
                    .filter(p -> p.getSubtype().name().equalsIgnoreCase(product.getSubtype().toString()))
                    .hasElements()
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Cliente personal ya tiene una cuenta de tipo " + product.getSubtype()));
                        }
                        return repository.save(product);
                    });
        }

        //solo un crédito personal por cliente
        if (product.getSubtype().name().equalsIgnoreCase("CREDIT_PERSONAL")) {
            return repository.findAll()
                    .filter(p -> p.getClientId().equals(product.getClientId()))
                    .filter(p -> p.getSubtype().name().equalsIgnoreCase("CREDIT_PERSONAL"))
                    .hasElements()
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Un cliente personal solo puede tener un crédito personal"));
                        }
                        return repository.save(product);
                    });
        }
        //guarda el producto
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