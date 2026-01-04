import { useState } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import FolderList from "./components/ListDirectories";
import EachFolder from "./components/EachFolder";
import CreateDirectory from "./components/CreateDirectory";

function App() {
  const [showForm, setShowForm] = useState(false);
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<FolderList />} />
        <Route path="/:folderName" element={<EachFolder />} />
      </Routes>

      <button onClick={() => setShowForm(!showForm)}>
        Create New Folder
      </button>

      {showForm && <CreateDirectory />}
    </BrowserRouter>
  )
}

export default App
