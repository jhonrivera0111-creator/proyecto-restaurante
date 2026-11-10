-- V4: agrega telefono opcional y descuento opcional al pedido.
--
-- Columnas nuevas, todas ALTER TABLE ADD COLUMN simples (SQLite permite CHECK en
-- ADD COLUMN cuando la expresion no referencia otras columnas ni tablas —
-- verificado contra SQLite 3.45+/3.47 (version usada por el driver del proyecto),
-- no requiere rebuild de tabla):
--   telefono               varchar(30), nullable  — texto libre, nunca numerico
--   descuento_cop          numeric(19,0) not null default 0, CHECK >= 0 — mismo
--                          scale que COP. El limite contra el subtotal NO se
--                          modela aqui: el subtotal se deriva de pedido_detalle,
--                          no es una columna de pedido, así que esa regla vive
--                          en el backend (PedidoServiceImpl.validarInvarianteDescuento).
--   descuento_aplicado_por varchar(150), nullable  — nombre del operador (solo si hubo descuento)
--
-- default 0 en descuento_cop cubre automaticamente los pedidos existentes
-- (se interpretan con descuento cero, sin backfill necesario).

ALTER TABLE pedido ADD COLUMN telefono varchar(30);
ALTER TABLE pedido ADD COLUMN descuento_cop numeric(19,0) not null default 0 check (descuento_cop >= 0);
ALTER TABLE pedido ADD COLUMN descuento_aplicado_por varchar(150);
