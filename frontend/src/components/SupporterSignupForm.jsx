import React, {useState} from "react";

export const SupporterSignupForm = ({onSubmit, onChange}) => {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
    });

    return (

        <div style={{maxWidth: '400px', margin: '50px auto'}}>
            <h3>Supporter</h3>
            <form onSubmit={onSubmit}>
                <input
                    type={"text"}
                    placeholder="First Name"
                    required={true}
                    onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                />
                <br/>
                <input
                    type={"text"}
                    placeholder="Last Name"
                    required={true}
                    onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                />
                <br/>
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
export default SupporterSignupForm