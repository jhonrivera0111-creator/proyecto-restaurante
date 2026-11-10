package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;

public interface IPedidoFinalizacionService {

    /**
     * Ejecuta los efectos secundarios de finalizar un pedido:
     * <ol>
     *   <li>Descuenta inventario según los snapshots de composición de cada detalle.</li>
     *   <li>Acredita los fondos por moneda usada con un único movimiento COBRO_PEDIDO por moneda.</li>
     * </ol>
     * <p>
     * <strong>Precondición:</strong> el pedido debe estar en estado FINALIZADO y persisted.
     * Debe llamarse dentro de una transacción activa ({@code Propagation.MANDATORY}).
     * </p>
     *
     * @param pedido   pedido ya guardado con estado FINALIZADO
     * @param totalCop total a acreditar en COP (sin incluir el cambio al cliente)
     */
    void ejecutar(Pedido pedido, BigDecimal totalCop);

    /**
     * Igual que {@link #ejecutar}, pero SIN descontar inventario — para
     * cuando un pedido que estaba en estado FIADO termina de pagarse. El
     * inventario ya se descontó en el momento en que se marcó como fiado
     * (la comida ya se sirvió); acá solo falta acreditar el fondo.
     * <p>
     * <strong>Precondición:</strong> igual que {@link #ejecutar}, requiere
     * transacción activa ({@code Propagation.MANDATORY}).
     * </p>
     */
    void ejecutarSoloCobro(Pedido pedido, BigDecimal totalCop);

    /**
     * Descuenta SOLO inventario, sin acreditar fondo — para cuando un pedido
     * se marca como FIADO: la comida ya se sirvió (el stock debe reflejarlo)
     * pero el dinero todavía no ha entrado.
     * <p>
     * <strong>Precondición:</strong> igual que {@link #ejecutar}, requiere
     * transacción activa ({@code Propagation.MANDATORY}).
     * </p>
     */
    void ejecutarSoloInventario(Pedido pedido);

    /**
     * Revierte (suma de vuelta) el descuento de inventario aplicado al
     * marcar un pedido como FIADO — usado al anular un pedido fiado.
     * <p>
     * <strong>Precondición:</strong> igual que {@link #ejecutar}, requiere
     * transacción activa ({@code Propagation.MANDATORY}).
     * </p>
     */
    void revertirInventario(Pedido pedido);
}
