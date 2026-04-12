import React, {useState} from "react";

export const MissionarySignupForm = ({onSubmit, onChange}) => {
    const [formData, setFormData] = useState({
        displayName: '',
        region: '',
        email: '',
        password: '',
    });

    return (

        <div style={{maxWidth: '400px', margin: '50px auto'}}>
            <h3>Missionary</h3>
            <form onSubmit={onSubmit}>
                <input
                    type="email"
                    placeholder="Email"
                    required={true}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                />
                <br/>
                <input
                    type="password"
                    placeholder="Password"
                    required={true}
                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                />
                <br/>
                <input
                    type={"text"}
                    placeholder="Display Name"
                    required={true}
                    onChange={(e) => setFormData({...formData, displayName: e.target.value})}
                />
                <br/>
                <input
                    type={"text"}
                    placeholder="Region"
                    required={false}
                    onChange={(e) => setFormData({...formData, displayName: e.target.value})}
                />
                <br/>
                <button
                    type="submit"
                    style={{
                        margin: '20px auto',
                        display: 'block',
                        padding: '10px 25px'
                    }}
                >
                    Sign Up
                </button>
            </form>
            <div className="social-login">
                <button
                    onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                    style={{
                        margin: '50px auto',
                        display: 'block',
                        padding: '10px 25px'
                    }}
                >
                    Sign Up with Google
                </button>
            </div>
        </div>
    )
}
export default MissionarySignupForm