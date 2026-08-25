import React, { createContext, useContext, useState } from 'react';
import { DEFAULT_PROJECT } from '../utils/constants';

const ProjectContext = createContext();

export const ProjectProvider = ({ children }) => {
  const [currentProject, setCurrentProject] = useState(DEFAULT_PROJECT);
  const [openFiles, setOpenFiles] = useState(DEFAULT_PROJECT.files.slice(0, 3));
  const [activeFileId, setActiveFileId] = useState(DEFAULT_PROJECT.files[0]?.id || null);
  const [searchQuery, setSearchQuery] = useState('');
  const [unsavedFiles, setUnsavedFiles] = useState(new Set());

  const activeFile = openFiles.find((f) => f.id === activeFileId) || openFiles[0] || null;

  const selectFile = (file) => {
    if (!openFiles.some((f) => f.id === file.id)) {
      setOpenFiles((prev) => [...prev, file]);
    }
    setActiveFileId(file.id);
  };

  const closeFileTab = (fileId, e) => {
    if (e) e.stopPropagation();
    const updated = openFiles.filter((f) => f.id !== fileId);
    setOpenFiles(updated);
    if (activeFileId === fileId && updated.length > 0) {
      setActiveFileId(updated[updated.length - 1].id);
    }
  };

  const updateFileContent = (fileId, newContent) => {
    setOpenFiles((prev) =>
      prev.map((f) => (f.id === fileId ? { ...f, content: newContent } : f))
    );
    setCurrentProject((prev) => ({
      ...prev,
      files: prev.files.map((f) => (f.id === fileId ? { ...f, content: newContent } : f))
    }));
    setUnsavedFiles((prev) => new Set(prev).add(fileId));
  };

  const saveFile = (fileId) => {
    setUnsavedFiles((prev) => {
      const next = new Set(prev);
      next.delete(fileId);
      return next;
    });
    // In a real app, this would also trigger a PUT request to the backend.
  };

  const createNewFile = (fileName, language = 'java') => {
    const ext = fileName.includes('.') ? '' : language === 'java' ? '.java' : '.py';
    const fullName = fileName + ext;
    const newFile = {
      id: 'f-' + Date.now(),
      name: fullName,
      path: `src/${fullName}`,
      type: 'file',
      language,
      content: `// New file: ${fullName}\n`
    };

    setCurrentProject((prev) => ({
      ...prev,
      files: [...prev.files, newFile]
    }));
    selectFile(newFile);
  };

  const loadProject = (newProject) => {
    if (!newProject) return;
    setCurrentProject(newProject);
    const filesToOpen = (newProject.files || []).filter(f => f.type !== 'directory').slice(0, 3);
    setOpenFiles(filesToOpen);
    setActiveFileId(filesToOpen[0]?.id || (newProject.files && newProject.files[0]?.id) || null);
  };

  return (
    <ProjectContext.Provider
      value={{
        currentProject,
        setCurrentProject,
        loadProject,
        openFiles,
        activeFileId,
        activeFile,
        searchQuery,
        setSearchQuery,
        selectFile,
        closeFileTab,
        updateFileContent,
        createNewFile,
        unsavedFiles,
        saveFile
      }}
    >
      {children}
    </ProjectContext.Provider>
  );
};

export const useProject = () => useContext(ProjectContext);
