import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../components/useAuth";

export default function AuthCallback() {
    const { login } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const tokenParam = params.get("token");
        if (tokenParam) {
            login(tokenParam);
            navigate("/", { replace: true });
        } else {
            navigate("/login", { replace: true });
        }
    }, []); 

    return <p>Signing you in...</p>;
}