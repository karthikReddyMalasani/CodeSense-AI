/**
 * SimpleMarkdown - lightweight markdown renderer with no external dependencies.
 * Supports: headings, bold, italic, inline code, code blocks, lists, blockquotes, horizontal rules.
 */
export default function SimpleMarkdown({ children }) {
  if (!children) return null;
  const text = String(children);

  const lines = text.split('\n');
  const elements = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Fenced code block
    if (line.startsWith('```')) {
      const lang = line.slice(3).trim();
      const codeLines = [];
      i++;
      while (i < lines.length && !lines[i].startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      elements.push(
        <pre key={i} style={{ background: 'var(--surface, #1e1e2e)', border: '1px solid var(--border, #333)', borderRadius: '6px', padding: '12px', overflowX: 'auto', margin: '8px 0' }}>
          <code style={{ fontFamily: 'var(--font-mono, monospace)', fontSize: '12px', color: 'var(--text, #cdd6f4)' }}>
            {codeLines.join('\n')}
          </code>
        </pre>
      );
      i++;
      continue;
    }

    // Headings
    if (line.startsWith('### ')) {
      elements.push(<h3 key={i} style={{ fontSize: '16px', fontWeight: '600', margin: '16px 0 8px' }}>{renderInline(line.slice(4))}</h3>);
      i++; continue;
    }
    if (line.startsWith('## ')) {
      elements.push(<h2 key={i} style={{ fontSize: '18px', fontWeight: '700', margin: '20px 0 10px', borderBottom: '1px solid var(--border, #333)', paddingBottom: '6px' }}>{renderInline(line.slice(3))}</h2>);
      i++; continue;
    }
    if (line.startsWith('# ')) {
      elements.push(<h1 key={i} style={{ fontSize: '22px', fontWeight: '700', margin: '20px 0 12px' }}>{renderInline(line.slice(2))}</h1>);
      i++; continue;
    }

    // Horizontal rule
    if (line.match(/^[-*_]{3,}$/)) {
      elements.push(<hr key={i} style={{ border: 'none', borderTop: '1px solid var(--border, #333)', margin: '16px 0' }} />);
      i++; continue;
    }

    // Blockquote
    if (line.startsWith('> ')) {
      elements.push(
        <blockquote key={i} style={{ borderLeft: '3px solid var(--accent, #3b82d4)', paddingLeft: '12px', margin: '8px 0', color: 'var(--text-muted, #888)', fontStyle: 'italic' }}>
          {renderInline(line.slice(2))}
        </blockquote>
      );
      i++; continue;
    }

    // Unordered list
    if (line.match(/^[*\-+] /)) {
      const items = [];
      while (i < lines.length && lines[i].match(/^[*\-+] /)) {
        items.push(<li key={i} style={{ margin: '3px 0' }}>{renderInline(lines[i].slice(2))}</li>);
        i++;
      }
      elements.push(<ul key={`ul-${i}`} style={{ paddingLeft: '20px', margin: '8px 0' }}>{items}</ul>);
      continue;
    }

    // Ordered list
    if (line.match(/^\d+\. /)) {
      const items = [];
      while (i < lines.length && lines[i].match(/^\d+\. /)) {
        const content = lines[i].replace(/^\d+\. /, '');
        items.push(<li key={i} style={{ margin: '3px 0' }}>{renderInline(content)}</li>);
        i++;
      }
      elements.push(<ol key={`ol-${i}`} style={{ paddingLeft: '20px', margin: '8px 0' }}>{items}</ol>);
      continue;
    }

    // Empty line
    if (line.trim() === '') {
      elements.push(<br key={i} />);
      i++; continue;
    }

    // Paragraph
    elements.push(<p key={i} style={{ margin: '6px 0', lineHeight: '1.65' }}>{renderInline(line)}</p>);
    i++;
  }

  return <div>{elements}</div>;
}

function renderInline(text) {
  // Split on code spans, bold, italic
  const parts = [];
  const pattern = /(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|__[^_]+__|_[^_]+_)/g;
  let last = 0;
  let match;
  let keyIdx = 0;

  while ((match = pattern.exec(text)) !== null) {
    if (match.index > last) parts.push(<span key={keyIdx++}>{text.slice(last, match.index)}</span>);
    const m = match[0];
    if (m.startsWith('`')) {
      parts.push(<code key={keyIdx++} style={{ background: 'var(--surface, #1e1e2e)', padding: '1px 5px', borderRadius: '4px', fontFamily: 'var(--font-mono, monospace)', fontSize: '12px' }}>{m.slice(1, -1)}</code>);
    } else if (m.startsWith('**') || m.startsWith('__')) {
      parts.push(<strong key={keyIdx++}>{m.slice(2, -2)}</strong>);
    } else {
      parts.push(<em key={keyIdx++}>{m.slice(1, -1)}</em>);
    }
    last = match.index + m.length;
  }
  if (last < text.length) parts.push(<span key={keyIdx++}>{text.slice(last)}</span>);
  return parts.length > 0 ? parts : text;
}
