// Custom sign function: no-op para builds de desarrollo sin certificado de firma.
// electron-builder llama esta función en lugar de signtool, evitando la descarga
// de winCodeSign (que falla en Windows sin Developer Mode por sus symlinks de macOS).
exports.default = async function (_configuration) {
  // No firma — salta silenciosamente
};
