import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../components/useAuth";

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
    }, [login, navigate]);

    useEffect(() => {
        if (token) {
            navigate("/", { replace: true });
        }
    }, [token, navigate]); 

    return <p>Signing you in...</p>;
}