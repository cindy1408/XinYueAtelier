import { apiFetch } from '../api/apiFetch';
import { API_URL } from '../config';

export default function Login() {
const handleGoogleLogin = () => {
    window.location.href = `${API_URL}/api/auth/oauth2/authorization/google`;
};

    return (
        <div>
            <h1>Atelier</h1>
            <button onClick={handleGoogleLogin}>Sign in with Google</button>
        </div>
    );
}