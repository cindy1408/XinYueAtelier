import { API_URL } from '../config';

export default function Login() {

    const handleGoogleLogin = () => {
        window.location.assign(
            `${API_URL}/oauth2/authorization/google`
        );
    };

    return (
        <div>
            <h1>Login</h1>
            <button onClick={handleGoogleLogin}>
                Login with Google
            </button>
        </div>
    );
}