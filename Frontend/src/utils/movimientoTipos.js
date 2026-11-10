/**
 * Strings que devuelve FondoMapper.resolveTipo() en el backend.
 * Deben mantenerse sincronizados con el switch en FondoMapper.java.
 */
export const TIPO_MOV = {
  COBRO:   "cobro",
  ENTRADA: "entrada",
  SALIDA:  "salida",
  AJUSTE:  "ajuste",
  REVERSO: "reverso",
};

export const TIPO_MOV_LABEL = {
  [TIPO_MOV.COBRO]:   "Ventas",
  [TIPO_MOV.ENTRADA]: "Entradas",
  [TIPO_MOV.SALIDA]:  "Salidas",
  [TIPO_MOV.AJUSTE]:  "Ajustes",
  [TIPO_MOV.REVERSO]: "Reversos",
};
