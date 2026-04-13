import React from "react";

export const SupporterSignupForm = ({formData, onChange}) => {
    const fieldStyle = {
        width: "100%",
        marginBottom: "12px",
        boxSizing: "border-box",
        padding: "12px",
        borderRadius: "8px",
        border: "1px solid var(--border-input)",
        backgroundColor: "var(--bg-input)",
        color: "var(--text-h)"
    };

    return (
        <>
            <h3 style={{color: "var(--text-h)"}}>Supporter</h3>
            <input
                type="text"
                name="firstName"
                placeholder="First Name"
                value={formData.firstName}
                required={true}
                onChange={onChange}
                style={fieldStyle}
            />
            <input
                type="text"
                name="lastName"
                placeholder="Last Name"
                value={formData.lastName}
                required={true}
                onChange={onChange}
                style={fieldStyle}
            />
            <input
                type="email"
                name="email"
                placeholder="Email"
                value={formData.email}
                required={true}
                onChange={onChange}
                style={fieldStyle}
            />
            <input
                type="password"
                name="password"
                placeholder="Password"
                value={formData.password}
                required={true}
                onChange={onChange}
                style={fieldStyle}
            />
        </>
    );
};

export default SupporterSignupForm;
