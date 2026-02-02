function DeleteFileModal({ file, onCancel, onConfirm }) {
  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.5)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
      }}
    >
      <div
        style={{
          background: "white",
          padding: "20px",
          borderRadius: "8px",
          width: "320px",
          textAlign: "center",
        }}
      >
        <h3>Delete file?</h3>

        <p style={{ marginBottom: "8px" }}>Are you sure you want to delete:</p>

        <strong>{file.fileName}</strong>

        <div style={{ marginTop: "16px" }}>
          <button
            onClick={onConfirm}
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

          <button onClick={onCancel}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

export default DeleteFileModal;
