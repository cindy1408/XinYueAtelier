/* eslint-disable react-hooks/set-state-in-effect */

import { useParams } from "react-router-dom";
import { useEffect, useState, useCallback } from "react";
import PatternUpload from "./PatternUpload";
import CreateFolder from "./CreateFolder";
import FolderList from "./FolderList";
import EditFolderModal from "./EditFolderModal";
import DeleteFileModal from "./DeleteFileModal";

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
      const res = await fetch(`http://localhost:8080/folder/${folderId}`);
      const data = await res.json();
      setFolder(data);
    } catch (err) {
      console.error("Failed to fetch folder", err);
    }
  }, [folderId]);

  const fetchChildren = useCallback(async () => {
    try {
      const res = await fetch(
        `http://localhost:8080/folder/${folderId}/children`,
      );
      const data = await res.json();

      setChildren(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch children:", err);
    }
  }, [folderId]);

  const fetchFiles = useCallback(async () => {
    try {
      const res = await fetch(
        `http://localhost:8080/patterns/${folderId}/files`,
      );
      const data = await res.json();
      setFiles(data);
    } catch (err) {
      console.error("Failed to fetch files", err);
    }
  }, [folderId]);

  const handleDeleteFile = async (fileId) => {
    try {
      const res = await fetch(`http://localhost:8080/patterns/${fileId}`, {
        method: "DELETE",
      });
      if (res.ok) {
        fetchFiles();
      } else {
        console.error("Failed to delete file");
      }
    } catch (err) {
      console.error("Error deleting file:", err);
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

      {/* Create a subfolder under this folder */}
      <CreateFolder
        parentId={folderId}
        onCreated={() => fetchChildren()} // refresh subfolders after creation
      />

      {/* Subfolders */}
      <h3>Subfolders</h3>
      {children.length === 0 ? (
        <p>No subfolders</p>
      ) : (
        <FolderList
          folders={children}
          onEdit={(folder) => setEditFolder(folder)}
        />
      )}

      <h3>Files</h3>
      {files.length === 0 ? (
        <p>No files in this folder</p>
      ) : (
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "24px",
          }}
        >
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
              <iframe
                src={`http://localhost:8080/patterns/preview/${file.id}`}
                width="200px"
                height="260px"
                style={{ border: "none" }}
              />
              <h4 style={{ textAlign: "center" }}>{file.title}</h4>
              <div style={{ display: "flex", gap: "8px" }}>
                <button onClick={() => setModalFile(file)}>View</button>
                <button
                  onClick={() =>
                    window.open(
                      `http://localhost:8080/patterns/download/${file.id}`,
                      "_blank",
                    )
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
          <iframe
            src={`http://localhost:8080/patterns/preview/${modalFile.id}`}
            style={{
              width: "90%",
              height: "90%",
              border: "none",
              borderRadius: "8px",
            }}
          />
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
