import React from "react";

export const MissionarySignupForm = ({formData, onChange}) => {
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
            <h3 style={{color: "var(--text-h)"}}>Missionary</h3>
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
            <input
                type="text"
                name="displayName"
                placeholder="Display Name"
                value={formData.displayName}
                required={true}
                onChange={onChange}
                style={fieldStyle}
            />
            <input
                type="text"
                name="region"
                placeholder="Region"
                value={formData.region}
                onChange={onChange}
                style={fieldStyle}
            />
            <textarea
                name="biography"
                placeholder="Biography"
                value={formData.biography}
                onChange={onChange}
                style={{...fieldStyle, minHeight: "110px"}}
            />
        </>
    );
};

export default MissionarySignupForm;
