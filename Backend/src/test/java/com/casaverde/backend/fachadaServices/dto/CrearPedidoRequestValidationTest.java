package com.casaverde.backend.fachadaServices.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CrearPedidoRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    @Test
    void nombreNuloGeneraViolacionConMensajeObligatorio() {
        CrearPedidoRequest req = new CrearPedidoRequest();
        req.setNombre(null);

        Set<ConstraintViolation<CrearPedidoRequest>> violaciones = VALIDATOR.validate(req);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("El nombre del cliente es obligatorio");
    }

    @Test
    void nombreVacioGeneraViolacionConMensajeObligatorio() {
        CrearPedidoRequest req = new CrearPedidoRequest();
        req.setNombre("");

        Set<ConstraintViolation<CrearPedidoRequest>> violaciones = VALIDATOR.validate(req);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("El nombre del cliente es obligatorio");
    }

    @Test
    void nombreSoloEspaciosGeneraViolacionConMensajeObligatorio() {
        CrearPedidoRequest req = new CrearPedidoRequest();
        req.setNombre("   ");

        Set<ConstraintViolation<CrearPedidoRequest>> violaciones = VALIDATOR.validate(req);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .contains("El nombre del cliente es obligatorio");
    }

    @Test
    void nombreValidoNoGeneraViolacionDeNombre() {
        CrearPedidoRequest req = new CrearPedidoRequest();
        req.setNombre("Juan");

        Set<ConstraintViolation<CrearPedidoRequest>> violaciones = VALIDATOR.validate(req);

        assertThat(violaciones)
                .extracting(ConstraintViolation::getMessage)
                .doesNotContain("El nombre del cliente es obligatorio");
    }
}
