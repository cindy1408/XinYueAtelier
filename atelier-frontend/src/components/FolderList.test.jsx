import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, it, expect } from 'vitest'
import FolderList from "./FolderList";

const mockFolders = [
    {
        id: "123",
        folderName: "Bodice Patterns",
        garmentType: "DRESS",
        origin: "SELF_DRAFTED",
        level: "ADVANCED",
        imagePath: null,
    },
];

describe("FolderList", () => {
    it("renders folder information", () => {
        render(
            <MemoryRouter>
                <FolderList folders={mockFolders} onEdit={() => { }} />
            </MemoryRouter>,
        );

        expect(screen.getByText("Bodice Patterns")).toBeInTheDocument();
        expect(screen.getByText("DRESS")).toBeInTheDocument();
        expect(screen.getByText("SELF_DRAFTED")).toBeInTheDocument();
        expect(screen.getByText("ADVANCED")).toBeInTheDocument();
        expect(screen.getByText("Go to folder")).toBeInTheDocument();
    });

    it("shows fallback text when no folders are provided", () => {
        render(
            <MemoryRouter>
                <FolderList folders={[]} onEdit={() => { }} />
            </MemoryRouter>,
        );

        expect(screen.getByText("No folders found")).toBeInTheDocument();
    });
});
