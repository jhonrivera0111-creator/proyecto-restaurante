/**
 * useModalKeyboard — atajo de teclado para modales.
 *
 * - Escape  → llama onEscape  (cerrar modal / cancelar)
 * - Enter   → llama onEnter   (confirmar), SOLO si el foco NO está
 *             en textarea, select, button, a, o dentro de un <form>
 *             (los forms manejan Enter nativamente vía tipo submit).
 *
 * Usa un stack global para que solo el modal más al frente responda.
 * Cuando hay un ConfirmDialog sobre otro modal, ConfirmDialog monta
 * después y queda al tope del stack; al hacer Esc cierra el diálogo,
 * no el modal de fondo.
 */
import { useEffect, useRef } from "react";

// Stack de entradas activas — el último elemento es el modal al frente.
const _stack = [];

export function useModalKeyboard({ onEscape, onEnter } = {}) {
  // Refs para no tener que re-registrar el listener cuando cambian los callbacks.
  const escapeRef = useRef(onEscape);
  const enterRef = useRef(onEnter);
  useEffect(() => {
    escapeRef.current = onEscape;
  }, [onEscape]);
  useEffect(() => {
    enterRef.current = onEnter;
  }, [onEnter]);

  useEffect(() => {
    const id = Symbol();
    const entry = { id, escapeRef, enterRef };
    _stack.push(entry);

    const handler = (e) => {
      // Solo actúa el modal que está al frente
      if (_stack[_stack.length - 1]?.id !== id) return;

      if (e.key === "Escape") {
        if (escapeRef.current) {
          e.preventDefault();
          escapeRef.current();
        }
        return;
      }

      if (e.key === "Enter") {
        const tag = document.activeElement?.tagName ?? "";
        const type = document.activeElement?.type ?? "";
        // Dejar que textarea, select, enlaces y botones manejen Enter solos
        if (["TEXTAREA", "SELECT", "A"].includes(tag)) return;
        if (tag === "BUTTON") return;
        // Si el foco está dentro de un form, el submit nativo lo resuelve
        if (document.activeElement?.form) return;
        // checkbox/radio también los dejamos pasar
        if (type === "checkbox" || type === "radio") return;

        if (enterRef.current) {
          e.preventDefault();
          enterRef.current();
        }
      }
    };

    window.addEventListener("keydown", handler);
    return () => {
      window.removeEventListener("keydown", handler);
      const idx = _stack.findIndex((e) => e.id === id);
      if (idx !== -1) _stack.splice(idx, 1);
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
