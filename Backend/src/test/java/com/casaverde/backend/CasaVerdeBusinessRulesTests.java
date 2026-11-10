package com.casaverde.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.HistorialAuditoriaRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoPrecioHistorialRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.TasaCambioRepository;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.ActualizarDetalleRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;
import com.casaverde.backend.fachadaServices.dto.CierreCajaRequest;
import com.casaverde.backend.fachadaServices.dto.ComposicionDTO;
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.GastoRequest;
import com.casaverde.backend.fachadaServices.dto.LoginRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoRequest;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;
import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;
import com.casaverde.backend.fachadaServices.services.pedidoService.ICierreCajaService;
import com.casaverde.backend.fachadaServices.services.pedidoService.IPedidoService;
import com.casaverde.backend.fachadaServices.services.pedidoService.IResumenService;
import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;
import com.casaverde.backend.fachadaServices.services.cajaService.IGastoService;
import com.casaverde.backend.fachadaServices.services.productoService.IProductoService;
import com.casaverde.backend.fachadaServices.services.productoService.IUsuarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// app.security.enabled=true en application.properties base (el perfil "test" no lo
// pisa). Se desactiva aqui porque el objetivo de los tests MockMvc de esta clase es
// Bean Validation (@Valid), no el filtro de autenticacion — evita mezclar dos
// responsabilidades distintas en la misma prueba.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.enabled=false")
@Transactional
class CasaVerdeBusinessRulesTests {

        @Autowired
        private MockMvc mvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private IPedidoService pedidoService;

        @Autowired
        private IGastoService gastoService;

        @Autowired
        private IFondoService fondoService;

        @Autowired
        private IUsuarioService usuarioService;

        @Autowired
        private ICierreCajaService cierreCajaService;

        @Autowired
        private IProductoService productoService;

        @Autowired
        private ProductoRepository productoRepository;

        @Autowired
        private InventarioRepository inventarioRepository;

        @Autowired
        private TasaCambioRepository tasaCambioRepository;

        @Autowired
        private CajaMovimientoRepository cajaMovimientoRepository;

        @Autowired
        private FondoRepository fondoRepository;

        @Autowired
        private HistorialAuditoriaRepository historialAuditoriaRepository;

        @Autowired
        private ProductoPrecioHistorialRepository precioHistorialRepository;

        @Autowired
        private com.casaverde.backend.fachadaServices.services.pedidoService.ICuentaCobrarService cuentaCobrarService;

        @Autowired
        private com.casaverde.backend.fachadaServices.services.compraService.ICompraService compraService;

        @Autowired
        private com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository proveedorRepository;

        @Autowired
        private IResumenService resumenService;

        @Autowired
        private PedidoRepository pedidoRepository;

        @Autowired
        private MovimientoInventarioRepository movimientoInventarioRepository;

        @Test
        void noFinalizaPedidoSiNoHayStockSuficiente() {
                Inventario cuy = inventarioRepository.findByInsumo("cuy").orElseThrow();
                cuy.setStockActual(BigDecimal.ZERO);
                inventarioRepository.save(cuy);

                Producto productoCuy = productoRepository.findByEliminadoFalseOrderByOrdenAsc().stream()
                                .filter(p -> "Cuy".equals(p.getNombre()))
                                .findFirst()
                                .orElseThrow();
                productoCuy.setPrecio(BigDecimal.valueOf(1000));
                productoRepository.save(productoCuy);

                CrearPedidoRequest request = new CrearPedidoRequest();
                request.setNombre("Cliente prueba stock");
                request.setDetalles(List.of(new DetalleItemRequest(productoCuy.getId(), null, null, 1)));

                PedidoDTO pedido = pedidoService.crear(request);
                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

                assertThatThrownBy(() -> pedidoService.finalizarPedido(pedido.getId()))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("Stock insuficiente de cuy");
        }

        @Test
        void gastoInventariableAumentaStockYDejaMovimiento() {
                ajustarFondo("COP", BigDecimal.valueOf(20000));
                Inventario gallina = inventarioRepository.findByInsumo("gallina").orElseThrow();
                BigDecimal stockInicial = gallina.getStockActual();

                GastoRequest request = new GastoRequest();
                request.setConcepto("Compra prueba gallinas");
                request.setCategoria("insumos");
                request.setMoneda("COP");
                request.setMonto(BigDecimal.valueOf(10000));
                request.setInsumoInventario("gallina");
                request.setCantidadInventario(new BigDecimal("3"));

                gastoService.registrar(request);

                Inventario actualizado = inventarioRepository.findByInsumo("gallina").orElseThrow();
                assertThat(actualizado.getStockActual()).isEqualByComparingTo(stockInicial.add(new BigDecimal("3")));
                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .anySatisfy(m -> {
                                        assertThat(m.getTipo()).isEqualTo(CajaMovimientoTipo.GASTO);
                                        assertThat(m.getMonto()).isEqualByComparingTo(BigDecimal.valueOf(-10000));
                                });
        }

        @Test
        void gastoSinSaldoDisponibleEsRechazado() {
                GastoRequest request = new GastoRequest();
                request.setConcepto("Gasto sin caja");
                request.setCategoria("otros");
                request.setMoneda("COP");
                request.setMonto(BigDecimal.valueOf(1000));

                assertThatThrownBy(() -> gastoService.registrar(request))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("no tiene saldo suficiente");
                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void pagoUsdAceptaCentavosYCalculaEquivalenteCop() {
                PedidoDTO pedido = crearPedidoManual("Cliente USD", BigDecimal.valueOf(5000));
                BigDecimal tasa = tasaCambioRepository.findVigente().orElseThrow().getValorCop();
                BigDecimal montoUsd = new BigDecimal("0.25");

                PedidoDTO actualizado = pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("USD", montoUsd));

                assertThat(actualizado.getPagos()).hasSize(1);
                assertThat(actualizado.getPagos().get(0).getMonto()).isEqualByComparingTo("0.25");
                assertThat(actualizado.getPagos().get(0).getMontoCop())
                                .isEqualByComparingTo(montoUsd.multiply(tasa).setScale(0, RoundingMode.HALF_UP));
        }

