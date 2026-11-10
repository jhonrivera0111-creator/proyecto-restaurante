package com.casaverde.backend.fachadaServices.services.compraService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCompra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.capaAccesoADatos.models.entitys.AdelantoProveedor;
import com.casaverde.backend.capaAccesoADatos.repositories.AbonoCompraRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.AdelantoProveedorRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.CompraRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;
import com.casaverde.backend.fachadaServices.mapper.AbonoCompraMapper;
import com.casaverde.backend.fachadaServices.mapper.CompraMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements ICompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraServiceImpl.class);

    private final CompraRepository compraRepo;
    private final ProveedorRepository proveedorRepo;
    private final AbonoCompraRepository abonoRepo;
    private final AdelantoProveedorRepository adelantoProveedorRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final CompraMapper compraMapper;
    private final AbonoCompraMapper abonoMapper;

    @Transactional(readOnly = true)
    public List<CompraDTO> listar(LocalDate fecha, EstadoCompra estado) {
        List<Compra> compras;
        if (fecha != null && estado != null) {
            compras = compraRepo.findByFechaAndEstadoOrderByCreadoEnDesc(fecha, estado);
        } else if (fecha != null) {
            compras = compraRepo.findByFechaOrderByCreadoEnDesc(fecha);
        } else if (estado != null) {
            compras = compraRepo.findByEstadoOrderByCreadoEnDesc(estado);
        } else {
            compras = compraRepo.findAllByOrderByCreadoEnDesc();
        }
        if (compras.isEmpty()) return List.of();
        List<Long> ids = compras.stream().map(Compra::getId).toList();
        Map<Long, List<AbonoCompra>> abonosPorCompra = abonoRepo.findByCompraIdIn(ids)
                .stream().collect(Collectors.groupingBy(a -> a.getCompra().getId()));
        return compras.stream()
                .map(c -> enrichCompraDTO(c, abonosPorCompra.getOrDefault(c.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompraDTO obtener(Long id) {
        Compra compra = compraRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        return enrichCompraDTO(compra, abonoRepo.findByCompraIdOrderByCreadoEnDesc(id));
    }

    @Transactional
    public CompraDTO registrar(CompraRequest req) {
        var proveedor = proveedorRepo.findById(req.getProveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + req.getProveedorId()));
        if (!proveedor.getActivo())
            throw new ValidacionException("El proveedor está inactivo");

        String insumoNorm = req.getInsumo().trim().toLowerCase();
        if (inventarioRepo.findByInsumo(insumoNorm).isEmpty())
            throw new ValidacionException("El insumo '" + insumoNorm + "' no existe en el inventario");

        BigDecimal totalCop = req.getCantidad().multiply(req.getPrecioUnitario())
                .setScale(0, RoundingMode.HALF_UP);

        Compra compra = compraRepo.save(Compra.builder()
                .proveedor(proveedor)
                .proveedorNombre(proveedor.getNombre())
                .insumo(insumoNorm)
                .cantidad(req.getCantidad())
                .precioUnitario(req.getPrecioUnitario().setScale(0, RoundingMode.HALF_UP))
                .totalCop(totalCop)
                .estado(EstadoCompra.PENDIENTE)
                .observacion(req.getObservacion())
                .fecha(req.getFecha())
                .build());

        sumarInventario(insumoNorm, req.getCantidad(), compra.getId());
        auditoriaService.registrar("Compra", compra.getId(),
                "REGISTRAR", "Compra de " + req.getCantidad() + " " + insumoNorm
                        + " a " + proveedor.getNombre() + " por $" + totalCop);
        log.info("[registrar] insumo='{}' cantidad={} proveedor='{}' totalCop={}", insumoNorm, req.getCantidad(), proveedor.getNombre(), totalCop);

        return enrichCompraDTO(compra, List.of());
    }

    @Transactional
    public void anular(Long id) {
        log.info("[anular] compraId={}", id);
        Compra compra = compraRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        if (compra.getEstado() != EstadoCompra.PENDIENTE)
            throw new ValidacionException("Solo se pueden anular compras sin abonos (estado PENDIENTE)");
        if (!abonoRepo.findByCompraIdOrderByCreadoEnDesc(id).isEmpty())
            throw new ValidacionException("No se puede anular una compra que tiene abonos registrados");

        revertirInventario(compra.getInsumo(), compra.getCantidad(), compra.getId());
        auditoriaService.registrar("Compra", compra.getId(), "ANULAR",
                "Compra anulada: " + compra.getCantidad() + " " + compra.getInsumo());
        compraRepo.delete(compra);
    }

    @Transactional
    public AbonoCompraDTO registrarAbono(Long compraId, AbonoCompraRequest req) {
        Compra compra = compraRepo.findById(compraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + compraId));
        if (compra.getEstado() == EstadoCompra.PAGADO)
            throw new ValidacionException("La compra ya está pagada completamente");

        // Una sola query de abonos; totalAbonado se deriva de la lista sin query extra
        List<AbonoCompra> abonosExistentes = abonoRepo.findByCompraIdOrderByCreadoEnDesc(compraId);
        BigDecimal totalAbonado = abonosExistentes.stream()
                .map(AbonoCompra::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoPendiente = compra.getTotalCop().subtract(totalAbonado);

        AbonoCompra abono;

        if (req.getAdelantoProveedorId() != null) {
            AdelantoProveedor adelanto = adelantoProveedorRepo.findById(req.getAdelantoProveedorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Adelanto proveedor no encontrado: " + req.getAdelantoProveedorId()));
            if (!adelanto.getActivo())
                throw new ValidacionException("El adelanto al proveedor ya fue agotado");
            if (!adelanto.getProveedor().getId().equals(compra.getProveedor().getId()))
                throw new ValidacionException("El adelanto no pertenece al proveedor de esta compra");

            BigDecimal montoCop = saldoPendiente.min(adelanto.getSaldoRestante());

            adelanto.setSaldoRestante(adelanto.getSaldoRestante().subtract(montoCop));
            if (adelanto.getSaldoRestante().compareTo(BigDecimal.ZERO) <= 0) {
                adelanto.setSaldoRestante(BigDecimal.ZERO);
                adelanto.setActivo(false);
            }
            adelantoProveedorRepo.save(adelanto);

            // El fondo fue debitado al registrar el adelanto (ADELANTO_PROVEEDOR).
            // Esta aplicación descuenta del saldo del adelanto sin tocar el fondo.
            String monedaAdelanto = adelanto.getMoneda();
            BigDecimal tasaUsd = tasaCambioService.getValorVigente();
            BigDecimal montoOriginal = MonedaConstants.USD.equals(monedaAdelanto)
                    ? montoCop.divide(tasaUsd, 2, RoundingMode.HALF_UP)
                    : montoCop;
            BigDecimal tasaUsadaAdelanto = MonedaConstants.USD.equals(monedaAdelanto) ? tasaUsd : null;

            abono = abonoRepo.save(AbonoCompra.builder()
                    .compra(compra)
                    .monto(montoOriginal)
                    .moneda(monedaAdelanto)
                    .montoCop(montoCop)
                    .tasaUsada(tasaUsadaAdelanto)
                    .fondoDebitado("ADELANTO_PROVEEDOR")
                    .adelantoProveedor(adelanto)
                    .fecha(req.getFecha())
                    .build());

            auditoriaService.registrar("AbonoCompra", abono.getId(), "REGISTRAR",
                    "Abono $" + montoOriginal + " " + monedaAdelanto + " (=" + montoCop + " COP) aplicado de adelanto proveedor #"
                            + adelanto.getId() + " a compra #" + compraId);

            compra.setEstado(EstadoCompra.calcular(compra.getTotalCop(), totalAbonado.add(montoCop)));
            compraRepo.save(compra);

        } else {
            if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
                throw new ValidacionException("El monto del abono debe ser mayor a 0");
            if (req.getMoneda() == null || req.getMoneda().isBlank())
                throw new ValidacionException("La moneda es obligatoria");

            String moneda = req.getMoneda().trim().toUpperCase();
            BigDecimal tasaUsd = tasaCambioService.getValorVigente();
            BigDecimal montoCop = monetaryService.convertirACop(moneda, req.getMonto(), tasaUsd);
            BigDecimal tasaUsada = MonedaConstants.USD.equals(moneda) ? tasaUsd : null;

            if (montoCop.compareTo(saldoPendiente) > 0)
                throw new ValidacionException(
                        "El abono ($" + montoCop + ") supera el saldo pendiente ($" + saldoPendiente + ")");

            cajaMovimientoService.registrar(
                    CajaMovimientoTipo.COMPRA_PAGO,
                    moneda,
                    req.getMonto().negate(),
                    montoCop.negate(),
                    "compra",
                    compraId,
                    "Pago compra #" + compraId + " (" + compra.getInsumo() + ")",
                    null);

            abono = abonoRepo.save(AbonoCompra.builder()
                    .compra(compra)
                    .monto(req.getMonto())
                    .moneda(moneda)
                    .montoCop(montoCop)
                    .tasaUsada(tasaUsada)
                    .fondoDebitado(moneda)
                    .fecha(req.getFecha())
                    .build());

            compra.setEstado(EstadoCompra.calcular(compra.getTotalCop(), totalAbonado.add(montoCop)));
            compraRepo.save(compra);

            auditoriaService.registrar("AbonoCompra", abono.getId(), "REGISTRAR",
                    "Abono $" + req.getMonto() + " " + moneda + " a compra #" + compraId);
        }

        log.info("[abono] compraId={} monto={} moneda={} conAdelanto={}", compraId, req.getMonto(), req.getMoneda(), req.getAdelantoProveedorId() != null);
        return abonoMapper.toDTO(abono);
    }

    @Transactional(readOnly = true)
    public List<AbonoCompraDTO> listarAbonos(Long compraId) {
        if (!compraRepo.existsById(compraId))
            throw new RecursoNoEncontradoException("Compra no encontrada: " + compraId);
        return abonoRepo.findByCompraIdOrderByCreadoEnDesc(compraId).stream()
                .map(abonoMapper::toDTO)
                .collect(Collectors.toList());
    }

    // --- helpers ---

    private CompraDTO enrichCompraDTO(Compra compra, List<AbonoCompra> abonos) {
        CompraDTO dto = compraMapper.toDTO(compra);
        BigDecimal totalAbonado = abonos.stream()
                .map(AbonoCompra::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalAbonado(totalAbonado);
        dto.setSaldoPendiente(compra.getTotalCop().subtract(totalAbonado));
        dto.setAbonos(abonos.stream().map(abonoMapper::toDTO).collect(Collectors.toList()));
        return dto;
    }

    private void sumarInventario(String insumo, BigDecimal cantidad, Long compraId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("Insumo no encontrado en inventario: " + insumo));
        BigDecimal nuevo = inv.getStockActual().add(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo("entrada")
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("compra")
                .referenciaId(compraId)
                .observacion("Compra #" + compraId)
                .build());
    }

    private void revertirInventario(String insumo, BigDecimal cantidad, Long compraId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException("Insumo no encontrado en inventario: " + insumo));
        BigDecimal nuevo = inv.getStockActual().subtract(cantidad);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidacionException(
                    "Stock insuficiente para revertir la compra. Stock actual de " + insumo
                    + ": " + inv.getStockActual() + ", a revertir: " + cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo("ajuste")
                .cantidad(cantidad.negate())
                .stockResultante(nuevo)
                .referenciaTipo("compra_anulada")
                .referenciaId(compraId)
                .observacion("Anulación compra #" + compraId)
                .build());
    }
}
