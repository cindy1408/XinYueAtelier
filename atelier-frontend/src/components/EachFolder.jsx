import { useParams } from "react-router-dom";
import { useEffect, useState, useCallback } from "react";
import PatternUpload from "./PatternUpload";
import CreateFolder from "./CreateFolder";
import FolderList from "./FolderList";
import EditFolderModal from "./EditFolderModal";
import DeleteFileModal from "./DeleteFileModal";
import { apiFetch } from '../api/apiFetch';
import { API_URL } from '../config';

function PatternPreview({ fileId, width = "200px", height = "260px" }) {
  const [blobUrl, setBlobUrl] = useState(null);

  useEffect(() => {
    let objectUrl;
    apiFetch(`/patterns/preview/${fileId}`)
      .then(res => res.json())
      .then(({ url }) => fetch(url))          // fetch the S3 PDF directly
      .then(res => res.blob())                 // convert to blob
      .then(blob => {
        objectUrl = URL.createObjectURL(blob); // create local blob URL
        setBlobUrl(objectUrl);
      })
      .catch(console.error);

    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl); // cleanup on unmount
    };
  }, [fileId]);

  if (!blobUrl) return <div style={{ width, height, display: "flex", alignItems: "center", justifyContent: "center" }}>Loading...</div>;

  return (
  <iframe
    src={blobUrl}
    style={{ width, height, border: "none", borderRadius: "8px" }}
  />
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

  const fetchFolder = useCallback(async () => {
    try {
      const res = await apiFetch(`/folder/${folderId}`);
      const data = await res.json();
      setFolder(data);
    } catch (err) {
      console.error("Failed to fetch folder", err);
    }
  }, [folderId]);

  const fetchChildren = useCallback(async () => {
    try {
      const res = await apiFetch(`/folder/${folderId}/children`);
      const data = await res.json();
      setChildren(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch children:", err);
    }
  }, [folderId]);

  const fetchFiles = useCallback(async () => {
    try {
      const res = await apiFetch(`/patterns/${folderId}/files`);
      const data = await res.json();
      setFiles(data);
    } catch (err) {
      console.error("Failed to fetch files", err);
    }
  }, [folderId]);

  const handleDeleteFile = async (fileId) => {
    try {
      const res = await apiFetch(`/patterns/${fileId}`, { method: 'DELETE' });
      if (res.ok) {
        fetchFiles();
      } else {
        console.error("Failed to delete file");
      }
    } catch (err) {
      console.error("Error deleting file:", err);
    }
  };

  const handleDeleteFolder = async (folder) => {
  if (!window.confirm(`Delete "${folder.folderName}" and all its contents?`)) return;
  try {
    const res = await apiFetch(`/folder/${folder.id}`, { method: 'DELETE' });
    if (res.ok) {
      fetchChildren(); 
    } else {
      console.error("Failed to delete folder");
    }
  } catch (err) {
    console.error("Error deleting folder:", err);
  }
};

  useEffect(() => {
    fetchFolder();
    fetchChildren();
    fetchFiles();
  }, [fetchFolder, fetchChildren, fetchFiles]);

  return (
    <div>
      <h2>Folder: {folder ? folder.folderName : "Loading..."}</h2>

      <PatternUpload onUpload={fetchFiles} />

      <CreateFolder
        parentId={folderId}
        onCreated={() => fetchChildren()}
      />

      <h3>Subfolders</h3>
      {children.length === 0 ? (
        <p>No subfolders</p>
      ) : (
        <FolderList
          folders={children}
          onEdit={(folder) => setEditFolder(folder)}
          onDelete={handleDeleteFolder}
        />
      )}

      <h3>Files</h3>
      {files.length === 0 ? (
        <p>No files in this folder</p>
      ) : (
        <div style={{ display: "flex", flexWrap: "wrap", gap: "24px" }}>
          {files.map((file) => (
            <div
              key={file.id}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                border: "1px solid #ddd",
                padding: "12px",
                borderRadius: "8px",
                boxSizing: "border-box",
                width: "350px",
                gap: "12px",
              }}
            >
              <PatternPreview fileId={file.id} />
              <h4 style={{ textAlign: "center" }}>{file.title}</h4>
              <div style={{ display: "flex", gap: "8px" }}>
                <button onClick={() => setModalFile(file)}>View</button>
                <button
                  onClick={() =>
                    window.open(`${API_URL}/patterns/download/${file.id}`, "_blank")
                  }
                >
                  Download
                </button>
                <button onClick={() => setFileToDelete(file)}>🗑️</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalFile && (
        <div
          style={{
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
          }}
          onClick={() => setModalFile(null)}
        >
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