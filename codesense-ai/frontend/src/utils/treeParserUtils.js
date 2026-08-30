/**
 * Tree Parser Utility
 * Converts flat file lists into a hierarchical, recursive tree structure.
 * Supports Windows (\) and Unix (/) path separators, folder-first sorting,
 * search filtering, and unlimited nesting depth.
 */

export function buildFileTree(files, rootName = 'project') {
    if (!Array.isArray(files) || files.length === 0) {
        return {
            name: rootName,
            type: 'folder',
            path: '',
            children: []
        };
    }

    const rootChildren = {};

    files.forEach(fileObj => {
        // Determine raw path string from various possible object structures
        const rawPath = typeof fileObj === 'string'
            ? fileObj
            : (fileObj.filePath || fileObj.path || fileObj.name || fileObj.fileName || '');

        if (!rawPath) return;

        // Normalize Windows backslashes to forward slashes & remove leading/trailing slashes
        const normalized = rawPath.replace(/\\/g, '/').replace(/^\/+|\/+$/g, '');
        const parts = normalized.split('/').filter(Boolean);

        if (parts.length === 0) return;

        let currentMap = rootChildren;

        for (let i = 0; i < parts.length; i++) {
            const partName = parts[i];
            const isFile = i === parts.length - 1;
            const currentPath = parts.slice(0, i + 1).join('/');

            if (isFile) {
                // File node
                currentMap[partName] = {
                    id: fileObj.id || `file-${currentPath}`,
                    name: partName,
                    type: 'file',
                    path: currentPath,
                    fileData: typeof fileObj === 'object' ? fileObj : { filePath: currentPath, fileName: partName }
                };
            } else {
                // Folder node
                if (!currentMap[partName]) {
                    currentMap[partName] = {
                        id: `folder-${currentPath}`,
                        name: partName,
                        type: 'folder',
                        path: currentPath,
                        childrenMap: {}
                    };
                }
                currentMap = currentMap[partName].childrenMap;
            }
        }
    });

    // Recursive conversion from map to sorted array
    function convertMapToArray(mapNode) {
        const nodes = Object.values(mapNode);

        const processedNodes = nodes.map(node => {
            if (node.type === 'folder') {
                const children = convertMapToArray(node.childrenMap);
                const { childrenMap, ...folderWithoutMap } = node;
                return {
                    ...folderWithoutMap,
                    children
                };
            }
            return node;
        });

        // Sort: Folders first (alphabetically), then Files (alphabetically)
        return processedNodes.sort((a, b) => {
            if (a.type !== b.type) {
                return a.type === 'folder' ? -1 : 1;
            }
            return a.name.localeCompare(b.name, undefined, { sensitivity: 'base', numeric: true });
        });
    }

    return {
        id: 'root',
        name: rootName,
        type: 'folder',
        path: '',
        children: convertMapToArray(rootChildren)
    };
}

/**
 * Filter tree nodes by search query.
 * Keeps matching file nodes and automatically retains their parent folder hierarchy.
 */
export function filterTree(node, query) {
    if (!query || !query.trim()) return { node, matchingPaths: new Set() };

    const cleanQuery = query.trim().toLowerCase();
    const matchingPaths = new Set();

    function search(curr) {
        if (curr.type === 'file') {
            const matches = curr.name.toLowerCase().includes(cleanQuery) || curr.path.toLowerCase().includes(cleanQuery);
            if (matches) {
                matchingPaths.add(curr.path);
                // Add all ancestor paths
                const parts = curr.path.split('/');
                for (let i = 1; i < parts.length; i++) {
                    matchingPaths.add(`folder-${parts.slice(0, i).join('/')}`);
                }
            }
            return matches ? curr : null;
        }

        if (curr.type === 'folder') {
            const filteredChildren = (curr.children || [])
                .map(child => search(child))
                .filter(Boolean);

            const folderMatches = curr.name.toLowerCase().includes(cleanQuery);

            if (folderMatches || filteredChildren.length > 0) {
                matchingPaths.add(curr.id);
                return {
                    ...curr,
                    children: filteredChildren
                };
            }
        }

        return null;
    }

    const filteredNode = search(node);
    return {
        node: filteredNode || { ...node, children: [] },
        matchingPaths
    };
}
