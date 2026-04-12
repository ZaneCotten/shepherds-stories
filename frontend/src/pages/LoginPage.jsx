import React, {useState} from 'react';
import axios from 'axios';
import {useNavigate} from 'react-router-dom';

const LoginPage = ({onLogin}) => {

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        const params = new URLSearchParams();
        params.append('email', email);
        params.append('password', password);

        try {
            const response = await axios.post('/api/auth/login', params, {
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            });

            const userData = {
                ...response.data,
                role: (response.data?.role || '').replace('ROLE_', '')
            };

            onLogin(userData); // Pass the whole object up to App.jsx

            if (userData.role === 'MISSIONARY') {
                navigate('/missionary');
            } else if (userData.role === 'SUPPORTER') {
                navigate('/supporter');
            } else {
                navigate('/home');
            }
        } catch (err) {
            const message = err.response?.data?.error || 'Login failed';
            setError(message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={{maxWidth: '400px', margin: '50px auto'}}>
            <h2>Login</h2>
            {error && <div style={{color: 'red'}}>{error}</div>}
            <form onSubmit={handleLogin}>
                <input
                    style={{margin: 'auto 25px'}}
                    placeholder="Email"
                    onChange={(e) => setEmail(e.target.value)}
                />
                <input
                    style={{margin: 'auto 25px'}}
                    type="password"
                    placeholder="Password"
                    onChange={(e) => setPassword(e.target.value)}
                />
                <button type="submit"
                        disabled={isLoading}
                        style={{
                            margin: '20px auto',
                            display: 'block',
                            padding: '10px 25px'
                        }}
                >
                    {isLoading ? 'Logging in...' : 'Login'}
                </button>
            </form>
            <div className="social-login">
                <button
                    onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                    style={{
                        margin: '20px auto',
                        display: 'block',
                        padding: '10px 25px'
                    }}
                >
                    Login with Google
                </button>
            </div>
            <hr/>
            <div>
                <h5>Don't have an account?</h5>
                <a
                    href="/register"
                    style={{
                        margin: 'auto',
                        textAlign: 'center',
                        textDecoration: 'none',
                        color: 'white'
                    }}
                >
                    <strong>Register an account</strong>
                </a>
            </div>
        </div>
    );
};

export default LoginPage;
