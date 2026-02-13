import { useNavigate } from "react-router-dom";
import { useState } from "react";

function FolderList({ folders, onEdit }) {
  const navigate = useNavigate();
  const [editFolder, setEditFolder] = useState(null);

  return (
    <div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: "24px" }}>
        {folders?.length > 0 ? (
          folders.map(
            (folder) =>
              folder && (
                <div
                  key={folder.id}
                  style={{
                    display: "flex",
                    flexDirection: "row",
                    border: "1px solid #ccc",
                    borderRadius: "8px",
                    padding: "12px",
                    backgroundColor: "#fafafa",
                    gap: "12px",
                  }}
                >
                  {folder.imagePath && (
                    <img
                      src={`http://localhost:8080/data/${folder.imagePath}`}
                      alt={folder.folderName}
                      style={{
                        width: "200px",
                        height: "auto",
                        objectFit: "contain",
                        borderRadius: "8px",
                      }}
                    />
                  )}
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      marginLeft: "12px",
                      gap: "4px",
                      color: "#333",
                    }}
                  >
                    <span>
                      <strong>Title:</strong> {folder.folderName}
                    </span>
                    <span>
                      <strong>Garment Type:</strong> {folder.garmentType}
                    </span>
                    <span>
                      <strong>Origin:</strong> {folder.origin}
                    </span>
                    <span>
                      <strong>Level:</strong> {folder.level}
                    </span>

                    <button onClick={() => navigate(`/${folder.id}`)}>
                      Go to folder
                    </button>

                    <button onClick={() => onEdit(folder)}>✏️ Edit</button>
                  </div>
                </div>
              ),
          )
        ) : (
          <p>No folders found</p>
        )}

        {editFolder && (
          <EditFolderModal
            folder={editFolder}
            onClose={() => setEditFolder(null)}
            onSaved={() => {
              onEdit();
              setEditFolder(null);
            }}
          />
        )}
      </div>
    </div>
  );
}

export default FolderList;
