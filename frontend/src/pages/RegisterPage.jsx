import {useState} from "react";
import {useNavigate, Link} from "react-router-dom";
import axios from "axios";
import MissionarySignupForm from "../components/MissionarySignupForm.jsx";
import SupporterSignupForm from "../components/SupporterSignupForm.jsx";

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
                id: response.data.id,
                username: response.data.username,
                role: response.data.role
            };
            localStorage.setItem("user", JSON.stringify(userData));
            onLogin(userData);
            navigate(registrationDto.role === "MISSIONARY" ? "/missionary" : "/supporter");
        } catch (error) {
            const body = error.response?.data;
            const message = typeof body === "string"
                ? body
                : body?.message || body?.error || "Registration failed";
            setError(message);
        }
    };

    return (
        <div style={{
            minHeight: "100vh",
            padding: "40px 16px",
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <div
                style={{
                    width: "100%",
                    maxWidth: "460px",
                    padding: "28px",
                    backgroundColor: "var(--bg-card)",
                    color: "var(--text)",
                    borderRadius: "12px",
                    border: "1px solid var(--border)"
                }}
            >
                <h2 style={{marginBottom: "20px", color: "var(--text-h)", textAlign: "center"}}>Register</h2>
                {error && <div style={{color: "var(--error)", marginBottom: "12px", textAlign: "center"}}>{error}</div>}
                <form onSubmit={handleRegister}>
                    <label htmlFor="role-select" style={{color: "var(--text-h)", fontWeight: "bold"}}>Role</label>
                    <select
                        id="role-select"
                        name="role"
                        value={formData.role}
                        onChange={handleChange}
                        style={{
                            width: "100%",
                            marginTop: "8px",
                            marginBottom: "12px",
                            boxSizing: "border-box",
                            padding: "12px",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)",
                            backgroundColor: "var(--bg-input)",
                            color: "var(--text-h)"
                        }}
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
                        style={{
                            width: "100%",
                            marginTop: "16px",
                            padding: "12px",
                            borderRadius: "8px",
                            border: "none",
                            backgroundColor: "var(--primary)",
                            color: "white",
                            cursor: "pointer",
                            fontWeight: "bold"
                        }}
                    >
                        Register
                    </button>
                </form>
                <div className="social-login">
                    <button
                        type="button"
                        onClick={() => window.location.href = "http://localhost:8080/oauth2/authorization/google"}
                        style={{
                            width: "100%",
                            marginTop: "12px",
                            padding: "12px",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)",
                            backgroundColor: "var(--bg-input)",
                            color: "var(--text-h)",
                            cursor: "pointer"
                        }}
                    >
                        Continue with Google
                    </button>
                </div>
                <hr style={{borderColor: 'var(--border)', margin: '20px 0'}}/>
                <div style={{textAlign: "center"}}>
                    <h5 style={{marginBottom: '8px', color: 'var(--text)'}}>Already have an account?</h5>
                    <Link
                        to="/login"
                        style={{
                            textDecoration: 'none',
                            color: 'var(--accent)'
                        }}
                    >
                        <strong>Log in</strong>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;
