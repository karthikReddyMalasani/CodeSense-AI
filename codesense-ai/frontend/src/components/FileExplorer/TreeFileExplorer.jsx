import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
    ChevronRight,
    ChevronDown,
    Folder,
    FolderOpen,
    FileCode,
    FileText,
    FileJson,
    FileImage,
    Archive,
    Search,
    X,
    Copy,
    ExternalLink,
    ChevronsDown,
    ChevronsUp
} from 'lucide-react';
import { buildFileTree, filterTree } from '../../utils/treeParserUtils';
import './TreeFileExplorer.css';

/**
 * Custom File Type Icon Mapping
 */
export const getFileIcon = (fileName) => {
    if (!fileName) return <FileText className="vscode-node-icon icon-doc" size={16} />;
    const lower = fileName.toLowerCase();

    if (lower.endsWith('.jsx') || lower.endsWith('.tsx')) {
        return <FileCode className="vscode-node-icon icon-code-react" size={16} />;
    }
    if (lower.endsWith('.ts')) {
        return <FileCode className="vscode-node-icon icon-code-ts" size={16} />;
    }
    if (lower.endsWith('.js') || lower.endsWith('.mjs') || lower.endsWith('.cjs')) {
        return <FileCode className="vscode-node-icon icon-code-js" size={16} />;
    }
    if (lower.endsWith('.java') || lower.endsWith('.class') || lower.endsWith('.jar')) {
        return <FileCode className="vscode-node-icon icon-code-java" size={16} />;
    }
    if (lower.endsWith('.py') || lower.endsWith('.pyw')) {
        return <FileCode className="vscode-node-icon icon-code-py" size={16} />;
    }
    if (lower.endsWith('.html') || lower.endsWith('.htm')) {
        return <FileCode className="vscode-node-icon icon-code-html" size={16} />;
    }
    if (lower.endsWith('.css') || lower.endsWith('.scss') || lower.endsWith('.less')) {
        return <FileCode className="vscode-node-icon icon-code-css" size={16} />;
    }
    if (lower.endsWith('.json') || lower.endsWith('.xml') || lower.endsWith('.yaml') || lower.endsWith('.yml')) {
        return <FileJson className="vscode-node-icon icon-json" size={16} />;
    }
    if (lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.svg') || lower.endsWith('.gif') || lower.endsWith('.ico')) {
        return <FileImage className="vscode-node-icon icon-image" size={16} />;
    }
    if (lower.endsWith('.zip') || lower.endsWith('.tar') || lower.endsWith('.gz') || lower.endsWith('.7z')) {
        return <Archive className="vscode-node-icon icon-archive" size={16} />;
    }
    return <FileText className="vscode-node-icon icon-doc" size={16} />;
};

/**
 * Recursive File/Folder Tree Node Component
 */
