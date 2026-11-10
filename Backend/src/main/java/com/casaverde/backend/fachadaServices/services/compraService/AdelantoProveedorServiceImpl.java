package com.casaverde.backend.fachadaServices.services.compraService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AdelantoProveedor;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.repositories.AdelantoProveedorRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorRequest;
import com.casaverde.backend.fachadaServices.mapper.AdelantoProveedorMapper;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdelantoProveedorServiceImpl implements IAdelantoProveedorService {

    private final AdelantoProveedorRepository adelantoRepo;
    private final ProveedorRepository proveedorRepo;
    private final InventarioRepository inventarioRepo;
    private final ICajaMovimientoService cajaMovimientoService;
    private final ITasaCambioService tasaCambioService;
    private final IAuditoriaService auditoriaService;
    private final AdelantoProveedorMapper mapper;

    @Override
    @Transactional
    public AdelantoProveedorDTO registrar(AdelantoProveedorRequest req) {
        var proveedor = proveedorRepo.findById(req.getProveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + req.getProveedorId()));
        if (!proveedor.getActivo())
            throw new ValidacionException("El proveedor está inactivo");
        if (req.getMonto() == null || req.getMonto().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El monto del adelanto debe ser mayor a 0");

        if (req.getInsumo() == null || req.getInsumo().isBlank())
            throw new ValidacionException("El insumo es obligatorio");
        String insumoNorm = req.getInsumo().trim().toLowerCase();
        if (inventarioRepo.findByInsumo(insumoNorm).isEmpty())
            throw new ValidacionException("El insumo '" + insumoNorm + "' no existe en el inventario");

        if (req.getPrecioUnitarioAcordado() == null || req.getPrecioUnitarioAcordado().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidacionException("El precio unitario acordado debe ser mayor a 0");

        String moneda = req.getMoneda() != null ? req.getMoneda().trim().toUpperCase() : MonedaConstants.COP;
        BigDecimal tasaUsd = tasaCambioService.getValorVigente();
        BigDecimal montoCop = MonedaConstants.USD.equals(moneda)
                ? req.getMonto().multiply(tasaUsd).setScale(0, RoundingMode.HALF_UP)
                : req.getMonto().setScale(0, RoundingMode.HALF_UP);

        // Guardar primero para tener el ID disponible en el movimiento de caja
        AdelantoProveedor adelanto = adelantoRepo.save(AdelantoProveedor.builder()
                .proveedor(proveedor)
                .proveedorNombreSnapshot(proveedor.getNombre())
                .monto(req.getMonto().setScale(0, RoundingMode.HALF_UP))
                .saldoRestante(montoCop)
                .moneda(moneda)
                .montoCopInicial(montoCop)
                .fondoDebitado(moneda)
                .insumo(insumoNorm)
                .precioUnitarioAcordado(req.getPrecioUnitarioAcordado().setScale(0, RoundingMode.HALF_UP))
                .fecha(req.getFecha())
                .observacion(req.getObservacion())
                .build());

        cajaMovimientoService.registrar(
                CajaMovimientoTipo.ADELANTO_PROVEEDOR,
                moneda,
                req.getMonto().negate(),
                montoCop.negate(),
                "adelanto_proveedor",
                adelanto.getId(),
                "Adelanto a proveedor " + proveedor.getNombre(),
                req.getObservacion());

        auditoriaService.registrar("AdelantoProveedor", adelanto.getId(), "REGISTRAR",
                "Adelanto $" + req.getMonto() + " " + moneda + " a " + proveedor.getNombre());

        AdelantoProveedorDTO dto = mapper.toDTO(adelanto);
        dto.setSaldoUsado(BigDecimal.ZERO);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdelantoProveedorDTO> listarPorProveedor(Long proveedorId) {
        if (!proveedorRepo.existsById(proveedorId))
            throw new RecursoNoEncontradoException("Proveedor no encontrado: " + proveedorId);
        return adelantoRepo.findByProveedorIdOrderByFechaDesc(proveedorId)
                .stream().map(a -> {
                    AdelantoProveedorDTO dto = mapper.toDTO(a);
                    dto.setSaldoUsado(a.getMontoCopInicial().subtract(a.getSaldoRestante()));
                    return dto;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdelantoProveedorDTO> listarActivosPorProveedor(Long proveedorId) {
        return adelantoRepo.findByProveedorIdAndActivoTrueOrderByFechaAsc(proveedorId)
                .stream().map(a -> {
                    AdelantoProveedorDTO dto = mapper.toDTO(a);
                    dto.setSaldoUsado(a.getMontoCopInicial().subtract(a.getSaldoRestante()));
                    return dto;
                }).collect(Collectors.toList());
    }
}
