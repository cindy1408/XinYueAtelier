import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function AuthCallback() {
    const { login, token } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const tokenParam = params.get("token");
        if (tokenParam) {
            login(tokenParam);
        } else {
            navigate("/login", { replace: true });
        }
    }, []);

    // ✅ Wait for token to be set in context, then navigate
    useEffect(() => {
        if (token) {
            navigate("/", { replace: true });
        }
    }, [token]);

    return <p>Signing you in...</p>;
}