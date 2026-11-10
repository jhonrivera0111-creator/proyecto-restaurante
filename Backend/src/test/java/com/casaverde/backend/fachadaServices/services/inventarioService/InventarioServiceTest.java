package com.casaverde.backend.fachadaServices.services.inventarioService;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;
import com.casaverde.backend.fachadaServices.mapper.InventarioMapper;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class InventarioServiceTest {

    @Mock private InventarioRepository inventarioRepo;
    @Mock private MovimientoInventarioRepository movimientoRepo;
    @Mock private IAuditoriaService auditoriaService;
    @Mock private InventarioMapper inventarioMapper;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private InventarioServiceImpl service;

    @Test
    void registrarMovimiento_tipoEntrada_incrementaStock() {
        // stock inicial: 5, entrada: 3 -> esperado: 8
        Inventario inv = Inventario.builder()
                .insumo("gallina")
                .stockActual(new BigDecimal("5"))
                .unidad("unidad")
                .build();

        when(inventarioRepo.findByInsumo("gallina")).thenReturn(Optional.of(inv));
        when(inventarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepo.save(any())).thenReturn(new MovimientoInventario());
        when(inventarioMapper.toMovimientoDTO(any())).thenReturn(new MovimientoInventarioDTO());

        MovimientoRequest req = new MovimientoRequest();
        req.setInsumo("gallina");
        req.setCantidad(new BigDecimal("3"));
        req.setTipo("entrada");

        service.registrarMovimiento(req);

        assertEquals(new BigDecimal("8"), inv.getStockActual());
        verify(inventarioRepo).save(inv);
        verify(movimientoRepo).save(any(MovimientoInventario.class));
        verify(auditoriaService).registrar(eq("inventario"), any(), eq("MOVIMIENTO_MANUAL"), anyString());
    }

    @Test
    void registrarMovimiento_salidaMayorQueStock_lanzaValidacionException() {
        // stock: 2, salida solicitada: 5 -> stock resultante negativo -> ValidacionException
        Inventario inv = Inventario.builder()
                .insumo("gallina")
                .stockActual(new BigDecimal("2"))
                .unidad("unidad")
                .build();

        when(inventarioRepo.findByInsumo("gallina")).thenReturn(Optional.of(inv));

        MovimientoRequest req = new MovimientoRequest();
        req.setInsumo("gallina");
        req.setCantidad(new BigDecimal("5"));
        req.setTipo("salida");

        assertThrows(ValidacionException.class, () -> service.registrarMovimiento(req));

        // Fondo no se toca: save no debe haberse llamado sobre el inventario
        verify(inventarioRepo, never()).save(any());
    }

    @Test
    void registrarMovimiento_tipoAjuste_setCantidadDirecta() {
        // ajuste ignora el stock actual y establece el valor exacto indicado
        Inventario inv = Inventario.builder()
                .insumo("cuy")
                .stockActual(new BigDecimal("10"))
                .unidad("unidad")
                .build();

        when(inventarioRepo.findByInsumo("cuy")).thenReturn(Optional.of(inv));
        when(inventarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepo.save(any())).thenReturn(new MovimientoInventario());
        when(inventarioMapper.toMovimientoDTO(any())).thenReturn(new MovimientoInventarioDTO());

        MovimientoRequest req = new MovimientoRequest();
        req.setInsumo("cuy");
        req.setCantidad(new BigDecimal("4"));
        req.setTipo("ajuste");

        service.registrarMovimiento(req);

        assertEquals(new BigDecimal("4"), inv.getStockActual());
        verify(inventarioRepo).save(inv);
    }
}
