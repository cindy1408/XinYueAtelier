import { useState } from "react";

function CreateDirectory() {
    const [name, setName] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!name) return alert("Please enter a folder name");


        const response = await fetch(`http://localhost:8080/directory/${name}`, {
            method: "POST",
        });

        if (response.ok) {
            alert("Directory created");
        } else {
            alert("Failed to create directory");
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Folder name"
            />
            <button type="submit">Create Directory</button>
        </form>
    );
}

export default CreateDirectory;