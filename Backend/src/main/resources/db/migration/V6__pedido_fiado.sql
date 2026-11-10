-- V6: pedidos fiados. Mismo patrón de finalizado_en — marca cuándo el
-- pedido pasó a estado FIADO (comida servida, inventario ya descontado,
-- pendiente de cobro). No requiere CHECK: pedido.estado nunca tuvo uno
-- (confirmado en V2, solo gasto/adelanto/pago_empleado/caja_movimiento/
-- movimiento_inventario/usuario tienen CHECK de enum).

ALTER TABLE pedido ADD COLUMN fiado_en timestamp;
