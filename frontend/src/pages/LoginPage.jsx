import React, {useState} from 'react';
import axios from 'axios';
import {Link, NavLink, useNavigate} from 'react-router-dom';
import PublicHeader from "../components/PublicHeader.jsx";

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
        <>
            <PublicHeader/>

            <div
                className="bg-white flex min-h-screen"
            >

                <div className="w-full p-6 bg-white rounded-lg shadow-md flex flex-col items-center justify-center">
                    <h2 className="mb-8 text-header-1 font-sans:roboto text-center text-accent-mid-green">Log in</h2>
                    {error && <div className="text-red-500">{error}</div>}
                    <form
                        onSubmit={handleLogin}
                        className="flex flex-col items-center"
                    >
                        <input
                            className="block w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green transition-all duration-300"
                            placeholder="Email"
                            autoFocus
                            onChange={(e) => setEmail(e.target.value)}
                        />
                        <input
                            className="block w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green transition-all duration-300"
                            type="password"
                            placeholder="Password"
                            onChange={(e) => setPassword(e.target.value)}
                        />
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-1/2 px-4 py-2.5 rounded bg-accent-mid-green text-white hover:bg-accent-light-green hover:scale-105 drop-shadow-md hover:cursor-pointer focus:outline-none focus:ring-2 focus:ring-accent-mid-green focus:ring-offset-2 transition-all duration-300"
                        >
                            {isLoading ?
                                (
                                    <>
                                        <div
                                            className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"/>
                                        Logging in...
                                    </>
                                )
                                : 'Login'}
                        </button>
                    </form>
                    <div
                        className="my-4 flex flex-col items-center"
                    >
                        <button
                            onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                            className="drop-shadow-md inline-flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-white border border-gray-300 shadow-sm hover:bg-gray-100 hover:scale-105 hover:cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-all duration-300"
                        >
                            <img className="h-5 w-5" src="https://authjs.dev/img/providers/google.svg"
                                 alt="Google Logo"/>
                            <span>Continue with Google</span>
                        </button>
                    </div>
                    <hr className="border w-full max-w-1/2 border-gray-300"/>
                    <div className="my-4">
                        <h5 className="inline px-4">Don't have an account?</h5>
                        <Link
                            to="/register"
                            className="inline-flex text-accent-mid-green hover:text-accent-light-green hover:scale-105 hover:cursor-pointer transition-all duration-300"
                        >
                            <strong>Register an account</strong>
                        </Link>
                    </div>
                </div>
                <div className="w-full max-w-md bg-accent-dark-green ">

                    <div
                        className="flex flex-col items-center justify-center h-full"
                    >
                        <NavLink
                            to="https://www.biblegateway.com/passage/?search=Matthew%2028&version=ESV"
                        >
                            <blockquote
                                className="text-left text-white text-lg font-serif italic p-12">

                                “Go therefore and make disciples of all nations, baptizing them in the name of the
                                Father and of the Son and of the Holy Spirit, teaching them to observe all that I have
                                commanded
                                you.
                                <br/>
                                <br/>

                                And behold, I am with you always, to the end of the age.”
                                <br/>
                                <br/>
                                <strong className="text-accent-light-green">- Matthew 28:19-20 (ESV)</strong>
                            </blockquote>
                        </NavLink>
                    </div>
                </div>
            </div>
        </>
    );
};

export default LoginPage;
