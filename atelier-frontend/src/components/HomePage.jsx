import { useEffect, useState, useCallback } from "react";
import FolderList from "./FolderList";
import CreateFolder from "./CreateFolder";
import EditFolderModal from "./EditFolderModal";
import { useAuth } from "./useAuth";
import { apiFetch } from '../api/apiFetch';

export default function HomePage() {
  const [folders, setFolders] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editFolder, setEditFolder] = useState(null);
  const [activeTab, setActiveTab] = useState("patterns"); // 👈 new

  const { token } = useAuth();



  const fetchFolders = useCallback(async () => {
    try {
      const res = await apiFetch(`/folder`);
      const data = await res.json();
      setFolders(data);
    } catch (err) {
      console.error("Failed to fetch folders", err);
    }
  }, [token]);

  useEffect(() => {
    const loadFolders = async () => {
      try {
        const res = await apiFetch(`/folder`);
        const data = await res.json();
        setFolders(data);
      } catch (err) {
        console.error("Failed to fetch folders", err);
      }
    };

    loadFolders();
  }, [token]);

  const handleFolderCreated = () => {
    fetchFolders();
    setShowForm(false);
  };

  const handleFolderUpdated = (updatedFolder) => {
    setFolders((prev) =>
      prev.map((f) => (f.id === updatedFolder.id ? updatedFolder : f))
    );
    setEditFolder(null);
  };

  const handleDeleteFolder = async (folder) => {
    if (!window.confirm(`Delete "${folder.folderName}" and all its contents?`)) return;
    try {
      const res = await apiFetch(`/folder/${folder.id}`, { method: 'DELETE' });
      if (res.ok) {
        fetchFolders();
      } else {
        console.error("Failed to delete folder");
      }
    } catch (err) {
      console.error("Error deleting folder:", err);
    }
  };

  const courseFolders = folders.filter(f => f.garmentType === "COURSE");
  const patternFolders = folders.filter(f => f.garmentType !== "COURSE");

  const displayedFolders = activeTab === "courses" ? courseFolders : patternFolders;

  return (
    <div>
      <h2>Folders</h2>

      <button onClick={() => setShowForm(!showForm)}>
        {showForm ? "Cancel" : "Create New Folder"}
      </button>

      {showForm && (
        <CreateFolder onCreated={handleFolderCreated} />
      )}

      <div style={{ display: "flex", gap: "8px", margin: "16px 0" }}>
        <button
          onClick={() => setActiveTab("patterns")}
          style={{ fontWeight: activeTab === "patterns" ? "bold" : "normal" }}
        >
          Patterns
        </button>
        <button
          onClick={() => setActiveTab("courses")}
          style={{ fontWeight: activeTab === "courses" ? "bold" : "normal" }}
        >
          Courses
        </button>
      </div>

      <FolderList
        folders={displayedFolders}
        onEdit={(folder) => setEditFolder(folder)}
        onDelete={handleDeleteFolder}
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