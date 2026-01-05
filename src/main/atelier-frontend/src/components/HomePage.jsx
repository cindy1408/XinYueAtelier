import { useEffect, useState } from "react";
import FolderList from "./ListDirectories";
import CreateDirectory from "./CreateDirectory";

export default function HomePage() {
    const [folders, setFolders] = useState([]);
    const [showForm, setShowForm] = useState(false);

    const fetchFolders = async () => {
        const res = await fetch("http://localhost:8080/directory");
        const data = await res.json();
        setFolders(data);
    }

    useEffect(() => {
        fetchFolders();
    }, []);

    return (
        <div>
            <FolderList folders={folders} />

            <button onClick={() => setShowForm(!showForm)}>
                {showForm ? "Cancel" : "Create New Folder"}
            </button>

            {showForm && (
                <CreateDirectory
                    onCreated={() => {
                        fetchFolders();
                        setShowForm(false);
                    }}
                />
            )}
        </div>
    );
}
