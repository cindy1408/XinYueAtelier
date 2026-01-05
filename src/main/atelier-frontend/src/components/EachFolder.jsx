import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import PatternUpload from "./PatternUpload";

function EachFolder() {
    const { folderName } = useParams();
    const [files, setFiles] = useState([]);

    const fetchFiles = async () => {
        try {
            const res = await fetch(
                `http://localhost:8080/directory/${folderName}/files`
            );
            const data = await res.json();
            setFiles(data);
        } catch (err) {
            console.error("Failed to fetch files", err);
        }
    };

    useEffect(() => {
        fetchFiles();
    }, [folderName]); 

    return (
        <div>
            <h2>Folder: {folderName}</h2>

            <h3>Files</h3>
            {files.length === 0 ? (
                <p>No files in this folder</p>
            ) : (
                <ul>
                    {files.map((file) => (
                        <li key={file}>{file}</li>
                    ))}
                </ul>
            )}

            <PatternUpload onUpload={() => {
                fetchFiles();
            }} />
        </div>
    );
}

export default EachFolder;
