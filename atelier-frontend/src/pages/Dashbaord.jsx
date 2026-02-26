import { useEffect } from "react";
import { useAuth } from "../components/AuthContext";
import axios from "axios";

export default function Dashboard() {
    const { token, user, setUser, logout } = useAuth();

    useEffect(() => {
        axios.get("http://localhost:8080/api/me", {
            headers: { Authorization: `Bearer ${token}` }
        }).then(res => setUser(res.data));
    }, []);

    return (
        <div>
            <h1>Dashboard</h1>
            {user && <p>Welcome, {user.name} ({user.email})</p>}
            <button onClick={logout}>Logout</button>
        </div>
    );
}