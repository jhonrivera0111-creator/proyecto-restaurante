import { useMutation, useQueryClient } from "@tanstack/react-query";
import { pedidoService } from "../services/pedidoService";

/**
 * Centraliza todas las mutations de pedidos.
 *
 * Cada mutation invalida la query ["pedidos"] automáticamente en onSuccess.
 * La lógica adicional (cerrar modales, actualizar estado local) se pasa al
 * llamar mutate(payload, { onSuccess: (res) => ... }) desde el sitio de uso.
 * TanStack Query v5 llama AMBOS onSuccess: el del hook y el del mutate().
 */
export function usePedidoMutations() {
  const qc = useQueryClient();
  const invalidar = () => qc.invalidateQueries({ queryKey: ["pedidos"] });

  /**
   * Finalizar pedido descuenta inventario (stock + movimientos) y acredita
   * fondos (backend: PedidoFinalizacionServiceImpl) — a diferencia del resto
   * de mutations, que solo tocan el pedido en sí. Se invalidan todas las
   * queries afectadas para que Inventario, Fondos y Resumen del día se
   * refresquen solos sin depender de su refetchInterval/polling.
   */
  const invalidarFinalizar = () => {
    qc.invalidateQueries({ queryKey: ["pedidos"] });
    qc.invalidateQueries({ queryKey: ["inventario-stock"] });
    qc.invalidateQueries({ queryKey: ["inventario-movimientos"] });
    qc.invalidateQueries({ queryKey: ["comparativo"] });
    qc.invalidateQueries({ queryKey: ["fondos"] });
    qc.invalidateQueries({ queryKey: ["fondos-historial"] });
    qc.invalidateQueries({ queryKey: ["resumen"] });
  };

  /** Crear pedido nuevo */
  const crearMut = useMutation({
    mutationFn: pedidoService.crear,
    onSuccess: invalidar,
  });

  /** Agregar ítem(s) al pedido (bulk diff) */
  const agregarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.agregarDetalle(id, req),
    onSuccess: invalidar,
  });

  /** Actualizar cantidad/precio de un ítem existente */
  const actualizarDetalleMut = useMutation({
    mutationFn: ({ id, detalleId, req }) => pedidoService.actualizarDetalle(id, detalleId, req),
    onSuccess: invalidar,
  });

  /** Quitar un ítem del pedido */
  const quitarDetalleMut = useMutation({
    mutationFn: ({ id, detalleId }) => pedidoService.quitarDetalle(id, detalleId),
    onSuccess: invalidar,
  });

  /** Actualizar campos del pedido (nombre, mesa, salón, observación…) */
  const actualizarMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.actualizar(id, req),
    onSuccess: invalidar,
  });

  /** Registrar un cobro/pago sobre el pedido */
  const pagoMut = useMutation({
    mutationFn: ({ id, req }) => pedidoService.registrarPago(id, req),
    onSuccess: invalidar,
  });

  /** Editar un pago ya registrado */
  const actualizarPagoMut = useMutation({
    mutationFn: ({ id, pagoId, req }) => pedidoService.actualizarPago(id, pagoId, req),
    onSuccess: invalidar,
  });

  /** Eliminar un pago del pedido */
  const quitarPagoMut = useMutation({
    mutationFn: ({ id, pagoId }) => pedidoService.quitarPago(id, pagoId),
    onSuccess: invalidar,
  });

  /** Finalizar pedido (descuenta inventario, cierra operativamente) */
  const finalizarMut = useMutation({
    mutationFn: (id) => pedidoService.finalizar(id),
    onSuccess: invalidarFinalizar,
  });

  /**
   * Carga histórica: crea, cobra y finaliza un pedido retroactivo en una sola
   * llamada — mismos efectos que finalizar (fondos + inventario reales).
   */
  const crearHistoricoMut = useMutation({
    mutationFn: pedidoService.crearHistorico,
    onSuccess: invalidarFinalizar,
  });

  /** Anular pedido */
  const anularMut = useMutation({
    mutationFn: (id) => pedidoService.anular(id),
    onSuccess: invalidar,
  });

  /**
   * Marcar como fiado: descuenta inventario (mismos efectos que finalizar
   * sobre stock/movimientos/resumen) pero NO toca fondos — el pedido pasa a
   * Cuentas por Cobrar hasta saldarse.
   */
  const fiarMut = useMutation({
    mutationFn: (id) => pedidoService.fiar(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["pedidos"] });
      qc.invalidateQueries({ queryKey: ["inventario-stock"] });
      qc.invalidateQueries({ queryKey: ["inventario-movimientos"] });
      qc.invalidateQueries({ queryKey: ["comparativo"] });
      qc.invalidateQueries({ queryKey: ["resumen"] });
    },
  });

  return {
    invalidar,
    crearMut,
    agregarMut,
    actualizarDetalleMut,
    quitarDetalleMut,
    actualizarMut,
    pagoMut,
    actualizarPagoMut,
    quitarPagoMut,
    finalizarMut,
    anularMut,
    fiarMut,
    crearHistoricoMut,
  };
}
