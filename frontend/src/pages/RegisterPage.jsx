import React, {useState} from 'react';
import axios from 'axios';
import {useNavigate} from 'react-router-dom';
import MissionarySignupForm from "../components/MissionarySignupForm.jsx";
import SupporterSignupForm from "../components/SupporterSignupForm.jsx";

const RegisterPage = () => {
    const [formData, setFormData] = useState({
        role: 'SUPPORTER', // Default role
        email: '',
        password: ''
    });
    const navigate = useNavigate();

    const [role, setRole] = useState('SUPPORTER'); // Default to SUPPORTER

    const handleRegister = async (e) => {
        e.preventDefault();
        try {
            // Hits your Spring Boot @PostMapping("/api/auth/register")
            await axios.post('/api/auth/register', formData);
            navigate('/login');
        } catch (err) {
            alert(`${role} registration failed`);
        }
    };

    return (
        <div className="register-page">
            <div>
                <h2 style={{margin: '25px'}}>Join Shepherd's Stories</h2>

                <button style={{margin: '0 25px'}} onClick={() => setRole('SUPPORTER')}>SUPPORTER</button>

                <button style={{margin: '0 25px'}} onClick={() => setRole('MISSIONARY')}>MISSIONARY</button>
                {role === 'SUPPORTER' ?
                    <SupporterSignupForm onChange={(e) => setFormData({
                        ...formData,
                        [e.target.name]: e.target.value
                    })} onSubmit={handleRegister}
                    /> :
                    <MissionarySignupForm onChange={(e) => setFormData({
                        ...formData,
                        [e.target.name]: e.target.value
                    })} onSubmit={handleRegister}
                    />
                }
            </div>
            <hr/>
            <div>
                <h5>Already have an account?</h5>
                <a
                    href="/login"
                    style={{
                        margin: 'auto',
                        textAlign: 'center',
                        textDecoration: 'none',
                        color: 'white'
                    }}
                >
                    <strong>Log in</strong>
                </a>
            </div>
        </div>

    );
};

export default RegisterPage;