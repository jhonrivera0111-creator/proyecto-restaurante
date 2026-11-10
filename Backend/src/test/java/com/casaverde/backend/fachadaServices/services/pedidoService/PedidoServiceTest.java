package com.casaverde.backend.fachadaServices.services.pedidoService;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.mapper.PedidoMapper;
import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepo;
    @Mock private ProductoRepository productoRepo;
    @Mock private SalonRepository salonRepo;
    @Mock private CajaMovimientoRepository cajaMovimientoRepo;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepo;
    @Mock private IComprobanteService comprobanteService;
    @Mock private ITasaCambioService tasaCambioService;
    @Mock private IFondoService fondoService;
    @Mock private IMonetaryService monetaryService;
    @Mock private IAuditoriaService auditoriaService;
    @Mock private IPedidoFinalizacionService finalizacionService;
    @Mock private ObjectMapper objectMapper;

    // PedidoMapper is a plain @Component with no dependencies.
    // Mockito/ByteBuddy cannot instrument it because it references the Lombok-generated
    // Pedido class via a classloader that is not visible during retransformation.
    // We instantiate it directly and pass it to the constructor — no Mockito involvement.
    private final PedidoMapper pedidoMapper = new PedidoMapper();

    private PedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PedidoServiceImpl(
                pedidoRepo,
                productoRepo,
                salonRepo,
                cajaMovimientoRepo,
                movimientoInventarioRepo,
                comprobanteService,
                tasaCambioService,
                fondoService,
                monetaryService,
                auditoriaService,
                finalizacionService,
                objectMapper,
                pedidoMapper
        );
    }

    // ─── crear ────────────────────────────────────────────────────────────────

    @Test
    void crear_conNombreValido_guardaPedidoConEstadoPendiente() {
        CrearPedidoRequest req = new CrearPedidoRequest();
        req.setNombre("Juan");

        when(pedidoRepo.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(monetaryService.normalizarPrecioCop(any())).thenReturn(BigDecimal.ZERO);
        doNothing().when(auditoriaService).registrar(any(), any(), any(), any());

        service.crear(req);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepo, atLeastOnce()).save(captor.capture());
        assertEquals(EstadoPedido.PENDIENTE, captor.getAllValues().get(0).getEstado());
    }

    // ─── agregarDetalle ───────────────────────────────────────────────────────

    @Test
    void agregarDetalle_enPedidoFinalizado_lanzaValidacionException() {
        Pedido pedido = pedidoBase(EstadoPedido.FINALIZADO);
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));

        DetalleItemRequest req = new DetalleItemRequest();
        req.setProductoId(10L);
        req.setCantidad(1);

        assertThrows(ValidacionException.class, () -> service.agregarDetalle(1L, req));
    }

    // ─── registrarPago ────────────────────────────────────────────────────────

    @Test
    void registrarPago_enPedidoPendiente_noTocaFondo() {
        // registrarPago() ahora exige al menos un ítem (un pedido sin ítems no
        // puede cobrarse) — pedidoConDetalleSinPago() en vez de pedidoBase().
        Pedido pedido = pedidoConDetalleSinPago();
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));
        when(monetaryService.normalizarMoneda(any())).thenReturn(MonedaConstants.COP);
        when(monetaryService.normalizarMontoEntrada(any(), any())).thenReturn(new BigDecimal("10000"));
        when(monetaryService.convertirACop(any(), any(), any())).thenReturn(new BigDecimal("10000"));
        when(pedidoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditoriaService).registrar(any(), any(), any(), any());

        RegistrarPagoRequest req = new RegistrarPagoRequest();
        req.setMoneda(MonedaConstants.COP);
        req.setMonto(new BigDecimal("10000"));

        service.registrarPago(1L, req);

        verifyNoInteractions(finalizacionService);
    }

    // ─── finalizarPedido ──────────────────────────────────────────────────────

    @Test
    void finalizarPedido_sinDetalles_lanzaValidacionException() {
        Pedido pedido = pedidoBase(EstadoPedido.PENDIENTE);
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(ValidacionException.class, () -> service.finalizarPedido(1L));
    }

    @Test
    void finalizarPedido_cuandoSaldoPendiente_lanzaValidacionException() {
        Pedido pedido = pedidoConDetalleSinPago();
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(ValidacionException.class, () -> service.finalizarPedido(1L));
    }

    @Test
    void finalizarPedido_cuandoPagadoCompleto_cambiaEstadoFinalizado() {
        Pedido pedido = pedidoConDetallePagado();
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditoriaService).registrar(any(), any(), any(), any());

        service.finalizarPedido(1L);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepo).save(captor.capture());
        assertEquals(EstadoPedido.FINALIZADO, captor.getValue().getEstado());
        verify(finalizacionService).ejecutar(any(Pedido.class), any(BigDecimal.class));
    }

    // ─── anularPedido ─────────────────────────────────────────────────────────

    @Test
    void anularPedido_enPedidoPendiente_cambiaEstadoAAnulado() {
        Pedido pedido = pedidoBase(EstadoPedido.PENDIENTE);
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditoriaService).registrar(any(), any(), any(), any());

        service.anularPedido(1L);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepo).save(captor.capture());
        assertEquals(EstadoPedido.ANULADO, captor.getValue().getEstado());
    }

    @Test
    void anularPedido_enPedidoFinalizado_lanzaValidacionException() {
        Pedido pedido = pedidoBase(EstadoPedido.FINALIZADO);
        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(ValidacionException.class, () -> service.anularPedido(1L));
        verifyNoInteractions(fondoService);
    }

    @Test
    void anularPedido_conPagosRegistrados_noTocaFondo() {
        Pedido pedido = pedidoBase(EstadoPedido.PENDIENTE);
        Pago pago = new Pago();
        pago.setMontoCop(new BigDecimal("5000"));
        pago.setMoneda(MonedaConstants.COP);
        pedido.getPagos().add(pago);

        when(pedidoRepo.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditoriaService).registrar(any(), any(), any(), any());

        service.anularPedido(1L);

        verifyNoInteractions(fondoService);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Pedido pedidoBase(EstadoPedido estado) {
        return Pedido.builder().id(1L).nombre("Test").estado(estado).build();
    }

    private Pedido pedidoConDetalleSinPago() {
        Pedido p = Pedido.builder().id(1L).nombre("Test").estado(EstadoPedido.PENDIENTE).build();
        PedidoDetalle d = new PedidoDetalle();
        d.setPrecioUnitarioCop(new BigDecimal("20000"));
        d.setCantidad(1);
        p.getDetalles().add(d);
        return p;
    }

    private Pedido pedidoConDetallePagado() {
        Pedido p = pedidoConDetalleSinPago();
        Pago pago = new Pago();
        pago.setMontoCop(new BigDecimal("20000"));
        pago.setMoneda(MonedaConstants.COP);
        p.getPagos().add(pago);
        return p;
    }
}
