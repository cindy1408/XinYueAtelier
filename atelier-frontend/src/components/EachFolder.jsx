import { useParams } from "react-router-dom";
import { useEffect, useState, useCallback } from "react";
import PatternUpload from "./PatternUpload";
import CreateFolder from "./CreateFolder";
import FolderList from "./FolderList";
import EditFolderModal from "./EditFolderModal";
import DeleteFileModal from "./DeleteFileModal";
import { apiFetch } from '../api/apiFetch';
import { API_URL } from '../config';

const styles = {
  fileCard: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    border: "1px solid #ddd",
    padding: "12px",
    borderRadius: "8px",
    boxSizing: "border-box",
    width: "350px",
    gap: "12px",
  },
  fileActions: {
    display: "flex",
    gap: "8px",
  },
  errorBanner: {
    color: "#b00020",
    marginBottom: "12px",
  },
  fileGrid: {
    display: "flex",
    flexWrap: "wrap",
    gap: "24px",
  },
  modalOverlay: {
    position: "fixed",
    top: 0,
    left: 0,
    width: "100vw",
    height: "100vh",
    backgroundColor: "rgba(0,0,0,0.7)",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 1000,
  },
};

function PatternPreview({ fileId, width = "200px", height = "260px" }) {
  const [blobUrl, setBlobUrl] = useState(null);

  useEffect(() => {
    let cancelled = false;
    let objectUrl;

    apiFetch(`/patterns/preview/${fileId}`)
      .then((res) => res.json())
      .then(({ url }) => fetch(url))
      .then((res) => res.blob())
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setBlobUrl(objectUrl);
      })
      .catch((err) => {
        if (!cancelled) console.error("Failed to load preview:", err);
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [fileId]);

  if (!blobUrl) {
    return (
      <div
        style={{
          width,
          height,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        Loading...
      </div>
    );
  }

  return (
    <iframe
      title={`pattern-preview-${fileId}`}
      src={blobUrl}
      style={{ width, height, border: "none", borderRadius: "8px" }}
    />
  );
}

function FileCard({ file, onView, onDelete }) {
  return (
    <div style={styles.fileCard}>
      <PatternPreview fileId={file.id} />
      <h4 style={{ textAlign: "center" }}>{file.title}</h4>
      <div style={styles.fileActions}>
        <button onClick={() => onView(file)}>View</button>
        <button
          onClick={() =>
            window.open(`${API_URL}/patterns/download/${file.id}`, "_blank")
          }
        >
          Download
        </button>
        <button aria-label={`Delete ${file.title}`} onClick={() => onDelete(file)}>
          🗑️
        </button>
      </div>
    </div>
  );
}

function EachFolder() {
  const { folderId } = useParams();
  const [folder, setFolder] = useState(null);
  const [files, setFiles] = useState([]);
  const [children, setChildren] = useState([]);
  const [modalFile, setModalFile] = useState(null);
  const [editFolder, setEditFolder] = useState(null);
  const [fileToDelete, setFileToDelete] = useState(null);
  const [error, setError] = useState(null);

  const fetchFolder = useCallback(async () => {
    try {
      const res = await apiFetch(`/folder/${folderId}`);
      if (!res.ok) throw new Error(`Failed to fetch folder (${res.status})`);
      setFolder(await res.json());
    } catch (err) {
      console.error(err);
      setError("Couldn't load this folder.");
    }
  }, [folderId]);

  const fetchChildren = useCallback(async () => {
    try {
      const res = await apiFetch(`/folder/${folderId}/children`);
      if (!res.ok) throw new Error(`Failed to fetch children (${res.status})`);
      const data = await res.json();
      setChildren(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(err);
      setError("Couldn't load subfolders.");
    }
  }, [folderId]);

  const fetchFiles = useCallback(async () => {
    try {
      const res = await apiFetch(`/patterns/${folderId}/files`);
      if (!res.ok) throw new Error(`Failed to fetch files (${res.status})`);
      setFiles(await res.json());
    } catch (err) {
      console.error(err);
      setError("Couldn't load files.");
    }
  }, [folderId]);

  useEffect(() => {
    setError(null);
    fetchFolder();
    fetchChildren();
    fetchFiles();
  }, [fetchFolder, fetchChildren, fetchFiles]);

  const handleDeleteFile = async (fileId) => {
    try {
      const res = await apiFetch(`/patterns/${fileId}`, { method: "DELETE" });
      if (!res.ok) throw new Error("Failed to delete file");
      fetchFiles();
    } catch (err) {
      console.error(err);
      setError("Couldn't delete that file. Please try again.");
    }
  };

  const handleDeleteFolder = async (folderToDelete) => {
    if (!window.confirm(`Delete "${folderToDelete.folderName}" and all its contents?`)) {
      return;
    }
    try {
      const res = await apiFetch(`/folder/${folderToDelete.id}`, { method: "DELETE" });
      if (!res.ok) throw new Error("Failed to delete folder");
      fetchChildren();
    } catch (err) {
      console.error(err);
      setError("Couldn't delete that folder. Please try again.");
    }
  };

  return (
    <div>
      <h2>Folder: {folder ? folder.folderName : "Loading..."}</h2>

      {error && <div style={styles.errorBanner}>{error}</div>}

      <PatternUpload onUpload={fetchFiles} />

      <CreateFolder parentId={folderId} onCreated={fetchChildren} />

      <h3>Subfolders</h3>
      {children.length === 0 ? (
        <p>No subfolders</p>
      ) : (
        <FolderList
          folders={children}
          onEdit={(f) => setEditFolder(f)}
          onDelete={handleDeleteFolder}
        />
      )}

      <h3>Files</h3>
      {files.length === 0 ? (
        <p>No files in this folder</p>
      ) : (
        <div style={styles.fileGrid}>
          {files.map((file) => (
            <FileCard
              key={file.id}
              file={file}
              onView={setModalFile}
              onDelete={setFileToDelete}
            />
          ))}
        </div>
      )}

      {modalFile && (
        <div style={styles.modalOverlay} onClick={() => setModalFile(null)}>
          <PatternPreview fileId={modalFile.id} width="85vw" height="85vh" />
        </div>
      )}

      {editFolder && (
        <EditFolderModal
          folder={editFolder}
          onClose={() => setEditFolder(null)}
          onSaved={() => {
            fetchChildren();
            setEditFolder(null);
          }}
        />
      )}

      {fileToDelete && (
        <DeleteFileModal
          file={fileToDelete}
          onCancel={() => setFileToDelete(null)}
          onConfirm={async () => {
            await handleDeleteFile(fileToDelete.id);
            setFileToDelete(null);
          }}
        />
      )}
    </div>
  );
}

export default EachFolder;