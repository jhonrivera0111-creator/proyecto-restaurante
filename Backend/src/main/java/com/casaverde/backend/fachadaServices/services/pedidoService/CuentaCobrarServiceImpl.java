package com.casaverde.backend.fachadaServices.services.pedidoService;

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

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCuentaCobrar;
import com.casaverde.backend.capaAccesoADatos.models.entitys.CuentaCobrar;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCuentaCobrar;
import com.casaverde.backend.capaAccesoADatos.repositories.AbonoCuentaCobrarRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.CuentaCobrarRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest;
import com.casaverde.backend.fachadaServices.mapper.CuentaCobrarMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuentaCobrarServiceImpl implements ICuentaCobrarService {

    private static final Logger log = LoggerFactory.getLogger(CuentaCobrarServiceImpl.class);

    private final CuentaCobrarRepository cuentaRepo;
    private final AbonoCuentaCobrarRepository abonoRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final CuentaCobrarMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaCobrarDTO> listar(EstadoCuentaCobrar estado, LocalDate fecha, String q) {
        List<CuentaCobrar> lista;
        if (q != null && !q.isBlank()) {
            lista = cuentaRepo.findByClienteNombreContainingIgnoreCaseOrderByCreadoEnDesc(q.trim());
        } else if (estado != null && fecha != null) {
            lista = cuentaRepo.findByEstadoAndFechaOrderByCreadoEnDesc(estado, fecha);
        } else if (estado != null) {
            lista = cuentaRepo.findByEstadoOrderByCreadoEnDesc(estado);
        } else if (fecha != null) {
            lista = cuentaRepo.findByFechaOrderByCreadoEnDesc(fecha);
        } else {
            lista = cuentaRepo.findAllByOrderByCreadoEnDesc();
        }
        if (lista.isEmpty()) return List.of();
        List<Long> ids = lista.stream().map(CuentaCobrar::getId).toList();
        Map<Long, List<AbonoCuentaCobrar>> abonosPor = abonoRepo.findByCuentaIdIn(ids)
                .stream().collect(Collectors.groupingBy(a -> a.getCuenta().getId()));
        return lista.stream()
                .map(c -> enrich(c, abonosPor.getOrDefault(c.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaCobrarDTO obtener(Long id) {
        CuentaCobrar c = findCuenta(id);
        return enrich(c, abonoRepo.findByCuentaIdOrderByCreadoEnDesc(id));
    }

    @Override
    @Transactional
    public CuentaCobrarDTO registrar(CuentaCobrarRequest req) {
        CuentaCobrar cuenta = cuentaRepo.save(CuentaCobrar.builder()
                .clienteNombre(req.getClienteNombre().trim())
                .concepto(req.getConcepto())
                .monto(req.getMonto().setScale(0, RoundingMode.HALF_UP))
                .celular(req.getCelular())
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());

        auditoriaService.registrar("CuentaCobrar", cuenta.getId(), "REGISTRAR",
                "Deuda de " + req.getClienteNombre() + " por $" + req.getMonto());
        log.info("[registrar] cliente='{}' monto={}", req.getClienteNombre(), req.getMonto());
        return enrich(cuenta, List.of());
    }

    @Override
    @Transactional
    public void anular(Long id) {
        CuentaCobrar cuenta = findCuenta(id);
        if (cuenta.getEstado() != EstadoCuentaCobrar.PENDIENTE)
            throw new ValidacionException("Solo se pueden anular cuentas sin abonos (estado PENDIENTE)");
        if (!abonoRepo.findByCuentaIdOrderByCreadoEnDesc(id).isEmpty())
            throw new ValidacionException("No se puede anular una cuenta con abonos registrados");
        auditoriaService.registrar("CuentaCobrar", id, "ANULAR",
                "Cuenta de " + cuenta.getClienteNombre() + " anulada");
        cuentaRepo.delete(cuenta);
    }

    @Override
    @Transactional
    public AbonoCuentaCobrarDTO registrarAbono(Long cuentaId, AbonoCuentaCobrarRequest req) {
        CuentaCobrar cuenta = findCuenta(cuentaId);
        if (cuenta.getEstado() == EstadoCuentaCobrar.CANCELADO)
            throw new ValidacionException("La cuenta ya está cancelada");

        String moneda = req.getMoneda() != null ? req.getMoneda().trim().toUpperCase() : MonedaConstants.COP;
        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoCop = monetaryService.convertirACop(moneda, req.getMonto(), tasaUsd);
        BigDecimal tasaUsada = MonedaConstants.USD.equals(moneda) ? tasaUsd : null;

        BigDecimal totalAbonado = abonoRepo.sumMontoCopByCuentaId(cuentaId);
        BigDecimal saldoPendiente = cuenta.getMonto().subtract(totalAbonado);
        if (montoCop.compareTo(saldoPendiente) > 0)
            throw new ValidacionException(
                    "El cobro ($" + montoCop + ") supera el saldo pendiente ($" + saldoPendiente + ")");

        // Acredita el fondo — el cliente paga y el dinero entra
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.COBRO_CUENTA_COBRAR,
                moneda,
                req.getMonto(),
                montoCop,
                "cuenta_cobrar",
                cuentaId,
                "Cobro cuenta " + cuenta.getClienteNombre(),
                req.getObservacion());

        AbonoCuentaCobrar abono = abonoRepo.save(AbonoCuentaCobrar.builder()
                .cuenta(cuenta)
                .monto(req.getMonto())
                .moneda(moneda)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .fondoAcreditado(moneda)
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());

        BigDecimal nuevoTotal = totalAbonado.add(montoCop);
        cuenta.setEstado(EstadoCuentaCobrar.calcular(cuenta.getMonto(), nuevoTotal));
        cuentaRepo.save(cuenta);

        auditoriaService.registrar("AbonoCuentaCobrar", abono.getId(), "REGISTRAR",
                "Cobro $" + req.getMonto() + " " + moneda + " de " + cuenta.getClienteNombre());
        log.info("[abono] cuentaId={} monto={} moneda={}", cuentaId, req.getMonto(), moneda);
        return mapper.toAbonoDTO(abono);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbonoCuentaCobrarDTO> listarAbonos(Long cuentaId) {
        if (!cuentaRepo.existsById(cuentaId))
            throw new RecursoNoEncontradoException("Cuenta no encontrada: " + cuentaId);
        return abonoRepo.findByCuentaIdOrderByCreadoEnDesc(cuentaId)
                .stream().map(mapper::toAbonoDTO).collect(Collectors.toList());
    }

    // --- helpers ---

    private CuentaCobrar findCuenta(Long id) {
        return cuentaRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta por cobrar no encontrada: " + id));
    }

    private CuentaCobrarDTO enrich(CuentaCobrar c, List<AbonoCuentaCobrar> abonos) {
        CuentaCobrarDTO dto = mapper.toDTO(c);
        BigDecimal totalAbonado = abonos.stream()
                .map(AbonoCuentaCobrar::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setAbonos(abonos.stream().map(mapper::toAbonoDTO).collect(Collectors.toList()));
        dto.setTotalAbonado(totalAbonado);
        dto.setSaldoPendiente(c.getMonto().subtract(totalAbonado));
        return dto;
    }
}
