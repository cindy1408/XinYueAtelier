import { useNavigate } from "react-router-dom";

function FolderList({ folders }) {
    const navigate = useNavigate();

    return (
        <div>
            <h2>Folders</h2>
            <ul>
                {folders.map((folder) => (
                    <li key={folder}>
                        <button onClick={() => navigate(`/${folder.id}`)}>
                            {folder.folderName}
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export default FolderList;
