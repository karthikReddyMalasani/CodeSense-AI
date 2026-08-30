import React, { createContext, useContext, useState } from 'react';
import { DEFAULT_PROJECT } from '../utils/constants';

const ProjectContext = createContext();

const normalizeEntryPath = (entryPath = '') =>
  String(entryPath || '').replace(/\\/g, '/').replace(/^\/+/, '').replace(/\/+$/, '').replace(/\/+/g, '/').trim();

const makeFolderEntry = (folderPath, idPrefix = 'folder') => {
  const normalized = normalizeEntryPath(folderPath);
  const name = normalized.split('/').filter(Boolean).pop() || 'folder';
  return {
    id: `${idPrefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name,
    path: normalized,
    type: 'directory',
    language: 'folder',
    content: ''
  };
};

const ensureFolderEntries = (files, folderPath) => {
  const normalized = normalizeEntryPath(folderPath);
  if (!normalized) return files;

  const nextFiles = [...files];
  const folderParts = normalized.split('/').filter(Boolean);
  let currentPath = '';

  folderParts.forEach((part) => {
    currentPath = currentPath ? `${currentPath}/${part}` : part;
    const exists = nextFiles.some(file => file.type === 'directory' && normalizeEntryPath(file.path) === currentPath);
    if (!exists) {
      nextFiles.push(makeFolderEntry(currentPath, 'folder'));
    }
  });

  return nextFiles;
};

export const ProjectProvider = ({ children }) => {
  const [currentProject, setCurrentProject] = useState(DEFAULT_PROJECT);
  const [openFiles, setOpenFiles] = useState(DEFAULT_PROJECT.files.slice(0, 3));
  const [activeFileId, setActiveFileId] = useState(DEFAULT_PROJECT.files[0]?.id || null);
  const [searchQuery, setSearchQuery] = useState('');
  const [unsavedFiles, setUnsavedFiles] = useState(new Set());

  const activeFile = openFiles.find((f) => f.id === activeFileId) || openFiles[0] || null;

  const selectFile = (file) => {
    if (!file) return;
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

  const createFolder = (folderPath) => {
    const cleanPath = normalizeEntryPath(folderPath);
    if (!cleanPath) return null;

    const folderEntry = makeFolderEntry(cleanPath, 'folder');
    setCurrentProject((prev) => {
      const files = prev.files || [];
      if (files.some(file => file.type === 'directory' && normalizeEntryPath(file.path) === cleanPath)) {
        return prev;
      }

      const nextFiles = ensureFolderEntries(files, cleanPath);
      return {
        ...prev,
        files: [...nextFiles.filter(file => normalizeEntryPath(file.path) !== cleanPath), folderEntry]
      };
    });
    return folderEntry;
  };

  const createNewFile = (fileName, language = 'java', targetPath = '') => {
    const cleanedFileName = String(fileName || '').trim();
    if (!cleanedFileName) return null;

    const normalizedTarget = normalizeEntryPath(targetPath);
    const splitFilePath = normalizeEntryPath(cleanedFileName.replace(/\\/g, '/')).split('/').filter(Boolean);
    const rawFileName = splitFilePath.pop() || cleanedFileName;
    const parentDir = splitFilePath.join('/');
    const desiredFolder = normalizedTarget ? normalizedTarget : parentDir;
    const fullPathParts = [desiredFolder, rawFileName].filter(Boolean);
    const nameWithExt = rawFileName.includes('.') ? rawFileName : `${rawFileName}.${language === 'python' ? 'py' : language === 'javascript' ? 'js' : language === 'typescript' ? 'ts' : language === 'json' ? 'json' : 'java'}`;
    const finalPath = fullPathParts.length ? `${fullPathParts.join('/')}/${nameWithExt}`.replace(/\/+/g, '/').replace(/\/\/+/g, '/') : nameWithExt;

    const newFile = {
      id: 'f-' + Date.now() + '-' + Math.random().toString(16).slice(2),
      name: nameWithExt,
      path: finalPath,
      type: 'file',
      language,
      content: `// New file: ${nameWithExt}\n`
    };

    setCurrentProject((prev) => {
      const files = prev.files || [];
      const withFolders = desiredFolder ? ensureFolderEntries(files, desiredFolder) : files;
      if (withFolders.some(file => normalizeEntryPath(file.path) === normalizeEntryPath(finalPath))) {
        return prev;
      }

      return {
        ...prev,
        files: [...withFolders, newFile]
      };
    });

    selectFile(newFile);
    return newFile;
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
        createFolder,
        unsavedFiles,
        saveFile
      }}
    >
      {children}
    </ProjectContext.Provider>
  );
};

export const useProject = () => useContext(ProjectContext);
