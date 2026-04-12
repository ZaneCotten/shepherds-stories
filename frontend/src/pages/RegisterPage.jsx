import {useState} from "react";
import {useNavigate} from "react-router-dom";
import axios from "axios";
import MissionarySignupForm from "../components/MissionarySignupForm.jsx";
import SupporterSignupForm from "../components/SupporterSignupForm.jsx";

const RegisterPage = () => {
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

    const handleChange = (e) => {
        const {name, value} = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value
        }));
    };

    const handleRegister = async (e) => {
        e.preventDefault();

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
            await axios.post("/api/auth/register", registrationDto);
            navigate("/login");
        } catch (error) {
            console.error("Registration failed:", error);
        }
    };

    return (
        <div style={{maxWidth: "400px", margin: "50px auto"}}>
            <h2>Register</h2>
            <form onSubmit={handleRegister}>
                <label htmlFor="role-select">Role</label>
                <br/>
                <select id="role-select" name="role" value={formData.role} onChange={handleChange}>
                    <option value="SUPPORTER">Supporter</option>
                    <option value="MISSIONARY">Missionary</option>
                </select>
                <br/>
                <br/>

                {formData.role === "SUPPORTER" ? (
                    <SupporterSignupForm formData={formData} onChange={handleChange}/>
                ) : (
                    <MissionarySignupForm formData={formData} onChange={handleChange}/>
                )}
                <br/>
                <button type="submit">Register</button>
            </form>
            <div className="social-login">
                <button
                    type="button"
                    onClick={() => window.location.href = "http://localhost:8080/oauth2/authorization/google"}
                    style={{
                        margin: "20px auto",
                        display: "block",
                        padding: "10px 25px"
                    }}
                >
                    Sign Up with Google
                </button>
            </div>
        </div>
    );
};

export default RegisterPage;
