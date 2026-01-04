import { useParams } from "react-router-dom";
import PatternUpload from "./PatternUpload";

function EachFolder() {
    const { folderName } = useParams();

    return (
        <div>
            <h2>Folder: {folderName}</h2>
            <PatternUpload />
        </div>
    );
}

export default EachFolder;
