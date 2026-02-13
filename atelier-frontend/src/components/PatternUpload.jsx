import { useState } from "react";
import { useParams } from "react-router-dom";

function PatternUpload({ onUpload }) {
  const { folderId } = useParams();
  const [title, setTitle] = useState("");
  const [pdf, setPdf] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();

    const formData = new FormData();
    formData.append("title", title);
    formData.append("patternPdf", pdf);

    try {
      const response = await fetch(
        `http://localhost:8080/patterns/${folderId}`,
        {
          method: "POST",
          body: formData,
        },
      );
      if (!response.ok) {
        const errText = await response.text();
        alert("Failed to create folder: " + errText);
      } else {
                  setTitle("");
                  setPdf(null);
                  onUpload();
      }
    } catch (err) {
      console.error(err);
      alert("Error uploading pattern");
      return;
    }
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