const TreeNode = ({
    node,
    depth = 0,
    selectedFilePath,
    expandedFolders,
    toggleFolder,
    onFileSelect,
    onContextMenu
}) => {
    const isFolder = node.type === 'folder';
    const isExpanded = isFolder && (expandedFolders[node.id] !== false);
    const isSelected = !isFolder && selectedFilePath === node.path;

    const handleRowClick = (e) => {
        e.stopPropagation();
        if (isFolder) {
            toggleFolder(node.id);
        } else {
            onFileSelect(node.fileData || node);
        }
    };

    const handleContextMenuClick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        onContextMenu(e, node);
    };

    return (
        <div>
            <div
                className={`vscode-tree-row ${isSelected ? 'active' : ''}`}
                style={{ paddingLeft: `${depth * 14 + 10}px` }}
                onClick={handleRowClick}
                onContextMenu={handleContextMenuClick}
                title={node.path || node.name}
            >
                {isFolder ? (
                    <>
                        <span className="vscode-tree-chevron">
                            {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                        </span>
                        {isExpanded ? (
                            <FolderOpen size={16} className="vscode-node-icon icon-folder-open" />
                        ) : (
                            <Folder size={16} className="vscode-node-icon icon-folder" />
                        )}
                    </>
                ) : (
                    <>
                        <span className="vscode-tree-chevron" />
                        {getFileIcon(node.name)}
                    </>
                )}

                <span className="vscode-node-name">{node.name}</span>

                {!isFolder && node.fileData?.binary && (
                    <span className="vscode-file-badge">bin</span>
                )}
            </div>

            {isFolder && isExpanded && Array.isArray(node.children) && (
                <div>
                    {node.children.map(child => (
                        <TreeNode
                            key={child.id || child.path}
                            node={child}
                            depth={depth + 1}
                            selectedFilePath={selectedFilePath}
                            expandedFolders={expandedFolders}
                            toggleFolder={toggleFolder}
                            onFileSelect={onFileSelect}
                            onContextMenu={onContextMenu}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

/**
 * Main VS Code Style Tree File Explorer Component
 */
export default function TreeFileExplorer({
    files = [],
    rootName = 'project',
    selectedFile = null,
    onFileSelect = () => { }
}) {
    const [searchQuery, setSearchQuery] = useState('');
    const [expandedFolders, setExpandedFolders] = useState({});
    const [contextMenu, setContextMenu] = useState(null);
    const containerRef = useRef(null);

    // Build recursive tree structure from flat files list
    const fullTree = useMemo(() => {
        return buildFileTree(files, rootName);
    }, [files, rootName]);

    // Filter tree when search input changes
    const { node: displayTree, matchingPaths } = useMemo(() => {
        return filterTree(fullTree, searchQuery);
    }, [fullTree, searchQuery]);

    // Expand parent folders automatically on search match
    useEffect(() => {
        if (searchQuery.trim() && matchingPaths.size > 0) {
            setExpandedFolders(prev => {
                const next = { ...prev };
                matchingPaths.forEach(p => {
                    next[p] = true;
                });
                return next;
            });
        }
    }, [searchQuery, matchingPaths]);

    const toggleFolder = (folderId) => {
        setExpandedFolders(prev => ({
            ...prev,
            [folderId]: prev[folderId] === false ? true : false
        }));
    };

    const expandAll = () => {
        const nextState = {};
        function collectFolders(n) {
            if (n.type === 'folder') {
                nextState[n.id] = true;
                (n.children || []).forEach(collectFolders);
            }
        }
        collectFolders(fullTree);
        setExpandedFolders(nextState);
    };

    const collapseAll = () => {
        const nextState = {};
        function collectFolders(n) {
            if (n.type === 'folder') {
                nextState[n.id] = false;
                (n.children || []).forEach(collectFolders);
            }
        }
        collectFolders(fullTree);
        setExpandedFolders(nextState);
    };

    // Handle right-click context menu
    const handleContextMenu = (e, node) => {
        setContextMenu({
            mouseX: e.clientX,
            mouseY: e.clientY,
            node
        });
    };

    const closeContextMenu = () => {
        setContextMenu(null);
    };

    useEffect(() => {
        const handleClickOutside = () => closeContextMenu();
        window.addEventListener('click', handleClickOutside);
        return () => window.removeEventListener('click', handleClickOutside);
    }, []);

    const selectedFilePath = selectedFile?.filePath || selectedFile?.path || '';

    return (
        <div className="vscode-tree-explorer" ref={containerRef}>
            {/* Search & Action Bar */}
            <div className="vscode-tree-header">
                <div className="vscode-tree-title-row">
                    <span className="vscode-tree-title">FILES ({files.length})</span>
                    <div className="vscode-tree-actions">
                        <button
                            className="vscode-action-btn"
                            onClick={expandAll}
                            title="Expand All Folders"
                        >
                            <ChevronsDown size={14} />
                        </button>
                        <button
                            className="vscode-action-btn"
                            onClick={collapseAll}
                            title="Collapse All Folders"
                        >
                            <ChevronsUp size={14} />
                        </button>
                    </div>
                </div>

                {/* Search input */}
                <div className="vscode-tree-search">
                    <Search size={13} className="vscode-search-icon" />
                    <input
                        type="text"
                        placeholder="Search files by name..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                    {searchQuery && (
                        <button
                            className="vscode-search-clear"
                            onClick={() => setSearchQuery('')}
                            title="Clear search"
                        >
                            <X size={12} />
                        </button>
                    )}
                </div>
            </div>

            {/* Tree Content Area */}
            <div className="vscode-tree-container">
                {displayTree && Array.isArray(displayTree.children) && displayTree.children.length > 0 ? (
                    displayTree.children.map(child => (
                        <TreeNode
                            key={child.id || child.path}
                            node={child}
                            depth={0}
                            selectedFilePath={selectedFilePath}
                            expandedFolders={expandedFolders}
                            toggleFolder={toggleFolder}
                            onFileSelect={onFileSelect}
                            onContextMenu={handleContextMenu}
                        />
                    ))
                ) : (
                    <div className="vscode-tree-empty">
                        {searchQuery ? `No files matching "${searchQuery}"` : 'No files found in repository'}
                    </div>
                )}
            </div>

            {/* Context Menu */}
            {contextMenu && (
                <div
                    className="vscode-context-menu"
                    style={{ top: `${contextMenu.mouseY}px`, left: `${contextMenu.mouseX}px` }}
                    onClick={(e) => e.stopPropagation()}
                >
                    {contextMenu.node.type === 'file' && (
                        <div
                            className="vscode-context-item"
                            onClick={() => {
                                onFileSelect(contextMenu.node.fileData || contextMenu.node);
                                closeContextMenu();
                            }}
                        >
                            <ExternalLink size={13} />
                            Open File
                        </div>
                    )}

                    <div
                        className="vscode-context-item"
                        onClick={() => {
                            navigator.clipboard.writeText(contextMenu.node.path || contextMenu.node.name);
                            closeContextMenu();
                        }}
                    >
                        <Copy size={13} />
                        Copy Relative Path
                    </div>

                    <div
                        className="vscode-context-item"
                        onClick={() => {
                            navigator.clipboard.writeText(contextMenu.node.name);
                            closeContextMenu();
                        }}
                    >
                        <Copy size={13} />
                        Copy Name
                    </div>
                </div>
            )}
        </div>
    );
}
