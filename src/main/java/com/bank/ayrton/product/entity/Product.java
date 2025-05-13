package com.bank.ayrton.product.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String type;      //activo, pasivo
    private ProductSubtype subtype;   //ahorro, cuenta_corriente, plazo_fijo...(en ingles)
    private String clientId;  //id del cliente
    private Double maintenanceFee;         //para cuenta corriente
    private Integer monthlyMovementLimit;  //para cuenta ahorro
    private Integer allowedMovementDay;    //para cuenta plazo fijo
    private Double creditLimit;            //para créditos y tarjetas
    private List<String> holders;              //titulares (empresas)
    private List<String> authorizedSignatories; //firmantes autorizados

}
