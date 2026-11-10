package com.casaverde.backend.fachadaServices.services.empleadoService;

import com.casaverde.backend.capaAccesoADatos.models.entitys.*;
import com.casaverde.backend.capaAccesoADatos.models.enums.*;
import com.casaverde.backend.capaAccesoADatos.repositories.*;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.*;
import com.casaverde.backend.fachadaServices.mapper.EmpleadoMapper;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock private EmpleadoRepository empleadoRepo;
    @Mock private AdelantoRepository adelantoRepo;
    @Mock private TrabajoDiaRepository trabajoDiaRepo;
    @Mock private PagoEmpleadoRepository pagoRepo;
    @Mock private VacacionRepository vacacionRepo;
    @Mock private ICajaMovimientoService cajaMovimientoService;
    @Mock private ITasaCambioService tasaCambioService;
    @Mock private IMonetaryService monetaryService;
    @Mock private IAuditoriaService auditoriaService;
    @Mock private EmpleadoMapper empleadoMapper;

    @InjectMocks
    private EmpleadoServiceImpl service;

    // ─── liquidar ────────────────────────────────────────────────────────────

    @Test
    void liquidar_quincenalSinAdelantos_netoIgualASalario() {
        Empleado emp = empleado(TipoEmpleado.QUINCENAL, new BigDecimal("800000"));
        when(empleadoRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(1L)).thenReturn(List.of());
        when(tasaCambioService.getValorVigente()).thenReturn(new BigDecimal("4200"));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(empleadoMapper.toPagoDTO(any())).thenReturn(new PagoEmpleadoDTO());

        PagoEmpleadoRequest req = new PagoEmpleadoRequest();
        req.setMoneda(MonedaConstants.COP);

        service.liquidar(1L, req);

        ArgumentCaptor<PagoEmpleado> captor = ArgumentCaptor.forClass(PagoEmpleado.class);
        verify(pagoRepo).save(captor.capture());
        assertEquals(new BigDecimal("800000"), captor.getValue().getTotalNeto());
    }

    @Test
    void liquidar_quincenalConAdelanto_descontaDelNeto() {
        Empleado emp = empleado(TipoEmpleado.QUINCENAL, new BigDecimal("800000"));
        Adelanto adelanto = Adelanto.builder()
                .montoCop(new BigDecimal("100000"))
                .build();

        when(empleadoRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(1L)).thenReturn(List.of(adelanto));
        when(tasaCambioService.getValorVigente()).thenReturn(new BigDecimal("4200"));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(empleadoMapper.toPagoDTO(any())).thenReturn(new PagoEmpleadoDTO());

        PagoEmpleadoRequest req = new PagoEmpleadoRequest();
        req.setMoneda(MonedaConstants.COP);

        service.liquidar(1L, req);

        ArgumentCaptor<PagoEmpleado> captor = ArgumentCaptor.forClass(PagoEmpleado.class);
        verify(pagoRepo).save(captor.capture());
        assertEquals(new BigDecimal("700000"), captor.getValue().getTotalNeto());
    }

    @Test
    void liquidar_porDiaSinDiasPendientes_lanzaValidacionException() {
        Empleado emp = empleado(TipoEmpleado.POR_DIA, new BigDecimal("60000"));
        when(empleadoRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(1L)).thenReturn(List.of());
        when(trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(1L)).thenReturn(List.of());

        PagoEmpleadoRequest req = new PagoEmpleadoRequest();
        req.setMoneda(MonedaConstants.COP);

        assertThrows(ValidacionException.class, () -> service.liquidar(1L, req));
    }

    // ─── registrarAdelanto ───────────────────────────────────────────────────

    @Test
    void registrarAdelanto_tipoEfectivo_debitaFondoInmediatamente() {
        Empleado emp = empleado(TipoEmpleado.QUINCENAL, new BigDecimal("800000"));
        when(empleadoRepo.findById(1L)).thenReturn(Optional.of(emp));
        when(tasaCambioService.getValorVigente()).thenReturn(new BigDecimal("4200"));
        when(monetaryService.convertirACop(any(), any(), any())).thenReturn(new BigDecimal("50000"));
        when(adelantoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(empleadoMapper.toAdelantoDTO(any())).thenReturn(new AdelantoDTO());

        AdelantoRequest req = new AdelantoRequest();
        req.setTipo(TipoAdelanto.EFECTIVO);
        req.setMonto(new BigDecimal("50000"));
        req.setMoneda(MonedaConstants.COP);
        req.setFecha(LocalDate.now());

        service.registrarAdelanto(1L, req);

        verify(cajaMovimientoService).registrar(
                eq(CajaMovimientoTipo.ADELANTO_EMPLEADO),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void registrarAdelanto_tipoEspecieSinObservacion_lanzaValidacionException() {
        Empleado emp = empleado(TipoEmpleado.QUINCENAL, new BigDecimal("800000"));
        when(empleadoRepo.findById(1L)).thenReturn(Optional.of(emp));

        AdelantoRequest req = new AdelantoRequest();
        req.setTipo(TipoAdelanto.ESPECIE);
        req.setMonto(new BigDecimal("50000"));
        req.setFecha(LocalDate.now());
        // observacion = null → debe lanzar excepción

        assertThrows(ValidacionException.class, () -> service.registrarAdelanto(1L, req));
        verifyNoInteractions(cajaMovimientoService);
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private Empleado empleado(TipoEmpleado tipo, BigDecimal salario) {
        return Empleado.builder()
                .id(1L).nombre("Test").tipo(tipo).salario(salario).activo(true).build();
    }
}
