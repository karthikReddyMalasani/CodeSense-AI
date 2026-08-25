import React, { useState } from 'react';
import { useTheme } from '../context/ThemeContext';
import { KEYBOARD_SHORTCUTS } from '../utils/constants';
import { Settings as SettingsIcon, Sun, Moon, Server, Keyboard, Sliders } from 'lucide-react';
import { useEditor } from '../context/EditorContext';

const Settings = () => {
  const { theme, setTheme } = useTheme();
  const { editorPreferences, setEditorPreferences } = useEditor();
  const [apiUrl, setApiUrl] = useState(import.meta.env.VITE_API_BASE_URL || 'https://codesense-ai-tuo7.onrender.com');
  const [localPrefs, setLocalPrefs] = useState(editorPreferences);
  const [saved, setSaved] = useState(false);

  const handlePrefChange = (key, value) => {
    setLocalPrefs(prev => ({ ...prev, [key]: value }));
  };

  const handleSaveSettings = (e) => {
    e.preventDefault();
    setEditorPreferences(localPrefs);
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="settings-page-container">
      <div className="settings-header">
        <SettingsIcon className="header-icon glow" />
        <div>
          <h2>Application Settings</h2>
          <p>Configure theme preferences, backend REST endpoints, and IDE shortcuts.</p>
        </div>
      </div>

      <form onSubmit={handleSaveSettings} className="settings-grid">
        {/* Appearance Settings */}
        <div className="settings-card">
          <h3>
            <Sun className="card-subicon" /> Visual Theme
          </h3>
          <div className="form-group">
            <label>Interface Theme:</label>
            <div className="theme-options">
              <button
                type="button"
                className={`theme-card-option ${theme === 'dark' ? 'active' : ''}`}
                onClick={() => setTheme('dark')}
              >
                <Moon className="opt-icon" />
                <span>Dark IDE Theme (Recommended)</span>
              </button>

              <button
                type="button"
                className={`theme-card-option ${theme === 'light' ? 'active' : ''}`}
                onClick={() => setTheme('light')}
              >
                <Sun className="opt-icon" />
                <span>Clean Light Theme</span>
              </button>
            </div>
          </div>
        </div>

        {/* Backend & API Settings */}
        <div className="settings-card">
          <h3>
            <Server className="card-subicon" /> Backend API Server
          </h3>
          <div className="form-group">
            <label>API Base URL (VITE_API_BASE_URL):</label>
            <input
              type="text"
              className="form-input"
              value={apiUrl}
              onChange={(e) => setApiUrl(e.target.value)}
              placeholder="https://codesense-ai-tuo7.onrender.com"
            />
            <span className="form-help">
              If the server is unreachable, CodeAssist AI automatically switches to offline Demo Mode.
            </span>
          </div>
        </div>

        {/* Editor Preferences */}
        <div className="settings-card">
          <h3>
            <Sliders className="card-subicon" /> Monaco Editor Preferences
          </h3>
          <div className="options-grid">
            <div className="form-group">
              <label>Font Size (px):</label>
              <select className="form-select" value={localPrefs.fontSize} onChange={(e) => handlePrefChange('fontSize', e.target.value)}>
                <option value="12">12 px</option>
                <option value="14">14 px</option>
                <option value="16">16 px</option>
                <option value="18">18 px</option>
              </select>
            </div>

            <div className="form-group">
              <label>Tab Indent Size:</label>
              <select className="form-select" value={localPrefs.tabSize} onChange={(e) => handlePrefChange('tabSize', e.target.value)}>
                <option value="2">2 spaces</option>
                <option value="4">4 spaces</option>
              </select>
            </div>
          </div>

          <div className="options-grid mt-3">
            <label className="checkbox-label">
              <input type="checkbox" checked={localPrefs.minimap} onChange={(e) => handlePrefChange('minimap', e.target.checked)} />
              <span>Enable Code Minimap on right side</span>
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={localPrefs.wordWrap} onChange={(e) => handlePrefChange('wordWrap', e.target.checked)} />
              <span>Enable Word Wrap</span>
            </label>
            <label className="checkbox-label">
              <input type="checkbox" checked={localPrefs.autoSave} onChange={(e) => handlePrefChange('autoSave', e.target.checked)} />
              <span>Enable Auto Save</span>
            </label>
          </div>
        </div>

        {/* Keyboard Shortcuts Reference */}
        <div className="settings-card">
          <h3>
            <Keyboard className="card-subicon" /> Keyboard Shortcuts Reference
          </h3>
          <div className="shortcuts-table-wrapper">
            <table className="shortcuts-table">
              <thead>
                <tr>
                  <th>Shortcut Key</th>
                  <th>Action Description</th>
                </tr>
              </thead>
              <tbody>
                {KEYBOARD_SHORTCUTS.map((sc, idx) => (
                  <tr key={idx}>
                    <td>
                      <kbd className="kbd">{sc.key}</kbd>
                    </td>
                    <td>{sc.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="settings-submit-bar">
          <button type="submit" className="btn btn-primary btn-save-settings">
            <span>{saved ? 'Settings Saved!' : 'Save Preferences'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};

export default Settings;
