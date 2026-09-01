import { useEffect, useRef, useState } from 'react';
import { Code2, Expand, Maximize2, Minus, Plus, RotateCcw } from 'lucide-react';
import mermaid from 'mermaid';
import { createMermaidId, validateMermaidSource } from '../../utils/mermaidUtils';

let mermaidConfigured = false;

function configureMermaid() {
  if (mermaidConfigured) return;
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    theme: 'base',
    themeVariables: {
      primaryColor: '#dbeafe',
      primaryTextColor: '#172033',
      primaryBorderColor: '#2563eb',
      lineColor: '#64748b',
      secondaryColor: '#f1f5f9',
      tertiaryColor: '#ecfeff',
      textColor: '#172033',
      mainBkg: '#ffffff',
      nodeBorder: '#2563eb',
      clusterBkg: '#f8fafc',
      clusterBorder: '#94a3b8',
      edgeLabelBackground: '#ffffff'
    }
  });
  mermaidConfigured = true;
}

export default function DiagramRenderer({ title, source, className = '' }) {
  const diagramRef = useRef(null);
  const shellRef = useRef(null);
  const [scale, setScale] = useState(1);
  const [state, setState] = useState({ status: 'idle', error: '', source: '' });
  const [showSource, setShowSource] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const renderDiagram = async () => {
      if (!diagramRef.current) return;
      diagramRef.current.innerHTML = '';
      setScale(1);
      setState({ status: 'loading', error: '', source: '' });
      try {
        configureMermaid();
        const normalized = validateMermaidSource(source);
        const { svg } = await mermaid.render(createMermaidId(), normalized);
        if (cancelled || !diagramRef.current) return;
        diagramRef.current.innerHTML = svg;
        setState({ status: 'ready', error: '', source: normalized });
      } catch (error) {
        if (cancelled) return;
        setState({
          status: 'error',
          error: error instanceof Error ? error.message : String(error),
          source: typeof source === 'string' ? source : ''
        });
      }
    };
    renderDiagram();
    return () => {
      cancelled = true;
      if (diagramRef.current) diagramRef.current.innerHTML = '';
    };
  }, [source]);

  const changeScale = (amount) => setScale(value => Math.min(2.5, Math.max(0.5, value + amount)));
  const fitDiagram = () => {
    const shell = shellRef.current;
    const svg = diagramRef.current?.querySelector('svg');
    if (!shell || !svg) return;
    const width = svg.viewBox?.baseVal?.width || svg.getBoundingClientRect().width;
    const height = svg.viewBox?.baseVal?.height || svg.getBoundingClientRect().height;
    if (width && height) setScale(Math.min(1, shell.clientWidth / width, shell.clientHeight / height || 1));
  };

  if (!source) return null;

  return (
    <div className={`diagram-renderer ${className}`}>
      <div className="diagram-renderer-toolbar">
        <span>{title}</span>
        <div className="diagram-renderer-actions">
          <button type="button" className="icon-button" onClick={() => setShowSource(value => !value)} title="View Mermaid source"><Code2 size={15} /></button>
          <button type="button" className="icon-button" onClick={() => changeScale(0.1)} title="Zoom in"><Plus size={15} /></button>
          <button type="button" className="icon-button" onClick={() => changeScale(-0.1)} title="Zoom out"><Minus size={15} /></button>
          <button type="button" className="icon-button" onClick={() => setScale(1)} title="Reset zoom"><RotateCcw size={15} /></button>
          <button type="button" className="icon-button" onClick={fitDiagram} title="Fit diagram"><Expand size={15} /></button>
          <button type="button" className="icon-button" onClick={() => shellRef.current?.requestFullscreen?.()} title="Fullscreen"><Maximize2 size={15} /></button>
        </div>
      </div>
      {state.status === 'loading' && <div className="diagram-renderer-status">Rendering diagram...</div>}
      {state.status === 'error' && (
        <div className="diagram-renderer-error">
          <strong>Unable to render diagram</strong>
          <button type="button" className="btn btn-secondary btn-small" onClick={() => setShowSource(true)}>View Mermaid Source</button>
          <details><summary>Rendering error</summary><pre>{state.error}</pre></details>
        </div>
      )}
      <div ref={shellRef} className={`diagram-renderer-shell ${state.status === 'error' ? 'has-error' : ''}`}>
        <div ref={diagramRef} className="diagram-renderer-svg" style={{ transform: `scale(${scale})` }} aria-label={title || 'Mermaid diagram'} />
      </div>
      {showSource && <pre className="diagram-renderer-source">{state.source || source}</pre>}
    </div>
  );
}