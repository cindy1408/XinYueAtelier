import { useState } from "react";
import { useParams } from "react-router-dom";

function CreateFolder({ onCreated }) {
  const { folderId } = useParams();
  const parentId = folderId ?? null;
  const [title, setTitle] = useState("");
  const [garmentType, setGarmentType] = useState("COURSE");
  const [level, setLevel] = useState("BEGINNER");
  const [origin, setOrigin] = useState("DRAFTED");
  const [image, setImage] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!title) return alert("Please enter a folder name");
    if (!image) return alert("Please select a cover image");

    setLoading(true);

    const formData = new FormData();
    formData.append("title", title);
    formData.append("garmentType", garmentType);
    formData.append("level", level);
    formData.append("origin", origin);
    formData.append("image", image);

    const url = parentId
      ? `http://localhost:8080/folder/${parentId}`
      : `http://localhost:8080/folder`;

    try {
      const response = await fetch(url, {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        onCreated();
        setTitle("");
        setGarmentType("COURSE");
        setLevel("BEGINNER");
        setOrigin("DRAFTED");
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
        Garment Type
        <select
          value={garmentType}
          onChange={(e) => setGarmentType(e.target.value)}
        >
          <option value="COURSE">Course</option>
          <option value="ACCESSORY">Accessory</option>
          <option value="BLAZER">Blazer</option>
          <option value="BLOUSE">Blouse</option>
          <option value="BRIDAL">Bridal</option>
          <option value="DRESS">Dress</option>
          <option value="KNIT">Knit</option>
          <option value="OUTERWEAR">Outerwear</option>
          <option value="SKIRT">Skirt</option>
          <option value="TROUSERS">Trousers</option>
          <option value="UNDERWEAR">Underwear</option>
        </select>
      </label>

      <label>
        Origin
        <select value={origin} onChange={(e) => setOrigin(e.target.value)}>
          <option value="DRAFTED">Drafted</option>
          <option value="ACQUIRED">Acquired</option>
        </select>
      </label>

      <label>
        Level
        <select value={level} onChange={(e) => setLevel(e.target.value)}>
          <option value="BEGINNER">Beginner</option>
          <option value="INTERMEDIATE">Intermediate</option>
          <option value="ADVANCE">Advance</option>
        </select>
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

export default CreateFolder;
