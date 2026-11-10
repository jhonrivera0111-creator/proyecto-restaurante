import { useState } from "react";
import ProductosTab  from "./tabs/ProductosTab";
import SalonesTab    from "./tabs/SalonesTab";
import TasaUsdTab    from "./tabs/TasaUsdTab";
import FondosTab     from "./tabs/FondosTab";
import UsuariosTab   from "./tabs/UsuariosTab";
import InventarioTab from "./tabs/InventarioTab";
import CategoriasTab from "./tabs/CategoriasTab";
import AuditoriaTab  from "./tabs/AuditoriaTab";
import BackupsTab    from "./tabs/BackupsTab";
import ReportesTab   from "./tabs/ReportesTab";

const TABS = [
  { id: "productos",  label: "Productos"  },
  { id: "salones",    label: "Salones"    },
  { id: "tasa",       label: "Tasa USD"   },
  { id: "fondos",     label: "Fondos"     },
  { id: "usuarios",   label: "Usuarios"   },
  { id: "inventario", label: "Inventario" },
  { id: "categorias", label: "Categorías" },
  { id: "auditoria",  label: "Auditoría"  },
  { id: "backups",    label: "Backups"    },
  { id: "reportes",   label: "Reportes"   },
];

export default function ConfiguracionPage() {
  const [tab, setTab] = useState("productos");

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Configuración</h1>
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab${tab === t.id ? " activo" : ""}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "productos"  && <ProductosTab />}
      {tab === "salones"    && <SalonesTab />}
      {tab === "tasa"       && <TasaUsdTab />}
      {tab === "fondos"     && <FondosTab />}
      {tab === "usuarios"   && <UsuariosTab />}
      {tab === "inventario" && <InventarioTab />}
      {tab === "categorias" && <CategoriasTab />}
      {tab === "auditoria"  && <AuditoriaTab  />}
      {tab === "backups"    && <BackupsTab    />}
      {tab === "reportes"   && <ReportesTab   />}
    </div>
  );
}
