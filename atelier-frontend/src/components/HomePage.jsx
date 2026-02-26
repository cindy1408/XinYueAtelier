/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useState, useCallback } from "react";
import FolderList from "./FolderList";
import CreateFolder from "./CreateFolder";
import EditFolderModal from "./EditFolderModal";
import { useAuth } from "./useAuth";

export default function HomePage() {
  const [folders, setFolders] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editFolder, setEditFolder] = useState(null);

const { token } = useAuth();

   const fetchFolders = useCallback(async () => {
        try {
            const res = await fetch("http://localhost:8080/folder", {
                headers: { Authorization: `Bearer ${token}` }
            });
            const data = await res.json();
            setFolders(data);
        } catch (err) {
            console.error("Failed to fetch folders", err);
        }
    }, [token]); 

  useEffect(() => {
        fetchFolders();
    }, [fetchFolders]); 

  // Called by CreateFolder after new folder is created
  const handleFolderCreated = () => {
    fetchFolders();
    setShowForm(false);
  };

  // Called by EditFolderModal after folder is updated
  const handleFolderUpdated = (updatedFolder) => {
    setFolders((prev) =>
      prev.map((f) => (f.id === updatedFolder.id ? updatedFolder : f)),
    );
    setEditFolder(null);
  };

  return (
    <div>
      <h2>Folders</h2>

      <button onClick={() => setShowForm(!showForm)}>
        {showForm ? "Cancel" : "Create New Folder"}
      </button>

      {showForm && (
        <CreateFolder
          onCreated={() => {
            handleFolderCreated();
          }}
        />
      )}

      <FolderList
        folders={folders}
        onEdit={(folder) => setEditFolder(folder)}
      />

      {editFolder && (
        <EditFolderModal
          folder={editFolder}
          onSaved={handleFolderUpdated}
          onClose={() => setEditFolder(null)}
        />
      )}
    </div>
  );
}
