-- V1: Esquema inicial Casa Verde.
-- A partir de esta version Flyway es la fuente de verdad del esquema.

CREATE TABLE abono_compra (
    fecha date not null,
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    adelanto_proveedor_id integer,
    compra_id integer not null,
    creado_en timestamp not null,
    id integer,
    version bigint,
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    primary key (id),
    foreign key (compra_id) references compra(id),
    foreign key (adelanto_proveedor_id) references adelanto_proveedor(id)
);

CREATE TABLE abono_cuenta_cobrar (
    fecha date not null,
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    creado_en timestamp not null,
    cuenta_cobrar_id integer not null,
    id integer,
    version bigint,
    fondo_acreditado varchar(30) not null,
    moneda varchar(30) not null,
    observacion TEXT,
    primary key (id),
    foreign key (cuenta_cobrar_id) references cuenta_cobrar(id)
);

CREATE TABLE adelanto (
    fecha date not null,
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    pagado boolean not null,
    tasa_usada numeric(19,2),
    creado_en timestamp not null,
    empleado_id integer not null,
    id integer,
    version bigint,
    tipo VARCHAR(20) DEFAULT 'EFECTIVO' not null check (tipo in ('EFECTIVO','ESPECIE')),
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    observacion TEXT,
    primary key (id),
    foreign key (empleado_id) references empleado(id)
);

CREATE TABLE adelanto_proveedor (
    activo boolean not null,
    fecha date not null,
    monto numeric(19,0) not null,
    monto_cop_inicial numeric(19,0) not null,
    precio_unitario_acordado numeric(19,0) not null,
    saldo_restante numeric(19,0) not null,
    creado_en timestamp not null,
    id integer,
    proveedor_id integer not null,
    version bigint,
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    insumo varchar(100) not null,
    proveedor_nombre varchar(150) not null,
    observacion TEXT,
    primary key (id),
    foreign key (proveedor_id) references proveedor(id)
);

CREATE TABLE caja_movimiento (
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    saldo_resultante numeric(19,2) not null,
    id integer,
    referencia_id bigint,
    registrado_en timestamp not null,
    moneda varchar(30) not null,
    referencia_tipo varchar(50),
    tipo varchar(50) not null,
    concepto varchar(300) not null,
    observacion TEXT,
    primary key (id)
);

CREATE TABLE categoria_producto (
    activo boolean not null,
    orden integer not null,
    id integer,
    nombre varchar(255) not null unique,
    primary key (id)
);

CREATE TABLE cierre_caja (
    balance_neto numeric(19,0) not null,
    base_cop numeric(19,0),
    billetes_100k integer,
    billetes_10k integer,
    billetes_20k integer,
    billetes_2k integer,
    billetes_50k integer,
    billetes_5k integer,
    cobrado_total_cop numeric(19,0) not null,
    contadoCop numeric(19,0) not null,
    contadoNequi numeric(19,0) not null,
    contadoUsd numeric(19,2) not null,
    diferenciaCop numeric(19,0) not null,
    diferenciaNequi numeric(19,0) not null,
    diferenciaUsd numeric(19,2) not null,
    esperadoCop numeric(19,0) not null,
    esperadoNequi numeric(19,0) not null,
    esperadoUsd numeric(19,2) not null,
    fecha date not null,
    gastos_total_cop numeric(19,0) not null,
    monedas_cop numeric(19,0),
    cerrado_en timestamp not null,
    id integer,
    usuario_id bigint,
    usuario_nombre varchar(150),
    backup_path varchar(500),
    observacion TEXT,
    primary key (id),
    unique (fecha)
);

CREATE TABLE compra (
    cantidad numeric(19,4) not null,
    fecha date not null,
    precio_unitario numeric(19,0) not null,
    total_cop numeric(19,0) not null,
    creado_en timestamp not null,
    id integer,
    proveedor_id integer not null,
    version bigint,
    estado varchar(20) not null check (estado in ('PENDIENTE','ABONADO_PARCIAL','PAGADO')),
    insumo varchar(100) not null,
    proveedor_nombre varchar(150) not null,
    observacion TEXT,
    primary key (id),
    foreign key (proveedor_id) references proveedor(id)
);

CREATE TABLE configuracion_general (
    actualizado_en timestamp,
    clave varchar(100) not null,
    descripcion varchar(300),
    valor TEXT not null,
    primary key (clave)
);

CREATE TABLE cuenta_cobrar (
    fecha date not null,
    monto numeric(19,0) not null,
    creado_en timestamp not null,
    id integer,
    version bigint,
    celular varchar(20),
    estado varchar(50) not null check (estado in ('PENDIENTE','ABONADO_PARCIAL','CANCELADO')),
    cliente_nombre varchar(150) not null,
    concepto varchar(255),
    observacion varchar(255),
    primary key (id)
);

