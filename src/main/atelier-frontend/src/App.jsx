import { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import FolderList from "./components/ListDirectories";
import EachFolder from "./components/EachFolder";
import CreateDirectory from "./components/CreateDirectory";

function App() {
  const [folders, setFolders] = useState([]);
  const [showForm, setShowForm] = useState(false);

  const fetchFolders = async () => {
    const res = await fetch("http://localhost:8080/directory");
    const data = await res.json();
    setFolders(data);
  }

  useEffect(() => {
    fetchFolders();
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<FolderList folders={folders} />} />
        <Route path="/:folderName" element={<EachFolder />} />
      </Routes>

      <button onClick={() => setShowForm(!showForm)}>
        Create New Folder
      </button>

      {showForm && <CreateDirectory onCreated={() => { fetchFolders(); setShowForm(false); }} />}
    </BrowserRouter>
  )
}

export default App
