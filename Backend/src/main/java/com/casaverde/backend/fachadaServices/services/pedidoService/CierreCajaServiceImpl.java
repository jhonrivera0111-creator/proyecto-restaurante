package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CierreCaja;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Usuario;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.dto.BackupDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IBackupService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;
import com.casaverde.backend.fachadaServices.dto.CierreCajaRequest;
import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.CierreCajaMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.CierreCajaRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PedidoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * CIERRE DE CAJA diario: compara lo que el sistema dice que hay en cada fondo
 * ("esperado") contra lo que el dueño cuenta físicamente ("contado") y guarda
 * la diferencia. Reglas: un solo cierre por fecha; se bloquea si quedan pedidos
 * activos ese día; al cerrar crea un backup automático de la base de datos.
 */
@Service
@RequiredArgsConstructor
public class CierreCajaServiceImpl implements ICierreCajaService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final CierreCajaRepository cierreRepo;
    private final PedidoRepository pedidoRepo;
    private final FondoRepository fondoRepo;
    private final UsuarioRepository usuarioRepo;
    private final IResumenService resumenService;
    private final IBackupService backupService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final CierreCajaMapper cierreCajaMapper;

    @Transactional(readOnly = true)
    public List<CierreCajaDTO> listar() {
        return cierreRepo.findAllByOrderByFechaDesc().stream()
                .map(cierreCajaMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CierreCajaDTO obtener(LocalDate fecha) {
        return cierreRepo.findByFecha(fecha)
                .map(cierreCajaMapper::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay cierre de caja para la fecha: " + fecha));
    }

    /**
     * Cierra la caja de una fecha: valida que no esté cerrada ni haya pedidos
     * abiertos, calcula esperado vs contado por fondo (Efectivo, USD, NEQUI),
     * genera backup y guarda el cierre con su diferencia.
     */
    @Transactional
    public CierreCajaDTO cerrar(CierreCajaRequest request) {
        LocalDate fecha = request.getFecha() != null ? request.getFecha() : LocalDate.now(ZONA);
        if (cierreRepo.existsByFecha(fecha)) {
            throw new ValidacionException("La caja ya fue cerrada para la fecha: " + fecha);
        }

        // Una reserva solo bloquea el cierre del día exacto al que corresponde
        // (fechaReserva == fecha). Reservas de otro día (antes o después) no
        // cuentan; pedidos sin reserva cuentan siempre que estén activos.
        long pedidosAbiertos = pedidoRepo.countActivosParaFecha(EstadoPedido.activos(), fecha);
        if (pedidosAbiertos > 0) {
            throw new ValidacionException("No se puede cerrar caja con " + pedidosAbiertos + " pedido(s) pendiente(s)");
        }

        Usuario usuario = null;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepo.findById(request.getUsuarioId())
                    .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado o inactivo"));
        }

        // El arqueo físico (billetes) valida EFECTIVO, no COP — COP ya no se
        // cuenta a mano en el cierre: solo crece por consignaciones desde
        // Efectivo y se confía en el saldo que lleva el sistema.
        BigDecimal esperadoEfectivo = saldoFondo(MonedaConstants.EFECTIVO);
        BigDecimal esperadoUsd = saldoFondo(MonedaConstants.USD);
        BigDecimal esperadoNequi = saldoTotalNequi();

        if (tieneDenominaciones(request)) {
            request.setContadoEfectivo(calcularContadoEfectivoDesdeDenominaciones(request));
        }
        BigDecimal contadoEfectivo = normalizarOCero(MonedaConstants.EFECTIVO, request.getContadoEfectivo(), esperadoEfectivo);
        BigDecimal contadoUsd = normalizarOCero(MonedaConstants.USD, request.getContadoUsd(), esperadoUsd);
        BigDecimal contadoNequi = normalizarOCero(MonedaConstants.NEQUI, request.getContadoNequi(), esperadoNequi);

        ResumenDiaDTO resumen = resumenService.obtener(fecha);
        BackupDTO backup = backupService.crearBackup("cierre-" + fecha);

        CierreCaja cierre = CierreCaja.builder()
                .fecha(fecha)
                .usuarioId(usuario != null ? usuario.getId() : null)
                .usuarioNombre(usuario != null ? usuario.getNombre() : null)
                .esperadoEfectivo(esperadoEfectivo)
                .esperadoUsd(esperadoUsd)
                .esperadoNequi(esperadoNequi)
                .contadoEfectivo(contadoEfectivo)
                .contadoUsd(contadoUsd)
                .contadoNequi(contadoNequi)
                .diferenciaEfectivo(contadoEfectivo.subtract(esperadoEfectivo))
                .diferenciaUsd(contadoUsd.subtract(esperadoUsd))
                .diferenciaNequi(contadoNequi.subtract(esperadoNequi))
                .cobradoTotalCop(resumen.getCobradoTotalCop())
                .gastosTotalCop(resumen.getGastosTotalCop())
                .balanceNeto(resumen.getBalanceNeto())
                .observacion(request.getObservacion())
                .backupPath(backup.getPath())
                .baseEfectivo(request.getBaseEfectivo())
                .billetes100k(request.getBilletes100k())
                .billetes50k(request.getBilletes50k())
                .billetes20k(request.getBilletes20k())
                .billetes10k(request.getBilletes10k())
                .billetes5k(request.getBilletes5k())
                .billetes2k(request.getBilletes2k())
                .monedasEfectivo(request.getMonedasEfectivo())
                .build();

        CierreCaja guardado = cierreRepo.save(cierre);
        auditoriaService.registrar("cierre_caja", guardado.getId(), "CERRAR",
                "Cierre de caja " + fecha + " con backup " + backup.getNombre());
        return cierreCajaMapper.toDTO(guardado);
    }

    private BigDecimal saldoFondo(String moneda) {
        return fondoRepo.findById(moneda)
                .map(Fondo::getSaldo)
                .orElse(BigDecimal.ZERO);
    }

    // Suma COP equivalente de NEQUI principal + todas las cuentas NEQUI adicionales
    private BigDecimal saldoTotalNequi() {
        return fondoRepo.findAll().stream()
                .filter(f -> f.getMoneda().startsWith(MonedaConstants.NEQUI))
                .map(Fondo::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal denom(Integer cantidad, long valorUnitario) {
        if (cantidad == null || cantidad == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(cantidad).multiply(BigDecimal.valueOf(valorUnitario));
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private boolean tieneDenominaciones(CierreCajaRequest req) {
        return req.getBaseEfectivo() != null
            || req.getBilletes100k() != null
            || req.getBilletes50k() != null
            || req.getBilletes20k() != null
            || req.getBilletes10k() != null
            || req.getBilletes5k() != null
            || req.getBilletes2k() != null
            || req.getMonedasEfectivo() != null;
    }

    private BigDecimal calcularContadoEfectivoDesdeDenominaciones(CierreCajaRequest req) {
        return safe(req.getBaseEfectivo())
            .add(denom(req.getBilletes100k(), 100_000L))
            .add(denom(req.getBilletes50k(),   50_000L))
            .add(denom(req.getBilletes20k(),   20_000L))
            .add(denom(req.getBilletes10k(),   10_000L))
            .add(denom(req.getBilletes5k(),     5_000L))
            .add(denom(req.getBilletes2k(),     2_000L))
            .add(safe(req.getMonedasEfectivo()));
    }

    private BigDecimal normalizarOCero(String moneda, BigDecimal valor, BigDecimal fallback) {
        BigDecimal normalizado = monetaryService.normalizarSaldoFondo(moneda, valor != null ? valor : fallback);
        if (normalizado.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El conteo del fondo " + moneda + " no puede ser negativo");
        }
        return normalizado;
    }

}
