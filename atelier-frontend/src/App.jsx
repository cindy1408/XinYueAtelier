import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "./components/HomePage";
import EachFolder from "./components/EachFolder";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/:folderId" element={<EachFolder />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
