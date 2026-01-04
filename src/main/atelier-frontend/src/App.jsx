import { BrowserRouter, Routes, Route } from "react-router-dom";
import FolderList from "./components/ListDirectories";
import EachFolder from "./components/EachFolder";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<FolderList />} />
        <Route path="/:folderName" element={<EachFolder />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
