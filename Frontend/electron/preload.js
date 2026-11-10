// preload.js
const { contextBridge, ipcRenderer } = require("electron");

console.log("Preload cargado");

contextBridge.exposeInMainWorld("casaverde", {
  onUpdateStatus: (callback) => {
    const listener = (_event, payload) => callback(payload);
    ipcRenderer.on("update-status", listener);
    return () => ipcRenderer.removeListener("update-status", listener);
  },
});
