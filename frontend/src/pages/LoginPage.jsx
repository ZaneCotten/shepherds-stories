import React, {useState} from 'react';
import axios from 'axios';
import {Link, useNavigate} from 'react-router-dom';

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
        <div style={{
            minHeight: '100vh',
            padding: '40px 16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <div
                style={{
                    width: '100%',
                    maxWidth: '420px',
                    padding: '28px',
                    backgroundColor: 'var(--bg-card)',
                    color: 'var(--text)',
                    borderRadius: '12px',
                    border: '1px solid var(--border)'
                }}
            >
                <h2 style={{marginBottom: '20px', color: 'var(--text-h)', textAlign: 'center'}}>Login</h2>
                {error && <div style={{color: 'var(--error)', marginBottom: '12px', textAlign: 'center'}}>{error}</div>}
                <form onSubmit={handleLogin}>
                    <input
                        style={{
                            width: '100%',
                            marginBottom: '12px',
                            boxSizing: 'border-box',
                            padding: '12px',
                            borderRadius: '8px',
                            border: '1px solid var(--border-input)',
                            backgroundColor: 'var(--bg-input)',
                            color: 'var(--text-h)'
                        }}
                        placeholder="Email"
                        onChange={(e) => setEmail(e.target.value)}
                    />
                    <input
                        style={{
                            width: '100%',
                            marginBottom: '16px',
                            boxSizing: 'border-box',
                            padding: '12px',
                            borderRadius: '8px',
                            border: '1px solid var(--border-input)',
                            backgroundColor: 'var(--bg-input)',
                            color: 'var(--text-h)'
                        }}
                        type="password"
                        placeholder="Password"
                        onChange={(e) => setPassword(e.target.value)}
                    />
                    <button
                        type="submit"
                        disabled={isLoading}
                        style={{
                            width: '100%',
                            padding: '12px',
                            borderRadius: '8px',
                            border: 'none',
                            backgroundColor: 'var(--primary)',
                            color: 'white',
                            cursor: 'pointer',
                            fontWeight: 'bold'
                        }}
                    >
                        {isLoading ? 'Logging in...' : 'Login'}
                    </button>
                </form>
                <div className="social-login">
                    <button
                        onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                        style={{
                            width: '100%',
                            marginTop: '12px',
                            padding: '12px',
                            borderRadius: '8px',
                            border: '1px solid var(--border-input)',
                            backgroundColor: 'var(--bg-input)',
                            color: 'var(--text-h)',
                            cursor: 'pointer'
                        }}
                    >
                        Continue with Google
                    </button>
                </div>
                <hr style={{borderColor: 'var(--border)', margin: '20px 0'}}/>
                <div style={{textAlign: 'center'}}>
                    <h5 style={{marginBottom: '8px', color: 'var(--text)'}}>Don't have an account?</h5>
                    <Link
                        to="/register"
                        style={{
                            textDecoration: 'none',
                            color: 'var(--accent)'
                        }}
                    >
                        <strong>Register an account</strong>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
