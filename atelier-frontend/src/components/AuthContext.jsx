import {useState, useEffect } from "react";
import { AuthContext } from "./AuthContextInstance";

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch("http://localhost:8080/api/me", {
            credentials: "include"
        })
            .then(async res => {
                if (res.ok) {
                    const user = await res.json();
                    setUser(user);
                } else {
                    setUser(null);
                }
            })
            .finally(() => setLoading(false));
    }, []);

    return (
        <AuthContext.Provider value={{ user, setUser,loading }}>
            {children}
        </AuthContext.Provider>
    );
}