CREATE TABLE empleado (
    activo boolean not null,
    salario numeric(19,0) not null,
    creado_en timestamp not null,
    id integer,
    version bigint,
    tipo varchar(50) not null check (tipo in ('QUINCENAL','POR_DIA')),
    nombre varchar(255) not null,
    primary key (id)
);

CREATE TABLE fondo (
    saldo numeric(19,2) not null,
    actualizado_en timestamp,
    version bigint,
    moneda varchar(30) not null,
    alias varchar(100),
    primary key (moneda)
);

CREATE TABLE gasto (
    cantidad_inventario numeric(19,4),
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    id integer,
    registrado_en timestamp not null,
    version bigint,
    categoria varchar(30) not null,
    moneda varchar(30) not null,
    insumo_inventario varchar(100),
    concepto varchar(300) not null,
    observacion TEXT,
    primary key (id)
);

CREATE TABLE historial_auditoria (
    entidad_id bigint,
    id integer,
    registrado_en timestamp not null,
    accion varchar(100) not null,
    entidad varchar(100) not null,
    usuario varchar(150) not null,
    detalle TEXT,
    primary key (id)
);

CREATE TABLE inventario (
    alerta_minima numeric(19,4),
    stock_actual numeric(19,4) not null,
    actualizado_en timestamp,
    id integer,
    version bigint,
    unidad varchar(20) not null,
    insumo varchar(100) not null unique,
    primary key (id)
);

CREATE TABLE movimiento_inventario (
    cantidad numeric(19,4) not null,
    stock_resultante numeric(19,4) not null,
    id integer,
    referencia_id bigint,
    registrado_en timestamp not null,
    tipo varchar(20) not null,
    referencia_tipo varchar(30),
    insumo varchar(100) not null,
    observacion TEXT,
    primary key (id)
);

CREATE TABLE pago (
    monto numeric(19,2) not null,
    monto_cop numeric(19,0) not null,
    tasa_usada numeric(19,2),
    id integer,
    pedido_id integer not null,
    registrado_en timestamp not null,
    version bigint,
    moneda varchar(30) not null,
    primary key (id),
    foreign key (pedido_id) references pedido(id) on delete cascade
);

CREATE TABLE pago_empleado (
    dias_trabajados integer,
    monto_cop numeric(19,0) not null,
    monto_pagado numeric(19,2) not null,
    periodo_fin date not null,
    periodo_inicio date not null,
    salario_bruto numeric(19,0) not null,
    tasa_usada numeric(19,2),
    total_adelantos numeric(19,0) not null,
    total_neto numeric(19,0) not null,
    creado_en timestamp not null,
    empleado_id integer not null,
    id integer,
    tipo varchar(20) not null check (tipo in ('QUINCENA','COBRO_DIAS')),
    fondo_debitado varchar(30) not null,
    moneda varchar(30) not null,
    observacion TEXT,
    primary key (id),
    foreign key (empleado_id) references empleado(id)
);

CREATE TABLE pedido (
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
    estado varchar(50) not null check (estado in ('PENDIENTE','ABONADO_PARCIAL','PAGADO','FINALIZADO','ANULADO')),
    mesa varchar(50),
    nombre varchar(150),
    observacion TEXT,
    primary key (id),
    foreign key (salon_id) references salon(id)
);

CREATE TABLE pedido_detalle (
    cantidad integer not null,
    precio_unitario_cop numeric(19,0) not null,
    id integer,
    pedido_id integer not null,
    producto_id integer,
    version bigint,
    composicion_snapshot TEXT,
    nombre_snapshot varchar(255) not null,
    primary key (id),
    foreign key (pedido_id) references pedido(id) on delete cascade,
    foreign key (producto_id) references producto(id)
);

CREATE TABLE producto (
    activo boolean not null,
    afecta_inventario boolean not null,
    eliminado boolean not null,
    precio numeric(19,0) not null,
    actualizado_en timestamp,
    categoria_id integer,
    creado_en timestamp,
    id integer,
    version bigint,
    nombre varchar(255) not null,
    primary key (id),
    foreign key (categoria_id) references categoria_producto(id)
);

CREATE TABLE producto_composicion_inventario (
    cantidad numeric(19,4) not null,
    id integer,
    producto_id integer not null,
    insumo varchar(100) not null,
    primary key (id),
    unique (producto_id, insumo),
    foreign key (producto_id) references producto(id) on delete cascade
);

CREATE TABLE producto_precio_historial (
    precio_anterior numeric(19,0),
    precio_nuevo numeric(19,0) not null,
    id integer,
    producto_id integer not null,
    registrado_en timestamp not null,
    usuario_id bigint,
    producto_nombre varchar(150) not null,
    motivo varchar(200) not null,
    primary key (id),
    foreign key (producto_id) references producto(id)
);

