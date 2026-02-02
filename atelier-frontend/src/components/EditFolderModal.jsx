import { useState } from "react";

function EditFolderModal({ folder, onClose, onSaved }) {
  const [formData, setFormData] = useState({ ...folder });
  const [newImage, setNewImage] = useState(null);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleImageChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setNewImage(e.target.files[0]);
    }
  };

  const handleSave = async () => {
    const data = new FormData();
    data.append("folderName", formData.folderName);
    data.append("garmentType", formData.garmentType);
    data.append("origin", formData.origin);
    data.append("level", formData.level);
    if (newImage) data.append("image", newImage);

    try {
      const res = await fetch(`http://localhost:8080/folder/${folder.id}`, {
        method: "PUT",
        body: data,
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error("Failed to save folder: " + text);
      }

      const updatedFolder = await res.json();
      onSaved(updatedFolder);
    } catch (err) {
      console.error(err);
      alert(err.message);
    }
  };

  return (
    <div
      className="modal"
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        background: "rgba(0,0,0,0.5)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <div
        style={{
          background: "white",
          padding: "24px",
          borderRadius: "8px",
          minWidth: "300px",
        }}
      >
        <h2>Edit Folder</h2>

        {/* Folder Name */}
        <input
          name="folderName"
          value={formData.folderName}
          onChange={handleChange}
          placeholder="Folder Name"
          style={{ width: "100%", marginBottom: "8px" }}
        />

        {/* Garment Type Dropdown */}
        <select
          name="garmentType"
          value={formData.garmentType}
          onChange={handleChange}
          style={{ width: "100%", marginBottom: "8px" }}
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

        {/* Origin Dropdown */}
        <select
          name="origin"
          value={formData.origin}
          onChange={handleChange}
          style={{ width: "100%", marginBottom: "8px" }}
        >
          <option value="">Select Origin</option>
          <option value="DRAFTED">DRAFTED</option>
          <option value="ACQUIRED">ACQUIRED</option>
        </select>

        {/* Level Dropdown */}
        <select
          name="level"
          value={formData.level}
          onChange={handleChange}
          style={{ width: "100%", marginBottom: "8px" }}
        >
          <option value="">Select Level</option>
          <option value="BEGINNER">BEGINNER</option>
          <option value="INTERMEDIATE">INTERMEDIATE</option>
          <option value="ADVANCE">ADVANCE</option>
        </select>

        {/* Cover Image Upload */}
        <div style={{ marginBottom: "12px" }}>
          <label>Change Cover Image:</label>
          <input type="file" accept="image/*" onChange={handleImageChange} />
          {folder.imagePath && !newImage && (
            <div style={{ marginTop: "8px" }}>
              <img
                src={`http://localhost:8080/data/${folder.imagePath}`}
                alt={folder.folderName}
                style={{
                  width: "100%",
                  maxHeight: "150px",
                  objectFit: "contain",
                }}
              />
            </div>
          )}
          {newImage && (
            <div style={{ marginTop: "8px" }}>
              <img
                src={URL.createObjectURL(newImage)}
                alt="New Cover"
                style={{
                  width: "100%",
                  maxHeight: "150px",
                  objectFit: "contain",
                }}
              />
            </div>
          )}
        </div>

        <div style={{ marginTop: "12px" }}>
          <button onClick={handleSave} style={{ marginRight: "8px" }}>
            Save
          </button>
          <button onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

export default EditFolderModal;