        @Test
        void pagoCopRechazaCentavos() {
                PedidoDTO pedido = crearPedidoManual("Cliente COP decimal", BigDecimal.valueOf(5000));

                assertThatThrownBy(() -> pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", new BigDecimal("0.25"))))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("COP no debe tener centavos");
        }

        @Test
        void pedidoTransicionaPorEstadosDePagoHastaFinalizar() {
                PedidoDTO pedido = crearPedidoManual("Cliente estados", BigDecimal.valueOf(1000));

                assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE.name());

                PedidoDTO parcial = pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(250)));
                assertThat(parcial.getEstado()).isEqualTo(EstadoPedido.ABONADO_PARCIAL.name());

                PedidoDTO pagado = pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(750)));
                assertThat(pagado.getEstado()).isEqualTo(EstadoPedido.PAGADO.name());

                PedidoDTO finalizado = pedidoService.finalizarPedido(pedido.getId());
                assertThat(finalizado.getEstado()).isEqualTo(EstadoPedido.FINALIZADO.name());
                assertThat(finalizado.getFinalizadoEn()).isNotNull();
        }

        @Test
        void pagoConSobranteSeparaSaldoPendienteYValorCambio() {
                PedidoDTO pedido = crearPedidoManual("Cliente cambio", BigDecimal.valueOf(1000));

                PedidoDTO pagado = pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1200)));

                assertThat(pagado.getEstado()).isEqualTo(EstadoPedido.PAGADO.name());
                assertThat(pagado.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(pagado.getValorCambio()).isEqualByComparingTo(BigDecimal.valueOf(200));
        }

        @Test
        void listarPedidosActivosSoportaDetallesYPagosSinMultipleBagFetch() {
                PedidoDTO pedido = crearPedidoManual("Cliente listado", BigDecimal.valueOf(1000));
                pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(500)));

                List<PedidoDTO> activos = pedidoService.listarActivos();

                assertThat(activos)
                                .anySatisfy(p -> {
                                        assertThat(p.getId()).isEqualTo(pedido.getId());
                                        assertThat(p.getDetalles()).hasSize(1);
                                        assertThat(p.getPagos()).hasSize(1);
                                });
        }

        @Test
        void pagoRegistraLibroCajaAuditoriaYActualizaFondo() {
                PedidoDTO pedido = crearPedidoManual("Cliente caja", BigDecimal.valueOf(1000));

                // El fondo solo se acredita al finalizar, no en el momento del pago
                pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));
                pedidoService.finalizarPedido(pedido.getId());

                // Cobro en COP efectivo acredita el fondo EFECTIVO (caja física), no COP
                // directo — ver PedidoFinalizacionServiceImpl.
                assertThat(fondoRepository.findById("EFECTIVO").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.valueOf(1000));
                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .extracting(m -> m.getTipo() + "|" + m.getReferenciaTipo() + "|" + m.getReferenciaId())
                                .contains(CajaMovimientoTipo.COBRO_PEDIDO.name() + "|pedido|" + pedido.getId());
                assertThat(historialAuditoriaRepository.findByEntidadAndEntidadIdOrderByRegistradoEnDesc("pedido",
                                pedido.getId()))
                                .anySatisfy(a -> assertThat(a.getAccion()).isEqualTo("REGISTRAR_PAGO"));
        }

        @Test
        void anularPedidoReviertePagosEnLibroCajaYFondo() {
                PedidoDTO pedido = crearPedidoManual("Cliente reverso", BigDecimal.valueOf(1000));
                pedidoService.registrarPago(
                                pedido.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

                PedidoDTO anulado = pedidoService.anularPedido(pedido.getId());

                assertThat(anulado.getEstado()).isEqualTo(EstadoPedido.ANULADO.name());
                // El fondo solo se acredita al finalizar; anular antes de finalizar no genera
                // impacto en fondos
                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .noneMatch(m -> CajaMovimientoTipo.REVERSO_PAGO.equals(m.getTipo())
                                                || CajaMovimientoTipo.COBRO_PEDIDO.equals(m.getTipo()));
        }

        @Test
        void loginLocalSoloExigeClaveAlAdmin() {
                List<UsuarioDTO> usuarios = usuarioService.listarActivos();
                UsuarioDTO admin = usuarios.stream().filter(u -> "ADMIN".equals(u.getRol())).findFirst().orElseThrow();
                UsuarioDTO operador = usuarioService.crearOperador(new CrearOperadorRequest("Operador Test"));

                assertThat(usuarioService.login(new LoginRequest(operador.getId(), null)).getNombre())
                                .isEqualTo(operador.getNombre());
                assertThatThrownBy(() -> usuarioService.login(new LoginRequest(admin.getId(), "mala")))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("incorrecta");
                assertThat(usuarioService.login(new LoginRequest(admin.getId(), "admin123")).getRol())
                                .isEqualTo("ADMIN");
        }

        @Test
        void cierreCajaBloqueaPedidosAbiertosYCreaBackupCuandoNoHayPendientes() {
                PedidoDTO pedido = crearPedidoManual("Cliente pendiente cierre", BigDecimal.valueOf(1000));
                CierreCajaRequest bloqueado = new CierreCajaRequest();
                bloqueado.setFecha(LocalDate.now());

                assertThatThrownBy(() -> cierreCajaService.cerrar(bloqueado))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("pedido(s) pendiente(s)");

                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));
                pedidoService.finalizarPedido(pedido.getId());

                CierreCajaRequest request = new CierreCajaRequest();
                request.setFecha(LocalDate.now());
                // El pedido se pagó en COP efectivo, que ahora acredita el fondo
                // EFECTIVO (no COP) al finalizar — ver PedidoFinalizacionServiceImpl.
                request.setContadoEfectivo(BigDecimal.valueOf(1000));
                request.setContadoUsd(BigDecimal.ZERO);
                request.setContadoNequi(BigDecimal.ZERO);

                CierreCajaDTO cierre = cierreCajaService.cerrar(request);
                assertThat(cierre.getId()).isNotNull();
                assertThat(cierre.getBackupPath()).contains("casaverde-");
                assertThat(cierre.getDiferenciaEfectivo()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void cambioDePrecioQuedaEnHistorial() {
                Producto producto = productoRepository.findByEliminadoFalseOrderByOrdenAsc().stream()
                                .filter(p -> "Gaseosa".equals(p.getNombre()))
                                .findFirst()
                                .orElseThrow();

                productoService.actualizarPrecio(producto.getId(), BigDecimal.valueOf(3500), null);

                assertThat(precioHistorialRepository.findByProducto_IdOrderByRegistradoEnDesc(producto.getId()))
                                .anySatisfy(h -> {
                                        assertThat(h.getPrecioAnterior()).isEqualByComparingTo(BigDecimal.ZERO);
                                        assertThat(h.getPrecioNuevo()).isEqualByComparingTo(BigDecimal.valueOf(3500));
                                        assertThat(h.getMotivo()).isEqualTo("ACTUALIZAR_PRECIO");
                                });
        }

        @Test
        void actualizarProductoConComposicionExistenteNoRompeUniqueConstraint() {
                Producto cuy = productoRepository.findByEliminadoFalseOrderByOrdenAsc().stream()
                                .filter(p -> "Cuy".equals(p.getNombre()))
                                .findFirst()
                                .orElseThrow();
                assertThat(cuy.getComposicion()).hasSize(1);

                ProductoRequest req = ProductoRequest.builder()
                                .nombre("Cuy")
                                .precio(BigDecimal.valueOf(28000))
                                .categoriaId(cuy.getCategoria().getId())
                                .afectaInventario(true)
                                // mismo insumo que ya existe en la composición — reproduce el bug de
                                // clear()+re-add violando UNIQUE(producto_id, insumo)
                                .composicion(List.of(ComposicionDTO.builder().insumo("cuy")
                                                .cantidad(BigDecimal.ONE).build()))
                                .build();

                // flush explícito: el INSERT/DELETE de composicion solo se ejecuta contra la
                // BD al hacer flush — findById() sirve desde la sesión (1st-level cache) y
                // NO lo dispara, así que sin este flush el test pasa aunque el bug siga ahí.
                assertThatCode(() -> {
                        productoService.actualizar(cuy.getId(), req);
                        productoRepository.flush();
                }).doesNotThrowAnyException();

                Producto actualizado = productoRepository.findById(cuy.getId()).orElseThrow();
                assertThat(actualizado.getPrecio()).isEqualByComparingTo(BigDecimal.valueOf(28000));
                assertThat(actualizado.getComposicion()).hasSize(1);
                assertThat(actualizado.getComposicion().get(0).getInsumo()).isEqualTo("cuy");
        }

        @Test
        void reordenarProductosPersisteElNuevoOrden() {
                List<Long> idsInvertidos = productoRepository.findByEliminadoFalseOrderByOrdenAsc().stream()
                                .map(Producto::getId)
                                .sorted(Comparator.reverseOrder())
                                .toList();

                productoService.reordenar(idsInvertidos);

                List<Long> idsResultantes = productoRepository.findByEliminadoFalseOrderByOrdenAsc().stream()
                                .map(Producto::getId)
                                .toList();
                assertThat(idsResultantes).isEqualTo(idsInvertidos);
        }

        // Estos tres casos ejercitan @Valid/Bean Validation sobre CompraRequest y
        // AbonoCuentaCobrarRequest. @Valid solo se dispara cuando el request pasa por
        // el DispatcherServlet (MethodArgumentNotValidException se resuelve al
        // enlazar los argumentos del controller); llamar al service directo, como
        // antes, nunca la ejercita. Por eso se prueban vía MockMvc contra el
        // endpoint real en lugar de invocar el service. app.security.enabled=false
        // en el perfil "test" (ver application-test.properties), asi que no hace
        // falta autenticacion para llegar al filtro de validacion.

        @Test
        void registrarAbonoCuentaCobrarConMontoNegativoEsRechazadoPorValidacion() throws Exception {
                ajustarFondo("COP", BigDecimal.valueOf(50000));

                var req = new com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest();
                req.setClienteNombre("Pedro Test");
                req.setMonto(BigDecimal.valueOf(30000));
                req.setFecha(LocalDate.now());
                var cuenta = cuentaCobrarService.registrar(req);

                var abonoReq = new com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest();
                abonoReq.setMonto(BigDecimal.valueOf(-5000));
                abonoReq.setMoneda("COP");
                abonoReq.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/cuentas-cobrar/{id}/abonos", cuenta.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(abonoReq)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("El monto debe ser mayor a 0"));

                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.valueOf(50000));
        }

        @Test
        void registrarAbonoCuentaCobrarConMontoCeroEsRechazadoPorValidacion() throws Exception {
                ajustarFondo("COP", BigDecimal.valueOf(50000));

                var req = new com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest();
                req.setClienteNombre("Pedro Test Cero");
                req.setMonto(BigDecimal.valueOf(30000));
                req.setFecha(LocalDate.now());
                var cuenta = cuentaCobrarService.registrar(req);

                var abonoReq = new com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest();
                abonoReq.setMonto(BigDecimal.ZERO);
                abonoReq.setMoneda("COP");
                abonoReq.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/cuentas-cobrar/{id}/abonos", cuenta.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(abonoReq)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("El monto debe ser mayor a 0"));
        }

        @Test
        void registrarAbonoCuentaCobrarConMontoNuloEsRechazadoPorValidacion() throws Exception {
                var req = new com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest();
                req.setClienteNombre("Pedro Test Nulo");
                req.setMonto(BigDecimal.valueOf(30000));
                req.setFecha(LocalDate.now());
                var cuenta = cuentaCobrarService.registrar(req);

                var abonoReq = new com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest();
                abonoReq.setMonto(null);
                abonoReq.setMoneda("COP");
                abonoReq.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/cuentas-cobrar/{id}/abonos", cuenta.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(abonoReq)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("El monto es obligatorio"));
        }

        @Test
        void registrarCompraConCantidadNegativaEsRechazadaPorValidacion() throws Exception {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Cantidad Negativa")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.GALLINA)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("gallina");
                req.setCantidad(BigDecimal.valueOf(-5));
                req.setPrecioUnitario(BigDecimal.valueOf(15000));
                req.setFecha(LocalDate.now());

                BigDecimal stockAntes = inventarioRepository.findByInsumo("gallina").orElseThrow().getStockActual();

                mvc.perform(post("/api/v1/compras")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("La cantidad debe ser mayor a 0"));

                assertThat(inventarioRepository.findByInsumo("gallina").orElseThrow().getStockActual())
                                .isEqualByComparingTo(stockAntes);
        }

        @Test
        void registrarCompraConCantidadCeroEsRechazadaPorValidacion() throws Exception {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Cantidad Cero")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.GALLINA)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("gallina");
                req.setCantidad(BigDecimal.ZERO);
                req.setPrecioUnitario(BigDecimal.valueOf(15000));
                req.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/compras")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("La cantidad debe ser mayor a 0"));
        }

        @Test
        void registrarCompraConCantidadNulaEsRechazadaPorValidacion() throws Exception {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Cantidad Nula")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.GALLINA)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("gallina");
                req.setCantidad(null);
                req.setPrecioUnitario(BigDecimal.valueOf(15000));
                req.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/compras")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("La cantidad es obligatoria"));
        }

        @Test
        void registrarCompraConPrecioNegativoEsRechazadaPorValidacion() throws Exception {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Precio Negativo")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.CUY)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("cuy");
                req.setCantidad(BigDecimal.valueOf(3));
                req.setPrecioUnitario(BigDecimal.valueOf(-1000));
                req.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/compras")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("El precio unitario debe ser mayor a 0"));
        }

        @Test
        void registrarCompraConPrecioCeroEsRechazadaPorValidacion() throws Exception {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Precio Cero")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.CUY)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("cuy");
                req.setCantidad(BigDecimal.valueOf(3));
                req.setPrecioUnitario(BigDecimal.ZERO);
                req.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/compras")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("El precio unitario debe ser mayor a 0"));
        }

        @Test
        void registrarCompraConPrecioNuloEsRechazadaPorValidacion() throws Exception {
                var proveedor = proveedorRepository.save(
                                com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                                .nombre("Proveedor Test Precio Nulo")
                                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.CUY)
                                                .activo(true)
                                                .build());

                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("cuy");
                req.setCantidad(BigDecimal.valueOf(3));
                req.setPrecioUnitario(null);
                req.setFecha(LocalDate.now());

                mvc.perform(post("/api/v1/compras")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.mensaje").value("El precio unitario es obligatorio"));
        }

        @Test
        void anularCompraConStockInsuficienteEsRechazada() {
                // Setup: create an active proveedor
                var proveedor = com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor.builder()
                                .nombre("Proveedor Test Anulacion")
                                .tipo(com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor.GALLINA)
                                .activo(true)
                                .build();
                proveedor = proveedorRepository.save(proveedor);

                // Register a purchase of 5 gallinas (stock goes up by 5)
                var req = new com.casaverde.backend.fachadaServices.dto.CompraRequest();
                req.setProveedorId(proveedor.getId());
                req.setInsumo("gallina");
                req.setCantidad(BigDecimal.valueOf(5));
                req.setPrecioUnitario(BigDecimal.valueOf(30000));
                req.setFecha(LocalDate.now());
                var compra = compraService.registrar(req);

                // Manually reduce stock to 2 (simulate pedidos consuming stock between purchase
                // and cancellation)
                Inventario gallina = inventarioRepository.findByInsumo("gallina").orElseThrow();
                gallina.setStockActual(BigDecimal.valueOf(2));
                inventarioRepository.save(gallina);

                // Act & Assert: anular should fail, NOT silently corrupt inventory
                assertThatThrownBy(() -> compraService.anular(compra.getId()))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("Stock insuficiente");

                // Stock should remain at 2 (unchanged after the failed anulacion)
                assertThat(inventarioRepository.findByInsumo("gallina").orElseThrow().getStockActual())
                                .isEqualByComparingTo(BigDecimal.valueOf(2));
        }

        @Test
        void crearPedidoConFechaReservaPasadaEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reserva pasada");
                req.setFechaReserva(LocalDate.now().minusDays(1).toString());

                assertThatThrownBy(() -> pedidoService.crear(req))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("La fecha de reserva ya pasó");
        }

        @Test
        void crearPedidoConFechaFuturaEsAceptado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reserva futura");
                req.setFechaReserva(LocalDate.now().plusDays(1).toString());

                PedidoDTO pedido = pedidoService.crear(req);
                assertThat(pedido.getId()).isNotNull();
                assertThat(pedido.getFechaReserva()).isEqualTo(LocalDate.now().plusDays(1).toString());
        }

        // ── Resumen del día: animales vendidos (fracciones, exclusiones) ──────────

        @Test
        void resumenCuentaUnCuyCompletoVendido() {
                asegurarStockAlto();
                crearPagarYFinalizar("Cliente cuy", List.of(detalleProducto("Cuy", 1)), null);

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCuyesVendidos()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        void resumenCuentaCantidadFraccionadaDeCuy() {
                asegurarStockAlto();
                crearPagarYFinalizar("Cliente medio cuy", List.of(detalleProducto("1/2 Cuy", 1)), null);

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCuyesVendidos()).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        @Test
        void resumenCuentaUnaGallinaCompletaVendida() {
                asegurarStockAlto();
                crearPagarYFinalizar("Cliente gallina", List.of(detalleProducto("Gallina", 1)), null);

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getGallinasVendidas()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        void resumenCuentaCantidadFraccionadaDeGallina() {
                asegurarStockAlto();
                crearPagarYFinalizar("Cliente media gallina", List.of(detalleProducto("1/2 Gallina", 1)), null);

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getGallinasVendidas()).isEqualByComparingTo(new BigDecimal("0.5"));
        }

        @Test
        void resumenCuentaCuyYGallinaEnElMismoPedido() {
                asegurarStockAlto();
                crearPagarYFinalizar("Cliente mixto",
                                List.of(detalleProducto("Cuy", 1), detalleProducto("Gallina", 1)), null);

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCuyesVendidos()).isEqualByComparingTo(BigDecimal.ONE);
                assertThat(resumen.getGallinasVendidas()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        void resumenNoCuentaPedidoAnuladoComoVendido() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente anulado");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                pedidoService.anularPedido(pedido.getId());

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCuyesVendidos()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(resumen.getPedidosAnulados()).isEqualTo(1);
        }

        // ── Carga histórica: crear+cobrar+finalizar retroactivo ────────────────────

        @Test
        void crearHistoricoFechaElPedidoEnElDiaIndicadoNoEnHoy() {
                asegurarStockAlto();
                LocalDate fechaHistorica = hoyBogota().minusDays(3);

                var req = new com.casaverde.backend.fachadaServices.dto.PedidoHistoricoRequest();
                req.setNombre("Cliente histórico");
                req.setFecha(fechaHistorica);
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                req.setPagos(List.of(new RegistrarPagoRequest("COP", BigDecimal.valueOf(10000))));

                PedidoDTO creado = pedidoService.crearHistorico(req);

                assertThat(creado.getEstado()).isEqualTo("FINALIZADO");
                Pedido entidad = pedidoRepository.findById(creado.getId()).orElseThrow();
                assertThat(entidad.getCreadoEn().toLocalDate()).isEqualTo(fechaHistorica);
                assertThat(entidad.getFinalizadoEn().toLocalDate()).isEqualTo(fechaHistorica);

                // El pago en COP acredita el fondo EFECTIVO (caja física), no el fondo COP —
                // ver PedidoFinalizacionServiceImpl.registrarCobroFinal.
                assertThat(fondoRepository.findById("EFECTIVO").orElseThrow().getSaldo())
                                .isEqualByComparingTo(BigDecimal.valueOf(10000));
                var movimiento = cajaMovimientoRepository
                                .findByReferenciaTipoAndReferenciaId("pedido", creado.getId()).get(0);
                assertThat(movimiento.getRegistradoEn().toLocalDate()).isEqualTo(fechaHistorica);

                var movInventario = movimientoInventarioRepository
                                .findByReferenciaTipoAndReferenciaId("pedido", creado.getId()).get(0);
                assertThat(movInventario.getRegistradoEn().toLocalDate()).isEqualTo(fechaHistorica);

                // El punto entero de la carga histórica: debe aparecer en el resumen del
                // día real, no en el de hoy.
                assertThat(resumenService.obtener(fechaHistorica).getCuyesVendidos())
                                .isEqualByComparingTo(BigDecimal.ONE);
                assertThat(resumenService.obtener(hoyBogota()).getCuyesVendidos())
                                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        // Criterio final: una reserva (fechaReserva != null) solo pertenece al
        // resumen/cierre de "fecha" cuando fechaReserva es EXACTAMENTE esa fecha —
        // ni antes (vencida de otro día) ni después (futura). Un pedido sin reserva
        // (fechaReserva == null) conserva el criterio anterior sin cambios: por
        // creadoEn en el resumen, sin restricción de fecha en el cierre.
        //
        // crear() rechaza fechaReserva en el pasado (no se puede reservar "ayer"),
        // así que para simular una reserva vencida de un día anterior se crea con
        // fechaReserva=hoy (válido) y se "envejece" directamente vía repositorio —
        // exactamente el escenario real: una reserva de ayer que nadie resolvió.

        @Test
        void reservaDeAyerQuedaExcluidaDelResumenDeHoy() {
                asegurarStockAlto();
                Long pedidoId = crearReservaHoyYEnvejecerA(hoyBogota().minusDays(1));

                ResumenDiaDTO resumenHoy = resumenService.obtener(hoyBogota());
                assertThat(resumenHoy.getPedidosTotales()).isEqualTo(0);
                assertThat(resumenHoy.getPedidosAbiertos()).isEqualTo(0);
                assertThat(pedidoId).isNotNull();
        }

        @Test
        void reservaDeHoyIncluidaEnElResumen() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reserva hoy");
                req.setFechaReserva(hoyBogota().toString());
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", pedido.getTotalCop()));
                pedidoService.finalizarPedido(pedido.getId());

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCuyesVendidos()).isEqualByComparingTo(BigDecimal.ONE);
                assertThat(resumen.getPedidosFinalizados()).isEqualTo(1);
        }

        @Test
        void reservaDeMananaExcluidaDelResumenDeHoy() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reserva mañana");
                req.setFechaReserva(hoyBogota().plusDays(1).toString());
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                pedidoService.crear(req);

                ResumenDiaDTO resumenHoy = resumenService.obtener(hoyBogota());
                assertThat(resumenHoy.getPedidosTotales()).isEqualTo(0);
                assertThat(resumenHoy.getPedidosAbiertos()).isEqualTo(0);
                assertThat(resumenHoy.getCuyesVendidos()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void reservaDeAyerNoBloqueaElCierreDeHoy() {
                asegurarStockAlto();
                crearReservaHoyYEnvejecerA(hoyBogota().minusDays(1));

                CierreCajaRequest cierre = new CierreCajaRequest();
                cierre.setFecha(hoyBogota());
                assertThat(cierreCajaService.cerrar(cierre)).isNotNull();
        }

        @Test
        void reservaDeMananaNoBloqueaElCierreDeHoy() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reserva mañana cierre");
                req.setFechaReserva(hoyBogota().plusDays(1).toString());
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                pedidoService.crear(req);

                CierreCajaRequest cierre = new CierreCajaRequest();
                cierre.setFecha(hoyBogota());
                assertThat(cierreCajaService.cerrar(cierre)).isNotNull();
        }

        @Test
        void reservaDeHoyActivaSiBloqueaElCierre() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reserva hoy sin resolver");
                req.setFechaReserva(hoyBogota().toString());
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                pedidoService.crear(req); // PENDIENTE — nunca se paga ni finaliza

                CierreCajaRequest cierre = new CierreCajaRequest();
                cierre.setFecha(hoyBogota());
                assertThatThrownBy(() -> cierreCajaService.cerrar(cierre))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("pendiente");
        }

        // ── Pedidos: teléfono opcional ─────────────────────────────────────────────

        @Test
        void pedidoCreadoSinTelefonoQuedaSinTelefono() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente sin telefono");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(1000), 1)));

                PedidoDTO creado = pedidoService.crear(req);
                assertThat(creado.getTelefono()).isNull();
        }

        @Test
        void pedidoCreadoConTelefonoSePuedeConsultarDespues() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente con telefono");
                req.setTelefono("+57 300 123 4567");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(1000), 1)));

                PedidoDTO creado = pedidoService.crear(req);
                PedidoDTO consultado = pedidoService.obtener(creado.getId());
                assertThat(consultado.getTelefono()).isEqualTo("+57 300 123 4567");
        }

        // ── Pedidos: descuento opcional ─────────────────────────────────────────────

        @Test
        void pedidoSinDescuentoConservaComportamientoAnterior() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente sin descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(5000), 1)));

                PedidoDTO creado = pedidoService.crear(req);
                assertThat(creado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(creado.getDescuentoAplicadoPor()).isNull();
                assertThat(creado.getTotalCop()).isEqualByComparingTo(creado.getSubtotalCop());
        }

        @Test
        void pedidoConDescuentoValidoCalculaSubtotalDescuentoYTotal() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente con descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(5000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(1000));

                PedidoDTO creado = pedidoService.crear(req);
                assertThat(creado.getSubtotalCop()).isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(creado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                assertThat(creado.getTotalCop()).isEqualByComparingTo(BigDecimal.valueOf(4000));
                assertThat(creado.getDescuentoAplicadoPor()).isNotNull();
        }

        // Regla final: 0 <= descuentoCop < subtotalCop (estricta). Mientras el
        // sistema no soporte un flujo de pedidos gratuitos, descuento == subtotal
        // se rechaza igual que descuento > subtotal — un pedido de total $0 nunca
        // llega a PAGADO (EstadoPedido.calcular: total<=0 -> PENDIENTE) y quedaría
        // atascado sin poder finalizarse.
        @Test
        void descuentoIgualAlSubtotalEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente descuento total");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(3000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(3000));

                assertThatThrownBy(() -> pedidoService.crear(req))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("debe ser menor al subtotal");
        }

        @Test
        void descuentoMayorAlSubtotalEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente descuento excesivo");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(2000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(2001));

                assertThatThrownBy(() -> pedidoService.crear(req))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("debe ser menor al subtotal");
        }

        @Test
        void descuentoNegativoEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente descuento negativo");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(2000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(-500));

                assertThatThrownBy(() -> pedidoService.crear(req))
                                .isInstanceOf(ValidacionException.class);
        }

        // Ambos tests confirman persistencia real (no cache de primer nivel):
        // 1) confirman la pedido con TestTransaction.flagForCommit()+end() (commit real),
        // 2) intentan la operación inválida en una transacción NUEVA y capturan la excepción,
        // 3) cierran esa transacción (rollback — nunca se llamó flagForCommit),
        // 4) abren OTRA transacción nueva (persistence context limpio, sin cache) y
        //    reconsultan desde la base real para confirmar que nada cambió.
        @Test
        void eliminarProductoQueDejariaDescuentoMayorOIgualAlSubtotalEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente elimina item con descuento");
                req.setDetalles(List.of(
                                new DetalleItemRequest(null, "Item A", BigDecimal.valueOf(3000), 1),
                                new DetalleItemRequest(null, "Item B", BigDecimal.valueOf(2000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(4000)); // subtotal 5000, valido (4000 < 5000)
                PedidoDTO pedido = pedidoService.crear(req);
                assertThat(pedido.getSubtotalCop()).isEqualByComparingTo(BigDecimal.valueOf(5000));
                Long pedidoId = pedido.getId();
                Long detalleAEliminar = pedido.getDetalles().stream()
                                .filter(d -> d.getPrecioUnitarioCop().compareTo(BigDecimal.valueOf(3000)) == 0)
                                .findFirst().orElseThrow().getId();

                TestTransaction.flagForCommit();
                TestTransaction.end(); // commit real del pedido de setup

                long auditoriaAntes = historialAuditoriaRepository.count();
                long cajaAntes = cajaMovimientoRepository.count();
                long movInventarioAntes = movimientoInventarioRepository.count();

                TestTransaction.start();
                // Quitar el item de 3000 dejaría subtotal=2000 < descuento=4000 -> rechazado
                assertThatThrownBy(() -> pedidoService.quitarDetalle(pedidoId, detalleAEliminar))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("quedaría mayor o igual al nuevo subtotal");
                TestTransaction.end(); // rollback (nunca se marcó para commit)

                TestTransaction.start();
                Pedido reconsultado = pedidoRepository.findById(pedidoId).orElseThrow();
                assertThat(reconsultado.getDetalles()).hasSize(2);
                BigDecimal subtotalReal = reconsultado.getDetalles().stream()
                                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(subtotalReal).isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(reconsultado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.valueOf(4000));
                TestTransaction.end();

                assertThat(historialAuditoriaRepository.count()).isEqualTo(auditoriaAntes);
                assertThat(cajaMovimientoRepository.count()).isEqualTo(cajaAntes);
                assertThat(movimientoInventarioRepository.count()).isEqualTo(movInventarioAntes);

                // El pedido de setup se committeó de verdad (flagForCommit arriba) para
                // poder reconsultarlo en una transacción nueva. anularPedido() no basta:
                // pedidosTotales cuenta TODO lo creado hoy (incluye ANULADO) — hay que
                // borrarlo de verdad para no contaminar otros tests que cuentan pedidos
                // de "hoy" (resumen) o activos (cierre de caja).
                TestTransaction.start();
                pedidoRepository.deleteById(pedidoId);
                TestTransaction.flagForCommit();
                TestTransaction.end();

                TestTransaction.start(); // deja una transaccion activa para el cierre normal del test
        }

        @Test
        void reducirCantidadQueDejariaDescuentoIgualAlSubtotalEsRechazada() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente reduce cantidad con descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(1500), 4))); // subtotal 6000
                req.setDescuentoCop(BigDecimal.valueOf(3000)); // valido (3000 < 6000)
                PedidoDTO pedido = pedidoService.crear(req);
                Long pedidoId = pedido.getId();
                Long detalleId = pedido.getDetalles().get(0).getId();

                TestTransaction.flagForCommit();
                TestTransaction.end();

                long auditoriaAntes = historialAuditoriaRepository.count();
                long cajaAntes = cajaMovimientoRepository.count();

                TestTransaction.start();
                ActualizarDetalleRequest actualizar = new ActualizarDetalleRequest();
                actualizar.setCantidad(2); // nuevo subtotal = 1500*2 = 3000 == descuento -> invalido
                assertThatThrownBy(() -> pedidoService.actualizarDetalle(pedidoId, detalleId, actualizar))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("quedaría mayor o igual al nuevo subtotal");
                TestTransaction.end();

                TestTransaction.start();
                Pedido reconsultado = pedidoRepository.findById(pedidoId).orElseThrow();
                assertThat(reconsultado.getDetalles()).hasSize(1);
                assertThat(reconsultado.getDetalles().get(0).getCantidad()).isEqualTo(4);
                BigDecimal subtotalReal = reconsultado.getDetalles().stream()
                                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(subtotalReal).isEqualByComparingTo(BigDecimal.valueOf(6000));
                assertThat(reconsultado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.valueOf(3000));
                TestTransaction.end();

                assertThat(historialAuditoriaRepository.count()).isEqualTo(auditoriaAntes);
                assertThat(cajaMovimientoRepository.count()).isEqualTo(cajaAntes);

                // Mismo motivo que en el test anterior: borrar de verdad, no solo anular.
                TestTransaction.start();
                pedidoRepository.deleteById(pedidoId);
                TestTransaction.flagForCommit();
                TestTransaction.end();

                TestTransaction.start();
        }

        @Test
        void subtotalDescuentoYTotalSeMuestranCorrectamenteEnDtoDetalleYComprobante() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente comprobante con descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(5000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(1000));
                PedidoDTO pedido = pedidoService.crear(req);

                // DTO
                assertThat(pedido.getSubtotalCop()).isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(pedido.getDescuentoCop()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                assertThat(pedido.getTotalCop()).isEqualByComparingTo(BigDecimal.valueOf(4000));

                // Detalle: el precio unitario histórico NO se toca por el descuento
                assertThat(pedido.getDetalles().get(0).getPrecioUnitarioCop())
                                .isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(pedido.getDetalles().get(0).getSubtotalCop())
                                .isEqualByComparingTo(BigDecimal.valueOf(5000));

                // Comprobante
                String comprobante = pedidoService.comprobanteTexto(pedido.getId());
                assertThat(comprobante).contains("Subtotal COP: 5000");
                assertThat(comprobante).contains("Descuento COP: -1000");
                assertThat(comprobante).contains("Total COP: 4000");
        }

        @Test
        void cobroYCajaUsanElTotalDespuesDelDescuento() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente cobro con descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(10000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(2000));
                PedidoDTO pedido = pedidoService.crear(req);
                assertThat(pedido.getTotalCop()).isEqualByComparingTo(BigDecimal.valueOf(8000));

                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", BigDecimal.valueOf(8000)));
                pedidoService.finalizarPedido(pedido.getId());

                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .anySatisfy(m -> {
                                        if (m.getTipo() == CajaMovimientoTipo.COBRO_PEDIDO
                                                        && ("Cobro pedido #" + pedido.getId()).equals(m.getConcepto())) {
                                                assertThat(m.getMontoCop()).isEqualByComparingTo(BigDecimal.valueOf(8000));
                                        }
                                });

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCobradoTotalCop()).isGreaterThanOrEqualTo(BigDecimal.valueOf(8000));
        }

        @Test
        void inventarioDescuentaLasMismasCantidadesConYSinDescuento() {
                asegurarStockAlto();
                Inventario cuyAntes1 = inventarioRepository.findByInsumo("cuy").orElseThrow();
                BigDecimal stockAntesSinDescuento = cuyAntes1.getStockActual();
                crearPagarYFinalizar("Cliente sin descuento inventario", List.of(detalleProducto("Cuy", 1)), null);
                BigDecimal consumidoSinDescuento = stockAntesSinDescuento
                                .subtract(inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual());

                BigDecimal stockAntesConDescuento = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();
                crearPagarYFinalizar("Cliente con descuento inventario", List.of(detalleProducto("Cuy", 1)),
                                BigDecimal.ONE);
                BigDecimal consumidoConDescuento = stockAntesConDescuento
                                .subtract(inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual());

                assertThat(consumidoConDescuento).isEqualByComparingTo(consumidoSinDescuento);
        }

        // ── Pedido vacío (sin ítems) ─────────────────────────────────────────────────
        // crear() no exige detalles no vacíos (req.getDetalles() puede ser null/[]).
        // Con subtotal=0, la única forma de cumplir 0<=descuento<subtotal es
        // descuento=0 — no se usa un caso especial que "deje pasar" un pedido vacío
        // con descuento distinto de cero, la misma condición general ya lo cubre
        // (descuento>0 && descuento>=subtotal(0) siempre es true para cualquier
        // descuento positivo).

        @Test
        void pedidoVacioConDescuentoCeroEsPermitido() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente pedido vacio");

                PedidoDTO creado = pedidoService.crear(req);
                assertThat(creado.getSubtotalCop()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(creado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(creado.getTotalCop()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(creado.getDetalles()).isEmpty();
        }

        @Test
        void pedidoVacioConDescuentoPositivoEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente pedido vacio con descuento");
                req.setDescuentoCop(BigDecimal.valueOf(100));

                assertThatThrownBy(() -> pedidoService.crear(req))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("debe ser menor al subtotal");
        }

        @Test
        void pedidoConProductosYDescuentoCero() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente con productos sin descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(2000), 1)));

                PedidoDTO creado = pedidoService.crear(req);
                assertThat(creado.getSubtotalCop()).isEqualByComparingTo(BigDecimal.valueOf(2000));
                assertThat(creado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(creado.getTotalCop()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        }

        // "pedido con productos y descuento válido" ya cubierto por
        // pedidoConDescuentoValidoCalculaSubtotalDescuentoYTotal (arriba).

        @Test
        void pedidoVacioNoPuedeFinalizarseNiCobrarse() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente pedido vacio finalizar");
                PedidoDTO creado = pedidoService.crear(req);

                assertThatThrownBy(() -> pedidoService.finalizarPedido(creado.getId()))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("no tiene ítems");

                assertThatThrownBy(() -> pedidoService.registrarPago(creado.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000))))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("sin ítems");
        }

        // ── Edición del descuento (flujo de actualizarPedido, mismo mecanismo que
        // el resto de "datos generales") ─────────────────────────────────────────────

        @Test
        void editarDescuentoAntesDePagar() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente edita descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(5000), 1)));
                PedidoDTO pedido = pedidoService.crear(req); // sin descuento inicial

                ActualizarPedidoRequest edit = new ActualizarPedidoRequest();
                edit.setNombre(pedido.getNombre());
                edit.setDescuentoCop(BigDecimal.valueOf(1000));
                PedidoDTO actualizado = pedidoService.actualizarPedido(pedido.getId(), edit);

                assertThat(actualizado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                assertThat(actualizado.getSubtotalCop()).isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(actualizado.getTotalCop()).isEqualByComparingTo(BigDecimal.valueOf(4000));
                assertThat(actualizado.getDescuentoAplicadoPor()).isNotNull();
        }

        @Test
        void cambiarDescuentoValidoPorCeroLimpiaOperador() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente quita descuento");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(5000), 1)));
                req.setDescuentoCop(BigDecimal.valueOf(1000));
                PedidoDTO pedido = pedidoService.crear(req);
                assertThat(pedido.getDescuentoAplicadoPor()).isNotNull();

                ActualizarPedidoRequest edit = new ActualizarPedidoRequest();
                edit.setNombre(pedido.getNombre());
                edit.setDescuentoCop(BigDecimal.ZERO);
                PedidoDTO actualizado = pedidoService.actualizarPedido(pedido.getId(), edit);

                assertThat(actualizado.getDescuentoCop()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(actualizado.getDescuentoAplicadoPor()).isNull();
                assertThat(actualizado.getTotalCop()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        }

        @Test
        void editarDescuentoAValorInvalidoEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente descuento invalido edicion");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(3000), 1)));
                PedidoDTO pedido = pedidoService.crear(req);

                ActualizarPedidoRequest edit = new ActualizarPedidoRequest();
                edit.setNombre(pedido.getNombre());
                edit.setDescuentoCop(BigDecimal.valueOf(3000)); // igual al subtotal -> invalido

                assertThatThrownBy(() -> pedidoService.actualizarPedido(pedido.getId(), edit))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("debe ser menor al subtotal");
        }

        @Test
        void editarDescuentoDespuesDePagoEsRechazado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente descuento tras pago");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(4000), 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", BigDecimal.valueOf(1000)));

                ActualizarPedidoRequest edit = new ActualizarPedidoRequest();
                edit.setNombre(pedido.getNombre());
                edit.setDescuentoCop(BigDecimal.valueOf(500));

                assertThatThrownBy(() -> pedidoService.actualizarPedido(pedido.getId(), edit))
                                .isInstanceOf(ValidacionException.class)
                                .hasMessageContaining("después de registrar un pago");
        }

        @Test
        void editarDescuentoEnPedidoFinalizadoEsRechazado() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente descuento finalizado");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", pedido.getTotalCop()));
                pedidoService.finalizarPedido(pedido.getId());

                ActualizarPedidoRequest edit = new ActualizarPedidoRequest();
                edit.setNombre(pedido.getNombre());
                edit.setDescuentoCop(BigDecimal.valueOf(500));

                // Rechazado por validarAbierto() — un FINALIZADO no admite ningún PATCH,
                // no solo el de descuento.
                assertThatThrownBy(() -> pedidoService.actualizarPedido(pedido.getId(), edit))
                                .isInstanceOf(ValidacionException.class);
        }

        @Test
        void operadorQueModificaDescuentoQuedaRegistrado() {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente registra operador edicion");
                req.setDetalles(List.of(new DetalleItemRequest(null, "Item", BigDecimal.valueOf(2000), 1)));
                PedidoDTO pedido = pedidoService.crear(req);

                ActualizarPedidoRequest edit = new ActualizarPedidoRequest();
                edit.setNombre(pedido.getNombre());
                edit.setDescuentoCop(BigDecimal.valueOf(500));
                PedidoDTO actualizado = pedidoService.actualizarPedido(pedido.getId(), edit);

                // Sin JWT en este contexto de test, resolverUsuario() cae a "sistema-local"
                // — lo que se prueba es que el campo se llena (trazabilidad de que ALGUIEN
                // aplicó el descuento), nunca queda null cuando el descuento es positivo.
                assertThat(actualizado.getDescuentoAplicadoPor()).isNotNull();
        }

        // ── Pedidos fiados ───────────────────────────────────────────────────────

        @Test
        void marcarFiadoDescuentaInventarioSinAcreditarFondo() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente fiado cuy");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);

                BigDecimal stockAntes = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();

                PedidoDTO fiado = pedidoService.marcarFiado(pedido.getId());

                assertThat(fiado.getEstado()).isEqualTo(EstadoPedido.FIADO.name());
                assertThat(fiado.getFiadoEn()).isNotNull();
                BigDecimal stockDespues = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();
                assertThat(stockDespues).isEqualByComparingTo(stockAntes.subtract(BigDecimal.ONE));

                // Nada pagado todavía — ningún fondo debe acreditarse al fiar
                assertThat(fondoRepository.findById("COP").orElseThrow().getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(fondoRepository.findById("EFECTIVO").orElseThrow().getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(cajaMovimientoRepository.findAllByOrderByRegistradoEnDesc())
                                .noneMatch(m -> CajaMovimientoTipo.COBRO_PEDIDO.equals(m.getTipo()));
        }

        @Test
        void pedidoFiadoNoPermiteEditarItemsPeroSiRegistrarPago() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente fiado congelado");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                PedidoDTO fiado = pedidoService.marcarFiado(pedido.getId());

                assertThatThrownBy(() -> pedidoService.agregarDetalle(fiado.getId(), detalleProducto("Gallina", 1)))
                                .isInstanceOf(ValidacionException.class);

                // Pero sí se le puede seguir abonando — congelado no significa cerrado a pagos
                PedidoDTO conAbono = pedidoService.registrarPago(fiado.getId(),
                                new RegistrarPagoRequest("COP", BigDecimal.valueOf(5000)));
                assertThat(conAbono.getTotalAbonado()).isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(conAbono.getEstado()).isEqualTo(EstadoPedido.FIADO.name());
        }

        @Test
        void saldarFiadoAcreditaFondoSinVolverADescontarInventario() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente fiado saldado");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                PedidoDTO fiado = pedidoService.marcarFiado(pedido.getId());
                BigDecimal stockTrasFiar = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();

                pedidoService.registrarPago(fiado.getId(), new RegistrarPagoRequest("COP", fiado.getTotalCop()));
                PedidoDTO finalizado = pedidoService.finalizarPedido(fiado.getId());

                assertThat(finalizado.getEstado()).isEqualTo(EstadoPedido.FINALIZADO.name());
                assertThat(fondoRepository.findById("EFECTIVO").orElseThrow().getSaldo())
                                .isEqualByComparingTo(fiado.getTotalCop());

                BigDecimal stockTrasSaldar = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();
                assertThat(stockTrasSaldar).isEqualByComparingTo(stockTrasFiar);

                long salidasDeEstePedido = movimientoInventarioRepository.findAll().stream()
                                .filter(m -> "pedido".equals(m.getReferenciaTipo())
                                                && pedido.getId().equals(m.getReferenciaId())
                                                && "salida".equals(m.getTipo()))
                                .count();
                assertThat(salidasDeEstePedido).isEqualTo(1);
        }

        @Test
        void anularPedidoFiadoRevierteInventario() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente fiado anulado");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                BigDecimal stockAntes = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();

                PedidoDTO fiado = pedidoService.marcarFiado(pedido.getId());
                PedidoDTO anulado = pedidoService.anularPedido(fiado.getId());

                assertThat(anulado.getEstado()).isEqualTo(EstadoPedido.ANULADO.name());
                BigDecimal stockDespues = inventarioRepository.findByInsumo("cuy").orElseThrow().getStockActual();
                assertThat(stockDespues).isEqualByComparingTo(stockAntes);
        }

        @Test
        void pedidoFiadoNoBloqueaCierreDeCaja() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente fiado no bloquea cierre");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                pedidoService.marcarFiado(pedido.getId());

                CierreCajaRequest request = new CierreCajaRequest();
                request.setFecha(LocalDate.now());
                request.setContadoEfectivo(BigDecimal.ZERO);
                request.setContadoUsd(BigDecimal.ZERO);
                request.setContadoNequi(BigDecimal.ZERO);

                CierreCajaDTO cierre = cierreCajaService.cerrar(request);
                assertThat(cierre.getId()).isNotNull();
        }

        @Test
        void resumenCuentaConsumoDeCocinaDePedidoFiadoElMismoDia() {
                asegurarStockAlto();
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Cliente fiado resumen");
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO pedido = pedidoService.crear(req);
                pedidoService.marcarFiado(pedido.getId());

                ResumenDiaDTO resumen = resumenService.obtener(hoyBogota());
                assertThat(resumen.getCuyesVendidos()).isEqualByComparingTo(BigDecimal.ONE);
        }

        // ── Helpers para los casos de arriba ────────────────────────────────────────

        private LocalDate hoyBogota() {
                return LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        }

        /**
         * crear() rechaza fechaReserva en el pasado, así que para simular una
         * reserva vencida (creada para hoy, nunca resuelta, ahora "de ayer" u
         * otro día anterior) se crea válida para hoy y se envejece directo vía
         * repositorio — el mismo estado en que quedaría una reserva real sin
         * atender.
         */
        private Long crearReservaHoyYEnvejecerA(LocalDate fechaEnvejecida) {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre("Reserva envejecida a " + fechaEnvejecida);
                req.setFechaReserva(hoyBogota().toString());
                req.setDetalles(List.of(detalleProducto("Cuy", 1)));
                PedidoDTO creado = pedidoService.crear(req);

                Pedido entidad = pedidoRepository.findById(creado.getId()).orElseThrow();
                entidad.setFechaReserva(fechaEnvejecida);
                pedidoRepository.save(entidad);
                return creado.getId();
        }

        private void asegurarStockAlto() {
                for (String insumo : new String[] { "cuy", "gallina" }) {
                        Inventario inv = inventarioRepository.findByInsumo(insumo).orElseThrow();
                        inv.setStockActual(BigDecimal.valueOf(50));
                        inventarioRepository.save(inv);
                }
                // El seed inicial (DataInitializer) crea Cuy/Gallina y sus fracciones con
                // precio 0.0 (se configuran luego desde la UI) — sin precio no hay saldo
                // que cobrar y finalizarPedido() nunca llega a PAGADO.
                for (String nombre : new String[] { "Cuy", "1/2 Cuy", "1/4 Cuy", "Gallina", "1/2 Gallina" }) {
                        Producto p = productoPorNombre(nombre);
                        p.setPrecio(BigDecimal.valueOf(10000));
                        productoRepository.save(p);
                }
        }

        private Producto productoPorNombre(String nombre) {
                return productoRepository.findByEliminadoFalseOrderByOrdenAsc().stream()
                                .filter(p -> nombre.equals(p.getNombre()))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Producto no encontrado en seed: " + nombre));
        }

        private DetalleItemRequest detalleProducto(String nombreProducto, int cantidad) {
                return new DetalleItemRequest(productoPorNombre(nombreProducto).getId(), null, null, cantidad);
        }

        private PedidoDTO crearPagarYFinalizar(String cliente, List<DetalleItemRequest> detalles, BigDecimal descuentoCop) {
                CrearPedidoRequest req = new CrearPedidoRequest();
                req.setNombre(cliente);
                req.setDetalles(detalles);
                if (descuentoCop != null) req.setDescuentoCop(descuentoCop);
                PedidoDTO pedido = pedidoService.crear(req);
                if (pedido.getTotalCop().compareTo(BigDecimal.ZERO) > 0) {
                        pedidoService.registrarPago(pedido.getId(), new RegistrarPagoRequest("COP", pedido.getTotalCop()));
                }
                return pedidoService.finalizarPedido(pedido.getId());
        }

        private PedidoDTO crearPedidoManual(String cliente, BigDecimal precioCop) {
                CrearPedidoRequest request = new CrearPedidoRequest();
                request.setNombre(cliente);
                request.setDetalles(List.of(new DetalleItemRequest(null, "Producto manual", precioCop, 1)));
                return pedidoService.crear(request);
        }

        private void ajustarFondo(String moneda, BigDecimal saldo) {
                AjusteCapitalRequest request = new AjusteCapitalRequest();
                request.setNuevoSaldo(saldo);
                request.setMotivo("Preparacion prueba");
                fondoService.ajustar(moneda, request);
        }
}
