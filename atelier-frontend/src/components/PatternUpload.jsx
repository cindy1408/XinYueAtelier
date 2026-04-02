import { useState } from "react";
import { useParams } from "react-router-dom";
import { apiFetch } from '../api/apiFetch';

function PatternUpload({ onUpload, uploadPath }) {
  const { folderId } = useParams();
  const [title, setTitle] = useState("");
  const [pdf, setPdf] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const formData = new FormData();
    formData.append("title", title);
    formData.append("patternPdf", pdf);

    const path = uploadPath ?? `/patterns/${folderId}`;

    try {
      const response = await apiFetch(path, {
        method: "POST",
        body: formData,
      });
      if (!response.ok) {
        const errText = await response.text();
        alert("Failed to upload pattern: " + errText);
      } else {
        setTitle("");
        setPdf(null);
        onUpload();
        alert("Pattern uploaded");
      }
    } catch (err) {
      console.error(err);
      alert("Error uploading pattern");
    }
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