import { createContext, useContext, useState, useEffect } from "react";

const AuthContext = createContext();

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem("token"));
    const [user, setUser]   = useState(null);
     const [loading, setLoading] = useState(!!localStorage.getItem("token"));

    useEffect(() => {
        if (!token) {
            setLoading(false); // ✅ no token, stop loading
            return;
        }
        fetch("http://localhost:8080/api/me", {
            headers: { Authorization: `Bearer ${token}` }
        })
            .then(res => {
                if (!res.ok) throw new Error("Unauthorized");
                return res.json();
            })
            .then(data => {
                setUser(data);
                setLoading(false); // ✅ success, stop loading
            })
            .catch(() => {
                localStorage.removeItem("token");
                setToken(null);
                setUser(null);
                setLoading(false); // ✅ already there
            });
    }, [token]);


    const login = (newToken) => {
        localStorage.setItem("token", newToken);
        setToken(newToken);
    };

    const logout = () => {
        localStorage.removeItem("token");
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ token, user, setUser, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => useContext(AuthContext);