package com.bank.ayrton.product.api.product;

import com.bank.ayrton.product.dto.DebitCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface DebitCardRepository extends ReactiveMongoRepository<DebitCard, String> {
}