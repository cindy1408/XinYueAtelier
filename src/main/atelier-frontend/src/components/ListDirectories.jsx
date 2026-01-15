import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import EditFolderModal from "./EditFolderModal";

function FolderList() {
    const navigate = useNavigate();
    const [localFolders, setLocalFolders] = useState([]);
    const [editFolder, setEditFolder] = useState(null);

    // Fetch folders from backend
    const fetchFolders = async () => {
        try {
            const res = await fetch("http://localhost:8080/directory");
            const data = await res.json();
            setLocalFolders(Array.isArray(data) ? data : []);
        } catch (err) {
            console.error("Failed to fetch folders:", err);
        }
    };

    useEffect(() => {
        fetchFolders();
    }, []);

    // Called by modal after save
    const handleSaved = (updatedFolder) => {
        if (!updatedFolder?.id) return;
        setLocalFolders(prev =>
            prev.map(f => f.id === updatedFolder.id ? updatedFolder : f)
        );
        setEditFolder(null); // close modal
    };

    return (
        <div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "24px" }}>
                {localFolders?.length > 0 ? (
                    localFolders.map(folder => folder && (
                        <div
                            key={folder.id}
                            style={{
                                display: "flex",
                                flexDirection: "row",
                                border: "1px solid #ccc",
                                borderRadius: "8px",
                                padding: "12px",
                                backgroundColor: "#fafafa",
                                gap: "12px" // space between image and text
                            }}
                        >
                            {folder.imagePath && (
                                <img src=
                                    {`http://localhost:8080/data/${folder.imagePath}`}
                                    alt={folder.folderName}
                                    style={{
                                        width: "200px",
                                        height: "auto",
                                        objectFit: "contain",
                                        borderRadius: "8px"
                                    }} />)}
                            <div style={{
                                display: "flex",
                                flexDirection: "column",
                                marginLeft: "12px",
                                gap: "4px"
                            }}
                            >
                                <span><strong>Title:</strong> {folder.folderName}</span>
                                <span><strong>Garment Type:</strong> {folder.garmentType}</span>
                                <span><strong>Origin:</strong> {folder.origin}</span>
                                <span><strong>Level:</strong> {folder.level}</span>

                                <button onClick={() => navigate(`/${folder.id}`)}>
                                    Go to folder
                                </button>

                                <button onClick={() => setEditFolder(folder)}>✏️ Edit</button>
                            </div>
                        </div>
                    ))
                ) : (
                    <p>No folders found</p>
                )}
            </div>

            {editFolder && (
                <EditFolderModal
                    folder={editFolder}
                    onClose={() => setEditFolder(null)}
                    onSaved={handleSaved} 
                />
            )}
        </div>
    );
}

export default FolderList;
