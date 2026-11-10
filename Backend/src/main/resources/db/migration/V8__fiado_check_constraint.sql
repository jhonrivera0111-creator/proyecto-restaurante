-- V8: agrega 'FIADO' al CHECK de pedido.estado.
--
-- V1__baseline definio: check (estado in ('PENDIENTE','ABONADO_PARCIAL',
-- 'PAGADO','FINALIZADO','ANULADO')) — sin 'FIADO'. V6__pedido_fiado agrego
-- la columna fiado_en asumiendo (en su comentario) que estado nunca tuvo
-- CHECK, pero si lo tenia. Resultado: cualquier UPDATE que pusiera
-- estado='FIADO' fallaba con SQLITE_CONSTRAINT_CHECK — el boton "Fiar"
-- nunca funciono en ninguna instalacion con esta migracion aplicada.
--
-- SQLite no soporta modificar un CHECK existente con ALTER TABLE: hay que
-- recrear la tabla completa, copiar los datos y renombrar. El FK enforcement
-- de SQLite solo valida DML (INSERT/UPDATE/DELETE) sobre la tabla hija, no
-- DDL sobre la tabla padre — DROP TABLE pedido no rompe pedido_detalle/pago
-- aunque la referencien por FK, siempre que pedido se recree con el mismo
-- nombre antes de la siguiente escritura sobre esas tablas.

CREATE TABLE pedido_new (
    cant_personas integer,
    fecha_reserva date,
    hora_reserva time(6),
    para_llevar boolean,
    actualizado_en timestamp,
    creado_en timestamp not null,
    finalizado_en timestamp,
    id integer,
    salon_id integer,
    version bigint,
    estado varchar(50) not null check (estado in ('PENDIENTE','ABONADO_PARCIAL','PAGADO','FINALIZADO','ANULADO','FIADO')),
    mesa varchar(50),
    nombre varchar(150),
    observacion TEXT,
    telefono varchar(30),
    descuento_cop numeric(19,0) not null default 0 check (descuento_cop >= 0),
    descuento_aplicado_por varchar(150),
    fiado_en timestamp,
    primary key (id),
    foreign key (salon_id) references salon(id)
);

INSERT INTO pedido_new (
    cant_personas, fecha_reserva, hora_reserva, para_llevar, actualizado_en,
    creado_en, finalizado_en, id, salon_id, version, estado, mesa, nombre,
    observacion, telefono, descuento_cop, descuento_aplicado_por, fiado_en
)
SELECT
    cant_personas, fecha_reserva, hora_reserva, para_llevar, actualizado_en,
    creado_en, finalizado_en, id, salon_id, version, estado, mesa, nombre,
    observacion, telefono, descuento_cop, descuento_aplicado_por, fiado_en
FROM pedido;

DROP TABLE pedido;

ALTER TABLE pedido_new RENAME TO pedido;

CREATE INDEX idx_pedido_estado_creado on pedido (estado, creado_en);
CREATE INDEX idx_pedido_creado on pedido (creado_en);
CREATE INDEX idx_pedido_finalizado on pedido (finalizado_en);
CREATE INDEX idx_pedido_salon_id on pedido (salon_id);
