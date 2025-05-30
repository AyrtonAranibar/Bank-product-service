package com.bank.ayrton.product.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "debit_cards")
public class DebitCard {

    @Id
    private String id;

    private String clientId; // cliente dueño de la tarjeta

    private String mainAccountId; // ID de la cuenta principal

    private List<String> linkedAccountIds; // Cuentas asociadas en orden
}