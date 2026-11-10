export default function Placeholder({ titulo }) {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">{titulo}</h1>
      </div>
      <div className="card">
        <div className="card-body empty-state">
          <p>Módulo en construcción — Sprint 2</p>
        </div>
      </div>
    </div>
  );
}
