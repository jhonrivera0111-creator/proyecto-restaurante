package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;
import com.casaverde.backend.capaAccesoADatos.repositories.ProductoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.fachadaServices.dto.ActualizarDetalleRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoHistoricoRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoTotales;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.mapper.PedidoMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements IPedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final SalonRepository salonRepo;
    private final CajaMovimientoRepository cajaMovimientoRepo;
    private final MovimientoInventarioRepository movimientoInventarioRepo;
    private final IComprobanteService comprobanteService;
    private final ITasaCambioService tasaCambioService;
    private final IFondoService fondoService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final IPedidoFinalizacionService finalizacionService;
    private final ObjectMapper objectMapper;
    private final PedidoMapper pedidoMapper;

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarActivos() {
        return pedidoRepo.findByEstadoInOrderByCreadoEnDesc(EstadoPedido.activos())
                .stream().map(p -> pedidoMapper.toDTO(p, calcularTotales(p))).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepo.findAllByOrderByCreadoEnDesc()
                .stream().map(p -> pedidoMapper.toDTO(p, calcularTotales(p))).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PedidoDTO> listarTodosPaginado(Pageable pageable) {
        return pedidoRepo.findAllByOrderByCreadoEnDesc(pageable)
                .map(p -> pedidoMapper.toDTO(p, calcularTotales(p)));
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long id) {
        Pedido pedido = findOrThrow(id);
        return pedidoMapper.toDTO(pedido, calcularTotales(pedido));
    }

    @Transactional(readOnly = true)
    public String comprobanteTexto(Long id) {
        Pedido pedido = findOrThrow(id);
        return comprobanteService.generar(pedidoMapper.toDTO(pedido, calcularTotales(pedido)));
    }

    @Transactional
    public PedidoDTO crear(CrearPedidoRequest req) {
        boolean tieneFecha = req.getFechaReserva() != null && !req.getFechaReserva().isBlank();
        boolean tieneHora  = req.getHoraReserva()  != null && !req.getHoraReserva().isBlank();
        if (tieneFecha || tieneHora) {
            ZonedDateTime now   = ZonedDateTime.now(ZoneId.of("America/Bogota"));
            LocalDate     today = now.toLocalDate();
            LocalDate rFecha = tieneFecha ? LocalDate.parse(req.getFechaReserva()) : today;
            if (rFecha.isBefore(today))
                throw new ValidacionException("La fecha de reserva ya pasó");
            if (rFecha.isEqual(today) && tieneHora) {
                LocalTime rHora = LocalTime.parse(req.getHoraReserva());
                if (!rHora.isAfter(now.toLocalTime()))
                    throw new ValidacionException("La hora de reserva ya pasó");
            }
        }

        log.info("[crear] nombre='{}' detalles={}", req.getNombre(), req.getDetalles() != null ? req.getDetalles().size() : 0);

        Pedido pedido = Pedido.builder()
                .nombre(req.getNombre().trim())
                .mesa(req.getMesa())
                .fechaReserva(req.getFechaReserva() != null && !req.getFechaReserva().isBlank()
                        ? LocalDate.parse(req.getFechaReserva())
                        : null)
                .horaReserva(req.getHoraReserva() != null && !req.getHoraReserva().isBlank()
                        ? LocalTime.parse(req.getHoraReserva())
                        : null)
                .cantPersonas(req.getCantPersonas())
                .observacion(req.getObservacion())
                .paraLlevar(Boolean.TRUE.equals(req.getParaLlevar()))
                .telefono(normalizarTelefono(req.getTelefono()))
                .estado(EstadoPedido.PENDIENTE)
                .build();

        if (req.getSalonId() != null) {
            pedido.setSalon(salonRepo.findById(req.getSalonId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + req.getSalonId())));
        }

        if (req.getDetalles() != null) {
            for (DetalleItemRequest d : req.getDetalles()) {
                pedido.getDetalles().add(buildDetalle(pedido, d));
            }
        }

        BigDecimal subtotal = calcularTotales(pedido).subtotalCop();
        BigDecimal descuento = monetaryService.normalizarPrecioCop(req.getDescuentoCop());
        if (descuento.compareTo(BigDecimal.ZERO) > 0 && descuento.compareTo(subtotal) >= 0)
            throw new ValidacionException(
                    "El descuento ($" + descuento + ") debe ser menor al subtotal ($" + subtotal + ")");
        pedido.setDescuentoCop(descuento);
        if (descuento.compareTo(BigDecimal.ZERO) > 0)
            pedido.setDescuentoAplicadoPor(auditoriaService.usuarioActual());

        pedidoRepo.save(pedido);

        if (req.getAbono() != null && req.getAbono().compareTo(BigDecimal.ZERO) > 0) {
            String moneda = monetaryService
                    .normalizarMoneda(resolverMoneda(req.getMonedaAbono() != null ? req.getMonedaAbono() : MonedaConstants.COP));
            BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getAbono());
            BigDecimal tasa = MonedaConstants.USD.equals(moneda) ? getTasaVigente() : null;
            BigDecimal montoCop = monetaryService.convertirACop(moneda, monto, tasa);
            // El abono queda registrado en el pedido; el fondo solo se acredita al
            // finalizar
            pedido.getPagos().add(Pago.builder()
                    .pedido(pedido).moneda(moneda).monto(monto)
                    .montoCop(montoCop).tasaUsada(tasa).build());
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "CREAR",
                "Pedido creado para " + guardado.getNombre());
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO actualizarPedido(Long pedidoId, ActualizarPedidoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "actualizar");

        if (req.getNombre() != null && !req.getNombre().isBlank())
            pedido.setNombre(req.getNombre().trim());

        pedido.setFechaReserva(req.getFechaReserva() != null && !req.getFechaReserva().isBlank()
                ? LocalDate.parse(req.getFechaReserva())
                : null);
        pedido.setHoraReserva(req.getHoraReserva() != null && !req.getHoraReserva().isBlank()
                ? LocalTime.parse(req.getHoraReserva())
                : null);
        pedido.setMesa(req.getMesa());
        pedido.setCantPersonas(req.getCantPersonas());
        pedido.setObservacion(req.getObservacion());
        pedido.setTelefono(normalizarTelefono(req.getTelefono()));
        // Solo actualizar paraLlevar si viene explícito; null = no tocar
        if (req.getParaLlevar() != null) {
            pedido.setParaLlevar(req.getParaLlevar());
        }

        if (req.getSalonId() != null) {
            pedido.setSalon(salonRepo.findById(req.getSalonId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + req.getSalonId())));
        } else {
            pedido.setSalon(null);
        }

        // descuentoCop null = no se intenta modificar el descuento (la mayoría de
        // los PATCH de "datos generales" no lo tocan). Si viene, y difiere del
        // actual, se valida y se re-audita — nunca se confía en el total que
        // pudiera calcular el frontend, siempre se recalcula aquí.
        if (req.getDescuentoCop() != null) {
            BigDecimal nuevoDescuento = monetaryService.normalizarPrecioCop(req.getDescuentoCop());
            if (nuevoDescuento.compareTo(pedido.getDescuentoCop()) != 0) {
                if (!pedido.getPagos().isEmpty())
                    throw new ValidacionException(
                            "No se puede modificar el descuento después de registrar un pago");
                BigDecimal subtotalActual = calcularTotales(pedido).subtotalCop();
                if (nuevoDescuento.compareTo(BigDecimal.ZERO) > 0 && nuevoDescuento.compareTo(subtotalActual) >= 0)
                    throw new ValidacionException("El descuento ($" + nuevoDescuento
                            + ") debe ser menor al subtotal ($" + subtotalActual + ")");
                pedido.setDescuentoCop(nuevoDescuento);
                // Sin razón funcional para conservar trazabilidad al volver a cero — se
                // limpia el operador registrado previamente.
                pedido.setDescuentoAplicadoPor(
                        nuevoDescuento.compareTo(BigDecimal.ZERO) > 0 ? auditoriaService.usuarioActual() : null);
            }
        }

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR",
                "Datos generales del pedido actualizados");
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO agregarDetalle(Long pedidoId, DetalleItemRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "agregar ítems");

        Optional<PedidoDetalle> existente = buscarDetalleExistente(pedido, req);
        if (existente.isPresent()) {
            existente.get().setCantidad(existente.get().getCantidad() + req.getCantidad());
        } else {
            pedido.getDetalles().add(buildDetalle(pedido, req));
        }

        recalcularEstadoPago(pedido);
        validarInvarianteDescuento(pedido, calcularTotales(pedido).subtotalCop(), "agregar este ítem");
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "AGREGAR_ITEM",
                "Item agregado o acumulado en el pedido");
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO quitarDetalle(Long pedidoId, Long detalleId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "quitar ítems");
        boolean removed = pedido.getDetalles().removeIf(d -> d.getId().equals(detalleId));
        recalcularEstadoPago(pedido);
        if (!removed)
            throw new RecursoNoEncontradoException("Ítem no encontrado: " + detalleId);
        validarInvarianteDescuento(pedido, calcularTotales(pedido).subtotalCop(), "quitar este ítem");
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "QUITAR_ITEM",
                "Item eliminado del pedido: " + detalleId);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO actualizarDetalle(Long pedidoId, Long detalleId, ActualizarDetalleRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarAbierto(pedido, "editar ítems");

        PedidoDetalle detalle = pedido.getDetalles().stream()
                .filter(d -> d.getId().equals(detalleId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Ítem no encontrado: " + detalleId));

        if (req.getCantidad() == 0) {
            pedido.getDetalles().remove(detalle);
        } else {
            detalle.setCantidad(req.getCantidad());
        }
        recalcularEstadoPago(pedido);
        validarInvarianteDescuento(pedido, calcularTotales(pedido).subtotalCop(), "actualizar este ítem");
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_ITEM",
                "Item actualizado en el pedido: " + detalleId);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO registrarPago(Long pedidoId, RegistrarPagoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        if (EstadoPedido.ANULADO == pedido.getEstado())
            throw new ValidacionException("No se puede registrar pago en un pedido ANULADO");
        if (EstadoPedido.FINALIZADO == pedido.getEstado())
            throw new ValidacionException("El pedido ya está FINALIZADO");
        if (pedido.getDetalles().isEmpty())
            throw new ValidacionException("No se puede registrar un pago en un pedido sin ítems");

        String moneda = monetaryService.normalizarMoneda(resolverMoneda(req.getMoneda()));
        BigDecimal monto = monetaryService.normalizarMontoEntrada(moneda, req.getMonto());
        BigDecimal tasaUsada = MonedaConstants.USD.equals(moneda) ? getTasaVigente() : null;
        BigDecimal montoCop = monetaryService.convertirACop(moneda, monto, tasaUsada);

        // El cobro queda en el pedido; el fondo solo se acredita al finalizar
        Pago pago = Pago.builder()
                .pedido(pedido).moneda(moneda).monto(monto)
                .montoCop(montoCop).tasaUsada(tasaUsada).build();
        pedido.getPagos().add(pago);

        recalcularEstadoPago(pedido);
        log.info("[registrarPago] pedidoId={} moneda={} montoCop={}", pedidoId, moneda, montoCop);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "REGISTRAR_PAGO",
                "Pago " + moneda + " " + monto + " equivalente COP " + montoCop);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO finalizarPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        log.info("[finalizar] pedidoId={}", pedidoId);
        boolean veniaDeFiado = EstadoPedido.FIADO == pedido.getEstado();
        if (!veniaDeFiado) {
            validarAbierto(pedido, "finalizar");
        }

        if (pedido.getDetalles().isEmpty())
            throw new ValidacionException("El pedido no tiene ítems");

        PedidoTotales totalesFinalizacion = calcularTotales(pedido);
        BigDecimal total = totalesFinalizacion.totalCop();
        BigDecimal abonado = totalesFinalizacion.totalAbonado();
        // El invariante 0 <= descuento < subtotal ya se garantiza en cada punto de
        // escritura (crear/agregar/actualizar/quitar ítem); se re-verifica aquí
        // porque este es el punto sin retorno: se descuenta inventario (o se
        // acredita el fondo, si ya venía fiado) con "total".
        validarInvarianteDescuento(pedido, totalesFinalizacion.subtotalCop(), "finalizar el pedido");

        if (veniaDeFiado) {
            // El inventario ya se descontó al marcar el pedido como fiado — acá
            // solo falta que la cuenta quede saldada en $0 para poder cerrarla.
            if (abonado.compareTo(total) < 0) {
                throw new ValidacionException(String.format(
                        "Saldo pendiente del fiado: $%.0f COP. Registre el abono completo antes de saldar la cuenta.",
                        total.subtract(abonado)));
            }
        } else {
            recalcularEstadoPago(pedido);
            if (EstadoPedido.PAGADO != pedido.getEstado()) {
                throw new ValidacionException(String.format(
                        "Saldo pendiente: $%.0f COP. Registre el pago completo antes de finalizar.",
                        total.subtract(abonado)));
            }

            // Validar que la hora de la reserva ya haya llegado. No aplica a un
            // pedido que viene de fiado: la comida ya se sirvió en su momento.
            if (pedido.getFechaReserva() != null || pedido.getHoraReserva() != null) {
                java.time.ZoneId bogota = java.time.ZoneId.of("America/Bogota");
                LocalDate hoy = LocalDate.now(bogota);
                LocalDate fechaRes = pedido.getFechaReserva() != null ? pedido.getFechaReserva() : hoy;
                boolean reservaFutura;
                if (fechaRes.isAfter(hoy)) {
                    reservaFutura = true;
                } else if (fechaRes.isBefore(hoy)) {
                    reservaFutura = false;
                } else { // misma fecha: revisar hora
                    reservaFutura = pedido.getHoraReserva() != null &&
                            pedido.getHoraReserva().isAfter(LocalTime.now(bogota));
                }
                if (reservaFutura) {
                    String cuando = (pedido.getFechaReserva() != null && pedido.getHoraReserva() != null)
                            ? pedido.getFechaReserva() + " a las " + pedido.getHoraReserva()
                            : pedido.getFechaReserva() != null ? pedido.getFechaReserva().toString()
                                    : "las " + pedido.getHoraReserva();
                    throw new ValidacionException(
                            "No se puede finalizar antes de la reserva (" + cuando + ")");
                }
            }
        }

        pedido.setEstado(EstadoPedido.FINALIZADO);
        pedido.setFinalizadoEn(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        Pedido guardado = pedidoRepo.save(pedido);

        if (veniaDeFiado) {
            finalizacionService.ejecutarSoloCobro(guardado, total);
        } else {
            finalizacionService.ejecutar(guardado, total);
        }

        log.info("[finalizar] pedidoId={} COMPLETADO", pedidoId);
        auditoriaService.registrar("pedido", guardado.getId(), veniaDeFiado ? "SALDAR_FIADO" : "FINALIZAR",
                (veniaDeFiado ? "Cuenta fiada saldada con total COP " : "Pedido finalizado con total COP ") + total);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    /**
     * Carga retroactiva: crea, cobra y finaliza un pedido en una sola operación
     * (afecta fondos e inventario de verdad, igual que un pedido normal), y al
     * final retrasa creadoEn/finalizadoEn del pedido y registradoEn de los
     * movimientos de caja/inventario/pagos generados a la fecha real indicada.
     * Sin esto, Resumen/Historial/Libro de caja ubicarían todo en "hoy" — ver
     * ResumenServiceImpl (usa finalizadoEn/fiadoEn, no fechaReserva) y
     * CajaMovimiento/MovimientoInventario/Pago (registradoEn vía @PrePersist,
     * sin forma de pasarlo por la API normal).
     * No toca la validación de "fecha de reserva ya pasó": esta carga nunca
     * setea fechaReserva/horaReserva, así que esa validación ni se ejecuta.
     */
    @Transactional
    public PedidoDTO crearHistorico(PedidoHistoricoRequest req) {
        log.info("[crearHistorico] nombre='{}' fecha={} detalles={}",
                req.getNombre(), req.getFecha(), req.getDetalles().size());

        CrearPedidoRequest crearReq = new CrearPedidoRequest();
        crearReq.setNombre(req.getNombre());
        crearReq.setDetalles(req.getDetalles());
        crearReq.setDescuentoCop(req.getDescuentoCop());
        String nota = "Carga histórica (" + req.getFecha() + ")";
        crearReq.setObservacion(req.getObservacion() != null && !req.getObservacion().isBlank()
                ? req.getObservacion() + " — " + nota
                : nota);
        Long pedidoId = crear(crearReq).getId();

        for (RegistrarPagoRequest pagoReq : req.getPagos()) {
            registrarPago(pedidoId, pagoReq);
        }
        finalizarPedido(pedidoId);

        LocalDateTime fechaHistorica = req.getFecha().atTime(12, 0);
        Pedido pedido = findOrThrow(pedidoId);
        pedido.setCreadoEn(fechaHistorica);
        pedido.setFinalizadoEn(fechaHistorica);
        for (Pago p : pedido.getPagos())
            p.setRegistradoEn(fechaHistorica);
        pedidoRepo.save(pedido);

        cajaMovimientoRepo.findByReferenciaTipoAndReferenciaId("pedido", pedidoId)
                .forEach(m -> { m.setRegistradoEn(fechaHistorica); cajaMovimientoRepo.save(m); });
        movimientoInventarioRepo.findByReferenciaTipoAndReferenciaId("pedido", pedidoId)
                .forEach(m -> { m.setRegistradoEn(fechaHistorica); movimientoInventarioRepo.save(m); });

        auditoriaService.registrar("pedido", pedidoId, "CARGA_HISTORICA",
                "Pedido histórico cargado con fecha " + req.getFecha()
                        + " — fondos e inventario afectados retroactivamente");
        log.info("[crearHistorico] pedidoId={} fecha={} COMPLETADO", pedidoId, req.getFecha());
        return obtener(pedidoId);
    }

    @Transactional
    public PedidoDTO marcarFiado(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        log.info("[marcarFiado] pedidoId={}", pedidoId);
        validarAbierto(pedido, "marcar como fiado");

        if (pedido.getDetalles().isEmpty())
            throw new ValidacionException("El pedido no tiene ítems");

        PedidoTotales totales = calcularTotales(pedido);
        validarInvarianteDescuento(pedido, totales.subtotalCop(), "marcar el pedido como fiado");

        pedido.setEstado(EstadoPedido.FIADO);
        pedido.setFiadoEn(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        Pedido guardado = pedidoRepo.save(pedido);

        // La comida se sirve ahora: el inventario se descuenta de inmediato,
        // igual que al finalizar. El fondo NO se toca — no ha entrado dinero
        // (o solo entró parcialmente); el saldo pendiente queda como deuda,
        // visible desde Cuentas por Cobrar hasta que se termine de pagar.
        finalizacionService.ejecutarSoloInventario(guardado);

        log.info("[marcarFiado] pedidoId={} FIADO saldo={}", pedidoId, totales.saldoPendiente());
        auditoriaService.registrar("pedido", guardado.getId(), "MARCAR_FIADO",
                "Pedido marcado como fiado. Saldo pendiente: $" + totales.saldoPendiente());
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO anularPedido(Long pedidoId) {
        Pedido pedido = findOrThrow(pedidoId);
        log.info("[anular] pedidoId={}", pedidoId);
        if (EstadoPedido.ANULADO == pedido.getEstado())
            throw new ValidacionException("El pedido ya está ANULADO");
        if (EstadoPedido.FINALIZADO == pedido.getEstado())
            throw new ValidacionException("No se puede anular un pedido FINALIZADO");

        boolean veniaDeFiado = EstadoPedido.FIADO == pedido.getEstado();

        // Los pagos previos nunca tocaron el fondo (el fondo solo se acredita al
        // finalizar), por lo que no hay nada que revertir ahí — solo cambiar el
        // estado. El inventario sí se descontó si el pedido venía fiado (la
        // comida ya se sirvió): eso se revierte antes de anular.
        pedido.setEstado(EstadoPedido.ANULADO);
        Pedido guardado = pedidoRepo.save(pedido);

        if (veniaDeFiado) {
            finalizacionService.revertirInventario(guardado);
        }

        auditoriaService.registrar("pedido", guardado.getId(), "ANULAR",
                "Pedido anulado con " + pedido.getPagos().size() + " pago(s) sin impacto en fondos"
                        + (veniaDeFiado ? "; inventario revertido (venía fiado)" : ""));
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO quitarPago(Long pedidoId, Long pagoId) {
        Pedido pedido = findOrThrow(pedidoId);
        validarPuedeRecibirPago(pedido, "eliminar cobros");

        Pago pago = pedido.getPagos().stream()
                .filter(p -> p.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Cobro no encontrado: " + pagoId));

        // El pago nunca ingresó al fondo; simplemente se elimina del pedido
        pedido.getPagos().remove(pago);
        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "QUITAR_PAGO",
                "Pago eliminado: " + pagoId);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    @Transactional
    public PedidoDTO actualizarPago(Long pedidoId, Long pagoId, ActualizarPagoRequest req) {
        Pedido pedido = findOrThrow(pedidoId);
        validarPuedeRecibirPago(pedido, "editar pagos");

        Pago pago = pedido.getPagos().stream()
                .filter(p -> p.getId().equals(pagoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + pagoId));

        // Calcular nuevos valores (sin movimiento de fondo — solo se acredita al
        // finalizar)
        String nuevaMoneda = monetaryService.normalizarMoneda(resolverMoneda(req.getMoneda()));
        BigDecimal nuevoMonto = monetaryService.normalizarMontoEntrada(nuevaMoneda, req.getMonto());
        BigDecimal nuevaTasa = MonedaConstants.USD.equals(nuevaMoneda) ? getTasaVigente() : null;
        BigDecimal nuevoMontoCop = monetaryService.convertirACop(nuevaMoneda, nuevoMonto, nuevaTasa);

        pago.setMoneda(nuevaMoneda);
        pago.setMonto(nuevoMonto);
        pago.setMontoCop(nuevoMontoCop);
        pago.setTasaUsada(nuevaTasa);

        recalcularEstadoPago(pedido);
        Pedido guardado = pedidoRepo.save(pedido);
        auditoriaService.registrar("pedido", guardado.getId(), "ACTUALIZAR_PAGO",
                "Pago actualizado: " + pagoId + " a " + nuevaMoneda + " " + nuevoMonto);
        return pedidoMapper.toDTO(guardado, calcularTotales(guardado));
    }

    // ---- private helpers ----

    private Optional<PedidoDetalle> buscarDetalleExistente(Pedido pedido, DetalleItemRequest req) {
        if (req.getProductoId() != null) {
            return pedido.getDetalles().stream()
                    .filter(d -> d.getProducto() != null
                            && d.getProducto().getId().equals(req.getProductoId()))
                    .findFirst();
        }
        String nombre = req.getNombreManual() != null ? req.getNombreManual().trim() : "";
        BigDecimal precio = monetaryService.normalizarPrecioCop(req.getPrecioManual());
        return pedido.getDetalles().stream()
                .filter(d -> d.getProducto() == null
                        && nombre.equalsIgnoreCase(d.getNombreSnapshot())
                        && precio.compareTo(d.getPrecioUnitarioCop()) == 0)
                .findFirst();
    }

    private PedidoDetalle buildDetalle(Pedido pedido, DetalleItemRequest req) {
        PedidoDetalle detalle = new PedidoDetalle();
        detalle.setPedido(pedido);
        detalle.setCantidad(req.getCantidad());

        if (req.getProductoId() != null) {
            Producto prod = productoRepo.findById(req.getProductoId())
                    .orElseThrow(
                            () -> new RecursoNoEncontradoException("Producto no encontrado: " + req.getProductoId()));
            if (!Boolean.TRUE.equals(prod.getActivo()))
                throw new ValidacionException(
                        "El producto '" + prod.getNombre() + "' está inactivo y no puede agregarse a pedidos");
            detalle.setProducto(prod);
            detalle.setNombreSnapshot(prod.getNombre());
            detalle.setPrecioUnitarioCop(prod.getPrecio());
            if (!prod.getComposicion().isEmpty()) {
                detalle.setComposicionSnapshot(serializarComposicion(prod.getComposicion()));
            }
        } else {
            detalle.setNombreSnapshot(req.getNombreManual().trim());
            detalle.setPrecioUnitarioCop(monetaryService.normalizarPrecioCop(req.getPrecioManual()));
        }

        return detalle;
    }

    private String serializarComposicion(List<ProductoComposicionInventario> composicion) {
        try {
            List<Map<String, Object>> list = composicion.stream()
                    .map(c -> Map.<String, Object>of("insumo", c.getInsumo(), "cantidad", c.getCantidad()))
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new ValidacionException("Error serializando composición de inventario: " + e.getMessage());
        }
    }

    /**
     * Si la moneda es exactamente "NEQUI" la resuelve a la cuenta activa
     * configurada
     * (puede ser NEQUI_2, NEQUI_3, etc.). Cualquier otra moneda se devuelve sin
     * cambio.
     */
    private String resolverMoneda(String moneda) {
        if (moneda == null)
            return MonedaConstants.COP;
        return MonedaConstants.NEQUI.equalsIgnoreCase(moneda.trim()) ? fondoService.getCuentaNequiActiva() : moneda.trim();
    }

    private BigDecimal getTasaVigente() {
        return tasaCambioService.getValorVigente();
    }

    /**
     * Recorta espacios externos; cadena en blanco se guarda como null (nunca se
     * inventa el número). SQLite no impone longitud real sobre varchar(30) — el
     * límite de 30 caracteres (igual al de la columna, el DTO y el frontend) se
     * exige aquí porque @Size en el DTO solo se aplica si la llamada pasa por
     * @Valid en el controller.
     */
    private String normalizarTelefono(String telefono) {
        if (telefono == null) return null;
        String limpio = telefono.trim();
        if (limpio.isEmpty()) return null;
        if (limpio.length() > 30)
            throw new ValidacionException("El teléfono no puede superar 30 caracteres");
        return limpio;
    }

    /**
     * Garantiza 0 <= descuentoCop < subtotalCop tras cualquier cambio que pueda
     * alterar el subtotal (agregar/actualizar/quitar detalle, finalizar). Si el
     * pedido no tiene descuento (0) no hay nada que validar — un subtotal 0 con
     * descuento 0 es válido (p.ej. al quitar el último ítem). No reduce ni anula
     * el descuento automáticamente: rechaza la operación completa.
     */
    private void validarInvarianteDescuento(Pedido pedido, BigDecimal nuevoSubtotal, String accion) {
        BigDecimal descuento = pedido.getDescuentoCop();
        if (descuento == null || descuento.compareTo(BigDecimal.ZERO) <= 0) return;
        if (descuento.compareTo(nuevoSubtotal) >= 0) {
            throw new ValidacionException("No se puede " + accion + ": el descuento ($" + descuento
                    + ") quedaría mayor o igual al nuevo subtotal ($" + nuevoSubtotal
                    + "). Ajusta el descuento antes de continuar.");
        }
    }

    private void recalcularEstadoPago(Pedido pedido) {
        if (!pedido.getEstado().permiteEdicion()) return;
        PedidoTotales t = calcularTotales(pedido);
        pedido.setEstado(EstadoPedido.calcular(t.totalCop(), t.totalAbonado()));
    }

    private void validarAbierto(Pedido pedido, String accion) {
        if (!pedido.getEstado().permiteEdicion()) {
            log.warn("[validarAbierto] pedidoId={} accion='{}' estado={}", pedido.getId(), accion, pedido.getEstado());
            throw new ValidacionException("No se puede " + accion + " en un pedido " + pedido.getEstado());
        }
    }

    /**
     * Igual que validarAbierto, pero permite además el estado FIADO — un
     * pedido fiado tiene los ítems congelados (validarAbierto lo rechaza)
     * pero debe poder seguir recibiendo/editando/quitando abonos hasta
     * saldarse.
     */
    private void validarPuedeRecibirPago(Pedido pedido, String accion) {
        if (!pedido.getEstado().permiteRegistrarPago()) {
            log.warn("[validarPuedeRecibirPago] pedidoId={} accion='{}' estado={}", pedido.getId(), accion, pedido.getEstado());
            throw new ValidacionException("No se puede " + accion + " en un pedido " + pedido.getEstado());
        }
    }

    private PedidoTotales calcularTotales(Pedido pedido) {
        return PedidoTotales.of(pedido);
    }

    private Pedido findOrThrow(Long id) {
        return pedidoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

}
