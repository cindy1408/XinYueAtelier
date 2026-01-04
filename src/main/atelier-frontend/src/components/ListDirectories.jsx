import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

function FolderList() {
    const [folders, setFolders] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        async function fetchFolders() {
            const res = await fetch("http://localhost:8080/directory");
            const data = await res.json();
            setFolders(data);
        }
        fetchFolders();
    }, []);

    return (
        <div>
            <h2>Folders</h2>
            <ul>
                {folders.map((folder) => (
                    <li key={folder}>
                        <button onClick={() => navigate(`/${folder}`)}>
                            {folder}
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default FolderList;
