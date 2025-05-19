package com.bank.ayrton.product.api.product;

import com.bank.ayrton.product.entity.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

// al heredar de ReactiveMongoRepository heredamos tambien metodos predefinidos para usar mongodb
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
    //aqui se pueden agregar metodos personalizados
    Flux<Product> findByClientId(String id);
}
