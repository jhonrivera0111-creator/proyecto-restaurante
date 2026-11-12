package com.casaverde.backend.fachadaServices.services.inventarioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.TipoMovimientoInventario;
import com.casaverde.backend.fachadaServices.dto.ComparativoMensualDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;
import com.casaverde.backend.capaAccesoADatos.models.entitys.MovimientoInventario;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.InventarioMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.InventarioRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.MovimientoInventarioRepository;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements IInventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioServiceImpl.class);

    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final IAuditoriaService auditoriaService;
    private final InventarioMapper inventarioMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<InventarioDTO> listarStock() {
        return inventarioRepo.findAllByOrderByInsumoAsc().stream()
                .map(inventarioMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventarioDTO obtenerStock(String insumo) {
        return inventarioMapper.toDTO(findInsumoOrThrow(insumo));
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarMovimientos() {
        return movimientoRepo.findAllByOrderByRegistradoEnDesc().stream()
                .map(inventarioMapper::toMovimientoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarMovimientosPorInsumo(String insumo) {
        return movimientoRepo.findByInsumoOrderByRegistradoEnDesc(insumo).stream()
                .map(inventarioMapper::toMovimientoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MovimientoInventarioDTO> listarMovimientosPaginado(String insumo, Pageable pageable) {
        return (insumo != null && !insumo.isBlank()
                ? movimientoRepo.findByInsumoOrderByRegistradoEnDesc(insumo.trim(), pageable)
                : movimientoRepo.findAllByOrderByRegistradoEnDesc(pageable))
                .map(inventarioMapper::toMovimientoDTO);
    }

    @Transactional
    public MovimientoInventarioDTO registrarMovimiento(MovimientoRequest req) {
        validarRequest(req);

        Inventario inv = inventarioRepo.findByInsumo(req.getInsumo())
                .orElseGet(() -> {
                    Inventario nuevo = Inventario.builder()
                            .insumo(req.getInsumo().trim())
                            .stockActual(BigDecimal.ZERO)
                            .unidad("unidad")
                            .build();
                    return inventarioRepo.save(nuevo);
                });

        BigDecimal nuevoStock = calcularNuevoStock(inv.getStockActual(), req);
        if (nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("Stock insuficiente. Stock actual: " + inv.getStockActual()
                    + " — solicitado: " + req.getCantidad());
        }

        inv.setStockActual(nuevoStock);
        inventarioRepo.save(inv);

        MovimientoInventario mov = MovimientoInventario.builder()
                .insumo(req.getInsumo().trim())
                .tipo(req.getTipo())
                .cantidad(req.getCantidad())
                .stockResultante(nuevoStock)
                .referenciaTipo("ajuste_manual")
                .observacion(req.getObservacion())
                .build();

        MovimientoInventario guardado = movimientoRepo.save(mov);
        log.info("[movimiento] insumo='{}' tipo={} cantidad={} stockResultante={}", req.getInsumo(), req.getTipo(), req.getCantidad(), nuevoStock);
        auditoriaService.registrar("inventario", inv.getId(), "MOVIMIENTO_MANUAL",
                req.getTipo() + " " + req.getCantidad() + " " + req.getInsumo());
        return inventarioMapper.toMovimientoDTO(guardado);
    }

    private BigDecimal calcularNuevoStock(BigDecimal actual, MovimientoRequest req) {
        return switch (req.getTipo()) {
            case "entrada" -> actual.add(req.getCantidad());
            case "salida" -> actual.subtract(req.getCantidad());
            case "ajuste" -> req.getCantidad();
            default -> throw new ValidacionException("Tipo de movimiento inválido: " + req.getTipo());
        };
    }

    private void validarRequest(MovimientoRequest req) {
        if (!List.of("entrada", "salida", "ajuste").contains(req.getTipo())) {
            throw new ValidacionException("Tipo inválido. Use: entrada, salida, ajuste");
        }
    }

    private Inventario findInsumoOrThrow(String insumo) {
        return inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + insumo));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    /**
     * Descuenta del stock los insumos que consume un pedido (se llama al
     * finalizar o al fiar). Primero suma lo que necesita cada insumo, luego
     * VALIDA que alcance el stock (si no, lanza error y no se descuenta nada)
     * y por último registra una salida en el historial de movimientos.
     */
    public void descontarPorPedido(Pedido pedido) {
        log.info("[descontar] pedidoId={} detalles={}", pedido.getId(), pedido.getDetalles().size());
        Map<String, BigDecimal> consumos = calcularConsumos(pedido);
        validarStock(consumos);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarSalida(consumo.getKey(), consumo.getValue(), pedido.getId());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    /** Devuelve al stock lo descontado (solo al anular un pedido fiado); deja un movimiento de entrada como rastro. */
    public void revertirPorPedido(Pedido pedido) {
        log.info("[revertir] pedidoId={} detalles={}", pedido.getId(), pedido.getDetalles().size());
        Map<String, BigDecimal> consumos = calcularConsumos(pedido);
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            registrarEntradaReversion(consumo.getKey(), consumo.getValue(), pedido.getId());
        }
    }

    private void registrarEntradaReversion(String insumo, BigDecimal cantidad, Long pedidoId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException(
                        "No existe inventario configurado para el insumo: " + insumo));
        BigDecimal nuevo = inv.getStockActual().add(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo(TipoMovimientoInventario.ENTRADA.valor())
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("pedido_anulado")
                .referenciaId(pedidoId)
                .observacion("Anulación de pedido fiado #" + pedidoId)
                .build());
    }

    @Override
    /**
     * Cuánto de cada insumo consume el pedido. Lee la receta guardada en cada
     * línea (composicionSnapshot, JSON) y multiplica por la cantidad vendida.
     * Ej.: 2 x "1/2 Gallina" (0.5 gallina c/u) => gallina: 1.0.
     * Las líneas sin receta (gaseosa, arroz...) se ignoran: no afectan inventario.
     */
    public Map<String, BigDecimal> calcularConsumos(Pedido pedido) {
        Map<String, BigDecimal> consumos = new LinkedHashMap<>();
        for (PedidoDetalle detalle : pedido.getDetalles()) {
            if (detalle.getComposicionSnapshot() == null || detalle.getComposicionSnapshot().isBlank())
                continue;
            for (Map<String, Object> item : leerSnapshot(detalle)) {
                Object insumoRaw = item.get("insumo");
                Object cantidadRaw = item.get("cantidad");
                if (!(insumoRaw instanceof String insumo) || insumo.isBlank()
                        || !(cantidadRaw instanceof Number)) {
                    throw new ValidacionException(
                            "Composicion de inventario invalida en el item: " + detalle.getNombreSnapshot());
                }
                BigDecimal cantPorUnidad = new BigDecimal(cantidadRaw.toString()).setScale(4, RoundingMode.HALF_UP);
                if (cantPorUnidad.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidacionException("Cantidad de inventario invalida para " + insumo
                            + " en el item: " + detalle.getNombreSnapshot());
                }
                // Misma normalización que ProductoServiceImpl.aplicarComposicion() usa al
                // guardar la composición (trim+lowercase) — así el insumo aquí (leído del
                // snapshot) siempre coincide con las claves de "inventario", sin importar
                // cómo se haya escrito originalmente.
                consumos.merge(insumo.trim().toLowerCase(),
                        cantPorUnidad.multiply(BigDecimal.valueOf(detalle.getCantidad())),
                        BigDecimal::add);
            }
        }
        return consumos;
    }

    private List<Map<String, Object>> leerSnapshot(PedidoDetalle detalle) {
        try {
            return objectMapper.readValue(detalle.getComposicionSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ValidacionException(
                    "No se pudo leer la composicion de inventario del item: " + detalle.getNombreSnapshot());
        }
    }

    private void validarStock(Map<String, BigDecimal> consumos) {
        for (Map.Entry<String, BigDecimal> consumo : consumos.entrySet()) {
            Inventario inv = inventarioRepo.findByInsumo(consumo.getKey())
                    .orElseThrow(() -> new ValidacionException(
                            "No existe inventario configurado para el insumo: " + consumo.getKey()));
            if (inv.getStockActual().compareTo(consumo.getValue()) < 0) {
                log.warn("[validarStock] stock insuficiente insumo='{}' stockActual={} requerido={}", consumo.getKey(), inv.getStockActual(), consumo.getValue());
                throw new ValidacionException("Stock insuficiente de " + consumo.getKey()
                        + ". Disponible: " + inv.getStockActual()
                        + ", requerido: " + consumo.getValue());
            }
        }
    }

    @Transactional
    @Override
    public InventarioDTO actualizarAlerta(String insumo, BigDecimal alertaMinima) {
        if (alertaMinima == null || alertaMinima.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("La alerta mínima debe ser un valor positivo o cero");
        }
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + insumo));
        inv.setAlertaMinima(alertaMinima);
        return inventarioMapper.toDTO(inventarioRepo.save(inv));
    }

    private void registrarSalida(String insumo, BigDecimal cantidad, Long pedidoId) {
        Inventario inv = inventarioRepo.findByInsumo(insumo)
                .orElseThrow(() -> new ValidacionException(
                        "No existe inventario configurado para el insumo: " + insumo));
        if (inv.getStockActual().compareTo(cantidad) < 0) {
            throw new ValidacionException("Stock insuficiente de " + insumo
                    + ". Disponible: " + inv.getStockActual()
                    + ", requerido: " + cantidad);
        }
        BigDecimal nuevo = inv.getStockActual().subtract(cantidad);
        inv.setStockActual(nuevo);
        inventarioRepo.save(inv);
        movimientoRepo.save(MovimientoInventario.builder()
                .insumo(insumo)
                .tipo(TipoMovimientoInventario.SALIDA.valor())
                .cantidad(cantidad)
                .stockResultante(nuevo)
                .referenciaTipo("pedido")
                .referenciaId(pedidoId)
                .observacion("Finalización pedido #" + pedidoId)
                .build());
    }

    @Transactional(readOnly = true)
    @Override
    public ComparativoMensualDTO comparativoAnual(int año) {
        LocalDateTime inicio = LocalDateTime.of(año, 1, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(año + 1, 1, 1, 0, 0);
        List<MovimientoInventario> movimientos = movimientoRepo
                .findByTipoAndReferenciaTipoAndRegistradoEnBetween("salida", "pedido", inicio, fin);

        Map<Integer, Map<String, BigDecimal>> mapa = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            mapa.put(m, new HashMap<>());
        }

        for (MovimientoInventario mov : movimientos) {
            int mes = mov.getRegistradoEn().getMonthValue();
            String insumo = mov.getInsumo().toLowerCase();
            BigDecimal acum = mapa.get(mes).getOrDefault(insumo, BigDecimal.ZERO).add(mov.getCantidad());
            mapa.get(mes).put(insumo, acum);
        }

        List<ComparativoMensualDTO.FilaMes> meses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, BigDecimal> fila = mapa.get(m);
            meses.add(ComparativoMensualDTO.FilaMes.builder()
                .mes(m)
                .gallina(fila.getOrDefault("gallina", BigDecimal.ZERO))
                .cuy(fila.getOrDefault("cuy", BigDecimal.ZERO))
                .build());
        }

        return ComparativoMensualDTO.builder().año(año).meses(meses).build();
    }
}
