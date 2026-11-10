package com.casaverde.backend.fachadaServices.services.cajaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.capaAccesoADatos.repositories.ConfiguracionGeneralRepository;
import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.EntradaFondoRequest;
import com.casaverde.backend.fachadaServices.dto.EstadoNequiMesDTO;
import com.casaverde.backend.fachadaServices.dto.FondoDTO;
import com.casaverde.backend.fachadaServices.dto.GastoRequest;
import com.casaverde.backend.fachadaServices.dto.MovimientoFondoDTO;
import com.casaverde.backend.fachadaServices.dto.TransferenciaFondoRequest;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.FondoMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FondoServiceImpl implements IFondoService {

    private static final Logger log = LoggerFactory.getLogger(FondoServiceImpl.class);

    private final FondoRepository fondoRepo;
    private final CajaMovimientoRepository cajaMovimientoRepo;
    private final ConfiguracionGeneralRepository configRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final IGastoService gastoService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final FondoMapper fondoMapper;

    @Transactional(readOnly = true)
    public List<FondoDTO> listar() {
        return fondoRepo.findAll().stream()
                .map(fondoMapper::toDTO)
                .sorted(Comparator.comparing(FondoDTO::getMoneda))
                .collect(Collectors.toList());
    }

    @Transactional
    public FondoDTO ajustar(String moneda, AjusteCapitalRequest req) {
        String monedaNormalizada = monetaryService.normalizarMoneda(moneda);
        if (req.getNuevoSaldo() == null)
            throw new ValidacionException("El nuevo saldo es obligatorio");
        BigDecimal nuevoSaldo = monetaryService.normalizarSaldoFondo(monedaNormalizada, req.getNuevoSaldo());
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidacionException("El saldo del fondo no puede ser negativo");

        Fondo fondo = fondoRepo.findById(monedaNormalizada)
                .orElseThrow(() -> new RecursoNoEncontradoException("Fondo no encontrado: " + monedaNormalizada));

        BigDecimal saldoAnterior = fondo.getSaldo();
        BigDecimal delta = nuevoSaldo.subtract(saldoAnterior);

        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            log.info("[ajustar] moneda={} nuevoSaldo={}", monedaNormalizada, nuevoSaldo);
            cajaMovimientoService.registrar(
                    CajaMovimientoTipo.AJUSTE_FONDO,
                    monedaNormalizada,
                    delta,
                    null,
                    "fondo",
                    null,
                    "Ajuste manual fondo " + monedaNormalizada,
                    req.getMotivo());
            fondo.setSaldo(nuevoSaldo);
        }

        auditoriaService.registrar("fondo", null, "AJUSTAR",
                "Fondo " + monedaNormalizada + " ajustado de " + saldoAnterior + " a " + nuevoSaldo);
        return fondoMapper.toDTO(fondo);
    }

    @Transactional
    public FondoDTO registrarEntrada(EntradaFondoRequest req) {
        if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto debe ser mayor a cero");
        if (req.getConcepto() == null || req.getConcepto().isBlank())
            throw new ValidacionException("El concepto es obligatorio");

        String monedaNorm = monetaryService.normalizarMoneda(req.getMoneda());

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.ENTRADA_MANUAL,
                monedaNorm,
                req.getMonto(),
                null,
                null,
                null,
                req.getConcepto(),
                req.getObservacion());

        log.info("[entrada] moneda={} monto={} concepto='{}'", monedaNorm, req.getMonto(), req.getConcepto());
        auditoriaService.registrar("fondo", null, "ENTRADA_MANUAL",
                "Entrada manual " + monedaNorm + " " + req.getMonto() + ": " + req.getConcepto());

        return fondoMapper.toDTO(fondoRepo.findById(monedaNorm)
                .orElseThrow(() -> new RecursoNoEncontradoException("Fondo no encontrado: " + monedaNorm)));
    }

    @Transactional(readOnly = true)
    public List<MovimientoFondoDTO> historial() {
        return historial(null, null, null);
    }

    @Transactional(readOnly = true)
    public List<MovimientoFondoDTO> historial(LocalDate fecha, String moneda, String tipo) {
        String monedaNorm = (moneda != null && !moneda.isBlank()) ? monetaryService.normalizarMoneda(moneda) : null;
        CajaMovimientoTipo tipoEnum = null;
        if (tipo != null && !tipo.isBlank()) {
            try {
                tipoEnum = CajaMovimientoTipo.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;
        return cajaMovimientoRepo.findConFiltrosSinPaginar(monedaNorm, tipoEnum, inicio, fin)
                .stream().map(fondoMapper::toMovimientoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MovimientoFondoDTO> historialPaginado(LocalDate fecha, String moneda, String tipo, Pageable pageable) {
        String monedaNorm = (moneda != null && !moneda.isBlank()) ? monetaryService.normalizarMoneda(moneda) : null;

        CajaMovimientoTipo tipoEnum = null;
        if (tipo != null && !tipo.isBlank()) {
            try {
                tipoEnum = CajaMovimientoTipo.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // tipo desconocido — sin filtro de tipo
            }
        }

        LocalDateTime inicio = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime fin = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;

        return cajaMovimientoRepo.findConFiltros(monedaNorm, tipoEnum, inicio, fin, pageable)
                .map(fondoMapper::toMovimientoDTO);
    }

    @Transactional
    public void registrarTransferencia(TransferenciaFondoRequest req) {
        String origen = monetaryService.normalizarMoneda(req.getMonedaOrigen());
        String destino = monetaryService.normalizarMoneda(req.getMonedaDestino());

        if (origen.equals(destino))
            throw new ValidacionException("El fondo de origen y destino deben ser diferentes");
        if (req.getMontoOrigen() == null || req.getMontoOrigen().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto de origen debe ser mayor a cero");
        if (req.getMontoDestino() == null || req.getMontoDestino().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto de destino debe ser mayor a cero");

        String conceptoBase = (req.getConcepto() != null && !req.getConcepto().isBlank())
                ? req.getConcepto()
                : "Transferencia " + origen + " → " + destino;

        // --- GMF 4x1000 — solo sobre la porción que excede el cupo libre mensual ---
        BigDecimal montoOrigenEfectivo = req.getMontoOrigen();
        if (Boolean.TRUE.equals(req.getAplicar4x1000()) && origen.startsWith(MonedaConstants.NEQUI)) {
            // Cupo libre restante en este mes para la cuenta origen
            BigDecimal disponibleLibre = estadoNequiMes(origen).getDisponibleLibre();
            // Si el cupo ya está agotado (libre=0), grava todo; si no, solo el excedente
            BigDecimal montoGravable = req.getMontoOrigen().subtract(disponibleLibre)
                    .max(BigDecimal.ZERO);
            BigDecimal gmf = montoGravable
                    .multiply(new BigDecimal("4"))
                    .divide(new BigDecimal("1000"), 0, RoundingMode.DOWN);
            if (gmf.compareTo(BigDecimal.ZERO) > 0) {
                gastoService.registrar(new GastoRequest(
                        "GMF 4x1000 " + origen,
                        "servicios",
                        origen,
                        gmf,
                        null,
                        null,
                        "4x1000 sobre retiro de " + origen));
                montoOrigenEfectivo = req.getMontoOrigen().subtract(gmf);
            }
        }

        BigDecimal montoCopSalida = calcMontoCop(origen, montoOrigenEfectivo, destino, req.getMontoDestino());
        BigDecimal montoCopEntrada = calcMontoCop(destino, req.getMontoDestino(), origen, montoOrigenEfectivo);

        log.info("[transferencia] origen={} destino={} montoOrigen={}", origen, destino, montoOrigenEfectivo);
        cajaMovimientoService.registrar(
                CajaMovimientoTipo.TRANSFERENCIA_SALIDA,
                origen,
                montoOrigenEfectivo.negate(),
                montoCopSalida.negate(),
                "transferencia", null,
                conceptoBase,
                req.getObservacion());

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.TRANSFERENCIA_ENTRADA,
                destino,
                req.getMontoDestino(),
                montoCopEntrada,
                "transferencia", null,
                conceptoBase,
                req.getObservacion());

        auditoriaService.registrar("fondo", null, "TRANSFERENCIA",
                origen + " " + req.getMontoOrigen() + " → " + destino + " " + req.getMontoDestino()
                        + (Boolean.TRUE.equals(req.getAplicar4x1000()) ? " (con 4x1000)" : ""));
    }

    @Transactional
    public FondoDTO crearCuentaNequi(String alias) {
        if (alias == null || alias.isBlank())
            throw new ValidacionException("El alias de la cuenta Nequi es obligatorio");
        String aliasTrimmed = alias.trim();

        // Verificar que no exista otro fondo NEQUI con el mismo alias
        boolean aliasExiste = fondoRepo.findAll().stream()
                .filter(f -> f.getMoneda().startsWith(MonedaConstants.NEQUI))
                .anyMatch(f -> aliasTrimmed.equalsIgnoreCase(f.getAlias()));
        if (aliasExiste)
            throw new ValidacionException("Ya existe una cuenta Nequi con el alias: " + aliasTrimmed);

        // Generar ID único: NEQUI_2, NEQUI_3, …
        long countNequi = fondoRepo.findAll().stream()
                .filter(f -> f.getMoneda().startsWith(MonedaConstants.NEQUI))
                .count();
        String nuevaMoneda = MonedaConstants.PREFIJO_NEQUI + (countNequi + 1);
        // Asegurar unicidad por si hay brechas por eliminaciones
        while (fondoRepo.findById(nuevaMoneda).isPresent()) {
            countNequi++;
            nuevaMoneda = MonedaConstants.PREFIJO_NEQUI + (countNequi + 1);
        }

        Fondo nuevo = fondoRepo.save(Fondo.builder()
                .moneda(nuevaMoneda)
                .alias(aliasTrimmed)
                .saldo(BigDecimal.ZERO)
                .build());
        auditoriaService.registrar("fondo", null, "CREAR_NEQUI",
                "Nueva cuenta Nequi creada: " + nuevaMoneda + " (" + aliasTrimmed + ")");
        return fondoMapper.toDTO(nuevo);
    }

    @Transactional
    public void eliminarCuentaNequi(String moneda) {
        String m = moneda.toUpperCase();
        if (!m.startsWith(MonedaConstants.NEQUI) || MonedaConstants.NEQUI.equals(m))
            throw new ValidacionException("Solo se pueden eliminar cuentas Nequi adicionales (no la principal)");
        Fondo fondo = fondoRepo.findById(m)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta Nequi no encontrada: " + m));
        if (fondo.getSaldo().compareTo(BigDecimal.ZERO) != 0)
            throw new ValidacionException(
                    "La cuenta debe tener saldo 0 antes de eliminarse. Saldo actual: " + fondo.getSaldo());

        // Si era la cuenta activa, resetear a NEQUI
        String activa = getCuentaNequiActiva();
        if (m.equals(activa)) {
            configRepo.findById("cuenta_nequi_activa").ifPresent(c -> {
                c.setValor(MonedaConstants.NEQUI);
                configRepo.save(c);
            });
        }
        fondoRepo.delete(fondo);
        auditoriaService.registrar("fondo", null, "ELIMINAR_NEQUI", "Cuenta Nequi eliminada: " + m);
    }

    @Transactional(readOnly = true)
    public String getCuentaNequiActiva() {
        return configRepo.findById("cuenta_nequi_activa")
                .map(c -> c.getValor())
                .filter(v -> v != null && !v.isBlank())
                .orElse(MonedaConstants.NEQUI);
    }

    @Transactional(readOnly = true)
    public EstadoNequiMesDTO estadoNequiMes(String fondoMoneda) {
        String cuenta = (fondoMoneda != null && !fondoMoneda.isBlank()) ? fondoMoneda.toUpperCase()
                : getCuentaNequiActiva();
        YearMonth mes = YearMonth.now(ZoneId.of("America/Bogota"));
        BigDecimal acumulado = cajaMovimientoRepo.sumSalidasFondoEnMes(
                cuenta,
                mes.atDay(1).atStartOfDay(),
                mes.plusMonths(1).atDay(1).atStartOfDay());

        BigDecimal limite = configRepo.findById("limite_exento_nequi_mensual")
                .map(c -> {
                    try {
                        return new BigDecimal(c.getValor());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);

        BigDecimal libre = limite.subtract(acumulado).max(BigDecimal.ZERO);
        return new EstadoNequiMesDTO(acumulado, limite, libre, mes.toString());
    }

    /**
     * Calcula el equivalente en COP de un monto en la moneda dada.
     * Para USD, usa el monto explícito del otro lado (tasa personalizada).
     * Para COP y NEQUI, la paridad es 1:1.
     */
    private BigDecimal calcMontoCop(String moneda, BigDecimal monto,
            String monedaOtra, BigDecimal montoOtro) {
        if (MonedaConstants.USD.equals(moneda) && !MonedaConstants.USD.equals(monedaOtra)) {
            return montoOtro.setScale(0, java.math.RoundingMode.HALF_UP);
        }
        return monto.setScale(0, java.math.RoundingMode.HALF_UP);
    }

}
