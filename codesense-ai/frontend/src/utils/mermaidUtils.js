const DIAGRAM_HEADERS = new Set(['graph', 'flowchart', 'classDiagram', 'sequenceDiagram', 'erDiagram', 'stateDiagram', 'journey', 'gantt', 'mindmap', 'pie', 'gitGraph']);

export function normalizeMermaidSource(source) {
  if (typeof source !== 'string') return '';
  return source
    .replace(/^\s*```(?:mermaid)?\s*/i, '')
    .replace(/\s*```\s*$/i, '')
    .replace(/\r\n?/g, '\n')
    .trim();
}

export function validateMermaidSource(source) {
  const normalized = normalizeMermaidSource(source);
  if (!normalized) throw new Error('The diagram source is empty.');

  const header = normalized.split('\n', 1)[0].trim().split(/\s+/, 1)[0];
  if (!DIAGRAM_HEADERS.has(header)) {
    throw new Error(`Unsupported Mermaid diagram type: ${header || 'unknown'}`);
  }
  return normalized;
}

export function createMermaidId() {
  return `codesense-diagram-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}