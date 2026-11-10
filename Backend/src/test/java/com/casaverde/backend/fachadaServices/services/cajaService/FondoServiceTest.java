package com.casaverde.backend.fachadaServices.services.cajaService;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ConfiguracionGeneralRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.EntradaFondoRequest;
import com.casaverde.backend.fachadaServices.dto.FondoDTO;
import com.casaverde.backend.fachadaServices.dto.TransferenciaFondoRequest;
import com.casaverde.backend.fachadaServices.mapper.FondoMapper;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FondoServiceTest {

    @Mock private FondoRepository fondoRepo;
    @Mock private CajaMovimientoRepository cajaMovimientoRepo;
    @Mock private ConfiguracionGeneralRepository configRepo;
    @Mock private ICajaMovimientoService cajaMovimientoService;
    @Mock private IGastoService gastoService;
    @Mock private IMonetaryService monetaryService;
    @Mock private IAuditoriaService auditoriaService;
    @Mock private FondoMapper fondoMapper;

    @InjectMocks
    private FondoServiceImpl service;

    // -----------------------------------------------------------------------
    // ajustar
    // -----------------------------------------------------------------------

    @Test
    void ajustar_conNuevoSaldoMayor_registraMovimientoPositivo() {
        Fondo fondo = new Fondo();
        fondo.setSaldo(new BigDecimal("50000"));

        when(monetaryService.normalizarMoneda(MonedaConstants.COP)).thenReturn(MonedaConstants.COP);
        when(monetaryService.normalizarSaldoFondo(MonedaConstants.COP, new BigDecimal("80000")))
                .thenReturn(new BigDecimal("80000"));
        when(fondoRepo.findById(MonedaConstants.COP)).thenReturn(Optional.of(fondo));
        when(fondoMapper.toDTO(any())).thenReturn(new FondoDTO());

        AjusteCapitalRequest req = new AjusteCapitalRequest(new BigDecimal("80000"), null);

        service.ajustar(MonedaConstants.COP, req);

        verify(cajaMovimientoService).registrar(
                eq(CajaMovimientoTipo.AJUSTE_FONDO),
                eq(MonedaConstants.COP),
                eq(new BigDecimal("30000")),
                isNull(),
                eq("fondo"),
                isNull(),
                contains("Ajuste manual"),
                isNull());
    }

    @Test
    void ajustar_conSaldoNegativo_lanzaValidacionException() {
        when(monetaryService.normalizarMoneda(MonedaConstants.COP)).thenReturn(MonedaConstants.COP);
        when(monetaryService.normalizarSaldoFondo(eq(MonedaConstants.COP), any()))
                .thenReturn(new BigDecimal("-1"));

        AjusteCapitalRequest req = new AjusteCapitalRequest(new BigDecimal("-1"), null);

        assertThrows(ValidacionException.class, () -> service.ajustar(MonedaConstants.COP, req));
        verifyNoInteractions(cajaMovimientoService);
    }

    // -----------------------------------------------------------------------
    // registrarEntrada
    // -----------------------------------------------------------------------

    @Test
    void registrarEntrada_conDatosValidos_registraMovimientoCaja() {
        when(monetaryService.normalizarMoneda(MonedaConstants.COP)).thenReturn(MonedaConstants.COP);
        when(fondoRepo.findById(MonedaConstants.COP)).thenReturn(Optional.of(new Fondo()));
        when(fondoMapper.toDTO(any())).thenReturn(new FondoDTO());

        EntradaFondoRequest req = new EntradaFondoRequest(
                MonedaConstants.COP, new BigDecimal("10000"), "Capital inicial", null);

        service.registrarEntrada(req);

        verify(cajaMovimientoService).registrar(
                eq(CajaMovimientoTipo.ENTRADA_MANUAL),
                eq(MonedaConstants.COP),
                eq(new BigDecimal("10000")),
                isNull(),
                isNull(),
                isNull(),
                eq("Capital inicial"),
                isNull());
    }

    @Test
    void registrarEntrada_sinConcepto_lanzaValidacionException() {
        EntradaFondoRequest req = new EntradaFondoRequest(
                MonedaConstants.COP, new BigDecimal("10000"), "  ", null);

        assertThrows(ValidacionException.class, () -> service.registrarEntrada(req));
        verifyNoInteractions(cajaMovimientoService);
    }

    // -----------------------------------------------------------------------
    // registrarTransferencia
    // -----------------------------------------------------------------------

    @Test
    void registrarTransferencia_mismoOrigenYDestino_lanzaValidacionException() {
        when(monetaryService.normalizarMoneda(MonedaConstants.COP)).thenReturn(MonedaConstants.COP);

        TransferenciaFondoRequest req = new TransferenciaFondoRequest(
                MonedaConstants.COP,
                new BigDecimal("10000"),
                MonedaConstants.COP,
                new BigDecimal("10000"),
                null,
                null,
                null);

        assertThrows(ValidacionException.class, () -> service.registrarTransferencia(req));
        verifyNoInteractions(cajaMovimientoService);
    }
}
