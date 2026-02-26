export default function Login() {
const handleGoogleLogin = () => {
    window.location.href = "http://localhost:8080/api/auth/oauth2/authorization/google";
};

    return (
        <div>
            <h1>Atelier</h1>
            <button onClick={handleGoogleLogin}>Sign in with Google</button>
        </div>
    );
}