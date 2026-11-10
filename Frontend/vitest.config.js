import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { createRequire } from "module";

const pkg = createRequire(import.meta.url)("./package.json");

// Config de pruebas separada de vite.config.js para no cargar el plugin
// de Tailwind (que necesita entorno de build) durante la ejecución de tests.
export default defineConfig({
  plugins: [react()],
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version),
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.js"],
    css: false, // Tailwind no se procesa en tests — las classNames son strings normales
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      exclude: [
        "node_modules/**",
        "electron/**",
        "dist/**",
        "src/test/**",
        "*.config.*",
        "src/main.jsx",
      ],
    },
  },
});
