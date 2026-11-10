-- V7: orden manual de productos (drag & drop en Configuración > Productos).
-- Backfill preserva el orden visible actual (alfabético) como punto de
-- partida, para que el catálogo no "salte" al desplegar esta versión.

ALTER TABLE producto ADD COLUMN orden integer not null default 0;

UPDATE producto
SET orden = (
    SELECT COUNT(*)
    FROM producto p2
    WHERE p2.eliminado = 0 AND p2.nombre < producto.nombre
)
WHERE eliminado = 0;
