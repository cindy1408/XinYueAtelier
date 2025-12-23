import { useState } from "react";

function PatternUpload() {
    const [title, setTitle] = useState("");
    const [level, setLevel] = useState("");
    const [origin, setOrigin] = useState("DRAFTED");
    const [pdf, setPdf] = useState(null);
    const [image, setImage] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const formData = new FormData();
        formData.append("title", title);
        formData.append("level", level);
        formData.append("origin", origin);
        formData.append("patternPdf", pdf);
        formData.append("image", image);

        await fetch("http://localhost:8080/patterns", {
            method: "POST",
            body: formData,
        });

        alert("Pattern uploaded");
    };

    return (
        <form onSubmit={handleSubmit}>
            <h2>Upload Pattern</h2>

            <label>
                Title
                <input
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                />
            </label>

            <label>
                Level
                <input
                    type="text"
                    value={level}
                    onChange={(e) => setLevel(e.target.value)}
                />
            </label>

            <label>
                Origin
                <select
                    value={origin}
                    onChange={(e) => setOrigin(e.target.value)}
                >
                    <option value="DRAFTED">Drafted</option>
                    <option value="ACQUIRED">Acquired</option>
                </select>
            </label>

            <label>
                PDF Pattern
                <input
                    type="file"
                    accept="application/pdf"
                    onChange={(e) => setPdf(e.target.files[0])}
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

            <button type="submit">Upload</button>
        </form>
    );
}

export default PatternUpload;
