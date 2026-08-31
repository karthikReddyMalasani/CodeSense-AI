/**
 * SimpleMarkdown - lightweight markdown renderer with no external dependencies.
 * Supports: headings, bold, italic, inline code, code blocks, lists, blockquotes, tables, horizontal rules.
 */
export default function SimpleMarkdown({ children }) {
  if (!children) return null;
  const text = String(children);

  const lines = text.split('\n');
  const elements = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    if (line.startsWith('```')) {
      const codeLines = [];
      i++;
      while (i < lines.length && !lines[i].startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      elements.push(
        <pre key={i} style={{ background: 'var(--bg-soft)', border: '1px solid var(--border)', borderRadius: '6px', padding: '12px', overflowX: 'auto', margin: '8px 0' }}>
          <code style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--text)' }}>
            {codeLines.join('\n')}
          </code>
        </pre>
      );
      i++;
      continue;
    }

    if (line.includes('|') && i + 1 < lines.length && lines[i + 1].includes('|') && lines[i + 1].match(/^\|[\s\-:|]+\|$/)) {
      const headerLine = line.split('|').filter(cell => cell.trim()).map(cell => cell.trim());
      const alignLine = lines[i + 1].split('|').filter(cell => cell.trim());
      const rows = [];

      i += 2;
      while (i < lines.length && lines[i].includes('|')) {
        const cells = lines[i].split('|').filter(cell => cell.trim()).map(cell => cell.trim());
        if (cells.length > 0) rows.push(cells);
        i++;
      }

      const getAlign = (alignStr = '') => {
        if (alignStr.startsWith(':') && alignStr.endsWith(':')) return 'center';
        if (alignStr.endsWith(':')) return 'right';
        if (alignStr.startsWith(':')) return 'left';
        return 'left';
      };

      elements.push(
        <div key={`table-${i}`} style={{ width: '100%', overflowX: 'auto', margin: '12px 0' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid var(--border)', borderRadius: '6px', background: 'var(--bg-card)' }}>
            <thead>
              <tr style={{ background: 'var(--accent)', borderBottom: '2px solid var(--border)' }}>
                {headerLine.map((header, idx) => (
                  <th key={idx} style={{ padding: '10px 12px', textAlign: getAlign(alignLine[idx]), fontWeight: '600', color: 'var(--accent-foreground)', borderRight: idx < headerLine.length - 1 ? '1px solid var(--border)' : 'none' }}>
                    {renderInline(header)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, rowIdx) => (
                <tr key={rowIdx} style={{ borderBottom: '1px solid var(--border)' }}>
                  {row.map((cell, cellIdx) => (
                    <td key={cellIdx} style={{ padding: '10px 12px', textAlign: getAlign(alignLine[cellIdx]), color: 'var(--card-foreground)', borderRight: cellIdx < row.length - 1 ? '1px solid var(--border)' : 'none', verticalAlign: 'top' }}>
                      {renderInline(cell)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
      continue;
    }

    if (line.startsWith('### ')) {
      elements.push(<h3 key={i} style={{ fontSize: '16px', fontWeight: '600', margin: '16px 0 8px', color: 'var(--foreground)' }}>{renderInline(line.slice(4))}</h3>);
      i++; continue;
    }
    if (line.startsWith('## ')) {
      elements.push(<h2 key={i} style={{ fontSize: '18px', fontWeight: '700', margin: '20px 0 10px', color: 'var(--foreground)', borderBottom: '1px solid var(--border)', paddingBottom: '6px' }}>{renderInline(line.slice(3))}</h2>);
      i++; continue;
    }
    if (line.startsWith('# ')) {
      elements.push(<h1 key={i} style={{ fontSize: '22px', fontWeight: '700', margin: '20px 0 12px', color: 'var(--foreground)' }}>{renderInline(line.slice(2))}</h1>);
      i++; continue;
    }

    if (line.match(/^[-*_]{3,}$/)) {
      elements.push(<hr key={i} style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '16px 0' }} />);
      i++; continue;
    }

    if (line.startsWith('> ')) {
      elements.push(
        <blockquote key={i} style={{ borderLeft: '3px solid var(--accent-color)', paddingLeft: '12px', margin: '8px 0', color: 'var(--text-muted)', fontStyle: 'italic' }}>
          {renderInline(line.slice(2))}
        </blockquote>
      );
      i++; continue;
    }

    if (line.match(/^[*\-+] /)) {
      const items = [];
      while (i < lines.length && lines[i].match(/^[*\-+] /)) {
        items.push(<li key={i} style={{ margin: '3px 0', color: 'var(--foreground)' }}>{renderInline(lines[i].slice(2))}</li>);
        i++;
      }
      elements.push(<ul key={`ul-${i}`} style={{ paddingLeft: '20px', margin: '8px 0', color: 'var(--foreground)' }}>{items}</ul>);
      continue;
    }

    if (line.match(/^\d+\. /)) {
      const items = [];
      while (i < lines.length && lines[i].match(/^\d+\. /)) {
        const content = lines[i].replace(/^\d+\. /, '');
        items.push(<li key={i} style={{ margin: '3px 0', color: 'var(--foreground)' }}>{renderInline(content)}</li>);
        i++;
      }
      elements.push(<ol key={`ol-${i}`} style={{ paddingLeft: '20px', margin: '8px 0', color: 'var(--foreground)' }}>{items}</ol>);
      continue;
    }

    if (line.trim() === '') {
      elements.push(<br key={i} />);
      i++; continue;
    }

    elements.push(<p key={i} style={{ margin: '6px 0', lineHeight: '1.65', color: 'var(--foreground)' }}>{renderInline(line)}</p>);
    i++;
  }

  return <div>{elements}</div>;
}

function renderInline(text) {
  const parts = [];
  const pattern = /(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|__[^_]+__|_[^_]+_)/g;
  let last = 0;
  let match;
  let keyIdx = 0;

  while ((match = pattern.exec(text)) !== null) {
    if (match.index > last) parts.push(<span key={keyIdx++}>{text.slice(last, match.index)}</span>);
    const m = match[0];
    if (m.startsWith('`')) {
      parts.push(<code key={keyIdx++} style={{ background: 'var(--secondary)', padding: '1px 5px', borderRadius: '4px', fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--secondary-foreground)' }}>{m.slice(1, -1)}</code>);
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
