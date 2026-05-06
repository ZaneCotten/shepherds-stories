import React, {useState} from "react";
import {useNavigate, Link, NavLink} from "react-router-dom";
import axios from "axios";
import MissionarySignupForm from "../components/MissionarySignupForm.jsx";
import SupporterSignupForm from "../components/SupporterSignupForm.jsx";
import PublicHeader from "../components/PublicHeader.jsx";
import {validatePassword} from "../utils/passwordValidator";

const RegisterPage = ({onLogin}) => {
    const [formData, setFormData] = useState({
        role: "SUPPORTER",
        email: "",
        password: "",
        firstName: "",
        lastName: "",
        displayName: "",
        region: "",
        biography: ""
    });

    const navigate = useNavigate();
    const [error, setError] = useState("");
    const [passwordError, setPasswordError] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleChange = (e) => {
        const {name, value} = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value
        }));
        if (name === "password") {
            setPasswordError(false);
        }
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

        if (!validatePassword(formData.password)) {
            setPasswordError(true)
            setIsLoading(false);
            return;
        }

        const registrationDto = {
            email: formData.email,
            password: formData.password,
            role: formData.role,
            ...(formData.role === "SUPPORTER" && {
                firstName: formData.firstName,
                lastName: formData.lastName
            }),
            ...(formData.role === "MISSIONARY" && {
                displayName: formData.displayName,
                region: formData.region,
                biography: formData.biography
            })
        };

        try {
            const response = await axios.post("/api/auth/register", registrationDto);
            const userData = {
                ...response.data,
                role: (response.data?.role || '').replace('ROLE_', '')
            };

            onLogin(userData);
            navigate(registrationDto.role === "MISSIONARY" ? "/missionary" : "/supporter");
        } catch (err) {
            const data = err.response?.data;
            const message = data?.error || data?.detail || data?.message || "Registration failed";
            setError(message);
            if (message.toLowerCase().includes("password")) {
                setPasswordError(true);
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <PublicHeader/>

            <div className="bg-white flex flex-col md:flex-row min-h-screen">
                <div
                    className="w-full md:w-1/2 p-6 md:p-12 bg-white rounded-lg shadow-md flex flex-col items-center justify-center">
                    <h2 className="mb-8 text-header-3 sm:text-header-1 font-sans:roboto text-center text-accent-mid-green leading-tight">Register</h2>
                    {error && <div className="text-red-500 mb-4 text-center max-w-md">{error}</div>}

                    <form onSubmit={handleRegister} className="flex flex-col items-center w-full">
                        <select
                            name="role"
                            value={formData.role}
                            onChange={handleChange}
                            className="block w-full max-w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green hover:cursor-pointer hover:bg-gray-100 transition-all duration-300"
                        >
                            <option value="SUPPORTER">Supporter</option>
                            <option value="MISSIONARY">Missionary</option>
                        </select>

                        {formData.role === "SUPPORTER" ? (
                            <SupporterSignupForm formData={formData} onChange={handleChange}
                                                 passwordError={passwordError}/>
                        ) : (
                            <MissionarySignupForm formData={formData} onChange={handleChange}
                                                  passwordError={passwordError}/>
                        )}

                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full max-w-md mt-4 px-4 py-2.5 rounded bg-accent-mid-green text-white hover:bg-accent-light-green hover:scale-105 hover:cursor-pointer drop-shadow-md transition-all duration-300"
                        >
                            {isLoading ? "Registering..." : "Register"}
                        </button>
                    </form>

                    <div className="my-4 flex flex-col items-center">
                        <button
                            onClick={() => {
                                const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
                                window.location.href = `${backendUrl}/oauth2/authorization/google`;
                            }}
                            className="drop-shadow-md flex w-full max-w-md items-center justify-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-white border border-gray-300 shadow-sm hover:bg-gray-100 hover:cursor-pointer hover:scale-105 transition-all duration-300"
                        >
                            <img className="h-5 w-5" src="https://authjs.dev/img/providers/google.svg"
                                 alt="Google Logo"/>
                            <span>Continue with Google</span>
                        </button>
                    </div>

                    <hr className="border w-full max-w-md border-gray-300"/>
                    <div className="my-4 flex flex-col items-center justify-center sm:flex-row sm:gap-2 text-center">
                        <h5 className="text-gray-600">Already have an account?</h5>
                        <Link
                            to="/login"
                            className="text-accent-mid-green hover:text-accent-light-green hover:scale-105 transition-all duration-300"
                        >
                            <strong>Log in</strong>
                        </Link>
                    </div>
                </div>


                <div className="w-full md:w-1/2 bg-accent-dark-green flex-1">
                    <div className="flex flex-col items-center justify-center h-full">
                        <NavLink to="https://www.biblegateway.com/passage/?search=ephesians%206&version=ESV">
                            <blockquote className="text-left text-white text-lg font-serif italic p-12">
                                “Pray in the Spirit at all times and on every occasion.
                                <br/><br/>
                                Stay alert and be persistent in your prayers for all believers everywhere.”
                                <br/><br/>


                                <strong className="text-accent-light-green">– Ephesians 6:18 (NLT)</strong>
                            </blockquote>
                        </NavLink>
                    </div>
                </div>
            </div>
        </>
    );
};

export default RegisterPage;