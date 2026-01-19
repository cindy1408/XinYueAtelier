import { useEffect, useState } from "react";
import FolderList from "./ListFolders";
import CreateFolder from "./CreateFolder";

export default function HomePage() {
    const [folders, setFolders] = useState([]);
    const [showForm, setShowForm] = useState(false);

    const fetchFolders = async () => {
        const res = await fetch("http://localhost:8080/folder");
        const data = await res.json();
        setFolders(data);
    }

    useEffect(() => {
        fetchFolders();
    }, []);

    return (
        <div>
            <h2>Folders</h2>

            <button onClick={() => setShowForm(!showForm)}>
                {showForm ? "Cancel" : "Create New Folder"}
            </button>

            <FolderList folders={folders} />

            {showForm && (
                <CreateFolder
                    onCreated={() => {
                        fetchFolders();
                        setShowForm(false);
                    }}
                />
            )}
        </div>
    );
}
