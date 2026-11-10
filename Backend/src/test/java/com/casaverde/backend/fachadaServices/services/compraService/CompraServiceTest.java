package com.casaverde.backend.fachadaServices.services.compraService;

import com.casaverde.backend.capaAccesoADatos.models.entitys.*;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;
import com.casaverde.backend.capaAccesoADatos.repositories.*;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.*;
import com.casaverde.backend.fachadaServices.mapper.AbonoCompraMapper;
import com.casaverde.backend.fachadaServices.mapper.CompraMapper;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CompraServiceTest {

    @Mock private CompraRepository compraRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private AbonoCompraRepository abonoRepo;
    @Mock private AdelantoProveedorRepository adelantoProveedorRepo;
    @Mock private InventarioRepository inventarioRepo;
    @Mock private MovimientoInventarioRepository movimientoRepo;
    @Mock private FondoRepository fondoRepo;
    @Mock private CajaMovimientoRepository cajaMovimientoRepo;
    @Mock private ICajaMovimientoService cajaMovimientoService;
    @Mock private ITasaCambioService tasaCambioService;
    @Mock private IMonetaryService monetaryService;
    @Mock private IAuditoriaService auditoriaService;
    @Mock private CompraMapper compraMapper;
    @Mock private AbonoCompraMapper abonoMapper;

    @InjectMocks
    private CompraServiceImpl service;

    // ─── registrar ───────────────────────────────────────────────────────────

    @Test
    void registrar_conDatosValidos_sumaInventario() {
        Proveedor prov = Proveedor.builder()
                .id(1L).nombre("Granja").tipo(TipoProveedor.GALLINA).activo(true).build();
        Compra compraGuardada = Compra.builder()
                .id(1L)
                .proveedor(prov)
                .insumo("gallina")
                .cantidad(new BigDecimal("5"))
                .precioUnitario(new BigDecimal("20000"))
                .totalCop(new BigDecimal("100000"))
                .estado(EstadoCompra.PENDIENTE)
                .fecha(LocalDate.now())
                .build();
        Inventario inv = Inventario.builder()
                .insumo("gallina")
                .stockActual(new BigDecimal("10"))
                .build();

        when(proveedorRepo.findById(1L)).thenReturn(Optional.of(prov));
        when(compraRepo.save(any())).thenReturn(compraGuardada);
        when(inventarioRepo.findByInsumo("gallina")).thenReturn(Optional.of(inv));
        when(inventarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepo.save(any())).thenReturn(new MovimientoInventario());
        // registrar() pasa List.of() directo a enrichCompraDTO (no consulta abonoRepo,
        // una compra recien creada no puede tener abonos) — solo se stubea compraMapper.
        when(compraMapper.toDTO(any())).thenReturn(new CompraDTO());

        CompraRequest req = new CompraRequest();
        req.setProveedorId(1L);
        req.setInsumo("gallina");
        req.setCantidad(new BigDecimal("5"));
        req.setPrecioUnitario(new BigDecimal("20000"));
        req.setFecha(LocalDate.now());

        service.registrar(req);

        // Stock: 10 + 5 = 15
        assertEquals(new BigDecimal("15"), inv.getStockActual());
    }

    // ─── anular ──────────────────────────────────────────────────────────────

    @Test
    void anular_enEstadoPendienteSinAbonos_eliminaCompraYRevierteInventario() {
        Inventario inv = Inventario.builder()
                .insumo("gallina")
                .stockActual(new BigDecimal("15"))
                .build();
        Compra compra = Compra.builder()
                .id(1L)
                .insumo("gallina")
                .cantidad(new BigDecimal("5"))
                .estado(EstadoCompra.PENDIENTE)
                .build();

        when(compraRepo.findById(1L)).thenReturn(Optional.of(compra));
        when(abonoRepo.findByCompraIdOrderByCreadoEnDesc(1L)).thenReturn(List.of());
        when(inventarioRepo.findByInsumo("gallina")).thenReturn(Optional.of(inv));
        when(inventarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepo.save(any())).thenReturn(new MovimientoInventario());

        service.anular(1L);

        verify(compraRepo).delete(compra);
        // Stock: 15 - 5 = 10
        assertEquals(new BigDecimal("10"), inv.getStockActual());
    }

    @Test
    void anular_conAbonos_lanzaValidacionException() {
        Compra compra = Compra.builder()
                .id(1L)
                .insumo("gallina")
                .estado(EstadoCompra.PENDIENTE)
                .build();

        when(compraRepo.findById(1L)).thenReturn(Optional.of(compra));
        when(abonoRepo.findByCompraIdOrderByCreadoEnDesc(1L))
                .thenReturn(List.of(new AbonoCompra()));

        assertThrows(ValidacionException.class, () -> service.anular(1L));
        verify(compraRepo, never()).delete(any());
    }

    @Test
    void anular_enEstadoAbonado_lanzaValidacionException() {
        Compra compra = Compra.builder()
                .id(1L)
                .estado(EstadoCompra.ABONADO_PARCIAL)
                .build();

        when(compraRepo.findById(1L)).thenReturn(Optional.of(compra));

        // estado != PENDIENTE → excepción antes de consultar abonos
        assertThrows(ValidacionException.class, () -> service.anular(1L));
        verify(compraRepo, never()).delete(any());
    }
}
