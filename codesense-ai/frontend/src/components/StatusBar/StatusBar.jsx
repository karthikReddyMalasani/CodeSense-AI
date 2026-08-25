import React from 'react';
import { useEditor } from '../../context/EditorContext';
import { Wifi, Radio, Code2, Globe } from 'lucide-react';

const StatusBar = () => {
  const { activeLanguage, isDemoMode } = useEditor();

  return (
    <footer className="ide-status-bar">
      <div className="status-left">
        <span className="status-item">
          <Code2 className="status-icon" /> {activeLanguage.toUpperCase()}
        </span>
        <span className="status-item">UTF-8</span>
        <span className="status-item">Ln 1, Col 1</span>
        <span className="status-item">Spaces: 4</span>
      </div>

      <div className="status-right">
        <span className={`connection-badge ${isDemoMode ? 'demo' : 'connected'}`}>
          {isDemoMode ? (
            <>
              <Radio className="status-icon demo" /> Demo Mode (Offline Fallback)
            </>
          ) : (
            <>
              <Wifi className="status-icon connected" /> Backend Connected
            </>
          )}
        </span>
        <span className="status-item">
          <Globe className="status-icon" /> CodeAssist AI v1.0.0
        </span>
      </div>
    </footer>
  );
};

export default StatusBar;
