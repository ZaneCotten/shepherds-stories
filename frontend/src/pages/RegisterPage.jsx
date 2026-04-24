import React, {useState} from "react";
import {useNavigate, Link, NavLink} from "react-router-dom";
import axios from "axios";
import MissionarySignupForm from "../components/MissionarySignupForm.jsx";
import SupporterSignupForm from "../components/SupporterSignupForm.jsx";
import PublicHeader from "../components/PublicHeader.jsx";

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
    const [isLoading, setIsLoading] = useState(false);

    const handleChange = (e) => {
        const {name, value} = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value
        }));
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

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
            const message = err.response?.data?.error || "Registration failed";
            setError(message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <PublicHeader/>

            <div className="bg-white flex min-h-screen">
                {/* Form Column */}
                <div className="w-full p-6 bg-white rounded-lg shadow-md flex flex-col items-center justify-center">
                    <h2 className="mb-8 text-header-1 font-sans:roboto text-center text-accent-mid-green">Register</h2>
                    {error && <div className="text-red-500 mb-4">{error}</div>}

                    <form onSubmit={handleRegister} className="flex flex-col items-center w-full">
                        <select
                            name="role"
                            value={formData.role}
                            onChange={handleChange}
                            className="block w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green hover:cursor-pointer hover:bg-gray-100 transition-all duration-300"
                        >
                            <option value="SUPPORTER">Supporter</option>
                            <option value="MISSIONARY">Missionary</option>
                        </select>

                        {formData.role === "SUPPORTER" ? (
                            <SupporterSignupForm formData={formData} onChange={handleChange}/>
                        ) : (
                            <MissionarySignupForm formData={formData} onChange={handleChange}/>
                        )}

                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-1/5 mt-4 px-4 py-2.5 rounded bg-accent-mid-green text-white hover:bg-accent-light-green hover:scale-105 hover:cursor-pointer drop-shadow-md transition-all duration-300"
                        >
                            {isLoading ? "Registering..." : "Register"}
                        </button>
                    </form>

                    <div className="my-4 flex flex-col items-center">
                        <button
                            onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                            className="drop-shadow-md inline-flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium text-gray-700 bg-white border border-gray-300 shadow-sm hover:bg-gray-100 hover:cursor-pointer hover:scale-105 transition-all duration-300"
                        >
                            <img className="h-5 w-5" src="https://authjs.dev/img/providers/google.svg"
                                 alt="Google Logo"/>
                            <span>Continue with Google</span>
                        </button>
                    </div>

                    <hr className="border w-full max-w-1/2 border-gray-300"/>
                    <div className="my-4">
                        <h5 className="inline px-4">Already have an account?</h5>
                        <Link
                            to="/login"
                            className="inline-flex text-accent-mid-green hover:text-accent-light-green hover:scale-105 transition-all duration-300"
                        >
                            <strong>Log in</strong>
                        </Link>
                    </div>
                </div>


                <div className="w-full max-w-md bg-accent-dark-green">
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