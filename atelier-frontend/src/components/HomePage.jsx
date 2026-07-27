import { useEffect, useState, useCallback } from "react";
import FolderList from "./FolderList";
import CreateFolder from "./CreateFolder";
import EditFolderModal from "./EditFolderModal";
import { apiFetch } from '../api/apiFetch';

export default function HomePage() {
  const [folders, setFolders] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editFolder, setEditFolder] = useState(null);
  const [activeTab, setActiveTab] = useState("patterns");

  const fetchFolders = useCallback(async (signal) => {
    try {
      const res = await apiFetch(`/folder`, { signal });
      const data = await res.json();
      setFolders(data);
    } catch (err) {
      if (err.name !== "AbortError") {
        console.error("Failed to fetch folders", err);
      }
    }
  }, []);

  const handleFolderCreated = () => {
    fetchFolders();
    setShowForm(false);
  };

  useEffect(() => {
    const controller = new AbortController();
    // eslint-disable-next-line
    fetchFolders(controller.signal);
    return () => controller.abort();
  }, [fetchFolders]);

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