CREATE TABLE proveedor (
    activo boolean not null,
    creado_en timestamp not null,
    id integer,
    tipo varchar(20) not null check (tipo in ('CUY','GALLINA','AMBOS','OTROS')),
    nombre varchar(150) not null unique,
    telefono varchar(255),
    primary key (id)
);

CREATE TABLE salon (
    activo boolean not null,
    aforo integer,
    eliminado boolean not null,
    id integer,
    nombre varchar(100) not null,
    primary key (id)
);

CREATE TABLE tasa_cambio (
    valor_cop numeric(19,2) not null,
    creado_en timestamp,
    id integer,
    vigente_desde timestamp not null,
    primary key (id)
);

CREATE TABLE trabajo_dia (
    fecha date not null,
    pagado boolean not null,
    creado_en timestamp not null,
    empleado_id integer not null,
    id integer,
    observacion varchar(255),
    primary key (id),
    foreign key (empleado_id) references empleado(id)
);

CREATE TABLE usuario (
    activo boolean not null,
    requiere_password boolean not null,
    creado_en timestamp not null,
    id integer,
    nombre varchar(255) not null unique,
    password_hash varchar(255),
    rol varchar(255) not null,
    primary key (id)
);

CREATE TABLE vacacion (
    fecha_fin date not null,
    fecha_inicio date not null,
    creado_en timestamp not null,
    empleado_id integer not null,
    id integer,
    observacion varchar(255),
    primary key (id),
    foreign key (empleado_id) references empleado(id)
);

CREATE INDEX idx_abono_cc_cuenta_creado on abono_cuenta_cobrar (cuenta_cobrar_id, creado_en);
CREATE INDEX idx_abono_compra_compra_creado on abono_compra (compra_id, creado_en);
CREATE INDEX idx_adelanto_empleado_pagado on adelanto (empleado_id, pagado);
CREATE INDEX idx_adelanto_prov_proveedor_activo on adelanto_proveedor (proveedor_id, activo);
CREATE INDEX idx_auditoria_entidad on historial_auditoria (entidad, entidad_id, registrado_en);
CREATE INDEX idx_auditoria_registrado on historial_auditoria (registrado_en);
CREATE INDEX idx_cc_cliente_nombre on cuenta_cobrar (cliente_nombre);
CREATE INDEX idx_cc_estado_creado on cuenta_cobrar (estado, creado_en);
CREATE INDEX idx_cc_fecha_creado on cuenta_cobrar (fecha, creado_en);
CREATE INDEX idx_cm_registrado on caja_movimiento (registrado_en);
CREATE INDEX idx_cm_moneda_registrado on caja_movimiento (moneda, registrado_en);
CREATE INDEX idx_cm_referencia on caja_movimiento (referencia_tipo, referencia_id);
CREATE INDEX idx_compra_estado_fecha on compra (estado, fecha);
CREATE INDEX idx_compra_fecha_creado on compra (fecha, creado_en);
CREATE INDEX idx_compra_proveedor_estado on compra (proveedor_id, estado);
CREATE INDEX idx_gasto_registrado on gasto (registrado_en);
CREATE INDEX idx_mi_insumo_registrado on movimiento_inventario (insumo, registrado_en);
CREATE INDEX idx_mi_tipo_ref_insumo on movimiento_inventario (tipo, referencia_tipo, insumo);
CREATE INDEX idx_mi_registrado on movimiento_inventario (registrado_en);
CREATE INDEX idx_pago_empleado_empleado on pago_empleado (empleado_id);
CREATE INDEX idx_pago_pedido_id on pago (pedido_id);
CREATE INDEX idx_pago_registrado_en on pago (registrado_en);
CREATE INDEX idx_pd_pedido_id on pedido_detalle (pedido_id);
CREATE INDEX idx_pd_producto_id on pedido_detalle (producto_id);
CREATE INDEX idx_pedido_estado_creado on pedido (estado, creado_en);
CREATE INDEX idx_pedido_creado on pedido (creado_en);
CREATE INDEX idx_pedido_finalizado on pedido (finalizado_en);
CREATE INDEX idx_pedido_salon_id on pedido (salon_id);
CREATE INDEX idx_precio_producto on producto_precio_historial (producto_id, registrado_en);
CREATE UNIQUE INDEX uk_producto_nombre_visible on producto (nombre) where eliminado = 0;
CREATE INDEX idx_producto_categoria_id on producto (categoria_id);
CREATE UNIQUE INDEX uk_salon_nombre_visible on salon (nombre) where eliminado = 0;
CREATE INDEX idx_tasa_vigente_desde on tasa_cambio (vigente_desde);
CREATE INDEX idx_trabajo_dia_empleado_pagado on trabajo_dia (empleado_id, pagado);
CREATE INDEX idx_vacacion_empleado on vacacion (empleado_id);
