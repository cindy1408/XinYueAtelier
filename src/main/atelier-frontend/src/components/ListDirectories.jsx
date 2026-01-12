import { useNavigate } from "react-router-dom";

function FolderList({ folders }) {
    const navigate = useNavigate();

    return (
        <div>
            <h2>Folders</h2>
            <div
                style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "24px"
                }}
            >
                {folders.map((folder) => (
                    <div
                        key={folder.id}
                        style={{
                            display: "flex",
                            border: "1px solid #ccc",
                            borderRadius: "8px",
                            overflow: "hidden",
                            padding: "12px",
                            boxSizing: "border-box",
                            alignItems: "flex-start", // image and text align at top
                            backgroundColor: "#fafafa"
                        }}
                    >
                        {folder.imagePath && (
                            <img
                                src={`http://localhost:8080/data/${folder.imagePath}`}
                                alt={folder.folderName}
                                style={{
                                    width: "200px",  // keep your preferred width
                                    height: "auto",  // natural height
                                    objectFit: "cover",
                                    borderRadius: "8px"
                                }}
                            />
                        )}
                        <div
                            style={{
                                display: "flex",
                                flexDirection: "column",
                                marginLeft: "12px",
                                gap: "4px"
                            }}
                        >
                            <span><strong>Title:</strong> {folder.folderName}</span>
                            <span><strong>Origin:</strong> {folder.origin}</span>
                            <span><strong>Level:</strong> {folder.level}</span>
                            <button
                                onClick={() => navigate(`/${folder.id}`)}
                                style={{
                                    fontWeight: "bold",
                                    cursor: "pointer",
                                    padding: "4px 8px",
                                    borderRadius: "4px",
                                    border: "1px solid #888",
                                    backgroundColor: "#f0f0f0"
                                }}
                            >
                                {folder.folderName}
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default FolderList;
