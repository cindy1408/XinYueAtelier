import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import PatternUpload from "./PatternUpload";

function EachFolder() {
    const { folderId } = useParams();
    const [folder, setFolder] = useState(null);
    const [files, setFiles] = useState([]);

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

            <h3>Files</h3>
            {files.length === 0 ? (
                <p>No files in this folder</p>
            ) : (
                <ul>
                    {files.map((file) => (
                        <li key={file.id}>
                            {file.title}
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
                        </li>
                    ))}
                </ul>
            )}

            <PatternUpload onUpload={() => { fetchFiles(); }} />
        </div>
    );
}

export default EachFolder;
