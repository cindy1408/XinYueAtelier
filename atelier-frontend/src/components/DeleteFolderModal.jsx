function DeleteFolderModal({ folder, onClose, onSaved }) {
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
        <h2>Delete Folder</h2>
        <p>
          Are you sure you want to delete the folder "
          <strong>{folder.folderName}</strong>"? This action cannot be undone.
        </p>

        <div style={{ marginTop: "16px" }}>
          <button
            onClick={async () => {
              try {
                const res = await fetch(
                  `http://localhost:8080/folder/${folder.id}`,
                  {
                    method: "DELETE",
                  },
                );

                if (!res.ok) {
                  const text = await res.text();
                  throw new Error("Failed to delete folder: " + text);
                }

                onSaved(folder.id);
              } catch (err) {
                console.error(err);
                alert(err.message);
              }
            }}
            style={{
              marginRight: "8px",
              background: "#d11a2a",
              color: "white",
              padding: "6px 12px",
              borderRadius: "4px",
            }}
          >
            Delete
          </button>
          <button onClick={onClose}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

export default DeleteFolderModal;
