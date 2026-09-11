import { useState } from "react";
import { useParams } from "react-router-dom";
import { apiFetch } from "../api/apiFetch";

function CreateSubFolder({ onCreated }) {
    const { folderId } = useParams();
    const parentId = folderId ?? null;
    const [title, setTitle] = useState("");
    const [image, setImage] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!title) return alert("Please enter a folder name");
        if (!image) return alert("Please select a cover image");

        setLoading(true);

        const formData = new FormData();
        formData.append("title", title);
        formData.append("image", image);

        try {
            const response = await apiFetch(parentId ? `/folder/${parentId}` : `/folder`, {
                method: "POST",
                body: formData,
            });

            if (response.ok) {
                onCreated();
                setTitle("");
                setImage(null);
            } else {
                const errText = await response.text();
                alert("Failed to create folder: " + errText);
            }
        } catch (err) {
            console.error(err);
            alert("Error creating folder");
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>Create Folder</h2>
            <label>
                Folder Name
                <input
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                />
            </label>

            <label>
                Cover Image
                <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setImage(e.target.files[0])}
                    required
                />
            </label>

            <button type="submit" disabled={loading}>
                {loading ? "Creating..." : "Create"}
            </button>
        </form>
    );
}

export default CreateSubFolder;
