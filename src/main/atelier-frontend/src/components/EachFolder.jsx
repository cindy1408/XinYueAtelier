import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import PatternUpload from "./PatternUpload";

function EachFolder() {
    const { folderId } = useParams();
    const [folder, setFolder] = useState(null);
    const [files, setFiles] = useState([]);
    const [modalFile, setModalFile] = useState(null);

    const fetchFolder = async () => {
        try {
            const res = await fetch(
                `http://localhost:8080/directory/${folderId}`
            );
            const data = await res.json();
            setFolder(data);
        } catch (err) {
            console.error("Failed to fetch folder", err);
        }
    };

    const fetchFiles = async () => {
        try {
            const res = await fetch(
                `http://localhost:8080/patterns/${folderId}/files`
            );
            const data = await res.json();
            setFiles(data);
        } catch (err) {
            console.error("Failed to fetch files", err);
        }
    };

    useEffect(() => {
        fetchFolder();
        fetchFiles();
    }, [folderId]);

    return (
        <div>
            <h2>Folder: {folder ? folder.folderName : "Loading..."}</h2>

            <PatternUpload onUpload={fetchFiles} />

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
                                            "_blank"
                                        )
                                    }
                                >
                                    Download
                                </button>
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
        </div>

    );
}

export default EachFolder;
