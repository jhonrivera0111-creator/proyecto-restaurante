package com.casaverde.backend.fachadaServices.services.empleadoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Adelanto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Empleado;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PagoEmpleado;
import com.casaverde.backend.capaAccesoADatos.models.entitys.TrabajoDia;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Vacacion;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoAdelanto;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoEmpleado;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoPagoEmpleado;
import com.casaverde.backend.capaAccesoADatos.repositories.AdelantoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.EmpleadoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.PagoEmpleadoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.TrabajoDiaRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.VacacionRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AdelantoDTO;
import com.casaverde.backend.fachadaServices.dto.AdelantoRequest;
import com.casaverde.backend.fachadaServices.dto.EmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.EmpleadoRequest;
import com.casaverde.backend.fachadaServices.dto.EmpleadoResumenDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoRequest;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaDTO;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaRequest;
import com.casaverde.backend.fachadaServices.dto.VacacionDTO;
import com.casaverde.backend.fachadaServices.dto.VacacionRequest;
import com.casaverde.backend.fachadaServices.mapper.EmpleadoMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpleadoServiceImpl implements IEmpleadoService {

    private static final Logger log = LoggerFactory.getLogger(EmpleadoServiceImpl.class);

    private final EmpleadoRepository empleadoRepo;
    private final AdelantoRepository adelantoRepo;
    private final TrabajoDiaRepository trabajoDiaRepo;
    private final PagoEmpleadoRepository pagoRepo;
    private final VacacionRepository vacacionRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final EmpleadoMapper empleadoMapper;

    // ─── Empleados ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoDTO> listar(boolean soloActivos) {
        List<Empleado> lista = soloActivos
                ? empleadoRepo.findByActivoTrueOrderByNombreAsc()
                : empleadoRepo.findAllByOrderByNombreAsc();
        return lista.stream().map(empleadoMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EmpleadoDTO crear(EmpleadoRequest req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            throw new ValidacionException("El nombre es obligatorio");
        if (req.getSalario() == null || req.getSalario().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El salario debe ser mayor a 0");
        Empleado emp = empleadoRepo.save(Empleado.builder()
                .nombre(req.getNombre().trim())
                .tipo(req.getTipo())
                .salario(req.getSalario().setScale(0, RoundingMode.HALF_UP))
                .build());
        auditoriaService.registrar("Empleado", emp.getId(), "CREAR",
                "Empleado " + emp.getNombre() + " tipo " + emp.getTipo() + " creado");
        return empleadoMapper.toDTO(emp);
    }

    @Override
    @Transactional
    public EmpleadoDTO actualizar(Long id, EmpleadoRequest req) {
        Empleado emp = findEmpleado(id);
        if (req.getNombre() != null && !req.getNombre().isBlank())
            emp.setNombre(req.getNombre().trim());
        if (req.getSalario() != null) {
            if (req.getSalario().compareTo(BigDecimal.ZERO) <= 0)
                throw new ValidacionException("El salario debe ser mayor a 0");
            emp.setSalario(req.getSalario().setScale(0, RoundingMode.HALF_UP));
        }
        auditoriaService.registrar("Empleado", id, "ACTUALIZAR", "Empleado #" + id + " actualizado");
        return empleadoMapper.toDTO(empleadoRepo.save(emp));
    }

    @Override
    @Transactional
    public EmpleadoDTO toggle(Long id) {
        Empleado emp = findEmpleado(id);
        emp.setActivo(!emp.getActivo());
        auditoriaService.registrar("Empleado", id,
                emp.getActivo() ? "ACTIVAR" : "DESACTIVAR",
                "Empleado #" + id + " " + (emp.getActivo() ? "activado" : "desactivado"));
        return empleadoMapper.toDTO(empleadoRepo.save(emp));
    }

    // ─── Adelantos ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdelantoDTO> listarAdelantos(Long empleadoId) {
        findEmpleado(empleadoId);
        return adelantoRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId)
                .stream().map(empleadoMapper::toAdelantoDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdelantoDTO registrarAdelanto(Long empleadoId, AdelantoRequest req) {
        Empleado emp = findEmpleado(empleadoId);
        TipoAdelanto tipo = req.getTipo() != null ? req.getTipo() : TipoAdelanto.EFECTIVO;

        if (tipo == TipoAdelanto.ESPECIE && (req.getObservacion() == null || req.getObservacion().isBlank()))
            throw new ValidacionException("La observación es obligatoria para descuentos en especie");

        String moneda = req.getMoneda() != null ? req.getMoneda().trim().toUpperCase() : MonedaConstants.COP;
        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoCop = monetaryService.convertirACop(moneda, req.getMonto(), tasaUsd);
        BigDecimal tasaUsada = MonedaConstants.USD.equals(moneda) ? tasaUsd : null;

        if (tipo == TipoAdelanto.EFECTIVO) {
            cajaMovimientoService.registrar(
                    CajaMovimientoTipo.ADELANTO_EMPLEADO,
                    moneda,
                    req.getMonto().negate(),
                    montoCop.negate(),
                    "empleado",
                    empleadoId,
                    "Adelanto a " + emp.getNombre(),
                    req.getObservacion());
        }

        Adelanto adelanto = adelantoRepo.save(Adelanto.builder()
                .empleado(emp)
                .tipo(tipo)
                .monto(req.getMonto())
                .moneda(moneda)
                .montoCop(montoCop)
                .tasaUsada(tasaUsada)
                .fondoDebitado(tipo == TipoAdelanto.EFECTIVO ? moneda : "ESPECIE")
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());

        auditoriaService.registrar("Adelanto", adelanto.getId(), "REGISTRAR",
                "Adelanto " + tipo.name() + " $" + req.getMonto() + " " + moneda + " a " + emp.getNombre());
        log.info("[adelanto] empleadoId={} tipo={} monto={} moneda={}", empleadoId, tipo, req.getMonto(), moneda);
        return empleadoMapper.toAdelantoDTO(adelanto);
    }

    // ─── Días trabajados ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TrabajoDiaDTO> listarDias(Long empleadoId, boolean soloNoPagados) {
        findEmpleado(empleadoId);
        List<TrabajoDia> dias = soloNoPagados
                ? trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId)
                : trabajoDiaRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId);
        return dias.stream().map(empleadoMapper::toTrabajoDiaDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TrabajoDiaDTO registrarDia(Long empleadoId, TrabajoDiaRequest req) {
        Empleado emp = findEmpleado(empleadoId);
        if (emp.getTipo() != TipoEmpleado.POR_DIA)
            throw new ValidacionException("Solo se registran días para empleados de tipo POR_DIA");
        TrabajoDia dia = trabajoDiaRepo.save(TrabajoDia.builder()
                .empleado(emp)
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());
        auditoriaService.registrar("TrabajoDia", dia.getId(), "REGISTRAR",
                "Día " + dia.getFecha() + " para " + emp.getNombre());
        return empleadoMapper.toTrabajoDiaDTO(dia);
    }

    @Override
    @Transactional
    public void eliminarDia(Long empleadoId, Long diaId) {
        TrabajoDia dia = trabajoDiaRepo.findById(diaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Día no encontrado: " + diaId));
        if (!dia.getEmpleado().getId().equals(empleadoId))
            throw new ValidacionException("El día no pertenece al empleado indicado");
        if (dia.getPagado())
            throw new ValidacionException("No se puede eliminar un día ya pagado");
        trabajoDiaRepo.delete(dia);
        auditoriaService.registrar("TrabajoDia", diaId, "ELIMINAR",
                "Día " + dia.getFecha() + " de empleado #" + empleadoId + " eliminado");
    }

    // ─── Pagos / Liquidación ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PagoEmpleadoDTO> listarPagos(Long empleadoId) {
        findEmpleado(empleadoId);
        return pagoRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId)
                .stream().map(empleadoMapper::toPagoDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmpleadoResumenDTO previewPago(Long empleadoId) {
        Empleado emp = findEmpleado(empleadoId);
        List<Adelanto> adelantosPend = adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId);
        List<TrabajoDia> diasPend = emp.getTipo() == TipoEmpleado.POR_DIA
                ? trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId)
                : List.of();

        BigDecimal totalAdelantos = adelantosPend.stream()
                .map(Adelanto::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        int diasCount = diasPend.size();
        BigDecimal bruto = emp.getTipo() == TipoEmpleado.QUINCENAL
                ? emp.getSalario()
                : emp.getSalario().multiply(BigDecimal.valueOf(diasCount));
        BigDecimal neto = bruto.subtract(totalAdelantos);

        return EmpleadoResumenDTO.builder()
                .empleado(empleadoMapper.toDTO(emp))
                .adelantosPendientes(adelantosPend.stream()
                        .map(empleadoMapper::toAdelantoDTO).collect(Collectors.toList()))
                .diasPendientes(diasPend.stream()
                        .map(empleadoMapper::toTrabajoDiaDTO).collect(Collectors.toList()))
                .totalAdelantosCop(totalAdelantos)
                .totalDiasPendientes(diasCount)
                .salarioBrutoEstimado(bruto)
                .netoEstimado(neto)
                .build();
    }

    @Override
    @Transactional
    public PagoEmpleadoDTO liquidar(Long empleadoId, PagoEmpleadoRequest req) {
        Empleado emp = findEmpleado(empleadoId);

        List<Adelanto> adelantosPend = adelantoRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId);
        BigDecimal totalAdelantos = adelantosPend.stream()
                .map(Adelanto::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal bruto;
        Integer diasCount = null;
        TipoPagoEmpleado tipoPago;
        List<TrabajoDia> diasPend = null;

        if (emp.getTipo() == TipoEmpleado.QUINCENAL) {
            bruto = emp.getSalario();
            tipoPago = TipoPagoEmpleado.QUINCENA;
        } else {
            diasPend = trabajoDiaRepo.findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(empleadoId);
            if (diasPend.isEmpty())
                throw new ValidacionException("El empleado no tiene días pendientes de cobro");
            diasCount = diasPend.size();
            bruto = emp.getSalario().multiply(BigDecimal.valueOf(diasCount));
            tipoPago = TipoPagoEmpleado.COBRO_DIAS;
        }

        BigDecimal neto = bruto.subtract(totalAdelantos);
        if (neto.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidacionException(
                    "Los adelantos ($" + totalAdelantos + ") superan el salario bruto ($" + bruto + ")");

        String moneda = req.getMoneda().trim().toUpperCase();
        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoPagado;
        BigDecimal tasaUsada = null;

        if (MonedaConstants.USD.equals(moneda)) {
            tasaUsada = tasaUsd;
            montoPagado = neto.divide(tasaUsd, 2, RoundingMode.HALF_UP);
        } else {
            montoPagado = neto;
        }

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.PAGO_EMPLEADO,
                moneda,
                montoPagado.negate(),
                neto.negate(),
                "empleado",
                empleadoId,
                "Pago " + tipoPago.name() + " " + emp.getNombre(),
                req.getObservacion());

        adelantosPend.forEach(a -> a.setPagado(true));
        adelantoRepo.saveAll(adelantosPend);

        if (diasPend != null) {
            diasPend.forEach(d -> d.setPagado(true));
            trabajoDiaRepo.saveAll(diasPend);
        }

        log.info("[liquidar] empleadoId={} bruto={} adelantos={} neto={}", empleadoId, bruto, totalAdelantos, neto);
        PagoEmpleado pago = pagoRepo.save(PagoEmpleado.builder()
                .empleado(emp)
                .tipo(tipoPago)
                .periodoInicio(req.getPeriodoInicio())
                .periodoFin(req.getPeriodoFin())
                .diasTrabajados(diasCount)
                .salarioBruto(bruto)
                .totalAdelantos(totalAdelantos)
                .totalNeto(neto)
                .moneda(moneda)
                .montoPagado(montoPagado)
                .montoCop(neto)
                .tasaUsada(tasaUsada)
                .fondoDebitado(moneda)
                .observacion(req.getObservacion())
                .build());

        auditoriaService.registrar("PagoEmpleado", pago.getId(), "LIQUIDAR",
                "Pago " + tipoPago + " a " + emp.getNombre() + " neto: $" + neto);
        return empleadoMapper.toPagoDTO(pago);
    }

    // ─── Vacaciones ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VacacionDTO> listarVacaciones(Long empleadoId) {
        findEmpleado(empleadoId);
        return vacacionRepo.findByEmpleadoIdOrderByCreadoEnDesc(empleadoId)
                .stream().map(empleadoMapper::toVacacionDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VacacionDTO registrarVacacion(Long empleadoId, VacacionRequest req) {
        Empleado emp = findEmpleado(empleadoId);
        if (req.getFechaFin() != null && req.getFechaInicio() != null
                && req.getFechaFin().isBefore(req.getFechaInicio())) {
            throw new ValidacionException(
                    "La fecha de fin de vacación no puede ser anterior a la fecha de inicio");
        }
        Vacacion vac = vacacionRepo.save(Vacacion.builder()
                .empleado(emp)
                .fechaInicio(req.getFechaInicio())
                .fechaFin(req.getFechaFin())
                .observacion(req.getObservacion())
                .build());
        auditoriaService.registrar("Vacacion", vac.getId(), "REGISTRAR",
                "Vacación " + req.getFechaInicio() + " - " + req.getFechaFin() + " para " + emp.getNombre());
        return empleadoMapper.toVacacionDTO(vac);
    }

    @Override
    @Transactional
    public void eliminarVacacion(Long empleadoId, Long vacId) {
        Vacacion vac = vacacionRepo.findById(vacId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vacación no encontrada: " + vacId));
        if (!vac.getEmpleado().getId().equals(empleadoId))
            throw new ValidacionException("La vacación no pertenece al empleado indicado");
        vacacionRepo.delete(vac);
        auditoriaService.registrar("Vacacion", vacId, "ELIMINAR",
                "Vacación de empleado #" + empleadoId + " eliminada");
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private Empleado findEmpleado(Long id) {
        return empleadoRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado no encontrado: " + id));
    }
}
