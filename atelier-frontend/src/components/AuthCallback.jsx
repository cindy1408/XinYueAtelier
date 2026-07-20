
import { useNavigate } from "react-router-dom";
import { useAuth } from "../components/useAuth";
import {useEffect} from "react";

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

    useEffect(() => {
        if (token) {
            navigate("/", { replace: true });
        }
    }, [token]);

    return <p>Signing you in...</p>;
}