import { useState } from "react";
import { useParams } from "react-router-dom";

function PatternUpload() {
    const { folderName } = useParams();
    const [title, setTitle] = useState("");
    const [pdf, setPdf] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const formData = new FormData();
        formData.append("title", title);
        formData.append("patternPdf", pdf);

        await fetch(`http://localhost:8080/patterns/${folderName}`, {
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
                PDF Pattern
                <input
                    type="file"
                    accept="application/pdf"
                    onChange={(e) => setPdf(e.target.files[0])}
                    required
                />
            </label>

            <button type="submit">Upload</button>
        </form>
    );
}

export default PatternUpload;
